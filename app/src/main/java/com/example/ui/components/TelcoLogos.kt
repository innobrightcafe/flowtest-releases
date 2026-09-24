package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * High-quality custom rendered Telco Logos for MTN, Airtel, Glo, 9Mobile (T2), Vitel
 */
@Composable
fun TelcoLogo(
    network: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    val normalized = network.trim().uppercase()
    when {
        normalized.contains("MTN") -> MtnLogo(size = size, modifier = modifier)
        normalized.contains("AIRTEL") -> AirtelLogo(size = size, modifier = modifier)
        normalized.contains("GLO") -> GloLogo(size = size, modifier = modifier)
        normalized.contains("9MOBILE") || normalized.contains("T2") || normalized.contains("9MOB") -> NineMobileLogo(size = size, modifier = modifier)
        normalized.contains("VITEL") -> VitelLogo(size = size, modifier = modifier)
        else -> GenericTelcoLogo(network = network, size = size, modifier = modifier)
    }
}

@Composable
fun MtnLogo(size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0xFFFFCC00)), // MTN Yellow
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(size * 0.75f)
                .height(size * 0.45f)
                .border(1.5.dp, Color.Black, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "MTN",
                color = Color.Black,
                fontWeight = FontWeight.Black,
                fontSize = (size.value * 0.22f).sp,
                letterSpacing = (-0.5).sp
            )
        }
    }
}

@Composable
fun AirtelLogo(size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0xFFE50914)), // Airtel Red
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "airtel",
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = (size.value * 0.28f).sp,
            letterSpacing = (-1).sp
        )
    }
}

@Composable
fun GloLogo(size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0xFF00A859)), // Glo Green
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "glo",
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = (size.value * 0.35f).sp
        )
    }
}

@Composable
fun NineMobileLogo(size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0xFF005C42)), // 9Mobile Dark Emerald/Green
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "9",
                color = Color(0xFFC6DA2B), // Lime accent
                fontWeight = FontWeight.Black,
                fontSize = (size.value * 0.35f).sp
            )
            Text(
                text = "mob",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.22f).sp
            )
        }
    }
}

@Composable
fun VitelLogo(size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0xFF003366)), // Vitel Blue
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "vitel",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.28f).sp
        )
    }
}

@Composable
fun GenericTelcoLogo(network: String, size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0xFF1E293B)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = network.take(2).uppercase(),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.3f).sp
        )
    }
}

/**
 * Brand color for each telco network
 */
fun getNetworkColor(network: String): Color {
    val normalized = network.trim().uppercase()
    return when {
        normalized.contains("MTN") -> Color(0xFFFFCC00) // MTN Vibrant Yellow
        normalized.contains("AIRTEL") -> Color(0xFFE50914) // Airtel Crimson Red
        normalized.contains("GLO") -> Color(0xFF00A859) // Glo Electric Green
        normalized.contains("9MOBILE") || normalized.contains("T2") || normalized.contains("9MOB") -> Color(0xFF005C42) // 9Mobile Dark Emerald
        normalized.contains("VITEL") -> Color(0xFF0052CC) // Vitel Deep Blue
        else -> com.example.ui.theme.CyberCyan
    }
}

/**
 * Appropriate text/icon content color ensuring maximum contrast over the network brand color
 */
fun getNetworkContentColor(network: String): Color {
    val normalized = network.trim().uppercase()
    return when {
        normalized.contains("MTN") -> Color(0xFF0A0A0A) // Pitch black on yellow for perfect readability
        else -> Color.White
    }
}
