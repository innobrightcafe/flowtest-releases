package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.ui.viewmodel.VpnViewModel
import com.example.ui.theme.*
import java.util.Locale

data class VpnSubscriptionPlan(
    val id: String,
    val title: String,
    val minutes: Long,
    val priceNaira: Double,
    val wholesaleCostNaira: Double,
    val profitMarginPercent: Int,
    val maxDevices: Int,
    val description: String,
    val icon: ImageVector,
    val accentColor: Color,
    val isModelA: Boolean = true,
    val quotaBytes: Long? = null,
    val isPopular: Boolean = false,
    val badge: String? = null
)

// Model A: Time-Based Subscriptions (Unlimited Bandwidth)
val MODEL_A_TIME_PLANS = listOf(
    VpnSubscriptionPlan(
        id = "plan_1w",
        title = "1-Week Pass",
        minutes = 7 * 24 * 60L,
        priceNaira = 1500.0,
        wholesaleCostNaira = 600.0,
        profitMarginPercent = 60,
        maxDevices = 1,
        description = "Very popular for short-term tasks, exams & quick travel. 7 days unmetered.",
        icon = Icons.Default.Bolt,
        accentColor = CyberCyan,
        isModelA = true,
        badge = "Short-Term Pick"
    ),
    VpnSubscriptionPlan(
        id = "plan_1m_std",
        title = "1-Month Standard",
        minutes = 30 * 24 * 60L,
        priceNaira = 5000.0,
        wholesaleCostNaira = 2200.0,
        profitMarginPercent = 56,
        maxDevices = 2,
        description = "Complete privacy & WireGuard unthrottled streaming on 1-2 devices.",
        icon = Icons.Default.Security,
        accentColor = ElectricEmerald,
        isModelA = true,
        isPopular = true,
        badge = "Most Popular"
    ),
    VpnSubscriptionPlan(
        id = "plan_1m_prem",
        title = "1-Month Premium VIP",
        minutes = 30 * 24 * 60L,
        priceNaira = 7500.0,
        wholesaleCostNaira = 3000.0,
        profitMarginPercent = 60,
        maxDevices = 5,
        description = "Up to 5 devices, priority VIP servers (Dubai & London), 4K streaming.",
        icon = Icons.Default.Stars,
        accentColor = Color(0xFFD946EF),
        isModelA = true,
        badge = "VIP Power"
    )
)

// Model B: "Bought Time" & Data-Metered Packages (Pay-Per-GB)
val MODEL_B_METERED_PLANS = listOf(
    VpnSubscriptionPlan(
        id = "metered_24h",
        title = "24-Hour Continuous Uptime",
        minutes = 24 * 60L,
        priceNaira = 500.0,
        wholesaleCostNaira = 150.0,
        profitMarginPercent = 70,
        maxDevices = 1,
        description = "24 Hours uninterrupted emergency tunnel. Budget single-day pass.",
        icon = Icons.Default.Timelapse,
        accentColor = GlowingAmber,
        isModelA = false,
        badge = "Day Pass"
    ),
    VpnSubscriptionPlan(
        id = "metered_10gb",
        title = "10 GB Secure Data Pass",
        minutes = 30 * 24 * 60L,
        priceNaira = 800.0,
        wholesaleCostNaira = 300.0,
        profitMarginPercent = 62,
        maxDevices = 1,
        description = "Budget-conscious choice for online banking, chats, and essential downloads.",
        icon = Icons.Default.DataUsage,
        accentColor = CyberCyan,
        isModelA = false,
        quotaBytes = 10L * 1024 * 1024 * 1024,
        badge = "Starter GB"
    ),
    VpnSubscriptionPlan(
        id = "metered_25gb",
        title = "25 GB Ultra Stream Pass",
        minutes = 30 * 24 * 60L,
        priceNaira = 1800.0,
        wholesaleCostNaira = 700.0,
        profitMarginPercent = 61,
        maxDevices = 2,
        description = "Optimized for YouTube, Netflix & remote work sessions with zero lag.",
        icon = Icons.Default.Stream,
        accentColor = Color(0xFF007AFF),
        isModelA = false,
        quotaBytes = 25L * 1024 * 1024 * 1024,
        isPopular = true,
        badge = "Streaming Pick"
    ),
    VpnSubscriptionPlan(
        id = "metered_50gb",
        title = "50 GB Power Downloader",
        minutes = 30 * 24 * 60L,
        priceNaira = 3200.0,
        wholesaleCostNaira = 1200.0,
        profitMarginPercent = 62,
        maxDevices = 3,
        description = "Heavy downloading, gaming & unthrottled bandwidth on mobile & desktop.",
        icon = Icons.Default.CloudDownload,
        accentColor = ElectricEmerald,
        isModelA = false,
        quotaBytes = 50L * 1024 * 1024 * 1024,
        badge = "Heavy Data"
    )
)

@Composable
fun VpnTimeStoreDialog(
    viewModel: VpnViewModel,
    vpnTimeRemainingMinutes: Long,
    walletBalance: Double,
    onDismiss: () -> Unit,
    onFundWalletClick: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = Model A (Time), 1 = Model B (Metered)
    val activePlans = if (selectedTab == 0) MODEL_A_TIME_PLANS else MODEL_B_METERED_PLANS
    var selectedPlan by remember {
        mutableStateOf<VpnSubscriptionPlan?>(
            MODEL_A_TIME_PLANS.firstOrNull { it.isPopular } ?: MODEL_A_TIME_PLANS.first()
        )
    }

    var isPurchasing by remember { mutableStateOf(false) }
    var purchaseMessage by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CyberCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.VpnKey,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "VPN TIERS & PLANS",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        color = TextPrimary,
                                        letterSpacing = 0.5.sp
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = ElectricEmerald.copy(alpha = 0.18f),
                                    border = BorderStroke(0.5.dp, ElectricEmerald)
                                ) {
                                    Text(
                                        text = "FlowTest Package",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = ElectricEmerald,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp
                                        ),
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "WireGuard Fast Roam • MTN/Airtel/Glo Tuned",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Balances Row (Time Remaining & Wallet Balance)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = DarkSurface,
                        border = BorderStroke(1.dp, DarkCardBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                "CURRENT ACCESS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextMuted,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = viewModel.formatVpnRemainingTime(vpnTimeRemainingMinutes),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    color = ElectricEmerald,
                                    fontWeight = FontWeight.Black
                                )
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = DarkSurface,
                        border = BorderStroke(1.dp, DarkCardBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                "WALLET BALANCE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextMuted,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "₦${String.format(Locale.US, "%,.2f", walletBalance)}",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    color = CyberCyan,
                                    fontWeight = FontWeight.Black
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Model Selection Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = DarkSurface,
                    contentColor = CyberCyan,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = {
                            selectedTab = 0
                            selectedPlan = MODEL_A_TIME_PLANS.firstOrNull { it.isPopular } ?: MODEL_A_TIME_PLANS.first()
                        },
                        text = {
                            Text(
                                text = "Time Subscriptions",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = {
                            selectedTab = 1
                            selectedPlan = MODEL_B_METERED_PLANS.firstOrNull { it.isPopular } ?: MODEL_B_METERED_PLANS.first()
                        },
                        text = {
                            Text(
                                text = "Bought Time & GB",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }
                    )
                }

                if (purchaseMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSuccess) ElectricEmerald.copy(alpha = 0.15f) else WarningRed.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, if (isSuccess) ElectricEmerald else WarningRed)
                    ) {
                        Text(
                            text = purchaseMessage ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (isSuccess) ElectricEmerald else WarningRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Plans List
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(activePlans) { plan ->
                        val isSelected = selectedPlan?.id == plan.id
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) plan.accentColor.copy(alpha = 0.12f) else DarkSurface,
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                color = if (isSelected) plan.accentColor else DarkCardBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { selectedPlan = plan }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(plan.accentColor.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = plan.icon,
                                            contentDescription = null,
                                            tint = plan.accentColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = plan.title,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary
                                                )
                                            )
                                            if (!plan.badge.isNullOrEmpty()) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = if (plan.isPopular) ElectricEmerald else plan.accentColor.copy(alpha = 0.2f),
                                                    border = BorderStroke(0.5.dp, plan.accentColor)
                                                ) {
                                                    Text(
                                                        text = plan.badge,
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            color = if (plan.isPopular) DarkObsidian else plan.accentColor,
                                                            fontWeight = FontWeight.Black,
                                                            fontSize = 8.sp
                                                        ),
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = plan.description,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = TextSecondary,
                                                fontSize = 10.sp
                                            ),
                                            maxLines = 2
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Devices: ${plan.maxDevices}",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = TextMuted,
                                                    fontSize = 9.sp
                                                )
                                            )
                                            Text(
                                                text = "• Margin: ${plan.profitMarginPercent}%",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = ElectricEmerald,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                        }
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "₦${String.format(Locale.US, "%,.0f", plan.priceNaira)}",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Black,
                                            color = if (isSelected) plan.accentColor else TextPrimary
                                        )
                                    )
                                    Text(
                                        text = "Instant Activation",
                                        style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 8.sp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Action CTA
                selectedPlan?.let { plan ->
                    val canAfford = walletBalance >= plan.priceNaira
                    if (!canAfford) {
                        Button(
                            onClick = {
                                onDismiss()
                                onFundWalletClick()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GlowingAmber,
                                contentColor = DarkObsidian
                            )
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "FUND WALLET (NEEDS ₦${String.format(Locale.US, "%,.2f", plan.priceNaira - walletBalance)} MORE)",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black)
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                isPurchasing = true
                                purchaseMessage = null
                                viewModel.purchaseVpnResellersPlanWithWallet(
                                    planId = plan.id,
                                    planTitle = plan.title,
                                    minutesToAdd = plan.minutes,
                                    packagePrice = plan.priceNaira,
                                    wholesaleCost = plan.wholesaleCostNaira,
                                    isModelA = plan.isModelA,
                                    quotaBytes = plan.quotaBytes
                                ) { result ->
                                    isPurchasing = false
                                    isSuccess = result.isSuccess
                                    purchaseMessage = result.message
                                }
                            },
                            enabled = !isPurchasing,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberCyan,
                                contentColor = DarkObsidian
                            )
                        ) {
                            if (isPurchasing) {
                                FlowButtonLoadingLine(color = DarkObsidian, width = 44.dp)
                            } else {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "ACTIVATE ${plan.title.uppercase()} • ₦${String.format(Locale.US, "%,.0f", plan.priceNaira)}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
