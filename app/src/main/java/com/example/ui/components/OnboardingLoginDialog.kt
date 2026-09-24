package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ui.viewmodel.VpnViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingLoginDialog(
    viewModel: VpnViewModel,
    onDismiss: () -> Unit
) {
    val userVirtualAccount by viewModel.userVirtualAccount.collectAsStateWithLifecycle()
    var isSignUpTab by remember { mutableStateOf(true) }

    var fullNameInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("••••••••") }

    var isLoading by remember { mutableStateOf(false) }
    var successMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(userVirtualAccount) {
        if (fullNameInput.isEmpty()) fullNameInput = userVirtualAccount.fullName
        if (emailInput.isEmpty()) emailInput = userVirtualAccount.email
        if (phoneInput.isEmpty()) phoneInput = userVirtualAccount.phoneNumber
    }

    ModalBottomSheet(
        onDismissRequest = { if (!isLoading) onDismiss() },
        containerColor = DarkSurfaceElevated,
        scrimColor = Color.Black.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isSignUpTab) "COT DIGITAL ONBOARDING" else "USER LOGIN",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = TextPrimary,
                            letterSpacing = 1.2.sp
                        )
                    )

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tab Switcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurface)
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSignUpTab) CyberCyan else Color.Transparent)
                            .clickable { isSignUpTab = true }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "SIGNUP & ACCOUNT",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isSignUpTab) DarkObsidian else TextSecondary
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (!isSignUpTab) CyberCyan else Color.Transparent)
                            .clickable { isSignUpTab = false }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "LOGIN",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (!isSignUpTab) DarkObsidian else TextSecondary
                            )
                        )
                    }
                }
            Spacer(modifier = Modifier.height(16.dp))

            if (successMsg != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CyberCyan.copy(alpha = 0.15f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = successMsg!!,
                        color = CyberCyan,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

                if (isSignUpTab) {
                    Text(
                        text = "Register to generate your instant 10-digit NUBAN account number for wallet funding & cheap data.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = fullNameInput,
                        onValueChange = { fullNameInput = it },
                        label = { Text("Full Name") },
                        placeholder = { Text("Enter your full name") },
                        singleLine = true,
                        leadingIcon = { Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = CyberCyan) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Email Address") },
                        placeholder = { Text("Enter email") },
                        singleLine = true,
                        leadingIcon = { Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = CyberCyan) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { phoneInput = it },
                        label = { Text("Phone Number (Verification & Default)") },
                        placeholder = { Text("Enter phone") },
                        supportingText = {
                            Text(
                                text = "💡 This will be your default number for purchases & bank transfer narration code.",
                                color = CyberCyan,
                                fontSize = 10.sp
                            )
                        },
                        singleLine = true,
                        leadingIcon = { Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = CyberCyan) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        text = "Login with your email or phone number to access your dedicated virtual account and wallet.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Email or Phone Number") },
                        placeholder = { Text("user@example.com / 080XXXXXXXX") },
                        singleLine = true,
                        leadingIcon = { Icon(imageVector = Icons.Default.AccountCircle, contentDescription = null, tint = CyberCyan) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("Password / PIN") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
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

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    isLoading = true
                    if (isSignUpTab) {
                        viewModel.registerUserAndGenerateVirtualAccount(fullNameInput, emailInput, phoneInput) { acc ->
                            isLoading = false
                            successMsg = "Account Activated! Dedicated NUBAN: ${acc.accountNumber} (${acc.bankName})"
                        }
                    } else {
                        viewModel.loginUser(emailInput, fullNameInput) { acc ->
                            isLoading = false
                            successMsg = "Welcome back, ${acc.fullName}! Wallet ready."
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    FlowButtonLoadingLine(color = DarkObsidian, width = 48.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("PROCESSING...", color = DarkObsidian, fontWeight = FontWeight.Bold)
                } else {
                    Text(
                        text = if (isSignUpTab) "ACTIVATE DEDICATED VIRTUAL ACCOUNT" else "LOGIN TO ACCOUNT",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black, color = DarkObsidian)
                    )
                }
            }
        }
    }
}
