package com.example.ui.components

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.FundingTransactionStatus
import com.example.data.repository.AutomatedTransferCheckResult
import com.example.data.ui.viewmodel.VpnViewModel
import com.example.ui.theme.*
import com.example.util.AppNotificationManager
import com.example.util.CompanyConstants
import com.example.util.PhoneNarrationParser
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class FundingDrawerStep {
    TRANSFER_DETAILS,
    VERIFYING,
    ADMIN_REPORT
}

/**
 * High-craft 2-Step Funding Drawer:
 * Step 1: Shows ONLY the corporate account details and what's required to make the transfer.
 * Step 2: Once user taps "I Have Made The Transfer", Step 1 details are hidden and replaced with
 *         real-time dual verification (Moniepoint Webhook + Gmail Bank Alert scan),
 *         celebratory success display with auto/manual redirect to Home, or failure display with
 *         1-tap redirect to Admin Report Form.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FundWalletDialog(
    viewModel: VpnViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val clipboardManager = LocalClipboardManager.current

    val userVirtualAccount by viewModel.userVirtualAccount.collectAsStateWithLifecycle()
    val isFetchingBalance by viewModel.isFetchingPairgateBalance.collectAsStateWithLifecycle()
    val activeConfirmationCode by viewModel.activeConfirmationCode.collectAsStateWithLifecycle()
    val depositSessionExpiresAt by viewModel.depositSessionExpiresAt.collectAsStateWithLifecycle()
    val expectedDepositAmount by viewModel.expectedDepositAmount.collectAsStateWithLifecycle()
    val fundingTransactionStatus by viewModel.fundingTransactionStatus.collectAsStateWithLifecycle()
    val userWalletBalance by viewModel.userWalletBalance.collectAsStateWithLifecycle()

    fun safeDismiss() {
        keyboardController?.hide()
        focusManager.clearFocus()
        onDismiss()
    }

    // Corporate Account Info
    val companyBankName = CompanyConstants.BANK_NAME
    val companyAccountNumber = CompanyConstants.ACCOUNT_NUMBER
    val companyAccountName = CompanyConstants.ACCOUNT_NAME

    val rawUserPhone = userVirtualAccount.phoneNumber.trim()
    val normalizedUserPhone = PhoneNarrationParser.normalizePhoneNumber(rawUserPhone) ?: rawUserPhone

    var depositAmountInput by remember(expectedDepositAmount) {
        mutableStateOf(if (expectedDepositAmount > 0) expectedDepositAmount.toInt().toString() else "1000")
    }

    var currentStep by remember { mutableStateOf(FundingDrawerStep.TRANSFER_DETAILS) }

    // Start / Ensure active deposit session and fresh status on opening
    LaunchedEffect(Unit) {
        if (fundingTransactionStatus is FundingTransactionStatus.Confirmed) {
            viewModel.resetFundingTransactionStatus()
        }
        val initialAmt = depositAmountInput.toDoubleOrNull() ?: 1000.0
        if (depositSessionExpiresAt <= 0L || System.currentTimeMillis() >= depositSessionExpiresAt) {
            viewModel.ensureActiveDepositSession(initialAmount = initialAmt)
        }
    }

    // Countdown Timer logic
    var remainingMillis by remember(depositSessionExpiresAt) {
        mutableStateOf(
            if (depositSessionExpiresAt > 0L) (depositSessionExpiresAt - System.currentTimeMillis()).coerceAtLeast(0L)
            else 0L
        )
    }
    LaunchedEffect(depositSessionExpiresAt) {
        while (depositSessionExpiresAt > 0L) {
            val diff = (depositSessionExpiresAt - System.currentTimeMillis()).coerceAtLeast(0L)
            remainingMillis = diff
            if (diff <= 0L) break
            delay(1000L)
        }
    }
    val isCodeExpired = remainingMillis <= 0L && depositSessionExpiresAt > 0L
    val minutes = (remainingMillis / 1000) / 60
    val seconds = (remainingMillis / 1000) % 60
    val timerText = String.format(Locale.US, "%02d:%02d", minutes, seconds)

    // Verification & Results State
    var isScanningWebhook by remember { mutableStateOf(false) }
    var isAutoPollingActive by remember { mutableStateOf(false) }
    var automatedCheckResult by remember { mutableStateOf<AutomatedTransferCheckResult?>(null) }
    var autoRedirectSeconds by remember { mutableIntStateOf(5) }

    // Session PIN locked to the code displayed when viewing transfer details
    var sessionConfirmationCode by rememberSaveable { mutableStateOf(activeConfirmationCode) }
    LaunchedEffect(activeConfirmationCode, currentStep) {
        if (activeConfirmationCode.isNotBlank()) {
            if (sessionConfirmationCode.isBlank() || currentStep == FundingDrawerStep.TRANSFER_DETAILS) {
                sessionConfirmationCode = activeConfirmationCode
            }
        }
    }

    // AUTOMATE WEBHOOK:
    // Once user clicks "Add Funds", the system starts background webhook polling immediately.
    // The generated code is compared against webhook notifications so that even before the user
    // returns to click "I have transferred", the system can already credit them!
    LaunchedEffect(sessionConfirmationCode, depositAmountInput) {
        val targetAmt = depositAmountInput.toDoubleOrNull() ?: expectedDepositAmount
        val codeToWatch = sessionConfirmationCode.ifBlank { activeConfirmationCode }
        if (codeToWatch.isNotBlank()) {
            viewModel.startAutomatedWebhookDepositMonitor(
                confirmationCode = codeToWatch,
                expectedAmount = targetAmt
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopAutomatedWebhookDepositMonitor()
        }
    }

    // Copy Feedback states
    var copiedAccountFeedback by remember { mutableStateOf(false) }
    var copiedPinFeedback by remember { mutableStateOf(false) }
    var copiedRefFeedback by remember { mutableStateOf(false) }

    // Admin Report Form States
    var alertPhoneInput by remember(userVirtualAccount.phoneNumber) {
        mutableStateOf(userVirtualAccount.phoneNumber.trim())
    }
    var alertAmountInput by remember(depositAmountInput) {
        mutableStateOf(depositAmountInput)
    }
    var alertConfirmationInput by remember(sessionConfirmationCode) {
        mutableStateOf(sessionConfirmationCode)
    }
    var alertSenderNameInput by remember { mutableStateOf("") }
    var isSendingAlert by remember { mutableStateOf(false) }
    var alertSuccessFeedback by remember { mutableStateOf<String?>(null) }
    var networkErrorMessage by remember { mutableStateOf<String?>(null) }

    val isFundingAccountSaved by viewModel.isFundingAccountSaved.collectAsState()
    val savedFundingBankName by viewModel.savedFundingBankName.collectAsState()
    val savedFundingAccountNumber by viewModel.savedFundingAccountNumber.collectAsState()
    var showFundingReminderDrawer by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (viewModel.shouldShowFundingAccountReminder()) {
            showFundingReminderDrawer = true
        }
    }

    // Core Dual-Source Inbound Check Function
    fun triggerAutomatedCheck(onComplete: (Boolean) -> Unit = {}) {
        if (!com.example.data.util.NetworkUtils.isNetworkAvailable(context)) {
            isScanningWebhook = false
            isAutoPollingActive = false
            networkErrorMessage = "No network connection. Please check your mobile data or Wi-Fi to verify your deposit."
            viewModel.setFundingTransactionStatus(FundingTransactionStatus.Failed("No network connection. Please check your mobile data or Wi-Fi."))
            onComplete(false)
            return
        }

        val parsedAmount = depositAmountInput.toDoubleOrNull() ?: expectedDepositAmount
        val phoneToUse = alertPhoneInput.trim().ifBlank { normalizedUserPhone }
        val pinToCheck = sessionConfirmationCode.ifBlank { activeConfirmationCode }
        isScanningWebhook = true
        automatedCheckResult = null
        networkErrorMessage = null
        viewModel.setFundingTransactionStatus(FundingTransactionStatus.Verifying)

        viewModel.checkDualSourceInboundTransfer(
            confirmationCode = pinToCheck,
            expectedAmount = parsedAmount,
            userPhone = phoneToUse
        ) { result ->
            isScanningWebhook = false
            automatedCheckResult = result
            if (result.isPendingNetwork) {
                isAutoPollingActive = false
                networkErrorMessage = result.message.ifBlank { "Network connection is offline or weak. Please check your mobile data or Wi-Fi." }
                onComplete(false)
            } else if (result.isFoundAndCredited) {
                isAutoPollingActive = false
                remainingMillis = 0L
                networkErrorMessage = null
                viewModel.clearDepositSession()
                onComplete(true)
            } else {
                onComplete(false)
            }
        }
    }

    // Auto-polling verification loop with strict timeout & offline protection
    LaunchedEffect(currentStep) {
        if (currentStep == FundingDrawerStep.VERIFYING && fundingTransactionStatus !is FundingTransactionStatus.Confirmed) {
            if (!com.example.data.util.NetworkUtils.isNetworkAvailable(context)) {
                isAutoPollingActive = false
                isScanningWebhook = false
                networkErrorMessage = "No network connection. Please check your mobile data or Wi-Fi to verify your deposit."
                viewModel.setFundingTransactionStatus(FundingTransactionStatus.Failed("No network connection"))
                return@LaunchedEffect
            }

            isAutoPollingActive = true
            networkErrorMessage = null
            var isConfirmed = false
            triggerAutomatedCheck { found ->
                if (found) isConfirmed = true
            }

            var pollAttempts = 0
            val maxPollAttempts = 4 // ~16 seconds max timeout
            while (!isConfirmed && pollAttempts < maxPollAttempts && currentStep == FundingDrawerStep.VERIFYING && viewModel.fundingTransactionStatus.value !is FundingTransactionStatus.Confirmed) {
                delay(4000L)
                pollAttempts++
                if (!com.example.data.util.NetworkUtils.isNetworkAvailable(context)) {
                    isAutoPollingActive = false
                    isScanningWebhook = false
                    networkErrorMessage = "Network connection lost. Please check your mobile data or Wi-Fi."
                    viewModel.setFundingTransactionStatus(FundingTransactionStatus.Failed("Network connection lost"))
                    break
                }
                if (currentStep == FundingDrawerStep.VERIFYING && viewModel.fundingTransactionStatus.value !is FundingTransactionStatus.Confirmed) {
                    triggerAutomatedCheck { found ->
                        if (found) isConfirmed = true
                    }
                }
            }

            // Timed out: stop scanning immediately to prevent hanging/endless waiting
            isAutoPollingActive = false
            isScanningWebhook = false
            if (!isConfirmed && viewModel.fundingTransactionStatus.value !is FundingTransactionStatus.Confirmed) {
                if (networkErrorMessage == null && !com.example.data.util.NetworkUtils.isNetworkAvailable(context)) {
                    networkErrorMessage = "Network connection timed out. Please check your internet connection and try again."
                }
            }
        }
    }

    // Auto-redirect timer when transaction is confirmed
    LaunchedEffect(fundingTransactionStatus) {
        if (fundingTransactionStatus is FundingTransactionStatus.Confirmed) {
            currentStep = FundingDrawerStep.VERIFYING
            autoRedirectSeconds = 5
            while (autoRedirectSeconds > 0) {
                delay(1000L)
                autoRedirectSeconds--
            }
            safeDismiss()
        }
    }

    fun shareDepositDetails() {
        val parsedAmt = depositAmountInput.ifBlank { "1,000" }
        val shareText = """
            FlowTest Wallet Deposit Details:
            ━━━━━━━━━━━━━━━━━━━━━━
            • Amount: ₦$parsedAmt
            • Bank: $companyBankName
            • Account Number: $companyAccountNumber
            • Account Name: $companyAccountName
            • Narration PIN: $activeConfirmationCode
            ━━━━━━━━━━━━━━━━━━━━━━
            Important: Paste "$activeConfirmationCode" into the transfer remarks/narration for instant automatic credit.
        """.trimIndent()
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "FlowTest Deposit Details")
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        context.startActivity(Intent.createChooser(intent, "Share Deposit Details"))
    }

    fun shareReceipt(confirmed: FundingTransactionStatus.Confirmed) {
        val dateStr = SimpleDateFormat("dd-MMM-yyyy HH:mm", Locale.US).format(Date(confirmed.timestamp))
        val text = """
            FlowTest Wallet Deposit Receipt:
            ━━━━━━━━━━━━━━━━━━━━━━
            • Status: SUCCESSFUL & CREDITED
            • Amount: ₦${String.format(Locale.US, "%,.2f", confirmed.amount)}
            • New Balance: ₦${String.format(Locale.US, "%,.2f", confirmed.newBalance)}
            • Reference: ${confirmed.reference}
            • Source: ${confirmed.source}
            • Date: $dateStr
            ━━━━━━━━━━━━━━━━━━━━━━
            Instant settlement verified.
        """.trimIndent()
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Share Deposit Receipt"))
    }

    fun submitAdminReport() {
        val amount = alertAmountInput.toDoubleOrNull()
        if (amount == null || amount <= 0) return
        val phone = alertPhoneInput.trim().ifBlank { normalizedUserPhone }
        val codeUsed = alertConfirmationInput.trim().ifBlank { activeConfirmationCode }
        val sender = alertSenderNameInput.trim().ifBlank { "Customer ($phone)" }

        isSendingAlert = true
        viewModel.reportStalledDepositToAdmin(
            phone = phone,
            amount = amount,
            reference = codeUsed,
            senderName = sender,
            userNote = "Sender: $sender | PIN Used: $codeUsed"
        ) {
            isSendingAlert = false
            alertSuccessFeedback = "Report forwarded to Admin Desk. Reference: $codeUsed"
        }
    }

    ModalBottomSheet(
        onDismissRequest = { if (!isScanningWebhook && !isFetchingBalance) safeDismiss() },
        sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true
        ),
        containerColor = DarkSurfaceElevated,
        scrimColor = Color.Black.copy(alpha = 0.72f),
        dragHandle = {
            Surface(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(44.dp)
                    .height(4.dp),
                shape = CircleShape,
                color = TextMuted.copy(alpha = 0.45f)
            ) {}
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
                .testTag("fund_wallet_drawer")
        ) {
            // STEP PROGRESS HEADER
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Title & 2-Step indicator
                Column {
                    Text(
                        text = when (currentStep) {
                            FundingDrawerStep.TRANSFER_DETAILS -> "Add Money to Wallet"
                            FundingDrawerStep.VERIFYING -> "Verifying Payment"
                            FundingDrawerStep.ADMIN_REPORT -> "Report to Admin Desk"
                        },
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 19.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = when (currentStep) {
                            FundingDrawerStep.TRANSFER_DETAILS -> "Transfer to Corporate Account"
                            FundingDrawerStep.VERIFYING -> "Verifying Payment"
                            FundingDrawerStep.ADMIN_REPORT -> "Admin Reconciliation Report"
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = CyberCyan,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.5.sp
                        )
                    )
                }

                // Close Button
                IconButton(
                    onClick = { safeDismiss() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // =========================================================================
            // ANIMATED STEP CONTENT: Directional Sliding & Fading
            // =========================================================================
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState.ordinal > initialState.ordinal) {
                        // Moving forward: Slide in from right, fade out to left
                        (slideInHorizontally { width -> width } + fadeIn())
                            .togetherWith(slideOutHorizontally { width -> -width } + fadeOut())
                    } else {
                        // Moving backward: Slide in from left, fade out to right
                        (slideInHorizontally { width -> -width } + fadeIn())
                            .togetherWith(slideOutHorizontally { width -> width } + fadeOut())
                    }
                },
                label = "FundingDrawerStepTransition"
            ) { targetStep ->
                when (targetStep) {
                    // =====================================================================
                    // STEP 1: TRANSFER DETAILS ONLY
                    // =====================================================================
                    FundingDrawerStep.TRANSFER_DETAILS -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Instant Webhook Auto-Credit Status Banner
                            if (!isFundingAccountSaved) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = CyberCyan.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.45f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showFundingReminderDrawer = true }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(CyberCyan.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(imageVector = Icons.Default.AccountBalance, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("Automate Instant Funding", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("⚡ AUTO", color = GlowingAmber, fontWeight = FontWeight.Black, fontSize = 9.sp)
                                            }
                                            Text("Save sender bank details so webhooks auto-credit without codes.", color = TextMuted, fontSize = 11.sp, lineHeight = 14.sp)
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Button(
                                            onClick = { showFundingReminderDrawer = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(30.dp)
                                        ) {
                                            Text("Link", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = ElectricEmerald.copy(alpha = 0.1f),
                                    border = BorderStroke(1.dp, ElectricEmerald.copy(alpha = 0.35f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showFundingReminderDrawer = true }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = ElectricEmerald, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Auto-Credit Active: $savedFundingBankName (••••${if (savedFundingAccountNumber.length >= 4) savedFundingAccountNumber.takeLast(4) else savedFundingAccountNumber})",
                                                color = ElectricEmerald,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 11.5.sp,
                                                maxLines = 1
                                            )
                                        }
                                        Text("Edit", color = CyberCyan, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                            }

                            // 1. Sleek Amount Input & Quick Chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "TRANSFER AMOUNT",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = CyberCyan,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp,
                                        letterSpacing = 0.5.sp
                                    )
                                )

                                // Session timer pill
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isCodeExpired) WarningRed.copy(alpha = 0.15f) else GlowingAmber.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, if (isCodeExpired) WarningRed.copy(alpha = 0.5f) else GlowingAmber.copy(alpha = 0.4f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Timer,
                                            contentDescription = null,
                                            tint = if (isCodeExpired) WarningRed else GlowingAmber,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isCodeExpired) "Expired" else timerText,
                                            color = if (isCodeExpired) WarningRed else GlowingAmber,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedTextField(
                                value = depositAmountInput,
                                onValueChange = { newVal ->
                                    val clean = newVal.filter { it.isDigit() }
                                    depositAmountInput = clean
                                    alertAmountInput = clean
                                    clean.toDoubleOrNull()?.let { amt ->
                                        viewModel.setExpectedDepositAmount(amt)
                                    }
                                },
                                prefix = {
                                    Text("₦", color = ElectricEmerald, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                },
                                placeholder = { Text("1000", color = TextMuted) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyberCyan,
                                    unfocusedBorderColor = DarkCardBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedContainerColor = DarkSurface,
                                    unfocusedContainerColor = DarkSurface
                                ),
                                textStyle = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    fontSize = 18.sp
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("fund_amount_input")
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Quick Preset Chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(500, 1000, 2000, 5000, 10000).forEach { preset ->
                                    val isSelected = depositAmountInput == preset.toString()
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) CyberCyan.copy(alpha = 0.2f) else DarkSurface,
                                        border = BorderStroke(1.dp, if (isSelected) CyberCyan else DarkCardBorder),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                depositAmountInput = preset.toString()
                                                alertAmountInput = preset.toString()
                                                viewModel.setExpectedDepositAmount(preset.toDouble())
                                            }
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(vertical = 7.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "₦${if (preset >= 1000) "${preset / 1000}k" else preset}",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) CyberCyan else TextSecondary,
                                                    fontSize = 12.sp
                                                )
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // 2. ACCOUNT DETAILS CARD (Clean, High-Contrast UI/UX Pro Max)
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF09131C)),
                                border = BorderStroke(1.5.dp, CyberCyan.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth().testTag("fund_account_details_card")
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    // Bank name badge + Share button
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = CyberCyan.copy(alpha = 0.15f),
                                            border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.4f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .clip(CircleShape)
                                                        .background(ElectricEmerald)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = companyBankName.uppercase(),
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = CyberCyan,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.5.sp,
                                                        letterSpacing = 0.5.sp
                                                    )
                                                )
                                            }
                                        }

                                        IconButton(
                                            onClick = { shareDepositDetails() },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Share,
                                                contentDescription = "Share",
                                                tint = CyberCyan,
                                                modifier = Modifier.size(17.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Account Number Section
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "ACCOUNT NUMBER",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = TextSecondary,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = companyAccountNumber,
                                                style = MaterialTheme.typography.headlineMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    fontFamily = FontFamily.SansSerif,
                                                    fontSize = 24.sp,
                                                    letterSpacing = 1.5.sp
                                                )
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = companyAccountName,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = ElectricEmerald,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                )
                                            )
                                        }

                                        // Copy Account Button
                                        Button(
                                            onClick = {
                                                clipboardManager.setText(AnnotatedString(companyAccountNumber))
                                                copiedAccountFeedback = true
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (copiedAccountFeedback) CyberCyan else CyberCyan.copy(alpha = 0.15f),
                                                contentColor = if (copiedAccountFeedback) DarkObsidian else CyberCyan
                                            ),
                                            border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f)),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (copiedAccountFeedback) Icons.Default.Check else Icons.Outlined.ContentCopy,
                                                contentDescription = null,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (copiedAccountFeedback) "COPIED" else "COPY",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.5.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))
                                    HorizontalDivider(color = DarkCardBorder.copy(alpha = 0.7f))
                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Narration / Remarks Code Section
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "TRANSFER REMARKS / NARRATION",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = GlowingAmber,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = 0.4.sp
                                                )
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = activeConfirmationCode,
                                                style = MaterialTheme.typography.headlineMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = GlowingAmber,
                                                    fontSize = 22.sp,
                                                    letterSpacing = 1.sp
                                                )
                                            )
                                            Text(
                                                text = "Paste this PIN in your bank app remarks",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = TextSecondary,
                                                    fontSize = 11.5.sp
                                                )
                                            )
                                        }

                                        // Copy Narration Button
                                        Button(
                                            onClick = {
                                                clipboardManager.setText(AnnotatedString(activeConfirmationCode))
                                                copiedPinFeedback = true
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (copiedPinFeedback) CyberCyan else GlowingAmber.copy(alpha = 0.15f),
                                                contentColor = if (copiedPinFeedback) DarkObsidian else GlowingAmber
                                            ),
                                            border = BorderStroke(1.dp, if (copiedPinFeedback) CyberCyan else GlowingAmber.copy(alpha = 0.5f)),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (copiedPinFeedback) Icons.Default.Check else Icons.Outlined.ContentCopy,
                                                contentDescription = null,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (copiedPinFeedback) "COPIED" else "COPY",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.5.sp
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // 3. PROMINENT ACTION BUTTON ("I HAVE MADE THE TRANSFER")
                            Button(
                                onClick = {
                                    // Transition to Step 2: drawer will NOT show these details anymore
                                    currentStep = FundingDrawerStep.VERIFYING
                                    triggerAutomatedCheck()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CyberCyan,
                                    contentColor = DarkObsidian
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("i_have_transferred_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "I HAVE MADE THE TRANSFER",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }

                    // =====================================================================
                    // STEP 2: VERIFYING & SETTLEMENT DISPLAY (Step 1 details hidden)
                    // =====================================================================
                    FundingDrawerStep.VERIFYING -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            when {
                                // -------------------------------------------------------------
                                // STATE A: SETTLEMENT CONFIRMED & CREDITED (SUCCESS DISPLAY)
                                // -------------------------------------------------------------
                                fundingTransactionStatus is FundingTransactionStatus.Confirmed || automatedCheckResult?.isFoundAndCredited == true -> {
                                    val confirmed = fundingTransactionStatus as? FundingTransactionStatus.Confirmed
                                    val creditedAmt = confirmed?.amount ?: automatedCheckResult?.amountCredited ?: depositAmountInput.toDoubleOrNull() ?: 1000.0
                                    val ref = confirmed?.reference ?: automatedCheckResult?.reference ?: "REF-CONFIRMED"
                                    val source = confirmed?.source ?: "BANK TRANSFER"
                                    val newBal = confirmed?.newBalance ?: userWalletBalance

                                    // Celebratory Glowing Badge
                                    Box(
                                        modifier = Modifier
                                            .size(80.dp)
                                            .clip(CircleShape)
                                            .background(ElectricEmerald.copy(alpha = 0.15f))
                                            .border(2.5.dp, ElectricEmerald, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Success",
                                            tint = ElectricEmerald,
                                            modifier = Modifier.size(48.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Surface(
                                        color = ElectricEmerald.copy(alpha = 0.18f),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, ElectricEmerald.copy(alpha = 0.4f))
                                    ) {
                                        Text(
                                            text = "TRANSFER CONFIRMED & CREDITED",
                                            color = ElectricEmerald,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Text(
                                        text = "+₦${String.format(Locale.US, "%,.2f", creditedAmt)}",
                                        style = MaterialTheme.typography.displaySmall.copy(
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 36.sp
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Reconciled Receipt Summary Card
                                    Card(
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                                        border = BorderStroke(1.dp, DarkCardBorder),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            // New Wallet Balance
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Updated Wallet Balance", color = TextSecondary, fontSize = 13.sp)
                                                Text(
                                                    "₦${String.format(Locale.US, "%,.2f", newBal)}",
                                                    color = ElectricEmerald,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp
                                                )
                                            }

                                            HorizontalDivider(color = DarkCardBorder.copy(alpha = 0.6f))

                                            // Settlement Source
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Settlement Route", color = TextSecondary, fontSize = 13.sp)
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(7.dp)
                                                            .clip(CircleShape)
                                                            .background(ElectricEmerald)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = source,
                                                        color = CyberCyan,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.5.sp
                                                    )
                                                }
                                            }

                                            HorizontalDivider(color = DarkCardBorder.copy(alpha = 0.6f))

                                            // Reference ID
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Bank Reference", color = TextSecondary, fontSize = 13.sp)
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.clickable {
                                                        clipboardManager.setText(AnnotatedString(ref))
                                                        copiedRefFeedback = true
                                                    }
                                                ) {
                                                    Text(
                                                        text = if (ref.length > 18) ref.take(18) + "..." else ref,
                                                        color = TextPrimary,
                                                        fontSize = 12.5.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                    Spacer(modifier = Modifier.width(5.dp))
                                                    Icon(
                                                        imageVector = if (copiedRefFeedback) Icons.Default.Check else Icons.Outlined.ContentCopy,
                                                        contentDescription = null,
                                                        tint = if (copiedRefFeedback) ElectricEmerald else CyberCyan,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(20.dp))

                                    // Action Buttons: Return Home (Redirect) & Share Receipt
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        if (confirmed != null) {
                                            OutlinedButton(
                                                onClick = { shareReceipt(confirmed) },
                                                shape = RoundedCornerShape(12.dp),
                                                border = BorderStroke(1.dp, CyberCyan),
                                                modifier = Modifier.weight(1f).height(48.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Share,
                                                    contentDescription = null,
                                                    tint = CyberCyan,
                                                    modifier = Modifier.size(17.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("RECEIPT", color = CyberCyan, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                            }
                                        }

                                        Button(
                                            onClick = { safeDismiss() },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = CyberCyan,
                                                contentColor = DarkObsidian
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1.3f).height(48.dp).testTag("return_home_success_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Home,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "RETURN HOME (${autoRedirectSeconds}s)",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Auto-redirecting back to home in ${autoRedirectSeconds}s...",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = TextSecondary,
                                            fontSize = 12.sp
                                        )
                                    )
                                }

                                // -------------------------------------------------------------
                                // STATE B: NETWORK OFFLINE / TIMEOUT / BAD CONNECTION
                                // -------------------------------------------------------------
                                networkErrorMessage != null -> {
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(CircleShape)
                                            .background(GlowingAmber.copy(alpha = 0.15f))
                                            .border(2.dp, GlowingAmber.copy(alpha = 0.7f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.WifiOff,
                                            contentDescription = "No Network",
                                            tint = GlowingAmber,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Text(
                                        text = "Network Unavailable",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = GlowingAmber,
                                            fontSize = 17.sp
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = networkErrorMessage ?: "Cannot scan for transfer verification without an active data connection. Please check your network and tap Retry.",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = TextSecondary,
                                            fontSize = 12.5.sp,
                                            lineHeight = 18.sp
                                        ),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )

                                    Spacer(modifier = Modifier.height(20.dp))

                                    Button(
                                        onClick = {
                                            networkErrorMessage = null
                                            isAutoPollingActive = true
                                            triggerAutomatedCheck()
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = CyberCyan,
                                            contentColor = DarkObsidian
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("retry_network_check_button")
                                    ) {
                                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("RETRY SCAN", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Button(
                                        onClick = { currentStep = FundingDrawerStep.ADMIN_REPORT },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = GlowingAmber,
                                            contentColor = DarkObsidian
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("report_offline_admin_button")
                                    ) {
                                        Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(17.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("REPORT TRANSFER TO ADMIN", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    TextButton(
                                        onClick = {
                                            networkErrorMessage = null
                                            isScanningWebhook = false
                                            isAutoPollingActive = false
                                            currentStep = FundingDrawerStep.TRANSFER_DETAILS
                                        }
                                    ) {
                                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextMuted)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Back to Account Details", color = TextMuted, fontSize = 12.5.sp)
                                    }
                                }

                                // -------------------------------------------------------------
                                // STATE C: PROCESSING / VERIFYING IN PROGRESS
                                // -------------------------------------------------------------
                                isScanningWebhook || isAutoPollingActive || fundingTransactionStatus is FundingTransactionStatus.Verifying -> {
                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Verification Badge
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(CircleShape)
                                            .background(CyberCyan.copy(alpha = 0.12f))
                                            .border(1.5.dp, CyberCyan.copy(alpha = 0.4f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Sync,
                                            contentDescription = "Verifying",
                                            tint = CyberCyan,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(20.dp))

                                    Text(
                                        text = "Verifying Transfer...",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            fontSize = 18.sp
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "Confirming payment of ₦${depositAmountInput.ifBlank { "1,000" }} (PIN: $sessionConfirmationCode)",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = TextSecondary,
                                            fontSize = 13.sp,
                                            lineHeight = 18.sp
                                        ),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )

                                    Spacer(modifier = Modifier.height(20.dp))

                                    FlowLoadingLine(
                                        modifier = Modifier.fillMaxWidth(0.7f),
                                        color = CyberCyan,
                                        trackColor = DarkSurfaceElevated,
                                        height = 4.dp
                                    )

                                    Spacer(modifier = Modifier.height(24.dp))

                                    TextButton(
                                        onClick = {
                                            isScanningWebhook = false
                                            isAutoPollingActive = false
                                            currentStep = FundingDrawerStep.TRANSFER_DETAILS
                                        }
                                    ) {
                                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextMuted)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Cancel & Back to Account Details", color = TextMuted, fontSize = 12.sp)
                                    }
                                }

                                // -------------------------------------------------------------
                                // STATE C: DEFINITIVE FAILURE / NOT DETECTED
                                // -------------------------------------------------------------
                                else -> {
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(CircleShape)
                                            .background(GlowingAmber.copy(alpha = 0.15f))
                                            .border(2.dp, GlowingAmber.copy(alpha = 0.7f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = "Not Detected",
                                            tint = GlowingAmber,
                                            modifier = Modifier.size(38.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Text(
                                        text = "Payment Not Detected Yet",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = GlowingAmber,
                                            fontSize = 17.sp
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "We have not detected your transfer of ₦${depositAmountInput.ifBlank { "1,000" }} with PIN $sessionConfirmationCode yet.\n\nIf you just transferred, your bank may need a minute to complete settlement, or you can submit a quick report directly to Admin.",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = TextSecondary,
                                            fontSize = 12.5.sp,
                                            lineHeight = 18.sp
                                        ),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 12.dp)
                                    )

                                    Spacer(modifier = Modifier.height(20.dp))

                                    // Action 1: Check Again
                                    Button(
                                        onClick = {
                                            isAutoPollingActive = true
                                            triggerAutomatedCheck()
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = CyberCyan,
                                            contentColor = DarkObsidian
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("check_again_button")
                                    ) {
                                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("CHECK AGAIN", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Action 2: Report to Admin Form (Instant transition)
                                    Button(
                                        onClick = { currentStep = FundingDrawerStep.ADMIN_REPORT },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = GlowingAmber,
                                            contentColor = DarkObsidian
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("report_to_admin_button")
                                    ) {
                                        Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(17.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("REPORT TRANSFER TO ADMIN", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    TextButton(
                                        onClick = { currentStep = FundingDrawerStep.TRANSFER_DETAILS }
                                    ) {
                                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextMuted)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Back to Account Details", color = TextMuted, fontSize = 12.5.sp)
                                    }
                                }
                            }
                        }
                    }

                    // =====================================================================
                    // STEP 3: ADMIN REPORT FORM (Redirected when transfer delayed)
                    // =====================================================================
                    FundingDrawerStep.ADMIN_REPORT -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Back button + Header
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { currentStep = FundingDrawerStep.VERIFYING }
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = CyberCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Back to Verification",
                                    color = CyberCyan,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "If your bank has already debited your account, submit your transfer details below. The Admin will reconcile against the bank statement and credit your wallet immediately.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondary,
                                    fontSize = 12.5.sp,
                                    lineHeight = 17.sp
                                )
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Field 1: Phone Number
                            OutlinedTextField(
                                value = alertPhoneInput,
                                onValueChange = { alertPhoneInput = it },
                                label = { Text("Sender Phone Number", fontSize = 12.sp) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyberCyan,
                                    unfocusedBorderColor = DarkCardBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("admin_report_phone_input")
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Field 2: Amount Transferred
                            OutlinedTextField(
                                value = alertAmountInput,
                                onValueChange = { alertAmountInput = it.filter { ch -> ch.isDigit() } },
                                label = { Text("Amount Transferred (₦)", fontSize = 12.sp) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyberCyan,
                                    unfocusedBorderColor = DarkCardBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("admin_report_amount_input")
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Field 3: Confirmation PIN Used
                            OutlinedTextField(
                                value = alertConfirmationInput,
                                onValueChange = { alertConfirmationInput = it.trim() },
                                label = { Text("Confirmation PIN / Reference Used", fontSize = 12.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyberCyan,
                                    unfocusedBorderColor = DarkCardBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("admin_report_pin_input")
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Field 4: Sender Bank / Name (Optional)
                            OutlinedTextField(
                                value = alertSenderNameInput,
                                onValueChange = { alertSenderNameInput = it },
                                label = { Text("Your Bank / Sender Name (e.g. GTBank / John)", fontSize = 12.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyberCyan,
                                    unfocusedBorderColor = DarkCardBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("admin_report_sender_input")
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Submit Button
                            Button(
                                onClick = { submitAdminReport() },
                                enabled = !isSendingAlert,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GlowingAmber,
                                    contentColor = DarkObsidian
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("submit_admin_report_button")
                            ) {
                                if (isSendingAlert) {
                                    FlowButtonLoadingLine(color = DarkObsidian)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("SUBMITTING TO ADMIN...", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                } else {
                                    Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(17.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("SUBMIT REPORT TO ADMIN DESK", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }

                            // Success confirmation and redirect to home
                            alertSuccessFeedback?.let { feedback ->
                                Spacer(modifier = Modifier.height(12.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = ElectricEmerald.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, ElectricEmerald.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = ElectricEmerald,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Report Lodged with Admin",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = ElectricEmerald,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.5.sp
                                                )
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = feedback,
                                            style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, fontSize = 12.sp)
                                        )

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Button(
                                            onClick = { safeDismiss() },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = CyberCyan,
                                                contentColor = DarkObsidian
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(imageVector = Icons.Default.Home, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("RETURN TO HOME", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
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

    if (showFundingReminderDrawer) {
        FundingAccountReminderDrawer(
            viewModel = viewModel,
            onDismiss = { showFundingReminderDrawer = false }
        )
    }
}
