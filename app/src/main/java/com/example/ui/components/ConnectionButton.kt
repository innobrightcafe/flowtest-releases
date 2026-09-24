package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.vpn.VpnState
import com.example.ui.theme.*

@Composable
fun ConnectionButton(
    vpnState: VpnState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isConnected = vpnState == VpnState.CONNECTED
    val isConnecting = vpnState == VpnState.CONNECTING

    // Infinite breathing animation for connecting or active state
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isConnected) 1.08f else if (isConnecting) 1.15f else 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isConnecting) 600 else 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = if (isConnected) 0.6f else if (isConnecting) 0.8f else 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isConnecting) 600 else 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    val mainColor = when (vpnState) {
        VpnState.CONNECTED -> CyberCyan
        VpnState.CONNECTING -> GlowingAmber
        VpnState.DISCONNECTED -> CyberCyan
        VpnState.DISCONNECTING -> WarningRed
    }

    Box(
        modifier = modifier
            .size(170.dp)
            .testTag("vpn_connect_button_container"),
        contentAlignment = Alignment.Center
    ) {
        // Outer Pulsing Neon Glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(pulseScale)
                .clip(CircleShape)
                .background(mainColor.copy(alpha = pulseAlpha * 0.25f))
        )

        // Outer Ring Border
        Box(
            modifier = Modifier
                .size(138.dp)
                .clip(CircleShape)
                .border(2.dp, mainColor.copy(alpha = 0.4f), CircleShape)
                .background(DarkSurfaceElevated.copy(alpha = 0.8f)),
            contentAlignment = Alignment.Center
        ) {
            // Main Interactive Circle Button
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(mainColor)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick
                    )
                    .testTag("vpn_power_button"),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (isConnected) Icons.Default.Lock else if (isConnecting) Icons.Default.PowerSettingsNew else Icons.Default.LockOpen,
                        contentDescription = "Power VPN",
                        tint = DarkObsidian,
                        modifier = Modifier.size(38.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = when (vpnState) {
                            VpnState.CONNECTED -> "SECURED"
                            VpnState.CONNECTING -> "TUNNELING"
                            VpnState.DISCONNECTED -> "TAP TO CONNECT"
                            VpnState.DISCONNECTING -> "STOPPING"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            fontSize = 9.sp
                        ),
                        color = DarkObsidian
                    )
                }
            }
        }
    }
}
