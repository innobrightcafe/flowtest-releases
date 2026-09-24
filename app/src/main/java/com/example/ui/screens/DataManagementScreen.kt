package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.db.FirewallAppEntity
import com.example.data.ui.viewmodel.VpnViewModel
import com.example.data.util.NetworkUtils
import com.example.ui.components.DataUsageFilterSheet
import com.example.ui.components.FlowButtonLoadingLine
import com.example.ui.theme.*
import com.example.util.AppNotificationManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataManagementScreen(
    viewModel: VpnViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToServices: () -> Unit
) {
    val context = LocalContext.current
    val firewallApps by viewModel.firewallApps.collectAsStateWithLifecycle()
    val dataStats by viewModel.dataSaverStats.collectAsStateWithLifecycle()
    val vpnState by viewModel.vpnState.collectAsStateWithLifecycle()
    val purchasedTotalMb by viewModel.purchasedDataTotalMb.collectAsStateWithLifecycle()
    val estimatedRemainingMb by viewModel.estimatedDataBalanceMb.collectAsStateWithLifecycle()
    val isReminderEnabled by viewModel.isDataRenewalReminderEnabled.collectAsStateWithLifecycle()
    val dataReminderThresholdMb by viewModel.dataReminderThresholdMb.collectAsStateWithLifecycle()
    val smartSaverProfile by viewModel.smartSaverProfile.collectAsStateWithLifecycle()
    val isScanningApps by viewModel.isScanningDeviceApps.collectAsStateWithLifecycle()
    val activeUsageFilter by viewModel.activeUsageFilter.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("All") }
    var ultraSaverEnabled by remember { mutableStateOf(true) }
    var adShieldEnabled by remember { mutableStateOf(true) }
    var hotspotLimiterEnabled by remember { mutableStateOf(false) }
    var dailyQuotaGb by remember { mutableFloatStateOf(3.0f) }
    var showQuotaDialog by remember { mutableStateOf(false) }
    var showCalibrateDialog by remember { mutableStateOf(false) }
    var showUsageFilterSheet by remember { mutableStateOf(false) }
    var calibrateInputGb by remember { mutableStateOf("2.0") }
    var optimizationBannerText by remember { mutableStateOf<String?>(null) }

    // Auto sync and account for device apps and data traffic on screen launch
    LaunchedEffect(Unit) {
        viewModel.refreshPhoneDataAccounting(context)
    }

    // Strictly ensure apps are retained and displayed cleanly without package manager query drops
    val verifiedInstalledApps = remember(firewallApps) {
        if (firewallApps.isEmpty()) emptyList() else {
            val pm = context.packageManager
            firewallApps.filter { app ->
                try {
                    pm.getApplicationInfo(app.packageName, 0)
                    true
                } catch (e: Exception) {
                    true // Retain scanned applications from DB
                }
            }
        }
    }

    val totalUsedMb = remember(verifiedInstalledApps) {
        verifiedInstalledApps.sumOf { it.dataUsageMb }
    }
    val totalConsumedMb = remember(totalUsedMb) {
        if (totalUsedMb > 0) totalUsedMb else viewModel.getConsumedDataMb()
    }
    val totalConsumedGb = totalConsumedMb / 1024.0

    // Live remaining balance calculated under the hood (purchases add, apps & hotspot deduct)
    val remainingBalanceMb = estimatedRemainingMb.coerceAtLeast(0.0)
    val remainingBalanceGb = remainingBalanceMb / 1024.0
    val displayBalanceFormatted = if (remainingBalanceMb >= 1024.0) {
        val gb = remainingBalanceMb / 1024.0
        val text = String.format(java.util.Locale.US, "%.2f", gb).trimEnd('0').trimEnd('.')
        "$text GB"
    } else {
        "${Math.round(remainingBalanceMb).toInt()} MB"
    }

    val isNearReminder = isReminderEnabled && remainingBalanceMb <= dataReminderThresholdMb
    val balancePercent = if (purchasedTotalMb > 0.0) {
        ((remainingBalanceMb / purchasedTotalMb) * 100).toInt().coerceIn(0, 100)
    } else 0

    val filteredApps = remember(verifiedInstalledApps, searchQuery, selectedCategoryFilter) {
        verifiedInstalledApps.filter { app ->
            val matchesSearch = searchQuery.isBlank() || 
                app.appName.contains(searchQuery, ignoreCase = true) ||
                app.packageName.contains(searchQuery, ignoreCase = true)
            val matchesCat = when (selectedCategoryFilter) {
                "All" -> true
                "Blocked" -> app.isBlocked || app.isBackgroundFrozen
                "High Usage" -> app.dataUsageMb > 150.0
                "Social" -> app.category.equals("Social", ignoreCase = true)
                "Video" -> app.category.equals("Video", ignoreCase = true)
                "System" -> app.category.equals("System", ignoreCase = true)
                else -> true
            }
            matchesSearch && matchesCat
        }.sortedByDescending { it.dataUsageMb }
    }

    val dataSavedMb by viewModel.effectiveDataSavedMb.collectAsStateWithLifecycle()
    val hasUsageAccess by viewModel.hasUsageAccess.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "DATA SAVER & MANAGEMENT",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "Bandwidth Optimization • Per-App Firewall",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = CyberCyan,
                                fontSize = 11.sp
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            AppNotificationManager.showDataAlertNotification(
                                context = context,
                                title = "📊 Data Management Report",
                                message = "Saved ${(dataSavedMb / 1024.0).let { String.format("%.2f GB", it) }} bandwidth today. Firewall is active across ${firewallApps.count { it.isBlocked }} restricted apps."
                            )
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Send Alert",
                            tint = ElectricEmerald
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        containerColor = DarkObsidian
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Usage Access Permission Banner (if not yet granted by user)
            if (!hasUsageAccess) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .border(1.5.dp, Color(0xFFFFB74D), RoundedCornerShape(18.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF231A0B))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFFB74D).copy(alpha = 0.18f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.BarChart,
                                        contentDescription = null,
                                        tint = Color(0xFFFFB74D),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "USAGE ACCESS",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFFB74D),
                                            letterSpacing = 0.5.sp
                                        )
                                    )
                                    Text(
                                        text = "Enable usage access to view app data consumption.",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = TextPrimary,
                                            fontSize = 12.sp
                                        )
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    NetworkUtils.openUsageAccessSettings(context)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFFB74D),
                                    contentColor = DarkObsidian
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = "Enable Access",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // 1. Live Data Balance & Phone Accounting Hero Meter
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .border(1.dp, CyberCyan, RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        // Header Row with Balance Health Status
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
                                        imageVector = Icons.Default.DataUsage,
                                        contentDescription = "Data Balance",
                                        tint = CyberCyan,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "ESTIMATED DATA BALANCE",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = TextSecondary,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp
                                        )
                                    )
                                    Text(
                                        text = if (purchasedTotalMb <= 0.0) "0 MB (No Plan Active)" else "$displayBalanceFormatted Balance",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Black,
                                            color = if (isNearReminder) Color(0xFFFF5252) else if (purchasedTotalMb <= 0.0) TextMuted else CyberCyan
                                        )
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = CyberCyan.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    CyberCyan.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier.clickable { showCalibrateDialog = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = "Calibrate Balance",
                                        tint = CyberCyan,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "CALIBRATE",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = CyberCyan,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Progress Bar showing remaining health
                        val progressFraction = (balancePercent / 100f).coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (isNearReminder) Color(0xFFFF5252) else CyberCyan,
                            trackColor = DarkSurface
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Accounting Metrics: Consumed (Apps & Hotspot) vs Saved vs Reminder Threshold
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Device Consumed across all apps & hotspot tethering
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                color = DarkSurface,
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, DarkCardBorder)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("USED TODAY", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold))
                                    Spacer(modifier = Modifier.height(2.dp))
                                    val consumedDisplay = if (totalConsumedGb >= 1.0) String.format("%.2f GB", totalConsumedGb) else "${totalConsumedMb.toInt()} MB"
                                    Text(
                                        text = consumedDisplay,
                                        style = MaterialTheme.typography.titleMedium.copy(color = if (isNearReminder) Color(0xFFFF5252) else Color(0xFFFFAB00), fontWeight = FontWeight.Black)
                                    )
                                    Text("${verifiedInstalledApps.size} Apps & Hotspot", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 10.sp))
                                }
                            }

                            // Saved by Smart Saver
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                color = DarkSurface,
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, DarkCardBorder)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("DATA SAVED", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold))
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = String.format("%.0f MB", dataSavedMb),
                                        style = MaterialTheme.typography.titleMedium.copy(color = ElectricEmerald, fontWeight = FontWeight.Black)
                                    )
                                    Text("Smart Shield", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 10.sp))
                                }
                            }

                            // Reminder Alert volume
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { showQuotaDialog = true },
                                shape = RoundedCornerShape(14.dp),
                                color = DarkSurface,
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, DarkCardBorder)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("REMINDER AT", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold))
                                    Spacer(modifier = Modifier.height(2.dp))
                                    val reminderDisplay = if (dataReminderThresholdMb >= 1024.0) String.format("%.1f GB", dataReminderThresholdMb / 1024.0) else "${dataReminderThresholdMb.toInt()} MB"
                                    Text(
                                        text = reminderDisplay,
                                        style = MaterialTheme.typography.titleMedium.copy(color = if (isReminderEnabled) CyberCyan else TextMuted, fontWeight = FontWeight.Black)
                                    )
                                    Text(if (isReminderEnabled) "Notification ON" else "Set Alert", style = MaterialTheme.typography.labelSmall.copy(color = if (isReminderEnabled) CyberCyan else TextSecondary, fontSize = 10.sp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Actions Row: SMART OPTIMIZE, READJUST USAGE, SET PLAN
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.applySmartOptimization(context) { savedMb ->
                                        optimizationBannerText = "⚡ Smart Optimization applied! Saved ~${savedMb.toInt()} MB of background drain."
                                    }
                                },
                                modifier = Modifier.weight(1.1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CyberCyan,
                                    contentColor = DarkObsidian
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = "Optimize",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("OPTIMIZE", fontWeight = FontWeight.Black, fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    viewModel.readjustAppUsageCycle(context)
                                    optimizationBannerText = "🔄 Usage counter readjusted to 0 MB! Active balance preserved at $displayBalanceFormatted."
                                },
                                modifier = Modifier.weight(1.2f),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, GlowingAmber.copy(alpha = 0.7f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = GlowingAmber),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RestartAlt,
                                    contentDescription = "Readjust Usage",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("READJUST", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = { showCalibrateDialog = true },
                                modifier = Modifier.weight(1.1f),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.6f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Calibrate",
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("SET PLAN", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Optional Optimization Feedback Banner
            if (optimizationBannerText != null) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = ElectricEmerald.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ElectricEmerald)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = optimizationBannerText ?: "",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { optimizationBannerText = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Intelligent Data Optimization Controls & Smart Saver Modes
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.dp, DarkCardBorder, RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SMART DATA SAVER PROFILES",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = CyberCyan.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = smartSaverProfile,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = CyberCyan,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 10.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Smart Profile Selection Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val profiles = listOf(
                                "SMART_BALANCED" to "Smart Balanced",
                                "AGGRESSIVE" to "Ultra Saver",
                                "OFF" to "Unrestricted"
                            )
                            profiles.forEach { (modeKey, label) ->
                                val isSelected = smartSaverProfile == modeKey
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { viewModel.setSmartSaverProfile(modeKey, context) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) CyberCyan else DarkSurface,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) CyberCyan else DarkCardBorder
                                    )
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = if (isSelected) DarkObsidian else TextSecondary,
                                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                                fontSize = 11.sp
                                            ),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Quick Batch Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.toggleCategoryFirewall("Social", true) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(0.8.dp, DarkCardBorder),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                contentPadding = PaddingValues(vertical = 6.dp, horizontal = 4.dp)
                            ) {
                                Text("❄️ Freeze Social", fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }

                            OutlinedButton(
                                onClick = { viewModel.toggleCategoryFirewall("Video", true) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(0.8.dp, DarkCardBorder),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                contentPadding = PaddingValues(vertical = 6.dp, horizontal = 4.dp)
                            ) {
                                Text("🎬 Freeze Video", fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }

                            OutlinedButton(
                                onClick = { viewModel.toggleAllFirewallApps(false) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(0.8.dp, DarkCardBorder),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricEmerald),
                                contentPadding = PaddingValues(vertical = 6.dp, horizontal = 4.dp)
                            ) {
                                Text("🟢 Unfreeze All", fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Ultra Saver Mode Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Ultra Data Compression", style = MaterialTheme.typography.titleSmall.copy(color = TextPrimary, fontWeight = FontWeight.Bold))
                                Text("Compresses media streams & saves up to 40% on browsing", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp))
                            }
                            Switch(
                                checked = ultraSaverEnabled,
                                onCheckedChange = { 
                                    ultraSaverEnabled = it 
                                    if (it) viewModel.setSmartSaverProfile("SMART_BALANCED", context)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = DarkObsidian,
                                    checkedTrackColor = CyberCyan
                                )
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = DarkCardBorder)

                        // Ad & Script Shield Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("DNS Ad & Telemetry Shield", style = MaterialTheme.typography.titleSmall.copy(color = TextPrimary, fontWeight = FontWeight.Bold))
                                Text("Blocks video ads and background tracking scripts", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp))
                            }
                            Switch(
                                checked = adShieldEnabled,
                                onCheckedChange = { adShieldEnabled = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = DarkObsidian,
                                    checkedTrackColor = ElectricEmerald
                                )
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = DarkCardBorder)

                        // Hotspot Tethering Limiter
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Hotspot Tethering Guard", style = MaterialTheme.typography.titleSmall.copy(color = TextPrimary, fontWeight = FontWeight.Bold))
                                Text("Stops connected PCs or phones from downloading heavy updates", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp))
                            }
                            Switch(
                                checked = hotspotLimiterEnabled,
                                onCheckedChange = { hotspotLimiterEnabled = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = DarkObsidian,
                                    checkedTrackColor = Color(0xFFFFAB00)
                                )
                            )
                        }
                    }
                }
            }

            // 3. Wholesale SME Data Top-Up Banner (Instant Upsell)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.dp, CyberCyan, RoundedCornerShape(18.dp))
                        .clickable { onNavigateToServices() },
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Text("⚡", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Buy Cheap SME Data Bundles",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Black
                                    )
                                )
                                Text(
                                    text = "MTN, Airtel, Glo from ₦230/GB • Instant Auto-Delivery",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = CyberCyan,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }

                        Button(
                            onClick = onNavigateToServices,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberCyan,
                                contentColor = DarkObsidian
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("BUY DATA", fontWeight = FontWeight.Black, fontSize = 11.sp)
                        }
                    }
                }
            }

            // 4. Detailed Data Maximization & Benefit Guide Matrix
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.TipsAndUpdates,
                                contentDescription = null,
                                tint = GlowingAmber,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "HOW TO MAXIMIZE YOUR DATA & EARN BENEFITS",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = DarkSurface,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text("⚡", fontSize = 18.sp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Save up to 85% Mobile Bandwidth",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = ElectricEmerald
                                            )
                                        )
                                        Text(
                                            text = "Our active WireGuard tunnel compresses images to WebP and transcodes video feeds to optimal bitrates, extending your data life significantly.",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = TextSecondary,
                                                fontSize = 11.sp,
                                                lineHeight = 15.sp
                                            )
                                        )
                                    }
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = DarkSurface,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text("🛡️", fontSize = 18.sp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Real-Time Firewall & Ad Shield",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = CyberCyan
                                            )
                                        )
                                        Text(
                                            text = "Block intrusive background trackers, autoplaying ads, and stealth background app syncing to save 400MB - 1GB daily.",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = TextSecondary,
                                                fontSize = 11.sp,
                                                lineHeight = 15.sp
                                            )
                                        )
                                    }
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = DarkSurface,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text("💰", fontSize = 18.sp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Earn Cashback & Rewards on Every Top-Up",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = GlowingAmber
                                            )
                                        )
                                        Text(
                                            text = "Every SME data bundle or airtime purchase generates instant cashback into your reward wallet and awards bonus points redeemable for unlimited VPN time.",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = TextSecondary,
                                                fontSize = 11.sp,
                                                lineHeight = 15.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. Per-App Firewall Manager Header & Filters
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "REAL-TIME PER-APP DATA ACCOUNTING",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextMuted,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            )
                            Text(
                                text = "${filteredApps.size} Installed Applications Tracked",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = CyberCyan,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        // Rescan Button
                        OutlinedButton(
                            onClick = { viewModel.refreshPhoneDataAccounting(context) },
                            enabled = !isScanningApps,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.6f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            if (isScanningApps) {
                                FlowButtonLoadingLine(color = CyberCyan, width = 22.dp, height = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Scanning...", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Rescan Apps",
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Sync Apps", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Active Measurement Filter Bar & Drawer Action
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = DarkSurface,
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, CyberCyan.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.DateRange,
                                            contentDescription = "Usage Filter",
                                            tint = CyberCyan,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "USAGE PERIOD: ${activeUsageFilter.period.label.uppercase()}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = CyberCyan,
                                                fontWeight = FontWeight.Black,
                                                letterSpacing = 0.5.sp,
                                                fontSize = 11.sp
                                            )
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${activeUsageFilter.getFormattedWindow()} • ${activeUsageFilter.networkType.label}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = CyberCyan.copy(alpha = 0.15f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan),
                                    modifier = Modifier.clickable { showUsageFilterSheet = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Tune,
                                            contentDescription = "Open Filter Drawer",
                                            tint = CyberCyan,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Filter Drawer",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = CyberCyan,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 11.sp
                                            )
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Quick Period Toggle Chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val quickPeriods = listOf(
                                    com.example.data.model.UsagePeriod.TODAY to "Daily",
                                    com.example.data.model.UsagePeriod.LAST_7_DAYS to "Weekly",
                                    com.example.data.model.UsagePeriod.LAST_30_DAYS to "Monthly",
                                    com.example.data.model.UsagePeriod.CUSTOM to "Custom"
                                )
                                quickPeriods.forEach { (period, label) ->
                                    val isSelected = activeUsageFilter.period == period
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) CyberCyan.copy(alpha = 0.25f) else DarkSurfaceElevated,
                                        border = androidx.compose.foundation.BorderStroke(
                                            if (isSelected) 1.dp else 0.5.dp,
                                            if (isSelected) CyberCyan else DarkCardBorder
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                if (period == com.example.data.model.UsagePeriod.CUSTOM) {
                                                    showUsageFilterSheet = true
                                                } else {
                                                    viewModel.setUsageFilter(
                                                        activeUsageFilter.copy(period = period),
                                                        saveAsDefault = false
                                                    )
                                                }
                                            }
                                    ) {
                                        Text(
                                            text = label,
                                            modifier = Modifier.padding(vertical = 6.dp),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                                color = if (isSelected) CyberCyan else TextMuted,
                                                fontSize = 10.5.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Search input
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search installed apps...", color = TextMuted, fontSize = 13.sp) },
                        leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = TextMuted) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = TextMuted)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = DarkSurface,
                            unfocusedContainerColor = DarkSurface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Sliding Tab Indicator for categories
                    val categories = listOf("All", "High Usage", "Blocked", "System")
                    val selectedCatIndex = categories.indexOf(selectedCategoryFilter).coerceAtLeast(0)
                    ScrollableTabRow(
                        selectedTabIndex = selectedCatIndex,
                        containerColor = DarkSurface,
                        contentColor = CyberCyan,
                        edgePadding = 4.dp,
                        indicator = { tabPositions ->
                            if (selectedCatIndex in tabPositions.indices) {
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedCatIndex]),
                                    height = 3.dp,
                                    color = CyberCyan
                                )
                            }
                        },
                        divider = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                    ) {
                        categories.forEachIndexed { index, cat ->
                            val isSel = selectedCategoryFilter == cat
                            Tab(
                                selected = isSel,
                                onClick = { selectedCategoryFilter = cat },
                                text = {
                                    Text(
                                        text = cat,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSel) CyberCyan else TextSecondary
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // 5. App Firewall Items
            items(filteredApps, key = { it.packageName }) { app ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(
                            0.5.dp,
                            if (app.isBlocked) Color.Red.copy(alpha = 0.4f) else DarkCardBorder,
                            RoundedCornerShape(14.dp)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (app.isBlocked) Color(0xFF221115) else DarkSurfaceElevated
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (app.category.lowercase()) {
                                            "social" -> Color(0xFF1E88E5).copy(alpha = 0.2f)
                                            "streaming" -> Color(0xFFE53935).copy(alpha = 0.2f)
                                            "gaming" -> Color(0xFF8E24AA).copy(alpha = 0.2f)
                                            else -> CyberCyan.copy(alpha = 0.2f)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = app.appName.take(1).uppercase(),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        color = TextPrimary
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Text(
                                    text = app.appName,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                )
                                val formattedDataUsage = when {
                                    app.dataUsageMb >= 1024.0 -> String.format(java.util.Locale.US, "%.2f GB", app.dataUsageMb / 1024.0)
                                    app.dataUsageMb >= 1.0 -> String.format(java.util.Locale.US, "%.1f MB", app.dataUsageMb)
                                    app.dataUsageMb > 0.0 -> String.format(java.util.Locale.US, "%.0f KB", app.dataUsageMb * 1024.0)
                                    else -> "0.0 MB"
                                }
                                Text(
                                    text = "${app.category} • $formattedDataUsage consumed",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (app.dataUsageMb > 200.0) Color(0xFFFFAB00) else TextSecondary,
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (app.isBlocked) Color.Red.copy(alpha = 0.2f) else ElectricEmerald.copy(alpha = 0.15f),
                                modifier = Modifier.clickable {
                                    viewModel.toggleFirewallApp(app.packageName, !app.isBlocked)
                                    if (!app.isBlocked) {
                                        AppNotificationManager.showDataAlertNotification(
                                            context = context,
                                            title = "❄️ App Frozen in Background",
                                            message = "${app.appName} background data has been restricted by Firewall."
                                        )
                                    }
                                }
                            ) {
                                Text(
                                    text = if (app.isBlocked) "RESTRICTED" else "ACTIVE",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (app.isBlocked) Color(0xFFFF5252) else ElectricEmerald,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 10.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Switch(
                                checked = !app.isBlocked,
                                onCheckedChange = { allowAccess ->
                                    viewModel.toggleFirewallApp(app.packageName, !allowAccess)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = DarkObsidian,
                                    checkedTrackColor = ElectricEmerald,
                                    uncheckedThumbColor = TextMuted,
                                    uncheckedTrackColor = DarkSurface
                                ),
                                modifier = Modifier.scale(0.8f)
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    if (showQuotaDialog) {
        AlertDialog(
            onDismissRequest = { showQuotaDialog = false },
            containerColor = DarkSurface,
            title = {
                Text(
                    text = "Configure Daily Data Limit",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "Set maximum daily mobile data usage threshold. The app will notify you when 85% of your quota is reached.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Target Limit: ${String.format("%.1f GB", dailyQuotaGb)}",
                        style = MaterialTheme.typography.titleMedium.copy(color = CyberCyan, fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = dailyQuotaGb,
                        onValueChange = { dailyQuotaGb = it },
                        valueRange = 0.5f..10.0f,
                        steps = 18,
                        colors = SliderDefaults.colors(
                            thumbColor = CyberCyan,
                            activeTrackColor = CyberCyan,
                            inactiveTrackColor = DarkCardBorder
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showQuotaDialog = false
                        AppNotificationManager.showDataAlertNotification(
                            context = context,
                            title = "🎯 Daily Data Limit Configured",
                            message = "Daily data limit updated to ${String.format("%.1f GB", dailyQuotaGb)}. Smart tracking active."
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian)
                ) {
                    Text("Save Limit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuotaDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }

    if (showCalibrateDialog) {
        com.example.ui.components.SyncCarrierBalanceDialog(
            currentRemainingMb = estimatedRemainingMb,
            currentPurchasedMb = purchasedTotalMb,
            onDismiss = { showCalibrateDialog = false },
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
                showCalibrateDialog = false
            }
        )
    }

    if (showUsageFilterSheet) {
        DataUsageFilterSheet(
            currentFilter = activeUsageFilter,
            onDismiss = { showUsageFilterSheet = false },
            onApplyFilter = { newFilter, saveDefault ->
                viewModel.setUsageFilter(newFilter, saveAsDefault = saveDefault)
            }
        )
    }
}
