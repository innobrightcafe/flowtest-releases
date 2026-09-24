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
import com.example.data.db.SavedRecipientEntity
import com.example.data.ui.viewmodel.VpnViewModel
import com.example.ui.theme.*

/**
 * ElectricitySheet
 * Built in the Cyber Luxury Dark Navy Palette matching DataBundlesSheet:
 * - Top App Bar with back button, centered title, and live wallet balance chip
 * - DISCO Distribution Company Selector (IKEDC, EKEDC, AEDC, IBEDC, EEDC, KEDCO)
 * - Meter Type Tabs ("PREPAID (Token)" | "POSTPAID (Bill)") with CyberCyan underline indicator
 * - Mode Tabs ("My Meter" | "Other Meter") with CyberCyan underline indicator
 * - Meter Number input with instant Customer Verification Badge
 * - Amount Preset Pills (₦1,000, ₦2,000, ₦3,000, ₦5,000, ₦10,000, ₦20,000) + Custom Amount
 * - Bottom Summary Card & Action CTA with Recipient Safety Verification Drawer
 * - Transaction Success Receipt with generated token, VPN bonus time, and loyalty points
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ElectricitySheet(
    viewModel: VpnViewModel,
    onOpenTracker: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val walletBalance by viewModel.userWalletBalance.collectAsStateWithLifecycle()
    val savedRecipients by viewModel.savedRecipients.collectAsStateWithLifecycle()

    var isMyMeter by remember { mutableStateOf(false) }
    var selectedDisco by remember { mutableStateOf(viewModel.getUserOption("last_electricity_disco", "IKEDC (Ikeja)")) }
    var meterType by remember { mutableStateOf(viewModel.getUserOption("last_electricity_type", "PREPAID")) } // "PREPAID" or "POSTPAID"
    var meterNumberInput by remember { mutableStateOf(viewModel.getUserOption("last_electricity_meter", "")) }
    var verifiedAccountName by remember { mutableStateOf<String?>(null) }
    var verifiedMeterResult by remember { mutableStateOf<com.example.data.model.MeterVerificationResult?>(null) }
    var isVerifying by remember { mutableStateOf(false) }

    var amountInput by remember { mutableStateOf(viewModel.getUserOption("last_electricity_amount", "2000")) }

    var isProcessing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var purchaseReceipt by remember { mutableStateOf<PurchaseSuccessReceipt?>(null) }
    var showSafetyVerificationDrawer by remember { mutableStateOf(false) }
    var showPhoneVerificationDialog by remember { mutableStateOf(false) }
    var saveRecipientToQuickPay by remember { mutableStateOf(true) }

    val faceValue = amountInput.toDoubleOrNull() ?: 2000.0
    val payableAmount = faceValue
    val cashbackRateBills by viewModel.cashbackRateBillsPercent.collectAsStateWithLifecycle()
    // Safe cashback strictly capped within our commission (0.5% - 1.0%, max ₦20) so we never lose money
    val calculatedCashback = minOf(faceValue * (cashbackRateBills / 100.0), 20.0).coerceAtLeast(0.0)

    val discos = listOf(
        "IKEDC (Ikeja)",
        "EKEDC (Eko)",
        "AEDC (Abuja)",
        "IBEDC (Ibadan)",
        "EEDC (Enugu)",
        "KEDCO (Kano)",
        "PHED (Port Harcourt)",
        "BEDC (Benin)",
        "KAEDCO (Kaduna)",
        "JEDC (Jos)",
        "YEDC (Yola)",
        "ABA (Aba Power)"
    )

    val presetAmounts = listOf("1000", "2000", "3000", "5000", "10000", "20000")

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
                            text = "Electricity Bills",
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
                    // ENERGY & CONSUMPTION TRACKER SHORTCUT BANNER
                    val sheetMeterConfig by viewModel.electricityMeterConfig.collectAsStateWithLifecycle()
                    val sheetAppliances by viewModel.electricityAppliances.collectAsStateWithLifecycle()
                    val sheetDailyBurnKwh = remember(sheetAppliances) { sheetAppliances.filter { it.isEnabled }.sumOf { it.dailyKwh } }
                    val sheetRemainingKwh = remember(sheetMeterConfig, sheetDailyBurnKwh) { sheetMeterConfig.getEstimatedRemainingKwh(sheetDailyBurnKwh) }
                    val sheetDaysRemaining = remember(sheetMeterConfig, sheetDailyBurnKwh) { sheetMeterConfig.getEstimatedDaysRemaining(sheetDailyBurnKwh) }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = DarkSurfaceElevated,
                        border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onOpenTracker?.invoke()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(GlowingAmber.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Bolt, contentDescription = null, tint = GlowingAmber, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Electricity Consumption & Insights",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    )
                                    val runwayStr = if (sheetDailyBurnKwh <= 0.0) "${String.format(java.util.Locale.US, "%.1f", sheetRemainingKwh)} kWh balance"
                                    else "${String.format(java.util.Locale.US, "%.1f", sheetRemainingKwh)} kWh • ~${String.format(java.util.Locale.US, "%.1f", sheetDaysRemaining)} days power left"
                                    Text(
                                        text = runwayStr,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = CyberCyan,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                }
                            }

                            if (onOpenTracker != null) {
                                Button(
                                    onClick = { onOpenTracker() },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("Dashboard →", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 2. DISCO SELECTION ROW
                    Text(
                        text = "1. Select Electricity Distribution Company (DISCO)",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        discos.forEach { disco ->
                            val isSelected = selectedDisco == disco
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) CyberCyan.copy(alpha = 0.18f) else DarkSurface,
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) CyberCyan else DarkCardBorder
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        selectedDisco = disco
                                        verifiedAccountName = null
                                        verifiedMeterResult = null
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("⚡", fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = disco,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) CyberCyan else TextPrimary,
                                            fontSize = 12.sp
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3. METER TYPE SELECTOR: "PREPAID (Token)" | "POSTPAID (Bill)"
                    Surface(
                        color = DarkSurface,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, DarkCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { meterType = "PREPAID" }
                                    .padding(vertical = 11.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "PREPAID (Token)",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (meterType == "PREPAID") FontWeight.Bold else FontWeight.Medium,
                                        color = if (meterType == "PREPAID") CyberCyan else TextMuted,
                                        fontSize = 13.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.55f)
                                        .height(3.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(if (meterType == "PREPAID") CyberCyan else Color.Transparent)
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { meterType = "POSTPAID" }
                                    .padding(vertical = 11.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "POSTPAID (Monthly Bill)",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (meterType == "POSTPAID") FontWeight.Bold else FontWeight.Medium,
                                        color = if (meterType == "POSTPAID") CyberCyan else TextMuted,
                                        fontSize = 13.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.55f)
                                        .height(3.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(if (meterType == "POSTPAID") CyberCyan else Color.Transparent)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // SAVED METERS (Dedicated meter number management - not contacts)
                    val savedMeters = savedRecipients.filter {
                        it.recipientType.equals("electricity", ignoreCase = true) ||
                        it.recipientType.equals("electric", ignoreCase = true)
                    }

                    if (savedMeters.isNotEmpty()) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.ElectricBolt,
                                        contentDescription = null,
                                        tint = CyberCyan,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "SAVED METERS (${savedMeters.size})",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = TextSecondary,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 11.sp,
                                            letterSpacing = 0.5.sp
                                        )
                                    )
                                }
                                Text(
                                    text = "Tap to load",
                                    style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 10.sp)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(horizontal = 2.dp)
                            ) {
                                items(savedMeters, key = { it.id }) { meter ->
                                    val isSelected = meterNumberInput.trim() == meter.identifier.trim()
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) CyberCyan.copy(alpha = 0.15f) else DarkSurface,
                                        border = BorderStroke(1.dp, if (isSelected) CyberCyan else DarkCardBorder),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                meterNumberInput = meter.identifier.trim()
                                                if (meter.institutionOrProvider.isNotBlank()) {
                                                    val match = discos.firstOrNull { it.contains(meter.institutionOrProvider, ignoreCase = true) }
                                                    if (match != null) selectedDisco = match
                                                }
                                                if (!meter.bankAccountName.isNullOrBlank()) {
                                                    verifiedAccountName = meter.bankAccountName
                                                } else if (meter.name.isNotBlank() && meter.name != "Beneficiary" && meter.name != "Electricity Meter") {
                                                    verifiedAccountName = meter.name
                                                }
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                val displayName = if (meter.name.isNotBlank() && meter.name != "Beneficiary") meter.name else "Meter ${meter.identifier.takeLast(4)}"
                                                Text(
                                                    text = displayName,
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isSelected) CyberCyan else TextPrimary,
                                                        fontSize = 11.5.sp
                                                    ),
                                                    maxLines = 1
                                                )
                                                Text(
                                                    text = meter.identifier.trim(),
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontFamily = FontFamily.Monospace,
                                                        color = TextMuted,
                                                        fontSize = 10.sp
                                                    )
                                                )
                                                if (meter.institutionOrProvider.isNotBlank()) {
                                                    Text(
                                                        text = meter.institutionOrProvider,
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            color = ElectricEmerald,
                                                            fontSize = 9.5.sp,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            IconButton(
                                                onClick = {
                                                    viewModel.deleteRecipient(meter.id)
                                                    Toast.makeText(context, "Meter removed from saved list", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(22.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Remove meter",
                                                    tint = TextMuted,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 4. METER NUMBER INPUT & VERIFICATION
                    Text(
                        text = "2. Meter Number & Customer Verification",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = meterNumberInput,
                            onValueChange = {
                                meterNumberInput = it
                                verifiedAccountName = null
                                verifiedMeterResult = null
                            },
                            placeholder = { Text("Enter 11-digit Meter Number", color = TextMuted, fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.ElectricBolt, contentDescription = "Meter", tint = CyberCyan, modifier = Modifier.size(18.dp))
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        val clip = clipboardManager.getText()?.text ?: ""
                                        if (clip.isNotBlank()) {
                                            meterNumberInput = clip.trim()
                                            verifiedAccountName = null
                                            verifiedMeterResult = null
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = CyberCyan, modifier = Modifier.size(16.dp))
                                }
                            },
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
                            modifier = Modifier.weight(1f)
                        )

                        Button(
                            onClick = {
                                if (!com.example.data.util.NetworkUtils.isNetworkAvailable(context)) {
                                    Toast.makeText(context, "No network connection. Please check your mobile data or Wi-Fi.", Toast.LENGTH_LONG).show()
                                    errorMessage = "No network connection. Please check your mobile data or Wi-Fi to verify meter."
                                    return@Button
                                }
                                val cleanNum = meterNumberInput.trim()
                                if (cleanNum.length >= 8) {
                                    isVerifying = true
                                    viewModel.verifyMeterDetails(
                                        serviceId = selectedDisco,
                                        customerId = cleanNum,
                                        type = meterType.lowercase()
                                    ) { success, result ->
                                        isVerifying = false
                                        if (success && result.isValid) {
                                            verifiedMeterResult = result
                                            verifiedAccountName = result.customerName
                                        } else {
                                            verifiedMeterResult = null
                                            verifiedAccountName = null
                                            Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                                        }
                                    }
                                } else {
                                    Toast.makeText(context, "Please enter at least 8 digits on your meter card", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = !isVerifying,
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(52.dp)
                        ) {
                            if (isVerifying) {
                                FlowButtonLoadingLine(color = DarkObsidian, width = 36.dp)
                            } else {
                                Text("VERIFY", fontWeight = FontWeight.Black, fontSize = 12.sp)
                            }
                        }
                    }

                    // Verified Badge Card & Official Meter Details
                    verifiedMeterResult?.let { res ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = Color(0xFF041913),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.2.dp, ElectricEmerald.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Verified,
                                            contentDescription = "Verified",
                                            tint = ElectricEmerald,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "VERIFIED METER DETAILS",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = ElectricEmerald,
                                                fontWeight = FontWeight.Black,
                                                letterSpacing = 0.8.sp,
                                                fontSize = 10.5.sp
                                            )
                                        )
                                    }
                                    Surface(
                                        color = ElectricEmerald.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "OFFICIAL RECORD",
                                            color = ElectricEmerald,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Registered Customer Name
                                Text(
                                    text = res.customerName,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        fontSize = 13.sp
                                    )
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                // Meter Number & Account Code
                                Text(
                                    text = "Meter: ${res.meterNumber} • ${res.accountCode}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = CyberCyan,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp
                                    )
                                )

                                // Service / Premise Address
                                if (res.serviceAddress.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "📍 ${res.serviceAddress}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color(0xFFE2E8F0),
                                            fontSize = 11.sp,
                                            lineHeight = 15.sp
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // DISCO & Tariff Class
                                Text(
                                    text = "⚡ ${res.discoName} • Tariff: ${res.tariffClass}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TextMuted,
                                        fontSize = 10.sp
                                    )
                                )

                                // Instant Save Meter Button
                                val isMeterAlreadySaved = savedMeters.any { it.identifier.trim() == res.meterNumber.trim() }
                                if (!isMeterAlreadySaved) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = {
                                            viewModel.saveRecipient(
                                                name = res.customerName.ifBlank { "Meter ${res.meterNumber.takeLast(4)}" },
                                                recipientType = "electricity",
                                                identifier = res.meterNumber.trim(),
                                                institutionOrProvider = res.discoName.ifBlank { selectedDisco },
                                                bankAccountName = res.customerName,
                                                isFavorite = true
                                            )
                                            Toast.makeText(context, "Meter saved successfully for 1-tap recharges!", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan.copy(alpha = 0.2f), contentColor = CyberCyan),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(34.dp)
                                    ) {
                                        Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Save Meter for Later", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    } ?: verifiedAccountName?.let { name ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = ElectricEmerald.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, ElectricEmerald.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Verified", tint = ElectricEmerald, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("✓ Meter Registered To: $name", style = MaterialTheme.typography.labelSmall.copy(color = ElectricEmerald, fontWeight = FontWeight.Bold))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 5. RECHARGE AMOUNT PRESETS
                    Text(
                        text = "3. Select Recharge Amount",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presetAmounts.take(3).forEach { denom ->
                            val isSelected = amountInput == denom
                            val amtVal = denom.toDouble()
                            val earnAmt = minOf(amtVal * (cashbackRateBills / 100.0), 20.0).toInt()
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) CyberCyan.copy(alpha = 0.2f) else DarkSurface,
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) CyberCyan else DarkCardBorder
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { amountInput = denom }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "₦$denom",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Black,
                                            color = if (isSelected) CyberCyan else TextPrimary,
                                            fontSize = 14.sp
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (earnAmt > 0) "+₦$earnAmt back" else "Instant Vend",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = ElectricEmerald,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 10.sp
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presetAmounts.drop(3).take(3).forEach { denom ->
                            val isSelected = amountInput == denom
                            val amtVal = denom.toDouble()
                            val earnAmt = minOf(amtVal * (cashbackRateBills / 100.0), 20.0).toInt()
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) CyberCyan.copy(alpha = 0.2f) else DarkSurface,
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) CyberCyan else DarkCardBorder
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { amountInput = denom }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "₦$denom",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Black,
                                            color = if (isSelected) CyberCyan else TextPrimary,
                                            fontSize = 14.sp
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (earnAmt > 0) "+₦$earnAmt back" else "Instant Vend",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = ElectricEmerald,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 10.sp
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // CUSTOM AMOUNT FIELD
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Custom Amount (₦500 - ₦100,000)", color = TextMuted) },
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

                    // SAVE METER TO QUICK PAY TOGGLE
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
                                        text = "Save meter number & details for later",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    )
                                    Text(
                                        text = "Auto-save meter and DisCo for 1-tap recharges",
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

                // 6. BOTTOM PAYMENT BAR & ACTION CTA
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
                                Text("Total Payable", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 11.sp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("₦${String.format("%,.2f", payableAmount)}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, color = CyberCyan))
                                }
                                if (calculatedCashback > 0.0) {
                                    Text("Earn +₦${String.format("%,.2f", calculatedCashback)} Cashback", style = MaterialTheme.typography.labelSmall.copy(color = ElectricEmerald, fontSize = 10.sp, fontWeight = FontWeight.Bold))
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
                                if (meterNumberInput.trim().length < 8) {
                                    errorMessage = "Please enter a valid meter number."
                                    return@Button
                                }
                                val enteredAmt = amountInput.toDoubleOrNull() ?: 0.0
                                if (enteredAmt < 1000.0) {
                                    errorMessage = "Minimum electricity purchase for $selectedDisco is ₦1,000 as required by electricity providers."
                                    return@Button
                                }
                                showSafetyVerificationDrawer = true
                            },
                            enabled = !isProcessing,
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            if (isProcessing) {
                                FlowButtonLoadingLine(color = DarkObsidian, width = 48.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("GENERATING TOKEN...", fontWeight = FontWeight.Black)
                            } else {
                                Text("PAY ELECTRICITY • ₦${String.format("%,.2f", payableAmount)}", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black, color = DarkObsidian, letterSpacing = 0.5.sp))
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(GlowingAmber.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⚡", fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("CHECK & CONFIRM METER", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, color = TextPrimary))
                        Text("Your 20-digit PIN will be generated to enter into your meter", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp))
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
                        Text("METER TARGET", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontWeight = FontWeight.Bold))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = meterNumberInput.trim(),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = CyberCyan
                            )
                        )
                        Text(
                            text = "$selectedDisco • $meterType",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 12.sp)
                        )
                        verifiedMeterResult?.let { res ->
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "👤 ${res.customerName}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "📍 ${res.serviceAddress}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFFE2E8F0),
                                    fontSize = 11.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "⚡ Tariff: ${res.tariffClass}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = CyberCyan,
                                    fontSize = 10.sp
                                )
                            )
                        } ?: verifiedAccountName?.let {
                            Text(text = "✓ $it", style = MaterialTheme.typography.bodySmall.copy(color = ElectricEmerald, fontSize = 12.sp))
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = DarkCardBorder)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Recharge Amount", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
                            Text("₦${String.format("%,.2f", faceValue)}", style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, fontWeight = FontWeight.Bold))
                        }
                        if (calculatedCashback > 0.0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Cashback Reward", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
                                Text("+₦${String.format("%,.2f", calculatedCashback)}", style = MaterialTheme.typography.bodyMedium.copy(color = ElectricEmerald, fontWeight = FontWeight.Black))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // SAVE METER TOGGLE
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (saveRecipientToQuickPay) Icons.Default.BookmarkAdded else Icons.Default.BookmarkBorder,
                                contentDescription = "Quick Pay",
                                tint = if (saveRecipientToQuickPay) CyberCyan else TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Save meter number & details for later",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                )
                                Text(
                                    text = "Auto-save meter and DisCo for 1-tap recharges",
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

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { showSafetyVerificationDrawer = false },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        border = BorderStroke(1.dp, DarkCardBorder)
                    ) {
                        Text("✏️ Edit Details", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            if (!com.example.data.util.NetworkUtils.isNetworkAvailable(context)) {
                                Toast.makeText(context, "No network connection. Please check your mobile data or Wi-Fi.", Toast.LENGTH_LONG).show()
                                errorMessage = "No network connection. Please check your mobile data or Wi-Fi to complete electricity recharge."
                                showSafetyVerificationDrawer = false
                                return@Button
                            }
                            showSafetyVerificationDrawer = false
                            isProcessing = true

                            val payAmt = payableAmount.toInt()
                            viewModel.payPairgateBill(
                                billerId = selectedDisco,
                                customerId = meterNumberInput.trim(),
                                amountNaira = payAmt,
                                tokenPin = null,
                                meterUnits = null,
                                customerName = verifiedMeterResult?.customerName ?: verifiedAccountName,
                                meterNumber = verifiedMeterResult?.meterNumber ?: meterNumberInput.trim(),
                                serviceAddress = verifiedMeterResult?.serviceAddress,
                                discoName = verifiedMeterResult?.discoName ?: selectedDisco,
                                meterType = meterType,
                                tariffClass = verifiedMeterResult?.tariffClass
                            ) { ok, msg, ref ->
                                isProcessing = false
                                if (ok) {
                                    viewModel.saveUserOption("last_electricity_disco", selectedDisco)
                                    viewModel.saveUserOption("last_electricity_type", meterType)
                                    viewModel.saveUserOption("last_electricity_meter", meterNumberInput.trim())
                                    viewModel.saveUserOption("last_electricity_amount", amountInput.trim())

                                    val finalRef = ref ?: "FLOW-BILL-${System.currentTimeMillis()}"
                                    val txLog = viewModel.vtuTransactionLogs.value.find { it.reference.equals(finalRef, ignoreCase = true) }
                                    val liveToken = txLog?.tokenPin
                                    val liveUnits = txLog?.meterUnits
                                    val isPending = txLog?.status.equals("PENDING", ignoreCase = true)

                                    val parsedUnits = liveUnits?.replace("kWh", "", ignoreCase = true)?.trim()?.toDoubleOrNull()
                                        ?: (payableAmount / viewModel.electricityMeterConfig.value.tariffRatePerKwh.coerceAtLeast(50.0))
                                    viewModel.addPurchasedElectricityToken(
                                        unitsKwh = parsedUnits,
                                        amountNaira = payableAmount,
                                        tokenPin = liveToken ?: "",
                                        meterNumber = meterNumberInput.trim()
                                    )

                                    if (saveRecipientToQuickPay) {
                                        val custName = verifiedMeterResult?.customerName ?: verifiedAccountName ?: "Electricity Meter"
                                        viewModel.saveRecipient(
                                            name = custName,
                                            recipientType = "electricity",
                                            identifier = meterNumberInput.trim(),
                                            institutionOrProvider = selectedDisco,
                                            bankAccountName = verifiedAccountName,
                                            isFavorite = true
                                        )
                                    }

                                    val finalMsg = if (!liveToken.isNullOrBlank()) {
                                        "Token PIN: $liveToken" + (if (!liveUnits.isNullOrBlank()) " • Units: $liveUnits" else "") + "\nEnter this PIN into your meter keypad followed by Enter (↵)."
                                    } else if (isPending) {
                                        "Purchase submitted to $selectedDisco. Transaction is currently processing with reference $finalRef. Your token will appear once confirmed by DisCo."
                                    } else {
                                        msg
                                    }

                                    purchaseReceipt = PurchaseSuccessReceipt(
                                        serviceTitle = "Electricity Token ($selectedDisco)",
                                        providerOrType = selectedDisco,
                                        recipient = meterNumberInput.trim(),
                                        amountPaid = payableAmount,
                                        reference = finalRef,
                                        newBalance = viewModel.userWalletBalance.value,
                                        message = finalMsg,
                                        bonusVpnTime = "+2 Hours Unlimited VPN Time",
                                        bonusPoints = 30,
                                        tokenPin = liveToken,
                                        meterUnits = liveUnits,
                                        customerName = verifiedMeterResult?.customerName ?: verifiedAccountName,
                                        meterNumber = verifiedMeterResult?.meterNumber ?: meterNumberInput.trim(),
                                        serviceAddress = verifiedMeterResult?.serviceAddress,
                                        discoName = verifiedMeterResult?.discoName ?: selectedDisco,
                                        meterType = meterType,
                                        tariffClass = verifiedMeterResult?.tariffClass
                                    )
                                } else {
                                    errorMessage = msg
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1.3f)
                            .height(48.dp),
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
            description = "Free verification to secure your recharge.",
            onDismiss = { showPhoneVerificationDialog = false },
            onSuccess = {
                showPhoneVerificationDialog = false
            }
        )
    }
}
