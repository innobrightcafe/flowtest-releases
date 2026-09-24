package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Velocity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.api.RecentDataPurchase
import com.example.data.ui.viewmodel.VpnViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.launch

/**
 * AirtimeSheet
 * Built in the Cyber Luxury Dark Navy Palette matching DataBundlesSheet:
 * - Top App Bar with back button, centered title, and live wallet balance chip
 * - Authentic 4-network selector (MTN, Airtel, Glo, 9mobile) using TelcoLogo
 * - Mode Tabs ("Buy For Self" | "Buy For Others") with solid CyberCyan underline indicator
 * - Denomination Preset Pills (₦100, ₦200, ₦500, ₦1,000, ₦2,000, ₦5,000) + Custom Amount
 * - Quick Repurchase ("Buy Again - 1-Tap Repeat") carousel
 * - Prominent Admin Discount & Cashback banner (⚡ 5% OFF)
 * - Bottom Summary Card & Action CTA with Recipient Safety Verification Drawer
 * - Transaction Success Receipt with VPN bonus time and loyalty points
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AirtimeSheet(
    viewModel: VpnViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    val walletBalance by viewModel.userWalletBalance.collectAsStateWithLifecycle()
    val userVirtualAccount by viewModel.userVirtualAccount.collectAsStateWithLifecycle()
    val savedRecipients by viewModel.savedRecipients.collectAsStateWithLifecycle()
    val recentPurchases by viewModel.recentDataPurchases.collectAsStateWithLifecycle()
    val configuredCashbackPct by viewModel.cashbackRateAirtimePercent.collectAsStateWithLifecycle()

    val myPhoneNumber = remember(userVirtualAccount) {
        userVirtualAccount.phoneNumber.trim()
    }

    var isBuyForSelf by remember { mutableStateOf(true) }
    var selectedNetwork by remember { mutableStateOf(viewModel.getUserOption("last_airtime_network", "MTN")) }
    var amountInput by remember { mutableStateOf(viewModel.getUserOption("last_airtime_amount", "500")) }
    var recipientPhoneInput by remember { mutableStateOf(myPhoneNumber) }

    var showPhoneVerificationDialog by remember { mutableStateOf(false) }
    var showFixPhoneDialog by remember { mutableStateOf(false) }
    var fixPhoneInput by remember { mutableStateOf("") }

    var isProcessing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var purchaseReceipt by remember { mutableStateOf<PurchaseSuccessReceipt?>(null) }
    var showSafetyVerificationDialog by remember { mutableStateOf(false) }
    var showTransactionPaywall by remember { mutableStateOf(false) }
    var showSavedRecipientsManager by remember { mutableStateOf(false) }
    var saveRecipientToQuickPay by remember { mutableStateOf(true) }

    val serviceDiscounts by viewModel.serviceDiscounts.collectAsStateWithLifecycle()
    val discountPct = serviceDiscounts["airtime"] ?: 5.0
    val faceValue = amountInput.toDoubleOrNull() ?: 500.0
    val discountedPayable = faceValue * (1.0 - (discountPct / 100.0))
    val totalSavings = faceValue - discountedPayable
    val effectiveCashbackRate = if (configuredCashbackPct > 0.0) configuredCashbackPct else 2.0
    val calculatedCashback = kotlin.math.round((discountedPayable * effectiveCashbackRate) / 100.0).toInt().coerceAtLeast(1)
    val rateLabel = if (effectiveCashbackRate % 1.0 == 0.0) "${effectiveCashbackRate.toInt()}%" else "$effectiveCashbackRate%"

    val effectiveTarget = if (isBuyForSelf) myPhoneNumber else recipientPhoneInput.trim()

    val launchContactPicker = rememberContactPicker { _, phone ->
        if (phone.isNotBlank()) {
            val clean = phone.replace("+234", "0").replace(" ", "").replace("-", "")
            recipientPhoneInput = clean
            isBuyForSelf = false
            val detected = com.example.data.util.NigerianCarrierDetector.detectCarrier(clean)
            if (detected != null) {
                selectedNetwork = detected
            }
        }
    }

    LaunchedEffect(myPhoneNumber, isBuyForSelf) {
        if (isBuyForSelf && myPhoneNumber.isNotBlank()) {
            val detected = com.example.data.util.NigerianCarrierDetector.detectCarrier(myPhoneNumber)
            if (detected != null) {
                selectedNetwork = detected
            }
        }
    }

    val presetAmounts = listOf("100", "200", "500", "1000", "2000", "5000")

    val blockParentScroll = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset = Offset(0f, available.y)

            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity
            ): Velocity = Velocity(0f, available.y)
        }
    }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    LaunchedEffect(purchaseReceipt) {
        if (purchaseReceipt != null) {
            sheetState.expand()
        }
    }

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = { if (!isProcessing && !showTransactionPaywall && !showSafetyVerificationDialog) onDismiss() },
        containerColor = DarkObsidian,
        scrimColor = Color.Black.copy(alpha = 0.75f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(44.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.35f))
            )
        }
    ) {
        AnimatedContent(
            targetState = purchaseReceipt != null,
            transitionSpec = {
                if (targetState) {
                    (slideInHorizontally { width -> width } + fadeIn())
                        .togetherWith(slideOutHorizontally { width -> -width } + fadeOut())
                } else {
                    (slideInHorizontally { width -> -width } + fadeIn())
                        .togetherWith(slideOutHorizontally { width -> width } + fadeOut())
                }
            },
            label = "AirtimeReceiptTransition"
        ) { hasReceipt ->
            if (hasReceipt && purchaseReceipt != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 48.dp)
            ) {
                // Header with title and close button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Transaction Receipt",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    IconButton(
                        onClick = {
                            purchaseReceipt = null
                            onDismiss()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Receipt",
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                TransactionSuccessReceiptView(
                    receipt = purchaseReceipt!!,
                    onReturnHome = {
                        purchaseReceipt = null
                        onDismiss()
                    },
                    onMakeAnotherPurchase = {
                        purchaseReceipt = null
                        errorMessage = null
                        recipientPhoneInput = myPhoneNumber
                        isBuyForSelf = true
                    },
                    onSaveRecipient = { name, type, identifier, provider ->
                        viewModel.saveRecipient(
                            name = name,
                            recipientType = type,
                            identifier = identifier,
                            institutionOrProvider = provider
                        )
                    }
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkObsidian)
                    .nestedScroll(blockParentScroll)
            ) {
                // 1. TOP APP BAR
                Surface(
                    color = DarkSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            detectVerticalDragGestures { change, _ ->
                                change.consume()
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = { if (!isProcessing) onDismiss() },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextPrimary
                            )
                        }

                        Text(
                            text = "Airtime Recharge",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                color = TextPrimary,
                                fontSize = 19.sp
                            )
                        )

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = DarkSurfaceElevated,
                            border = BorderStroke(1.dp, DarkCardBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(ElectricEmerald)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "₦${String.format("%,.0f", walletBalance)}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = CyberCyan,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }
                    }
                }

                // SCROLLABLE CONTENT BODY
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .nestedScroll(blockParentScroll)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // 2. NETWORK SELECTION BAR
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        listOf("MTN", "Airtel", "Glo", "9mobile").forEach { net ->
                            val isSelected = selectedNetwork.equals(net, ignoreCase = true)
                            val netColor = getNetworkColor(net)
                            val netContentColor = getNetworkContentColor(net)

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) netColor.copy(alpha = 0.18f) else DarkSurface,
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) netColor else DarkCardBorder
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(72.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { selectedNetwork = net }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(vertical = 6.dp, horizontal = 2.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    TelcoLogo(network = net, size = 24.dp)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = net,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                            color = if (isSelected) netColor else TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    // Cashback tag in network color
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (isSelected) netColor else netColor.copy(alpha = 0.25f)
                                    ) {
                                        Text(
                                            text = if (isSelected) "+₦$calculatedCashback" else "$rateLabel back",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = if (isSelected) netContentColor else netColor,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 8.5.sp
                                            ),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3. TOP MODE TABS: "Buy For Self" | "Buy For Others"
                    Surface(
                        color = DarkSurface,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, DarkCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            // TAB 1: Buy For Self
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        isBuyForSelf = true
                                        recipientPhoneInput = myPhoneNumber
                                    }
                                    .padding(vertical = 11.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Buy For Self",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isBuyForSelf) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isBuyForSelf) CyberCyan else TextMuted,
                                        fontSize = 13.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.55f)
                                        .height(3.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(if (isBuyForSelf) CyberCyan else Color.Transparent)
                                )
                            }

                            // TAB 2: Buy For Others
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        isBuyForSelf = false
                                        if (recipientPhoneInput == myPhoneNumber) {
                                            recipientPhoneInput = ""
                                        }
                                    }
                                    .padding(vertical = 11.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Buy For Others",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (!isBuyForSelf) FontWeight.Bold else FontWeight.Medium,
                                        color = if (!isBuyForSelf) CyberCyan else TextMuted,
                                        fontSize = 13.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.55f)
                                        .height(3.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(if (!isBuyForSelf) CyberCyan else Color.Transparent)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // RECIPIENT DISPLAY / INPUT
                    if (isBuyForSelf) {
                        if (myPhoneNumber.isNotBlank()) {
                            val myCarrier = remember(myPhoneNumber) { com.example.data.util.NigerianCarrierDetector.detectCarrier(myPhoneNumber) }
                            val isSelfMismatch = myCarrier != null && com.example.data.util.NigerianCarrierDetector.isMismatch(selectedNetwork, myPhoneNumber)

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = DarkSurface,
                                border = BorderStroke(1.dp, if (isSelfMismatch) WarningRed else CyberCyan.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.PhoneAndroid,
                                            contentDescription = "My Line",
                                            tint = if (isSelfMismatch) WarningRed else CyberCyan,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "Recipient: $myPhoneNumber",
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary,
                                                    fontSize = 13.sp
                                                )
                                            )
                                            Text(
                                                text = if (isSelfMismatch) "Carrier: $myCarrier (Mismatch with $selectedNetwork)" else "My Line • ${myCarrier ?: selectedNetwork} • Verified",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = if (isSelfMismatch) WarningRed else ElectricEmerald,
                                                    fontSize = 11.sp
                                                )
                                            )
                                        }
                                    }
                                    if (isSelfMismatch && myCarrier != null) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = WarningRed,
                                            modifier = Modifier.clickable { selectedNetwork = myCarrier }
                                        ) {
                                            Text(
                                                text = "FIX TO $myCarrier",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 10.sp
                                                ),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                            )
                                        }
                                    } else {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = ElectricEmerald.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "Active Line",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = ElectricEmerald,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp
                                                ),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = DarkSurface,
                                border = BorderStroke(1.dp, GlowingAmber.copy(alpha = 0.6f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        fixPhoneInput = ""
                                        showFixPhoneDialog = true
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(
                                            imageVector = Icons.Default.WarningAmber,
                                            contentDescription = null,
                                            tint = GlowingAmber,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "No Phone Number Linked",
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary,
                                                    fontSize = 13.sp
                                                )
                                            )
                                            Text(
                                                text = "Tap to enter your phone number to recharge",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = GlowingAmber,
                                                    fontSize = 11.sp
                                                )
                                            )
                                        }
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = GlowingAmber.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "FIX NUMBER",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = GlowingAmber,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 10.sp
                                            ),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = recipientPhoneInput,
                                onValueChange = { input ->
                                    recipientPhoneInput = input
                                    val clean = input.replace("+234", "0").replace(" ", "").replace("-", "")
                                    val detected = com.example.data.util.NigerianCarrierDetector.detectCarrier(clean)
                                    if (detected != null && !detected.equals(selectedNetwork, ignoreCase = true)) {
                                        selectedNetwork = detected
                                    }
                                },
                                placeholder = { Text("Enter recipient 11-digit phone number", color = TextMuted, fontSize = 13.sp) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.PhoneIphone,
                                        contentDescription = "Phone",
                                        tint = CyberCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                trailingIcon = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                val clip = clipboardManager.getText()?.text ?: ""
                                                if (clip.isNotBlank()) {
                                                    val clean = clip.replace("+234", "0").replace(" ", "").replace("-", "")
                                                    recipientPhoneInput = clean
                                                    val detected = com.example.data.util.NigerianCarrierDetector.detectCarrier(clean)
                                                    if (detected != null) {
                                                        selectedNetwork = detected
                                                    }
                                                }
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentPaste,
                                                contentDescription = "Paste",
                                                tint = CyberCyan,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { launchContactPicker() },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Contacts,
                                                contentDescription = "Contacts",
                                                tint = ElectricEmerald,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyberCyan,
                                    unfocusedBorderColor = DarkCardBorder,
                                    focusedContainerColor = DarkSurface,
                                    unfocusedContainerColor = DarkSurface,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // CARRIER AUTO-DETECTION & MISMATCH ALERT
                            val cleanInput = recipientPhoneInput.replace("+234", "0").replace(" ", "").replace("-", "")
                            val detectedCarrier = remember(cleanInput) { com.example.data.util.NigerianCarrierDetector.detectCarrier(cleanInput) }
                            val isCarrierMismatch = detectedCarrier != null && com.example.data.util.NigerianCarrierDetector.isMismatch(selectedNetwork, cleanInput)

                            if (detectedCarrier != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                AnimatedVisibility(visible = isCarrierMismatch) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFF3B1515),
                                        border = BorderStroke(1.dp, WarningRed),
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                modifier = Modifier.weight(1f),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("⚠️", fontSize = 16.sp)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(
                                                        text = "CARRIER MISMATCH DETECTED",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            color = WarningRed,
                                                            fontWeight = FontWeight.Black,
                                                            fontSize = 11.sp
                                                        )
                                                    )
                                                    Text(
                                                        text = "Line belongs to $detectedCarrier, but $selectedNetwork is selected.",
                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                            color = Color(0xFFFFCDD2),
                                                            fontSize = 10.5.sp
                                                        )
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Button(
                                                onClick = { selectedNetwork = detectedCarrier },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = WarningRed,
                                                    contentColor = Color.White
                                                ),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Text("Switch to $detectedCarrier", fontWeight = FontWeight.Black, fontSize = 10.5.sp)
                                            }
                                        }
                                    }
                                }

                                AnimatedVisibility(visible = !isCarrierMismatch && cleanInput.length >= 4) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = DarkSurfaceElevated,
                                        border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.3f)),
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TelcoLogo(network = detectedCarrier, size = 16.dp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Auto-detected: $detectedCarrier Line",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = CyberCyan,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp
                                                )
                                            )
                                            Spacer(modifier = Modifier.weight(1f))
                                            Text(
                                                text = "✓ Matched",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = ElectricEmerald,
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 10.5.sp
                                                )
                                            )
                                        }
                                    }
                                }
                            }

                            // Quick Beneficiary Chips & Contacts Access
                            val airtimeRecipients = savedRecipients.filter { it.recipientType.equals("airtime", ignoreCase = true) || it.recipientType.equals("data", ignoreCase = true) }
                            if (airtimeRecipients.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Saved Contacts (${airtimeRecipients.size})",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = GlowingAmber,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    )
                                    Text(
                                        text = "Tap to Auto-Fill",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = TextMuted,
                                            fontSize = 10.sp
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(airtimeRecipients.take(8)) { rec ->
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = DarkSurfaceElevated,
                                            border = BorderStroke(0.5.dp, CyberCyan.copy(alpha = 0.4f)),
                                            modifier = Modifier.clickable {
                                                recipientPhoneInput = rec.identifier
                                                val detected = com.example.data.util.NigerianCarrierDetector.detectCarrier(rec.identifier)
                                                if (detected != null) {
                                                    selectedNetwork = detected
                                                } else if (rec.institutionOrProvider.isNotBlank()) {
                                                    selectedNetwork = rec.institutionOrProvider
                                                }
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                TelcoLogo(network = rec.institutionOrProvider.ifEmpty { "MTN" }, size = 12.dp)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "${rec.name} (${rec.identifier})",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = TextSecondary,
                                                        fontSize = 10.sp
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 4. ADMIN DISCOUNT & CASHBACK BANNER
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1E1738),
                        border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(CyberCyan.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("⚡", fontSize = 16.sp)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (discountPct > 0.0) "OFFICIAL ADMIN PROMO: ${if (discountPct % 1.0 == 0.0) discountPct.toInt().toString() else String.format("%.1f", discountPct)}% OFF" else "OFFICIAL WHOLESALE RATE: 100% VALUE",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, color = CyberCyan)
                                    )
                                    Text(
                                        text = if (discountPct > 0.0) "Save ₦${String.format("%,.2f", totalSavings)} + Earn ₦$calculatedCashback Cashback Bonus!" else "Instant VTU Delivery + Earn ₦$calculatedCashback Cashback Bonus!",
                                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 10.sp)
                                    )
                                }
                            }

                            Surface(
                                color = if (discountPct > 0.0) CyberCyan else DarkSurfaceElevated,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (discountPct > 0.0) "${if (discountPct % 1.0 == 0.0) discountPct.toInt().toString() else String.format("%.1f", discountPct)}% OFF" else "BEST VALUE",
                                    style = MaterialTheme.typography.labelSmall.copy(color = if (discountPct > 0.0) DarkObsidian else TextPrimary, fontWeight = FontWeight.Black, fontSize = 11.sp),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            val netColor = getNetworkColor(selectedNetwork)
                            val netContentColor = getNetworkContentColor(selectedNetwork)
                            Surface(
                                color = netColor,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "+₦$calculatedCashback BACK",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = netContentColor,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // 5. BUY AGAIN (FAST REPEAT) - PLACED AT THE TOP BEFORE SELECT RECHARGE
                    val buyAgainList = if (!isBuyForSelf) {
                        // For Others tab: Hold buy again of numbers user just bought for
                        recentPurchases.filter { rp ->
                            rp.recipientPhone.isNotBlank() &&
                            (myPhoneNumber.isBlank() || rp.recipientPhone != myPhoneNumber)
                        }
                    } else {
                        // For Self tab: Recent top-ups for self or this network
                        recentPurchases.filter { rp ->
                            (myPhoneNumber.isNotBlank() && rp.recipientPhone == myPhoneNumber) ||
                            rp.network.equals(selectedNetwork, ignoreCase = true)
                        }
                    }

                    if (buyAgainList.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (!isBuyForSelf) "⚡ Buy Again for Others (Recent Recipients)" else "⚡ Buy Again (Recent Top-Ups)",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = CyberCyan,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            )
                            Text(
                                text = "Fast Repeat",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(buyAgainList.take(6)) { rp ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = DarkSurfaceElevated,
                                    border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.35f)),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            selectedNetwork = rp.network
                                            if (!isBuyForSelf) {
                                                recipientPhoneInput = rp.recipientPhone
                                            }
                                            amountInput = rp.amountNaira.toInt().toString()
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TelcoLogo(network = rp.network, size = 24.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "₦${String.format("%,d", rp.amountNaira.toInt())} Airtime",
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary
                                                )
                                            )
                                            if (!isBuyForSelf && rp.recipientPhone.isNotBlank()) {
                                                Text(
                                                    text = rp.recipientPhone,
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        color = CyberCyan,
                                                        fontSize = 11.sp,
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                )
                                            } else {
                                                Text(
                                                    text = rp.network.uppercase(),
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        color = TextMuted,
                                                        fontSize = 10.sp
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 6. DENOMINATION PRESET PILLS
                    Text(
                        text = "Select Recharge Amount",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val netColor = getNetworkColor(selectedNetwork)
                        val netContentColor = getNetworkContentColor(selectedNetwork)
                        presetAmounts.take(3).forEach { denom ->
                            val isSelected = amountInput == denom
                            val amtVal = denom.toDouble()
                            val payVal = amtVal * (1.0 - (discountPct / 100.0))
                            val denomCashback = kotlin.math.round((payVal * effectiveCashbackRate) / 100.0).toInt().coerceAtLeast(1)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) netColor.copy(alpha = 0.18f) else DarkSurface,
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) netColor else DarkCardBorder
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { amountInput = denom }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "₦$denom",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Black,
                                            color = if (isSelected) netColor else TextPrimary,
                                            fontSize = 15.sp
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Pay ₦${payVal.toInt()}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = ElectricEmerald,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 10.sp
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (isSelected) netColor else netColor.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "+₦$denomCashback",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = if (isSelected) netContentColor else netColor,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 8.5.sp
                                            ),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val netColor = getNetworkColor(selectedNetwork)
                        val netContentColor = getNetworkContentColor(selectedNetwork)
                        presetAmounts.drop(3).take(3).forEach { denom ->
                            val isSelected = amountInput == denom
                            val amtVal = denom.toDouble()
                            val payVal = amtVal * (1.0 - (discountPct / 100.0))
                            val denomCashback = kotlin.math.round((payVal * effectiveCashbackRate) / 100.0).toInt().coerceAtLeast(1)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) netColor.copy(alpha = 0.18f) else DarkSurface,
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) netColor else DarkCardBorder
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { amountInput = denom }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "₦$denom",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Black,
                                            color = if (isSelected) netColor else TextPrimary,
                                            fontSize = 15.sp
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Pay ₦${payVal.toInt()}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = ElectricEmerald,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 10.sp
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (isSelected) netColor else netColor.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "+₦$denomCashback",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = if (isSelected) netContentColor else netColor,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 8.5.sp
                                            ),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // CUSTOM AMOUNT FIELD
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Custom Amount (₦50 - ₦50,000)", color = TextMuted) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedContainerColor = DarkSurface,
                            unfocusedContainerColor = DarkSurface,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // SAVE NUMBER TO QUICK PAY TOGGLE
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = DarkSurfaceElevated,
                        border = BorderStroke(1.dp, if (saveRecipientToQuickPay) CyberCyan.copy(alpha = 0.5f) else DarkCardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { saveRecipientToQuickPay = !saveRecipientToQuickPay }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = if (saveRecipientToQuickPay) Icons.Default.BookmarkAdded else Icons.Default.BookmarkBorder,
                                    contentDescription = "Quick Pay",
                                    tint = if (saveRecipientToQuickPay) CyberCyan else TextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Save details for Quick Pay",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    )
                                    Text(
                                        text = "Save this contact & network for 1-tap top-ups",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = TextMuted,
                                            fontSize = 10.sp
                                        )
                                    )
                                }
                            }
                            Switch(
                                checked = saveRecipientToQuickPay,
                                onCheckedChange = { saveRecipientToQuickPay = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = DarkObsidian,
                                    checkedTrackColor = CyberCyan,
                                    uncheckedThumbColor = TextMuted,
                                    uncheckedTrackColor = DarkSurface
                                )
                            )
                        }
                    }

                    // ERROR MESSAGE BANNER
                    errorMessage?.let { msg ->
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF2E1616),
                            border = BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.8f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.ErrorOutline, contentDescription = "Error", tint = Color(0xFFFF5252), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = msg, style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFFFCDD2), fontSize = 12.sp))
                                }
                                IconButton(onClick = { errorMessage = null }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = TextMuted, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                }

                // 7. BOTTOM PAYMENT BAR & ACTION CTA
                Surface(
                    color = DarkSurface,
                    border = BorderStroke(1.dp, DarkCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        // Summary breakdown
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Your Wallet Balance", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 11.sp))
                                Text("₦${String.format("%,.2f", walletBalance)}", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text("Discounted Payable", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 11.sp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("₦${String.format("%,.2f", discountedPayable)}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, color = CyberCyan))
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Save ₦${String.format("%,.2f", totalSavings)}", style = MaterialTheme.typography.labelSmall.copy(color = ElectricEmerald, fontSize = 10.sp, fontWeight = FontWeight.Bold))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    val netColor = getNetworkColor(selectedNetwork)
                                    val netContentColor = getNetworkContentColor(selectedNetwork)
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = netColor
                                    ) {
                                        Text(
                                            text = "+₦$calculatedCashback Back",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = netContentColor,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 9.sp
                                            ),
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Review & Pay Button
                        Button(
                            onClick = {
                                if (!viewModel.isUserPhoneVerified()) {
                                    showPhoneVerificationDialog = true
                                    return@Button
                                }
                                if (isBuyForSelf && myPhoneNumber.isBlank()) {
                                    showFixPhoneDialog = true
                                    errorMessage = "Please link your phone number before purchasing airtime."
                                    return@Button
                                }
                                if (effectiveTarget.isBlank() || effectiveTarget.length < 10) {
                                    errorMessage = "Please enter a valid 11-digit phone number."
                                    return@Button
                                }
                                if (com.example.data.util.NetworkUtils.isLowNetwork(context)) {
                                    Toast.makeText(context, "⚡ Low network detected: Optimizing transfer...", Toast.LENGTH_SHORT).show()
                                }
                                showSafetyVerificationDialog = true
                            },
                            enabled = !isProcessing,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = getNetworkColor(selectedNetwork),
                                contentColor = getNetworkContentColor(selectedNetwork)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            if (isProcessing) {
                                FlowButtonLoadingLine(color = getNetworkContentColor(selectedNetwork), width = 48.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("DISPATCHING AIRTIME...", fontWeight = FontWeight.Black, color = getNetworkContentColor(selectedNetwork))
                            } else {
                                Text(
                                    "BUY NOW • ₦${String.format("%,.2f", discountedPayable)}",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Black,
                                        color = getNetworkContentColor(selectedNetwork),
                                        letterSpacing = 0.5.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // SAFETY RECIPIENT VERIFICATION MODAL DIALOG
    if (showSafetyVerificationDialog) {
        AlertDialog(
            onDismissRequest = { showSafetyVerificationDialog = false },
            containerColor = DarkSurfaceElevated,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(GlowingAmber.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⚠️", fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "CONFIRM AIRTIME DESTINATION",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, color = TextPrimary)
                        )
                        Text(
                            text = "VTU dispatches cannot be reversed once credited",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
                        )
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = DarkSurface,
                        border = BorderStroke(1.5.dp, CyberCyan.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("TARGET LINE", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontWeight = FontWeight.Bold))
                                Text(if (effectiveTarget == myPhoneNumber) "My Phone Number" else "Recipient Line", style = MaterialTheme.typography.labelSmall.copy(color = CyberCyan, fontWeight = FontWeight.Bold))
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TelcoLogo(network = selectedNetwork, size = 32.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = effectiveTarget,
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.Monospace,
                                            color = CyberCyan
                                        )
                                    )
                                    Text(
                                        text = "${selectedNetwork.uppercase()} • Instant VTU Top-Up",
                                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 12.sp)
                                    )
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = DarkCardBorder)

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Face Value", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
                                Text("₦${String.format("%,.2f", faceValue)}", style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, fontWeight = FontWeight.Bold))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Discounted Payable", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
                                Text("₦${String.format("%,.2f", discountedPayable)}", style = MaterialTheme.typography.bodyMedium.copy(color = ElectricEmerald, fontWeight = FontWeight.Black))
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            val netColor = getNetworkColor(selectedNetwork)
                            val netContentColor = getNetworkContentColor(selectedNetwork)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Instant Cashback", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = netColor
                                ) {
                                    Text(
                                        text = "+₦$calculatedCashback Cashback",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = netContentColor,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 11.sp
                                        ),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }

                    val detectedNetInDialog = com.example.data.util.NigerianCarrierDetector.detectCarrier(effectiveTarget)
                    val isMismatchInDialog = detectedNetInDialog != null && com.example.data.util.NigerianCarrierDetector.isMismatch(selectedNetwork, effectiveTarget)

                    if (isMismatchInDialog && detectedNetInDialog != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF3B1515),
                            border = BorderStroke(1.2.dp, WarningRed),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("⚠️", fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "NETWORK MISMATCH DETECTED",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = WarningRed,
                                            fontWeight = FontWeight.Black
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$effectiveTarget belongs to $detectedNetInDialog, but $selectedNetwork is selected. Upstream carrier will decline mismatched requests.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFFFCDD2), fontSize = 11.sp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { selectedNetwork = detectedNetInDialog },
                                    colors = ButtonDefaults.buttonColors(containerColor = WarningRed, contentColor = Color.White),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().height(36.dp)
                                ) {
                                    Text("SWITCH TO ${detectedNetInDialog.uppercase()} & PROCEED SAFELY", fontWeight = FontWeight.Black, fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // SAVE BENEFICIARY / QUICK PAY TOGGLE
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = DarkSurface,
                        border = BorderStroke(1.dp, if (saveRecipientToQuickPay) CyberCyan.copy(alpha = 0.5f) else DarkCardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { saveRecipientToQuickPay = !saveRecipientToQuickPay }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = if (saveRecipientToQuickPay) Icons.Default.BookmarkAdded else Icons.Default.BookmarkBorder,
                                    contentDescription = "Quick Pay",
                                    tint = if (saveRecipientToQuickPay) CyberCyan else TextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Save details for Quick Pay",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.5.sp
                                        )
                                    )
                                    Text(
                                        text = "1-tap recharges next time",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = TextMuted,
                                            fontSize = 9.5.sp
                                        )
                                    )
                                }
                            }
                            Switch(
                                checked = saveRecipientToQuickPay,
                                onCheckedChange = { saveRecipientToQuickPay = it },
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
            },
            confirmButton = {
                Button(
                    onClick = {
                        val detectedNet = com.example.data.util.NigerianCarrierDetector.detectCarrier(effectiveTarget)
                        if (detectedNet != null && com.example.data.util.NigerianCarrierDetector.isMismatch(selectedNetwork, effectiveTarget)) {
                            selectedNetwork = detectedNet
                            Toast.makeText(context, "Network auto-corrected to $detectedNet", Toast.LENGTH_SHORT).show()
                        }
                        if (com.example.data.util.NetworkUtils.isLowNetwork(context)) {
                            Toast.makeText(context, "⚡ Low network detected: Dispatching...", Toast.LENGTH_SHORT).show()
                        }
                        showSafetyVerificationDialog = false
                        showTransactionPaywall = true
                    },
                    modifier = Modifier.height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = getNetworkColor(selectedNetwork),
                        contentColor = getNetworkContentColor(selectedNetwork)
                    )
                ) {
                    Text(
                        "✅ Authorize & Send",
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        color = getNetworkContentColor(selectedNetwork)
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showSafetyVerificationDialog = false },
                    modifier = Modifier.height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    border = BorderStroke(1.dp, DarkCardBorder)
                ) {
                    Text("✏️ Edit", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        )
    }

    // MANDATORY TRANSACTION AUTHORIZATION PAYWALL (BIOMETRIC / PIN)
    if (showTransactionPaywall) {
        TransactionAuthorizationDialog(
            serviceTitle = "${selectedNetwork.uppercase()} Airtime Top-Up",
            recipient = effectiveTarget,
            amountNaira = discountedPayable,
            viewModel = viewModel,
            onAuthorized = {
                showTransactionPaywall = false
                isProcessing = true
                viewModel.purchasePairgateAirtime(selectedNetwork, faceValue.toInt(), effectiveTarget) { ok, msg, ref ->
                    isProcessing = false
                    if (ok) {
                        viewModel.saveUserOption("last_airtime_network", selectedNetwork)
                        viewModel.saveUserOption("last_airtime_amount", faceValue.toInt().toString())
                        if (saveRecipientToQuickPay && effectiveTarget.isNotBlank()) {
                            viewModel.saveRecipient(
                                name = if (isBuyForSelf) "My Phone ($selectedNetwork)" else "$selectedNetwork Airtime - $effectiveTarget",
                                recipientType = "airtime",
                                identifier = effectiveTarget,
                                institutionOrProvider = selectedNetwork,
                                isFavorite = true
                            )
                        }
                        viewModel.recordRecentDataPurchase(
                            com.example.data.api.RecentDataPurchase(
                                network = selectedNetwork,
                                planId = "airtime_${faceValue.toInt()}",
                                planName = "₦${faceValue.toInt()} Airtime",
                                recipientPhone = effectiveTarget,
                                amountNaira = faceValue,
                                validity = "Instant",
                                category = "Airtime",
                                timestamp = System.currentTimeMillis()
                            )
                        )
                        purchaseReceipt = PurchaseSuccessReceipt(
                            serviceTitle = "Airtime Top-Up ($selectedNetwork)",
                            providerOrType = selectedNetwork,
                            recipient = effectiveTarget,
                            amountPaid = discountedPayable,
                            reference = ref ?: "PG-AIR-${(100000..999999).random()}",
                            newBalance = viewModel.userWalletBalance.value,
                            message = msg,
                            bonusVpnTime = "+1 Hour Unlimited VPN Time",
                            bonusPoints = (faceValue / 50).toInt().coerceAtLeast(10)
                        )
                    } else {
                        errorMessage = msg
                    }
                }
            },
            onDismiss = {
                showTransactionPaywall = false
            }
        )
    }

    // FIX / LINK PHONE NUMBER DIALOG
    if (showFixPhoneDialog) {
        var localError by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { showFixPhoneDialog = false },
            containerColor = DarkSurfaceElevated,
            title = {
                Text(
                    text = "Link Your Phone Number",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "To recharge your own line, please enter your primary 11-digit phone number.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = fixPhoneInput,
                        onValueChange = {
                            if (it.length <= 11) fixPhoneInput = it.filter { char -> char.isDigit() }
                            localError = null
                        },
                        placeholder = { Text("080XXXXXXXX", color = TextMuted) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        isError = localError != null,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    if (localError != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(localError!!, color = WarningRed, fontSize = 11.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = fixPhoneInput.trim()
                        if (trimmed.length != 11 || !trimmed.startsWith("0")) {
                            localError = "Please enter a valid 11-digit phone number starting with 0"
                            return@Button
                        }
                        showFixPhoneDialog = false
                        showPhoneVerificationDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian)
                ) {
                    Text("Verify with Google", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFixPhoneDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }

    if (showPhoneVerificationDialog) {
        PhoneVerificationDialog(
            viewModel = viewModel,
            initialPhone = if (isBuyForSelf) myPhoneNumber else recipientPhoneInput,
            title = "Free Verification",
            description = "Free verification to verify recipient number.",
            onDismiss = { showPhoneVerificationDialog = false },
            onSuccess = { verified ->
                showPhoneVerificationDialog = false
                if (isBuyForSelf) {
                    recipientPhoneInput = verified
                }
            }
        )
    }
}
}
