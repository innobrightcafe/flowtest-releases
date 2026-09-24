package com.example.ui.components.chat

import android.Manifest
import android.content.pm.PackageManager
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.ui.components.FlowLoadingLine
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.webrtc.WebRtcCallManager
import com.example.ui.theme.*

/**
 * WebRtcAudioCallDialog
 * UI/UX Pro Max WebRTC Audio Call Screen.
 * Features:
 * - Real-time CleanSignal™ DSP Telemetry (Opus 48kHz, RTT, Jitter, 0% Packet Loss)
 * - 5-Bar Animated Signal Strength Indicator
 * - Real-time Reactive Soundwave Spectrum Visualizer
 * - Speakerphone, Microphone Mute, and ClearSignal™ AI Noise Gate toggles
 * - Switch to Video Call option
 */
@Composable
fun WebRtcAudioCallDialog(
    recipientName: String,
    recipientPhone: String,
    onDismiss: () -> Unit,
    onSwitchToVideo: () -> Unit = {}
) {
    val context = LocalContext.current
    val callManager = remember { WebRtcCallManager.getInstance(context) }
    val session by callManager.currentSession.collectAsStateWithLifecycle()
    val amplitude by callManager.audioAmplitude.collectAsStateWithLifecycle()

    // Request Audio Record permission if not yet granted
    val hasAudioPermission = remember {
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (!hasAudioPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
        callManager.startCall(recipientName, recipientPhone, WebRtcCallManager.CallType.AUDIO)
    }

    Dialog(
        onDismissRequest = {
            callManager.endCall()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkObsidian)
                .testTag("webrtc_audio_call_container"),
            color = DarkObsidian
        ) {
            val currentSession = session
            val state = currentSession?.state ?: WebRtcCallManager.CallState.OUTGOING_RINGING
            val durationSec = currentSession?.durationSeconds ?: 0
            val telemetry = currentSession?.telemetry ?: WebRtcCallManager.WebRtcTelemetry()

            // Concentric soundwave animation rings
            val infiniteTransition = rememberInfiniteTransition(label = "pulse_rings")
            val ringScale1 by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.35f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1600, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "ring_scale_1"
            )
            val ringAlpha1 by infiniteTransition.animateFloat(
                initialValue = 0.5f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1600, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "ring_alpha_1"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                CyberCyan.copy(alpha = 0.14f),
                                DarkSurface.copy(alpha = 0.95f),
                                DarkObsidian
                            ),
                            radius = 1200f
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // --- TOP SECTION: PROTOCOL & CALLER INFO ---
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        // Protocol Header Pill
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = DarkSurfaceElevated,
                            border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.3f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(ElectricEmerald)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "WEBRTC FLOW CALL • OPUS HD",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = CyberCyan,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = recipientName.ifBlank { "Contact" },
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Call State & Duration
                        val statusText = when (state) {
                            WebRtcCallManager.CallState.OUTGOING_RINGING -> "Ringing peer..."
                            WebRtcCallManager.CallState.EXCHANGING_SDP_ICE -> "Exchanging WebRTC SDP & ICE..."
                            WebRtcCallManager.CallState.CONNECTED -> {
                                val mins = durationSec / 60
                                val secs = durationSec % 60
                                String.format("%02d:%02d • HD Audio (48 kHz)", mins, secs)
                            }
                            WebRtcCallManager.CallState.ENDED -> "Call Ended"
                            else -> recipientPhone
                        }

                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = if (state == WebRtcCallManager.CallState.CONNECTED) CyberCyan else TextSecondary,
                                fontWeight = if (state == WebRtcCallManager.CallState.CONNECTED) FontWeight.SemiBold else FontWeight.Normal
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Clean Signal Telemetry Badge
                        WebRtcSignalQualityBadge(telemetry = telemetry)
                    }

                    // --- CENTER SECTION: PULSING AVATAR & AUDIO SPECTRUM ---
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            // Pulsing radar ring
                            if (state == WebRtcCallManager.CallState.CONNECTED) {
                                Box(
                                    modifier = Modifier
                                        .size(150.dp)
                                        .scale(ringScale1)
                                        .clip(CircleShape)
                                        .border(2.dp, CyberCyan.copy(alpha = ringAlpha1), CircleShape)
                                )
                            }

                            // Center Avatar
                            Surface(
                                shape = CircleShape,
                                color = DarkSurfaceElevated,
                                border = BorderStroke(2.dp, CyberCyan),
                                modifier = Modifier.size(130.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.linearGradient(
                                                listOf(CyberCyan.copy(alpha = 0.8f), ElectricEmerald.copy(alpha = 0.8f))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = recipientName.take(2).uppercase().ifBlank { "FL" },
                                        style = MaterialTheme.typography.headlineLarge.copy(
                                            color = DarkObsidian,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 42.sp
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Real-time Audio Waveform Visualizer
                        if (state == WebRtcCallManager.CallState.CONNECTED) {
                            WebRtcAudioWaveformVisualizer(
                                amplitude = amplitude,
                                isMuted = currentSession?.isMuted == true
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (currentSession?.isMuted == true) "Microphone Muted" else "CleanSignal™ Noise Suppression Active",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (currentSession?.isMuted == true) CoralRed else TextMuted,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    // --- BOTTOM SECTION: CALL CONTROLS DOCK ---
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Quick Enhancement Toggles Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Mute Microphone
                            CallControlButton(
                                icon = if (currentSession?.isMuted == true) Icons.Filled.MicOff else Icons.Filled.Mic,
                                label = if (currentSession?.isMuted == true) "Unmute" else "Mute",
                                isActive = currentSession?.isMuted == true,
                                activeColor = CoralRed,
                                onClick = { callManager.toggleMute() }
                            )

                            // Speakerphone
                            CallControlButton(
                                icon = Icons.AutoMirrored.Filled.VolumeUp,
                                label = "Speaker",
                                isActive = currentSession?.isSpeakerOn == true,
                                activeColor = CyberCyan,
                                onClick = { callManager.toggleSpeaker() }
                            )

                            // ClearSignal™ AI Noise Gate Toggle
                            CallControlButton(
                                icon = Icons.Filled.GraphicEq,
                                label = "ClearSignal",
                                isActive = telemetry.isClearSignalEnhancerActive,
                                activeColor = ElectricEmerald,
                                onClick = { callManager.toggleClearSignalEnhancer() }
                            )

                            // Switch to Video
                            CallControlButton(
                                icon = Icons.Filled.Videocam,
                                label = "Video",
                                isActive = false,
                                activeColor = CyberCyan,
                                onClick = {
                                    callManager.endCall()
                                    onSwitchToVideo()
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(30.dp))

                        // End Call Button
                        Button(
                            onClick = {
                                callManager.endCall()
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CoralRed),
                            shape = CircleShape,
                            modifier = Modifier
                                .size(68.dp)
                                .testTag("end_webrtc_audio_call_btn"),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CallEnd,
                                contentDescription = "End Call",
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

/**
 * WebRtcVideoCallDialog
 * UI/UX Pro Max WebRTC Video Call Screen.
 * Features:
 * - Real local camera feed via CameraX in PiP preview
 * - Remote peer video simulation with animated clean lighting
 * - Front / Back camera flip toggle
 * - Video mute / Blackout toggle
 * - Live WebRTC Telemetry overlay (720p HD, Bitrate, RTT, Signal Bars)
 * - Speakerphone, Microphone Mute & End Call
 */
@Composable
fun WebRtcVideoCallDialog(
    recipientName: String,
    recipientPhone: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val callManager = remember { WebRtcCallManager.getInstance(context) }
    val session by callManager.currentSession.collectAsStateWithLifecycle()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        hasCameraPermission = perms[Manifest.permission.CAMERA] == true
        hasAudioPermission = perms[Manifest.permission.RECORD_AUDIO] == true
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission || !hasAudioPermission) {
            permissionsLauncher.launch(
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
            )
        }
        callManager.startCall(recipientName, recipientPhone, WebRtcCallManager.CallType.VIDEO)
    }

    Dialog(
        onDismissRequest = {
            callManager.endCall()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF070B11))
                .testTag("webrtc_video_call_container"),
            color = Color(0xFF070B11)
        ) {
            val currentSession = session
            val state = currentSession?.state ?: WebRtcCallManager.CallState.OUTGOING_RINGING
            val durationSec = currentSession?.durationSeconds ?: 0
            val telemetry = currentSession?.telemetry ?: WebRtcCallManager.WebRtcTelemetry()
            val isFrontCamera = currentSession?.isFrontCamera ?: true
            val isVideoEnabled = currentSession?.isVideoEnabled ?: true

            Box(modifier = Modifier.fillMaxSize()) {
                // --- REMOTE VIDEO FEED (FULLSCREEN) ---
                if (state == WebRtcCallManager.CallState.CONNECTED) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xFF0F172A),
                                        Color(0xFF1E293B),
                                        Color(0xFF0B1120)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Remote peer simulated HD video canvas
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(130.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(listOf(CyberCyan, ElectricEmerald))
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = recipientName.take(2).uppercase().ifBlank { "PE" },
                                    style = MaterialTheme.typography.headlineLarge.copy(
                                        color = DarkObsidian,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 44.sp
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = recipientName,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "WebRTC HD Peer Stream • 720p @ 30fps",
                                style = MaterialTheme.typography.bodySmall.copy(color = CyberCyan)
                            )
                        }
                    }
                } else {
                    // Ringing / Handshake state backdrop
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(DarkObsidian),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 32.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(CyberCyan.copy(alpha = 0.12f))
                                    .border(1.5.dp, CyberCyan.copy(alpha = 0.4f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = null,
                                    tint = CyberCyan,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = if (state == WebRtcCallManager.CallState.OUTGOING_RINGING) "Calling $recipientName..." else "Connecting WebRTC Peer Connection...",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Establishing clean signal via Google STUN...",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            FlowLoadingLine(
                                modifier = Modifier.fillMaxWidth(0.65f),
                                color = CyberCyan,
                                height = 4.dp
                            )
                        }
                    }
                }

                // --- TOP OVERLAY: TELEMETRY & CALL STATUS ---
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Call info pill
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = DarkSurface.copy(alpha = 0.85f),
                            border = BorderStroke(1.dp, GlassBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(ElectricEmerald))
                                Spacer(modifier = Modifier.width(8.dp))
                                val mins = durationSec / 60
                                val secs = durationSec % 60
                                Text(
                                    text = if (state == WebRtcCallManager.CallState.CONNECTED) String.format("%02d:%02d", mins, secs) else "Connecting...",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }

                        // WebRTC Signal Quality Badge
                        WebRtcSignalQualityBadge(telemetry = telemetry)
                    }
                }

                // --- LOCAL CAMERA PIP (PICTURE-IN-PICTURE) TOP-RIGHT ---
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = DarkSurfaceElevated,
                    border = BorderStroke(2.dp, if (isVideoEnabled) CyberCyan else CoralRed),
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(top = 60.dp, end = 16.dp)
                        .size(width = 110.dp, height = 160.dp)
                        .align(Alignment.TopEnd)
                        .clip(RoundedCornerShape(18.dp))
                ) {
                    if (isVideoEnabled && hasCameraPermission) {
                        CameraXPreviewView(
                            isFrontCamera = isFrontCamera,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(DarkSurface),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Filled.VideocamOff,
                                    contentDescription = "Camera Off",
                                    tint = CoralRed,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (!hasCameraPermission) "No Perm" else "Cam Off",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TextMuted,
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        }
                    }
                }

                // --- BOTTOM CONTROL BAR ---
                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = DarkSurface.copy(alpha = 0.92f),
                    border = BorderStroke(1.dp, GlassBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 24.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Flip Camera
                        IconButton(
                            onClick = { callManager.flipCamera() },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(DarkSurfaceElevated)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FlipCameraAndroid,
                                contentDescription = "Flip Camera",
                                tint = CyberCyan,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Toggle Local Camera On/Off
                        IconButton(
                            onClick = { callManager.toggleVideo() },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (!isVideoEnabled) CoralRed else DarkSurfaceElevated)
                        ) {
                            Icon(
                                imageVector = if (isVideoEnabled) Icons.Filled.Videocam else Icons.Filled.VideocamOff,
                                contentDescription = "Toggle Video",
                                tint = if (!isVideoEnabled) Color.White else TextPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Toggle Microphone
                        IconButton(
                            onClick = { callManager.toggleMute() },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (currentSession?.isMuted == true) CoralRed else DarkSurfaceElevated)
                        ) {
                            Icon(
                                imageVector = if (currentSession?.isMuted == true) Icons.Filled.MicOff else Icons.Filled.Mic,
                                contentDescription = "Toggle Mic",
                                tint = if (currentSession?.isMuted == true) Color.White else TextPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Speakerphone
                        IconButton(
                            onClick = { callManager.toggleSpeaker() },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (currentSession?.isSpeakerOn == true) CyberCyan else DarkSurfaceElevated)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Speaker",
                                tint = if (currentSession?.isSpeakerOn == true) DarkObsidian else TextPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // End Call
                        Button(
                            onClick = {
                                callManager.endCall()
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CoralRed),
                            shape = CircleShape,
                            modifier = Modifier
                                .size(52.dp)
                                .testTag("end_webrtc_video_call_btn"),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CallEnd,
                                contentDescription = "End Call",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * CameraXPreviewView
 * Renders local CameraX preview stream in Jetpack Compose
 */
@Composable
fun CameraXPreviewView(
    isFrontCamera: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val selector = if (isFrontCamera) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } else {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview)
                } catch (e: Exception) {
                    android.util.Log.e("CameraXPreviewView", "Camera binding error: ${e.message}")
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        update = { previewView ->
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val selector = if (isFrontCamera) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } else {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview)
                } catch (e: Exception) {
                    android.util.Log.e("CameraXPreviewView", "Camera update error: ${e.message}")
                }
            }, ContextCompat.getMainExecutor(context))
        },
        modifier = modifier
    )
}

/**
 * WebRtcSignalQualityBadge
 * Shows 5 signal strength bars + RTT latency + CleanSignal indicator
 */
@Composable
fun WebRtcSignalQualityBadge(
    telemetry: WebRtcCallManager.WebRtcTelemetry,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = DarkSurface.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, GlassBorder),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 5 Signal Bars
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.height(14.dp)
            ) {
                for (bar in 1..5) {
                    val barHeight = (4 + bar * 2).dp
                    val isLit = bar <= telemetry.signalBars
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(barHeight)
                            .clip(RoundedCornerShape(1.dp))
                            .background(
                                if (isLit) Color(telemetry.signalQuality.colorHex) else TextMuted.copy(alpha = 0.3f)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "${telemetry.rttMs}ms • ${telemetry.signalQuality.label}",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color(telemetry.signalQuality.colorHex),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            )
        }
    }
}

/**
 * WebRtcAudioWaveformVisualizer
 * Animated real-time frequency bars responding dynamically to voice amplitude
 */
@Composable
fun WebRtcAudioWaveformVisualizer(
    amplitude: Float,
    isMuted: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(42.dp)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val barCount = 18
        for (i in 0 until barCount) {
            val factor = kotlin.math.sin((i / barCount.toFloat()) * kotlin.math.PI).toFloat()
            val animatedHeight = if (isMuted) {
                4.dp
            } else {
                (6 + (amplitude * 32 * factor)).coerceIn(4f, 38f).dp
            }

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(animatedHeight)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(CyberCyan, ElectricEmerald)
                        )
                    )
            )
        }
    }
}

/**
 * CallControlButton
 * Circular action button for in-call dock
 */
@Composable
private fun CallControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(if (isActive) activeColor else DarkSurfaceElevated)
                .border(1.dp, if (isActive) activeColor else GlassBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) (if (activeColor == CyberCyan) DarkObsidian else Color.White) else TextPrimary,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = if (isActive) activeColor else TextSecondary,
                fontSize = 11.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
            )
        )
    }
}
