package com.example.ui.components.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
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

@Composable
fun AdminPricingTab(
    viewModel: VpnViewModel,
    modifier: Modifier = Modifier
) {
    val markupPercent by viewModel.vtuMarkupPercent.collectAsStateWithLifecycle()
    val serviceDiscounts by viewModel.serviceDiscounts.collectAsStateWithLifecycle()
    val serviceMarkups by viewModel.serviceMarkups.collectAsStateWithLifecycle()
    val comboPacks by viewModel.comboPacks.collectAsStateWithLifecycle()
    val cashbackRatePercent by viewModel.cashbackRatePercent.collectAsStateWithLifecycle()
    val cashbackRateAirtime by viewModel.cashbackRateAirtimePercent.collectAsStateWithLifecycle()
    val cashbackRateData by viewModel.cashbackRateDataPercent.collectAsStateWithLifecycle()
    val cashbackRateBills by viewModel.cashbackRateBillsPercent.collectAsStateWithLifecycle()
    val referralNaira by viewModel.referralCommissionNaira.collectAsStateWithLifecycle()
    val referralPct by viewModel.referralCommissionPercent.collectAsStateWithLifecycle()
    val referralBonusPts by viewModel.referralBonusPoints.collectAsStateWithLifecycle()
    val ptsEarnRate by viewModel.pointsEarnRatePerHundredNaira.collectAsStateWithLifecycle()
    val ptsRedeemRate by viewModel.pointsRedemptionRateNairaPer100Pts.collectAsStateWithLifecycle()
    val minProfitBuffer by viewModel.adminMinProfitBufferPercent.collectAsStateWithLifecycle()
    val dataPricingStrategy by viewModel.dataPricingStrategy.collectAsStateWithLifecycle()
    val telcoDiscountPercent by viewModel.telcoDiscountPercent.collectAsStateWithLifecycle()

    val smsWholesale by viewModel.smsWholesaleCost.collectAsStateWithLifecycle()
    val smsRetail by viewModel.smsRetailPrice.collectAsStateWithLifecycle()
    val smsMarkup by viewModel.adminSmsMarkupPercent.collectAsStateWithLifecycle()
    val smsSenderId by viewModel.smsSenderId.collectAsStateWithLifecycle()

    val redemptionActivities by viewModel.redemptionActivities.collectAsStateWithLifecycle()
    val referralMinProfitThreshold by viewModel.referralMinProfitThreshold.collectAsStateWithLifecycle()

    var showEditConfigDialog by remember { mutableStateOf(false) }
    var optimizationSuccessNotice by remember { mutableStateOf(false) }
    var editingActivity by remember { mutableStateOf<RedemptionActivity?>(null) }
    var showAddActivityDialog by remember { mutableStateOf(false) }

    val estNetProfitAirtime = markupPercent - cashbackRateAirtime - (referralPct * 0.5)
    val estNetProfitData = markupPercent - cashbackRateData - (referralPct * 0.5)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Reseller Profit Margin Guard & Auto-Optimizer
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, if (estNetProfitAirtime >= minProfitBuffer && estNetProfitData >= minProfitBuffer) ElectricEmerald else GlowingAmber, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.TrendingUp, contentDescription = "Profit Margin", tint = ElectricEmerald)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PROFITABILITY MARGIN GUARD",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                        )
                    }

                    Surface(
                        color = if (estNetProfitData >= 1.0) ElectricEmerald.copy(alpha = 0.2f) else GlowingAmber.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (estNetProfitData >= 1.0) "✓ PROFIT PROTECTED" else "⚠️ TIGHT MARGIN",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (estNetProfitData >= 1.0) ElectricEmerald else GlowingAmber,
                                fontWeight = FontWeight.Black,
                                fontSize = 9.sp
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Profit Safeguard Matrix Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = DarkSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Airtime Net Profit", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 10.sp))
                            Text(
                                text = "+${String.format("%.1f", estNetProfitAirtime)}%",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = if (estNetProfitAirtime >= 1.0) ElectricEmerald else GlowingAmber
                                )
                            )
                            Text("After ${cashbackRateAirtime}% cashback", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 9.sp))
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = DarkSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Data Net Profit", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 10.sp))
                            Text(
                                text = "+${String.format("%.1f", estNetProfitData)}%",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = if (estNetProfitData >= 1.0) ElectricEmerald else GlowingAmber
                                )
                            )
                            Text("After ${cashbackRateData}% cashback", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 9.sp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Auto-Optimize Button
                Button(
                    onClick = {
                        viewModel.optimizeProfitMargins()
                        optimizationSuccessNotice = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("optimize_profit_margins_btn")
                ) {
                    Icon(imageVector = Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "AUTO-OPTIMIZE PROFIT SAFEGUARD",
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp
                    )
                }

                if (optimizationSuccessNotice) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "✓ Margins auto-aligned! Retail markup set to +3.5%, Data Cashback at 1.5%, Airtime at 1.0%, guaranteeing solid profit.",
                        style = MaterialTheme.typography.bodySmall.copy(color = ElectricEmerald, fontSize = 10.sp, textAlign = TextAlign.Center),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // 🛡️ VPNRESELLERS TIERED PRICING & WHOLESALE MARGINS
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, CyberCyan.copy(alpha = 0.6f), RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.VpnKey, contentDescription = null, tint = CyberCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "VPNRESELLERS WHOLESALE & MARGINS",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                        )
                    }

                    Surface(
                        color = ElectricEmerald.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "56% - 70% MARGIN",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = ElectricEmerald,
                                fontWeight = FontWeight.Black,
                                fontSize = 9.sp
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Wholesale benchmark: ~$1.00 - $1.99 USD (~₦1,500 - ₦3,000 NGN) per active user/mo. Built with WireGuard connectionless roaming for unstable MTN / Airtel / Glo switching.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "MODEL A: TIME-BASED SUBSCRIPTIONS (MONTHLY/WEEKLY)",
                    style = MaterialTheme.typography.labelSmall.copy(color = CyberCyan, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                )
                Spacer(modifier = Modifier.height(6.dp))

                listOf(
                    Triple("1-Week Pass (Popular Short-Term)", "Retail: ₦1,500 • Wholesale: ~₦600", "+60% Margin (₦900 Profit)"),
                    Triple("1-Month Standard (1-2 Devices)", "Retail: ₦5,000 • Wholesale: ~₦2,200", "+56% Margin (₦2,800 Profit)"),
                    Triple("1-Month Premium VIP (Up to 5 Devs)", "Retail: ₦7,500 • Wholesale: ~₦3,000", "+60% Margin (₦4,500 Profit)")
                ).forEach { (plan, cost, margin) ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DarkSurface,
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, DarkCardBorder),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(plan, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 11.sp))
                                Text(cost, style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 10.sp))
                            }
                            Text(margin, style = MaterialTheme.typography.labelSmall.copy(color = ElectricEmerald, fontWeight = FontWeight.Black, fontSize = 10.sp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "MODEL B: BOUGHT TIME & PAY-PER-GB PACKAGES",
                    style = MaterialTheme.typography.labelSmall.copy(color = GlowingAmber, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                )
                Spacer(modifier = Modifier.height(6.dp))

                listOf(
                    Triple("24-Hour Continuous Uptime", "Retail: ₦500 • Wholesale: ~₦150", "+70% Margin (₦350 Profit)"),
                    Triple("10 GB Secure Data Pass", "Retail: ₦800 • Wholesale: ~₦300", "+62% Margin (₦500 Profit)"),
                    Triple("25 GB Ultra Stream Pass", "Retail: ₦1,800 • Wholesale: ~₦700", "+61% Margin (₦1,100 Profit)"),
                    Triple("50 GB Power Downloader", "Retail: ₦3,200 • Wholesale: ~₦1,200", "+62% Margin (₦2,000 Profit)")
                ).forEach { (plan, cost, margin) ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DarkSurface,
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, DarkCardBorder),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(plan, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 11.sp))
                                Text(cost, style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 10.sp))
                            }
                            Text(margin, style = MaterialTheme.typography.labelSmall.copy(color = ElectricEmerald, fontWeight = FontWeight.Black, fontSize = 10.sp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = CyberCyan.copy(alpha = 0.1f),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, CyberCyan.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Public, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Localized Endpoints: 🇬🇧 UK London (78ms) • 🇿🇦 South Africa (65ms) • 🇺🇸 US New York (112ms) • 🇦🇪 UAE Dubai (98ms). Primary Protocol: WireGuard.",
                            style = MaterialTheme.typography.bodySmall.copy(color = CyberCyan, fontSize = 10.sp)
                        )
                    }
                }
            }
        }

        // 2. Data Pricing Strategy & Smart Telco Cap Control
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.PriceCheck, contentDescription = null, tint = CyberCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "DATA PRICING & MARKET STRATEGY",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                        )
                    }

                    Surface(
                        color = CyberCyan.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = when (dataPricingStrategy) {
                                "SMART_TELCO_CAP" -> "PARITY PROTECTED"
                                "COMPETITIVE_DISCOUNT" -> "SELL LOWER"
                                else -> "FLAT MARKUP"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = CyberCyan,
                                fontWeight = FontWeight.Black,
                                fontSize = 9.sp
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Controls how wholesale bundles from Pairgate are priced for end users compared to official MTN/Airtel market rates.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Strategy selector
                listOf(
                    Triple(
                        "SMART_TELCO_CAP",
                        "🎯 Smart Telco Parity (Recommended)",
                        "Official bundles (e.g. ₦1,800 MTN 7GB) strictly capped at ₦1,800 face value. Never overcharges users. SME bundles get +${markupPercent}% markup."
                    ),
                    Triple(
                        "COMPETITIVE_DISCOUNT",
                        "⚡ Sell Lower Than Telco",
                        "Sell direct bundles 1% - 2% below official telco price to beat the market (e.g. ₦1,800 sells for ₦1,780). Attracts massive volume."
                    ),
                    Triple(
                        "FLAT_MARKUP",
                        "🏷️ Flat Markup (+${markupPercent}%)",
                        "Applies +${markupPercent}% markup indiscriminately. May make direct retail bundles cost more than official telco platform."
                    )
                ).forEach { (stratKey, title, desc) ->
                    val isSelected = dataPricingStrategy == stratKey
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) Color(0xFF0D283E) else DarkSurface,
                        border = androidx.compose.foundation.BorderStroke(
                            if (isSelected) 1.5.dp else 1.dp,
                            if (isSelected) CyberCyan else DarkCardBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { viewModel.updateDataPricingStrategy(stratKey) }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = if (isSelected) CyberCyan else TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { viewModel.updateDataPricingStrategy(stratKey) },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = CyberCyan,
                                        unselectedColor = TextMuted
                                    ),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 10.sp)
                            )
                        }
                    }
                }

                if (dataPricingStrategy == "COMPETITIVE_DISCOUNT") {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Discount Below Telco Retail:",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(0.0, 0.5, 1.0, 1.5, 2.0).forEach { disc ->
                            val isSel = telcoDiscountPercent == disc
                            FilterChip(
                                selected = isSel,
                                onClick = { viewModel.updateTelcoDiscountPercent(disc) },
                                label = { Text(if (disc == 0.0) "0% OFF" else "-${disc}%", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricEmerald,
                                    selectedLabelColor = DarkObsidian,
                                    containerColor = DarkSurface,
                                    labelColor = TextPrimary
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // SME Markup Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("SME / CG Data Markup:", style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, fontWeight = FontWeight.Bold))
                        Text("Applied to wholesale corporate data", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 9.sp))
                    }
                    Text("+$markupPercent%", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, color = ElectricEmerald))
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(1.5, 2.5, 3.5, 5.0, 7.5).forEach { m ->
                        val isSelected = markupPercent == m
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.updateVtuMarkupPercent(m) },
                            label = { Text("+$m%") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricEmerald,
                                selectedLabelColor = DarkObsidian,
                                containerColor = DarkSurface,
                                labelColor = TextPrimary
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 3. Telco Market Price vs Pairgate Analysis Table Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, DarkCardBorder, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.CompareArrows, contentDescription = null, tint = GlowingAmber)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "MARKET PRICE & PROFIT BENCHMARK",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Live comparison of wholesale Pairgate rates vs official MTN retail prices and your active client selling price:",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Comparison Table Items
                val samplePlans = listOf(
                    TelcoComparisonItem(
                        name = "MTN 7GB (7-Day / 2-Day)",
                        type = "Direct Gifting",
                        wholesale = 1800.0,
                        officialTelco = 1800.0,
                        oldBuggyRetail = 1865.0,
                        isOfficialDenom = true
                    ),
                    TelcoComparisonItem(
                        name = "MTN 1.5GB (7-Day)",
                        type = "Direct Gifting",
                        wholesale = 1000.0,
                        officialTelco = 1000.0,
                        oldBuggyRetail = 1035.0,
                        isOfficialDenom = true
                    ),
                    TelcoComparisonItem(
                        name = "MTN 3.5GB (7-Day)",
                        type = "Direct Gifting",
                        wholesale = 1500.0,
                        officialTelco = 1500.0,
                        oldBuggyRetail = 1555.0,
                        isOfficialDenom = true
                    ),
                    TelcoComparisonItem(
                        name = "MTN 1GB SME (30-Day)",
                        type = "SME Wholesale",
                        wholesale = 285.0,
                        officialTelco = 1000.0,
                        oldBuggyRetail = 295.0,
                        isOfficialDenom = false
                    ),
                    TelcoComparisonItem(
                        name = "MTN 2GB SME (30-Day)",
                        type = "SME Wholesale",
                        wholesale = 570.0,
                        officialTelco = 1500.0,
                        oldBuggyRetail = 590.0,
                        isOfficialDenom = false
                    ),
                    TelcoComparisonItem(
                        name = "MTN 5GB SME (30-Day)",
                        type = "SME Wholesale",
                        wholesale = 1425.0,
                        officialTelco = 2500.0,
                        oldBuggyRetail = 1475.0,
                        isOfficialDenom = false
                    )
                )

                samplePlans.forEach { item ->
                    val retailPrice = com.example.data.api.PairgateVerifiedPlans.getRetailPrice(
                        wholesale = item.wholesale,
                        markupPercent = markupPercent,
                        isGiftingOrDirect = item.isOfficialDenom,
                        pricingStrategy = dataPricingStrategy,
                        discountPercent = telcoDiscountPercent
                    )

                    val isCheaperThanTelco = retailPrice < item.officialTelco
                    val isParityWithTelco = retailPrice == item.officialTelco

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = DarkSurface,
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, DarkCardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Text(
                                        text = "${item.type} • Pairgate Wholesale: ₦${item.wholesale.toInt()} • MTN: ₦${item.officialTelco.toInt()}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = TextSecondary,
                                            fontSize = 9.sp
                                        )
                                    )
                                }

                                Surface(
                                    color = when {
                                        isCheaperThanTelco -> ElectricEmerald.copy(alpha = 0.2f)
                                        isParityWithTelco -> CyberCyan.copy(alpha = 0.2f)
                                        else -> GlowingAmber.copy(alpha = 0.2f)
                                    },
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = when {
                                            isCheaperThanTelco -> "⚡ ₦${retailPrice.toInt()} (-₦${(item.officialTelco - retailPrice).toInt()})"
                                            isParityWithTelco -> "🎯 ₦${retailPrice.toInt()} (PARITY)"
                                            else -> "₦${retailPrice.toInt()}"
                                        },
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = when {
                                                isCheaperThanTelco -> ElectricEmerald
                                                isParityWithTelco -> CyberCyan
                                                else -> GlowingAmber
                                            },
                                            fontWeight = FontWeight.Black,
                                            fontSize = 9.sp
                                        ),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            if (item.isOfficialDenom && retailPrice <= item.officialTelco) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "✓ Overcharge eliminated! Previously showed ₦${item.oldBuggyRetail.toInt()}. Now matched/discounted.",
                                    style = MaterialTheme.typography.labelSmall.copy(color = ElectricEmerald, fontSize = 8.5.sp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Explanatory breakdown callout
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E1738),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GlowingAmber.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = GlowingAmber, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "HOW RESELLER PROFIT & MARKUPS WORK",
                                style = MaterialTheme.typography.labelSmall.copy(color = GlowingAmber, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "• Official Gifting (e.g. ₦1,800 MTN 7GB): Pairgate sells direct plans at telco face value (₦1,800). The app's previous blanket +3.5% formula added +₦65, causing the ₦1,865 overcharge. With Smart Parity, it is strictly capped at ₦1,800. Profit is obtained through wholesale provider cashback/rebates without taxing the user.\n• SME / Corporate Data (e.g. ₦285 for 1GB): You buy for ₦285 wholesale and sell for ₦295 (+₦10 net margin). The user still saves over 70% compared to MTN's official ₦1,000 price!",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, fontSize = 9.5.sp, lineHeight = 13.sp)
                        )
                    }
                }
            }
        }

        // 3. Admin Rewards, Referral & Cashback Suite Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, GlowingAmber.copy(alpha = 0.4f), RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.CardGiftcard, contentDescription = "Cashback", tint = GlowingAmber)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "REWARDS & REFERRAL SYSTEM CONFIG",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                        )
                    }

                    IconButton(
                        onClick = { showEditConfigDialog = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit Config", tint = CyberCyan, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Rate Breakdown Grid
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    RewardConfigRow(title = "Data Cashback Rate:", value = "${String.format("%.1f", cashbackRateData)}%", hint = "Paid on data resale", tint = GlowingAmber)
                    RewardConfigRow(title = "Airtime Cashback Rate:", value = "${String.format("%.1f", cashbackRateAirtime)}%", hint = "Paid on airtime recharge", tint = GlowingAmber)
                    RewardConfigRow(title = "Bills & Utility Cashback:", value = "${String.format("%.1f", cashbackRateBills)}%", hint = "Electricity & Cable", tint = GlowingAmber)
                    RewardConfigRow(title = "Referral Bonus Cash:", value = "₦${String.format("%.2f", referralNaira)}", hint = "Direct payout per referral", tint = ElectricEmerald)
                    RewardConfigRow(title = "Referral Bonus Points:", value = "$referralBonusPts Pts", hint = "Awarded to referrer", tint = CyberCyan)
                    RewardConfigRow(title = "Points Earn Rate:", value = "$ptsEarnRate Pt / ₦100", hint = "On purchases", tint = CyberCyan)
                    RewardConfigRow(title = "Points Redemption Value:", value = "100 Pts = ₦${ptsRedeemRate.toInt()}", hint = "Wallet conversion rate", tint = ElectricEmerald)
                    RewardConfigRow(title = "Min Admin Profit Threshold:", value = "₦${String.format(java.util.Locale.US, "%,.0f", referralMinProfitThreshold)}", hint = "Referrer paid ONLY when referred friend's buys generate this profit for admin (Zero Out-Of-Pocket)", tint = ElectricEmerald)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { showEditConfigDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurface, contentColor = GlowingAmber),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GlowingAmber.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("CONFIGURE REWARD AMOUNTS & RATES", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 3B. REDEMPTION ACTIVITIES & TELCO CREDIT MANAGEMENT
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, GlowingAmber.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Redeem, contentDescription = "Redemption Config", tint = GlowingAmber)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "REDEMPTION ACTIVITIES & TELCO CREDIT",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                    }

                    Button(
                        onClick = { showAddActivityDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = GlowingAmber, contentColor = DarkObsidian),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ADD", fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Activate or deactivate redemption items. If deactivated, items appear faded and paused in the app. Setup network credits (e.g., 90 Pts = ₦120 MTN credit), VPN time, and cash.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    redemptionActivities.forEach { act ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = DarkSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .alpha(if (act.isActive) 1f else 0.42f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                when {
                                                    act.network?.contains("MTN", true) == true -> Color(0xFFFFCC00).copy(alpha = 0.2f)
                                                    act.network?.contains("AIRTEL", true) == true -> Color(0xFFFF3333).copy(alpha = 0.2f)
                                                    act.network?.contains("GLO", true) == true -> Color(0xFF22AA33).copy(alpha = 0.2f)
                                                    act.category == RedemptionCategory.VPN_TIME -> CyberCyan.copy(alpha = 0.2f)
                                                    act.category == RedemptionCategory.CASH -> GlowingAmber.copy(alpha = 0.2f)
                                                    else -> ElectricEmerald.copy(alpha = 0.2f)
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val netText = (act.network ?: "").take(3)
                                        Text(
                                            text = if (netText.isNotBlank()) netText else if (act.category == RedemptionCategory.VPN_TIME) "VPN" else "₦",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = TextPrimary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = act.title,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                        )
                                        Text(
                                            text = "${act.pointsCost} Pts ➔ ${if (act.category == RedemptionCategory.VPN_TIME) "${(act.rewardValue / 60).toInt()}h VPN" else "₦${act.rewardValue.toInt()}"} • ${if (act.isActive) "Active" else "Inactive (Faded)"}",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = if (act.isActive) CyberCyan else TextMuted,
                                                fontSize = 10.sp
                                            )
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { editingActivity = act },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit",
                                            tint = CyberCyan,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))

                                    Switch(
                                        checked = act.isActive,
                                        onCheckedChange = { active ->
                                            viewModel.toggleRedemptionActivityActive(act.id, active)
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = DarkObsidian,
                                            checkedTrackColor = ElectricEmerald,
                                            uncheckedThumbColor = TextMuted,
                                            uncheckedTrackColor = DarkSurfaceElevated
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. SMS & Direct Messaging Pricing Configuration Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, CyberCyan.copy(alpha = 0.4f), RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Chat, contentDescription = "SMS Config", tint = CyberCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SMS GATEWAY & DIRECT CHAT PRICING",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Direct In-App Messages between Feed App users (Flow Chat) are 100% FREE. SMS is routed via FlowTest SMS gateway with wholesale-to-retail markup.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        color = DarkSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Flow Chat", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 9.sp))
                            Text("₦0.00 FREE", style = MaterialTheme.typography.titleSmall.copy(color = ElectricEmerald, fontWeight = FontWeight.Black))
                            Text("In-App P2P Chat", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 9.sp))
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        color = DarkSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("SMS Retail", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 9.sp))
                            Text("₦${String.format("%.2f", smsRetail)}/SMS", style = MaterialTheme.typography.titleSmall.copy(color = CyberCyan, fontWeight = FontWeight.Black))
                            Text("Cost: ₦${String.format("%.2f", smsWholesale)} • Profit: ₦${String.format("%.2f", smsRetail - smsWholesale)}", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 9.sp))
                        }
                    }
                }
            }
        }

        // 5. Admin Wholesale Markups & Service Discounts Configuration Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, DarkCardBorder, RoundedCornerShape(20.dp)),
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
                        Icon(imageVector = Icons.Default.LocalOffer, contentDescription = "Markups", tint = CyberCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ADMIN MARKUPS & DISCOUNTS SETUP",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { viewModel.fetchServicesFromApi() },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CloudDownload, contentDescription = "Fetch", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("FETCH API", fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Set Wholesale Markup (+%) and User Discount (-%). Net profit is automatically calculated after customer cashback rewards.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                listOf(
                    "airtime" to "Airtime Vending",
                    "data" to "Data Bundles (SME & Gifting)",
                    "cable" to "Cable TV Subs",
                    "utility" to "Electricity Bills",
                    "recharge" to "Recharge Card PINs",
                    "betting" to "Betting Funding",
                    "education" to "Exam E-PINs",
                    "giftcard" to "Gift Vouchers"
                ).forEach { (srvKey, srvName) ->
                    val currentDiscount = serviceDiscounts[srvKey] ?: 0.0
                    val currentMarkup = serviceMarkups[srvKey] ?: 15.0
                    val cashbackBonus = if (srvKey == "airtime") cashbackRateAirtime else if (srvKey == "data") cashbackRateData else cashbackRateBills
                    val netProfitMargin = currentMarkup - currentDiscount - cashbackBonus

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = DarkSurface,
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, DarkCardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(srvName, style = MaterialTheme.typography.labelSmall.copy(color = TextPrimary, fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)

                                Spacer(modifier = Modifier.width(8.dp))

                                Surface(
                                    color = if (netProfitMargin >= 0) ElectricEmerald.copy(alpha = 0.2f) else Color.Red.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = if (netProfitMargin >= 0) "✓ PROFIT: +${String.format("%.1f", netProfitMargin)}%" else "⚠️ LOSS: ${String.format("%.1f", netProfitMargin)}%",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (netProfitMargin >= 0) ElectricEmerald else Color(0xFFFF5252),
                                            fontWeight = FontWeight.Black,
                                            fontSize = 10.sp
                                        ),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        maxLines = 1
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Wholesale Markup (+%):", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 10.sp))
                                Text("+${currentMarkup.toInt()}%", style = MaterialTheme.typography.labelSmall.copy(color = CyberCyan, fontWeight = FontWeight.Bold, fontSize = 10.sp))
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf(5.0, 10.0, 15.0, 20.0, 25.0, 30.0).forEach { mk ->
                                    val isSel = currentMarkup == mk
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isSel) CyberCyan else DarkSurfaceElevated,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { viewModel.updateServiceMarkup(srvKey, mk) }
                                    ) {
                                        Text(
                                            text = "+${mk.toInt()}%",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSel) DarkObsidian else TextSecondary,
                                                fontSize = 9.sp
                                            ),
                                            modifier = Modifier.padding(vertical = 4.dp),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Promotional Discount Setup:", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 10.sp))
                                Text(
                                    text = if (currentDiscount > 0.0) "🔥 PROMO: -${currentDiscount.toInt()}% Off" else "0% Off (Promo Default / Standard)",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (currentDiscount > 0.0) GlowingAmber else ElectricEmerald,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf(0.0, 2.0, 3.0, 5.0, 10.0, 15.0, 20.0).forEach { disc ->
                                    val isSel = currentDiscount == disc
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isSel) GlowingAmber else DarkSurfaceElevated,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { viewModel.updateServiceDiscount(srvKey, disc) }
                                    ) {
                                        Text(
                                            text = if (disc == 0.0) "0% Off" else "-${disc.toInt()}%",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSel) DarkObsidian else TextSecondary,
                                                fontSize = 9.sp
                                            ),
                                            modifier = Modifier.padding(vertical = 4.dp),
                                            textAlign = TextAlign.Center
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

    // Modal Dialog to edit all Reward, Referral and Cashback Parameters
    if (showEditConfigDialog) {
        RewardConfigEditDialog(
            initialAirtimeCashback = cashbackRateAirtime,
            initialDataCashback = cashbackRateData,
            initialBillsCashback = cashbackRateBills,
            initialReferralNaira = referralNaira,
            initialReferralBonusPts = referralBonusPts,
            initialPtsEarnRate = ptsEarnRate,
            initialPtsRedeemRate = ptsRedeemRate,
            initialMinProfitBuffer = minProfitBuffer,
            initialMinProfitThreshold = referralMinProfitThreshold,
            onDismiss = { showEditConfigDialog = false },
            onSave = { airtimeCb, dataCb, billsCb, refNaira, refPts, earnRate, redeemRate, minBuf, minProfit ->
                viewModel.updateRewardReferralConfig(
                    cashbackAirtime = airtimeCb,
                    cashbackData = dataCb,
                    cashbackBills = billsCb,
                    referralNaira = refNaira,
                    referralPct = 1.0,
                    referralPts = refPts,
                    pointsEarnPerHundred = earnRate,
                    pointsRedemptionNairaPer100 = redeemRate,
                    minProfitBuffer = minBuf,
                    minProfitThreshold = minProfit
                )
                showEditConfigDialog = false
            }
        )
    }

    editingActivity?.let { act ->
        EditRedemptionActivityDialog(
            activity = act,
            onDismiss = { editingActivity = null },
            onSave = { pts, value, title ->
                viewModel.updateRedemptionActivity(
                    id = act.id,
                    pointsCost = pts,
                    rewardValue = value,
                    title = title
                )
                editingActivity = null
            }
        )
    }

    if (showAddActivityDialog) {
        AddRedemptionActivityDialog(
            onDismiss = { showAddActivityDialog = false },
            onAdd = { newAct ->
                viewModel.addRedemptionActivity(newAct)
                showAddActivityDialog = false
            }
        )
    }
}

@Composable
fun RewardConfigRow(title: String, value: String, hint: String, tint: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp))
            Text(hint, style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 9.sp))
        }
        Text(value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = tint))
    }
}

@Composable
fun RewardConfigEditDialog(
    initialAirtimeCashback: Double,
    initialDataCashback: Double,
    initialBillsCashback: Double,
    initialReferralNaira: Double,
    initialReferralBonusPts: Int,
    initialPtsEarnRate: Int,
    initialPtsRedeemRate: Double,
    initialMinProfitBuffer: Double,
    initialMinProfitThreshold: Double,
    onDismiss: () -> Unit,
    onSave: (Double, Double, Double, Double, Int, Int, Double, Double, Double) -> Unit
) {
    var airtimeCb by remember { mutableStateOf(initialAirtimeCashback.toString()) }
    var dataCb by remember { mutableStateOf(initialDataCashback.toString()) }
    var billsCb by remember { mutableStateOf(initialBillsCashback.toString()) }
    var refNaira by remember { mutableStateOf(initialReferralNaira.toString()) }
    var refPts by remember { mutableStateOf(initialReferralBonusPts.toString()) }
    var earnRate by remember { mutableStateOf(initialPtsEarnRate.toString()) }
    var redeemRate by remember { mutableStateOf(initialPtsRedeemRate.toString()) }
    var minBuf by remember { mutableStateOf(initialMinProfitBuffer.toString()) }
    var minProfit by remember { mutableStateOf(initialMinProfitThreshold.toInt().toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Text(
                text = "Edit Reward & Referral Config",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = dataCb,
                    onValueChange = { dataCb = it },
                    label = { Text("Data Cashback (%)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = airtimeCb,
                    onValueChange = { airtimeCb = it },
                    label = { Text("Airtime Cashback (%)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = refNaira,
                    onValueChange = { refNaira = it },
                    label = { Text("Referral Bonus Cash (₦)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = refPts,
                    onValueChange = { refPts = it },
                    label = { Text("Referral Reward Points (Pts)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = minProfit,
                    onValueChange = { minProfit = it },
                    label = { Text("Min Admin Profit Threshold (₦) [Zero Out-Of-Pocket]") },
                    supportingText = { Text("Bonus paid only after referred friend generates this net profit for admin") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = redeemRate,
                    onValueChange = { redeemRate = it },
                    label = { Text("Points Redemption Value (₦ per 100 Pts)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        airtimeCb.toDoubleOrNull() ?: 1.0,
                        dataCb.toDoubleOrNull() ?: 1.5,
                        billsCb.toDoubleOrNull() ?: 0.5,
                        refNaira.toDoubleOrNull() ?: 50.0,
                        refPts.toIntOrNull() ?: 50,
                        earnRate.toIntOrNull() ?: 1,
                        redeemRate.toDoubleOrNull() ?: 2.0,
                        minBuf.toDoubleOrNull() ?: 1.5,
                        minProfit.toDoubleOrNull() ?: 50.0
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian)
            ) {
                Text("SAVE CONFIG", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = TextSecondary)
            }
        }
    )
}

@Composable
fun EditRedemptionActivityDialog(
    activity: RedemptionActivity,
    onDismiss: () -> Unit,
    onSave: (Int, Double, String) -> Unit
) {
    var title by remember { mutableStateOf(activity.title) }
    var pointsCost by remember { mutableStateOf(activity.pointsCost.toString()) }
    var rewardValue by remember { mutableStateOf(activity.rewardValue.toInt().toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Text(
                text = "Edit Redemption: ${activity.title}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Display Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = pointsCost,
                    onValueChange = { pointsCost = it },
                    label = { Text("Points Cost (Pts)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = rewardValue,
                    onValueChange = { rewardValue = it },
                    label = {
                        Text(if (activity.category == RedemptionCategory.VPN_TIME) "Reward Value (Minutes)" else "Reward Value (₦ Credit)")
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val pts = pointsCost.toIntOrNull() ?: activity.pointsCost
                    val rVal = rewardValue.toDoubleOrNull() ?: activity.rewardValue
                    onSave(pts, rVal, title)
                },
                colors = ButtonDefaults.buttonColors(containerColor = GlowingAmber, contentColor = DarkObsidian)
            ) {
                Text("SAVE", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = TextSecondary)
            }
        }
    )
}

@Composable
fun AddRedemptionActivityDialog(
    onDismiss: () -> Unit,
    onAdd: (RedemptionActivity) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var subtitle by remember { mutableStateOf("") }
    var network by remember { mutableStateOf("MTN") }
    var pointsCost by remember { mutableStateOf("90") }
    var creditAmount by remember { mutableStateOf("120") }
    var selectedCategory by remember { mutableStateOf(RedemptionCategory.NETWORK_CREDIT) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Text(
                text = "Add Redemption Activity",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        RedemptionCategory.NETWORK_CREDIT to "Telco",
                        RedemptionCategory.VPN_TIME to "VPN",
                        RedemptionCategory.CASH to "Cash"
                    ).forEach { (cat, label) ->
                        val isSel = selectedCategory == cat
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSel) GlowingAmber else DarkSurfaceElevated,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    selectedCategory = cat
                                    if (cat == RedemptionCategory.NETWORK_CREDIT && title.isBlank()) {
                                        title = "$network ₦$creditAmount"
                                        subtitle = "Airtime Credit"
                                    } else if (cat == RedemptionCategory.VPN_TIME && title.isBlank()) {
                                        title = "1 Day VPN"
                                        subtitle = "24h VPN Time"
                                    } else if (cat == RedemptionCategory.CASH && title.isBlank()) {
                                        title = "₦100 Cash"
                                        subtitle = "To Wallet"
                                    }
                                }
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) DarkObsidian else TextSecondary
                                ),
                                modifier = Modifier.padding(vertical = 6.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                if (selectedCategory == RedemptionCategory.NETWORK_CREDIT) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("MTN", "AIRTEL", "GLO", "9MOBILE").forEach { net ->
                            val isSel = network == net
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSel) CyberCyan else DarkSurfaceElevated,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        network = net
                                        title = "$net ₦$creditAmount"
                                    }
                            ) {
                                Text(
                                    text = net,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSel) DarkObsidian else TextSecondary,
                                        fontSize = 9.sp
                                    ),
                                    modifier = Modifier.padding(vertical = 5.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Activity Title (e.g. MTN ₦120)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = subtitle,
                    onValueChange = { subtitle = it },
                    label = { Text("Subtitle (e.g. Airtime Credit)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = pointsCost,
                    onValueChange = { pointsCost = it },
                    label = { Text("Points Cost (e.g. 90)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = creditAmount,
                    onValueChange = { creditAmount = it },
                    label = {
                        Text(if (selectedCategory == RedemptionCategory.VPN_TIME) "Reward Minutes (e.g. 1440)" else "Credit Value ₦ (e.g. 120)")
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val pts = pointsCost.toIntOrNull() ?: 90
                    val rVal = creditAmount.toDoubleOrNull() ?: 120.0
                    val finalTitle = title.ifBlank { if (selectedCategory == RedemptionCategory.NETWORK_CREDIT) "$network ₦${rVal.toInt()}" else "Redeem Activity" }
                    val finalSubtitle = subtitle.ifBlank { if (selectedCategory == RedemptionCategory.NETWORK_CREDIT) "Airtime Credit" else "Reward" }
                    val id = "custom_" + System.currentTimeMillis()
                    val newAct = RedemptionActivity(
                        id = id,
                        title = finalTitle,
                        subtitle = finalSubtitle,
                        category = selectedCategory,
                        pointsCost = pts,
                        rewardValue = rVal,
                        network = if (selectedCategory == RedemptionCategory.NETWORK_CREDIT) network else "",
                        isActive = true,
                        iconType = if (selectedCategory == RedemptionCategory.NETWORK_CREDIT) network.lowercase() else if (selectedCategory == RedemptionCategory.VPN_TIME) "vpn" else "cash"
                    )
                    onAdd(newAct)
                },
                colors = ButtonDefaults.buttonColors(containerColor = GlowingAmber, contentColor = DarkObsidian)
            ) {
                Text("ADD ACTIVITY", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = TextSecondary)
            }
        }
    )
}

data class TelcoComparisonItem(
    val name: String,
    val type: String,
    val wholesale: Double,
    val officialTelco: Double,
    val oldBuggyRetail: Double,
    val isOfficialDenom: Boolean
)

