package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ui.viewmodel.VpnViewModel
import com.example.ui.components.FlowtestEmblem
import com.example.ui.theme.*
import com.example.util.BiometricAuthHelper

enum class SessionUnlockMode {
    BIOMETRIC,
    PASSCODE
}

@Composable
fun SessionLockScreen(
    viewModel: VpnViewModel,
    onUnlocked: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { BiometricAuthHelper.findFragmentActivity(context) }
    val userVirtualAccount by viewModel.userVirtualAccount.collectAsStateWithLifecycle()
    val isFingerprintEnabled by viewModel.isFingerprintEnabled.collectAsStateWithLifecycle()
    val autoLogoutReason by viewModel.autoLogoutReason.collectAsStateWithLifecycle()
    val isAccountLocked by viewModel.isAccountLocked.collectAsStateWithLifecycle()

    var unlockMode by remember { mutableStateOf(SessionUnlockMode.BIOMETRIC) }
    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    var showEmergencyLockDialog by remember { mutableStateOf(false) }
    var showReactivateDialog by remember { mutableStateOf(false) }
    var showResetPinDialog by remember { mutableStateOf(false) }
    var resetNewPin by remember { mutableStateOf("") }
    var resetError by remember { mutableStateOf<String?>(null) }
    var reactivateCodeOrPin by remember { mutableStateOf("") }
    var isReactivating by remember { mutableStateOf(false) }
    var reactivateError by remember { mutableStateOf<String?>(null) }

    // User display name: Extract first name in UPPERCASE from signup / profile
    val displayName = remember(userVirtualAccount.fullName, userVirtualAccount.email, userVirtualAccount.phoneNumber) {
        val candidate = userVirtualAccount.fullName.trim().ifBlank {
            context.getSharedPreferences("auth_prefs", android.content.Context.MODE_PRIVATE)
                .getString("saved_user_name", "")?.trim() ?: ""
        }
        if (candidate.isNotBlank() && candidate != "Valued User" && candidate != "User") {
            val first = candidate.split(" ").firstOrNull { it.isNotBlank() } ?: candidate
            first.uppercase()
        } else {
            val emailPrefix = userVirtualAccount.email.substringBefore("@").filter { it.isLetterOrDigit() }
            if (emailPrefix.isNotBlank() && emailPrefix.length >= 2) {
                emailPrefix.uppercase()
            } else if (userVirtualAccount.phoneNumber.isNotBlank()) {
                "USER ${userVirtualAccount.phoneNumber.takeLast(4)}"
            } else {
                "VALUED USER"
            }
        }
    }

    // Biometric Trigger Function
    val triggerBiometricPrompt: () -> Unit = {
        if (isAccountLocked) {
            pinError = "Account is locked. Tap Reactivate above."
        } else {
            pinError = null
            try {
                if (activity != null && BiometricAuthHelper.canAuthenticate(context)) {
                    BiometricAuthHelper.promptBiometric(
                        activity = activity,
                        title = "Fingerprint / Face Unlock",
                        subtitle = "Touch sensor or verify face to unlock your session",
                        negativeButtonText = "Use Passcode",
                        onSuccess = {
                            onUnlocked()
                        },
                        onError = { err ->
                            if (err != "cancelled") {
                                pinError = err
                            }
                            unlockMode = SessionUnlockMode.PASSCODE
                        }
                    )
                } else {
                    // If device has no biometric hardware or not enrolled, switch to Passcode
                    unlockMode = SessionUnlockMode.PASSCODE
                    if (isFingerprintEnabled) {
                        pinError = "Biometric sensor unavailable or not enrolled. Please use PIN."
                    }
                }
            } catch (e: Throwable) {
                unlockMode = SessionUnlockMode.PASSCODE
                pinError = "Biometrics unavailable. Please enter your PIN."
            }
        }
    }

    // Automatically trigger biometric unlock on initial load if biometrics are enabled and not locked
    LaunchedEffect(Unit) {
        try {
            if (!isAccountLocked && isFingerprintEnabled && BiometricAuthHelper.canAuthenticate(context)) {
                triggerBiometricPrompt()
            } else {
                unlockMode = SessionUnlockMode.PASSCODE
            }
        } catch (e: Throwable) {
            unlockMode = SessionUnlockMode.PASSCODE
        }
    }

    // Prevent bypassing lock screen via system back press
    BackHandler {
        if (unlockMode == SessionUnlockMode.PASSCODE && isFingerprintEnabled) {
            unlockMode = SessionUnlockMode.BIOMETRIC
            pinError = null
        }
    }

    // Pulse animation for the fingerprint button
    val infiniteTransition = rememberInfiniteTransition(label = "BiometricPulse")
    val haloPulse by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "HaloPulse"
    )
    val haloAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "HaloAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkObsidian)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("session_lock_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ==========================================
            // TOP SECTION: LOGO & "LOCK YOUR ACCOUNT"
            // ==========================================
            Column {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // FlowTest Emblem with smooth rotation
                    FlowtestEmblem(
                        size = 46.dp,
                        animateRotationOnLoad = true
                    )

                    // Right: "Lost your phone? Lock your account" / "Account Locked (Reactivate)"
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                if (isAccountLocked) {
                                    showReactivateDialog = true
                                } else {
                                    showEmergencyLockDialog = true
                                }
                            }
                            .padding(4.dp)
                            .testTag("lock_account_emergency_btn")
                    ) {
                        Text(
                            text = if (isAccountLocked) "Security Freeze" else "Lost your phone?",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (isAccountLocked) WarningRed else TextMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isAccountLocked) Icons.Default.LockReset else Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (isAccountLocked) WarningRed else CyberCyan,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isAccountLocked) "Reactivate account" else "Lock your account",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (isAccountLocked) WarningRed else CyberCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(44.dp))

                // ==========================================
                // GREETING: "Welcome Back, $displayName"
                // ==========================================
                Text(
                    text = "Welcome Back,",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 28.sp,
                        letterSpacing = (-0.5).sp
                    )
                )
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        color = CyberCyan,
                        fontWeight = FontWeight.Black,
                        fontSize = 32.sp,
                        letterSpacing = (-0.5).sp
                    )
                )

                if (isAccountLocked) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = WarningRed.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, WarningRed.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showReactivateDialog = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = WarningRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Account Locked for Security",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = WarningRed,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = "Tap here to reactivate using your security PIN or SMS verification code.",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TextSecondary,
                                        fontSize = 10.sp
                                    )
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = WarningRed,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                if (autoLogoutReason.isNotBlank() && !autoLogoutReason.contains("seen no activity", ignoreCase = true)) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = autoLogoutReason,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            // ==========================================
            // CENTER CONTENT: BIOMETRIC CARD OR PASSCODE
            // ==========================================
            AnimatedContent(
                targetState = unlockMode,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
                },
                label = "UnlockModeTransition"
            ) { mode ->
                when (mode) {
                    SessionUnlockMode.BIOMETRIC -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Elevated Central Card
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = DarkSurface,
                                border = BorderStroke(1.dp, DarkCardBorder),
                                shadowElevation = 8.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 36.dp, horizontal = 20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Fingerprint Unlock",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 20.sp
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(28.dp))

                                    // Interactive Biometric Sensor Squircle
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.size(116.dp)
                                    ) {
                                        // Outer pulsing halo
                                        Box(
                                            modifier = Modifier
                                                .size(112.dp)
                                                .scale(haloPulse)
                                                .clip(RoundedCornerShape(32.dp))
                                                .background(CyberCyan.copy(alpha = haloAlpha * 0.28f))
                                        )

                                        // Central Button
                                        Surface(
                                            shape = RoundedCornerShape(26.dp),
                                            color = DarkSurfaceElevated,
                                            border = BorderStroke(1.5.dp, CyberCyan.copy(alpha = 0.5f)),
                                            modifier = Modifier
                                                .size(92.dp)
                                                .clip(RoundedCornerShape(26.dp))
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = null
                                                ) {
                                                    triggerBiometricPrompt()
                                                }
                                                .testTag("session_lock_fingerprint_btn")
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier.fillMaxSize()
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Fingerprint,
                                                    contentDescription = "Fingerprint sensor",
                                                    tint = CyberCyan,
                                                    modifier = Modifier.size(54.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(20.dp))

                                    Text(
                                        text = "Tap to verify thumbprint or face",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = TextSecondary,
                                            fontSize = 12.sp
                                        )
                                    )

                                    if (pinError != null) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = pinError!!,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = WarningRed,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // "Use Passcode Instead" Button
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = DarkSurface,
                                border = BorderStroke(1.dp, DarkCardBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        unlockMode = SessionUnlockMode.PASSCODE
                                        pinError = null
                                    }
                                    .testTag("session_lock_use_passcode_btn")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = CyberCyan,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "Use Passcode Instead",
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                color = CyberCyan,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            )
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            TextButton(
                                onClick = {
                                    showResetPinDialog = true
                                    resetError = null
                                    resetNewPin = ""
                                },
                                modifier = Modifier.testTag("session_lock_reset_pin_biometric_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VpnKey,
                                    contentDescription = null,
                                    tint = CyberCyan,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Forgot or Reset PIN",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = CyberCyan,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }
                    }

                    SessionUnlockMode.PASSCODE -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            PasscodeKeypadView(
                                pin = enteredPin,
                                error = pinError,
                                canUseBiometrics = BiometricAuthHelper.canAuthenticate(context),
                                onDigitEntered = { digit ->
                                    if (enteredPin.length < 6) {
                                        val nextPin = enteredPin + digit
                                        enteredPin = nextPin
                                        pinError = null

                                        // Verify at 4 digits first; if correct unlock immediately!
                                        if (nextPin.length == 4) {
                                            if (isAccountLocked) {
                                                pinError = "Account is locked. Tap Reactivate above."
                                                enteredPin = ""
                                            } else if (viewModel.verifyPin(nextPin)) {
                                                onUnlocked()
                                            }
                                        } else if (nextPin.length == 6) {
                                            if (isAccountLocked) {
                                                pinError = "Account is locked. Tap Reactivate above."
                                                enteredPin = ""
                                            } else if (viewModel.verifyPin(nextPin)) {
                                                onUnlocked()
                                            } else {
                                                pinError = "Incorrect Passcode. Try again or tap Reset PIN below."
                                                enteredPin = ""
                                            }
                                        }
                                    }
                                },
                                onBackspace = {
                                    if (enteredPin.isNotEmpty()) {
                                        enteredPin = enteredPin.dropLast(1)
                                        pinError = null
                                    }
                                },
                                onSwitchToBiometric = {
                                    unlockMode = SessionUnlockMode.BIOMETRIC
                                    enteredPin = ""
                                    pinError = null
                                    triggerBiometricPrompt()
                                }
                            )

                            // Quick Reset PIN / Forgot PIN shortcut
                            Spacer(modifier = Modifier.height(10.dp))
                            TextButton(
                                onClick = {
                                    showResetPinDialog = true
                                    resetError = null
                                    resetNewPin = ""
                                },
                                modifier = Modifier.testTag("reset_pin_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VpnKey,
                                    contentDescription = null,
                                    tint = CyberCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Forgot or want to reset PIN?",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = CyberCyan,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // ==========================================
            // BOTTOM FOOTER: REGULATORY & SECURITY
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Secured with ",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    )
                    Text(
                        text = "WireGuard® Zero-Trust",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                    Text(
                        text = " & Hardware Biometrics.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            Toast.makeText(context, "FlowTest bank-grade AES-256-GCM & ChaCha20 encryption active.", Toast.LENGTH_SHORT).show()
                        }
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Read our ",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    )
                    Text(
                        text = "Privacy Policy ↗",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = CyberCyan,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }

        // Emergency Freeze Dialog
        if (showEmergencyLockDialog) {
            AlertDialog(
                onDismissRequest = { showEmergencyLockDialog = false },
                icon = {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(WarningRed.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Lock Account",
                            tint = WarningRed,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                title = {
                    Text(
                        text = "Emergency Device Lock?",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        ),
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Text(
                        text = "If your phone is lost or compromised, locking your account will instantly terminate all active sessions, invalidate local tokens, and safeguard your wallet funds.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextSecondary,
                            fontSize = 13.sp
                        ),
                        textAlign = TextAlign.Center
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showEmergencyLockDialog = false
                            viewModel.lockAccountEmergency()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberCyan,
                            contentColor = DarkObsidian
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Lock Account Now", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEmergencyLockDialog = false }) {
                        Text("Cancel", color = TextMuted)
                    }
                },
                containerColor = DarkSurfaceElevated,
                shape = RoundedCornerShape(22.dp)
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
                            text = "Enter your 4 to 6 digit security PIN or SMS verification code to reactivate your account and unlock your wallet:",
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
                            val id = userVirtualAccount.phoneNumber.ifBlank { userVirtualAccount.email }
                            viewModel.unlockOrReactivateAccount(
                                identifier = id,
                                verificationCodeOrPin = reactivateCodeOrPin,
                                onSuccess = { msg ->
                                    isReactivating = false
                                    showReactivateDialog = false
                                    reactivateCodeOrPin = ""
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    onUnlocked()
                                },
                                onError = { err ->
                                    isReactivating = false
                                    reactivateError = err
                                }
                            )
                        },
                        enabled = !isReactivating && reactivateCodeOrPin.length >= 4,
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (isReactivating) "REACTIVATING..." else "REACTIVATE NOW", fontWeight = FontWeight.Bold)
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
                },
                shape = RoundedCornerShape(22.dp)
            )
        }

        // Reset Security PIN Dialog
        if (showResetPinDialog) {
            AlertDialog(
                onDismissRequest = {
                    showResetPinDialog = false
                    resetError = null
                    resetNewPin = ""
                },
                containerColor = DarkSurfaceElevated,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.VpnKey, contentDescription = null, tint = CyberCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Set / Reset Passcode", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Set a new 4 or 6 digit security PIN for your wallet. It will be saved securely and immediately unlock your session:",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 12.sp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedTextField(
                            value = resetNewPin,
                            onValueChange = { input ->
                                val digitsOnly = input.filter { it.isDigit() }
                                if (digitsOnly.length <= 6) {
                                    resetNewPin = digitsOnly
                                    resetError = null
                                }
                            },
                            label = { Text("New 4 or 6 Digit PIN") },
                            placeholder = { Text("e.g. 1234 or 123456") },
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
                        if (resetError != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = resetError ?: "",
                                style = MaterialTheme.typography.bodySmall.copy(color = WarningRed, fontSize = 11.sp)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (resetNewPin.length == 4 || resetNewPin.length == 6) {
                                viewModel.setSecurityPin(resetNewPin)
                                showResetPinDialog = false
                                Toast.makeText(context, "New security PIN saved! Session unlocked.", Toast.LENGTH_SHORT).show()
                                onUnlocked()
                            } else {
                                resetError = "Please enter exactly 4 or 6 digits."
                            }
                        },
                        enabled = resetNewPin.length in listOf(4, 6),
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("SAVE & UNLOCK", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showResetPinDialog = false
                        resetError = null
                        resetNewPin = ""
                    }) {
                        Text("Cancel", color = TextMuted)
                    }
                },
                shape = RoundedCornerShape(22.dp)
            )
        }
    }
}

@Composable
private fun PasscodeKeypadView(
    pin: String,
    error: String?,
    canUseBiometrics: Boolean,
    onDigitEntered: (String) -> Unit,
    onBackspace: () -> Unit,
    onSwitchToBiometric: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Enter Security Passcode",
            style = MaterialTheme.typography.titleMedium.copy(
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        )

        Spacer(modifier = Modifier.height(14.dp))

        // PIN Indicators (Dynamic: shows 4 or 6 dots based on input length)
        val maxDots = if (pin.length > 4) 6 else 4
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until maxDots) {
                val isFilled = i < pin.length
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(if (isFilled) CyberCyan else Color.Transparent)
                        .border(
                            width = 1.5.dp,
                            color = if (isFilled) CyberCyan else DarkCardBorder,
                            shape = CircleShape
                        )
                )
            }
        }

        if (error != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = WarningRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Numeric Keypad (1-9, Switch, 0, Backspace)
        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("BIOMETRIC", "0", "BACKSPACE")
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            rows.forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    row.forEach { item ->
                        when (item) {
                            "BIOMETRIC" -> {
                                if (canUseBiometrics) {
                                    IconButton(
                                        onClick = onSwitchToBiometric,
                                        modifier = Modifier
                                            .size(62.dp)
                                            .clip(CircleShape)
                                            .background(DarkSurface)
                                            .border(1.dp, DarkCardBorder, CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Fingerprint,
                                            contentDescription = "Switch to Fingerprint",
                                            tint = CyberCyan,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.size(62.dp))
                                }
                            }
                            "BACKSPACE" -> {
                                IconButton(
                                    onClick = onBackspace,
                                    modifier = Modifier
                                        .size(62.dp)
                                        .clip(CircleShape)
                                        .background(DarkSurface)
                                        .border(1.dp, DarkCardBorder, CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                                            contentDescription = "Delete Digit",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                            }
                            else -> {
                                Surface(
                                    shape = CircleShape,
                                    color = DarkSurface,
                                    border = BorderStroke(1.dp, DarkCardBorder),
                                    modifier = Modifier
                                        .size(62.dp)
                                        .clip(CircleShape)
                                        .clickable { onDigitEntered(item) }
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Text(
                                            text = item,
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                color = TextPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 22.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        )
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
