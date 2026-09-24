package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.platform.testTag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import com.example.ui.components.FlowButtonLoadingLine
import com.example.ui.components.FlowtestLogo
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext
import com.example.ui.components.OtpChipsInput
import com.example.util.findActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkObsidian
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ElectricEmerald
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningRed
import com.example.data.ui.viewmodel.VpnViewModel
import kotlinx.coroutines.launch

@Composable
fun RegisterScreen(
    viewModel: VpnViewModel,
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: () -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var transactionPin by remember { mutableStateOf("") }

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var pinVisible by remember { mutableStateOf(false) }

    var isVerifying by remember { mutableStateOf(false) }
    var verificationChannel by remember { mutableStateOf("email") } // "email" or "phone"
    var emailPinInput by remember { mutableStateOf("") }
    var phonePinInput by remember { mutableStateOf("") }
    var pinsDispatchedNotice by remember { mutableStateOf<String?>(null) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var registeredEmailState by remember { mutableStateOf("") }

    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkObsidian)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // BRAND BADGE
            FlowtestLogo(
                emblemSize = 68.dp,
                fontSize = 24.sp,
                showWordmark = true,
                textColor = TextPrimary,
                primaryColor = CyberCyan
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (isVerifying) "Verify Email & Phone" else "Create Your Verified Account",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = CyberCyan
                )
            )

            Text(
                text = if (isVerifying) "Two-Factor Security Verification • 6-Digit PIN Codes" else "Firebase Authenticated • Instant NUBAN Virtual Account",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary,
                    fontSize = 12.sp
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Error Message Alert
            AnimatedVisibility(
                visible = errorMessage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                errorMessage?.let { err ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF3E1418)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF5252)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = err,
                                color = Color(0xFFFF8A80),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                }
            }

            // FORM CONTAINER
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
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (!isVerifying) {
                        // 1. Full Name
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = {
                                fullName = it
                                errorMessage = null
                            },
                            label = { Text("Full Name (First and Last)") },
                            placeholder = { Text("e.g. John Doe") },
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = CyberCyan)
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberCyan,
                                unfocusedBorderColor = DarkCardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = DarkSurface,
                                unfocusedContainerColor = DarkSurface
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("register_fullname_input")
                        )

                        // 2. Email Address
                        OutlinedTextField(
                            value = email,
                            onValueChange = {
                                email = it
                                errorMessage = null
                            },
                            label = { Text("Email Address") },
                            placeholder = { Text("e.g. user@example.com") },
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.Email, contentDescription = null, tint = CyberCyan)
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberCyan,
                                unfocusedBorderColor = DarkCardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = DarkSurface,
                                unfocusedContainerColor = DarkSurface
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("register_email_input")
                        )

                        // 3. Phone Number (Mandatory)
                        Column {
                            OutlinedTextField(
                                value = phone,
                                onValueChange = {
                                    phone = it.filter { char -> char.isDigit() || char == '+' }
                                    errorMessage = null
                                },
                                label = { Text("Phone Number (Required)") },
                                placeholder = { Text("e.g. 08012345678") },
                                singleLine = true,
                                leadingIcon = {
                                    Icon(Icons.Default.Phone, contentDescription = null, tint = CyberCyan)
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Phone,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyberCyan,
                                    unfocusedBorderColor = DarkCardBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedContainerColor = DarkSurface,
                                    unfocusedContainerColor = DarkSurface
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().testTag("register_phone_input")
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "💡 Needed to link your dedicated NUBAN bank account & SMS receipts.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            )
                        }

                        // 4. Password
                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                errorMessage = null
                            },
                            label = { Text("Password (Min 6 chars)") },
                            placeholder = { Text("••••••••") },
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = CyberCyan)
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
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberCyan,
                                unfocusedBorderColor = DarkCardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = DarkSurface,
                                unfocusedContainerColor = DarkSurface
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("register_password_input")
                        )

                        // 5. Confirm Password
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = {
                                confirmPassword = it
                                errorMessage = null
                            },
                            label = { Text("Confirm Password") },
                            placeholder = { Text("••••••••") },
                            singleLine = true,
                            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = CyberCyan)
                            },
                            trailingIcon = {
                                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                    Icon(
                                        imageVector = if (confirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (confirmPasswordVisible) "Hide" else "Show",
                                        tint = TextSecondary
                                    )
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberCyan,
                                unfocusedBorderColor = DarkCardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = DarkSurface,
                                unfocusedContainerColor = DarkSurface
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("register_confirm_password_input")
                        )

                        // 6. Security PIN for Login & Transactions (4-6 digits)
                        Column {
                            OutlinedTextField(
                                value = transactionPin,
                                onValueChange = {
                                    if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                                        transactionPin = it
                                        errorMessage = null
                                    }
                                },
                                label = { Text("Security PIN (Login & Transactions)") },
                                placeholder = { Text("e.g. 123456 (4-6 digits)") },
                                singleLine = true,
                                visualTransformation = if (pinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                leadingIcon = {
                                    Icon(Icons.Default.Pin, contentDescription = null, tint = CyberCyan)
                                },
                                trailingIcon = {
                                    IconButton(onClick = { pinVisible = !pinVisible }) {
                                        Icon(
                                            imageVector = if (pinVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (pinVisible) "Hide" else "Show",
                                            tint = TextSecondary
                                        )
                                    }
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.NumberPassword,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { focusManager.clearFocus() }
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyberCyan,
                                    unfocusedBorderColor = DarkCardBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedContainerColor = DarkSurface,
                                    unfocusedContainerColor = DarkSurface
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().testTag("register_pin_input")
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "🔒 This single PIN is used for both app login and authorizing transactions.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = CyberCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // VERIFICATION CHANNEL SELECTOR (EMAIL OR PHONE)
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "VERIFY ACCOUNT VIA",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary,
                                    letterSpacing = 1.sp
                                )
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Email Option
                                Surface(
                                    onClick = { verificationChannel = "email" },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (verificationChannel == "email") CyberCyan.copy(alpha = 0.15f) else DarkSurface,
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = if (verificationChannel == "email") 1.5.dp else 1.dp,
                                        color = if (verificationChannel == "email") CyberCyan else DarkCardBorder
                                    ),
                                    modifier = Modifier.weight(1f).height(48.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Email,
                                            contentDescription = null,
                                            tint = if (verificationChannel == "email") CyberCyan else TextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Email OTP",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (verificationChannel == "email") FontWeight.Bold else FontWeight.Medium,
                                                color = if (verificationChannel == "email") CyberCyan else TextSecondary
                                            )
                                        )
                                    }
                                }

                                // Phone Option
                                Surface(
                                    onClick = { verificationChannel = "phone" },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (verificationChannel == "phone") CyberCyan.copy(alpha = 0.15f) else DarkSurface,
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = if (verificationChannel == "phone") 1.5.dp else 1.dp,
                                        color = if (verificationChannel == "phone") CyberCyan else DarkCardBorder
                                    ),
                                    modifier = Modifier.weight(1f).height(48.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Phone,
                                            contentDescription = null,
                                            tint = if (verificationChannel == "phone") CyberCyan else TextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Phone SMS",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (verificationChannel == "phone") FontWeight.Bold else FontWeight.Medium,
                                                color = if (verificationChannel == "phone") CyberCyan else TextSecondary
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // PROCEED TO VERIFY BUTTON
                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                if (fullName.trim().isBlank()) {
                                    errorMessage = "Please enter your full name."
                                    return@Button
                                }
                                if (verificationChannel == "email") {
                                    if (email.trim().isBlank() || !email.contains("@") || !email.contains(".")) {
                                        errorMessage = "Please enter a valid email address."
                                        return@Button
                                    }
                                } else {
                                    if (phone.trim().isBlank() || phone.trim().length < 10) {
                                        errorMessage = "Please provide your active phone number."
                                        return@Button
                                    }
                                }
                                if (password.length < 6) {
                                    errorMessage = "Password must be at least 6 characters."
                                    return@Button
                                }
                                if (password != confirmPassword) {
                                    errorMessage = "Passwords do not match."
                                    return@Button
                                }
                                if (transactionPin.length < 4) {
                                    errorMessage = "Please set a 4 to 6 digit transaction PIN."
                                    return@Button
                                }

                                isLoading = true
                                errorMessage = null
                                viewModel.sendRegistrationVerificationPins(
                                    email = email,
                                    phone = phone,
                                    verificationMethod = verificationChannel,
                                    activity = activity,
                                    onSuccess = { _, _ ->
                                        isLoading = false
                                        isVerifying = true
                                        errorMessage = null
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
                                .height(52.dp)
                                .testTag("register_verify_proceed_btn")
                        ) {
                            if (isLoading) {
                                FlowButtonLoadingLine(color = DarkObsidian, width = 48.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "SENDING VERIFICATION CODE...",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Black,
                                        color = DarkObsidian
                                    )
                                )
                            } else {
                                Text(
                                    text = if (verificationChannel == "email") "SEND EMAIL VERIFICATION CODE" else "SEND PHONE SMS CODE",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.5.sp,
                                        color = DarkObsidian
                                    )
                                )
                            }
                        }
                    } else {
                        // SINGLE-FACTOR VERIFICATION VIEW (EMAIL OR PHONE)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Security, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(20.dp))
                                    Text(
                                        text = if (verificationChannel == "email") "Email Code Sent" else "SMS Code Sent",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = CyberCyan
                                        )
                                    )
                                }
                                Text(
                                    text = if (verificationChannel == "email")
                                        "Please enter the 6-digit verification code sent to your Gmail inbox or spam:"
                                    else
                                        "Please enter the 6-digit verification code sent via SMS to:",
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 12.sp)
                                )
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(
                                        if (verificationChannel == "email") Icons.Default.Email else Icons.Default.Phone,
                                        contentDescription = null,
                                        tint = CyberCyan,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = if (verificationChannel == "email") email.trim() else phone.trim(),
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = TextPrimary,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp
                                        )
                                    )
                                }
                            }
                        }

                        if (verificationChannel == "email") {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "6-DIGIT EMAIL CODE",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = CyberCyan,
                                            letterSpacing = 0.5.sp
                                        )
                                    )
                                    Text(
                                        text = "Check Gmail Inbox",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = TextMuted,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                                OtpChipsInput(
                                    value = emailPinInput,
                                    onValueChange = {
                                        emailPinInput = it
                                        errorMessage = null
                                    },
                                    codeLength = 6,
                                    isError = errorMessage != null,
                                    onComplete = { focusManager.clearFocus() },
                                    testTagPrefix = "register_email_otp"
                                )
                            }
                        } else {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "6-DIGIT GOOGLE SMS PIN",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = CyberCyan,
                                            letterSpacing = 0.5.sp
                                        )
                                    )
                                    Text(
                                        text = "Google Auth SMS",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = TextMuted,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                                OtpChipsInput(
                                    value = phonePinInput,
                                    onValueChange = {
                                        phonePinInput = it
                                        errorMessage = null
                                    },
                                    codeLength = 6,
                                    isError = errorMessage != null,
                                    onComplete = { focusManager.clearFocus() },
                                    testTagPrefix = "register_phone_otp"
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // COMPLETE REGISTRATION BUTTON
                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                if (verificationChannel == "email") {
                                    if (emailPinInput.length != 6) {
                                        errorMessage = "Please enter the 6-digit verification code sent to your email."
                                        return@Button
                                    }
                                } else {
                                    if (phonePinInput.length != 6) {
                                        errorMessage = "Please enter the 6-digit verification code sent to your phone."
                                        return@Button
                                    }
                                }

                                isLoading = true
                                errorMessage = null
                                viewModel.completeRegistrationAfterVerification(
                                    fullName = fullName,
                                    email = email,
                                    phone = phone,
                                    password = password,
                                    pin = transactionPin,
                                    emailPinEntered = emailPinInput,
                                    phonePinEntered = phonePinInput,
                                    verificationMethod = verificationChannel,
                                    onSuccess = { _ ->
                                        isLoading = false
                                        registeredEmailState = email.trim()
                                        showSuccessDialog = true
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
                                .height(52.dp)
                                .testTag("register_complete_btn")
                        ) {
                            if (isLoading) {
                                FlowButtonLoadingLine(color = DarkObsidian, width = 48.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "VERIFYING & CREATING...",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Black,
                                        color = DarkObsidian
                                    )
                                )
                            } else {
                                Text(
                                    text = "VERIFY & COMPLETE REGISTRATION",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.5.sp,
                                        color = DarkObsidian
                                    )
                                )
                            }
                        }

                        // RESEND AND EDIT BUTTONS
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    if (isLoading) return@TextButton
                                    isLoading = true
                                    errorMessage = null
                                    viewModel.sendRegistrationVerificationPins(
                                        email = email,
                                        phone = phone,
                                        verificationMethod = verificationChannel,
                                        activity = activity,
                                        onSuccess = { _, _ ->
                                            isLoading = false
                                            errorMessage = if (verificationChannel == "email")
                                                "Fresh verification code sent to your email!"
                                            else
                                                "Fresh verification code sent to your phone via Google SMS!"
                                        },
                                        onError = { err ->
                                            isLoading = false
                                            errorMessage = err
                                        }
                                    )
                                }
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Resend Code", color = CyberCyan, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                            }

                            TextButton(
                                onClick = {
                                    isVerifying = false
                                    errorMessage = null
                                }
                            ) {
                                Icon(Icons.Default.ArrowBack, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Edit Info", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // BACK TO LOGIN LINK
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Already have an account? ",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                )
                Text(
                    text = "Sign In",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = CyberCyan
                    ),
                    modifier = Modifier.clickable { onNavigateToLogin() }
                )
            }

            Spacer(modifier = Modifier.height(36.dp))
        }
    }

    // REGISTRATION SUCCESS & EMAIL VERIFICATION ALERT DIALOG
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {
                showSuccessDialog = false
                onRegisterSuccess()
            },
            containerColor = DarkSurfaceElevated,
            shape = RoundedCornerShape(22.dp),
            icon = {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(ElectricEmerald.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = ElectricEmerald,
                        modifier = Modifier.size(34.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "Account Created! 🎉",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Your account has been registered in Firebase.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        ),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "A verification link was sent to $registeredEmailState. Please verify your email from your inbox. Your dedicated FlowTest NUBAN virtual account has been created.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            lineHeight = 18.sp
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        onRegisterSuccess()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberCyan,
                        contentColor = DarkObsidian
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = "CONTINUE TO FLOWTEST",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Black,
                            color = DarkObsidian
                        )
                    )
                }
            }
        )
    }
}
