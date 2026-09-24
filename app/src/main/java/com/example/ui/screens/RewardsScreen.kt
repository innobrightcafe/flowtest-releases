package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.RedemptionActivity
import com.example.data.model.RedemptionCategory
import com.example.data.ui.viewmodel.VpnViewModel
import com.example.ui.theme.*
import com.example.util.ApkSharingHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RewardsScreen(
    viewModel: VpnViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    val walletBalance by viewModel.userWalletBalance.collectAsStateWithLifecycle()
    val userVirtualAccount by viewModel.userVirtualAccount.collectAsStateWithLifecycle()
    val cashbackBalance by viewModel.cashbackBalance.collectAsStateWithLifecycle()
    val cashbackRatePercent by viewModel.cashbackRatePercent.collectAsStateWithLifecycle()
    val referralEarnings by viewModel.referralEarnings.collectAsStateWithLifecycle()
    val referralCount by viewModel.referralCount.collectAsStateWithLifecycle()
    val activeReferralCount by viewModel.activeReferralCount.collectAsStateWithLifecycle()
    val inactiveReferralCount by viewModel.inactiveReferralCount.collectAsStateWithLifecycle()
    val pendingReferralEarnings by viewModel.pendingReferralEarnings.collectAsStateWithLifecycle()
    
    val vpnTimeRemainingMinutes by viewModel.vpnTimeRemainingMinutes.collectAsStateWithLifecycle()
    val rewardPoints by viewModel.rewardPoints.collectAsStateWithLifecycle()
    val pointsRedemptionRate by viewModel.pointsRedemptionRateNairaPer100Pts.collectAsStateWithLifecycle()
    val isProUser by viewModel.isProUser.collectAsStateWithLifecycle()
    val totalTimeAccumulatedMinutes by viewModel.totalTimeAccumulatedMinutes.collectAsStateWithLifecycle()
    val addTimeActivityLogs by viewModel.addTimeActivityLogs.collectAsStateWithLifecycle()

    val redemptionActivities by viewModel.redemptionActivities.collectAsStateWithLifecycle()
    val userCumulativeSpend by viewModel.userCumulativeSpend.collectAsStateWithLifecycle()
    val userCumulativeProfit by viewModel.userCumulativeProfit.collectAsStateWithLifecycle()
    val referralMinProfitThreshold by viewModel.referralMinProfitThreshold.collectAsStateWithLifecycle()
    val hasAwardedReferralBonus by viewModel.hasAwardedReferralBonus.collectAsStateWithLifecycle()
    val referralCommissionNaira by viewModel.referralCommissionNaira.collectAsStateWithLifecycle()
    val referralBonusPoints by viewModel.referralBonusPoints.collectAsStateWithLifecycle()

    var selectedCategoryFilter by remember { mutableStateOf<RedemptionCategory?>(null) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    val referralCode = "FLOW-" + userVirtualAccount.accountNumber.takeLast(5)
    val referralLink = "https://flowtest2026.com/ref/$referralCode"

    val filteredActivities = remember(redemptionActivities, selectedCategoryFilter) {
        if (selectedCategoryFilter == null) redemptionActivities
        else redemptionActivities.filter { it.category == selectedCategoryFilter }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "REWARDS & REDEMPTION",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = TextPrimary,
                            letterSpacing = 1.sp
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    // Top Share Icon opens Native System Share
                    IconButton(
                        onClick = {
                            ApkSharingHelper.launchNativeShare(
                                context = context,
                                coroutineScope = coroutineScope,
                                referralCode = referralCode
                            )
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = CyberCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkObsidian)
            )
        },
        containerColor = DarkObsidian,
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // 1. HERO REWARD POINTS & VPN TIME CARD
            item {
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
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(CyberCyan.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("⚡", fontSize = 22.sp)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "VPN RUNNING TIME",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = TextMuted,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                    )
                                    Text(
                                        text = viewModel.formatVpnRemainingTime(vpnTimeRemainingMinutes),
                                        style = MaterialTheme.typography.headlineSmall.copy(
                                            fontWeight = FontWeight.Black,
                                            color = ElectricEmerald
                                        )
                                    )
                                }
                            }

                            // Pro Badge
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isProUser) GlowingAmber.copy(alpha = 0.2f) else DarkSurface,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isProUser) GlowingAmber else DarkCardBorder
                                )
                            ) {
                                Text(
                                    text = if (isProUser) "PRO ACTIVE ⭐" else "FREE TIER",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isProUser) GlowingAmber else TextMuted
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Reward Points", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 10.sp))
                                Text("$rewardPoints Pts", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, color = GlowingAmber))
                            }

                            Box(modifier = Modifier.height(26.dp).width(1.dp).background(Color.White.copy(alpha = 0.1f)))

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Total Purchases", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 10.sp))
                                Text("₦${String.format(java.util.Locale.US, "%,.0f", userCumulativeSpend)}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
                            }

                            Box(modifier = Modifier.height(26.dp).width(1.dp).background(Color.White.copy(alpha = 0.1f)))

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Cashback Bal", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 10.sp))
                                Text("₦${String.format(java.util.Locale.US, "%,.2f", cashbackBalance)}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = CyberCyan))
                            }
                        }
                    }
                }
            }

            // 2. REDEMPTIONS HEADER & CATEGORY FILTER
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Redeem, contentDescription = null, tint = GlowingAmber, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "REDEMPTION STORE",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    color = TextPrimary,
                                    letterSpacing = 0.5.sp
                                )
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = GlowingAmber.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "Balance: $rewardPoints Pts",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = GlowingAmber,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Text(
                        text = "Points earned from buying airtime, data and electricity. Redeem for VPN time, cash, or network credits.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    )

                    // Filter chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChip(
                                selected = selectedCategoryFilter == null,
                                onClick = { selectedCategoryFilter = null },
                                label = { Text("All (${redemptionActivities.size})", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyberCyan,
                                    selectedLabelColor = DarkObsidian,
                                    containerColor = DarkSurface,
                                    labelColor = TextMuted
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedCategoryFilter == RedemptionCategory.VPN_TIME,
                                onClick = { selectedCategoryFilter = RedemptionCategory.VPN_TIME },
                                label = { Text("VPN Time", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyberCyan,
                                    selectedLabelColor = DarkObsidian,
                                    containerColor = DarkSurface,
                                    labelColor = TextMuted
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedCategoryFilter == RedemptionCategory.CASH,
                                onClick = { selectedCategoryFilter = RedemptionCategory.CASH },
                                label = { Text("Cash", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GlowingAmber,
                                    selectedLabelColor = DarkObsidian,
                                    containerColor = DarkSurface,
                                    labelColor = TextMuted
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedCategoryFilter == RedemptionCategory.NETWORK_CREDIT,
                                onClick = { selectedCategoryFilter = RedemptionCategory.NETWORK_CREDIT },
                                label = { Text("Network Credit", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricEmerald,
                                    selectedLabelColor = DarkObsidian,
                                    containerColor = DarkSurface,
                                    labelColor = TextMuted
                                )
                            )
                        }
                    }
                }
            }

            // 3. REDEMPTION CARDS (ARRANGED 3 PER ROW)
            items(filteredActivities.chunked(3)) { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (activity in rowItems) {
                        Box(modifier = Modifier.weight(1f)) {
                            RedemptionItemCard(
                                activity = activity,
                                userPoints = rewardPoints,
                                onRedeem = {
                                    viewModel.executeRedemption(
                                        activity = activity,
                                        onSuccess = { msg -> snackbarMessage = msg },
                                        onError = { err -> snackbarMessage = err }
                                    )
                                }
                            )
                        }
                    }
                    // Fill row spacing if fewer than 3 items
                    repeat(3 - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            // 4. CASHBACK REWARDS & TRANSFER CARD
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(GlowingAmber.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("💰", fontSize = 17.sp)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "CASHBACK WALLET",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Black,
                                            color = TextPrimary
                                        )
                                    )
                                    Text(
                                        text = "${String.format(java.util.Locale.US, "%.1f", cashbackRatePercent)}% earned on purchases",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = GlowingAmber,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = ElectricEmerald.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "ACTIVE",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = ElectricEmerald,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = DarkSurface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "AVAILABLE",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = TextMuted,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Text(
                                        text = "₦${String.format(java.util.Locale.US, "%,.2f", cashbackBalance)}",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Black,
                                            color = ElectricEmerald
                                        )
                                    )
                                }

                                Button(
                                    onClick = {
                                        viewModel.transferCashbackToMainWallet(
                                            onSuccess = { amt ->
                                                snackbarMessage = "🎉 Successfully transferred ₦${String.format(java.util.Locale.US, "%,.2f", amt)} cashback to main wallet!"
                                            },
                                            onError = { err -> snackbarMessage = err }
                                        )
                                    },
                                    enabled = cashbackBalance > 0.0,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CyberCyan,
                                        contentColor = DarkObsidian
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text(
                                        text = "TRANSFER TO MAIN",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. REFERRAL PROGRAM & PROFIT PROTECTED REWARDS
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "REFERRAL & COMMISSIONS",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    color = TextPrimary
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Clear Explanation of Profit-Protected Referral Policy
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = DarkSurface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "🛡️ PROFIT-FIRST REFERRAL REWARDS (ZERO OUT-OF-POCKET)",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = CyberCyan,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 10.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "All points and referral earnings are funded strictly from net platform profits. Referrers receive ₦${String.format(java.util.Locale.US, "%,.0f", referralCommissionNaira)} cash + $referralBonusPoints Pts once their invited friend generates at least ₦${String.format(java.util.Locale.US, "%,.0f", referralMinProfitThreshold)} in cumulative profit for the admin from buying airtime, data, or bills.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                val progress = (userCumulativeProfit / referralMinProfitThreshold).coerceIn(0.0, 1.0).toFloat()
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = if (progress >= 1f) ElectricEmerald else CyberCyan,
                                    trackColor = DarkObsidian
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Generated Profit: ₦${String.format(java.util.Locale.US, "%,.2f", userCumulativeProfit)} (Spend: ₦${String.format(java.util.Locale.US, "%,.0f", userCumulativeSpend)})",
                                        style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 9.sp)
                                    )
                                    Text(
                                        text = if (userCumulativeProfit >= referralMinProfitThreshold) "Profit Target Met (Paid) ✅" else "Goal: ₦${String.format(java.util.Locale.US, "%,.0f", referralMinProfitThreshold)} Profit",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (userCumulativeProfit >= referralMinProfitThreshold) ElectricEmerald else GlowingAmber,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Live Referral Statistics Box with Active vs Inactive Breakdown
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = DarkSurface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("AVAILABLE REFERRAL EARNINGS", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold))
                                        Text("₦${String.format(java.util.Locale.US, "%,.2f", referralEarnings)}", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, color = ElectricEmerald))
                                        Text(
                                            text = if (pendingReferralEarnings > 0.0) "₦${String.format(java.util.Locale.US, "%,.2f", pendingReferralEarnings)} Pending" else "No Pending Earnings",
                                            style = MaterialTheme.typography.labelSmall.copy(color = GlowingAmber, fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold)
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            viewModel.transferReferralEarningsToMainWallet(
                                                onSuccess = { amt ->
                                                    snackbarMessage = "🎉 Successfully transferred ₦${String.format(java.util.Locale.US, "%,.2f", amt)} referral bonus to main wallet!"
                                                },
                                                onError = { err -> snackbarMessage = err }
                                            )
                                        },
                                        enabled = referralEarnings > 0.0,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = CyberCyan,
                                            contentColor = DarkObsidian
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Text("TRANSFER TO MAIN", fontWeight = FontWeight.Black, fontSize = 10.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = DarkCardBorder)
                                Spacer(modifier = Modifier.height(10.dp))

                                // Active vs Inactive Breakdown Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Active referrals card (Goal met -> Earned & withdrawable)
                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        color = ElectricEmerald.copy(alpha = 0.08f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, ElectricEmerald.copy(alpha = 0.3f))
                                    ) {
                                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .clip(CircleShape)
                                                        .background(ElectricEmerald)
                                                )
                                                Spacer(modifier = Modifier.width(5.dp))
                                                Text(
                                                    text = "$activeReferralCount Active",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = ElectricEmerald,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp
                                                    )
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "₦${String.format(java.util.Locale.US, "%,.2f", referralEarnings)} earned",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = TextPrimary,
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 12.sp
                                                )
                                            )
                                            Text(
                                                text = "Ready to withdraw",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = TextMuted,
                                                    fontSize = 8.5.sp
                                                )
                                            )
                                        }
                                    }

                                    // Inactive referrals card (Pending -> Mr B hasn't met ₦50 profit goal yet)
                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        color = GlowingAmber.copy(alpha = 0.08f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, GlowingAmber.copy(alpha = 0.3f))
                                    ) {
                                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .clip(CircleShape)
                                                        .background(GlowingAmber)
                                                )
                                                Spacer(modifier = Modifier.width(5.dp))
                                                Text(
                                                    text = "$inactiveReferralCount Inactive",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = GlowingAmber,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp
                                                    )
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "₦${String.format(java.util.Locale.US, "%,.2f", pendingReferralEarnings)} pending",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = TextPrimary,
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 12.sp
                                                )
                                            )
                                            Text(
                                                text = "Unlocks at ₦${String.format(java.util.Locale.US, "%,.0f", referralMinProfitThreshold)} profit",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = TextMuted,
                                                    fontSize = 8.5.sp
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Referral Code Box
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(DarkSurface)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("REFERRAL CODE", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold))
                                Text(
                                    text = referralCode,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        color = CyberCyan,
                                        letterSpacing = 1.sp
                                    )
                                )
                            }

                            Surface(
                                color = CyberCyan.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        clipboardManager.setText(AnnotatedString(referralCode))
                                        snackbarMessage = "Referral Code Copied!"
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy", style = MaterialTheme.typography.labelSmall.copy(color = CyberCyan, fontWeight = FontWeight.Bold, fontSize = 10.sp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Native Share Button directly opening OS share sheet
                        Button(
                            onClick = {
                                ApkSharingHelper.launchNativeShare(
                                    context = context,
                                    coroutineScope = coroutineScope,
                                    referralCode = referralCode
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberCyan,
                                contentColor = DarkObsidian
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SHARE APP & REFER",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                )
                            )
                        }
                    }
                }
            }

            // 6. ACTIVITY HISTORY HEADER & LOGS
            if (addTimeActivityLogs.isNotEmpty()) {
                item {
                    Text(
                        text = "RECENT ACTIVITY & REDEMPTIONS",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            letterSpacing = 1.sp
                        )
                    )
                }

                items(addTimeActivityLogs.take(15)) { log ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(ElectricEmerald.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("⚡", fontSize = 14.sp)
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Text(
                                text = log,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    fontSize = 11.5.sp
                                )
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }

        snackbarMessage?.let { msg ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Snackbar(
                    containerColor = CyberCyan,
                    contentColor = DarkObsidian,
                    action = {
                        TextButton(onClick = { snackbarMessage = null }) {
                            Text("OK", color = DarkObsidian, fontWeight = FontWeight.Bold)
                        }
                    }
                ) {
                    Text(msg, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Modern 3-per-row card for each redemption activity.
 * When inactive, fades with alpha = 0.38f and shows 'PAUSED'.
 * Has a small redeem button at the bottom.
 */
@Composable
private fun RedemptionItemCard(
    activity: RedemptionActivity,
    userPoints: Int,
    onRedeem: () -> Unit
) {
    val isAffordable = userPoints >= activity.pointsCost
    val isActive = activity.isActive

    // Category styling
    val (accentColor, iconText) = when {
        activity.network.contains("MTN", ignoreCase = true) -> Color(0xFFFFCC00) to "MTN"
        activity.network.contains("AIRTEL", ignoreCase = true) -> Color(0xFFFF3333) to "AIR"
        activity.network.contains("GLO", ignoreCase = true) -> Color(0xFF22AA33) to "GLO"
        activity.network.contains("9MOBILE", ignoreCase = true) -> Color(0xFF006633) to "9M"
        activity.category == RedemptionCategory.VPN_TIME -> CyberCyan to "⏱️"
        activity.category == RedemptionCategory.CASH -> GlowingAmber to "₦"
        activity.category == RedemptionCategory.PRO_UPGRADE -> GlowingAmber to "⭐"
        else -> ElectricEmerald to "🎁"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isActive) 1f else 0.38f),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isActive && isAffordable) accentColor.copy(alpha = 0.5f) else DarkCardBorder
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Category Icon Badge
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = iconText,
                    fontSize = if (iconText.length > 2) 9.sp else 12.sp,
                    fontWeight = FontWeight.Black,
                    color = accentColor
                )
            }

            // Title
            Text(
                text = activity.title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = TextPrimary,
                    fontSize = 11.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            // Subtitle
            Text(
                text = activity.subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextMuted,
                    fontSize = 9.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            // Cost Pill
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = GlowingAmber.copy(alpha = 0.15f)
            ) {
                Text(
                    text = "${activity.pointsCost} Pts",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = GlowingAmber,
                        fontWeight = FontWeight.Black,
                        fontSize = 9.sp
                    ),
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Small Redeem Button
            Button(
                onClick = onRedeem,
                enabled = isActive && isAffordable,
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = DarkObsidian,
                    disabledContainerColor = DarkSurface,
                    disabledContentColor = TextMuted
                ),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp)
            ) {
                Text(
                    text = when {
                        !isActive -> "PAUSED"
                        isAffordable -> "REDEEM"
                        else -> "NEED PTS"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 8.5.sp
                    )
                )
            }
        }
    }
}
