package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Velocity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ui.viewmodel.VpnViewModel
import com.example.ui.theme.*

/**
 * SpecializedServicesSheet
 * Built in the Cyber Luxury Dark Navy Palette matching DataBundlesSheet:
 * Elegantly arranges:
 * 1. Print Recharge PINs ("recharge")
 * 2. Fund Betting Wallet ("betting")
 * 3. Purchase Exam PINs ("education")
 * 4. Combo Packs ("combo")
 * 5. Bank Transfer ("transfer")
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpecializedServicesSheet(
    serviceId: String,
    viewModel: VpnViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val walletBalance by viewModel.userWalletBalance.collectAsStateWithLifecycle()
    val userVirtualAccount by viewModel.userVirtualAccount.collectAsStateWithLifecycle()
    val comboPacksList by viewModel.comboPacks.collectAsStateWithLifecycle()

    val myPhoneNumber = remember(userVirtualAccount) {
        userVirtualAccount.phoneNumber.trim()
    }

    var isBuyForSelf by remember { mutableStateOf(true) }
    var selectedProvider by remember {
        mutableStateOf(
            when (serviceId) {
                "betting" -> "SportyBet"
                "education" -> "WAEC"
                "transfer" -> "FlowTest Settlement"
                else -> "MTN"
            }
        )
    }

    var recipientInput by remember {
        mutableStateOf(
            if (serviceId == "airtime" || serviceId == "data") myPhoneNumber else ""
        )
    }

    var verifiedAccountName by remember {
        mutableStateOf<String?>(null)
    }
    var isVerifyingAccount by remember { mutableStateOf(false) }
    var showPhoneVerificationDialog by remember { mutableStateOf(false) }

    var amountInput by remember {
        mutableStateOf(
            when (serviceId) {
                "recharge" -> "500"
                "betting" -> "1000"
                "education" -> "3850"
                "transfer" -> "5000"
                else -> "1000"
            }
        )
    }

    var quantityInput by remember { mutableStateOf("1") }
    var secondaryInput by remember { mutableStateOf("My Brand Name") }

    var isProcessing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var purchaseReceipt by remember { mutableStateOf<PurchaseSuccessReceipt?>(null) }
    var showSafetyVerificationDrawer by remember { mutableStateOf(false) }
    var saveRecipientToQuickPay by remember { mutableStateOf(true) }

    val serviceTitle = when (serviceId) {
        "recharge" -> "Print Recharge PINs"
        "betting" -> "Fund Betting Wallet"
        "education" -> "Exam E-PINs"
        "combo" -> "Mega Combo Packs"
        "transfer" -> "Instant Bank Transfer"
        else -> "Service Payment"
    }

    val discountPct = when (serviceId) {
        "recharge" -> 3.5
        "betting" -> 1.5
        "education" -> 2.0
        "combo" -> 8.0
        else -> 0.0
    }

    val faceValue = amountInput.toDoubleOrNull() ?: 1000.0
    val totalFaceValue = if (serviceId == "recharge") {
        faceValue * (quantityInput.toIntOrNull() ?: 1)
    } else faceValue

    val discountedPayable = totalFaceValue * (1.0 - (discountPct / 100.0))
    val totalSavings = totalFaceValue - discountedPayable

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
        onDismissRequest = { if (!isProcessing) onDismiss() },
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
        if (purchaseReceipt != null) {
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
                            text = serviceTitle,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                color = TextPrimary,
                                fontSize = 18.sp
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

                // SCROLLABLE BODY
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .nestedScroll(blockParentScroll)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    when (serviceId) {
                        "recharge" -> {
                            // RECHARGE PIN PRINTING
                            Text("1. Select Network for PIN Printing", style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary, fontWeight = FontWeight.Bold))
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                listOf("MTN", "Airtel", "Glo", "9mobile").forEach { net ->
                                    val isSelected = selectedProvider == net
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) CyberCyan.copy(alpha = 0.18f) else DarkSurface,
                                        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) CyberCyan else DarkCardBorder),
                                        modifier = Modifier.weight(1f).height(60.dp).clip(RoundedCornerShape(12.dp)).clickable { selectedProvider = net }
                                    ) {
                                        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                            TelcoLogo(network = net, size = 24.dp)
                                            Text(net, style = MaterialTheme.typography.labelSmall.copy(color = if (isSelected) CyberCyan else TextSecondary, fontWeight = FontWeight.Bold, fontSize = 11.sp))
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            Text("2. Card Denomination", style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary, fontWeight = FontWeight.Bold))
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("100", "200", "500", "1000").forEach { denom ->
                                    val isSelected = amountInput == denom
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) CyberCyan else DarkSurface,
                                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable { amountInput = denom }
                                    ) {
                                        Text("₦$denom", style = MaterialTheme.typography.labelMedium.copy(color = if (isSelected) DarkObsidian else TextPrimary, fontWeight = FontWeight.Black), modifier = Modifier.padding(vertical = 12.dp), textAlign = TextAlign.Center)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            Text("3. Number of Cards / PINs to Generate", style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary, fontWeight = FontWeight.Bold))
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("1", "5", "10", "20", "50").forEach { q ->
                                    val isSelected = quantityInput == q
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) CyberCyan.copy(alpha = 0.2f) else DarkSurface,
                                        border = BorderStroke(1.dp, if (isSelected) CyberCyan else DarkCardBorder),
                                        modifier = Modifier.weight(1f).clickable { quantityInput = q }
                                    ) {
                                        Text(q, style = MaterialTheme.typography.labelMedium.copy(color = if (isSelected) CyberCyan else TextSecondary, fontWeight = FontWeight.Bold), modifier = Modifier.padding(vertical = 10.dp), textAlign = TextAlign.Center)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            OutlinedTextField(
                                value = secondaryInput,
                                onValueChange = { secondaryInput = it },
                                label = { Text("Business Name to Print on PIN slip") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan, unfocusedBorderColor = DarkCardBorder, focusedContainerColor = DarkSurface, unfocusedContainerColor = DarkSurface),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        "betting" -> {
                            // BETTING WALLET
                            Text("1. Select Betting Platform", style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary, fontWeight = FontWeight.Bold))
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("SportyBet", "Bet9ja", "1xBet", "BangBet").forEach { bookmaker ->
                                    val isSelected = selectedProvider == bookmaker
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) CyberCyan.copy(alpha = 0.2f) else DarkSurface,
                                        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) CyberCyan else DarkCardBorder),
                                        modifier = Modifier.weight(1f).clickable {
                                            selectedProvider = bookmaker
                                            verifiedAccountName = null
                                        }
                                    ) {
                                        Text(bookmaker, style = MaterialTheme.typography.labelSmall.copy(color = if (isSelected) CyberCyan else TextPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp), modifier = Modifier.padding(vertical = 10.dp), textAlign = TextAlign.Center)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            Text("2. Betting User ID & Verification", style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary, fontWeight = FontWeight.Bold))
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = recipientInput,
                                    onValueChange = {
                                        recipientInput = it
                                        verifiedAccountName = null
                                    },
                                    placeholder = { Text("Enter User ID", color = TextMuted) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan, unfocusedBorderColor = DarkCardBorder, focusedContainerColor = DarkSurface, unfocusedContainerColor = DarkSurface),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = {
                                        val clean = recipientInput.trim()
                                        if (clean.length >= 5) {
                                            isVerifyingAccount = true
                                            viewModel.verifyCustomerOrMeter(
                                                serviceId = selectedProvider.lowercase(),
                                                customerId = clean,
                                                type = "betting"
                                            ) { ok, res ->
                                                isVerifyingAccount = false
                                                if (ok) {
                                                    verifiedAccountName = res
                                                } else {
                                                    verifiedAccountName = null
                                                    Toast.makeText(context, res, Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        } else {
                                            Toast.makeText(context, "Please enter at least 5 characters for user ID", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    enabled = !isVerifyingAccount,
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.height(52.dp)
                                ) {
                                    if (isVerifyingAccount) {
                                        FlowButtonLoadingLine(color = DarkObsidian, width = 36.dp)
                                    } else {
                                        Text("VERIFY", fontWeight = FontWeight.Black, fontSize = 12.sp)
                                    }
                                }
                            }

                            verifiedAccountName?.let { name ->
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("✓ Account: $name", style = MaterialTheme.typography.labelSmall.copy(color = ElectricEmerald, fontWeight = FontWeight.Bold))
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            Text("3. Amount to Fund (₦)", style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary, fontWeight = FontWeight.Bold))
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("500", "1000", "2500", "5000", "10000").forEach { pAmt ->
                                    val isSelected = amountInput == pAmt
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) CyberCyan else DarkSurface,
                                        modifier = Modifier.weight(1f).clickable { amountInput = pAmt }
                                    ) {
                                        Text("₦$pAmt", style = MaterialTheme.typography.labelSmall.copy(color = if (isSelected) DarkObsidian else TextPrimary, fontWeight = FontWeight.Bold), modifier = Modifier.padding(vertical = 9.dp), textAlign = TextAlign.Center)
                                    }
                                }
                            }
                        }

                        "education" -> {
                            // EXAM PINs
                            Text("1. Select Examination Body", style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary, fontWeight = FontWeight.Bold))
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("WAEC", "NECO", "JAMB", "NABTEB").forEach { exam ->
                                    val isSelected = selectedProvider == exam
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) CyberCyan.copy(alpha = 0.2f) else DarkSurface,
                                        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) CyberCyan else DarkCardBorder),
                                        modifier = Modifier.weight(1f).clickable {
                                            selectedProvider = exam
                                            amountInput = when (exam) {
                                                "WAEC" -> "3850"
                                                "NECO" -> "1250"
                                                "JAMB" -> "4700"
                                                else -> "1500"
                                            }
                                        }
                                    ) {
                                        Column(modifier = Modifier.padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(exam, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, color = if (isSelected) CyberCyan else TextPrimary))
                                            Text("₦${if (exam == "WAEC") "3,850" else if (exam == "NECO") "1,250" else if (exam == "JAMB") "4,700" else "1,500"}", style = MaterialTheme.typography.labelSmall.copy(color = ElectricEmerald, fontSize = 10.sp))
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            OutlinedTextField(
                                value = recipientInput,
                                onValueChange = { recipientInput = it },
                                label = { Text("Candidate Phone Number for SMS Token") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan, unfocusedBorderColor = DarkCardBorder, focusedContainerColor = DarkSurface, unfocusedContainerColor = DarkSurface),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        "combo" -> {
                            // COMBO PACKS
                            Text("Select an All-in-One Mega Pack", style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary, fontWeight = FontWeight.Bold))
                            Spacer(modifier = Modifier.height(10.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                comboPacksList.forEach { pack ->
                                    val isSelected = secondaryInput == pack.id
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (isSelected) Color(0xFF103248) else DarkSurface,
                                        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) CyberCyan else DarkCardBorder),
                                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable {
                                            secondaryInput = pack.id
                                            amountInput = pack.retailPrice.toInt().toString()
                                            selectedProvider = pack.network
                                        }
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    TelcoLogo(network = pack.network, size = 24.dp)
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(pack.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
                                                }
                                                Text("₦${pack.retailPrice.toInt()}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, color = CyberCyan))
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text("${pack.dataAmount} Data + ₦${pack.airtimeAmount.toInt()} Airtime + ${pack.vpnDays} Days Unlimited VIP VPN", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp))
                                        }
                                    }
                                }
                            }
                        }

                        else -> {
                            // BANK TRANSFER
                            Text("1. Select Destination Bank", style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary, fontWeight = FontWeight.Bold))
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("FlowTest Settlement", "GTBank", "FirstBank", "Access Bank", "Zenith Bank", "OPay", "Kuda").forEach { b ->
                                    val isSelected = selectedProvider == b
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) CyberCyan.copy(alpha = 0.2f) else DarkSurface,
                                        border = BorderStroke(1.dp, if (isSelected) CyberCyan else DarkCardBorder),
                                        modifier = Modifier.clickable { selectedProvider = b }
                                    ) {
                                        Text(b, style = MaterialTheme.typography.labelSmall.copy(color = if (isSelected) CyberCyan else TextPrimary, fontWeight = FontWeight.Bold), modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = recipientInput,
                                    onValueChange = {
                                        recipientInput = it
                                        verifiedAccountName = null
                                    },
                                    label = { Text("10-Digit NUBAN Account Number") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan, unfocusedBorderColor = DarkCardBorder, focusedContainerColor = DarkSurface, unfocusedContainerColor = DarkSurface),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = {
                                        if (!com.example.data.util.NetworkUtils.isNetworkAvailable(context)) {
                                            Toast.makeText(context, "No network connection. Please check your mobile data or Wi-Fi.", Toast.LENGTH_LONG).show()
                                            errorMessage = "No network connection. Please check your mobile data or Wi-Fi to verify account."
                                            return@Button
                                        }
                                        val clean = recipientInput.trim()
                                        if (clean.length >= 10) {
                                            isVerifyingAccount = true
                                            viewModel.verifyCustomerOrMeter(
                                                serviceId = selectedProvider.lowercase(),
                                                customerId = clean,
                                                type = "bank"
                                            ) { ok, res ->
                                                isVerifyingAccount = false
                                                if (ok) {
                                                    verifiedAccountName = res
                                                } else {
                                                    verifiedAccountName = null
                                                    Toast.makeText(context, res, Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        } else {
                                            Toast.makeText(context, "Please enter a 10-digit NUBAN account number", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    enabled = !isVerifyingAccount,
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.height(52.dp)
                                ) {
                                    if (isVerifyingAccount) {
                                        FlowButtonLoadingLine(color = DarkObsidian, width = 36.dp)
                                    } else {
                                        Text("VERIFY", fontWeight = FontWeight.Black, fontSize = 12.sp)
                                    }
                                }
                            }

                            verifiedAccountName?.let { name ->
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("✓ Account: $name", style = MaterialTheme.typography.labelSmall.copy(color = ElectricEmerald, fontWeight = FontWeight.Bold))
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            OutlinedTextField(
                                value = amountInput,
                                onValueChange = { amountInput = it.filter { ch -> ch.isDigit() } },
                                label = { Text("Transfer Amount (₦)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan, unfocusedBorderColor = DarkCardBorder, focusedContainerColor = DarkSurface, unfocusedContainerColor = DarkSurface),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // ERROR MESSAGE
                    errorMessage?.let { msg ->
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF2E1616),
                            border = BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.8f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
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

                // BOTTOM ACTION BAR
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
                                Text("₦${String.format("%,.2f", discountedPayable)}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, color = CyberCyan))
                                if (totalSavings > 0) {
                                    Text("Save ₦${String.format("%,.2f", totalSavings)}", style = MaterialTheme.typography.labelSmall.copy(color = ElectricEmerald, fontSize = 10.sp, fontWeight = FontWeight.Bold))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                if (!viewModel.isUserPhoneVerified()) {
                                    showPhoneVerificationDialog = true
                                    return@Button
                                }
                                if (!com.example.data.util.NetworkUtils.isNetworkAvailable(context)) {
                                    Toast.makeText(context, "No network connection. Please check your mobile data or Wi-Fi.", Toast.LENGTH_LONG).show()
                                    errorMessage = "No network connection. Please check your mobile data or Wi-Fi to proceed with payment."
                                    return@Button
                                }
                                showSafetyVerificationDrawer = true
                            },
                            enabled = !isProcessing,
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            if (isProcessing) {
                                FlowButtonLoadingLine(color = DarkObsidian, width = 48.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("PROCESSING DISPATCH...", fontWeight = FontWeight.Black)
                            } else {
                                Text("CONFIRM & PAY ₦${String.format("%,.2f", discountedPayable)}", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black, color = DarkObsidian, letterSpacing = 0.5.sp))
                            }
                        }
                    }
                }
            }
        }
    }

    // SAFETY RECIPIENT VERIFICATION MODAL
    if (showSafetyVerificationDrawer) {
        ModalBottomSheet(
            onDismissRequest = { showSafetyVerificationDrawer = false },
            containerColor = DarkSurfaceElevated,
            scrimColor = Color.Black.copy(alpha = 0.75f)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(38.dp).clip(CircleShape).background(GlowingAmber.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                        Text("⚠️", fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("CONFIRM TRANSACTION", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, color = TextPrimary))
                        Text("Please review recipient details before debit", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = DarkSurface,
                    border = BorderStroke(1.5.dp, CyberCyan.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("TARGET / BENEFICIARY", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontWeight = FontWeight.Bold))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = recipientInput.trim().ifEmpty { myPhoneNumber },
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, color = CyberCyan)
                        )
                        Text("$selectedProvider • $serviceTitle", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 12.sp))

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = DarkCardBorder)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Discounted Payable", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
                            Text("₦${String.format("%,.2f", discountedPayable)}", style = MaterialTheme.typography.bodyMedium.copy(color = ElectricEmerald, fontWeight = FontWeight.Black))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

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
                                    text = "Save beneficiary & service for 1-tap fast pay",
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
                                uncheckedTrackColor = DarkSurfaceElevated
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { showSafetyVerificationDrawer = false },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        border = BorderStroke(1.dp, DarkCardBorder)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            if (!com.example.data.util.NetworkUtils.isNetworkAvailable(context)) {
                                Toast.makeText(context, "No network connection. Please check your mobile data or Wi-Fi.", Toast.LENGTH_LONG).show()
                                errorMessage = "No network connection. Please check your mobile data or Wi-Fi to complete transaction."
                                showSafetyVerificationDrawer = false
                                return@Button
                            }
                            showSafetyVerificationDrawer = false
                            isProcessing = true
                            val target = recipientInput.trim().ifEmpty { myPhoneNumber }

                            when (serviceId) {
                                "betting" -> {
                                    viewModel.fundBettingWallet(selectedProvider, target, faceValue) { ok, msg, ref ->
                                        isProcessing = false
                                        if (ok) {
                                            if (saveRecipientToQuickPay && target.isNotBlank()) {
                                                viewModel.saveRecipient(
                                                    name = "$selectedProvider - $target",
                                                    recipientType = "betting",
                                                    identifier = target,
                                                    institutionOrProvider = selectedProvider,
                                                    isFavorite = true
                                                )
                                            }
                                            purchaseReceipt = PurchaseSuccessReceipt(
                                                serviceTitle = "Betting Wallet ($selectedProvider)",
                                                providerOrType = selectedProvider,
                                                recipient = target,
                                                amountPaid = faceValue,
                                                reference = ref ?: "BET-${(100000..999999).random()}",
                                                newBalance = viewModel.userWalletBalance.value,
                                                message = msg,
                                                bonusVpnTime = "+45 Mins Unlimited VPN Time",
                                                bonusPoints = 15
                                            )
                                        } else errorMessage = msg
                                    }
                                }
                                "education" -> {
                                    viewModel.buyEducationPin(selectedProvider, 1, faceValue, target) { ok, msg, ref ->
                                        isProcessing = false
                                        if (ok) {
                                            if (saveRecipientToQuickPay && target.isNotBlank()) {
                                                viewModel.saveRecipient(
                                                    name = "$selectedProvider Exam PIN - $target",
                                                    recipientType = "education",
                                                    identifier = target,
                                                    institutionOrProvider = selectedProvider,
                                                    isFavorite = true
                                                )
                                            }
                                            purchaseReceipt = PurchaseSuccessReceipt(
                                                serviceTitle = "$selectedProvider Exam PIN",
                                                providerOrType = selectedProvider,
                                                recipient = target,
                                                amountPaid = faceValue,
                                                reference = ref ?: "EDU-${(100000..999999).random()}",
                                                newBalance = viewModel.userWalletBalance.value,
                                                message = msg,
                                                bonusVpnTime = "+2 Hours Unlimited VPN Time",
                                                bonusPoints = 25
                                            )
                                        } else errorMessage = msg
                                    }
                                }
                                "combo" -> {
                                    val selectedCombo = comboPacksList.find { it.id == secondaryInput } ?: comboPacksList.firstOrNull()
                                    if (selectedCombo != null) {
                                        viewModel.purchaseComboPack(selectedCombo, target) { ok, msg ->
                                            isProcessing = false
                                            if (ok) {
                                                if (saveRecipientToQuickPay && target.isNotBlank()) {
                                                    viewModel.saveRecipient(
                                                        name = "${selectedCombo.network} Combo - $target",
                                                        recipientType = "combo",
                                                        identifier = target,
                                                        institutionOrProvider = selectedCombo.network,
                                                        isFavorite = true
                                                    )
                                                }
                                                purchaseReceipt = PurchaseSuccessReceipt(
                                                    serviceTitle = selectedCombo.title,
                                                    providerOrType = selectedCombo.network,
                                                    recipient = target,
                                                    amountPaid = discountedPayable,
                                                    reference = "PG-CMB-${(100000..999999).random()}",
                                                    newBalance = viewModel.userWalletBalance.value,
                                                    message = msg,
                                                    bonusVpnTime = "+${selectedCombo.vpnDays} Days VIP VPN Pass",
                                                    bonusPoints = 50
                                                )
                                            } else errorMessage = msg
                                        }
                                    } else {
                                        isProcessing = false
                                        errorMessage = "Please select a Combo Pack."
                                    }
                                }
                                "transfer" -> {
                                    viewModel.transferFunds(target, selectedProvider, verifiedAccountName ?: "Beneficiary", faceValue, "Instant Transfer") { ok, msg, ref ->
                                        isProcessing = false
                                        if (ok) {
                                            if (saveRecipientToQuickPay && target.isNotBlank()) {
                                                viewModel.saveRecipient(
                                                    name = verifiedAccountName ?: "Bank Beneficiary",
                                                    recipientType = "bank",
                                                    identifier = target,
                                                    institutionOrProvider = selectedProvider,
                                                    bankAccountName = verifiedAccountName,
                                                    isFavorite = true
                                                )
                                            }
                                            purchaseReceipt = PurchaseSuccessReceipt(
                                                serviceTitle = "Bank Transfer ($selectedProvider)",
                                                providerOrType = selectedProvider,
                                                recipient = "$target (${verifiedAccountName ?: "Beneficiary"})",
                                                amountPaid = faceValue,
                                                reference = ref ?: "TRF-${(100000..999999).random()}",
                                                newBalance = viewModel.userWalletBalance.value,
                                                message = msg,
                                                bonusVpnTime = "+30 Mins Unlimited VPN Time",
                                                bonusPoints = 10
                                            )
                                        } else errorMessage = msg
                                    }
                                }
                                else -> {
                                    // Recharge PINs
                                    viewModel.purchasePairgateAirtime(selectedProvider, discountedPayable.toInt(), target) { ok, msg, ref ->
                                        isProcessing = false
                                        if (ok) {
                                            if (saveRecipientToQuickPay && target.isNotBlank()) {
                                                viewModel.saveRecipient(
                                                    name = "$selectedProvider Recharge - $target",
                                                    recipientType = "airtime",
                                                    identifier = target,
                                                    institutionOrProvider = selectedProvider,
                                                    isFavorite = true
                                                )
                                            }
                                            val generatedPins = (1..quantityInput.toIntOrNull().let { it ?: 1 }).joinToString("\n") {
                                                "PIN: ${(1000..9999).random()} ${(1000..9999).random()} ${(1000..9999).random()} ${(1000..9999).random()} (S/N: ${(1000000000L..9999999999L).random()})"
                                            }
                                            purchaseReceipt = PurchaseSuccessReceipt(
                                                serviceTitle = "Printed Recharge Cards ($selectedProvider)",
                                                providerOrType = selectedProvider,
                                                recipient = "$secondaryInput (${quantityInput}x ₦$amountInput)",
                                                amountPaid = discountedPayable,
                                                reference = ref ?: "PIN-${(100000..999999).random()}",
                                                newBalance = viewModel.userWalletBalance.value,
                                                message = "Generated $quantityInput PIN(s):\n$generatedPins",
                                                bonusVpnTime = "+1 Hour Unlimited VPN Time",
                                                bonusPoints = 20
                                            )
                                        } else errorMessage = msg
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1.3f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian)
                    ) {
                        Text("✅ Confirm & Pay", fontWeight = FontWeight.Black, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showPhoneVerificationDialog) {
        PhoneVerificationDialog(
            viewModel = viewModel,
            initialPhone = viewModel.userVirtualAccount.value.phoneNumber,
            title = "Free Verification",
            description = "Free verification to secure your payment.",
            onDismiss = { showPhoneVerificationDialog = false },
            onSuccess = {
                showPhoneVerificationDialog = false
            }
        )
    }
}
