package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ui.viewmodel.VpnViewModel
import com.example.ui.motion.MotionTransitions
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.interaction.MutableInteractionSource
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.example.ui.screens.DataRenewalReminderDialog
import com.example.ui.theme.*
import com.example.BuildConfig
import com.example.ui.components.FlowLoadingLine
import com.example.util.AppUpdateManager
import com.example.util.UpdateStatus

/**
 * User Avatar Button for Top Bar
 */
@Composable
fun UserAvatarButton(
    userVirtualAccount: VpnViewModel.UserVirtualAccount,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val savedNameFromPrefs = remember {
        try {
            val prefs = context.getSharedPreferences("auth_prefs", android.content.Context.MODE_PRIVATE)
            prefs.getString("saved_user_name", "") ?: ""
        } catch (_: Exception) { "" }
    }
    val effectiveName = userVirtualAccount.fullName.ifBlank { savedNameFromPrefs.ifBlank { "User" } }

    val initials = remember(effectiveName) {
        val parts = effectiveName.trim().split(" ")
        if (parts.size >= 2) {
            "${parts[0].take(1)}${parts[1].take(1)}".uppercase()
        } else if (parts.isNotEmpty() && parts[0].isNotEmpty()) {
            parts[0].take(2).uppercase()
        } else {
            "AZ"
        }
    }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .clickable { onClick() }
            .background(CyberCyan)
            .padding(2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(DarkSurfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Black,
                    color = CyberCyan,
                    fontSize = 13.sp
                )
            )

            // Online Badge Dot
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(ElectricEmerald)
                    .border(1.5.dp, DarkObsidian, CircleShape)
                    .align(Alignment.TopEnd)
            )
        }
    }
}

/**
 * Full-Screen Profile & Settings Drawer
 */
@Composable
fun UserProfileDrawer(
    viewModel: VpnViewModel,
    onDismiss: () -> Unit,
    onOpenAdminDashboard: () -> Unit,
    onOpenFundWallet: () -> Unit,
    onOpenHistory: () -> Unit
) {
    val userVirtualAccount by viewModel.userVirtualAccount.collectAsStateWithLifecycle()
    val userWalletBalance by viewModel.userWalletBalance.collectAsStateWithLifecycle()
    val isFingerprintEnabled by viewModel.isFingerprintEnabled.collectAsStateWithLifecycle()
    val isTransactionBiometricEnabled by viewModel.isTransactionBiometricEnabled.collectAsStateWithLifecycle()
    val isAppLoggedIn by viewModel.isAppLoggedIn.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
    val isDataRenewalReminderEnabled by viewModel.isDataRenewalReminderEnabled.collectAsStateWithLifecycle()
    val dataReminderThresholdMb by viewModel.dataReminderThresholdMb.collectAsStateWithLifecycle()
    val killSwitchEnabled by viewModel.killSwitchEnabled.collectAsStateWithLifecycle()
    val dataSaverStats by viewModel.dataSaverStats.collectAsStateWithLifecycle()
    val purchasedDataTotalMb by viewModel.purchasedDataTotalMb.collectAsStateWithLifecycle()
    val firewallApps by viewModel.firewallApps.collectAsStateWithLifecycle()
    val totalAppsConsumedMb = remember(firewallApps) { viewModel.getTotalAppsConsumedMb() }
    val estimatedRemainingMb by viewModel.estimatedDataBalanceMb.collectAsStateWithLifecycle()
    var showDataRenewalReminderDialog by remember { mutableStateOf(false) }

    val updateStatus by AppUpdateManager.updateStatus.collectAsStateWithLifecycle()
    var showAppUpdateDialog by remember { mutableStateOf(false) }

    val clipboardManager = LocalClipboardManager.current
    var copySuccessMsg by remember { mutableStateOf<String?>(null) }

    var showChangePinSection by remember { mutableStateOf(false) }
    var oldPinInput by remember { mutableStateOf("") }
    var newPinInput by remember { mutableStateOf("") }
    var pinMsg by remember { mutableStateOf<String?>(null) }
    var isPinSuccess by remember { mutableStateOf(false) }

    var showFundingBankSection by remember { mutableStateOf(false) }
    val savedFundingBankName by viewModel.savedFundingBankName.collectAsStateWithLifecycle()
    val savedFundingAccountNumber by viewModel.savedFundingAccountNumber.collectAsStateWithLifecycle()
    val savedFundingAccountName by viewModel.savedFundingAccountName.collectAsStateWithLifecycle()
    val isFundingAccountSaved by viewModel.isFundingAccountSaved.collectAsStateWithLifecycle()

    var fundingBankInput by remember(savedFundingBankName) { mutableStateOf(savedFundingBankName) }
    var fundingAccountInput by remember(savedFundingAccountNumber) { mutableStateOf(savedFundingAccountNumber) }
    var fundingNameInput by remember(savedFundingAccountName, userVirtualAccount.fullName) {
        mutableStateOf(savedFundingAccountName.ifBlank { userVirtualAccount.fullName })
    }
    var fundingSuccessMsg by remember { mutableStateOf<String?>(null) }
    var fundingErrorMsg by remember { mutableStateOf<String?>(null) }
    var isSavingFundingBank by remember { mutableStateOf(false) }

    var showPhoneEditDialog by remember { mutableStateOf(false) }
    var phoneEditInput by remember { mutableStateOf("") }
    var phoneEditError by remember { mutableStateOf<String?>(null) }

    var showAdminPasscodeDialog by remember { mutableStateOf(false) }
    var adminPasscodeInput by remember { mutableStateOf("") }
    var adminPasscodeError by remember { mutableStateOf<String?>(null) }
    var showBeneficiariesManager by remember { mutableStateOf(false) }
    var showAboutUsDialog by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val savedNameFromPrefs = remember {
        try {
            val prefs = context.getSharedPreferences("auth_prefs", android.content.Context.MODE_PRIVATE)
            prefs.getString("saved_user_name", "") ?: ""
        } catch (_: Exception) { "" }
    }

    val savedRecipients by viewModel.savedRecipients.collectAsStateWithLifecycle()

    var isDrawerVisible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        isDrawerVisible = true
    }

    val requestCloseDrawer: () -> Unit = {
        scope.launch {
            isDrawerVisible = false
            kotlinx.coroutines.delay(MotionTransitions.DRAWER_TRANSITION_MS.toLong() - 40)
            onDismiss()
        }
    }

    val scrimAlpha by animateFloatAsState(
        targetValue = if (isDrawerVisible) 0.72f else 0.0f,
        animationSpec = tween(MotionTransitions.DRAWER_TRANSITION_MS, easing = FastOutSlowInEasing),
        label = "UserProfileScrimAlpha"
    )

    Dialog(
        onDismissRequest = { requestCloseDrawer() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrimAlpha))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { requestCloseDrawer() },
            contentAlignment = Alignment.BottomCenter
        ) {
            AnimatedVisibility(
                visible = isDrawerVisible,
                enter = MotionTransitions.DrawerTransitions.springSlideInFromBottom,
                exit = MotionTransitions.DrawerTransitions.springSlideOutToBottom
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.94f)
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { /* absorb taps inside sheet */ },
                    color = DarkObsidian
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        // Drag handle pill
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(vertical = 8.dp)
                                .width(44.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(alpha = 0.35f))
                        )

                        // Top Navigation Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { requestCloseDrawer() },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(DarkSurfaceElevated)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Close Profile Drawer",
                                    tint = TextPrimary
                                )
                            }

                    Text(
                        text = "PROFILE & SETTINGS",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp,
                            color = TextPrimary
                        )
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ElectricEmerald.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ElectricEmerald.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(ElectricEmerald)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "VERIFIED",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricEmerald,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Profile Avatar Banner Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.dp, GlassBorder, RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(CyberCyan)
                                .padding(3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(DarkSurface),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "User Avatar",
                                    tint = CyberCyan,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val firebaseUser by viewModel.firebaseUser.collectAsStateWithLifecycle()
                        val isEmailVerified by viewModel.isEmailVerified.collectAsStateWithLifecycle()

                        val baseDisplayName = userVirtualAccount.fullName.ifBlank {
                            firebaseUser?.displayName?.takeIf { it.isNotBlank() } ?: savedNameFromPrefs.takeIf { it.isNotBlank() } ?: "Valued User"
                        }
                        val sanitizedDisplayName = baseDisplayName
                            .replace("Pairgate", "FlowTest", ignoreCase = true)
                            .replace("Inosoft", "FlowTest", ignoreCase = true)
                            .replace("PAIRGATE / ", "")
                            .replace("PAIRGATE", "FLOWTEST")

                        val sanitizedEmail = userVirtualAccount.email
                            .replace("Pairgate", "FlowTest", ignoreCase = true)
                            .replace("Inosoft", "FlowTest", ignoreCase = true)
                            .ifBlank { firebaseUser?.email ?: "No email registered" }

                        Text(
                            text = sanitizedDisplayName,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                color = TextPrimary
                            )
                        )

                        Text(
                            text = sanitizedEmail,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Firebase Email Verification Status Pill
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isEmailVerified) ElectricEmerald.copy(alpha = 0.15f) else Color(0xFFFF9800).copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isEmailVerified) ElectricEmerald else Color(0xFFFF9800))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isEmailVerified) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (isEmailVerified) ElectricEmerald else Color(0xFFFF9800),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isEmailVerified) "Authenticated & Verified" else "Unverified Email (Tap to verify)",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isEmailVerified) ElectricEmerald else Color(0xFFFF9800),
                                        fontSize = 10.5.sp
                                    ),
                                    modifier = Modifier.clickable {
                                        if (!isEmailVerified) {
                                            viewModel.resendEmailVerification(
                                                onSuccess = {
                                                    android.widget.Toast.makeText(context, "Verification email sent to $sanitizedEmail", android.widget.Toast.LENGTH_LONG).show()
                                                },
                                                onError = { err ->
                                                    android.widget.Toast.makeText(context, err, android.widget.Toast.LENGTH_LONG).show()
                                                }
                                            )
                                        }
                                    }
                                )
                            }
                        }

                        if (userVirtualAccount.phoneNumber.isNotBlank() && userVirtualAccount.phoneNumber != "Unassigned") {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        phoneEditInput = userVirtualAccount.phoneNumber
                                        phoneEditError = null
                                        showPhoneEditDialog = true
                                    }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = userVirtualAccount.phoneNumber,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextMuted,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Phone",
                                    tint = CyberCyan.copy(alpha = 0.7f),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = CyberCyan.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, CyberCyan),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        phoneEditInput = ""
                                        phoneEditError = null
                                        showPhoneEditDialog = true
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = "Link Phone",
                                        tint = CyberCyan,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "+ Link Real Phone Number",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = CyberCyan,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Quick Actions: Saved Contacts & Transaction History
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { showBeneficiariesManager = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Bookmarks, contentDescription = "Beneficiaries", tint = CyberCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Contacts (${savedRecipients.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onOpenHistory()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.History, contentDescription = "History", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("History", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Profile Settings & Security
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                        // 1. DATA RENEWAL ALERT & THRESHOLD CARD
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .border(1.dp, DarkCardBorder, RoundedCornerShape(18.dp)),
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (isDataRenewalReminderEnabled) Icons.Default.Shield else Icons.Outlined.Shield,
                                            contentDescription = "Renewal Alert",
                                            tint = if (isDataRenewalReminderEnabled) ElectricEmerald else CyberCyan,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "DATA RENEWAL ALERT",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary,
                                                fontSize = 11.sp
                                            )
                                        )
                                    }

                                    Switch(
                                        checked = isDataRenewalReminderEnabled,
                                        onCheckedChange = { isChecked ->
                                            viewModel.updateDataRenewalReminderSettings(
                                                isChecked,
                                                dataReminderThresholdMb,
                                                System.currentTimeMillis() + 28L * 86400000L,
                                                context
                                            )
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = DarkObsidian,
                                            checkedTrackColor = CyberCyan
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Receive proactive low-data alerts on your phone when remaining data drops below your set limit so your connection never drops unexpectedly.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = DarkSurfaceElevated,
                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, DarkCardBorder),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { showDataRenewalReminderDialog = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "Current Alert Limit",
                                                style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 10.sp)
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = if (dataReminderThresholdMb >= 1024.0) String.format(java.util.Locale.US, "%.1f GB Remaining", dataReminderThresholdMb / 1024.0) else "${dataReminderThresholdMb.toInt()} MB Remaining",
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    color = if (isDataRenewalReminderEnabled) ElectricEmerald else TextPrimary,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                )
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = CyberCyan.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "Configure ⚙️",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = CyberCyan,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp
                                                ),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 2. DATA SAVER & TRAFFIC RULES CARD
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .border(1.dp, DarkCardBorder, RoundedCornerShape(18.dp)),
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "DATA SAVER & TRAFFIC OPTIMIZATION",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary,
                                        fontSize = 10.sp
                                    )
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Master Firewall / Data Saver
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Shield,
                                            contentDescription = "Data Saver",
                                            tint = ElectricEmerald,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text("Data Saver Mode", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
                                            Text("Save up to 85% background MB", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 11.sp))
                                        }
                                    }

                                    Switch(
                                        checked = dataSaverStats?.isMasterFirewallEnabled == true,
                                        onCheckedChange = { viewModel.toggleMasterFirewall(it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = DarkObsidian,
                                            checkedTrackColor = ElectricEmerald
                                        )
                                    )
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = DarkCardBorder)

                                // Ad & Tracker Blocker
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Block,
                                            contentDescription = "Ad Blocker",
                                            tint = WarningRed,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text("Ad & Tracker Shield", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
                                            Text("Block invasive trackers & ad banners", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 11.sp))
                                        }
                                    }

                                    Switch(
                                        checked = dataSaverStats?.isAdBlockerEnabled == true,
                                        onCheckedChange = { viewModel.toggleAdBlocker(it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = DarkObsidian,
                                            checkedTrackColor = CyberCyan
                                        )
                                    )
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = DarkCardBorder)

                                // Compression Proxy
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Compress,
                                            contentDescription = "Compression",
                                            tint = CyberCyan,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text("Data Compression Proxy", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
                                            Text("Compress web images and traffic", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 11.sp))
                                        }
                                    }

                                    Switch(
                                        checked = dataSaverStats?.isCompressionProxyEnabled == true,
                                        onCheckedChange = { viewModel.toggleCompressionProxy(it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = DarkObsidian,
                                            checkedTrackColor = CyberCyan
                                        )
                                    )
                                }
                            }
                        }

                        // 3. SECURITY & PREFERENCES CARD
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .border(1.dp, DarkCardBorder, RoundedCornerShape(18.dp)),
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "SECURITY & PREFERENCES",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary,
                                        fontSize = 10.sp
                                    )
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Primary Phone Number
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.PhoneAndroid,
                                            contentDescription = "Phone Number",
                                            tint = CyberCyan,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "Primary Phone Number",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary
                                                )
                                            )
                                            Text(
                                                text = if (userVirtualAccount.phoneNumber.isNotBlank() && userVirtualAccount.phoneNumber != "Unassigned") userVirtualAccount.phoneNumber else "Not Linked (Tap to Set)",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = CyberCyan,
                                                    fontSize = 11.sp,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            )
                                        }
                                    }

                                    TextButton(
                                        onClick = {
                                            phoneEditInput = if (userVirtualAccount.phoneNumber != "Unassigned") userVirtualAccount.phoneNumber else ""
                                            phoneEditError = null
                                            showPhoneEditDialog = true
                                        }
                                    ) {
                                        Text(
                                            text = if (userVirtualAccount.phoneNumber.isNotBlank() && userVirtualAccount.phoneNumber != "Unassigned") "Change" else "Set Up",
                                            color = CyberCyan,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = DarkCardBorder)

                                // Biometric Switch
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Fingerprint,
                                            contentDescription = "Biometrics",
                                            tint = CyberCyan,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "Biometric Lock",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary
                                                )
                                            )
                                            Text(
                                                text = "Unlock COT Digital with Fingerprint",
                                                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 11.sp)
                                            )
                                        }
                                    }

                                    Switch(
                                        checked = isFingerprintEnabled,
                                        onCheckedChange = { viewModel.toggleFingerprint(it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = DarkObsidian,
                                            checkedTrackColor = CyberCyan
                                        )
                                    )
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = DarkCardBorder)

                                // Transaction Biometric Paywall Switch
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Shield,
                                            contentDescription = "Paywall Biometrics",
                                            tint = ElectricEmerald,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "Transaction Biometrics",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary
                                                )
                                            )
                                            Text(
                                                text = "Authorize airtime & data with thumbprint/face",
                                                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 11.sp)
                                            )
                                        }
                                    }

                                    Switch(
                                        checked = isTransactionBiometricEnabled,
                                        onCheckedChange = { viewModel.toggleTransactionBiometric(it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = DarkObsidian,
                                            checkedTrackColor = ElectricEmerald
                                        )
                                    )
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = DarkCardBorder)

                                // Kill Switch
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.PowerSettingsNew,
                                            contentDescription = "Kill Switch",
                                            tint = CyberCyan,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "VPN Kill Switch",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary
                                                )
                                            )
                                            Text(
                                                text = "Block all internet if VPN unexpectedly drops",
                                                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 11.sp)
                                            )
                                        }
                                    }

                                    Switch(
                                        checked = killSwitchEnabled,
                                        onCheckedChange = { viewModel.toggleKillSwitch(it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = DarkObsidian,
                                            checkedTrackColor = CyberCyan
                                        )
                                    )
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = DarkCardBorder)

                                // Change Passcode Row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showChangePinSection = !showChangePinSection },
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Passcode",
                                            tint = ElectricEmerald,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "Security PIN (Login & Transactions)",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary
                                                )
                                            )
                                            Text(
                                                text = "Update passcode for login & transaction authorization",
                                                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 11.sp)
                                            )
                                        }
                                    }

                                    Icon(
                                        imageVector = if (showChangePinSection) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = "Toggle PIN edit",
                                        tint = TextMuted
                                    )
                                }

                                if (showChangePinSection) {
                                    Spacer(modifier = Modifier.height(12.dp))

                                    OutlinedTextField(
                                        value = oldPinInput,
                                        onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) oldPinInput = it },
                                        label = { Text("Current PIN") },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan, unfocusedBorderColor = DarkCardBorder),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    OutlinedTextField(
                                        value = newPinInput,
                                        onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) newPinInput = it },
                                        label = { Text("New PIN (4-6 Digits)") },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan, unfocusedBorderColor = DarkCardBorder),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    pinMsg?.let { msg ->
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = msg,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = if (isPinSuccess) ElectricEmerald else WarningRed,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Button(
                                        onClick = {
                                            viewModel.changeUserPin(
                                                oldPin = oldPinInput,
                                                newPin = newPinInput,
                                                onSuccess = {
                                                    isPinSuccess = true
                                                    pinMsg = "Security PIN Updated Successfully!"
                                                    oldPinInput = ""
                                                    newPinInput = ""
                                                },
                                                onError = { err ->
                                                    isPinSuccess = false
                                                    pinMsg = err
                                                }
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("UPDATE SECURITY PIN", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // =========================================================================
                    // FUNDING BANK ACCOUNT CARD (AUTOMATE WEBHOOKS)
                    // Matches user request: "Add it to the profile, just as you added the form for changing pin."
                    // =========================================================================
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = DarkSurfaceElevated,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isFundingAccountSaved) CyberCyan.copy(alpha = 0.5f) else DarkCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showFundingBankSection = !showFundingBankSection },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalance,
                                        contentDescription = "Funding Bank",
                                        tint = CyberCyan,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "Funding Bank Account",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary
                                                )
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (isFundingAccountSaved) ElectricEmerald.copy(alpha = 0.15f) else GlowingAmber.copy(alpha = 0.15f),
                                                border = androidx.compose.foundation.BorderStroke(
                                                    1.dp,
                                                    if (isFundingAccountSaved) ElectricEmerald.copy(alpha = 0.5f) else GlowingAmber.copy(alpha = 0.4f)
                                                )
                                            ) {
                                                Text(
                                                    text = if (isFundingAccountSaved) "● LINKED" else "● NOT SET",
                                                    color = if (isFundingAccountSaved) ElectricEmerald else GlowingAmber,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 9.sp,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = if (isFundingAccountSaved) "$savedFundingBankName • ${if (savedFundingAccountNumber.length >= 4) "••••" + savedFundingAccountNumber.takeLast(4) else savedFundingAccountNumber} ($savedFundingAccountName)"
                                                else "Link sender bank for 100% instant automated transfers",
                                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 11.sp),
                                            maxLines = 1
                                        )
                                    }
                                }

                                Icon(
                                    imageVector = if (showFundingBankSection) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = "Toggle Funding Bank Edit",
                                    tint = TextMuted
                                )
                            }

                            if (showFundingBankSection) {
                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "Enter the bank account you transfer from. Whenever you send money to our Moniepoint corporate account from this bank account, our webhook detects it and auto-credits your wallet in seconds—no codes or manual verification needed!",
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.5.sp, lineHeight = 16.sp)
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = fundingBankInput,
                                    onValueChange = { fundingBankInput = it },
                                    label = { Text("Sender Bank Name (e.g. OPay, Moniepoint, GTBank)") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CyberCyan,
                                        unfocusedBorderColor = DarkCardBorder,
                                        focusedLabelColor = CyberCyan,
                                        unfocusedLabelColor = TextMuted,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = fundingAccountInput,
                                    onValueChange = { input ->
                                        val digits = input.filter { it.isDigit() }
                                        if (digits.length <= 11) fundingAccountInput = digits
                                    },
                                    label = { Text("Sender Account Number (10 Digits)") },
                                    singleLine = true,
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                    trailingIcon = {
                                        IconButton(
                                            onClick = {
                                                val clipText = clipboardManager.getText()?.text ?: ""
                                                val digits = clipText.filter { it.isDigit() }
                                                if (digits.isNotBlank()) fundingAccountInput = digits.take(11)
                                            }
                                        ) {
                                            Icon(imageVector = Icons.Default.ContentPaste, contentDescription = "Paste", tint = CyberCyan, modifier = Modifier.size(18.dp))
                                        }
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CyberCyan,
                                        unfocusedBorderColor = DarkCardBorder,
                                        focusedLabelColor = CyberCyan,
                                        unfocusedLabelColor = TextMuted,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = fundingNameInput,
                                    onValueChange = { fundingNameInput = it },
                                    label = { Text("Sender Account Name (As in your bank app)") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CyberCyan,
                                        unfocusedBorderColor = DarkCardBorder,
                                        focusedLabelColor = CyberCyan,
                                        unfocusedLabelColor = TextMuted,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                fundingErrorMsg?.let { err ->
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = err,
                                        style = MaterialTheme.typography.bodySmall.copy(color = WarningRed, fontWeight = FontWeight.Bold)
                                    )
                                }

                                fundingSuccessMsg?.let { msg ->
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = msg,
                                        style = MaterialTheme.typography.bodySmall.copy(color = ElectricEmerald, fontWeight = FontWeight.Bold)
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Button(
                                    onClick = {
                                        val cleanBank = fundingBankInput.trim()
                                        val cleanAcc = fundingAccountInput.trim()
                                        val cleanName = fundingNameInput.trim()

                                        if (cleanBank.isBlank()) {
                                            fundingErrorMsg = "Please enter your bank name."
                                            fundingSuccessMsg = null
                                            return@Button
                                        }
                                        if (cleanAcc.length < 10) {
                                            fundingErrorMsg = "Please enter a valid 10-digit account number."
                                            fundingSuccessMsg = null
                                            return@Button
                                        }
                                        if (cleanName.isBlank()) {
                                            fundingErrorMsg = "Please enter your account holder name."
                                            fundingSuccessMsg = null
                                            return@Button
                                        }

                                        fundingErrorMsg = null
                                        isSavingFundingBank = true
                                        viewModel.saveFundingBankAccount(cleanBank, cleanAcc, cleanName) { success, msg ->
                                            isSavingFundingBank = false
                                            if (success) {
                                                fundingSuccessMsg = "Funding Bank Account Saved! Webhook Auto-Detection Active."
                                            } else {
                                                fundingErrorMsg = msg
                                            }
                                        }
                                    },
                                    enabled = !isSavingFundingBank,
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (isSavingFundingBank) {
                                        FlowButtonLoadingLine(color = DarkObsidian, width = 32.dp, height = 2.dp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("SAVING...", fontWeight = FontWeight.Bold)
                                    } else {
                                        Text("SAVE FUNDING BANK DETAILS", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                Spacer(modifier = Modifier.height(20.dp))

                // In-App System Updates Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = DarkSurfaceElevated,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (updateStatus is UpdateStatus.ReadyToInstall) ElectricEmerald.copy(alpha = 0.8f)
                        else CyberCyan.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            AppUpdateManager.checkForUpdates(context)
                            showAppUpdateDialog = true
                        }
                ) {
                    Column {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
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
                                        .background(
                                            if (updateStatus is UpdateStatus.ReadyToInstall)
                                                ElectricEmerald.copy(alpha = 0.15f)
                                            else CyberCyan.copy(alpha = 0.15f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (updateStatus is UpdateStatus.ReadyToInstall)
                                            Icons.Default.Bolt
                                        else Icons.Default.SystemUpdate,
                                        contentDescription = "App Updates",
                                        tint = if (updateStatus is UpdateStatus.ReadyToInstall)
                                            ElectricEmerald
                                        else CyberCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "In-App Updates & Auto-Upgrade",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary,
                                                fontSize = 12.5.sp
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            color = if (updateStatus is UpdateStatus.ReadyToInstall)
                                                ElectricEmerald.copy(alpha = 0.2f)
                                            else CyberCyan.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = if (updateStatus is UpdateStatus.ReadyToInstall) "READY"
                                                else if (updateStatus is UpdateStatus.Checking) "SCANNING..."
                                                else if (updateStatus is UpdateStatus.Downloading) "${(updateStatus as UpdateStatus.Downloading).progressPercent}%"
                                                else "v${BuildConfig.VERSION_NAME}",
                                                color = if (updateStatus is UpdateStatus.ReadyToInstall)
                                                    ElectricEmerald
                                                else CyberCyan,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Black,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = if (updateStatus is UpdateStatus.ReadyToInstall)
                                            "⚡ Update downloaded! Tap to install (keeps all data)"
                                        else if (updateStatus is UpdateStatus.Checking)
                                            "Scanning FlowTest Cloud & GitHub Releases..."
                                        else if (updateStatus is UpdateStatus.Downloading)
                                            "Downloading update in background..."
                                        else "Check updates, auto-download over internet, no uninstall",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (updateStatus is UpdateStatus.ReadyToInstall) ElectricEmerald else TextSecondary,
                                            fontSize = 10.sp
                                        )
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = null,
                                tint = if (updateStatus is UpdateStatus.ReadyToInstall) ElectricEmerald else CyberCyan,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        if (updateStatus is UpdateStatus.Checking) {
                            FlowLoadingLine(
                                modifier = Modifier.fillMaxWidth(),
                                color = CyberCyan,
                                height = 2.dp
                            )
                        } else if (updateStatus is UpdateStatus.Downloading) {
                            FlowLoadingLine(
                                progress = (updateStatus as UpdateStatus.Downloading).progressPercent / 100f,
                                modifier = Modifier.fillMaxWidth(),
                                color = CyberCyan,
                                height = 2.dp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Share App Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = DarkSurfaceElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val refCode = "FLOW-" + userVirtualAccount.accountNumber.takeLast(5)
                            com.example.util.ApkSharingHelper.launchNativeShare(
                                context = context,
                                coroutineScope = coroutineScope,
                                referralCode = refCode
                            )
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
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
                                    .background(CyberCyan.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share",
                                    tint = CyberCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Share App",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    fontSize = 13.sp
                                )
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // About Us & Corporate Profile Button
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurfaceElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAboutUsDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FlowtestEmblem(
                                size = 22.dp,
                                primaryColor = CyberCyan,
                                glowAlpha = 0.2f,
                                animateRotationOnLoad = false
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "About FlowTest & INOSOFTTECH",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        fontSize = 12.sp
                                    )
                                )
                                Text(
                                    text = "Nigeria Office • CAC RC 9710966",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TextMuted,
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        }
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = TextMuted, modifier = Modifier.size(12.dp))
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Logout Button - Small, clean, with generous bottom padding
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.logout()
                            onDismiss()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = WarningRed.copy(alpha = 0.08f),
                            contentColor = WarningRed
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, WarningRed.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Logout",
                            tint = WarningRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Log Out",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = WarningRed,
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }
        }

        // Phone Edit & Verification Dialog
        if (showPhoneEditDialog) {
            PhoneVerificationDialog(
                viewModel = viewModel,
                initialPhone = phoneEditInput.ifBlank { userVirtualAccount.phoneNumber },
                onDismiss = { showPhoneEditDialog = false },
                onSuccess = { verifiedPhone ->
                    showPhoneEditDialog = false
                    copySuccessMsg = "Phone number verified and updated to $verifiedPhone!"
                }
            )
        }

        // Data Renewal Reminder Dialog
        if (showDataRenewalReminderDialog) {
            DataRenewalReminderDialog(
                enabled = isDataRenewalReminderEnabled,
                currentThresholdMb = dataReminderThresholdMb,
                estimatedRemainingMb = estimatedRemainingMb,
                onSave = { enabled, thresholdMb, renewalTimestamp ->
                    viewModel.updateDataRenewalReminderSettings(
                        enabled = enabled,
                        thresholdMb = thresholdMb,
                        renewalTimestamp = renewalTimestamp,
                        context = context
                    )
                    showDataRenewalReminderDialog = false
                },
                onDismiss = {
                    showDataRenewalReminderDialog = false
                }
            )
        }

        // Admin Master Passcode Unlock Dialog
        if (showAdminPasscodeDialog) {
            AlertDialog(
                onDismissRequest = { showAdminPasscodeDialog = false },
                containerColor = DarkSurfaceElevated,
                shape = RoundedCornerShape(18.dp),
                icon = {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(CyberCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.AdminPanelSettings, contentDescription = "Admin Unlock", tint = CyberCyan, modifier = Modifier.size(28.dp))
                    }
                },
                title = {
                    Text(
                        text = "Unlock Administrator Privileges",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, color = TextPrimary),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Enter your Master Admin Passcode or Key to return to Admin mode with full package management access:",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedTextField(
                            value = adminPasscodeInput,
                            onValueChange = {
                                adminPasscodeInput = it
                                adminPasscodeError = null
                            },
                            label = { Text("Master Admin Passcode") },
                            placeholder = { Text("779900") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberCyan,
                                unfocusedBorderColor = DarkCardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        adminPasscodeError?.let { err ->
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = err,
                                style = MaterialTheme.typography.bodySmall.copy(color = WarningRed, fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val success = viewModel.authenticateAdminWithKey(adminPasscodeInput)
                            if (success) {
                                showAdminPasscodeDialog = false
                                copySuccessMsg = "Admin Mode Unlocked!"
                            } else {
                                adminPasscodeError = "Invalid Passcode. Enter master key (Default: 779900)"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("UNLOCK", fontWeight = FontWeight.Black)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showAdminPasscodeDialog = false }
                    ) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            )
        }

        if (showBeneficiariesManager) {
            SavedRecipientsManagerDialog(
                recipients = savedRecipients,
                onDismiss = { showBeneficiariesManager = false },
                onSaveNewRecipient = { name, type, identifier, provider, bankAccountName, isFavorite ->
                    viewModel.saveRecipient(
                        name = name,
                        recipientType = type,
                        identifier = identifier,
                        institutionOrProvider = provider,
                        bankAccountName = bankAccountName,
                        isFavorite = isFavorite
                    )
                },
                onDeleteRecipient = { id -> viewModel.deleteRecipient(id) },
                onToggleFavorite = { r -> viewModel.toggleFavoriteRecipient(r) }
            )
        }

        if (showAboutUsDialog) {
            AlertDialog(
                onDismissRequest = { showAboutUsDialog = false },
                containerColor = DarkSurfaceElevated,
                shape = RoundedCornerShape(20.dp),
                icon = {
                    FlowtestEmblem(
                        size = 52.dp,
                        primaryColor = CyberCyan,
                        glowAlpha = 0.35f,
                        animateRotationOnLoad = true
                    )
                },
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "FlowTest",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, color = TextPrimary),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Operated by INOSOFTTECH LIMITED",
                            style = MaterialTheme.typography.labelSmall.copy(color = ElectricEmerald, fontWeight = FontWeight.Bold),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "CAC RC NO. 9710966",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 9.sp),
                            textAlign = TextAlign.Center
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "FlowTest is an encrypted digital platform offering high-speed VPN tunnel routing, instant utility & electricity payments, airtime/data vending, and automated direct bank settlement.",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp, lineHeight = 16.sp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = DarkCardBorder, thickness = 0.8.dp)
                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "🇳🇬 NIGERIA OFFICE",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontWeight = FontWeight.Black, fontSize = 10.sp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "• West Africa HQ: 7 Woruola Adebola Street, Lekki, Lagos, Nigeria\n• Tel/WhatsApp: +234 8137545370 / 08137545370",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, fontSize = 10.sp, lineHeight = 14.sp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = DarkCardBorder, thickness = 0.8.dp)
                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "OFFICIAL SETTLEMENT BANK",
                            style = MaterialTheme.typography.labelSmall.copy(color = CyberCyan, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "FlowTest Settlement • 6666468328\nAccount Name: INOSOFTTECH LIMITED",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showAboutUsDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Close", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // In-App Update Dialog
        if (showAppUpdateDialog && updateStatus !is UpdateStatus.Idle) {
            AppUpdateDialog(
                status = updateStatus,
                onDismiss = { showAppUpdateDialog = false }
            )
        }
        }
    }
}
}
