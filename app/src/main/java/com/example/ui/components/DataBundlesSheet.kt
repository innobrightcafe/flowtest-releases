package com.example.ui.components

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.api.DataBundleDisplayPlan
import com.example.data.api.PairgateCatalogHelper
import com.example.data.api.PairgateDataPlanItem
import com.example.data.api.PairgateVerifiedPlans
import com.example.data.api.RecentDataPurchase
import com.example.data.db.SavedRecipientEntity
import com.example.data.ui.viewmodel.VpnViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.launch

/**
 * DataBundlesSheet
 * Combines the requested arrangement (reference layout) with our signature app theme:
 * - Cyber Luxury Dark Navy Palette (DarkObsidian, DarkSurface, CyberCyan, ElectricEmerald)
 * - Network Selection Bar with genuine TelcoLogos (MTN, Airtel, Glo, 9mobile) like in Airtime
 * - Mode Tabs ("Buy For Self" | "Buy For Others") with CyberCyan underline indicator
 * - Category Filter Pills with CyberCyan selection borders
 * - Validity Sub-Tabs: Daily, Weekly, Monthly, 2-Months, All
 * - Quick Repurchase: 1-tap re-ordering with TelcoLogo indicators
 * - Polished plan cards with More Info & Buy Now buttons styled in our theme
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataBundlesSheet(
    viewModel: VpnViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    val walletBalance by viewModel.userWalletBalance.collectAsStateWithLifecycle()
    val userVirtualAccount by viewModel.userVirtualAccount.collectAsStateWithLifecycle()
    val livePairgatePlans by viewModel.pairgateDataPlans.collectAsStateWithLifecycle()
    val recentDataPurchases by viewModel.recentDataPurchases.collectAsStateWithLifecycle()
    val favoritePlanKeys by viewModel.favoriteDataPlanKeys.collectAsStateWithLifecycle()
    val savedRecipients by viewModel.savedRecipients.collectAsStateWithLifecycle()

    val myPhoneNumber = remember(userVirtualAccount) {
        userVirtualAccount.phoneNumber.trim()
    }

    // Tabs & Filters State
    var isBuyForSelf by remember { mutableStateOf(true) }
    var selectedNetwork by remember { mutableStateOf(viewModel.getUserOption("last_data_network", "MTN")) }
    var selectedCategoryPill by remember { mutableStateOf("All Plans") }
    var selectedValidityTab by remember { mutableStateOf("All") }

    // Inputs
    var recipientPhoneInput by remember { mutableStateOf(myPhoneNumber) }
    var showPhoneVerificationDialog by remember { mutableStateOf(false) }
    var showFixPhoneDialog by remember { mutableStateOf(false) }
    var fixPhoneInput by remember { mutableStateOf("") }
    var saveRecipientToQuickPay by remember { mutableStateOf(true) }

    // Confirmation & More Info Dialogs
    var planForMoreInfo by remember { mutableStateOf<DataBundleDisplayPlan?>(null) }
    var planForPurchase by remember { mutableStateOf<DataBundleDisplayPlan?>(null) }
    var planForAuth by remember { mutableStateOf<DataBundleDisplayPlan?>(null) }
    var quickRepurchaseTarget by remember { mutableStateOf<RecentDataPurchase?>(null) }

    // Processing & Receipt States
    var isPurchasing by remember { mutableStateOf(false) }
    var purchaseReceipt by remember { mutableStateOf<PurchaseSuccessReceipt?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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

    val vtuMarkupPercent by viewModel.vtuMarkupPercent.collectAsStateWithLifecycle()
    val configuredDataCashbackPct by viewModel.cashbackRateDataPercent.collectAsStateWithLifecycle()
    val effectiveDataCashbackRate = if (configuredDataCashbackPct > 0.0) configuredDataCashbackPct else 1.5
    val dataCashbackLabel = if (effectiveDataCashbackRate % 1.0 == 0.0) "${effectiveDataCashbackRate.toInt()}%" else "$effectiveDataCashbackRate%"
    val dataPricingStrategy by viewModel.dataPricingStrategy.collectAsStateWithLifecycle()
    val telcoDiscountPercent by viewModel.telcoDiscountPercent.collectAsStateWithLifecycle()
    val isFetchingDataPlans by viewModel.isFetchingDataPlans.collectAsStateWithLifecycle()

    // Master Catalog of Plans for the current selected network with category-selective dynamic markup
    val displayPlans = remember(selectedNetwork, livePairgatePlans, vtuMarkupPercent, dataPricingStrategy, telcoDiscountPercent) {
        PairgateCatalogHelper.buildCatalogForNetwork(
            network = selectedNetwork,
            livePlans = livePairgatePlans,
            generalMarkup = vtuMarkupPercent,
            smeMarkup = 8.0,
            cgMarkup = 6.0,
            directMarkup = if (dataPricingStrategy == "FLAT_MARKUP") vtuMarkupPercent else 0.0,
            broadbandMarkup = 4.0,
            socialMarkup = 5.0,
            pricingStrategy = dataPricingStrategy,
            discountPercent = telcoDiscountPercent
        )
    }

    LaunchedEffect(selectedNetwork, displayPlans) {
        val availableCategories = displayPlans.map { it.category }.toSet()
        if (selectedCategoryPill != "All Plans" && selectedCategoryPill != "Favorites" && !availableCategories.contains(selectedCategoryPill)) {
            selectedCategoryPill = "All Plans"
        }
    }

    // Filtered plans based on category & validity
    val filteredPlans = remember(displayPlans, selectedCategoryPill, selectedValidityTab, favoritePlanKeys) {
        val categoryFiltered = when (selectedCategoryPill) {
            "Favorites" -> {
                val favs = displayPlans.filter { plan ->
                    favoritePlanKeys.contains("${plan.network}_${plan.id}") ||
                            favoritePlanKeys.contains(plan.id)
                }
                if (favs.isNotEmpty()) favs else displayPlans.take(4)
            }
            "All Plans" -> {
                displayPlans
            }
            "SME Bundles" -> {
                displayPlans.filter { it.category == "SME Bundles" }
            }
            "CG Bundles" -> {
                displayPlans.filter { it.category == "CG Bundles" }
            }
            "Broadband Router Bundles" -> {
                displayPlans.filter { it.category == "Broadband Router Bundles" }
            }
            "Social Bundles" -> {
                displayPlans.filter { it.category == "Social Bundles" }
            }
            "Gifting", "Data Bundles" -> {
                displayPlans.filter { it.category == "Gifting" || it.category == "Data Bundles" }
            }
            else -> {
                displayPlans
            }
        }

        if (selectedValidityTab == "All" || selectedCategoryPill == "Favorites") {
            categoryFiltered
        } else {
            categoryFiltered.filter { it.validityTab.equals(selectedValidityTab, ignoreCase = true) }
        }
    }

    var searchQuery by remember { mutableStateOf("") }

    val searchedPlans = remember(filteredPlans, displayPlans, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isBlank()) {
            filteredPlans
        } else {
            displayPlans.filter { plan ->
                plan.name.lowercase().contains(q) ||
                plan.description.lowercase().contains(q) ||
                plan.validityText.lowercase().contains(q) ||
                plan.validityTab.lowercase().contains(q) ||
                plan.category.lowercase().contains(q) ||
                plan.infoBreakdown.lowercase().contains(q) ||
                plan.price.toString().contains(q) ||
                plan.id.lowercase().contains(q)
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

    var allowSheetDismiss by remember { mutableStateOf(false) }
    var drawerToast by remember { mutableStateOf<DrawerToastMessage?>(null) }

    fun showDrawerToast(message: String, title: String? = null, type: DrawerToastType = DrawerToastType.INFO) {
        drawerToast = DrawerToastMessage(
            message = message,
            title = title,
            type = type,
            id = System.currentTimeMillis()
        )
    }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { targetValue ->
            if (targetValue == SheetValue.Hidden) {
                allowSheetDismiss
            } else {
                true
            }
        }
    )

    BackHandler(enabled = !isPurchasing) {
        if (purchaseReceipt != null) {
            purchaseReceipt = null
        } else if (planForPurchase != null) {
            planForPurchase = null
        } else if (planForMoreInfo != null) {
            planForMoreInfo = null
        } else {
            allowSheetDismiss = true
            onDismiss()
        }
    }

    LaunchedEffect(purchaseReceipt) {
        if (purchaseReceipt != null) {
            sheetState.expand()
        }
    }

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = {
            if (!isPurchasing) {
                allowSheetDismiss = true
                onDismiss()
            }
        },
        containerColor = DarkObsidian,
        scrimColor = Color.Black.copy(alpha = 0.75f),
        modifier = if (purchaseReceipt != null) Modifier.fillMaxHeight(0.95f) else Modifier,
        dragHandle = null
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
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
            label = "DataBundleReceiptTransition"
        ) { hasReceipt ->
            if (hasReceipt && purchaseReceipt != null) {
            val receiptScrollState = rememberScrollState()
            LaunchedEffect(purchaseReceipt) {
                receiptScrollState.scrollTo(0)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(receiptScrollState)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 64.dp)
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
                // 1. TOP APP BAR: Back Arrow, Centered "Data Bundles" Title, Wallet Balance Chip
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
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = {
                                if (!isPurchasing) {
                                    allowSheetDismiss = true
                                    onDismiss()
                                }
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Text(
                            text = "Data Bundles",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 18.sp
                            ),
                            textAlign = TextAlign.Center
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Wallet Balance Chip
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = DarkSurfaceElevated,
                                border = BorderStroke(1.dp, DarkCardBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(ElectricEmerald)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "₦${String.format("%,.0f", walletBalance)}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = CyberCyan,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }

                            // Close 'X' Button
                            IconButton(
                                onClick = {
                                    if (!isPurchasing) {
                                        allowSheetDismiss = true
                                        onDismiss()
                                    }
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(DarkSurfaceElevated)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close Data Drawer",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // SCROLLABLE CONTENT BODY: All controls & plans in single LazyColumn
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .nestedScroll(blockParentScroll),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    // 2. NETWORK SELECTOR (Like Airtime with authentic TelcoLogos)
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkSurface)
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "SELECT TELECOM NETWORK",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                )
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { viewModel.fetchPairgateDataPlans(forceRefresh = true) }
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isFetchingDataPlans) {
                                        FlowButtonLoadingLine(color = CyberCyan, width = 24.dp, height = 2.dp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Syncing...", fontSize = 11.sp, color = CyberCyan, fontWeight = FontWeight.Bold)
                                    } else {
                                        Icon(Icons.Default.Refresh, contentDescription = "Sync", modifier = Modifier.size(11.dp), tint = CyberCyan)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Sync Live Plans", fontSize = 11.sp, color = CyberCyan, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
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
                                color = if (isSelected) netColor.copy(alpha = 0.18f) else DarkSurfaceElevated,
                                border = BorderStroke(
                                    if (isSelected) 1.5.dp else 1.dp,
                                    if (isSelected) netColor else DarkCardBorder
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
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (isSelected) netColor else netColor.copy(alpha = 0.25f)
                                    ) {
                                        Text(
                                            text = "$dataCashbackLabel back",
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
                }
            }

            // 3. TOP MODE TABS: "Buy For Self" | "Buy For Others" with Theme Underline
            item {
                Surface(
                    color = DarkSurfaceElevated,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        // Buy For Self Tab
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    isBuyForSelf = true
                                    recipientPhoneInput = myPhoneNumber
                                }
                                .padding(top = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Buy For Self",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isBuyForSelf) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isBuyForSelf) CyberCyan else TextMuted,
                                    fontSize = 15.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            // Solid CyberCyan indicator line
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .background(if (isBuyForSelf) CyberCyan else Color.Transparent)
                            )
                        }

                        // Buy For Others Tab
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    isBuyForSelf = false
                                    if (recipientPhoneInput == myPhoneNumber) {
                                        recipientPhoneInput = ""
                                    }
                                }
                                .padding(top = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Buy For Others",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (!isBuyForSelf) FontWeight.Bold else FontWeight.Normal,
                                    color = if (!isBuyForSelf) CyberCyan else TextMuted,
                                    fontSize = 15.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            // Solid CyberCyan indicator line
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .background(if (!isBuyForSelf) CyberCyan else Color.Transparent)
                            )
                        }
                    }
                }
            }

            // Recipient Context / Quick Line Display
            item {
                AnimatedVisibility(visible = isBuyForSelf) {
                    if (myPhoneNumber.isNotBlank()) {
                        val myCarrier = remember(myPhoneNumber) { com.example.data.util.NigerianCarrierDetector.detectCarrier(myPhoneNumber) }
                        val isSelfMismatch = myCarrier != null && com.example.data.util.NigerianCarrierDetector.isMismatch(selectedNetwork, myPhoneNumber)

                        Surface(
                            color = DarkSurface,
                            border = BorderStroke(1.dp, if (isSelfMismatch) WarningRed else DarkCardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.PhoneAndroid,
                                        contentDescription = null,
                                        tint = if (isSelfMismatch) WarningRed else CyberCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isSelfMismatch) "Recipient: $myPhoneNumber (Belongs to $myCarrier, not $selectedNetwork)" else "Recipient: $myPhoneNumber (My Line • ${myCarrier ?: selectedNetwork})",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = if (isSelfMismatch) WarningRed else TextSecondary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                }
                                if (isSelfMismatch && myCarrier != null) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = WarningRed,
                                        modifier = Modifier.clickable { selectedNetwork = myCarrier }
                                    ) {
                                        Text(
                                            text = "SWITCH TO $myCarrier",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                } else {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = CyberCyan.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "Active Line",
                                            color = CyberCyan,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Surface(
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
                                    .padding(horizontal = 16.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(
                                        imageVector = Icons.Default.WarningAmber,
                                        contentDescription = null,
                                        tint = GlowingAmber,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "No Phone Linked • Tap to fix your number",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = GlowingAmber,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = GlowingAmber.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "FIX NUMBER",
                                        color = GlowingAmber,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Buy For Others Input Bar
                AnimatedVisibility(visible = !isBuyForSelf) {
                    Surface(
                        color = DarkSurface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
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
                                label = { Text("Enter Recipient Phone Number", color = TextMuted, fontSize = 12.sp) },
                                placeholder = { Text("08012345678", color = TextMuted.copy(alpha = 0.6f), fontSize = 13.sp) },
                                leadingIcon = {
                                    Icon(Icons.Default.Phone, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                                },
                                trailingIcon = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                val clip = clipboardManager.getText()?.text
                                                if (!clip.isNullOrBlank()) {
                                                    val clean = clip.trim().replace("+234", "0").replace(" ", "").replace("-", "")
                                                    recipientPhoneInput = clean
                                                    val detected = com.example.data.util.NigerianCarrierDetector.detectCarrier(clean)
                                                    if (detected != null) {
                                                        selectedNetwork = detected
                                                    }
                                                }
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                        }
                                        IconButton(
                                            onClick = { launchContactPicker() },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Contacts, contentDescription = "Contacts", tint = CyberCyan, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = CyberCyan,
                                    unfocusedBorderColor = DarkCardBorder,
                                    cursorColor = CyberCyan,
                                    focusedContainerColor = DarkSurfaceElevated,
                                    unfocusedContainerColor = DarkSurfaceElevated
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

                            // Quick Beneficiary Chips
                            if (savedRecipients.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(savedRecipients.take(5)) { recipient: SavedRecipientEntity ->
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = DarkSurfaceElevated,
                                            border = BorderStroke(0.5.dp, DarkCardBorder),
                                            modifier = Modifier.clickable {
                                                recipientPhoneInput = recipient.identifier
                                                val detected = com.example.data.util.NigerianCarrierDetector.detectCarrier(recipient.identifier)
                                                if (detected != null) {
                                                    selectedNetwork = detected
                                                }
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = recipient.name,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = TextPrimary,
                                                        fontSize = 10.sp
                                                    )
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = recipient.identifier.takeLast(4),
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = CyberCyan,
                                                        fontSize = 10.sp
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // SAVE BENEFICIARY / QUICK PAY TOGGLE
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = DarkSurfaceElevated,
                                border = BorderStroke(1.dp, if (saveRecipientToQuickPay) CyberCyan.copy(alpha = 0.5f) else DarkCardBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
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
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
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
                                                text = "Save this number for instant 1-tap recharges",
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
                        }
                    }
                }
            }

            // 4. CATEGORY PILLS ROW (Horizontally Scrollable)
            item {
                val allCategoryPills = listOf(
                    "All Plans" to "All Bundles",
                    "Favorites" to "★ Favorites",
                    "CG Bundles" to "FlowTurbo™ (CG)",
                    "SME Bundles" to "FlowSaver™ (SME)",
                    "Broadband Router Bundles" to "FlowBroadband™",
                    "Social Bundles" to "FlowSocial™",
                    "Gifting" to "FlowDirect™"
                )
                val availableCategorySet = remember(displayPlans) {
                    displayPlans.map { it.category }.toSet()
                }
                val categoryPills = remember(allCategoryPills, availableCategorySet) {
                    allCategoryPills.filter { (catKey, _) ->
                        catKey == "All Plans" || catKey == "Favorites" || availableCategorySet.contains(catKey)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categoryPills.forEach { (catKey, catLabel) ->
                        val isSelected = selectedCategoryPill == catKey
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) CyberCyan.copy(alpha = 0.16f) else DarkSurface,
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) CyberCyan else DarkCardBorder
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { selectedCategoryPill = catKey }
                        ) {
                            Text(
                                text = catLabel,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) CyberCyan else TextSecondary,
                                    fontSize = 13.sp
                                ),
                                modifier = Modifier.padding(horizontal = 15.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            // 5. VALIDITY SUB-TABS: All, Daily, Weekly, Monthly, 2-Months with CyberCyan Underline
            item {
                val validityTabs = listOf("All", "Daily", "Weekly", "Monthly", "2-Months")

                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = DarkObsidian,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            validityTabs.forEach { tab ->
                                val isSelected = selectedValidityTab == tab
                                Column(
                                    modifier = Modifier
                                        .clickable { selectedValidityTab = tab }
                                        .padding(vertical = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = tab,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) TextPrimary else TextMuted,
                                            fontSize = 14.sp
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .width(28.dp)
                                            .height(2.5.dp)
                                            .background(if (isSelected) CyberCyan else Color.Transparent)
                                    )
                                }
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "More Validity Tabs",
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Search Bar
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = DarkSurface,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (searchQuery.isNotBlank()) CyberCyan else DarkCardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
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
                            tint = if (searchQuery.isNotBlank()) CyberCyan else TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
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
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search plans (e.g. 1GB, SME, Monthly, ₦280)...",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = TextMuted,
                                            fontSize = 13.sp
                                        )
                                    )
                                }
                                innerTextField()
                            }
                        )
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
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
            }

            // Error Banner if present
            errorMessage?.let { msg ->
                item {
                    Spacer(modifier = Modifier.height(2.dp))
                    Surface(
                        color = WarningRed.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, WarningRed.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = WarningRed, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = msg, style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, fontSize = 12.sp), modifier = Modifier.weight(1f))
                            IconButton(onClick = { errorMessage = null }, modifier = Modifier.size(20.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = TextPrimary, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }

            // Quick Repurchase Section with authentic TelcoLogos
            if (searchQuery.isBlank() && recentDataPurchases.isNotEmpty() && (selectedCategoryPill == "Favorites" || selectedCategoryPill == "Data Bundles")) {
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        QuickRepurchaseHeaderSection(
                            recentPurchases = recentDataPurchases,
                            onQuickBuy = { recent ->
                                val targetPhone = if (isBuyForSelf) myPhoneNumber else recipientPhoneInput.ifBlank { recent.recipientPhone }
                                quickRepurchaseTarget = recent.copy(recipientPhone = targetPhone)
                            }
                        )
                    }
                }
            }

            if (isFetchingDataPlans) {
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                        ReflectiveBundleListSkeleton(
                            count = 3,
                            statusText = "Syncing live bundles for $selectedNetwork...",
                            subText = "Connecting with upstream carrier network for latest packages",
                            onRetrySync = { viewModel.fetchPairgateDataPlans(forceRefresh = true) }
                        )
                    }
                }
            } else if (searchedPlans.isEmpty()) {
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = DarkSurface,
                            border = BorderStroke(1.dp, DarkCardBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.SearchOff, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(if (searchQuery.isNotBlank()) "No Matching Plans for '$searchQuery'" else "No Plans Found", style = MaterialTheme.typography.titleSmall.copy(color = TextPrimary, fontWeight = FontWeight.Bold))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(if (searchQuery.isNotBlank()) "Try searching another size like 1GB, 2GB, 5GB or clear the search query." else "Try switching validity to 'All' or selecting another category.", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, textAlign = TextAlign.Center))
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            searchQuery = ""
                                            selectedCategoryPill = "All Plans"
                                            selectedValidityTab = "All"
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.White),
                                        shape = RoundedCornerShape(20.dp)
                                    ) {
                                        Text("View All Bundles", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    Button(
                                        onClick = {
                                            viewModel.fetchPairgateDataPlans(forceRefresh = true)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated, contentColor = CyberCyan),
                                        border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f)),
                                        shape = RoundedCornerShape(20.dp)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Sync Live Plans", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                items(searchedPlans, key = { "${it.network}_${it.id}_${it.name}" }) { plan ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        DataBundleCard(
                            plan = plan,
                            onMoreInfo = { planForMoreInfo = plan },
                            onBuyNow = {
                                if (!com.example.data.util.NetworkUtils.isNetworkAvailable(context)) {
                                    showDrawerToast("No network connection. Please check your mobile data or Wi-Fi.", "Offline", DrawerToastType.WARNING)
                                    errorMessage = "No network connection. Please check your mobile data or Wi-Fi to purchase data bundle."
                                    return@DataBundleCard
                                }
                                val targetPhone = if (isBuyForSelf) myPhoneNumber else recipientPhoneInput.trim()
                                if (isBuyForSelf && targetPhone.isBlank()) {
                                    showFixPhoneDialog = true
                                    showDrawerToast("Please link your phone number first", "Phone Required", DrawerToastType.WARNING)
                                    return@DataBundleCard
                                }
                                if (targetPhone.isBlank() || targetPhone.length < 10) {
                                    showDrawerToast("Please enter a valid 11-digit recipient phone number", "Invalid Number", DrawerToastType.WARNING)
                                } else {
                                    planForPurchase = plan
                                }
                            }
                        )
                    }
                }
            }
                }
            }
        }
        }

        TopDrawerNotificationBanner(
            toast = drawerToast,
            onDismiss = { drawerToast = null },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 8.dp)
                .zIndex(9999f)
        )
    }
    }

    // --- DIALOG 1: MORE INFO DIALOG ---
    planForMoreInfo?.let { plan ->
        AlertDialog(
            onDismissRequest = { planForMoreInfo = null },
            containerColor = DarkSurfaceElevated,
            shape = RoundedCornerShape(18.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TelcoLogo(network = plan.network, size = 32.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(text = plan.name, style = MaterialTheme.typography.titleMedium.copy(color = TextPrimary, fontWeight = FontWeight.Bold))
                        Text(text = "${plan.network} • ${plan.validityText}", style = MaterialTheme.typography.labelSmall.copy(color = ElectricEmerald, fontWeight = FontWeight.SemiBold))
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        color = DarkSurface,
                        border = BorderStroke(1.dp, DarkCardBorder),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Package Breakdown", style = MaterialTheme.typography.labelMedium.copy(color = CyberCyan, fontWeight = FontWeight.Bold))
                            Text(plan.infoBreakdown, style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 13.sp))
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Price", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted))
                            Text("₦${plan.price.toInt()}", style = MaterialTheme.typography.titleSmall.copy(color = CyberCyan, fontWeight = FontWeight.Bold))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Balance Check", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted))
                            Text(plan.ussdCode, style = MaterialTheme.typography.titleSmall.copy(color = TextPrimary, fontWeight = FontWeight.Bold))
                        }
                    }

                    Text(
                        text = "Fair usage policies apply. Bundles rollover upon renewal before expiry.",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 11.sp)
                    )
                }
            },
            confirmButton = {
                val netColor = getNetworkColor(plan.network)
                val netContentColor = getNetworkContentColor(plan.network)
                Button(
                    onClick = {
                        val selected = plan
                        planForMoreInfo = null
                        val targetPhone = if (isBuyForSelf) myPhoneNumber else recipientPhoneInput.trim()
                        if (isBuyForSelf && targetPhone.isBlank()) {
                            showFixPhoneDialog = true
                            showDrawerToast("Please link your phone number first", "Phone Required", DrawerToastType.WARNING)
                            return@Button
                        }
                        if (targetPhone.isBlank() || targetPhone.length < 10) {
                            showDrawerToast("Please enter a valid 11-digit recipient phone number", "Invalid Number", DrawerToastType.WARNING)
                        } else {
                            planForPurchase = selected
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = netColor, contentColor = netContentColor),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("Buy Now (₦${plan.price.toInt()})", fontWeight = FontWeight.Bold, color = netContentColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { planForMoreInfo = null }) {
                    Text("Close", color = TextSecondary)
                }
            }
        )
    }

    // --- DIALOG 2: INSTANT PURCHASE CONFIRMATION MODAL ---
    planForPurchase?.let { plan ->
        val effectiveRecipient = if (isBuyForSelf) myPhoneNumber else recipientPhoneInput.trim()
        val hasSufficientBalance = walletBalance >= plan.price
        val detectedRecipientNet = com.example.data.util.NigerianCarrierDetector.detectCarrier(effectiveRecipient)
        val isDataCarrierMismatch = detectedRecipientNet != null && !detectedRecipientNet.equals(plan.network, ignoreCase = true)

        AlertDialog(
            onDismissRequest = { if (!isPurchasing) planForPurchase = null },
            containerColor = DarkSurfaceElevated,
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TelcoLogo(network = plan.network, size = 32.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Confirm Data Purchase",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        color = DarkSurface,
                        border = BorderStroke(1.dp, DarkCardBorder),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Plan", color = TextMuted, fontSize = 12.sp)
                                Text(plan.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Network", color = TextMuted, fontSize = 12.sp)
                                Text(plan.network, color = CyberCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Recipient", color = TextMuted, fontSize = 12.sp)
                                Text(effectiveRecipient, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Validity", color = TextMuted, fontSize = 12.sp)
                                Text(plan.validityText, color = ElectricEmerald, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                            HorizontalDivider(color = DarkCardBorder, thickness = 1.dp)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Amount Payable", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("₦${String.format("%,.2f", plan.price)}", color = CyberCyan, fontWeight = FontWeight.Black, fontSize = 16.sp)
                            }
                        }
                    }

                    // Wallet Balance Status
                    Surface(
                        color = if (hasSufficientBalance) ElectricEmerald.copy(alpha = 0.15f) else WarningRed.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(0.5.dp, if (hasSufficientBalance) ElectricEmerald.copy(alpha = 0.4f) else WarningRed.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "App Wallet: ₦${String.format("%,.2f", walletBalance)}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (hasSufficientBalance) ElectricEmerald else WarningRed,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            )
                            if (!hasSufficientBalance) {
                                Text(
                                    text = "Low Balance",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = WarningRed,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // SAVE BENEFICIARY / QUICK PAY TOGGLE
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DarkSurfaceElevated,
                        border = BorderStroke(1.dp, if (saveRecipientToQuickPay) CyberCyan.copy(alpha = 0.5f) else DarkCardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
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
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
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
                                        text = "Save this number for instant 1-tap recharges",
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

                    if (isDataCarrierMismatch && detectedRecipientNet != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF3B1515),
                            border = BorderStroke(1.2.dp, WarningRed),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("⚠️", fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "CARRIER MISMATCH DETECTED",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = WarningRed,
                                            fontWeight = FontWeight.Black
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Recipient line $effectiveRecipient belongs to $detectedRecipientNet, but this is a ${plan.network.uppercase()} bundle. Upstream will fail or refund this purchase.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFFFCDD2), fontSize = 11.sp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        selectedNetwork = detectedRecipientNet
                                        planForPurchase = null
                                        Toast.makeText(context, "Switched to $detectedRecipientNet data plans", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = WarningRed, contentColor = Color.White),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().height(36.dp)
                                ) {
                                    Text("SWITCH TO ${detectedRecipientNet.uppercase()} PLANS", fontWeight = FontWeight.Black, fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    if (!errorMessage.isNullOrBlank()) {
                        Surface(
                            color = Color(0xFF2E090F),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFFF3B30)),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFFF5252),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = errorMessage!!,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        lineHeight = 16.sp
                                    )
                                )
                            }
                        }
                    }

                    if (isPurchasing) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            FlowLoadingLine(
                                modifier = Modifier.fillMaxWidth(0.85f),
                                color = CyberCyan,
                                trackColor = DarkSurfaceElevated,
                                height = 4.dp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Activating Data Bundle...", color = TextPrimary, fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                val netColor = getNetworkColor(plan.network)
                val netContentColor = getNetworkContentColor(plan.network)
                Button(
                    onClick = {
                        if (!viewModel.isUserPhoneVerified()) {
                            planForPurchase = null
                            showPhoneVerificationDialog = true
                            return@Button
                        }
                        if (isBuyForSelf && myPhoneNumber.isBlank()) {
                            showFixPhoneDialog = true
                            errorMessage = "Please link your phone number before purchasing."
                            return@Button
                        }
                        if (isDataCarrierMismatch && detectedRecipientNet != null) {
                            errorMessage = "Line $effectiveRecipient is a $detectedRecipientNet line, but this is a ${plan.network} bundle. Please switch to $detectedRecipientNet plans to prevent loss."
                            return@Button
                        }
                        if (!com.example.data.util.NetworkUtils.isNetworkAvailable(context)) {
                            showDrawerToast("No network connection. Please check your mobile data or Wi-Fi.", "Offline", DrawerToastType.WARNING)
                            errorMessage = "No network connection. Please check your mobile data or Wi-Fi to complete your purchase."
                            return@Button
                        }
                        if (!hasSufficientBalance) {
                            showDrawerToast("Insufficient wallet balance. Please fund your wallet first.", "Low Balance", DrawerToastType.WARNING)
                            errorMessage = "Insufficient wallet balance (Current: ₦${walletBalance.toInt()}). Please fund your wallet to buy ₦${plan.price.toInt()} bundle."
                            return@Button
                        }
                        val chosenPlan = plan
                        planForPurchase = null
                        planForAuth = chosenPlan
                    },
                    enabled = !isPurchasing,
                    colors = ButtonDefaults.buttonColors(containerColor = netColor, contentColor = netContentColor),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = "Authorize & Pay ₦${plan.price.toInt()}",
                        fontWeight = FontWeight.Black,
                        color = netContentColor
                    )
                }
            },
            dismissButton = {
                if (!isPurchasing) {
                    TextButton(onClick = { planForPurchase = null }) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            }
        )
    }

    // MANDATORY TRANSACTION AUTHORIZATION PAYWALL (BIOMETRIC / PIN)
    planForAuth?.let { plan ->
        val effectiveRecipient = if (isBuyForSelf) myPhoneNumber else recipientPhoneInput.trim()
        TransactionAuthorizationDialog(
            serviceTitle = "${plan.network} Data (${plan.name})",
            recipient = effectiveRecipient,
            amountNaira = plan.price,
            viewModel = viewModel,
            onAuthorized = {
                planForAuth = null
                isPurchasing = true
                errorMessage = null
                coroutineScope.launch {
                    viewModel.purchasePairgateData(
                        network = plan.network,
                        planId = plan.id,
                        amountNaira = plan.price,
                        phone = effectiveRecipient,
                        category = plan.category,
                        planName = plan.name,
                        onResult = { success, msg, ref, isPending ->
                            isPurchasing = false
                            if (success) {
                                viewModel.saveUserOption("last_data_network", plan.network)
                                if (saveRecipientToQuickPay && effectiveRecipient.isNotBlank()) {
                                    viewModel.saveRecipient(
                                        name = if (isBuyForSelf) "My Phone (${plan.network})" else "${plan.network} Data - $effectiveRecipient",
                                        recipientType = "data",
                                        identifier = effectiveRecipient,
                                        institutionOrProvider = plan.network,
                                        isFavorite = true
                                    )
                                }
                                showDrawerToast(
                                    message = msg.ifBlank { "${plan.name} activated for $effectiveRecipient" },
                                    title = if (isPending) "Order Queued" else "Purchase Successful",
                                    type = DrawerToastType.SUCCESS
                                )
                                purchaseReceipt = PurchaseSuccessReceipt(
                                    serviceTitle = "${plan.network} Data",
                                    providerOrType = plan.network,
                                    recipient = effectiveRecipient,
                                    amountPaid = plan.price,
                                    reference = ref ?: ("TRX-" + System.currentTimeMillis()),
                                    newBalance = (walletBalance - plan.price).coerceAtLeast(0.0),
                                    message = msg,
                                    bonusVpnTime = "+2 Hours Unlimited VPN Time",
                                    bonusPoints = 25,
                                    timestamp = java.text.SimpleDateFormat("MMM dd, yyyy • hh:mm a", java.util.Locale.getDefault()).format(java.util.Date()),
                                    balanceBefore = walletBalance,
                                    isPending = isPending,
                                    status = if (isPending) "PENDING" else "SUCCESS"
                                )
                            } else {
                                errorMessage = msg
                                showDrawerToast(
                                    message = msg,
                                    title = if (msg.contains("too many", true) || msg.contains("rate limit", true)) "Upstream Busy" else "Order Declined",
                                    type = DrawerToastType.ERROR
                                )
                            }
                        }
                    )
                }
            },
            onDismiss = {
                planForAuth = null
            }
        )
    }

    // --- DRAWER 3: DIRECT BUY AGAIN DRAWER ---
    quickRepurchaseTarget?.let { recent ->
        DirectDataRepurchaseBottomSheet(
            purchase = recent,
            viewModel = viewModel,
            onDismiss = { quickRepurchaseTarget = null },
            onSuccess = { receipt ->
                quickRepurchaseTarget = null
                purchaseReceipt = receipt
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
                        text = "To recharge data on your line, please enter your primary 11-digit phone number.",
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

/**
 * DataBundleCard
 * Recreates the arrangement shown in the reference screenshot, but with our clean FlowTest dark cyberpunk theme:
 * - Rounded dark card (DarkSurface, DarkCardBorder)
 * - Left: Plan title in crisp white TextPrimary + subtle telco logo badge
 * - Right: Price in bright CyberCyan (e.g. ₦500)
 * - Description: Allowance breakdown in muted slate TextSecondary
 * - Validity label: in ElectricEmerald ("Valid for 7 days")
 * - Bottom Row Action Pills:
 *   - Left: "ⓘ More Info" (DarkSurfaceElevated with DarkCardBorder)
 *   - Right: "Buy Now" (CyberCyan pill with white text)
 */
@Composable
private fun DataBundleCard(
    plan: DataBundleDisplayPlan,
    onMoreInfo: () -> Unit,
    onBuyNow: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = DarkSurface,
        border = BorderStroke(1.dp, DarkCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // TOP ROW: Title on left with tiny carrier chip, Bold CyberCyan Price on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TelcoLogo(network = plan.network, size = 20.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = plan.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 15.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "₦${plan.price.toInt()}",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan,
                        fontSize = 18.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // DESCRIPTION TEXT
            Text(
                text = plan.description,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            // VALIDITY LABEL in ElectricEmerald
            Text(
                text = plan.validityText,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = ElectricEmerald,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // BOTTOM ROW ACTION BUTTONS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Pill Button: "ⓘ More Info"
                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = DarkSurfaceElevated,
                    border = BorderStroke(1.dp, DarkCardBorder),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .clickable { onMoreInfo() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "More Info",
                            tint = CyberCyan,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "More Info",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        )
                    }
                }

                // Right Pill Button: "Buy Now"
                val netColor = getNetworkColor(plan.network)
                val netContentColor = getNetworkContentColor(plan.network)
                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = netColor,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .clickable { onBuyNow() }
                ) {
                    Text(
                        text = "Buy Now",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = netContentColor,
                            fontSize = 13.sp
                        ),
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 9.dp)
                    )
                }
            }
        }
    }
}

/**
 * QuickRepurchaseHeaderSection
 * Displays previous data purchases styled in FlowTest simple "Buy Again" card (Image 1 layout in a carousel)
 */
@Composable
private fun QuickRepurchaseHeaderSection(
    recentPurchases: List<RecentDataPurchase>,
    onQuickBuy: (RecentDataPurchase) -> Unit
) {
    if (recentPurchases.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Buy Again",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 15.sp
                )
            )
            Text(
                text = "1-Tap Direct Purchase",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = CyberCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(recentPurchases.take(6)) { recent: RecentDataPurchase ->
                val periodText = recent.validity.takeIf { it.isNotBlank() } ?: "30 Days"
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = DarkSurface,
                    border = BorderStroke(1.dp, GlassBorder),
                    modifier = Modifier
                        .width(310.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onQuickBuy(recent) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(CyberCyan.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Repeat,
                                    contentDescription = null,
                                    tint = CyberCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${recent.network} Data • ₦${String.format(java.util.Locale.US, "%,.2f", recent.amountNaira)}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        fontSize = 13.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Period: $periodText • To: ${recent.recipientPhone}",
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

                        val repNetColor = getNetworkColor(recent.network)
                        val repNetContentColor = getNetworkContentColor(recent.network)
                        Button(
                            onClick = { onQuickBuy(recent) },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = repNetColor,
                                contentColor = repNetContentColor
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text(
                                text = "Buy again",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = repNetContentColor
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

