package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.launch

data class OnboardingPageData(
    val title: String,
    val subtitle: String,
    val badge: String,
    val icon: ImageVector,
    val iconBgColor: Color,
    val featureList: List<String>
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onFinishOnboarding: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    val pages = listOf(
        OnboardingPageData(
            title = "Military-Grade Encrypted VPN",
            subtitle = "Bypass firewalls, protect your privacy, and access high-speed global proxy nodes with zero logs.",
            badge = "SECURE & ANONYMOUS",
            icon = Icons.Default.Shield,
            iconBgColor = CyberCyan,
            featureList = listOf("WireGuard & OpenVPN Protocols", "Automatic Kill-Switch & Anti-DNS Leak", "Unlimited Bandwidth & Ultra-Low Ping")
        ),
        OnboardingPageData(
            title = "Official Company Bank Transfer",
            subtitle = "Fund your wallet instantly by transferring to the official Company Bank Account with your registered phone number as narration.",
            badge = "INSTANT DEPOSITS",
            icon = Icons.Default.AccountBalance,
            iconBgColor = ElectricEmerald,
            featureList = listOf("Direct Bank Transfer to Company Account", "Automated Wallet Credit via Webhook", "Zero Hidden Deposit Charges")
        ),
        OnboardingPageData(
            title = "Cheapest VTU Airtime & Data Store",
            subtitle = "Purchase cheap SME & Corporate Data bundles, Airtime VTU, Electricity bills, and Cable TV subscriptions directly.",
            badge = "VTU DATA & AIRTIME",
            icon = Icons.Default.Bolt,
            iconBgColor = Color(0xFFFFB74D),
            featureList = listOf("MTN 1GB from ₦260 (Instant Top-up)", "Airtel, Glo & 9mobile CG Data", "Electricity Prepaid & Cable Subscriptions")
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkObsidian)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Bar (Skip Button)
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
                        .background(CyberCyan.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.VpnKey, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "FLOWTEST",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, color = TextPrimary)
                )
            }

            TextButton(onClick = onFinishOnboarding) {
                Text(
                    text = "Skip to Login",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = TextMuted)
                )
            }
        }

        // Pager Content
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 16.dp)
        ) { page ->
            val pageData = pages[page]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Hero Icon Banner
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceElevated)
                        .border(2.dp, CyberCyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = pageData.icon,
                        contentDescription = pageData.title,
                        tint = pageData.iconBgColor,
                        modifier = Modifier.size(60.dp)
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = pageData.iconBgColor.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, pageData.iconBgColor.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = pageData.badge,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            color = pageData.iconBgColor,
                            letterSpacing = 1.2.sp
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = pageData.title,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = pageData.subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Feature Highlights List
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, GlassBorder, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        pageData.featureList.forEach { feat ->
                            Row(
                                modifier = Modifier.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = pageData.iconBgColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = feat,
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Footer Actions & Dots Indicator
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Page Indicator Dots
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                repeat(pages.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(8.dp)
                            .width(if (isSelected) 24.dp else 8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isSelected) CyberCyan else DarkCardBorder)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (pagerState.currentPage < pages.size - 1) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("NEXT", fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = {
                        if (pagerState.currentPage < pages.size - 1) {
                            onNavigateToRegister()
                        } else {
                            onNavigateToRegister()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(if (pagerState.currentPage < pages.size - 1) 1f else 2f)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("GET STARTED", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black, color = DarkObsidian))
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = DarkObsidian, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
