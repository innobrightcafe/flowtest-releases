package com.example.ui.components.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ui.viewmodel.VpnViewModel
import com.example.ui.components.BookkeepingAnalyticsDashboard
import com.example.ui.components.FlowButtonLoadingLine
import com.example.ui.theme.*

@Composable
fun AdminFinancialsTab(
    viewModel: VpnViewModel,
    onFundWalletClick: () -> Unit,
    onEditSettlementClick: () -> Unit,
    onShowStatusMsg: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val walletBalance by viewModel.pairgateWalletBalance.collectAsStateWithLifecycle()
    val isFetchingBalance by viewModel.isFetchingPairgateBalance.collectAsStateWithLifecycle()
    val isAuthenticated by viewModel.isPairgateAuthenticated.collectAsStateWithLifecycle()
    val markupPercent by viewModel.vtuMarkupPercent.collectAsStateWithLifecycle()
    val accountInfo by viewModel.pairgateResellerAccountInfo.collectAsStateWithLifecycle()
    val statusMessage by viewModel.pairgateStatusMessage.collectAsStateWithLifecycle()
    val adminProfile by viewModel.adminProfile.collectAsStateWithLifecycle()
    val resellerAccount by viewModel.pairgateResellerAccount.collectAsStateWithLifecycle()
    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Reseller Wallet Balance Hero Card (WITH LIVE API PULL & SYNC)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .border(1.dp, if (isAuthenticated) CyberCyan.copy(alpha = 0.6f) else GlowingAmber, RoundedCornerShape(22.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(CyberCyan.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.AccountBalanceWallet, contentDescription = "Wallet", tint = CyberCyan)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "RESELLER LIVE BALANCE",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontWeight = FontWeight.Bold)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "₦${String.format("%,.2f", walletBalance)}",
                                    style = MaterialTheme.typography.headlineLarge.copy(
                                        fontWeight = FontWeight.Black,
                                        color = CyberCyan
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (isFetchingBalance) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    FlowButtonLoadingLine(color = CyberCyan, width = 32.dp, height = 2.dp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            color = CyberCyan.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(enabled = !isFetchingBalance) {
                                    viewModel.fetchPairgateResellerBalance()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.Sync, contentDescription = "Sync", tint = CyberCyan, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("PULL", style = MaterialTheme.typography.labelSmall.copy(color = CyberCyan, fontWeight = FontWeight.Black))
                            }
                        }

                        Button(
                            onClick = onFundWalletClick,
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Funds", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("FUND", fontWeight = FontWeight.Black, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = DarkCardBorder)
                Spacer(modifier = Modifier.height(12.dp))

                // Account Info & Status Line
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isAuthenticated) ElectricEmerald else GlowingAmber)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isAuthenticated) "API Status: CONNECTED & AUTHENTICATED" else "API Status: PENDING AUTH",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isAuthenticated) ElectricEmerald else GlowingAmber,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Text(
                        text = "Margin: +$markupPercent%",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontWeight = FontWeight.Bold)
                    )
                }

                accountInfo?.let { acc ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = acc,
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    )
                }

                statusMessage?.let { msg ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (msg.contains("Authenticated") || msg.contains("Synced")) ElectricEmerald else CyberCyan,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }

        // Real-Time Multi-Utility Bookkeeping Engine & Transaction Ledger
        BookkeepingAnalyticsDashboard(viewModel = viewModel)

        // 2. Pairgate Master Reseller Profile & Settlement Account
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.AdminPanelSettings, contentDescription = "Admin Profile", tint = CyberCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PAIRGATE MASTER RESELLER PROFILE",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        color = ElectricEmerald.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = adminProfile?.tierLevel ?: "Tier-1 Reseller",
                            color = ElectricEmerald,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Reseller Entity", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 10.sp))
                        Text(adminProfile?.resellerName ?: "Omodiale Aimiebe Innocent", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
                        Text(adminProfile?.businessName ?: "COT Digital Telecom & Reseller", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp))
                    }

                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        Text("Registered Contact", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 10.sp))
                        Text(adminProfile?.email ?: "innobright2010@gmail.com", style = MaterialTheme.typography.bodyMedium.copy(color = CyberCyan, fontWeight = FontWeight.Bold))
                        Text(adminProfile?.phone ?: "+234 816 829 0134", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = DarkCardBorder)
                Spacer(modifier = Modifier.height(10.dp))

                // Company Bank Account Box (For User Deposits & Settlements)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("OFFICIAL COMPANY BANK ACCOUNT (FOR USER DEPOSITS)", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold))
                                Text("${resellerAccount.bankName} • ${resellerAccount.bankAccountNumber}", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = CyberCyan))
                                Text(resellerAccount.bankAccountName, style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp))
                            }

                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(resellerAccount.bankAccountNumber))
                                    onShowStatusMsg("Copied Company Account (${resellerAccount.bankAccountNumber})")
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(imageVector = Icons.Outlined.ContentCopy, contentDescription = "Copy", tint = CyberCyan, modifier = Modifier.size(16.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = onEditSettlementClick,
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan.copy(alpha = 0.2f), contentColor = CyberCyan),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("UPDATE COMPANY BANK ACCOUNT DETAILS", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = {
                                viewModel.resetAllUserBalancesToZero {
                                    onShowStatusMsg("All user balances have been reset to ₦0.00 successfully.")
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = GlowingAmber),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GlowingAmber.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = "Reset Balances", modifier = Modifier.size(14.dp), tint = GlowingAmber)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("RESET ALL USER BALANCES TO ₦0.00", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = GlowingAmber)
                        }
                    }
                }
            }
        }
    }
}
