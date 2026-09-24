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
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.SolidColor
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Velocity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.db.SavedRecipientEntity
import com.example.data.ui.viewmodel.VpnViewModel
import com.example.ui.theme.*

data class CableBouquetItem(
    val id: String,
    val name: String,
    val provider: String,
    val originalPrice: Double,
    val discountedPrice: Double,
    val channels: String,
    val validity: String,
    val category: String, // "Popular", "Standard", "Premium"
    val isHot: Boolean = false
)

/**
 * CableTvSheet
 * Built in the Cyber Luxury Dark Navy Palette matching DataBundlesSheet:
 * - Top App Bar with back button, centered title, and live wallet balance chip
 * - Provider Selection Bar (GOTV, DSTV, Startimes, Showmax)
 * - Mode Tabs ("My Decoder" | "Other Decoder") with solid CyberCyan underline indicator
 * - Smartcard / IUC Input with Instant Account Verification Badge
 * - Category Filter Pills: All Bouquets, Popular, Standard, Premium
 * - Rich Bouquet Card list with Channels count, Validity, and Select buttons
 * - Bottom Summary Card & Action CTA with Recipient Safety Verification Drawer
 * - Transaction Success Receipt with VPN bonus time and loyalty points
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CableTvSheet(
    viewModel: VpnViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val walletBalance by viewModel.userWalletBalance.collectAsStateWithLifecycle()
    val savedRecipients by viewModel.savedRecipients.collectAsStateWithLifecycle()

    var isMyDecoder by remember { mutableStateOf(false) }
    var selectedProvider by remember { mutableStateOf(viewModel.getUserOption("last_tv_provider", "GOTV")) }
    var selectedCategoryPill by remember { mutableStateOf("All Bouquets") }
    var smartcardInput by remember { mutableStateOf(viewModel.getUserOption("last_tv_smartcard", "")) }
    var verifiedAccountName by remember { mutableStateOf<String?>(null) }
    var isVerifying by remember { mutableStateOf(false) }
    var showPhoneVerificationDialog by remember { mutableStateOf(false) }

    // Bouquet Catalog
    val allBouquets = remember(selectedProvider) {
        when (selectedProvider) {
            "DSTV" -> listOf(
                CableBouquetItem("dstv-padi", "DStv Padi", "DSTV", 3600.0, 3450.0, "45+ Channels", "30 Days", "Standard"),
                CableBouquetItem("dstv-yanga", "DStv Yanga", "DSTV", 5100.0, 4900.0, "85+ Channels", "30 Days", "Popular", isHot = true),
                CableBouquetItem("dstv-confam", "DStv Confam", "DSTV", 9300.0, 8950.0, "105+ Channels", "30 Days", "Popular"),
                CableBouquetItem("dstv-compact", "DStv Compact", "DSTV", 15700.0, 15100.0, "130+ Channels", "30 Days", "Premium", isHot = true),
                CableBouquetItem("dstv-compact-plus", "DStv Compact Plus", "DSTV", 25000.0, 24200.0, "145+ Channels (Full Sports)", "30 Days", "Premium"),
                CableBouquetItem("dstv-premium", "DStv Premium Full", "DSTV", 37000.0, 35900.0, "160+ Channels & HD", "30 Days", "Premium")
            )
            "Startimes" -> listOf(
                CableBouquetItem("st-nova", "Startimes Nova", "Startimes", 1900.0, 1800.0, "35+ Channels", "30 Days", "Standard"),
                CableBouquetItem("st-basic", "Startimes Basic", "Startimes", 3300.0, 3150.0, "50+ Channels", "30 Days", "Popular", isHot = true),
                CableBouquetItem("st-smart", "Startimes Smart", "Startimes", 4700.0, 4500.0, "65+ Channels", "30 Days", "Standard"),
                CableBouquetItem("st-classic", "Startimes Classic", "Startimes", 5500.0, 5250.0, "80+ Channels", "30 Days", "Popular"),
                CableBouquetItem("st-super", "Startimes Super Package", "Startimes", 8200.0, 7850.0, "100+ Channels & Sports", "30 Days", "Premium", isHot = true)
            )
            "Showmax" -> listOf(
                CableBouquetItem("sh-mobile", "Showmax Entertainment Mobile", "Showmax", 1600.0, 1500.0, "Mobile Streaming", "30 Days", "Standard"),
                CableBouquetItem("sh-all", "Showmax Entertainment All Devices", "Showmax", 3200.0, 3050.0, "HD TV & Mobile", "30 Days", "Popular", isHot = true),
                CableBouquetItem("sh-pl", "Showmax Premier League Mobile", "Showmax", 3200.0, 3050.0, "Live EPL Matches", "30 Days", "Popular", isHot = true),
                CableBouquetItem("sh-pro", "Showmax Pro All Devices", "Showmax", 5500.0, 5250.0, "Full Sports & Movies", "30 Days", "Premium")
            )
            else -> listOf(
                // GOTV
                CableBouquetItem("gotv-small", "GOtv Smallie", "GOTV", 1575.0, 1500.0, "30+ Channels", "30 Days", "Standard"),
                CableBouquetItem("gotv-jinja", "GOtv Jinja Plan", "GOTV", 3300.0, 3180.0, "45+ Channels", "30 Days", "Popular"),
                CableBouquetItem("gotv-jolli", "GOtv Jolli Package", "GOTV", 4850.0, 4680.0, "65+ Channels", "30 Days", "Popular", isHot = true),
                CableBouquetItem("gotv-max", "GOtv Max Package", "GOTV", 7200.0, 6950.0, "75+ Channels (Full Sports)", "30 Days", "Premium", isHot = true),
                CableBouquetItem("gotv-supa", "GOtv Supa Package", "GOTV", 9600.0, 9250.0, "85+ Channels", "30 Days", "Premium"),
                CableBouquetItem("gotv-supa-plus", "GOtv Supa+ (All EPL Matches)", "GOTV", 15700.0, 15150.0, "Complete Channels & EPL", "30 Days", "Premium", isHot = true)
            )
        }
    }

    var selectedBouquet by remember(selectedProvider) { mutableStateOf(allBouquets.first()) }

    var isProcessing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var purchaseReceipt by remember { mutableStateOf<PurchaseSuccessReceipt?>(null) }
    var showSafetyVerificationDrawer by remember { mutableStateOf(false) }
    var saveRecipientToQuickPay by remember { mutableStateOf(true) }
    var showSavedRecipientsManager by remember { mutableStateOf(false) }

    val discountPct = 3.0
    val faceValue = selectedBouquet.originalPrice
    val discountedPayable = selectedBouquet.discountedPrice
    val totalSavings = faceValue - discountedPayable
    val calculatedCashback = (discountedPayable * 0.02).toInt()

    val filteredBouquets = remember(allBouquets, selectedCategoryPill) {
        when (selectedCategoryPill) {
            "Popular" -> allBouquets.filter { it.isHot || it.category == "Popular" }
            "Standard" -> allBouquets.filter { it.category == "Standard" }
            "Premium" -> allBouquets.filter { it.category == "Premium" }
            else -> allBouquets
        }
    }

    var bouquetSearchQuery by remember { mutableStateOf("") }

    val searchedBouquets = remember(filteredBouquets, allBouquets, bouquetSearchQuery) {
        val q = bouquetSearchQuery.trim().lowercase()
        if (q.isBlank()) {
            filteredBouquets
        } else {
            allBouquets.filter {
                it.name.lowercase().contains(q) ||
                it.id.lowercase().contains(q) ||
                it.category.lowercase().contains(q) ||
                it.channels.lowercase().contains(q) ||
                it.validity.lowercase().contains(q) ||
                it.discountedPrice.toString().contains(q) ||
                it.originalPrice.toString().contains(q)
            }
        }
    }

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
                            text = "Cable TV Subscription",
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
                    // 2. PROVIDER SELECTION BAR
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("GOTV", "DSTV", "Startimes", "Showmax").forEach { prov ->
                            val isSelected = selectedProvider.equals(prov, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) CyberCyan.copy(alpha = 0.18f) else DarkSurface,
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) CyberCyan else DarkCardBorder
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(58.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        selectedProvider = prov
                                        verifiedAccountName = null
                                    }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(vertical = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = when (prov) {
                                            "GOTV" -> "📺 GOtv"
                                            "DSTV" -> "📡 DStv"
                                            "Startimes" -> "⭐ StarTimes"
                                            else -> "🎬 Showmax"
                                        },
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                            color = if (isSelected) CyberCyan else TextPrimary,
                                            fontSize = 12.sp
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3. MODE TABS: "My Decoder" | "Other Decoder"
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
                                    .clickable {
                                        isMyDecoder = true
                                        val saved = savedRecipients.firstOrNull { it.recipientType.equals("cable", ignoreCase = true) }
                                        smartcardInput = saved?.identifier ?: ""
                                        verifiedAccountName = saved?.bankAccountName ?: saved?.name
                                    }
                                    .padding(vertical = 11.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "My Decoder",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isMyDecoder) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isMyDecoder) CyberCyan else TextMuted,
                                        fontSize = 13.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.55f)
                                        .height(3.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(if (isMyDecoder) CyberCyan else Color.Transparent)
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        isMyDecoder = false
                                        smartcardInput = ""
                                        verifiedAccountName = null
                                    }
                                    .padding(vertical = 11.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Other Decoder",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (!isMyDecoder) FontWeight.Bold else FontWeight.Medium,
                                        color = if (!isMyDecoder) CyberCyan else TextMuted,
                                        fontSize = 13.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.55f)
                                        .height(3.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(if (!isMyDecoder) CyberCyan else Color.Transparent)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // QUICK PAY / SAVED BENEFICIARIES
                    val cableRecipients = savedRecipients.filter {
                        it.recipientType.equals("cable", ignoreCase = true) ||
                        it.recipientType.equals("tv", ignoreCase = true)
                    }
                    QuickRecipientPickerRow(
                        recipients = cableRecipients,
                        recipientTypeFilter = "cable",
                        onRecipientSelected = { recipient ->
                            smartcardInput = recipient.identifier.trim()
                            if (recipient.institutionOrProvider.isNotBlank()) {
                                val match = listOf("GOTV", "DSTV", "Startimes", "Showmax").firstOrNull { it.contains(recipient.institutionOrProvider, ignoreCase = true) }
                                if (match != null) selectedProvider = match
                            }
                            if (!recipient.bankAccountName.isNullOrBlank()) {
                                verifiedAccountName = recipient.bankAccountName
                            } else if (recipient.name.isNotBlank() && recipient.name != "Beneficiary") {
                                verifiedAccountName = recipient.name
                            }
                        },
                        onManageClick = { showSavedRecipientsManager = true }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 4. SMARTCARD / IUC INPUT & VERIFICATION
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = smartcardInput,
                            onValueChange = {
                                smartcardInput = it
                                verifiedAccountName = null
                            },
                            placeholder = { Text("Enter 10-digit IUC / Smartcard Number", color = TextMuted, fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.CreditCard, contentDescription = "Smartcard", tint = CyberCyan, modifier = Modifier.size(18.dp))
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        val clip = clipboardManager.getText()?.text ?: ""
                                        if (clip.isNotBlank()) {
                                            smartcardInput = clip.trim()
                                            verifiedAccountName = null
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
                                    errorMessage = "No network connection. Please check your mobile data or Wi-Fi to verify smartcard."
                                    return@Button
                                }
                                val cleanNum = smartcardInput.trim()
                                if (cleanNum.length >= 8) {
                                    isVerifying = true
                                    viewModel.verifyCustomerOrMeter(
                                        serviceId = selectedProvider.lowercase(),
                                        customerId = cleanNum,
                                        type = "cable"
                                    ) { success, result ->
                                        isVerifying = false
                                        if (success) {
                                            verifiedAccountName = result
                                        } else {
                                            verifiedAccountName = null
                                            Toast.makeText(context, result, Toast.LENGTH_LONG).show()
                                        }
                                    }
                                } else {
                                    Toast.makeText(context, "Please enter at least 8 digits on your smartcard / IUC", Toast.LENGTH_SHORT).show()
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

                    // Verified Badge Card
                    verifiedAccountName?.let { name ->
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
                                Text("✓ Account Holder: $name", style = MaterialTheme.typography.labelSmall.copy(color = ElectricEmerald, fontWeight = FontWeight.Bold))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // SAVE SMARTCARD TO QUICK PAY TOGGLE
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
                                        text = "Auto-save IUC/Smartcard for 1-tap TV renewals",
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

                    Spacer(modifier = Modifier.height(14.dp))

                    // 5. CATEGORY FILTER PILLS
                    Text(
                        text = "Select Subscription Package",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("All Bouquets", "Popular", "Standard", "Premium").forEach { pill ->
                            val isSelected = selectedCategoryPill == pill
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) CyberCyan else DarkSurface,
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isSelected) CyberCyan else DarkCardBorder
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable { selectedCategoryPill = pill }
                            ) {
                                Text(
                                    text = pill,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isSelected) DarkObsidian else TextSecondary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Bouquet Search Box
                    Surface(
                        color = DarkSurface,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (bouquetSearchQuery.isNotBlank()) CyberCyan else DarkCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search Packages",
                                tint = if (bouquetSearchQuery.isNotBlank()) CyberCyan else TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            BasicTextField(
                                value = bouquetSearchQuery,
                                onValueChange = { bouquetSearchQuery = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 8.dp),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = TextPrimary,
                                    fontSize = 13.sp
                                ),
                                singleLine = true,
                                cursorBrush = SolidColor(CyberCyan),
                                decorationBox = { innerTextField ->
                                    if (bouquetSearchQuery.isEmpty()) {
                                        Text(
                                            text = "Search $selectedProvider packages (e.g. Max, Jolli, Compact)...",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = TextMuted,
                                                fontSize = 12.sp
                                            )
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                            if (bouquetSearchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { bouquetSearchQuery = "" },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear Search",
                                        tint = TextMuted,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 6. BOUQUET CARDS LIST
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        searchedBouquets.forEach { bq ->
                            val isSelected = selectedBouquet.id == bq.id
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) Color(0xFF103248) else DarkSurface,
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) CyberCyan else DarkCardBorder
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { selectedBouquet = bq }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = bq.name,
                                                style = MaterialTheme.typography.titleSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary
                                                )
                                            )
                                            if (bq.isHot) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    color = GlowingAmber.copy(alpha = 0.2f),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = "POPULAR 🔥",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            color = GlowingAmber,
                                                            fontSize = 8.sp,
                                                            fontWeight = FontWeight.Black
                                                        ),
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = "${bq.channels} • ${bq.validity}",
                                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "₦${bq.originalPrice.toInt()}",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = TextMuted,
                                                    textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough,
                                                    fontSize = 11.sp
                                                )
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Pay ₦${bq.discountedPrice.toInt()}",
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    color = CyberCyan,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
                                                )
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "(Save ₦${(bq.originalPrice - bq.discountedPrice).toInt()})",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = ElectricEmerald,
                                                    fontSize = 10.sp
                                                )
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = if (isSelected) CyberCyan else DarkSurfaceElevated,
                                        border = BorderStroke(1.dp, if (isSelected) CyberCyan else DarkCardBorder),
                                        modifier = Modifier.clickable { selectedBouquet = bq }
                                    ) {
                                        Text(
                                            text = if (isSelected) "Selected ✓" else "Select",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = if (isSelected) DarkObsidian else TextSecondary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            ),
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
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
                                Text("Save ₦${String.format("%,.2f", totalSavings)}", style = MaterialTheme.typography.labelSmall.copy(color = ElectricEmerald, fontSize = 10.sp, fontWeight = FontWeight.Bold))
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
                                    errorMessage = "No network connection. Please check your mobile data or Wi-Fi to recharge Cable TV."
                                    return@Button
                                }
                                if (smartcardInput.trim().length < 8) {
                                    errorMessage = "Please enter a valid Smartcard / IUC number."
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
                                Text("RENEWING SUBSCRIPTION...", fontWeight = FontWeight.Black)
                            } else {
                                Text("SUBSCRIBE NOW • ₦${String.format("%,.2f", discountedPayable)}", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black, color = DarkObsidian, letterSpacing = 0.5.sp))
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
                        Text("⚠️", fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("CHECK & CONFIRM DECODER", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, color = TextPrimary))
                        Text("Subscriptions are credited immediately to provider", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp))
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
                        Text("SMARTCARD / IUC TARGET", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontWeight = FontWeight.Bold))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = smartcardInput.trim(),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = CyberCyan
                            )
                        )
                        verifiedAccountName?.let {
                            Text(text = "✓ $it", style = MaterialTheme.typography.bodySmall.copy(color = ElectricEmerald, fontSize = 12.sp))
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = DarkCardBorder)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Selected Bouquet", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
                            Text(selectedBouquet.name, style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, fontWeight = FontWeight.Bold))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Net Payable", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
                            Text("₦${String.format("%,.2f", discountedPayable)}", style = MaterialTheme.typography.bodyMedium.copy(color = ElectricEmerald, fontWeight = FontWeight.Black))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // SAVE BENEFICIARY / QUICK PAY TOGGLE
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
                                    text = "Save details for Quick Pay",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                )
                                Text(
                                    text = "Auto-fill decoder & package for 1-tap renewal",
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
                                errorMessage = "No network connection. Please check your mobile data or Wi-Fi to recharge Cable TV."
                                showSafetyVerificationDrawer = false
                                return@Button
                            }
                            showSafetyVerificationDrawer = false
                            isProcessing = true
                            viewModel.payPairgateBill(selectedProvider, smartcardInput.trim(), discountedPayable.toInt()) { ok, msg, ref ->
                                isProcessing = false
                                if (ok) {
                                    viewModel.saveUserOption("last_tv_provider", selectedProvider)
                                    viewModel.saveUserOption("last_tv_smartcard", smartcardInput.trim())

                                    if (saveRecipientToQuickPay) {
                                        val custName = verifiedAccountName ?: "Cable TV ($selectedProvider)"
                                        viewModel.saveRecipient(
                                            name = custName,
                                            recipientType = "cable",
                                            identifier = smartcardInput.trim(),
                                            institutionOrProvider = selectedProvider,
                                            bankAccountName = verifiedAccountName,
                                            isFavorite = true
                                        )
                                    }

                                    purchaseReceipt = PurchaseSuccessReceipt(
                                        serviceTitle = "Cable TV (${selectedBouquet.name})",
                                        providerOrType = selectedProvider,
                                        recipient = smartcardInput.trim(),
                                        amountPaid = discountedPayable,
                                        reference = ref ?: "PG-CAB-${(100000..999999).random()}",
                                        newBalance = viewModel.userWalletBalance.value,
                                        message = msg,
                                        bonusVpnTime = "+2 Hours Unlimited VPN Time",
                                        bonusPoints = 30
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

    if (showSavedRecipientsManager) {
        SavedRecipientsManagerDialog(
            recipients = savedRecipients.filter {
                it.recipientType.equals("cable", ignoreCase = true) ||
                it.recipientType.equals("tv", ignoreCase = true)
            },
            onDismiss = { showSavedRecipientsManager = false },
            onSaveNewRecipient = { name, _, identifier, provider, bankAccountName, isFavorite ->
                viewModel.saveRecipient(
                    name = name,
                    recipientType = "cable",
                    identifier = identifier,
                    institutionOrProvider = provider.ifBlank { selectedProvider },
                    bankAccountName = bankAccountName,
                    isFavorite = isFavorite
                )
            },
            onDeleteRecipient = { recId ->
                viewModel.deleteRecipient(recId)
            },
            onToggleFavorite = { rec ->
                viewModel.toggleFavoriteRecipient(rec)
            },
            onSelectRecipientForAction = { recipient ->
                smartcardInput = recipient.identifier.trim()
                if (recipient.institutionOrProvider.isNotBlank()) {
                    val match = listOf("GOTV", "DSTV", "Startimes", "Showmax").firstOrNull { it.contains(recipient.institutionOrProvider, ignoreCase = true) }
                    if (match != null) selectedProvider = match
                }
                if (!recipient.bankAccountName.isNullOrBlank()) {
                    verifiedAccountName = recipient.bankAccountName
                }
                showSavedRecipientsManager = false
            }
        )
    }

    if (showPhoneVerificationDialog) {
        PhoneVerificationDialog(
            viewModel = viewModel,
            initialPhone = viewModel.userVirtualAccount.value.phoneNumber,
            title = "Free Verification",
            description = "Free verification to secure your subscription.",
            onDismiss = { showPhoneVerificationDialog = false },
            onSuccess = {
                showPhoneVerificationDialog = false
            }
        )
    }
}
