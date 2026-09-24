package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.api.GeminiSmsAssistant
import com.example.data.api.HttpSmsService
import com.example.data.ui.viewmodel.VpnViewModel
import com.example.data.util.ContactsHelper
import com.example.data.util.DeviceContact
import com.example.ui.components.FlowButtonLoadingLine
import com.example.ui.components.chat.*
import com.example.ui.motion.MotionTransitions
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsChatScreen(
    viewModel: VpnViewModel,
    onNavigateBack: () -> Unit = {},
    onNavigateToBulkSms: () -> Unit = {},
    initialRecipientPhone: String? = null,
    modifier: Modifier = Modifier
) {
    val conversations by viewModel.smsConversations.collectAsStateWithLifecycle()
    val walletBalance by viewModel.userWalletBalance.collectAsStateWithLifecycle()
    val wholesaleCost by viewModel.smsWholesaleCost.collectAsStateWithLifecycle()
    val retailPrice by viewModel.smsRetailPrice.collectAsStateWithLifecycle()
    val markupPercent by viewModel.adminSmsMarkupPercent.collectAsStateWithLifecycle()
    val senderId by viewModel.smsSenderId.collectAsStateWithLifecycle()

    var selectedConversationId by remember {
        mutableStateOf(
            if (!initialRecipientPhone.isNullOrBlank()) {
                conversations.firstOrNull { it.recipientPhone.contains(initialRecipientPhone) }?.id
            } else {
                conversations.firstOrNull()?.id
            }
        )
    }

    val context = LocalContext.current
    var isViewingThread by remember { mutableStateOf(selectedConversationId != null) }
    var showNewChatDialog by remember { mutableStateOf(false) }
    var showAiPromptDialog by remember { mutableStateOf(false) }
    var showContactPickerDialog by remember { mutableStateOf(false) }
    var showAudioCallDialog by remember { mutableStateOf(false) }
    var showVideoCallDialog by remember { mutableStateOf(false) }
    var activeCallPeerName by remember { mutableStateOf("") }
    var activeCallPeerPhone by remember { mutableStateOf("") }
    var sendMode by remember(selectedConversationId) {
        mutableStateOf(ChatSendMode.FLOW_CHAT)
    }

    val contactPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        showContactPickerDialog = true
    }

    fun requestContactSync() {
        if (ContactsHelper.hasContactPermission(context)) {
            showContactPickerDialog = true
        } else {
            contactPermissionLauncher.launch(android.Manifest.permission.READ_CONTACTS)
        }
    }

    val currentConversation = conversations.firstOrNull { it.id == selectedConversationId }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(DarkObsidian)
            .testTag("sms_chat_screen_container"),
        containerColor = DarkObsidian,
        topBar = {
            if (isViewingThread && currentConversation != null) {
                // Authentic iOS Messages / WhatsApp Thread Header
                Surface(
                    color = DarkSurface,
                    tonalElevation = 6.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { isViewingThread = false },
                            modifier = Modifier.size(36.dp).testTag("back_to_conversations_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to list",
                                tint = CyberCyan
                            )
                        }

                        // Avatar with iOS Style Initial Circle & Online Indicator
                        Box(
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(currentConversation.avatarBgColorHex)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = currentConversation.recipientName.take(2).uppercase(),
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                            }
                            if (currentConversation.isOnline) {
                                Box(
                                    modifier = Modifier
                                        .size(11.dp)
                                        .clip(CircleShape)
                                        .background(FlowChatCyan)
                                        .border(1.5.dp, DarkSurface, CircleShape)
                                        .align(Alignment.BottomEnd)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = currentConversation.recipientName,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = currentConversation.flagEmoji, fontSize = 13.sp)
                            }
                            Text(
                                text = when {
                                    currentConversation.isTyping -> "typing..."
                                    sendMode == ChatSendMode.FLOW_CHAT -> "Flow Chat (₦0.00)"
                                    else -> "SMS (₦${String.format("%.2f", retailPrice)}/SMS)"
                                },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = when {
                                        currentConversation.isTyping -> FlowChatCyan
                                        sendMode == ChatSendMode.FLOW_CHAT -> FlowChatCyan
                                        else -> SmsBlue
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Mode Selector Button (Quick toggle between Flow Chat & SMS)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (sendMode == ChatSendMode.FLOW_CHAT) FlowChatCyan.copy(alpha = 0.18f) else SmsBlue.copy(alpha = 0.18f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (sendMode == ChatSendMode.FLOW_CHAT) FlowChatCyan else SmsBlue
                            ),
                            modifier = Modifier
                                .clickable {
                                    sendMode = if (sendMode == ChatSendMode.FLOW_CHAT) ChatSendMode.SMS else ChatSendMode.FLOW_CHAT
                                }
                                .testTag("toggle_send_mode_btn")
                        ) {
                            Text(
                                text = if (sendMode == ChatSendMode.FLOW_CHAT) "💬 Flow Chat" else "📱 SMS",
                                color = if (sendMode == ChatSendMode.FLOW_CHAT) FlowChatCyan else SmsBlue,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(2.dp))

                        // Cloudflare R2 Backup Action
                        val isBackingUpChat by viewModel.isBackingUpChat.collectAsStateWithLifecycle()
                        IconButton(
                            onClick = {
                                viewModel.syncChatToR2(currentConversation.id) { res ->
                                    android.widget.Toast.makeText(
                                        context,
                                        if (res.isSuccess) "☁️ R2 Backup: ${res.message}" else "⚠️ Backup error: ${res.message}",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            enabled = !isBackingUpChat,
                            modifier = Modifier.size(34.dp).testTag("sync_chat_r2_btn")
                        ) {
                            if (isBackingUpChat) {
                                FlowButtonLoadingLine(
                                    width = 20.dp,
                                    height = 2.dp,
                                    color = Color(0xFFF6821F)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.CloudUpload,
                                    contentDescription = "Backup Chat to Cloudflare R2",
                                    tint = Color(0xFFF6821F),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(2.dp))

                        // Audio VoIP Call Action
                        IconButton(
                            onClick = {
                                activeCallPeerName = currentConversation.recipientName
                                activeCallPeerPhone = currentConversation.recipientPhone
                                showAudioCallDialog = true
                            },
                            modifier = Modifier.size(34.dp).testTag("start_audio_call_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Call,
                                contentDescription = "Voice Call",
                                tint = FlowChatCyan,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        // Video Call Action
                        IconButton(
                            onClick = {
                                activeCallPeerName = currentConversation.recipientName
                                activeCallPeerPhone = currentConversation.recipientPhone
                                showVideoCallDialog = true
                            },
                            modifier = Modifier.size(34.dp).testTag("start_video_call_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Videocam,
                                contentDescription = "Video Call",
                                tint = FlowChatCyan,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                    }
                }
            } else {
                // iOS Messages Conversation List Header
                Surface(
                    color = DarkSurface,
                    tonalElevation = 6.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
                ) {
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
                                onClick = onNavigateBack,
                                modifier = Modifier.size(36.dp).testTag("sms_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = CyberCyan
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Flow Chat",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Black,
                                        color = TextPrimary,
                                        letterSpacing = 0.5.sp
                                    )
                                )
                                Text(
                                    text = "Flow Chat (₦0.00) • Balance: ₦${String.format("%,.2f", walletBalance)}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = FlowChatCyan,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onNavigateToBulkSms,
                                modifier = Modifier
                                    .size(38.dp)
                                    .testTag("header_bulk_sms_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.SendToMobile,
                                    contentDescription = "Bulk SMS & BYOD SaaS",
                                    tint = ElectricEmerald
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = { requestContactSync() },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Contacts,
                                    contentDescription = "Sync Contacts",
                                    tint = FlowChatCyan
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            // Cloudflare R2 All Chats Sync Button
                            IconButton(
                                onClick = {
                                    if (conversations.isEmpty()) {
                                        android.widget.Toast.makeText(context, "No active chats to backup", android.widget.Toast.LENGTH_SHORT).show()
                                    } else {
                                        var backedUpCount = 0
                                        conversations.forEach { conv ->
                                            viewModel.syncChatToR2(conv.id) {
                                                backedUpCount++
                                                if (backedUpCount == conversations.size) {
                                                    android.widget.Toast.makeText(context, "☁️ Cloudflare R2: $backedUpCount conversations backed up!", android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.size(38.dp).testTag("backup_all_chats_r2_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CloudSync,
                                    contentDescription = "Cloudflare R2 Backup All",
                                    tint = Color(0xFFF6821F)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = { showNewChatDialog = true },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(FlowChatCyan)
                                    .testTag("compose_new_sms_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = "New Message",
                                    tint = DarkObsidian,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = isViewingThread && currentConversation != null,
                transitionSpec = {
                    if (targetState) {
                        // Forward push to chat thread: clean snappy slide + fade
                        slideInHorizontally(
                            initialOffsetX = { fullWidth -> fullWidth / 3 },
                            animationSpec = MotionTransitions.PageOffsetSpec
                        ) + fadeIn(animationSpec = tween(MotionTransitions.FADE_MS)) togetherWith
                        slideOutHorizontally(
                            targetOffsetX = { fullWidth -> -fullWidth / 4 },
                            animationSpec = MotionTransitions.PageOffsetSpec
                        ) + fadeOut(animationSpec = tween(MotionTransitions.FADE_MS))
                    } else {
                        // Popping back to conversations list
                        slideInHorizontally(
                            initialOffsetX = { fullWidth -> -fullWidth / 4 },
                            animationSpec = MotionTransitions.PageOffsetSpec
                        ) + fadeIn(animationSpec = tween(MotionTransitions.FADE_MS)) togetherWith
                        slideOutHorizontally(
                            targetOffsetX = { fullWidth -> fullWidth },
                            animationSpec = MotionTransitions.PageOffsetSpec
                        ) + fadeOut(animationSpec = tween(MotionTransitions.FADE_MS))
                    }
                },
                label = "ChatBodyTransition"
            ) { viewingThread ->
                if (viewingThread && currentConversation != null) {
                    SmsThreadConversationView(
                        conversation = currentConversation,
                        viewModel = viewModel,
                        retailPricePerSegment = retailPrice,
                        walletBalance = walletBalance,
                        currentSendMode = sendMode,
                        onSendModeChange = { sendMode = it },
                        onStartAudioCall = { name, phone ->
                            activeCallPeerName = name
                            activeCallPeerPhone = phone
                            showAudioCallDialog = true
                        },
                        onStartVideoCall = { name ->
                            activeCallPeerName = name
                            activeCallPeerPhone = currentConversation.recipientPhone
                            showVideoCallDialog = true
                        },
                        onOpenAiAssistant = { showAiPromptDialog = true }
                    )
                } else {
                    // Conversations List
                    SmsConversationsListView(
                        conversations = conversations,
                        onSelectConversation = { conv ->
                            selectedConversationId = conv.id
                            isViewingThread = true
                        },
                        onNewMessageClick = { showNewChatDialog = true },
                        onOpenBulkSms = onNavigateToBulkSms
                    )
                }
            }
        }
    }

    // New Message Dialog with Free Chat / Carrier SMS Selector
    if (showNewChatDialog) {
        NewSmsConversationDialog(
            viewModel = viewModel,
            retailPrice = retailPrice,
            onDismiss = { showNewChatDialog = false },
            onRequestContactSync = { requestContactSync() },
            onConversationCreated = { convId ->
                selectedConversationId = convId
                isViewingThread = true
                showNewChatDialog = false
            }
        )
    }

    // Contact Picker Dialog
    if (showContactPickerDialog) {
        ContactPickerDialog(
            onDismiss = { showContactPickerDialog = false },
            onSelectContact = { contact ->
                showContactPickerDialog = false
                viewModel.sendDirectMessage(
                    recipientPhone = contact.phoneNumber,
                    messageText = "Hey ${contact.name}! Connected on Flow Chat.",
                    recipientName = contact.name
                ) { success, _ ->
                    if (success) {
                        selectedConversationId = contact.phoneNumber
                        isViewingThread = true
                    }
                }
            }
        )
    }

    // AI Message Composer & Refiner Dialog
    if (showAiPromptDialog) {
        AiSmsComposerDialog(
            recipientName = currentConversation?.recipientName ?: "Customer",
            onDismiss = { showAiPromptDialog = false },
            onApplyGeneratedText = { aiText ->
                showAiPromptDialog = false
            }
        )
    }

    // WebRTC Audio Call with CleanSignal™ DSP
    if (showAudioCallDialog) {
        AudioCallDialog(
            recipientName = activeCallPeerName.ifBlank { "Contact" },
            recipientPhone = activeCallPeerPhone,
            onDismiss = { showAudioCallDialog = false },
            onSwitchToVideo = {
                showAudioCallDialog = false
                showVideoCallDialog = true
            }
        )
    }

    // WebRTC Video Call with CameraX & CleanSignal™
    if (showVideoCallDialog) {
        VideoCallDialog(
            recipientName = activeCallPeerName.ifBlank { "Contact" },
            recipientPhone = activeCallPeerPhone,
            onDismiss = { showVideoCallDialog = false }
        )
    }
}

@Composable
fun SmsConversationsListView(
    conversations: List<VpnViewModel.SmsConversationItem>,
    onSelectConversation: (VpnViewModel.SmsConversationItem) -> Unit,
    onNewMessageClick: () -> Unit,
    onOpenBulkSms: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // "ALL", "FLOW", "SMS", "UNREAD"

    val filteredConversations = remember(conversations, searchQuery, selectedFilter) {
        conversations.filter { conv ->
            val matchesSearch = searchQuery.isBlank() ||
                conv.recipientName.contains(searchQuery, ignoreCase = true) ||
                conv.recipientPhone.contains(searchQuery, ignoreCase = true) ||
                conv.lastMessage.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                "FLOW" -> conv.isOnFeedApp
                "SMS" -> !conv.isOnFeedApp
                "UNREAD" -> conv.unreadCount > 0
                else -> true
            }
            matchesSearch && matchesFilter
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkObsidian)
    ) {
        // Search & Filter Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // iOS Style Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search chats or enter number...", color = TextMuted, fontSize = 13.sp) },
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = "Search", tint = CyberCyan, modifier = Modifier.size(18.dp))
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear", tint = TextMuted, modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DarkSurfaceElevated,
                    unfocusedContainerColor = DarkSurfaceElevated,
                    focusedBorderColor = CyberCyan,
                    unfocusedBorderColor = GlassBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            )

            // High-Impact Bulk SMS & BYOD SaaS Gateway Callout Banner
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                border = androidx.compose.foundation.BorderStroke(1.dp, ElectricEmerald.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clickable { onOpenBulkSms() }
                    .testTag("banner_bulk_sms_byod")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(ElectricEmerald.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SendToMobile,
                            contentDescription = null,
                            tint = ElectricEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Bulk SMS & BYOD SaaS",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    color = TextPrimary,
                                    fontSize = 12.sp
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = ElectricEmerald.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "100% DND BYPASS",
                                    color = ElectricEmerald,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = "Real SIM routes • ₦7.50/SMS or ₦5,000/mo BYOD Gateway",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedFilter == "ALL",
                        onClick = { selectedFilter = "ALL" },
                        label = { Text("All (${conversations.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyberCyan,
                            selectedLabelColor = DarkObsidian,
                            containerColor = DarkSurfaceElevated,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selectedFilter == "ALL",
                            borderColor = GlassBorder,
                            selectedBorderColor = CyberCyan
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == "FLOW",
                        onClick = { selectedFilter = "FLOW" },
                        leadingIcon = {
                            Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(FlowChatCyan))
                        },
                        label = { Text("Flow Chat", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = FlowChatCyan,
                            selectedLabelColor = DarkObsidian,
                            containerColor = DarkSurfaceElevated,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selectedFilter == "FLOW",
                            borderColor = GlassBorder,
                            selectedBorderColor = FlowChatCyan
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == "SMS",
                        onClick = { selectedFilter = "SMS" },
                        leadingIcon = {
                            Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(SmsBlue))
                        },
                        label = { Text("SMS", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SmsBlue,
                            selectedLabelColor = Color.White,
                            containerColor = DarkSurfaceElevated,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selectedFilter == "SMS",
                            borderColor = GlassBorder,
                            selectedBorderColor = SmsBlue
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == "UNREAD",
                        onClick = { selectedFilter = "UNREAD" },
                        label = { Text("Unread", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GlowingAmber,
                            selectedLabelColor = DarkObsidian,
                            containerColor = DarkSurfaceElevated,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selectedFilter == "UNREAD",
                            borderColor = GlassBorder,
                            selectedBorderColor = GlowingAmber
                        )
                    )
                }
            }
        }

        if (filteredConversations.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(CyberCyan.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Chat,
                        contentDescription = "No Conversations",
                        tint = CyberCyan,
                        modifier = Modifier.size(40.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (searchQuery.isNotBlank()) "No Matching Chats" else "No Conversations Yet",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Enjoy 100% free Flow Chat between app users, or send SMS worldwide.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onNewMessageClick,
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "New",
                        tint = DarkObsidian,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "START NEW CHAT",
                        color = DarkObsidian,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                item {
                    // Feature Announcement Card: Flow Chat + SMS
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(FlowChatCyan.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Chat,
                                        contentDescription = "Flow Chat",
                                        tint = FlowChatCyan,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "FLOW CHAT & SMS",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Black,
                                            color = FlowChatCyan,
                                            fontSize = 10.sp
                                        )
                                    )
                                    Text(
                                        text = "Direct Flow Chat P2P • Global SMS enabled",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = TextSecondary,
                                            fontSize = 10.5.sp
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = FlowChatCyan.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "FLOW CHAT",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = FlowChatCyan,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 9.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                items(filteredConversations, key = { it.id }) { conv ->
                    IosConversationItemCard(
                        conversation = conv,
                        onClick = { onSelectConversation(conv) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun IosConversationItemCard(
    conversation: VpnViewModel.SmsConversationItem,
    onClick: () -> Unit
) {
    val timeFormatted = remember(conversation.lastTimestamp) {
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        sdf.format(Date(conversation.lastTimestamp))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, DarkCardBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("conversation_item_${conversation.id}"),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circle Avatar with WhatsApp/iMessage online indicator
            Box(
                modifier = Modifier.size(48.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(conversation.avatarBgColorHex)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = conversation.recipientName.take(2).uppercase(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }

                if (conversation.isOnline) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(FlowChatCyan)
                            .border(1.5.dp, DarkSurface, CircleShape)
                        .align(Alignment.BottomEnd)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = conversation.recipientName,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = conversation.flagEmoji, fontSize = 13.sp)

                        if (conversation.isOnFeedApp) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = FlowChatCyan.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "FLOW",
                                    color = FlowChatCyan,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = timeFormatted,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextMuted,
                            fontSize = 10.5.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = conversation.lastMessage,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (conversation.unreadCount > 0) TextPrimary else TextSecondary,
                            fontWeight = if (conversation.unreadCount > 0) FontWeight.SemiBold else FontWeight.Normal
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (conversation.unreadCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(FlowChatCyan),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${conversation.unreadCount}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp,
                                    color = DarkObsidian
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SmsThreadConversationView(
    conversation: VpnViewModel.SmsConversationItem,
    viewModel: VpnViewModel,
    retailPricePerSegment: Double,
    walletBalance: Double,
    currentSendMode: ChatSendMode,
    onSendModeChange: (ChatSendMode) -> Unit,
    onStartAudioCall: (name: String, phone: String) -> Unit,
    onStartVideoCall: (name: String) -> Unit,
    onOpenAiAssistant: () -> Unit
) {
    var messageInput by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var smartReplies by remember { mutableStateOf<List<String>>(emptyList()) }
    var showAttachmentSheet by remember { mutableStateOf(false) }
    var isRecordingVoice by remember { mutableStateOf(false) }
    var voiceDurationSec by remember { mutableStateOf(0) }
    var replyingToMessage by remember { mutableStateOf<VpnViewModel.SmsMessageItem?>(null) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val threadContext = LocalContext.current

    // Zero-permission modern visual photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            viewModel.uploadMediaAndSendDirect(
                chatId = conversation.id,
                uri = uri,
                fileName = "photo_${System.currentTimeMillis()}.jpg",
                caption = "📷 Photo shared via Flow Chat",
                isPhoto = true
            ) { success, msg ->
                android.widget.Toast.makeText(threadContext, msg, android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Document file picker launcher
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            viewModel.uploadMediaAndSendDirect(
                chatId = conversation.id,
                uri = uri,
                fileName = "doc_${System.currentTimeMillis()}.pdf",
                caption = "📄 Document shared via Flow Chat",
                isPhoto = false
            ) { success, msg ->
                android.widget.Toast.makeText(threadContext, msg, android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Voice memo recording timer
    LaunchedEffect(isRecordingVoice) {
        if (isRecordingVoice) {
            voiceDurationSec = 0
            while (isRecordingVoice) {
                kotlinx.coroutines.delay(1000L)
                voiceDurationSec++
            }
        }
    }

    // Auto-generate smart replies when conversation or last message updates
    LaunchedEffect(conversation.id, conversation.messages.size) {
        val lastMsg = conversation.messages.lastOrNull()?.text ?: conversation.lastMessage
        smartReplies = GeminiSmsAssistant.generateSmartReplies(lastMsg)
    }

    // Auto-scroll to bottom when new messages or typing state updates
    LaunchedEffect(conversation.messages.size, conversation.isTyping) {
        if (conversation.messages.isNotEmpty()) {
            listState.animateScrollToItem(conversation.messages.size + if (conversation.isTyping) 1 else 0)
        }
    }

    val costMetrics = remember(messageInput) {
        viewModel.calculateSmsCost(messageInput, 1)
    }

    val handleSendMessage: () -> Unit = {
        if (messageInput.isNotBlank() && !isSending) {
            isSending = true
            val textToSend = messageInput
            messageInput = ""
            replyingToMessage = null
            focusManager.clearFocus()

            if (currentSendMode == ChatSendMode.FLOW_CHAT) {
                viewModel.sendDirectMessage(
                    recipientPhone = conversation.recipientPhone,
                    messageText = textToSend,
                    recipientName = conversation.recipientName
                ) { _, _ ->
                    isSending = false
                }
            } else {
                viewModel.sendChatMessage(
                    recipientPhone = conversation.recipientPhone,
                    messageText = textToSend,
                    recipientName = conversation.recipientName
                ) { _, _ ->
                    isSending = false
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkObsidian)
    ) {
        // Message Thread List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            item {
                // Encryption & Gateway Verification Notice (Authentic iOS Header)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (currentSendMode == ChatSendMode.FLOW_CHAT) FlowChatCyan.copy(alpha = 0.12f) else SmsBlue.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (currentSendMode == ChatSendMode.FLOW_CHAT) FlowChatCyan.copy(alpha = 0.3f) else SmsBlue.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (currentSendMode == ChatSendMode.FLOW_CHAT) Icons.Filled.Lock else Icons.Filled.Sensors,
                                    contentDescription = null,
                                    tint = if (currentSendMode == ChatSendMode.FLOW_CHAT) FlowChatCyan else SmsBlue,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (currentSendMode == ChatSendMode.FLOW_CHAT) "END-TO-END ENCRYPTED FLOW CHAT" else "SMS GATEWAY",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (currentSendMode == ChatSendMode.FLOW_CHAT) FlowChatCyan else SmsBlue,
                                        letterSpacing = 1.sp
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (currentSendMode == ChatSendMode.FLOW_CHAT)
                                    "Zero carrier charges • Unlimited messages, audio memos & photos"
                                else
                                    "Real-time carrier delivery via SIM gateway • Standard GSM 7-bit",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 10.sp,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            )
                        }
                    }
                }
            }

            items(conversation.messages, key = { it.id }) { msg ->
                IMessageBubble(
                    message = msg,
                    recipientName = conversation.recipientName,
                    onReactionSelect = { emoji ->
                        viewModel.toggleMessageReaction(conversation.recipientPhone, msg.id, emoji)
                    },
                    onReplyClick = {
                        replyingToMessage = msg
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            if (conversation.isTyping) {
                item {
                    TypingIndicatorBubble()
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }

        // Replying Preview Banner
        AnimatedVisibility(
            visible = replyingToMessage != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            replyingToMessage?.let { replyMsg ->
                Surface(
                    color = DarkSurfaceElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(28.dp)
                                    .background(if (replyMsg.isOutgoing) FlowChatCyan else SmsBlue, RoundedCornerShape(2.dp))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (replyMsg.isOutgoing) "Replying to yourself" else "Replying to ${conversation.recipientName}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (replyMsg.isOutgoing) FlowChatCyan else SmsBlue,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                )
                                Text(
                                    text = replyMsg.text,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        IconButton(
                            onClick = { replyingToMessage = null },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Cancel Reply", tint = TextMuted, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // Smart Reply Suggestions Row (iOS 1-Tap quick reply chips)
        if (smartReplies.isNotEmpty() && !isRecordingVoice) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(smartReplies) { reply ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = DarkSurfaceElevated,
                        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
                        modifier = Modifier.clickable {
                            messageInput = reply
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = "AI Reply",
                                tint = CyberCyan,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = reply,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextPrimary,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }
            }
        }

        // AI Composition & Mode Indicator Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // AI Assistant Quick Trigger
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onOpenAiAssistant() }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = "AI Assistant",
                    tint = CyberCyan,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "AI Smart Copilot",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan,
                        fontSize = 11.sp
                    )
                )
            }

            // Mode & Cost Indicator
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (currentSendMode == ChatSendMode.FLOW_CHAT) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = FlowChatCyan.copy(alpha = 0.15f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(FlowChatCyan))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Flow Chat • ₦0.00",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = FlowChatCyan,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                } else {
                    Text(
                        text = "${costMetrics.charCount}/160 (${costMetrics.segmentCount} Seg)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (costMetrics.charCount > 160) GlowingAmber else TextMuted,
                            fontSize = 10.sp
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "₦${String.format("%.2f", costMetrics.totalCost)}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = SmsBlue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }

        // Bottom Message Composer (iOS Messages & WhatsApp hybrid style)
        Surface(
            color = DarkSurface,
            tonalElevation = 8.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
        ) {
            if (isRecordingVoice) {
                // Live Audio Memo Recording Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Cancel Voice Note
                    IconButton(
                        onClick = { isRecordingVoice = false },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = "Cancel Recording", tint = CoralRed)
                    }

                    // Pulsing Red Record Light & Timer
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                    ) {
                        val infiniteTransition = rememberInfiniteTransition(label = "recording_pulse")
                        val alpha by infiniteTransition.animateFloat(
                            initialValue = 0.3f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(600),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "record_alpha"
                        )
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(CoralRed.copy(alpha = alpha))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Recording Voice Memo: ${voiceDurationSec / 60}:${String.format("%02d", voiceDurationSec % 60)}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        )
                    }

                    // Send Voice Note Button
                    IconButton(
                        onClick = {
                            val duration = if (voiceDurationSec > 0) voiceDurationSec else 3
                            isRecordingVoice = false
                            viewModel.sendDirectMessage(
                                recipientPhone = conversation.recipientPhone,
                                messageText = "Voice Message (${duration}s)",
                                recipientName = conversation.recipientName,
                                messageType = "VOICE",
                                voiceDurationSec = duration
                            )
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(FlowChatCyan)
                            .testTag("send_voice_memo_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send Voice Memo",
                            tint = DarkObsidian,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // iOS Plus Button (Opens rich attachment drawer)
                    IconButton(
                        onClick = { showAttachmentSheet = true },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceElevated)
                            .testTag("open_attachments_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Add Media",
                            tint = CyberCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Input Pill
                    OutlinedTextField(
                        value = messageInput,
                        onValueChange = { messageInput = it },
                        placeholder = {
                            Text(
                                text = if (currentSendMode == ChatSendMode.FLOW_CHAT) "Flow Chat" else "SMS",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sms_message_input"),
                        shape = RoundedCornerShape(22.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DarkSurfaceElevated,
                            unfocusedContainerColor = DarkSurfaceElevated,
                            focusedBorderColor = if (currentSendMode == ChatSendMode.FLOW_CHAT) FlowChatCyan else SmsBlue,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        maxLines = 4,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = { handleSendMessage() }
                        )
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    if (messageInput.isBlank()) {
                        // Mic Button for 1-Tap Voice Recording
                        IconButton(
                            onClick = { isRecordingVoice = true },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(DarkSurfaceElevated)
                                .testTag("record_audio_note_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Mic,
                                contentDescription = "Record Voice Memo",
                                tint = FlowChatCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        // Send Circle Button (Primary FlowChatCyan for Flow Chat, Blue for SMS)
                        IconButton(
                            onClick = { handleSendMessage() },
                            enabled = !isSending,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (currentSendMode == ChatSendMode.FLOW_CHAT) FlowChatCyan else SmsBlue)
                                .testTag("send_sms_button")
                        ) {
                            if (isSending) {
                                FlowButtonLoadingLine(
                                    width = 22.dp,
                                    height = 2.dp,
                                    color = if (currentSendMode == ChatSendMode.FLOW_CHAT) DarkObsidian else Color.White
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send",
                                    tint = if (currentSendMode == ChatSendMode.FLOW_CHAT) DarkObsidian else Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Attachment Modal Bottom Sheet
    if (showAttachmentSheet) {
        IosAttachmentSheet(
            onSendPhoto = { description ->
                showAttachmentSheet = false
                // Trigger modern zero-permission photo picker
                photoPickerLauncher.launch(
                    androidx.activity.result.PickVisualMediaRequest(
                        ActivityResultContracts.PickVisualMedia.ImageOnly
                    )
                )
            },
            onSendVoice = {
                showAttachmentSheet = false
                isRecordingVoice = true
            },
            onSendDocument = {
                showAttachmentSheet = false
                // Trigger document file picker for PDF, DOC, TXT, etc.
                documentPickerLauncher.launch(
                    arrayOf(
                        "application/pdf",
                        "application/msword",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        "text/plain"
                    )
                )
            },
            onCloudBackup = {
                showAttachmentSheet = false
                viewModel.syncChatToR2(conversation.id) { res ->
                    android.widget.Toast.makeText(
                        threadContext,
                        if (res.isSuccess) "☁️ R2 Backup: ${res.message}" else "⚠️ Backup error: ${res.message}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            },
            onSendGift = { code ->
                showAttachmentSheet = false
                viewModel.sendDirectMessage(
                    recipientPhone = conversation.recipientPhone,
                    messageText = "🎁 Sent you 1GB Free Data Pass! Code: $code",
                    recipientName = conversation.recipientName,
                    messageType = "GIFT",
                    mediaDescription = code
                )
            },
            onSendLocation = {
                showAttachmentSheet = false
                viewModel.sendDirectMessage(
                    recipientPhone = conversation.recipientPhone,
                    messageText = "📍 Location Shared: Lagos, Nigeria (Via Flow Encrypted P2P)",
                    recipientName = conversation.recipientName
                )
            },
            onDismiss = { showAttachmentSheet = false }
        )
    }
}

// Backward compatibility bridge
@Composable
fun IosMessageBubble(
    message: VpnViewModel.SmsMessageItem,
    recipientPhone: String,
    recipientName: String = ""
) {
    IMessageBubble(
        message = message,
        recipientName = recipientName.ifBlank { recipientPhone },
        onReactionSelect = {},
        onReplyClick = {}
    )
}

@Composable
fun NewSmsConversationDialog(
    viewModel: VpnViewModel,
    retailPrice: Double,
    onDismiss: () -> Unit,
    onRequestContactSync: () -> Unit = {},
    onConversationCreated: (String) -> Unit
) {
    val context = LocalContext.current
    var phoneInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }
    var messageInput by remember { mutableStateOf("") }
    var newChatMode by remember { mutableStateOf(ChatSendMode.FLOW_CHAT) }
    var isSending by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var showInternalContactPicker by remember { mutableStateOf(false) }

    val countrySuggestion = remember(phoneInput) {
        GeminiSmsAssistant.detectCountryCode(phoneInput)
    }

    val costMetrics = remember(messageInput) {
        viewModel.calculateSmsCost(messageInput, 1)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = "New Chat",
                        tint = if (newChatMode == ChatSendMode.FLOW_CHAT) FlowChatCyan else SmsBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (newChatMode == ChatSendMode.FLOW_CHAT) "New Flow Chat" else "New SMS",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }

                TextButton(
                    onClick = { showInternalContactPicker = true },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Filled.Contacts, contentDescription = null, tint = FlowChatCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Contacts", color = FlowChatCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Mode Selector Strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurfaceElevated)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Flow Chat Option
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (newChatMode == ChatSendMode.FLOW_CHAT) FlowChatCyan else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { newChatMode = ChatSendMode.FLOW_CHAT }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Bolt,
                                contentDescription = null,
                                tint = if (newChatMode == ChatSendMode.FLOW_CHAT) DarkObsidian else TextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Flow Chat (₦0)",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (newChatMode == ChatSendMode.FLOW_CHAT) DarkObsidian else TextMuted
                            )
                        }
                    }

                    // SMS Option
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (newChatMode == ChatSendMode.SMS) SmsBlue else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { newChatMode = ChatSendMode.SMS }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Sensors,
                                contentDescription = null,
                                tint = if (newChatMode == ChatSendMode.SMS) Color.White else TextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "SMS",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (newChatMode == ChatSendMode.SMS) Color.White else TextMuted
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Recipient Phone Number",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = phoneInput,
                    onValueChange = {
                        phoneInput = it
                        errorMsg = null
                    },
                    placeholder = { Text("e.g. 080XXXXXXXX or +234...", color = TextMuted) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (newChatMode == ChatSendMode.FLOW_CHAT) FlowChatCyan else SmsBlue,
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                // Auto-suggest Country Code Banner / Pill
                if (countrySuggestion != null && !phoneInput.startsWith("+")) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = CyberCyan.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                phoneInput = countrySuggestion.normalizedNumber
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = countrySuggestion.country.flagEmoji, fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Apply ${countrySuggestion.country.dialPrefix} (${countrySuggestion.country.countryName})",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = CyberCyan,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                            Text(
                                text = "TAP TO FIX",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 9.sp,
                                    color = DarkObsidian
                                ),
                                modifier = Modifier
                                    .background(CyberCyan, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Contact Name (Optional)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    placeholder = { Text("e.g. John Doe / Client", color = TextMuted) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (newChatMode == ChatSendMode.FLOW_CHAT) FlowChatCyan else SmsBlue,
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "First Message",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    if (newChatMode == ChatSendMode.FLOW_CHAT) {
                        Text(
                            text = "Unlimited Free P2P • ₦0.00",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = FlowChatCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        )
                    } else {
                        Text(
                            text = "${costMetrics.charCount}/160 • Cost: ₦${String.format("%.2f", costMetrics.totalCost)}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = SmsBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = messageInput,
                    onValueChange = { messageInput = it },
                    placeholder = { Text(if (newChatMode == ChatSendMode.FLOW_CHAT) "Type Flow Chat message..." else "Type SMS...", color = TextMuted) },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (newChatMode == ChatSendMode.FLOW_CHAT) FlowChatCyan else SmsBlue,
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                if (errorMsg != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMsg ?: "",
                        color = CoralRed,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (phoneInput.isBlank() || messageInput.isBlank()) {
                        errorMsg = "Please enter both phone number and message."
                        return@Button
                    }
                    isSending = true
                    val normalized = countrySuggestion?.normalizedNumber ?: HttpSmsService.normalizePhoneNumber(phoneInput)
                    if (newChatMode == ChatSendMode.FLOW_CHAT) {
                        viewModel.sendDirectMessage(
                            recipientPhone = normalized,
                            messageText = messageInput,
                            recipientName = nameInput.ifBlank { normalized }
                        ) { success, msg ->
                            isSending = false
                            if (success) {
                                onConversationCreated(normalized)
                            } else {
                                errorMsg = msg
                            }
                        }
                    } else {
                        viewModel.sendChatMessage(
                            recipientPhone = normalized,
                            messageText = messageInput,
                            recipientName = nameInput.ifBlank { normalized }
                        ) { success, msg ->
                            isSending = false
                            if (success) {
                                onConversationCreated(normalized)
                            } else {
                                errorMsg = msg
                            }
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (newChatMode == ChatSendMode.FLOW_CHAT) FlowChatCyan else SmsBlue
                ),
                enabled = !isSending
            ) {
                if (isSending) {
                    FlowButtonLoadingLine(
                        width = 44.dp,
                        color = if (newChatMode == ChatSendMode.FLOW_CHAT) DarkObsidian else Color.White
                    )
                } else {
                    Text(
                        text = if (newChatMode == ChatSendMode.FLOW_CHAT) "START FLOW CHAT" else "SEND SMS",
                        color = if (newChatMode == ChatSendMode.FLOW_CHAT) DarkObsidian else Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )

    if (showInternalContactPicker) {
        ContactPickerDialog(
            onDismiss = { showInternalContactPicker = false },
            onSelectContact = { contact ->
                phoneInput = contact.phoneNumber
                nameInput = contact.name
                showInternalContactPicker = false
            }
        )
    }
}

@Composable
fun ContactPickerDialog(
    onDismiss: () -> Unit,
    onSelectContact: (DeviceContact) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    val allContacts = remember { ContactsHelper.getDeviceContacts(context) }
    val filteredContacts = remember(searchQuery, allContacts) {
        if (searchQuery.isBlank()) {
            allContacts
        } else {
            allContacts.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.phoneNumber.contains(searchQuery)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Contacts,
                        contentDescription = "Contacts",
                        tint = CyberCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Select Contact",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = CyberCyan.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${allContacts.size} SYNCED",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = CyberCyan,
                            fontSize = 9.sp
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search name or phone...", color = TextMuted, fontSize = 12.sp) },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (filteredContacts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No matching contacts found.",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredContacts, key = { it.id + it.phoneNumber }) { contact ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = DarkSurfaceElevated,
                                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectContact(contact) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(CyberCyan),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = contact.name.take(2).uppercase(),
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = contact.name,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${contact.flagEmoji} ${contact.phoneNumber}",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = CyberCyan,
                                                fontSize = 11.sp
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Icon(
                                        imageVector = Icons.Filled.ChevronRight,
                                        contentDescription = null,
                                        tint = TextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
fun PricingRow(
    label: String,
    value: String,
    isHighlight: Boolean = false,
    valueColor: Color = TextPrimary
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                color = if (isHighlight) TextPrimary else TextSecondary,
                fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Normal
            )
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                color = if (isHighlight) CyberCyan else valueColor
            )
        )
    }
}

@Composable
fun AiSmsComposerDialog(
    recipientName: String,
    onDismiss: () -> Unit,
    onApplyGeneratedText: (String) -> Unit
) {
    var promptInput by remember { mutableStateOf("") }
    var selectedTone by remember { mutableStateOf("Formal Business") }
    var generatedResult by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val tones = listOf(
        "💼 Formal Business",
        "⚡ Quick Reminder",
        "🏷️ Promo & Sales",
        "🚨 Urgent Notice",
        "🎉 Friendly Casual",
        "🔐 2FA / OTP Code"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = "AI Assistant",
                    tint = CyberCyan,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AI SMS Copywriter",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Select Message Tone",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(tones) { tone ->
                        val isSelected = selectedTone.contains(tone.drop(3))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) CyberCyan else DarkSurfaceElevated,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) CyberCyan else GlassBorder
                            ),
                            modifier = Modifier.clickable { selectedTone = tone }
                        ) {
                            Text(
                                text = tone,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isSelected) DarkObsidian else TextPrimary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 11.sp
                                ),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "What is this SMS about?",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = promptInput,
                    onValueChange = { promptInput = it },
                    placeholder = { Text("e.g. Notify client their package has arrived at pickup depot...", color = TextMuted) },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        isGenerating = true
                        scope.launch {
                            val res = GeminiSmsAssistant.composeSmsWithAi(
                                rawTopicOrDraft = promptInput,
                                tone = selectedTone,
                                recipientName = recipientName
                            )
                            generatedResult = res
                            isGenerating = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                    enabled = !isGenerating
                ) {
                    if (isGenerating) {
                        FlowButtonLoadingLine(width = 44.dp, color = DarkObsidian)
                    } else {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = "Generate",
                            tint = DarkObsidian,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "GENERATE 1-SEGMENT SMS",
                            color = DarkObsidian,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.5.sp
                        )
                    }
                }

                if (generatedResult.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "AI Generated Message (${generatedResult.length}/160 Chars):",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = ElectricEmerald,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = DarkSurfaceElevated,
                        border = androidx.compose.foundation.BorderStroke(1.dp, ElectricEmerald.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = generatedResult,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextPrimary,
                                fontSize = 13.sp
                            ),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (generatedResult.isNotBlank()) {
                Button(
                    onClick = { onApplyGeneratedText(generatedResult) },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian)
                ) {
                    Text("USE THIS COPY", color = DarkObsidian, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = TextSecondary)
            }
        }
    )
}
