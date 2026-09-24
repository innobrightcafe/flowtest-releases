package com.example.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Locale

/**
 * Utility helper for exporting, caching, and sharing the installed FlowTest application APK
 * from phone to phone (via Quick Share, Bluetooth, Wi-Fi Direct, WhatsApp, Files, etc.)
 * with zero reliance on active internet connection.
 */
object ApkSharingHelper {

    private const val SHARED_DIR_NAME = "shared_apk"

    /**
     * Gets the dynamic APK file name including the current installed app version.
     */
    fun getApkFileName(context: Context): String {
        val vName = try {
            val pkgName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0)).versionName
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            }
            pkgName?.takeIf { it.isNotBlank() } ?: com.example.BuildConfig.VERSION_NAME
        } catch (e: Exception) {
            com.example.BuildConfig.VERSION_NAME
        }
        return "FlowTest_v${vName}.apk"
    }

    /**
     * Locates the source installed APK on the current Android device.
     */
    fun getInstalledApkSource(context: Context): File? {
        val path = context.applicationInfo?.sourceDir ?: context.packageCodePath
        if (path.isNullOrBlank()) return null
        val file = File(path)
        return if (file.exists() && file.length() > 0) file else null
    }

    /**
     * Formats the size of the installed APK in Megabytes.
     */
    fun getApkSizeMb(context: Context): String {
        val src = getInstalledApkSource(context)
        val bytes = src?.length() ?: 0L
        if (bytes <= 0L) return "18.5 MB"
        val mb = bytes.toDouble() / (1024.0 * 1024.0)
        return String.format(Locale.US, "%.1f MB", mb)
    }

    /**
     * Copies the installed APK into a shareable cache directory recognized by FileProvider.
     */
    suspend fun prepareShareableApk(context: Context): File? = withContext(Dispatchers.IO) {
        try {
            val srcFile = getInstalledApkSource(context) ?: return@withContext null
            val targetDir = File(context.cacheDir, SHARED_DIR_NAME)
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }
            val fileName = getApkFileName(context)
            val targetFile = File(targetDir, fileName)

            // Remove any outdated APK files from prior builds in the cache directory
            targetDir.listFiles()?.forEach { oldFile ->
                if (oldFile.name != fileName && oldFile.name.endsWith(".apk")) {
                    try { oldFile.delete() } catch (_: Exception) {}
                }
            }

            // Copy fresh if target doesn't exist, size differs, or source is newer than target
            val isOutOfDate = !targetFile.exists() ||
                targetFile.length() != srcFile.length() ||
                targetFile.lastModified() < srcFile.lastModified()

            if (isOutOfDate) {
                FileInputStream(srcFile).use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output, bufferSize = 64 * 1024)
                    }
                }
                targetFile.setLastModified(srcFile.lastModified())
            }
            targetFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Shares the FlowTest APK via Android's native share sheet.
     * Compatible with Quick Share / Nearby Share, Bluetooth, Xender, WhatsApp, etc.
     */
    fun shareApkFile(
        context: Context,
        apkFile: File,
        targetBluetoothOnly: Boolean = false
    ) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val apkUri: Uri = FileProvider.getUriForFile(context, authority, apkFile)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.android.package-archive"
                putExtra(Intent.EXTRA_STREAM, apkUri)
                putExtra(Intent.EXTRA_SUBJECT, "FlowTest Android App")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Here is the official FlowTest Android app (APK). Install directly to enjoy encrypted VPN tunnels and automated VTU services!"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (targetBluetoothOnly) {
                // Attempt to target Bluetooth direct transfer
                val btIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/vnd.android.package-archive"
                    putExtra(Intent.EXTRA_STREAM, apkUri)
                    setPackage("com.android.bluetooth")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                val activities = context.packageManager.queryIntentActivities(btIntent, PackageManager.MATCH_DEFAULT_ONLY)
                if (activities.isNotEmpty()) {
                    for (resolveInfo in activities) {
                        context.grantUriPermission(
                            resolveInfo.activityInfo.packageName,
                            apkUri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    }
                    context.startActivity(btIntent)
                    return
                }
            }

            // Fallback or standard: System Chooser with Quick Share, Nearby Share, Bluetooth, etc.
            val chooserTitle = "Share FlowTest APK to Nearby Phone"
            val chooser = Intent.createChooser(shareIntent, chooserTitle).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val resolveList = context.packageManager.queryIntentActivities(chooser, PackageManager.MATCH_DEFAULT_ONLY)
            for (resolveInfo in resolveList) {
                context.grantUriPermission(
                    resolveInfo.activityInfo.packageName,
                    apkUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }

            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Could not launch share sheet: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Directly launches the native Android system share chooser to share the app (APK file and invite text)
     * to ANY app (WhatsApp, Telegram, Bluetooth, Messages, Drive, Quick Share, Gmail, etc.) without any custom drawer.
     */
    fun launchNativeShare(
        context: Context,
        coroutineScope: kotlinx.coroutines.CoroutineScope? = null,
        referralCode: String? = null
    ) {
        val shareMessage = buildString {
            append("🚀 FlowTest - High-Speed VIP VPN & Automated Cheap VTU\n")
            append("Enjoy encrypted WireGuard tunnels and discounted Data, Airtime & Utilities!\n\n")
            if (!referralCode.isNullOrBlank()) {
                append("🎁 Referral Code: $referralCode\n")
                append("🔗 Join with link: https://flowtest2026.com/ref/$referralCode\n\n")
            }
            append("🌐 Direct Download: $OFFICIAL_DOWNLOAD_URL")
        }

        val triggerShare = { apkFile: File? ->
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                if (apkFile != null && apkFile.exists()) {
                    val authority = "${context.packageName}.fileprovider"
                    val apkUri: Uri = FileProvider.getUriForFile(context, authority, apkFile)
                    type = "application/vnd.android.package-archive"
                    putExtra(Intent.EXTRA_STREAM, apkUri)
                    putExtra(Intent.EXTRA_SUBJECT, "FlowTest App (APK)")
                    putExtra(Intent.EXTRA_TEXT, shareMessage)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } else {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "FlowTest App")
                    putExtra(Intent.EXTRA_TEXT, shareMessage)
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(sendIntent, "Share FlowTest App to any app").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            try {
                if (apkFile != null && apkFile.exists()) {
                    val authority = "${context.packageName}.fileprovider"
                    val apkUri: Uri = FileProvider.getUriForFile(context, authority, apkFile)
                    val resolveList = context.packageManager.queryIntentActivities(chooser, PackageManager.MATCH_DEFAULT_ONLY)
                    for (resolveInfo in resolveList) {
                        context.grantUriPermission(
                            resolveInfo.activityInfo.packageName,
                            apkUri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    }
                }
                context.startActivity(chooser)
            } catch (e: Exception) {
                e.printStackTrace()
                // Simple plain-text fallback chooser
                try {
                    val textIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareMessage)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(Intent.createChooser(textIntent, "Share FlowTest").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                } catch (err: Exception) {
                    Toast.makeText(context, "Could not launch share: ${err.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        if (coroutineScope != null) {
            coroutineScope.launch(Dispatchers.IO) {
                val apk = prepareShareableApk(context) ?: getInstalledApkSource(context)
                withContext(Dispatchers.Main) {
                    triggerShare(apk)
                }
            }
        } else {
            val cachedDir = File(context.cacheDir, SHARED_DIR_NAME)
            val fileName = getApkFileName(context)
            val cachedApk = File(cachedDir, fileName).takeIf { it.exists() } ?: getInstalledApkSource(context)
            triggerShare(cachedApk)
        }
    }

    /**
     * Copies the APK to the public Downloads folder or external files directory so the user
     * can transfer it manually via OTG Flash Drive, USB, or local file manager.
     */
    suspend fun saveApkToDownloads(context: Context): Result<String> = withContext(Dispatchers.IO) {
        try {
            val srcFile = getInstalledApkSource(context)
                ?: return@withContext Result.failure(Exception("App source APK not found"))

            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val destDir = if (downloadsDir != null && downloadsDir.exists()) {
                downloadsDir
            } else {
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
            }

            val fileName = getApkFileName(context)
            val destFile = File(destDir, fileName)
            FileInputStream(srcFile).use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output, bufferSize = 64 * 1024)
                }
            }
            destFile.setLastModified(srcFile.lastModified())

            Result.success("Saved to: ${destFile.absolutePath}")
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    const val OFFICIAL_DOWNLOAD_URL = "https://flowtest2026.com/download"

    /**
     * Friendly share message for messaging apps or web referrals.
     */
    fun getShareInvitationText(): String {
        return """
            🚀 FlowTest - High-Speed VIP VPN & VTU Platform
            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            Get unthrottled internet, secure WireGuard/Shadowsocks tunnels, and discounted airtime, SME data bundles, electricity & TV subscriptions!
            
            🌐 Download Direct APK: $OFFICIAL_DOWNLOAD_URL
            📲 Share directly with nearby devices via Quick Share or Bluetooth!
            
            🏢 Operated by INOSOFTTECH LIMITED (RC 9710966)
            📞 Support: +234 8137545370 / support@flowtest2026.com
        """.trimIndent()
    }
}
