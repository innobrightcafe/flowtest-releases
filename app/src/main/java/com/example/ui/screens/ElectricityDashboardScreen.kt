package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ElectricityAppliance
import com.example.data.model.ElectricityMeterConfig
import com.example.data.ui.viewmodel.VpnViewModel
import com.example.ui.components.ElectricitySheet
import com.example.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ElectricityDashboardScreen(
    viewModel: VpnViewModel,
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val meterConfig by viewModel.electricityMeterConfig.collectAsStateWithLifecycle()
    val appliances by viewModel.electricityAppliances.collectAsStateWithLifecycle()
    val walletBalance by viewModel.userWalletBalance.collectAsStateWithLifecycle()

    var activeCategoryFilter by remember { mutableStateOf("All") }
    var showBuyTokenSheet by remember { mutableStateOf(false) }
    var showCalibrateDialog by remember { mutableStateOf(false) }
    var showAlertSettingsDialog by remember { mutableStateOf(false) }
    var showEditMeterDialog by remember { mutableStateOf(false) }
    var showAddApplianceDialog by remember { mutableStateOf(false) }

    // Aggregate Calculations
    val activeAppliances = remember(appliances) { appliances.filter { it.isEnabled } }
    val totalDailyBurnKwh = remember(activeAppliances) { activeAppliances.sumOf { it.dailyKwh } }
    val totalConnectedWattage = remember(activeAppliances) { activeAppliances.sumOf { it.wattage * it.quantity } }
    val remainingKwh = remember(meterConfig, totalDailyBurnKwh) { meterConfig.getEstimatedRemainingKwh(totalDailyBurnKwh) }
    val daysRemaining = remember(meterConfig, totalDailyBurnKwh) { meterConfig.getEstimatedDaysRemaining(totalDailyBurnKwh) }
    val hoursRemaining = remember(meterConfig, totalDailyBurnKwh) { meterConfig.getEstimatedHoursRemaining(totalDailyBurnKwh) }
    val estValueNaira = remember(meterConfig, totalDailyBurnKwh) { meterConfig.getEstimatedValueNaira(totalDailyBurnKwh) }
    val dailyCostNaira = remember(totalDailyBurnKwh, meterConfig.tariffRatePerKwh) { totalDailyBurnKwh * meterConfig.tariffRatePerKwh }
    val monthlyCostNaira = remember(dailyCostNaira) { dailyCostNaira * 30.0 }

    val isLowBalance = remainingKwh <= meterConfig.lowBalanceAlertThresholdKwh && meterConfig.isLowBalanceAlertEnabled

    val categories = listOf("All", "Cooling", "Refrigeration", "Entertainment", "Lighting", "Kitchen", "Pumping", "Other")

    val filteredAppliances = remember(appliances, activeCategoryFilter) {
        if (activeCategoryFilter == "All") appliances
        else appliances.filter { it.category.equals(activeCategoryFilter, ignoreCase = true) }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkObsidian,
        topBar = {
            Surface(
                color = DarkSurface,
                border = BorderStroke(1.dp, DarkCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Electricity Dashboard",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        color = TextPrimary,
                                        fontSize = 17.sp
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isLowBalance) GlowingAmber else ElectricEmerald)
                                )
                            }
                            Text(
                                text = "Smart Meter & Energy Profiler",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    // Live Wallet Balance pill
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = DarkSurfaceElevated,
                        border = BorderStroke(1.dp, DarkCardBorder),
                        modifier = Modifier.clickable { showBuyTokenSheet = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = "Top Up",
                                tint = GlowingAmber,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "₦%,.0f".format(Locale.US, walletBalance),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = CyberCyan,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 40.dp)
        ) {
            // 1. HERO ESTIMATED METER BALANCE CARD (UI/UX PRO MAX)
            item {
                HeroMeterBalanceCard(
                    remainingKwh = remainingKwh,
                    daysRemaining = daysRemaining,
                    hoursRemaining = hoursRemaining,
                    estValueNaira = estValueNaira,
                    dailyBurnKwh = totalDailyBurnKwh,
                    dailyCostNaira = dailyCostNaira,
                    isLowBalance = isLowBalance,
                    lowThresholdKwh = meterConfig.lowBalanceAlertThresholdKwh,
                    onBuyTokenClick = { showBuyTokenSheet = true },
                    onCalibrateClick = { showCalibrateDialog = true },
                    onAlertSettingsClick = { showAlertSettingsDialog = true }
                )
            }

            // 2. METER & DISCO INFO STRIP
            item {
                MeterInfoStrip(
                    meterConfig = meterConfig,
                    onEditClick = { showEditMeterDialog = true },
                    onCopyMeterNumber = {
                        if (meterConfig.meterNumber.isNotBlank()) {
                            clipboardManager.setText(AnnotatedString(meterConfig.meterNumber))
                            Toast.makeText(context, "Meter number copied: ${meterConfig.meterNumber}", Toast.LENGTH_SHORT).show()
                        } else {
                            showEditMeterDialog = true
                        }
                    }
                )
            }

            // 3. APPLIANCES ENERGY PROFILER SECTION
            item {
                ApplianceProfilerHeader(
                    totalConnectedWattage = totalConnectedWattage,
                    totalDailyBurnKwh = totalDailyBurnKwh,
                    monthlyCostNaira = monthlyCostNaira,
                    onAddApplianceClick = { showAddApplianceDialog = true }
                )
            }

            // Category Filter Chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { category ->
                        val isSelected = activeCategoryFilter.equals(category, ignoreCase = true)
                        val count = if (category == "All") appliances.size
                        else appliances.count { it.category.equals(category, ignoreCase = true) }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) CyberCyan.copy(alpha = 0.15f) else DarkSurface,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) CyberCyan else DarkCardBorder
                            ),
                            modifier = Modifier.clickable { activeCategoryFilter = category }
                        ) {
                            Text(
                                text = "$category ($count)",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isSelected) CyberCyan else TextSecondary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }
            }

            // 4. APPLIANCE LIST ITEMS
            items(filteredAppliances, key = { it.id }) { appliance ->
                ApplianceCardItem(
                    appliance = appliance,
                    tariffRate = meterConfig.tariffRatePerKwh,
                    onToggle = { isEnabled ->
                        viewModel.toggleElectricityAppliance(appliance.id, isEnabled)
                    },
                    onHoursChange = { hours ->
                        viewModel.updateElectricityApplianceHours(appliance.id, hours)
                    },
                    onQuantityChange = { qty ->
                        viewModel.updateElectricityApplianceQuantity(appliance.id, qty)
                    },
                    onDelete = {
                        viewModel.deleteElectricityAppliance(appliance.id)
                        Toast.makeText(context, "${appliance.name} removed", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // 5. "WHAT-IF" ENERGY SAVINGS & OPTIMIZATION INSIGHTS
            item {
                EnergyInsightsCard(
                    appliances = activeAppliances,
                    tariffRate = meterConfig.tariffRatePerKwh,
                    totalDailyBurnKwh = totalDailyBurnKwh,
                    onResetDefaults = {
                        viewModel.resetElectricityAppliancesToDefault()
                        Toast.makeText(context, "Reset appliances to standard presets", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    // --- DIALOGS ---

    if (showBuyTokenSheet) {
        ElectricitySheet(
            viewModel = viewModel,
            onDismiss = { showBuyTokenSheet = false }
        )
    }

    if (showCalibrateDialog) {
        CalibrateMeterDialog(
            currentConfig = meterConfig,
            totalDailyBurnKwh = totalDailyBurnKwh,
            onDismiss = { showCalibrateDialog = false },
            onConfirm = { exactKwh ->
                viewModel.calibrateElectricityMeter(exactKwh)
                showCalibrateDialog = false
                Toast.makeText(context, "Meter calibrated to ${String.format(Locale.US, "%.1f", exactKwh)} kWh", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showAlertSettingsDialog) {
        AlertSettingsDialog(
            currentThreshold = meterConfig.lowBalanceAlertThresholdKwh,
            isEnabled = meterConfig.isLowBalanceAlertEnabled,
            onDismiss = { showAlertSettingsDialog = false },
            onSave = { threshold, enabled ->
                viewModel.updateElectricityAlertSettings(threshold, enabled)
                showAlertSettingsDialog = false
                Toast.makeText(context, "Alert settings saved!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showEditMeterDialog) {
        EditMeterDetailsDialog(
            currentConfig = meterConfig,
            onDismiss = { showEditMeterDialog = false },
            onSave = { number, discoId, discoName, band, rate ->
                viewModel.updateElectricityMeterDetails(number, discoId, discoName, band, rate)
                showEditMeterDialog = false
                Toast.makeText(context, "Meter profile updated!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showAddApplianceDialog) {
        AddApplianceDialog(
            onDismiss = { showAddApplianceDialog = false },
            onAdd = { newAppliance ->
                viewModel.addElectricityAppliance(newAppliance)
                showAddApplianceDialog = false
                Toast.makeText(context, "${newAppliance.name} added!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

// ==========================================
// SUBCOMPONENTS: HERO & CARDS
// ==========================================

@Composable
private fun HeroMeterBalanceCard(
    remainingKwh: Double,
    daysRemaining: Double,
    hoursRemaining: Double,
    estValueNaira: Double,
    dailyBurnKwh: Double,
    dailyCostNaira: Double,
    isLowBalance: Boolean,
    lowThresholdKwh: Double,
    onBuyTokenClick: () -> Unit,
    onCalibrateClick: () -> Unit,
    onAlertSettingsClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(
            1.dp,
            if (isLowBalance) GlowingAmber.copy(alpha = 0.8f) else CyberCyan.copy(alpha = 0.35f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Row: Status badge & Alert chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isLowBalance) GlowingAmber.copy(alpha = 0.15f) else ElectricEmerald.copy(alpha = 0.15f),
                    border = BorderStroke(
                        1.dp,
                        if (isLowBalance) GlowingAmber else ElectricEmerald
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isLowBalance) Icons.Default.Warning else Icons.Default.ElectricBolt,
                            contentDescription = null,
                            tint = if (isLowBalance) GlowingAmber else ElectricEmerald,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isLowBalance) "LOW TOKEN ALERT" else "ESTIMATED RUNWAY",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isLowBalance) GlowingAmber else ElectricEmerald,
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp
                            )
                        )
                    }
                }

                // Threshold indicator chip
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurface,
                    border = BorderStroke(1.dp, DarkCardBorder),
                    modifier = Modifier.clickable { onAlertSettingsClick() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Alert config",
                            tint = CyberCyan,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Alert at ≤ ${String.format(Locale.US, "%.0f", lowThresholdKwh)} kWh",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Giant Remaining Balance Display
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = String.format(Locale.US, "%.1f", remainingKwh),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.Black,
                        color = if (isLowBalance) GlowingAmber else TextPrimary,
                        fontSize = 52.sp,
                        fontFamily = FontFamily.Monospace
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "kWh",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = CyberCyan,
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp
                    ),
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }

            // Runway estimate pill (e.g. ~9.5 Days Remaining / 228 Hours)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isLowBalance) GlowingAmber.copy(alpha = 0.2f) else CyberCyan.copy(alpha = 0.12f),
                border = BorderStroke(
                    1.dp,
                    if (isLowBalance) GlowingAmber.copy(alpha = 0.5f) else CyberCyan.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = if (isLowBalance) GlowingAmber else CyberCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (dailyBurnKwh <= 0.0) "No active appliances connected"
                        else if (daysRemaining >= 1.0) "~${String.format(Locale.US, "%.1f", daysRemaining)} Days Left (${String.format(Locale.US, "%.0f", hoursRemaining)}h)"
                        else "~${String.format(Locale.US, "%.0f", hoursRemaining)} Hours Left",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = if (isLowBalance) GlowingAmber else CyberCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sub-metrics: Est. Value in Naira + Daily Burn Rate
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface, RoundedCornerShape(14.dp))
                    .border(1.dp, DarkCardBorder, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ESTIMATED VALUE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Text(
                        text = "≈ ₦%,.0f".format(Locale.US, estValueNaira),
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .height(28.dp)
                        .width(1.dp)
                        .background(DarkCardBorder)
                )

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "DAILY BURN RATE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Text(
                        text = "${String.format(Locale.US, "%.1f", dailyBurnKwh)} kWh/d (₦%,.0f)".format(Locale.US, dailyCostNaira),
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = GlowingAmber,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    )
                }
            }

            if (isLowBalance) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF331400),
                    border = BorderStroke(1.dp, GlowingAmber),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = GlowingAmber,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Meter balance is below ${String.format(Locale.US, "%.0f", lowThresholdKwh)} kWh! Recharge now to avoid disconnection.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = GlowingAmber,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action CTAs: "Buy Token" primary + "Calibrate" secondary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onBuyTokenClick,
                    modifier = Modifier
                        .weight(1.4f)
                        .height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberCyan,
                        contentColor = DarkObsidian
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Buy Token",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                    )
                }

                OutlinedButton(
                    onClick = onCalibrateClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TextPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Calibrate",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun MeterInfoStrip(
    meterConfig: ElectricityMeterConfig,
    onEditClick: () -> Unit,
    onCopyMeterNumber: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, DarkCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceElevated)
                        .border(1.dp, DarkCardBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ElectricMeter,
                        contentDescription = null,
                        tint = GlowingAmber,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = meterConfig.discoName,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 13.sp
                        )
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onCopyMeterNumber() }
                    ) {
                        Text(
                            text = if (meterConfig.meterNumber.isNotBlank()) "Meter: ${meterConfig.meterNumber}" else "Tap to set meter number",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (meterConfig.meterNumber.isNotBlank()) CyberCyan else TextSecondary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                        if (meterConfig.meterNumber.isNotBlank()) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy",
                                tint = TextSecondary,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                    Text(
                        text = "${meterConfig.tariffBand} • ₦${String.format(Locale.US, "%.1f", meterConfig.tariffRatePerKwh)}/kWh",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    )
                }
            }

            IconButton(
                onClick = onEditClick,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceElevated)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Profile",
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun ApplianceProfilerHeader(
    totalConnectedWattage: Int,
    totalDailyBurnKwh: Double,
    monthlyCostNaira: Double,
    onAddApplianceClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "HOUSEHOLD APPLIANCES",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )
                )
                Text(
                    text = "Energy Consumption Profiler",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = TextPrimary,
                        fontSize = 16.sp
                    )
                )
            }

            Button(
                onClick = onAddApplianceClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DarkSurfaceElevated,
                    contentColor = CyberCyan
                ),
                border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f)),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Add Device",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Summary load strip
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = DarkSurface,
            border = BorderStroke(1.dp, DarkCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Power,
                        contentDescription = null,
                        tint = GlowingAmber,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Connected Load: ${"%,d".format(Locale.US, totalConnectedWattage)}W (${String.format(Locale.US, "%.2f", totalConnectedWattage / 1000.0)}kW)",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                Text(
                    text = "Est. ₦%,.0f/mo".format(Locale.US, monthlyCostNaira),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = CyberCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun ApplianceCardItem(
    appliance: ElectricityAppliance,
    tariffRate: Double,
    onToggle: (Boolean) -> Unit,
    onHoursChange: (Double) -> Unit,
    onQuantityChange: (Int) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (appliance.isEnabled) DarkSurface else DarkSurface.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.dp,
            if (appliance.isEnabled) DarkCardBorder else DarkCardBorder.copy(alpha = 0.4f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Top Row: Icon, Title & Wattage, Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (appliance.isEnabled) CyberCyan.copy(alpha = 0.12f)
                                else DarkSurfaceElevated
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getIconForAppliance(appliance.iconKey, appliance.category),
                            contentDescription = null,
                            tint = if (appliance.isEnabled) CyberCyan else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = appliance.name,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (appliance.isEnabled) TextPrimary else TextSecondary,
                                fontSize = 14.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = DarkSurfaceElevated
                            ) {
                                Text(
                                    text = "${appliance.wattage}W",
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = GlowingAmber,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = appliance.category,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (appliance.isCustom) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete",
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    Switch(
                        checked = appliance.isEnabled,
                        onCheckedChange = onToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyberCyan,
                            checkedTrackColor = CyberCyan.copy(alpha = 0.25f),
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = DarkSurfaceElevated
                        )
                    )
                }
            }

            // Controls & metrics if appliance is enabled
            AnimatedVisibility(visible = appliance.isEnabled) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    HorizontalDivider(color = DarkCardBorder.copy(alpha = 0.5f), thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Quantity Stepper + Live Calculation Pill
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Stepper for quantity
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Qty:",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = CircleShape,
                                color = DarkSurfaceElevated,
                                border = BorderStroke(1.dp, DarkCardBorder),
                                modifier = Modifier
                                    .size(24.dp)
                                    .clickable {
                                        if (appliance.quantity > 1) {
                                            onQuantityChange(appliance.quantity - 1)
                                        }
                                    },
                                content = {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("-", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${appliance.quantity}",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = CircleShape,
                                color = DarkSurfaceElevated,
                                border = BorderStroke(1.dp, DarkCardBorder),
                                modifier = Modifier
                                    .size(24.dp)
                                    .clickable {
                                        onQuantityChange(appliance.quantity + 1)
                                    },
                                content = {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("+", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            )
                        }

                        // Real-time daily burn pill
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = DarkSurfaceElevated,
                            border = BorderStroke(1.dp, DarkCardBorder)
                        ) {
                            Text(
                                text = "${String.format(Locale.US, "%.2f", appliance.dailyKwh)} kWh/d • ₦%,.0f/d".format(Locale.US, appliance.dailyCostNaira(tariffRate)),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = CyberCyan,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Daily Hours Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Daily usage: ${String.format(Locale.US, "%.1f", appliance.hoursPerDay)} hrs/day",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        )
                    }

                    Slider(
                        value = appliance.hoursPerDay.toFloat(),
                        onValueChange = { onHoursChange(it.toDouble()) },
                        valueRange = 0.5f..24f,
                        steps = 46, // roughly half-hour steps
                        colors = SliderDefaults.colors(
                            thumbColor = CyberCyan,
                            activeTrackColor = CyberCyan,
                            inactiveTrackColor = DarkSurfaceElevated
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(26.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EnergyInsightsCard(
    appliances: List<ElectricityAppliance>,
    tariffRate: Double,
    totalDailyBurnKwh: Double,
    onResetDefaults: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, DarkCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = GlowingAmber,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Energy Optimization & Tips",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 14.sp
                        )
                    )
                }

                Text(
                    text = "Reset Presets",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = CyberCyan,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp
                    ),
                    modifier = Modifier.clickable { onResetDefaults() }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Category breakdown bars
            val categorySums = appliances.groupBy { it.category }.mapValues { entry ->
                entry.value.sumOf { it.dailyKwh }
            }

            if (totalDailyBurnKwh > 0.0) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    categorySums.entries.sortedByDescending { it.value }.take(4).forEach { (category, kwh) ->
                        val pct = (kwh / totalDailyBurnKwh).coerceIn(0.0, 1.0)
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = category,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                )
                                Text(
                                    text = "${String.format(Locale.US, "%.1f", kwh)} kWh (${String.format(Locale.US, "%.0f", pct * 100)}%)",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            LinearProgressIndicator(
                                progress = { pct.toFloat() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = if (category.contains("Cool", ignoreCase = true)) CyberCyan
                                else if (category.contains("Refrig", ignoreCase = true)) ElectricEmerald
                                else GlowingAmber,
                                trackColor = DarkSurfaceElevated
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Smart Recommendation Bullet
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = DarkSurfaceElevated,
                border = BorderStroke(1.dp, DarkCardBorder)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = ElectricEmerald,
                        modifier = Modifier
                            .size(15.dp)
                            .padding(top = 1.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (totalDailyBurnKwh > 15.0) {
                            "Pro-Tip: Shifting heavy loads like water borehole pumping and pressing irons to off-peak hours can extend your meter token runway significantly!"
                        } else {
                            "Good job! Your energy footprint is lean. Consider setting an alert threshold at 15 kWh so you get timely recharge notifications."
                        },
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

// ==========================================
// DIALOGS: CALIBRATE, ALERTS, METER, ADD
// ==========================================

@Composable
private fun CalibrateMeterDialog(
    currentConfig: ElectricityMeterConfig,
    totalDailyBurnKwh: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var kwhInput by remember {
        mutableStateOf(currentConfig.formatRemainingBalance(totalDailyBurnKwh))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurfaceElevated,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Tune, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Calibrate Meter Keypad", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column {
                Text(
                    text = "Look at the screen on your physical prepaid meter keypad / CIU box. Enter the exact kWh units displayed right now to re-synchronize FlowTest's live burn tracker.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 12.sp)
                )
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = kwhInput,
                    onValueChange = { kwhInput = it },
                    label = { Text("Physical Meter kWh Reading") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = DarkCardBorder,
                        focusedLabelColor = CyberCyan
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val kwh = kwhInput.toDoubleOrNull() ?: 0.0
                    onConfirm(kwh)
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian)
            ) {
                Text("Save Reading", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun AlertSettingsDialog(
    currentThreshold: Double,
    isEnabled: Boolean,
    onDismiss: () -> Unit,
    onSave: (Double, Boolean) -> Unit
) {
    var thresholdInput by remember { mutableStateOf(String.format(Locale.US, "%.0f", currentThreshold)) }
    var alertsEnabled by remember { mutableStateOf(isEnabled) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurfaceElevated,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = GlowingAmber, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Low Balance Alert", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column {
                Text(
                    text = "Get automatic push alerts and notifications when your remaining electricity units reach a minimum critical balance.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 12.sp)
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enable Low Token Alerts", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Switch(
                        checked = alertsEnabled,
                        onCheckedChange = { alertsEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = CyberCyan, checkedTrackColor = CyberCyan.copy(alpha = 0.3f))
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = thresholdInput,
                    onValueChange = { thresholdInput = it },
                    label = { Text("Alert Threshold (kWh)") },
                    enabled = alertsEnabled,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = DarkCardBorder,
                        focusedLabelColor = CyberCyan
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Suggested: 15 kWh (~1 to 2 days of runway for typical homes)",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 10.sp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val threshold = thresholdInput.toDoubleOrNull() ?: 15.0
                    onSave(threshold, alertsEnabled)
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian)
            ) {
                Text("Save Settings", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun EditMeterDetailsDialog(
    currentConfig: ElectricityMeterConfig,
    onDismiss: () -> Unit,
    onSave: (meterNumber: String, discoId: String, discoName: String, tariffBand: String, tariffRate: Double) -> Unit
) {
    var meterNumber by remember { mutableStateOf(currentConfig.meterNumber) }
    var selectedDisco by remember { mutableStateOf(currentConfig.discoName) }
    var selectedBand by remember { mutableStateOf(currentConfig.tariffBand) }
    var tariffRateInput by remember { mutableStateOf(String.format(Locale.US, "%.2f", currentConfig.tariffRatePerKwh)) }

    val discos = listOf(
        "Ikeja Electric (IKEDC)",
        "Eko Electricity (EKEDC)",
        "Abuja Electricity (AEDC)",
        "Ibadan Electricity (IBEDC)",
        "Enugu Electricity (EEDC)",
        "Kano Electricity (KEDCO)",
        "Port Harcourt Electricity (PHED)",
        "Benin Electricity (BEDC)",
        "Kaduna Electric (KAEDCO)",
        "Jos Electricity (JED)"
    )

    val bands = listOf(
        "Band A (20+ hrs) - ₦209.50/kWh" to 209.50,
        "Band B (16-20 hrs) - ₦68.50/kWh" to 68.50,
        "Band C (12-16 hrs) - ₦52.00/kWh" to 52.00,
        "Custom Tariff Rate" to (currentConfig.tariffRatePerKwh)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurfaceElevated,
        title = {
            Text("Meter & Tariff Profile", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = meterNumber,
                    onValueChange = { meterNumber = it },
                    label = { Text("Prepaid Meter Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = DarkCardBorder
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))
                Text("DisCo Provider", color = TextSecondary, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(4.dp))

                var discoDropdownExpanded by remember { mutableStateOf(false) }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = DarkSurface,
                    border = BorderStroke(1.dp, DarkCardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { discoDropdownExpanded = true }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(selectedDisco, color = TextPrimary, fontSize = 13.sp)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextSecondary)
                    }
                    DropdownMenu(
                        expanded = discoDropdownExpanded,
                        onDismissRequest = { discoDropdownExpanded = false }
                    ) {
                        discos.forEach { disco ->
                            DropdownMenuItem(
                                text = { Text(disco) },
                                onClick = {
                                    selectedDisco = disco
                                    discoDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text("Tariff Band / Rate (₦/kWh)", color = TextSecondary, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(4.dp))

                bands.forEach { (label, rate) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedBand = label
                                tariffRateInput = String.format(Locale.US, "%.2f", rate)
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedBand == label,
                            onClick = {
                                selectedBand = label
                                tariffRateInput = String.format(Locale.US, "%.2f", rate)
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = CyberCyan)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(label, color = TextPrimary, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = tariffRateInput,
                    onValueChange = { tariffRateInput = it },
                    label = { Text("Tariff Rate (₦/kWh)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = DarkCardBorder
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val rate = tariffRateInput.toDoubleOrNull() ?: 209.50
                    val discoId = selectedDisco.lowercase().filter { it.isLetter() }
                    onSave(meterNumber.trim(), discoId, selectedDisco, selectedBand, rate)
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian)
            ) {
                Text("Save Profile", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun AddApplianceDialog(
    onDismiss: () -> Unit,
    onAdd: (ElectricityAppliance) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var wattageInput by remember { mutableStateOf("150") }
    var quantityInput by remember { mutableStateOf("1") }
    var hoursInput by remember { mutableStateOf("6.0") }
    var selectedCategory by remember { mutableStateOf("Cooling") }

    val categories = listOf("Cooling", "Refrigeration", "Entertainment", "Lighting", "Kitchen", "Pumping", "Other")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurfaceElevated,
        title = {
            Text("Add Household Device", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Device Name (e.g. Standing Fan)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = DarkCardBorder
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text("Category", color = TextSecondary, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(4.dp))

                var catExpanded by remember { mutableStateOf(false) }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = DarkSurface,
                    border = BorderStroke(1.dp, DarkCardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { catExpanded = true }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(selectedCategory, color = TextPrimary, fontSize = 13.sp)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextSecondary)
                    }
                    DropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    selectedCategory = cat
                                    catExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = wattageInput,
                    onValueChange = { wattageInput = it },
                    label = { Text("Wattage (Watts, e.g. 75)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = DarkCardBorder
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = quantityInput,
                        onValueChange = { quantityInput = it },
                        label = { Text("Qty") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = DarkCardBorder
                        )
                    )

                    OutlinedTextField(
                        value = hoursInput,
                        onValueChange = { hoursInput = it },
                        label = { Text("Hours/Day") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1.3f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = DarkCardBorder
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalName = if (name.isNotBlank()) name.trim() else "Custom Appliance"
                    val wattage = wattageInput.toIntOrNull() ?: 100
                    val qty = quantityInput.toIntOrNull() ?: 1
                    val hours = hoursInput.toDoubleOrNull() ?: 4.0
                    val iconKey = when {
                        finalName.contains("fan", true) -> "fan"
                        finalName.contains("ac", true) || finalName.contains("air", true) -> "ac"
                        finalName.contains("fridge", true) || finalName.contains("freezer", true) -> "fridge"
                        finalName.contains("tv", true) -> "tv"
                        finalName.contains("light", true) || finalName.contains("bulb", true) -> "bulb"
                        finalName.contains("pump", true) -> "pump"
                        finalName.contains("iron", true) -> "iron"
                        finalName.contains("cook", true) || finalName.contains("kettle", true) -> "kettle"
                        else -> "default"
                    }

                    onAdd(
                        ElectricityAppliance(
                            id = "app_custom_${System.currentTimeMillis()}",
                            name = finalName,
                            category = selectedCategory,
                            wattage = wattage,
                            quantity = qty,
                            hoursPerDay = hours,
                            isEnabled = true,
                            isCustom = true,
                            iconKey = iconKey
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian)
            ) {
                Text("Add Device", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

private fun getIconForAppliance(iconKey: String, category: String): ImageVector {
    return when (iconKey.lowercase()) {
        "ac" -> Icons.Default.AcUnit
        "fridge" -> Icons.Default.Kitchen
        "fan" -> Icons.Default.Cyclone
        "tv" -> Icons.Default.Tv
        "bulb" -> Icons.Default.Lightbulb
        "pump" -> Icons.Default.WaterDrop
        "kettle" -> Icons.Default.SoupKitchen
        "microwave" -> Icons.Default.Microwave
        "pc" -> Icons.Default.Computer
        "iron" -> Icons.Default.DryCleaning
        else -> when (category.lowercase()) {
            "cooling" -> Icons.Default.AcUnit
            "refrigeration" -> Icons.Default.Kitchen
            "entertainment" -> Icons.Default.Tv
            "lighting" -> Icons.Default.Lightbulb
            "pumping" -> Icons.Default.WaterDrop
            "kitchen" -> Icons.Default.Kitchen
            else -> Icons.Default.Power
        }
    }
}
