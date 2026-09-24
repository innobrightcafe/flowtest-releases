package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import android.widget.Toast
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import com.example.data.api.PairgateCatalogHelper
import com.example.data.api.DataBundleDisplayPlan
import com.example.data.db.TelcoBundleEntity
import com.example.data.ui.viewmodel.VpnViewModel
import com.example.ui.components.FlowButtonLoadingLine
import com.example.ui.components.FullServicesGridSection
import com.example.ui.components.FundWalletDialog
import com.example.ui.components.OffersSectionCard
import com.example.ui.components.OnboardingLoginDialog
import com.example.ui.components.PhoneVerificationDialog
import com.example.ui.components.PurchaseSuccessReceipt
import com.example.ui.components.ReflectiveBundleListSkeleton
import com.example.ui.components.RewardsSectionCard
import com.example.ui.components.ServiceItem
import com.example.ui.components.ServicePurchasingBottomSheet
import com.example.ui.components.TransactionHistoryDialog
import com.example.ui.components.TransactionSuccessReceiptView
import com.example.ui.components.WalletAccountHeaderCard
import com.example.ui.theme.*

data class BadgeItem(val title: String, val subtitle: String, val icon: String, val isUnlocked: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataTrackerScreen(
    viewModel: VpnViewModel,
    onNavigateBack: () -> Unit = {},
    onNavigateToRewards: () -> Unit = {},
    onNavigateToElectricityDashboard: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val stats by viewModel.dataSaverStats.collectAsStateWithLifecycle()
    val bundles by viewModel.telcoBundles.collectAsStateWithLifecycle()
    val firewallApps by viewModel.firewallApps.collectAsStateWithLifecycle()

    val walletBalance by viewModel.userWalletBalance.collectAsStateWithLifecycle()
    val userVirtualAccount by viewModel.userVirtualAccount.collectAsStateWithLifecycle()
    val cashbackBalance by viewModel.cashbackBalance.collectAsStateWithLifecycle()
    val referralEarnings by viewModel.referralEarnings.collectAsStateWithLifecycle()
    val vtuTransactionLogs by viewModel.vtuTransactionLogs.collectAsStateWithLifecycle()
    val depositSessionExpiresAt by viewModel.depositSessionExpiresAt.collectAsStateWithLifecycle()
    val activeConfirmationCode by viewModel.activeConfirmationCode.collectAsStateWithLifecycle()
    val isDepositReportedPendingAdmin by viewModel.isDepositReportedPendingAdmin.collectAsStateWithLifecycle()
    val reportedDepositReference by viewModel.reportedDepositReference.collectAsStateWithLifecycle()

    var showFundDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showOnboardingDialog by remember { mutableStateOf(false) }
    var selectedDrawerService by remember { mutableStateOf<ServiceItem?>(null) }
    var copySuccessMsg by remember { mutableStateOf<String?>(null) }
    val clipboardManager = LocalClipboardManager.current

    var activeTabCategory by remember { mutableStateOf("Data") } // Data, Airtime, Cable, Electricity
    var selectedProvider by remember { mutableStateOf("All") }
    var phoneNumberInput by remember(userVirtualAccount.phoneNumber) {
        mutableStateOf(userVirtualAccount.phoneNumber.trim())
    }
    var airtimeAmountInput by remember { mutableStateOf("500") }
    var cableDecoderInput by remember { mutableStateOf("") }
    var electricityMeterInput by remember { mutableStateOf("") }
    var selectedCablePackage by remember { mutableStateOf("DSTV Yanga - ₦5,100") }
    var selectedDisco by remember { mutableStateOf("Ikeja Electric (IKEDC)") }
    val context = LocalContext.current

    var showPhoneSetupDialog by remember { mutableStateOf(false) }
    var newPhoneInput by remember { mutableStateOf("") }
    var showDataSafetyDrawer by remember { mutableStateOf(false) }

    var selectedBundleForBuy by remember { mutableStateOf<DataBundleDisplayPlan?>(null) }
    var purchaseResultMsg by remember { mutableStateOf<String?>(null) }
    var purchaseReceipt by remember { mutableStateOf<PurchaseSuccessReceipt?>(null) }
    var isPurchasing by remember { mutableStateOf(false) }

    var isAlertDismissed by remember { mutableStateOf(false) }
    var showSyncBalanceDialog by remember { mutableStateOf(false) }

    val livePairgatePlans by viewModel.pairgateDataPlans.collectAsStateWithLifecycle()
    val isFetchingDataPlans by viewModel.isFetchingDataPlans.collectAsStateWithLifecycle()
    val vtuMarkupPercent by viewModel.vtuMarkupPercent.collectAsStateWithLifecycle()
    val dataPricingStrategy by viewModel.dataPricingStrategy.collectAsStateWithLifecycle()
    val telcoDiscountPercent by viewModel.telcoDiscountPercent.collectAsStateWithLifecycle()

    val dailyLimit = stats?.dailyLimitMb ?: 1000
    val totalDeviceBytes = remember {
        val rx = android.net.TrafficStats.getTotalRxBytes()
        val tx = android.net.TrafficStats.getTotalTxBytes()
        if (rx > 0 && tx > 0) rx + tx else 0L
    }
    val usedTodayMb = (totalDeviceBytes / (1024 * 1024)).toInt()
    val progress = (usedTodayMb.toFloat() / dailyLimit.toFloat()).coerceIn(0f, 1f)
    val streakDays = stats?.streakDays ?: 1

    val estimatedRemainingMb by viewModel.estimatedDataBalanceMb.collectAsStateWithLifecycle()
    val dataReminderThresholdMb by viewModel.dataReminderThresholdMb.collectAsStateWithLifecycle()
    val isReminderEnabled by viewModel.isDataRenewalReminderEnabled.collectAsStateWithLifecycle()

    val estimatedRemainingGb = estimatedRemainingMb / 1024.0
    val reminderThresholdGb = dataReminderThresholdMb / 1024.0
    val isAlertTriggered = isReminderEnabled && estimatedRemainingMb <= dataReminderThresholdMb
    val dataHealthPercent = if (dataReminderThresholdMb > 0) ((estimatedRemainingMb / (dataReminderThresholdMb * 2.0)) * 100).toInt().coerceIn(0, 100) else 100

    val filteredBundles: List<DataBundleDisplayPlan> = remember(livePairgatePlans, vtuMarkupPercent, selectedProvider, dataPricingStrategy, telcoDiscountPercent) {
        if (selectedProvider.equals("All", ignoreCase = true)) {
            listOf("MTN", "Airtel", "Glo", "9mobile").flatMap { net ->
                PairgateCatalogHelper.buildCatalogForNetwork(
                    network = net,
                    livePlans = livePairgatePlans,
                    markupPercent = vtuMarkupPercent,
                    pricingStrategy = dataPricingStrategy,
                    discountPercent = telcoDiscountPercent
                )
            }.sortedBy { it.price }
        } else {
            PairgateCatalogHelper.buildCatalogForNetwork(
                network = selectedProvider,
                livePlans = livePairgatePlans,
                markupPercent = vtuMarkupPercent,
                pricingStrategy = dataPricingStrategy,
                discountPercent = telcoDiscountPercent
            )
        }
    }

    val badges = listOf(
        BadgeItem("Data Guardian", "Saved over 1 GB", "🛡️", true),
        BadgeItem("Airtime & Data", "Instant Top-Up", "⚡", true),
        BadgeItem("Ad Shield Hero", "2,000+ Ads Blocked", "🦸", true),
        BadgeItem("Zero-Waste Streamer", "10 Proxy Sessions", "📺", false)
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkObsidian)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("data_tracker_screen_container")
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "AIRTIME & DATA HUB",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp,
                        color = TextPrimary
                    )
                )
                Text(
                    text = "Instant Data Bundles, Airtime & Utilities",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = ElectricEmerald.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(1.dp, ElectricEmerald.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "⚡ INSTANT TOP-UP", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = ElectricEmerald))
                }
            }
        }

        // Wallet Account Card
        WalletAccountHeaderCard(
            userVirtualAccount = userVirtualAccount,
            walletBalance = walletBalance,
            onAddMoneyClick = { showFundDialog = true },
            onHistoryClick = { showHistoryDialog = true },
            depositSessionExpiresAt = depositSessionExpiresAt,
            activeConfirmationCode = activeConfirmationCode,
            isDepositReportedPendingAdmin = isDepositReportedPendingAdmin,
            reportedDepositReference = reportedDepositReference,
            onDismissReportedStatus = { viewModel.dismissReportedAdminStatus() },
            onCancelSession = { viewModel.clearDepositSession() }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Services 4 per line Grid (Clicking each opens purchasing drawer)
        FullServicesGridSection(
            onServiceClick = { service ->
                if (service.id == "power_tracker" || service.id == "electricity_dashboard") {
                    onNavigateToElectricityDashboard()
                } else {
                    selectedDrawerService = service
                }
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 3rd Box: Offers Section
        OffersSectionCard(
            onOfferClick = { promoOffer ->
                selectedDrawerService = ServiceItem("data", promoOffer.title, Icons.Default.Smartphone, promoOffer.subtitle)
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Rewards Program Section
        RewardsSectionCard(
            cashbackAmount = String.format("₦%,.2f", cashbackBalance),
            referralAmount = String.format("₦%,.2f", referralEarnings),
            onCashbackClick = { onNavigateToRewards() },
            onReferralClick = { onNavigateToRewards() }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Real-Time High-Consumption Alert Banner
        val highUsageApp = firewallApps.firstOrNull { it.dataUsageMb >= 100.0 && !it.isCellularBlocked && !it.isBackgroundFrozen }
        if (!isAlertDismissed && highUsageApp != null && stats?.isHighDepletionAlertEnabled == true) {
            Card(
                colors = CardDefaults.cardColors(containerColor = WarningRed.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, WarningRed.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(WarningRed.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Warning,
                                contentDescription = "Alert",
                                tint = WarningRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = "High Data Depletion Alert!",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = "${highUsageApp.appName} consumed ${String.format(java.util.Locale.US, "%.0f MB", highUsageApp.dataUsageMb)} today.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = WarningRed,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    viewModel.toggleCellularBlocked(highUsageApp.packageName, true)
                                    isAlertDismissed = true
                                }
                        ) {
                            Text(
                                text = "Freeze",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                ),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }

                        IconButton(
                            onClick = { isAlertDismissed = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Dismiss",
                                tint = TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Approximate Data Balance Tracker Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = DarkSurface
            ),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SignalCellularAlt,
                            contentDescription = "Data Balance",
                            tint = CyberCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "APPROXIMATE DATA BALANCE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                letterSpacing = 0.8.sp
                            )
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ElectricEmerald.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, ElectricEmerald.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "$dataHealthPercent% Remaining",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = ElectricEmerald,
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = if (estimatedRemainingGb >= 1.0) String.format("%.2f", estimatedRemainingGb) else "${estimatedRemainingMb.toInt()}",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    color = if (dataHealthPercent < 15) WarningRed else CyberCyan,
                                    fontSize = 32.sp
                                )
                            )
                            Text(
                                text = if (estimatedRemainingGb >= 1.0) " GB" else " MB",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(bottom = 4.dp, start = 2.dp)
                            )
                        }
                        Text(
                            text = "Estimated Remaining Data on Device",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Reminder Alert",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        )
                        Text(
                            text = if (isAlertTriggered) "🚨 Low Data Alert" else if (reminderThresholdGb >= 1.0) "At ${String.format("%.1f GB", reminderThresholdGb)}" else "At ${dataReminderThresholdMb.toInt()} MB",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = if (isAlertTriggered) WarningRed else CyberCyan,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LinearProgressIndicator(
                    progress = { (dataHealthPercent / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (dataHealthPercent < 15) WarningRed else CyberCyan,
                    trackColor = DarkSurfaceElevated
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Calculated from in-app recharges & live usage stats",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextMuted,
                            fontSize = 10.sp
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = CyberCyan.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(0.8.dp, CyberCyan.copy(alpha = 0.4f)),
                            modifier = Modifier.clickable { showSyncBalanceDialog = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Sync, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(11.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Sync SIM",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = CyberCyan,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        }
                        Text(
                            text = "+ Top Up",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = ElectricEmerald,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable {
                                    activeTabCategory = "Data"
                                }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // Daily Data Budget Meter Gauge
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DAILY DATA BUDGET METER",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DarkSurfaceElevated,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                val nextLimit = if (dailyLimit >= 2000) 500 else dailyLimit + 500
                                viewModel.updateDailyLimitMb(nextLimit)
                            }
                    ) {
                        Text(
                            text = "Cap: $dailyLimit MB ✏️",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = CyberCyan,
                                fontSize = 11.sp
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "$usedTodayMb",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Black,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = " / $dailyLimit MB Used",
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = TextMuted
                            ),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            color = if (progress > 0.8f) WarningRed else CyberCyan
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    color = if (progress > 0.8f) WarningRed else CyberCyan,
                    trackColor = DarkSurfaceElevated
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Remaining Data Safe Zone: ${dailyLimit - usedTodayMb} MB",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = ElectricEmerald,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }

        // Gamified Badges Section
        Text(
            text = "DATA SAVER ACHIEVEMENTS",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = TextSecondary
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            items(badges) { badge ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (badge.isUnlocked) DarkSurface else DarkSurface.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (badge.isUnlocked) CyberCyan.copy(alpha = 0.3f) else DarkCardBorder
                    ),
                    modifier = Modifier.width(140.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = badge.icon, fontSize = 28.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = badge.title,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (badge.isUnlocked) TextPrimary else TextMuted
                            )
                        )
                        Text(
                            text = badge.subtitle,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                color = TextMuted
                            )
                        )
                    }
                }
            }
        }

        // Telecom Data Bundle Store Title & Telco Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "NIGERIAN TELCO DATA BUNDLE STORE",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
            )
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { viewModel.fetchPairgateDataPlans(forceRefresh = true) }
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isFetchingDataPlans) {
                    FlowButtonLoadingLine(color = CyberCyan, width = 20.dp, height = 2.dp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Syncing...", fontSize = 11.sp, color = CyberCyan, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = "Sync", modifier = Modifier.size(12.dp), tint = CyberCyan)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Sync Live Plans", fontSize = 11.sp, color = CyberCyan)
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("All", "MTN", "Airtel", "Glo", "9mobile").forEach { provider ->
                val isSelected = selectedProvider == provider
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedProvider = provider },
                    label = { Text(provider, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyberCyan.copy(alpha = 0.2f),
                        selectedLabelColor = CyberCyan,
                        containerColor = DarkSurface,
                        labelColor = TextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = DarkCardBorder,
                        selectedBorderColor = CyberCyan
                    )
                )
            }
        }

        // Telecom Bundle Cards List
        if (isFetchingDataPlans) {
            ReflectiveBundleListSkeleton(
                count = 3,
                statusText = "Syncing live bundles for $selectedProvider...",
                subText = "Connecting with upstream carrier network for latest packages",
                onRetrySync = { viewModel.fetchPairgateDataPlans(forceRefresh = true) }
            )
        } else if (filteredBundles.isNotEmpty()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
            filteredBundles.forEach { bundle ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (bundle.isHot) CyberCyan.copy(alpha = 0.5f) else DarkCardBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bundle_card_${bundle.id}")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = when (bundle.network.uppercase()) {
                                        "MTN" -> Color(0xFFFFCC00).copy(alpha = 0.2f)
                                        "AIRTEL" -> Color(0xFFFF0000).copy(alpha = 0.2f)
                                        "GLO" -> Color(0xFF00FF00).copy(alpha = 0.2f)
                                        else -> CyberCyan.copy(alpha = 0.2f)
                                    }
                                ) {
                                    Text(
                                        text = bundle.network,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Black,
                                            color = when (bundle.network.uppercase()) {
                                                "MTN" -> Color(0xFFFFCC00)
                                                "AIRTEL" -> Color(0xFFFF4444)
                                                "GLO" -> Color(0xFF00E676)
                                                else -> CyberCyan
                                            }
                                        ),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = bundle.name,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                )
                            }

                            if (bundle.isHot) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = GlowingAmber.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "HOT 🔥",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = GlowingAmber
                                        ),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "₦${bundle.price.toInt()}",
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        color = CyberCyan
                                    )
                                )
                                Text(
                                    text = "${bundle.validityText} • ${bundle.category}",
                                    style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                                )
                            }

                            Button(
                                onClick = { selectedBundleForBuy = bundle },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("buy_bundle_button_${bundle.id}")
                            ) {
                                Text(text = "Buy Plan", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
        } else {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = DarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.WifiOff, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("No Packages Found for $selectedProvider", style = MaterialTheme.typography.titleSmall.copy(color = TextPrimary, fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Tap below to sync verified packages directly from upstream carrier network.", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, textAlign = TextAlign.Center))
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.fetchPairgateDataPlans(forceRefresh = true) },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.White),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sync Live Plans", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }

    // VTU Purchase Dialog
    selectedBundleForBuy?.let { bundle ->
        AlertDialog(
            onDismissRequest = { if (!isPurchasing) selectedBundleForBuy = null },
            containerColor = DarkSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(GlowingAmber.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⚠️", fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Confirm Target Number", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(text = "Plan: ${bundle.name} (${bundle.network})", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    Text(text = "Payable Amount: ₦${bundle.price.toInt()}", color = CyberCyan, fontWeight = FontWeight.Bold)
                    
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Destination Number", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontWeight = FontWeight.Bold))
                        val myRealPhone = userVirtualAccount.phoneNumber.trim()
                        if (myRealPhone.isNotBlank() && myRealPhone != "Unassigned") {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = CyberCyan.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, CyberCyan),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { phoneNumberInput = myRealPhone }
                            ) {
                                Text(
                                    text = "Use My Default ($myRealPhone)",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = CyberCyan,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = GlowingAmber.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, GlowingAmber),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        newPhoneInput = ""
                                        showPhoneSetupDialog = true
                                    }
                            ) {
                                Text(
                                    text = "+ Set Up Default Number",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = GlowingAmber,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = phoneNumberInput,
                        onValueChange = { phoneNumberInput = it },
                        placeholder = { Text("080XXXXXXXX (or enter recipient number)", color = TextMuted) },
                        supportingText = {
                            Text("💡 Pre-filled with your default number. You can change this to buy for friends or other lines.", color = TextMuted, fontSize = 10.sp)
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF2D1810),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GlowingAmber.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = GlowingAmber, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Check carefully: ${bundle.network} data will be credited directly to $phoneNumberInput",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!com.example.data.util.NetworkUtils.isNetworkAvailable(context)) {
                            android.widget.Toast.makeText(context, "No network connection. Please check your mobile data or Wi-Fi.", android.widget.Toast.LENGTH_LONG).show()
                            purchaseResultMsg = "No network connection. Please check your mobile data or Wi-Fi to purchase data bundle."
                            return@Button
                        }
                        isPurchasing = true
                        viewModel.purchasePairgateData(
                            network = bundle.network,
                            planId = bundle.id,
                            amountNaira = bundle.price,
                            phone = phoneNumberInput,
                            category = bundle.category,
                            planName = bundle.rawName.ifBlank { bundle.name }
                        ) { success, msg, ref, isPending ->
                            isPurchasing = false
                            selectedBundleForBuy = null
                            if (success) {
                                purchaseReceipt = PurchaseSuccessReceipt(
                                    serviceTitle = "Data Bundle (${bundle.name})",
                                    providerOrType = bundle.network,
                                    recipient = phoneNumberInput,
                                    amountPaid = bundle.price,
                                    reference = ref ?: "PG-DAT-${(100000..999999).random()}",
                                    newBalance = viewModel.userWalletBalance.value,
                                    message = msg,
                                    bonusVpnTime = "+2 Hours Unlimited VPN Time",
                                    bonusPoints = 25,
                                    isPending = isPending,
                                    status = if (isPending) "PENDING" else "SUCCESS"
                                )
                            } else {
                                purchaseResultMsg = msg
                            }
                        }
                    },
                    enabled = !isPurchasing && phoneNumberInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian)
                ) {
                    if (isPurchasing) {
                        FlowButtonLoadingLine(color = DarkObsidian, width = 42.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Processing Top-Up...")
                    } else {
                        Text("Confirm & Pay ₦${bundle.price.toInt()}", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { selectedBundleForBuy = null },
                    enabled = !isPurchasing
                ) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }

    if (showPhoneSetupDialog) {
        PhoneVerificationDialog(
            viewModel = viewModel,
            initialPhone = newPhoneInput.ifBlank { userVirtualAccount.phoneNumber },
            onDismiss = { showPhoneSetupDialog = false },
            onSuccess = { verifiedPhone ->
                phoneNumberInput = verifiedPhone
                newPhoneInput = verifiedPhone
                showPhoneSetupDialog = false
            }
        )
    }

    // Success Receipt Drawer
    purchaseReceipt?.let { receipt ->
        val receiptSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        LaunchedEffect(receipt) {
            receiptSheetState.expand()
        }
        ModalBottomSheet(
            onDismissRequest = { purchaseReceipt = null },
            sheetState = receiptSheetState,
            containerColor = DarkObsidian,
            scrimColor = Color.Black.copy(alpha = 0.75f),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 10.dp)
                        .width(44.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.35f))
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 48.dp)
            ) {
                // Header with title and close button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Transaction Receipt",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    IconButton(
                        onClick = {
                            purchaseReceipt = null
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Receipt",
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                TransactionSuccessReceiptView(
                    receipt = receipt,
                    onReturnHome = {
                        purchaseReceipt = null
                    },
                    onMakeAnotherPurchase = {
                        purchaseReceipt = null
                    },
                    onSaveRecipient = { name, type, identifier, provider ->
                        viewModel.saveRecipient(
                            name = name,
                            recipientType = type,
                            identifier = identifier,
                            institutionOrProvider = provider
                        )
                    }
                )
            }
        }
    }

    // Error / Status Result Dialog
    purchaseResultMsg?.let { msg ->
        val clipboardManager = LocalClipboardManager.current
        val context = LocalContext.current
        AlertDialog(
            onDismissRequest = { purchaseResultMsg = null },
            containerColor = DarkSurface,
            icon = { Text(text = if (msg.contains("successful", ignoreCase = true) || msg.contains("Ref:", ignoreCase = true)) "🎉" else "⚠️", fontSize = 36.sp) },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Transaction Status", color = TextPrimary, fontWeight = FontWeight.Bold)
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(msg))
                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Error Message",
                            tint = CyberCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            text = {
                Text(text = msg, color = TextSecondary)
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(msg))
                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp), tint = CyberCyan)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy")
                    }
                    Button(
                        onClick = { purchaseResultMsg = null },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("OK")
                    }
                }
            }
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

    if (showOnboardingDialog) {
        OnboardingLoginDialog(
            viewModel = viewModel,
            onDismiss = { showOnboardingDialog = false }
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

    if (showSyncBalanceDialog) {
        val purchasedTotalMb by viewModel.purchasedDataTotalMb.collectAsStateWithLifecycle()
        com.example.ui.components.SyncCarrierBalanceDialog(
            currentRemainingMb = estimatedRemainingMb,
            currentPurchasedMb = purchasedTotalMb,
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

