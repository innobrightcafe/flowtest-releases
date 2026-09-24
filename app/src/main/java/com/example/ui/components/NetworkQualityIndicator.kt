package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SignalWifiBad
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.util.NetworkQuality
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ElectricEmerald
import com.example.ui.theme.GlowingAmber
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

private val OfflineRed = Color(0xFFEF4444)

/**
 * UI/UX Pro Max animated indicator for Low or No Network states.
 * Informs the user in real time when connection is weak or offline,
 * clarifying that transactions will be queued and sent to Admin for completion.
 */
@Composable
fun NetworkQualityIndicator(
    networkQuality: NetworkQuality,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = networkQuality != NetworkQuality.ONLINE,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        val (bgTint, borderTint, iconTint, title, description, icon) = when (networkQuality) {
            NetworkQuality.NO_NETWORK -> NetworkNoticeConfig(
                bgTint = OfflineRed.copy(alpha = 0.12f),
                borderTint = OfflineRed.copy(alpha = 0.6f),
                iconTint = OfflineRed,
                title = "No Internet Connection",
                description = "Purchases will be saved offline and sent to Admin for fulfillment.",
                icon = Icons.Default.WifiOff
            )
            NetworkQuality.LOW_NETWORK -> NetworkNoticeConfig(
                bgTint = GlowingAmber.copy(alpha = 0.12f),
                borderTint = GlowingAmber.copy(alpha = 0.5f),
                iconTint = GlowingAmber,
                title = "Low / Unstable Network",
                description = "Orders may take longer or queue for Admin reconciliation.",
                icon = Icons.Default.SignalWifiBad
            )
            NetworkQuality.ONLINE -> NetworkNoticeConfig(
                bgTint = ElectricEmerald.copy(alpha = 0.10f),
                borderTint = ElectricEmerald.copy(alpha = 0.4f),
                iconTint = ElectricEmerald,
                title = "Network Connected",
                description = "Online switch ready.",
                icon = Icons.Default.Wifi
            )
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            shape = RoundedCornerShape(12.dp),
            color = DarkSurfaceElevated,
            border = BorderStroke(1.dp, borderTint)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bgTint)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(iconTint.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = iconTint,
                            fontSize = 12.sp
                        )
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextMuted,
                            fontSize = 11.sp,
                            lineHeight = 14.sp
                        )
                    )
                }
            }
        }
    }
}

private data class NetworkNoticeConfig(
    val bgTint: Color,
    val borderTint: Color,
    val iconTint: Color,
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
