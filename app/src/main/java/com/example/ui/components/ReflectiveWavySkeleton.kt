package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberCyanVariant
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkObsidian
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ElectricEmerald
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * High-performance UI UX Pro Max reflective wavy shimmer brush.
 * Sweeps luminous CyberCyan light and subtle white glints across dark obsidian surfaces,
 * creating a futuristic, fluid water-wave reflective effect.
 */
@Composable
fun rememberReflectiveWavyBrush(
    durationMillis: Int = 1350,
    travelDistance: Float = 1600f
): Brush {
    val infiniteTransition = rememberInfiniteTransition(label = "wavy_shimmer_transition")
    val offset by infiniteTransition.animateFloat(
        initialValue = -travelDistance * 0.4f,
        targetValue = travelDistance,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavy_offset"
    )

    // Vibrant layered reflection: Elevated dark core -> CyberCyan glow -> Bright white reflection crest -> CyberCyan fade
    return Brush.linearGradient(
        colors = listOf(
            DarkSurfaceElevated,
            DarkSurfaceElevated.copy(alpha = 0.95f),
            CyberCyan.copy(alpha = 0.14f),
            CyberCyanVariant.copy(alpha = 0.32f),
            Color.White.copy(alpha = 0.28f), // Luminous specular crest
            CyberCyanVariant.copy(alpha = 0.32f),
            CyberCyan.copy(alpha = 0.14f),
            DarkSurfaceElevated.copy(alpha = 0.95f),
            DarkSurfaceElevated
        ),
        start = Offset(x = offset, y = offset * 0.35f),
        end = Offset(x = offset + 520f, y = (offset + 520f) * 0.35f)
    )
}

/**
 * Base reflective skeleton bone component with custom shape and dimensions.
 */
@Composable
fun ReflectiveSkeletonBone(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp),
    height: Dp = 16.dp,
    width: Dp? = null
) {
    val brush = rememberReflectiveWavyBrush()
    val widthMod = if (width != null) modifier.width(width) else modifier.fillMaxWidth()

    Box(
        modifier = widthMod
            .height(height)
            .clip(shape)
            .background(brush)
            .border(0.5.dp, CyberCyan.copy(alpha = 0.12f), shape)
    )
}

/**
 * Reflective Wavy Skeleton Card that precisely mirrors the layout and dimensions of [DataBundleCard].
 */
@Composable
fun ReflectiveBundleCardSkeleton(
    modifier: Modifier = Modifier
) {
    val brush = rememberReflectiveWavyBrush()
    val infiniteTransition = rememberInfiniteTransition(label = "card_pulse")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "border_alpha"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = DarkSurface,
        border = BorderStroke(1.dp, CyberCyan.copy(alpha = borderAlpha)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Top Row: Logo Bone + Title Bone + Price Bone
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Telco circular logo bone
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(brush)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    // Bundle title bone
                    ReflectiveSkeletonBone(
                        modifier = Modifier.weight(1f),
                        height = 16.dp,
                        shape = RoundedCornerShape(6.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Price tag bone
                ReflectiveSkeletonBone(
                    width = 68.dp,
                    height = 20.dp,
                    shape = RoundedCornerShape(6.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Description bone (two lines of fluid skeleton)
            ReflectiveSkeletonBone(
                height = 12.dp,
                shape = RoundedCornerShape(4.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            ReflectiveSkeletonBone(
                width = 180.dp,
                height = 12.dp,
                shape = RoundedCornerShape(4.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Validity pill bone
            ReflectiveSkeletonBone(
                width = 130.dp,
                height = 14.dp,
                shape = RoundedCornerShape(6.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Bottom row action buttons skeleton
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Info button bone
                ReflectiveSkeletonBone(
                    width = 96.dp,
                    height = 32.dp,
                    shape = RoundedCornerShape(50.dp)
                )

                // Buy Now button bone
                ReflectiveSkeletonBone(
                    width = 90.dp,
                    height = 32.dp,
                    shape = RoundedCornerShape(50.dp)
                )
            }
        }
    }
}

/**
 * Full Reflective Wavy Skeleton Loader for bundle lists.
 * Includes a live sync status bar with active spinning carrier indicator and retry options.
 */
@Composable
fun ReflectiveBundleListSkeleton(
    modifier: Modifier = Modifier,
    count: Int = 3,
    statusText: String = "Syncing carrier data plans...",
    subText: String = "Negotiating available routes with upstream network",
    onRetrySync: (() -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Sync Status Banner Card
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = DarkSurfaceElevated.copy(alpha = 0.85f),
            border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CyberCyan.copy(alpha = 0.15f))
                            .border(1.dp, CyberCyan.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Syncing",
                            tint = CyberCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 13.sp
                            )
                        )
                        Text(
                            text = subText,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowLoadingLine(
                            modifier = Modifier.fillMaxWidth(0.9f),
                            color = CyberCyan,
                            trackColor = DarkObsidian,
                            height = 3.dp
                        )
                    }
                }

                if (onRetrySync != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CyberCyan.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onRetrySync() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Retry Sync",
                                tint = CyberCyan,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Sync",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = CyberCyan,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }
            }
        }

        // Stacked reflective wavy bundle card skeletons
        repeat(count) {
            ReflectiveBundleCardSkeleton()
        }
    }
}
