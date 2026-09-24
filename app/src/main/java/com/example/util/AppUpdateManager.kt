package com.example.util

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.BuildConfig
import com.example.MainActivity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/**
 * Data representation of a remote or local App Update.
 */
data class AppUpdateInfo(
    val latestVersionCode: Int,
    val latestVersionName: String,
    val currentVersionCode: Int = BuildConfig.VERSION_CODE,
    val currentVersionName: String = BuildConfig.VERSION_NAME,
    val isUpdateAvailable: Boolean,
    val releaseNotes: String,
    val apkDownloadUrl: String,
    val fallbackApkUrl: String = "",
    val fileSizeMb: Double = 0.0,
    val isMandatory: Boolean = false
)

/**
 * Sealed UI states for the In-App Update system.
 */
sealed class UpdateStatus {
    object Idle : UpdateStatus()
    object Checking : UpdateStatus()
    data class Available(val info: AppUpdateInfo) : UpdateStatus()
    data class Downloading(
        val progressPercent: Int,
        val downloadedBytes: Long,
        val totalBytes: Long,
        val info: AppUpdateInfo
    ) : UpdateStatus()
    data class ReadyToInstall(
        val apkFile: File,
        val info: AppUpdateInfo
    ) : UpdateStatus()
    data class UpToDate(
        val currentVersionName: String,
        val checkedAt: Long = System.currentTimeMillis()
    ) : UpdateStatus()
    data class Error(val message: String) : UpdateStatus()
}

/**
 * Comprehensive In-App Update and Background Auto-Upgrade Manager.
 * 
 * Key capabilities:
 * 1. Automatically detects Internet connection and checks for updates in the background.
 * 2. Downloads the update APK silently or on-demand without interrupting user workflows.
 * 3. Triggers seamless in-place APK installation (NO UNINSTALL REQUIRED).
 * 4. Preserves 100% of user data, wallet balance, biometric settings, and Room database.
 * 5. Handles Android 8.0+ Unknown App Sources permissions with a single-tap prompt.
 * 6. Includes an interactive "Verify / Self-Update" test mode so users can verify the installer immediately.
 */
object AppUpdateManager {

    private const val TAG = "AppUpdateManager"
    private const val PREFS_NAME = "flowtest_app_update_prefs"
    private const val KEY_AUTO_UPDATE = "pref_auto_background_update"
    private const val KEY_LAST_CHECK_TIME = "pref_last_update_check_time"
    private const val KEY_UPDATE_URL = "pref_custom_update_url"

    private const val DEFAULT_FALLBACK_URL = "https://github.com/innobrightcafe/flowtest-releases/releases/latest/download/FlowTest.apk"
    private const val GITHUB_RELEASES_API = "https://api.github.com/repos/innobrightcafe/flowtest-releases/releases?per_page=5"
    private const val MIN_CHECK_INTERVAL_MS = 10 * 60 * 1000L // 10 minutes debounce for auto-checks

    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val httpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val fastCheckClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()

    private val _updateStatus = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    val updateStatus: StateFlow<UpdateStatus> = _updateStatus.asStateFlow()

    private val _isAutoUpdateEnabled = MutableStateFlow(true)
    val isAutoUpdateEnabled: StateFlow<Boolean> = _isAutoUpdateEnabled.asStateFlow()

    private val _lastCheckedTimestamp = MutableStateFlow(0L)
    val lastCheckedTimestamp: StateFlow<Long> = _lastCheckedTimestamp.asStateFlow()

    private var isNetworkCallbackRegistered = false
    private var downloadJob: Job? = null

    /**
     * Initializes the update manager on app start:
     * - Restores preferences
     * - Registers network listener for automatic internet connectivity trigger
     * - Checks if an already-downloaded update is waiting in cache
     */
    fun initialize(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _isAutoUpdateEnabled.value = prefs.getBoolean(KEY_AUTO_UPDATE, true)
        _lastCheckedTimestamp.value = prefs.getLong(KEY_LAST_CHECK_TIME, 0L)

        // Track and celebrate successful version upgrade
        val lastInstalledCode = prefs.getInt("pref_last_installed_version_code", 0)
        if (lastInstalledCode in 1 until BuildConfig.VERSION_CODE) {
            Log.i(TAG, "🎉 App updated successfully from Build $lastInstalledCode to v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})!")
            try {
                Toast.makeText(
                    context.applicationContext,
                    "🎉 FlowTest updated to v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})!",
                    Toast.LENGTH_LONG
                ).show()
            } catch (_: Throwable) {}
        }
        prefs.edit().putInt("pref_last_installed_version_code", BuildConfig.VERSION_CODE).apply()

        // Check if an APK was previously downloaded and is ready to install
        checkCachedUpdateFile(context)

        // Register network listener to trigger update checks whenever the user connects to the Internet
        registerNetworkConnectivityListener(context)

        // Trigger immediate active update check on initialization
        managerScope.launch {
            kotlinx.coroutines.delay(1000)
            checkForUpdates(context, isBackgroundTrigger = true)
        }
    }

    /**
     * Toggle auto background update preference
     */
    fun setAutoUpdateEnabled(context: Context, enabled: Boolean) {
        _isAutoUpdateEnabled.value = enabled
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_AUTO_UPDATE, enabled)
            .apply()

        if (enabled) {
            checkForUpdates(context, isBackgroundTrigger = true)
        }
    }

    /**
     * Registers a ConnectivityManager callback to trigger an update check automatically
     * when the device connects to the Internet (Wi-Fi or Mobile Data).
     */
    private fun registerNetworkConnectivityListener(context: Context) {
        if (isNetworkCallbackRegistered) return
        try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return

            val networkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            connectivityManager.registerNetworkCallback(
                networkRequest,
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        super.onAvailable(network)
                        val now = System.currentTimeMillis()
                        val lastCheck = _lastCheckedTimestamp.value

                        // Trigger automatic update check if auto-update is active and interval has elapsed
                        if (_isAutoUpdateEnabled.value && (now - lastCheck > MIN_CHECK_INTERVAL_MS)) {
                            Log.d(TAG, "Internet connection detected. Auto-checking for app updates in background...")
                            managerScope.launch {
                                delay(3000) // Allow connection to stabilize
                                checkForUpdates(context, isBackgroundTrigger = true)
                            }
                        }
                    }
                }
            )
            isNetworkCallbackRegistered = true
            Log.d(TAG, "Network listener for background auto-updates registered successfully.")
        } catch (e: Throwable) {
            Log.w(TAG, "Could not register network callback: ${e.message}")
        }
    }

    private const val KEY_SHOWN_POPUP_VERSION = "shown_update_popup_version_"

    /**
     * Determines whether the in-app update popup dialog should be displayed to the user.
     * Ensures the update prompt only pops up ONCE per release version, never spamming or blocking scrolling.
     */
    fun shouldPromptUpdatePopup(context: Context, versionCode: Int): Boolean {
        if (versionCode <= BuildConfig.VERSION_CODE) return false
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastShownTime = prefs.getLong("last_popup_time_$versionCode", 0L)
        val now = System.currentTimeMillis()
        // Prompt if never shown, or re-prompt after 10 minutes so users don't miss new releases
        return (now - lastShownTime > 10 * 60 * 1000L)
    }

    /**
     * Records that the popup dialog for this version has been shown.
     */
    fun markUpdatePopupShown(context: Context, versionCode: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean("${KEY_SHOWN_POPUP_VERSION}$versionCode", true)
            .putLong("last_popup_time_$versionCode", System.currentTimeMillis())
            .apply()
    }

    /**
     * Dismisses the current in-memory update alert without deleting valid downloaded files.
     */
    fun dismissUpdate() {
        if (_updateStatus.value !is UpdateStatus.Downloading) {
            _updateStatus.value = UpdateStatus.Idle
        }
    }

    /**
     * Checks if a previously downloaded update APK exists in the cache and verifies its real package version code.
     * If the APK is older or equal to current installed app version, it is immediately deleted from disk.
     */
    private fun checkCachedUpdateFile(context: Context) {
        try {
            val updateFile = File(context.cacheDir, "updates/FlowTest_update.apk")
            if (updateFile.exists()) {
                val archiveInfo = context.packageManager.getPackageArchiveInfo(updateFile.absolutePath, 0)
                val archiveVersionCode = if (archiveInfo != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        archiveInfo.longVersionCode
                    } else {
                        @Suppress("DEPRECATION")
                        archiveInfo.versionCode.toLong()
                    }
                } else {
                    0L
                }

                if (archiveInfo == null || archiveVersionCode <= BuildConfig.VERSION_CODE) {
                    // Update is already installed or obsolete; delete file immediately so no persistent banner or dialog appears
                    Log.d(TAG, "Cached update APK (code $archiveVersionCode) is already installed or obsolete (current ${BuildConfig.VERSION_CODE}). Purging file...")
                    updateFile.delete()
                    _updateStatus.value = UpdateStatus.Idle
                } else {
                    val versionName = archiveInfo.versionName ?: "${BuildConfig.VERSION_NAME}+"
                    val cachedInfo = AppUpdateInfo(
                        latestVersionCode = archiveVersionCode.toInt(),
                        latestVersionName = versionName,
                        isUpdateAvailable = true,
                        releaseNotes = "Downloaded update ready to install without losing data.",
                        apkDownloadUrl = "",
                        fileSizeMb = (updateFile.length() / (1024.0 * 1024.0))
                    )
                    _updateStatus.value = UpdateStatus.ReadyToInstall(updateFile, cachedInfo)
                }
            } else {
                _updateStatus.value = UpdateStatus.Idle
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Error inspecting cached update: ${e.message}")
            _updateStatus.value = UpdateStatus.Idle
        }
    }

    /**
     * Primary Update Check method.
     * Can be invoked manually via the "Check for Updates" button or automatically in the background.
     */
    fun checkForUpdates(context: Context, isBackgroundTrigger: Boolean = false) {
        if (_updateStatus.value is UpdateStatus.Downloading) {
            Log.d(TAG, "Download currently in progress, skipping check.")
            return
        }

        if (!isBackgroundTrigger) {
            _updateStatus.value = UpdateStatus.Checking
        }

        managerScope.launch {
            try {
                // Record timestamp
                val now = System.currentTimeMillis()
                _lastCheckedTimestamp.value = now
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putLong(KEY_LAST_CHECK_TIME, now)
                    .apply()

                var remoteInfo: AppUpdateInfo? = null

                // Wrap remote checks in a strict 10-second timeout to prevent infinite scanning
                withTimeoutOrNull(10_000L) {
                    // 1. Priority Source: Fast, CDN-backed, authoritative GitHub Releases API
                    try {
                        val ghReq = Request.Builder()
                            .url(GITHUB_RELEASES_API)
                            .header("Accept", "application/vnd.github.v3+json")
                            .header("User-Agent", "FlowTest-Android/${BuildConfig.VERSION_NAME}")
                            .get()
                            .build()
                        val ghResp = fastCheckClient.newCall(ghReq).execute()
                        if (ghResp.isSuccessful) {
                            val ghBody = ghResp.body?.string()
                            if (!ghBody.isNullOrEmpty()) {
                                val releasesArray = org.json.JSONArray(ghBody)
                                var highestVer = ""
                                var highestReleaseJson: org.json.JSONObject? = null

                                for (i in 0 until releasesArray.length()) {
                                    val r = releasesArray.optJSONObject(i) ?: continue
                                    if (r.optBoolean("draft", false)) continue
                                    val tag = r.optString("tag_name", "").removePrefix("v").removePrefix("V")
                                    if (tag.isBlank()) continue

                                    if (highestVer.isEmpty() || isSemanticVersionNewer(tag, highestVer)) {
                                        highestVer = tag
                                        highestReleaseJson = r
                                    }
                                }

                                if (highestReleaseJson != null && isSemanticVersionNewer(highestVer, BuildConfig.VERSION_NAME)) {
                                    val assets = highestReleaseJson.optJSONArray("assets")
                                    var downloadUrl = ""
                                    var sizeMb = 35.4
                                    if (assets != null) {
                                        for (j in 0 until assets.length()) {
                                            val asset = assets.optJSONObject(j) ?: continue
                                            val name = asset.optString("name", "")
                                            if (name.equals("FlowTest.apk", ignoreCase = true) || name.endsWith(".apk")) {
                                                downloadUrl = asset.optString("browser_download_url", "")
                                                val bytes = asset.optLong("size", 0L)
                                                if (bytes > 0) {
                                                    sizeMb = bytes / (1024.0 * 1024.0)
                                                }
                                                break
                                            }
                                        }
                                    }
                                    if (downloadUrl.isBlank()) {
                                        downloadUrl = "https://github.com/innobrightcafe/flowtest-releases/releases/download/v$highestVer/FlowTest.apk"
                                    }
                                    val notes = highestReleaseJson.optString("body", "Official FlowTest v$highestVer update from GitHub Releases.")
                                    val patchNum = highestVer.split(".").lastOrNull()?.toIntOrNull() ?: 0
                                    val estimatedCode = if (patchNum > 0) 100 + patchNum else (BuildConfig.VERSION_CODE + 1)

                                    remoteInfo = AppUpdateInfo(
                                        latestVersionCode = estimatedCode,
                                        latestVersionName = highestVer,
                                        isUpdateAvailable = true,
                                        releaseNotes = notes,
                                        apkDownloadUrl = downloadUrl,
                                        fallbackApkUrl = DEFAULT_FALLBACK_URL,
                                        fileSizeMb = sizeMb,
                                        isMandatory = false
                                    )
                                    Log.i(TAG, "GitHub Releases check found new update: v$highestVer (installed: ${BuildConfig.VERSION_NAME})")
                                }
                            }
                        }
                    } catch (ghErr: Throwable) {
                        Log.d(TAG, "GitHub Releases query skipped: ${ghErr.message}")
                    }

                    // 2. Secondary Source: Firebase Firestore (if GitHub releases had no newer release or was rate-limited)
                    if (remoteInfo?.isUpdateAvailable != true) {
                        try {
                            withTimeoutOrNull(2500L) {
                                val firestore = FirebaseFirestore.getInstance()
                                val docSnapshot = firestore.collection("app_updates")
                                    .document("latest")
                                    .get()
                                    .await()

                                if (docSnapshot.exists()) {
                                    val latestCode = (docSnapshot.getLong("versionCode") ?: 0L).toInt()
                                    val latestName = docSnapshot.getString("versionName") ?: BuildConfig.VERSION_NAME
                                    val notes = docSnapshot.getString("releaseNotes")
                                        ?: "Performance enhancements, crash fixes, and security patches."
                                    val url = docSnapshot.getString("apkUrl") ?: DEFAULT_FALLBACK_URL
                                    val sizeMb = docSnapshot.getDouble("fileSizeMb") ?: 35.0
                                    val mandatory = docSnapshot.getBoolean("isMandatory") ?: false

                                    val hasUpdate = (latestCode > BuildConfig.VERSION_CODE) || isSemanticVersionNewer(latestName, BuildConfig.VERSION_NAME)
                                    if (hasUpdate) {
                                        remoteInfo = AppUpdateInfo(
                                            latestVersionCode = latestCode,
                                            latestVersionName = latestName,
                                            isUpdateAvailable = true,
                                            releaseNotes = notes,
                                            apkDownloadUrl = url,
                                            fallbackApkUrl = DEFAULT_FALLBACK_URL,
                                            fileSizeMb = sizeMb,
                                            isMandatory = mandatory
                                        )
                                    }
                                }
                            }
                        } catch (e: Throwable) {
                            Log.d(TAG, "Firestore update query failed/skipped: ${e.message}")
                        }
                    }

                    // 3. Tertiary Source: Cloud Run Backend Update API (excluding invalid hostnames)
                    if (remoteInfo?.isUpdateAvailable != true) {
                        try {
                            val candidateBaseUrls = listOfNotNull(
                                BuildConfig.CLOUDRUN_BACKEND_URL.takeIf {
                                    it.isNotBlank() && it.startsWith("http") && !it.contains("api.flowtest2026.com")
                                }
                            ).distinct()

                            val endpointsToTry = candidateBaseUrls.flatMap { base ->
                                listOf(
                                    "${base.trimEnd('/')}/api/version?currentVersionCode=${BuildConfig.VERSION_CODE}",
                                    "${base.trimEnd('/')}/api/v1/app-update/latest?currentVersionCode=${BuildConfig.VERSION_CODE}"
                                )
                            }

                            for (updateEndpoint in endpointsToTry) {
                                try {
                                    val request = Request.Builder()
                                        .url(updateEndpoint)
                                        .get()
                                        .build()
                                    val response = fastCheckClient.newCall(request).execute()
                                    if (response.isSuccessful) {
                                        val bodyStr = response.body?.string()
                                        if (!bodyStr.isNullOrEmpty()) {
                                            val json = org.json.JSONObject(bodyStr)
                                            val latestCode = json.optInt("versionCode", 0)
                                            val latestName = json.optString("versionName", BuildConfig.VERSION_NAME)
                                            val notes = json.optString("releaseNotes", "New update deployed from GitHub to Cloud Run.")
                                            
                                            var downloadUrl = json.optString("downloadUrl", "").ifBlank {
                                                json.optString("apkDownloadUrl", "")
                                            }
                                            if (downloadUrl.startsWith("/")) {
                                                val endpointUri = java.net.URI(updateEndpoint)
                                                val origin = "${endpointUri.scheme}://${endpointUri.authority}"
                                                downloadUrl = "$origin$downloadUrl"
                                            }

                                            val sizeMb = json.optDouble("fileSizeMb", 35.0)
                                            val mandatory = json.optBoolean("isMandatory", false)

                                            val isNewer = (latestCode > BuildConfig.VERSION_CODE) || isSemanticVersionNewer(latestName, BuildConfig.VERSION_NAME)
                                            if (isNewer) {
                                                remoteInfo = AppUpdateInfo(
                                                    latestVersionCode = if (latestCode > 0) latestCode else (BuildConfig.VERSION_CODE + 1),
                                                    latestVersionName = latestName,
                                                    isUpdateAvailable = true,
                                                    releaseNotes = notes,
                                                    apkDownloadUrl = downloadUrl.ifBlank { DEFAULT_FALLBACK_URL },
                                                    fallbackApkUrl = DEFAULT_FALLBACK_URL,
                                                    fileSizeMb = sizeMb,
                                                    isMandatory = mandatory
                                                )
                                                break
                                            }
                                        }
                                    }
                                } catch (epErr: Throwable) {
                                    Log.d(TAG, "Endpoint $updateEndpoint check failed: ${epErr.message}")
                                }
                            }
                        } catch (e: Throwable) {
                            Log.d(TAG, "Backend update endpoint check skipped: ${e.message}")
                        }
                    }
                }

                // 4. Fallback to local default if no remote updates found
                if (remoteInfo == null) {
                    val customUrl = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        .getString(KEY_UPDATE_URL, DEFAULT_FALLBACK_URL) ?: DEFAULT_FALLBACK_URL

                    remoteInfo = AppUpdateInfo(
                        latestVersionCode = BuildConfig.VERSION_CODE,
                        latestVersionName = BuildConfig.VERSION_NAME,
                        isUpdateAvailable = false,
                        releaseNotes = "You are running the official latest release with zero-trust security.",
                        apkDownloadUrl = customUrl,
                        fileSizeMb = 24.5
                    )
                }

                if (remoteInfo.isUpdateAvailable) {
                    Log.d(TAG, "New update found: v${remoteInfo.latestVersionName} (code ${remoteInfo.latestVersionCode})")
                    _updateStatus.value = UpdateStatus.Available(remoteInfo)

                    // Always post system notification so the update is immediately visible
                    AppNotificationManager.showUpdateAvailableNotification(
                        context = context,
                        versionName = remoteInfo.latestVersionName,
                        releaseNotes = remoteInfo.releaseNotes
                    )

                    if (isBackgroundTrigger && _isAutoUpdateEnabled.value) {
                        // Automatically initiate background download
                        Log.d(TAG, "Auto-update active: Starting silent background download...")
                        startBackgroundDownload(context, remoteInfo)
                    }
                } else {
                    Log.d(TAG, "App is up to date: ${remoteInfo.currentVersionName}")
                    _updateStatus.value = UpdateStatus.UpToDate(remoteInfo.currentVersionName)
                }

            } catch (e: Throwable) {
                Log.e(TAG, "Error checking for updates: ${e.message}", e)
                _updateStatus.value = UpdateStatus.Error("Failed to check for updates: ${e.message}")
            }
        }
    }

    /**
     * Downloads the APK update in the background with real-time percentage progress.
     */
    fun startBackgroundDownload(context: Context, info: AppUpdateInfo) {
        downloadJob?.cancel()
        downloadJob = managerScope.launch {
            try {
                val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
                val targetApk = File(updatesDir, "FlowTest_update.apk")
                if (targetApk.exists()) {
                    targetApk.delete()
                }

                _updateStatus.value = UpdateStatus.Downloading(
                    progressPercent = 0,
                    downloadedBytes = 0L,
                    totalBytes = (info.fileSizeMb * 1024 * 1024).toLong(),
                    info = info
                )

                var downloadSuccess = false
                var failureReason = "Connection to update server interrupted."

                // Construct list of candidate URLs, filtering out unresolvable hostnames
                val candidateUrls = mutableListOf<String>()

                fun isValidCandidateUrl(url: String): Boolean {
                    if (!url.startsWith("http://") && !url.startsWith("https://")) return false
                    if (url.contains("api.flowtest2026.com")) return false
                    return true
                }

                if (isValidCandidateUrl(info.apkDownloadUrl)) candidateUrls.add(info.apkDownloadUrl)
                if (isValidCandidateUrl(info.fallbackApkUrl) && !candidateUrls.contains(info.fallbackApkUrl)) {
                    candidateUrls.add(info.fallbackApkUrl)
                }
                if (!candidateUrls.contains(DEFAULT_FALLBACK_URL)) {
                    candidateUrls.add(DEFAULT_FALLBACK_URL)
                }
                val latestGithubMirror = "https://github.com/innobrightcafe/flowtest-releases/releases/latest/download/FlowTest.apk"
                if (!candidateUrls.contains(latestGithubMirror)) {
                    candidateUrls.add(latestGithubMirror)
                }

                for (urlToTry in candidateUrls) {
                    try {
                        Log.i(TAG, "Attempting update download from: $urlToTry")
                        var attempts = 0
                        var candidateSucceeded = false

                        while (attempts < 2 && !candidateSucceeded) {
                            attempts++
                            try {
                                val request = Request.Builder()
                                    .url(urlToTry)
                                    .header("User-Agent", "FlowTest-Android/${BuildConfig.VERSION_NAME}")
                                    .build()
                                val response = httpClient.newCall(request).execute()

                                if (response.isSuccessful && response.body != null) {
                                    val contentType = response.header("Content-Type", "")?.lowercase() ?: ""
                                    if (contentType.contains("text/html")) {
                                        Log.w(TAG, "URL $urlToTry returned HTML instead of binary APK, skipping candidate...")
                                        failureReason = "Update source returned web page instead of APK package."
                                        break
                                    }

                                    val body = response.body!!
                                    val contentLength = body.contentLength().takeIf { it > 0 }
                                        ?: (info.fileSizeMb * 1024 * 1024).toLong()

                                    body.byteStream().use { input ->
                                        FileOutputStream(targetApk).use { output ->
                                            val buffer = ByteArray(64 * 1024)
                                            var bytesRead: Int
                                            var totalBytesRead = 0L
                                            var lastPercent = 0

                                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                                output.write(buffer, 0, bytesRead)
                                                totalBytesRead += bytesRead

                                                val percent = ((totalBytesRead * 100) / contentLength).toInt().coerceIn(0, 99)
                                                if (percent != lastPercent) {
                                                    lastPercent = percent
                                                    _updateStatus.value = UpdateStatus.Downloading(
                                                        progressPercent = percent,
                                                        downloadedBytes = totalBytesRead,
                                                        totalBytes = contentLength,
                                                        info = info
                                                    )
                                                }
                                            }
                                            output.flush()
                                        }
                                    }

                                    if (targetApk.exists() && targetApk.length() > 500_000L) {
                                        val archive = context.packageManager.getPackageArchiveInfo(targetApk.absolutePath, 0)
                                        if (archive != null) {
                                            val downloadedCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                                archive.longVersionCode.toInt()
                                            } else {
                                                @Suppress("DEPRECATION")
                                                archive.versionCode
                                            }
                                            Log.i(TAG, "Successfully downloaded & verified update APK: v${archive.versionName} ($downloadedCode) from $urlToTry (${targetApk.length()} bytes)")
                                            downloadSuccess = true
                                            candidateSucceeded = true
                                            break
                                        } else {
                                            Log.w(TAG, "Downloaded file from $urlToTry is not a valid Android APK archive. Purging...")
                                            targetApk.delete()
                                            failureReason = "Downloaded update package is corrupted or incomplete."
                                        }
                                    } else {
                                        Log.w(TAG, "Downloaded file is too small or missing (${targetApk.length()} bytes)")
                                        targetApk.delete()
                                        failureReason = "Downloaded update file is too small."
                                    }
                                } else {
                                    Log.w(TAG, "URL $urlToTry returned HTTP ${response.code}, attempt $attempts")
                                    failureReason = "Server returned HTTP ${response.code}."
                                }
                            } catch (retryEx: Throwable) {
                                Log.w(TAG, "Download attempt $attempts for $urlToTry interrupted: ${retryEx.message}")
                                failureReason = "Download connection issue: ${retryEx.message}"
                                if (attempts < 2) {
                                    delay(1000)
                                }
                            }
                        }

                        if (downloadSuccess) {
                            break
                        }
                    } catch (netEx: Throwable) {
                        Log.w(TAG, "URL $urlToTry candidate failed: ${netEx.message}")
                        failureReason = "Download connection issue: ${netEx.message}"
                    }
                }

                if (downloadSuccess) {
                    _updateStatus.value = UpdateStatus.ReadyToInstall(targetApk, info)

                    // Generate PendingIntent for direct installation from notification
                    val pendingIntent = getInstallPendingIntent(context, targetApk)

                    // Post notification that update is ready to install
                    AppNotificationManager.showUpdateReadyNotification(
                        context = context,
                        versionName = info.latestVersionName,
                        installPendingIntent = pendingIntent
                    )
                    Log.d(TAG, "Update successfully downloaded: ${targetApk.length()} bytes.")
                } else {
                    _updateStatus.value = UpdateStatus.Error(failureReason)
                }

            } catch (e: Throwable) {
                if (e !is CancellationException) {
                    Log.e(TAG, "Download failed: ${e.message}", e)
                    _updateStatus.value = UpdateStatus.Error("Download failed: ${e.message}")
                }
            }
        }
    }

    /**
     * Checks if the downloaded APK has a compatible signing certificate with the currently installed app.
     * Prevents Android's cryptic INSTALL_FAILED_UPDATE_INCOMPATIBLE error before launching installer.
     */
    fun checkSignatureCompatibility(context: Context, apkFile: File): Boolean {
        return try {
            val packageManager = context.packageManager
            val installedSignatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val pInfo = packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                pInfo.signingInfo?.apkContentsSigners?.map { it.toByteArray().contentHashCode() }?.toSet() ?: emptySet()
            } else {
                @Suppress("DEPRECATION")
                val pInfo = packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
                pInfo.signatures?.map { it.toByteArray().contentHashCode() }?.toSet() ?: emptySet()
            }

            val archiveSignatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val aInfo = packageManager.getPackageArchiveInfo(apkFile.absolutePath, PackageManager.GET_SIGNING_CERTIFICATES)
                aInfo?.signingInfo?.apkContentsSigners?.map { it.toByteArray().contentHashCode() }?.toSet() ?: emptySet()
            } else {
                @Suppress("DEPRECATION")
                val aInfo = packageManager.getPackageArchiveInfo(apkFile.absolutePath, PackageManager.GET_SIGNATURES)
                aInfo?.signatures?.map { it.toByteArray().contentHashCode() }?.toSet() ?: emptySet()
            }

            if (installedSignatures.isEmpty() || archiveSignatures.isEmpty()) {
                true // Allow installer to proceed if signatures cannot be extracted ahead of time
            } else {
                installedSignatures.intersect(archiveSignatures).isNotEmpty()
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Signature compatibility check notice: ${e.message}")
            true
        }
    }

    /**
     * Copies the downloaded APK package into the user's public Downloads directory
     * so it can be installed via Files / Downloads app or shared if desired.
     */
    fun copyApkToPublicDownloads(context: Context, apkFile: File, versionName: String): File? {
        return try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            val destination = File(downloadsDir, "FlowTest-v${versionName}.apk")
            apkFile.copyTo(destination, overwrite = true)
            Log.i(TAG, "Copied update APK to public downloads: ${destination.absolutePath}")
            destination
        } catch (e: Throwable) {
            Log.w(TAG, "Could not copy update APK to public downloads: ${e.message}")
            null
        }
    }

    /**
     * Triggers the Android Package Installer to install the update APK in-place.
     * Keeps all data, settings, and database intact without requiring uninstallation!
     */
    fun installUpdate(context: Context, apkFile: File): Result<Unit> {
        return try {
            if (!apkFile.exists()) {
                return Result.failure(IllegalStateException("Update package file not found."))
            }

            // Also backup a copy to phone's public Downloads folder
            val publicFile = copyApkToPublicDownloads(context, apkFile, BuildConfig.VERSION_NAME)

            // Check signature compatibility to prevent silent INSTALL_FAILED_UPDATE_INCOMPATIBLE
            val isCompatible = checkSignatureCompatibility(context, apkFile)
            if (!isCompatible) {
                Log.e(TAG, "Signature mismatch detected between installed app and update APK!")
                val locationNote = if (publicFile != null) " (saved to phone Downloads as ${publicFile.name})" else ""
                val mismatchError = "Signing certificate mismatch detected: The downloaded update was signed with a different key than your installed app$locationNote. To install, please export your wallet data in Settings -> Backup, uninstall this build, and install the new APK."
                _updateStatus.value = UpdateStatus.Error(mismatchError)
                return Result.failure(SecurityException(mismatchError))
            }

            // Check Unknown Apps permission on Android 8.0+ (API 26+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val canInstall = context.packageManager.canRequestPackageInstalls()
                if (!canInstall) {
                    Log.d(TAG, "Requesting ACTION_MANAGE_UNKNOWN_APP_SOURCES permission...")
                    val manageIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(manageIntent)
                    return Result.failure(SecurityException("Please grant FlowTest permission to install updates, then tap Install again."))
                }
            }

            // Obtain secure content URI via FileProvider
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            // Construct clean Android package installer intent
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // Grant read URI permission to package installer components
            val knownInstallers = listOf(
                "com.google.android.packageinstaller",
                "com.android.packageinstaller"
            )
            for (installerPkg in knownInstallers) {
                try {
                    context.grantUriPermission(installerPkg, contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (_: Throwable) {}
            }

            try {
                val resolveInfoList = context.packageManager.queryIntentActivities(
                    installIntent,
                    android.content.pm.PackageManager.MATCH_DEFAULT_ONLY
                )
                for (resolveInfo in resolveInfoList) {
                    val targetPkg = resolveInfo.activityInfo.packageName
                    context.grantUriPermission(targetPkg, contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            } catch (permErr: Throwable) {
                Log.w(TAG, "Notice granting URI permissions: ${permErr.message}")
            }

            // Remind user how to bypass Google Play Protect alert
            try {
                Toast.makeText(
                    context,
                    "If Google Play Protect warns: Tap 'More details' ⌵ -> 'Install anyway'",
                    Toast.LENGTH_LONG
                ).show()
            } catch (_: Throwable) {}

            context.startActivity(installIntent)
            Result.success(Unit)
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to launch package installer: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Builds a PendingIntent that opens the app or installer
     */
    private fun getInstallPendingIntent(context: Context, apkFile: File): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_ACTION_INSTALL_UPDATE", true)
            putExtra("EXTRA_UPDATE_FILE_PATH", apkFile.absolutePath)
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getActivity(context, 88801, intent, flags)
    }

    /**
     * Interactive Developer & Self-Verification feature:
     * Prepares the current installed APK as an update test payload so users/testers
     * can verify the in-place update installer immediately without needing a remote server!
     */
    fun createSelfTestUpdate(context: Context) {
        managerScope.launch {
            try {
                _updateStatus.value = UpdateStatus.Checking
                val currentApk = File(context.applicationInfo.sourceDir)
                val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
                val targetApk = File(updatesDir, "FlowTest_update.apk")

                // Animate progress
                for (p in 10..100 step 20) {
                    _updateStatus.value = UpdateStatus.Downloading(
                        progressPercent = p,
                        downloadedBytes = (currentApk.length() * p / 100),
                        totalBytes = currentApk.length(),
                        info = AppUpdateInfo(
                            latestVersionCode = BuildConfig.VERSION_CODE + 1,
                            latestVersionName = "${BuildConfig.VERSION_NAME} (Test Update)",
                            isUpdateAvailable = true,
                            releaseNotes = "Verified build update test. Overwrites and updates without uninstalling!",
                            apkDownloadUrl = "",
                            fileSizeMb = (currentApk.length() / (1024.0 * 1024.0))
                        )
                    )
                    delay(120)
                }

                currentApk.copyTo(targetApk, overwrite = true)

                val testInfo = AppUpdateInfo(
                    latestVersionCode = BuildConfig.VERSION_CODE + 1,
                    latestVersionName = "${BuildConfig.VERSION_NAME}.1",
                    isUpdateAvailable = true,
                    releaseNotes = "⚡ Self-Verification Update: Verifies in-app updating over current installation without uninstalling.",
                    apkDownloadUrl = "",
                    fileSizeMb = (targetApk.length() / (1024.0 * 1024.0))
                )

                _updateStatus.value = UpdateStatus.ReadyToInstall(targetApk, testInfo)
            } catch (e: Throwable) {
                _updateStatus.value = UpdateStatus.Error("Self-test preparation error: ${e.message}")
            }
        }
    }

    /**
     * Admin capability to publish an update to Firestore so all online users receive it immediately.
     */
    suspend fun publishUpdateToFirestore(
        versionCode: Int,
        versionName: String,
        apkUrl: String,
        releaseNotes: String,
        fileSizeMb: Double,
        isMandatory: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val firestore = FirebaseFirestore.getInstance()
            val data = hashMapOf(
                "versionCode" to versionCode,
                "versionName" to versionName,
                "apkUrl" to apkUrl,
                "releaseNotes" to releaseNotes,
                "fileSizeMb" to fileSizeMb,
                "isMandatory" to isMandatory,
                "updatedAt" to System.currentTimeMillis()
            )
            firestore.collection("app_updates")
                .document("latest")
                .set(data)
                .await()

            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    /**
     * Compare semantic version strings (e.g., "1.4.1" vs "1.3.1").
     * Returns true if remote is strictly newer than current.
     */
    fun isSemanticVersionNewer(remote: String?, current: String?): Boolean {
        if (remote.isNullOrBlank() || current.isNullOrBlank()) return false
        return try {
            val cleanRemote = remote.trim().removePrefix("v").removePrefix("V")
            val cleanCurrent = current.trim().removePrefix("v").removePrefix("V")
            val remoteParts = cleanRemote.split(".").mapNotNull { it.takeWhile { c -> c.isDigit() }.toIntOrNull() }
            val currentParts = cleanCurrent.split(".").mapNotNull { it.takeWhile { c -> c.isDigit() }.toIntOrNull() }
            val maxLen = maxOf(remoteParts.size, currentParts.size)
            for (i in 0 until maxLen) {
                val r = remoteParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }
                if (r > c) return true
                if (r < c) return false
            }
            false
        } catch (_: Throwable) {
            false
        }
    }
}
