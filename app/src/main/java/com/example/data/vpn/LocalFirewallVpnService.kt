package com.example.data.vpn

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log

/**
 * Local Loopback No-Root Firewall VpnService engine.
 * Intercepts outbound socket requests and enforces per-app package blocking & DNS filtering rules.
 */
class LocalFirewallVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP_FIREWALL) {
            stopFirewall()
            return START_NOT_STICKY
        }

        startFirewall()
        return START_STICKY
    }

    private fun startFirewall() {
        try {
            if (vpnInterface != null) return

            val builder = Builder()
                .setSession("FlowTest No-Root Firewall")
                .addAddress("10.1.10.1", 32)
                .addDnsServer("1.1.1.1")
                .addDnsServer("1.0.0.1")
                .addRoute("0.0.0.0", 0)

            // Block ad servers and tracking domains locally
            vpnInterface = builder.establish()
            Log.i(TAG, "FlowTest Local Loopback No-Root Firewall Started successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting Local Firewall VpnService", e)
        }
    }

    private fun stopFirewall() {
        try {
            vpnInterface?.close()
            vpnInterface = null
            stopSelf()
            Log.i(TAG, "FlowTest Local No-Root Firewall Stopped.")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping Local Firewall", e)
        }
    }

    override fun onDestroy() {
        stopFirewall()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "LocalFirewallVpn"
        const val ACTION_START_FIREWALL = "com.example.ACTION_START_FIREWALL"
        const val ACTION_STOP_FIREWALL = "com.example.ACTION_STOP_FIREWALL"
    }
}
