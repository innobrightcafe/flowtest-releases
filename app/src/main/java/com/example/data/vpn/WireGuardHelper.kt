package com.example.data.vpn

import java.security.SecureRandom
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

data class WireGuardConfig(
    val serverName: String,
    val clientPrivateKey: String,
    val clientAddress: String = "10.66.66.2/32, fd42:42:42::2/128",
    val dns: String = "1.1.1.1, 1.0.0.1",
    val serverPublicKey: String,
    val presharedKey: String? = null,
    val endpoint: String,
    val allowedIps: String = "0.0.0.0/0, ::/0",
    val persistentKeepalive: Int = 25
) {
    fun toConfString(): String {
        return StringBuilder().apply {
            append("[Interface]\n")
            append("PrivateKey = $clientPrivateKey\n")
            append("Address = $clientAddress\n")
            append("DNS = $dns\n\n")
            append("[Peer]\n")
            append("PublicKey = $serverPublicKey\n")
            if (!presharedKey.isNull_or_empty()) {
                append("PresharedKey = $presharedKey\n")
            }
            append("Endpoint = $endpoint\n")
            append("AllowedIPs = $allowedIps\n")
            append("PersistentKeepalive = $persistentKeepalive\n")
        }.toString()
    }
}

private fun String?.isNull_or_empty(): Boolean = this == null || this.isEmpty()

object WireGuardHelper {

    @OptIn(ExperimentalEncodingApi::class)
    fun generateRandomKey(): String {
        val randomBytes = ByteArray(32)
        SecureRandom().nextBytes(randomBytes)
        return Base64.encode(randomBytes)
    }

    /**
     * Generates a ready-to-run Cloud-Init user_data script for Hetzner Cloud VPS.
     * Installs WireGuard, configures IP forwarding, iptables NAT, and creates the client peer.
     */
    fun generateHetznerCloudInitScript(
        clientPublicKey: String,
        serverPort: Int = 51820
    ): String {
        return """
#!/bin/bash
set -e

# Update & Install WireGuard + Tools
apt-get update -y
apt-get install -y wireguard iptables qrencode unbound

# Enable IPv4 & IPv6 Forwarding
sysctl -w net.ipv4.ip_forward=1
sysctl -w net.ipv6.conf.all.forwarding=1
echo "net.ipv4.ip_forward=1" >> /etc/sysctl.conf
echo "net.ipv6.conf.all.forwarding=1" >> /etc/sysctl.conf

# Detect main network interface
SERVER_PUB_NIC=$(ip route show default | awk '/default/ {print ${'$'}5}')

# Generate Server Private/Public keys
cd /etc/wireguard
umask 077
wg genkey | tee server_private.key | wg pubkey > server_public.key
SERVER_PRIV_KEY=$(cat server_private.key)

# Create wg0.conf
cat <<EOF > /etc/wireguard/wg0.conf
[Interface]
Address = 10.66.66.1/24, fd42:42:42::1/64
ListenPort = $serverPort
PrivateKey = ${'$'}SERVER_PRIV_KEY
PostUp = iptables -A FORWARD -i wg0 -j ACCEPT; iptables -t nat -A POSTROUTING -o ${'$'}SERVER_PUB_NIC -j MASQUERADE
PostDown = iptables -D FORWARD -i wg0 -j ACCEPT; iptables -t nat -D POSTROUTING -o ${'$'}SERVER_PUB_NIC -j MASQUERADE

# Client Peer
[Peer]
PublicKey = $clientPublicKey
AllowedIPs = 10.66.66.2/32, fd42:42:42::2/128
EOF

# Start WireGuard
systemctl enable wg-quick@wg0
systemctl start wg-quick@wg0

echo "WireGuard setup complete!"
""".trimIndent()
    }

    /**
     * Parse WireGuard .conf text content into WireGuardConfig
     */
    fun parseConfString(confText: String, serverName: String = "Imported Server"): WireGuardConfig? {
        return try {
            var privateKey = ""
            var address = "10.66.66.2/32"
            var dns = "1.1.1.1"
            var publicKey = ""
            var endpoint = ""
            var allowedIps = "0.0.0.0/0, ::/0"

            val lines = confText.lines()
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.startsWith("PrivateKey", ignoreCase = true)) {
                    privateKey = trimmed.substringAfter("=").trim()
                } else if (trimmed.startsWith("Address", ignoreCase = true)) {
                    address = trimmed.substringAfter("=").trim()
                } else if (trimmed.startsWith("DNS", ignoreCase = true)) {
                    dns = trimmed.substringAfter("=").trim()
                } else if (trimmed.startsWith("PublicKey", ignoreCase = true)) {
                    publicKey = trimmed.substringAfter("=").trim()
                } else if (trimmed.startsWith("Endpoint", ignoreCase = true)) {
                    endpoint = trimmed.substringAfter("=").trim()
                } else if (trimmed.startsWith("AllowedIPs", ignoreCase = true)) {
                    allowedIps = trimmed.substringAfter("=").trim()
                }
            }

            if (privateKey.isNotEmpty() && publicKey.isNotEmpty() && endpoint.isNotEmpty()) {
                WireGuardConfig(
                    serverName = serverName,
                    clientPrivateKey = privateKey,
                    clientAddress = address,
                    dns = dns,
                    serverPublicKey = publicKey,
                    endpoint = endpoint,
                    allowedIps = allowedIps
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }
}
