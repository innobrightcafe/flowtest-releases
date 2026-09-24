package com.example.ui.components.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.db.InboundNotificationAuditLogEntity
import com.example.data.ui.viewmodel.VpnViewModel
import com.example.ui.theme.*

@Composable
fun AdminAuditLogsTab(
    viewModel: VpnViewModel,
    modifier: Modifier = Modifier
) {
    val transactionLogs by viewModel.vtuTransactionLogs.collectAsStateWithLifecycle()
    val webhookLogs by viewModel.pairgateWebhookLogs.collectAsStateWithLifecycle()
    val httpSmsDeliveryLogs by viewModel.httpSmsDeliveryLogs.collectAsStateWithLifecycle()
    val inboundAuditLogs by viewModel.inboundAuditLogs.collectAsStateWithLifecycle()

    var logFilter by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    var inboundSourceFilter by remember { mutableStateOf("ALL") }
    var expandedAuditLogId by remember { mutableStateOf<String?>(null) }

    val filteredInboundLogs = remember(inboundAuditLogs, inboundSourceFilter) {
        inboundAuditLogs.filter { log ->
            when (inboundSourceFilter) {
                "MONIEPOINT" -> log.source.contains("MONIEPOINT", ignoreCase = true)
                "PAIRGATE" -> log.source.contains("PAIRGATE", ignoreCase = true)
                "GMAIL" -> log.source.contains("GMAIL", ignoreCase = true)
                else -> true
            }
        }
    }

    val filteredLogs = remember(transactionLogs, logFilter, searchQuery) {
        transactionLogs.filter { log ->
            val matchesFilter = when (logFilter) {
                "DATA" -> log.type.contains("data", ignoreCase = true)
                "AIRTIME" -> log.type.contains("airtime", ignoreCase = true)
                "WALLET" -> log.type.contains("wallet", ignoreCase = true) || log.type.contains("fund", ignoreCase = true)
                else -> true
            }
            val matchesSearch = if (searchQuery.isBlank()) true else {
                log.type.contains(searchQuery, ignoreCase = true) ||
                log.recipient.contains(searchQuery, ignoreCase = true) ||
                log.reference.contains(searchQuery, ignoreCase = true)
            }
            matchesFilter && matchesSearch
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Filter & Search Header Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, CyberCyan.copy(alpha = 0.4f), RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = "Logs", tint = CyberCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "REAL-TIME VTU TRANSACTION LOGS",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                        )
                    }

                    Surface(
                        color = CyberCyan.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${filteredLogs.size} Records",
                            color = CyberCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by reference, recipient phone, type...", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = CyberCyan, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = TextMuted, modifier = Modifier.size(16.dp))
                            }
                        }
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

                Spacer(modifier = Modifier.height(10.dp))

                // Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("ALL" to "All Logs", "DATA" to "Data", "AIRTIME" to "Airtime", "WALLET" to "Funding").forEach { (key, label) ->
                        val isSelected = logFilter == key
                        FilterChip(
                            selected = isSelected,
                            onClick = { logFilter = key },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyberCyan,
                                selectedLabelColor = DarkObsidian,
                                containerColor = DarkSurface,
                                labelColor = TextSecondary
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Transactions List
        if (filteredLogs.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = DarkSurfaceElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(imageVector = Icons.Default.Receipt, contentDescription = null, tint = TextMuted, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (searchQuery.isBlank()) "No transaction logs recorded yet" else "No transactions match \"$searchQuery\"",
                        color = TextMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        } else {
            filteredLogs.forEach { log ->
                val isCredit = log.type.contains("Deposit", ignoreCase = true) ||
                        log.type.contains("Fund", ignoreCase = true) ||
                        log.confirmationSource.contains("MONIEPOINT", ignoreCase = true) ||
                        log.confirmationSource.contains("GMAIL", ignoreCase = true) ||
                        log.confirmationSource.contains("ADMIN", ignoreCase = true)

                val sourceBadgeColor = when {
                    log.confirmationSource.contains("WEBHOOK", ignoreCase = true) -> ElectricEmerald
                    log.confirmationSource.contains("GMAIL", ignoreCase = true) -> GlowingAmber
                    log.confirmationSource.contains("ADMIN", ignoreCase = true) -> Color(0xFF818CF8)
                    else -> CyberCyan
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, DarkCardBorder, RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
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
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (isCredit) ElectricEmerald.copy(alpha = 0.15f) else CyberCyan.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isCredit) Icons.Default.CheckCircle else Icons.Default.SwapVert,
                                        contentDescription = "Status",
                                        tint = if (isCredit) ElectricEmerald else CyberCyan,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f, fill = false)) {
                                    Text(
                                        text = log.type,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Recipient: ${log.recipient}",
                                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 11.sp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = "${if (isCredit) "+" else "-"}₦${String.format(java.util.Locale.US, "%,.2f", log.amountNaira)}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = if (isCredit) ElectricEmerald else CyberCyan
                                ),
                                maxLines = 1
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = DarkCardBorder.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(8.dp))

                        // Confirmation Source + Completion Time + Reference
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Confirmation Source Badge
                            Surface(
                                color = sourceBadgeColor.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(6.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, sourceBadgeColor.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(sourceBadgeColor)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = log.confirmationSource.ifBlank { "PAIRGATE API" },
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = sourceBadgeColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp
                                        )
                                    )
                                }
                            }

                            // Completion Time
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = log.completionTime.ifBlank { log.timestamp },
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TextMuted,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Ref: ${log.reference}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextMuted,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Inbound Notification & Webhook Reconciliation Audit Logs
        Spacer(modifier = Modifier.height(10.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, CyberCyan.copy(alpha = 0.35f), RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = CyberCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "INBOUND NOTIFICATION AUDIT TRAIL",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                            )
                            Text(
                                text = "Moniepoint, Pairgate & Gmail Reconciliation Logs",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
                            )
                        }
                    }

                    Surface(
                        color = CyberCyan.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${inboundAuditLogs.size} Events",
                            color = CyberCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Real Notification Sync Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.fetchActualLiveNotifications {}
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("FETCH LIVE NOTIFICATIONS", fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.syncGmailCreditAlerts {}
                        },
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA855F7)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = Color(0xFFA855F7), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SYNC GMAIL ALERTS", color = Color(0xFFA855F7), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }

                    if (inboundAuditLogs.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.clearAllInboundAuditLogs() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Clear Logs", tint = WarningRed, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Source Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("ALL" to "All (${inboundAuditLogs.size})", "MONIEPOINT" to "Moniepoint", "PAIRGATE" to "Pairgate", "GMAIL" to "Gmail").forEach { (key, label) ->
                        val isSelected = inboundSourceFilter == key
                        FilterChip(
                            selected = isSelected,
                            onClick = { inboundSourceFilter = key },
                            label = { Text(label, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyberCyan,
                                selectedLabelColor = DarkObsidian,
                                containerColor = DarkSurface,
                                labelColor = TextSecondary
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (filteredInboundLogs.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DarkObsidian,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = "No inbound notification audit records found. Tap 'FETCH LIVE NOTIFICATIONS' or 'SYNC GMAIL ALERTS' to audit incoming bank notifications and verify HMAC ledger reconciliation.",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 11.sp),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                } else {
                    filteredInboundLogs.take(10).forEach { log ->
                        val sourceColor = when {
                            log.source.contains("MONIEPOINT", ignoreCase = true) -> CyberCyan
                            log.source.contains("PAIRGATE", ignoreCase = true) -> ElectricEmerald
                            log.source.contains("GMAIL", ignoreCase = true) -> Color(0xFFA855F7)
                            else -> TextSecondary
                        }

                        val statusColor = when {
                            log.reconciliationStatus.contains("RECONCILED", ignoreCase = true) || log.reconciliationStatus.contains("MATCHED", ignoreCase = true) -> ElectricEmerald
                            log.reconciliationStatus.contains("DUPLICATE", ignoreCase = true) -> GlowingAmber
                            log.reconciliationStatus.contains("FAILED", ignoreCase = true) -> WarningRed
                            else -> GlowingAmber
                        }

                        val isExpanded = expandedAuditLogId == log.id

                        Surface(
                            color = DarkObsidian,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (log.signatureVerified) DarkCardBorder else WarningRed.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                // Row 1: Source badge, Status badge, Amount
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            color = sourceColor.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(6.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, sourceColor.copy(alpha = 0.4f))
                                        ) {
                                            Text(
                                                text = log.source.replace("_NOTIFICATION", "").replace("_WEBHOOK", ""),
                                                color = sourceColor,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        Surface(
                                            color = statusColor.copy(alpha = 0.12f),
                                            shape = RoundedCornerShape(6.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.35f))
                                        ) {
                                            Text(
                                                text = log.reconciliationStatus,
                                                color = statusColor,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = "₦${String.format("%,.2f", log.amount)}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (log.signatureVerified) TextPrimary else WarningRed,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 13.sp
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Row 2: Reference & Sender
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Ref: ${log.reference}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = TextSecondary,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = log.parsedSender.ifBlank { "Bank Customer" },
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = TextPrimary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Row 3: Narration & Detected Code / Phone
                                Text(
                                    text = "Narration: \"${log.parsedNarration.ifBlank { "Direct Bank Transfer" }}\"",
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 10.5.sp),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                if (log.detectedConfirmationCode != null || log.detectedPhone != null) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        log.detectedConfirmationCode?.let { code ->
                                            Text("Detected PIN: $code", color = CyberCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                        log.detectedPhone?.let { phone ->
                                            Text("Phone: $phone", color = ElectricEmerald, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Row 4: Balance Before -> Balance After
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Wallet: ₦${String.format("%,.2f", log.balanceBefore)} → ₦${String.format("%,.2f", log.balanceAfter)}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (log.balanceAfter > log.balanceBefore) ElectricEmerald else TextMuted,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Text(
                                        text = log.completedAtFormatted,
                                        style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 9.5.sp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                // Row 5: Signature details & Reconciliation notes
                                Text(
                                    text = "Sig: ${log.signatureDetails} • ${log.reconciliationNotes}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 9.5.sp),
                                    maxLines = if (isExpanded) 10 else 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                // Expand / Collapse Raw Payload
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(
                                        onClick = { expandedAuditLogId = if (isExpanded) null else log.id },
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text(
                                            text = if (isExpanded) "Hide Raw Payload ▲" else "View Raw Payload ▼",
                                            fontSize = 9.5.sp,
                                            color = CyberCyan
                                        )
                                    }
                                }

                                AnimatedVisibility(visible = isExpanded) {
                                    Surface(
                                        color = DarkSurface,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                    ) {
                                        Text(
                                            text = log.rawPayload,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 9.sp,
                                                color = TextSecondary
                                            ),
                                            modifier = Modifier.padding(8.dp)
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
}
