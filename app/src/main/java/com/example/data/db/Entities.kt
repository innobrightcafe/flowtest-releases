package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vpn_servers")
data class ServerEntity(
    @PrimaryKey val id: String,
    val countryName: String,
    val countryCode: String,
    val flagEmoji: String,
    val cityName: String,
    val datacenter: String,
    val ipAddress: String,
    val hetznerServerId: Long? = null,
    val isHetznerServer: Boolean = false,
    val pingMs: Int,
    val serverLoadPercent: Int,
    val protocol: String = "WireGuard",
    val isFavorite: Boolean = false,
    val isProOnly: Boolean = false,
    val status: String = "Active"
)

@Entity(tableName = "hetzner_accounts")
data class HetznerAccountEntity(
    @PrimaryKey val id: Int = 1,
    val apiKey: String,
    val projectName: String = "Hetzner Cloud VPN",
    val serverCount: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "vpn_configs")
data class VpnConfigEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val privateKey: String,
    val address: String,
    val dns: String,
    val publicKey: String,
    val endpoint: String,
    val allowedIps: String = "0.0.0.0/0, ::/0",
    val presharedKey: String? = null,
    val isDefault: Boolean = false
)

@Entity(tableName = "connection_logs")
data class ConnectionLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val serverName: String,
    val countryCode: String,
    val durationSeconds: Long,
    val bytesDownloaded: Long,
    val bytesUploaded: Long,
    val protocol: String
)

@Entity(tableName = "firewall_apps")
data class FirewallAppEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val category: String,
    val isCellularBlocked: Boolean = false,
    val isWifiBlocked: Boolean = false,
    val isBackgroundFrozen: Boolean = false,
    val bytesUsedToday: Long = 0L,
    val iconName: String = "default"
) {
    val isBlocked: Boolean
        get() = isCellularBlocked || isWifiBlocked || isBackgroundFrozen

    val dataUsageMb: Double
        get() = bytesUsedToday / (1024.0 * 1024.0)
}

@Entity(tableName = "data_saver_stats")
data class DataSaverStatsEntity(
    @PrimaryKey val id: Int = 1,
    val dailyLimitMb: Int = 1000,
    val isMasterFirewallEnabled: Boolean = true,
    val isAdBlockerEnabled: Boolean = true,
    val isTrackerBlockerEnabled: Boolean = true,
    val isCompressionProxyEnabled: Boolean = true,
    val isHighDepletionAlertEnabled: Boolean = true,
    val totalBytesSaved: Long = 0L,
    val totalAdsBlocked: Int = 0,
    val streakDays: Int = 1
)

@Entity(tableName = "telco_bundles")
data class TelcoBundleEntity(
    @PrimaryKey val id: String,
    val provider: String,
    val planName: String,
    val dataMb: Long,
    val priceNaira: Int,
    val durationDays: Int,
    val ussdCode: String,
    val isRecommended: Boolean = false
)
