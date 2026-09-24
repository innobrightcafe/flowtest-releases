package com.example

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

/**
 * Custom Application class for FlowTest.
 * Provides defensive initialization, global uncaught exception handling,
 * and crash diagnostics to prevent "FlowTest keeps stopping" errors on diverse Android devices.
 */
class FlowTestApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // 0. If running in dedicated :crash_handler process, skip app initialization
        val processName = getProcessNameCompat()
        if (processName != null && processName.endsWith(":crash_handler")) {
            Log.d(TAG, "Starting isolated :crash_handler process.")
            return
        }

        // 1. Install Global Uncaught Exception Logger & Crash File Generator
        com.example.util.CrashLogger.install(this)

        // 2. Defensively initialize FirebaseApp with explicit fallback options
        try {
            val fallbackOptions = FirebaseOptions.Builder()
                .setApplicationId("1:78057514400:android:1c1b20123712d9491fb0c5")
                .setProjectId("getmehost-db")
                .setApiKey("AIzaSyDDzkOG2bmtM45xv_kaY9t1VN8gXv028Hc")
                .setStorageBucket("getmehost-db.firebasestorage.app")
                .setGcmSenderId("78057514400")
                .build()

            if (FirebaseApp.getApps(this).isEmpty()) {
                val autoApp = try {
                    FirebaseApp.initializeApp(this)
                } catch (autoErr: Throwable) {
                    Log.w(TAG, "Default FirebaseApp auto-init warning: ${autoErr.message}")
                    null
                }
                // If auto-init returned null or apps list is still empty, apply explicit fallback options
                if (autoApp == null || FirebaseApp.getApps(this).isEmpty()) {
                    Log.i(TAG, "Applying explicit fallback FirebaseOptions.")
                    FirebaseApp.initializeApp(this, fallbackOptions)
                }
            }
            Log.d(TAG, "FirebaseApp initialized safely. Apps count: ${FirebaseApp.getApps(this).size}")
        } catch (e: Throwable) {
            Log.w(TAG, "FirebaseApp primary init notice: ${e.message}. Attempting fallback.")
            try {
                val fallbackOptions = FirebaseOptions.Builder()
                    .setApplicationId("1:78057514400:android:1c1b20123712d9491fb0c5")
                    .setProjectId("getmehost-db")
                    .setApiKey("AIzaSyDDzkOG2bmtM45xv_kaY9t1VN8gXv028Hc")
                    .setStorageBucket("getmehost-db.firebasestorage.app")
                    .setGcmSenderId("78057514400")
                    .build()
                FirebaseApp.initializeApp(this, fallbackOptions)
            } catch (fallbackErr: Throwable) {
                Log.e(TAG, "FirebaseApp fallback failed: ${fallbackErr.message}")
            }
        }

        // 3. Synchronous Proactive Database Health & Schema Pre-Flight Check
        preflightDatabaseIntegrity()
    }

    private fun preflightDatabaseIntegrity() {
        try {
            // Synchronously verify database and heal if corrupt or schema mismatched before UI starts
            val db = com.example.data.db.AppDatabase.getInstance(this)
            Log.d(TAG, "Room Database integrity verified successfully.")
        } catch (e: Throwable) {
            Log.e(TAG, "Database integrity issue detected on startup, resetting database: ${e.message}")
            try {
                com.example.data.db.AppDatabase.resetDatabase(this)
                com.example.data.db.AppDatabase.getInstance(this)
                Log.i(TAG, "Database self-healed and re-initialized cleanly.")
            } catch (recoveryErr: Throwable) {
                Log.e(TAG, "Database reset failed: ${recoveryErr.message}")
            }
        }
    }

    companion object {
        private const val TAG = "FlowTestApplication"

        private fun getProcessNameCompat(): String? {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                return Application.getProcessName()
            }
            // Fallback for Android 7.0 - 8.1 (API 24-27)
            try {
                java.io.File("/proc/self/cmdline").let { file ->
                    if (file.exists()) {
                        val name = file.readText().trim { it <= ' ' || it == '\u0000' }
                        if (name.isNotBlank()) return name
                    }
                }
            } catch (_: Throwable) {}
            return null
        }
    }
}
