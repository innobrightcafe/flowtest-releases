package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ui.viewmodel.VpnViewModel
import com.example.ui.components.AppUpdateDialog
import com.example.ui.components.UpdateNotificationBanner
import com.example.ui.components.UsageAccessEnforcementDialog
import com.example.ui.navigation.MainNavGraph
import com.example.ui.theme.FlowTestTheme
import com.example.util.AppNotificationManager
import com.example.util.AppUpdateManager
import com.example.util.UpdateStatus
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File

class MainActivity : FragmentActivity() {

    private val vpnViewModel: VpnViewModel by viewModels()
    private var pausedTimestamp: Long = 0L
    private var lastUserActivityRecorded: Long = 0L
    private val requestedUpdateDialog = MutableStateFlow(false)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                AppNotificationManager.initNotificationChannels(this)
            } catch (e: Throwable) {
                Log.w("MainActivity", "Notification channel init after permission: ${e.message}")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Initialize Android notification channels defensively
        try {
            AppNotificationManager.initNotificationChannels(this)
        } catch (e: Throwable) {
            Log.w("MainActivity", "Safe notification init: ${e.message}")
        }

        // 2. Initialize in-app update and background auto-updater engine
        try {
            AppUpdateManager.initialize(this)
        } catch (e: Throwable) {
            Log.w("MainActivity", "Safe update manager init: ${e.message}")
        }

        // 3. Handle update installation intent if launched from an update notification
        handleUpdateIntent(intent)

        // 4. Handle receipt PDF download intent from in-app/system notification
        handleNotificationDownloadIntent(intent)

        // Request POST_NOTIFICATIONS runtime permission on Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            } catch (e: Throwable) {
                Log.w("MainActivity", "Notification permission request safe notice: ${e.message}")
            }
        }

        setContent {
            FlowTestTheme {
                val updateStatus by AppUpdateManager.updateStatus.collectAsStateWithLifecycle()
                val externalDialogRequest by requestedUpdateDialog.collectAsStateWithLifecycle()
                var showUpdateDialog by remember { mutableStateOf(false) }
                var showUpdateBanner by remember { mutableStateOf(true) }

                // Automatically trigger the update popup and show notification banner when an update is available
                LaunchedEffect(updateStatus) {
                    val info = when (val s = updateStatus) {
                        is UpdateStatus.Available -> s.info
                        is UpdateStatus.ReadyToInstall -> s.info
                        else -> null
                    }
                    if (info != null && info.isUpdateAvailable) {
                        showUpdateBanner = true
                        if (AppUpdateManager.shouldPromptUpdatePopup(this@MainActivity, info.latestVersionCode)) {
                            showUpdateDialog = true
                            AppUpdateManager.markUpdatePopupShown(this@MainActivity, info.latestVersionCode)
                        }
                    }
                }

                // Listen for explicit requests from notification taps
                LaunchedEffect(externalDialogRequest) {
                    if (externalDialogRequest) {
                        showUpdateDialog = true
                        requestedUpdateDialog.value = false
                    }
                }

                val hasUsageAccess by vpnViewModel.hasUsageAccess.collectAsStateWithLifecycle()
                var dismissUsageAccessPrompt by remember { mutableStateOf(false) }

                // Enforce Usage Access permission for per-app data tracking with safe dismiss
                if (!hasUsageAccess && !dismissUsageAccessPrompt) {
                    UsageAccessEnforcementDialog(
                        onGrantClick = {
                            com.example.data.util.NetworkUtils.openUsageAccessSettings(this@MainActivity)
                        },
                        onDismiss = {
                            dismissUsageAccessPrompt = true
                        }
                    )
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    MainNavGraph(viewModel = vpnViewModel)

                    // In-App Notification Banner at top of screen (shows update notification popup)
                    AnimatedVisibility(
                        visible = showUpdateBanner && !showUpdateDialog && (updateStatus is UpdateStatus.Available || updateStatus is UpdateStatus.ReadyToInstall),
                        enter = fadeIn() + slideInVertically { -it },
                        exit = fadeOut() + slideOutVertically { -it },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .statusBarsPadding()
                            .zIndex(99f)
                    ) {
                        UpdateNotificationBanner(
                            status = updateStatus,
                            onOpenUpdateDialog = {
                                showUpdateDialog = true
                            },
                            onDismissBanner = {
                                showUpdateBanner = false
                            }
                        )
                    }

                    // In-App Update Dialog (Only pops up when an update is available or ready)
                    if (showUpdateDialog && (updateStatus is UpdateStatus.Available || updateStatus is UpdateStatus.ReadyToInstall || updateStatus is UpdateStatus.Downloading)) {
                        AppUpdateDialog(
                            status = updateStatus,
                            onDismiss = {
                                showUpdateDialog = false
                                AppUpdateManager.dismissUpdate()
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleUpdateIntent(intent)
        handleNotificationDownloadIntent(intent)
    }

    private fun handleUpdateIntent(intent: Intent?) {
        if (intent == null) return
        try {
            if (intent.getBooleanExtra("EXTRA_SHOW_UPDATE_DIALOG", false)) {
                requestedUpdateDialog.value = true
            }
            if (intent.getBooleanExtra("EXTRA_ACTION_INSTALL_UPDATE", false)) {
                val filePath = intent.getStringExtra("EXTRA_UPDATE_FILE_PATH")
                if (!filePath.isNullOrEmpty()) {
                    val updateFile = File(filePath)
                    if (updateFile.exists()) {
                        AppUpdateManager.installUpdate(this, updateFile)
                    }
                }
            }
        } catch (e: Throwable) {
            Log.e("MainActivity", "Failed to process update intent: ${e.message}")
        }
    }

    private fun handleNotificationDownloadIntent(intent: Intent?) {
        if (intent == null) return
        val ref = intent.getStringExtra("EXTRA_ACTION_DOWNLOAD_RECEIPT_REF")
        if (!ref.isNullOrBlank()) {
            try {
                val token = intent.getStringExtra("EXTRA_ACTION_DOWNLOAD_RECEIPT_TOKEN")?.takeIf { it.isNotBlank() }
                val disco = intent.getStringExtra("EXTRA_ACTION_DOWNLOAD_RECEIPT_DISCO") ?: "Electricity"
                val meter = intent.getStringExtra("EXTRA_ACTION_DOWNLOAD_RECEIPT_METER") ?: ""
                val amount = intent.getDoubleExtra("EXTRA_ACTION_DOWNLOAD_RECEIPT_AMOUNT", 0.0)
                val address = intent.getStringExtra("EXTRA_ACTION_DOWNLOAD_RECEIPT_ADDRESS")?.takeIf { it.isNotBlank() }
                val customer = intent.getStringExtra("EXTRA_ACTION_DOWNLOAD_RECEIPT_CUSTOMER")?.takeIf { it.isNotBlank() }

                val receiptData = com.example.util.TransactionReceiptData(
                    transactionId = ref,
                    reference = ref,
                    title = "$disco Electricity Bill",
                    serviceType = "Electricity Bill",
                    recipient = meter,
                    meterNumber = meter,
                    amountPaid = amount,
                    tokenPin = token,
                    serviceAddress = address,
                    customerName = customer,
                    discoName = disco
                )

                com.example.util.PdfReceiptGenerator.downloadReceiptPdf(this, receiptData) { file ->
                    com.example.util.PdfReceiptGenerator.openOrShareReceipt(this, file)
                }
            } catch (e: Throwable) {
                Log.w("MainActivity", "Receipt download from notification failed: ${e.message}")
            }
        }
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        val now = System.currentTimeMillis()
        // Throttle to at most once every 3 seconds to avoid blocking the main UI thread during scroll gestures
        if (now - lastUserActivityRecorded > 3000L) {
            lastUserActivityRecorded = now
            vpnViewModel.recordUserActivity()
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            pausedTimestamp = System.currentTimeMillis()
            vpnViewModel.onAppBackgrounded(pausedTimestamp)
        } catch (e: Throwable) {
            Log.w("MainActivity", "onPause safe notice: ${e.message}")
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            val hasAccess = vpnViewModel.checkUsageAccess(this)
            if (hasAccess) {
                vpnViewModel.refreshPhoneDataAccounting(this, force = true)
            }
            if (pausedTimestamp > 0L) {
                vpnViewModel.onAppForegrounded(pausedTimestamp)
                pausedTimestamp = 0L
            } else {
                vpnViewModel.recordUserActivity()
            }
        } catch (e: Throwable) {
            Log.w("MainActivity", "onResume safe notice: ${e.message}")
        }
    }
}
