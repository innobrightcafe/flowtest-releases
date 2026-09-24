package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.*
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ui.viewmodel.VpnViewModel
import com.example.data.ui.viewmodel.VpnViewModel.UserVirtualAccount
import com.example.ui.components.FlowButtonLoadingLine
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class CountryCodeOption(val name: String, val code: String, val flag: String)

@Composable
fun VtuOnboardingWizardScreen(
    viewModel: VpnViewModel,
    onNavigateToLogin: () -> Unit,
    onOnboardingComplete: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    // Wizard Step State (1: Email, 2: 6-Digit Code, 3: Profile & Biometrics, 4: Success & NUBAN)
    var currentStep by remember { mutableIntStateOf(1) }

    // Session Tokens & Payloads
    var emailInput by remember { mutableStateOf("") }
    var emailToken by remember { mutableStateOf("") }
    var verifiedAccessToken by remember { mutableStateOf("") }

    // 6-Digit Verification Code Boxes
    val otpDigits = remember { mutableStateListOf("", "", "", "", "", "") }
    val otpFocusRequesters = remember { List(6) { FocusRequester() } }
    var countdownSeconds by remember { mutableIntStateOf(60) }
    var isTimerRunning by remember { mutableStateOf(false) }

    // Step 3 Profile & Biometrics Inputs
    var firstNameInput by remember { mutableStateOf("") }
    var lastNameInput by remember { mutableStateOf("") }
    val countryOptions = listOf(
        CountryCodeOption("Nigeria", "+234", "🇳🇬"),
        CountryCodeOption("United States", "+1", "🇺🇸"),
        CountryCodeOption("United Kingdom", "+44", "🇬🇧"),
        CountryCodeOption("Ghana", "+233", "🇬🇭"),
        CountryCodeOption("Kenya", "+254", "🇰🇪")
    )
    var selectedCountry by remember { mutableStateOf(countryOptions[0]) }
    var showCountryDropdown by remember { mutableStateOf(false) }
    var rawPhoneNumber by remember { mutableStateOf("") }
    var transactionPin by remember { mutableStateOf("") }
    var isPinVisible by remember { mutableStateOf(false) }
    var enableBiometrics by remember { mutableStateOf(true) }

    // Step 4 Generated NUBAN Result
    var generatedNubanAcc by remember { mutableStateOf<UserVirtualAccount?>(null) }

    // UI Loading & Feedback States
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    // Countdown Timer LaunchedEffect for Step 2
    LaunchedEffect(currentStep, isTimerRunning) {
        if (currentStep == 2 && isTimerRunning) {
            while (countdownSeconds > 0) {
                delay(1000L)
                countdownSeconds -= 1
            }
            isTimerRunning = false
        }
    }

    // Clear Toast message after delay
    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(3000L)
            toastMessage = null
        }
    }

    // Function to submit 6-digit PIN
    fun handleConfirm6DigitCode() {
        val fullCode = otpDigits.joinToString("")
        if (fullCode.length < 6) {
            errorMessage = "Please enter all 6 digits of the verification code."
            return
        }

        isLoading = true
        errorMessage = null
        focusManager.clearFocus()

        viewModel.confirmEmailCodeApi(
            emailToken = emailToken,
            code = fullCode,
            onSuccess = { token ->
                isLoading = false
                verifiedAccessToken = token
                toastMessage = "Email verified! Continue to profile setup."
                currentStep = 3
            },
            onError = { err ->
                isLoading = false
                errorMessage = err
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkObsidian)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // TOP HEADER & STEP INDICATOR
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(CyberCyan.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "VTU",
                        tint = CyberCyan,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "FLOWTEST",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Black,
                            color = TextPrimary,
                            letterSpacing = 1.sp
                        )
                    )
                    Text(
                        text = "VTU Airtime, Data & NUBAN Account",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = CyberCyan,
                            fontSize = 10.sp
                        )
                    )
                }
            }

            if (currentStep < 4) {
                TextButton(onClick = onNavigateToLogin) {
                    Text("Sign In", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // STEP PROGRESS BAR
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(4) { idx ->
                val stepNum = idx + 1
                val isCompleted = currentStep > stepNum
                val isCurrent = currentStep == stepNum
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            when {
                                isCompleted -> ElectricEmerald
                                isCurrent -> CyberCyan
                                else -> DarkCardBorder
                            }
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "STEP $currentStep OF 4",
                style = MaterialTheme.typography.labelSmall.copy(color = CyberCyan, fontWeight = FontWeight.Bold)
            )
            Text(
                text = when (currentStep) {
                    1 -> "Email Capture"
                    2 -> "6-Digit Code"
                    3 -> "Profile & Security"
                    else -> "NUBAN Ready"
                },
                style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // INLINE TOAST / ERROR MESSAGES
        AnimatedVisibility(visible = errorMessage != null) {
            errorMessage?.let { err ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF3E1212)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF5252))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFFF5252))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = err, style = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontSize = 12.sp))
                    }
                }
            }
        }

        AnimatedVisibility(visible = toastMessage != null) {
            toastMessage?.let { msg ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0D3320)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ElectricEmerald)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = ElectricEmerald)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = msg, style = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontSize = 12.sp))
                    }
                }
            }
        }

        // STEP SCREENS ANIMATED CROSSFADE
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { width -> width } + fadeIn() togetherWith slideOutHorizontally { width -> -width } + fadeOut()
                    } else {
                        slideInHorizontally { width -> -width } + fadeIn() togetherWith slideOutHorizontally { width -> width } + fadeOut()
                    }
                },
                label = "WizardStepTransition"
            ) { step ->
                when (step) {
                    1 -> {
                        // SCREEN 1: EMAIL CAPTURE
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(10.dp))

                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(DarkSurfaceElevated)
                                    .border(2.dp, CyberCyan, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MarkEmailRead,
                                    contentDescription = "Email Setup",
                                    tint = CyberCyan,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Create Your Account",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    textAlign = TextAlign.Center
                                )
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Enter your email address to receive a 6-digit verification code & activate your instant NUBAN account.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center,
                                    fontSize = 13.sp
                                )
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            OutlinedTextField(
                                value = emailInput,
                                onValueChange = { emailInput = it.trim() },
                                label = { Text("Email Address") },
                                placeholder = { Text("e.g. user@flowtest2026.com") },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = CyberCyan)
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyberCyan,
                                    unfocusedBorderColor = DarkCardBorder,
                                    focusedLabelColor = CyberCyan,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = {
                                    if (emailInput.isBlank() || !emailInput.contains("@")) {
                                        errorMessage = "Please enter a valid email address."
                                        return@Button
                                    }
                                    isLoading = true
                                    errorMessage = null
                                    viewModel.requestEmailVerificationApi(
                                        email = emailInput,
                                        onSuccess = { token ->
                                            isLoading = false
                                            emailToken = token
                                            countdownSeconds = 60
                                            isTimerRunning = true
                                            toastMessage = "Verification code sent to $emailInput"
                                            currentStep = 2
                                        },
                                        onError = { err ->
                                            isLoading = false
                                            errorMessage = err
                                        }
                                    )
                                },
                                enabled = !isLoading && emailInput.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CyberCyan,
                                    contentColor = DarkObsidian
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                            ) {
                                if (isLoading) {
                                    FlowButtonLoadingLine(color = DarkObsidian, width = 48.dp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("SENDING VERIFICATION CODE...", fontWeight = FontWeight.Bold)
                                } else {
                                    Text("SEND CODE & CONTINUE", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = ElectricEmerald, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("WHAT YOU GET:", style = MaterialTheme.typography.labelSmall.copy(color = TextPrimary, fontWeight = FontWeight.Bold))
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("• Direct 10-Digit Company Bank Transfer with instant credit", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp))
                                    Text("• ₦5,000 Welcome Wallet Balance loaded", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp))
                                    Text("• Unrestricted VTU Airtime & Data purchases with instant cashback", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp))
                                }
                            }
                        }
                    }

                    2 -> {
                        // SCREEN 2: 6-DIGIT CODE INPUT
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(10.dp))

                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(DarkSurfaceElevated)
                                    .border(2.dp, CyberCyan, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Pin,
                                    contentDescription = "Code",
                                    tint = CyberCyan,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Enter 6-Digit Verification Code",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    textAlign = TextAlign.Center
                                )
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "We sent a 6-digit confirmation code to $emailInput. Please check your inbox or spam folder.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center,
                                    fontSize = 12.sp
                                )
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            // 6 INDIVIDUAL NUMERIC BOXES WITH AUTOMATIC FOCUS FORWARD
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                repeat(6) { index ->
                                    OutlinedTextField(
                                        value = otpDigits[index],
                                        onValueChange = { newValue ->
                                            if (newValue.length <= 1 && newValue.all { it.isDigit() }) {
                                                otpDigits[index] = newValue
                                                if (newValue.isNotEmpty()) {
                                                    if (index < 5) {
                                                        otpFocusRequesters[index + 1].requestFocus()
                                                    } else {
                                                        // 6th digit typed - trigger auto submit!
                                                        handleConfirm6DigitCode()
                                                    }
                                                }
                                            } else if (newValue.isEmpty()) {
                                                otpDigits[index] = ""
                                                if (index > 0) {
                                                    otpFocusRequesters[index - 1].requestFocus()
                                                }
                                            }
                                        },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 3.dp)
                                            .focusRequester(otpFocusRequesters[index]),
                                        textStyle = MaterialTheme.typography.titleLarge.copy(
                                            textAlign = TextAlign.Center,
                                            fontWeight = FontWeight.Black,
                                            color = CyberCyan
                                        ),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = CyberCyan,
                                            unfocusedBorderColor = DarkCardBorder,
                                            focusedContainerColor = DarkSurfaceElevated,
                                            unfocusedContainerColor = DarkSurface
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // COUNTDOWN TIMER & RESEND LINK
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                if (countdownSeconds > 0) {
                                    Icon(imageVector = Icons.Default.Timer, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Resend Code in ${countdownSeconds}s",
                                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 12.sp)
                                    )
                                } else {
                                    TextButton(
                                        onClick = {
                                            isLoading = true
                                            viewModel.requestEmailVerificationApi(
                                                email = emailInput,
                                                onSuccess = { token ->
                                                    isLoading = false
                                                    emailToken = token
                                                    countdownSeconds = 60
                                                    isTimerRunning = true
                                                    toastMessage = "New verification code sent!"
                                                },
                                                onError = { err ->
                                                    isLoading = false
                                                    errorMessage = err
                                                }
                                            )
                                        }
                                    ) {
                                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Resend Verification Code", color = CyberCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = { handleConfirm6DigitCode() },
                                enabled = !isLoading && otpDigits.joinToString("").length == 6,
                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                            ) {
                                if (isLoading) {
                                    FlowButtonLoadingLine(color = DarkObsidian, width = 48.dp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("VERIFYING CODE...", fontWeight = FontWeight.Bold)
                                } else {
                                    Text("VERIFY & ADVANCE", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            TextButton(onClick = { currentStep = 1 }) {
                                Text("Change Email Address", color = TextMuted, fontSize = 12.sp)
                            }
                        }
                    }

                    3 -> {
                        // SCREEN 3: PROFILE COMPLETION & BIOMETRICS
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(10.dp))

                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(DarkSurfaceElevated)
                                    .border(2.dp, ElectricEmerald, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LockPerson,
                                    contentDescription = "Security",
                                    tint = ElectricEmerald,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Profile & Security Setup",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    textAlign = TextAlign.Center
                                )
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Set up your legal name, phone number, 4-6 digit transaction PIN, and enable biometric fingerprint login.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center,
                                    fontSize = 12.sp
                                )
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // FIRST NAME & LAST NAME (FOR VIRTUAL BANK ACCOUNT)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = firstNameInput,
                                    onValueChange = { firstNameInput = it },
                                    label = { Text("First Name") },
                                    placeholder = { Text("First name") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CyberCyan,
                                        unfocusedBorderColor = DarkCardBorder,
                                        focusedLabelColor = CyberCyan,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.weight(1f)
                                )

                                OutlinedTextField(
                                    value = lastNameInput,
                                    onValueChange = { lastNameInput = it },
                                    label = { Text("Last Name") },
                                    placeholder = { Text("Last name") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CyberCyan,
                                        unfocusedBorderColor = DarkCardBorder,
                                        focusedLabelColor = CyberCyan,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // PHONE NUMBER WITH COUNTRY CODE DROPDOWN
                            Text(
                                text = "Phone Number (Linked to NUBAN)",
                                style = MaterialTheme.typography.labelMedium.copy(color = TextPrimary, fontWeight = FontWeight.Bold),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Country Selector Box
                                Surface(
                                    onClick = { showCountryDropdown = true },
                                    shape = RoundedCornerShape(14.dp),
                                    color = DarkSurfaceElevated,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                                    modifier = Modifier.height(56.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(selectedCountry.flag, fontSize = 18.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(selectedCountry.code, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = TextMuted)
                                    }

                                    DropdownMenu(
                                        expanded = showCountryDropdown,
                                        onDismissRequest = { showCountryDropdown = false },
                                        modifier = Modifier.background(DarkSurfaceElevated)
                                    ) {
                                        countryOptions.forEach { country ->
                                            DropdownMenuItem(
                                                text = {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(country.flag, fontSize = 18.sp)
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text("${country.name} (${country.code})", color = TextPrimary, fontSize = 13.sp)
                                                    }
                                                },
                                                onClick = {
                                                    selectedCountry = country
                                                    showCountryDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                OutlinedTextField(
                                    value = rawPhoneNumber,
                                    onValueChange = { if (it.length <= 11 && it.all { ch -> ch.isDigit() }) rawPhoneNumber = it },
                                    placeholder = { Text("8012345678") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CyberCyan,
                                        unfocusedBorderColor = DarkCardBorder,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // SECURE TRANSACTION PIN (4-6 DIGITS)
                            OutlinedTextField(
                                value = transactionPin,
                                onValueChange = { if (it.length <= 6 && it.all { ch -> ch.isDigit() }) transactionPin = it },
                                label = { Text("Create 4-6 Digit Transaction PIN") },
                                placeholder = { Text("e.g. 123456") },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = CyberCyan)
                                },
                                trailingIcon = {
                                    IconButton(onClick = { isPinVisible = !isPinVisible }) {
                                        Icon(
                                            imageVector = if (isPinVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = null,
                                            tint = TextMuted
                                        )
                                    }
                                },
                                singleLine = true,
                                visualTransformation = if (isPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyberCyan,
                                    unfocusedBorderColor = DarkCardBorder,
                                    focusedLabelColor = CyberCyan,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // BIOMETRIC SWITCH TOGGLE
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(imageVector = Icons.Default.Fingerprint, contentDescription = null, tint = ElectricEmerald, modifier = Modifier.size(28.dp))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text("Thumbprint / Biometrics Login", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
                                            Text("Enable fast device hardware biometric authentication", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 11.sp))
                                        }
                                    }

                                    Switch(
                                        checked = enableBiometrics,
                                        onCheckedChange = { enableBiometrics = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = DarkObsidian,
                                            checkedTrackColor = ElectricEmerald,
                                            uncheckedThumbColor = TextMuted,
                                            uncheckedTrackColor = DarkSurface
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = {
                                    if (rawPhoneNumber.length < 10) {
                                        errorMessage = "Please enter a valid 10 to 11 digit phone number."
                                        return@Button
                                    }
                                    if (transactionPin.length < 4) {
                                        errorMessage = "Transaction PIN must be 4 to 6 numeric digits."
                                        return@Button
                                    }

                                    val fullPhone = selectedCountry.code + rawPhoneNumber
                                    val fullName = "$firstNameInput $lastNameInput".trim()
                                    isLoading = true
                                    errorMessage = null

                                    viewModel.completeProfileOnboardingApi(
                                        verifiedAccessToken = verifiedAccessToken.ifBlank { "token_live_${System.currentTimeMillis()}" },
                                        fullName = fullName,
                                        email = emailInput,
                                        phone = fullPhone,
                                        pin = transactionPin,
                                        enableBiometrics = enableBiometrics,
                                        onSuccess = { acc ->
                                            isLoading = false
                                            generatedNubanAcc = acc
                                            toastMessage = "NUBAN account generated successfully!"
                                            currentStep = 4
                                        },
                                        onError = { err ->
                                            isLoading = false
                                            errorMessage = err
                                        }
                                    )
                                },
                                enabled = !isLoading && rawPhoneNumber.isNotBlank() && transactionPin.length >= 4,
                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                            ) {
                                if (isLoading) {
                                    FlowButtonLoadingLine(color = DarkObsidian, width = 48.dp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("GENERATING NUBAN ACCOUNT...", fontWeight = FontWeight.Bold)
                                } else {
                                    Text("GENERATE NUBAN & FINISH", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    else -> {
                        // SCREEN 4: SUCCESS & NUBAN DISPLAY
                        val account = generatedNubanAcc ?: viewModel.userVirtualAccount.collectAsState().value

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(10.dp))

                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(ElectricEmerald.copy(alpha = 0.2f))
                                    .border(3.dp, ElectricEmerald, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Success",
                                    tint = ElectricEmerald,
                                    modifier = Modifier.size(48.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Account Setup Complete!",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    color = TextPrimary,
                                    textAlign = TextAlign.Center
                                )
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Your profile is registered! Fund your wallet by transferring to the official Company Bank Account below with your phone number as narration.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center,
                                    fontSize = 12.sp
                                )
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // HIGHLIGHTED NUBAN CARD CONTAINER
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .border(1.dp, CyberCyan, RoundedCornerShape(20.dp)),
                                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = ElectricEmerald.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = account.bankName.ifBlank { "FlowTest Settlement" },
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = ElectricEmerald,
                                                    fontWeight = FontWeight.Black
                                                ),
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }

                                        Text(
                                            text = "TIER 2 VERIFIED",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = CyberCyan,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp
                                            )
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text(
                                        text = "NUBAN ACCOUNT NUMBER",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = TextMuted,
                                            letterSpacing = 1.2.sp,
                                            fontSize = 10.sp
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = account.accountNumber.ifBlank { "8123456789" },
                                        style = MaterialTheme.typography.headlineLarge.copy(
                                            fontWeight = FontWeight.Black,
                                            color = CyberCyan,
                                            letterSpacing = 3.sp,
                                            fontSize = 32.sp
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = account.accountName.ifBlank { "FLOWTEST VTU" },
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // COPY ACCOUNT NUMBER BUTTON
                                    Button(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("NUBAN Account", account.accountNumber.ifBlank { "8123456789" })
                                            clipboard.setPrimaryClip(clip)
                                            toastMessage = "NUBAN Account Number Copied to Clipboard!"
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("COPY ACCOUNT NUMBER", fontWeight = FontWeight.Black, fontSize = 12.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // WELCOME BONUS CARD
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = DarkSurface)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.CardGiftcard, contentDescription = null, tint = Color(0xFFFFB74D), modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("WELCOME PERKS LOADED", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Wallet Starting Balance:", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted))
                                        Text("₦5,000.00", style = MaterialTheme.typography.bodySmall.copy(color = ElectricEmerald, fontWeight = FontWeight.Bold))
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Cashback Points:", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted))
                                        Text("250 PTS", style = MaterialTheme.typography.bodySmall.copy(color = CyberCyan, fontWeight = FontWeight.Bold))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = onOnboardingComplete,
                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                            ) {
                                Text("START USING VTU DASHBOARD", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black))
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(imageVector = Icons.Default.RocketLaunch, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
