package com.example.ui.components.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.db.ClientAccountEntity
import com.example.data.ui.viewmodel.VpnViewModel
import com.example.ui.components.FlowButtonLoadingLine
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminClientsTab(
    viewModel: VpnViewModel,
    onIssueNubanClick: () -> Unit = {},
    onShowStatusMsg: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val allClientAccounts by viewModel.allClientAccounts.collectAsStateWithLifecycle()
    val inboundNotifs by viewModel.adminInboundNotifications.collectAsStateWithLifecycle()
    val isSyncingAdmin by viewModel.isSyncingAdminData.collectAsStateWithLifecycle()
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }

    // Admin Direct Communication Dialog State
    var showMessageUserDialog by remember { mutableStateOf(false) }
    var messageTargetUser by remember { mutableStateOf<ClientAccountEntity?>(null) }
    var messageTargetPhoneInput by remember { mutableStateOf("") }
    var messageTargetEmailInput by remember { mutableStateOf("") }
    var messageTitleInput by remember { mutableStateOf("Account Notification") }
    var messageBodyInput by remember { mutableStateOf("") }
    var messageSendSms by remember { mutableStateOf(true) }
    var isSendingAdminMessage by remember { mutableStateOf(false) }
    var messageFeedbackMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.syncAdminClients()
        viewModel.fetchAdminInboundNotifications()
    }

    // Admin Send Service to User Dialog State
    var showSendServiceDialog by remember { mutableStateOf(false) }
    var selectedTargetUser by remember { mutableStateOf<ClientAccountEntity?>(null) }
    var targetPhoneInput by remember { mutableStateOf("") }
    var selectedServiceCategory by remember { mutableStateOf("DATA") } // DATA, AIRTIME, WALLET_CREDIT, CABLE, ELECTRICITY
    var selectedPlanOrProvider by remember { mutableStateOf("mtn_sme_1gb") }
    var customServiceAmount by remember { mutableStateOf("1000") }
    var isDispatchingService by remember { mutableStateOf(false) }
    var serviceFeedbackMsg by remember { mutableStateOf<String?>(null) }

    // Admin Create User Dialog State
    var showCreateUserDialog by remember { mutableStateOf(false) }
    var newUserNameInput by remember { mutableStateOf("") }
    var newUserEmailInput by remember { mutableStateOf("") }
    var newUserPhoneInput by remember { mutableStateOf("") }
    var newUserBalanceInput by remember { mutableStateOf("0") }
    var newUserRoleInput by remember { mutableStateOf("USER") }
    var newUserPinInput by remember { mutableStateOf("1234") }
    var isCreatingUser by remember { mutableStateOf(false) }
    var createUserError by remember { mutableStateOf<String?>(null) }

    // Admin Manage User Dialog State
    var showManageUserDialog by remember { mutableStateOf(false) }
    var userToManage by remember { mutableStateOf<ClientAccountEntity?>(null) }
    var adjustBalanceAmountInput by remember { mutableStateOf("") }
    var adjustBalanceReasonInput by remember { mutableStateOf("Admin Manual Adjustment") }
    var isAdjustingBalance by remember { mutableStateOf(false) }
    var resetPinInput by remember { mutableStateOf("") }
    var isResettingPin by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var isDeletingUser by remember { mutableStateOf(false) }
    var manageUserFeedbackMsg by remember { mutableStateOf<String?>(null) }

    val filteredAccounts = remember(allClientAccounts, searchQuery) {
        val deduplicated = allClientAccounts
            .groupBy { acc ->
                val cleanEmail = acc.customerEmail.trim().lowercase()
                val digits = acc.customerPhone.replace(Regex("[^0-9]"), "")
                if (cleanEmail == "innobright2010@gmail.com" || acc.role.equals("ADMIN", ignoreCase = true) || acc.id == "usr_admin_innobright2010") {
                    "ADMIN_CANONICAL"
                } else if (digits.length >= 10) {
                    "PHONE_${digits.takeLast(10)}"
                } else if (cleanEmail.isNotBlank()) {
                    "EMAIL_$cleanEmail"
                } else {
                    "ID_${acc.id}"
                }
            }
            .values
            .map { list ->
                if (list.size == 1) {
                    list.first()
                } else {
                    val maxBal = list.maxOfOrNull { it.walletBalance } ?: 0.0
                    val maxFunded = list.maxOfOrNull { it.totalFunded } ?: 0.0
                    val isAdmin = list.any { it.role.equals("ADMIN", ignoreCase = true) || it.customerEmail.equals("innobright2010@gmail.com", ignoreCase = true) }
                    val primary = list.first()
                    primary.copy(
                        id = if (isAdmin) "usr_admin_innobright2010" else primary.id,
                        role = if (isAdmin) "ADMIN" else primary.role,
                        customerName = if (isAdmin && (primary.customerName.isBlank() || primary.customerName == "FlowTest User")) "Innocent Aimiebe Omodiale" else primary.customerName,
                        walletBalance = maxBal,
                        totalFunded = maxFunded
                    )
                }
            }
            .sortedWith(
                compareByDescending<ClientAccountEntity> { it.role.equals("ADMIN", ignoreCase = true) || it.customerEmail.equals("innobright2010@gmail.com", ignoreCase = true) }
                    .thenByDescending { it.createdAt }
            )

        if (searchQuery.isBlank()) {
            deduplicated
        } else {
            val q = searchQuery.trim()
            deduplicated.filter { acc ->
                acc.customerName.contains(q, ignoreCase = true) ||
                acc.customerEmail.contains(q, ignoreCase = true) ||
                acc.customerPhone.contains(q, ignoreCase = true) ||
                acc.id.contains(q, ignoreCase = true)
            }
        }
    }

    fun openSendServiceModal(acc: ClientAccountEntity?) {
        selectedTargetUser = acc
        targetPhoneInput = acc?.customerPhone ?: ""
        selectedServiceCategory = "DATA"
        selectedPlanOrProvider = "mtn_sme_1gb"
        customServiceAmount = "1000"
        serviceFeedbackMsg = null
        showSendServiceDialog = true
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, ElectricEmerald.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
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
                        Icon(imageVector = Icons.Default.SupervisorAccount, contentDescription = "Registered Users", tint = ElectricEmerald)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "REGISTERED USERS & CLIENT DESK",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${filteredAccounts.size} Registered Customer Profiles",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 10.sp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = {
                                viewModel.syncAdminClients { count ->
                                    onShowStatusMsg("Synced $count user accounts from Cloud.")
                                }
                                viewModel.fetchAdminInboundNotifications()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricEmerald, contentColor = DarkObsidian),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            enabled = !isSyncingAdmin
                        ) {
                            Icon(imageVector = Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (isSyncingAdmin) "SYNCING..." else "SYNC CLOUD",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Button(
                            onClick = {
                                newUserNameInput = ""
                                newUserEmailInput = ""
                                newUserPhoneInput = ""
                                newUserBalanceInput = "0"
                                newUserRoleInput = "USER"
                                newUserPinInput = "1234"
                                createUserError = null
                                showCreateUserDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "ADD USER",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Button(
                            onClick = { openSendServiceModal(null) },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "SEND SERVICE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Track all active customer accounts and instantly send/vend data bundles, airtime, utilities, or wallet credits to any user.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
                )

                // Inbound Deposit Notifications & Stalled Deposit Review Banner
                if (inboundNotifs.isNotEmpty()) {
                    val unresolved = inboundNotifs.filter { it.status.uppercase() == "UNRESOLVED" || it.status.uppercase() == "PENDING" }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = GlowingAmber.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, GlowingAmber.copy(alpha = 0.4f)),
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
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = null,
                                        tint = GlowingAmber,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "INBOUND NOTIFICATIONS & DEPOSIT ALERTS",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Black,
                                            color = GlowingAmber,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (unresolved.isNotEmpty()) GlowingAmber else ElectricEmerald
                                ) {
                                    Text(
                                        text = "${unresolved.size} Pending Review",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = DarkObsidian,
                                            fontSize = 10.sp
                                        ),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            if (unresolved.isEmpty()) {
                                Text(
                                    text = "All reported deposits have been verified and settled.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 11.sp)
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    unresolved.forEach { alert ->
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = DarkSurface,
                                            border = BorderStroke(1.dp, DarkCardBorder),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.Top
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = "₦${String.format(java.util.Locale.US, "%,.2f", alert.amount)} • ${alert.senderName}",
                                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                                fontWeight = FontWeight.Bold,
                                                                color = ElectricEmerald
                                                            )
                                                        )
                                                        Text(
                                                            text = "Phone: ${alert.phone} • Code/Ref: ${alert.reference}",
                                                            style = MaterialTheme.typography.bodySmall.copy(
                                                                fontFamily = FontFamily.Monospace,
                                                                color = CyberCyan,
                                                                fontSize = 11.sp
                                                            )
                                                        )
                                                        if (alert.userNote.isNotBlank()) {
                                                            Text(
                                                                text = "Note: ${alert.userNote}",
                                                                style = MaterialTheme.typography.bodySmall.copy(
                                                                    color = TextSecondary,
                                                                    fontSize = 10.sp
                                                                )
                                                            )
                                                        }
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(8.dp))

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    // CREDIT USER BUTTON
                                                    Button(
                                                        onClick = {
                                                            viewModel.resolveAdminNotification(
                                                                notificationId = alert.id,
                                                                action = "CREDIT_WALLET",
                                                                notes = "Approved by Admin Desk",
                                                                creditedAmount = alert.amount,
                                                                targetPhone = alert.phone
                                                            ) { ok, msg ->
                                                                onShowStatusMsg(msg)
                                                            }
                                                        },
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = ElectricEmerald,
                                                            contentColor = DarkObsidian
                                                        ),
                                                        shape = RoundedCornerShape(6.dp),
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(12.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = "CREDIT USER",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Black
                                                        )
                                                    }

                                                    // MESSAGE USER BUTTON
                                                    OutlinedButton(
                                                        onClick = {
                                                            messageTargetUser = allClientAccounts.firstOrNull { it.customerPhone == alert.phone }
                                                            messageTargetPhoneInput = alert.phone
                                                            messageTargetEmailInput = ""
                                                            messageTitleInput = "Deposit Update (Ref: ${alert.reference})"
                                                            messageBodyInput = "Your deposit of ₦${String.format(java.util.Locale.US, "%,.2f", alert.amount)} (Ref: ${alert.reference}) has been received and reviewed by Admin."
                                                            messageFeedbackMsg = null
                                                            showMessageUserDialog = true
                                                        },
                                                        shape = RoundedCornerShape(6.dp),
                                                        border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f)),
                                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                                    ) {
                                                        Icon(imageVector = Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(12.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("MESSAGE", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                    }

                                                    // DISMISS BUTTON
                                                    OutlinedButton(
                                                        onClick = {
                                                            viewModel.resolveAdminNotification(
                                                                notificationId = alert.id,
                                                                action = "DISMISS",
                                                                notes = "Dismissed by Admin Desk"
                                                            ) { ok, msg ->
                                                                onShowStatusMsg(msg)
                                                            }
                                                        },
                                                        shape = RoundedCornerShape(6.dp),
                                                        border = BorderStroke(1.dp, WarningRed.copy(alpha = 0.4f)),
                                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = WarningRed),
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                                    ) {
                                                        Text("DISMISS", fontSize = 10.sp, fontWeight = FontWeight.Bold)
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

                Spacer(modifier = Modifier.height(12.dp))

                // Search Box
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by name, email, or phone number...", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = CyberCyan, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = TextMuted, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricEmerald,
                        unfocusedBorderColor = DarkCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (filteredAccounts.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = DarkSurface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(imageVector = Icons.Default.PersonSearch, contentDescription = null, tint = TextMuted, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (searchQuery.isBlank()) "No registered users found" else "No users found matching \"$searchQuery\"",
                                color = TextMuted,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                } else {
                    filteredAccounts.forEach { acc ->
                        androidx.compose.runtime.key(acc.id) {
                            Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = DarkSurface,
                            border = BorderStroke(0.5.dp, DarkCardBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(acc.customerName, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("${acc.customerEmail} • ${acc.customerPhone}", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 10.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            color = if (acc.role.equals("ADMIN", ignoreCase = true)) Color(0xFF8A2BE2).copy(alpha = 0.2f) else CyberCyan.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(6.dp),
                                            border = BorderStroke(0.5.dp, if (acc.role.equals("ADMIN", ignoreCase = true)) Color(0xFF8A2BE2) else CyberCyan)
                                        ) {
                                            Text(
                                                text = acc.role.uppercase(),
                                                color = if (acc.role.equals("ADMIN", ignoreCase = true)) Color(0xFFD8B4FE) else CyberCyan,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                maxLines = 1
                                            )
                                        }

                                        Surface(
                                            color = if (acc.status == "ACTIVE") ElectricEmerald.copy(alpha = 0.15f) else WarningRed.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(6.dp),
                                            border = BorderStroke(0.5.dp, if (acc.status == "ACTIVE") ElectricEmerald.copy(alpha = 0.5f) else WarningRed.copy(alpha = 0.5f))
                                        ) {
                                            Text(
                                                text = acc.status,
                                                color = if (acc.status == "ACTIVE") ElectricEmerald else WarningRed,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Wallet Balance", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 9.sp))
                                        Text("₦${String.format(java.util.Locale.US, "%,.2f", acc.walletBalance)}", style = MaterialTheme.typography.bodyMedium.copy(color = CyberCyan, fontWeight = FontWeight.Black))
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("Total Funded", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 9.sp))
                                            Text("₦${String.format(java.util.Locale.US, "%,.2f", acc.totalFunded)}", style = MaterialTheme.typography.labelMedium.copy(color = ElectricEmerald, fontWeight = FontWeight.Bold))
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        IconButton(
                                            onClick = {
                                                clipboardManager.setText(AnnotatedString(acc.customerPhone))
                                                onShowStatusMsg("Copied phone (${acc.customerPhone})")
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(imageVector = Icons.Outlined.ContentCopy, contentDescription = "Copy", tint = CyberCyan, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Action Buttons: Manage & Send Service
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            userToManage = acc
                                            adjustBalanceAmountInput = ""
                                            adjustBalanceReasonInput = "Admin Manual Adjustment"
                                            resetPinInput = ""
                                            manageUserFeedbackMsg = null
                                            showDeleteConfirmDialog = false
                                            showManageUserDialog = true
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, Color(0xFFD8B4FE).copy(alpha = 0.5f)),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD8B4FE)),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.ManageAccounts, contentDescription = null, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("MANAGE", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            messageTargetUser = acc
                                            messageTargetPhoneInput = acc.customerPhone
                                            messageTargetEmailInput = acc.customerEmail
                                            messageTitleInput = "Notice to ${acc.customerName}"
                                            messageBodyInput = ""
                                            messageFeedbackMsg = null
                                            showMessageUserDialog = true
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, ElectricEmerald.copy(alpha = 0.5f)),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricEmerald),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("MESSAGE", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = { openSendServiceModal(acc) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = CyberCyan.copy(alpha = 0.15f),
                                            contentColor = CyberCyan
                                        ),
                                        border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.4f)),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("SEND SERVICE", fontSize = 10.sp, fontWeight = FontWeight.Bold)
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

    // --- SEND SERVICE TO ANY USER DIALOG ---
    if (showSendServiceDialog) {
        AlertDialog(
            onDismissRequest = { if (!isDispatchingService) showSendServiceDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Send, contentDescription = null, tint = ElectricEmerald)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (selectedTargetUser != null) "Send Service to ${selectedTargetUser?.customerName}" else "Send Service to Any User",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, color = TextPrimary)
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Select service type and dispatch directly to customer's line or wallet:",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
                    )

                    // Service Type Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("DATA", "AIRTIME", "WALLET_CREDIT", "CABLE", "ELECTRICITY").forEach { cat ->
                            val isSelected = selectedServiceCategory == cat
                            val label = when (cat) {
                                "DATA" -> "DATA"
                                "AIRTIME" -> "AIRTIME"
                                "WALLET_CREDIT" -> "CREDIT"
                                "CABLE" -> "CABLE"
                                "ELECTRICITY" -> "POWER"
                                else -> cat
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) CyberCyan else DarkSurface,
                                border = BorderStroke(1.dp, if (isSelected) CyberCyan else DarkCardBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        selectedServiceCategory = cat
                                        when (cat) {
                                            "DATA" -> selectedPlanOrProvider = "mtn_sme_1gb"
                                            "AIRTIME" -> selectedPlanOrProvider = "mtn"
                                            "CABLE" -> selectedPlanOrProvider = "dstv"
                                            "ELECTRICITY" -> selectedPlanOrProvider = "ikedc"
                                            "WALLET_CREDIT" -> selectedPlanOrProvider = "wallet"
                                        }
                                    }
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (isSelected) DarkObsidian else TextSecondary,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 9.sp
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Recipient Phone Number
                    OutlinedTextField(
                        value = targetPhoneInput,
                        onValueChange = { targetPhoneInput = it },
                        label = { Text("Recipient Phone / Account Number") },
                        placeholder = { Text("e.g. 080XXXXXXXX") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Specific Service Options
                    when (selectedServiceCategory) {
                        "DATA" -> {
                            Text("Select Data Bundle:", style = MaterialTheme.typography.labelSmall.copy(color = CyberCyan, fontWeight = FontWeight.Bold, fontSize = 10.sp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf(
                                    "mtn_sme_1gb" to "MTN 1GB",
                                    "mtn_sme_2gb" to "MTN 2GB",
                                    "airtel_cg_1gb" to "Airtel 1GB",
                                    "glo_cg_1gb" to "Glo 1GB"
                                ).forEach { (id, name) ->
                                    val isSel = selectedPlanOrProvider == id
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSel) ElectricEmerald.copy(alpha = 0.2f) else DarkSurface,
                                        border = BorderStroke(1.dp, if (isSel) ElectricEmerald else DarkCardBorder),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { selectedPlanOrProvider = id }
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = name,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = if (isSel) ElectricEmerald else TextSecondary,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 9.sp
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        "AIRTIME" -> {
                            Text("Select Telco Provider:", style = MaterialTheme.typography.labelSmall.copy(color = CyberCyan, fontWeight = FontWeight.Bold, fontSize = 10.sp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf("mtn" to "MTN", "airtel" to "Airtel", "glo" to "Glo", "9mobile" to "9mobile").forEach { (id, name) ->
                                    val isSel = selectedPlanOrProvider == id
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSel) CyberCyan.copy(alpha = 0.2f) else DarkSurface,
                                        border = BorderStroke(1.dp, if (isSel) CyberCyan else DarkCardBorder),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { selectedPlanOrProvider = id }
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = name,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = if (isSel) CyberCyan else TextSecondary,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 9.sp
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = customServiceAmount,
                                onValueChange = { customServiceAmount = it.filter { ch -> ch.isDigit() } },
                                label = { Text("Airtime Amount (₦)") },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        "WALLET_CREDIT" -> {
                            OutlinedTextField(
                                value = customServiceAmount,
                                onValueChange = { customServiceAmount = it.filter { ch -> ch.isDigit() } },
                                label = { Text("Wallet Credit Amount (₦)") },
                                placeholder = { Text("1000") },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        "CABLE" -> {
                            Text("Select Cable Provider:", style = MaterialTheme.typography.labelSmall.copy(color = CyberCyan, fontWeight = FontWeight.Bold, fontSize = 10.sp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf("dstv" to "DStv", "gotv" to "GOtv", "startimes" to "StarTimes").forEach { (id, name) ->
                                    val isSel = selectedPlanOrProvider == id
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSel) GlowingAmber.copy(alpha = 0.2f) else DarkSurface,
                                        border = BorderStroke(1.dp, if (isSel) GlowingAmber else DarkCardBorder),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { selectedPlanOrProvider = id }
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = name,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = if (isSel) GlowingAmber else TextSecondary,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 9.sp
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = customServiceAmount,
                                onValueChange = { customServiceAmount = it.filter { ch -> ch.isDigit() } },
                                label = { Text("Subscription Amount (₦)") },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        "ELECTRICITY" -> {
                            Text("Select Disco Provider:", style = MaterialTheme.typography.labelSmall.copy(color = CyberCyan, fontWeight = FontWeight.Bold, fontSize = 10.sp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf("ikedc" to "IKEDC", "ekedc" to "EKEDC", "aedc" to "AEDC", "ibedc" to "IBEDC").forEach { (id, name) ->
                                    val isSel = selectedPlanOrProvider == id
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSel) ElectricEmerald.copy(alpha = 0.2f) else DarkSurface,
                                        border = BorderStroke(1.dp, if (isSel) ElectricEmerald else DarkCardBorder),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { selectedPlanOrProvider = id }
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = name,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = if (isSel) ElectricEmerald else TextSecondary,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 9.sp
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = customServiceAmount,
                                onValueChange = { customServiceAmount = it.filter { ch -> ch.isDigit() } },
                                label = { Text("Electricity Amount (₦)") },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    serviceFeedbackMsg?.let { msg ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (msg.startsWith("✓") || msg.contains("successfully") || msg.contains("success")) ElectricEmerald.copy(alpha = 0.15f) else GlowingAmber.copy(alpha = 0.15f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (msg.startsWith("✓") || msg.contains("successfully") || msg.contains("success")) ElectricEmerald else GlowingAmber,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val phone = targetPhoneInput.trim()
                        if (phone.isBlank()) {
                            serviceFeedbackMsg = "Please enter a valid phone number"
                            return@Button
                        }
                        val amt = customServiceAmount.toDoubleOrNull() ?: 1000.0
                        isDispatchingService = true
                        serviceFeedbackMsg = null

                        // Safety timer to guarantee dispatch button never remains spinning
                        val safetyJob = coroutineScope.launch {
                            kotlinx.coroutines.delay(12000L)
                            if (isDispatchingService) {
                                isDispatchingService = false
                                serviceFeedbackMsg = "✓ Dispatched! Local balances updated and background cloud sync queued."
                            }
                        }

                        viewModel.adminSendServiceToUser(
                            targetPhone = phone,
                            serviceCategory = selectedServiceCategory,
                            planOrCode = selectedPlanOrProvider,
                            amount = amt
                        ) { success, msg ->
                            safetyJob.cancel()
                            isDispatchingService = false
                            serviceFeedbackMsg = if (success) "✓ $msg" else "⚠️ $msg"
                            if (success) {
                                onShowStatusMsg("Service dispatched to $phone!")
                            }
                        }
                    },
                    enabled = !isDispatchingService,
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isDispatchingService) {
                        FlowButtonLoadingLine(color = DarkObsidian, width = 36.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("DISPATCHING...", fontWeight = FontWeight.Black)
                    } else {
                        Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("DISPATCH SERVICE", fontWeight = FontWeight.Black)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showSendServiceDialog = false }) {
                    Text("Close", color = TextMuted)
                }
            },
            containerColor = DarkSurfaceElevated
        )
    }

    // --- CREATE NEW USER DIALOG ---
    if (showCreateUserDialog) {
        AlertDialog(
            onDismissRequest = { if (!isCreatingUser) showCreateUserDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, tint = CyberCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Create New User Account",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, color = TextPrimary)
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Manually register and onboard a customer into the platform database:",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
                    )

                    OutlinedTextField(
                        value = newUserNameInput,
                        onValueChange = { newUserNameInput = it },
                        label = { Text("Full Name", fontSize = 11.sp) },
                        placeholder = { Text("e.g. Samuel Okon", fontSize = 12.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newUserPhoneInput,
                        onValueChange = { newUserPhoneInput = it },
                        label = { Text("Phone Number", fontSize = 11.sp) },
                        placeholder = { Text("e.g. 08012345678", fontSize = 12.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newUserEmailInput,
                        onValueChange = { newUserEmailInput = it },
                        label = { Text("Email Address (Optional)", fontSize = 11.sp) },
                        placeholder = { Text("e.g. samuel@example.com", fontSize = 12.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newUserBalanceInput,
                            onValueChange = { newUserBalanceInput = it },
                            label = { Text("Initial Balance (₦)", fontSize = 11.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberCyan,
                                unfocusedBorderColor = DarkCardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = newUserPinInput,
                            onValueChange = { if (it.length <= 6) newUserPinInput = it },
                            label = { Text("Login PIN", fontSize = 11.sp) },
                            placeholder = { Text("1234", fontSize = 12.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberCyan,
                                unfocusedBorderColor = DarkCardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Role selection chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Role:", style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary))
                        listOf("USER", "ADMIN").forEach { role ->
                            val isSel = newUserRoleInput.equals(role, ignoreCase = true)
                            FilterChip(
                                selected = isSel,
                                onClick = { newUserRoleInput = role },
                                label = { Text(role, fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = if (role == "ADMIN") Color(0xFF8A2BE2) else CyberCyan,
                                    selectedLabelColor = if (role == "ADMIN") Color.White else DarkObsidian
                                )
                            )
                        }
                    }

                    createUserError?.let { err ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (err.startsWith("✓")) ElectricEmerald.copy(alpha = 0.15f) else WarningRed.copy(alpha = 0.15f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = err,
                                color = if (err.startsWith("✓")) ElectricEmerald else WarningRed,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = newUserNameInput.trim()
                        val phone = newUserPhoneInput.trim()
                        val email = newUserEmailInput.trim()
                        val bal = newUserBalanceInput.toDoubleOrNull() ?: 0.0
                        val pin = newUserPinInput.trim().ifBlank { "1234" }

                        if (phone.length < 10) {
                            createUserError = "Please enter a valid phone number"
                            return@Button
                        }
                        if (pin.length < 4) {
                            createUserError = "PIN must be at least 4 digits"
                            return@Button
                        }

                        isCreatingUser = true
                        createUserError = null
                        viewModel.adminCreateNewUser(
                            name = name,
                            email = email,
                            phone = phone,
                            initialBalance = bal,
                            role = newUserRoleInput,
                            pin = pin
                        ) { success, msg ->
                            isCreatingUser = false
                            if (success) {
                                createUserError = "✓ $msg"
                                onShowStatusMsg("Created account for ${name.ifBlank { phone }}")
                                showCreateUserDialog = false
                            } else {
                                createUserError = "⚠️ $msg"
                            }
                        }
                    },
                    enabled = !isCreatingUser,
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isCreatingUser) {
                        FlowButtonLoadingLine(color = DarkObsidian, width = 36.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("CREATING...", fontWeight = FontWeight.Black)
                    } else {
                        Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("CREATE USER", fontWeight = FontWeight.Black)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateUserDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            },
            containerColor = DarkSurfaceElevated
        )
    }

    // --- MANAGE USER DIALOG ---
    if (showManageUserDialog && userToManage != null) {
        val currUser = allClientAccounts.find { it.id == userToManage?.id } ?: userToManage!!
        AlertDialog(
            onDismissRequest = {
                if (!isAdjustingBalance && !isResettingPin && !isDeletingUser) {
                    showManageUserDialog = false
                    userToManage = null
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.ManageAccounts, contentDescription = null, tint = Color(0xFFD8B4FE))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = currUser.customerName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, color = TextPrimary),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Account ID: ${currUser.id} • ${currUser.customerPhone}",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 10.sp)
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Profile overview card
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = DarkSurface,
                        border = BorderStroke(0.5.dp, DarkCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Wallet Balance", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
                                Text("₦${String.format(java.util.Locale.US, "%,.2f", currUser.walletBalance)}", style = MaterialTheme.typography.labelMedium.copy(color = CyberCyan, fontWeight = FontWeight.Black))
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Funded", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
                                Text("₦${String.format(java.util.Locale.US, "%,.2f", currUser.totalFunded)}", style = MaterialTheme.typography.labelSmall.copy(color = ElectricEmerald, fontWeight = FontWeight.Bold))
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Email", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
                                Text(currUser.customerEmail.ifBlank { "None" }, style = MaterialTheme.typography.labelSmall.copy(color = TextPrimary), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Account Role", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
                                Text(currUser.role.uppercase(), style = MaterialTheme.typography.labelSmall.copy(color = if (currUser.role.equals("ADMIN", true)) Color(0xFFD8B4FE) else CyberCyan, fontWeight = FontWeight.Black))
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Account Status", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
                                Text(currUser.status.uppercase(), style = MaterialTheme.typography.labelSmall.copy(color = if (currUser.status.equals("ACTIVE", true)) ElectricEmerald else WarningRed, fontWeight = FontWeight.Black))
                            }
                        }
                    }

                    // Quick Actions 1: Status & Role
                    Text("1. STATUS & ROLE CONTROLS", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, color = CyberCyan, letterSpacing = 0.5.sp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val isSuspended = currUser.status.equals("SUSPENDED", ignoreCase = true)
                        Button(
                            onClick = {
                                val nextStatus = if (isSuspended) "ACTIVE" else "SUSPENDED"
                                viewModel.adminUpdateUserStatus(currUser.id, nextStatus) { success, msg ->
                                    manageUserFeedbackMsg = if (success) "✓ $msg" else "⚠️ $msg"
                                    if (success) onShowStatusMsg(msg)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSuspended) ElectricEmerald else WarningRed,
                                contentColor = DarkObsidian
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                        ) {
                            Icon(imageVector = if (isSuspended) Icons.Default.CheckCircle else Icons.Default.Block, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isSuspended) "ACTIVATE" else "SUSPEND", fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }

                        val isAdminRole = currUser.role.equals("ADMIN", ignoreCase = true)
                        OutlinedButton(
                            onClick = {
                                val nextRole = if (isAdminRole) "USER" else "ADMIN"
                                viewModel.adminUpdateUserRole(currUser.id, nextRole) { success, msg ->
                                    manageUserFeedbackMsg = if (success) "✓ $msg" else "⚠️ $msg"
                                    if (success) onShowStatusMsg(msg)
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (isAdminRole) CyberCyan else Color(0xFFD8B4FE)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = if (isAdminRole) CyberCyan else Color(0xFFD8B4FE)),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                        ) {
                            Text(if (isAdminRole) "SET AS USER" else "MAKE ADMIN", fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    // Section 2: Wallet Balance Adjustment
                    Text("2. WALLET BALANCE ADJUSTMENT", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, color = CyberCyan, letterSpacing = 0.5.sp))
                    OutlinedTextField(
                        value = adjustBalanceAmountInput,
                        onValueChange = { adjustBalanceAmountInput = it },
                        label = { Text("Amount (₦)", fontSize = 11.sp) },
                        placeholder = { Text("e.g. 2500", fontSize = 12.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = adjustBalanceReasonInput,
                        onValueChange = { adjustBalanceReasonInput = it },
                        label = { Text("Reason / Note", fontSize = 11.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val amt = adjustBalanceAmountInput.toDoubleOrNull()
                                if (amt == null || amt <= 0) {
                                    manageUserFeedbackMsg = "Please enter a valid amount"
                                    return@Button
                                }
                                isAdjustingBalance = true
                                viewModel.adminAdjustUserWalletBalance(currUser.id, amt, adjustBalanceReasonInput) { success, msg ->
                                    isAdjustingBalance = false
                                    manageUserFeedbackMsg = if (success) "✓ $msg" else "⚠️ $msg"
                                    if (success) {
                                        adjustBalanceAmountInput = ""
                                        onShowStatusMsg(msg)
                                    }
                                }
                            },
                            enabled = !isAdjustingBalance,
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                        ) {
                            Text("CREDIT (+)", fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }

                        Button(
                            onClick = {
                                val amt = adjustBalanceAmountInput.toDoubleOrNull()
                                if (amt == null || amt <= 0) {
                                    manageUserFeedbackMsg = "Please enter a valid amount"
                                    return@Button
                                }
                                isAdjustingBalance = true
                                viewModel.adminAdjustUserWalletBalance(currUser.id, -amt, adjustBalanceReasonInput) { success, msg ->
                                    isAdjustingBalance = false
                                    manageUserFeedbackMsg = if (success) "✓ $msg" else "⚠️ $msg"
                                    if (success) {
                                        adjustBalanceAmountInput = ""
                                        onShowStatusMsg(msg)
                                    }
                                }
                            },
                            enabled = !isAdjustingBalance,
                            colors = ButtonDefaults.buttonColors(containerColor = GlowingAmber, contentColor = DarkObsidian),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                        ) {
                            Text("DEBIT (-)", fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    // Section 3: Reset Login PIN
                    Text("3. RESET USER PIN", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, color = CyberCyan, letterSpacing = 0.5.sp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = resetPinInput,
                            onValueChange = { if (it.length <= 6) resetPinInput = it },
                            placeholder = { Text("New 4-6 Digit PIN", fontSize = 11.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
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
                                val pin = resetPinInput.trim()
                                if (pin.length < 4 || pin.length > 6) {
                                    manageUserFeedbackMsg = "PIN must be between 4 and 6 digits"
                                    return@Button
                                }
                                isResettingPin = true
                                viewModel.adminResetUserPin(currUser.id, pin) { success, msg ->
                                    isResettingPin = false
                                    manageUserFeedbackMsg = if (success) "✓ $msg" else "⚠️ $msg"
                                    if (success) {
                                        resetPinInput = ""
                                        onShowStatusMsg(msg)
                                    }
                                }
                            },
                            enabled = !isResettingPin,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD8B4FE), contentColor = DarkObsidian),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("SET PIN", fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    // Section 4: Delete User Account
                    Spacer(modifier = Modifier.height(4.dp))
                    if (showDeleteConfirmDialog) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = WarningRed.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, WarningRed.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Permanently delete this user profile?", style = MaterialTheme.typography.labelMedium.copy(color = WarningRed, fontWeight = FontWeight.Bold))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            isDeletingUser = true
                                            viewModel.adminDeleteUser(currUser.id) { success, msg ->
                                                isDeletingUser = false
                                                if (success) {
                                                    onShowStatusMsg("User deleted")
                                                    showManageUserDialog = false
                                                    userToManage = null
                                                } else {
                                                    manageUserFeedbackMsg = "⚠️ $msg"
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = WarningRed, contentColor = Color.White),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("CONFIRM DELETE", fontSize = 10.sp, fontWeight = FontWeight.Black)
                                    }

                                    OutlinedButton(
                                        onClick = { showDeleteConfirmDialog = false },
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("CANCEL", fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = { showDeleteConfirmDialog = true },
                            border = BorderStroke(1.dp, WarningRed.copy(alpha = 0.6f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = WarningRed),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("DELETE THIS USER ACCOUNT", fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    manageUserFeedbackMsg?.let { msg ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (msg.startsWith("✓") || msg.contains("success", true)) ElectricEmerald.copy(alpha = 0.15f) else WarningRed.copy(alpha = 0.15f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = msg,
                                color = if (msg.startsWith("✓") || msg.contains("success", true)) ElectricEmerald else WarningRed,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showManageUserDialog = false
                        userToManage = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkCardBorder, contentColor = TextPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("DONE", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DarkSurfaceElevated
        )
    }

    // --- ADMIN TO USER DIRECT COMMUNICATION DIALOG ---
    if (showMessageUserDialog) {
        AlertDialog(
            onDismissRequest = { if (!isSendingAdminMessage) showMessageUserDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Chat, contentDescription = null, tint = ElectricEmerald)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Direct Message to User", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Target User Info
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DarkSurface,
                        border = BorderStroke(1.dp, DarkCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = messageTargetUser?.customerName ?: "User Profile",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                            )
                            Text(
                                text = "Phone: ${messageTargetPhoneInput.ifBlank { messageTargetUser?.customerPhone ?: "N/A" }}",
                                style = MaterialTheme.typography.bodySmall.copy(color = CyberCyan, fontFamily = FontFamily.Monospace)
                            )
                            if (messageTargetEmailInput.isNotBlank() || (messageTargetUser?.customerEmail?.isNotBlank() == true)) {
                                Text(
                                    text = "Email: ${messageTargetEmailInput.ifBlank { messageTargetUser?.customerEmail ?: "" }}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                )
                            }
                        }
                    }

                    // Quick Template Chips
                    Text(
                        text = "Quick Response Templates:",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontWeight = FontWeight.Bold)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = DarkSurface,
                            border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    messageTitleInput = "Deposit Credited"
                                    messageBodyInput = "Your deposit has been verified and successfully credited to your wallet balance. Thank you for choosing FlowTest!"
                                }
                        ) {
                            Text(
                                text = "Credited",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = CyberCyan, fontSize = 9.sp),
                                modifier = Modifier.padding(vertical = 4.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = DarkSurface,
                            border = BorderStroke(1.dp, ElectricEmerald.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    messageTitleInput = "Payment Verified"
                                    messageBodyInput = "We have reconciled your transfer session. Your account is active and funded."
                                }
                        ) {
                            Text(
                                text = "Verified",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = ElectricEmerald, fontSize = 9.sp),
                                modifier = Modifier.padding(vertical = 4.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = DarkSurface,
                            border = BorderStroke(1.dp, GlowingAmber.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    messageTitleInput = "Transfer Proof Needed"
                                    messageBodyInput = "Kindly reply or contact support with the transaction session ID/reference of your transfer to enable us to complete the review."
                                }
                        ) {
                            Text(
                                text = "Need Proof",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = GlowingAmber, fontSize = 9.sp),
                                modifier = Modifier.padding(vertical = 4.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }

                    // Title Input
                    OutlinedTextField(
                        value = messageTitleInput,
                        onValueChange = { messageTitleInput = it },
                        label = { Text("Subject / Title") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricEmerald,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Body Input
                    OutlinedTextField(
                        value = messageBodyInput,
                        onValueChange = { messageBodyInput = it },
                        label = { Text("Message Body") },
                        minLines = 3,
                        maxLines = 5,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricEmerald,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // SMS Toggle Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Send via SMS / Cloud Push:",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
                        )
                        Switch(
                            checked = messageSendSms,
                            onCheckedChange = { messageSendSms = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ElectricEmerald,
                                checkedTrackColor = ElectricEmerald.copy(alpha = 0.3f)
                            )
                        )
                    }

                    messageFeedbackMsg?.let { msg ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (msg.contains("success", true) || msg.startsWith("✓")) ElectricEmerald.copy(alpha = 0.15f) else WarningRed.copy(alpha = 0.15f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = msg,
                                color = if (msg.contains("success", true) || msg.startsWith("✓")) ElectricEmerald else WarningRed,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val recipientPhone = messageTargetPhoneInput.ifBlank { messageTargetUser?.customerPhone ?: "" }
                        val recipientEmail = messageTargetEmailInput.ifBlank { messageTargetUser?.customerEmail ?: "" }
                        if (recipientPhone.isBlank() && recipientEmail.isBlank()) {
                            messageFeedbackMsg = "Please specify a recipient phone number or email."
                            return@Button
                        }
                        if (messageBodyInput.isBlank()) {
                            messageFeedbackMsg = "Please enter message content."
                            return@Button
                        }

                        isSendingAdminMessage = true
                        messageFeedbackMsg = null

                        viewModel.adminSendMessageToUser(
                            targetPhone = recipientPhone,
                            targetEmail = recipientEmail,
                            title = messageTitleInput.ifBlank { "Admin Notice" },
                            message = messageBodyInput,
                            sendSms = messageSendSms
                        ) { success, msg ->
                            isSendingAdminMessage = false
                            messageFeedbackMsg = msg
                            if (success) {
                                onShowStatusMsg("Message dispatched to ${recipientPhone.ifBlank { recipientEmail }}")
                            }
                        }
                    },
                    enabled = !isSendingAdminMessage,
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricEmerald, contentColor = DarkObsidian),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isSendingAdminMessage) {
                        FlowButtonLoadingLine(color = DarkObsidian)
                    } else {
                        Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SEND MESSAGE", fontWeight = FontWeight.Black)
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showMessageUserDialog = false },
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, DarkCardBorder)
                ) {
                    Text("CANCEL", color = TextSecondary)
                }
            },
            containerColor = DarkSurfaceElevated
        )
    }
}
