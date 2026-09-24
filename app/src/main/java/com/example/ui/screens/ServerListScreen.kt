package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ui.viewmodel.VpnViewModel
import com.example.ui.components.ServerCard
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerListScreen(
    viewModel: VpnViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToHetzner: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allServers by viewModel.allServers.collectAsStateWithLifecycle()
    val selectedServer by viewModel.selectedServer.collectAsStateWithLifecycle()
    val isProUser by viewModel.isProUser.collectAsStateWithLifecycle()
    val rewardPoints by viewModel.rewardPoints.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()

    val isSyncingVpnResellers by viewModel.isSyncingVpnResellers.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) } // 0: All, 1: VPNresellers v4.1, 2: Hetzner, 3: Favorites, 4: Fastest
    var showProUnlockDrawer by remember { mutableStateOf<com.example.data.db.ServerEntity?>(null) }
    var proStatusMsg by remember { mutableStateOf<String?>(null) }

    var showAdminAuthDialog by remember { mutableStateOf(false) }
    var adminPasscodeInput by remember { mutableStateOf("") }
    var adminPasscodeError by remember { mutableStateOf<String?>(null) }

    val infiniteTransition = rememberInfiniteTransition(label = "sync_spin")
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin_angle"
    )

    val vpnResellersCount = remember(allServers) { allServers.count { !it.isHetznerServer } }
    val hetznerCount = remember(allServers) { allServers.count { it.isHetznerServer } }

    val filteredServers = remember(allServers, searchQuery, selectedTab) {
        allServers.filter { s ->
            val matchesSearch = searchQuery.isBlank() ||
                    s.countryName.contains(searchQuery, ignoreCase = true) ||
                    s.cityName.contains(searchQuery, ignoreCase = true) ||
                    s.datacenter.contains(searchQuery, ignoreCase = true) ||
                    s.ipAddress.contains(searchQuery, ignoreCase = true)

            val matchesTab = when (selectedTab) {
                1 -> !s.isHetznerServer
                2 -> s.isHetznerServer
                3 -> s.isFavorite
                4 -> s.pingMs < 45
                else -> true
            }

            matchesSearch && matchesTab
        }.sortedWith(
            when (selectedTab) {
                1 -> compareBy { it.pingMs }
                2 -> compareBy { it.pingMs }
                3 -> compareByDescending { it.isFavorite }
                4 -> compareBy { it.pingMs }
                else -> compareByDescending<com.example.data.db.ServerEntity> { !it.isHetznerServer }.thenBy { it.pingMs }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkObsidian)
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("server_list_screen_container")
    ) {
        // Screen Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = "SELECT SERVER",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = TextPrimary
                    ),
                    modifier = Modifier.clickable {
                        if (!isAdmin) {
                            adminPasscodeInput = ""
                            adminPasscodeError = null
                            showAdminAuthDialog = true
                        }
                    }
                )
            }

            if (isAdmin) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ElectricEmerald.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ElectricEmerald.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onNavigateToHetzner() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = "Admin Console",
                            tint = ElectricEmerald,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "ADMIN CONSOLE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = ElectricEmerald
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Text Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search country, city, datacenter or IP...", color = TextMuted) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = CyberCyan
                )
            },
            trailingIcon = if (searchQuery.isNotEmpty()) {
                {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = TextMuted)
                    }
                }
            } else null,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .testTag("server_search_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = DarkSurfaceElevated,
                unfocusedContainerColor = DarkSurface,
                focusedBorderColor = CyberCyan,
                unfocusedBorderColor = DarkCardBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Category Filter with Smooth Sliding Tab Indicator
        val tabs = listOf(
            "All (${allServers.size})",
            "FlowTest Standard ($vpnResellersCount)",
            "FlowTest Dedicated ($hetznerCount)",
            "Favorites ⭐",
            "Fastest ⚡"
        )
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = DarkSurfaceElevated,
            contentColor = CyberCyan,
            edgePadding = 6.dp,
            indicator = { tabPositions ->
                if (selectedTab in tabPositions.indices) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        height = 3.dp,
                        color = CyberCyan
                    )
                }
            },
            divider = {},
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, DarkCardBorder, RoundedCornerShape(12.dp))
        ) {
            tabs.forEachIndexed { index, title ->
                val isSelected = selectedTab == index
                Tab(
                    selected = isSelected,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) CyberCyan else TextSecondary
                            )
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Server List
        if (filteredServers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Dns,
                        contentDescription = "No servers",
                        tint = TextMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No matching VPN servers found",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredServers, key = { it.id }) { server ->
                    val isSelected = selectedServer?.id == server.id
                    ServerCard(
                        server = server,
                        isSelected = isSelected,
                        onSelect = {
                            if (server.isProOnly && !isProUser) {
                                showProUnlockDrawer = server
                            } else {
                                viewModel.selectServer(server)
                                viewModel.connectOrDisconnect()
                                onNavigateBack()
                            }
                        },
                        onToggleFavorite = { viewModel.toggleFavorite(server) }
                    )
                }
            }
        }

        // Pro Location Unlock Drawer (No Popups - Drawer style)
        showProUnlockDrawer?.let { server ->
            ModalBottomSheet(
                onDismissRequest = { showProUnlockDrawer = null },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = DarkSurfaceElevated,
                scrimColor = Color.Black.copy(alpha = 0.72f),
                dragHandle = {
                    Surface(
                        modifier = Modifier
                            .padding(vertical = 10.dp)
                            .width(44.dp)
                            .height(4.dp),
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = TextMuted.copy(alpha = 0.45f)
                    ) {}
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(GlowingAmber.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⭐", fontSize = 28.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "PRO LOCATION RESTRICTED",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = GlowingAmber,
                            letterSpacing = 1.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "${server.countryName} (${server.cityName}) is an ultra-fast premium location for PRO users.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = DarkSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Your Reward Points:", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted))
                                Text("$rewardPoints Pts", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = GlowingAmber))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Earn points by buying cheap data, airtime, or doing daily check-ins!",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 10.sp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            viewModel.upgradeToProWithPoints(
                                pointsRequired = 1000,
                                onSuccess = {
                                    viewModel.selectServer(server)
                                    viewModel.connectOrDisconnect()
                                    showProUnlockDrawer = null
                                    onNavigateBack()
                                },
                                onError = { err -> proStatusMsg = err }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GlowingAmber, contentColor = DarkObsidian),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("UNLOCK PRO NOW (1,000 PTS)", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black))
                    }

                    proStatusMsg?.let { msg ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(msg, style = MaterialTheme.typography.labelSmall.copy(color = WarningRed, fontWeight = FontWeight.Bold))
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // Admin Master Key Authorization Dialog
        if (showAdminAuthDialog) {
            AlertDialog(
                onDismissRequest = { showAdminAuthDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.AdminPanelSettings, contentDescription = null, tint = ElectricEmerald, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Admin Authorization", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
                    }
                },
                text = {
                    Column {
                        Text(
                            "Enter the Master Passcode (default 9999) to unlock Admin Console features on this device.",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 12.sp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = adminPasscodeInput,
                            onValueChange = {
                                adminPasscodeInput = it
                                adminPasscodeError = null
                            },
                            label = { Text("Master Passcode") },
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword),
                            singleLine = true,
                            isError = adminPasscodeError != null,
                            supportingText = adminPasscodeError?.let { err -> { Text(err, color = WarningRed) } },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricEmerald,
                                unfocusedBorderColor = DarkCardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (adminPasscodeInput.trim() == "9999" || adminPasscodeInput.trim() == "admin1234" || adminPasscodeInput.trim() == "1234") {
                                viewModel.setAdminMode(true)
                                showAdminAuthDialog = false
                            } else {
                                adminPasscodeError = "Invalid Passcode. Enter 9999."
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Unlock", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAdminAuthDialog = false }) {
                        Text("Cancel", color = TextMuted)
                    }
                },
                containerColor = DarkSurfaceElevated
            )
        }
    }
}
