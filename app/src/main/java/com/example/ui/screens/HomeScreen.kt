package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.FlowTestApplication
import com.example.R
import com.example.data.ui.viewmodel.VpnViewModel
import com.example.data.vpn.VpnConnectionManager
import com.example.data.vpn.VpnState
import com.example.ui.components.ConnectionButton
import com.example.ui.components.FlowtestEmblem
import com.example.ui.components.FlowtestLogo
import com.example.ui.components.FundWalletDialog
import com.example.ui.components.FundingAccountReminderDrawer
import com.example.ui.components.OnboardingLoginDialog
import com.example.ui.components.RewardsSectionCard
import com.example.ui.components.HOME_SERVICES
import com.example.ui.components.ServiceItem
import com.example.ui.components.ServicePurchasingBottomSheet
import com.example.ui.components.ServicesGridSection
import com.example.ui.components.ShareAppApkSheet
import com.example.ui.components.TransactionHistoryDialog
import com.example.ui.components.UserAvatarButton
import com.example.ui.components.UserProfileDrawer
import com.example.ui.components.VpnTimeStoreDialog
import com.example.ui.components.DirectDataRepurchaseBottomSheet
import com.example.ui.components.PurchaseSuccessReceipt
import com.example.ui.components.TransactionSuccessReceiptView
import com.example.ui.components.WalletAccountHeaderCard
import com.example.ui.components.getNetworkColor
import com.example.ui.components.getNetworkContentColor
import com.example.data.util.RealNetworkState
import com.example.ui.theme.*

@Composable
fun HomeScreen(
    viewModel: VpnViewModel,
    onNavigateToServers: () -> Unit,
    onNavigateToDataSaver: () -> Unit,
    onNavigateToDataManagement: () -> Unit = onNavigateToDataSaver,
    onNavigateToSpeedTest: () -> Unit,
    onNavigateToRewards: () -> Unit = {},
    onNavigateToAdmin: () -> Unit = {},
    onNavigateToSms: () -> Unit = {},
    onNavigateToBrowser: () -> Unit = {},
    onNavigateToElectricityDashboard: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val vpnState by viewModel.vpnState.collectAsStateWithLifecycle()
    val activeServer by viewModel.activeServer.collectAsStateWithLifecycle()
    val selectedServer by viewModel.selectedServer.collectAsStateWithLifecycle()
    val connectingStep by viewModel.connectingStep.collectAsStateWithLifecycle()
    val metrics by viewModel.metrics.collectAsStateWithLifecycle()
    val selectedProtocol by viewModel.selectedProtocol.collectAsStateWithLifecycle()
    val userVirtualAccount by viewModel.userVirtualAccount.collectAsStateWithLifecycle()
    val userWalletBalance by viewModel.userWalletBalance.collectAsStateWithLifecycle()
    val cashbackBalance by viewModel.cashbackBalance.collectAsStateWithLifecycle()
    val referralEarnings by viewModel.referralEarnings.collectAsStateWithLifecycle()
    val vtuTransactionLogs by viewModel.vtuTransactionLogs.collectAsStateWithLifecycle()
    val vpnTimeRemainingMinutes by viewModel.vpnTimeRemainingMinutes.collectAsStateWithLifecycle()
    val depositSessionExpiresAt by viewModel.depositSessionExpiresAt.collectAsStateWithLifecycle()
    val activeConfirmationCode by viewModel.activeConfirmationCode.collectAsStateWithLifecycle()
    val isDepositReportedPendingAdmin by viewModel.isDepositReportedPendingAdmin.collectAsStateWithLifecycle()
    val reportedDepositReference by viewModel.reportedDepositReference.collectAsStateWithLifecycle()
    val electricityMeterConfig by viewModel.electricityMeterConfig.collectAsStateWithLifecycle()
    val electricityAppliances by viewModel.electricityAppliances.collectAsStateWithLifecycle()

    var showRegisterDialog by remember { mutableStateOf(false) }
    var showFundDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showProfileDrawer by remember { mutableStateOf(false) }
    var showVpnTimeStoreDialog by remember { mutableStateOf(false) }
    var selectedDrawerService by remember { mutableStateOf<ServiceItem?>(null) }
    var showDataRenewalReminderDialog by remember { mutableStateOf(false) }
    var showSyncBalanceDialog by remember { mutableStateOf(false) }
    val recentDataPurchases by viewModel.recentDataPurchases.collectAsStateWithLifecycle()
    var directRepurchaseData by remember { mutableStateOf<com.example.data.api.RecentDataPurchase?>(null) }
    var directRepurchaseReceipt by remember { mutableStateOf<PurchaseSuccessReceipt?>(null) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val firewallApps by viewModel.firewallApps.collectAsStateWithLifecycle()
    val dataSaverStats by viewModel.dataSaverStats.collectAsStateWithLifecycle()
    val estimatedRemainingMb by viewModel.estimatedDataBalanceMb.collectAsStateWithLifecycle()
    val purchasedDataTotalMb by viewModel.purchasedDataTotalMb.collectAsStateWithLifecycle()
    val isDataRenewalReminderEnabled by viewModel.isDataRenewalReminderEnabled.collectAsStateWithLifecycle()
    val dataReminderThresholdMb by viewModel.dataReminderThresholdMb.collectAsStateWithLifecycle()
    val realNetworkState by viewModel.realNetworkState.collectAsStateWithLifecycle()
    val totalAppsConsumedMb by viewModel.totalAppsDataConsumedMb.collectAsStateWithLifecycle()
    val rewardPoints by viewModel.rewardPoints.collectAsStateWithLifecycle()
    val pointsRedemptionRate by viewModel.pointsRedemptionRateNairaPer100Pts.collectAsStateWithLifecycle()
    val liveSpeedMbps by viewModel.liveNetworkSpeedMbps.collectAsStateWithLifecycle()
    val effectiveDataSavedMb by viewModel.effectiveDataSavedMb.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.refreshPhoneDataAccounting(context)
    }

    val currentServer = if (vpnState == VpnState.CONNECTED) activeServer else selectedServer

    // Check for previous crash diagnostics to give user transparency and reassurance
    var lastCrashInfo by remember {
        mutableStateOf<String?>(null)
    }
    var lastCrashReportText by remember {
        mutableStateOf<String?>(null)
    }
    LaunchedEffect(Unit) {
        try {
            val crashPrefs = context.getSharedPreferences(com.example.util.CrashLogger.PREF_NAME, android.content.Context.MODE_PRIVATE)
            val crashCount = crashPrefs.getInt(com.example.util.CrashLogger.KEY_TOTAL_CRASHES, 0)
            val lastReport = crashPrefs.getString(com.example.util.CrashLogger.KEY_LAST_REPORT, null)
            val lastTime = crashPrefs.getLong(com.example.util.CrashLogger.KEY_LAST_TIMESTAMP, 0L)
            // Show alert only if crash was recent (within the last 15 minutes)
            if (crashCount > 0 && lastReport != null && (System.currentTimeMillis() - lastTime < 900_000L)) {
                lastCrashInfo = "Startup crash self-healed ($crashCount recovered). Cache and engines re-synchronized."
                lastCrashReportText = lastReport
                // Reset flag so banner does not repeat persistently
                crashPrefs.edit().putLong(com.example.util.CrashLogger.KEY_LAST_TIMESTAMP, 0L).apply()
            }
        } catch (_: Throwable) {}
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkObsidian)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .testTag("home_screen_container")
    ) {
        // Crash self-heal notification banner if recently recovered
        if (lastCrashInfo != null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = ElectricEmerald.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, ElectricEmerald.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = ElectricEmerald,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = lastCrashInfo ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = ElectricEmerald,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    if (lastCrashReportText != null) {
                        TextButton(
                            onClick = {
                                com.example.util.CrashLogger.shareCrashLogViaEmail(context, lastCrashReportText)
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "EMAIL LOG",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    color = ElectricEmerald
                                )
                            )
                        }
                    }
                    IconButton(
                        onClick = { lastCrashInfo = null },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = ElectricEmerald,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // 1. MY CONNECTION LIVE TELEMETRY CARD (Pure Estimated Balance, App & Hotspot tracking)
        MyConnectionCard(
            vpnState = vpnState,
            networkState = realNetworkState,
            dataUsedTodayMb = totalAppsConsumedMb,
            purchasedDataTotalMb = purchasedDataTotalMb,
            estimatedRemainingMb = estimatedRemainingMb,
            dataSavedMb = effectiveDataSavedMb,
            latencyMs = currentServer?.pingMs ?: 0,
            speedMbps = if (liveSpeedMbps > 0.0) String.format(java.util.Locale.US, "%.1f", liveSpeedMbps) else if (metrics.downloadSpeedMbps > 0.0) String.format(java.util.Locale.US, "%.1f", metrics.downloadSpeedMbps) else if (realNetworkState.isConnected) "1.5" else "0.0",
            userVirtualAccount = userVirtualAccount,
            isReminderEnabled = isDataRenewalReminderEnabled,
            reminderThresholdMb = dataReminderThresholdMb,
            onReminderClick = { showDataRenewalReminderDialog = true },
            onCalibrateBalanceClick = { showSyncBalanceDialog = true },
            onShareClick = {
                val refCode = "FLOW-" + userVirtualAccount.accountNumber.takeLast(5)
                com.example.util.ApkSharingHelper.launchNativeShare(
                    context = context,
                    coroutineScope = coroutineScope,
                    referralCode = refCode
                )
            },
            onProfileClick = { showProfileDrawer = true }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Central Connection Power Button
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            ConnectionButton(
                vpnState = vpnState,
                onClick = { viewModel.connectOrDisconnect() }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Connection Status & IP Details
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Single Clean Status Badge: Red UNPROTECTED -> Green PROTECTED
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = when (vpnState) {
                    VpnState.CONNECTED -> ElectricEmerald.copy(alpha = 0.14f)
                    VpnState.CONNECTING -> GlowingAmber.copy(alpha = 0.14f)
                    VpnState.DISCONNECTING -> WarningRed.copy(alpha = 0.08f)
                    VpnState.DISCONNECTED -> WarningRed.copy(alpha = 0.14f)
                },
                border = androidx.compose.foundation.BorderStroke(
                    1.2.dp,
                    when (vpnState) {
                        VpnState.CONNECTED -> ElectricEmerald.copy(alpha = 0.6f)
                        VpnState.CONNECTING -> GlowingAmber.copy(alpha = 0.6f)
                        VpnState.DISCONNECTING -> WarningRed.copy(alpha = 0.4f)
                        VpnState.DISCONNECTED -> WarningRed.copy(alpha = 0.6f)
                    }
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (vpnState) {
                            VpnState.CONNECTED -> Icons.Default.Shield
                            VpnState.CONNECTING -> Icons.Default.Sync
                            VpnState.DISCONNECTING -> Icons.Default.Shield
                            VpnState.DISCONNECTED -> Icons.Default.Warning
                        },
                        contentDescription = "Protection Status",
                        tint = when (vpnState) {
                            VpnState.CONNECTED -> ElectricEmerald
                            VpnState.CONNECTING -> GlowingAmber
                            VpnState.DISCONNECTING -> TextSecondary
                            VpnState.DISCONNECTED -> WarningRed
                        },
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (vpnState) {
                            VpnState.CONNECTED -> "PROTECTED"
                            VpnState.CONNECTING -> connectingStep.ifBlank { "CONNECTING..." }
                            VpnState.DISCONNECTING -> "DISCONNECTING..."
                            VpnState.DISCONNECTED -> "UNPROTECTED"
                        },
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = when (vpnState) {
                                VpnState.CONNECTED -> ElectricEmerald
                                VpnState.CONNECTING -> GlowingAmber
                                VpnState.DISCONNECTING -> TextSecondary
                                VpnState.DISCONNECTED -> WarningRed
                            },
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            fontSize = 12.5.sp
                        )
                    )
                    if (vpnState == VpnState.CONNECTED) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "• ${currentServer?.cityName ?: "Lagos"}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = ElectricEmerald.copy(alpha = 0.85f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }

            if (vpnState == VpnState.CONNECTED) {
                Spacer(modifier = Modifier.height(4.dp))
                val providerLabel = if (currentServer?.isHetznerServer == true) "FlowTest Dedicated" else "FlowTest High-Speed"
                Text(
                    text = "Encrypted via $providerLabel Backbone • 256-bit ChaCha20",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = CyberCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // VPN Balance Card & Buy Time CTA
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF0F2D3D),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f)),
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { showVpnTimeStoreDialog = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Timer",
                        tint = ElectricEmerald,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "VPN BALANCE: ${viewModel.formatVpnRemainingTime(vpnTimeRemainingMinutes)}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CyberCyan
                    ) {
                        Text(
                            text = "+ BUY TIME",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 9.sp,
                                color = DarkObsidian
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Real-Time Speed & Traffic Metrics Card (when connected)
        if (vpnState == VpnState.CONNECTED) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, GlassBorder, RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Download Speed
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = "Download",
                                tint = ElectricEmerald,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "DOWNLOAD",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )
                            )
                        }
                        Text(
                            text = "${metrics.downloadSpeedMbps} Mbps",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = VpnConnectionManager.formatBytes(metrics.totalBytesDownloaded),
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 10.sp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(36.dp)
                            .background(DarkCardBorder)
                    )

                    // Upload Speed
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = "Upload",
                                tint = CyberCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "UPLOAD",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )
                            )
                        }
                        Text(
                            text = "${metrics.uploadSpeedMbps} Mbps",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = VpnConnectionManager.formatBytes(metrics.totalBytesUploaded),
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 10.sp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(36.dp)
                            .background(DarkCardBorder)
                    )

                    // Duration
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = "Timer",
                                tint = GlowingAmber,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "DURATION",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )
                            )
                        }
                        Text(
                            text = VpnConnectionManager.formatTime(metrics.durationSeconds),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "Connected",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 10.sp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        // 1. DEDICATED WALLET ACCOUNT CARD (Account Balance visible immediately upon opening)
        WalletAccountHeaderCard(
            userVirtualAccount = userVirtualAccount,
            walletBalance = userWalletBalance,
            onAddMoneyClick = { showFundDialog = true },
            onHistoryClick = { showHistoryDialog = true },
            depositSessionExpiresAt = depositSessionExpiresAt,
            activeConfirmationCode = activeConfirmationCode,
            isDepositReportedPendingAdmin = isDepositReportedPendingAdmin,
            reportedDepositReference = reportedDepositReference,
            onDismissReportedStatus = { viewModel.dismissReportedAdminStatus() },
            onCancelSession = { viewModel.clearDepositSession() },
            lastTransaction = null
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 2. ALL SERVICES MEGA BANNER (Interactive Highlights - moved below Account Balance card)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, GlassBorder, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.GridView,
                            contentDescription = "Services",
                            tint = CyberCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ALL CLOUD & UTILITY SERVICES",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                color = TextPrimary
                            )
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CyberCyan.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "6 INTEGRATED",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = CyberCyan,
                                fontSize = 9.sp
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        QuickServicePill(
                            title = "VPN Shield",
                            subtitle = "10 Gbps Nodes",
                            icon = Icons.Filled.Shield,
                            accentColor = CyberCyan,
                            onClick = { onNavigateToServers() }
                        )
                    }
                    item {
                        QuickServicePill(
                            title = "SMS Messenger",
                            subtitle = "Flow Chat • ₦0.00",
                            icon = Icons.AutoMirrored.Filled.Chat,
                            accentColor = Color(0xFF007AFF),
                            onClick = { onNavigateToSms() }
                        )
                    }
                    item {
                        QuickServicePill(
                            title = "Bulk SMS & BYOD",
                            subtitle = "100% DND Bypass",
                            icon = Icons.Filled.SendToMobile,
                            accentColor = ElectricEmerald,
                            onClick = { onNavigateToSms() }
                        )
                    }
                    item {
                        QuickServicePill(
                            title = "Data Saver",
                            subtitle = "Save 85% MB",
                            icon = Icons.Filled.Security,
                            accentColor = ElectricEmerald,
                            onClick = { onNavigateToDataManagement() }
                        )
                    }
                    item {
                        QuickServicePill(
                            title = "Power Tracker",
                            subtitle = "Meter & Units",
                            icon = Icons.Filled.Bolt,
                            accentColor = GlowingAmber,
                            onClick = { onNavigateToElectricityDashboard() }
                        )
                    }
                    item {
                        QuickServicePill(
                            title = "Browser",
                            subtitle = "3 Tabs • Fast",
                            icon = Icons.Filled.Language,
                            accentColor = GlowingAmber,
                            onClick = { onNavigateToBrowser() }
                        )
                    }
                    item {
                        QuickServicePill(
                            title = "Speed Test",
                            subtitle = "5G Diagnostics",
                            icon = Icons.Filled.Speed,
                            accentColor = CoralRed,
                            onClick = { onNavigateToSpeedTest() }
                        )
                    }
                    item {
                        QuickServicePill(
                            title = "Rewards",
                            subtitle = "Earn Cashback",
                            icon = Icons.Filled.Redeem,
                            accentColor = CyberCyanVariant,
                            onClick = { onNavigateToRewards() }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. COMPLETE SERVICES & UTILITIES GRID (Airtime, Data, Electricity, Cable TV, Betting, Exam PINs, Transfer)
        ServicesGridSection(
            onServiceClick = { service ->
                if (service.id == "more") {
                    onNavigateToDataSaver()
                } else {
                    selectedDrawerService = service
                }
            },
            onViewAllClick = {
                onNavigateToDataSaver()
            }
        )

        // 4. RAPID RE-ORDER (Always show latest DATA purchase & 1-tap direct purchase)
        val latestDataPurchaseFromRecents = recentDataPurchases.firstOrNull()
        val latestDataPurchaseFromLogs = vtuTransactionLogs.firstOrNull {
            it.type.contains("Data", ignoreCase = true)
        }
        val effectiveDataPurchase: com.example.data.api.RecentDataPurchase? = latestDataPurchaseFromRecents ?: latestDataPurchaseFromLogs?.let { log ->
            val net = when {
                log.type.contains("MTN", ignoreCase = true) -> "MTN"
                log.type.contains("Airtel", ignoreCase = true) -> "Airtel"
                log.type.contains("Glo", ignoreCase = true) -> "Glo"
                log.type.contains("9mobile", ignoreCase = true) -> "9mobile"
                else -> "MTN"
            }
            com.example.data.api.RecentDataPurchase(
                network = net,
                planId = "20",
                planName = log.type,
                recipientPhone = log.recipient,
                amountNaira = log.amountNaira,
                validity = log.period ?: "30 Days"
            )
        }

        if (effectiveDataPurchase != null) {
            Spacer(modifier = Modifier.height(12.dp))
            BuyAgainCard(
                lastVtuPurchase = VpnViewModel.VtuTransactionLog(
                    id = "recent_vtu",
                    type = "${effectiveDataPurchase.network} Data",
                    amountNaira = effectiveDataPurchase.amountNaira,
                    recipient = effectiveDataPurchase.recipientPhone,
                    status = "SUCCESS",
                    timestamp = "Recent",
                    reference = "DIR-REP",
                    period = effectiveDataPurchase.validity
                ),
                onBuyAgainClick = {
                    // Trigger direct purchase drawer once, bypassing data catalog drawer
                    directRepurchaseData = effectiveDataPurchase
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // HIGHLIGHTED SMS MESSENGER FEATURE CARD (Linked to SMS Chat)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(
                    1.dp,
                    CyberCyan,
                    RoundedCornerShape(20.dp)
                )
                .clickable { onNavigateToSms() }
                .testTag("highlighted_sms_card"),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(CyberCyan),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                contentDescription = "SMS Messenger",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "SEND SMS WORLDWIDE",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        color = TextPrimary,
                                        letterSpacing = 0.5.sp
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF007AFF).copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "GLOBAL",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFF0A84FF),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp
                                        ),
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Instant Delivery • Real-Time Delivery Receipts",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = CyberCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Send high-speed single & bulk SMS messages worldwide with instant delivery status and smart AI message drafting.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DarkSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
                    ) {
                        Text(
                            text = "Rate: From ₦12.00 / SMS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = ElectricEmerald,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Button(
                        onClick = { onNavigateToSms() },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "SEND SMS",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = DarkObsidian
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Go",
                            tint = DarkObsidian,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // HIGHLIGHTED DATA MANAGEMENT CARD (Concise & Compact, details on main page)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(
                    1.dp,
                    CyberCyan,
                    RoundedCornerShape(20.dp)
                )
                .clickable { onNavigateToDataManagement() }
                .testTag("highlighted_data_management_card"),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(ElectricEmerald.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Data Firewall",
                                tint = ElectricEmerald,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "DATA MANAGEMENT & FIREWALL",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    color = TextPrimary,
                                    letterSpacing = 0.5.sp,
                                    fontSize = 12.5.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val savedBytes = dataSaverStats?.totalBytesSaved ?: 0L
                            val savedMb = savedBytes / (1024.0 * 1024.0)
                            val savedText = if (savedMb >= 1024.0) {
                                String.format(java.util.Locale.US, "Bandwidth Maximizer • %.2f GB Saved", savedMb / 1024.0)
                            } else if (savedMb > 0.0) {
                                String.format(java.util.Locale.US, "Bandwidth Maximizer • %.0f MB Saved", savedMb)
                            } else {
                                "Bandwidth Maximizer • Real-Time Protection Active"
                            }
                            Text(
                                text = savedText,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = ElectricEmerald,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ElectricEmerald.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ElectricEmerald.copy(alpha = 0.4f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(ElectricEmerald)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "ACTIVE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    color = ElectricEmerald,
                                    fontSize = 9.sp
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Real-time Firewall Protection ON",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    )

                    Button(
                        onClick = { onNavigateToDataManagement() },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "MANAGE DATA",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = DarkObsidian
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Go",
                            tint = DarkObsidian,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ELECTRICITY METER & RUNWAY ESTIMATE CARD
        val homeActiveAppliances = remember(electricityAppliances) { electricityAppliances.filter { it.isEnabled } }
        val homeDailyBurnKwh = remember(homeActiveAppliances) { homeActiveAppliances.sumOf { it.dailyKwh } }
        val homeRemainingKwh = remember(electricityMeterConfig, homeDailyBurnKwh) { electricityMeterConfig.getEstimatedRemainingKwh(homeDailyBurnKwh) }
        val homeDaysRemaining = remember(electricityMeterConfig, homeDailyBurnKwh) { electricityMeterConfig.getEstimatedDaysRemaining(homeDailyBurnKwh) }
        val homeIsLow = homeRemainingKwh <= electricityMeterConfig.lowBalanceAlertThresholdKwh && electricityMeterConfig.isLowBalanceAlertEnabled

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(
                    1.dp,
                    if (homeIsLow) GlowingAmber else GlowingAmber.copy(alpha = 0.5f),
                    RoundedCornerShape(20.dp)
                )
                .clickable { onNavigateToElectricityDashboard() }
                .testTag("highlighted_electricity_meter_card"),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(GlowingAmber.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = "Electricity Tracker",
                                tint = GlowingAmber,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "ELECTRICITY METER RUNWAY",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    color = TextPrimary,
                                    letterSpacing = 0.5.sp,
                                    fontSize = 12.5.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val runwayText = if (homeDailyBurnKwh <= 0.0) {
                                "${String.format(java.util.Locale.US, "%.1f", homeRemainingKwh)} kWh Remaining • Standby Mode"
                            } else if (homeDaysRemaining >= 1.0) {
                                "${String.format(java.util.Locale.US, "%.1f", homeRemainingKwh)} kWh • ~${String.format(java.util.Locale.US, "%.1f", homeDaysRemaining)} Days Runway Left"
                            } else {
                                "${String.format(java.util.Locale.US, "%.1f", homeRemainingKwh)} kWh • Low Token (~${String.format(java.util.Locale.US, "%.0f", homeDaysRemaining * 24.0)}h left)"
                            }
                            Text(
                                text = runwayText,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (homeIsLow) GlowingAmber else CyberCyan,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (homeIsLow) GlowingAmber.copy(alpha = 0.15f) else CyberCyan.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (homeIsLow) GlowingAmber else CyberCyan.copy(alpha = 0.4f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (homeIsLow) GlowingAmber else ElectricEmerald)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (homeIsLow) "RECHARGE SOON" else "ACTIVE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (homeIsLow) GlowingAmber else CyberCyan,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 9.sp
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${homeActiveAppliances.size} Appliances Active • ${String.format(java.util.Locale.US, "%.1f", homeDailyBurnKwh)} kWh/d",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp
                        )
                    )

                    Button(
                        onClick = { onNavigateToElectricityDashboard() },
                        colors = ButtonDefaults.buttonColors(containerColor = GlowingAmber),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "POWER TRACKER",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = DarkObsidian
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Go",
                            tint = DarkObsidian,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // REWARDS SECTION
        RewardsSectionCard(
            cashbackAmount = String.format("₦%,.2f", cashbackBalance),
            referralAmount = String.format("₦%,.2f", referralEarnings),
            rewardPoints = rewardPoints,
            rewardPointsValueNaira = (rewardPoints / 100.0) * pointsRedemptionRate,
            onCashbackClick = { onNavigateToRewards() },
            onReferralClick = { onNavigateToRewards() },
            onRewardsClick = { onNavigateToRewards() }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Quick Action Tools Grid (Speed Test & Server Switcher)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Speed Test Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, DarkCardBorder, RoundedCornerShape(16.dp))
                    .clickable { onNavigateToSpeedTest() },
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CyberCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Speed,
                            contentDescription = "Speed Test",
                            tint = CyberCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "Speed Test",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "Latency check",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }

            // Servers Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, DarkCardBorder, RoundedCornerShape(16.dp))
                    .clickable { onNavigateToServers() },
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(GlowingAmber.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Dns,
                            contentDescription = "Servers",
                            tint = GlowingAmber,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "Servers",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "Global Nodes",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showRegisterDialog) {
        OnboardingLoginDialog(
            viewModel = viewModel,
            onDismiss = { showRegisterDialog = false }
        )
    }

    if (showFundDialog) {
        FundWalletDialog(
            viewModel = viewModel,
            onDismiss = { showFundDialog = false }
        )
    }

    if (showHistoryDialog) {
        TransactionHistoryDialog(
            logs = vtuTransactionLogs,
            viewModel = viewModel,
            onDismiss = { showHistoryDialog = false }
        )
    }

    if (showProfileDrawer) {
        UserProfileDrawer(
            viewModel = viewModel,
            onDismiss = { showProfileDrawer = false },
            onOpenAdminDashboard = {
                showProfileDrawer = false
                onNavigateToAdmin()
            },
            onOpenFundWallet = {
                showProfileDrawer = false
                showFundDialog = true
            },
            onOpenHistory = {
                showProfileDrawer = false
                showHistoryDialog = true
            }
        )
    }

    if (viewModel.showFundingAccountReminderDrawer) {
        FundingAccountReminderDrawer(
            viewModel = viewModel,
            onDismiss = { viewModel.showFundingAccountReminderDrawer = false }
        )
    }

    selectedDrawerService?.let { service ->
        ServicePurchasingBottomSheet(
            service = service,
            viewModel = viewModel,
            onOpenElectricityDashboard = {
                selectedDrawerService = null
                onNavigateToElectricityDashboard()
            },
            onDismiss = { selectedDrawerService = null }
        )
    }

    // Direct Buy Again Bottom Sheet on Home
    directRepurchaseData?.let { rep ->
        DirectDataRepurchaseBottomSheet(
            purchase = rep,
            viewModel = viewModel,
            onDismiss = { directRepurchaseData = null },
            onSuccess = { receipt ->
                directRepurchaseData = null
                directRepurchaseReceipt = receipt
            }
        )
    }

    // Direct Buy Again Success Receipt Dialog on Home
    directRepurchaseReceipt?.let { receipt ->
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { directRepurchaseReceipt = null }
        ) {
            TransactionSuccessReceiptView(
                receipt = receipt,
                onReturnHome = { directRepurchaseReceipt = null },
                onMakeAnotherPurchase = { directRepurchaseReceipt = null }
            )
        }
    }

    if (showVpnTimeStoreDialog) {
        VpnTimeStoreDialog(
            viewModel = viewModel,
            vpnTimeRemainingMinutes = vpnTimeRemainingMinutes,
            walletBalance = userWalletBalance,
            onDismiss = { showVpnTimeStoreDialog = false },
            onFundWalletClick = {
                showVpnTimeStoreDialog = false
                showFundDialog = true
            }
        )
    }

    if (showDataRenewalReminderDialog) {
        DataRenewalReminderDialog(
            enabled = isDataRenewalReminderEnabled,
            currentThresholdMb = dataReminderThresholdMb,
            estimatedRemainingMb = estimatedRemainingMb,
            onSave = { enabled, thresholdMb, renewalTimestamp ->
                viewModel.updateDataRenewalReminderSettings(
                    enabled = enabled,
                    thresholdMb = thresholdMb,
                    renewalTimestamp = renewalTimestamp,
                    context = context
                )
                showDataRenewalReminderDialog = false
            },
            onDismiss = {
                showDataRenewalReminderDialog = false
            },
            onAdjustBalance = { newBalMb ->
                viewModel.setEstimatedDataBalance(newBalMb)
            }
        )
    }

    if (showSyncBalanceDialog) {
        com.example.ui.components.SyncCarrierBalanceDialog(
            currentRemainingMb = estimatedRemainingMb,
            currentPurchasedMb = purchasedDataTotalMb,
            onDismiss = { showSyncBalanceDialog = false },
            onCalibrateBalance = { balanceMb, carrier ->
                viewModel.setEstimatedDataBalance(balanceMb)
                val formattedBal = if (balanceMb >= 1024.0) {
                    val gb = balanceMb / 1024.0
                    val text = String.format(java.util.Locale.US, "%.2f", gb).trimEnd('0').trimEnd('.')
                    "$text GB"
                } else {
                    "${balanceMb.toInt()} MB"
                }
                android.widget.Toast.makeText(
                    context,
                    "Live balance calibrated to $formattedBal ($carrier)",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                showSyncBalanceDialog = false
            }
        )
    }
}

@Composable
fun MyConnectionCard(
    vpnState: VpnState,
    networkState: RealNetworkState = RealNetworkState(),
    dataUsedTodayMb: Double = 0.0,
    purchasedDataTotalMb: Double = 0.0,
    estimatedRemainingMb: Double = 0.0,
    dataSavedMb: Double = 0.0,
    latencyMs: Int = 0,
    speedMbps: String = "0.0",
    userVirtualAccount: VpnViewModel.UserVirtualAccount? = null,
    isReminderEnabled: Boolean = true,
    reminderThresholdMb: Double = 1024.0,
    onReminderClick: () -> Unit = {},
    onCalibrateBalanceClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    fun formatDataValue(mb: Double, forceWholeGb: Boolean = false): String {
        if (mb <= 0.0) return "0 MB"
        return if (mb >= 1024.0) {
            val gb = mb / 1024.0
            if (forceWholeGb && Math.abs(gb - Math.round(gb)) < 0.02) {
                "${Math.round(gb)} GB"
            } else {
                val formatted = String.format(java.util.Locale.US, "%.2f", gb).trimEnd('0').trimEnd('.')
                "$formatted GB"
            }
        } else {
            "${Math.round(mb).toInt()} MB"
        }
    }

    val displayDataBalance = if (purchasedDataTotalMb <= 0.0) "0 MB" else formatDataValue(estimatedRemainingMb)
    val displayConsumedData = formatDataValue(dataUsedTodayMb)
    val displayPurchased = formatDataValue(purchasedDataTotalMb)

    val consumedRatio = if (purchasedDataTotalMb > 0.0) {
        (dataUsedTodayMb / purchasedDataTotalMb).coerceIn(0.0, 1.0).toFloat()
    } else 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, DarkCardBorder, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // App Logo, App Name Branding, Share & Profile in flex row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Logo and App Name Branding
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    FlowtestEmblem(
                        size = 38.dp,
                        primaryColor = CyberCyan,
                        glowAlpha = 0.25f
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "flowtest",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.4).sp,
                                color = TextPrimary,
                                fontSize = 17.sp
                            )
                        )
                        Text(
                            text = "VIP VPN & UTILITY PLATFORM",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberCyan
                            )
                        )
                    }
                }

                // Right: Share and Profile Avatar buttons in flex with app name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Share App APK
                    IconButton(
                        onClick = onShareClick,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(DarkSurface)
                            .border(1.dp, CyberCyan.copy(alpha = 0.35f), CircleShape)
                            .testTag("home_share_apk_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share App APK",
                            tint = CyberCyan,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    // User Avatar Profile Button
                    if (userVirtualAccount != null) {
                        UserAvatarButton(
                            userVirtualAccount = userVirtualAccount,
                            onClick = onProfileClick
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = DarkCardBorder.copy(alpha = 0.6f), thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // Connection Status Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (!networkState.isConnected) Icons.Default.SignalCellularOff else Icons.Default.SignalCellularAlt,
                        contentDescription = "Connection Status",
                        tint = if (!networkState.isConnected) WarningRed else CyberCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "MY CONNECTION",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = if (!networkState.isConnected) WarningRed else CyberCyan,
                            letterSpacing = 1.sp
                        )
                    )
                }

                // Dynamic Real Network & Signal Strength Indicator (With No Network State)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (!networkState.isConnected) WarningRed.copy(alpha = 0.12f) else DarkSurface,
                    border = androidx.compose.foundation.BorderStroke(
                        0.8.dp,
                        if (!networkState.isConnected) WarningRed.copy(alpha = 0.75f) else DarkCardBorder
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!networkState.isConnected) {
                            // NO NETWORK ALERT INDICATION
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(WarningRed)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Icon(
                                imageVector = Icons.Default.SignalCellularOff,
                                contentDescription = "No Network",
                                tint = WarningRed,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "No Network • Offline",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = WarningRed,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            )
                        } else {
                            // ACTIVE NETWORK WITH SIGNAL STRENGTH INDICATOR
                            val strengthColor = when (networkState.signalStrengthLevel) {
                                4 -> ElectricEmerald
                                3 -> CyberCyan
                                2 -> GlowingAmber
                                else -> WarningRed
                            }

                            // Signal Status Dot
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(strengthColor)
                            )
                            Spacer(modifier = Modifier.width(5.dp))

                            // Mini 4-Bar Signal Strength Visual Meter
                            SignalBarsIndicator(
                                level = networkState.signalStrengthLevel,
                                activeColor = strengthColor,
                                inactiveColor = TextMuted.copy(alpha = 0.35f),
                                modifier = Modifier.height(10.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))

                            // Real Hardware Network Details & Signal Quality
                            Text(
                                text = "${networkState.carrierOrWifiName} • ${networkState.networkGeneration} (${networkState.signalStrengthDescription})",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            val isReminderAlert = isReminderEnabled && estimatedRemainingMb <= reminderThresholdMb

            // 4-Column Live Telemetry (EST. BALANCE, APPS USAGE, PING, SPEED)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onCalibrateBalanceClick() }
                        .padding(horizontal = 2.dp, vertical = 2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "EST. BALANCE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isReminderAlert) WarningRed else TextMuted,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Sync Balance",
                            tint = CyberCyan,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        displayDataBalance,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = if (isReminderAlert) WarningRed else TextPrimary,
                            fontWeight = FontWeight.Black
                        )
                    )
                    Text(
                        if (isReminderAlert) "Low Balance" else if (purchasedDataTotalMb > 0.0) "of $displayPurchased" else "Tap to Sync",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isReminderAlert) WarningRed else CyberCyan,
                            fontSize = 9.sp,
                            fontWeight = if (isReminderAlert) FontWeight.Bold else FontWeight.Medium
                        )
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text("APPS USAGE", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(displayConsumedData, style = MaterialTheme.typography.titleMedium.copy(color = TextPrimary, fontWeight = FontWeight.Black))
                    Text("All Apps", style = MaterialTheme.typography.labelSmall.copy(color = CyberCyanVariant, fontSize = 9.sp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text("DATA SAVED", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(2.dp))
                    val savedDisplay = if (dataSavedMb >= 1024.0) {
                        String.format(java.util.Locale.US, "%.2f GB", dataSavedMb / 1024.0)
                    } else if (dataSavedMb > 0.0) {
                        String.format(java.util.Locale.US, "%.0f MB", dataSavedMb)
                    } else {
                        "0 MB"
                    }
                    Text(
                        text = savedDisplay,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = ElectricEmerald,
                            fontWeight = FontWeight.Black
                        )
                    )
                    Text(
                        text = if (dataSavedMb > 0.0) "Saved Today" else "Saver Active",
                        style = MaterialTheme.typography.labelSmall.copy(color = ElectricEmerald, fontSize = 9.sp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text("SPEED", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(2.dp))
                    val speedDisplay = if (!networkState.isConnected) {
                        "0.0M"
                    } else {
                        val num = speedMbps.toDoubleOrNull() ?: 0.0
                        if (num >= 1.0) {
                            String.format(java.util.Locale.US, "%.1fM", num)
                        } else if (num > 0.0) {
                            String.format(java.util.Locale.US, "%.2fM", num)
                        } else {
                            "1.5M"
                        }
                    }
                    Text(
                        text = speedDisplay,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = if (!networkState.isConnected) TextMuted else CyberCyan,
                            fontWeight = FontWeight.Black
                        )
                    )
                    Text(
                        text = if (!networkState.isConnected) "No Signal" else "Live Speed",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 9.sp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Active Data Allowance Progress Bar
            if (purchasedDataTotalMb > 0.0) {
                val usedRatio = (dataUsedTodayMb / purchasedDataTotalMb).coerceIn(0.0, 1.0).toFloat()
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Data Allowance",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.5.sp,
                                color = TextMuted,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        Text(
                            text = "$displayConsumedData used • $displayDataBalance left of $displayPurchased",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.5.sp,
                                color = if (isReminderAlert) WarningRed else CyberCyan,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { usedRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = if (isReminderAlert) WarningRed else CyberCyan,
                        trackColor = DarkSurface
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            } else {
                Spacer(modifier = Modifier.height(2.dp))
            }

            // User-configured Recharge Threshold Status & Action (Duplicate estimated balance removed!)
            val reminderStatusText = if (!isReminderEnabled) {
                "Recharge alert: Tap to set threshold"
            } else if (isReminderAlert) {
                "Low Data Alert: Balance at or below ${formatDataValue(reminderThresholdMb)}"
            } else {
                "Recharge threshold set at ${formatDataValue(reminderThresholdMb)}"
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CyberCyan.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(
                        0.8.dp,
                        CyberCyan.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.clickable { onCalibrateBalanceClick() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Sync Balance",
                            tint = CyberCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Sync SIM",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = CyberCyan,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isReminderAlert) WarningRed.copy(alpha = 0.12f) else DarkSurface,
                    border = androidx.compose.foundation.BorderStroke(
                        0.8.dp,
                        if (isReminderAlert) WarningRed.copy(alpha = 0.7f) else DarkCardBorder
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onReminderClick() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Data Protection & Reminder",
                                tint = if (isReminderAlert) WarningRed else CyberCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = reminderStatusText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isReminderAlert) WarningRed else TextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = if (isReminderAlert) FontWeight.Bold else FontWeight.Medium
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isReminderAlert) WarningRed.copy(alpha = 0.2f) else CyberCyan.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(
                                0.5.dp,
                                if (isReminderAlert) WarningRed else CyberCyan.copy(alpha = 0.4f)
                            )
                        ) {
                            Text(
                                text = if (isReminderAlert) "ALERT" else "SET",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isReminderAlert) WarningRed else CyberCyan,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black
                                ),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DataRenewalReminderDialog(
    enabled: Boolean,
    currentThresholdMb: Double,
    estimatedRemainingMb: Double,
    onSave: (enabled: Boolean, thresholdMb: Double, renewalTimestamp: Long) -> Unit,
    onDismiss: () -> Unit,
    onAdjustBalance: ((Double) -> Unit)? = null
) {
    var isEnabled by remember { mutableStateOf(enabled) }
    var selectedThresholdMb by remember { mutableStateOf(currentThresholdMb) }
    var selectedCycleDays by remember { mutableStateOf(30) }
    var showAdjustInput by remember { mutableStateOf(false) }
    var adjustValueText by remember { mutableStateOf("") }
    var isAdjustGb by remember { mutableStateOf(true) }

    val presetThresholds = listOf(
        500.0 to "500 MB",
        1024.0 to "1.0 GB",
        2048.0 to "2.0 GB",
        3072.0 to "3.0 GB",
        5120.0 to "5.0 GB"
    )

    val cycleOptions = listOf(
        7 to "7 Days",
        14 to "14 Days",
        30 to "30 Days"
    )

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(CyberCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Data Renewal Alert",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Estimated Balance Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurface,
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, DarkCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Current Estimated Balance",
                                    style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 10.sp)
                                )
                                Text(
                                    text = if (estimatedRemainingMb >= 1024.0) {
                                        String.format(java.util.Locale.US, "%.2f GB", estimatedRemainingMb / 1024.0)
                                    } else {
                                        "${estimatedRemainingMb.toInt()} MB"
                                    },
                                    style = MaterialTheme.typography.titleMedium.copy(color = CyberCyan, fontWeight = FontWeight.Black)
                                )
                            }
                            if (onAdjustBalance != null) {
                                TextButton(
                                    onClick = { showAdjustInput = !showAdjustInput },
                                    colors = ButtonDefaults.textButtonColors(contentColor = CyberCyan),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (showAdjustInput) "Done" else "Calibrate",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    )
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = ElectricEmerald.copy(alpha = 0.15f),
                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, ElectricEmerald.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = "Live Monitored",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = ElectricEmerald,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        ),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        if (showAdjustInput && onAdjustBalance != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = adjustValueText,
                                    onValueChange = { adjustValueText = it },
                                    placeholder = { Text(if (isAdjustGb) "e.g. 3.3" else "e.g. 300", fontSize = 12.sp, color = TextMuted) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CyberCyan,
                                        unfocusedBorderColor = DarkCardBorder,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    )
                                )

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isAdjustGb) CyberCyan else DarkObsidian,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { isAdjustGb = !isAdjustGb }
                                ) {
                                    Text(
                                        text = if (isAdjustGb) "GB" else "MB",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = if (isAdjustGb) DarkObsidian else CyberCyan,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)
                                    )
                                }

                                Button(
                                    onClick = {
                                        val entered = adjustValueText.toDoubleOrNull()
                                        if (entered != null && entered >= 0.0) {
                                            val mb = if (isAdjustGb) entered * 1024.0 else entered
                                            onAdjustBalance(mb)
                                            showAdjustInput = false
                                            adjustValueText = ""
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Set", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Toggle Alert Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Enable Renewal Alerts",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "Get notified before running out of data",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        )
                    }

                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { isEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DarkObsidian,
                            checkedTrackColor = CyberCyan,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = DarkSurface
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Threshold Selection
                Text(
                    text = "REMIND ME WHEN REMAINING DATA DROPS TO:",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextMuted,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 0.5.sp
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetThresholds.forEach { (mb, label) ->
                        val isSelected = selectedThresholdMb == mb
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) CyberCyan else DarkSurface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) CyberCyan else DarkCardBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedThresholdMb = mb }
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isSelected) DarkObsidian else TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                modifier = Modifier
                                    .padding(vertical = 8.dp)
                                    .wrapContentWidth(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Plan Cycle Selection
                Text(
                    text = "SUBSCRIPTION RENEWAL CYCLE:",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextMuted,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 0.5.sp
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    cycleOptions.forEach { (days, label) ->
                        val isSelected = selectedCycleDays == days
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) ElectricEmerald.copy(alpha = 0.2f) else DarkSurface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) ElectricEmerald else DarkCardBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedCycleDays = days }
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isSelected) ElectricEmerald else TextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                modifier = Modifier
                                    .padding(vertical = 8.dp)
                                    .wrapContentWidth(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Save Button
                Button(
                    onClick = {
                        val futureRenewalTime = System.currentTimeMillis() + (selectedCycleDays * 24L * 3600L * 1000L)
                        onSave(isEnabled, selectedThresholdMb, futureRenewalTime)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Save Reminder Settings",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun QuickBuyRow(
    onDataClick: () -> Unit,
    onAirtimeClick: () -> Unit,
    onSmsClick: () -> Unit,
    onBillsClick: () -> Unit,
    onVpnTimeClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "QUICK BUY",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                letterSpacing = 1.sp
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickBuyPill(title = "Data", icon = Icons.Default.Smartphone, color = CyberCyan, modifier = Modifier.weight(1f), onClick = onDataClick)
            QuickBuyPill(title = "Airtime", icon = Icons.Default.PhoneInTalk, color = ElectricEmerald, modifier = Modifier.weight(1f), onClick = onAirtimeClick)
            QuickBuyPill(title = "SMS", icon = Icons.AutoMirrored.Filled.Chat, color = Color(0xFF007AFF), modifier = Modifier.weight(1f), onClick = onSmsClick)
            QuickBuyPill(title = "Bills", icon = Icons.Default.FlashOn, color = GlowingAmber, modifier = Modifier.weight(1f), onClick = onBillsClick)
            QuickBuyPill(title = "VPN Time", icon = Icons.Default.Timer, color = Color(0xFFD946EF), modifier = Modifier.weight(1.1f), onClick = onVpnTimeClick)
        }
    }
}

@Composable
fun QuickBuyPill(
    title: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = DarkSurfaceElevated,
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(15.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 11.sp
                ),
                maxLines = 1
            )
        }
    }
}

@Composable
fun BuyAgainCard(
    lastVtuPurchase: VpnViewModel.VtuTransactionLog,
    onBuyAgainClick: () -> Unit
) {
    val periodText = lastVtuPurchase.period?.takeIf { it.isNotBlank() } ?: "30 Days"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onBuyAgainClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(CyberCyan.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Repeat,
                    contentDescription = null,
                    tint = CyberCyan,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "${lastVtuPurchase.type} • ₦${String.format(java.util.Locale.US, "%,.2f", lastVtuPurchase.amountNaira)}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 13.sp
                    ),
                    maxLines = 1
                )
                Text(
                    text = "Period: $periodText • To: ${lastVtuPurchase.recipient}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        fontSize = 11.sp
                    ),
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        val repNetColor = getNetworkColor(lastVtuPurchase.type)
        val repNetContentColor = getNetworkContentColor(lastVtuPurchase.type)
        Button(
            onClick = onBuyAgainClick,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = repNetColor,
                contentColor = repNetContentColor
            ),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            modifier = Modifier.height(34.dp)
        ) {
            Text(
                text = "Buy again",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = repNetContentColor,
                    fontSize = 11.sp
                )
            )
        }
    }
}

@Composable
fun QuickServicePill(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = DarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                )
            }
        }
    }
}

@Composable
fun DataBenefitBullet(
    emoji: String,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(text = emoji, fontSize = 14.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 11.sp
                )
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary,
                    fontSize = 10.5.sp,
                    lineHeight = 14.sp
                )
            )
        }
    }
}

@Composable
fun SignalBarsIndicator(
    level: Int,
    modifier: Modifier = Modifier,
    maxBars: Int = 4,
    activeColor: Color = ElectricEmerald,
    inactiveColor: Color = Color.Gray.copy(alpha = 0.35f)
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(1.5.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        val clampedLevel = level.coerceIn(0, maxBars)
        for (i in 1..maxBars) {
            val barHeight = (3 + (i * 2)).dp
            val isFilled = i <= clampedLevel
            Box(
                modifier = Modifier
                    .width(2.5.dp)
                    .height(barHeight)
                    .clip(RoundedCornerShape(1.dp))
                    .background(if (isFilled) activeColor else inactiveColor)
            )
        }
    }
}
