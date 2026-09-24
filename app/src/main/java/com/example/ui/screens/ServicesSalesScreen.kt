package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import android.widget.Toast
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.data.ui.viewmodel.VpnViewModel
import com.example.data.util.ContactsHelper
import com.example.data.util.DeviceContact
import com.example.ui.components.FlowButtonLoadingLine
import com.example.ui.components.PhoneVerificationDialog
import com.example.ui.components.PurchaseSuccessReceipt
import com.example.ui.components.TelcoLogo
import com.example.ui.components.TransactionHistoryDialog
import com.example.ui.components.TransactionSuccessReceiptView
import com.example.ui.components.NetworkQualityIndicator
import com.example.ui.components.getNetworkColor
import com.example.ui.components.getNetworkContentColor
import com.example.ui.theme.*

data class BeneficiaryItem(
    val id: String,
    val name: String,
    val phone: String,
    val network: String,
    val initials: String
)

data class AirtimePreset(
    val amount: Int,
    val cashback: Int
)

enum class ServicesSalesStep {
    ENTER_NUMBER,
    ADD_AMOUNT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServicesSalesScreen(
    viewModel: VpnViewModel,
    initialServiceType: String = "airtime", // airtime, data, cable, utility
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val userVirtualAccount by viewModel.userVirtualAccount.collectAsStateWithLifecycle()
    val walletBalance by viewModel.userWalletBalance.collectAsStateWithLifecycle()
    val vtuLogs by viewModel.vtuTransactionLogs.collectAsStateWithLifecycle()
    val networkQuality by viewModel.networkQuality.collectAsStateWithLifecycle()

    var activeStep by remember { mutableStateOf(ServicesSalesStep.ENTER_NUMBER) }
    var selectedNetwork by remember { mutableStateOf("GLO") }
    var phoneNumber by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("500") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterChip by remember { mutableStateOf("Recents") }

    // Drawers (NO POPUPS)
    var showChangeNetworkDrawer by remember { mutableStateOf(false) }
    var showPaymentConfirmDrawer by remember { mutableStateOf(false) }
    var showHistoryDrawer by remember { mutableStateOf(false) }
    var showContactsDrawer by remember { mutableStateOf(false) }
    var showSetupPhoneDrawer by remember { mutableStateOf(false) }
    var newPhoneSetupInput by remember { mutableStateOf("") }
    var phoneSetupError by remember { mutableStateOf<String?>(null) }

    var isProcessingTx by remember { mutableStateOf(false) }
    var txReceiptMsg by remember { mutableStateOf<String?>(null) }
    var purchaseReceipt by remember { mutableStateOf<PurchaseSuccessReceipt?>(null) }
    var directRepurchaseData by remember { mutableStateOf<com.example.data.api.RecentDataPurchase?>(null) }

    // Real Beneficiaries derived from transactions and device contacts
    val context = LocalContext.current
    val deviceContacts by produceState<List<DeviceContact>>(initialValue = ContactsHelper.DEFAULT_FEED_COMMUNITY_USERS) {
        value = withContext(Dispatchers.IO) {
            ContactsHelper.getDeviceContacts(context)
        }
    }

    val beneficiaries = remember(vtuLogs, deviceContacts, userVirtualAccount) {
        val list = mutableListOf<BeneficiaryItem>()
        // From real transaction history
        vtuLogs.filter { it.recipient.isNotBlank() && it.recipient.length >= 10 }
            .distinctBy { it.recipient }
            .take(6)
            .forEachIndexed { idx, log ->
                val net = when {
                    log.type.contains("mtn", ignoreCase = true) -> "MTN"
                    log.type.contains("airtel", ignoreCase = true) -> "Airtel"
                    log.type.contains("glo", ignoreCase = true) -> "Glo"
                    log.type.contains("9mobile", ignoreCase = true) -> "9mobile"
                    else -> "MTN"
                }
                val matched = deviceContacts.firstOrNull {
                    it.phoneNumber.replace(Regex("[^0-9]"), "") == log.recipient.replace(Regex("[^0-9]"), "")
                }
                val displayName = matched?.name ?: "Recipient • ${log.recipient.takeLast(4)}"
                val initials = if (matched != null && matched.name.isNotBlank()) {
                    matched.name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").uppercase()
                } else "TX"
                list.add(BeneficiaryItem(id = "tx_$idx", name = displayName, phone = log.recipient, network = net, initials = initials))
            }
        list
    }

    val filteredBeneficiaries = remember(searchQuery, selectedFilterChip, beneficiaries, deviceContacts) {
        if (selectedFilterChip == "Contacts") {
            val contactItems = deviceContacts.mapIndexed { idx, c ->
                BeneficiaryItem(
                    id = "c_$idx",
                    name = c.name,
                    phone = c.phoneNumber,
                    network = "MTN",
                    initials = c.name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").uppercase().ifEmpty { "C" }
                )
            }
            if (searchQuery.isBlank()) contactItems
            else contactItems.filter {
                it.name.contains(searchQuery, ignoreCase = true) || it.phone.contains(searchQuery)
            }
        } else {
            if (searchQuery.isBlank()) beneficiaries
            else beneficiaries.filter {
                it.name.contains(searchQuery, ignoreCase = true) || it.phone.contains(searchQuery)
            }
        }
    }

    // Presets with Cashback (Matching Screenshot Images 7 & 8)
    val airtimePresets = listOf(
        AirtimePreset(100, 2),
        AirtimePreset(200, 4),
        AirtimePreset(300, 6),
        AirtimePreset(500, 10),
        AirtimePreset(1000, 20),
        AirtimePreset(2000, 40)
    )

    val currentAmountVal = amountText.toIntOrNull() ?: 0
    val calculatedCashback = (currentAmountVal * 0.02).toInt() // 2% Cashback

    val userOwnPhone = remember(userVirtualAccount) {
        userVirtualAccount.phoneNumber.trim()
    }

    Scaffold(
        containerColor = Color(0xFF090D18), // Deep Dark Navy background
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            if (activeStep == ServicesSalesStep.ADD_AMOUNT) {
                                activeStep = ServicesSalesStep.ENTER_NUMBER
                            } else {
                                onNavigateBack()
                            }
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF111C2E))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = if (activeStep == ServicesSalesStep.ENTER_NUMBER) "Buy Airtime" else "Add amount",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 18.sp
                        )
                    )
                }

                // Top Right Clock/History Icon
                IconButton(
                    onClick = { showHistoryDrawer = true },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF111C2E))
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "History Logs",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            NetworkQualityIndicator(
                networkQuality = networkQuality,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            )

            when (activeStep) {
                ServicesSalesStep.ENTER_NUMBER -> {
                    // --- STEP 1: ENTER PHONE NUMBER & SELECT BENEFICIARY ---

                    // 1. Purple Cashback Banner
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF28183E)), // Deep purple
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🎉  Enjoy up to 2% cashback on all airtime purchases",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFE9D5FF),
                                    fontSize = 13.sp
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 1.5 BUY AGAIN CAROUSEL (Image 1 simple design with carousel of recent purchases)
                    val recentDataPurchases by viewModel.recentDataPurchases.collectAsStateWithLifecycle()
                    if (recentDataPurchases.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Buy Again",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
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
                                items(recentDataPurchases.take(6)) { recent ->
                                    val periodText = recent.validity.takeIf { it.isNotBlank() } ?: "30 Days"
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = Color(0xFF111C2E),
                                        border = BorderStroke(1.dp, Color(0xFF1E293B)),
                                        modifier = Modifier
                                            .width(310.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .clickable { directRepurchaseData = recent }
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
                                                            color = Color.White,
                                                            fontSize = 13.sp
                                                        ),
                                                        maxLines = 1,
                                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = "Period: $periodText • To: ${recent.recipientPhone}",
                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                            color = Color(0xFF94A3B8),
                                                            fontSize = 11.sp
                                                        ),
                                                        maxLines = 1,
                                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            val repNetColor = getNetworkColor(recent.network)
                                            val repNetContentColor = getNetworkContentColor(recent.network)
                                            Button(
                                                onClick = { directRepurchaseData = recent },
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
                                                        color = repNetContentColor,
                                                        fontSize = 11.sp
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }

                    // 2. "Enter phone number" Card Container
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF111C2E)),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Enter phone number",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 15.sp
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Phone input field with contact icon
                            OutlinedTextField(
                                value = phoneNumber,
                                onValueChange = { phoneNumber = it },
                                placeholder = { Text("0803 000 0000", color = Color(0xFF64748B)) },
                                singleLine = true,
                                trailingIcon = {
                                    IconButton(onClick = { showContactsDrawer = true }) {
                                        Text("📇", fontSize = 20.sp)
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFF090D18),
                                    unfocusedContainerColor = Color(0xFF090D18),
                                    focusedBorderColor = CyberCyan,
                                    unfocusedBorderColor = Color(0xFF1E293B),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            if (userOwnPhone.isNotBlank()) {
                                // "Buy For Self • My Real Number >" Quick Button
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF1E293B),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            phoneNumber = userOwnPhone.replace(" ", "")
                                            activeStep = ServicesSalesStep.ADD_AMOUNT
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.PhoneAndroid,
                                                contentDescription = "My Phone",
                                                tint = CyberCyan,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = "Buy For Self  •  $userOwnPhone",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    color = Color.White,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 13.sp
                                                )
                                            )
                                        }

                                        Icon(
                                            imageVector = Icons.Default.ChevronRight,
                                            contentDescription = "Select",
                                            tint = Color(0xFF94A3B8),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            } else {
                                // Prompt to set up real primary phone number
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF141F31),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.45f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            newPhoneSetupInput = ""
                                            phoneSetupError = null
                                            showSetupPhoneDrawer = true
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(CyberCyan.copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = "Set Up Phone",
                                                    tint = CyberCyan,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = "No Primary Phone Number Set",
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.5.sp
                                                    )
                                                )
                                                Text(
                                                    text = "Tap to link your phone for quick 1-tap recharges",
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        color = Color(0xFF94A3B8),
                                                        fontSize = 10.5.sp
                                                    )
                                                )
                                            }
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = CyberCyan
                                        ) {
                                            Text(
                                                text = "+ SET UP",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 10.sp,
                                                    color = DarkObsidian
                                                ),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            if (phoneNumber.length >= 10) {
                                Spacer(modifier = Modifier.height(14.dp))

                                val netBtnColor = getNetworkColor(selectedNetwork)
                                val netContentColor = getNetworkContentColor(selectedNetwork)
                                Button(
                                    onClick = { activeStep = ServicesSalesStep.ADD_AMOUNT },
                                    colors = ButtonDefaults.buttonColors(containerColor = netBtnColor, contentColor = netContentColor),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("PROCEED WITH $phoneNumber", color = netContentColor, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 3. "Select beneficiary" Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                        border = BorderStroke(1.dp, DarkCardBorder),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Select beneficiary",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 15.sp
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Search bar
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search name or phone number", color = Color(0xFF64748B), fontSize = 13.sp) },
                                singleLine = true,
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search",
                                        tint = Color(0xFF64748B)
                                    )
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFF090D18),
                                    unfocusedContainerColor = Color(0xFF090D18),
                                    focusedBorderColor = CyberCyan,
                                    unfocusedBorderColor = Color(0xFF1E293B),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Filter Pills (Recents, Contacts, All Beneficiaries)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = if (selectedFilterChip == "Recents") CyberCyan else DarkSurface,
                                        modifier = Modifier.clickable { selectedFilterChip = "Recents" }
                                    ) {
                                        Text(
                                            text = "Recents",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                color = if (selectedFilterChip == "Recents") DarkObsidian else Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            ),
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = if (selectedFilterChip == "Contacts") CyberCyan else DarkSurface,
                                        modifier = Modifier.clickable { selectedFilterChip = "Contacts" }
                                    ) {
                                        Text(
                                            text = "Contacts",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                color = if (selectedFilterChip == "Contacts") DarkObsidian else Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            ),
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "All Beneficiaries >",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = CyberCyan,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    ),
                                    modifier = Modifier
                                        .clickable { showContactsDrawer = true }
                                        .padding(4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            if (filteredBeneficiaries.isEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF0F172A),
                                    border = BorderStroke(1.dp, Color(0xFF1E293B)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = if (selectedFilterChip == "Contacts") "No matching device contacts found." else "No recent beneficiaries yet.",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = Color(0xFF94A3B8),
                                                fontSize = 13.sp
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Enter a phone number above or pick from contacts to vend.",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Color(0xFF64748B),
                                                fontSize = 11.sp
                                            )
                                        )
                                    }
                                }
                            } else {
                                // Beneficiary Items List
                                filteredBeneficiaries.forEach { beneficiary ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                phoneNumber = beneficiary.phone
                                                selectedNetwork = beneficiary.network.uppercase()
                                                activeStep = ServicesSalesStep.ADD_AMOUNT
                                            }
                                            .padding(vertical = 10.dp, horizontal = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Avatar Circle with Network Overlay Badge
                                        Box(modifier = Modifier.size(44.dp)) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF1E3A8A)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = beneficiary.initials,
                                                    style = MaterialTheme.typography.titleSmall.copy(
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                )
                                            }

                                            // Small Telco Badge overlay
                                            Box(
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .align(Alignment.BottomEnd)
                                            ) {
                                                TelcoLogo(network = beneficiary.network, size = 18.dp)
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(14.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = beneficiary.name,
                                                style = MaterialTheme.typography.titleSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    fontSize = 14.sp
                                                )
                                            )
                                            Text(
                                                text = "${beneficiary.phone}  •  ${beneficiary.network}",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = Color(0xFF94A3B8),
                                                    fontSize = 12.sp
                                                )
                                            )
                                        }

                                        Icon(
                                            imageVector = Icons.Default.ChevronRight,
                                            contentDescription = "Select",
                                            tint = Color(0xFF64748B),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    HorizontalDivider(color = Color(0xFF1E293B), thickness = 0.5.dp)
                                }
                            }
                        }
                    }
                }

                ServicesSalesStep.ADD_AMOUNT -> {
                    // --- STEP 2: ADD AMOUNT & SELECT PRESET (Images 7 & 8) ---

                    // Network Subheader Row (Clickable to change network drawer)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF111C2E),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp)
                            .clickable { showChangeNetworkDrawer = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TelcoLogo(network = selectedNetwork, size = 32.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "$selectedNetwork  v  ${if (phoneNumber.isNotBlank()) phoneNumber else userOwnPhone}",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Change Network",
                                tint = CyberCyan,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Amount Input Container
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                        border = BorderStroke(1.dp, DarkCardBorder),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Allowed limit: ₦100 - ₦500,000",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Large Currency Text Field
                            OutlinedTextField(
                                value = amountText,
                                onValueChange = {
                                    if (it.all { char -> char.isDigit() }) {
                                        amountText = it
                                    }
                                },
                                prefix = {
                                    Text(
                                        text = "₦ ",
                                        style = MaterialTheme.typography.headlineLarge.copy(
                                            fontWeight = FontWeight.Black,
                                            color = Color.White,
                                            fontSize = 28.sp
                                        )
                                    )
                                },
                                textStyle = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    fontSize = 28.sp
                                ),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedBorderColor = CyberCyan,
                                    unfocusedBorderColor = Color(0xFF1E293B)
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Purple Cashback pill tag
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF28183E)
                            ) {
                                Text(
                                    text = "Cashback: ₦$calculatedCashback",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = Color(0xFFE9D5FF),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 3x2 Grid of Presets (Matching Image 7 & 8)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val row1 = airtimePresets.take(3)
                        val row2 = airtimePresets.drop(3)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            row1.forEach { preset ->
                                AirtimePresetTile(
                                    preset = preset,
                                    isSelected = amountText == preset.amount.toString(),
                                    onClick = { amountText = preset.amount.toString() },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            row2.forEach { preset ->
                                AirtimePresetTile(
                                    preset = preset,
                                    isSelected = amountText == preset.amount.toString(),
                                    onClick = { amountText = preset.amount.toString() },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Primary Action Button "Continue"
                    val netBtnColor = getNetworkColor(selectedNetwork)
                    val netContentColor = getNetworkContentColor(selectedNetwork)
                    Button(
                        onClick = { showPaymentConfirmDrawer = true },
                        enabled = currentAmountVal >= 100,
                        colors = ButtonDefaults.buttonColors(containerColor = netBtnColor, contentColor = netContentColor),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(
                            text = "Continue",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = netContentColor,
                                fontSize = 16.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // --- DRAWER 1: CHANGE MOBILE NETWORK DRAWER (NO POPUPS) ---
    if (showChangeNetworkDrawer) {
        ModalBottomSheet(
            onDismissRequest = { showChangeNetworkDrawer = false },
            containerColor = Color(0xFF111C2E),
            scrimColor = Color.Black.copy(alpha = 0.6f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
            ) {
                // Drawer Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Change mobile network",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 18.sp
                        )
                    )

                    IconButton(onClick = { showChangeNetworkDrawer = false }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF94A3B8))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Network Items List (Matching Image 5 & 6)
                val networks = listOf(
                    Triple("MTN", "MTN", false),
                    Triple("AIRTEL", "AIRTEL", false),
                    Triple("T2(9Mobile)", "9MOBILE", false),
                    Triple("GLO", "GLO", true), // Suggested
                    Triple("VITEL WIRELESS", "VITEL", false)
                )

                networks.forEach { (displayName, netCode, isSuggested) ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF090D18),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                selectedNetwork = netCode
                                showChangeNetworkDrawer = false
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TelcoLogo(network = netCode, size = 36.dp)

                                Spacer(modifier = Modifier.width(14.dp))

                                Text(
                                    text = displayName,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 15.sp
                                    )
                                )

                                if (isSuggested) {
                                    Spacer(modifier = Modifier.width(10.dp))

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFF1E3A8A)
                                    ) {
                                        Text(
                                            text = "✓ Suggested",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Color(0xFF60A5FA),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp
                                            ),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }

                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Select",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // --- DRAWER 2: PAYMENT & RECIPIENT NUMBER SAFETY CHECK DRAWER (NO POPUPS) ---
    if (showPaymentConfirmDrawer) {
        val targetPhone = if (phoneNumber.isNotBlank()) phoneNumber else userOwnPhone

        val confirmSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        LaunchedEffect(purchaseReceipt) {
            if (purchaseReceipt != null) {
                confirmSheetState.expand()
            }
        }

        ModalBottomSheet(
            sheetState = confirmSheetState,
            onDismissRequest = { if (!isProcessingTx) showPaymentConfirmDrawer = false },
            containerColor = Color(0xFF111C2E),
            scrimColor = Color.Black.copy(alpha = 0.7f),
            modifier = if (purchaseReceipt != null) Modifier.fillMaxHeight(0.95f) else Modifier,
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
            val drawerScrollState = rememberScrollState()
            LaunchedEffect(purchaseReceipt) {
                if (purchaseReceipt != null) {
                    drawerScrollState.scrollTo(0)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(drawerScrollState)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 64.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (purchaseReceipt != null) "Transaction Receipt 🎉" else "Review & Check Number",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            fontSize = 18.sp
                        )
                    )

                    IconButton(onClick = { if (!isProcessingTx) showPaymentConfirmDrawer = false }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF94A3B8))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                purchaseReceipt?.let { receipt ->
                    TransactionSuccessReceiptView(
                        receipt = receipt,
                        onReturnHome = {
                            showPaymentConfirmDrawer = false
                            purchaseReceipt = null
                            phoneNumber = ""
                            onNavigateBack()
                        },
                        onMakeAnotherPurchase = {
                            showPaymentConfirmDrawer = false
                            purchaseReceipt = null
                            activeStep = ServicesSalesStep.ENTER_NUMBER
                            phoneNumber = ""
                        }
                    )
                } ?: txReceiptMsg?.let { errorMsg ->
                    val clipboardManager = LocalClipboardManager.current
                    val context = LocalContext.current
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2E1616)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF5252)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "⚠️ Transaction Alert",
                                    style = MaterialTheme.typography.titleSmall.copy(color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)
                                )
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(errorMsg))
                                        Toast.makeText(context, "Error message copied to clipboard", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy Error Message",
                                        tint = Color(0xFFFF8A80),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(errorMsg, style = MaterialTheme.typography.bodySmall.copy(color = Color.White), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(errorMsg))
                                        Toast.makeText(context, "Error message copied to clipboard", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF8A80)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF8A80).copy(alpha = 0.6f)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp), tint = Color(0xFFFF8A80))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Copy Error", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { txReceiptMsg = null },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252), contentColor = Color.White),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Try Again", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } ?: run {
                    // 1. High-Signal Warning Alert Banner
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF2B1C05),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF59E0B).copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("⚠️", fontSize = 18.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "DOUBLE-CHECK RECIPIENT NUMBER",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        color = CyberCyan,
                                        fontSize = 11.sp,
                                        letterSpacing = 0.5.sp
                                    )
                                )
                                Text(
                                    text = "Please verify that this is your intended number. Recharges to wrong lines cannot be reversed or refunded once dispatched.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFFE2E8F0),
                                        fontSize = 11.sp,
                                        lineHeight = 14.sp
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 2. High-Visibility Highlighted Recipient Phone Number Box
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF070B14),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, CyberCyan),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "TARGET RECIPIENT MSISDN",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF94A3B8),
                                    fontSize = 10.sp,
                                    letterSpacing = 1.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = targetPhone.ifEmpty { "No Number Specified" },
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = CyberCyan,
                                    letterSpacing = 2.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 24.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                TelcoLogo(network = selectedNetwork, size = 18.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "$selectedNetwork Network • ${if (targetPhone == userOwnPhone && userOwnPhone.isNotEmpty()) "My Primary Number" else "Direct Recipient"}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3. Transaction Breakdown Summary
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                        border = BorderStroke(1.dp, DarkCardBorder),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Network Carrier", color = Color(0xFF94A3B8), fontSize = 13.sp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TelcoLogo(network = selectedNetwork, size = 20.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(selectedNetwork, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Recharge Amount", color = Color(0xFF94A3B8), fontSize = 13.sp)
                                Text("₦$currentAmountVal", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Cashback Earned", color = Color(0xFF94A3B8), fontSize = 13.sp)
                                Text("+ ₦$calculatedCashback", color = Color(0xFFE9D5FF), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Wallet Balance", color = Color(0xFF94A3B8), fontSize = 13.sp)
                                Text("₦${String.format("%,.2f", walletBalance)}", color = Color(0xFF38BDF8), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            HorizontalDivider(color = DarkCardBorder)

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Wallet Debit Total", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("₦$currentAmountVal", color = CyberCyan, fontWeight = FontWeight.Black, fontSize = 16.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // 4. Two Buttons: "Edit / Fix Number" vs "Confirm & Pay"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                showPaymentConfirmDrawer = false
                                activeStep = ServicesSalesStep.ENTER_NUMBER
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Edit Number", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                isProcessingTx = true
                                viewModel.vendAirtimeWithEngine(
                                    providerId = selectedNetwork,
                                    phone = targetPhone,
                                    requestedAmount = currentAmountVal.toDouble(),
                                    onResult = { res ->
                                        isProcessingTx = false
                                        if (res.isSuccess) {
                                            txReceiptMsg = null
                                            purchaseReceipt = PurchaseSuccessReceipt(
                                                serviceTitle = "Airtime Vending",
                                                providerOrType = selectedNetwork,
                                                recipient = targetPhone,
                                                amountPaid = currentAmountVal.toDouble(),
                                                reference = res.transactionId ?: "PG-AIR-${(100000..999999).random()}",
                                                newBalance = res.newWalletBalance,
                                                message = res.message,
                                                bonusVpnTime = "+1 Hour Unlimited VPN Time",
                                                bonusPoints = (currentAmountVal / 50).coerceAtLeast(10)
                                            )
                                        } else {
                                            txReceiptMsg = "Transaction Failed: ${res.message}"
                                        }
                                    }
                                )
                            },
                            enabled = !isProcessingTx && walletBalance >= currentAmountVal && targetPhone.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = getNetworkColor(selectedNetwork),
                                contentColor = getNetworkContentColor(selectedNetwork)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(50.dp)
                        ) {
                            val netContentColor = getNetworkContentColor(selectedNetwork)
                            if (isProcessingTx) {
                                FlowButtonLoadingLine(color = netContentColor, width = 32.dp, height = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Vending...", color = netContentColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            } else {
                                Text("Confirm & Send", color = netContentColor, fontWeight = FontWeight.Black, fontSize = 13.5.sp)
                            }
                        }
                    }

                    if (walletBalance < currentAmountVal) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Insufficient balance (Wallet: ₦${String.format("%.2f", walletBalance)}). Please fund your wallet.",
                            color = Color(0xFFFF5252),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }

    // --- DRAWER: SETUP USER REAL PHONE NUMBER & VERIFICATION DIALOG ---
    if (showSetupPhoneDrawer) {
        PhoneVerificationDialog(
            viewModel = viewModel,
            initialPhone = newPhoneSetupInput.ifBlank { phoneNumber },
            onDismiss = { showSetupPhoneDrawer = false },
            onSuccess = { verified ->
                phoneNumber = verified
                newPhoneSetupInput = verified
                showSetupPhoneDrawer = false
            }
        )
    }

    // --- DRAWER 3: CONTACTS / BENEFICIARIES SELECTION DRAWER ---
    if (showContactsDrawer) {
        ModalBottomSheet(
            onDismissRequest = { showContactsDrawer = false },
            containerColor = Color(0xFF111C2E),
            scrimColor = Color.Black.copy(alpha = 0.6f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "Select From Saved Contacts",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                )

                Spacer(modifier = Modifier.height(16.dp))

                beneficiaries.forEach { contact ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                phoneNumber = contact.phone
                                selectedNetwork = contact.network.uppercase()
                                showContactsDrawer = false
                                activeStep = ServicesSalesStep.ADD_AMOUNT
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TelcoLogo(network = contact.network, size = 36.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(contact.name, color = Color.White, fontWeight = FontWeight.Bold)
                            Text("${contact.phone} • ${contact.network}", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        }
                    }
                    HorizontalDivider(color = Color(0xFF1E293B))
                }
            }
        }
    }

    // --- DRAWER 3.5: DIRECT BUY AGAIN PURCHASE DRAWER ---
    directRepurchaseData?.let { rep ->
        com.example.ui.components.DirectDataRepurchaseBottomSheet(
            purchase = rep,
            viewModel = viewModel,
            onDismiss = { directRepurchaseData = null },
            onSuccess = { receipt ->
                directRepurchaseData = null
                purchaseReceipt = receipt
                showPaymentConfirmDrawer = true
            }
        )
    }

    // --- DRAWER 4: TRANSACTION HISTORY DRAWER ---
    if (showHistoryDrawer) {
        TransactionHistoryDialog(
            logs = vtuLogs,
            viewModel = viewModel,
            onDismiss = { showHistoryDrawer = false }
        )
    }
}

/**
 * 3x2 Preset Tile Composable matching Image 7 & 8
 */
@Composable
fun AirtimePresetTile(
    preset: AirtimePreset,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(80.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) CyberCyan.copy(alpha = 0.15f) else DarkSurfaceElevated
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) CyberCyan else DarkCardBorder
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Purple cashback tag
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF382352)
            ) {
                Text(
                    text = "₦${preset.cashback} cashback",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFFE9D5FF),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    ),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "₦${preset.amount}",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    fontSize = 16.sp
                )
            )
        }
    }
}
