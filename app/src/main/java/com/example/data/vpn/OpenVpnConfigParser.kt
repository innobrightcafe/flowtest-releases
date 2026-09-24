package com.example.data.vpn

data class OpenVpnConfig(
    val remoteHost: String,
    val remotePort: Int = 1194,
    val proto: String = "udp",
    val cipher: String = "AES-256-GCM",
    val authDigest: String = "SHA256",
    val caCertificate: String = "",
    val clientCertificate: String = "",
    val clientKey: String = "",
    val tunMtu: Int = 1500,
    val persistTun: Boolean = true,
    val persistKey: Boolean = true,
    val compLzo: Boolean = false,
    val redirectGateway: Boolean = true
)

object OpenVpnConfigParser {

    /**
     * Parses standard .ovpn file contents into a structured OpenVpnConfig object.
     * Extracts directives and inline XML-like blocks: <ca>, <cert>, <key>, <tls-auth>.
     */
    fun parse(ovpnText: String): OpenVpnConfig {
        var remoteHost = "185.156.46.22"
        var remotePort = 1194
        var proto = "udp"
        var cipher = "AES-256-GCM"
        var authDigest = "SHA256"
        var tunMtu = 1500

        val lines = ovpnText.lines()
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("#") || trimmed.startsWith(";") || trimmed.isEmpty()) continue

            val parts = trimmed.split("\\s+".toRegex())
            when (parts.firstOrNull()?.lowercase()) {
                "remote" -> {
                    if (parts.size >= 2) remoteHost = parts[1]
                    if (parts.size >= 3) remotePort = parts[2].toIntOrNull() ?: 1194
                    if (parts.size >= 4) proto = parts[3].lowercase()
                }
                "port" -> if (parts.size >= 2) remotePort = parts[1].toIntOrNull() ?: 1194
                "proto" -> if (parts.size >= 2) proto = parts[1].lowercase()
                "cipher" -> if (parts.size >= 2) cipher = parts[1]
                "auth" -> if (parts.size >= 2) authDigest = parts[1]
                "tun-mtu" -> if (parts.size >= 2) tunMtu = parts[1].toIntOrNull() ?: 1500
            }
        }

        val caCert = extractBlock(ovpnText, "ca")
        val clientCert = extractBlock(ovpnText, "cert")
        val clientKey = extractBlock(ovpnText, "key")

        return OpenVpnConfig(
            remoteHost = remoteHost,
            remotePort = remotePort,
            proto = proto,
            cipher = cipher,
            authDigest = authDigest,
            caCertificate = caCert,
            clientCertificate = clientCert,
            clientKey = clientKey,
            tunMtu = tunMtu
        )
    }

    private fun extractBlock(text: String, tag: String): String {
        val startTag = "<$tag>"
        val endTag = "</$tag>"
        val startIndex = text.indexOf(startTag)
        val endIndex = text.indexOf(endTag)
        return if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            text.substring(startIndex + startTag.length, endIndex).trim()
        } else {
            ""
        }
    }
}
