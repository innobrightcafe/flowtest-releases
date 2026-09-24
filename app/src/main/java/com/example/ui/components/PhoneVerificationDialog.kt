package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ui.viewmodel.VpnViewModel
import com.example.ui.theme.*
import com.example.util.findActivity
import kotlinx.coroutines.delay

/**
 * Free Phone Verification Dialog.
 * Direct, simple verification with automated background safety validation.
 */
@Composable
fun PhoneVerificationDialog(
    viewModel: VpnViewModel,
    initialPhone: String = "",
    title: String = "Free Verification",
    description: String = "Free verification to secure your transactions.",
    onDismiss: () -> Unit,
    onSuccess: (verifiedPhone: String) -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val isSendingOtp by viewModel.isSendingPhoneOtp.collectAsStateWithLifecycle()
    val phoneVerificationId by viewModel.phoneVerificationId.collectAsStateWithLifecycle()

    var step by remember { mutableIntStateOf(1) } // 1: Enter Phone, 2: Enter Code
    var phoneInput by remember { mutableStateOf(initialPhone.ifBlank { "" }) }
    var otpInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var resendCooldown by remember { mutableIntStateOf(0) }
    var isVerifying by remember { mutableStateOf(false) }

    // Resend countdown timer
    LaunchedEffect(resendCooldown) {
        if (resendCooldown > 0) {
            delay(1000L)
            resendCooldown -= 1
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = DarkSurfaceElevated,
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.35f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .testTag("google_phone_verification_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Icon Badge
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(if (step == 1) CyberCyan.copy(alpha = 0.12f) else ElectricEmerald.copy(alpha = 0.12f))
                        .border(
                            1.5.dp,
                            if (step == 1) CyberCyan.copy(alpha = 0.6f) else ElectricEmerald.copy(alpha = 0.6f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (step == 1) Icons.Default.VerifiedUser else Icons.Default.Sms,
                        contentDescription = null,
                        tint = if (step == 1) CyberCyan else ElectricEmerald,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (step == 1) title else "Verification Code",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 17.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (step == 1) {
                        description
                    } else {
                        "Enter 6-digit code sent to ${phoneInput.trim()}."
                    },
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                // STEP 1: ENTER PHONE NUMBER
                AnimatedVisibility(visible = step == 1) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = phoneInput,
                            onValueChange = { input ->
                                phoneInput = input.filter { it.isDigit() || it == '+' }
                                errorMessage = null
                            },
                            label = { Text("Phone Number") },
                            placeholder = { Text("080XXXXXXXX", color = TextMuted) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = CyberCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberCyan,
                                unfocusedBorderColor = DarkCardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("phone_verification_input")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = DarkSurface,
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, DarkCardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = ElectricEmerald,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Free verification • No SMS charges",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    }
                }

                // STEP 2: ENTER OTP CODE
                AnimatedVisibility(visible = step == 2) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        OtpChipsInput(
                            value = otpInput,
                            onValueChange = { input ->
                                val filtered = input.filter { it.isDigit() }.take(6)
                                otpInput = filtered
                                errorMessage = null
                            },
                            codeLength = 6,
                            isError = errorMessage != null,
                            onComplete = { completedCode ->
                                if (completedCode.length == 6 && !isVerifying) {
                                    isVerifying = true
                                    viewModel.verifyGooglePhoneCode(
                                        verificationId = phoneVerificationId,
                                        code = completedCode,
                                        phoneNumber = phoneInput.trim(),
                                        onSuccess = { verified ->
                                            isVerifying = false
                                            successMessage = "Verified."
                                            onSuccess(verified)
                                            onDismiss()
                                        },
                                        onError = { err ->
                                            isVerifying = false
                                            errorMessage = err
                                        }
                                    )
                                }
                            },
                            testTagPrefix = "dialog_phone_otp"
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Change Number & Resend Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Change Number",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = CyberCyan,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        step = 1
                                        otpInput = ""
                                        errorMessage = null
                                    }
                                    .padding(4.dp)
                            )

                            if (resendCooldown > 0) {
                                Text(
                                    text = "Resend in ${resendCooldown}s",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    ),
                                    modifier = Modifier.padding(4.dp)
                                )
                            } else {
                                Text(
                                    text = "Resend Code",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = GlowingAmber,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable {
                                            resendCooldown = 60
                                            errorMessage = null
                                            viewModel.sendGooglePhoneVerification(
                                                activity = activity,
                                                phoneNumber = phoneInput.trim(),
                                                onCodeSent = { _ ->
                                                    successMessage = "Code resent."
                                                },
                                                onAutoVerified = { verified ->
                                                    onSuccess(verified)
                                                    onDismiss()
                                                },
                                                onError = { err ->
                                                    errorMessage = err
                                                }
                                            )
                                        }
                                        .padding(4.dp)
                                )
                            }
                        }
                    }
                }

                // Error Message
                errorMessage?.let { err ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = WarningRed.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, WarningRed.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = err,
                            color = WarningRed,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                // Success Feedback Message
                successMessage?.let { msg ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ElectricEmerald.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, ElectricEmerald.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = msg,
                            color = ElectricEmerald,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Text("Cancel", color = TextMuted, fontSize = 12.sp)
                    }

                    if (step == 1) {
                        Button(
                            onClick = {
                                val trimmed = phoneInput.trim()
                                if (trimmed.length < 10) {
                                    errorMessage = "Enter valid 11-digit number."
                                } else {
                                    errorMessage = null
                                    viewModel.sendGooglePhoneVerification(
                                        activity = activity,
                                        phoneNumber = trimmed,
                                        onCodeSent = { _ ->
                                            step = 2
                                            resendCooldown = 60
                                            successMessage = "Code sent."
                                        },
                                        onAutoVerified = { verified ->
                                            onSuccess(verified)
                                            onDismiss()
                                        },
                                        onError = { err ->
                                            errorMessage = err
                                        }
                                    )
                                }
                            },
                            enabled = !isSendingOtp && phoneInput.trim().length >= 10,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberCyan,
                                contentColor = DarkObsidian
                            ),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(44.dp)
                                .testTag("send_google_code_btn")
                        ) {
                            if (isSendingOtp) {
                                FlowButtonLoadingLine(color = DarkObsidian)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sending...", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Send Code", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                if (otpInput.trim().length != 6) {
                                    errorMessage = "Enter 6-digit code."
                                } else {
                                    isVerifying = true
                                    viewModel.verifyGooglePhoneCode(
                                        verificationId = phoneVerificationId,
                                        code = otpInput.trim(),
                                        phoneNumber = phoneInput.trim(),
                                        onSuccess = { verified ->
                                            isVerifying = false
                                            onSuccess(verified)
                                            onDismiss()
                                        },
                                        onError = { err ->
                                            isVerifying = false
                                            errorMessage = err
                                        }
                                    )
                                }
                            },
                            enabled = !isVerifying && otpInput.trim().length == 6,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberCyan,
                                contentColor = DarkObsidian
                            ),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(44.dp)
                                .testTag("verify_google_code_btn")
                        ) {
                            if (isVerifying) {
                                FlowButtonLoadingLine(color = DarkObsidian)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Verifying...", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Verify", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
