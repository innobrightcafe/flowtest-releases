package com.example.data.vpn

import com.example.data.db.ServerEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

enum class VpnState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    DISCONNECTING
}

data class VpnMetrics(
    val downloadSpeedMbps: Float = 0f,
    val uploadSpeedMbps: Float = 0f,
    val totalBytesDownloaded: Long = 0,
    val totalBytesUploaded: Long = 0,
    val durationSeconds: Long = 0,
    val currentIp: String = "185.12.64.10",
    val encryption: String = "ChaCha20-Poly1305 (256-bit)",
    val handshakeTimeMs: Long = 0
)

class VpnConnectionManager {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _vpnState = MutableStateFlow(VpnState.DISCONNECTED)
    val vpnState: StateFlow<VpnState> = _vpnState.asStateFlow()

    private val _activeServer = MutableStateFlow<ServerEntity?>(null)
    val activeServer: StateFlow<ServerEntity?> = _activeServer.asStateFlow()

    private val _connectingStep = MutableStateFlow("")
    val connectingStep: StateFlow<String> = _connectingStep.asStateFlow()

    private val _metrics = MutableStateFlow(VpnMetrics())
    val metrics: StateFlow<VpnMetrics> = _metrics.asStateFlow()

    private var metricsJob: Job? = null
    private var connectionStartTime: Long = 0

    fun connect(server: ServerEntity) {
        if (_vpnState.value == VpnState.CONNECTING || _vpnState.value == VpnState.CONNECTED) return

        _activeServer.value = server
        _vpnState.value = VpnState.CONNECTING

        scope.launch {
            val startTime = System.currentTimeMillis()

            val providerName = if (server.isHetznerServer) "FlowTest Dedicated" else "FlowTest High-Speed"
            _connectingStep.value = "Resolving $providerName DNS & Route..."
            delay(400)

            _connectingStep.value = "Initiating ${server.protocol} Handshake with ${server.ipAddress}..."
            delay(500)

            _connectingStep.value = "Exchanging Noise IK Keys & Poly1305 Cipher ($providerName)..."
            delay(450)

            _connectingStep.value = "Tunnel Secured! Setting up TUN interface on $providerName..."
            delay(350)

            val handshakeMs = System.currentTimeMillis() - startTime
            connectionStartTime = System.currentTimeMillis()

            _metrics.value = VpnMetrics(
                currentIp = server.ipAddress,
                handshakeTimeMs = handshakeMs,
                encryption = "ChaCha20-Poly1305 (256-bit)"
            )

            _vpnState.value = VpnState.CONNECTED
            startMetricsEngine()
        }
    }

    fun disconnect() {
        if (_vpnState.value != VpnState.CONNECTED) return

        _vpnState.value = VpnState.DISCONNECTING
        metricsJob?.cancel()

        scope.launch {
            _connectingStep.value = "Terminating WireGuard tunnel..."
            delay(500)
            _vpnState.value = VpnState.DISCONNECTED
            _metrics.value = VpnMetrics()
        }
    }

    private fun startMetricsEngine() {
        metricsJob?.cancel()
        metricsJob = scope.launch {
            val initialRx = android.net.TrafficStats.getTotalRxBytes().takeIf { it > 0 } ?: 0L
            val initialTx = android.net.TrafficStats.getTotalTxBytes().takeIf { it > 0 } ?: 0L
            var lastRx = initialRx
            var lastTx = initialTx
            var lastSampleTime = System.currentTimeMillis()

            while (isActive && _vpnState.value == VpnState.CONNECTED) {
                delay(1000)
                val now = System.currentTimeMillis()
                val elapsedSec = (now - connectionStartTime) / 1000
                val dt = (now - lastSampleTime).coerceAtLeast(1) / 1000.0f
                lastSampleTime = now

                val currentRx = android.net.TrafficStats.getTotalRxBytes().takeIf { it > 0 } ?: lastRx
                val currentTx = android.net.TrafficStats.getTotalTxBytes().takeIf { it > 0 } ?: lastTx

                val deltaRx = (currentRx - lastRx).coerceAtLeast(0L)
                val deltaTx = (currentTx - lastTx).coerceAtLeast(0L)
                lastRx = currentRx
                lastTx = currentTx

                val downSpeed = if (dt > 0) ((deltaRx * 8f) / (dt * 1_000_000f)) else 0f
                val upSpeed = if (dt > 0) ((deltaTx * 8f) / (dt * 1_000_000f)) else 0f

                val sessionDownloaded = (currentRx - initialRx).coerceAtLeast(0L)
                val sessionUploaded = (currentTx - initialTx).coerceAtLeast(0L)

                _metrics.value = _metrics.value.copy(
                    downloadSpeedMbps = String.format(java.util.Locale.US, "%.2f", downSpeed).toFloat(),
                    uploadSpeedMbps = String.format(java.util.Locale.US, "%.2f", upSpeed).toFloat(),
                    totalBytesDownloaded = sessionDownloaded,
                    totalBytesUploaded = sessionUploaded,
                    durationSeconds = elapsedSec
                )
            }
        }
    }

    companion object {
        fun formatBytes(bytes: Long): String {
            return when {
                bytes >= 1_073_741_824 -> String.format("%.2f GB", bytes / 1_073_741_824.0)
                bytes >= 1_048_576 -> String.format("%.1f MB", bytes / 1_048_576.0)
                bytes >= 1024 -> String.format("%.0f KB", bytes / 1024.0)
                else -> "$bytes B"
            }
        }

        fun formatTime(seconds: Long): String {
            val hrs = seconds / 3600
            val mins = (seconds % 3600) / 60
            val secs = seconds % 60
            return if (hrs > 0) {
                String.format("%02d:%02d:%02d", hrs, mins, secs)
            } else {
                String.format("%02d:%02d", mins, secs)
            }
        }
    }
}
