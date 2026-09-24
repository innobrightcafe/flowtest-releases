package com.example.data.util

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.Build
import android.telephony.TelephonyManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

enum class NetworkQuality {
    ONLINE,       // Strong, fast internet connectivity
    LOW_NETWORK,  // Poor bandwidth (<1500 kbps), high latency, or cellular 2G/weak signal
    NO_NETWORK    // Disconnected / Offline
}

enum class NetworkTransportType {
    WIFI,
    CELLULAR,
    ETHERNET,
    VPN,
    NONE
}

data class RealNetworkState(
    val isConnected: Boolean = false,
    val hasInternet: Boolean = false,
    val transportType: NetworkTransportType = NetworkTransportType.NONE,
    val carrierOrWifiName: String = "No Network",
    val networkGeneration: String = "Offline",
    val signalStrengthLevel: Int = 0, // 0..4 (0 = None/Offline, 1 = Poor, 2 = Fair, 3 = Good, 4 = Excellent)
    val signalDbm: Int? = null,
    val signalStrengthDescription: String = "No Signal",
    val downstreamBandwidthKbps: Int = 0
) {
    val isWifi: Boolean get() = transportType == NetworkTransportType.WIFI
    val isCellular: Boolean get() = transportType == NetworkTransportType.CELLULAR
}

object NetworkUtils {

    /**
     * Checks if the device has an active network connection (Cellular, Wi-Fi, Ethernet, or VPN).
     * Works reliably across slow 2G/3G/EDGE networks, unvalidated captive portals, and low-data states.
     */
    fun isNetworkAvailable(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return true // Optimistic: do not block VTU transactions if ConnectivityManager is temporarily null

            val activeNetwork = cm.activeNetwork
            if (activeNetwork != null) {
                val caps = cm.getNetworkCapabilities(activeNetwork)
                if (caps != null) {
                    val hasTransport = caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
                    if (hasTransport) return true
                }
            }

            // Fallback: Check any connected network adapter (e.g. secondary SIM or during network handover)
            val allNetworks = cm.allNetworks
            if (allNetworks.isNotEmpty()) {
                for (net in allNetworks) {
                    val caps = cm.getNetworkCapabilities(net) ?: continue
                    if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                        return true
                    }
                }
            }

            // If no active network transport is detected, check if device is in airplane mode
            val isAirplaneMode = android.provider.Settings.Global.getInt(
                context.contentResolver,
                android.provider.Settings.Global.AIRPLANE_MODE_ON, 0
            ) != 0
            !isAirplaneMode
        } catch (e: Exception) {
            true // Optimistic fallback: let actual HTTP request proceed rather than falsely rejecting user
        }
    }

    /**
     * Accurately determines current network quality: ONLINE, LOW_NETWORK, or NO_NETWORK.
     * Prevents classifying slow 2G/3G or unvalidated cellular data as NO_NETWORK.
     */
    fun getCurrentNetworkQuality(context: Context): NetworkQuality {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return NetworkQuality.ONLINE
            val activeNetwork = cm.activeNetwork
                ?: return if (isNetworkAvailable(context)) NetworkQuality.LOW_NETWORK else NetworkQuality.NO_NETWORK
            val capabilities = cm.getNetworkCapabilities(activeNetwork)
                ?: return if (isNetworkAvailable(context)) NetworkQuality.LOW_NETWORK else NetworkQuality.NO_NETWORK

            val hasTransport = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)

            if (!hasTransport) return NetworkQuality.NO_NETWORK

            val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            val isValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            val downstreamKbps = capabilities.linkDownstreamBandwidthKbps

            // If transport is connected but unvalidated (e.g. captive portal / zero balance) or slow (<2000 kbps),
            // classify as LOW_NETWORK rather than disconnecting the user
            if (!hasInternet || !isValidated || (downstreamKbps in 1..2000)) {
                NetworkQuality.LOW_NETWORK
            } else {
                NetworkQuality.ONLINE
            }
        } catch (e: Exception) {
            NetworkQuality.ONLINE
        }
    }

    /**
     * Returns true if user is on a slow or low-bandwidth connection, enabling ultra-lean payloads.
     */
    fun isLowNetwork(context: Context): Boolean {
        return getCurrentNetworkQuality(context) == NetworkQuality.LOW_NETWORK
    }

    /**
     * Inspects real device network hardware, transport type, carrier, and signal strength.
     */
    fun getRealNetworkState(context: Context): RealNetworkState {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return RealNetworkState()
            val activeNetwork = cm.activeNetwork
                ?: return RealNetworkState()
            val capabilities = cm.getNetworkCapabilities(activeNetwork)
                ?: return RealNetworkState()

            val hasCapabilityInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            if (!hasCapabilityInternet) {
                return RealNetworkState(
                    isConnected = false,
                    hasInternet = false,
                    transportType = NetworkTransportType.NONE,
                    carrierOrWifiName = "No Network",
                    networkGeneration = "Disconnected",
                    signalStrengthLevel = 0,
                    signalStrengthDescription = "No Signal",
                    downstreamBandwidthKbps = 0
                )
            }

            val isValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            val downstreamKbps = capabilities.linkDownstreamBandwidthKbps

            val isWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            val isCellular = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
            val isEthernet = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            val isVpn = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)

            if (isWifi) {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                var ssid: String? = null
                var wifiLevel = 0
                var rssiDbm: Int? = null

                try {
                    @Suppress("DEPRECATION")
                    val wifiInfo = wifiManager?.connectionInfo
                    if (wifiInfo != null) {
                        val rawSsid = wifiInfo.ssid
                        if (!rawSsid.isNullOrBlank() && rawSsid != "<unknown ssid>") {
                            ssid = rawSsid.removeSurrounding("\"")
                        }
                        rssiDbm = wifiInfo.rssi
                        wifiLevel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && wifiManager != null) {
                            wifiManager.calculateSignalLevel(wifiInfo.rssi).coerceIn(0, 4)
                        } else {
                            @Suppress("DEPRECATION")
                            WifiManager.calculateSignalLevel(wifiInfo.rssi, 5).coerceIn(0, 4)
                        }
                    }
                } catch (e: Exception) {
                    // Ignore security exception
                }

                if (wifiLevel == 0) {
                    wifiLevel = when {
                        downstreamKbps >= 25000 -> 4
                        downstreamKbps >= 10000 -> 3
                        downstreamKbps >= 3000 -> 2
                        downstreamKbps > 0 -> 1
                        else -> 3
                    }
                }

                val desc = getSignalDescription(wifiLevel)
                return RealNetworkState(
                    isConnected = true,
                    hasInternet = isValidated,
                    transportType = NetworkTransportType.WIFI,
                    carrierOrWifiName = if (!ssid.isNullOrBlank()) "Wi-Fi ($ssid)" else "Wi-Fi",
                    networkGeneration = "Wi-Fi",
                    signalStrengthLevel = wifiLevel,
                    signalDbm = rssiDbm,
                    signalStrengthDescription = desc,
                    downstreamBandwidthKbps = downstreamKbps
                )
            }

            if (isCellular) {
                val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
                var carrierName = "Cellular"
                var cellularLevel = 0
                var generation = "4G LTE"

                try {
                    val rawCarrier = tm?.networkOperatorName?.takeIf { it.isNotBlank() }
                        ?: tm?.simOperatorName?.takeIf { it.isNotBlank() }

                    if (!rawCarrier.isNullOrBlank()) {
                        carrierName = when {
                            rawCarrier.contains("mtn", ignoreCase = true) -> "MTN NG"
                            rawCarrier.contains("airtel", ignoreCase = true) -> "Airtel NG"
                            rawCarrier.contains("glo", ignoreCase = true) -> "Glo NG"
                            rawCarrier.contains("9mobile", ignoreCase = true) || rawCarrier.contains("etisalat", ignoreCase = true) -> "9mobile NG"
                            else -> rawCarrier.trim()
                        }
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && tm != null) {
                        val sig = tm.signalStrength
                        if (sig != null && sig.level in 0..4) {
                            cellularLevel = sig.level
                        }
                    }

                    val netType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && tm != null) {
                        tm.dataNetworkType
                    } else {
                        @Suppress("DEPRECATION")
                        tm?.networkType ?: TelephonyManager.NETWORK_TYPE_UNKNOWN
                    }

                    generation = when (netType) {
                        TelephonyManager.NETWORK_TYPE_NR -> "5G"
                        TelephonyManager.NETWORK_TYPE_LTE -> "4G LTE"
                        TelephonyManager.NETWORK_TYPE_HSPAP,
                        TelephonyManager.NETWORK_TYPE_HSPA,
                        TelephonyManager.NETWORK_TYPE_HSDPA,
                        TelephonyManager.NETWORK_TYPE_HSUPA,
                        TelephonyManager.NETWORK_TYPE_UMTS -> "3G"
                        TelephonyManager.NETWORK_TYPE_EDGE,
                        TelephonyManager.NETWORK_TYPE_GPRS,
                        TelephonyManager.NETWORK_TYPE_CDMA,
                        TelephonyManager.NETWORK_TYPE_1xRTT -> "2G"
                        else -> {
                            if (downstreamKbps >= 15000) "4G LTE" else if (downstreamKbps >= 3000) "3G" else "Cellular"
                        }
                    }
                } catch (e: Exception) {
                    // Fallback to defaults
                }

                if (cellularLevel == 0) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val sigDbm = capabilities.signalStrength
                        if (sigDbm != NetworkCapabilities.SIGNAL_STRENGTH_UNSPECIFIED) {
                            cellularLevel = when {
                                sigDbm >= -75 -> 4
                                sigDbm >= -88 -> 3
                                sigDbm >= -102 -> 2
                                sigDbm >= -115 -> 1
                                else -> 1
                            }
                        }
                    }
                }

                if (cellularLevel == 0) {
                    cellularLevel = when {
                        downstreamKbps >= 20000 -> 4
                        downstreamKbps >= 8000 -> 3
                        downstreamKbps >= 2000 -> 2
                        downstreamKbps > 0 -> 1
                        else -> 3
                    }
                }

                val desc = getSignalDescription(cellularLevel)
                return RealNetworkState(
                    isConnected = true,
                    hasInternet = isValidated,
                    transportType = NetworkTransportType.CELLULAR,
                    carrierOrWifiName = carrierName,
                    networkGeneration = generation,
                    signalStrengthLevel = cellularLevel,
                    signalStrengthDescription = desc,
                    downstreamBandwidthKbps = downstreamKbps
                )
            }

            if (isEthernet) {
                return RealNetworkState(
                    isConnected = true,
                    hasInternet = isValidated,
                    transportType = NetworkTransportType.ETHERNET,
                    carrierOrWifiName = "Ethernet",
                    networkGeneration = "LAN",
                    signalStrengthLevel = 4,
                    signalStrengthDescription = "Excellent",
                    downstreamBandwidthKbps = downstreamKbps
                )
            }

            if (isVpn) {
                return RealNetworkState(
                    isConnected = true,
                    hasInternet = isValidated,
                    transportType = NetworkTransportType.VPN,
                    carrierOrWifiName = "Encrypted Tunnel",
                    networkGeneration = "VPN",
                    signalStrengthLevel = 4,
                    signalStrengthDescription = "Secure",
                    downstreamBandwidthKbps = downstreamKbps
                )
            }

            return RealNetworkState(
                isConnected = true,
                hasInternet = isValidated,
                transportType = NetworkTransportType.NONE,
                carrierOrWifiName = "Connected",
                networkGeneration = "Online",
                signalStrengthLevel = 3,
                signalStrengthDescription = "Good",
                downstreamBandwidthKbps = downstreamKbps
            )
        } catch (e: Exception) {
            return RealNetworkState(
                isConnected = false,
                hasInternet = false,
                transportType = NetworkTransportType.NONE,
                carrierOrWifiName = "No Network",
                networkGeneration = "Offline",
                signalStrengthLevel = 0,
                signalStrengthDescription = "No Signal",
                downstreamBandwidthKbps = 0
            )
        }
    }

    private fun getSignalDescription(level: Int): String {
        return when (level) {
            4 -> "Excellent"
            3 -> "Good"
            2 -> "Fair"
            1 -> "Weak"
            else -> "No Signal"
        }
    }

    /**
     * Observes real-time network connectivity and hardware signal strength as a Kotlin Flow.
     */
    fun observeRealNetworkState(context: Context): Flow<RealNetworkState> = callbackFlow {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (cm == null) {
            trySend(RealNetworkState())
            close()
            return@callbackFlow
        }

        // Send initial state immediately
        trySend(getRealNetworkState(context))

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(getRealNetworkState(context))
            }

            override fun onLost(network: Network) {
                trySend(getRealNetworkState(context))
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                trySend(getRealNetworkState(context))
            }

            override fun onUnavailable() {
                trySend(RealNetworkState())
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        try {
            cm.registerNetworkCallback(request, callback)
        } catch (e: Exception) {
            trySend(getRealNetworkState(context))
        }

        awaitClose {
            try {
                cm.unregisterNetworkCallback(callback)
            } catch (e: Exception) {
                // Ignore unregister exceptions
            }
        }
    }.distinctUntilChanged()

    /**
     * Observes real-time network connectivity quality as a Kotlin Flow.
     */
    fun observeNetworkQuality(context: Context): Flow<NetworkQuality> = callbackFlow {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (cm == null) {
            trySend(NetworkQuality.NO_NETWORK)
            close()
            return@callbackFlow
        }

        // Send initial state immediately
        trySend(getCurrentNetworkQuality(context))

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(getCurrentNetworkQuality(context))
            }

            override fun onLost(network: Network) {
                trySend(getCurrentNetworkQuality(context))
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                trySend(getCurrentNetworkQuality(context))
            }

            override fun onUnavailable() {
                trySend(NetworkQuality.NO_NETWORK)
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        try {
            cm.registerNetworkCallback(request, callback)
        } catch (e: Exception) {
            trySend(getCurrentNetworkQuality(context))
        }

        awaitClose {
            try {
                cm.unregisterNetworkCallback(callback)
            } catch (e: Exception) {
                // Ignore unregister exceptions
            }
        }
    }.distinctUntilChanged()

    /**
     * Checks if the app has PACKAGE_USAGE_STATS permission granted to query NetworkStatsManager per-app bytes.
     */
    fun hasUsageStatsPermission(context: Context): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? android.app.AppOpsManager ?: return false
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    context.packageName
                )
            }
            mode == android.app.AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Opens system usage access settings so the user can grant permission for exact app data tracking.
     */
    fun openUsageAccessSettings(context: Context) {
        // First try package-specific usage access settings if supported
        try {
            val intent = Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                data = android.net.Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return
        } catch (e: Exception) {
            // Some devices reject package URI on usage access; fall through to standard intent
        }

        try {
            val intent = Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e2: Exception) {
                // Ignore
            }
        }
    }

    /**
     * Opens App Info page directly so users can allow restricted settings on Android 13+.
     */
    fun openAppDetailsSettings(context: Context) {
        try {
            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val intent = Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {}
        }
    }
}

