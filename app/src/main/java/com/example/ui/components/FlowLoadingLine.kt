package com.example.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkObsidian
import com.example.ui.theme.DarkSurfaceElevated

/**
 * Standardized High-Precision Line Loader for FlowTest (UI/UX Pro Max).
 * Used consistently across Funding Drawer, App Updater, Sheets, Cards, and Full Screens.
 */
@Composable
fun FlowLoadingLine(
    modifier: Modifier = Modifier,
    progress: Float? = null,
    color: Color = CyberCyan,
    trackColor: Color = DarkSurfaceElevated,
    height: Dp = 4.dp
) {
    val clippedModifier = modifier
        .height(height)
        .clip(RoundedCornerShape(height / 2))

    if (progress != null) {
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = clippedModifier,
            color = color,
            trackColor = trackColor
        )
    } else {
        LinearProgressIndicator(
            modifier = clippedModifier,
            color = color,
            trackColor = trackColor
        )
    }
}

/**
 * Compact Line Loader for Buttons and compact containers.
 * Replaces round circular loaders inside action buttons with a sleek loading line.
 */
@Composable
fun FlowButtonLoadingLine(
    modifier: Modifier = Modifier,
    color: Color = DarkObsidian,
    trackColor: Color = color.copy(alpha = 0.25f),
    width: Dp = 42.dp,
    height: Dp = 3.dp
) {
    LinearProgressIndicator(
        modifier = modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(height / 2)),
        color = color,
        trackColor = trackColor
    )
}
