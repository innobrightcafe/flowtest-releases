package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.api.RecentDataPurchase
import com.example.data.ui.viewmodel.VpnViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectDataRepurchaseBottomSheet(
    purchase: RecentDataPurchase,
    viewModel: VpnViewModel,
    onDismiss: () -> Unit,
    onSuccess: (PurchaseSuccessReceipt) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val walletBalance by viewModel.userWalletBalance.collectAsStateWithLifecycle()
    var recipientPhone by remember { mutableStateOf(purchase.recipientPhone) }
    var isProcessing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showPhoneVerificationDialog by remember { mutableStateOf(false) }

    val hasSufficientBalance = walletBalance >= purchase.amountNaira

    ModalBottomSheet(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        sheetState = sheetState,
        containerColor = DarkObsidian,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(TextMuted.copy(alpha = 0.4f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(CyberCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Repeat,
                            contentDescription = "Buy Again",
                            tint = CyberCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Buy Again",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = TextPrimary,
                                fontSize = 18.sp
                            )
                        )
                        Text(
                            text = "1-Tap Direct Purchase",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = CyberCyan,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        )
                    }
                }

                IconButton(
                    onClick = { if (!isProcessing) onDismiss() },
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

            // Order Summary Card (styled cleanly like Image 1)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = DarkSurfaceElevated,
                border = BorderStroke(1.dp, DarkCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PLAN",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextMuted,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = CyberCyan.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = purchase.network.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = CyberCyan,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Text(
                        text = "${purchase.network} Data • ${purchase.planName}",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 15.sp
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Validity Period:",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 12.sp)
                        )
                        Text(
                            text = purchase.validity.ifBlank { "30 Days" },
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = ElectricEmerald,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        )
                    }

                    HorizontalDivider(color = DarkCardBorder, thickness = 1.dp)

                    // Recipient Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recipient Phone:",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 12.sp)
                        )
                        Text(
                            text = recipientPhone,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        )
                    }

                    // Wallet Balance Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Wallet Balance:",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 12.sp)
                        )
                        Text(
                            text = "₦${String.format(Locale.US, "%,.2f", walletBalance)}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = if (hasSufficientBalance) ElectricEmerald else GlowingAmber,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        )
                    }

                    HorizontalDivider(color = DarkCardBorder, thickness = 1.dp)

                    // Price Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Amount to Pay:",
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "₦${String.format(Locale.US, "%,.2f", purchase.amountNaira)}",
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = CyberCyan,
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp
                            )
                        )
                    }
                }
            }

            // Error notice if any
            if (errorMessage != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF3B1B1F),
                    border = BorderStroke(1.dp, Color(0xFFF85149)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFFFF8B8B),
                            fontSize = 12.sp
                        ),
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Confirm & Pay Button
            Button(
                onClick = {
                    if (!viewModel.isUserPhoneVerified()) {
                        showPhoneVerificationDialog = true
                        return@Button
                    }
                    if (!com.example.data.util.NetworkUtils.isNetworkAvailable(context)) {
                        Toast.makeText(context, "No network connection. Please check your internet.", Toast.LENGTH_LONG).show()
                        errorMessage = "No network connection. Please check your connection."
                        return@Button
                    }
                    if (!hasSufficientBalance) {
                        Toast.makeText(context, "Insufficient wallet balance. Please fund your wallet first.", Toast.LENGTH_LONG).show()
                        errorMessage = "Insufficient wallet balance (₦${String.format(Locale.US, "%,.2f", walletBalance)}). Needed: ₦${String.format(Locale.US, "%,.2f", purchase.amountNaira)}"
                        return@Button
                    }

                    isProcessing = true
                    errorMessage = null

                    coroutineScope.launch {
                        viewModel.purchasePairgateData(
                            network = purchase.network,
                            planId = purchase.planId,
                            amountNaira = purchase.amountNaira,
                            phone = recipientPhone.trim(),
                            category = purchase.category,
                            planName = purchase.planName,
                            onResult = { success, msg, ref, isPending ->
                                isProcessing = false
                                if (success) {
                                    viewModel.saveUserOption("last_data_network", purchase.network)
                                    viewModel.saveRecipient(
                                        name = "${purchase.network} Data - ${recipientPhone.trim()}",
                                        recipientType = "data",
                                        identifier = recipientPhone.trim(),
                                        institutionOrProvider = purchase.network,
                                        isFavorite = true
                                    )
                                    val receipt = PurchaseSuccessReceipt(
                                        serviceTitle = "${purchase.network} Data (Buy Again)",
                                        providerOrType = purchase.network,
                                        recipient = recipientPhone.trim(),
                                        amountPaid = purchase.amountNaira,
                                        reference = ref ?: ("TRX-" + System.currentTimeMillis()),
                                        newBalance = (walletBalance - purchase.amountNaira).coerceAtLeast(0.0),
                                        message = msg,
                                        bonusVpnTime = "+2 Hours Unlimited VPN Time",
                                        bonusPoints = 25,
                                        timestamp = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()).format(Date()),
                                        balanceBefore = walletBalance,
                                        isPending = isPending,
                                        status = if (isPending) "PENDING" else "SUCCESS"
                                    )
                                    onSuccess(receipt)
                                    onDismiss()
                                } else {
                                    errorMessage = msg
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                }
                            }
                        )
                    }
                },
                enabled = !isProcessing,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = getNetworkColor(purchase.network),
                    contentColor = getNetworkContentColor(purchase.network)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("confirm_buy_again_btn")
            ) {
                val netContentColor = getNetworkContentColor(purchase.network)
                if (isProcessing) {
                    FlowButtonLoadingLine(color = netContentColor, width = 48.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Processing Repurchase...",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, color = netContentColor)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = netContentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Confirm & Pay ₦${purchase.amountNaira.toInt()}",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Black,
                            color = netContentColor,
                            fontSize = 15.sp
                        )
                    )
                }
            }

            // Cancel Button
            TextButton(
                onClick = { if (!isProcessing) onDismiss() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Cancel",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                )
            }
        }
    }

    if (showPhoneVerificationDialog) {
        PhoneVerificationDialog(
            viewModel = viewModel,
            initialPhone = recipientPhone.trim(),
            title = "Free Verification",
            description = "Free verification to verify recipient number.",
            onDismiss = { showPhoneVerificationDialog = false },
            onSuccess = {
                showPhoneVerificationDialog = false
            }
        )
    }
}
