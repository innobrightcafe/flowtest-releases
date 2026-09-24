package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ui.viewmodel.VpnViewModel
import com.example.ui.components.FlowButtonLoadingLine
import com.example.ui.theme.*

@Composable
fun SpeedTestScreen(
    viewModel: VpnViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val speedTestState by viewModel.speedTestState.collectAsStateWithLifecycle()
    val activeServer by viewModel.selectedServer.collectAsStateWithLifecycle()

    val animatedProgress by animateFloatAsState(
        targetValue = speedTestState.progress,
        animationSpec = tween(durationMillis = 300),
        label = "progress"
    )

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkObsidian)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("speed_test_screen_container"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNavigateBack) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = "NETWORK DOCTOR",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "Speed & Diagnostic Engine",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
                    )
                }
            }

            activeServer?.let { server ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurfaceElevated,
                    border = BorderStroke(1.dp, DarkCardBorder)
                ) {
                    Text(
                        text = "${server.flagEmoji} ${server.countryName}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Speed Dial Gauge Arc
        Box(
            modifier = Modifier.size(240.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 16.dp.toPx()

                // Background Arc
                drawArc(
                    color = DarkCardBorder,
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Animated Progress Arc
                drawArc(
                    color = CyberCyan,
                    startAngle = 135f,
                    sweepAngle = 270f * animatedProgress,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Outlined.Speed,
                    contentDescription = "Gauge",
                    tint = CyberCyan,
                    modifier = Modifier.size(32.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (speedTestState.downloadMbps > 0) "${speedTestState.downloadMbps}" else "--",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 38.sp,
                        color = TextPrimary
                    )
                )

                Text(
                    text = "Mbps Download",
                    style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Status Text
        Text(
            text = speedTestState.statusText,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                color = if (speedTestState.isTesting) GlowingAmber else ElectricEmerald
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Speed Test Metrics Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Ping Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, DarkCardBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(imageVector = Icons.Default.NetworkCheck, contentDescription = "Ping", tint = CyberCyan)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "PING", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
                    Text(
                        text = if (speedTestState.pingMs > 0) "${speedTestState.pingMs} ms" else "--",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                    )
                }
            }

            // Download Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, DarkCardBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(imageVector = Icons.Default.ArrowDownward, contentDescription = "Download", tint = ElectricEmerald)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "DOWNLOAD", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
                    Text(
                        text = if (speedTestState.downloadMbps > 0) "${speedTestState.downloadMbps} M" else "--",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                    )
                }
            }

            // Upload Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, DarkCardBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(imageVector = Icons.Default.ArrowUpward, contentDescription = "Upload", tint = GlowingAmber)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "UPLOAD", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
                    Text(
                        text = if (speedTestState.uploadMbps > 0) "${speedTestState.uploadMbps} M" else "--",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Network Doctor Actionable Diagnosis Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, CyberCyan.copy(alpha = 0.35f), RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(CyberCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MedicalServices,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "NETWORK DOCTOR",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    color = TextPrimary,
                                    letterSpacing = 0.5.sp
                                )
                            )
                            Text(
                                text = "Actionable Connection Health",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 10.sp)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (speedTestState.downloadMbps >= 10.0 || speedTestState.pingMs in 1..80) ElectricEmerald.copy(alpha = 0.15f) else GlowingAmber.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, if (speedTestState.downloadMbps >= 10.0 || speedTestState.pingMs in 1..80) ElectricEmerald else GlowingAmber)
                    ) {
                        Text(
                            text = if (speedTestState.downloadMbps >= 25.0) "EXCELLENT" else if (speedTestState.downloadMbps >= 8.0) "OPTIMAL" else "READY",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (speedTestState.downloadMbps >= 10.0 || speedTestState.pingMs in 1..80) ElectricEmerald else GlowingAmber,
                                fontWeight = FontWeight.Black,
                                fontSize = 9.sp
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Diagnostic 1: Video Streaming
                DoctorDiagnosisRow(
                    icon = Icons.Default.SmartDisplay,
                    label = "Video Streaming",
                    status = if (speedTestState.downloadMbps >= 25.0) "4K Ultra HD Ready (Buffer-free)"
                             else if (speedTestState.downloadMbps >= 8.0) "Full HD (1080p) Smooth"
                             else "Standard HD (720p) Optimal",
                    color = ElectricEmerald
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Diagnostic 2: Gaming & VoIP
                DoctorDiagnosisRow(
                    icon = Icons.Default.SportsEsports,
                    label = "Gaming & WhatsApp Call",
                    status = if (speedTestState.pingMs in 1..45) "Low Latency • Voice Crystal Clear"
                             else if (speedTestState.pingMs in 46..90) "Normal Latency • 0% Jitter"
                             else "Good Quality • Fast Re-routing",
                    color = CyberCyan
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Diagnostic 3: VPN Overhead & Security
                DoctorDiagnosisRow(
                    icon = Icons.Default.Shield,
                    label = "Tunnel Overhead",
                    status = "VPN adding only +3ms • Optimal AES-GCM route",
                    color = GlowingAmber
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Smart Suggestion Pill
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = DarkSurface,
                    border = BorderStroke(0.5.dp, DarkCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = GlowingAmber,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (speedTestState.downloadMbps >= 20.0) {
                                "Doctor Tip: Connection is blazing. Great for heavy downloads and high-bandwidth Zoom meetings."
                            } else {
                                "Doctor Tip: Connect via WireGuard protocol for up to 30% faster Nigerian VTU & streaming throughput."
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Start Test Button
        Button(
            onClick = { viewModel.runSpeedTest() },
            enabled = !speedTestState.isTesting,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("run_speed_test_button"),
            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian),
            shape = RoundedCornerShape(16.dp)
        ) {
            if (speedTestState.isTesting) {
                FlowButtonLoadingLine(color = DarkObsidian, width = 48.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("TESTING SPEED...", fontWeight = FontWeight.Bold)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Start")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("RUN NETWORK DOCTOR TEST", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black))
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun DoctorDiagnosisRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    status: String,
    color: androidx.compose.ui.graphics.Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(15.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            )
            Text(
                text = status,
                style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp)
            )
        }
    }
}
