package com.example.util

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Looper
import android.os.Process
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.BuildConfig
import com.example.ui.activities.CrashReportActivity
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.system.exitProcess

/**
 * Global Exception Handler and Crash Diagnostic Logger for FlowTest.
 * Logs all uncaught application exceptions to a local text file and provides
 * seamless user sharing via email and clipboard to aid in rapid bug fixing.
 */
object CrashLogger {

    private const val TAG = "CrashLogger"
    const val PREF_NAME = "flowtest_crash_diagnostics"
    const val KEY_LAST_REPORT = "last_crash_report"
    const val KEY_LAST_TIMESTAMP = "last_crash_timestamp"
    const val KEY_TOTAL_CRASHES = "total_crash_count"
    const val KEY_CRASH_FILE_PATH = "last_crash_file_path"
    const val SUPPORT_EMAIL = "innobright2010@gmail.com"

    private const val CRASH_DIR_NAME = "crash_logs"
    private const val LATEST_LOG_FILENAME = "latest_crash.txt"
    private const val HISTORY_LOG_FILENAME = "crash_history.log"

    private var isInstalled = false

    /**
     * Install the global uncaught exception handler in the Application.
     */
    fun install(application: Application) {
        if (isInstalled) return
        isInstalled = true

        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val isMainThread = thread == Looper.getMainLooper().thread || thread.name.equals("main", ignoreCase = true)

            try {
                Log.e(TAG, "FATAL: Uncaught exception intercepted on thread '${thread.name}' (isMain=$isMainThread)", throwable)

                // 1. Generate full diagnostic report and write to local text file
                val (reportText, crashFile) = recordCrashToFile(application, thread, throwable)

                // 2. Proactively auto-heal database if this was a Room/SQLite schema mismatch
                val isDbError = throwable.message?.contains("Room cannot verify the data integrity", ignoreCase = true) == true ||
                                throwable.message?.contains("migration", ignoreCase = true) == true ||
                                throwable is android.database.sqlite.SQLiteException
                if (isDbError) {
                    try {
                        Log.w(TAG, "Room schema corruption detected. Resetting local database...")
                        com.example.data.db.AppDatabase.resetDatabase(application)
                    } catch (dbErr: Throwable) {
                        Log.e(TAG, "Auto-heal database reset error: ${dbErr.message}")
                    }
                }

                // 3. Launch dedicated CrashReportActivity so the user sees the error and can email it
                try {
                    val crashIntent = Intent(application, CrashReportActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        putExtra(CrashReportActivity.EXTRA_CRASH_REPORT, reportText)
                        putExtra(CrashReportActivity.EXTRA_CRASH_FILE, crashFile.absolutePath)
                        putExtra(CrashReportActivity.EXTRA_IS_MAIN_THREAD, isMainThread)
                    }
                    application.startActivity(crashIntent)

                    // Allow Android ActivityManager a brief window to dispatch the intent to the new process
                    try {
                        Thread.sleep(400)
                    } catch (_: InterruptedException) {}

                    // Terminate current crashed process cleanly
                    Process.killProcess(Process.myPid())
                    exitProcess(10)
                    return@setDefaultUncaughtExceptionHandler
                } catch (actErr: Throwable) {
                    Log.e(TAG, "Failed to launch CrashReportActivity: ${actErr.message}")
                }

            } catch (loggingErr: Throwable) {
                Log.e(TAG, "Critical error during crash logging: ${loggingErr.message}", loggingErr)
            }

            // Fallback to previous handler if our dedicated crash UI fails
            previousHandler?.uncaughtException(thread, throwable)
        }

        Log.i(TAG, "FlowTest Global CrashLogger installed successfully.")
    }

    /**
     * Records a crash report to a local text file and SharedPreferences.
     * Returns the full report text and the target text File.
     */
    @Synchronized
    fun recordCrashToFile(context: Context, thread: Thread, throwable: Throwable): Pair<String, File> {
        val now = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS z", Locale.US).apply {
            timeZone = TimeZone.getDefault()
        }
        val formattedDate = dateFormat.format(Date(now))

        val isMain = thread == Looper.getMainLooper().thread || thread.name.equals("main", ignoreCase = true)
        val stackTrace = getFullStackTraceString(throwable)

        val runtime = Runtime.getRuntime()
        val freeMemoryMb = runtime.freeMemory() / (1024 * 1024)
        val totalMemoryMb = runtime.totalMemory() / (1024 * 1024)
        val maxMemoryMb = runtime.maxMemory() / (1024 * 1024)

        val report = buildString {
            appendLine("=================================================================")
            appendLine("FLOWTEST CRASH & BUG REPORT")
            appendLine("=================================================================")
            appendLine("App Version       : v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})")
            appendLine("Package Name      : ${context.packageName}")
            appendLine("Timestamp         : $formattedDate (epoch: $now)")
            appendLine("Device Model      : ${Build.MANUFACTURER} ${Build.MODEL} (${Build.PRODUCT})")
            appendLine("Brand / Board     : ${Build.BRAND} / ${Build.BOARD}")
            appendLine("Android OS        : Android ${Build.VERSION.RELEASE} (API level ${Build.VERSION.SDK_INT})")
            appendLine("Build ID / Finger : ${Build.DISPLAY} / ${Build.FINGERPRINT}")
            appendLine("Supported ABIs    : ${Build.SUPPORTED_ABIS.joinToString(", ")}")
            appendLine("Memory (Free/Tot) : ${freeMemoryMb}MB free / ${totalMemoryMb}MB total (Max: ${maxMemoryMb}MB)")
            appendLine("Thread Info       : '${thread.name}' (id=${thread.id}, isMainThread=$isMain)")
            appendLine("-----------------------------------------------------------------")
            appendLine("EXCEPTION DETAILS:")
            appendLine("Type              : ${throwable.javaClass.name}")
            appendLine("Message           : ${throwable.message ?: "(null message)"}")
            appendLine("Cause Chain       : ${getCauseSummary(throwable)}")
            appendLine("-----------------------------------------------------------------")
            appendLine("FULL STACK TRACE:")
            appendLine(stackTrace)
            appendLine("=================================================================")
            appendLine("Generated automatically by FlowTest CrashLogger Engine.")
        }

        // 1. Write to primary latest_crash.txt file in internal app storage
        val crashDir = File(context.filesDir, CRASH_DIR_NAME).apply { mkdirs() }
        val latestFile = File(crashDir, LATEST_LOG_FILENAME)

        try {
            FileOutputStream(latestFile, false).use { fos ->
                fos.write(report.toByteArray(Charsets.UTF_8))
                fos.flush()
                try {
                    fos.fd.sync() // Ensure write hits disk
                } catch (_: Throwable) {}
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Failed writing latest crash log: ${e.message}")
        }

        // 2. Append to persistent crash history log
        try {
            val historyFile = File(crashDir, HISTORY_LOG_FILENAME)
            FileOutputStream(historyFile, true).use { fos ->
                fos.write("\n\n--- CRASH AT $formattedDate ---\n".toByteArray(Charsets.UTF_8))
                fos.write(report.toByteArray(Charsets.UTF_8))
                fos.flush()
            }
        } catch (_: Throwable) {}

        // 3. Save to SharedPreferences for rapid UI checks
        try {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val count = prefs.getInt(KEY_TOTAL_CRASHES, 0) + 1
            prefs.edit()
                .putString(KEY_LAST_REPORT, report)
                .putString(KEY_CRASH_FILE_PATH, latestFile.absolutePath)
                .putLong(KEY_LAST_TIMESTAMP, now)
                .putInt(KEY_TOTAL_CRASHES, count)
                .commit()
        } catch (_: Throwable) {}

        return Pair(report, latestFile)
    }

    /**
     * Reads the latest saved crash log text, or null if none exists.
     */
    fun getLatestCrashLog(context: Context): String? {
        // First attempt reading from local text file
        try {
            val file = getLatestCrashLogFile(context)
            if (file != null && file.exists() && file.length() > 0) {
                return file.readText(Charsets.UTF_8)
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Error reading crash text file: ${e.message}")
        }

        // Fallback to SharedPreferences
        return try {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            prefs.getString(KEY_LAST_REPORT, null)
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Returns the local text File for the latest crash report.
     */
    fun getLatestCrashLogFile(context: Context): File? {
        val file = File(context.filesDir, "$CRASH_DIR_NAME/$LATEST_LOG_FILENAME")
        return if (file.exists() && file.length() > 0) file else null
    }

    /**
     * Opens an Intent to share the crash report via Email (pre-filling recipient, subject, body,
     * and attaching the local text file via FileProvider).
     */
    fun shareCrashLogViaEmail(
        context: Context,
        customReportText: String? = null,
        crashFile: File? = null
    ) {
        val report = customReportText ?: getLatestCrashLog(context) ?: "No crash log recorded."
        val fileToAttach = crashFile ?: getLatestCrashLogFile(context)

        val subject = "[FlowTest Crash Report] v${BuildConfig.VERSION_NAME} - ${Build.MANUFACTURER} ${Build.MODEL}"

        val emailBody = buildString {
            appendLine("Hi FlowTest Developer / Support Team,")
            appendLine()
            appendLine("Here is the crash report from my device for FlowTest Android App.")
            appendLine()
            appendLine("--- DEVICE & APP INFO ---")
            appendLine("App Version: v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE}, SDK ${Build.VERSION.SDK_INT})")
            appendLine()
            appendLine("--- FULL CRASH LOG ---")
            appendLine(report)
            appendLine()
            appendLine("--- END OF REPORT ---")
        }

        val emailIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(SUPPORT_EMAIL))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, emailBody)

            // Attach local file via FileProvider if available
            if (fileToAttach != null && fileToAttach.exists()) {
                try {
                    val fileUri: Uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        fileToAttach
                    )
                    putExtra(Intent.EXTRA_STREAM, fileUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (uriErr: Throwable) {
                    Log.w(TAG, "FileProvider URI resolution notice: ${uriErr.message}")
                }
            }
        }

        try {
            val chooser = Intent.createChooser(emailIntent, "Send Crash Report via Email").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to launch email chooser: ${e.message}")
            // Fallback: Copy to clipboard and notify
            copyToClipboard(context, report)
            Toast.makeText(context, "No email client found. Log copied to clipboard!", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Copies the given text to the Android system clipboard.
     */
    fun copyToClipboard(context: Context, text: String) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = ClipData.newPlainText("FlowTest Crash Log", text)
            clipboard?.setPrimaryClip(clip)
            Toast.makeText(context, "Crash log copied to clipboard!", Toast.LENGTH_SHORT).show()
        } catch (e: Throwable) {
            Log.e(TAG, "Clipboard copy error: ${e.message}")
        }
    }

    /**
     * Clears recorded crash logs and resets count.
     */
    fun clearCrashLogs(context: Context) {
        try {
            val crashDir = File(context.filesDir, CRASH_DIR_NAME)
            if (crashDir.exists()) {
                crashDir.deleteRecursively()
            }
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            prefs.edit().clear().apply()
            Toast.makeText(context, "Crash logs cleared.", Toast.LENGTH_SHORT).show()
        } catch (e: Throwable) {
            Log.e(TAG, "Failed clearing crash logs: ${e.message}")
        }
    }

    private fun getFullStackTraceString(throwable: Throwable): String {
        return try {
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            throwable.printStackTrace(pw)
            pw.flush()
            sw.toString()
        } catch (_: Throwable) {
            Log.getStackTraceString(throwable)
        }
    }

    private fun getCauseSummary(throwable: Throwable): String {
        val causes = mutableListOf<String>()
        var current: Throwable? = throwable.cause
        while (current != null && causes.size < 5) {
            causes.add("${current.javaClass.simpleName}: ${current.message}")
            current = current.cause
        }
        return if (causes.isEmpty()) "None" else causes.joinToString(" -> ")
    }
}
