package com.example.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ui.viewmodel.VpnViewModel
import com.example.ui.motion.MotionTransitions
import com.example.ui.screens.*
import com.example.ui.theme.*

sealed class Screen(val route: String, val title: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    object Home : Screen("home", "Connect", Icons.Filled.Shield, Icons.Outlined.Shield)
    object Sms : Screen("sms", "Chats", Icons.AutoMirrored.Filled.Chat, Icons.AutoMirrored.Outlined.Chat)
    object Browser : Screen("browser", "Browser", Icons.Filled.Language, Icons.Outlined.Language)
    object DataSaver : Screen("data_saver", "Services", Icons.Filled.DataUsage, Icons.Outlined.DataUsage)
    object DataManagement : Screen("data_management", "Data Control", Icons.Filled.Security, Icons.Outlined.Security)
    object Firewall : Screen("firewall", "Firewall", Icons.Filled.Security, Icons.Outlined.Security)
    object Rewards : Screen("rewards", "Rewards", Icons.Filled.Redeem, Icons.Outlined.Redeem)
    object Servers : Screen("servers", "Locations", Icons.Filled.Dns, Icons.Outlined.Dns)
    object Hetzner : Screen("hetzner", "Admin", Icons.Filled.AdminPanelSettings, Icons.Outlined.AdminPanelSettings)
    object SpeedTest : Screen("speed_test", "Speed", Icons.Filled.Speed, Icons.Outlined.Speed)
    object Settings : Screen("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
    object BulkSms : Screen("bulk_sms", "Bulk SMS", Icons.Filled.SendToMobile, Icons.Outlined.SendToMobile)
    object ElectricityDashboard : Screen("electricity_dashboard", "Power", Icons.Filled.Bolt, Icons.Outlined.Bolt)
}

@Composable
fun MainNavGraph(
    viewModel: VpnViewModel,
    modifier: Modifier = Modifier
) {
    val isAppLoggedIn by viewModel.isAppLoggedIn.collectAsStateWithLifecycle()
    val isSessionLocked by viewModel.isSessionLocked.collectAsStateWithLifecycle()

    if (!isAppLoggedIn) {
        AuthFlowScreen(
            viewModel = viewModel,
            onAuthenticated = {
                viewModel.setAppLoggedIn(true)
            }
        )
    } else if (isSessionLocked) {
        SessionLockScreen(
            viewModel = viewModel,
            onUnlocked = {
                viewModel.unlockSession()
            }
        )
    } else {
        // Zero-Flicker Screen State Architecture:
        // Navigation driven entirely by an in-memory lightweight state variable (currentScreen).
        // Compose doesn't recreate activity or restart pages; it smoothly swaps Composables at 60-120fps.
        var currentScreen by rememberSaveable { mutableStateOf(Screen.Home.route) }
        var backStack by rememberSaveable { mutableStateOf(listOf(Screen.Home.route)) }
        var isBackTransition by remember { mutableStateOf(false) }

        val items = listOf(
            Screen.Home,
            Screen.Sms,
            Screen.DataSaver,
            Screen.Browser,
            Screen.Servers
        )

        val primaryTabRoutes = remember {
            listOf(
                Screen.Home.route,
                Screen.Sms.route,
                Screen.DataSaver.route,
                Screen.Browser.route,
                Screen.Servers.route
            )
        }

        val navigateToTab: (String) -> Unit = { route ->
            if (currentScreen != route) {
                val currentTabIdx = primaryTabRoutes.indexOf(currentScreen)
                val targetTabIdx = primaryTabRoutes.indexOf(route)
                isBackTransition = if (currentTabIdx != -1 && targetTabIdx != -1) {
                    targetTabIdx < currentTabIdx
                } else {
                    false
                }
                backStack = if (route == Screen.Home.route) {
                    listOf(Screen.Home.route)
                } else {
                    listOf(Screen.Home.route, route)
                }
                currentScreen = route
            }
        }

        val navigateToScreen: (String) -> Unit = { route ->
            if (currentScreen != route) {
                isBackTransition = false
                backStack = backStack + route
                currentScreen = route
            }
        }

        val navigateBack: () -> Unit = {
            if (backStack.size > 1) {
                isBackTransition = true
                val updatedStack = backStack.dropLast(1)
                backStack = updatedStack
                currentScreen = updatedStack.last()
            } else if (currentScreen != Screen.Home.route) {
                isBackTransition = true
                currentScreen = Screen.Home.route
                backStack = listOf(Screen.Home.route)
            }
        }

        // Native Android back handling
        BackHandler(enabled = backStack.size > 1 || currentScreen != Screen.Home.route) {
            navigateBack()
        }

        Scaffold(
            modifier = modifier,
            bottomBar = {
                NavigationBar(
                    containerColor = DarkSurface,
                    tonalElevation = 8.dp,
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .testTag("main_bottom_navigation")
                ) {
                    items.forEach { screen ->
                        val isSelected = currentScreen == screen.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                navigateToTab(screen.route)
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                    contentDescription = screen.title,
                                    tint = if (isSelected) CyberCyan else TextSecondary
                                )
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    fontSize = 10.sp,
                                    color = if (isSelected) CyberCyan else TextSecondary
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = DarkSurfaceElevated
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            AnimatedContent(
                targetState = currentScreen,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                transitionSpec = {
                    val initialIdx = primaryTabRoutes.indexOf(initialState)
                    val targetIdx = primaryTabRoutes.indexOf(targetState)
                    if (initialIdx != -1 && targetIdx != -1) {
                        // Tab Transitions: directional horizontal slide + subtle fade
                        if (targetIdx > initialIdx) {
                            (slideInHorizontally { width -> width } + fadeIn(animationSpec = tween(220)))
                                .togetherWith(slideOutHorizontally { width -> -width } + fadeOut(animationSpec = tween(220)))
                        } else {
                            (slideInHorizontally { width -> -width } + fadeIn(animationSpec = tween(220)))
                                .togetherWith(slideOutHorizontally { width -> width } + fadeOut(animationSpec = tween(220)))
                        }
                    } else {
                        // Deep Screen Transitions: Forward / Backward directional sliding & fading
                        if (isBackTransition) {
                            // Moving backward: Slide in from left, fade out to right
                            (slideInHorizontally { width -> -width } + fadeIn(animationSpec = tween(220)))
                                .togetherWith(slideOutHorizontally { width -> width } + fadeOut(animationSpec = tween(220)))
                        } else {
                            // Moving forward: Slide in from right, fade out to left
                            (slideInHorizontally { width -> width } + fadeIn(animationSpec = tween(220)))
                                .togetherWith(slideOutHorizontally { width -> -width } + fadeOut(animationSpec = tween(220)))
                        }
                    }
                },
                label = "ZeroFlickerScreenTransition"
            ) { screenRoute ->
                when (screenRoute) {
                    Screen.Home.route -> {
                        HomeScreen(
                            viewModel = viewModel,
                            onNavigateToServers = { navigateToScreen(Screen.Servers.route) },
                            onNavigateToDataSaver = { navigateToScreen(Screen.DataSaver.route) },
                            onNavigateToDataManagement = { navigateToScreen(Screen.DataManagement.route) },
                            onNavigateToSpeedTest = { navigateToScreen(Screen.SpeedTest.route) },
                            onNavigateToRewards = { navigateToScreen(Screen.Rewards.route) },
                            onNavigateToAdmin = { navigateToScreen(Screen.Hetzner.route) },
                            onNavigateToSms = { navigateToScreen(Screen.Sms.route) },
                            onNavigateToBrowser = { navigateToScreen(Screen.Browser.route) },
                            onNavigateToElectricityDashboard = { navigateToScreen(Screen.ElectricityDashboard.route) }
                        )
                    }

                    Screen.Sms.route -> {
                        SmsChatScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navigateBack() },
                            onNavigateToBulkSms = { navigateToScreen(Screen.BulkSms.route) }
                        )
                    }

                    Screen.BulkSms.route -> {
                        ByodBulkSmsScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navigateBack() }
                        )
                    }

                    Screen.DataManagement.route -> {
                        DataManagementScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navigateBack() },
                            onNavigateToServices = { navigateToScreen(Screen.DataSaver.route) }
                        )
                    }

                    Screen.Firewall.route -> {
                        FirewallScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navigateBack() }
                        )
                    }

                    Screen.Browser.route -> {
                        BrowserScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navigateBack() }
                        )
                    }

                    Screen.DataSaver.route -> {
                        DataTrackerScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navigateBack() },
                            onNavigateToRewards = { navigateToScreen(Screen.Rewards.route) },
                            onNavigateToElectricityDashboard = { navigateToScreen(Screen.ElectricityDashboard.route) }
                        )
                    }

                    Screen.Rewards.route -> {
                        RewardsScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navigateBack() }
                        )
                    }

                    Screen.Servers.route -> {
                        ServerListScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navigateBack() },
                            onNavigateToHetzner = { navigateToScreen(Screen.Hetzner.route) }
                        )
                    }

                    Screen.Hetzner.route -> {
                        AdminDashboardScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navigateBack() }
                        )
                    }

                    Screen.SpeedTest.route -> {
                        SpeedTestScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navigateBack() }
                        )
                    }

                    Screen.Settings.route -> {
                        SettingsScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navigateBack() }
                        )
                    }

                    Screen.ElectricityDashboard.route -> {
                        ElectricityDashboardScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navigateBack() }
                        )
                    }

                    else -> {
                        HomeScreen(
                            viewModel = viewModel,
                            onNavigateToServers = { navigateToScreen(Screen.Servers.route) },
                            onNavigateToDataSaver = { navigateToScreen(Screen.DataSaver.route) },
                            onNavigateToDataManagement = { navigateToScreen(Screen.DataManagement.route) },
                            onNavigateToSpeedTest = { navigateToScreen(Screen.SpeedTest.route) },
                            onNavigateToRewards = { navigateToScreen(Screen.Rewards.route) },
                            onNavigateToAdmin = { navigateToScreen(Screen.Hetzner.route) },
                            onNavigateToSms = { navigateToScreen(Screen.Sms.route) },
                            onNavigateToBrowser = { navigateToScreen(Screen.Browser.route) },
                            onNavigateToElectricityDashboard = { navigateToScreen(Screen.ElectricityDashboard.route) }
                        )
                    }
                }
            }
        }
    }
}
