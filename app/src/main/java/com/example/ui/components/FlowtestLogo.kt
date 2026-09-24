package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberCyanVariant
import com.example.ui.theme.TextPrimary

enum class LogoOrientation {
    VERTICAL,
    HORIZONTAL
}

/**
 * Flowtest Official Brand Logo Composable.
 * Features a circular emblem with an embossed security shield in the app's primary color (CyberCyan),
 * paired with the clean lowercase "flowtest" wordmark, on a completely transparent background.
 */
@Composable
fun FlowtestLogo(
    modifier: Modifier = Modifier,
    emblemSize: Dp = 72.dp,
    showWordmark: Boolean = true,
    fontSize: TextUnit = 26.sp,
    textColor: Color = TextPrimary,
    primaryColor: Color = CyberCyan,
    orientation: LogoOrientation = LogoOrientation.VERTICAL,
    enableGlowPulse: Boolean = false,
    animateRotationOnLoad: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "LogoGlowTransition")
    val pulseAlpha by if (enableGlowPulse) {
        infiniteTransition.animateFloat(
            initialValue = 0.25f,
            targetValue = 0.55f,
            animationSpec = infiniteRepeatable(
                animation = tween(1400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "LogoPulseAlpha"
        )
    } else {
        rememberUpdatedState(0.35f)
    }

    when (orientation) {
        LogoOrientation.VERTICAL -> {
            Column(
                modifier = modifier,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                FlowtestEmblem(
                    size = emblemSize,
                    primaryColor = primaryColor,
                    glowAlpha = pulseAlpha,
                    animateRotationOnLoad = animateRotationOnLoad
                )

                if (showWordmark) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "flowtest",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            color = textColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = fontSize,
                            letterSpacing = (-0.5).sp,
                            fontFamily = FontFamily.SansSerif
                        )
                    )
                }
            }
        }
        LogoOrientation.HORIZONTAL -> {
            Row(
                modifier = modifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                FlowtestEmblem(
                    size = emblemSize,
                    primaryColor = primaryColor,
                    glowAlpha = pulseAlpha,
                    animateRotationOnLoad = animateRotationOnLoad
                )

                if (showWordmark) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "flowtest",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = textColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = fontSize,
                            letterSpacing = (-0.5).sp,
                            fontFamily = FontFamily.SansSerif
                        )
                    )
                }
            }
        }
    }
}

/**
 * Pure vector circular shield emblem rendered dynamically with Canvas.
 * Fully transparent background with physical bevel lighting, dual-layer shield contours,
 * and primary brand color gradient.
 */
@Composable
fun FlowtestEmblem(
    size: Dp = 72.dp,
    primaryColor: Color = CyberCyan,
    glowAlpha: Float = 0.35f,
    animateRotationOnLoad: Boolean = true,
    modifier: Modifier = Modifier
) {
    val highlightColor = Color(0xFFE0F7FA)
    val darkBevelColor = Color(0xFF045366)
    val deepShadeColor = Color(0xFF02323E)
    val bgObsidianBlack = Color(0xFF07090E)
    val bgTealDeep = Color(0xFF003B46)

    // Entrance rotation animation (turns -360° and settles smoothly at 0°)
    val rotationAnim = remember { Animatable(if (animateRotationOnLoad) -360f else 0f) }
    if (animateRotationOnLoad) {
        LaunchedEffect(Unit) {
            rotationAnim.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = 1100,
                    easing = FastOutSlowInEasing
                )
            )
        }
    }

    Canvas(modifier = modifier.size(size).rotate(rotationAnim.value)) {
        val w = this.size.width
        val h = this.size.height
        val center = Offset(w / 2f, h / 2f)
        val radius = (w.coerceAtMost(h) / 2f) * 0.88f

        // 1. Transparent ambient cyan glow caustics behind the emblem
        if (glowAlpha > 0f) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = glowAlpha),
                        primaryColor.copy(alpha = glowAlpha * 0.3f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius * 1.35f
                ),
                radius = radius * 1.35f,
                center = center
            )
        }

        // 2. Circular Token with signature DIAGONAL split (Black top-right, CyberCyan bottom-left)
        val tokenCirclePath = Path().apply {
            addOval(Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius))
        }

        // Bottom-Left diagonal triangle path extending past the circle
        val diagonalBottomLeft = Path().apply {
            moveTo(center.x - radius * 1.5f, center.y - radius * 1.5f)
            lineTo(center.x + radius * 1.5f, center.y + radius * 1.5f)
            lineTo(center.x - radius * 1.5f, center.y + radius * 1.5f)
            close()
        }

        clipPath(path = tokenCirclePath) {
            // A) Base/Top-Right half: Deep Obsidian Black
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        bgObsidianBlack,
                        Color(0xFF050A10)
                    ),
                    start = Offset(center.x, center.y - radius),
                    end = Offset(center.x + radius, center.y + radius)
                )
            )

            // B) Bottom-Left diagonal half: Vibrant Primary CyberCyan
            drawPath(
                path = diagonalBottomLeft,
                brush = Brush.linearGradient(
                    colors = listOf(
                        CyberCyanVariant,
                        primaryColor,
                        darkBevelColor
                    ),
                    start = Offset(center.x - radius * 0.9f, center.y),
                    end = Offset(center.x, center.y + radius * 0.9f)
                )
            )

            // C) Diagonal Seam Line with crisp specular highlight
            drawLine(
                color = Color.White.copy(alpha = 0.65f),
                start = Offset(center.x - radius * 0.707f, center.y - radius * 0.707f),
                end = Offset(center.x + radius * 0.707f, center.y + radius * 0.707f),
                strokeWidth = radius * 0.028f
            )
        }

        // 3. Specular rim highlight around the circle edge
        drawCircle(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.85f),
                    highlightColor.copy(alpha = 0.45f),
                    Color.Transparent,
                    primaryColor.copy(alpha = 0.50f)
                ),
                start = Offset(center.x - radius, center.y - radius),
                end = Offset(center.x + radius, center.y + radius)
            ),
            radius = radius,
            center = center,
            style = Stroke(width = radius * 0.040f)
        )

        // 4. Subtle inner drop-shadow ring for 3D depth
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.Transparent,
                    deepShadeColor.copy(alpha = 0.35f)
                ),
                center = center,
                radius = radius
            ),
            radius = radius * 0.96f,
            center = center
        )

        // 5. Outer Shield (Embossed boundary contour)
        val outerShield = Path().apply {
            val sTop = center.y - radius * 0.52f
            val sBottom = center.y + radius * 0.58f
            val sLeft = center.x - radius * 0.42f
            val sRight = center.x + radius * 0.42f
            val sMidY = center.y + radius * 0.08f

            moveTo(center.x, sTop)
            // Top right shoulder curve
            cubicTo(
                center.x + radius * 0.12f, sTop + radius * 0.04f,
                sRight - radius * 0.10f, sTop + radius * 0.05f,
                sRight, sTop + radius * 0.08f
            )
            // Right vertical descent
            lineTo(sRight, sMidY)
            // Right lower curve to bottom tip
            cubicTo(
                sRight, sMidY + radius * 0.30f,
                center.x + radius * 0.20f, sBottom - radius * 0.06f,
                center.x, sBottom
            )
            // Left lower curve from bottom tip
            cubicTo(
                center.x - radius * 0.20f, sBottom - radius * 0.06f,
                sLeft, sMidY + radius * 0.30f,
                sLeft, sMidY
            )
            // Left vertical ascent
            lineTo(sLeft, sTop + radius * 0.08f)
            // Top left shoulder curve back to center top
            cubicTo(
                sLeft + radius * 0.10f, sTop + radius * 0.05f,
                center.x - radius * 0.12f, sTop + radius * 0.04f,
                center.x, sTop
            )
            close()
        }

        // Draw Outer Shield Inset Shadow (creates the sculpted bevel rim)
        drawPath(
            path = outerShield,
            brush = Brush.linearGradient(
                colors = listOf(
                    deepShadeColor,
                    darkBevelColor,
                    primaryColor.copy(alpha = 0.85f)
                ),
                start = Offset(center.x, center.y - radius * 0.5f),
                end = Offset(center.x, center.y + radius * 0.6f)
            )
        )

        // 6. Inner Shield (Raised face matching the reference image)
        val innerShield = Path().apply {
            val scaleInner = 0.82f
            val sTop = center.y - radius * 0.52f * scaleInner + radius * 0.03f
            val sBottom = center.y + radius * 0.58f * scaleInner + radius * 0.02f
            val sLeft = center.x - radius * 0.42f * scaleInner
            val sRight = center.x + radius * 0.42f * scaleInner
            val sMidY = center.y + radius * 0.08f * scaleInner

            moveTo(center.x, sTop)
            cubicTo(
                center.x + radius * 0.10f * scaleInner, sTop + radius * 0.035f,
                sRight - radius * 0.08f * scaleInner, sTop + radius * 0.045f,
                sRight, sTop + radius * 0.07f
            )
            lineTo(sRight, sMidY)
            cubicTo(
                sRight, sMidY + radius * 0.26f * scaleInner,
                center.x + radius * 0.18f * scaleInner, sBottom - radius * 0.05f,
                center.x, sBottom
            )
            cubicTo(
                center.x - radius * 0.18f * scaleInner, sBottom - radius * 0.05f,
                sLeft, sMidY + radius * 0.26f * scaleInner,
                sLeft, sMidY
            )
            lineTo(sLeft, sTop + radius * 0.07f)
            cubicTo(
                sLeft + radius * 0.08f * scaleInner, sTop + radius * 0.045f,
                center.x - radius * 0.10f * scaleInner, sTop + radius * 0.035f,
                center.x, sTop
            )
            close()
        }

        // Draw Inner Shield Face with bright CyberCyan sheen
        drawPath(
            path = innerShield,
            brush = Brush.linearGradient(
                colors = listOf(
                    CyberCyanVariant,
                    primaryColor,
                    darkBevelColor
                ),
                start = Offset(center.x - radius * 0.3f, center.y - radius * 0.45f),
                end = Offset(center.x + radius * 0.3f, center.y + radius * 0.55f)
            )
        )

        // 7. Bevel Hairline along inner shield
        drawPath(
            path = innerShield,
            brush = Brush.linearGradient(
                colors = listOf(
                    highlightColor.copy(alpha = 0.70f),
                    Color.White.copy(alpha = 0.25f),
                    Color.Transparent
                ),
                start = Offset(center.x - radius * 0.3f, center.y - radius * 0.45f),
                end = Offset(center.x + radius * 0.3f, center.y + radius * 0.55f)
            ),
            style = Stroke(width = radius * 0.025f)
        )
    }
}
