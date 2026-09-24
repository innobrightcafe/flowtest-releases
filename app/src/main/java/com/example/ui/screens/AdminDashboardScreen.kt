package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.api.HttpSmsService
import com.example.data.ui.viewmodel.VpnViewModel
import com.example.ui.components.FlowButtonLoadingLine
import com.example.ui.components.FlowLoadingLine
import com.example.ui.components.admin.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: VpnViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isFetchingBalance by viewModel.isFetchingPairgateBalance.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
    val allClientAccounts by viewModel.allClientAccounts.collectAsStateWithLifecycle()
    val transactionLogs by viewModel.vtuTransactionLogs.collectAsStateWithLifecycle()
    val httpSmsDeliveryLogs by viewModel.httpSmsDeliveryLogs.collectAsStateWithLifecycle()
    val resellerAccount by viewModel.pairgateResellerAccount.collectAsStateWithLifecycle()
    val smsRetailPrice by viewModel.smsRetailPrice.collectAsStateWithLifecycle()
    val smsSenderId by viewModel.smsSenderId.collectAsStateWithLifecycle()

    var selectedCategory by remember { mutableStateOf(AdminCategory.FINANCIALS) }
    var showCategoryMenu by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Dialog state controllers
    var showFundWalletDialog by remember { mutableStateOf(false) }
    var fundAmountInput by remember { mutableStateOf("10000") }

    var showEditSettlementDialog by remember { mutableStateOf(false) }
    var editBankNameInput by remember { mutableStateOf("") }
    var editAccountNumberInput by remember { mutableStateOf("") }
    var editAccountNameInput by remember { mutableStateOf("") }
    var editResellerNameInput by remember { mutableStateOf("") }
    var editBusinessNameInput by remember { mutableStateOf("") }

    var showTestSmsDialog by remember { mutableStateOf(false) }
    var testSmsSender by remember(smsSenderId) { mutableStateOf(smsSenderId) }
    var testSmsRecipient by remember { mutableStateOf("") }
    var testSmsMessage by remember { mutableStateOf("") }
    var isSendingTestSms by remember { mutableStateOf(false) }
    var testSmsResultMsg by remember { mutableStateOf<String?>(null) }

    var statusNotificationMsg by remember { mutableStateOf<String?>(null) }

    var adminPinInput by remember { mutableStateOf("") }
    var pinAuthError by remember { mutableStateOf<String?>(null) }

    // Trigger balance and profile fetch on initial screen open
    LaunchedEffect(isAdmin) {
        if (isAdmin) {
            viewModel.fetchPairgateResellerBalance()
            viewModel.fetchPairgateAdminProfile()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "ADMIN PORTAL",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.2.sp
                            )
                        )
                        Text(
                            text = selectedCategory.title,
                            style = MaterialTheme.typography.bodySmall.copy(color = CyberCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("admin_back_btn")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    // Quick Menu Dropdown
                    Box {
                        IconButton(onClick = { showCategoryMenu = true }) {
                            Icon(imageVector = Icons.Default.Apps, contentDescription = "Category Menu", tint = CyberCyan)
                        }

                        DropdownMenu(
                            expanded = showCategoryMenu,
                            onDismissRequest = { showCategoryMenu = false },
                            modifier = Modifier.background(DarkSurfaceElevated)
                        ) {
                            AdminCategory.entries.forEach { cat ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = cat.icon,
                                                contentDescription = cat.title,
                                                tint = if (selectedCategory == cat) CyberCyan else TextSecondary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = cat.title,
                                                color = if (selectedCategory == cat) CyberCyan else TextPrimary,
                                                fontWeight = if (selectedCategory == cat) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedCategory = cat
                                        showCategoryMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Balance Sync Action
                    IconButton(
                        onClick = { viewModel.fetchPairgateResellerBalance() },
                        enabled = !isFetchingBalance
                    ) {
                        if (isFetchingBalance) {
                            FlowButtonLoadingLine(color = CyberCyan, width = 20.dp, height = 2.dp)
                        } else {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Sync Balance", tint = CyberCyan)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        containerColor = DarkSurface
    ) { innerPadding ->
        if (!isAdmin) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .border(1.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(GlowingAmber.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Restricted Area",
                                tint = GlowingAmber,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "ADMIN ACCESS REQUIRED",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                color = TextPrimary
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Only authorized administrators can view this dashboard and control infrastructure settings. Enter your Master Admin Passcode below:",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        OutlinedTextField(
                            value = adminPinInput,
                            onValueChange = { adminPinInput = it },
                            label = { Text("Master Admin Passcode / Key") },
                            placeholder = { Text("Enter Passcode (e.g. 779900)") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberCyan,
                                unfocusedBorderColor = DarkCardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        pinAuthError?.let { err ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = err,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = WarningRed,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                val success = viewModel.authenticateAdminWithKey(adminPinInput)
                                if (success) {
                                    pinAuthError = null
                                } else {
                                    pinAuthError = "Invalid Passcode. Enter master key (Default: 779900)"
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberCyan,
                                contentColor = DarkObsidian
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.Key, contentDescription = "Unlock", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("UNLOCK ADMIN PORTAL", fontWeight = FontWeight.Black)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = onNavigateBack,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Return to App")
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Category Tabs Navigation Bar
                ScrollableTabRow(
                    selectedTabIndex = selectedCategory.ordinal,
                    containerColor = DarkSurface,
                    contentColor = CyberCyan,
                    edgePadding = 12.dp,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedCategory.ordinal]),
                            color = CyberCyan,
                            height = 3.dp
                        )
                    },
                    divider = { HorizontalDivider(color = DarkCardBorder) }
                ) {
                    AdminCategory.entries.forEach { category ->
                        val isSelected = selectedCategory == category
                        val badgeCount = when (category) {
                            AdminCategory.CLIENTS -> allClientAccounts.size
                            AdminCategory.AUDIT_LOGS -> transactionLogs.size
                            AdminCategory.SMS_GATEWAY -> httpSmsDeliveryLogs.size
                            else -> null
                        }

                        Tab(
                            selected = isSelected,
                            onClick = { selectedCategory = category },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = category.icon,
                                        contentDescription = category.shortLabel,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (isSelected) CyberCyan else TextMuted
                                    )
                                    Text(
                                        text = category.shortLabel,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) CyberCyan else TextSecondary,
                                        fontSize = 12.sp
                                    )
                                    if (badgeCount != null && badgeCount > 0) {
                                        Surface(
                                            color = if (isSelected) CyberCyan else DarkSurfaceElevated,
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text(
                                                text = "$badgeCount",
                                                color = if (isSelected) DarkObsidian else TextMuted,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        )
                    }
                }

                // Category Subtitle Strip
                Surface(
                    color = DarkSurfaceElevated,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = selectedCategory.description,
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp),
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Tab ${selectedCategory.ordinal + 1}/${AdminCategory.entries.size}",
                            style = MaterialTheme.typography.labelSmall.copy(color = CyberCyan, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        )
                    }
                }

                // Main Tab Content
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    item {
                        statusNotificationMsg?.let { msg ->
                            Surface(
                                color = ElectricEmerald.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, ElectricEmerald.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = ElectricEmerald, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = msg, style = MaterialTheme.typography.bodySmall.copy(color = ElectricEmerald, fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f))
                                    IconButton(onClick = { statusNotificationMsg = null }, modifier = Modifier.size(20.dp)) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = "Dismiss", tint = ElectricEmerald, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }

                    item {
                        when (selectedCategory) {
                            AdminCategory.FINANCIALS -> {
                                AdminFinancialsTab(
                                    viewModel = viewModel,
                                    onFundWalletClick = { showFundWalletDialog = true },
                                    onEditSettlementClick = {
                                        editBankNameInput = resellerAccount.bankName
                                        editAccountNumberInput = resellerAccount.bankAccountNumber
                                        editAccountNameInput = resellerAccount.bankAccountName
                                        editResellerNameInput = resellerAccount.resellerName
                                        editBusinessNameInput = resellerAccount.businessName
                                        showEditSettlementDialog = true
                                    },
                                    onShowStatusMsg = { msg -> statusNotificationMsg = msg }
                                )
                            }
                            AdminCategory.PRICING -> {
                                AdminPricingTab(viewModel = viewModel)
                            }
                            AdminCategory.CLIENTS -> {
                                AdminClientsTab(
                                    viewModel = viewModel,
                                    onShowStatusMsg = { msg -> statusNotificationMsg = msg }
                                )
                            }
                            AdminCategory.SMS_GATEWAY -> {
                                AdminSmsGatewayTab(
                                    viewModel = viewModel,
                                    onTestSmsClick = { showTestSmsDialog = true },
                                    onShowStatusMsg = { msg -> statusNotificationMsg = msg }
                                )
                            }
                            AdminCategory.SECURITY_API -> {
                                AdminSecurityTab(
                                    viewModel = viewModel,
                                    onNavigateBack = onNavigateBack,
                                    onShowStatusMsg = { msg -> statusNotificationMsg = msg }
                                )
                            }
                            AdminCategory.AUDIT_LOGS -> {
                                AdminAuditLogsTab(viewModel = viewModel)
                            }
                            AdminCategory.BROADCAST_OFFERS -> {
                                AdminAdvertsOffersTab(
                                    viewModel = viewModel,
                                    onShowStatusMsg = { msg -> statusNotificationMsg = msg }
                                )
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }

    // Modal Dialog: Fund Reseller Wallet
    if (showFundWalletDialog) {
        AlertDialog(
            onDismissRequest = { showFundWalletDialog = false },
            containerColor = DarkSurface,
            icon = {
                Icon(imageVector = Icons.Default.AccountBalanceWallet, contentDescription = "Fund", tint = ElectricEmerald, modifier = Modifier.size(32.dp))
            },
            title = { Text("Fund Reseller Wallet", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Select or enter funding amount to add to your VTU vending balance:", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("5000", "10000", "25000").forEach { amt ->
                            SuggestionChip(
                                onClick = { fundAmountInput = amt },
                                label = { Text("₦$amt") },
                                colors = SuggestionChipDefaults.suggestionChipColors(containerColor = DarkSurfaceElevated, labelColor = CyberCyan)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = fundAmountInput,
                        onValueChange = { fundAmountInput = it },
                        label = { Text("Amount (NGN)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = fundAmountInput.toDoubleOrNull() ?: 0.0
                        if (amt > 0) {
                            viewModel.fundAdminWallet(amt)
                            statusNotificationMsg = "Added ₦${String.format("%,.2f", amt)} to Reseller Wallet"
                        }
                        showFundWalletDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian)
                ) {
                    Text("Fund Wallet")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFundWalletDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }

    // Modal Dialog: Set Live Pairgate Settlement Account
    if (showEditSettlementDialog) {
        AlertDialog(
            onDismissRequest = { showEditSettlementDialog = false },
            containerColor = DarkSurface,
            icon = {
                Icon(imageVector = Icons.Default.AccountBalance, contentDescription = "Edit Account", tint = CyberCyan, modifier = Modifier.size(28.dp))
            },
            title = {
                Text("Set Live Pairgate Settlement Account", color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Configure the official company bank account where users make transfer deposits, and where Pairgate settles your payout.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )

                    Text("Quick Select Bank:", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontWeight = FontWeight.Bold))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("FlowTest Settlement", "Providus Bank", "Wema Bank", "SafeHaven MFB", "PalmPay", "OPay", "Kuda Bank", "Zenith Bank", "Access Bank", "GTBank", "First Bank").forEach { bName ->
                            val sel = editBankNameInput.trim().equals(bName, ignoreCase = true)
                            FilterChip(
                                selected = sel,
                                onClick = { editBankNameInput = bName },
                                label = { Text(bName, fontSize = 11.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyberCyan,
                                    selectedLabelColor = DarkObsidian,
                                    containerColor = DarkSurfaceElevated,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }

                    OutlinedTextField(
                        value = editBankNameInput,
                        onValueChange = { editBankNameInput = it },
                        label = { Text("Settlement Bank Name") },
                        placeholder = { Text("e.g. FlowTest Settlement / Providus Bank") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    OutlinedTextField(
                        value = editAccountNumberInput,
                        onValueChange = { editAccountNumberInput = it },
                        label = { Text("10-Digit Settlement Account Number") },
                        placeholder = { Text("6666468328") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    OutlinedTextField(
                        value = editAccountNameInput,
                        onValueChange = { editAccountNameInput = it },
                        label = { Text("Settlement Beneficiary Name") },
                        placeholder = { Text("e.g. FlowTest") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    OutlinedTextField(
                        value = editBusinessNameInput,
                        onValueChange = { editBusinessNameInput = it },
                        label = { Text("Business / Entity Name") },
                        placeholder = { Text("FlowTest") },
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
                        val cleanAcc = editAccountNumberInput.trim()
                        val cleanBank = editBankNameInput.trim()
                        if (cleanAcc.isNotBlank() && cleanBank.isNotBlank()) {
                            val accName = editAccountNameInput.trim().ifBlank { editBusinessNameInput.trim().ifBlank { "FlowTest" } }
                            val busName = editBusinessNameInput.trim().ifBlank { accName }
                            val resName = editResellerNameInput.trim().ifBlank { busName }

                            viewModel.updateAdminSettlementAccount(
                                bankName = cleanBank,
                                accountNumber = cleanAcc,
                                accountName = accName,
                                resellerName = resName,
                                businessName = busName
                            )
                            statusNotificationMsg = "Settlement account updated: $cleanBank ($cleanAcc) • $accName"
                            showEditSettlementDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian)
                ) {
                    Text("Save Account", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditSettlementDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }

    // Modal Dialog: Test SMS Dispatch
    if (showTestSmsDialog) {
        val (charCount, wordCount, pageCount) = HttpSmsService.calculateSmsMetrics(testSmsMessage)
        val estimatedCost = pageCount * smsRetailPrice

        AlertDialog(
            onDismissRequest = { if (!isSendingTestSms) showTestSmsDialog = false },
            containerColor = DarkSurface,
            icon = {
                Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Test SMS", tint = CyberCyan, modifier = Modifier.size(32.dp))
            },
            title = { Text("Test SMS Dispatch", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Sends a live encrypted SMS via HttpSMS gateway and triggers native system notification.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )

                    OutlinedTextField(
                        value = testSmsSender,
                        onValueChange = { testSmsSender = it },
                        label = { Text("Sender (From Gateway SIM Number)") },
                        placeholder = { Text("+2348137545370") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    OutlinedTextField(
                        value = testSmsRecipient,
                        onValueChange = { testSmsRecipient = it },
                        label = { Text("Recipient Phone Number (To)") },
                        placeholder = { Text("e.g. +2348012345678 or 08012345678") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    OutlinedTextField(
                        value = testSmsMessage,
                        onValueChange = { testSmsMessage = it },
                        label = { Text("SMS Content") },
                        placeholder = { Text("Enter live SMS text to dispatch via HttpSMS gateway...") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Surface(
                        color = DarkSurfaceElevated,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("$charCount Chars ($wordCount words)", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted))
                            Text("$pageCount Page(s) • Est: ₦${String.format("%.2f", estimatedCost)}", style = MaterialTheme.typography.labelSmall.copy(color = CyberCyan, fontWeight = FontWeight.Bold))
                        }
                    }

                    testSmsResultMsg?.let { msg ->
                        Text(
                            text = msg,
                            color = if (msg.contains("Sent", ignoreCase = true) || msg.contains("Success", ignoreCase = true)) ElectricEmerald else GlowingAmber,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (testSmsRecipient.isNotBlank() && testSmsMessage.isNotBlank()) {
                            isSendingTestSms = true
                            viewModel.sendSmsServiceBroadcast(
                                recipients = listOf(testSmsRecipient),
                                message = testSmsMessage,
                                senderId = testSmsSender.ifBlank { smsSenderId }
                            ) { success, msg, charged ->
                                isSendingTestSms = false
                                testSmsResultMsg = if (success) "✓ $msg (₦${String.format("%.2f", charged)} billed)" else "❌ $msg"
                            }
                        } else {
                            testSmsResultMsg = "Please enter recipient and message"
                        }
                    },
                    enabled = !isSendingTestSms,
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian)
                ) {
                    if (isSendingTestSms) {
                        FlowButtonLoadingLine(color = DarkObsidian, width = 40.dp)
                    } else {
                        Text("Send SMS", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showTestSmsDialog = false },
                    enabled = !isSendingTestSms
                ) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }
}
