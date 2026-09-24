package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import android.widget.Toast
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.api.PairgateCatalogHelper
import com.example.data.api.DataBundleDisplayPlan
import com.example.data.api.PairgateDataPlanItem
import com.example.data.api.RecentDataPurchase
import com.example.data.db.SavedRecipientEntity
import com.example.data.repository.MultiUtilityPricingEngine
import com.example.data.ui.viewmodel.VpnViewModel
import com.example.ui.theme.*
import com.example.util.PdfReceiptGenerator
import com.example.util.TransactionReceiptData

data class ServiceItem(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val description: String = ""
)

val HOME_SERVICES = listOf(
    ServiceItem("airtime", "Airtime", Icons.Default.PhoneInTalk, "Instant VTU recharge (5% off)"),
    ServiceItem("data", "Data", Icons.Default.Smartphone, "SME, Direct & Corporate bundles (3% off)"),
    ServiceItem("cable", "Cable TV", Icons.Default.Tv, "DSTV, GOTV & Startimes recharge"),
    ServiceItem("more", "More", Icons.Default.MoreHoriz, "View all VTU services")
)

val DEFAULT_SERVICES = listOf(
    ServiceItem("airtime", "Airtime", Icons.Default.PhoneInTalk, "Instant VTU recharge (5% off)"),
    ServiceItem("data", "Data", Icons.Default.Smartphone, "SME, Direct & Corporate bundles (15% off)"),
    ServiceItem("cable", "Cable TV", Icons.Default.Tv, "DSTV, GOTV & Startimes recharge"),
    ServiceItem("combo", "Combo Packs", Icons.Default.AutoAwesome, "SME Data + Airtime + VIP VPN Bundles"),
    ServiceItem("utility", "Electricity", Icons.Default.ElectricMeter, "Prepaid & Postpaid Meter Tokens"),
    ServiceItem("recharge", "Recharge PIN", Icons.Default.ConfirmationNumber, "Print Airtime PINs (4% off)"),
    ServiceItem("betting", "Bet Funding", Icons.Default.SportsSoccer, "SportyBet, 1xBet, Bet9ja top-up"),
    ServiceItem("education", "Exams", Icons.Default.School, "WAEC, NECO & JAMB E-PINs")
)

/**
 * Top Wallet Header Card matching screenshot layout:
 * - Account Number | Account Name [Copy Icon]
 * - ₦ Balance [Eye Toggle]
 * - Last updated status
 * - [+ Add Money] [History] buttons
 */
@Composable
fun WalletAccountHeaderCard(
    userVirtualAccount: VpnViewModel.UserVirtualAccount,
    walletBalance: Double,
    onAddMoneyClick: () -> Unit,
    onHistoryClick: () -> Unit,
    modifier: Modifier = Modifier,
    depositSessionExpiresAt: Long = 0L,
    activeConfirmationCode: String = "",
    isDepositReportedPendingAdmin: Boolean = false,
    reportedDepositReference: String = "",
    onDismissReportedStatus: () -> Unit = {},
    onCancelSession: () -> Unit = {},
    lastTransaction: VpnViewModel.VtuTransactionLog? = null
) {
    var isBalanceVisible by remember { mutableStateOf(true) }
    var copySuccessMsg by remember { mutableStateOf<String?>(null) }
    val clipboardManager = LocalClipboardManager.current

    // Live countdown timer computation
    var remainingMillis by remember(depositSessionExpiresAt) {
        mutableStateOf(
            if (depositSessionExpiresAt > 0L) (depositSessionExpiresAt - System.currentTimeMillis()).coerceAtLeast(0L)
            else 0L
        )
    }

    LaunchedEffect(depositSessionExpiresAt) {
        while (depositSessionExpiresAt > 0L) {
            val diff = (depositSessionExpiresAt - System.currentTimeMillis()).coerceAtLeast(0L)
            remainingMillis = diff
            if (diff <= 0L) break
            kotlinx.coroutines.delay(1000L)
        }
    }

    val hasOngoingCountdown = remainingMillis > 0L
    val minutes = (remainingMillis / 1000) / 60
    val seconds = (remainingMillis / 1000) % 60
    val timerText = String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)

    val glassBorderBrush = if (hasOngoingCountdown) {
        Brush.linearGradient(
            colors = listOf(
                GlowingAmber.copy(alpha = 0.92f),
                Color.White.copy(alpha = 0.70f),
                GlowingAmber.copy(alpha = 0.40f),
                GlowingAmber.copy(alpha = 0.85f)
            ),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.85f),       // Specular rim top-left
                CyberCyan.copy(alpha = 0.60f),        // Refractive cyan gleam
                Color.White.copy(alpha = 0.18f),       // Translucent edge
                Color.White.copy(alpha = 0.50f)        // Bottom bounce rim
            ),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(
                width = 1.2.dp,
                brush = glassBorderBrush,
                shape = RoundedCornerShape(24.dp)
            )
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0x38FFFFFF), // High translucency frosted top gleam
                        Color(0x1810233D), // Translucent sapphire glass tint
                        Color(0x201E3A5F), // Deep optical depth
                        Color(0x28FFFFFF)  // Specular bottom refraction
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            )
            .drawBehind {
                // Ambient glass caustics
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            CyberCyan.copy(alpha = 0.32f),
                            CyberCyan.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.15f, size.height * 0.10f),
                        radius = size.width * 0.65f
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF6366F1).copy(alpha = 0.20f),
                            Color(0xFF38BDF8).copy(alpha = 0.06f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.85f, size.height * 0.90f),
                        radius = size.width * 0.55f
                    )
                )
                // Ultra-thin top specular highlight hairline (physical glass edge glare)
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.90f),
                            Color.White.copy(alpha = 0.30f),
                            Color.Transparent
                        )
                    ),
                    start = Offset(size.width * 0.08f, 1f),
                    end = Offset(size.width * 0.92f, 1f),
                    strokeWidth = 1.5f
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Top Row: Pure Glass Badges (Wallet Badge + Account Number & Corporate Name)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.12f),
                        border = BorderStroke(
                            0.8.dp,
                            Brush.horizontalGradient(
                                listOf(Color.White.copy(alpha = 0.65f), CyberCyan.copy(alpha = 0.75f))
                            )
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            FlowtestEmblem(
                                size = 14.dp,
                                primaryColor = CyberCyan,
                                glowAlpha = 0f
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "FLOWTEST WALLET",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 9.sp,
                                    letterSpacing = 0.8.sp
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    val accNum = userVirtualAccount.accountNumber.ifBlank { MultiUtilityPricingEngine.CORPORATE_ACCOUNT_NUMBER }
                    val accName = MultiUtilityPricingEngine.CORPORATE_ACCOUNT_NAME
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.28f),
                        border = BorderStroke(0.7.dp, Color.White.copy(alpha = 0.20f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "$accNum | $accName",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White.copy(alpha = 0.95f),
                                    fontSize = 11.5.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(accNum))
                                    copySuccessMsg = "Copied $accNum!"
                                },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy Account Number",
                                    tint = CyberCyan,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }
            }

            copySuccessMsg?.let { msg ->
                Text(
                    text = msg,
                    style = MaterialTheme.typography.labelSmall.copy(color = CyberCyan, fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Middle: Balance Header Label + Amount with Frosted Glass Eye Button
            Text(
                text = "TOTAL AVAILABLE BALANCE",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color.White.copy(alpha = 0.70f),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.1.sp,
                    fontSize = 9.5.sp
                )
            )

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isBalanceVisible) "₦${String.format(java.util.Locale.US, "%,.2f", walletBalance)}" else "₦ ••••••••",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontSize = 30.sp,
                        letterSpacing = (-0.5).sp
                    )
                )
                Spacer(modifier = Modifier.width(10.dp))
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.14f),
                    border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.38f)),
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { isBalanceVisible = !isBalanceVisible }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = if (isBalanceVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle Balance Visibility",
                            tint = Color.White.copy(alpha = 0.90f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Dynamic Status Banner: Pending Admin Review OR Active Countdown Session
            if (isDepositReportedPendingAdmin) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.09f),
                    border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Deposit Report Under Admin Review",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                )
                                if (reportedDepositReference.isNotBlank()) {
                                    Text(
                                        text = "Reference: $reportedDepositReference",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = CyberCyan,
                                            fontSize = 10.sp
                                        )
                                    )
                                }
                            }
                        }
                        IconButton(
                            onClick = onDismissReportedStatus,
                            modifier = Modifier.size(22.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss status",
                                tint = Color.White.copy(alpha = 0.65f),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            } else if (hasOngoingCountdown) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = GlowingAmber.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, GlowingAmber.copy(alpha = 0.50f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onAddMoneyClick() }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(GlowingAmber)
                            )
                            Spacer(modifier = Modifier.width(7.dp))
                            Text(
                                text = "Transfer in progress (PIN: $activeConfirmationCode)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = GlowingAmber,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = timerText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = GlowingAmber,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = onCancelSession,
                                modifier = Modifier.size(22.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel Transfer Session",
                                    tint = GlowingAmber,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = "Automated Instant Settlements • Pure Cloud Synced",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White.copy(alpha = 0.60f),
                        fontSize = 10.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bottom Pure Glass Buttons Row: [+ FUND WALLET / Continue] [History]
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // If countdown is active, show Continue with Amber accent. Otherwise, glowing glass CyberCyan!
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Transparent,
                    border = BorderStroke(
                        1.dp,
                        if (hasOngoingCountdown) GlowingAmber.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.60f)
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            brush = if (hasOngoingCountdown) {
                                Brush.horizontalGradient(listOf(Color(0xFFD97706), Color(0xFFB45309)))
                            } else {
                                Brush.horizontalGradient(listOf(Color(0xFF00F0FF), Color(0xFF0284C7)))
                            }
                        )
                        .clickable { onAddMoneyClick() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (hasOngoingCountdown) Icons.Default.PlayArrow else Icons.Default.Add,
                            contentDescription = if (hasOngoingCountdown) "Continue Transfer" else "Fund Wallet",
                            tint = if (hasOngoingCountdown) Color.White else Color(0xFF031024),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (hasOngoingCountdown) "Continue" else "+ FUND WALLET",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.6.sp,
                                color = if (hasOngoingCountdown) Color.White else Color(0xFF031024)
                            )
                        )
                    }
                }

                // Frosted Glass History Button
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onHistoryClick() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "History",
                            tint = Color.White.copy(alpha = 0.95f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "History",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Home Page Services Section (Only 4 items: Airtime, Data, Cable, More, and "View All" link)
 */
@Composable
fun ServicesGridSection(
    onServiceClick: (ServiceItem) -> Unit,
    onViewAllClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Services",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 16.sp
                )
            )

            Text(
                text = "View All",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = CyberCyan,
                    fontSize = 13.sp
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onViewAllClick() }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 4 Columns Row (Airtime, Data, Cable TV, More)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HOME_SERVICES.forEach { item ->
                ServiceTile(
                    item = item,
                    onClick = {
                        if (item.id == "more") {
                            onViewAllClick()
                        } else {
                            onServiceClick(item)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Main Data Hub Full Services Grid (Uniform 4x2 grid: 4 on Line 1, 4 on Line 2 with identical dimensions)
 */
@Composable
fun FullServicesGridSection(
    onServiceClick: (ServiceItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val row1 = DEFAULT_SERVICES.take(4) // Airtime, Data, Cable TV, Combo Packs
    val row2 = DEFAULT_SERVICES.drop(4) // Electricity, Recharge PIN, Bet Funding, Exams

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Services & Utilities",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                fontSize = 16.sp
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Row 1 (4 items)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            row1.forEach { item ->
                ServiceTile(
                    item = item,
                    onClick = { onServiceClick(item) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Row 2 (4 items - exactly equal to Row 1)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            row2.forEach { item ->
                ServiceTile(
                    item = item,
                    onClick = { onServiceClick(item) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

data class PromoOfferItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val price: String,
    val originalPrice: String,
    val discountTag: String,
    val icon: ImageVector,
    val accentColor: Color
)

/**
 * 3rd Box: Offers (Special Offer Bundles Card)
 */
@Composable
fun OffersSectionCard(
    onOfferClick: (PromoOfferItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val offers = remember {
        listOf(
            PromoOfferItem(
                id = "offer_1",
                title = "MTN 10GB Mega Bundle",
                subtitle = "30 Days Validity • High Speed SME",
                price = "₦2,000",
                originalPrice = "₦2,500",
                discountTag = "20% OFF",
                icon = Icons.Default.Bolt,
                accentColor = GlowingAmber
            ),
            PromoOfferItem(
                id = "offer_2",
                title = "Airtel 5GB + Airtime Bonus",
                subtitle = "5GB Direct Data + ₦500 Talktime",
                price = "₦1,200",
                originalPrice = "₦1,500",
                discountTag = "HOT DEAL",
                icon = Icons.Default.LocalFireDepartment,
                accentColor = WarningRed
            ),
            PromoOfferItem(
                id = "offer_3",
                title = "DSTV Yanga + 1GB Data",
                subtitle = "Cable TV Subscription + 1GB Data Combo",
                price = "₦5,100",
                originalPrice = "₦5,800",
                discountTag = "COMBO",
                icon = Icons.Default.Tv,
                accentColor = CyberCyan
            ),
            PromoOfferItem(
                id = "offer_4",
                title = "WAEC E-PIN + 1.5GB Data",
                subtitle = "Result Checker PIN + 1.5GB Student Pack",
                price = "₦3,200",
                originalPrice = "₦3,800",
                discountTag = "STUDENT",
                icon = Icons.Default.School,
                accentColor = ElectricEmerald
            )
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, CyberCyan.copy(alpha = 0.25f), RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(GlowingAmber.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalOffer,
                            contentDescription = "Offers",
                            tint = GlowingAmber,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Offers",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 16.sp
                            )
                        )
                        Text(
                            text = "Exclusive Discounted Bundles & Combos",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = GlowingAmber.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GlowingAmber.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "HOT DEALS 🔥",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = GlowingAmber,
                            fontSize = 10.sp
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                offers.forEach { offer ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, DarkCardBorder, RoundedCornerShape(14.dp))
                            .clickable { onOfferClick(offer) },
                        colors = CardDefaults.cardColors(containerColor = DarkSurface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(offer.accentColor.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = offer.icon,
                                        contentDescription = offer.title,
                                        tint = offer.accentColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = offer.title,
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = offer.accentColor.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = offer.discountTag,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Black,
                                                    color = offer.accentColor,
                                                    fontSize = 9.sp
                                                ),
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = offer.subtitle,
                                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 11.sp)
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = offer.price,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        color = CyberCyan
                                    )
                                )
                                Text(
                                    text = offer.originalPrice,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TextMuted,
                                        textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough,
                                        fontSize = 10.sp
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

@Composable
fun ServiceTile(
    item: ServiceItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(86.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFF1E3554), RoundedCornerShape(16.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF112239))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 10.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF193252)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    tint = CyberCyan,
                    modifier = Modifier.size(19.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    fontSize = 11.5.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Rewards section matching screenshot
 */
@Composable
fun RewardsSectionCard(
    cashbackAmount: String = "₦0.00",
    referralAmount: String = "₦0.00",
    rewardPoints: Int = 0,
    rewardPointsValueNaira: Double = 0.0,
    onCashbackClick: () -> Unit = {},
    onReferralClick: () -> Unit = {},
    onRewardsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Rewards & Cashback",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 16.sp
                )
            )

            if (rewardPoints > 0 || cashbackAmount != "₦0.00" || referralAmount != "₦0.00") {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = GlowingAmber.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(0.8.dp, GlowingAmber.copy(alpha = 0.5f)),
                    modifier = Modifier.clickable { onRewardsClick() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "🎁 $rewardPoints PTS • REDEEM",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = GlowingAmber,
                                fontWeight = FontWeight.Black,
                                fontSize = 9.sp
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // If rewards collected, show an engaging callout banner
        if (rewardPoints > 0) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = DarkSurfaceElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onRewardsClick() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    ) {
                        Text("⭐", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "REWARDS BALANCE: $rewardPoints POINTS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = CyberCyan,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp,
                                    letterSpacing = 0.5.sp
                                )
                            )
                            Text(
                                text = "Convertible to ₦${String.format(java.util.Locale.US, "%,.2f", rewardPointsValueNaira)} instant wallet cash",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondary,
                                    fontSize = 10.5.sp
                                )
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = ElectricEmerald.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(0.6.dp, ElectricEmerald)
                    ) {
                        Text(
                            text = "CLAIM CASH",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = ElectricEmerald,
                                fontWeight = FontWeight.Black,
                                fontSize = 9.5.sp
                            ),
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Card 1: Points Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onRewardsClick() },
                colors = CardDefaults.cardColors(containerColor = Color(0xFF112239))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(GlowingAmber.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⭐", fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Points",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$rewardPoints Pts",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Black,
                            color = GlowingAmber,
                            fontSize = 15.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Card 2: Cashback Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onCashbackClick() },
                colors = CardDefaults.cardColors(containerColor = Color(0xFF112239))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2A3D1E)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🪙", fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Cashback",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = cashbackAmount,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Black,
                            color = ElectricEmerald,
                            fontSize = 15.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Card 3: Referrals Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onReferralClick() },
                colors = CardDefaults.cardColors(containerColor = Color(0xFF112239))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF3B2338)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("📢", fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Referrals",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = referralAmount,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Black,
                            color = TextPrimary,
                            fontSize = 15.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Universal Purchasing Bottom Sheet Drawer for Services
 * Unique custom drawers per service type with dynamic height, calculated % OFF discounts, and account verification.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServicePurchasingBottomSheet(
    service: ServiceItem,
    viewModel: VpnViewModel,
    onOpenElectricityDashboard: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    when (service.id) {
        "data" -> {
            DataBundlesSheet(
                viewModel = viewModel,
                onDismiss = onDismiss
            )
            return
        }
        "airtime" -> {
            AirtimeSheet(
                viewModel = viewModel,
                onDismiss = onDismiss
            )
            return
        }
        "cable" -> {
            CableTvSheet(
                viewModel = viewModel,
                onDismiss = onDismiss
            )
            return
        }
        "utility" -> {
            ElectricitySheet(
                viewModel = viewModel,
                onOpenTracker = onOpenElectricityDashboard,
                onDismiss = onDismiss
            )
            return
        }
        "recharge", "betting", "education", "combo", "transfer" -> {
            SpecializedServicesSheet(
                serviceId = service.id,
                viewModel = viewModel,
                onDismiss = onDismiss
            )
            return
        }
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    val walletBalance by viewModel.userWalletBalance.collectAsStateWithLifecycle()
    val userVirtualAccount by viewModel.userVirtualAccount.collectAsStateWithLifecycle()
    val serviceDiscounts by viewModel.serviceDiscounts.collectAsStateWithLifecycle()
    val livePairgatePlans by viewModel.pairgateDataPlans.collectAsStateWithLifecycle()
    val vtuMarkupPercent by viewModel.vtuMarkupPercent.collectAsStateWithLifecycle()
    val dataPricingStrategy by viewModel.dataPricingStrategy.collectAsStateWithLifecycle()
    val telcoDiscountPercent by viewModel.telcoDiscountPercent.collectAsStateWithLifecycle()
    val isFetchingDataPlans by viewModel.isFetchingDataPlans.collectAsStateWithLifecycle()
    val dataPlansStatusMessage by viewModel.dataPlansStatusMessage.collectAsStateWithLifecycle()
    val savedRecipients by viewModel.savedRecipients.collectAsStateWithLifecycle()
    val favoritePlanKeys by viewModel.favoriteDataPlanKeys.collectAsStateWithLifecycle()
    val recentDataPurchases by viewModel.recentDataPurchases.collectAsStateWithLifecycle()

    val myPhone = remember(userVirtualAccount) {
        userVirtualAccount.phoneNumber.trim()
    }

    var recipientInput by remember { mutableStateOf(myPhone) }
    var amountInput by remember { mutableStateOf("500") }
    var selectedOption by remember { mutableStateOf("MTN") }
    var selectedPeriodTab by remember { mutableStateOf("All") }
    var secondaryInput by remember { mutableStateOf("") }
    var selectedDataPlan by remember { mutableStateOf("MTN 1.0GB SME - ₦280") }
    var selectedDataPlanId by remember { mutableStateOf("1") }
    var quantityInput by remember { mutableStateOf("1") }
    var verifiedAccountName by remember { mutableStateOf<String?>(null) }
    var isVerifying by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var resultMessage by remember { mutableStateOf<String?>(null) }
    var purchaseReceipt by remember { mutableStateOf<PurchaseSuccessReceipt?>(null) }
    var showSafetyVerificationDrawer by remember { mutableStateOf(false) }
    var showSetupPhoneDialog by remember { mutableStateOf(false) }
    var newPhoneInputInSheet by remember { mutableStateOf("") }
    var showSavedRecipientsManager by remember { mutableStateOf(false) }
    var directRepurchaseData by remember { mutableStateOf<com.example.data.api.RecentDataPurchase?>(null) }

    val discountPct = serviceDiscounts[service.id] ?: 3.0
    val faceValueVal = amountInput.toDoubleOrNull() ?: 500.0
    val discountedPayableVal = faceValueVal * (1.0 - (discountPct / 100.0))
    val totalSavingsVal = faceValueVal - discountedPayableVal
    val calculatedCashback = (discountedPayableVal * 0.02).toInt()

    val blockParentScroll = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset = Offset(0f, available.y)

            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity
            ): Velocity = Velocity(0f, available.y)
        }
    }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    LaunchedEffect(purchaseReceipt) {
        if (purchaseReceipt != null) {
            sheetState.expand()
        }
    }

    val launchContactPicker = rememberContactPicker { name, phone ->
        if (phone.isNotBlank()) {
            recipientInput = phone
        }
    }

    var drawerToast by remember { mutableStateOf<DrawerToastMessage?>(null) }

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = { if (!isProcessing) onDismiss() },
        containerColor = DarkSurfaceElevated,
        scrimColor = Color.Black.copy(alpha = 0.65f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(44.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.35f))
            )
        }
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
        if (purchaseReceipt != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 48.dp)
            ) {
                // Header with title and close button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Transaction Receipt",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    IconButton(
                        onClick = {
                            purchaseReceipt = null
                            onDismiss()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Receipt",
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                TransactionSuccessReceiptView(
                    receipt = purchaseReceipt!!,
                    onReturnHome = {
                        purchaseReceipt = null
                        onDismiss()
                    },
                    onMakeAnotherPurchase = {
                        purchaseReceipt = null
                        resultMessage = null
                        recipientInput = myPhone
                    },
                    onSaveRecipient = { name, type, identifier, provider ->
                        viewModel.saveRecipient(
                            name = name,
                            recipientType = type,
                            identifier = identifier,
                            institutionOrProvider = provider
                        )
                    }
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .nestedScroll(blockParentScroll)
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
            ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(CyberCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = service.icon, contentDescription = service.title, tint = CyberCyan, modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = when (service.id) {
                                "airtime" -> "BUY AIRTIME"
                                "data" -> "BUY DATA BUNDLE"
                                "cable" -> "CABLE TV SUBSCRIPTION"
                                "utility" -> "ELECTRICITY BILL PAYMENT"
                                "recharge" -> "PRINT RECHARGE CARDS"
                                "betting" -> "FUND BETTING WALLET"
                                "education" -> "PURCHASE EXAM PIN"
                                "transfer" -> "INSTANT BANK TRANSFER"
                                "giftcard" -> "BUY GIFT VOUCHER"
                                else -> service.title.uppercase()
                            },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, color = TextPrimary, letterSpacing = 0.5.sp)
                        )
                        Text(text = service.description, style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp))
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Dynamic Discount & Cashback Banner
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF1E1738),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(CyberCyan.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("⚡", fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "OFFICIAL ADMIN DISCOUNT: $discountPct% OFF",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, color = CyberCyan)
                            )
                            Text(
                                text = "Save ₦${String.format("%,.2f", totalSavingsVal)} + Earn ₦$calculatedCashback Cashback Bonus!",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 10.sp)
                            )
                        }
                    }

                    Surface(
                        color = CyberCyan,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${discountPct.toInt()}% OFF",
                            style = MaterialTheme.typography.labelSmall.copy(color = DarkObsidian, fontWeight = FontWeight.Black, fontSize = 11.sp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SERVICE-SPECIFIC CUSTOM DRAWERS
            when (service.id) {
                "combo" -> {
                    // COMBO PACKS DRAWER
                    val comboPacks by viewModel.comboPacks.collectAsStateWithLifecycle()

                    Text("1. Select SME / Gifting Combo Pack", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(8.dp))

                    comboPacks.forEach { combo ->
                        val isSel = secondaryInput == combo.id || (secondaryInput.isEmpty() && combo.id == comboPacks.firstOrNull()?.id)
                        val savings = (combo.individualVal - combo.retailPrice).toInt()

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSel) Color(0xFF103248) else DarkSurface,
                            border = androidx.compose.foundation.BorderStroke(if (isSel) 1.5.dp else 1.dp, if (isSel) CyberCyan else DarkCardBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    secondaryInput = combo.id
                                    amountInput = combo.retailPrice.toInt().toString()
                                }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(combo.title, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
                                        if (combo.isPopular) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(color = GlowingAmber.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                                                Text("HOT 🔥", style = MaterialTheme.typography.labelSmall.copy(color = GlowingAmber, fontSize = 9.sp, fontWeight = FontWeight.Black), modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                            }
                                        }
                                    }
                                    Surface(color = CyberCyan.copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp)) {
                                        Text(combo.network, style = MaterialTheme.typography.labelSmall.copy(color = CyberCyan, fontSize = 10.sp, fontWeight = FontWeight.Black), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Includes: ${combo.dataAmount} + ₦${combo.airtimeAmount.toInt()} Airtime + ${combo.vpnDays} Days VIP VPN Pass", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp))

                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Pay ₦${combo.retailPrice.toInt()} (Valued @ ₦${combo.individualVal.toInt()})", style = MaterialTheme.typography.labelSmall.copy(color = CyberCyan, fontWeight = FontWeight.Bold))
                                    Text("⚡ SAVE ₦$savings", style = MaterialTheme.typography.labelSmall.copy(color = ElectricEmerald, fontWeight = FontWeight.Black, fontSize = 10.sp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    QuickRecipientPickerRow(
                        recipients = savedRecipients,
                        recipientTypeFilter = "contact",
                        onRecipientSelected = { r -> recipientInput = r.identifier },
                        onManageClick = { showSavedRecipientsManager = true },
                        onContactPicked = { name, phone -> recipientInput = phone }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("2. Beneficiary Phone Number", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontWeight = FontWeight.Bold))
                        ContactPickerButton(
                            onContactPicked = { name, phone -> recipientInput = phone },
                            label = "Contacts"
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = recipientInput,
                        onValueChange = { recipientInput = it },
                        placeholder = { Text("080XXXXXXXX", color = TextMuted) },
                        trailingIcon = {
                            IconButton(onClick = { launchContactPicker() }) {
                                Icon(imageVector = Icons.Default.ContactPhone, contentDescription = "Pick Contact", tint = CyberCyan, modifier = Modifier.size(18.dp))
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedContainerColor = DarkSurface,
                            unfocusedContainerColor = DarkSurface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                "airtime" -> {
                    // AIRTIME DRAWER
                    Text("1. Select Network", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("MTN", "Airtel", "Glo", "9mobile").forEach { net ->
                            val isSelected = selectedOption.equals(net, ignoreCase = true)
                            val netColor = getNetworkColor(net)
                            val netContentColor = getNetworkContentColor(net)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) netColor.copy(alpha = 0.18f) else DarkSurface,
                                border = androidx.compose.foundation.BorderStroke(
                                    if (isSelected) 1.5.dp else 1.dp,
                                    if (isSelected) netColor else DarkCardBorder
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(72.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { selectedOption = net }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(vertical = 6.dp, horizontal = 2.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    TelcoLogo(network = net, size = 24.dp)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = net,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                            color = if (isSelected) netColor else TextPrimary,
                                            fontSize = 11.sp
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (isSelected) netColor else netColor.copy(alpha = 0.25f)
                                    ) {
                                        Text(
                                            text = "Cashback",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = if (isSelected) netContentColor else netColor,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 8.5.sp
                                            ),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    QuickRecipientPickerRow(
                        recipients = savedRecipients,
                        recipientTypeFilter = "contact",
                        onRecipientSelected = { r ->
                            if (r.institutionOrProvider.isNotBlank()) selectedOption = r.institutionOrProvider
                            recipientInput = r.identifier
                        },
                        onManageClick = { showSavedRecipientsManager = true },
                        onContactPicked = { name, phone -> recipientInput = phone }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("2. Recipient Phone Number", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontWeight = FontWeight.Bold))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ContactPickerButton(
                                onContactPicked = { name, phone -> recipientInput = phone },
                                label = "Contacts"
                            )
                            if (myPhone.isNotEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = CyberCyan.copy(alpha = 0.15f),
                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, CyberCyan.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { recipientInput = myPhone }
                                ) {
                                    Text(
                                        text = "+ My Phone ($myPhone)",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = CyberCyan,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        ),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF1E293B),
                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, CyberCyan.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            newPhoneInputInSheet = ""
                                            showSetupPhoneDialog = true
                                        }
                                ) {
                                    Text(
                                        text = "+ Link Phone",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = CyberCyan,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        ),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = recipientInput,
                        onValueChange = { recipientInput = it },
                        placeholder = { Text("080XXXXXXXX", color = TextMuted) },
                        trailingIcon = {
                            IconButton(onClick = { launchContactPicker() }) {
                                Icon(imageVector = Icons.Default.ContactPhone, contentDescription = "Pick Contact", tint = CyberCyan, modifier = Modifier.size(18.dp))
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedContainerColor = DarkSurface,
                            unfocusedContainerColor = DarkSurface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("3. Amount Preset (₦)", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("100", "200", "500", "1000", "2000", "5000").forEach { preset ->
                            val isSel = amountInput == preset
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSel) CyberCyan else DarkSurface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) CyberCyan else DarkCardBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { amountInput = preset }
                            ) {
                                Text(
                                    text = "₦$preset",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSel) DarkObsidian else TextPrimary,
                                        fontSize = 11.sp
                                    ),
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it },
                        label = { Text("Custom Amount (₦)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedContainerColor = DarkSurface,
                            unfocusedContainerColor = DarkSurface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                "data" -> {
                    // DATA BUNDLE DRAWER - Buy Again Carousel
                    if (recentDataPurchases.isNotEmpty()) {
                        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Buy Again",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                )
                                Text(
                                    text = "1-Tap Direct Purchase",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = CyberCyan,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                recentDataPurchases.take(6).forEach { recent ->
                                    val periodText = recent.validity.takeIf { it.isNotBlank() } ?: "30 Days"
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = DarkSurface,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
                                        modifier = Modifier
                                            .width(310.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .clickable { directRepurchaseData = recent }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(CyberCyan.copy(alpha = 0.12f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Repeat,
                                                        contentDescription = null,
                                                        tint = CyberCyan,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "${recent.network} Data • ₦${String.format(java.util.Locale.US, "%,.2f", recent.amountNaira)}",
                                                        style = MaterialTheme.typography.bodyMedium.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            color = TextPrimary,
                                                            fontSize = 13.sp
                                                        ),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = "Period: $periodText • To: ${recent.recipientPhone}",
                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                            color = TextSecondary,
                                                            fontSize = 11.sp
                                                        ),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            val repNetColor = getNetworkColor(recent.network)
                                            val repNetContentColor = getNetworkContentColor(recent.network)
                                            Button(
                                                onClick = { directRepurchaseData = recent },
                                                shape = RoundedCornerShape(10.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = repNetColor,
                                                    contentColor = repNetContentColor
                                                ),
                                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                                modifier = Modifier.height(34.dp)
                                            ) {
                                                Text(
                                                    text = "Buy again",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = repNetContentColor,
                                                        fontSize = 11.sp
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                        }
                    }

                    Text("1. Select Network", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("MTN", "Airtel", "Glo", "9mobile").forEach { net ->
                            val isSelected = selectedOption.equals(net, ignoreCase = true)
                            val netColor = getNetworkColor(net)
                            val netContentColor = getNetworkContentColor(net)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) netColor.copy(alpha = 0.18f) else DarkSurface,
                                border = androidx.compose.foundation.BorderStroke(
                                    if (isSelected) 1.5.dp else 1.dp,
                                    if (isSelected) netColor else DarkCardBorder
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(72.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        selectedOption = net
                                        val defaultPlanId = when (net.uppercase()) {
                                            "MTN" -> "20"
                                            "AIRTEL" -> "90"
                                            "GLO" -> "61"
                                            "9MOBILE" -> "129"
                                            else -> "20"
                                        }
                                        val defaultPrice = when (net.uppercase()) {
                                            "MTN" -> "280"
                                            "AIRTEL" -> "290"
                                            "GLO" -> "265"
                                            "9MOBILE" -> "250"
                                            else -> "280"
                                        }
                                        selectedDataPlan = "$net 1.0GB SME - ₦$defaultPrice"
                                        selectedDataPlanId = defaultPlanId
                                        amountInput = defaultPrice
                                    }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(vertical = 6.dp, horizontal = 2.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    TelcoLogo(network = net, size = 24.dp)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = net,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                            color = if (isSelected) netColor else TextPrimary,
                                            fontSize = 11.sp
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (isSelected) netColor else netColor.copy(alpha = 0.25f)
                                    ) {
                                        Text(
                                            text = "Cashback",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = if (isSelected) netContentColor else netColor,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 8.5.sp
                                            ),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "2. Select Validity Period",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontWeight = FontWeight.Bold)
                        )
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { viewModel.fetchPairgateDataPlans(forceRefresh = true) }
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isFetchingDataPlans) {
                                FlowButtonLoadingLine(color = CyberCyan, width = 24.dp, height = 2.dp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Syncing...", fontSize = 11.sp, color = CyberCyan)
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = "Sync", modifier = Modifier.size(12.dp), tint = CyberCyan)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Sync Live Plans", fontSize = 11.sp, color = CyberCyan)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    val allCatalogPlans = remember(selectedOption, livePairgatePlans, vtuMarkupPercent, dataPricingStrategy, telcoDiscountPercent) {
                        PairgateCatalogHelper.buildCatalogForNetwork(
                            network = selectedOption,
                            livePlans = livePairgatePlans,
                            markupPercent = vtuMarkupPercent,
                            pricingStrategy = dataPricingStrategy,
                            discountPercent = telcoDiscountPercent
                        )
                    }

                    val hotPlans = remember(allCatalogPlans) {
                        allCatalogPlans.filter { it.isHot }
                    }
                    val dailyPlans = remember(allCatalogPlans) {
                        allCatalogPlans.filter { it.validityTab == "Daily" }
                    }
                    val weeklyPlans = remember(allCatalogPlans) {
                        allCatalogPlans.filter { it.validityTab == "Weekly" }
                    }
                    val monthlyPlans = remember(allCatalogPlans) {
                        allCatalogPlans.filter { it.validityTab == "Monthly" || it.validityTab == "2-Months" }
                    }
                    val favoritePlans = remember(allCatalogPlans, favoritePlanKeys) {
                        allCatalogPlans.filter { favoritePlanKeys.contains("${it.network}_${it.id}") || favoritePlanKeys.contains(it.id) }
                    }

                    val periodTabs = listOf(
                        Triple("All", "🌐 All Bundles", allCatalogPlans.size),
                        Triple("Favorites", "⭐ Favs", favoritePlans.size),
                        Triple("Hot", "🔥 Hot", hotPlans.size),
                        Triple("Daily", "⚡ Daily", dailyPlans.size),
                        Triple("Weekly", "📅 Weekly", weeklyPlans.size),
                        Triple("Monthly", "🗓️ Monthly", monthlyPlans.size)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        periodTabs.forEach { (tabKey, tabLabel, count) ->
                            val isSelected = selectedPeriodTab == tabKey
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) CyberCyan else DarkSurface,
                                border = androidx.compose.foundation.BorderStroke(
                                    if (isSelected) 1.5.dp else 1.dp,
                                    if (isSelected) CyberCyan else DarkCardBorder
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { selectedPeriodTab = tabKey }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = tabLabel,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) DarkObsidian else TextPrimary,
                                            fontSize = 12.sp
                                        )
                                    )
                                    if (count > 0) {
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Surface(
                                            color = if (isSelected) DarkObsidian.copy(alpha = 0.2f) else CyberCyan.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text(
                                                text = "$count",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = if (isSelected) DarkObsidian else CyberCyan,
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 10.sp
                                                ),
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val activePlansToDisplay = when (selectedPeriodTab) {
                        "All" -> allCatalogPlans
                        "Favorites" -> favoritePlans
                        "Hot" -> if (hotPlans.isNotEmpty()) hotPlans else allCatalogPlans
                        "Daily" -> dailyPlans
                        "Weekly" -> weeklyPlans
                        "Monthly" -> monthlyPlans
                        else -> allCatalogPlans
                    }

                    if (isFetchingDataPlans) {
                        ReflectiveBundleListSkeleton(
                            count = 3,
                            statusText = "Syncing carrier bundles for $selectedOption...",
                            subText = "Connecting with upstream carrier network for latest packages",
                            onRetrySync = { viewModel.fetchPairgateDataPlans(forceRefresh = true) }
                        )
                    } else if (activePlansToDisplay.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            activePlansToDisplay.forEach { plan ->
                                val pId = plan.id
                                val isSel = selectedDataPlanId == pId || selectedDataPlan == plan.name
                                val planPrice = plan.price
                                val origPrice = (planPrice * 1.15).toInt().coerceAtLeast((planPrice + 50).toInt())
                                val discPrice = planPrice.toInt()
                                val isFav = favoritePlanKeys.contains("${plan.network}_${plan.id}") || favoritePlanKeys.contains(plan.id)
                                val isHot = plan.isHot

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSel) Color(0xFF103248) else DarkSurface,
                                    border = androidx.compose.foundation.BorderStroke(if (isSel) 1.5.dp else 1.dp, if (isSel) CyberCyan else DarkCardBorder),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            selectedDataPlan = plan.name
                                            selectedDataPlanId = pId
                                            amountInput = discPrice.toString()
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = isSel,
                                                onClick = {
                                                    selectedDataPlan = plan.name
                                                    selectedDataPlanId = pId
                                                    amountInput = discPrice.toString()
                                                },
                                                colors = RadioButtonDefaults.colors(selectedColor = CyberCyan)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Column {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = plan.name,
                                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                                                    )
                                                    if (isHot) {
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Surface(color = GlowingAmber.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                                                            Text("HOT 🔥", style = MaterialTheme.typography.labelSmall.copy(color = GlowingAmber, fontSize = 8.sp, fontWeight = FontWeight.Black), modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                                        }
                                                    }
                                                }
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = "₦$origPrice",
                                                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough, fontSize = 11.sp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "Pay ₦$discPrice",
                                                        style = MaterialTheme.typography.bodySmall.copy(color = CyberCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "• ${plan.validityText}",
                                                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 10.sp)
                                                    )
                                                }
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Surface(
                                                color = ElectricEmerald.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = plan.category,
                                                    style = MaterialTheme.typography.labelSmall.copy(color = ElectricEmerald, fontSize = 10.sp, fontWeight = FontWeight.Black),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = { viewModel.toggleFavoriteDataPlan("${plan.network}_${plan.id}") },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (isFav) Icons.Filled.Star else Icons.Filled.StarBorder,
                                                    contentDescription = "Favorite",
                                                    tint = if (isFav) GlowingAmber else TextMuted,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else if (selectedPeriodTab == "Favorites") {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = DarkSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("⭐", fontSize = 24.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("No Starred Plans for $selectedOption", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
                                Text("Tap the star icon on any plan to save it to favorites for instant access.", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp, textAlign = TextAlign.Center), modifier = Modifier.padding(top = 2.dp, bottom = 8.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = CyberCyan.copy(alpha = 0.15f),
                                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { selectedPeriodTab = "All" }
                                ) {
                                    Text("Browse All Bundles", color = CyberCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "3. Recipient Phone Number",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // "Contacts" -> opens phone contacts picker
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = ElectricEmerald.copy(alpha = 0.12f),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, ElectricEmerald.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { launchContactPicker() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Contacts,
                                        contentDescription = "Contacts",
                                        tint = ElectricEmerald,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Contacts",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = ElectricEmerald,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                    )
                                }
                            }

                            // "Add" -> opens beneficiary manager contact list
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = DarkSurfaceElevated,
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, CyberCyan.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { showSavedRecipientsManager = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PersonAdd,
                                        contentDescription = "Add Beneficiary",
                                        tint = CyberCyan,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (savedRecipients.isNotEmpty()) "Add (${savedRecipients.size})" else "+ Add",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = CyberCyan,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                    )
                                }
                            }

                            // User's own Phone Number quick chip
                            if (myPhone.isNotEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = CyberCyan.copy(alpha = 0.15f),
                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, CyberCyan.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { recipientInput = myPhone }
                                ) {
                                    Text(
                                        text = "+ My Phone ($myPhone)",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = CyberCyan,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        ),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF1E293B),
                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, CyberCyan.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            newPhoneInputInSheet = ""
                                            showSetupPhoneDialog = true
                                        }
                                ) {
                                    Text(
                                        text = "+ Link Phone",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = CyberCyan,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        ),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = recipientInput,
                        onValueChange = { recipientInput = it },
                        placeholder = { Text("080XXXXXXXX", color = TextMuted) },
                        trailingIcon = {
                            IconButton(onClick = { launchContactPicker() }) {
                                Icon(imageVector = Icons.Default.ContactPhone, contentDescription = "Pick Contact", tint = CyberCyan, modifier = Modifier.size(18.dp))
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedContainerColor = DarkSurface,
                            unfocusedContainerColor = DarkSurface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                "cable" -> {
                    // CABLE TV DRAWER
                    Text("1. Select Cable TV Provider", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("GOTV", "DSTV", "Startimes", "Showmax").forEach { cab ->
                            val sel = selectedOption == cab
                            FilterChip(
                                selected = sel,
                                onClick = {
                                    selectedOption = cab
                                    verifiedAccountName = null
                                },
                                label = { Text(cab, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyberCyan,
                                    selectedLabelColor = DarkObsidian,
                                    containerColor = DarkSurface
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    QuickRecipientPickerRow(
                        recipients = savedRecipients,
                        recipientTypeFilter = "cable",
                        onRecipientSelected = { r ->
                            if (r.institutionOrProvider.isNotBlank()) selectedOption = r.institutionOrProvider
                            recipientInput = r.identifier
                            if (r.bankAccountName != null) verifiedAccountName = r.bankAccountName
                        },
                        onManageClick = { showSavedRecipientsManager = true }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("2. Smartcard / Decoder IUC Number", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = recipientInput,
                            onValueChange = {
                                recipientInput = it
                                verifiedAccountName = null
                            },
                            placeholder = { Text("7012398401", color = TextMuted) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan, unfocusedBorderColor = DarkCardBorder),
                            modifier = Modifier.weight(1f)
                        )

                        Button(
                            onClick = {
                                if (!com.example.data.util.NetworkUtils.isNetworkAvailable(context)) {
                                    android.widget.Toast.makeText(context, "No network connection. Please check your mobile data or Wi-Fi.", android.widget.Toast.LENGTH_LONG).show()
                                    resultMessage = "No network connection. Please check your mobile data or Wi-Fi to verify account."
                                    return@Button
                                }
                                val clean = recipientInput.trim()
                                if (clean.length >= 8) {
                                    isVerifying = true
                                    viewModel.verifyCustomerOrMeter(
                                        serviceId = selectedOption.lowercase(),
                                        customerId = clean,
                                        type = "cable"
                                    ) { ok, res ->
                                        isVerifying = false
                                        if (ok) {
                                            verifiedAccountName = res
                                        } else {
                                            verifiedAccountName = null
                                            android.widget.Toast.makeText(context, res, android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else {
                                    android.widget.Toast.makeText(context, "Please enter at least 8 digits", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = !isVerifying,
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("VERIFY", fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    verifiedAccountName?.let { name ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = ElectricEmerald.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ElectricEmerald.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Verified", tint = ElectricEmerald, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("✓ Account Holder: $name", style = MaterialTheme.typography.labelSmall.copy(color = ElectricEmerald, fontWeight = FontWeight.Bold))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("3. Cable Subscription Package", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(8.dp))

                    val cablePackages = listOf(
                        Triple("$selectedOption Small/Jinja Plan", 3300.0, 3200.0),
                        Triple("$selectedOption Standard/Jolli Plan", 4850.0, 4700.0),
                        Triple("$selectedOption Max/Super Plan", 7200.0, 6980.0),
                        Triple("$selectedOption Premium Full Package", 15700.0, 15200.0)
                    )

                    cablePackages.forEach { (pkgName, origP, discP) ->
                        val isSel = secondaryInput == pkgName
                        val pct = (((origP - discP) / origP) * 100.0).toInt()

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSel) Color(0xFF103248) else DarkSurface,
                            border = androidx.compose.foundation.BorderStroke(if (isSel) 1.5.dp else 1.dp, if (isSel) CyberCyan else DarkCardBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    secondaryInput = pkgName
                                    amountInput = discP.toInt().toString()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(pkgName, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
                                    Text("Pay ₦${discP.toInt()} (Save ₦${(origP - discP).toInt()})", style = MaterialTheme.typography.bodySmall.copy(color = CyberCyan, fontSize = 11.sp))
                                }
                                Surface(color = ElectricEmerald.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                                    Text("⚡ $pct% OFF", style = MaterialTheme.typography.labelSmall.copy(color = ElectricEmerald, fontSize = 10.sp, fontWeight = FontWeight.Black), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                        }
                    }
                }

                "utility" -> {
                    // ELECTRICITY DRAWER
                    QuickRecipientPickerRow(
                        recipients = savedRecipients,
                        recipientTypeFilter = "utility",
                        onRecipientSelected = { r ->
                            if (r.institutionOrProvider.isNotBlank()) selectedOption = r.institutionOrProvider
                            recipientInput = r.identifier
                            if (r.bankAccountName != null) verifiedAccountName = r.bankAccountName
                        },
                        onManageClick = { showSavedRecipientsManager = true }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("1. Select Electricity Disco", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("IKEDC (Ikeja)", "EKEDC (Eko)", "AEDC (Abuja)", "IBEDC").forEach { disco ->
                            val sel = selectedOption == disco
                            FilterChip(
                                selected = sel,
                                onClick = { selectedOption = disco },
                                label = { Text(disco, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CyberCyan, selectedLabelColor = DarkObsidian, containerColor = DarkSurface)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("2. Meter Type & Number", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf("PREPAID", "POSTPAID").forEach { mode ->
                            val isSel = secondaryInput.ifEmpty { "PREPAID" } == mode
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) CyberCyan else DarkSurface,
                                modifier = Modifier.weight(1f).clickable { secondaryInput = mode }
                            ) {
                                Text(
                                    text = mode,
                                    style = MaterialTheme.typography.labelSmall.copy(color = if (isSel) DarkObsidian else TextPrimary, fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = recipientInput,
                            onValueChange = { recipientInput = it },
                            placeholder = { Text("Enter Meter Number", color = TextMuted) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan, unfocusedBorderColor = DarkCardBorder),
                            modifier = Modifier.weight(1f)
                        )

                        Button(
                            onClick = {
                                val clean = recipientInput.trim()
                                if (clean.length >= 8) {
                                    isVerifying = true
                                    viewModel.verifyCustomerOrMeter(
                                        serviceId = selectedOption.lowercase(),
                                        customerId = clean,
                                        type = secondaryInput.ifBlank { "prepaid" }.lowercase()
                                    ) { ok, res ->
                                        isVerifying = false
                                        if (ok) {
                                            verifiedAccountName = res
                                            val displayName = res.substringBefore(" • ").ifBlank { "Meter $clean" }
                                            viewModel.saveRecipient(
                                                name = displayName,
                                                recipientType = "utility",
                                                identifier = clean,
                                                institutionOrProvider = selectedOption,
                                                bankAccountName = displayName
                                            )
                                        } else {
                                            verifiedAccountName = null
                                            android.widget.Toast.makeText(context, res, android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else {
                                    android.widget.Toast.makeText(context, "Please enter at least 8 digits", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = !isVerifying,
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("VERIFY", fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    verifiedAccountName?.let { name ->
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("✓ Meter Registered To: $name", style = MaterialTheme.typography.labelSmall.copy(color = ElectricEmerald, fontWeight = FontWeight.Bold))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("3. Amount to Recharge (₦)", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan, unfocusedBorderColor = DarkCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                "recharge" -> {
                    // RECHARGE CARD PRINTING DRAWER
                    Text("1. Select Network for PIN Printing", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("MTN", "Airtel", "Glo", "9mobile").forEach { net ->
                            val isSel = selectedOption == net
                            FilterChip(
                                selected = isSel,
                                onClick = { selectedOption = net },
                                label = { Text(net, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CyberCyan, selectedLabelColor = DarkObsidian, containerColor = DarkSurface)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("2. Card Denomination & Quantity", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("100", "200", "500", "1000").forEach { denom ->
                            val isSel = amountInput == denom
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) CyberCyan else DarkSurface,
                                modifier = Modifier.weight(1f).clickable { amountInput = denom }
                            ) {
                                Text("₦$denom", style = MaterialTheme.typography.labelSmall.copy(color = if (isSel) DarkObsidian else TextPrimary, fontWeight = FontWeight.Bold), modifier = Modifier.padding(vertical = 8.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = quantityInput,
                        onValueChange = { quantityInput = it },
                        label = { Text("Number of Cards / PINs") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan, unfocusedBorderColor = DarkCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                "betting" -> {
                    // BETTING DRAWER
                    Text("1. Select Bookmaker", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("SportyBet", "Bet9ja", "1xBet", "BangBet").forEach { plat ->
                            val sel = selectedOption == plat
                            FilterChip(
                                selected = sel,
                                onClick = { selectedOption = plat },
                                label = { Text(plat, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CyberCyan, selectedLabelColor = DarkObsidian, containerColor = DarkSurface)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = recipientInput,
                            onValueChange = { recipientInput = it },
                            label = { Text("Betting User ID / Account") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan, unfocusedBorderColor = DarkCardBorder),
                            modifier = Modifier.weight(1f)
                        )

                        Button(
                            onClick = {
                                val clean = recipientInput.trim()
                                if (clean.length >= 5) {
                                    isVerifying = true
                                    viewModel.verifyCustomerOrMeter(
                                        serviceId = selectedOption.lowercase(),
                                        customerId = clean,
                                        type = "betting"
                                    ) { ok, res ->
                                        isVerifying = false
                                        if (ok) {
                                            verifiedAccountName = res
                                        } else {
                                            verifiedAccountName = null
                                            android.widget.Toast.makeText(context, res, android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else {
                                    android.widget.Toast.makeText(context, "Please enter at least 5 characters", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = !isVerifying,
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("VERIFY", fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    verifiedAccountName?.let { name ->
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("✓ Verified User: $name", style = MaterialTheme.typography.labelSmall.copy(color = ElectricEmerald, fontWeight = FontWeight.Bold))
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it },
                        label = { Text("Fund Amount (₦)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan, unfocusedBorderColor = DarkCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                "transfer" -> {
                    // BANK TRANSFER DRAWER
                    QuickRecipientPickerRow(
                        recipients = savedRecipients,
                        recipientTypeFilter = "transfer",
                        onRecipientSelected = { r ->
                            if (r.institutionOrProvider.isNotBlank()) selectedOption = r.institutionOrProvider
                            recipientInput = r.identifier
                            if (r.bankAccountName != null) verifiedAccountName = r.bankAccountName
                        },
                        onManageClick = { showSavedRecipientsManager = true }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("1. Select Destination Bank", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("FlowTest Settlement", "GTBank", "FirstBank", "Access Bank", "Zenith").forEach { bank ->
                            val sel = selectedOption == bank
                            FilterChip(
                                selected = sel,
                                onClick = { selectedOption = bank },
                                label = { Text(bank, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CyberCyan, selectedLabelColor = DarkObsidian, containerColor = DarkSurface)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = recipientInput,
                            onValueChange = { recipientInput = it },
                            label = { Text("10-Digit NUBAN Account Number") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan, unfocusedBorderColor = DarkCardBorder),
                            modifier = Modifier.weight(1f)
                        )

                        Button(
                            onClick = {
                                val clean = recipientInput.trim()
                                if (clean.length >= 10) {
                                    isVerifying = true
                                    viewModel.verifyCustomerOrMeter(
                                        serviceId = selectedOption.lowercase(),
                                        customerId = clean,
                                        type = "bank"
                                    ) { ok, res ->
                                        isVerifying = false
                                        if (ok) {
                                            verifiedAccountName = res
                                        } else {
                                            verifiedAccountName = null
                                            android.widget.Toast.makeText(context, res, android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else {
                                    android.widget.Toast.makeText(context, "Please enter a 10-digit NUBAN account number", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = !isVerifying,
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("VERIFY", fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    verifiedAccountName?.let { name ->
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("✓ Account Name: $name", style = MaterialTheme.typography.labelSmall.copy(color = ElectricEmerald, fontWeight = FontWeight.Bold))
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it },
                        label = { Text("Transfer Amount (₦)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan, unfocusedBorderColor = DarkCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                else -> {
                    // DEFAULT SERVICE DRAWER
                    OutlinedTextField(
                        value = recipientInput,
                        onValueChange = { recipientInput = it },
                        label = { Text("Recipient / Account / Phone Number") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan, unfocusedBorderColor = DarkCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it },
                        label = { Text("Amount (₦)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan, unfocusedBorderColor = DarkCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Wallet Balance & Discounted Payable Summary
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = DarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Your App Wallet", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 10.sp))
                        Text("₦${String.format("%,.2f", walletBalance)}", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("Discounted Payable", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 10.sp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("₦${String.format("%,.2f", discountedPayableVal)}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, color = CyberCyan))
                        }
                        Text("Save ₦${String.format("%,.2f", totalSavingsVal)}", style = MaterialTheme.typography.labelSmall.copy(color = ElectricEmerald, fontSize = 10.sp, fontWeight = FontWeight.Bold))
                    }
                }
            }

            resultMessage?.let { msg ->
                val clipboardManager = LocalClipboardManager.current
                val context = LocalContext.current
                val isError = !msg.contains("successful", ignoreCase = true) && !msg.contains("Ref:", ignoreCase = true) && !msg.contains("approved", ignoreCase = true)

                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isError) Color(0xFF2E1616) else Color(0xFF0E2C22),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isError) Color(0xFFFF5252).copy(alpha = 0.8f) else ElectricEmerald.copy(alpha = 0.8f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isError) Icons.Default.ErrorOutline else Icons.Default.CheckCircleOutline,
                                contentDescription = if (isError) "Error" else "Status",
                                tint = if (isError) Color(0xFFFF5252) else ElectricEmerald,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (isError) Color(0xFFFFCDD2) else Color(0xFFA7F3D0),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(msg))
                                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy Error Message",
                                    tint = if (isError) Color(0xFFFF8A80) else CyberCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = { resultMessage = null },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            val effectiveTarget = recipientInput.trim().ifEmpty { myPhone }

            // Helper to execute transaction after safety check
            val executeTransaction: () -> Unit = executeTransaction@ {
                if (!com.example.data.util.NetworkUtils.isNetworkAvailable(context)) {
                    android.widget.Toast.makeText(context, "No network connection. Please check your mobile data or Wi-Fi.", android.widget.Toast.LENGTH_LONG).show()
                    resultMessage = "No network connection. Please check your mobile data or Wi-Fi to complete transaction."
                    return@executeTransaction
                }
                isProcessing = true
                val amt = amountInput.toDoubleOrNull() ?: 500.0
                when (service.id) {
                    "combo" -> {
                        val comboPacksList = viewModel.comboPacks.value
                        val selectedCombo = comboPacksList.find { it.id == secondaryInput } ?: comboPacksList.firstOrNull()
                        if (selectedCombo != null) {
                            viewModel.purchaseComboPack(selectedCombo, effectiveTarget) { ok, msg ->
                                isProcessing = false
                                if (ok) {
                                    purchaseReceipt = PurchaseSuccessReceipt(
                                        serviceTitle = selectedCombo.title,
                                        providerOrType = selectedCombo.network,
                                        recipient = effectiveTarget,
                                        amountPaid = discountedPayableVal,
                                        reference = "PG-CMB-${(100000..999999).random()}",
                                        newBalance = viewModel.userWalletBalance.value,
                                        message = msg,
                                        bonusVpnTime = "+${selectedCombo.vpnDays} Days VIP VPN Pass",
                                        bonusPoints = 50
                                    )
                                } else {
                                    resultMessage = msg
                                }
                            }
                        } else {
                            isProcessing = false
                            resultMessage = "Please select a Combo Pack."
                        }
                    }
                    "transfer" -> {
                        viewModel.transferFunds(recipientInput, selectedOption, secondaryInput.ifEmpty { "Beneficiary" }, amt, "Instant Transfer") { ok, msg, ref ->
                            isProcessing = false
                            if (ok) {
                                purchaseReceipt = PurchaseSuccessReceipt(
                                    serviceTitle = "Bank Transfer ($selectedOption)",
                                    providerOrType = selectedOption,
                                    recipient = "$recipientInput (${secondaryInput.ifEmpty { "Beneficiary" }})",
                                    amountPaid = amt,
                                    reference = ref ?: "TRF-${(100000..999999).random()}",
                                    newBalance = viewModel.userWalletBalance.value,
                                    message = msg,
                                    bonusVpnTime = "+30 Mins Unlimited VPN Time",
                                    bonusPoints = 10
                                )
                            } else {
                                resultMessage = msg
                            }
                        }
                    }
                    "airtime" -> {
                        viewModel.purchasePairgateAirtime(selectedOption, amt.toInt(), effectiveTarget) { ok, msg, ref ->
                            isProcessing = false
                            if (ok) {
                                purchaseReceipt = PurchaseSuccessReceipt(
                                    serviceTitle = "Airtime Top-Up",
                                    providerOrType = selectedOption,
                                    recipient = effectiveTarget,
                                    amountPaid = discountedPayableVal,
                                    reference = ref ?: "PG-AIR-${(100000..999999).random()}",
                                    newBalance = viewModel.userWalletBalance.value,
                                    message = msg,
                                    bonusVpnTime = "+1 Hour Unlimited VPN Time",
                                    bonusPoints = (amt / 50).toInt().coerceAtLeast(10)
                                )
                            } else {
                                resultMessage = msg
                            }
                        }
                    }
                    "betting" -> {
                        viewModel.fundBettingWallet(selectedOption, recipientInput, amt) { ok, msg, ref ->
                            isProcessing = false
                            if (ok) {
                                purchaseReceipt = PurchaseSuccessReceipt(
                                    serviceTitle = "Betting Wallet ($selectedOption)",
                                    providerOrType = selectedOption,
                                    recipient = recipientInput,
                                    amountPaid = amt,
                                    reference = ref ?: "BET-${(100000..999999).random()}",
                                    newBalance = viewModel.userWalletBalance.value,
                                    message = msg,
                                    bonusVpnTime = "+45 Mins Unlimited VPN Time",
                                    bonusPoints = 15
                                )
                            } else {
                                resultMessage = msg
                            }
                        }
                    }
                    "education" -> {
                        viewModel.buyEducationPin(selectedOption, 1, 3800.0, effectiveTarget) { ok, msg, ref ->
                            isProcessing = false
                            if (ok) {
                                purchaseReceipt = PurchaseSuccessReceipt(
                                    serviceTitle = "$selectedOption Exam PIN",
                                    providerOrType = selectedOption,
                                    recipient = effectiveTarget,
                                    amountPaid = 3800.0,
                                    reference = ref ?: "EDU-${(100000..999999).random()}",
                                    newBalance = viewModel.userWalletBalance.value,
                                    message = msg,
                                    bonusVpnTime = "+2 Hours Unlimited VPN Time",
                                    bonusPoints = 25
                                )
                            } else {
                                resultMessage = msg
                            }
                        }
                    }
                    "cable" -> {
                        viewModel.payPairgateBill(selectedOption, recipientInput, amt.toInt()) { ok, msg, ref ->
                            isProcessing = false
                            if (ok) {
                                purchaseReceipt = PurchaseSuccessReceipt(
                                    serviceTitle = "Cable TV ($selectedOption)",
                                    providerOrType = selectedOption,
                                    recipient = recipientInput,
                                    amountPaid = discountedPayableVal,
                                    reference = ref ?: "PG-CAB-${(100000..999999).random()}",
                                    newBalance = viewModel.userWalletBalance.value,
                                    message = msg,
                                    bonusVpnTime = "+2 Hours Unlimited VPN Time",
                                    bonusPoints = 30
                                )
                            } else {
                                resultMessage = msg
                            }
                        }
                    }
                    "utility" -> {
                        if (amt < 1000.0) {
                            isProcessing = false
                            resultMessage = "Minimum electricity purchase for $selectedOption is ₦1,000 as required by electricity providers."
                            return@executeTransaction
                        }
                        viewModel.payPairgateBill(
                            billerId = selectedOption,
                            customerId = recipientInput.trim(),
                            amountNaira = amt.toInt(),
                            customerName = verifiedAccountName?.substringBefore(" • "),
                            meterNumber = recipientInput.trim(),
                            discoName = selectedOption,
                            meterType = secondaryInput.ifBlank { "PREPAID" }
                        ) { ok, msg, ref ->
                            isProcessing = false
                            if (ok) {
                                val cleanNum = recipientInput.trim()
                                val displayName = verifiedAccountName?.substringBefore(" • ")?.ifBlank { "Meter $cleanNum" } ?: "Meter $cleanNum"
                                viewModel.saveRecipient(
                                    name = displayName,
                                    recipientType = "utility",
                                    identifier = cleanNum,
                                    institutionOrProvider = selectedOption,
                                    bankAccountName = verifiedAccountName?.substringBefore(" • ")
                                )

                                val finalRef = ref ?: "FLOW-BILL-${System.currentTimeMillis()}"
                                val txLog = viewModel.vtuTransactionLogs.value.firstOrNull { it.reference.equals(finalRef, ignoreCase = true) }
                                val liveToken = txLog?.tokenPin
                                val liveUnits = txLog?.meterUnits
                                val isPending = txLog?.status.equals("PENDING", ignoreCase = true) || liveToken.isNullOrBlank()

                                val finalMsg = if (!liveToken.isNullOrBlank()) {
                                    "Token PIN: $liveToken" + (if (!liveUnits.isNullOrBlank()) " • Units: $liveUnits" else "") + "\nEnter this PIN into your meter keypad followed by Enter (↵)."
                                } else if (isPending) {
                                    "Purchase submitted to $selectedOption. Transaction reference is $finalRef. DisCo is processing your token — it will appear once confirmed."
                                } else {
                                    msg
                                }

                                purchaseReceipt = PurchaseSuccessReceipt(
                                    serviceTitle = "Electricity Purchase ($selectedOption)",
                                    providerOrType = selectedOption,
                                    recipient = recipientInput,
                                    amountPaid = discountedPayableVal,
                                    reference = finalRef,
                                    newBalance = viewModel.userWalletBalance.value,
                                    message = finalMsg,
                                    bonusVpnTime = "+2 Hours Unlimited VPN Time",
                                    bonusPoints = 30,
                                    tokenPin = liveToken,
                                    meterUnits = liveUnits,
                                    customerName = verifiedAccountName?.substringBefore(" • "),
                                    meterNumber = cleanNum,
                                    discoName = selectedOption,
                                    meterType = secondaryInput.ifBlank { "PREPAID" }
                                )
                            } else {
                                resultMessage = msg
                            }
                        }
                    }
                    else -> {
                        viewModel.purchasePairgateData(
                            network = selectedOption,
                            planId = selectedDataPlanId.ifBlank { selectedDataPlan },
                            amountNaira = amt,
                            phone = effectiveTarget,
                            category = if (selectedDataPlan.contains("sme", ignoreCase = true)) "SME" else null,
                            planName = selectedDataPlan
                        ) { ok, msg, ref, isPending ->
                            isProcessing = false
                            if (ok) {
                                drawerToast = DrawerToastMessage(
                                    title = if (isPending) "Order Queued" else "Purchase Successful",
                                    message = msg.ifBlank { "Data Bundle activated for $effectiveTarget!" },
                                    type = DrawerToastType.SUCCESS
                                )
                                purchaseReceipt = PurchaseSuccessReceipt(
                                    serviceTitle = "Data Bundle ($selectedDataPlan)",
                                    providerOrType = selectedOption,
                                    recipient = effectiveTarget,
                                    amountPaid = discountedPayableVal,
                                    reference = ref ?: "PG-DAT-${(100000..999999).random()}",
                                    newBalance = viewModel.userWalletBalance.value,
                                    message = msg,
                                    bonusVpnTime = "+2 Hours Unlimited VPN Time",
                                    bonusPoints = 25,
                                    isPending = isPending,
                                    status = if (isPending) "PENDING" else "SUCCESS"
                                )
                            } else {
                                resultMessage = msg
                                drawerToast = DrawerToastMessage(
                                    title = if (msg.contains("too many", true) || msg.contains("rate limit", true)) "Upstream Busy" else "Order Notice",
                                    message = msg,
                                    type = DrawerToastType.ERROR
                                )
                            }
                        }
                    }
                }
            }

            // Primary Button triggers Safety Check Drawer
            Button(
                onClick = {
                    if (!com.example.data.util.NetworkUtils.isNetworkAvailable(context)) {
                        drawerToast = DrawerToastMessage("No network connection. Please check your mobile data or Wi-Fi.", "Offline", DrawerToastType.WARNING)
                        resultMessage = "No network connection. Please check your mobile data or Wi-Fi to proceed with your transaction."
                        return@Button
                    }
                    if (effectiveTarget.isBlank()) {
                        drawerToast = DrawerToastMessage("Please enter the recipient phone number or account ID.", "Input Required", DrawerToastType.WARNING)
                        resultMessage = "Please enter the recipient phone number or account ID."
                        return@Button
                    }
                    showSafetyVerificationDrawer = true
                },
                enabled = !isProcessing,
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (isProcessing) {
                    FlowButtonLoadingLine(color = DarkObsidian, width = 48.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("PROCESSING TRANSACTION...", color = DarkObsidian, fontWeight = FontWeight.Bold)
                } else {
                    Text("REVIEW & PAY ₦${String.format("%,.2f", discountedPayableVal)}", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black, color = DarkObsidian, letterSpacing = 0.5.sp))
                }
            }

            // DIRECT BUY AGAIN BOTTOM SHEET
            directRepurchaseData?.let { rep ->
                DirectDataRepurchaseBottomSheet(
                    purchase = rep,
                    viewModel = viewModel,
                    onDismiss = { directRepurchaseData = null },
                    onSuccess = { receipt ->
                        purchaseReceipt = receipt
                    }
                )
            }

            // SAFETY VERIFICATION DRAWER (Check recipient before final dispatch)
            if (showSafetyVerificationDrawer) {
                ModalBottomSheet(
                    onDismissRequest = { showSafetyVerificationDrawer = false },
                    containerColor = DarkSurfaceElevated,
                    scrimColor = Color.Black.copy(alpha = 0.75f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(GlowingAmber.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("⚠️", fontSize = 20.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "CHECK & CONFIRM RECIPIENT",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        color = TextPrimary,
                                        letterSpacing = 0.5.sp
                                    )
                                )
                                Text(
                                    text = "Verify the target number carefully",
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 12.sp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // High Visibility Warning Banner
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF2D1810),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GlowingAmber.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Alert",
                                    tint = GlowingAmber,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (service.id == "utility") {
                                        "Prepaid meter token PINs are generated for manual entry into your meter keypad. The system cannot recharge meters directly."
                                    } else {
                                        "Orders are dispatched immediately to telco/utility servers and CANNOT be refunded or reversed if sent to an unintended number."
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFFFD59E), fontSize = 12.sp, lineHeight = 16.sp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Prominent Recipient Card
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = DarkSurface,
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, CyberCyan.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "TARGET DESTINATION",
                                        style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = CyberCyan.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = if (effectiveTarget == myPhone && myPhone.isNotEmpty()) "My Primary Number" else "Recipient",
                                            style = MaterialTheme.typography.labelSmall.copy(color = CyberCyan, fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TelcoLogo(network = selectedOption, size = 34.dp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = effectiveTarget,
                                            style = MaterialTheme.typography.headlineSmall.copy(
                                                fontWeight = FontWeight.Black,
                                                fontFamily = FontFamily.Monospace,
                                                color = CyberCyan,
                                                letterSpacing = 1.5.sp
                                            )
                                        )
                                        Text(
                                            text = "${selectedOption.uppercase()} • ${service.title}",
                                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 12.sp)
                                        )
                                    }
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = DarkCardBorder)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Amount / Value", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
                                    Text("₦${String.format("%,.2f", faceValueVal)}", style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, fontWeight = FontWeight.Bold))
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Discounted Payable", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
                                    Text("₦${String.format("%,.2f", discountedPayableVal)}", style = MaterialTheme.typography.bodyMedium.copy(color = ElectricEmerald, fontWeight = FontWeight.Black))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showSafetyVerificationDrawer = false },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
                            ) {
                                Text("✏️ Edit Number", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            Button(
                                onClick = {
                                    showSafetyVerificationDrawer = false
                                    executeTransaction()
                                },
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian)
                            ) {
                                Text("✅ Confirm & Send", fontWeight = FontWeight.Black, fontSize = 13.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            // SETUP & VERIFY PHONE DIALOG (When user doesn't have phone set yet)
            if (showSetupPhoneDialog) {
                PhoneVerificationDialog(
                    viewModel = viewModel,
                    initialPhone = newPhoneInputInSheet.ifBlank { recipientInput },
                    onDismiss = { showSetupPhoneDialog = false },
                    onSuccess = { verifiedPhone ->
                        recipientInput = verifiedPhone
                        newPhoneInputInSheet = verifiedPhone
                        showSetupPhoneDialog = false
                    }
                )
            }

            // SAVED BENEFICIARIES MANAGER MODAL
            if (showSavedRecipientsManager) {
                SavedRecipientsManagerDialog(
                    recipients = savedRecipients,
                    onDismiss = { showSavedRecipientsManager = false },
                    onSaveNewRecipient = { name, type, identifier, provider, bankAccountName, isFavorite ->
                        viewModel.saveRecipient(
                            name = name,
                            recipientType = type,
                            identifier = identifier,
                            institutionOrProvider = provider,
                            bankAccountName = bankAccountName,
                            isFavorite = isFavorite
                        )
                    },
                    onDeleteRecipient = { id -> viewModel.deleteRecipient(id) },
                    onToggleFavorite = { r -> viewModel.toggleFavoriteRecipient(r) },
                    onSelectRecipientForAction = { r ->
                        if (r.institutionOrProvider.isNotBlank()) {
                            selectedOption = r.institutionOrProvider
                        }
                        recipientInput = r.identifier
                        if (r.bankAccountName != null || r.recipientType == "transfer" || r.recipientType == "utility" || r.recipientType == "cable") {
                            verifiedAccountName = r.bankAccountName ?: r.name
                        }
                        showSavedRecipientsManager = false
                    }
                )
            }
        }
        }

        TopDrawerNotificationBanner(
            toast = drawerToast,
            onDismiss = { drawerToast = null },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 8.dp)
                .zIndex(9999f)
        )
    }
    }
}

/**
 * Transaction History Dialog with Search, Category Filtering, and Instant PDF Receipt Export
 */
@Composable
fun TransactionHistoryDialog(
    logs: List<VpnViewModel.VtuTransactionLog>,
    viewModel: VpnViewModel? = null,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("All") }
    var showBeneficiariesManager by remember { mutableStateOf(false) }

    val liveLogs = viewModel?.vtuTransactionLogs?.collectAsStateWithLifecycle()?.value ?: logs
    val effectiveLogs = if (liveLogs.isNotEmpty()) liveLogs else logs

    val isFetching = viewModel?.isFetchingTransactionHistory?.collectAsStateWithLifecycle()?.value ?: false

    LaunchedEffect(Unit) {
        viewModel?.fetchTransactionHistory(forceRemote = false)
    }

    val savedRecipients = if (viewModel != null) {
        viewModel.savedRecipients.collectAsStateWithLifecycle().value
    } else {
        emptyList()
    }

    val categories = listOf("All", "Transfers", "Airtime", "Data", "Utilities", "Deposit")

    val filteredLogs = remember(effectiveLogs, searchQuery, selectedCategoryFilter) {
        effectiveLogs
            .filter { log ->
                !log.type.contains("refund", ignoreCase = true) &&
                !log.id.startsWith("TX-REF-") &&
                !log.reference.startsWith("TX-REF-")
            }
            .filter { log ->
            val matchesCategory = when (selectedCategoryFilter) {
                "All" -> true
                "Transfers" -> log.type.contains("Transfer", ignoreCase = true)
                "Airtime" -> log.type.contains("Airtime", ignoreCase = true)
                "Data" -> log.type.contains("Data", ignoreCase = true)
                "Utilities" -> log.type.contains("Utility", ignoreCase = true) ||
                        log.type.contains("Electricity", ignoreCase = true) ||
                        log.type.contains("Cable", ignoreCase = true) ||
                        log.type.contains("Bill", ignoreCase = true) ||
                        log.type.contains("Power", ignoreCase = true) ||
                        log.type.contains("Meter", ignoreCase = true) ||
                        log.discoName != null || log.tokenPin != null || log.meterNumber != null ||
                        listOf("ikedc", "ekedc", "ibedc", "aedc", "eedc", "bedc", "kedco", "phed", "jed", "kaedco", "yedc").any {
                            log.type.contains(it, ignoreCase = true) || log.recipient.contains(it, ignoreCase = true)
                        }
                "Deposit" -> log.type.contains("Deposit", ignoreCase = true) ||
                        log.type.contains("Fund", ignoreCase = true) ||
                        log.type.contains("Credit", ignoreCase = true)
                else -> true
            }

            val matchesSearch = searchQuery.isBlank() ||
                    log.type.contains(searchQuery, ignoreCase = true) ||
                    log.recipient.contains(searchQuery, ignoreCase = true) ||
                    log.reference.contains(searchQuery, ignoreCase = true) ||
                    log.id.contains(searchQuery, ignoreCase = true) ||
                    log.status.contains(searchQuery, ignoreCase = true) ||
                    (log.meterNumber?.contains(searchQuery, ignoreCase = true) == true) ||
                    (log.customerName?.contains(searchQuery, ignoreCase = true) == true) ||
                    (log.discoName?.contains(searchQuery, ignoreCase = true) == true) ||
                    (log.tokenPin?.contains(searchQuery, ignoreCase = true) == true) ||
                    (log.meterUnits?.contains(searchQuery, ignoreCase = true) == true)

            matchesCategory && matchesSearch
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurfaceElevated,
        shape = RoundedCornerShape(18.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.ReceiptLong, contentDescription = "History", tint = CyberCyan, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("TRANSACTION HISTORY", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, color = TextPrimary, letterSpacing = 0.5.sp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel?.fetchTransactionHistory(forceRemote = true) },
                        enabled = !isFetching
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh History",
                            tint = if (isFetching) CyberCyan.copy(alpha = 0.5f) else CyberCyan
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp)) {
                if (isFetching) {
                    FlowLoadingLine(
                        modifier = Modifier.fillMaxWidth(),
                        color = CyberCyan,
                        trackColor = DarkSurface,
                        height = 3.dp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by recipient, ref, meter, or type...", color = TextMuted, fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", tint = TextMuted, modifier = Modifier.size(14.dp))
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = DarkCardBorder,
                        focusedContainerColor = DarkSurface,
                        unfocusedContainerColor = DarkSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Category Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { category ->
                        val isSelected = selectedCategoryFilter == category
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategoryFilter = category },
                            label = { Text(category, fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyberCyan,
                                selectedLabelColor = DarkObsidian,
                                containerColor = DarkSurface,
                                labelColor = TextSecondary
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (filteredLogs.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = TextMuted.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            if (searchQuery.isBlank()) "No transaction history records found."
                            else "No transactions matching \"$searchQuery\".",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel?.fetchTransactionHistory(forceRemote = true) },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f))
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Refresh History", color = CyberCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(filteredLogs) { log ->
                            val refToUse = log.reference.ifBlank { log.id }

                            // Build receipt data model for PDF generator
                            val receiptData = remember(log) {
                                val isCredit = log.type.contains("Deposit", ignoreCase = true) || log.type.contains("Fund", ignoreCase = true)
                                val txType = when {
                                    log.type.contains("Transfer", ignoreCase = true) -> "Bank Transfer"
                                    log.type.contains("Airtime", ignoreCase = true) -> "Airtime Recharge"
                                    log.type.contains("Data", ignoreCase = true) -> "Data Bundle"
                                    log.type.contains("Electricity", ignoreCase = true) || log.type.contains("Utility", ignoreCase = true) -> "Electricity Bill"
                                    log.type.contains("Cable", ignoreCase = true) -> "Cable TV Sub"
                                    isCredit -> "Wallet Top-up"
                                    else -> log.type
                                }

                                TransactionReceiptData(
                                    transactionId = refToUse,
                                    reference = refToUse,
                                    title = txType,
                                    serviceType = log.type,
                                    recipient = log.recipient.ifBlank { "Self / FlowTest Wallet" },
                                    amountPaid = log.amountNaira,
                                    dateFormatted = log.timestamp,
                                    senderName = "FlowTest Client",
                                    senderAccount = "FlowTest Wallet (6666468328)",
                                    companyName = "FlowTest",
                                    status = if (log.status.isBlank()) "SUCCESSFUL" else log.status.uppercase(),
                                    balanceBefore = if (log.balanceBefore > 0) log.balanceBefore else null,
                                    balanceAfter = log.balanceAfter,
                                    narration = "Service transaction processed on FlowTest Secure Infrastructure.",
                                    tokenPin = log.tokenPin,
                                    meterUnits = log.meterUnits,
                                    customerName = log.customerName,
                                    meterNumber = log.meterNumber,
                                    serviceAddress = log.serviceAddress,
                                    discoName = log.discoName,
                                    meterType = log.meterType,
                                    tariffClass = log.tariffClass
                                )
                            }

                            Card(
                                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                                    // Row 1: Type + Status + Amount
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f, fill = false)
                                        ) {
                                            val categoryIcon = when {
                                                log.type.contains("Deposit", ignoreCase = true) || log.type.contains("Fund", ignoreCase = true) -> Icons.Default.AccountBalanceWallet
                                                log.type.contains("Airtime", ignoreCase = true) -> Icons.Default.PhoneAndroid
                                                log.type.contains("Data", ignoreCase = true) -> Icons.Default.Wifi
                                                log.type.contains("Utility", ignoreCase = true) || log.type.contains("Electricity", ignoreCase = true) -> Icons.Default.FlashOn
                                                log.type.contains("Cable", ignoreCase = true) -> Icons.Default.Tv
                                                else -> Icons.Default.SwapHoriz
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .size(26.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(
                                                        if (log.type.contains("Deposit", ignoreCase = true) || log.type.contains("Fund", ignoreCase = true))
                                                            ElectricEmerald.copy(alpha = 0.15f)
                                                        else CyberCyan.copy(alpha = 0.15f)
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = categoryIcon,
                                                    contentDescription = null,
                                                    tint = if (log.type.contains("Deposit", ignoreCase = true) || log.type.contains("Fund", ignoreCase = true))
                                                        ElectricEmerald else CyberCyan,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = log.type,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            val displayStatus = when {
                                                log.status.contains("success", ignoreCase = true) -> "SUCCESSFUL"
                                                log.status.contains("pend", ignoreCase = true) || log.status.contains("process", ignoreCase = true) -> "PENDING"
                                                else -> "FAILED"
                                            }
                                            val statusBadgeColor = when (displayStatus) {
                                                "SUCCESSFUL" -> ElectricEmerald
                                                "PENDING" -> GlowingAmber
                                                else -> CoralRed
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = statusBadgeColor.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = displayStatus,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = statusBadgeColor,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 9.sp
                                                    ),
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Text(
                                            text = "₦${String.format("%,.2f", log.amountNaira)}",
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Black,
                                                color = if (log.type.contains("Deposit", ignoreCase = true) || log.type.contains("Fund", ignoreCase = true)) ElectricEmerald else CyberCyan
                                            )
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Row 2: Recipient / User Details (Cleanly structured with parsing for long text)
                                    if (log.recipient.isNotBlank()) {
                                        val (primaryName, secondaryDetail) = remember(log.recipient) {
                                            val raw = log.recipient.trim()
                                            val parenStart = raw.indexOf('(')
                                            val parenEnd = raw.lastIndexOf(')')
                                            if (parenStart != -1 && parenEnd > parenStart) {
                                                val name = raw.substring(0, parenStart).trim()
                                                val details = raw.substring(parenStart + 1, parenEnd).trim()
                                                (name.ifBlank { "Customer" }) to details
                                            } else if (raw.contains(" - ")) {
                                                val parts = raw.split(" - ", limit = 2)
                                                parts[0].trim() to parts[1].trim()
                                            } else {
                                                raw to ""
                                            }
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = DarkSurfaceElevated,
                                            border = androidx.compose.foundation.BorderStroke(0.5.dp, DarkCardBorder),
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Person,
                                                        contentDescription = null,
                                                        tint = CyberCyan,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(5.dp))
                                                    Text(
                                                        text = "BENEFICIARY / DETAILS",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            color = TextMuted,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            letterSpacing = 0.5.sp
                                                        )
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(3.dp))
                                                Text(
                                                    text = primaryName,
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        color = TextPrimary,
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 12.5.sp
                                                    ),
                                                    maxLines = 2,
                                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                )
                                                if (secondaryDetail.isNotBlank()) {
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = secondaryDetail,
                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                            color = CyberCyan.copy(alpha = 0.85f),
                                                            fontFamily = FontFamily.Monospace,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Utility Premises & Proof of Address Details (if available)
                                    if (!log.customerName.isNullOrBlank() || !log.serviceAddress.isNullOrBlank() || !log.meterNumber.isNullOrBlank()) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF041913),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, ElectricEmerald.copy(alpha = 0.4f)),
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            imageVector = Icons.Default.Verified,
                                                            contentDescription = null,
                                                            tint = ElectricEmerald,
                                                            modifier = Modifier.size(13.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = "METER & SERVICE DETAILS",
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                color = ElectricEmerald,
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Black,
                                                                letterSpacing = 0.5.sp
                                                            )
                                                        )
                                                    }
                                                    Text(
                                                        text = "OFFICIAL RECORD",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            color = ElectricEmerald.copy(alpha = 0.8f),
                                                            fontSize = 8.5.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    )
                                                }

                                                if (!log.customerName.isNullOrBlank()) {
                                                    Spacer(modifier = Modifier.height(3.dp))
                                                    Text(
                                                        text = "Name: ${log.customerName}",
                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                            color = Color.White,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 11.sp
                                                        )
                                                    )
                                                }

                                                val meterToDisplay = log.meterNumber ?: log.recipient
                                                if (meterToDisplay.isNotBlank()) {
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = "Meter/Account: $meterToDisplay",
                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                            color = CyberCyan,
                                                            fontFamily = FontFamily.Monospace,
                                                            fontWeight = FontWeight.SemiBold,
                                                            fontSize = 10.5.sp
                                                        )
                                                    )
                                                }

                                                if (!log.serviceAddress.isNullOrBlank()) {
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = "Address: ${log.serviceAddress}",
                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                            color = Color(0xFFCBD5E1),
                                                            fontSize = 10.5.sp,
                                                            lineHeight = 13.sp
                                                        )
                                                    )
                                                }

                                                if (!log.discoName.isNullOrBlank() || !log.tariffClass.isNullOrBlank()) {
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = "DISCO: ${log.discoName ?: ""} ${if (!log.tariffClass.isNullOrBlank()) "• ${log.tariffClass}" else ""}".trim(),
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            color = TextMuted,
                                                            fontSize = 9.5.sp
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Token PIN Display for Electricity Prepaid Tokens in History
                                    if (!log.tokenPin.isNullOrBlank()) {
                                        var isPinCopiedInHistory by remember { mutableStateOf(false) }
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF031410),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, ElectricEmerald),
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f, fill = false)) {
                                                    Text(
                                                        text = "⚡ PREPAID TOKEN PIN",
                                                        color = ElectricEmerald,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Black
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = log.tokenPin,
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Black,
                                                        fontFamily = FontFamily.Monospace,
                                                        fontSize = 12.sp,
                                                        letterSpacing = 1.sp
                                                    )
                                                }
                                                IconButton(
                                                    onClick = {
                                                        clipboardManager.setText(AnnotatedString(log.tokenPin))
                                                        isPinCopiedInHistory = true
                                                        Toast.makeText(context, "Token PIN copied!", Toast.LENGTH_SHORT).show()
                                                    },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = if (isPinCopiedInHistory) Icons.Default.Check else Icons.Default.ContentCopy,
                                                        contentDescription = "Copy Token",
                                                        tint = ElectricEmerald,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Row 3: Balance Before & Balance After
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = DarkSurfaceElevated,
                                            border = androidx.compose.foundation.BorderStroke(0.5.dp, DarkCardBorder)
                                        ) {
                                            Text(
                                                text = "Before: ₦${String.format("%,.2f", log.balanceBefore)}",
                                                style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 10.sp),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }

                                        Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null, tint = TextMuted, modifier = Modifier.size(12.dp))

                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = CyberCyan.copy(alpha = 0.12f),
                                            border = androidx.compose.foundation.BorderStroke(0.5.dp, CyberCyan.copy(alpha = 0.3f))
                                        ) {
                                            Text(
                                                text = "After: ₦${String.format("%,.2f", log.balanceAfter)}",
                                                style = MaterialTheme.typography.labelSmall.copy(color = CyberCyan, fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    // Pending Status & Refund Controls / Refund Notification Banner
                                    if (log.status.contains("pending", ignoreCase = true) || log.status.contains("processing", ignoreCase = true)) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = GlowingAmber.copy(alpha = 0.12f),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, GlowingAmber.copy(alpha = 0.4f)),
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(imageVector = Icons.Default.HourglassEmpty, contentDescription = null, tint = GlowingAmber, modifier = Modifier.size(13.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("PENDING TELCO SWITCH", color = GlowingAmber, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                    Text("Auto-Refund Protected", color = ElectricEmerald, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                                                }
                                                Spacer(modifier = Modifier.height(6.dp))
                                                var isChecking by remember { mutableStateOf(false) }
                                                Button(
                                                    onClick = {
                                                        if (viewModel != null && !isChecking) {
                                                            isChecking = true
                                                            viewModel.queryAndEffectPairgateStatus(refToUse) { status, msg, _ ->
                                                                isChecking = false
                                                                val normStatus = when {
                                                                    status.contains("success", ignoreCase = true) -> "SUCCESSFUL"
                                                                    status.contains("pend", ignoreCase = true) || status.contains("process", ignoreCase = true) -> "PENDING"
                                                                    else -> "FAILED"
                                                                }
                                                                val cleanMsg = msg
                                                                    .replace(Regex("""(?i)pairgate\s*:\s*"""), "")
                                                                    .replace(Regex("""(?i)pairgate"""), "Carrier")
                                                                    .trim()
                                                                Toast.makeText(context, "Status: $normStatus • $cleanMsg", Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan.copy(alpha = 0.2f), contentColor = CyberCyan),
                                                    shape = RoundedCornerShape(6.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    modifier = Modifier.fillMaxWidth().height(30.dp)
                                                ) {
                                                    if (isChecking) {
                                                        FlowButtonLoadingLine(color = CyberCyan, width = 20.dp, height = 2.dp)
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                    } else {
                                                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(11.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                    }
                                                    Text("Check Status", fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Row 4: Transaction ID & Purchase Time with copy button
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .weight(1f, fill = false)
                                                .clip(RoundedCornerShape(6.dp))
                                                .clickable {
                                                    clipboardManager.setText(AnnotatedString(refToUse))
                                                    Toast.makeText(context, "ID copied: $refToUse", Toast.LENGTH_SHORT).show()
                                                }
                                        ) {
                                            Text(
                                                text = "ID: $refToUse",
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 10.sp,
                                                    color = TextMuted
                                                ),
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "Copy ID",
                                                tint = TextMuted,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Text(
                                            text = log.timestamp,
                                            maxLines = 1,
                                            style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 10.sp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Row 5: Action Buttons (Share PDF Receipt, Download PDF Receipt, Save Beneficiary)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Save Beneficiary button if recipient exists
                                        if (log.recipient.isNotBlank() && viewModel != null) {
                                            val isAlreadySaved = savedRecipients.any { it.identifier.trim() == log.recipient.trim() }
                                            if (!isAlreadySaved) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = GlowingAmber.copy(alpha = 0.12f),
                                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, GlowingAmber.copy(alpha = 0.4f)),
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .clickable {
                                                            val inferredType = when {
                                                                log.type.contains("Airtime", ignoreCase = true) -> "airtime"
                                                                log.type.contains("Data", ignoreCase = true) -> "data"
                                                                log.type.contains("Transfer", ignoreCase = true) -> "transfer"
                                                                log.type.contains("Electricity", ignoreCase = true) || log.type.contains("Utility", ignoreCase = true) -> "utility"
                                                                log.type.contains("Cable", ignoreCase = true) -> "cable"
                                                                else -> "transfer"
                                                            }
                                                            viewModel.saveRecipient(
                                                                name = "Beneficiary ${log.recipient.takeLast(4)}",
                                                                recipientType = inferredType,
                                                                identifier = log.recipient,
                                                                institutionOrProvider = if (log.type.contains("MTN", ignoreCase = true)) "MTN" else if (log.type.contains("Airtel", ignoreCase = true)) "Airtel" else "FlowTest"
                                                            )
                                                            Toast.makeText(context, "Saved to Beneficiaries!", Toast.LENGTH_SHORT).show()
                                                        }
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(imageVector = if (log.type.contains("Electricity", ignoreCase = true) || log.type.contains("Utility", ignoreCase = true)) Icons.Default.ElectricMeter else Icons.Default.PersonAdd, contentDescription = null, tint = GlowingAmber, modifier = Modifier.size(12.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        val isElectric = log.type.contains("Electricity", ignoreCase = true) || log.type.contains("Utility", ignoreCase = true)
                                                        Text(if (isElectric) "Save Meter" else "Save Contact", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GlowingAmber)
                                                    }
                                                }

                                                Spacer(modifier = Modifier.width(6.dp))
                                            }
                                        }

                                        // Download PDF
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = DarkSurfaceElevated,
                                            border = androidx.compose.foundation.BorderStroke(0.5.dp, DarkCardBorder),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .clickable {
                                                    PdfReceiptGenerator.downloadReceiptPdf(context, receiptData)
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(imageVector = Icons.Default.Download, contentDescription = "Download PDF", tint = CyberCyan, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Download PDF", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(6.dp))

                                        // Share PDF
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = CyberCyan.copy(alpha = 0.15f),
                                            border = androidx.compose.foundation.BorderStroke(0.5.dp, CyberCyan.copy(alpha = 0.4f)),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .clickable {
                                                    PdfReceiptGenerator.shareReceiptPdf(context, receiptData)
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(imageVector = Icons.Default.Share, contentDescription = "Share PDF", tint = CyberCyan, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Share PDF", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Saved Beneficiaries Manager modal
            if (showBeneficiariesManager && viewModel != null) {
                SavedRecipientsManagerDialog(
                    recipients = savedRecipients,
                    onDismiss = { showBeneficiariesManager = false },
                    onSaveNewRecipient = { name, type, identifier, provider, bankAccountName, isFavorite ->
                        viewModel.saveRecipient(
                            name = name,
                            recipientType = type,
                            identifier = identifier,
                            institutionOrProvider = provider,
                            bankAccountName = bankAccountName,
                            isFavorite = isFavorite
                        )
                    },
                    onDeleteRecipient = { id -> viewModel.deleteRecipient(id) },
                    onToggleFavorite = { r -> viewModel.toggleFavoriteRecipient(r) }
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (viewModel != null) {
                    OutlinedButton(
                        onClick = { showBeneficiariesManager = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Bookmarks, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Beneficiaries (${savedRecipients.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            }
        }
    )
}
