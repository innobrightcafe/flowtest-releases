package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.db.PendingOrderEntity
import com.example.data.db.PricingConfigEntity
import com.example.data.db.UnresolvedPaymentEntity
import com.example.data.repository.WebhookFulfillmentResult
import com.example.data.ui.viewmodel.VpnViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookkeepingAnalyticsDashboard(
    viewModel: VpnViewModel,
    modifier: Modifier = Modifier
) {
    val stats by viewModel.bookkeepingStats.collectAsStateWithLifecycle()
    val transactions by viewModel.bookkeepingTransactions.collectAsStateWithLifecycle()
    val pricingConfigs by viewModel.pricingConfigs.collectAsStateWithLifecycle()
    val userWallet by viewModel.userWalletState.collectAsStateWithLifecycle()
    val pendingOrders by viewModel.allPendingOrders.collectAsStateWithLifecycle()
    val unresolvedPayments by viewModel.allUnresolvedPayments.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) } // 0: Stats, 1: Moniepoint Webhook, 2: Pending Orders, 3: Unresolved, 4: Pricing, 5: Ledger
    var statusFeedbackMessage by remember { mutableStateOf<String?>(null) }

    var showWebhookDialog by remember { mutableStateOf(false) }
    var webhookAmountInput by remember { mutableStateOf("") }
    var webhookNarrationInput by remember { mutableStateOf("") }
    var webhookSenderName by remember { mutableStateOf("") }
    var webhookTxnRef by remember { mutableStateOf("") }

    var showVendingDialog by remember { mutableStateOf(false) }
    var vendingType by remember { mutableStateOf("data") }
    var recipientInput by remember { mutableStateOf("") }
    var customAmountInput by remember { mutableStateOf("") }

    var editingConfig by remember { mutableStateOf<PricingConfigEntity?>(null) }
    var editWholesaleInput by remember { mutableStateOf("") }
    var editRetailInput by remember { mutableStateOf("") }
    var editMarkupInput by remember { mutableStateOf("") }
    var editFeeInput by remember { mutableStateOf("") }

    // Resolve modal state
    var selectedUnresolved by remember { mutableStateOf<UnresolvedPaymentEntity?>(null) }
    var resolveAction by remember { mutableStateOf("CREDIT_WALLET") }
    var resolveTargetPhone by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(DarkSurfaceElevated)
            .border(1.dp, DarkCardBorder, RoundedCornerShape(22.dp))
            .padding(18.dp)
    ) {
        // Dashboard Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(CyberCyan.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Analytics, contentDescription = "Analytics", tint = CyberCyan)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "MONIEPOINT CORPORATE DESK & BOOKKEEPING",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, color = TextPrimary)
                    )
                    Text(
                        text = "Corporate Account: 6666468328 • 11-Digit Phone Matching",
                        style = MaterialTheme.typography.bodySmall.copy(color = ElectricEmerald, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    )
                }
            }

            IconButton(onClick = { viewModel.refreshBookkeepingStats() }) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh", tint = CyberCyan)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Corporate Account Banner
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("CORPORATE COLLECTION ACCOUNT", style = MaterialTheme.typography.labelSmall.copy(color = CyberCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold))
                    Text(
                        text = "6666468328 • Moniepoint MFB",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, color = Color.White, fontFamily = FontFamily.Monospace)
                    )
                    Text(
                        text = "Flow Telecommunications",
                        style = MaterialTheme.typography.labelSmall.copy(color = ElectricEmerald, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = { showWebhookDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("INGEST WEBHOOK", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { showVendingDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("VEND", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Navigation Tab Selector
        val tabs = listOf("KPIS", "WEBHOOK", "PENDING", "UNRESOLVED", "PRICING", "LEDGER")
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DarkSurface)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(tabs.size) { index ->
                val label = tabs[index]
                val isSelected = activeTab == index
                val badgeCount = when (index) {
                    2 -> pendingOrders.count { it.status == "pending" }
                    3 -> unresolvedPayments.count { it.status == "UNRESOLVED" }
                    else -> 0
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) CyberCyan else Color.Transparent,
                    modifier = Modifier.clickable { activeTab = index }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                color = if (isSelected) DarkObsidian else TextSecondary,
                                fontSize = 10.sp
                            )
                        )
                        if (badgeCount > 0) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                shape = CircleShape,
                                color = if (isSelected) DarkObsidian else GlowingAmber
                            ) {
                                Text(
                                    text = "$badgeCount",
                                    color = if (isSelected) CyberCyan else DarkObsidian,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Feedback
        statusFeedbackMessage?.let { msg ->
            Surface(
                color = if (msg.contains("Fulfilled") || msg.contains("Success") || msg.contains("credited")) ElectricEmerald.copy(alpha = 0.15f) else GlowingAmber.copy(alpha = 0.15f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (msg.contains("Fulfilled") || msg.contains("Success") || msg.contains("credited")) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (msg.contains("Fulfilled") || msg.contains("Success") || msg.contains("credited")) ElectricEmerald else GlowingAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (msg.contains("Fulfilled") || msg.contains("Success") || msg.contains("credited")) ElectricEmerald else GlowingAmber,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // TAB CONTENT
        when (activeTab) {
            0 -> {
                // KPI STATS
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("TOTAL REVENUE", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold))
                                Text(
                                    text = "₦${String.format("%,.2f", stats.totalRevenueProcessed)}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, color = CyberCyan)
                                )
                                Text("Client Bank Debits", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 9.sp))
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("WHOLESALE COST", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold))
                                Text(
                                    text = "₦${String.format("%,.2f", stats.totalWholesaleCostExpended)}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, color = TextPrimary)
                                )
                                Text("Wholesale API Cost", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 9.sp))
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = ElectricEmerald.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ElectricEmerald.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("NET PROFIT EARNED (REAL-TIME)", style = MaterialTheme.typography.labelSmall.copy(color = ElectricEmerald, fontWeight = FontWeight.Bold, fontSize = 9.sp))
                                Text(
                                    text = "₦${String.format("%,.2f", stats.totalNetProfit)}",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, color = ElectricEmerald)
                                )
                            }

                            val marginPercent = if (stats.totalRevenueProcessed > 0) (stats.totalNetProfit / stats.totalRevenueProcessed) * 100.0 else 0.0
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = ElectricEmerald
                            ) {
                                Text(
                                    text = "+${String.format("%.1f", marginPercent)}% MARGIN",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black, color = DarkObsidian),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Text("PROFIT BREAKDOWN BY SERVICE CATEGORY:", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontWeight = FontWeight.Bold, fontSize = 9.sp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CategoryProfitChip("Data", stats.profitByData, CyberCyan, Modifier.weight(1f))
                        CategoryProfitChip("Airtime", stats.profitByAirtime, ElectricEmerald, Modifier.weight(1f))
                        CategoryProfitChip("Utilities", stats.profitByUtilities, GlowingAmber, Modifier.weight(1f))
                        CategoryProfitChip("Cable TV", stats.profitByCable, Color(0xFFA855F7), Modifier.weight(1f))
                    }
                }
            }

            1 -> {
                // MONIEPOINT WEBHOOK CONTROLLER
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("MONIEPOINT INBOUND WEBHOOK CONTROLLER", style = MaterialTheme.typography.labelSmall.copy(color = CyberCyan, fontWeight = FontWeight.Bold, fontSize = 10.sp))
                    Text(
                        text = "Moniepoint automated inbound webhook handler. Extracts 11-digit phone number, matches latest pending order or user wallet, verifies amount, and triggers instant vending!",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = DarkSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("LIVE DOMAIN WEBHOOK ENDPOINT:", style = MaterialTheme.typography.labelSmall.copy(color = ElectricEmerald, fontSize = 9.sp, fontWeight = FontWeight.Bold))
                                Surface(shape = RoundedCornerShape(4.dp), color = ElectricEmerald.copy(alpha = 0.15f)) {
                                    Text("ACTIVE", color = ElectricEmerald, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                }
                            }
                            Text(
                                text = "https://api.flowtest2026.com/api/webhook/moniepoint",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("STANDARD MONIEPOINT WEBHOOK JSON:", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold))
                            Text(
                                text = "{\n  \"event_type\": \"ACCOUNT_TRANSACTION\",\n  \"amount\": 1500.00,\n  \"narration\": \"Deposit for Client Account\",\n  \"transaction_reference\": \"MNP-93821094\",\n  \"destination_account\": \"6666468328\",\n  \"destination_bank\": \"Moniepoint MFB\"\n}",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, color = CyberCyan, fontSize = 10.sp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                statusFeedbackMessage = "Fetching actual live notifications from Cloud Run Hub & Gmail..."
                                viewModel.fetchActualLiveNotifications { list ->
                                    statusFeedbackMessage = "✓ Fetched ${list.size} actual live notifications from Cloud Run Hub & Gmail for today."
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("FETCH ACTUAL NOTIFICATIONS", fontWeight = FontWeight.Black, fontSize = 10.sp)
                        }

                        Button(
                            onClick = { showWebhookDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("MANUAL TRANSACTION INGESTION", fontWeight = FontWeight.Black, fontSize = 10.sp)
                        }
                    }

                    // Gmail 2nd Layer Fallback Sync Button
                    OutlinedButton(
                        onClick = {
                            statusFeedbackMessage = "Connecting to Gmail (Innobrightcafe@gmail.com) via IMAP..."
                            viewModel.syncGmailCreditAlerts { res ->
                                statusFeedbackMessage = if (res.isSuccess) {
                                    "✓ Gmail Sync: ${res.message}"
                                } else {
                                    "⚠️ Gmail Sync: ${res.message}"
                                }
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("2ND LAYER: SCAN GMAIL CREDIT ALERTS (Innobrightcafe@gmail.com)", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                }
            }

            2 -> {
                // PENDING ORDERS LIST
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("ACTIVE & QUEUED PENDING ORDERS", style = MaterialTheme.typography.labelSmall.copy(color = CyberCyan, fontWeight = FontWeight.Bold, fontSize = 10.sp))
                        Text("${pendingOrders.count { it.status == "pending" }} Pending", style = MaterialTheme.typography.labelSmall.copy(color = GlowingAmber, fontWeight = FontWeight.Bold, fontSize = 10.sp))
                    }

                    if (pendingOrders.isEmpty()) {
                        Text("No pending orders. When a customer initiates an order or transfer, their pending order appears here.", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted))
                    } else {
                        pendingOrders.take(15).forEach { order ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (order.status == "pending") CyberCyan.copy(alpha = 0.4f) else DarkCardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            StatusPill(order.status)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(order.id, style = MaterialTheme.typography.labelSmall.copy(color = CyberCyan, fontWeight = FontWeight.Bold, fontSize = 10.sp))
                                        }

                                        Text(
                                            text = "₦${String.format("%,.2f", order.retailPrice)}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Black, color = ElectricEmerald, fontSize = 13.sp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("${order.serviceType.uppercase()} • ${order.planName.ifBlank { order.planId }}", style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp))
                                    Text("Customer Phone: ${order.phoneNumber} • PIN: ${order.narrationCode.ifBlank { "N/A" }}", style = MaterialTheme.typography.labelSmall.copy(color = GlowingAmber, fontWeight = FontWeight.Bold, fontSize = 10.sp))

                                    val orderAgeMinutes = ((System.currentTimeMillis() - order.createdAt) / 60000L).coerceAtLeast(0L)
                                    val remainingWaitMin = (30L - orderAgeMinutes).coerceAtLeast(0L)
                                    if (order.status == "pending") {
                                        Text(
                                            text = if (remainingWaitMin > 0) "⏱ Pending Admin completion (System auto-refund in ${remainingWaitMin}m)"
                                            else "⏱ >30m elapsed (Eligible for system auto-refund if unfulfilled)",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = if (remainingWaitMin > 0) CyberCyan else GlowingAmber,
                                                fontSize = 9.sp
                                            )
                                        )
                                    }

                                    if (order.status == "pending") {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    viewModel.retryPendingServiceOrder(order.id) { ok, msg ->
                                                        statusFeedbackMessage = msg
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Retry", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }

                                            Button(
                                                onClick = {
                                                    viewModel.cancelAndRefundPendingOrder(order.id, "Admin Cancel & Refund") { ok, msg ->
                                                        statusFeedbackMessage = msg
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = GlowingAmber, contentColor = DarkObsidian),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                modifier = Modifier.weight(1.2f)
                                            ) {
                                                Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Cancel & Refund", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }

                                            Button(
                                                onClick = {
                                                    viewModel.fulfillPendingOrderManually(order.id) { ok, msg ->
                                                        statusFeedbackMessage = msg
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                modifier = Modifier.weight(1.1f)
                                            ) {
                                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Manual Fulfill", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    } else if (order.status == "cancelled_refunded") {
                                        Text("Refunded back to ${order.phoneNumber} wallet", style = MaterialTheme.typography.labelSmall.copy(color = ElectricEmerald, fontSize = 9.sp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            3 -> {
                // UNRESOLVED PAYMENTS & FUNDING ERROR DESK
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("MANUAL REVIEW DESK (FUNDING ERRORS & UNRESOLVED PAYMENTS)", style = MaterialTheme.typography.labelSmall.copy(color = GlowingAmber, fontWeight = FontWeight.Bold, fontSize = 10.sp))
                        Text("${unresolvedPayments.count { it.status == "UNRESOLVED" }} Pending Review", style = MaterialTheme.typography.labelSmall.copy(color = GlowingAmber, fontWeight = FontWeight.Bold, fontSize = 10.sp))
                    }

                    if (unresolvedPayments.isEmpty()) {
                        Text("No pending funding error reports or unresolved payments! All incoming bank deposits have been verified.", style = MaterialTheme.typography.bodySmall.copy(color = ElectricEmerald))
                    } else {
                        unresolvedPayments.forEach { unres ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1404)),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, GlowingAmber.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        StatusPill(unres.status)
                                        Text("₦${String.format("%,.2f", unres.amount)}", style = MaterialTheme.typography.titleSmall.copy(color = GlowingAmber, fontWeight = FontWeight.Black))
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Customer / Sender: ${unres.senderName} • Ref: ${unres.bankReference}", style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, fontSize = 11.sp))
                                    Text("Phone / Account: ${unres.detectedPhone ?: "Unknown"}", style = MaterialTheme.typography.bodySmall.copy(color = CyberCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp))
                                    Text("Narration / Note: \"${unres.rawNarration}\"", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFECD79B), fontSize = 10.sp, fontFamily = FontFamily.Monospace))
                                    Text("Audit Note: ${unres.failureReason}", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFEF4444), fontSize = 10.sp))

                                    if (unres.status == "UNRESOLVED") {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Button(
                                                onClick = {
                                                    selectedUnresolved = unres
                                                    resolveTargetPhone = unres.detectedPhone ?: ""
                                                    resolveAction = "SEND_DATA"
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Credit Service", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }

                                            Button(
                                                onClick = {
                                                    selectedUnresolved = unres
                                                    resolveTargetPhone = unres.detectedPhone ?: ""
                                                    resolveAction = "CREDIT_WALLET"
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Credit Wallet", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }

                                            Button(
                                                onClick = {
                                                    viewModel.resolveUnresolvedPayment(
                                                        unresolvedId = unres.id,
                                                        action = "MARK_DECLINED",
                                                        targetPhone = unres.detectedPhone ?: "",
                                                        notes = "Declined by Admin"
                                                    ) { _, msg ->
                                                        statusFeedbackMessage = msg
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444), contentColor = Color.White),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                modifier = Modifier.weight(0.7f)
                                            ) {
                                                Text("Decline", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            4 -> {
                // PRICING MATRIX
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("GLOBAL PRICING CONFIGURATION MATRIX", style = MaterialTheme.typography.labelSmall.copy(color = CyberCyan, fontWeight = FontWeight.Bold, fontSize = 10.sp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(pricingConfigs) { config ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .width(170.dp)
                                    .clickable {
                                        editingConfig = config
                                        editWholesaleInput = config.wholesaleCost.toString()
                                        editRetailInput = config.userRetailPrice.toString()
                                        editMarkupInput = config.percentageMarkup.toString()
                                        editFeeInput = config.flatConvenienceFee.toString()
                                    }
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = config.id.uppercase(),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = CyberCyan, fontSize = 10.sp)
                                    )
                                    Text("Service: ${config.serviceType.uppercase()}", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 10.sp))

                                    Spacer(modifier = Modifier.height(4.dp))

                                    if (config.serviceType == "data") {
                                        Text("Wholesale: ₦${config.wholesaleCost}", style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, fontSize = 11.sp))
                                        Text("Retail: ₦${config.userRetailPrice}", style = MaterialTheme.typography.bodySmall.copy(color = ElectricEmerald, fontWeight = FontWeight.Bold, fontSize = 11.sp))
                                        Text("Profit: +₦${config.userRetailPrice - config.wholesaleCost}", style = MaterialTheme.typography.labelSmall.copy(color = CyberCyan, fontSize = 10.sp))
                                    } else if (config.serviceType == "airtime") {
                                        Text("Markup: +${config.percentageMarkup}%", style = MaterialTheme.typography.bodySmall.copy(color = ElectricEmerald, fontWeight = FontWeight.Bold, fontSize = 11.sp))
                                    } else {
                                        Text("Flat Fee: +₦${config.flatConvenienceFee}", style = MaterialTheme.typography.bodySmall.copy(color = GlowingAmber, fontWeight = FontWeight.Bold, fontSize = 11.sp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            5 -> {
                // LEDGER
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("ATOMIC REAL-TIME TRANSACTION LEDGER", style = MaterialTheme.typography.labelSmall.copy(color = CyberCyan, fontWeight = FontWeight.Bold, fontSize = 10.sp))

                    if (transactions.isEmpty()) {
                        Text("No transactions logged yet. Inbound bank transfers and vending transactions will appear here in real time.", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted))
                    } else {
                        transactions.take(8).forEach { tx ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1.5f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            StatusPill(tx.status)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(tx.serviceCategory.uppercase(), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 10.sp))
                                        }
                                        Text(tx.recipientOrAccount, style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp))
                                        Text("Ref: ${tx.reference}", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace))
                                        val timeDisplay = if (tx.completedAtFormatted.isNotBlank()) tx.completedAtFormatted else java.text.SimpleDateFormat("HH:mm:ss dd-MMM-yyyy", java.util.Locale.US).format(java.util.Date(tx.timestamp))
                                        val srcDisplay = if (tx.confirmationSource.isNotBlank()) tx.confirmationSource else "PAIRGATE API"
                                        Text("Source: $srcDisplay • $timeDisplay", style = MaterialTheme.typography.labelSmall.copy(color = CyberCyan.copy(alpha = 0.85f), fontSize = 9.sp))
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "Debited: ₦${String.format("%,.2f", tx.amountDebitedFromUser)}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 11.sp)
                                        )
                                        Text(
                                            text = "Profit: +₦${String.format("%,.2f", tx.netProfitEarned)}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, color = ElectricEmerald, fontSize = 10.sp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- MONIEPOINT INBOUND WEBHOOK INGESTION DIALOG ---
    if (showWebhookDialog) {
        AlertDialog(
            onDismissRequest = { showWebhookDialog = false },
            title = { Text("Moniepoint Transaction Ingestion", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Ingest and verify an incoming bank transfer for Corporate Moniepoint Account (6666468328). Automatically matches recipient phone number and credits the wallet ledger:", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))

                    Surface(
                        color = CyberCyan.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Live Settlement: Credits verified funds received to user wallet and settles ledger.",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = CyberCyan,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    OutlinedTextField(
                        value = webhookSenderName,
                        onValueChange = { webhookSenderName = it },
                        label = { Text("Sender Customer Name") },
                        placeholder = { Text("e.g. Adekunle Gold") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = webhookAmountInput,
                        onValueChange = { webhookAmountInput = it },
                        label = { Text("Amount (₦)") },
                        placeholder = { Text("e.g. 1500") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = webhookNarrationInput,
                        onValueChange = { webhookNarrationInput = it },
                        label = { Text("Bank Narration (11-digit phone)") },
                        placeholder = { Text("e.g. Transfer for 08137545370") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = webhookTxnRef,
                        onValueChange = { webhookTxnRef = it },
                        label = { Text("Transaction Ref (Optional)") },
                        placeholder = { Text("e.g. MNP-TRX-...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = webhookAmountInput.toDoubleOrNull() ?: 1000.0
                        val ref = webhookTxnRef.trim().ifBlank { "MNP-TRX-" + (1000000..9999999).random() }
                        viewModel.processMoniepointBankDepositWebhook(
                            transactionReference = ref,
                            amount = amt,
                            rawNarration = webhookNarrationInput,
                            senderName = webhookSenderName,
                            isSimulationOnly = false
                        ) { res ->
                            statusFeedbackMessage = res.message
                        }
                        showWebhookDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian)
                ) {
                    Text("VERIFY & CREDIT LEDGER", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWebhookDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            },
            containerColor = DarkSurfaceElevated
        )
    }

    // --- RESOLVE UNRESOLVED PAYMENT DIALOG ---
    selectedUnresolved?.let { unres ->
        var resolveServicePlan by remember { mutableStateOf("mtn_sme_1gb") }
        AlertDialog(
            onDismissRequest = { selectedUnresolved = null },
            title = { Text("Resolve Payment: ${unres.bankReference}", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Amount: ₦${unres.amount} • Sender: ${unres.senderName}", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
                    Text("Narration: \"${unres.rawNarration}\"", style = MaterialTheme.typography.labelSmall.copy(color = CyberCyan))

                    Text("Select Resolution Action:", style = MaterialTheme.typography.labelSmall.copy(color = ElectricEmerald, fontWeight = FontWeight.Bold, fontSize = 10.sp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            "CREDIT_WALLET" to "Credit Wallet",
                            "SEND_DATA" to "Send Data",
                            "SEND_AIRTIME" to "Send Airtime"
                        ).forEach { (act, label) ->
                            val isSel = resolveAction == act
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) CyberCyan else DarkSurface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) CyberCyan else DarkCardBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { resolveAction = act }
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (isSel) DarkObsidian else TextSecondary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp
                                        )
                                    )
                                }
                            }
                        }
                    }

                    if (resolveAction == "SEND_DATA") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("mtn_sme_1gb" to "1GB", "mtn_sme_2gb" to "2GB", "mtn_sme_5gb" to "5GB").forEach { (plan, lbl) ->
                                val isPlanSel = resolveServicePlan == plan
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isPlanSel) ElectricEmerald.copy(alpha = 0.2f) else DarkSurface,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isPlanSel) ElectricEmerald else DarkCardBorder),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { resolveServicePlan = plan }
                                ) {
                                    Text(
                                        text = lbl,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (isPlanSel) ElectricEmerald else TextSecondary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp
                                        ),
                                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = resolveTargetPhone,
                        onValueChange = { resolveTargetPhone = it },
                        label = { Text("Target User Phone (11 digits)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = resolveTargetPhone.trim()
                        if (resolveAction == "SEND_DATA") {
                            viewModel.adminSendServiceToUser(
                                targetPhone = target,
                                serviceCategory = "DATA",
                                planOrCode = resolveServicePlan,
                                amount = unres.amount
                            ) { success, msg ->
                                if (success) {
                                    viewModel.resolveUnresolvedPayment(unres.id, "RESOLVED_ORDER_PUSHED", target, "Dispatched data $resolveServicePlan") { _, rMsg ->
                                        statusFeedbackMessage = "$msg ($rMsg)"
                                    }
                                } else {
                                    statusFeedbackMessage = msg
                                }
                            }
                        } else if (resolveAction == "SEND_AIRTIME") {
                            viewModel.adminSendServiceToUser(
                                targetPhone = target,
                                serviceCategory = "AIRTIME",
                                planOrCode = "mtn",
                                amount = unres.amount
                            ) { success, msg ->
                                if (success) {
                                    viewModel.resolveUnresolvedPayment(unres.id, "RESOLVED_ORDER_PUSHED", target, "Dispatched airtime ₦${unres.amount}") { _, rMsg ->
                                        statusFeedbackMessage = "$msg ($rMsg)"
                                    }
                                } else {
                                    statusFeedbackMessage = msg
                                }
                            }
                        } else {
                            viewModel.resolveUnresolvedPayment(
                                unresolvedId = unres.id,
                                action = "CREDIT_WALLET",
                                targetPhone = target
                            ) { ok, msg ->
                                statusFeedbackMessage = msg
                            }
                        }
                        selectedUnresolved = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian)
                ) {
                    Text("Apply Resolution", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedUnresolved = null }) {
                    Text("Cancel", color = TextMuted)
                }
            },
            containerColor = DarkSurfaceElevated
        )
    }

    // --- LIVE VENDING DISPATCHER DIALOG ---
    if (showVendingDialog) {
        AlertDialog(
            onDismissRequest = { showVendingDialog = false },
            title = { Text("Live Multi-Utility Vending Engine", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column {
                    Text("Select service category to execute live debit and vending fulfillment:", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("data", "airtime", "utility", "cable").forEach { type ->
                            FilterChip(
                                selected = vendingType == type,
                                onClick = { vendingType = type },
                                label = { Text(type.uppercase(), fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CyberCyan, selectedLabelColor = DarkObsidian),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = recipientInput,
                        onValueChange = { recipientInput = it },
                        label = { Text("Recipient Phone / Meter / SmartCard") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (vendingType != "data") {
                        OutlinedTextField(
                            value = customAmountInput,
                            onValueChange = { customAmountInput = it },
                            label = { Text("Amount (₦)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = customAmountInput.toDoubleOrNull() ?: 1000.0
                        when (vendingType) {
                            "data" -> viewModel.purchaseDataPackageWithEngine("mtn_sme_1gb", recipientInput, false) { res -> statusFeedbackMessage = res.message }
                            "airtime" -> viewModel.vendAirtimeWithEngine("mtn", recipientInput, amt, false) { res -> statusFeedbackMessage = res.message }
                            "utility" -> viewModel.payUtilityOrCableWithEngine("electricity", "ikedc", recipientInput, amt, false) { res -> statusFeedbackMessage = res.message }
                            "cable" -> viewModel.payUtilityOrCableWithEngine("cable_tv", "dstv", recipientInput, amt, false) { res -> statusFeedbackMessage = res.message }
                        }
                        showVendingDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian)
                ) {
                    Text("DISPATCH VEND", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showVendingDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            },
            containerColor = DarkSurfaceElevated
        )
    }

    // --- EDIT PRICING CONFIG DIALOG ---
    editingConfig?.let { cfg ->
        AlertDialog(
            onDismissRequest = { editingConfig = null },
            title = { Text("Edit Global Pricing: ${cfg.id.uppercase()}", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column {
                    if (cfg.serviceType == "data") {
                        OutlinedTextField(
                            value = editWholesaleInput,
                            onValueChange = { editWholesaleInput = it },
                            label = { Text("Wholesale Cost (₦)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = editRetailInput,
                            onValueChange = { editRetailInput = it },
                            label = { Text("User Retail Price (₦)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else if (cfg.serviceType == "airtime") {
                        OutlinedTextField(
                            value = editMarkupInput,
                            onValueChange = { editMarkupInput = it },
                            label = { Text("Percentage Markup (%)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        OutlinedTextField(
                            value = editFeeInput,
                            onValueChange = { editFeeInput = it },
                            label = { Text("Flat Convenience Fee (₦)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updated = cfg.copy(
                            wholesaleCost = editWholesaleInput.toDoubleOrNull() ?: cfg.wholesaleCost,
                            userRetailPrice = editRetailInput.toDoubleOrNull() ?: cfg.userRetailPrice,
                            percentageMarkup = editMarkupInput.toDoubleOrNull() ?: cfg.percentageMarkup,
                            flatConvenienceFee = editFeeInput.toDoubleOrNull() ?: cfg.flatConvenienceFee
                        )
                        viewModel.savePricingConfigInEngine(updated) {
                            statusFeedbackMessage = "Saved pricing config for ${cfg.id}!"
                        }
                        editingConfig = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian)
                ) {
                    Text("SAVE CONFIG", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingConfig = null }) {
                    Text("Cancel", color = TextMuted)
                }
            },
            containerColor = DarkSurfaceElevated
        )
    }
}

@Composable
private fun CategoryProfitChip(label: String, amount: Double, color: Color, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = DarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 9.sp))
            Text(
                text = "₦${String.format("%,.0f", amount)}",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = color, fontSize = 11.sp)
            )
        }
    }
}

@Composable
private fun StatusPill(status: String) {
    val (bgColor, textColor, label) = when (status) {
        "success", "completed" -> Triple(ElectricEmerald.copy(alpha = 0.2f), ElectricEmerald, "COMPLETED")
        "pending" -> Triple(GlowingAmber.copy(alpha = 0.2f), GlowingAmber, "PENDING")
        "underpaid" -> Triple(Color(0xFFEF4444).copy(alpha = 0.2f), Color(0xFFEF4444), "UNDERPAID")
        "UNRESOLVED" -> Triple(GlowingAmber.copy(alpha = 0.2f), GlowingAmber, "UNRESOLVED")
        else -> Triple(Color(0xFFEF4444).copy(alpha = 0.2f), Color(0xFFEF4444), status.uppercase())
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bgColor
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(color = textColor, fontWeight = FontWeight.Bold, fontSize = 8.sp),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
