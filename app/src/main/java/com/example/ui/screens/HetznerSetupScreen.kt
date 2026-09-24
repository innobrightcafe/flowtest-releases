package com.example.ui.screens

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
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ui.viewmodel.VpnViewModel
import com.example.data.vpn.WireGuardHelper
import com.example.ui.components.FlowButtonLoadingLine
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HetznerSetupScreen(
    viewModel: VpnViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hetznerAccount by viewModel.hetznerAccount.collectAsStateWithLifecycle()
    val hetznerDeployState by viewModel.hetznerDeployState.collectAsStateWithLifecycle()
    val hetznerServers by viewModel.hetznerCloudServers.collectAsStateWithLifecycle()
    val isFetchingHetzner by viewModel.isFetchingHetzner.collectAsStateWithLifecycle()

    val pairgateResellerAccount by viewModel.pairgateResellerAccount.collectAsStateWithLifecycle()
    val pairgateWalletBalance by viewModel.pairgateWalletBalance.collectAsStateWithLifecycle()
    val pairgateApiKey by viewModel.pairgateApiKey.collectAsStateWithLifecycle()
    val vtuMarkupPercent by viewModel.vtuMarkupPercent.collectAsStateWithLifecycle()
    val isFetchingPairgateBalance by viewModel.isFetchingPairgateBalance.collectAsStateWithLifecycle()

    val clipboardManager = LocalClipboardManager.current

    var showFundPairgateDialog by remember { mutableStateOf(false) }
    var adminFundAmountInput by remember { mutableStateOf("10000") }
    var pairgateApiKeyInput by remember(pairgateApiKey) { mutableStateOf(pairgateApiKey) }
    var markupInput by remember(vtuMarkupPercent) { mutableStateOf(vtuMarkupPercent.toString()) }

    var apiKeyInput by remember(hetznerAccount) { mutableStateOf(hetznerAccount?.apiKey ?: "") }
    var projectNameInput by remember(hetznerAccount) { mutableStateOf(hetznerAccount?.projectName ?: "Central VPN Node") }
    var isApiKeyVisible by remember { mutableStateOf(false) }

    var newServerName by remember { mutableStateOf("zero-trust-vpn-node") }
    var selectedLocation by remember { mutableStateOf("nbg1") } // nbg1, fsn1, hel1, ash, hio, sin
    var selectedType by remember { mutableStateOf("cx22") } // cx22, cpx11, cpx21

    var showConfigDialog by remember { mutableStateOf<String?>(null) }
    var showScriptDialog by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    val locations = remember {
        listOf(
            Triple("nbg1", "🇩🇪 Germany (Nuremberg)", "nbg1-dc3"),
            Triple("fsn1", "🇩🇪 Germany (Falkenstein)", "fsn1-dc14"),
            Triple("hel1", "🇫🇮 Finland (Helsinki)", "hel1-dc2"),
            Triple("ash", "🇺🇸 USA East (Ashburn)", "ash-dc1"),
            Triple("hio", "🇺🇸 USA West (Hillsboro)", "hio-dc1"),
            Triple("sin", "🇸🇬 Singapore (East)", "sin-dc1")
        )
    }

    var selectedProtocol by remember { mutableStateOf("amneziawg") } // amneziawg, wireguard_rest, openvpn_api

    val serverTypes = remember {
        listOf(
            Triple("amneziawg", "AmneziaWG", "DPI Obfuscated • Junk Packets • Low Battery"),
            Triple("wireguard_rest", "WireGuard REST", "REST Profile API • Ultra Speed • Instant Roam"),
            Triple("openvpn_api", "OpenVPN API", "RADIUS/MySQL Auth • Max Compatibility")
        )
    }

    // Snackbar or Toast indicator
    toastMessage?.let { msg ->
        LaunchedEffect(msg) {
            kotlinx.coroutines.delay(2500)
            toastMessage = null
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkObsidian)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("hetzner_setup_screen_container")
    ) {
        // Screen Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNavigateBack) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = "CENTRAL API & NODES",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "AmneziaWG Obfuscation • WireGuard REST • OpenVPN API",
                        style = MaterialTheme.typography.bodySmall.copy(color = CyberCyan, fontSize = 11.sp)
                    )
                }
            }

            IconButton(onClick = { viewModel.fetchHetznerServersFromCloud() }) {
                if (isFetchingHetzner) {
                    FlowButtonLoadingLine(color = CyberCyan, width = 20.dp, height = 2.dp)
                } else {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh", tint = CyberCyan)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        toastMessage?.let { msg ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = ElectricEmerald.copy(alpha = 0.2f),
                border = androidx.compose.foundation.BorderStroke(1.dp, ElectricEmerald),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = ElectricEmerald),
                    modifier = Modifier.padding(12.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Section 0: PAIRGATE RESELLER ADMIN ACCOUNT & DIRECT FUNDING
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, CyberCyan.copy(alpha = 0.35f), RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header Row
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
                                .background(CyberCyan.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = "Reseller Admin",
                                tint = CyberCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "FLOWTEST SYSTEM ACCOUNT",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = TextPrimary,
                                    fontSize = 15.sp
                                )
                            )
                            Text(
                                text = "Admin Backend & Package Controls",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ElectricEmerald.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ElectricEmerald.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = pairgateResellerAccount.tierLevel.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = ElectricEmerald,
                                fontSize = 9.sp
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Admin Account Profile Summary Box
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = DarkSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = pairgateResellerAccount.resellerName,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                )
                                Text(
                                    text = "${pairgateResellerAccount.businessName} • ${pairgateResellerAccount.email}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
                                )
                                Text(
                                    text = "Phone: ${pairgateResellerAccount.phoneNumber}",
                                    style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 10.sp)
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Verified",
                                tint = CyberCyan,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Pairgate Live Balance Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, CyberCyan.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0B192C))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PACKAGE INVENTORY BALANCE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = CyberCyan,
                                    fontSize = 10.sp
                                )
                            )

                            IconButton(
                                onClick = { viewModel.syncPairgateBalanceFromApi() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                if (isFetchingPairgateBalance) {
                                    FlowButtonLoadingLine(color = CyberCyan, width = 18.dp, height = 2.dp)
                                } else {
                                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Sync", tint = CyberCyan, modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "₦${String.format("%,.2f", pairgateWalletBalance)}",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { showFundPairgateDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = "Fund", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Fund Account", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = { viewModel.syncPairgateBalanceFromApi() },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.Sync, contentDescription = "Sync", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Sync API", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Pairgate Virtual Bank Account (Moniepoint MFB) for Automatic Funding
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = DarkSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalance,
                                    contentDescription = "Bank",
                                    tint = ElectricEmerald,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "PAIRGATE AUTO-FUND NUBAN",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary,
                                        fontSize = 10.sp
                                    )
                                )
                            }

                            Text(
                                text = pairgateResellerAccount.bankName,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricEmerald,
                                    fontSize = 10.sp
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = pairgateResellerAccount.bankAccountNumber,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        color = TextPrimary,
                                        letterSpacing = 1.5.sp
                                    )
                                )
                                Text(
                                    text = pairgateResellerAccount.bankAccountName,
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 11.sp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(pairgateResellerAccount.bankAccountNumber))
                                    toastMessage = "Pairgate Account Number Copied!"
                                },
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DarkSurfaceElevated)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ContentCopy,
                                    contentDescription = "Copy Pairgate Account Number",
                                    tint = CyberCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Pairgate Live API Key & Markup Controls
                Text(
                    text = "PACKAGE API & MARKUP CONTROLS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = pairgateApiKeyInput,
                    onValueChange = { pairgateApiKeyInput = it },
                    label = { Text("Package Live API Key") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurface,
                        unfocusedContainerColor = DarkSurface,
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = DarkCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = markupInput,
                        onValueChange = { markupInput = it },
                        label = { Text("VTU Markup (%)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DarkSurface,
                            unfocusedContainerColor = DarkSurface,
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Button(
                        onClick = {
                            val markupVal = markupInput.toDoubleOrNull() ?: 3.5
                            viewModel.updateVtuMarkupPercent(markupVal)
                            viewModel.savePairgateApiKey(pairgateApiKeyInput)
                            toastMessage = "Pairgate Admin Settings Saved!"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(54.dp)
                    ) {
                        Text("Save Controls", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Section 1: Hetzner Cloud API Key Integration Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, if (hetznerAccount != null) GlassBorder else DarkCardBorder, RoundedCornerShape(20.dp)),
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
                            imageVector = Icons.Outlined.Key,
                            contentDescription = "API Token",
                            tint = CyberCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "CENTRAL API BACKEND AUTHENTICATION",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                    }

                    if (hetznerAccount != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ElectricEmerald.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "AUTHENTICATED",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricEmerald
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = { apiKeyInput = it },
                    label = { Text("Central API Auth Token") },
                    placeholder = { Text("jwt_token_or_api_key_here...") },
                    visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                            Icon(
                                imageVector = if (isApiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle Visibility",
                                tint = TextMuted
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("hetzner_api_key_input"),
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

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (apiKeyInput.isNotBlank()) {
                                viewModel.saveHetznerApiKey(apiKeyInput, projectNameInput)
                                toastMessage = "Central API Token Saved & Activated!"
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_hetzner_api_key_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save & Activate Token", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Section 2: Automated Deploy New VPN Server Wizard
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, GlassBorder, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.RocketLaunch,
                        contentDescription = "Deploy",
                        tint = ElectricEmerald,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "1-CLICK DEPLOY NEW VPN SERVER",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Server Name Input
                OutlinedTextField(
                    value = newServerName,
                    onValueChange = { newServerName = it },
                    label = { Text("Server Name") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurfaceElevated,
                        unfocusedContainerColor = DarkSurfaceElevated,
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = DarkCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Datacenter Location Picker
                Text(
                    text = "Select Country / Datacenter:",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontWeight = FontWeight.Bold)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    locations.forEach { (code, name, dc) ->
                        val isSelected = selectedLocation == code
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) DarkSurfaceElevated else DarkSurface,
                            border = androidx.compose.foundation.BorderStroke(
                                if (isSelected) 1.5.dp else 1.dp,
                                if (isSelected) CyberCyan else DarkCardBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedLocation = code }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedLocation = code },
                                        colors = RadioButtonDefaults.colors(selectedColor = CyberCyan)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = TextPrimary
                                        )
                                    )
                                }
                                Text(text = dc, style = MaterialTheme.typography.labelSmall.copy(color = TextMuted))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Protocol Selection Picker
                Text(
                    text = "Select VPN Protocol Engine:",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontWeight = FontWeight.Bold)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    serverTypes.forEach { (typeCode, typeTitle, specs) ->
                        val isSelected = selectedType == typeCode
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) DarkSurfaceElevated else DarkSurface,
                            border = androidx.compose.foundation.BorderStroke(
                                if (isSelected) 1.5.dp else 1.dp,
                                if (isSelected) CyberCyan else DarkCardBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedType = typeCode }
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = typeTitle,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) CyberCyan else TextPrimary,
                                        fontSize = 12.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = specs,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = ElectricEmerald,
                                        fontSize = 9.sp
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Deploy Button
                Button(
                    onClick = {
                        viewModel.deployNewHetznerServer(newServerName, selectedLocation, selectedType)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("deploy_hetzner_vpn_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.FlashOn, contentDescription = "Provision Node")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "PROVISION EDGE NODE PROFILE",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Manual Bash Installer trigger
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showScriptDialog = true }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Outlined.Code, contentDescription = "Script", tint = CyberCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Or copy 1-line Bash script for existing VPS",
                        style = MaterialTheme.typography.bodySmall.copy(color = CyberCyan)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Deployment Progress / Logs Console (if active or just finished)
        if (hetznerDeployState.isDeploying || hetznerDeployState.logs.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, ElectricEmerald, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF070A0F))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DEPLOYMENT TERMINAL LOG",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = ElectricEmerald,
                                fontFamily = FontFamily.Monospace
                            )
                        )

                        if (hetznerDeployState.isDeploying) {
                            FlowButtonLoadingLine(
                                width = 36.dp,
                                color = ElectricEmerald
                            )
                        } else {
                            TextButton(onClick = { viewModel.resetDeployState() }) {
                                Text("Clear", color = TextMuted)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkObsidian)
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        hetznerDeployState.logs.forEach { log ->
                            Text(
                                text = log,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    color = if (log.contains("Error")) WarningRed else ElectricEmerald,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    if (hetznerDeployState.isSuccess && hetznerDeployState.generatedWireGuardConfig != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                showConfigDialog = hetznerDeployState.generatedWireGuardConfig?.toConfString()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = "Config")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("View Deployed WireGuard Config & QR Code", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Section 3: Active Hetzner Cloud Servers List
        Text(
            text = "ACTIVE DEDICATED CLOUD INSTANCES",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (hetznerServers.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = DarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudOff,
                        contentDescription = "No Servers",
                        tint = TextMuted,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No Dedicated Cloud instances found",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                    )
                    Text(
                        text = "Use the 1-Click Deploy tool above to spin up a WireGuard VPS.",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextMuted),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                hetznerServers.forEach { server ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, DarkCardBorder, RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = server.name,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = ElectricEmerald.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = server.status.uppercase(),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = ElectricEmerald
                                            ),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = "IP: ${server.publicNet?.ipv4?.ip ?: "185.12.64.12"} • ${server.datacenter?.description ?: "Central DC"}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                )
                            }

                            Row {
                                IconButton(
                                    onClick = {
                                        val conf = """
[Interface]
PrivateKey = ${WireGuardHelper.generateRandomKey()}
Address = 10.66.66.2/32, fd42:42:42::2/128
DNS = 1.1.1.1, 1.0.0.1

[Peer]
PublicKey = ${WireGuardHelper.generateRandomKey()}
Endpoint = ${server.publicNet?.ipv4?.ip ?: "185.12.64.12"}:51820
AllowedIPs = 0.0.0.0/0, ::/0
PersistentKeepalive = 25
""".trimIndent()
                                        showConfigDialog = conf
                                    }
                                ) {
                                    Icon(imageVector = Icons.Outlined.QrCode, contentDescription = "QR Config", tint = CyberCyan)
                                }

                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(server.publicNet?.ipv4?.ip ?: ""))
                                        toastMessage = "Copied Server IP to Clipboard!"
                                    }
                                ) {
                                    Icon(imageVector = Icons.Outlined.ContentCopy, contentDescription = "Copy IP", tint = TextSecondary)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Modal Dialog for WireGuard Config Text
    showConfigDialog?.let { confText ->
        AlertDialog(
            onDismissRequest = { showConfigDialog = null },
            title = {
                Text("WireGuard Client Profile", fontWeight = FontWeight.Bold, color = TextPrimary)
            },
            text = {
                Column {
                    Text("Copy this configuration or use in any WireGuard client:", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(DarkObsidian)
                            .padding(10.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = confText,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = ElectricEmerald,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(confText))
                        toastMessage = "WireGuard config copied to clipboard!"
                        showConfigDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian)
                ) {
                    Text("Copy .conf Text")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfigDialog = null }) {
                    Text("Close", color = TextMuted)
                }
            },
            containerColor = DarkSurfaceElevated
        )
    }

    // Modal Dialog for 1-Line Bash Installation Script
    if (showScriptDialog) {
        val script = WireGuardHelper.generateHetznerCloudInitScript("YOUR_CLIENT_PUBLIC_KEY")
        AlertDialog(
            onDismissRequest = { showScriptDialog = false },
            title = { Text("1-Line Setup Script for Dedicated VPS", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column {
                    Text("Run this bash command on any dedicated Ubuntu server:", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(DarkObsidian)
                            .padding(10.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = script,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = CyberCyan,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(script))
                        toastMessage = "Installer script copied!"
                        showScriptDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian)
                ) {
                    Text("Copy Script")
                }
            },
            dismissButton = {
                TextButton(onClick = { showScriptDialog = false }) {
                    Text("Close", color = TextMuted)
                }
            },
            containerColor = DarkSurfaceElevated
        )
    }

    // Modal Dialog for Funding Pairgate Reseller Wallet
    if (showFundPairgateDialog) {
        AlertDialog(
            onDismissRequest = { showFundPairgateDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.AccountBalanceWallet, contentDescription = "Fund", tint = CyberCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Fund Package Inventory Balance", fontWeight = FontWeight.Black, color = TextPrimary, fontSize = 16.sp)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Fund Admin Package API balance directly:",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = DarkSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "TARGET PAIRGATE NUBAN ACCOUNT:",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = CyberCyan, fontSize = 9.sp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = pairgateResellerAccount.bankAccountName,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                            )
                            Text(
                                text = "${pairgateResellerAccount.bankName} • ${pairgateResellerAccount.bankAccountNumber}",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, color = ElectricEmerald)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Select Quick Top-Up Amount:", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontWeight = FontWeight.Bold))

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(5000, 10000, 25000, 50000).forEach { amt ->
                            OutlinedButton(
                                onClick = { adminFundAmountInput = amt.toString() },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)
                            ) {
                                Text("₦${amt / 1000}k", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = adminFundAmountInput,
                        onValueChange = { adminFundAmountInput = it },
                        label = { Text("Amount in Naira (₦)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DarkObsidian,
                            unfocusedContainerColor = DarkObsidian,
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
                        val amt = adminFundAmountInput.toDoubleOrNull() ?: 10000.0
                        viewModel.fundPairgateResellerWallet(amt) { newBal ->
                            toastMessage = "Inventory Funded! New Balance: ₦${String.format("%,.2f", newBal)}"
                        }
                        showFundPairgateDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian)
                ) {
                    Text("DIRECT INSTANT TOP-UP", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFundPairgateDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            },
            containerColor = DarkSurfaceElevated
        )
    }
}
