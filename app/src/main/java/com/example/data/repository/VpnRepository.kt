package com.example.data.repository

import com.example.data.api.*
import com.example.data.db.*
import com.example.data.cloud.BackupToCloudService
import com.example.data.cloud.R2BackupResult
import com.example.data.model.DataUsageFilter
import com.example.data.model.NetworkInterfaceFilter
import com.example.data.model.UsagePeriodType
import com.example.data.vpn.WireGuardHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.util.UUID

class VpnRepository(
    private val vpnDao: VpnDao,
    private val hetznerApiService: HetznerApiService = HetznerApiService.create()
) {

    val allServers: Flow<List<ServerEntity>> = vpnDao.getAllServers()
    val hetznerAccount: Flow<HetznerAccountEntity?> = vpnDao.getHetznerAccount()
    val allConfigs: Flow<List<VpnConfigEntity>> = vpnDao.getAllConfigs()
    val connectionLogs: Flow<List<ConnectionLogEntity>> = vpnDao.getConnectionLogs()
    val allFirewallApps: Flow<List<FirewallAppEntity>> = vpnDao.getAllFirewallApps()
    val dataSaverStats: Flow<DataSaverStatsEntity?> = vpnDao.getDataSaverStats()
    val telcoBundles: Flow<List<TelcoBundleEntity>> = vpnDao.getAllBundles()

    suspend fun updateBundles(bundles: List<TelcoBundleEntity>) = withContext(Dispatchers.IO) {
        vpnDao.insertBundles(bundles)
    }

    suspend fun seedDefaultServersIfEmpty() = withContext(Dispatchers.IO) {
        seedFirewallAndBundlesIfEmpty()
        val currentServers = allServers.firstOrNull()
        if (currentServers.isNullOrEmpty()) {
            val defaults = listOf(
                ServerEntity(
                    id = "reseller-uk-lon",
                    countryName = "United Kingdom",
                    countryCode = "GB",
                    flagEmoji = "🇬🇧",
                    cityName = "London (Docklands)",
                    datacenter = "Equinix LD8 London",
                    ipAddress = "185.156.46.22",
                    isHetznerServer = false,
                    pingMs = 78,
                    serverLoadPercent = 28,
                    protocol = "WireGuard",
                    isFavorite = true,
                    status = "Active"
                ),
                ServerEntity(
                    id = "reseller-za-jnb",
                    countryName = "South Africa",
                    countryCode = "ZA",
                    flagEmoji = "🇿🇦",
                    cityName = "Johannesburg",
                    datacenter = "Teraco JB1 NAPAfrica",
                    ipAddress = "102.130.112.18",
                    isHetznerServer = false,
                    pingMs = 65,
                    serverLoadPercent = 22,
                    protocol = "WireGuard",
                    isFavorite = true,
                    status = "Active"
                ),
                ServerEntity(
                    id = "reseller-us-nyc",
                    countryName = "United States",
                    countryCode = "US",
                    flagEmoji = "🇺🇸",
                    cityName = "New York City",
                    datacenter = "Equinix NY4",
                    ipAddress = "198.51.100.84",
                    isHetznerServer = false,
                    pingMs = 112,
                    serverLoadPercent = 35,
                    protocol = "WireGuard",
                    isFavorite = false,
                    status = "Active"
                ),
                ServerEntity(
                    id = "reseller-ae-dxb",
                    countryName = "United Arab Emirates",
                    countryCode = "AE",
                    flagEmoji = "🇦🇪",
                    cityName = "Dubai",
                    datacenter = "Datamena DX1 Dubai",
                    ipAddress = "185.107.56.12",
                    isHetznerServer = false,
                    pingMs = 98,
                    serverLoadPercent = 19,
                    protocol = "WireGuard",
                    isFavorite = false,
                    isProOnly = true,
                    status = "Active"
                ),
                ServerEntity(
                    id = "hetzner-fsn1",
                    countryName = "Germany",
                    countryCode = "DE",
                    flagEmoji = "🇩🇪",
                    cityName = "Frankfurt",
                    datacenter = "Interxion FRA1",
                    ipAddress = "185.12.64.12",
                    isHetznerServer = true,
                    pingMs = 82,
                    serverLoadPercent = 24,
                    protocol = "WireGuard",
                    isFavorite = false,
                    status = "Active"
                ),
                ServerEntity(
                    id = "hetzner-par1",
                    countryName = "France",
                    countryCode = "FR",
                    flagEmoji = "🇫🇷",
                    cityName = "Paris",
                    datacenter = "Telehouse 2",
                    ipAddress = "195.154.122.4",
                    isHetznerServer = true,
                    pingMs = 88,
                    serverLoadPercent = 20,
                    protocol = "WireGuard",
                    isFavorite = false,
                    status = "Active"
                ),
                ServerEntity(
                    id = "hetzner-nbg1",
                    countryName = "Germany",
                    countryCode = "DE",
                    flagEmoji = "🇩🇪",
                    cityName = "Nuremberg",
                    datacenter = "FlowTest Dedicated DE-1",
                    ipAddress = "168.119.122.5",
                    isHetznerServer = true,
                    pingMs = 18,
                    serverLoadPercent = 31,
                    protocol = "WireGuard",
                    isFavorite = false,
                    status = "Active"
                ),
                ServerEntity(
                    id = "hetzner-hel1",
                    countryName = "Finland",
                    countryCode = "FI",
                    flagEmoji = "🇫🇮",
                    cityName = "Helsinki",
                    datacenter = "FlowTest Dedicated FI-1",
                    ipAddress = "95.216.140.22",
                    isHetznerServer = true,
                    pingMs = 28,
                    serverLoadPercent = 18,
                    protocol = "WireGuard",
                    isFavorite = true,
                    status = "Active"
                ),
                ServerEntity(
                    id = "hetzner-ash",
                    countryName = "United States",
                    countryCode = "US",
                    flagEmoji = "🇺🇸",
                    cityName = "Ashburn (East)",
                    datacenter = "FlowTest Dedicated US-E1",
                    ipAddress = "5.161.42.10",
                    isHetznerServer = true,
                    pingMs = 85,
                    serverLoadPercent = 45,
                    protocol = "WireGuard",
                    isFavorite = false,
                    isProOnly = true,
                    status = "Active"
                ),
                ServerEntity(
                    id = "hetzner-hio",
                    countryName = "United States",
                    countryCode = "US",
                    flagEmoji = "🇺🇸",
                    cityName = "Hillsboro (West)",
                    datacenter = "FlowTest Dedicated US-W1",
                    ipAddress = "142.132.210.88",
                    isHetznerServer = true,
                    pingMs = 110,
                    serverLoadPercent = 38,
                    protocol = "WireGuard",
                    isFavorite = false,
                    isProOnly = false,
                    status = "Active"
                ),
                ServerEntity(
                    id = "global-sg",
                    countryName = "Singapore",
                    countryCode = "SG",
                    flagEmoji = "🇸🇬",
                    cityName = "Singapore",
                    datacenter = "FlowTest Dedicated SG-1",
                    ipAddress = "139.99.12.8",
                    isHetznerServer = true,
                    pingMs = 165,
                    serverLoadPercent = 25,
                    protocol = "WireGuard",
                    isFavorite = false,
                    isProOnly = true,
                    status = "Active"
                ),
                ServerEntity(
                    id = "global-tokyo",
                    countryName = "Japan",
                    countryCode = "JP",
                    flagEmoji = "🇯🇵",
                    cityName = "Tokyo",
                    datacenter = "Global TYO-Direct",
                    ipAddress = "103.102.160.5",
                    isHetznerServer = false,
                    pingMs = 142,
                    serverLoadPercent = 52,
                    protocol = "WireGuard",
                    isFavorite = false,
                    status = "Active"
                ),
                ServerEntity(
                    id = "global-london",
                    countryName = "United Kingdom",
                    countryCode = "GB",
                    flagEmoji = "🇬🇧",
                    cityName = "London",
                    datacenter = "Global LND-Core",
                    ipAddress = "178.62.204.11",
                    isHetznerServer = false,
                    pingMs = 32,
                    serverLoadPercent = 40,
                    protocol = "WireGuard",
                    isFavorite = false,
                    status = "Active"
                ),
                ServerEntity(
                    id = "global-amsterdam",
                    countryName = "Netherlands",
                    countryCode = "NL",
                    flagEmoji = "🇳🇱",
                    cityName = "Amsterdam",
                    datacenter = "Global AMS-Fiber",
                    ipAddress = "185.220.101.4",
                    isHetznerServer = false,
                    pingMs = 22,
                    serverLoadPercent = 29,
                    protocol = "WireGuard",
                    isFavorite = false,
                    status = "Active"
                )
            )
            vpnDao.insertServers(defaults)
        }
    }

    suspend fun toggleFavorite(serverId: String, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        vpnDao.updateFavorite(serverId, isFavorite)
    }

    suspend fun saveHetznerApiKey(apiKey: String, projectName: String) = withContext(Dispatchers.IO) {
        val account = HetznerAccountEntity(
            apiKey = apiKey.trim(),
            projectName = if (projectName.isBlank()) "FlowTest Dedicated Cloud" else projectName.trim(),
            updatedAt = System.currentTimeMillis()
        )
        vpnDao.saveHetznerAccount(account)
    }

    suspend fun removeHetznerAccount() = withContext(Dispatchers.IO) {
        vpnDao.clearHetznerAccount()
    }

    /**
     * Fetch active Hetzner Cloud Servers from API using stored or provided Hetzner token
     */
    suspend fun fetchHetznerCloudServers(token: String): Result<List<HetznerServer>> = withContext(Dispatchers.IO) {
        try {
            val formattedToken = if (token.startsWith("Bearer ")) token else "Bearer $token"
            val response = hetznerApiService.getServers(formattedToken)
            if (response.isSuccessful && response.body() != null) {
                val servers = response.body()!!.servers

                // Also convert & save these servers into Room
                val entities = servers.map { s ->
                    val ip = s.publicNet?.ipv4?.ip ?: "185.12.64.${s.id % 250}"
                    ServerEntity(
                        id = "hetzner-cloud-${s.id}",
                        countryName = s.datacenter?.location?.country?.uppercase() ?: "FlowTest Cloud",
                        countryCode = getCountryCodeFromName(s.datacenter?.location?.country ?: "DE"),
                        flagEmoji = getFlagEmoji(s.datacenter?.location?.country ?: "DE"),
                        cityName = s.datacenter?.location?.city ?: "Cloud Instance",
                        datacenter = "FlowTest ${s.datacenter?.name ?: "Cloud"}",
                        ipAddress = ip,
                        hetznerServerId = s.id,
                        isHetznerServer = true,
                        pingMs = (20..90).random(),
                        serverLoadPercent = (15..45).random(),
                        protocol = "WireGuard",
                        status = if (s.status == "running") "Active" else s.status
                    )
                }
                vpnDao.insertServers(entities)
                Result.success(servers)
            } else {
                Result.failure(Exception("Hetzner API HTTP ${response.code()}: ${response.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch & sync live VPN servers from VPNresellers API v4.1 (https://api.vpnresellers.com/docs/v4_1/)
     */
    suspend fun syncVpnResellersServers(token: String? = null): Result<List<VpnResellersServerItem>> = withContext(Dispatchers.IO) {
        try {
            val api = VpnResellersApiService.create(apiKey = token)
            val response = api.getServers()
            val servers = response.servers
            if (servers.isNotEmpty()) {
                val entities = servers.map { s ->
                    ServerEntity(
                        id = s.id,
                        countryName = s.name.ifBlank { s.cityName },
                        countryCode = s.countryCode,
                        flagEmoji = s.flagEmoji.ifBlank { "🌐" },
                        cityName = s.cityName,
                        datacenter = s.datacenter,
                        ipAddress = s.ipAddress,
                        hetznerServerId = null,
                        isHetznerServer = false,
                        pingMs = s.pingMs,
                        serverLoadPercent = s.serverLoadPercent,
                        protocol = s.protocols.firstOrNull() ?: "WireGuard",
                        isProOnly = s.isProOnly,
                        status = s.status
                    )
                }
                vpnDao.insertServers(entities)
                Result.success(servers)
            } else {
                Result.success(emptyList())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Automated deployment of a new Hetzner Cloud VPS instance with WireGuard script
     */
    suspend fun deployHetznerVpnServer(
        token: String,
        serverName: String,
        location: String,
        serverType: String = "cx22"
    ): Result<CreateHetznerServerResponse> = withContext(Dispatchers.IO) {
        try {
            val formattedToken = if (token.startsWith("Bearer ")) token else "Bearer $token"
            val clientPubKey = WireGuardHelper.generateRandomKey()
            val cloudInit = WireGuardHelper.generateHetznerCloudInitScript(clientPubKey)

            val request = CreateHetznerServerRequest(
                name = serverName.ifBlank { "flowtest-vpn-${System.currentTimeMillis() % 10000}" },
                serverType = serverType,
                location = location,
                image = "ubuntu-24.04",
                userData = cloudInit,
                startAfterCreate = true
            )

            val response = hetznerApiService.createServer(formattedToken, request)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val newServer = body.server
                val ip = newServer.publicNet?.ipv4?.ip ?: "185.12.64.${(10..240).random()}"

                val entity = ServerEntity(
                    id = "hetzner-cloud-${newServer.id}",
                    countryName = getCountryNameFromCode(location),
                    countryCode = location.uppercase().take(2),
                    flagEmoji = getFlagEmoji(location),
                    cityName = location.uppercase(),
                    datacenter = "FlowTest Dedicated $location",
                    ipAddress = ip,
                    hetznerServerId = newServer.id,
                    isHetznerServer = true,
                    pingMs = (15..45).random(),
                    serverLoadPercent = 12,
                    protocol = "WireGuard",
                    status = "Deploying"
                )
                vpnDao.insertServer(entity)

                Result.success(body)
            } else {
                Result.failure(Exception("Creation failed (${response.code()}): ${response.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addCustomServer(server: ServerEntity) = withContext(Dispatchers.IO) {
        vpnDao.insertServer(server)
    }

    suspend fun deleteServer(id: String) = withContext(Dispatchers.IO) {
        vpnDao.deleteServer(id)
    }

    suspend fun saveConfig(config: VpnConfigEntity) = withContext(Dispatchers.IO) {
        vpnDao.insertConfig(config)
    }

    suspend fun logConnection(
        serverName: String,
        countryCode: String,
        durationSeconds: Long,
        bytesDownloaded: Long,
        bytesUploaded: Long,
        protocol: String
    ) = withContext(Dispatchers.IO) {
        vpnDao.insertLog(
            ConnectionLogEntity(
                serverName = serverName,
                countryCode = countryCode,
                durationSeconds = durationSeconds,
                bytesDownloaded = bytesDownloaded,
                bytesUploaded = bytesUploaded,
                protocol = protocol
            )
        )
    }

    suspend fun updateCellularBlocked(packageName: String, isBlocked: Boolean) = withContext(Dispatchers.IO) {
        vpnDao.updateCellularBlocked(packageName, isBlocked)
    }

    suspend fun updateFirewallAppStatus(packageName: String, isBlocked: Boolean) = withContext(Dispatchers.IO) {
        vpnDao.updateAppFirewall(packageName, isBlocked)
    }

    suspend fun updateAllAppsFirewall(isBlocked: Boolean) = withContext(Dispatchers.IO) {
        vpnDao.updateAllAppsFirewall(isBlocked)
    }

    suspend fun updateCategoryFirewall(category: String, isBlocked: Boolean) = withContext(Dispatchers.IO) {
        vpnDao.updateCategoryFirewall(category, isBlocked)
    }

    private var lastAppsScanTimestamp: Long = 0L
    private var currentUsageFilter: DataUsageFilter = DataUsageFilter()

    /**
     * Scans real installed applications on the user's phone, collects their actual TrafficStats / NetworkStatsManager data consumption
     * based on the provided period (Daily, Weekly, Monthly, Custom) and network interface (All, Mobile, Wi-Fi).
     * Zero mock/synthetic data: actual bytes queried from the system.
     */
    suspend fun scanAndSyncDeviceApps(
        context: android.content.Context,
        force: Boolean = false,
        filter: DataUsageFilter = currentUsageFilter
    ) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (!force && filter == currentUsageFilter && now - lastAppsScanTimestamp < 60_000L) {
            return@withContext
        }
        currentUsageFilter = filter
        try {
            val pm = context.packageManager

            // Discover launcher activities to identify user-facing apps on the phone
            val launcherIntent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
                addCategory(android.content.Intent.CATEGORY_LAUNCHER)
            }
            val launcherApps = pm.queryIntentActivities(launcherIntent, 0)
            val launcherPkgSet = launcherApps.mapNotNull { it.activityInfo?.packageName }.toSet()

            // Query installed applications
            val installedApps = pm.getInstalledApplications(0)
            val installedPkgSet = installedApps.map { it.packageName }.toSet()

            // 1. PURGE UNINSTALLED APPS: Remove ANY app in the DB that is NOT installed on this device
            val currentDbApps = vpnDao.getAllFirewallApps().firstOrNull() ?: emptyList()
            for (dbApp in currentDbApps) {
                val isInstalled = installedPkgSet.contains(dbApp.packageName) || try {
                    pm.getApplicationInfo(dbApp.packageName, 0)
                    true
                } catch (e: Exception) {
                    false
                }
                if (!isInstalled) {
                    vpnDao.deleteApp(dbApp.packageName)
                }
            }

            val existingAppsMap = vpnDao.getAllFirewallApps().firstOrNull()?.associateBy { it.packageName } ?: emptyMap()
            val updatedAppEntities = mutableListOf<FirewallAppEntity>()

            val (filterStart, filterEnd) = filter.getTimeInterval(now)

            // Pre-query NetworkStatsManager in bulk for all UIDs if Usage Access is granted
            val uidUsageMap = mutableMapOf<Int, Long>()
            val hasUsage = com.example.data.util.NetworkUtils.hasUsageStatsPermission(context)
            if (hasUsage) {
                try {
                    val nsm = context.getSystemService(android.content.Context.NETWORK_STATS_SERVICE) as? android.app.usage.NetworkStatsManager
                    if (nsm != null) {
                        val bucket = android.app.usage.NetworkStats.Bucket()

                        // Query Mobile Network Data if requested by filter
                        if (filter.networkFilter == NetworkInterfaceFilter.ALL || filter.networkFilter == NetworkInterfaceFilter.MOBILE) {
                            try {
                                val mobileStats = nsm.querySummary(android.net.ConnectivityManager.TYPE_MOBILE, null, filterStart, filterEnd)
                                while (mobileStats.hasNextBucket()) {
                                    mobileStats.getNextBucket(bucket)
                                    val u = bucket.uid
                                    val b = bucket.rxBytes + bucket.txBytes
                                    if (b > 0L) {
                                        uidUsageMap[u] = (uidUsageMap[u] ?: 0L) + b
                                    }
                                }
                                mobileStats.close()
                            } catch (e: Exception) {
                                android.util.Log.w("VpnRepository", "Mobile summary query: ${e.message}")
                            }
                        }

                        // Query Wi-Fi Network Data if requested by filter
                        if (filter.networkFilter == NetworkInterfaceFilter.ALL || filter.networkFilter == NetworkInterfaceFilter.WIFI) {
                            try {
                                val wifiStats = nsm.querySummary(android.net.ConnectivityManager.TYPE_WIFI, null, filterStart, filterEnd)
                                while (wifiStats.hasNextBucket()) {
                                    wifiStats.getNextBucket(bucket)
                                    val u = bucket.uid
                                    val b = bucket.rxBytes + bucket.txBytes
                                    if (b > 0L) {
                                        uidUsageMap[u] = (uidUsageMap[u] ?: 0L) + b
                                    }
                                }
                                wifiStats.close()
                            } catch (e: Exception) {
                                android.util.Log.w("VpnRepository", "WiFi summary query: ${e.message}")
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("VpnRepository", "NetworkStatsManager bulk query error: ${e.message}")
                }
            }

            for (appInfo in installedApps) {
                val pkg = appInfo.packageName

                // Do not firewall this VPN application itself to avoid routing deadlocks
                if (pkg == context.packageName) continue

                val isSystemApp = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
                val isUpdatedSystemApp = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                val isLauncherApp = launcherPkgSet.contains(pkg)

                val uid = appInfo.uid
                val appLabel = try {
                    pm.getApplicationLabel(appInfo).toString().trim().ifBlank { pkg.substringAfterLast('.') }
                } catch (e: Exception) {
                    pkg.substringAfterLast('.')
                }

                val bytesUsed = queryAppUidBytes(
                    context = context,
                    uid = uid,
                    filterStart = filterStart,
                    filterEnd = filterEnd,
                    networkFilter = filter.networkFilter,
                    uidUsageMap = uidUsageMap,
                    hasUsagePermission = hasUsage
                )

                // Include if user-facing launcher app, user-installed app, or active network user
                val shouldInclude = isLauncherApp || !isSystemApp || isUpdatedSystemApp || bytesUsed > 0L

                if (shouldInclude) {
                    val category = determineAppCategory(pkg, appLabel, isSystemApp)
                    val existingApp = existingAppsMap[pkg]
                    val existingCellularBlocked = existingApp?.isCellularBlocked ?: false
                    val existingWifiBlocked = existingApp?.isWifiBlocked ?: false
                    val existingFrozen = existingApp?.isBackgroundFrozen ?: false

                    updatedAppEntities.add(
                        FirewallAppEntity(
                            packageName = pkg,
                            appName = appLabel,
                            category = category,
                            isCellularBlocked = existingCellularBlocked,
                            isWifiBlocked = existingWifiBlocked,
                            isBackgroundFrozen = existingFrozen,
                            bytesUsedToday = bytesUsed,
                            iconName = pkg
                        )
                    )
                }
            }

            // Arrange apps strictly according to highest consumers in this period (highest bytes first)
            updatedAppEntities.sortByDescending { it.bytesUsedToday }
            vpnDao.insertFirewallApps(updatedAppEntities)
            lastAppsScanTimestamp = now
        } catch (e: Exception) {
            android.util.Log.e("VpnRepository", "Error syncing device apps: ${e.message}")
        }
    }

    fun getOrInit24HourCycleStart(context: android.content.Context): Long {
        val prefs = context.getSharedPreferences("daily_cycle_prefs", android.content.Context.MODE_PRIVATE)
        var cycleStart = prefs.getLong("daily_usage_cycle_start_time", 0L)
        val now = System.currentTimeMillis()
        val midnightToday = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis

        val isDifferentDay = if (cycleStart > 0L) {
            val cal1 = java.util.Calendar.getInstance().apply { timeInMillis = cycleStart }
            val cal2 = java.util.Calendar.getInstance().apply { timeInMillis = now }
            cal1.get(java.util.Calendar.YEAR) != cal2.get(java.util.Calendar.YEAR) ||
            cal1.get(java.util.Calendar.DAY_OF_YEAR) != cal2.get(java.util.Calendar.DAY_OF_YEAR)
        } else true

        if (cycleStart <= 0L || isDifferentDay || cycleStart > now) {
            cycleStart = midnightToday
            val editor = prefs.edit()
            editor.putLong("daily_usage_cycle_start_time", midnightToday)
            try {
                val pm = context.packageManager
                val installed = pm.getInstalledApplications(0)
                for (app in installed) {
                    val uid = app.uid
                    val rx = android.net.TrafficStats.getUidRxBytes(uid).takeIf { it > 0 } ?: 0L
                    val tx = android.net.TrafficStats.getUidTxBytes(uid).takeIf { it > 0 } ?: 0L
                    editor.putLong("uid_baseline_$uid", rx + tx)
                }
            } catch (e: Exception) {
                // Ignore
            }
            editor.apply()
        }
        return cycleStart
    }

    suspend fun resetDailyAppUsageCycle(context: android.content.Context, resetTimestamp: Long = System.currentTimeMillis()) = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences("daily_cycle_prefs", android.content.Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putLong("daily_usage_cycle_start_time", resetTimestamp)

        try {
            val pm = context.packageManager
            val installed = pm.getInstalledApplications(0)
            for (app in installed) {
                val uid = app.uid
                val rx = android.net.TrafficStats.getUidRxBytes(uid).takeIf { it > 0 } ?: 0L
                val tx = android.net.TrafficStats.getUidTxBytes(uid).takeIf { it > 0 } ?: 0L
                val live = rx + tx
                if (live > 0L) {
                    editor.putLong("uid_baseline_$uid", live)
                } else {
                    editor.remove("uid_baseline_$uid")
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
        editor.apply()

        // Reset database usage counters to 0
        try {
            vpnDao.resetAllAppsBytesUsedToday()
        } catch (_: Exception) {}

        // Sync apps immediately for the fresh cycle
        try {
            scanAndSyncDeviceApps(context, force = true)
        } catch (e: Exception) {
            android.util.Log.e("VpnRepository", "Failed to resync app bytes in DB: ${e.message}")
        }
    }

    private fun queryAppUidBytes(
        context: android.content.Context,
        uid: Int,
        filterStart: Long,
        filterEnd: Long,
        networkFilter: NetworkInterfaceFilter,
        uidUsageMap: Map<Int, Long>,
        hasUsagePermission: Boolean
    ): Long {
        // 1. First check bulk pre-queried NetworkStatsManager stats
        val bulkBytes = uidUsageMap[uid]
        if (bulkBytes != null && bulkBytes > 0L) {
            return bulkBytes
        }

        // 1b. Targeted UID query if bulk missed it (e.g. multi-user or specialized virtual interface)
        if (hasUsagePermission) {
            try {
                val nsm = context.getSystemService(android.content.Context.NETWORK_STATS_SERVICE) as? android.app.usage.NetworkStatsManager
                if (nsm != null) {
                    var intervalBytes = 0L
                    val bucket = android.app.usage.NetworkStats.Bucket()

                    if (networkFilter == NetworkInterfaceFilter.ALL || networkFilter == NetworkInterfaceFilter.MOBILE) {
                        try {
                            val mobStats = nsm.queryDetailsForUid(
                                android.net.ConnectivityManager.TYPE_MOBILE,
                                null,
                                filterStart,
                                filterEnd,
                                uid
                            )
                            while (mobStats.hasNextBucket()) {
                                mobStats.getNextBucket(bucket)
                                intervalBytes += bucket.rxBytes + bucket.txBytes
                            }
                            mobStats.close()
                        } catch (_: Exception) {}
                    }

                    if (networkFilter == NetworkInterfaceFilter.ALL || networkFilter == NetworkInterfaceFilter.WIFI) {
                        try {
                            val wifiStats = nsm.queryDetailsForUid(
                                android.net.ConnectivityManager.TYPE_WIFI,
                                null,
                                filterStart,
                                filterEnd,
                                uid
                            )
                            while (wifiStats.hasNextBucket()) {
                                wifiStats.getNextBucket(bucket)
                                intervalBytes += bucket.rxBytes + bucket.txBytes
                            }
                            wifiStats.close()
                        } catch (_: Exception) {}
                    }

                    return intervalBytes // Real bytes: strictly 0L if unused during this specific filter interval
                }
            } catch (_: Exception) {}

            // When system Usage Access is granted and the app had 0 network traffic in this interval, return 0L
            return 0L
        }

        // 2. Kernel TrafficStats per-UID fallback (ONLY when UsageStats permission has not been granted by user)
        try {
            val rx = android.net.TrafficStats.getUidRxBytes(uid).takeIf { it > 0 } ?: 0L
            val tx = android.net.TrafficStats.getUidTxBytes(uid).takeIf { it > 0 } ?: 0L
            val liveBytes = rx + tx

            if (liveBytes > 0L) {
                val prefs = context.getSharedPreferences("daily_cycle_prefs", android.content.Context.MODE_PRIVATE)
                val baselineKey = "uid_baseline_$uid"
                val baseline = prefs.getLong(baselineKey, -1L)
                if (baseline == -1L || baseline <= 0L) {
                    // Start baseline now at current liveBytes - do NOT attribute boot-lifetime bytes to current usage
                    prefs.edit().putLong(baselineKey, liveBytes).apply()
                    return 0L
                } else if (liveBytes >= baseline) {
                    return liveBytes - baseline
                } else {
                    // Phone rebooted, reset baseline
                    prefs.edit().putLong(baselineKey, liveBytes).apply()
                    return 0L
                }
            }
        } catch (_: Exception) {}

        // ZERO mock data. If the app wasn't used or has no traffic, return 0L.
        return 0L
    }

    private fun determineAppCategory(packageName: String, label: String, isSystem: Boolean): String {
        val lowerPkg = packageName.lowercase()
        val lowerLabel = label.lowercase()
        return when {
            lowerPkg.contains("whatsapp") || lowerPkg.contains("telegram") || lowerPkg.contains("signal") || lowerPkg.contains("messenger") || lowerPkg.contains("messages") -> "Messaging"
            lowerPkg.contains("instagram") || lowerPkg.contains("twitter") || lowerPkg.contains("facebook") || lowerPkg.contains("snapchat") || lowerPkg.contains("linkedin") || lowerPkg.contains("reddit") -> "Social"
            lowerPkg.contains("youtube") || lowerPkg.contains("netflix") || lowerPkg.contains("tiktok") || lowerPkg.contains("spotify") || lowerPkg.contains("vlc") || lowerPkg.contains("primevideo") || lowerPkg.contains("twitch") -> "Video"
            lowerPkg.contains("chrome") || lowerPkg.contains("firefox") || lowerPkg.contains("browser") || lowerPkg.contains("opera") || lowerPkg.contains("edge") -> "Browser"
            lowerPkg.contains("game") || lowerPkg.contains("pubg") || lowerPkg.contains("roblox") || lowerPkg.contains("clash") || lowerPkg.contains("candy") -> "Gaming"
            isSystem -> "System"
            else -> "Productivity"
        }
    }

    /**
     * Smart Data Saver Engine:
     * Freezes background network traffic on data-heavy Social and Video streaming apps
     * while preserving critical Messaging & Phone apps.
     */
    suspend fun applySmartDataSaverRules(mode: String = "SMART_BALANCED") = withContext(Dispatchers.IO) {
        val apps = vpnDao.getAllFirewallApps().firstOrNull() ?: return@withContext
        val updated = apps.map { app ->
            when (mode) {
                "AGGRESSIVE" -> {
                    // Restrict everything except critical messaging
                    val isCritical = app.category.equals("Messaging", ignoreCase = true) || app.appName.contains("Phone", ignoreCase = true)
                    app.copy(isCellularBlocked = !isCritical, isBackgroundFrozen = !isCritical)
                }
                "SMART_BALANCED" -> {
                    // Restrict heavy video and social feeds from draining mobile data in background
                    val isHeavyConsumer = app.category.equals("Video", ignoreCase = true) || 
                                          app.category.equals("Social", ignoreCase = true) ||
                                          app.dataUsageMb > 250.0
                    if (isHeavyConsumer) {
                        app.copy(isBackgroundFrozen = true, isCellularBlocked = false)
                    } else {
                        app
                    }
                }
                "UNRESTRICT_ALL" -> {
                    app.copy(isCellularBlocked = false, isBackgroundFrozen = false)
                }
                else -> app
            }
        }
        vpnDao.insertFirewallApps(updated)
    }

    suspend fun updateWifiBlocked(packageName: String, isBlocked: Boolean) = withContext(Dispatchers.IO) {
        vpnDao.updateWifiBlocked(packageName, isBlocked)
    }

    suspend fun updateBackgroundFrozen(packageName: String, isFrozen: Boolean) = withContext(Dispatchers.IO) {
        vpnDao.updateBackgroundFrozen(packageName, isFrozen)
    }

    suspend fun updateDataSaverStats(stats: DataSaverStatsEntity) = withContext(Dispatchers.IO) {
        vpnDao.insertOrUpdateStats(stats)
    }

    private suspend fun seedFirewallAndBundlesIfEmpty() = withContext(Dispatchers.IO) {
        // Strictly DO NOT seed mock or fake firewall apps (such as fake Netflix or TikTok).
        // Real apps installed on the device are dynamically tracked and synced via scanAndSyncDeviceApps(context).
        
        // Clean any legacy mock packages if they exist in DB
        vpnDao.deleteKnownMockApps()
        val legacyMockPackages = listOf(
            "com.netflix.mediaclient",
            "com.netflix",
            "netflix",
            "com.zhiliaoapp.musically"
        )
        for (pkg in legacyMockPackages) {
            vpnDao.deleteApp(pkg)
        }

        val stats = dataSaverStats.firstOrNull()
        if (stats == null) {
            vpnDao.insertOrUpdateStats(
                DataSaverStatsEntity(
                    id = 1,
                    dailyLimitMb = 1000,
                    isMasterFirewallEnabled = true,
                    isAdBlockerEnabled = true,
                    isTrackerBlockerEnabled = true,
                    isCompressionProxyEnabled = true,
                    isHighDepletionAlertEnabled = true,
                    totalBytesSaved = 0L,
                    totalAdsBlocked = 0,
                    streakDays = 1
                )
            )
        } else if (stats.totalAdsBlocked == 2840 || stats.totalBytesSaved == 582_000_000L) {
            // Reset old mock values to 0
            vpnDao.insertOrUpdateStats(
                stats.copy(
                    totalBytesSaved = 0L,
                    totalAdsBlocked = 0,
                    streakDays = 1
                )
            )
        }

        val bundles = telcoBundles.firstOrNull()
        if (bundles.isNullOrEmpty()) {
            val defaultBundles = PairgateVerifiedPlans.ALL_PLANS.filter { it.isAvailable() }.take(20).map { plan ->
                val retailPrice = PairgateVerifiedPlans.getRetailPrice(plan.getEffectivePrice())
                val dataMbVal: Long = when {
                    plan.getEffectiveName().contains("500MB", ignoreCase = true) -> 500L
                    plan.getEffectiveName().contains("2GB", ignoreCase = true) -> 2000L
                    plan.getEffectiveName().contains("3GB", ignoreCase = true) -> 3000L
                    plan.getEffectiveName().contains("5GB", ignoreCase = true) -> 5000L
                    else -> 1000L
                }
                TelcoBundleEntity(
                    id = plan.getEffectivePlanId(),
                    provider = plan.getEffectiveProvider(),
                    planName = plan.getEffectiveName(),
                    dataMb = dataMbVal,
                    priceNaira = retailPrice.toInt(),
                    durationDays = plan.getEffectiveDurationDays(),
                    ussdCode = when (plan.getEffectiveProvider().uppercase()) {
                        "MTN" -> "*312*4*7#"
                        else -> "*323#"
                    },
                    isRecommended = plan.isHotPlan()
                )
            }
            vpnDao.insertBundles(defaultBundles)
        }
    }

    private fun getFlagEmoji(country: String): String {
        return when (country.lowercase()) {
            "de", "germany", "falkenstein", "nuremberg" -> "🇩🇪"
            "fi", "finland", "helsinki" -> "🇫🇮"
            "us", "usa", "united states", "ashburn", "hillsboro" -> "🇺🇸"
            "sg", "singapore" -> "🇸🇬"
            "jp", "japan", "tokyo" -> "🇯🇵"
            "gb", "uk", "united kingdom", "london" -> "🇬🇧"
            "nl", "netherlands", "amsterdam" -> "🇳🇱"
            "au", "australia", "sydney" -> "🇦🇺"
            "fr", "france", "paris" -> "🇫🇷"
            "ca", "canada", "toronto" -> "🇨🇦"
            else -> "🌐"
        }
    }

    private fun getCountryCodeFromName(country: String): String {
        return when (country.lowercase()) {
            "germany", "de" -> "DE"
            "finland", "fi" -> "FI"
            "united states", "us", "usa" -> "US"
            "singapore", "sg" -> "SG"
            "japan", "jp" -> "JP"
            "united kingdom", "gb", "uk" -> "GB"
            "netherlands", "nl" -> "NL"
            "france", "fr" -> "FR"
            else -> "EU"
        }
    }

    private fun getCountryNameFromCode(code: String): String {
        return when (code.lowercase()) {
            "nbg1", "fsn1", "de" -> "Germany"
            "hel1", "fi" -> "Finland"
            "ash", "hio", "us" -> "United States"
            "sin", "sg" -> "Singapore"
            "jp" -> "Japan"
            "gb" -> "United Kingdom"
            else -> "FlowTest Cloud Zone"
        }
    }

    /**
     * Backup and synchronize chat messages, transcripts, and metadata to Cloudflare R2.
     * Can be invoked directly from chat repository with a chatId and optional conversation data.
     */
    suspend fun syncChatToR2(
        context: android.content.Context,
        chatId: String,
        conversation: com.example.data.ui.viewmodel.VpnViewModel.SmsConversationItem? = null
    ): R2BackupResult = withContext(Dispatchers.IO) {
        if (conversation != null) {
            BackupToCloudService.uploadChatTranscriptToR2(context, conversation)
        } else {
            // If conversation instance is not passed directly, construct fallback backup payload
            val fallbackItem = com.example.data.ui.viewmodel.VpnViewModel.SmsConversationItem(
                id = chatId,
                recipientPhone = chatId,
                recipientName = "Chat $chatId",
                lastMessage = "Backup Snapshot",
                messages = emptyList<com.example.data.ui.viewmodel.VpnViewModel.SmsMessageItem>()
            )
            BackupToCloudService.uploadChatTranscriptToR2(context, fallbackItem)
        }
    }
}
