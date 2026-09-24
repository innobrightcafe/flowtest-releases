package com.example.ui.components

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkObsidian
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ElectricEmerald
import com.example.ui.theme.GlowingAmber
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningRed
import com.example.data.ui.viewmodel.VpnViewModel
import kotlinx.coroutines.launch

private val POPULAR_NIGERIAN_BANKS = listOf(
    "Moniepoint MFB",
    "OPay",
    "PalmPay",
    "Kuda Bank",
    "GTBank (Guaranty Trust)",
    "Access Bank",
    "Zenith Bank",
    "First Bank of Nigeria",
    "United Bank for Africa (UBA)",
    "Fidelity Bank",
    "Stanbic IBTC Bank",
    "FCMB",
    "Sterling Bank",
    "Union Bank",
    "Wema Bank / ALAT"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FundingAccountReminderDrawer(
    viewModel: VpnViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val currentSavedBank by viewModel.savedFundingBankName.collectAsState()
    val currentSavedAccNum by viewModel.savedFundingAccountNumber.collectAsState()
    val currentSavedAccName by viewModel.savedFundingAccountName.collectAsState()
    val userVirtualAccount by viewModel.userVirtualAccount.collectAsState()

    var bankNameInput by remember(currentSavedBank) { mutableStateOf(currentSavedBank) }
    var accountNumberInput by remember(currentSavedAccNum) { mutableStateOf(currentSavedAccNum) }
    var accountNameInput by remember(currentSavedAccName, userVirtualAccount.fullName) {
        mutableStateOf(currentSavedAccName.ifBlank { userVirtualAccount.fullName })
    }

    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(42.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF334155))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header with glowing icon and close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(CyberCyan.copy(alpha = 0.25f), ElectricEmerald.copy(alpha = 0.2f))
                                )
                            )
                            .border(1.dp, CyberCyan.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = "Bank",
                            tint = CyberCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Automate Webhook Funding",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    fontSize = 17.sp
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(ElectricEmerald.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = null,
                                        tint = ElectricEmerald,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "INSTANT",
                                        color = ElectricEmerald,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }

                        Text(
                            text = "Link your sender bank for zero-delay auto credit",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Informational Card Explaining the Automation
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = DarkSurfaceElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = GlowingAmber,
                        modifier = Modifier
                            .size(22.dp)
                            .padding(top = 1.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Save the account number and name you transfer from. Whenever money arrives at our Moniepoint corporate account from your bank, our automated webhook matches your details and funds your wallet instantly—even if you forget to write remarks or click 'I have sent money'!",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Quick Bank Chips
            Text(
                text = "1. Select or Enter Sender Bank",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = CyberCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                POPULAR_NIGERIAN_BANKS.take(6).forEach { bank ->
                    val isSelected = bankNameInput.equals(bank, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = { bankNameInput = bank },
                        label = {
                            Text(
                                text = bank,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = DarkSurfaceElevated,
                            labelColor = TextMuted,
                            selectedContainerColor = CyberCyan.copy(alpha = 0.2f),
                            selectedLabelColor = CyberCyan
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (isSelected) CyberCyan else DarkCardBorder,
                            selectedBorderColor = CyberCyan,
                            borderWidth = 1.dp,
                            enabled = true,
                            selected = isSelected
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = bankNameInput,
                onValueChange = { bankNameInput = it },
                label = { Text("Bank Name (e.g., OPay, Moniepoint, GTBank)") },
                singleLine = true,
                leadingIcon = {
                    Icon(imageVector = Icons.Default.AccountBalance, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyberCyan,
                    unfocusedBorderColor = DarkCardBorder,
                    focusedLabelColor = CyberCyan,
                    unfocusedLabelColor = TextMuted,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Account Number Input with Paste Button
            Text(
                text = "2. Sender Account Number",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = CyberCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = accountNumberInput,
                onValueChange = { input ->
                    val digits = input.filter { it.isDigit() }
                    if (digits.length <= 11) {
                        accountNumberInput = digits
                    }
                },
                label = { Text("10-Digit Account Number") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Tag, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            val clipMgr = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            val clipText = clipMgr?.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                            val digits = clipText.filter { it.isDigit() }
                            if (digits.isNotBlank()) {
                                accountNumberInput = digits.take(11)
                            }
                        }
                    ) {
                        Icon(imageVector = Icons.Default.ContentPaste, contentDescription = "Paste", tint = CyberCyan, modifier = Modifier.size(18.dp))
                    }
                },
                supportingText = {
                    Text(
                        text = "${accountNumberInput.length}/10 digits (Standard NUBAN)",
                        color = if (accountNumberInput.length == 10) ElectricEmerald else TextMuted,
                        fontSize = 11.sp
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyberCyan,
                    unfocusedBorderColor = DarkCardBorder,
                    focusedLabelColor = CyberCyan,
                    unfocusedLabelColor = TextMuted,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Account Name Input
            Text(
                text = "3. Sender Account Name (As shown in your bank)",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = CyberCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = accountNameInput,
                onValueChange = { accountNameInput = it },
                label = { Text("Account Holder / Depositor Name") },
                singleLine = true,
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyberCyan,
                    unfocusedBorderColor = DarkCardBorder,
                    focusedLabelColor = CyberCyan,
                    unfocusedLabelColor = TextMuted,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Feedback Messages
            AnimatedVisibility(
                visible = errorMessage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                errorMessage?.let { err ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(WarningRed.copy(alpha = 0.15f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.ErrorOutline, contentDescription = null, tint = WarningRed, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = err, color = WarningRed, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            AnimatedVisibility(
                visible = successMessage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                successMessage?.let { msg ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(ElectricEmerald.copy(alpha = 0.15f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = ElectricEmerald, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = msg, color = ElectricEmerald, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Action Buttons
            Button(
                onClick = {
                    val cleanBank = bankNameInput.trim()
                    val cleanAcc = accountNumberInput.trim()
                    val cleanName = accountNameInput.trim()

                    if (cleanBank.isBlank()) {
                        errorMessage = "Please enter or select your sender bank name."
                        return@Button
                    }
                    if (cleanAcc.length < 10) {
                        errorMessage = "Please enter a valid 10-digit Nigerian account number."
                        return@Button
                    }
                    if (cleanName.isBlank()) {
                        errorMessage = "Please enter the account holder name on the bank account."
                        return@Button
                    }

                    errorMessage = null
                    isSubmitting = true
                    viewModel.saveFundingBankAccount(
                        bankName = cleanBank,
                        accountNumber = cleanAcc,
                        accountName = cleanName
                    ) { success, msg ->
                        isSubmitting = false
                        if (success) {
                            successMessage = "Funding account linked! Webhook automation is now 100% active."
                            coroutineScope.launch {
                                kotlinx.coroutines.delay(1200L)
                                onDismiss()
                            }
                        } else {
                            errorMessage = msg
                        }
                    }
                },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberCyan,
                    contentColor = DarkObsidian
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (isSubmitting) {
                    FlowButtonLoadingLine(color = DarkObsidian, width = 32.dp, height = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Saving & Activating...", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                } else {
                    Icon(imageVector = Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SAVE & ACTIVATE AUTO-CREDIT", fontWeight = FontWeight.Black, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            TextButton(
                onClick = {
                    viewModel.dismissFundingAccountReminder(remindLater = true)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Remind Me Later",
                    color = TextMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
