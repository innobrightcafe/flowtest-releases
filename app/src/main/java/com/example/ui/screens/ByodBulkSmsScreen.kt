package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.api.HttpSmsDeliveryLog
import com.example.data.api.HttpSmsDeliveryStats
import com.example.data.api.HttpSmsService
import com.example.data.ui.viewmodel.VpnViewModel
import com.example.ui.components.FlowButtonLoadingLine
import com.example.ui.components.TransactionAuthorizationDialog
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ByodBulkSmsScreen(
    viewModel: VpnViewModel,
    onNavigateBack: () -> Unit = {},
    initialTab: Int = 0,
    modifier: Modifier = Modifier
) {
    val walletBalance by viewModel.userWalletBalance.collectAsStateWithLifecycle()
    val retailPrice by viewModel.smsRetailPrice.collectAsStateWithLifecycle()
    val senderId by viewModel.smsSenderId.collectAsStateWithLifecycle()
    val isByodSubscribed by viewModel.isByodSubscribed.collectAsStateWithLifecycle()
    val byodExpiry by viewModel.byodSubscriptionExpiry.collectAsStateWithLifecycle()
    val batchCredits by viewModel.byodBatchSmsCredits.collectAsStateWithLifecycle()
    val connectedDevices by viewModel.byodConnectedDevices.collectAsStateWithLifecycle()
    val contactGroups by viewModel.smsContactGroups.collectAsStateWithLifecycle()
    val deliveryStats by viewModel.httpSmsDeliveryStats.collectAsStateWithLifecycle()
    val deliveryLogs by viewModel.httpSmsDeliveryLogs.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(initialTab) } // 0 = Blaster, 1 = BYOD SaaS, 2 = Groups, 3 = Analytics
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Blaster States
    var blastRecipientsText by remember { mutableStateOf("") }
    var blastMessageText by remember { mutableStateOf("") }
    var useByodRoute by remember { mutableStateOf(isByodSubscribed || batchCredits > 0) }
    var isSendingBlast by remember { mutableStateOf(false) }

    // Modals
    var showConnectDeviceDialog by remember { mutableStateOf(false) }
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var showSaaSPlanDialog by remember { mutableStateOf(false) }
    var showBatchCreditsDialog by remember { mutableStateOf(false) }
    var selectedBatchPackForAuth by remember { mutableStateOf<VpnViewModel.ByodSmsBatchPack?>(null) }

    val tabs = listOf(
        "🚀 Bulk Blaster",
        "📱 BYOD Gateway",
        "👥 Audiences",
        "📊 Live Delivery"
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(
                color = DarkSurface,
                tonalElevation = 6.dp,
                border = BorderStroke(1.dp, GlassBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("byod_sms_back_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = CyberCyan
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Bulk SMS & BYOD SaaS",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        color = TextPrimary
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = ElectricEmerald.copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, ElectricEmerald.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = "100% DND BYPASS",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = ElectricEmerald,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 8.sp
                                        ),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Wallet: ₦${String.format(Locale.US, "%,.2f", walletBalance)} • Credits: $batchCredits SMS",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isByodSubscribed) ElectricEmerald.copy(alpha = 0.2f) else DarkSurfaceElevated,
                            border = BorderStroke(1.dp, if (isByodSubscribed) ElectricEmerald else GlassBorder),
                            modifier = Modifier.clickable { showSaaSPlanDialog = true }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (isByodSubscribed) "PRO SAAS" else "GET SAAS",
                                    color = if (isByodSubscribed) ElectricEmerald else CyberCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Tab Row
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = DarkSurface,
                        contentColor = CyberCyan,
                        divider = { HorizontalDivider(color = GlassBorder, thickness = 1.dp) }
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = {
                                    Text(
                                        text = title,
                                        fontSize = 11.sp,
                                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selectedTab == index) CyberCyan else TextMuted
                                    )
                                },
                                modifier = Modifier.testTag("sms_tab_$index")
                            )
                        }
                    }
                }
            }
        },
        containerColor = DarkObsidian
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> BulkSmsBlasterTab(
                    viewModel = viewModel,
                    recipientsText = blastRecipientsText,
                    onRecipientsChange = { blastRecipientsText = it },
                    messageText = blastMessageText,
                    onMessageChange = { blastMessageText = it },
                    useByod = useByodRoute,
                    onUseByodChange = { useByodRoute = it },
                    isByodSubscribed = isByodSubscribed,
                    batchCredits = batchCredits,
                    retailPricePerSms = retailPrice,
                    connectedDevices = connectedDevices,
                    contactGroups = contactGroups,
                    isSending = isSendingBlast,
                    onSendBlast = { recipients, msg, byodMode ->
                        isSendingBlast = true
                        viewModel.sendBulkSmsBlast(
                            recipients = recipients,
                            message = msg,
                            senderId = senderId,
                            useByodGateway = byodMode
                        ) { success, resultMsg, _ ->
                            isSendingBlast = false
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = if (success) "✓ $resultMsg" else "❌ $resultMsg"
                                )
                            }
                            if (success) {
                                blastMessageText = ""
                            }
                        }
                    },
                    onOpenSaaSModal = { showSaaSPlanDialog = true },
                    onOpenBatchModal = { showBatchCreditsDialog = true }
                )
                1 -> ByodSaaSDashboardTab(
                    isSubscribed = isByodSubscribed,
                    expiryTime = byodExpiry,
                    batchCredits = batchCredits,
                    connectedDevices = connectedDevices,
                    onConnectDeviceClick = { showConnectDeviceDialog = true },
                    onRemoveDevice = { viewModel.removeByodDevice(it) },
                    onSubscribeMonthly = { showSaaSPlanDialog = true },
                    onBuyBatchCredits = { showBatchCreditsDialog = true }
                )
                2 -> ContactGroupsTab(
                    groups = contactGroups,
                    onCreateGroupClick = { showCreateGroupDialog = true },
                    onDeleteGroup = { viewModel.deleteContactGroup(it) },
                    onBlastGroup = { group ->
                        blastRecipientsText = group.recipients.joinToString(", ")
                        selectedTab = 0
                    }
                )
                3 -> DeliveryAnalyticsTab(
                    stats = deliveryStats,
                    logs = deliveryLogs
                )
            }
        }
    }

    // Connect BYOD Android Device Modal
    if (showConnectDeviceDialog) {
        ConnectByodDeviceModal(
            onDismiss = { showConnectDeviceDialog = false },
            onConnect = { name, phone, operator, token ->
                viewModel.addByodDevice(name, phone, operator, token)
                showConnectDeviceDialog = false
                scope.launch {
                    snackbarHostState.showSnackbar("✓ Device $name paired to BYOD Gateway!")
                }
            }
        )
    }

    // Create Contact Group Modal
    if (showCreateGroupDialog) {
        CreateContactGroupModal(
            onDismiss = { showCreateGroupDialog = false },
            onCreate = { name, desc, phones ->
                viewModel.createContactGroup(name, desc, phones)
                showCreateGroupDialog = false
                scope.launch {
                    snackbarHostState.showSnackbar("✓ Group '$name' created with ${phones.size} contacts!")
                }
            }
        )
    }

    // Subscribe to BYOD SaaS Monthly Dialog (₦5,000/month)
    if (showSaaSPlanDialog) {
        ByodSaaSPlanModal(
            walletBalance = walletBalance,
            isCurrentlySubscribed = isByodSubscribed,
            expiryTime = byodExpiry,
            onDismiss = { showSaaSPlanDialog = false },
            onConfirmPurchase = {
                viewModel.purchaseByodMonthlySaaS { success, msg ->
                    showSaaSPlanDialog = false
                    scope.launch {
                        snackbarHostState.showSnackbar(if (success) "✓ $msg" else "❌ $msg")
                    }
                }
            }
        )
    }

    // Batch SMS Credits Modal
    if (showBatchCreditsDialog) {
        BatchCreditsModal(
            walletBalance = walletBalance,
            batchPacks = viewModel.byodBatchPacks,
            onDismiss = { showBatchCreditsDialog = false },
            onSelectPack = { pack ->
                selectedBatchPackForAuth = pack
            }
        )
    }

    // MANDATORY TRANSACTION AUTHORIZATION PAYWALL (BIOMETRIC / PIN)
    selectedBatchPackForAuth?.let { pack ->
        TransactionAuthorizationDialog(
            serviceTitle = "SMS Credits: ${pack.title}",
            recipient = "${pack.smsUnits} SMS Units",
            amountNaira = pack.priceNaira,
            viewModel = viewModel,
            onAuthorized = {
                val packToBuy = pack
                selectedBatchPackForAuth = null
                showBatchCreditsDialog = false
                viewModel.purchaseByodBatchPack(packToBuy) { success, msg ->
                    scope.launch {
                        snackbarHostState.showSnackbar(if (success) "✓ $msg" else "❌ $msg")
                    }
                }
            },
            onDismiss = {
                selectedBatchPackForAuth = null
            }
        )
    }
}

// -----------------------------------------------------------------------------------------
// TAB 1: BULK SMS BLASTER (100% DND BYPASS)
// -----------------------------------------------------------------------------------------

@Composable
fun BulkSmsBlasterTab(
    viewModel: VpnViewModel,
    recipientsText: String,
    onRecipientsChange: (String) -> Unit,
    messageText: String,
    onMessageChange: (String) -> Unit,
    useByod: Boolean,
    onUseByodChange: (Boolean) -> Unit,
    isByodSubscribed: Boolean,
    batchCredits: Int,
    retailPricePerSms: Double,
    connectedDevices: List<VpnViewModel.ByodDeviceItem>,
    contactGroups: List<VpnViewModel.ContactGroupItem>,
    isSending: Boolean,
    onSendBlast: (List<String>, String, Boolean) -> Unit,
    onOpenSaaSModal: () -> Unit,
    onOpenBatchModal: () -> Unit
) {
    val recipientList = remember(recipientsText) {
        recipientsText.split(",", "\n", ";")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }
    val (charCount, _, pageCount) = HttpSmsService.calculateSmsMetrics(messageText)
    val recipientCount = recipientList.size.coerceAtLeast(1)
    val totalCost = if (useByod && isByodSubscribed) 0.0 else recipientCount * pageCount * retailPricePerSms

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 100% Delivery DND-Bypass Value Proposition Banner
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                border = BorderStroke(1.dp, ElectricEmerald.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(ElectricEmerald.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = ElectricEmerald,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "100% Delivery Rate • DND Bypass",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        color = TextPrimary
                                    )
                                )
                                Text(
                                    text = "Zero corporate filter drops • Direct SIM routes",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = ElectricEmerald,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = DarkSurface,
                            border = BorderStroke(1.dp, GlassBorder)
                        ) {
                            Text(
                                text = "₦${String.format(Locale.US, "%.2f", retailPricePerSms)}/SMS",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Corporate aggregator routes get silently dropped by Nigerian telco Do-Not-Disturb (DND) filters. FlowTest utilizes real SIM card routes so every single alert, reminder, or OTP lands directly in your customer's inbox.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    )
                }
            }
        }

        // Route Selection (Platform SIM vs BYOD Gateway)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, GlassBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "SELECT DISPATCH ROUTE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextMuted,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Route 1: Platform SIM (₦7.50 / SMS)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (!useByod) CyberCyan.copy(alpha = 0.15f) else DarkSurfaceElevated,
                            border = BorderStroke(
                                1.5.dp,
                                if (!useByod) CyberCyan else GlassBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onUseByodChange(false) }
                                .testTag("route_platform_sim")
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = !useByod,
                                        onClick = { onUseByodChange(false) },
                                        colors = RadioButtonDefaults.colors(selectedColor = CyberCyan),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Platform SIM",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = TextPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "₦${String.format(Locale.US, "%.2f", retailPricePerSms)} / SMS • 100% DND Bypass",
                                    color = CyberCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "No device setup required",
                                    color = TextMuted,
                                    fontSize = 9.sp
                                )
                            }
                        }

                        // Route 2: BYOD Phone Gateway
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (useByod) ElectricEmerald.copy(alpha = 0.15f) else DarkSurfaceElevated,
                            border = BorderStroke(
                                1.5.dp,
                                if (useByod) ElectricEmerald else GlassBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onUseByodChange(true) }
                                .testTag("route_byod_gateway")
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = useByod,
                                        onClick = { onUseByodChange(true) },
                                        colors = RadioButtonDefaults.colors(selectedColor = ElectricEmerald),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "BYOD Device",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = TextPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isByodSubscribed) "₦0.00 / SMS (Pro SaaS)" else "$batchCredits Credits Left",
                                    color = ElectricEmerald,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${connectedDevices.size} phone(s) active",
                                    color = TextMuted,
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }

                    if (useByod && !isByodSubscribed && batchCredits == 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⚠️ Subscribe for ₦5,000/mo or buy batch credits to use BYOD.",
                                color = GlowingAmber,
                                fontSize = 10.sp,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = onOpenSaaSModal) {
                                Text("Get SaaS", color = CyberCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Quick Audience Chips
        if (contactGroups.isNotEmpty()) {
            item {
                Column {
                    Text(
                        text = "QUICK LOAD AUDIENCE GROUP",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextMuted,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(contactGroups) { group ->
                            FilterChip(
                                selected = false,
                                onClick = {
                                    val current = recipientsText.split(",", "\n").map { it.trim() }.filter { it.isNotBlank() }
                                    val combined = (current + group.recipients).distinct()
                                    onRecipientsChange(combined.joinToString(", "))
                                },
                                label = {
                                    Text(
                                        text = "${group.name} (${group.recipients.size})",
                                        fontSize = 11.sp
                                    )
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(14.dp))
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = DarkSurfaceElevated,
                                    labelColor = TextPrimary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = false,
                                    borderColor = GlassBorder
                                )
                            )
                        }
                    }
                }
            }
        }

        // Recipients Field
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RECIPIENTS (${recipientList.size} numbers)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextMuted,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    )
                    if (recipientsText.isNotBlank()) {
                        Text(
                            text = "Clear",
                            color = WarningRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { onRecipientsChange("") }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = recipientsText,
                    onValueChange = onRecipientsChange,
                    placeholder = {
                        Text(
                            "Enter phone numbers separated by comma or new line...\n(e.g., 08137545370, 08031234567, 09087654321)",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(95.dp)
                        .testTag("blast_recipients_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurfaceElevated,
                        unfocusedContainerColor = DarkSurfaceElevated,
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }
        }

        // Message Content Field with Character / Page Counter
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SMS MESSAGE CONTENT",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextMuted,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    )
                    Text(
                        text = "$charCount chars • $pageCount page(s)",
                        color = if (charCount > 160) GlowingAmber else CyberCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = messageText,
                    onValueChange = onMessageChange,
                    placeholder = {
                        Text(
                            "Type your blast message here. Direct SIM routing guarantees 100% delivery bypassing Do-Not-Disturb...",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .testTag("blast_message_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurfaceElevated,
                        unfocusedContainerColor = DarkSurfaceElevated,
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }
        }

        // Summary & Dispatch Action
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, GlassBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Recipients:", color = TextSecondary, fontSize = 12.sp)
                        Text("${recipientList.size} numbers", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Pages per SMS:", color = TextSecondary, fontSize = 12.sp)
                        Text("$pageCount page(s)", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Active Route:", color = TextSecondary, fontSize = 12.sp)
                        Text(
                            text = if (useByod) "BYOD Phone Gateway" else "Platform Direct SIM (100% Delivery)",
                            color = if (useByod) ElectricEmerald else CyberCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = GlassBorder)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Est. Total Charge:", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(
                            text = if (useByod && isByodSubscribed) "₦0.00 (Pro SaaS)" else "₦${String.format(Locale.US, "%,.2f", totalCost)}",
                            color = ElectricEmerald,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            onSendBlast(recipientList, messageText, useByod)
                        },
                        enabled = !isSending && recipientList.isNotEmpty() && messageText.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("send_bulk_blast_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (useByod) ElectricEmerald else CyberCyan,
                            disabledContainerColor = DarkSurfaceElevated
                        )
                    ) {
                        if (isSending) {
                            FlowButtonLoadingLine(color = DarkObsidian, width = 40.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("DISPATCHING BLAST...", color = DarkObsidian, fontWeight = FontWeight.Black)
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = DarkObsidian, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SEND 100% DELIVERY BLAST",
                                color = DarkObsidian,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// TAB 2: BYOD SAAS GATEWAY DASHBOARD (Flat ₦5,000/mo or Batch Token Packs)
// -----------------------------------------------------------------------------------------

@Composable
fun ByodSaaSDashboardTab(
    isSubscribed: Boolean,
    expiryTime: Long,
    batchCredits: Int,
    connectedDevices: List<VpnViewModel.ByodDeviceItem>,
    onConnectDeviceClick: () -> Unit,
    onRemoveDevice: (String) -> Unit,
    onSubscribeMonthly: () -> Unit,
    onBuyBatchCredits: () -> Unit
) {
    val expiryFormatted = remember(expiryTime) {
        if (expiryTime <= System.currentTimeMillis()) "Inactive"
        else SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US).format(Date(expiryTime))
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // SaaS Model Overview Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "BYOD SMS SaaS Platform",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = "Bring Your Own Device • Unlimited SIM Bundles",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = CyberCyan,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSubscribed) ElectricEmerald.copy(alpha = 0.2f) else DarkSurface,
                            border = BorderStroke(1.dp, if (isSubscribed) ElectricEmerald else GlassBorder)
                        ) {
                            Text(
                                text = if (isSubscribed) "ACTIVE SAAS" else "INACTIVE",
                                color = if (isSubscribed) ElectricEmerald else GlowingAmber,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Instead of relying on third-party aggregators, connect your own Android phones equipped with local SIM cards (MTN, Airtel, Glo) and send unlimited SMS using your carrier packs. We provide the enterprise SaaS dashboard, automated webhooks, audience management, and delivery analytics.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onSubscribeMonthly,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (isSubscribed) "Extend (₦5,000/mo)" else "Subscribe (₦5,000/mo)",
                                color = DarkObsidian,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp
                            )
                        }

                        OutlinedButton(
                            onClick = onBuyBatchCredits,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricEmerald),
                            border = BorderStroke(1.dp, ElectricEmerald),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Batch Credits ($batchCredits)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // Subscription Status Telemetry
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, GlassBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("SaaS Plan Status", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (isSubscribed) "Pro Monthly Active" else "Pay-As-You-Go",
                            color = if (isSubscribed) ElectricEmerald else TextPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                        Text(
                            text = if (isSubscribed) "Renewal: $expiryFormatted" else "Subscribe for unlimited gateway routing",
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("Batch Credits", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "$batchCredits SMS",
                            color = CyberCyan,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "${connectedDevices.size} Gateways Linked",
                            color = ElectricEmerald,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        // Connected BYOD Devices Header & Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LINKED ANDROID GATEWAYS (${connectedDevices.size})",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextMuted,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                )
                Button(
                    onClick = onConnectDeviceClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("pair_android_device_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = DarkObsidian, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Pair Device", color = DarkObsidian, fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
            }
        }

        // List of Connected BYOD Android Gateways
        if (connectedDevices.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, GlassBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = TextMuted, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No Android Gateway Connected", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(
                            "Tap 'Pair Device' above to link your Android phone & SIM card to route SMS blasts for free.",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(connectedDevices) { dev ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    border = BorderStroke(1.dp, GlassBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(ElectricEmerald.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = ElectricEmerald, modifier = Modifier.size(20.dp))
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = dev.deviceName,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = ElectricEmerald.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = dev.status,
                                        color = ElectricEmerald,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "${dev.phoneNumber} • ${dev.simOperator}",
                                color = CyberCyan,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "Sent Today: ${dev.smsSentToday} / ${dev.dailyLimit} • Battery: ${dev.batteryPercent}%",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }

                        IconButton(
                            onClick = { onRemoveDevice(dev.id) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Unlink", tint = WarningRed, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// TAB 3: AUDIENCE & CONTACT GROUPS
// -----------------------------------------------------------------------------------------

@Composable
fun ContactGroupsTab(
    groups: List<VpnViewModel.ContactGroupItem>,
    onCreateGroupClick: () -> Unit,
    onDeleteGroup: (String) -> Unit,
    onBlastGroup: (VpnViewModel.ContactGroupItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "AUDIENCE GROUPS",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "Segment customers, members, and team alerts",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                }

                Button(
                    onClick = onCreateGroupClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("create_audience_group_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = DarkObsidian, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Group", color = DarkObsidian, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (groups.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, GlassBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Group, contentDescription = null, tint = TextMuted, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No Contact Groups Yet", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(
                            "Create target groups like 'VIP Store Clients' or 'Sunday Church Alerts' to blast in 1 tap.",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(groups) { group ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    border = BorderStroke(1.dp, GlassBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = group.name,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                )
                                if (group.description.isNotBlank()) {
                                    Text(
                                        text = group.description,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }

                            IconButton(
                                onClick = { onDeleteGroup(group.id) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = WarningRed, modifier = Modifier.size(18.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val previewText = group.recipients.take(5).joinToString(", ") + if (group.recipients.size > 5) "..." else ""
                        Text(
                            text = "Contacts (${group.recipients.size}): $previewText",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = CyberCyan,
                                fontSize = 10.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = { onBlastGroup(group) },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = DarkObsidian, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Blast This Group (${group.recipients.size} Recipients)",
                                color = DarkObsidian,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// TAB 4: DELIVERY ANALYTICS & RECEIPTS
// -----------------------------------------------------------------------------------------

@Composable
fun DeliveryAnalyticsTab(
    stats: HttpSmsDeliveryStats,
    logs: List<HttpSmsDeliveryLog>
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                border = BorderStroke(1.dp, ElectricEmerald.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "LIVE DELIVERY TELEMETRY",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextMuted,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("DISPATCHED", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text("${stats.totalDispatched}", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 18.sp)
                        }
                        Column {
                            Text("DELIVERED (DND BYPASS)", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text("${stats.totalDelivered}", color = ElectricEmerald, fontWeight = FontWeight.Black, fontSize = 18.sp)
                        }
                        Column {
                            Text("DELIVERY RATE", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text("100%", color = CyberCyan, fontWeight = FontWeight.Black, fontSize = 18.sp)
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "RECENT DELIVERY RECEIPTS",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = TextMuted,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            )
        }

        if (logs.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, GlassBorder)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("No logs yet. Send a blast to view live delivery receipts.", color = TextMuted, fontSize = 12.sp)
                    }
                }
            }
        } else {
            items(logs) { log ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, GlassBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = log.recipient,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = TextPrimary
                            )
                            val timeStr = SimpleDateFormat("HH:mm:ss dd-MMM", Locale.US).format(Date(log.timestamp))
                            Text(
                                text = "$timeStr • From ${log.senderId}",
                                color = TextMuted,
                                fontSize = 9.sp
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (log.status.contains("DELIVERED")) ElectricEmerald.copy(alpha = 0.2f) else WarningRed.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = log.status,
                                color = if (log.status.contains("DELIVERED")) ElectricEmerald else WarningRed,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// MODALS & DIALOGS
// -----------------------------------------------------------------------------------------

@Composable
fun ConnectByodDeviceModal(
    onDismiss: () -> Unit,
    onConnect: (String, String, String, String) -> Unit
) {
    var deviceName by remember { mutableStateOf("Android SIM Gateway") }
    var phoneNumber by remember { mutableStateOf("+234") }
    var operator by remember { mutableStateOf("MTN NG") }
    var gatewayToken by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = ElectricEmerald)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Pair Android Device & SIM", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Connect your Android phone running the httpSMS agent. Blasts will send directly from your phone's SIM card using your carrier's local SMS pack.",
                    color = TextSecondary,
                    fontSize = 11.sp
                )

                OutlinedTextField(
                    value = deviceName,
                    onValueChange = { deviceName = it },
                    label = { Text("Device Name (e.g. Redmi Note 12)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("SIM Phone Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = operator,
                    onValueChange = { operator = it },
                    label = { Text("Network Operator (e.g. MTN, Airtel)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Webhook endpoint configured to https://api.flowtest2026.com/api/webhook/httpsms",
                    color = CyberCyan,
                    fontSize = 9.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConnect(deviceName, phoneNumber, operator, gatewayToken) },
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
            ) {
                Text("PAIR GATEWAY", color = DarkObsidian, fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextMuted) }
        },
        containerColor = DarkSurfaceElevated
    )
}

@Composable
fun CreateContactGroupModal(
    onDismiss: () -> Unit,
    onCreate: (String, String, List<String>) -> Unit
) {
    var groupName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.GroupAdd, contentDescription = null, tint = CyberCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Audience Group", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("Group Name (e.g. VIP Customers)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phoneInput,
                    onValueChange = { phoneInput = it },
                    label = { Text("Phone Numbers (comma or newline)") },
                    modifier = Modifier.fillMaxWidth().height(100.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val phones = phoneInput.split(",", "\n", ";").map { it.trim() }.filter { it.isNotBlank() }
                    onCreate(groupName, description, phones)
                },
                enabled = groupName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
            ) {
                Text("SAVE GROUP", color = DarkObsidian, fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextMuted) }
        },
        containerColor = DarkSurfaceElevated
    )
}

@Composable
fun ByodSaaSPlanModal(
    walletBalance: Double,
    isCurrentlySubscribed: Boolean,
    expiryTime: Long,
    onDismiss: () -> Unit,
    onConfirmPurchase: () -> Unit
) {
    val canAfford = walletBalance >= 5000.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, contentDescription = null, tint = ElectricEmerald)
                Spacer(modifier = Modifier.width(8.dp))
                Text("BYOD SaaS Monthly Gateway", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ElectricEmerald.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, ElectricEmerald.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("FLAT SUBSCRIPTION", color = ElectricEmerald, fontSize = 9.sp, fontWeight = FontWeight.Black)
                        Text("₦5,000 / Month", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        Text("Unlimited SIM Dispatches • Zero Markup Per SMS", color = ElectricEmerald, fontSize = 11.sp)
                    }
                }

                Text(
                    text = "✓ Connect unlimited Android phones as live SMS gateways.\n✓ Use your own unlimited telco bundles (MTN, Airtel, Glo).\n✓ Bypasses 100% of Do-Not-Disturb (DND) filters.\n✓ Enterprise webhooks, contact management & analytics included.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                )

                HorizontalDivider(color = GlassBorder)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Wallet Balance:", color = TextMuted, fontSize = 12.sp)
                    Text("₦${String.format(Locale.US, "%,.2f", walletBalance)}", color = if (canAfford) ElectricEmerald else WarningRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                if (!canAfford) {
                    Text(
                        text = "Insufficient wallet balance. Please fund your wallet to activate.",
                        color = WarningRed,
                        fontSize = 11.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmPurchase,
                enabled = canAfford,
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
            ) {
                Text(
                    text = if (isCurrentlySubscribed) "EXTEND (₦5,000)" else "ACTIVATE (₦5,000)",
                    color = DarkObsidian,
                    fontWeight = FontWeight.Black
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextMuted) }
        },
        containerColor = DarkSurfaceElevated
    )
}

@Composable
fun BatchCreditsModal(
    walletBalance: Double,
    batchPacks: List<VpnViewModel.ByodSmsBatchPack>,
    onDismiss: () -> Unit,
    onSelectPack: (VpnViewModel.ByodSmsBatchPack) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ConfirmationNumber, contentDescription = null, tint = CyberCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text("SMS Batch Token Packs", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Prefer not to pay a monthly subscription? Purchase batch credits that never expire, similar to VPN hours!",
                    color = TextSecondary,
                    fontSize = 11.sp
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    batchPacks.forEach { pack ->
                        val canAfford = walletBalance >= pack.priceNaira
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = DarkSurface,
                            border = BorderStroke(1.dp, if (pack.popular) CyberCyan else GlassBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = canAfford) { onSelectPack(pack) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(pack.title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        if (pack.popular) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = CyberCyan.copy(alpha = 0.2f)
                                            ) {
                                                Text(
                                                    "POPULAR",
                                                    color = CyberCyan,
                                                    fontSize = 7.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(pack.description, color = TextSecondary, fontSize = 9.sp)
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        "₦${String.format(Locale.US, "%,.0f", pack.priceNaira)}",
                                        color = if (canAfford) ElectricEmerald else WarningRed,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp
                                    )
                                    Text("${pack.smsUnits} SMS", color = TextMuted, fontSize = 9.sp)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = TextMuted) }
        },
        containerColor = DarkSurfaceElevated
    )
}
