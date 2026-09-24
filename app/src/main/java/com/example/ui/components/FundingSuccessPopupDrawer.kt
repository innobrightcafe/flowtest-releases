package com.example.ui.components

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FundingTransactionStatus
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Dedicated Success Popup Drawer shown when a deposit transaction is confirmed by webhook.
 * Replaces simple inline text with a rich, celebratory confirmation modal.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FundingSuccessPopupDrawer(
    confirmedState: FundingTransactionStatus.Confirmed,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var copiedRefMsg by remember { mutableStateOf<String?>(null) }
    var autoReturnSecondsRemaining by remember { mutableIntStateOf(5) }

    // Auto-return after 5 seconds
    LaunchedEffect(Unit) {
        while (autoReturnSecondsRemaining > 0) {
            kotlinx.coroutines.delay(1000L)
            autoReturnSecondsRemaining--
        }
        onDismiss()
    }

    val formattedTime = remember(confirmedState.timestamp) {
        SimpleDateFormat("HH:mm:ss dd-MMM-yyyy", Locale.US).format(Date(confirmedState.timestamp))
    }

    fun shareReceipt() {
        val receiptText = """
            FlowTest Deposit Receipt
            ━━━━━━━━━━━━━━━━━━━━━━
            • Status: CONFIRMED & CREDITED
            • Amount: +₦${String.format(Locale.US, "%,.2f", confirmedState.amount)}
            • New Balance: ₦${String.format(Locale.US, "%,.2f", confirmedState.newBalance)}
            • Confirmation PIN: ${confirmedState.confirmationCode.ifBlank { "N/A" }}
            • Reference: ${confirmedState.reference}
            • Source: ${confirmedState.source}
            • Date: $formattedTime
            ━━━━━━━━━━━━━━━━━━━━━━
            Instant settlement verified by FlowTest Automated Gateway.
        """.trimIndent()

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, receiptText)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share Deposit Receipt")
        context.startActivity(shareIntent)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
            confirmValueChange = { false }
        ),
        containerColor = DarkSurfaceElevated,
        scrimColor = Color.Black.copy(alpha = 0.7f),
        dragHandle = {
            Surface(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(44.dp)
                    .height(4.dp),
                shape = CircleShape,
                color = ElectricEmerald.copy(alpha = 0.6f)
            ) {}
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
                .testTag("funding_success_drawer"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Close Button Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Celebratory Checkmark with Glowing Ring
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(ElectricEmerald.copy(alpha = 0.15f))
                    .border(2.5.dp, ElectricEmerald, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Payment Confirmed",
                    tint = ElectricEmerald,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                color = ElectricEmerald.copy(alpha = 0.18f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, ElectricEmerald.copy(alpha = 0.4f))
            ) {
                Text(
                    text = "TRANSFER CONFIRMED",
                    color = ElectricEmerald,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "+₦${String.format(Locale.US, "%,.2f", confirmedState.amount)}",
                style = MaterialTheme.typography.displaySmall.copy(
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 38.sp,
                    letterSpacing = (-0.5).sp
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = confirmedState.message.ifBlank { "Your wallet has been credited and settled instantly." },
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = TextPrimary.copy(alpha = 0.9f),
                    fontSize = 13.5.sp,
                    lineHeight = 19.sp
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Reconciled Receipt Summary Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, DarkCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Item 1: Updated Wallet Balance
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "New Wallet Balance",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary.copy(alpha = 0.85f), fontSize = 13.sp)
                        )
                        Text(
                            text = "₦${String.format(Locale.US, "%,.2f", confirmedState.newBalance)}",
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = ElectricEmerald,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        )
                    }

                    HorizontalDivider(color = DarkCardBorder.copy(alpha = 0.6f))

                    // Item 2: Confirmation Source & Signature
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Settlement Method",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary.copy(alpha = 0.85f), fontSize = 13.sp)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(ElectricEmerald)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = confirmedState.source,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = CyberCyan,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp
                                )
                            )
                        }
                    }

                    // Item 3: Bank Reference ID with Copy action
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Bank Reference",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary.copy(alpha = 0.85f), fontSize = 13.sp)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                clipboardManager.setText(AnnotatedString(confirmedState.reference))
                                copiedRefMsg = "Reference copied to clipboard"
                            }
                        ) {
                            Text(
                                text = confirmedState.reference,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextPrimary,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = "Copy",
                                tint = CyberCyan,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    if (confirmedState.confirmationCode.isNotBlank()) {
                        // Item 4: Narration PIN Used
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Narration PIN Used",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary.copy(alpha = 0.85f), fontSize = 13.sp)
                            )
                            Text(
                                text = confirmedState.confirmationCode,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = GlowingAmber,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            )
                        }
                    }

                    // Item 5: Completed Timestamp
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Timestamp",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary.copy(alpha = 0.85f), fontSize = 13.sp)
                        )
                        Text(
                            text = formattedTime,
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 12.sp)
                        )
                    }
                }
            }

            copiedRefMsg?.let { msg ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "✓ $msg",
                    color = ElectricEmerald,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons: Done & Continue + Share Receipt
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { shareReceipt() },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CyberCyan),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "RECEIPT",
                        color = CyberCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp
                    )
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberCyan,
                        contentColor = DarkObsidian
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(48.dp)
                        .testTag("funding_success_done_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "RETURN HOME (${autoReturnSecondsRemaining}s)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Auto-returning home in ${autoReturnSecondsRemaining}s...",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}
