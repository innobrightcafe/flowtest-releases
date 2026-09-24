package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ui.viewmodel.VpnViewModel
import com.example.ui.components.AppUpdateSettingsCard
import com.example.ui.components.DataUsageFilterSheet
import com.example.ui.components.FlowtestEmblem
import com.example.ui.components.ShareAppApkSheet
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: VpnViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var aboutFeedbackMsg by remember { mutableStateOf<String?>(null) }
    val userVirtualAccount by viewModel.userVirtualAccount.collectAsStateWithLifecycle()
    val selectedProtocol by viewModel.selectedProtocol.collectAsStateWithLifecycle()
    val killSwitchEnabled by viewModel.killSwitchEnabled.collectAsStateWithLifecycle()
    val dnsProtection by viewModel.dnsProtection.collectAsStateWithLifecycle()
    val connectionLogs by viewModel.connectionLogs.collectAsStateWithLifecycle()
    val isDataRenewalReminderEnabled by viewModel.isDataRenewalReminderEnabled.collectAsStateWithLifecycle()
    val dataReminderThresholdMb by viewModel.dataReminderThresholdMb.collectAsStateWithLifecycle()
    val firewallApps by viewModel.firewallApps.collectAsStateWithLifecycle()
    val purchasedDataTotalMb by viewModel.purchasedDataTotalMb.collectAsStateWithLifecycle()
    val totalAppsConsumedMb = remember(firewallApps) {
        viewModel.getTotalAppsConsumedMb()
    }
    val estimatedRemainingMb by viewModel.estimatedDataBalanceMb.collectAsStateWithLifecycle()
    val activeUsageFilter by viewModel.activeUsageFilter.collectAsStateWithLifecycle()

    var showLogsModal by remember { mutableStateOf(false) }
    var showCrashLogsModal by remember { mutableStateOf(false) }
    var autoConnectWifi by remember { mutableStateOf(true) }
    var showChangePinDrawer by remember { mutableStateOf(false) }
    var showUpdatePhoneDrawer by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var showDataRenewalReminderDialog by remember { mutableStateOf(false) }
    var showUsageFilterSheet by remember { mutableStateOf(false) }

    val protocols = listOf("WireGuard", "OpenVPN (UDP)", "Shadowsocks")
    val dnsServers = listOf(
        "Cloudflare 1.1.1.1 (Encrypted)",
        "Quad9 9.9.9.9 (Malware Blocked)",
        "AdGuard DNS (Ad & Tracker Blocked)"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkObsidian)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("settings_screen_container")
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "SETTINGS & PROFILE",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = TextPrimary
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // USER ACCOUNT & SECURITY PROFILE CARD
        val isFingerprintEnabled by viewModel.isFingerprintEnabled.collectAsStateWithLifecycle()
        val isAutoLogoutEnabled by viewModel.isAutoLogoutEnabled.collectAsStateWithLifecycle()
        val autoLogoutTimeoutSeconds by viewModel.autoLogoutTimeoutSeconds.collectAsStateWithLifecycle()
        val secondsUntilAutoLogout by viewModel.secondsUntilAutoLogout.collectAsStateWithLifecycle()

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, CyberCyan, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(CyberCyan.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.AccountCircle, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = userVirtualAccount.fullName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                            )
                            Text(
                                text = userVirtualAccount.email,
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = ElectricEmerald.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "ACTIVE",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = ElectricEmerald, fontSize = 9.sp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                HorizontalDivider(color = DarkCardBorder)

                Spacer(modifier = Modifier.height(14.dp))

                // Primary Phone Number
                Text(
                    text = "PRIMARY PHONE & PROFILE",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, color = CyberCyan, letterSpacing = 1.2.sp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurface)
                        .clickable { showUpdatePhoneDrawer = true }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Primary Phone Number",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                            )
                            Text(
                                text = userVirtualAccount.phoneNumber.ifBlank { "Not set — Tap to add" },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (userVirtualAccount.phoneNumber.isNotBlank()) CyberCyan else TextMuted,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CyberCyan.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "UPDATE",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = CyberCyan, fontSize = 10.sp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                HorizontalDivider(color = DarkCardBorder)

                Spacer(modifier = Modifier.height(14.dp))

                // Security & Authentication Options
                Text(
                    text = "SECURITY CREDENTIALS",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, color = ElectricEmerald, letterSpacing = 1.2.sp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Change Security PIN (Login & Transactions)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurface)
                        .clickable { showChangePinDrawer = true }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.LockReset, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Security PIN (Login & Transactions)",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                            )
                            Text(
                                text = "Update passcode for account access & transaction authorization",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 10.sp)
                            )
                        }
                    }

                    Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Fingerprint Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurface)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Fingerprint, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Fingerprint Authentication",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                            )
                            Text(
                                text = "Allow instant biometric scanning login",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 10.sp)
                            )
                        }
                    }

                    Switch(
                        checked = isFingerprintEnabled,
                        onCheckedChange = { viewModel.toggleFingerprint(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = DarkObsidian, checkedTrackColor = ElectricEmerald)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Auto-Logout Inactivity Guard Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurface)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Timer, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Auto-Logout on Inactivity",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = Color(0xFFFFB300).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "SECURITY",
                                        color = Color(0xFFFFD54F),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = if (isAutoLogoutEnabled) {
                                    val mins = autoLogoutTimeoutSeconds / 60
                                    "Logs out after ${mins} min${if (mins > 1) "s" else ""} of no activity (Locks in ${secondsUntilAutoLogout}s)"
                                } else {
                                    "Disabled (Not recommended for wallet security)"
                                },
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 10.sp)
                            )
                        }
                    }

                    Switch(
                        checked = isAutoLogoutEnabled,
                        onCheckedChange = { viewModel.toggleAutoLogout(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = DarkObsidian, checkedTrackColor = Color(0xFFFFB300))
                    )
                }

                if (isAutoLogoutEnabled) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "INACTIVITY TIMEOUT DURATION",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // Timeout options: 1 min, 2 min, 5 min, 10 min
                    val timeoutOptions = listOf(
                        60L to "1 Min",
                        120L to "2 Mins (Default)",
                        300L to "5 Mins",
                        600L to "10 Mins"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        timeoutOptions.forEach { (sec, label) ->
                            val isSelected = autoLogoutTimeoutSeconds == sec
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) CyberCyan.copy(alpha = 0.2f) else DarkSurface,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) CyberCyan else DarkCardBorder
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.setAutoLogoutTimeoutSeconds(sec) }
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isSelected) CyberCyan else TextSecondary,
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal,
                                        fontSize = 9.sp
                                    ),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Lock Session Now Button
                Button(
                    onClick = {
                        viewModel.triggerAutoLogoutForInactivity("Session manually locked. Please re-authenticate to continue.")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2C1E08),
                        contentColor = Color(0xFFFFD54F)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB300).copy(alpha = 0.5f))
                ) {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = "Lock Session", tint = Color(0xFFFFD54F), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("LOCK SESSION NOW", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Logout Button
                OutlinedButton(
                    onClick = { viewModel.logout() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.5f))
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("LOGOUT ACCOUNT", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Section 1: VPN Protocol Selector
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, DarkCardBorder, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Outlined.Shield, contentDescription = "Protocol", tint = CyberCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "VPN PROTOCOL",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                protocols.forEach { proto ->
                    val isSelected = selectedProtocol == proto
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = proto,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) CyberCyan else TextPrimary
                            )
                        )

                        RadioButton(
                            selected = isSelected,
                            onClick = { viewModel.selectProtocol(proto) },
                            colors = RadioButtonDefaults.colors(selectedColor = CyberCyan)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Section 2: Security & Firewall Toggles
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, DarkCardBorder, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Outlined.Security, contentDescription = "Security", tint = ElectricEmerald)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SECURITY & FIREWALL",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Kill Switch Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Always-On Kill Switch", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
                        Text("Block all internet traffic if VPN disconnects unexpectedly", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp))
                    }
                    Switch(
                        checked = killSwitchEnabled,
                        onCheckedChange = { viewModel.toggleKillSwitch(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = DarkObsidian, checkedTrackColor = ElectricEmerald)
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = DarkCardBorder)

                // Auto-connect Wi-Fi Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto-Protect Untrusted Wi-Fi", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
                        Text("Automatically activate VPN when joining public Wi-Fi", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp))
                    }
                    Switch(
                        checked = autoConnectWifi,
                        onCheckedChange = { autoConnectWifi = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = DarkObsidian, checkedTrackColor = CyberCyan)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Section 3: DNS Leak Protection
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, DarkCardBorder, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "DNS LEAK PROTECTION",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                )

                Spacer(modifier = Modifier.height(12.dp))

                dnsServers.forEach { dns ->
                    val isSelected = dnsProtection == dns
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = dns,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) CyberCyan else TextPrimary
                            )
                        )

                        RadioButton(
                            selected = isSelected,
                            onClick = { viewModel.setDnsProtection(dns) },
                            colors = RadioButtonDefaults.colors(selectedColor = CyberCyan)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SECTION: DATA RENEWAL ALERT NOTIFICATION CONFIGURATION
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, DarkCardBorder, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isDataRenewalReminderEnabled) Icons.Default.Shield else Icons.Outlined.Shield,
                            contentDescription = "Renewal Alert",
                            tint = if (isDataRenewalReminderEnabled) ElectricEmerald else CyberCyan
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "DATA RENEWAL ALERT",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                        )
                    }

                    Switch(
                        checked = isDataRenewalReminderEnabled,
                        onCheckedChange = { isChecked ->
                            viewModel.updateDataRenewalReminderSettings(isChecked, dataReminderThresholdMb, System.currentTimeMillis() + 28L * 86400000L, context)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyberCyan,
                            checkedTrackColor = DarkSurface,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = DarkSurface
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Receive proactive low-data alerts on your phone when remaining data drops below your set limit so your connection never drops.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.5.sp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurface,
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, DarkCardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showDataRenewalReminderDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Current Alert Limit",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 10.sp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (dataReminderThresholdMb >= 1024.0) String.format(java.util.Locale.US, "%.1f GB Remaining", dataReminderThresholdMb / 1024.0) else "${dataReminderThresholdMb.toInt()} MB Remaining",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = if (isDataRenewalReminderEnabled) ElectricEmerald else TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp
                                )
                            )
                        }

                        Text(
                            text = "Configure ⚙️",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = CyberCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SECTION: GENERAL USAGE MEASUREMENT & FILTER (SETUP ONCE ACROSS ALL SERVICES)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, DarkCardBorder, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Usage Filter",
                            tint = CyberCyan
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "DATA USAGE MEASUREMENT FILTER",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CyberCyan.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(0.6.dp, CyberCyan.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "GLOBAL FILTER",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = CyberCyan,
                                fontWeight = FontWeight.Black,
                                fontSize = 9.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Configure your measurement period once here to automatically filter live usage by day, week, month, and network interface across all services and telemetry on the app.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.5.sp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Quick Period Selection Buttons (Daily, Weekly, Monthly)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val periods = listOf(
                        com.example.data.model.UsagePeriod.TODAY to "Daily (Today)",
                        com.example.data.model.UsagePeriod.LAST_7_DAYS to "Weekly (7D)",
                        com.example.data.model.UsagePeriod.LAST_30_DAYS to "Monthly (30D)"
                    )
                    periods.forEach { (period, label) ->
                        val isSelected = activeUsageFilter.period == period
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) CyberCyan.copy(alpha = 0.2f) else DarkSurface,
                            border = androidx.compose.foundation.BorderStroke(
                                if (isSelected) 1.dp else 0.5.dp,
                                if (isSelected) CyberCyan else DarkCardBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    viewModel.setUsageFilter(
                                        activeUsageFilter.copy(period = period),
                                        saveAsDefault = true
                                    )
                                    Toast.makeText(context, "Global filter set to: $label", Toast.LENGTH_SHORT).show()
                                }
                        ) {
                            Text(
                                text = label,
                                modifier = Modifier.padding(vertical = 10.dp),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal,
                                    color = if (isSelected) CyberCyan else TextSecondary,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Current Active Filter Bar with Drawer Trigger
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurface,
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, DarkCardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showUsageFilterSheet = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Active Window & Network",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 10.sp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${activeUsageFilter.getFormattedWindow()} • ${activeUsageFilter.networkType.label}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = ElectricEmerald,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp
                                )
                            )
                        }

                        Text(
                            text = "Filter Drawer ⚙️",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = CyberCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SECTION: ABOUT US & CORPORATE PROFILE
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, CyberCyan.copy(alpha = 0.4f), RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FlowtestEmblem(
                            size = 36.dp,
                            primaryColor = CyberCyan,
                            glowAlpha = 0.25f,
                            animateRotationOnLoad = false
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ABOUT US & COMPANY",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp,
                                    color = CyberCyan,
                                    fontSize = 11.sp
                                )
                            )
                            Text(
                                text = "FlowTest by INOSOFTTECH LIMITED",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = ElectricEmerald.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, ElectricEmerald.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "RC: 9710966",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = ElectricEmerald,
                                fontWeight = FontWeight.Black,
                                fontSize = 9.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // About the App & Company Overview
                Text(
                    text = "FlowTest is the premier digital infrastructure and utility platform operated by INOSOFTTECH LIMITED. FlowTest delivers high-speed military-grade VPN encryption, automated utility bill settlements, instant airtime & data vending, and real-time automated banking integration for seamless client operations.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 17.sp
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = DarkCardBorder, thickness = 0.8.dp)
                Spacer(modifier = Modifier.height(14.dp))

                // NIGERIA OFFICE SECTION
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "🇳🇬 NIGERIA OFFICE (West Africa HQ)",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            fontSize = 11.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Lekki Office Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkObsidian),
                    border = androidx.compose.foundation.BorderStroke(0.8.dp, DarkCardBorder)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Lekki Corporate Office",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = CyberCyan,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            )
                            IconButton(
                                onClick = {
                                    val addr = "7 Woruola Adebola Street, Lekki, Lagos, Nigeria"
                                    clipboardManager.setText(AnnotatedString(addr))
                                    Toast.makeText(context, "Nigeria Address Copied!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = TextMuted, modifier = Modifier.size(13.dp))
                            }
                        }
                        Text(
                            text = "7 Woruola Adebola Street, Lekki, Lagos, Nigeria",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, fontSize = 11.sp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Nigeria Direct Phone & WhatsApp
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkObsidian)
                        .clickable {
                            try {
                                val url = "https://wa.me/2348137545370"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                clipboardManager.setText(AnnotatedString("+2348137545370"))
                                Toast.makeText(context, "Nigeria Phone Copied: +234 8137545370", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "💬", fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "+234 8137545370 / 08137545370",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricEmerald,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                )
                            )
                            Text(text = "Nigeria Support (Call & WhatsApp)", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 9.sp))
                        }
                    }
                    Text(text = "Contact >", style = MaterialTheme.typography.labelSmall.copy(color = CyberCyan, fontWeight = FontWeight.Bold, fontSize = 10.sp))
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = DarkCardBorder, thickness = 0.8.dp)
                Spacer(modifier = Modifier.height(14.dp))

                // CORPORATE REGISTRATION & SETTLEMENT BADGE
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkObsidian,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "LEGAL ENTITY & BANKING",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    color = CyberCyan,
                                    fontSize = 9.sp
                                )
                            )
                            Text(
                                text = "OPERATED BY INOSOFTTECH LIMITED",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextMuted,
                                    fontSize = 8.sp
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Parent Company: INOSOFTTECH LIMITED\nBrand: FlowTest\nCAC RC Registration: 9710966\nSettlement: FlowTest Automated Bank Settlement\nAccount Number: 6666468328 (INOSOFTTECH LIMITED)",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 15.sp
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // IN-APP SYSTEM UPDATES & AUTO-UPGRADER
        AppUpdateSettingsCard()

        Spacer(modifier = Modifier.height(16.dp))

        // OFFLINE APP SHARING & PHONE-TO-PHONE DISTRIBUTION
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, CyberCyan.copy(alpha = 0.4f), RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(CyberCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share APK",
                                tint = CyberCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Share App",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = "Send app to nearby devices",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Share FlowTest directly with friends and family without internet.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextMuted,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        val refCode = "FLOW-" + userVirtualAccount.accountNumber.takeLast(5)
                        com.example.util.ApkSharingHelper.launchNativeShare(
                            context = context,
                            coroutineScope = coroutineScope,
                            referralCode = refCode
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberCyan,
                        contentColor = DarkObsidian
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.NearMe,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "TRANSFER APK TO NEARBY DEVICE",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Connection Logs Button
        OutlinedButton(
            onClick = { showLogsModal = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan)
        ) {
            Icon(imageVector = Icons.Outlined.History, contentDescription = "Logs")
            Spacer(modifier = Modifier.width(8.dp))
            Text("View Connection Audit Logs (${connectionLogs.size})", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Diagnostic Crash Logs & Bug Reporting Button
        OutlinedButton(
            onClick = { showCrashLogsModal = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = WarningRed),
            border = androidx.compose.foundation.BorderStroke(1.dp, WarningRed.copy(alpha = 0.5f))
        ) {
            Icon(imageVector = Icons.Default.BugReport, contentDescription = "Crash Logs", tint = WarningRed)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Crash Logs & Bug Reporting", fontWeight = FontWeight.Bold, color = TextPrimary)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Modal Dialog for Connection Audit Logs
    if (showLogsModal) {
        AlertDialog(
            onDismissRequest = { showLogsModal = false },
            title = { Text("VPN Connection Audit Logs", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                if (connectionLogs.isEmpty()) {
                    Text("No connection logs recorded yet.", color = TextSecondary)
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(connectionLogs) { log ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = DarkObsidian),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "${log.serverName} (${log.protocol})",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = ElectricEmerald)
                                    )
                                    Text(
                                        text = "Duration: ${log.durationSeconds}s • Downloaded: ${log.bytesDownloaded / 1024} KB",
                                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showLogsModal = false }, colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian)) {
                    Text("Close")
                }
            },
            containerColor = DarkSurfaceElevated
        )
    }

    // Modal Dialog for Crash Logs & Bug Reporting
    if (showCrashLogsModal) {
        val latestLog = remember { com.example.util.CrashLogger.getLatestCrashLog(context) }
        val crashFile = remember { com.example.util.CrashLogger.getLatestCrashLogFile(context) }

        AlertDialog(
            onDismissRequest = { showCrashLogsModal = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.BugReport, contentDescription = null, tint = WarningRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Crash Diagnostics & Bug Report", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (latestLog.isNullOrBlank()) {
                        Surface(
                            color = DarkObsidian,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = ElectricEmerald, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No Unhandled Crashes Recorded", fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("FlowTest engine and database are running stably on your device.", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, textAlign = TextAlign.Center))
                            }
                        }
                    } else {
                        Text(
                            text = "Diagnostic text log saved on device. You can email this directly to the developer to resolve any issues:",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.5.sp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = DarkObsidian,
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                        ) {
                            val logScroll = rememberScrollState()
                            Text(
                                text = latestLog,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = Color(0xFFE2E8F0)
                                ),
                                modifier = Modifier
                                    .padding(8.dp)
                                    .verticalScroll(logScroll)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!latestLog.isNullOrBlank()) {
                        Button(
                            onClick = {
                                com.example.util.CrashLogger.shareCrashLogViaEmail(context, latestLog, crashFile)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian)
                        ) {
                            Icon(imageVector = Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Email Report", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                com.example.util.CrashLogger.copyToClipboard(context, latestLog)
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan)
                        ) {
                            Text("Copy", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Button(
                        onClick = { showCrashLogsModal = false },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkCardBorder, contentColor = TextPrimary)
                    ) {
                        Text("Close")
                    }
                }
            },
            containerColor = DarkSurfaceElevated
        )
    }

    if (showChangePinDrawer) {
        var oldPinInput by remember { mutableStateOf("") }
        var newPinInput by remember { mutableStateOf("") }
        var pinErrorMsg by remember { mutableStateOf<String?>(null) }
        var pinSuccessMsg by remember { mutableStateOf<String?>(null) }

        ModalBottomSheet(
            onDismissRequest = { showChangePinDrawer = false },
            containerColor = DarkSurfaceElevated,
            scrimColor = Color.Black.copy(alpha = 0.6f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.LockReset, contentDescription = null, tint = CyberCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "CHANGE SECURITY PIN",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                        )
                    }
                    IconButton(onClick = { showChangePinDrawer = false }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Text(
                    text = "This PIN is used for both app login and authorizing transactions.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp),
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                pinErrorMsg?.let { err ->
                    Text(text = err, color = Color(0xFFFF5252), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 8.dp))
                }
                pinSuccessMsg?.let { msg ->
                    Text(text = msg, color = CyberCyan, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 8.dp))
                }

                OutlinedTextField(
                    value = oldPinInput,
                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) oldPinInput = it },
                    label = { Text("Current PIN (4-6 digits)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan, unfocusedBorderColor = DarkCardBorder, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = newPinInput,
                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) newPinInput = it },
                    label = { Text("New PIN (4-6 digits)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan, unfocusedBorderColor = DarkCardBorder, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        viewModel.changeUserPin(
                            oldPin = oldPinInput,
                            newPin = newPinInput,
                            onSuccess = {
                                pinSuccessMsg = "PIN Changed Successfully! Applied to Login & Transactions."
                                pinErrorMsg = null
                            },
                            onError = { err ->
                                pinErrorMsg = err
                                pinSuccessMsg = null
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("UPDATE SECURITY PIN", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black, color = DarkObsidian))
                }
            }
        }
    }

    if (showUpdatePhoneDrawer) {
        var newPhoneInput by remember { mutableStateOf(userVirtualAccount.phoneNumber) }
        var phoneErrorMsg by remember { mutableStateOf<String?>(null) }
        var phoneSuccessMsg by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(userVirtualAccount.phoneNumber) {
            newPhoneInput = userVirtualAccount.phoneNumber
        }

        ModalBottomSheet(
            onDismissRequest = { showUpdatePhoneDrawer = false },
            containerColor = DarkSurfaceElevated,
            scrimColor = Color.Black.copy(alpha = 0.6f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = CyberCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "UPDATE PRIMARY PHONE",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                        )
                    }
                    IconButton(onClick = { showUpdatePhoneDrawer = false }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Text(
                    text = "Your primary phone number is used for SMS receipts, payment confirmations, and account security.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp),
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                phoneErrorMsg?.let { err ->
                    Text(text = err, color = Color(0xFFFF5252), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 8.dp))
                }
                phoneSuccessMsg?.let { msg ->
                    Text(text = msg, color = CyberCyan, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 8.dp))
                }

                OutlinedTextField(
                    value = newPhoneInput,
                    onValueChange = {
                        newPhoneInput = it.filter { c -> c.isDigit() || c == '+' }
                        phoneErrorMsg = null
                    },
                    label = { Text("Primary Phone Number") },
                    placeholder = { Text("e.g. 08012345678") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = DarkCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        val clean = newPhoneInput.trim()
                        if (clean.length < 10) {
                            phoneErrorMsg = "Please enter a valid phone number (at least 10 digits)."
                            return@Button
                        }
                        viewModel.updateUserPhoneNumber(clean) {
                            phoneSuccessMsg = "Primary phone number successfully updated!"
                            phoneErrorMsg = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("SAVE PHONE NUMBER", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black, color = DarkObsidian))
                }
            }
        }
    }

    if (showDataRenewalReminderDialog) {
        DataRenewalReminderDialog(
            enabled = isDataRenewalReminderEnabled,
            currentThresholdMb = dataReminderThresholdMb,
            estimatedRemainingMb = estimatedRemainingMb,
            onSave = { enabled, thresholdMb, renewalTimestamp ->
                viewModel.updateDataRenewalReminderSettings(enabled, thresholdMb, renewalTimestamp, context)
                showDataRenewalReminderDialog = false
            },
            onDismiss = {
                showDataRenewalReminderDialog = false
            }
        )
    }

    if (showUsageFilterSheet) {
        DataUsageFilterSheet(
            currentFilter = activeUsageFilter,
            onDismiss = { showUsageFilterSheet = false },
            onApplyFilter = { newFilter, saveDefault ->
                viewModel.setUsageFilter(newFilter, saveAsDefault = saveDefault)
            }
        )
    }
}
