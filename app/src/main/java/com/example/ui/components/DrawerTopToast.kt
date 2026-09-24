package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay

enum class DrawerToastType {
    ERROR,
    SUCCESS,
    WARNING,
    INFO
}

data class DrawerToastMessage(
    val message: String,
    val title: String? = null,
    val type: DrawerToastType = DrawerToastType.INFO,
    val durationMs: Long = 6500L,
    val canCopy: Boolean = true,
    val id: Long = System.currentTimeMillis()
)

/**
 * TopDrawerNotificationBanner
 * Renders a high-visibility, elevated notification banner at the very TOP of a drawer,
 * modal bottom sheet, or screen. Guaranteed to be visible above all scrollable content and dialogs.
 */
@Composable
fun TopDrawerNotificationBanner(
    toast: DrawerToastMessage?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(toast?.id) {
        copied = false
        if (toast != null && toast.durationMs > 0) {
            delay(toast.durationMs)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = toast != null,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(320)
        ) + fadeIn(animationSpec = tween(320)),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(250)
        ) + fadeOut(animationSpec = tween(250)),
        modifier = modifier
            .fillMaxWidth()
            .zIndex(9999f)
    ) {
        if (toast != null) {
            val (bgColor1, bgColor2, borderColor, iconColor, iconVector, defaultTitle) = when (toast.type) {
                DrawerToastType.ERROR -> HexTuple(
                    Color(0xFF2E090F),
                    Color(0xFF190306),
                    Color(0xFFFF3B30),
                    Color(0xFFFF5252),
                    Icons.Filled.Warning,
                    "Order Notice"
                )
                DrawerToastType.SUCCESS -> HexTuple(
                    Color(0xFF042416),
                    Color(0xFF01140B),
                    Color(0xFF00E676),
                    Color(0xFF00E676),
                    Icons.Filled.CheckCircle,
                    "Order Activated"
                )
                DrawerToastType.WARNING -> HexTuple(
                    Color(0xFF291C05),
                    Color(0xFF170E02),
                    Color(0xFFFFB300),
                    Color(0xFFFFC107),
                    Icons.Filled.HourglassEmpty,
                    "Network Alert"
                )
                DrawerToastType.INFO -> HexTuple(
                    Color(0xFF0A1828),
                    Color(0xFF040B14),
                    Color(0xFF00E5FF),
                    Color(0xFF00E5FF),
                    Icons.Filled.Info,
                    "Gateway Status"
                )
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .shadow(16.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                color = Color.Transparent,
                border = BorderStroke(1.2.dp, borderColor)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(bgColor1, bgColor2)
                            )
                        )
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Status Icon Badge
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(iconColor.copy(alpha = 0.18f))
                                .border(0.8.dp, iconColor.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = iconVector,
                                contentDescription = null,
                                tint = iconColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Text content
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = toast.title ?: defaultTitle,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    letterSpacing = 0.3.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = toast.message,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.White.copy(alpha = 0.92f),
                                    fontSize = 11.5.sp,
                                    lineHeight = 16.sp
                                ),
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (copied) {
                                Text(
                                    text = "✓ Copied to clipboard",
                                    color = Color(0xFF00E676),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Action Buttons
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (toast.canCopy) {
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(toast.message))
                                        copied = true
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ContentCopy,
                                        contentDescription = "Copy message",
                                        tint = Color.White.copy(alpha = 0.7f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Dismiss",
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class HexTuple(
    val bg1: Color,
    val bg2: Color,
    val border: Color,
    val iconColor: Color,
    val icon: ImageVector,
    val defaultTitle: String
)
