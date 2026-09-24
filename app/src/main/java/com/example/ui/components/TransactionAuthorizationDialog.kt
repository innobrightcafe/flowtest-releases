package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ui.viewmodel.VpnViewModel
import com.example.ui.theme.*
import com.example.util.BiometricAuthHelper
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionAuthorizationDialog(
    serviceTitle: String,
    recipient: String,
    amountNaira: Double,
    viewModel: VpnViewModel,
    onAuthorized: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { BiometricAuthHelper.findFragmentActivity(context) }
    val isBiometricEnabled by viewModel.isTransactionBiometricEnabled.collectAsStateWithLifecycle()
    val isPinRequired by viewModel.isTransactionPinRequired.collectAsStateWithLifecycle()

    var showPinPad by remember { mutableStateOf(!isBiometricEnabled || !BiometricAuthHelper.canAuthenticate(context)) }
    var enteredPin by remember { mutableStateOf("") }
    var authError by remember { mutableStateOf<String?>(null) }

    // Launch biometric prompt
    val launchBiometricAuth: () -> Unit = {
        authError = null
        if (activity != null && BiometricAuthHelper.canAuthenticate(context)) {
            BiometricAuthHelper.promptBiometric(
                activity = activity,
                title = "Authorize Transaction",
                subtitle = "Pay ₦${String.format(Locale.US, "%,.2f", amountNaira)} for $recipient",
                negativeButtonText = "Use PIN",
                onSuccess = {
                    onAuthorized()
                },
                onError = { err ->
                    if (err == "cancelled") {
                        showPinPad = true
                    } else {
                        authError = err
                        showPinPad = true
                    }
                }
            )
        } else {
            showPinPad = true
        }
    }

    // Default to Biometric (Thumbprint / Face) if active
    LaunchedEffect(Unit) {
        if (isBiometricEnabled && BiometricAuthHelper.canAuthenticate(context)) {
            launchBiometricAuth()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkObsidian,
        scrimColor = Color.Black.copy(alpha = 0.78f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(42.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(CyberCyan.copy(alpha = 0.6f))
            )
        },
        modifier = Modifier.testTag("transaction_authorization_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Security Shield
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(CyberCyan.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Security Paywall",
                        tint = CyberCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "AUTHORIZE TRANSACTION",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 0.5.sp
                        )
                    )
                    Text(
                        text = "Zero-Trust Payment Gate",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Order Summary Card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = DarkSurfaceElevated,
                border = BorderStroke(1.dp, DarkCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Service Item",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 12.sp)
                        )
                        Text(
                            text = serviceTitle,
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        )
                    }

                    if (recipient.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Recipient / Target",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 12.sp)
                            )
                            Text(
                                text = recipient,
                                style = MaterialTheme.typography.bodySmall.copy(color = CyberCyan, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = DarkCardBorder)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total Payable",
                            style = MaterialTheme.typography.titleSmall.copy(color = TextSecondary, fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "₦${String.format(Locale.US, "%,.2f", amountNaira)}",
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = ElectricEmerald,
                                fontWeight = FontWeight.Black
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // BIOMETRIC OR PIN KEYPAD
            if (!showPinPad && isBiometricEnabled && BiometricAuthHelper.canAuthenticate(context)) {
                // Biometric Choice Card
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = DarkSurfaceElevated,
                    border = BorderStroke(1.dp, DarkCardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { launchBiometricAuth() }
                        .testTag("paywall_biometric_trigger_btn")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(CyberCyan.copy(alpha = 0.15f))
                                .border(1.5.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(22.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = "Biometric thumbprint or face",
                                tint = CyberCyan,
                                modifier = Modifier.size(42.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Touch Fingerprint Sensor / Face",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "Hardware-backed biometric authorization",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Switch to PIN button
                TextButton(
                    onClick = { showPinPad = true },
                    modifier = Modifier.testTag("paywall_switch_to_pin_btn")
                ) {
                    Text("Or Authorize with PIN Instead", color = CyberCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            } else {
                // PIN Keypad View
                Text(
                    text = "Enter 4-Digit Transaction PIN",
                    style = MaterialTheme.typography.titleSmall.copy(color = Color.White, fontWeight = FontWeight.Bold)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // PIN Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until 4) {
                        val isFilled = i < enteredPin.length
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(if (isFilled) CyberCyan else Color.Transparent)
                                .border(
                                    1.5.dp,
                                    if (isFilled) CyberCyan else Color(0xFF334155),
                                    CircleShape
                                )
                        )
                    }
                }

                if (authError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = authError!!,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = WarningRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Compact Numeric Keypad
                val keypadRows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("BIO", "0", "DEL")
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    keypadRows.forEach { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            row.forEach { item ->
                                when (item) {
                                    "BIO" -> {
                                        if (isBiometricEnabled && BiometricAuthHelper.canAuthenticate(context)) {
                                            IconButton(
                                                onClick = {
                                                    showPinPad = false
                                                    launchBiometricAuth()
                                                },
                                                modifier = Modifier
                                                    .size(54.dp)
                                                    .clip(CircleShape)
                                                    .background(DarkSurfaceElevated)
                                                    .border(1.dp, DarkCardBorder, CircleShape)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Fingerprint,
                                                    contentDescription = "Switch to Biometrics",
                                                    tint = CyberCyan,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        } else {
                                            Spacer(modifier = Modifier.size(54.dp))
                                        }
                                    }
                                    "DEL" -> {
                                        IconButton(
                                            onClick = {
                                                if (enteredPin.isNotEmpty()) {
                                                    enteredPin = enteredPin.dropLast(1)
                                                    authError = null
                                                }
                                            },
                                            modifier = Modifier
                                                .size(54.dp)
                                                .clip(CircleShape)
                                                .background(DarkSurfaceElevated)
                                                .border(1.dp, DarkCardBorder, CircleShape)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                                contentDescription = "Backspace",
                                                tint = TextSecondary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    else -> {
                                        Surface(
                                            shape = CircleShape,
                                            color = DarkSurfaceElevated,
                                            border = BorderStroke(1.dp, DarkCardBorder),
                                            modifier = Modifier
                                                .size(54.dp)
                                                .clip(CircleShape)
                                                .clickable {
                                                    if (enteredPin.length < 4) {
                                                        val next = enteredPin + item
                                                        enteredPin = next
                                                        authError = null
                                                        if (next.length == 4) {
                                                            if (viewModel.verifyPin(next)) {
                                                                onAuthorized()
                                                            } else {
                                                                authError = "Incorrect Transaction PIN."
                                                                enteredPin = ""
                                                            }
                                                        }
                                                    }
                                                }
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier.fillMaxSize()
                                            ) {
                                                Text(
                                                    text = item,
                                                    style = MaterialTheme.typography.titleMedium.copy(
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 18.sp,
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

            Spacer(modifier = Modifier.height(14.dp))

            // Cancel Button
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, DarkCardBorder),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
            ) {
                Text("Cancel Transaction", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
