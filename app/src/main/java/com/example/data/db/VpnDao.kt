package com.example.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface VpnDao {
    // Server Queries
    @Query("SELECT * FROM vpn_servers ORDER BY isFavorite DESC, pingMs ASC")
    fun getAllServers(): Flow<List<ServerEntity>>

    @Query("SELECT * FROM vpn_servers WHERE id = :id")
    suspend fun getServerById(id: String): ServerEntity?

    @Query("SELECT * FROM vpn_servers WHERE isHetznerServer = 1 ORDER BY pingMs ASC")
    fun getHetznerServers(): Flow<List<ServerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServers(servers: List<ServerEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServer(server: ServerEntity)

    @Query("UPDATE vpn_servers SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: String, isFavorite: Boolean)

    @Query("DELETE FROM vpn_servers WHERE id = :id")
    suspend fun deleteServer(id: String)

    // Hetzner Account Queries
    @Query("SELECT * FROM hetzner_accounts WHERE id = 1")
    fun getHetznerAccount(): Flow<HetznerAccountEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveHetznerAccount(account: HetznerAccountEntity)

    @Query("DELETE FROM hetzner_accounts WHERE id = 1")
    suspend fun clearHetznerAccount()

    // VPN Config Queries
    @Query("SELECT * FROM vpn_configs ORDER BY isDefault DESC, id DESC")
    fun getAllConfigs(): Flow<List<VpnConfigEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: VpnConfigEntity): Long

    @Query("DELETE FROM vpn_configs WHERE id = :id")
    suspend fun deleteConfig(id: Long)

    // Connection Logs
    @Query("SELECT * FROM connection_logs ORDER BY timestamp DESC LIMIT 50")
    fun getConnectionLogs(): Flow<List<ConnectionLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ConnectionLogEntity)

    @Query("DELETE FROM connection_logs")
    suspend fun clearLogs()

    // Firewall Apps
    @Query("SELECT * FROM firewall_apps ORDER BY bytesUsedToday DESC")
    fun getAllFirewallApps(): Flow<List<FirewallAppEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFirewallApps(apps: List<FirewallAppEntity>)

    @Query("DELETE FROM firewall_apps WHERE packageName NOT IN (:installedPackages)")
    suspend fun deleteAppsNotIn(installedPackages: List<String>)

    @Query("DELETE FROM firewall_apps WHERE packageName = :packageName")
    suspend fun deleteApp(packageName: String)

    @Query("DELETE FROM firewall_apps WHERE packageName LIKE '%netflix%' OR packageName LIKE '%tiktok%' OR appName LIKE '%Netflix%' OR appName LIKE '%TikTok%'")
    suspend fun deleteKnownMockApps()

    @Query("DELETE FROM firewall_apps")
    suspend fun clearAllFirewallApps()

    @Query("UPDATE firewall_apps SET isCellularBlocked = :isBlocked WHERE packageName = :packageName")
    suspend fun updateCellularBlocked(packageName: String, isBlocked: Boolean)

    @Query("UPDATE firewall_apps SET isWifiBlocked = :isBlocked WHERE packageName = :packageName")
    suspend fun updateWifiBlocked(packageName: String, isBlocked: Boolean)

    @Query("UPDATE firewall_apps SET isBackgroundFrozen = :isFrozen WHERE packageName = :packageName")
    suspend fun updateBackgroundFrozen(packageName: String, isFrozen: Boolean)

    @Query("UPDATE firewall_apps SET isCellularBlocked = :isBlocked, isBackgroundFrozen = :isBlocked WHERE packageName = :packageName")
    suspend fun updateAppFirewall(packageName: String, isBlocked: Boolean)

    @Query("UPDATE firewall_apps SET isCellularBlocked = :isBlocked, isBackgroundFrozen = :isBlocked")
    suspend fun updateAllAppsFirewall(isBlocked: Boolean)

    @Query("UPDATE firewall_apps SET isCellularBlocked = :isBlocked, isBackgroundFrozen = :isBlocked WHERE category = :category")
    suspend fun updateCategoryFirewall(category: String, isBlocked: Boolean)

    @Query("SELECT * FROM firewall_apps WHERE packageName = :packageName LIMIT 1")
    suspend fun getApp(packageName: String): FirewallAppEntity?

    @Query("UPDATE firewall_apps SET bytesUsedToday = 0")
    suspend fun resetAllAppsBytesUsedToday()

    // Data Saver Stats
    @Query("SELECT * FROM data_saver_stats WHERE id = 1")
    fun getDataSaverStats(): Flow<DataSaverStatsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateStats(stats: DataSaverStatsEntity)

    // Telco Bundles
    @Query("SELECT * FROM telco_bundles ORDER BY isRecommended DESC, priceNaira ASC")
    fun getAllBundles(): Flow<List<TelcoBundleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBundles(bundles: List<TelcoBundleEntity>)
}
