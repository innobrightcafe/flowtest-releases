package com.example.ui.components.admin

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.animation.core.*
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.api.PairgateWebhookSecurity
import com.example.data.ui.viewmodel.VpnViewModel
import com.example.ui.components.FlowButtonLoadingLine
import com.example.ui.theme.*
import com.example.util.AppNotificationManager

@Composable
fun AdminSecurityTab(
    viewModel: VpnViewModel,
    onNavigateBack: () -> Unit,
    onShowStatusMsg: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val adminProfile by viewModel.adminProfile.collectAsStateWithLifecycle()
    val adminMasterPasscode by viewModel.adminMasterPasscode.collectAsStateWithLifecycle()
    val moniepointWebhookUrl by viewModel.moniepointWebhookUrl.collectAsStateWithLifecycle()
    val moniepointWebhookSecret by viewModel.moniepointWebhookSecret.collectAsStateWithLifecycle()
    val webhookLogs by viewModel.pairgateWebhookLogs.collectAsStateWithLifecycle()
    val gmailAddress by viewModel.gmailAddress.collectAsStateWithLifecycle()
    val gmailAppPassword by viewModel.gmailAppPassword.collectAsStateWithLifecycle()
    val isGmailConfigured by viewModel.isGmailConfigured.collectAsStateWithLifecycle()
    val isSyncingGmail by viewModel.isSyncingGmailAlerts.collectAsStateWithLifecycle()
    val lastGmailSyncResult by viewModel.lastGmailSyncResult.collectAsStateWithLifecycle()
    val actualNotifications by viewModel.actualInboundNotifications.collectAsStateWithLifecycle()
    val isFetchingActualNotifications by viewModel.isFetchingActualNotifications.collectAsStateWithLifecycle()

    val vpnResellersApiKey by viewModel.vpnResellersApiKey.collectAsStateWithLifecycle()
    val isSyncingVpnResellers by viewModel.isSyncingVpnResellers.collectAsStateWithLifecycle()
    val vpnResellersSyncMessage by viewModel.vpnResellersSyncMessage.collectAsStateWithLifecycle()
    val allServers by viewModel.allServers.collectAsStateWithLifecycle()

    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val infiniteTransition = rememberInfiniteTransition(label = "admin_vpn_spin")
    val vpnSpinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "vpn_spin_angle"
    )

    var vpnSecretInput by remember(vpnResellersApiKey) { mutableStateOf(vpnResellersApiKey) }
    var isVpnSecretMasked by remember { mutableStateOf(true) }
    var vpnSecretSavedBanner by remember { mutableStateOf<String?>(null) }

    var hasNotificationPermission by remember {
        mutableStateOf(AppNotificationManager.canPostNotification(context))
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        if (isGranted) {
            AppNotificationManager.initNotificationChannels(context)
            onShowStatusMsg("✓ System notification permission granted!")
        } else {
            onShowStatusMsg("Notification permission was not granted.")
        }
    }

    var newAdminPasscodeInput by remember(adminMasterPasscode) { mutableStateOf(adminMasterPasscode) }
    var adminPasscodeChangeMsg by remember { mutableStateOf<String?>(null) }

    var isSecretMasked by remember { mutableStateOf(true) }
    var showSecretEditDialog by remember { mutableStateOf(false) }
    var customSecretInput by remember(moniepointWebhookSecret) { mutableStateOf(moniepointWebhookSecret) }
    var secretSavedBanner by remember { mutableStateOf<String?>(null) }

    var showUrlEditDialog by remember { mutableStateOf(false) }
    var customUrlInput by remember(moniepointWebhookUrl) { mutableStateOf(moniepointWebhookUrl) }

    var gmailEmailInput by remember(gmailAddress) { mutableStateOf(if (gmailAddress.isBlank()) "Innobrightcafe@gmail.com" else gmailAddress) }
    var gmailPasswordInput by remember(gmailAppPassword) { mutableStateOf(gmailAppPassword) }
    var isGmailPasswordMasked by remember { mutableStateOf(true) }
    var gmailStatusBanner by remember { mutableStateOf<String?>(null) }

    var testWebhookAmountInput by remember { mutableStateOf("") }
    var testWebhookSenderInput by remember { mutableStateOf("") }
    var testWebhookPhoneInput by remember { mutableStateOf("") }
    var testWebhookNarrationInput by remember { mutableStateOf("") }
    var testWebhookRefInput by remember { mutableStateOf("") }
    var isProcessingWebhook by remember { mutableStateOf(false) }
    var isLiveCreditWebhookInProgress by remember { mutableStateOf(false) }
    var testWebhookResultMsg by remember { mutableStateOf<String?>(null) }
    var notificationTriggeredMsg by remember { mutableStateOf<String?>(null) }
    var webhookSearchFilter by remember { mutableStateOf("") }
    var isQueryingWebhooks by remember { mutableStateOf(false) }
    var gmailSearchFilterInput by remember { mutableStateOf("") }
    var manualCreditRefInput by remember { mutableStateOf("") }
    var manualCreditAmountInput by remember { mutableStateOf("") }
    var manualCreditTargetInput by remember { mutableStateOf("") }
    var isManuallyCrediting by remember { mutableStateOf(false) }
    var manualCreditStatusMsg by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Admin Access & Master Passcode Controller Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFF0C1929))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Admin Active",
                            tint = CyberCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = "ADMINISTRATOR PRIVILEGES",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    color = CyberCyan,
                                    letterSpacing = 0.5.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Master Account Active (${adminProfile?.email ?: "innobright2010@gmail.com"})",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedButton(
                        onClick = {
                            viewModel.setAdminMode(false)
                            onNavigateBack()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = WarningRed),
                        border = androidx.compose.foundation.BorderStroke(1.dp, WarningRed.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Client Mode", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "How to Setup & Access Admin: Administrators are automatically recognized when logged in with the registered email (${adminProfile?.email ?: "innobright2010@gmail.com"}), or by entering the Master Passcode on any device.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newAdminPasscodeInput,
                        onValueChange = { newAdminPasscodeInput = it },
                        label = { Text("Master Admin Passcode") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    Button(
                        onClick = {
                            if (newAdminPasscodeInput.isNotBlank()) {
                                viewModel.updateAdminMasterPasscode(newAdminPasscodeInput)
                                adminPasscodeChangeMsg = "Master Passcode Saved!"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("SAVE", fontWeight = FontWeight.Bold)
                    }
                }

                adminPasscodeChangeMsg?.let { msg ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.labelSmall.copy(color = ElectricEmerald, fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        // 🛡️ VPNRESELLERS API v4.1 & WHOLESALE NODE ENGINE (Admin Controlled)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(
                    1.dp,
                    Brush.horizontalGradient(
                        listOf(
                            ElectricEmerald.copy(alpha = 0.6f),
                            CyberCyan.copy(alpha = 0.5f),
                            CyberCyanVariant.copy(alpha = 0.4f)
                        )
                    ),
                    RoundedCornerShape(20.dp)
                ),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(ElectricEmerald.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Dns,
                                contentDescription = "VPN Infrastructure",
                                tint = ElectricEmerald,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = "FLOWTEST STANDARD BACKBONE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    color = ElectricEmerald,
                                    letterSpacing = 0.5.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Global WireGuard Node Fleet & Gateway",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Live Status Pill
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (vpnResellersApiKey.isNotBlank()) ElectricEmerald.copy(alpha = 0.15f) else GlowingAmber.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (vpnResellersApiKey.isNotBlank()) ElectricEmerald.copy(alpha = 0.4f) else GlowingAmber.copy(alpha = 0.4f)
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (vpnResellersApiKey.isNotBlank()) ElectricEmerald else GlowingAmber)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (vpnResellersApiKey.isNotBlank()) "API ACTIVE" else "TOKEN OPTIONAL",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (vpnResellersApiKey.isNotBlank()) ElectricEmerald else GlowingAmber
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Stats / Status Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val wholesaleCount = remember(allServers) { allServers.count { !it.isHetznerServer } }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = DarkSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("ACTIVE NODES", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.Bold))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("$wholesaleCount Servers", style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary, fontWeight = FontWeight.Black))
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = DarkSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("BACKBONE ENDPOINT", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.Bold))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("flowtest.network", style = MaterialTheme.typography.bodyMedium.copy(color = CyberCyan, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 11.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "FLOWTEST BACKBONE TOKEN",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Authenticates requests to provision dedicated WireGuard configs and sync global nodes. Kept strictly private to admin.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 11.sp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = vpnSecretInput,
                    onValueChange = {
                        vpnSecretInput = it
                        vpnSecretSavedBanner = null
                    },
                    label = { Text("Bearer Secret Token") },
                    placeholder = { Text("Enter or paste token...") },
                    singleLine = true,
                    visualTransformation = if (isVpnSecretMasked) androidx.compose.ui.text.input.PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { isVpnSecretMasked = !isVpnSecretMasked }) {
                                Icon(
                                    imageVector = if (isVpnSecretMasked) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    contentDescription = if (isVpnSecretMasked) "Show" else "Hide",
                                    tint = TextSecondary
                                )
                            }
                            if (vpnSecretInput.isNotBlank()) {
                                IconButton(onClick = {
                                    clipboardManager.setText(AnnotatedString(vpnSecretInput))
                                    onShowStatusMsg("✓ Copied token to clipboard")
                                }) {
                                    Icon(
                                        imageVector = Icons.Outlined.ContentCopy,
                                        contentDescription = "Copy",
                                        tint = CyberCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricEmerald,
                        unfocusedBorderColor = DarkCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Save Token Button
                    Button(
                        onClick = {
                            viewModel.setVpnResellersApiKey(vpnSecretInput)
                            vpnSecretSavedBanner = "✓ Backbone API Token saved & nodes synced!"
                            onShowStatusMsg("✓ Backbone API secret updated")
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberCyan,
                            contentColor = DarkObsidian
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = "Save", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SAVE SECRET", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    // Sync Nodes Button
                    OutlinedButton(
                        onClick = {
                            viewModel.syncVpnResellersServers()
                            onShowStatusMsg("Syncing node fleet...")
                        },
                        enabled = !isSyncingVpnResellers,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = CyberCyan
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Sync Nodes",
                            modifier = Modifier
                                .size(16.dp)
                                .rotate(if (isSyncingVpnResellers) vpnSpinAngle else 0f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isSyncingVpnResellers) "SYNCING..." else "SYNC NODES",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                vpnSecretSavedBanner?.let { banner ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = banner,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = ElectricEmerald,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                vpnResellersSyncMessage?.let { syncMsg ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DarkSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = syncMsg,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. MONIEPOINT REAL-TIME WEBHOOK & REQUERY HUB
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(ElectricEmerald.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Security, contentDescription = "Security", tint = ElectricEmerald, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = "MONIEPOINT WEBHOOK MONITORING",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Instant 200 OK • HMAC-SHA256 • Requery API",
                                style = MaterialTheme.typography.labelSmall.copy(color = ElectricEmerald, fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        color = ElectricEmerald.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ElectricEmerald.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(ElectricEmerald))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("LIVE ENDPOINT", fontSize = 9.sp, fontWeight = FontWeight.Black, color = ElectricEmerald, maxLines = 1)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Public Endpoint Display
                Surface(
                    color = DarkObsidian,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "PUBLIC HTTPS WEBHOOK ENDPOINT (POST)",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = moniepointWebhookUrl,
                                style = MaterialTheme.typography.bodySmall.copy(color = CyberCyan, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(moniepointWebhookUrl))
                                    onShowStatusMsg("Moniepoint Webhook URL Copied!")
                                }
                            ) {
                                Icon(imageVector = Icons.Outlined.ContentCopy, contentDescription = "Copy URL", tint = CyberCyan, modifier = Modifier.size(18.dp))
                            }
                            IconButton(
                                onClick = {
                                    customUrlInput = moniepointWebhookUrl
                                    showUrlEditDialog = true
                                }
                            ) {
                                Icon(imageVector = Icons.Outlined.Edit, contentDescription = "Edit URL", tint = TextMuted, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Webhook Secret Security Card & Generator
                Surface(
                    color = Color(0xFF0D1B2A),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Outlined.Key, contentDescription = null, tint = GlowingAmber, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "MONIEPOINT WEBHOOK SECRET",
                                    style = MaterialTheme.typography.labelSmall.copy(color = GlowingAmber, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
                                )
                            }

                            Surface(
                                color = GlowingAmber.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "HMAC-SHA256",
                                    style = MaterialTheme.typography.labelSmall.copy(color = GlowingAmber, fontWeight = FontWeight.Bold, fontSize = 9.sp),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "This secret is used to cryptographically verify the 'moniepoint-signature' HTTP header on incoming transaction alerts, preventing payment spoofing.",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Secret String Box
                        Surface(
                            color = DarkObsidian,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isSecretMasked) {
                                            "••••••••••••••••••••••••••••••••••••••••"
                                        } else {
                                            moniepointWebhookSecret
                                        },
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = if (isSecretMasked) TextMuted else ElectricEmerald,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { isSecretMasked = !isSecretMasked },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isSecretMasked) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                            contentDescription = "Toggle Visibility",
                                            tint = TextMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(moniepointWebhookSecret))
                                            onShowStatusMsg("Webhook Secret Copied!")
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(imageVector = Icons.Outlined.ContentCopy, contentDescription = "Copy Secret", tint = GlowingAmber, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Generate New Secret Button
                            Button(
                                onClick = {
                                    val genSecret = viewModel.generateNewMoniepointWebhookSecret()
                                    isSecretMasked = false
                                    secretSavedBanner = "✓ New cryptographically secure secret generated!"
                                    onShowStatusMsg("Generated & Saved New Webhook Secret!")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = GlowingAmber, contentColor = DarkObsidian),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                Icon(imageVector = Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("GENERATE SECRET", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 10.sp))
                            }

                            // Edit Custom Secret Button
                            OutlinedButton(
                                onClick = {
                                    customSecretInput = moniepointWebhookSecret
                                    showSecretEditDialog = true
                                },
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.6f)),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                Icon(imageVector = Icons.Outlined.Edit, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("ENTER CUSTOM SECRET", color = CyberCyan, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp))
                            }
                        }

                        secretSavedBanner?.let { banner ->
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = banner, style = MaterialTheme.typography.labelSmall.copy(color = ElectricEmerald, fontWeight = FontWeight.Bold))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 4-Step Moniepoint Integration Architecture Checklist
                Text(
                    text = "MONIEPOINT SETUP & COMPLIANCE STEPS",
                    style = MaterialTheme.typography.labelSmall.copy(color = GlowingAmber, fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    color = DarkObsidian,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = ElectricEmerald, modifier = Modifier.size(15.dp).padding(top = 2.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Step 1: Public HTTPS Endpoint (/moniepoint-webhook)", style = MaterialTheme.typography.labelSmall.copy(color = TextPrimary, fontWeight = FontWeight.Bold))
                                Text("Instantly responds with HTTP 200 OK and offloads queries to background task queue.", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp))
                            }
                        }

                        Row(verticalAlignment = Alignment.Top) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = ElectricEmerald, modifier = Modifier.size(15.dp).padding(top = 2.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Step 2: Moniepoint Dashboard Registration", style = MaterialTheme.typography.labelSmall.copy(color = TextPrimary, fontWeight = FontWeight.Bold))
                                Text("Paste your URL and Secret in the Moniepoint developer portal to subscribe for live PAYMENT_SUCCESSFUL events.", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp))
                            }
                        }

                        Row(verticalAlignment = Alignment.Top) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = ElectricEmerald, modifier = Modifier.size(15.dp).padding(top = 2.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Step 3: Secure Signature Verification (HMAC-SHA256)", style = MaterialTheme.typography.labelSmall.copy(color = TextPrimary, fontWeight = FontWeight.Bold))
                                Text("Validates payload hash against header with MONIEPOINT_WEBHOOK_SECRET.", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp))
                            }
                        }

                        Row(verticalAlignment = Alignment.Top) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = ElectricEmerald, modifier = Modifier.size(15.dp).padding(top = 2.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Step 4: Moniepoint Requery API Fallback", style = MaterialTheme.typography.labelSmall.copy(color = TextPrimary, fontWeight = FontWeight.Bold))
                                Text("Queries GET /v1/transactions/merchants/{ref} on downtime gaps.", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = DarkCardBorder)
                Spacer(modifier = Modifier.height(14.dp))

                // Live Webhook Verification Console & Query Explorer
                Text(
                    text = "LIVE MONIEPOINT WEBHOOK & HMAC-SHA256 VERIFIER",
                    style = MaterialTheme.typography.labelSmall.copy(color = CyberCyan, fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Query Cloud Run webhook events, test HMAC-SHA256 signatures, dispatch real credit webhooks, and force-credit unresolved transfers.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Input fields: Amount, Sender Name, Narration/Code, Reference
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = testWebhookAmountInput,
                        onValueChange = { testWebhookAmountInput = it },
                        label = { Text("Amount (₦)") },
                        placeholder = { Text("1000") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = testWebhookSenderInput,
                        onValueChange = { testWebhookSenderInput = it },
                        label = { Text("Sender Name") },
                        placeholder = { Text("Sender Full Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.weight(1.4f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = testWebhookNarrationInput,
                        onValueChange = { testWebhookNarrationInput = it },
                        label = { Text("Narration / PIN (e.g. FT-1001 or 080XXXXXXXX)") },
                        placeholder = { Text("FT-1001 or 080XXXXXXXX") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.weight(1.5f)
                    )

                    OutlinedTextField(
                        value = testWebhookRefInput,
                        onValueChange = { testWebhookRefInput = it },
                        label = { Text("Bank Reference (Optional)") },
                        placeholder = { Text("MNP-LIVE-90218") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.weight(1.2f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Webhook Execution Buttons: Simulation Dry Run vs. Fetch Actual Live Notifications
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Button 1: Test Webhook Parser (Dry-Run - NEVER modifies user balance)
                    Button(
                        onClick = {
                            val amt = testWebhookAmountInput.toDoubleOrNull() ?: 1000.0
                            val narr = testWebhookNarrationInput.ifBlank { "FT-1001" }
                            val sender = testWebhookSenderInput.ifBlank { "Depositor Name" }
                            val ref = testWebhookRefInput.ifBlank { "MNP-TEST-" + (100000..999999).random() }
                            isProcessingWebhook = true
                            viewModel.dispatchLiveMoniepointWebhook(
                                amount = amt,
                                senderName = sender,
                                narration = narr,
                                reference = ref,
                                isSimulationOnly = true
                            ) { res ->
                                isProcessingWebhook = false
                                testWebhookResultMsg = "✓ TEST OK (Dry-Run): Webhook payload & narration '$narr' parsed successfully. Status: ${res.status}. (User balance was NOT modified)."
                                onShowStatusMsg("Webhook dry-run passed (no balance change)")
                            }
                        },
                        enabled = !isProcessingWebhook && !isFetchingActualNotifications,
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        if (isProcessingWebhook) {
                            FlowButtonLoadingLine(color = DarkObsidian, width = 28.dp, height = 2.dp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("TESTING...", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black))
                        } else {
                            Icon(imageVector = Icons.Default.Science, contentDescription = "Test Parser", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("TEST WEBHOOK PARSER", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 10.sp))
                        }
                    }

                    // Button 2: Fetch Actual Notifications from Cloud Run Hub & Gmail
                    Button(
                        onClick = {
                            viewModel.fetchActualLiveNotifications { list ->
                                testWebhookResultMsg = "✓ Fetched ${list.size} actual inbound notifications from Cloud Run Hub & Gmail for today."
                                onShowStatusMsg("Synced ${list.size} live notifications")
                            }
                        },
                        enabled = !isFetchingActualNotifications && !isProcessingWebhook,
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        if (isFetchingActualNotifications) {
                            FlowButtonLoadingLine(color = DarkObsidian, width = 28.dp, height = 2.dp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("FETCHING...", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black))
                        } else {
                            Icon(imageVector = Icons.Default.Sync, contentDescription = "Fetch Real", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("FETCH ACTUAL NOTIFICATIONS", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 9.5.sp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Security Tamper Audit
                    OutlinedButton(
                        onClick = {
                            val amt = testWebhookAmountInput.toDoubleOrNull() ?: 5000.0
                            isProcessingWebhook = true
                            viewModel.simulateIncomingWebhookDeposit(
                                amount = amt,
                                senderName = testWebhookSenderInput.ifBlank { "Unknown Impersonator" },
                                eventType = "PAYMENT_SUCCESSFUL",
                                tamperSignature = true,
                                onComplete = { log ->
                                    isProcessingWebhook = false
                                    testWebhookResultMsg = "Security Blocked: Invalid HMAC-SHA256 Signature (${log.status})"
                                }
                            )
                        },
                        enabled = !isProcessingWebhook && !isLiveCreditWebhookInProgress,
                        border = androidx.compose.foundation.BorderStroke(1.dp, WarningRed),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.GppBad, contentDescription = null, tint = WarningRed, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("TAMPER AUDIT", color = WarningRed, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp))
                    }

                    // Moniepoint Requery API Fallback
                    OutlinedButton(
                        onClick = {
                            val ref = testWebhookRefInput.ifBlank { "MNP-TX-" + (100000..999999).random() }
                            viewModel.requeryMoniepointTransaction(ref) { ok, msg, _ ->
                                testWebhookResultMsg = msg
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f)),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Icon(imageVector = Icons.Default.Sync, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("REQUERY API (GET)", color = CyberCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }

                testWebhookResultMsg?.let { msg ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (msg.contains("✓") || msg.contains("Verified")) ElectricEmerald else if (msg.contains("Blocked") || msg.contains("Invalid")) WarningRed else GlowingAmber,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = DarkCardBorder)
                Spacer(modifier = Modifier.height(12.dp))

                // Query & Filter Webhooks in Real Time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "WEBHOOK EVENT QUERY EXPLORER",
                        style = MaterialTheme.typography.labelSmall.copy(color = CyberCyan, fontWeight = FontWeight.Bold)
                    )

                    Button(
                        onClick = {
                            isQueryingWebhooks = true
                            viewModel.adminQueryWebhooksAndLedger(webhookSearchFilter) {
                                isQueryingWebhooks = false
                                onShowStatusMsg("Refreshed live webhook events from Cloud Run & Local DB")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan.copy(alpha = 0.2f), contentColor = CyberCyan),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        if (isQueryingWebhooks) {
                            FlowButtonLoadingLine(color = CyberCyan, width = 24.dp, height = 2.dp)
                        } else {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("REFRESH HUB", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = webhookSearchFilter,
                    onValueChange = {
                        webhookSearchFilter = it
                        viewModel.adminQueryWebhooksAndLedger(it)
                    },
                    label = { Text("Filter Webhooks by Reference, Sender, PIN, or Amount") },
                    placeholder = { Text("e.g. MNP-LIVE, 080XXXXXXXX, FT-1001, 1000") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = DarkCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Live Inbound Webhook Execution Logs with 1-Click Credit
                if (webhookLogs.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "WEBHOOK LOGS (${webhookLogs.size})",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        webhookLogs.take(6).forEach { log ->
                            Surface(
                                color = DarkObsidian,
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (log.signatureVerified) ElectricEmerald.copy(alpha = 0.3f) else WarningRed.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                color = if (log.signatureVerified) ElectricEmerald.copy(alpha = 0.2f) else WarningRed.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = if (log.signatureVerified) "200 OK" else "401 BAD SIG",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = if (log.signatureVerified) ElectricEmerald else WarningRed,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = log.reference,
                                                style = MaterialTheme.typography.labelSmall.copy(color = TextPrimary, fontWeight = FontWeight.Bold)
                                            )
                                        }

                                        Text(
                                            text = "₦${String.format(java.util.Locale.US, "%,.2f", log.amount)}",
                                            style = MaterialTheme.typography.labelSmall.copy(color = CyberCyan, fontWeight = FontWeight.Black)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Event: ${log.event} • Status: ${log.status}",
                                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 10.sp)
                                    )

                                    if (log.payloadJson.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = log.payloadJson,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = TextMuted,
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace
                                            ),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        Button(
                                            onClick = {
                                                viewModel.adminForceCreditPayment(
                                                    reference = log.reference,
                                                    amount = log.amount,
                                                    narration = log.payloadJson,
                                                    senderName = "Moniepoint Customer",
                                                    targetPhoneOrCode = ""
                                                ) { success, msg ->
                                                    onShowStatusMsg(msg)
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            modifier = Modifier.height(26.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("FORCE CREDIT TO USER", fontSize = 9.sp, fontWeight = FontWeight.Black)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Actual Inbound Notifications (Cloud Run Hub & Gmail for Today)
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = DarkCardBorder)
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ACTUAL INBOUND NOTIFICATIONS TODAY (${actualNotifications.size})",
                        style = MaterialTheme.typography.labelSmall.copy(color = CyberCyan, fontWeight = FontWeight.Bold)
                    )

                    Button(
                        onClick = {
                            viewModel.fetchActualLiveNotifications { list ->
                                onShowStatusMsg("Refreshed ${list.size} actual inbound notifications")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan.copy(alpha = 0.2f), contentColor = CyberCyan),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        if (isFetchingActualNotifications) {
                            FlowButtonLoadingLine(color = CyberCyan, width = 24.dp, height = 2.dp)
                        } else {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("SYNC REAL", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                if (actualNotifications.isEmpty()) {
                    Surface(
                        color = DarkObsidian,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No actual notifications fetched for today yet",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontWeight = FontWeight.Medium)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap 'FETCH ACTUAL NOTIFICATIONS' above to scan live Cloud Run webhook events and Gmail credit alerts for today.",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 10.sp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        actualNotifications.take(8).forEach { notif ->
                            Surface(
                                color = DarkObsidian,
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (notif.isCredited) ElectricEmerald.copy(alpha = 0.4f) else CyberCyan.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                color = if (notif.source == "WEBHOOK") CyberCyan.copy(alpha = 0.2f) else GlowingAmber.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = if (notif.source == "WEBHOOK") "MONIEPOINT WEBHOOK" else "GMAIL ALERT",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = if (notif.source == "WEBHOOK") CyberCyan else GlowingAmber,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = notif.reference,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = TextPrimary,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 10.sp
                                                )
                                            )
                                        }

                                        Text(
                                            text = "₦${String.format(java.util.Locale.US, "%,.2f", notif.amount)}",
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                color = ElectricEmerald,
                                                fontWeight = FontWeight.Black
                                            )
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "From: ",
                                            style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        )
                                        Text(
                                            text = notif.senderName,
                                            style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Narration: ",
                                            style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        )
                                        Text(
                                            text = notif.narration,
                                            style = MaterialTheme.typography.bodySmall.copy(color = CyberCyan, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f, fill = false)) {
                                            Text(
                                                text = if (notif.isCredited) "✓ Credited to Wallet" else notif.statusMessage,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = if (notif.isCredited) ElectricEmerald else GlowingAmber,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp
                                                )
                                            )
                                            Text(
                                                text = notif.dateStr,
                                                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 9.sp)
                                            )
                                        }

                                        if (!notif.isCredited && notif.amount > 0.0) {
                                            Button(
                                                onClick = {
                                                    viewModel.adminForceCreditPayment(
                                                        reference = notif.reference,
                                                        amount = notif.amount,
                                                        narration = notif.narration,
                                                        senderName = notif.senderName,
                                                        targetPhoneOrCode = ""
                                                    ) { success, msg ->
                                                        onShowStatusMsg(msg)
                                                        viewModel.fetchActualLiveNotifications()
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian),
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                modifier = Modifier.height(26.dp)
                                            ) {
                                                Text("CREDIT USER", fontSize = 8.sp, fontWeight = FontWeight.Black)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Gmail 2nd Layer Credit Alert Ingestion Controller (Innobrightcafe@gmail.com)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, CyberCyan.copy(alpha = 0.6f), RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(CyberCyan.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Email, contentDescription = "Gmail Alerts", tint = CyberCyan, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = "GMAIL 2ND LAYER CREDIT ALERT ENGINE",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Fallback Auto-Credit on Webhook Failure • IMAP SSL",
                                style = MaterialTheme.typography.labelSmall.copy(color = CyberCyan, fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        color = if (isGmailConfigured) ElectricEmerald.copy(alpha = 0.15f) else GlowingAmber.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isGmailConfigured) ElectricEmerald.copy(alpha = 0.4f) else GlowingAmber.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(if (isGmailConfigured) ElectricEmerald else GlowingAmber))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isGmailConfigured) "ACTIVE 2ND LAYER" else "PASSWORD REQUIRED", fontSize = 9.sp, fontWeight = FontWeight.Black, color = if (isGmailConfigured) ElectricEmerald else GlowingAmber, maxLines = 1)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "If the Moniepoint webhook is delayed or pending, this system connects to Gmail via IMAP (Port 993), parses 'Credit' alert emails for innobright2010@gmail.com, extracts the transfer narration & amount, and auto-credits the user's wallet.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
                )

                if (!isGmailConfigured) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = GlowingAmber.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GlowingAmber.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "⚠️ Gmail App Password required to read alerts. Generate a 16-character password at myaccount.google.com/apppasswords and enter it below.",
                            style = MaterialTheme.typography.bodySmall.copy(color = GlowingAmber, fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Email Address & App Password Configuration Fields
                OutlinedTextField(
                    value = gmailEmailInput,
                    onValueChange = { gmailEmailInput = it },
                    label = { Text("Gmail Alert Address") },
                    placeholder = { Text("innobright2010@gmail.com") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = DarkCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = gmailPasswordInput,
                    onValueChange = { gmailPasswordInput = it },
                    label = { Text("16-Character Gmail App Password") },
                    placeholder = { Text("abcd efgh ijkl mnop") },
                    singleLine = true,
                    visualTransformation = if (isGmailPasswordMasked) androidx.compose.ui.text.input.PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
                    leadingIcon = {
                        Icon(imageVector = Icons.Outlined.Key, contentDescription = null, tint = GlowingAmber, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        IconButton(onClick = { isGmailPasswordMasked = !isGmailPasswordMasked }) {
                            Icon(
                                imageVector = if (isGmailPasswordMasked) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                contentDescription = "Toggle Password Visibility",
                                tint = TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GlowingAmber,
                        unfocusedBorderColor = DarkCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Search Query Filter for targeted Gmail IMAP scan
                OutlinedTextField(
                    value = gmailSearchFilterInput,
                    onValueChange = { gmailSearchFilterInput = it },
                    label = { Text("Filter Email Scan (Ref, Narration PIN, Sender, or Phone)") },
                    placeholder = { Text("e.g. FT-1001, 080XXXXXXXX, Moniepoint, MNP") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = DarkCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Save Gmail Configuration Button
                    Button(
                        onClick = {
                            val cleanPass = gmailPasswordInput.replace("\\s+".toRegex(), "").trim()
                            val cleanEmail = gmailEmailInput.trim().ifBlank { "innobright2010@gmail.com" }
                            if (cleanPass.isBlank()) {
                                gmailStatusBanner = "⚠️ Please enter your 16-character Google App Password first."
                                onShowStatusMsg("App Password cannot be empty")
                                return@Button
                            }
                            viewModel.saveGmailAlertConfig(cleanEmail, cleanPass)
                            gmailStatusBanner = "✓ Gmail configuration saved for $cleanEmail!"
                            onShowStatusMsg("Saved Gmail App Password Configuration!")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SAVE CONFIG", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 10.sp))
                    }

                    // Live Scan & Sync Inbox Now Button
                    Button(
                        onClick = {
                            val cleanPass = gmailPasswordInput.replace("\\s+".toRegex(), "").trim()
                            val cleanEmail = gmailEmailInput.trim().ifBlank { "innobright2010@gmail.com" }
                            if (cleanPass.isNotBlank()) {
                                viewModel.saveGmailAlertConfig(cleanEmail, cleanPass)
                            }
                            if (cleanPass.isBlank() && gmailAppPassword.isBlank()) {
                                gmailStatusBanner = "⚠️ Enter and save your 16-character Google App Password above before scanning inbox."
                                onShowStatusMsg("App Password required to scan Gmail")
                                return@Button
                            }
                            viewModel.syncGmailCreditAlerts(targetPhone = gmailSearchFilterInput.ifBlank { null }) { res ->
                                gmailStatusBanner = if (res.isSuccess) {
                                    "✓ ${res.message}"
                                } else {
                                    "⚠️ ${res.message}"
                                }
                            }
                        },
                        enabled = !isSyncingGmail,
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        if (isSyncingGmail) {
                            FlowButtonLoadingLine(color = DarkObsidian, width = 28.dp, height = 2.dp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("SCANNING INBOX...", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp))
                        } else {
                            Icon(imageVector = Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("SCAN GMAIL INBOX NOW", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 10.sp))
                        }
                    }
                }

                gmailStatusBanner?.let { banner ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = banner,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (banner.startsWith("✓")) ElectricEmerald else GlowingAmber,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                // Display Last Sync Result Details & Logs
                lastGmailSyncResult?.let { res ->
                    var showAllGmailLogs by remember { mutableStateOf(false) }
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = DarkObsidian,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (res.isSuccess) ElectricEmerald.copy(alpha = 0.3f) else WarningRed.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (res.isSuccess) ElectricEmerald else WarningRed)
                                    )
                                    Text(
                                        text = if (res.isSuccess) "GMAIL SYNC ACTIVE (200 OK)" else "SYNC DIAGNOSTIC / TIMEOUT",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (res.isSuccess) ElectricEmerald else WarningRed,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 10.sp
                                        )
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "${res.alertsFound} Alerts / ${res.newlyCreditedCount} Credited",
                                        style = MaterialTheme.typography.labelSmall.copy(color = CyberCyan, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                                    )
                                    IconButton(
                                        onClick = {
                                            val fullReport = buildString {
                                                appendLine("=== GMAIL SYNC DIAGNOSTIC REPORT ===")
                                                appendLine("Status: ${if (res.isSuccess) "SUCCESS" else "FAILED / TIMEOUT"}")
                                                appendLine("Message: ${res.message}")
                                                appendLine("Alerts Found: ${res.alertsFound} | Credited: ${res.newlyCreditedCount}")
                                                appendLine("Total Credited: ₦${String.format(java.util.Locale.US, "%,.2f", res.totalAmountCredited)}")
                                                appendLine("\n--- IMAP Logs ---")
                                                res.logs.forEach { appendLine(it) }
                                            }
                                            clipboardManager.setText(AnnotatedString(fullReport))
                                            onShowStatusMsg("✓ Copied Gmail diagnostic logs to clipboard!")
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.ContentCopy,
                                            contentDescription = "Copy Error / Diagnostic Logs",
                                            tint = if (res.isSuccess) CyberCyan else WarningRed,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = res.message,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (res.isSuccess) TextPrimary else Color(0xFFFF8B8B),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )

                            // Show detected alerts with force credit button
                            val alertsToShow = (res.allParsedAlerts + res.creditedAlerts).distinctBy { it.reference }
                            if (alertsToShow.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "DETECTED EMAIL ALERTS (${alertsToShow.size})",
                                    style = MaterialTheme.typography.labelSmall.copy(color = ElectricEmerald, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                alertsToShow.forEach { alert ->
                                    val isAlreadySettled = res.creditedAlerts.any { it.reference == alert.reference } ||
                                            res.alreadyCreditedAlerts.any { it.reference == alert.reference }
                                    Surface(
                                        color = DarkSurfaceElevated,
                                        shape = RoundedCornerShape(8.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isAlreadySettled) ElectricEmerald.copy(alpha = 0.4f) else CyberCyan.copy(alpha = 0.4f)),
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp).fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = "₦${String.format(java.util.Locale.US, "%,.2f", alert.amount)}",
                                                        style = MaterialTheme.typography.labelSmall.copy(color = ElectricEmerald, fontWeight = FontWeight.Black)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "• ${alert.senderName.ifBlank { "Bank Customer" }}",
                                                        style = MaterialTheme.typography.labelSmall.copy(color = TextPrimary, fontWeight = FontWeight.Bold)
                                                    )
                                                }
                                                Text(
                                                    text = "Ref: ${alert.reference} • ${alert.dateStr}",
                                                    style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 9.sp)
                                                )
                                                Text(
                                                    text = "Narration: ${alert.rawNarration}",
                                                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 9.sp),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            if (isAlreadySettled) {
                                                Surface(
                                                    color = ElectricEmerald.copy(alpha = 0.2f),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = "✓ CREDITED",
                                                        color = ElectricEmerald,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Black,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                    )
                                                }
                                            } else {
                                                Button(
                                                    onClick = {
                                                        viewModel.adminForceCreditPayment(
                                                            reference = alert.reference,
                                                            amount = alert.amount,
                                                            narration = alert.rawNarration,
                                                            senderName = alert.senderName,
                                                            targetPhoneOrCode = alert.detectedConfirmationCode ?: ""
                                                        ) { success, msg ->
                                                            onShowStatusMsg(msg)
                                                            viewModel.fetchActualLiveNotifications()
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian),
                                                    shape = RoundedCornerShape(6.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    modifier = Modifier.height(28.dp)
                                                ) {
                                                    Text("CREDIT NOW", fontSize = 8.sp, fontWeight = FontWeight.Black)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (res.logs.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    val logsToDisplay = if (showAllGmailLogs) res.logs else res.logs.takeLast(5)
                                    logsToDisplay.forEach { logLine ->
                                        Text(
                                            text = "• $logLine",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = if (logLine.contains("Exception") || logLine.contains("Timeout") || logLine.contains("Failed")) WarningRed.copy(alpha = 0.9f) else TextMuted,
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        )
                                    }
                                }

                                if (res.logs.size > 5) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (showAllGmailLogs) "Collapse logs ▲" else "View all ${res.logs.size} logs ▼",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = CyberCyan,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            modifier = Modifier.clickable { showAllGmailLogs = !showAllGmailLogs }
                                        )

                                        Text(
                                            text = "Tap 📋 icon to copy full trace",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = TextMuted,
                                                fontSize = 8.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Manual Transaction Resolution & Immediate Wallet Creditor
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, GlowingAmber.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Build, contentDescription = "Manual Credit", tint = GlowingAmber)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "MANUAL RESOLUTION & DIRECT DEPOSIT DESK",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "If a customer transferred but narration had a typo, manually credit their wallet with full bookkeeping trail.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = manualCreditAmountInput,
                        onValueChange = { manualCreditAmountInput = it },
                        label = { Text("Amount (₦)") },
                        placeholder = { Text("1000") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GlowingAmber,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = manualCreditTargetInput,
                        onValueChange = { manualCreditTargetInput = it },
                        label = { Text("Target Phone / PIN") },
                        placeholder = { Text("080XXXXXXXX or FT-1001") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GlowingAmber,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.weight(1.4f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = manualCreditRefInput,
                    onValueChange = { manualCreditRefInput = it },
                    label = { Text("Bank Reference / Narration Note") },
                    placeholder = { Text("e.g. OPay-902188 or MNP-81920") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GlowingAmber,
                        unfocusedBorderColor = DarkCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val amt = manualCreditAmountInput.toDoubleOrNull()
                            if (amt == null || amt <= 0) {
                                manualCreditStatusMsg = "Please enter a valid amount (e.g. 1000)"
                                return@Button
                            }
                            isManuallyCrediting = true
                            viewModel.adminForceCreditPayment(
                                reference = manualCreditRefInput.ifBlank { "MANUAL-" + (100000..999999).random() },
                                amount = amt,
                                narration = "Manual Admin Credit: ${manualCreditRefInput.ifBlank { "Verified via Bank Receipt" }}",
                                senderName = "Verified Customer",
                                targetPhoneOrCode = manualCreditTargetInput
                            ) { success, msg ->
                                isManuallyCrediting = false
                                manualCreditStatusMsg = if (success) "✓ $msg" else "⚠️ $msg"
                                onShowStatusMsg(msg)
                            }
                        },
                        enabled = !isManuallyCrediting,
                        colors = ButtonDefaults.buttonColors(containerColor = GlowingAmber, contentColor = DarkObsidian),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        if (isManuallyCrediting) {
                            FlowButtonLoadingLine(color = DarkObsidian, width = 28.dp, height = 2.dp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("CREDITING...", fontWeight = FontWeight.Black, fontSize = 11.sp)
                        } else {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ CREDIT WALLET", fontWeight = FontWeight.Black, fontSize = 11.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            val amt = manualCreditAmountInput.toDoubleOrNull()
                            if (amt == null || amt < 0) {
                                manualCreditStatusMsg = "Please enter a valid balance (e.g. 500)"
                                return@OutlinedButton
                            }
                            isManuallyCrediting = true
                            viewModel.adminSetWalletBalance(
                                targetPhoneOrCode = manualCreditTargetInput,
                                exactNewBalance = amt,
                                reason = manualCreditRefInput.ifBlank { "Manual Balance Rectification" }
                            ) { success, msg ->
                                isManuallyCrediting = false
                                manualCreditStatusMsg = if (success) "✓ $msg" else "⚠️ $msg"
                                onShowStatusMsg(msg)
                            }
                        },
                        enabled = !isManuallyCrediting,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricEmerald),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ElectricEmerald),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.1f)
                    ) {
                        Text("SET BALANCE", fontWeight = FontWeight.Black, fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        isManuallyCrediting = true
                        viewModel.adminDeduplicateDoubleTransactions(
                            targetPhoneOrCode = manualCreditTargetInput
                        ) { success, msg ->
                            isManuallyCrediting = false
                            manualCreditStatusMsg = if (success) "✓ $msg" else "⚠️ $msg"
                            onShowStatusMsg(msg)
                        }
                    },
                    enabled = !isManuallyCrediting,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GlowingAmber),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GlowingAmber.copy(alpha = 0.7f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = GlowingAmber, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("AUTO-DEDUPLICATE DOUBLE TRANSACTIONS", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }

                manualCreditStatusMsg?.let { msg ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (msg.startsWith("✓")) ElectricEmerald else GlowingAmber,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }

        // 4. Native Android Notification Diagnostic Panel
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, if (hasNotificationPermission) ElectricEmerald.copy(alpha = 0.5f) else GlowingAmber.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Shield, contentDescription = "Security Notifications", tint = if (hasNotificationPermission) ElectricEmerald else GlowingAmber)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "NATIVE SYSTEM NOTIFICATIONS TEST CENTER",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (hasNotificationPermission) ElectricEmerald.copy(alpha = 0.15f) else WarningRed.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if (hasNotificationPermission) "ACTIVE" else "REQUIRES PERMISSION",
                            color = if (hasNotificationPermission) ElectricEmerald else WarningRed,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Test instant heads-up Android notifications for deposits, transactions, VPN security tunnel, SMS delivery, and firewall data savings.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
                )

                if (!hasNotificationPermission) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = DarkSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, GlowingAmber.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = GlowingAmber, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Android System Notification Permission Required",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = GlowingAmber, fontSize = 11.sp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "On Android 13+, apps require permission before notifications can appear in the status bar.",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 10.sp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        } else {
                                            AppNotificationManager.openNotificationSettings(context)
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
                                ) {
                                    Text("Grant Permission", color = DarkObsidian, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { AppNotificationManager.openNotificationSettings(context) },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("App Settings", color = TextPrimary, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            hasNotificationPermission = AppNotificationManager.canPostNotification(context)
                            if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            val sent = viewModel.testNativeNotification("tx_deposit")
                            notificationTriggeredMsg = if (sent) "✓ Sent 'Deposit Received' Notification to system tray!" else "⚠️ Permission denied in Android system. Tap 'Grant Permission'."
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("💰 Deposit", fontSize = 11.sp, color = ElectricEmerald, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            hasNotificationPermission = AppNotificationManager.canPostNotification(context)
                            if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            val sent = viewModel.testNativeNotification("tx_vtu")
                            notificationTriggeredMsg = if (sent) "✓ Sent 'VTU Data Bundle' Notification to system tray!" else "⚠️ Permission denied in Android system. Tap 'Grant Permission'."
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("⚡ VTU Sale", fontSize = 11.sp, color = CyberCyan, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            hasNotificationPermission = AppNotificationManager.canPostNotification(context)
                            if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            val sent = viewModel.testNativeNotification("vpn")
                            notificationTriggeredMsg = if (sent) "✓ Sent 'VPN Guard' Notification to system tray!" else "⚠️ Permission denied in Android system. Tap 'Grant Permission'."
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("🛡️ VPN State", fontSize = 11.sp, color = GlowingAmber, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            hasNotificationPermission = AppNotificationManager.canPostNotification(context)
                            if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            val sent = viewModel.testNativeNotification("sms")
                            notificationTriggeredMsg = if (sent) "✓ Sent 'SMS Delivered' Notification to system tray!" else "⚠️ Permission denied in Android system. Tap 'Grant Permission'."
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("✉️ SMS Alert", fontSize = 11.sp, color = CyberCyan, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            hasNotificationPermission = AppNotificationManager.canPostNotification(context)
                            if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            val sent = viewModel.testNativeNotification("data_quota")
                            notificationTriggeredMsg = if (sent) "✓ Sent 'Smart Firewall Alert' Notification to system tray!" else "⚠️ Permission denied in Android system. Tap 'Grant Permission'."
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("📊 Data Saver", fontSize = 11.sp, color = ElectricEmerald, fontWeight = FontWeight.Bold)
                    }
                }

                notificationTriggeredMsg?.let { msg ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (msg.startsWith("✓")) ElectricEmerald else GlowingAmber,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }

        // 9. GOOGLE ADSENSE SEARCH ENGINE CONFIGURATION (ADMIN ONLY)
        val adsenseCx by viewModel.adsenseSearchCx.collectAsStateWithLifecycle()
        val isAdSenseEnabled by viewModel.isAdSenseSearchEnabled.collectAsStateWithLifecycle()
        var tempAdSenseCx by remember(adsenseCx) { mutableStateOf(adsenseCx) }
        var tempAdSenseEnabled by remember(isAdSenseEnabled) { mutableStateOf(isAdSenseEnabled) }
        var adsenseSavedMsg by remember { mutableStateOf<String?>(null) }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, GlowingAmber.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(GlowingAmber.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.MonetizationOn, contentDescription = "AdSense", tint = GlowingAmber, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "GOOGLE ADSENSE SEARCH ENGINE",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                            )
                            Text(
                                text = "Programmable Search Engine & Ads Monetization",
                                style = MaterialTheme.typography.labelSmall.copy(color = GlowingAmber, fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Configure your Google Programmable Search Engine CX ID to monetize web searches made within the built-in Flow Browser. Search revenue routes directly to your linked Google AdSense account.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Enable AdSense Search Monetization",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    )
                    Switch(
                        checked = tempAdSenseEnabled,
                        onCheckedChange = { tempAdSenseEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DarkObsidian,
                            checkedTrackColor = ElectricEmerald,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = DarkSurfaceElevated
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = tempAdSenseCx,
                    onValueChange = { tempAdSenseCx = it },
                    label = { Text("Search Engine ID (CX) / Partner Pub ID") },
                    placeholder = { Text("e.g. partner-pub-XXXXXXXX:YYYYYY or 1a2b3c4d5e") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GlowingAmber,
                        unfocusedBorderColor = DarkCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        viewModel.updateAdSenseSearchConfig(tempAdSenseCx.trim(), tempAdSenseEnabled)
                        adsenseSavedMsg = "✓ Google AdSense Search Configuration Saved!"
                        onShowStatusMsg("Google AdSense Search Updated!")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GlowingAmber, contentColor = DarkObsidian),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("SAVE ADSENSE SEARCH CONFIGURATION", fontWeight = FontWeight.Black)
                }

                adsenseSavedMsg?.let { msg ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.labelSmall.copy(color = ElectricEmerald, fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }

    // Dialog: Edit Custom Secret
    if (showSecretEditDialog) {
        AlertDialog(
            onDismissRequest = { showSecretEditDialog = false },
            title = {
                Text(
                    text = "Configure Moniepoint Webhook Secret",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter the secret key configured on your Moniepoint developer portal. It will be used for SHA-256 HMAC verification.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = customSecretInput,
                        onValueChange = { customSecretInput = it },
                        label = { Text("Webhook Secret Key") },
                        singleLine = false,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GlowingAmber,
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
                        if (customSecretInput.isNotBlank()) {
                            viewModel.updateMoniepointWebhookSecret(customSecretInput.trim())
                            secretSavedBanner = "✓ Custom secret successfully saved!"
                            onShowStatusMsg("Webhook Secret Updated!")
                        }
                        showSecretEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GlowingAmber, contentColor = DarkObsidian)
                ) {
                    Text("Save Secret", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSecretEditDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkSurfaceElevated
        )
    }

    // Dialog: Edit Custom Webhook URL
    if (showUrlEditDialog) {
        AlertDialog(
            onDismissRequest = { showUrlEditDialog = false },
            title = {
                Text(
                    text = "Edit Public Webhook URL",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter the HTTPS endpoint URL to display for Moniepoint webhook routing.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = customUrlInput,
                        onValueChange = { customUrlInput = it },
                        label = { Text("Webhook URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
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
                        if (customUrlInput.isNotBlank()) {
                            viewModel.updateMoniepointWebhookUrl(customUrlInput.trim())
                            onShowStatusMsg("Webhook URL Updated!")
                        }
                        showUrlEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian)
                ) {
                    Text("Save URL", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUrlEditDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkSurfaceElevated
        )
    }
}
