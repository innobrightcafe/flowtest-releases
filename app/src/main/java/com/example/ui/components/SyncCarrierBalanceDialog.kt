package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncCarrierBalanceDialog(
    currentRemainingMb: Double,
    currentPurchasedMb: Double,
    onDismiss: () -> Unit,
    onCalibrateBalance: (balanceMb: Double, carrier: String) -> Unit
) {
    val context = LocalContext.current

    var selectedCarrier by remember { mutableStateOf("MTN") }
    val initialGb = if (currentRemainingMb > 0.0) {
        val gb = currentRemainingMb / 1024.0
        if (Math.abs(gb - Math.round(gb)) < 0.005) {
            "${Math.round(gb)}"
        } else {
            String.format(java.util.Locale.US, "%.2f", gb).trimEnd('0').trimEnd('.')
        }
    } else {
        "2.6"
    }

    var inputBalanceText by remember { mutableStateOf(initialGb) }
    var isGbUnit by remember { mutableStateOf(true) }

    val carriers = listOf("MTN", "Airtel", "Glo", "9mobile")

    val carrierUssdMap = mapOf(
        "MTN" to ("*323#" to "Or text '2' to 312"),
        "Airtel" to ("*323#" to "Or dial *140#"),
        "Glo" to ("*323#" to "Or dial *127*0#"),
        "9mobile" to ("*323#" to "Or dial *228#")
    )

    val quickPresets = listOf("1.0", "1.5", "2.0", "2.6", "3.0", "5.0", "10.0")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
            border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
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
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(CyberCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Sync Carrier Data Balance",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    fontSize = 15.sp
                                )
                            )
                            Text(
                                text = "Calibrate with myMTN or SIM balance",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Carrier Selector
                Text(
                    text = "Select SIM Provider:",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    carriers.forEach { carrier ->
                        val isSelected = selectedCarrier == carrier
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) CyberCyan.copy(alpha = 0.2f) else DarkSurface,
                            border = BorderStroke(
                                if (isSelected) 1.5.dp else 1.dp,
                                if (isSelected) CyberCyan else DarkCardBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedCarrier = carrier }
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = carrier,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isSelected) CyberCyan else TextPrimary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Check Balance via USSD banner
                val ussdInfo = carrierUssdMap[selectedCarrier] ?: ("*323#" to "")
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurface,
                    border = BorderStroke(0.5.dp, DarkCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Check exact ${selectedCarrier} balance",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            )
                            Text(
                                text = "${ussdInfo.first} • ${ussdInfo.second}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                            )
                        }

                        Button(
                            onClick = {
                                try {
                                    val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                                        data = Uri.parse("tel:" + Uri.encode(ussdInfo.first))
                                    }
                                    context.startActivity(dialIntent)
                                } catch (e: Exception) {
                                    // Ignore if dialer not available
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GlowingAmber.copy(alpha = 0.2f),
                                contentColor = GlowingAmber
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Dial ${ussdInfo.first}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Enter Current Balance field
                Text(
                    text = "Current Active Balance on ${selectedCarrier}:",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputBalanceText,
                        onValueChange = { inputBalanceText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        placeholder = { Text("e.g. 2.6", color = TextMuted) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = DarkSurface,
                            unfocusedContainerColor = DarkSurface
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Unit selector (GB / MB)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DarkSurface,
                        border = BorderStroke(1.dp, DarkCardBorder),
                        modifier = Modifier.height(52.dp)
                    ) {
                        Row(modifier = Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isGbUnit) CyberCyan else Color.Transparent,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { isGbUnit = true }
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    "GB",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (isGbUnit) TextPrimary else TextMuted
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (!isGbUnit) CyberCyan else Color.Transparent,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { isGbUnit = false }
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    "MB",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (!isGbUnit) TextPrimary else TextMuted
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quick Presets
                Text(
                    text = "Quick Presets:",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 10.sp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickPresets.forEach { preset ->
                        val isCurrent = inputBalanceText == preset
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isCurrent) CyberCyan.copy(alpha = 0.25f) else DarkSurface,
                            border = BorderStroke(
                                1.dp,
                                if (isCurrent) CyberCyan else DarkCardBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    inputBalanceText = preset
                                    isGbUnit = true
                                }
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${preset}G",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isCurrent) CyberCyan else TextPrimary,
                                        fontWeight = if (isCurrent) FontWeight.Black else FontWeight.Medium,
                                        fontSize = 10.5.sp
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Real-time accounting explanation
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = ElectricEmerald.copy(alpha = 0.1f),
                    border = BorderStroke(0.5.dp, ElectricEmerald.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = ElectricEmerald,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Flow will track and subtract mobile app consumption accurately in real time from this starting balance.",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextSecondary,
                                fontSize = 10.sp,
                                lineHeight = 13.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, DarkCardBorder)
                    ) {
                        Text("Cancel", color = TextMuted)
                    }

                    Button(
                        onClick = {
                            val parsed = inputBalanceText.toDoubleOrNull() ?: 2.6
                            val balanceMb = if (isGbUnit) parsed * 1024.0 else parsed
                            onCalibrateBalance(balanceMb, selectedCarrier)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1.5f),
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = TextPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Calibrate Live Balance", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                    }
                }
            }
        }
    }
}
