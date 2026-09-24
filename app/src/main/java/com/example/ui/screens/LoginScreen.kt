package com.example.ui.screens

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
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
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ui.viewmodel.VpnViewModel
import com.example.ui.components.AutoLogoutSecuritySheet
import com.example.ui.components.FlowButtonLoadingLine
import com.example.ui.components.FlowtestLogo
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class LoginViewMode {
    EMAIL_PASSWORD,
    PASSCODE,
    FINGERPRINT
}

@Composable
fun LoginScreen(
    viewModel: VpnViewModel,
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val userVirtualAccount by viewModel.userVirtualAccount.collectAsStateWithLifecycle()
    val isFingerprintEnabled by viewModel.isFingerprintEnabled.collectAsStateWithLifecycle()
    val showAutoLogoutDialog by viewModel.showAutoLogoutDialog.collectAsStateWithLifecycle()
    val autoLogoutReason by viewModel.autoLogoutReason.collectAsStateWithLifecycle()

    var viewMode by remember {
        mutableStateOf(LoginViewMode.EMAIL_PASSWORD)
    }

    var emailInput by remember(userVirtualAccount) {
        mutableStateOf(if (userVirtualAccount.email.isNotBlank() && !userVirtualAccount.email.contains("flowtest2026.com", ignoreCase = true)) userVirtualAccount.email else "")
    }
    var passwordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var forgotPasswordEmail by remember { mutableStateOf("") }
    var forgotPasswordSuccess by remember { mutableStateOf(false) }
    var isSendingResetEmail by remember { mutableStateOf(false) }

    var emailOrPhoneInput by remember(userVirtualAccount) {
        mutableStateOf(
            if (userVirtualAccount.phoneNumber.isNotBlank()) {
                userVirtualAccount.phoneNumber
            } else if (userVirtualAccount.email.isNotBlank()) {
                userVirtualAccount.email
            } else {
                ""
            }
        )
    }

    val userName = remember(userVirtualAccount) {
        val storedName = if (userVirtualAccount.fullName.isNotBlank()) {
            userVirtualAccount.fullName
        } else {
            context.getSharedPreferences("auth_prefs", android.content.Context.MODE_PRIVATE)
                .getString("saved_user_name", "")?.trim() ?: ""
        }
        if (storedName.isNotBlank() && storedName != "User" && storedName != "Valued User") {
            storedName.split(" ").firstOrNull { it.isNotBlank() }?.uppercase() ?: "USER"
        } else if (userVirtualAccount.accountName.isNotBlank() && userVirtualAccount.accountName != "Unassigned") {
            userVirtualAccount.accountName.replace("COT Digital - ", "").split(" ").firstOrNull { it.isNotBlank() }?.uppercase() ?: "USER"
        } else {
            "VALUED USER"
        }
    }

    var passcode by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var isBiometricScanning by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var showLockAccountDialog by remember { mutableStateOf(false) }
    var showLegalDialogTitle by remember { mutableStateOf<String?>(null) }

    // Security & Rate-Limiting Lockout States
    var failedPinAttempts by remember { mutableIntStateOf(0) }
    val isVmAccountLocked by viewModel.isAccountLocked.collectAsStateWithLifecycle()
    var isAccountLocked by remember { mutableStateOf(false) }
    val effectiveAccountLocked = isAccountLocked || isVmAccountLocked
    var showReactivateDialog by remember { mutableStateOf(false) }
    var reactivateCodeOrPin by remember { mutableStateOf("") }
    var isReactivating by remember { mutableStateOf(false) }
    var reactivateError by remember { mutableStateOf<String?>(null) }

    // SMS OTP & Reset PIN Modals
    var showSmsOtpModal by remember { mutableStateOf(false) }
    var showForgotPinResetModal by remember { mutableStateOf(false) }
    var otpPhoneInput by remember(userVirtualAccount) { mutableStateOf(userVirtualAccount.phoneNumber) }
    var smsCodeInput by remember { mutableStateOf("") }
    var resetNewPinInput by remember { mutableStateOf("") }
    var isSendingSms by remember { mutableStateOf(false) }
    var smsCooldownRemaining by remember { mutableLongStateOf(0L) }

    // Dual-Auth (Phone Number vs Email) state for Sign-In card
    var authMethodTab by remember { mutableIntStateOf(0) } // 0 = Phone Number (SMS), 1 = Email & Password
    var loginPhoneInput by remember(userVirtualAccount) {
        mutableStateOf(
            if (userVirtualAccount.phoneNumber.isNotBlank()) userVirtualAccount.phoneNumber else ""
        )
    }
    var phoneVerificationCode by remember { mutableStateOf("") }
    var phoneVerificationId by remember { mutableStateOf<String?>(null) }
    var isPhoneCodeSent by remember { mutableStateOf(false) }
    var isSendingPhoneVerification by remember { mutableStateOf(false) }
    var isVerifyingPhoneCode by remember { mutableStateOf(false) }
    var phoneResendCooldown by remember { mutableIntStateOf(0) }

    val scope = rememberCoroutineScope()

    // Live ticker for phone verification resend cooldown
    LaunchedEffect(phoneResendCooldown) {
        if (phoneResendCooldown > 0) {
            delay(1000L)
            phoneResendCooldown -= 1
        }
    }

    // SMS Cooldown live ticker
    LaunchedEffect(showSmsOtpModal, showForgotPinResetModal) {
        while (true) {
            smsCooldownRemaining = viewModel.getSmsCooldownRemainingSeconds()
            delay(1000L)
        }
    }

    // Launch Android native hardware biometric prompt
    fun launchHardwareBiometricPrompt(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            if (!viewModel.canUseBiometricQuickUnlock()) {
                onError("Biometric quick unlock is not active. Please sign in with your email and password.")
                return
            }

            val activity = context as? FragmentActivity
            if (activity == null) {
                viewModel.loginWithFingerprint(onSuccess, onError)
                return
            }

            val biometricManager = BiometricManager.from(context)
            val authenticators = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            } else {
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK
            }

            val canAuthStatus = try {
                biometricManager.canAuthenticate(authenticators)
            } catch (e: Throwable) {
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE
            }

            if (canAuthStatus == BiometricManager.BIOMETRIC_SUCCESS) {
                val executor = ContextCompat.getMainExecutor(context)
                val callback = object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        viewModel.loginWithFingerprint(onSuccess, onError)
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        onError(errString.toString())
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        onError("Thumbprint not recognized. Please try again.")
                    }
                }

                val promptBuilder = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Biometric Unlock")
                    .setSubtitle("Confirm your thumbprint to access your account")

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    promptBuilder.setAllowedAuthenticators(authenticators)
                } else {
                    promptBuilder.setNegativeButtonText("Use Password")
                }

                val promptInfo = promptBuilder.build()

                try {
                    val biometricPrompt = BiometricPrompt(activity, executor, callback)
                    biometricPrompt.authenticate(promptInfo)
                } catch (e: Throwable) {
                    onError("Biometric initialization notice: ${e.message}")
                }
            } else {
                onError("Biometric hardware unavailable or not enrolled.")
            }
        } catch (e: Throwable) {
            onError("Biometrics unavailable: ${e.message}")
        }
    }

    // Trigger fingerprint auth
    fun triggerFingerprintAuth() {
        if (effectiveAccountLocked) {
            errorMessage = "Account is locked for security. Tap 'Reactivate' to restore access."
            return
        }
        if (!viewModel.canUseBiometricQuickUnlock()) {
            errorMessage = "Please sign in with your email and password first."
            viewMode = LoginViewMode.EMAIL_PASSWORD
            return
        }
        isBiometricScanning = true
        errorMessage = null
        launchHardwareBiometricPrompt(
            onSuccess = {
                isBiometricScanning = false
                successMessage = "Thumbprint Authenticated!"
                scope.launch {
                    delay(250)
                    onLoginSuccess()
                }
            },
            onError = { err ->
                isBiometricScanning = false
                errorMessage = err
            }
        )
    }

    // Auto-launch thumbprint prompt ONLY if quick unlock is valid
    LaunchedEffect(Unit) {
        if (viewMode == LoginViewMode.FINGERPRINT && !effectiveAccountLocked && viewModel.canUseBiometricQuickUnlock()) {
            delay(300)
            triggerFingerprintAuth()
        }
    }

    // Biometric animation scale
    val infiniteTransition = rememberInfiniteTransition(label = "BiometricScan")
    val scanScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ScanScale"
    )

    // Submit 6-digit passcode with email or phone & rate limiting checks
    fun submitPasscode() {
        if (effectiveAccountLocked || failedPinAttempts >= 5) {
            isAccountLocked = true
            errorMessage = "Account locked for security. Tap 'Reactivate' to restore access or reset PIN via SMS."
            return
        }

        if (passcode.length < 4) {
            errorMessage = "Please enter your 4 to 6 digit passcode PIN."
            return
        }
        if (emailOrPhoneInput.isBlank()) {
            errorMessage = "Please enter your registered Phone Number or Email address."
            return
        }

        isLoading = true
        errorMessage = null
        viewModel.loginWithPin(
            emailOrPhone = emailOrPhoneInput,
            pin = passcode,
            onSuccess = {
                isLoading = false
                failedPinAttempts = 0
                isAccountLocked = false
                successMessage = "Welcome back!"
                scope.launch {
                    delay(250)
                    onLoginSuccess()
                }
            },
            onError = { err ->
                isLoading = false
                failedPinAttempts += 1
                passcode = ""
                if (failedPinAttempts >= 5) {
                    isAccountLocked = true
                    errorMessage = "Account temporarily locked! 5/5 failed PIN attempts. Click 'Reset PIN via SMS' to verify your phone."
                } else {
                    val remaining = 5 - failedPinAttempts
                    errorMessage = "Invalid PIN. $failedPinAttempts/5 attempts used ($remaining remaining before lockout)."
                }
            }
        )
    }

    // Send SMS Code via HttpSMS
    fun sendSmsVerificationOtp(targetPhone: String, onSuccessMsg: (String) -> Unit) {
        val remaining = viewModel.getSmsCooldownRemainingSeconds()
        if (remaining > 0) {
            val mins = remaining / 60
            val secs = remaining % 60
            val timeFormatted = if (mins > 0) "${mins}m ${secs}s" else "${secs}s"
            errorMessage = "Please wait $timeFormatted before requesting another SMS (5-minute cooldown limit)."
            return
        }

        isSendingSms = true
        errorMessage = null
        viewModel.sendOtpPinViaHttpSms(
            phone = targetPhone,
            onSuccess = { pin, msg ->
                isSendingSms = false
                smsCooldownRemaining = viewModel.getSmsCooldownRemainingSeconds()
                onSuccessMsg(msg)
            },
            onError = { err ->
                isSendingSms = false
                errorMessage = err
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkObsidian)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(28.dp))

            // BRAND LOGO HEADER
            FlowtestLogo(
                emblemSize = 72.dp,
                fontSize = 26.sp,
                showWordmark = true,
                textColor = TextPrimary,
                primaryColor = CyberCyan
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Secure Thumbprint & Passcode Authentication",
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 12.sp),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // UNIFIED TAB SWITCHER (EMAIL & PASS / PIN / REGISTER)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurface)
                    .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (viewMode == LoginViewMode.EMAIL_PASSWORD) CyberCyan else Color.Transparent)
                        .clickable {
                            viewMode = LoginViewMode.EMAIL_PASSWORD
                            errorMessage = null
                        }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "EMAIL & PASS",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = if (viewMode == LoginViewMode.EMAIL_PASSWORD) DarkObsidian else TextSecondary,
                            letterSpacing = 0.6.sp
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(0.9f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (viewMode == LoginViewMode.PASSCODE) CyberCyan else Color.Transparent)
                        .clickable {
                            viewMode = LoginViewMode.PASSCODE
                            errorMessage = null
                        }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "QUICK PIN",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = if (viewMode == LoginViewMode.PASSCODE) DarkObsidian else TextSecondary,
                            letterSpacing = 0.6.sp
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(0.9f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onNavigateToRegister() }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "REGISTER",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 0.6.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Error / Success Messages
            errorMessage?.let { err ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF3E1418)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF5252)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = err,
                            color = Color(0xFFFF8A80),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                        )
                        if (err.contains("REGISTER", ignoreCase = true) || err.contains("create an account", ignoreCase = true)) {
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(
                                onClick = { onNavigateToRegister() },
                                colors = ButtonDefaults.textButtonColors(contentColor = CyberCyan),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Go to Registration", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            successMessage?.let { msg ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = ElectricEmerald.copy(alpha = 0.15f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ElectricEmerald),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Text(
                        text = msg,
                        color = ElectricEmerald,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Crossfade(targetState = viewMode, label = "LoginViewModeTransition") { mode ->
                when (mode) {
                    LoginViewMode.EMAIL_PASSWORD -> {
                        // FIREBASE AUTHENTICATION (EMAIL & PASSWORD)
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(22.dp))
                                    .border(1.dp, GlassBorder, RoundedCornerShape(22.dp)),
                                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(22.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "AUTHENTICATION",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Black,
                                                    color = CyberCyan,
                                                    letterSpacing = 1.2.sp
                                                )
                                            )
                                            Text(
                                                text = "Sign In With Firebase",
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary
                                                )
                                            )
                                        }

                                        if (viewModel.canUseBiometricQuickUnlock()) {
                                            IconButton(
                                                onClick = {
                                                    viewMode = LoginViewMode.FINGERPRINT
                                                    triggerFingerprintAuth()
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Fingerprint,
                                                    contentDescription = "Quick Biometric",
                                                    tint = CyberCyan,
                                                    modifier = Modifier.size(26.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Dual-Auth Method Selector (Phone vs Email)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(DarkSurface)
                                            .padding(4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        // Tab 0: Phone Number
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(9.dp))
                                                .background(if (authMethodTab == 0) CyberCyan.copy(alpha = 0.15f) else Color.Transparent)
                                                .border(
                                                    width = if (authMethodTab == 0) 1.dp else 0.dp,
                                                    color = if (authMethodTab == 0) CyberCyan else Color.Transparent,
                                                    shape = RoundedCornerShape(9.dp)
                                                )
                                                .clickable {
                                                    authMethodTab = 0
                                                    errorMessage = null
                                                }
                                                .padding(vertical = 10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Phone,
                                                    contentDescription = null,
                                                    tint = if (authMethodTab == 0) CyberCyan else TextSecondary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = "GOOGLE PHONE AUTH",
                                                    style = MaterialTheme.typography.labelMedium.copy(
                                                        fontWeight = if (authMethodTab == 0) FontWeight.Bold else FontWeight.Medium,
                                                        color = if (authMethodTab == 0) CyberCyan else TextSecondary,
                                                        letterSpacing = 0.5.sp
                                                    )
                                                )
                                            }
                                        }

                                        // Tab 1: Email Address
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(9.dp))
                                                .background(if (authMethodTab == 1) CyberCyan.copy(alpha = 0.15f) else Color.Transparent)
                                                .border(
                                                    width = if (authMethodTab == 1) 1.dp else 0.dp,
                                                    color = if (authMethodTab == 1) CyberCyan else Color.Transparent,
                                                    shape = RoundedCornerShape(9.dp)
                                                )
                                                .clickable {
                                                    authMethodTab = 1
                                                    errorMessage = null
                                                }
                                                .padding(vertical = 10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Email,
                                                    contentDescription = null,
                                                    tint = if (authMethodTab == 1) CyberCyan else TextSecondary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = "EMAIL ADDRESS",
                                                    style = MaterialTheme.typography.labelMedium.copy(
                                                        fontWeight = if (authMethodTab == 1) FontWeight.Bold else FontWeight.Medium,
                                                        color = if (authMethodTab == 1) CyberCyan else TextSecondary,
                                                        letterSpacing = 0.5.sp
                                                    )
                                                )
                                            }
                                        }
                                    }

                                    if (authMethodTab == 0) {
                                        // -------------------------------------------------------------
                                        // PHONE NUMBER (GOOGLE AUTH SMS)
                                        // -------------------------------------------------------------
                                        if (!isPhoneCodeSent) {
                                            // Nigerian Phone Number input
                                            OutlinedTextField(
                                                value = loginPhoneInput,
                                                onValueChange = {
                                                    loginPhoneInput = it
                                                    errorMessage = null
                                                },
                                                label = { Text("Nigerian Phone Number") },
                                                placeholder = { Text("Enter phone") },
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(
                                                    keyboardType = KeyboardType.Phone,
                                                    imeAction = androidx.compose.ui.text.input.ImeAction.Done
                                                ),
                                                leadingIcon = {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.padding(start = 12.dp, end = 6.dp)
                                                    ) {
                                                        Text("🇳🇬", style = MaterialTheme.typography.bodyLarge)
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = "+234",
                                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                                color = CyberCyan,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Box(
                                                            modifier = Modifier
                                                                .width(1.dp)
                                                                .height(20.dp)
                                                                .background(DarkCardBorder)
                                                        )
                                                    }
                                                },
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = CyberCyan,
                                                    unfocusedBorderColor = DarkCardBorder,
                                                    focusedTextColor = TextPrimary,
                                                    unfocusedTextColor = TextPrimary,
                                                    focusedContainerColor = DarkSurface,
                                                    unfocusedContainerColor = DarkSurface
                                                ),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            )

                                            Text(
                                                text = "Google Phone Authentication sends a 6-digit SMS code with background Play Services verification.",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = TextSecondary,
                                                    fontSize = 11.5.sp
                                                )
                                            )

                                            Button(
                                                onClick = {
                                                    val clean = loginPhoneInput.trim()
                                                    if (clean.length < 10) {
                                                        errorMessage = "Please enter a valid Nigerian phone number."
                                                        return@Button
                                                    }
                                                    isSendingPhoneVerification = true
                                                    errorMessage = null
                                                    val act = context as? android.app.Activity
                                                    viewModel.sendGooglePhoneVerificationCode(
                                                        activity = act,
                                                        phoneNumber = clean,
                                                        onCodeSent = { vId, _ ->
                                                            isSendingPhoneVerification = false
                                                            phoneVerificationId = vId
                                                            isPhoneCodeSent = true
                                                            phoneResendCooldown = 60
                                                            successMessage = "Verification code sent."
                                                        },
                                                        onAutoVerified = {
                                                            isSendingPhoneVerification = false
                                                            successMessage = "Verified."
                                                            scope.launch {
                                                                delay(300)
                                                                onLoginSuccess()
                                                            }
                                                        },
                                                        onError = { err ->
                                                            isSendingPhoneVerification = false
                                                            errorMessage = err
                                                        }
                                                    )
                                                },
                                                enabled = !isSendingPhoneVerification,
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = CyberCyan,
                                                    contentColor = DarkObsidian
                                                ),
                                                shape = RoundedCornerShape(14.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(50.dp)
                                            ) {
                                                if (isSendingPhoneVerification) {
                                                    FlowButtonLoadingLine(
                                                        color = DarkObsidian,
                                                        width = 44.dp
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "SENDING...",
                                                        style = MaterialTheme.typography.labelLarge.copy(
                                                            fontWeight = FontWeight.Black,
                                                            color = DarkObsidian
                                                        )
                                                    )
                                                } else {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Send,
                                                            contentDescription = null,
                                                            tint = DarkObsidian,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                        Text(
                                                            text = "SEND CODE",
                                                            style = MaterialTheme.typography.labelLarge.copy(
                                                                fontWeight = FontWeight.Black,
                                                                letterSpacing = 1.sp,
                                                                color = DarkObsidian
                                                            )
                                                        )
                                                    }
                                                }
                                            }
                                        } else {
                                            // Code sent state
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(CyberCyan.copy(alpha = 0.08f))
                                                    .border(1.dp, CyberCyan.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(
                                                        text = "CODE SENT TO NIGERIA PHONE",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            color = CyberCyan,
                                                            fontWeight = FontWeight.Bold,
                                                            letterSpacing = 0.8.sp
                                                        )
                                                    )
                                                    Text(
                                                        text = loginPhoneInput,
                                                        style = MaterialTheme.typography.bodyMedium.copy(
                                                            color = TextPrimary,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    )
                                                }
                                                TextButton(
                                                    onClick = {
                                                        isPhoneCodeSent = false
                                                        phoneVerificationCode = ""
                                                        errorMessage = null
                                                    }
                                                ) {
                                                    Text(
                                                        text = "Change",
                                                        color = CyberCyan,
                                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                                    )
                                                }
                                            }

                                            OutlinedTextField(
                                                value = phoneVerificationCode,
                                                onValueChange = {
                                                    if (it.length <= 6) {
                                                        phoneVerificationCode = it.filter { ch -> ch.isDigit() }
                                                        errorMessage = null
                                                    }
                                                },
                                                label = { Text("6-Digit Verification Code") },
                                                placeholder = { Text("123456") },
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(
                                                    keyboardType = KeyboardType.NumberPassword,
                                                    imeAction = androidx.compose.ui.text.input.ImeAction.Done
                                                ),
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = Icons.Default.Lock,
                                                        contentDescription = null,
                                                        tint = CyberCyan
                                                    )
                                                },
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = CyberCyan,
                                                    unfocusedBorderColor = DarkCardBorder,
                                                    focusedTextColor = TextPrimary,
                                                    unfocusedTextColor = TextPrimary,
                                                    focusedContainerColor = DarkSurface,
                                                    unfocusedContainerColor = DarkSurface
                                                ),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            )

                                            Button(
                                                onClick = {
                                                    if (phoneVerificationCode.trim().length < 4) {
                                                        errorMessage = "Please enter the verification code sent to your phone."
                                                        return@Button
                                                    }
                                                    isVerifyingPhoneCode = true
                                                    errorMessage = null
                                                    viewModel.loginWithGooglePhoneAuth(
                                                        verificationId = phoneVerificationId,
                                                        smsCode = phoneVerificationCode,
                                                        phoneNumber = loginPhoneInput,
                                                        onSuccess = {
                                                            isVerifyingPhoneCode = false
                                                            successMessage = "Verified."
                                                            scope.launch {
                                                                delay(300)
                                                                onLoginSuccess()
                                                            }
                                                        },
                                                        onError = { err ->
                                                            isVerifyingPhoneCode = false
                                                            errorMessage = err
                                                        }
                                                    )
                                                },
                                                enabled = !isVerifyingPhoneCode,
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = CyberCyan,
                                                    contentColor = DarkObsidian
                                                ),
                                                shape = RoundedCornerShape(14.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(50.dp)
                                            ) {
                                                if (isVerifyingPhoneCode) {
                                                    FlowButtonLoadingLine(
                                                        color = DarkObsidian,
                                                        width = 44.dp
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "VERIFYING...",
                                                        style = MaterialTheme.typography.labelLarge.copy(
                                                            fontWeight = FontWeight.Black,
                                                            color = DarkObsidian
                                                        )
                                                    )
                                                } else {
                                                    Text(
                                                        text = "VERIFY",
                                                        style = MaterialTheme.typography.labelLarge.copy(
                                                            fontWeight = FontWeight.Black,
                                                            letterSpacing = 1.sp,
                                                            color = DarkObsidian
                                                        )
                                                    )
                                                }
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (phoneResendCooldown > 0) {
                                                    Text(
                                                        text = "Resend code in ${phoneResendCooldown}s",
                                                        color = TextSecondary,
                                                        style = MaterialTheme.typography.labelMedium
                                                    )
                                                } else {
                                                    TextButton(
                                                        onClick = {
                                                            val act = context as? android.app.Activity
                                                            phoneResendCooldown = 60
                                                            viewModel.sendGooglePhoneVerificationCode(
                                                                activity = act,
                                                                phoneNumber = loginPhoneInput,
                                                                onCodeSent = { vId, _ ->
                                                                    phoneVerificationId = vId
                                                                    successMessage = "Verification code resent!"
                                                                },
                                                                onAutoVerified = {
                                                                    onLoginSuccess()
                                                                },
                                                                onError = { err ->
                                                                    phoneResendCooldown = 0
                                                                    errorMessage = err
                                                                }
                                                            )
                                                        }
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Refresh,
                                                                contentDescription = null,
                                                                tint = CyberCyan,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                            Text(
                                                                text = "Didn't receive code? Resend SMS",
                                                                color = CyberCyan,
                                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        // -------------------------------------------------------------
                                        // EMAIL ADDRESS & PASSWORD
                                        // -------------------------------------------------------------
                                        // Email field
                                        OutlinedTextField(
                                            value = emailInput,
                                            onValueChange = {
                                                emailInput = it
                                                errorMessage = null
                                            },
                                            label = { Text("Email Address") },
                                            placeholder = { Text("Enter email") },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(
                                                keyboardType = KeyboardType.Email,
                                                imeAction = androidx.compose.ui.text.input.ImeAction.Next
                                            ),
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.Email,
                                                    contentDescription = null,
                                                    tint = CyberCyan
                                                )
                                            },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = CyberCyan,
                                                unfocusedBorderColor = DarkCardBorder,
                                                focusedTextColor = TextPrimary,
                                                unfocusedTextColor = TextPrimary,
                                                focusedContainerColor = DarkSurface,
                                                unfocusedContainerColor = DarkSurface
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        // Password field
                                        OutlinedTextField(
                                            value = passwordInput,
                                            onValueChange = {
                                                passwordInput = it
                                                errorMessage = null
                                            },
                                            label = { Text("Password") },
                                            placeholder = { Text("••••••••") },
                                            singleLine = true,
                                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                            keyboardOptions = KeyboardOptions(
                                                keyboardType = KeyboardType.Password,
                                                imeAction = androidx.compose.ui.text.input.ImeAction.Done
                                            ),
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.Lock,
                                                    contentDescription = null,
                                                    tint = CyberCyan
                                                )
                                            },
                                            trailingIcon = {
                                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                                    Icon(
                                                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                        contentDescription = if (passwordVisible) "Hide" else "Show",
                                                        tint = TextSecondary
                                                    )
                                                }
                                            },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = CyberCyan,
                                                unfocusedBorderColor = DarkCardBorder,
                                                focusedTextColor = TextPrimary,
                                                unfocusedTextColor = TextPrimary,
                                                focusedContainerColor = DarkSurface,
                                                unfocusedContainerColor = DarkSurface
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            TextButton(
                                                onClick = {
                                                    forgotPasswordEmail = emailInput
                                                    forgotPasswordSuccess = false
                                                    showForgotPasswordDialog = true
                                                }
                                            ) {
                                                Text(
                                                    text = "Forgot Password?",
                                                    color = CyberCyan,
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                                )
                                            }
                                        }

                                        // SIGN IN BUTTON
                                        Button(
                                            onClick = {
                                                if (emailInput.trim().isBlank() || !emailInput.contains("@")) {
                                                    errorMessage = "Please enter a valid registered email address."
                                                    return@Button
                                                }
                                                if (passwordInput.isBlank()) {
                                                    errorMessage = "Please enter your password."
                                                    return@Button
                                                }
                                                isLoading = true
                                                errorMessage = null
                                                viewModel.loginWithFirebaseAuth(
                                                    email = emailInput,
                                                    passcodeOrPassword = passwordInput,
                                                    onSuccess = {
                                                        isLoading = false
                                                        successMessage = "Signed in successfully!"
                                                        scope.launch {
                                                            delay(300)
                                                            onLoginSuccess()
                                                        }
                                                    },
                                                    onError = { err ->
                                                        isLoading = false
                                                        errorMessage = err
                                                    }
                                                )
                                            },
                                            enabled = !isLoading,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = CyberCyan,
                                                contentColor = DarkObsidian
                                            ),
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(50.dp)
                                        ) {
                                            if (isLoading) {
                                                FlowButtonLoadingLine(
                                                    color = DarkObsidian,
                                                    width = 44.dp
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "AUTHENTICATING...",
                                                    style = MaterialTheme.typography.labelLarge.copy(
                                                        fontWeight = FontWeight.Black,
                                                        color = DarkObsidian
                                                    )
                                                )
                                            } else {
                                                Text(
                                                    text = "SIGN IN TO FLOWTEST",
                                                    style = MaterialTheme.typography.labelLarge.copy(
                                                        fontWeight = FontWeight.Black,
                                                        letterSpacing = 1.sp,
                                                        color = DarkObsidian
                                                    )
                                                )
                                            }
                                        }
                                    }

                                    // Quick Navigation Affordances
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextButton(
                                            onClick = {
                                                viewMode = LoginViewMode.PASSCODE
                                                errorMessage = null
                                            }
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Pin,
                                                    contentDescription = null,
                                                    tint = CyberCyan,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Sign in with PIN",
                                                    color = CyberCyan,
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                                )
                                            }
                                        }

                                        TextButton(
                                            onClick = { onNavigateToRegister() }
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "Create Account",
                                                    color = CyberCyan,
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                    contentDescription = null,
                                                    tint = CyberCyan,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    LoginViewMode.FINGERPRINT -> {
                        // PRIMARY OPTION: THUMBPRINT UNLOCK VIEW
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(24.dp))
                                    .border(1.dp, GlassBorder, RoundedCornerShape(24.dp)),
                                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp, horizontal = 20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "PRIMARY OPTION",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Black,
                                                    color = CyberCyan,
                                                    letterSpacing = 1.2.sp
                                                )
                                            )
                                            Text(
                                                text = userName,
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary
                                                )
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (effectiveAccountLocked) WarningRed.copy(alpha = 0.15f) else CyberCyan.copy(alpha = 0.15f),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, if (effectiveAccountLocked) WarningRed.copy(alpha = 0.5f) else CyberCyan.copy(alpha = 0.4f)),
                                            modifier = Modifier.clickable {
                                                if (effectiveAccountLocked) {
                                                    showReactivateDialog = true
                                                } else {
                                                    showLockAccountDialog = true
                                                }
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = if (effectiveAccountLocked) Icons.Default.LockReset else Icons.Default.Lock,
                                                    contentDescription = if (effectiveAccountLocked) "Reactivate Account" else "Freeze Account",
                                                    tint = if (effectiveAccountLocked) WarningRed else CyberCyan,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (effectiveAccountLocked) "Locked (Reactivate)" else "Freeze",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = if (effectiveAccountLocked) WarningRed else CyberCyan,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 10.sp
                                                    )
                                                )
                                            }
                                        }
                                    }

                                    if (effectiveAccountLocked) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = WarningRed.copy(alpha = 0.12f),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, WarningRed.copy(alpha = 0.35f)),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { showReactivateDialog = true }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Warning,
                                                    contentDescription = null,
                                                    tint = WarningRed,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "Account is Temporarily Frozen",
                                                        style = MaterialTheme.typography.bodySmall.copy(color = WarningRed, fontWeight = FontWeight.Bold)
                                                    )
                                                    Text(
                                                        text = "Tap here to reactivate using your PIN or SMS code.",
                                                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 10.sp)
                                                    )
                                                }
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                    contentDescription = null,
                                                    tint = WarningRed,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(28.dp))

                                    // Fingerprint Icon Tile with Pulse Scale
                                    Box(
                                        modifier = Modifier
                                            .size(96.dp)
                                            .scale(if (isBiometricScanning) scanScale else 1.0f)
                                            .clip(RoundedCornerShape(26.dp))
                                            .background(DarkSurface)
                                            .border(2.dp, CyberCyan, RoundedCornerShape(26.dp))
                                            .clickable { triggerFingerprintAuth() },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Fingerprint,
                                            contentDescription = "Fingerprint Sensor",
                                            tint = if (isBiometricScanning) ElectricEmerald else CyberCyan,
                                            modifier = Modifier.size(56.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    if (isBiometricScanning) {
                                        Text(
                                            text = "Scanning thumbprint...",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                color = ElectricEmerald,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    } else {
                                        Text(
                                            text = "Tap sensor to unlock with thumbprint",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = TextPrimary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        )
                                        Text(
                                            text = "Fastest, hardware-encrypted biometric login",
                                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(28.dp))

                                    // Switch to Passcode Button
                                    Button(
                                        onClick = {
                                            viewMode = LoginViewMode.PASSCODE
                                            errorMessage = null
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(imageVector = Icons.Default.Pin, contentDescription = null, tint = DarkObsidian, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "ENTER 6-DIGIT CODE / PIN",
                                                style = MaterialTheme.typography.labelLarge.copy(
                                                    fontWeight = FontWeight.Black,
                                                    color = DarkObsidian
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    LoginViewMode.PASSCODE -> {
                        // SECONDARY OPTION: PHONE/EMAIL & PASSCODE LOGIN VIEW
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Phone or Email Input
                            OutlinedTextField(
                                value = emailOrPhoneInput,
                                onValueChange = { emailOrPhoneInput = it },
                                label = { Text("Phone Number or Email Address") },
                                placeholder = { Text("Enter phone or enter email") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (emailOrPhoneInput.contains("@")) Icons.Default.Email else Icons.Default.Phone,
                                        contentDescription = null,
                                        tint = CyberCyan
                                    )
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyberCyan,
                                    unfocusedBorderColor = DarkCardBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedContainerColor = DarkSurfaceElevated,
                                    unfocusedContainerColor = DarkSurface
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Enter 6-digit PIN Passcode",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                )

                                if (failedPinAttempts > 0) {
                                    Text(
                                        text = "$failedPinAttempts/5 attempts",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (failedPinAttempts >= 4) Color(0xFFFF5252) else CyberCyan,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // 6 DIGIT DISPLAY BOXES
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                (0 until 6).forEach { index ->
                                    val isFilled = index < passcode.length
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(DarkSurfaceElevated)
                                            .border(
                                                width = 1.dp,
                                                color = if (isFilled) CyberCyan else DarkCardBorder,
                                                shape = RoundedCornerShape(12.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isFilled) {
                                            Box(
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .clip(CircleShape)
                                                    .background(CyberCyan)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // NUMERIC KEYPAD 3x4
                            val keys = listOf(
                                listOf("1", "2", "3"),
                                listOf("4", "5", "6"),
                                listOf("7", "8", "9"),
                                listOf("DEL", "0", "OK")
                            )

                            Column(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                keys.forEach { row ->
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        row.forEach { key ->
                                            when (key) {
                                                "DEL" -> {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(58.dp)
                                                            .clip(CircleShape)
                                                            .background(DarkSurfaceElevated)
                                                            .clickable {
                                                                if (passcode.isNotEmpty()) {
                                                                    passcode = passcode.dropLast(1)
                                                                    errorMessage = null
                                                                }
                                                            },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                                                            contentDescription = "Backspace",
                                                            tint = TextPrimary,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                }

                                                "OK" -> {
                                                    val isComplete = passcode.length >= 4
                                                    Box(
                                                        modifier = Modifier
                                                            .size(58.dp)
                                                            .clip(CircleShape)
                                                            .background(if (isComplete) CyberCyan else DarkSurfaceElevated)
                                                            .clickable(enabled = !isLoading) {
                                                                if (isComplete) {
                                                                    submitPasscode()
                                                                }
                                                            },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        if (isLoading) {
                                                            FlowButtonLoadingLine(
                                                                width = 24.dp,
                                                                height = 2.dp,
                                                                color = DarkObsidian
                                                            )
                                                        } else {
                                                            Icon(
                                                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                                contentDescription = "Submit",
                                                                tint = if (isComplete) DarkObsidian else TextPrimary,
                                                                modifier = Modifier.size(22.dp)
                                                            )
                                                        }
                                                    }
                                                }

                                                else -> {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(58.dp)
                                                            .clip(CircleShape)
                                                            .background(DarkSurfaceElevated)
                                                            .clickable {
                                                                if (passcode.length < 6) {
                                                                    passcode += key
                                                                    errorMessage = null
                                                                    if (passcode.length == 6) {
                                                                        submitPasscode()
                                                                    }
                                                                }
                                                            },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = key,
                                                            style = MaterialTheme.typography.titleMedium.copy(
                                                                fontWeight = FontWeight.Bold,
                                                                color = TextPrimary,
                                                                fontSize = 20.sp
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

                            // SEND SMS CODE / FORGOT PIN ACTION
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = {
                                        otpPhoneInput = emailOrPhoneInput.ifBlank { userVirtualAccount.phoneNumber }
                                        showForgotPinResetModal = true
                                    }
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Sms,
                                            contentDescription = "SMS Reset",
                                            tint = CyberCyan,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Reset PIN via SMS",
                                            color = CyberCyan,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                TextButton(
                                    onClick = {
                                        viewMode = LoginViewMode.FINGERPRINT
                                        triggerFingerprintAuth()
                                    }
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Fingerprint,
                                            contentDescription = null,
                                            tint = CyberCyan,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Use Thumbprint", color = CyberCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // FOOTER LINKS: Privacy Policy & Terms of Service
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text = "Privacy Policy",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = CyberCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    ),
                    modifier = Modifier
                        .clickable { showLegalDialogTitle = "Privacy Policy" }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                )

                Text(
                    text = " • ",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                )

                Text(
                    text = "Terms of Service",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = CyberCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    ),
                    modifier = Modifier
                        .clickable { showLegalDialogTitle = "Terms of Service" }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                )
            }
        }
    }

    // Lock Account Dialog
    if (showLockAccountDialog) {
        AlertDialog(
            onDismissRequest = { showLockAccountDialog = false },
            containerColor = DarkSurfaceElevated,
            title = {
                Text("Freeze Your Account", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "If your device is misplaced or compromised, freezing your account revokes active sessions and protects your wallet balance until you verify with your registered phone number.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLockAccountDialog = false
                        viewModel.lockAccountEmergency("User requested freeze from login screen")
                        isAccountLocked = true
                        errorMessage = "Account locked for security. Tap 'Locked (Reactivate)' to restore access."
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian)
                ) {
                    Text("FREEZE ACCOUNT", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLockAccountDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }

    // Reactivate Account Dialog
    if (showReactivateDialog) {
        AlertDialog(
            onDismissRequest = {
                showReactivateDialog = false
                reactivateError = null
                reactivateCodeOrPin = ""
            },
            containerColor = DarkSurfaceElevated,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.LockReset, contentDescription = null, tint = CyberCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reactivate Account", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Enter your 4 to 6 digit security PIN or SMS verification code to reactivate your account and restore access:",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 12.sp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = reactivateCodeOrPin,
                        onValueChange = { if (it.length <= 6) reactivateCodeOrPin = it },
                        label = { Text("Security PIN or SMS Code") },
                        placeholder = { Text("Enter 4-6 digit PIN or code") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (reactivateError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = reactivateError ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(color = WarningRed, fontSize = 11.sp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isReactivating = true
                        reactivateError = null
                        viewModel.unlockOrReactivateAccount(
                            identifier = emailOrPhoneInput,
                            verificationCodeOrPin = reactivateCodeOrPin,
                            onSuccess = { msg ->
                                isReactivating = false
                                isAccountLocked = false
                                failedPinAttempts = 0
                                showReactivateDialog = false
                                successMessage = msg
                                reactivateCodeOrPin = ""
                            },
                            onError = { err ->
                                isReactivating = false
                                reactivateError = err
                            }
                        )
                    },
                    enabled = !isReactivating && reactivateCodeOrPin.length >= 4,
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian)
                ) {
                    Text(if (isReactivating) "REACTIVATING..." else "REACTIVATE ACCOUNT", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showReactivateDialog = false
                    reactivateError = null
                    reactivateCodeOrPin = ""
                }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }

    // Reset PIN via SMS Code Modal (Enforces 5-minute Cooldown)
    if (showForgotPinResetModal) {
        AlertDialog(
            onDismissRequest = { showForgotPinResetModal = false },
            containerColor = DarkSurfaceElevated,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Sms, contentDescription = null, tint = CyberCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reset PIN via SMS Code", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "We will send a 6-digit verification code to your registered phone number via FlowTest SMS (5-minute cooldown between requests):",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 12.sp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = otpPhoneInput,
                        onValueChange = { otpPhoneInput = it },
                        label = { Text("Phone Number") },
                        placeholder = { Text("Enter phone") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        leadingIcon = { Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = CyberCyan) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // SEND CODE BUTTON WITH COOLDOWN TIMER
                    Button(
                        onClick = {
                            sendSmsVerificationOtp(otpPhoneInput) { msg ->
                                successMessage = msg
                            }
                        },
                        enabled = !isSendingSms && smsCooldownRemaining == 0L && otpPhoneInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (smsCooldownRemaining > 0L) DarkSurface else CyberCyan,
                            contentColor = if (smsCooldownRemaining > 0L) TextMuted else DarkObsidian
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isSendingSms) {
                            FlowButtonLoadingLine(color = DarkObsidian, width = 36.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("SENDING SMS...", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        } else if (smsCooldownRemaining > 0L) {
                            val mins = smsCooldownRemaining / 60
                            val secs = smsCooldownRemaining % 60
                            val timeStr = if (mins > 0) "${mins}m ${secs}s" else "${secs}s"
                            Icon(imageVector = Icons.Default.Timer, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("WAIT $timeStr BEFORE RESENDING", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        } else {
                            Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SEND SMS VERIFICATION CODE", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = smsCodeInput,
                        onValueChange = { if (it.length <= 6 && it.all { ch -> ch.isDigit() }) smsCodeInput = it },
                        label = { Text("6-Digit SMS Code") },
                        placeholder = { Text("e.g. 123456") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        leadingIcon = { Icon(imageVector = Icons.Default.Pin, contentDescription = null, tint = ElectricEmerald) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricEmerald,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = resetNewPinInput,
                        onValueChange = { if (it.length <= 6 && it.all { ch -> ch.isDigit() }) resetNewPinInput = it },
                        label = { Text("New 4-6 Digit PIN") },
                        placeholder = { Text("e.g. 654321") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        leadingIcon = { Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = CyberCyan) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (smsCodeInput.length < 6) {
                            errorMessage = "Please enter the 6-digit SMS code."
                            return@Button
                        }
                        if (resetNewPinInput.length < 4) {
                            errorMessage = "New PIN must be 4 to 6 digits."
                            return@Button
                        }
                        showForgotPinResetModal = false
                        viewModel.resetPinWithSmsCode(
                            phone = otpPhoneInput,
                            code = smsCodeInput,
                            newPin = resetNewPinInput,
                            onSuccess = {
                                failedPinAttempts = 0
                                isAccountLocked = false
                                successMessage = "PIN Reset Successfully! You can now log in with your new PIN."
                            },
                            onError = { err ->
                                errorMessage = err
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian)
                ) {
                    Text("VERIFY CODE & SAVE PIN", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotPinResetModal = false }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }

    // Legal Terms Dialog
    showLegalDialogTitle?.let { title ->
        AlertDialog(
            onDismissRequest = { showLegalDialogTitle = null },
            containerColor = DarkSurfaceElevated,
            title = {
                Text(title, color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = if (title == "Privacy Policy") {
                        "Your privacy and data security are paramount. FlowTest uses end-to-end encryption for all VTU transactions, server connection telemetry, and wallet operations. We never share your personal data or phone records with third parties."
                    } else {
                        "By using FlowTest services, you agree to our fair usage policies for VTU vending, SME data distribution, and WireGuard VPN proxy connections. All transactions are logged for audit compliance and instant balance protection."
                    },
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showLegalDialogTitle = null },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian)
                ) {
                    Text("I Understand", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Forgot Password Dialog
    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showForgotPasswordDialog = false },
            containerColor = DarkSurfaceElevated,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = "Reset Account Password",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, color = TextPrimary)
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Enter your registered Firebase email address. We will send a secure password reset link to your inbox:",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = forgotPasswordEmail,
                        onValueChange = { forgotPasswordEmail = it },
                        label = { Text("Registered Email") },
                        placeholder = { Text("Enter email") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (forgotPasswordSuccess) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "✓ Reset link sent! Check your inbox.",
                            color = ElectricEmerald,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (forgotPasswordEmail.isNotBlank()) {
                            isSendingResetEmail = true
                            viewModel.sendPasswordResetEmail(
                                email = forgotPasswordEmail,
                                onSuccess = {
                                    isSendingResetEmail = false
                                    forgotPasswordSuccess = true
                                },
                                onError = { err ->
                                    isSendingResetEmail = false
                                    errorMessage = err
                                    showForgotPasswordDialog = false
                                }
                            )
                        }
                    },
                    enabled = !isSendingResetEmail,
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian)
                ) {
                    Text(if (isSendingResetEmail) "Sending..." else "Send Reset Link", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotPasswordDialog = false }) {
                    Text("Close", color = TextSecondary)
                }
            }
        )
    }

    // Modern Security Inactivity Auto-Logout Bottom Sheet
    AutoLogoutSecuritySheet(
        isVisible = showAutoLogoutDialog,
        onDismiss = { viewModel.dismissAutoLogoutDialog() },
        onUnlockWithBiometric = {
            viewModel.dismissAutoLogoutDialog()
            viewMode = LoginViewMode.FINGERPRINT
            triggerFingerprintAuth()
        },
        onLoginWithPin = {
            viewModel.dismissAutoLogoutDialog()
            viewMode = LoginViewMode.PASSCODE
        },
        isFingerprintEnabled = isFingerprintEnabled,
        userName = userName,
        accountNumber = userVirtualAccount.accountNumber,
        customMessage = autoLogoutReason
    )
}
