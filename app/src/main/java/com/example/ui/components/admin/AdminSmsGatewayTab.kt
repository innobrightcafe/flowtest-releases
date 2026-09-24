package com.example.ui.components.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ui.viewmodel.VpnViewModel
import com.example.ui.theme.*

@Composable
fun AdminSmsGatewayTab(
    viewModel: VpnViewModel,
    onTestSmsClick: () -> Unit,
    onShowStatusMsg: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val smsWholesaleCost by viewModel.smsWholesaleCost.collectAsStateWithLifecycle()
    val smsRetailPrice by viewModel.smsRetailPrice.collectAsStateWithLifecycle()
    val smsSenderId by viewModel.smsSenderId.collectAsStateWithLifecycle()
    val httpSmsApiKey by viewModel.httpSmsApiKey.collectAsStateWithLifecycle()
    val httpSmsWebhookUrl by viewModel.httpSmsWebhookUrl.collectAsStateWithLifecycle()
    val httpSmsDeliveryStats by viewModel.httpSmsDeliveryStats.collectAsStateWithLifecycle()
    val httpSmsDeliveryLogs by viewModel.httpSmsDeliveryLogs.collectAsStateWithLifecycle()

    val clipboardManager = LocalClipboardManager.current

    var smsWholesaleInput by remember(smsWholesaleCost) { mutableStateOf(smsWholesaleCost.toString()) }
    var smsRetailInput by remember(smsRetailPrice) { mutableStateOf(smsRetailPrice.toString()) }
    var smsSenderIdInput by remember(smsSenderId) { mutableStateOf(smsSenderId) }
    var smsApiKeyInput by remember(httpSmsApiKey) { mutableStateOf(httpSmsApiKey) }
    var smsSettingsSavedMsg by remember { mutableStateOf<String?>(null) }
    var httpSmsWebhookMsg by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. SMS Reselling System & HttpSMS Gateway Pricing Control Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Sms, contentDescription = "SMS System", tint = CyberCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SMS RESELLING & GATEWAY PRICING",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = onTestSmsClick,
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Send", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("TEST SMS", fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Resell encrypted OTP & transactional SMS to users via HttpSMS gateway. 160 GSM-7 characters per page. Set wholesale cost vs retail selling price.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Wholesale Cost vs Retail Price inputs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = smsWholesaleInput,
                        onValueChange = { smsWholesaleInput = it },
                        label = { Text("Wholesale Cost (₦/page)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    OutlinedTextField(
                        value = smsRetailInput,
                        onValueChange = { smsRetailInput = it },
                        label = { Text("User Retail Price (₦/page)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricEmerald,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = smsSenderIdInput,
                        onValueChange = { smsSenderIdInput = it },
                        label = { Text("Default Sender ID") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    OutlinedTextField(
                        value = smsApiKeyInput,
                        onValueChange = { smsApiKeyInput = it },
                        label = { Text("HttpSMS API Key") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                val wholesaleVal = smsWholesaleInput.toDoubleOrNull() ?: 2.50
                val retailVal = smsRetailInput.toDoubleOrNull() ?: 4.50
                val profitMargin = if (wholesaleVal > 0) ((retailVal - wholesaleVal) / wholesaleVal * 100.0).coerceAtLeast(0.0) else 50.0

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = DarkSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = "Encrypted", tint = ElectricEmerald, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("TLS 1.3 & AES-256 Encrypted", style = MaterialTheme.typography.labelSmall.copy(color = ElectricEmerald, fontWeight = FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = "Profit: +${String.format("%.1f", profitMargin)}% (₦${String.format("%.2f", retailVal - wholesaleVal)}/p)",
                            style = MaterialTheme.typography.labelSmall.copy(color = CyberCyan, fontWeight = FontWeight.Black),
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        val wCost = smsWholesaleInput.toDoubleOrNull() ?: 2.50
                        val rPrice = smsRetailInput.toDoubleOrNull() ?: 4.50
                        val markup = if (wCost > 0) ((rPrice - wCost) / wCost * 100.0).coerceAtLeast(0.0) else 50.0
                        viewModel.updateSmsPricingConfig(
                            wholesaleCost = wCost,
                            markupPercent = markup,
                            minFloorCharge = rPrice,
                            senderId = smsSenderIdInput,
                            apiKey = smsApiKeyInput
                        )
                        smsSettingsSavedMsg = "⚡ SMS Pricing & Gateway Settings Saved!"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("SAVE SMS RESELLER CONFIGURATION", fontWeight = FontWeight.Black)
                }

                smsSettingsSavedMsg?.let { msg ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.labelSmall.copy(color = ElectricEmerald, fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // SMS PRICING & PROFIT ENGINE (ADMIN ONLY)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = DarkObsidian,
                    border = androidx.compose.foundation.BorderStroke(1.dp, GlowingAmber.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MonetizationOn,
                                contentDescription = "Profit Engine",
                                tint = GlowingAmber,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "SMS PRICING & PROFIT ENGINE",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = GlowingAmber
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val est1kRevenue = 1000 * retailVal
                        val est1kCost = 1000 * wholesaleVal
                        val est1kProfit = est1kRevenue - est1kCost
                        val est10kProfit = est1kProfit * 10

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Wholesale Base Cost / SMS", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp))
                                Text("₦${String.format("%.2f", wholesaleVal)}", style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Admin Markup Margin", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp))
                                Text("+${String.format("%.1f", profitMargin)}%", style = MaterialTheme.typography.bodySmall.copy(color = CyberCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Retail Charge to User", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp))
                                Text("₦${String.format("%.2f", retailVal)}", style = MaterialTheme.typography.bodySmall.copy(color = ElectricEmerald, fontWeight = FontWeight.Black, fontSize = 11.sp))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Net Margin Per Message", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp))
                                Text("+₦${String.format("%.2f", retailVal - wholesaleVal)}", style = MaterialTheme.typography.bodySmall.copy(color = GlowingAmber, fontWeight = FontWeight.Bold, fontSize = 11.sp))
                            }

                            HorizontalDivider(color = DarkCardBorder, modifier = Modifier.padding(vertical = 4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Est. Net Profit (1,000 SMS)", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp))
                                Text("₦${String.format("%,.2f", est1kProfit)}", style = MaterialTheme.typography.bodySmall.copy(color = ElectricEmerald, fontWeight = FontWeight.Black, fontSize = 12.sp))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Est. Net Profit (10,000 SMS)", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp))
                                Text("₦${String.format("%,.2f", est10kProfit)}", style = MaterialTheme.typography.bodySmall.copy(color = ElectricEmerald, fontWeight = FontWeight.Black, fontSize = 12.sp))
                            }
                        }
                    }
                }
            }
        }

        // 2. HTTPSMS WEBHOOK & REAL-TIME DELIVERY (DLR) TRACKING CARD
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
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
                                .background(CyberCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.MarkEmailRead, contentDescription = "SMS Webhook", tint = CyberCyan, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = "HTTPSMS DELIVERY WEBHOOK (DLR)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Real-Time Handset Delivery Status",
                                style = MaterialTheme.typography.labelSmall.copy(color = CyberCyan, fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        color = ElectricEmerald.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ElectricEmerald.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(ElectricEmerald))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("DLR ACTIVE", fontSize = 9.sp, fontWeight = FontWeight.Black, color = ElectricEmerald, maxLines = 1)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "HttpSMS posts real-time delivery reports (SENT, DELIVERED, FAILED) back to this endpoint when the recipient handset acknowledges receipt.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Webhook URL display
                Surface(
                    color = DarkObsidian,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ACTIVE HTTPSMS DLR WEBHOOK URL",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = httpSmsWebhookUrl,
                                style = MaterialTheme.typography.bodySmall.copy(color = CyberCyan, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                            )
                        }

                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(httpSmsWebhookUrl))
                                httpSmsWebhookMsg = "DLR Webhook URL Copied to Clipboard!"
                            }
                        ) {
                            Icon(imageVector = Icons.Outlined.ContentCopy, contentDescription = "Copy", tint = CyberCyan, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                httpSmsWebhookMsg?.let { msg ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.labelSmall.copy(color = ElectricEmerald, fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Delivery Stats Summary
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = DarkSurface,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Total Sent", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 9.sp))
                            Text("${httpSmsDeliveryStats.totalDispatched}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, color = TextPrimary))
                        }
                    }

                    Surface(
                        color = DarkSurface,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Delivered", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 9.sp))
                            Text("${httpSmsDeliveryStats.totalDelivered}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, color = ElectricEmerald))
                        }
                    }

                    Surface(
                        color = DarkSurface,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Pending", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 9.sp))
                            Text("${httpSmsDeliveryStats.totalPending}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, color = CyberCyan))
                        }
                    }

                    Surface(
                        color = DarkSurface,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Failed", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 9.sp))
                            Text("${httpSmsDeliveryStats.totalFailed}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, color = if (httpSmsDeliveryStats.totalFailed > 0) WarningRed else TextMuted))
                        }
                    }
                }

                if (httpSmsDeliveryLogs.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "LIVE DLR DELIVERY STREAM (${httpSmsDeliveryLogs.size})",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    httpSmsDeliveryLogs.take(10).forEach { log ->
                        val formattedTime = remember(log.timestamp) {
                            val sdf = java.text.SimpleDateFormat("HH:mm:ss • dd MMM", java.util.Locale.getDefault())
                            sdf.format(java.util.Date(log.timestamp))
                        }
                        Surface(
                            color = DarkObsidian,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "To: ${log.recipient}", style = MaterialTheme.typography.labelSmall.copy(color = TextPrimary, fontWeight = FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(text = "$formattedTime • Ref: ${log.messageId}", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }

                                Surface(
                                    color = when (log.status.uppercase()) {
                                        "DELIVERED" -> ElectricEmerald.copy(alpha = 0.2f)
                                        "FAILED" -> WarningRed.copy(alpha = 0.2f)
                                        else -> CyberCyan.copy(alpha = 0.2f)
                                    },
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = log.status.uppercase(),
                                        color = when (log.status.uppercase()) {
                                            "DELIVERED" -> ElectricEmerald
                                            "FAILED" -> WarningRed
                                            else -> CyberCyan
                                        },
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = DarkSurface,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No live SMS dispatched yet. Real-time delivery reports (DLR) and network receipts will stream here as messages are sent.",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 11.sp),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }
    }
}
