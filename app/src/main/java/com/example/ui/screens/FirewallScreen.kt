package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.db.FirewallAppEntity
import com.example.data.ui.viewmodel.VpnViewModel
import com.example.ui.theme.*

@Composable
fun FirewallScreen(
    viewModel: VpnViewModel,
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val stats by viewModel.dataSaverStats.collectAsStateWithLifecycle()
    val firewallApps by viewModel.firewallApps.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.refreshPhoneDataAccounting(context)
    }

    var selectedFilter by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }

    val verifiedInstalledApps = remember(firewallApps) {
        val pm = context.packageManager
        firewallApps.filter { app ->
            try {
                pm.getApplicationInfo(app.packageName, 0)
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    val filteredApps = remember(verifiedInstalledApps, selectedFilter, searchQuery) {
        verifiedInstalledApps.filter { app ->
            val matchesFilter = when (selectedFilter) {
                "Cellular Blocked" -> app.isCellularBlocked
                "Background Frozen" -> app.isBackgroundFrozen
                "Video & Social" -> app.category == "Video" || app.category == "Social"
                else -> true
            }
            val matchesSearch = searchQuery.isEmpty() || app.appName.contains(searchQuery, ignoreCase = true)
            matchesFilter && matchesSearch
        }.sortedByDescending { it.dataUsageMb }
    }

    val isMasterActive = stats?.isMasterFirewallEnabled ?: true
    val totalAdsBlocked = stats?.totalAdsBlocked ?: 0
    val bytesSaved = stats?.totalBytesSaved ?: 0L

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkObsidian)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("firewall_screen_container")
    ) {
        // Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "NO-ROOT FIREWALL",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp,
                        color = TextPrimary
                    )
                )
                Text(
                    text = "Block Background Depletion & Ad Servers",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
            }

            // Firewall status badge
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isMasterActive) ElectricEmerald.copy(alpha = 0.15f) else WarningRed.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isMasterActive) ElectricEmerald.copy(alpha = 0.4f) else WarningRed.copy(alpha = 0.4f)
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { viewModel.toggleMasterFirewall(!isMasterActive) }
                    .testTag("firewall_master_toggle")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isMasterActive) ElectricEmerald else WarningRed)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isMasterActive) "ACTIVE" else "PAUSED",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isMasterActive) ElectricEmerald else WarningRed
                        )
                    )
                }
            }
        }

        // Live Firewall Analytics Card
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ADS & TRACKERS BLOCKED",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Text(
                        text = "$totalAdsBlocked",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = CyberCyan
                        )
                    )
                    Text(
                        text = "DNS Level Blocking",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                    )
                }

                VerticalDivider(
                    modifier = Modifier
                        .height(40.dp)
                        .width(1.dp),
                    color = DarkCardBorder
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp)
                ) {
                    Text(
                        text = "FIREWALL SAVINGS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Text(
                        text = "${bytesSaved / (1024 * 1024)} MB",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = ElectricEmerald
                        )
                    )
                    Text(
                        text = "Background Data Prevented",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                    )
                }
            }
        }

        // DNS & Ad-blocker quick Toggles Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChipCard(
                title = "Ad Server Shield",
                icon = Icons.Filled.Shield,
                isActive = stats?.isAdBlockerEnabled ?: true,
                onToggle = { viewModel.toggleAdBlocker(!(stats?.isAdBlockerEnabled ?: true)) },
                modifier = Modifier.weight(1f)
            )

            FilterChipCard(
                title = "Tracker Blocker",
                icon = Icons.Filled.Block,
                isActive = stats?.isTrackerBlockerEnabled ?: true,
                onToggle = { viewModel.toggleTrackerBlocker(!(stats?.isTrackerBlockerEnabled ?: true)) },
                modifier = Modifier.weight(1f)
            )
        }

        // Search bar & Filter Pills
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search installed apps...", color = TextMuted, fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = TextSecondary) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
                .testTag("firewall_search_input"),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = DarkSurface,
                unfocusedContainerColor = DarkSurface,
                focusedBorderColor = CyberCyan,
                unfocusedBorderColor = DarkCardBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            singleLine = true
        )

        // Category Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("All", "Cellular Blocked", "Background Frozen", "Video & Social").forEach { filterName ->
                val isSelected = selectedFilter == filterName
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedFilter = filterName },
                    label = { Text(filterName, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyberCyan.copy(alpha = 0.2f),
                        selectedLabelColor = CyberCyan,
                        containerColor = DarkSurface,
                        labelColor = TextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = DarkCardBorder,
                        selectedBorderColor = CyberCyan
                    )
                )
            }
        }

        // App List
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredApps, key = { it.packageName }) { app ->
                AppFirewallRow(
                    app = app,
                    onToggleCellular = { viewModel.toggleCellularBlocked(app.packageName, !app.isCellularBlocked) },
                    onToggleWifi = { viewModel.toggleWifiBlocked(app.packageName, !app.isWifiBlocked) },
                    onToggleBackground = { viewModel.toggleBackgroundFrozen(app.packageName, !app.isBackgroundFrozen) }
                )
            }
        }
    }
}

@Composable
private fun FilterChipCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isActive) CyberCyan.copy(alpha = 0.4f) else DarkCardBorder
        ),
        modifier = modifier.clickable { onToggle() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isActive) CyberCyan else TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isActive) TextPrimary else TextSecondary
                    )
                )
            }

            Switch(
                checked = isActive,
                onCheckedChange = { onToggle() },
                modifier = Modifier.scale(0.7f),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = DarkObsidian,
                    checkedTrackColor = CyberCyan,
                    uncheckedThumbColor = TextMuted,
                    uncheckedTrackColor = DarkSurfaceElevated
                )
            )
        }
    }
}

@Composable
private fun AppFirewallRow(
    app: FirewallAppEntity,
    onToggleCellular: () -> Unit,
    onToggleWifi: () -> Unit,
    onToggleBackground: () -> Unit
) {
    val iconVector = when (app.category.lowercase()) {
        "messaging" -> Icons.AutoMirrored.Filled.Chat
        "social" -> Icons.Filled.People
        "video" -> Icons.Filled.PlayCircle
        "browser" -> Icons.Filled.Public
        "gaming" -> Icons.Filled.SportsEsports
        "system" -> Icons.Filled.Settings
        else -> Icons.Filled.Apps
    }

    val usageText = when {
        app.dataUsageMb >= 1024.0 -> String.format(java.util.Locale.US, "%.2f GB used today", app.dataUsageMb / 1024.0)
        app.dataUsageMb >= 1.0 -> String.format(java.util.Locale.US, "%.1f MB used today", app.dataUsageMb)
        app.dataUsageMb > 0.0 -> String.format(java.util.Locale.US, "%.0f KB used today", app.dataUsageMb * 1024.0)
        else -> "0.0 MB used today"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("app_row_${app.packageName}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (app.isCellularBlocked) WarningRed.copy(alpha = 0.15f) else CyberCyan.copy(
                                alpha = 0.15f
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = app.appName,
                        tint = if (app.isCellularBlocked) WarningRed else CyberCyan,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = app.appName,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = DarkSurfaceElevated
                        ) {
                            Text(
                                text = app.category,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    color = TextSecondary
                                ),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = usageText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (app.dataUsageMb > 500.0) GlowingAmber else TextMuted
                        )
                    )
                }
            }

            // Quick Control Toggles
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cellular Block Toggle Button
                IconButton(
                    onClick = onToggleCellular,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            if (app.isCellularBlocked) WarningRed.copy(alpha = 0.2f) else DarkSurfaceElevated
                        )
                        .border(
                            1.dp,
                            if (app.isCellularBlocked) WarningRed else DarkCardBorder,
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = if (app.isCellularBlocked) Icons.Filled.SignalCellularOff else Icons.Filled.SignalCellular4Bar,
                        contentDescription = "Cellular Toggle",
                        tint = if (app.isCellularBlocked) WarningRed else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Background Freeze Toggle Button
                IconButton(
                    onClick = onToggleBackground,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            if (app.isBackgroundFrozen) CyberCyan.copy(alpha = 0.2f) else DarkSurfaceElevated
                        )
                        .border(
                            1.dp,
                            if (app.isBackgroundFrozen) CyberCyan else DarkCardBorder,
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Filled.AcUnit,
                        contentDescription = "Freeze Background Sync",
                        tint = if (app.isBackgroundFrozen) CyberCyan else TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
