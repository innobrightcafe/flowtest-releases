package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.util.AppUpdateInfo
import com.example.util.AppUpdateManager
import com.example.util.UpdateStatus

/**
 * UpdateNotificationBanner
 * Renders an elevated, interactive in-app notification popup banner at the top of the screen
 * whenever a new FlowTest update is available or ready to install.
 */
@Composable
fun UpdateNotificationBanner(
    status: UpdateStatus,
    onOpenUpdateDialog: () -> Unit,
    onDismissBanner: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isReady = status is UpdateStatus.ReadyToInstall
    val isAvailable = status is UpdateStatus.Available

    if (!isReady && !isAvailable) return

    val info: AppUpdateInfo = when (status) {
        is UpdateStatus.Available -> status.info
        is UpdateStatus.ReadyToInstall -> status.info
        else -> null
    } ?: return

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .shadow(16.dp, RoundedCornerShape(16.dp), spotColor = if (isReady) ElectricEmerald else CyberCyan)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onOpenUpdateDialog() },
        color = DarkSurfaceElevated,
        border = BorderStroke(
            1.2.dp,
            Brush.horizontalGradient(
                if (isReady) {
                    listOf(ElectricEmerald, ElectricEmerald.copy(alpha = 0.4f), DarkCardBorder)
                } else {
                    listOf(CyberCyan, CyberCyanVariant.copy(alpha = 0.4f), DarkCardBorder)
                }
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Glowing Indicator Icon
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        (if (isReady) ElectricEmerald else CyberCyan).copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            (if (isReady) ElectricEmerald else CyberCyan).copy(alpha = pulseAlpha * 0.25f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isReady) Icons.Default.Bolt else Icons.Default.SystemUpdate,
                        contentDescription = "Update Icon",
                        tint = if (isReady) ElectricEmerald else CyberCyanVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Text column
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (isReady) "Update Ready to Install" else "New Update Available",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = TextPrimary,
                            fontSize = 13.5.sp
                        )
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = (if (isReady) ElectricEmerald else CyberCyan).copy(alpha = 0.2f),
                        border = BorderStroke(0.8.dp, if (isReady) ElectricEmerald.copy(alpha = 0.5f) else CyberCyan.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "v${info.latestVersionName}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isReady) ElectricEmerald else CyberCyanVariant,
                                fontSize = 10.sp
                            ),
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = if (isReady) {
                        "Tap to install v${info.latestVersionName} without data loss."
                    } else {
                        info.releaseNotes.ifBlank { "Performance improvements and security updates." }
                    },
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        fontSize = 11.5.sp,
                        lineHeight = 15.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action Button
            Button(
                onClick = {
                    if (isReady && status is UpdateStatus.ReadyToInstall) {
                        AppUpdateManager.installUpdate(context, status.apkFile)
                    } else {
                        onOpenUpdateDialog()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberCyan,
                    contentColor = DarkObsidian
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Icon(
                    imageVector = if (isReady) Icons.Default.Bolt else Icons.Default.Download,
                    contentDescription = null,
                    tint = DarkObsidian,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isReady) "Install" else "Update",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 11.5.sp
                    )
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Close / Dismiss X
            IconButton(
                onClick = onDismissBanner,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss Banner",
                    tint = TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
