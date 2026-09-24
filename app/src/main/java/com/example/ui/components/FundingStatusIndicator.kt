package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FundingTransactionStatus
import com.example.ui.theme.*

/**
 * Reactive status indicator component for the wallet funding screen.
 * Automatically updates its UI from 'Idle' -> 'Verifying' -> 'Confirmed' or 'Failed'
 * upon receiving webhook verification and reconciliation feedback.
 */
@Composable
fun FundingStatusIndicator(
    status: FundingTransactionStatus,
    activeConfirmationCode: String,
    onRetryOrCheckNow: () -> Unit = {},
    onReportAlert: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Pulse animation for radar & verifying state
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_radar")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotationAngle"
    )

    AnimatedContent(
        targetState = status,
        transitionSpec = {
            (fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.96f)) togetherWith
                    (fadeOut(animationSpec = tween(200)) + scaleOut(targetScale = 0.96f))
        },
        label = "FundingStatusContentTransition",
        modifier = modifier.fillMaxWidth()
    ) { currentStatus ->
        when (currentStatus) {
            is FundingTransactionStatus.Idle -> {
                IdleStatusCard(
                    activeConfirmationCode = activeConfirmationCode,
                    pulseAlpha = pulseAlpha
                )
            }
            is FundingTransactionStatus.Verifying -> {
                VerifyingStatusCard(
                    pulseAlpha = pulseAlpha,
                    rotationAngle = rotationAngle
                )
            }
            is FundingTransactionStatus.Confirmed -> {
                ConfirmedStatusCard(
                    amount = currentStatus.amount,
                    reference = currentStatus.reference,
                    source = currentStatus.source
                )
            }
            is FundingTransactionStatus.Failed -> {
                FailedStatusCard(
                    reason = currentStatus.reason,
                    onRetry = onRetryOrCheckNow,
                    onReportAlert = onReportAlert
                )
            }
        }
    }
}

@Composable
private fun IdleStatusCard(
    activeConfirmationCode: String,
    pulseAlpha: Float
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.2.dp, CyberCyan.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("funding_status_idle")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
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
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(CyberCyan.copy(alpha = pulseAlpha))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "WEBHOOK LISTENER ACTIVE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = CyberCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 0.5.sp
                        )
                    )
                }

                Surface(
                    color = CyberCyan.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Sensors,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Awaiting Transfer",
                            color = CyberCyan,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Send transfer with PIN '$activeConfirmationCode' in remarks. Incoming webhook will automatically settle and credit your wallet instantly.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextPrimary.copy(alpha = 0.88f),
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp
                )
            )
        }
    }
}

@Composable
private fun VerifyingStatusCard(
    pulseAlpha: Float,
    rotationAngle: Float
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF071B26)),
        border = BorderStroke(1.5.dp, CyberCyan.copy(alpha = pulseAlpha)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("funding_status_verifying")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
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
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(CyberCyan.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Radar,
                            contentDescription = "Verifying",
                            tint = CyberCyan,
                            modifier = Modifier
                                .size(17.dp)
                                .rotate(rotationAngle)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "VERIFYING TRANSFER...",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = CyberCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                letterSpacing = 0.5.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = "Confirming payment with bank...",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }

                Surface(
                    color = CyberCyan.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "VERIFYING",
                        color = CyberCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Animated Gradient Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(DarkObsidian)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(pulseAlpha)
                        .background(CyberCyan)
                )
            }
        }
    }
}

@Composable
private fun ConfirmedStatusCard(
    amount: Double,
    reference: String,
    source: String
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF062217)),
        border = BorderStroke(1.5.dp, ElectricEmerald),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("funding_status_confirmed")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
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
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(ElectricEmerald.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Confirmed",
                            tint = ElectricEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "PAYMENT CONFIRMED",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = ElectricEmerald,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                letterSpacing = 0.5.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = "+₦${String.format(java.util.Locale.US, "%,.2f", amount)} Credited to Wallet",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        )
                    }
                }

                Surface(
                    color = ElectricEmerald.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, ElectricEmerald.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "RECONCILED",
                        color = ElectricEmerald,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Verified via $source (Ref: $reference). Settlement complete.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextPrimary.copy(alpha = 0.85f),
                    fontSize = 12.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FailedStatusCard(
    reason: String,
    onRetry: () -> Unit,
    onReportAlert: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF220A0A)),
        border = BorderStroke(1.2.dp, WarningRed.copy(alpha = 0.6f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("funding_status_failed")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = "Failed",
                        tint = WarningRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "TRANSFER NOT DETECTED YET",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = WarningRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            letterSpacing = 0.5.sp
                        )
                    )
                }

                Surface(
                    color = WarningRed.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, WarningRed.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = "UNRESOLVED",
                        color = WarningRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = reason.ifBlank { "The transfer was not detected before the countdown expired, or bank settlement is delayed." },
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextPrimary.copy(alpha = 0.88f),
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onRetry,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, GlowingAmber),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = GlowingAmber,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("RETRY CHECK", color = GlowingAmber, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onReportAlert,
                    colors = ButtonDefaults.buttonColors(containerColor = WarningRed, contentColor = Color.White),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ALERT ADMIN", fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}
