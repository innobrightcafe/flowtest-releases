package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DataUsageFilter
import com.example.data.model.NetworkInterfaceFilter
import com.example.data.model.UsagePeriodType
import com.example.data.util.NetworkUtils
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val TextTertiary = TextMuted
private val AmberOrange = GlowingAmber
private val CharcoalGrey = DarkSurfaceElevated

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataUsageFilterSheet(
    currentFilter: DataUsageFilter,
    onDismiss: () -> Unit,
    onApplyFilter: (DataUsageFilter, Boolean) -> Unit
) {
    val context = LocalContext.current
    val hasUsagePermission = remember { NetworkUtils.hasUsageStatsPermission(context) }

    var selectedPeriod by remember { mutableStateOf(currentFilter.periodType) }
    var selectedNetwork by remember { mutableStateOf(currentFilter.networkFilter) }
    var saveAsDefault by remember { mutableStateOf(false) }

    val now = remember { System.currentTimeMillis() }
    var customDaysBack by remember {
        mutableIntStateOf(
            if (currentFilter.periodType == UsagePeriodType.CUSTOM && currentFilter.customStartTimestamp != null) {
                ((now - currentFilter.customStartTimestamp) / (24L * 3600L * 1000L)).toInt().coerceIn(1, 90)
            } else 3
        )
    }

    val previewFilter = remember(selectedPeriod, selectedNetwork, customDaysBack) {
        if (selectedPeriod == UsagePeriodType.CUSTOM) {
            val start = now - (customDaysBack * 24L * 3600L * 1000L)
            DataUsageFilter(
                periodType = UsagePeriodType.CUSTOM,
                networkFilter = selectedNetwork,
                customStartTimestamp = start,
                customEndTimestamp = now
            )
        } else {
            DataUsageFilter(
                periodType = selectedPeriod,
                networkFilter = selectedNetwork
            )
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DarkObsidian,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = TextTertiary.copy(alpha = 0.5f)
            )
        },
        modifier = Modifier.testTag("data_usage_filter_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(ElectricEmerald.copy(alpha = 0.15f))
                            .border(1.dp, ElectricEmerald.copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Filter",
                            tint = ElectricEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "USAGE PERIOD & FILTER",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "Filter measured usage across all services",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Permission Warning if not granted
            if (!hasUsagePermission) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AmberOrange.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AmberOrange.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info",
                            tint = AmberOrange,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Usage Access Recommended",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = AmberOrange
                                )
                            )
                            Text(
                                text = "Grant Android Usage Access to measure exact historical data for Today, Weekly, and Monthly cycles.",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
                            )
                        }
                        TextButton(
                            onClick = { NetworkUtils.openUsageAccessSettings(context) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Grant", color = AmberOrange, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 1. Time Period Selection
            Text(
                text = "MEASUREMENT PERIOD",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = ElectricEmerald
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            val periods = listOf(
                Triple(UsagePeriodType.DAILY, "Today (Daily)", "From 12:00 AM today to current moment"),
                Triple(UsagePeriodType.WEEKLY, "Weekly (7 Days)", "Total data consumed over the last 7 days"),
                Triple(UsagePeriodType.MONTHLY, "Monthly (30 Days)", "Complete 30-day cumulative cycle"),
                Triple(UsagePeriodType.CUSTOM, "Custom Range", "Select custom number of days or interval")
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                periods.forEach { (period, title, desc) ->
                    val isSelected = selectedPeriod == period
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) ElectricEmerald.copy(alpha = 0.12f) else CharcoalGrey,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) ElectricEmerald else CharcoalGrey
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedPeriod = period }
                            .testTag("period_option_${period.name.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { selectedPeriod = period },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = ElectricEmerald,
                                    unselectedColor = TextTertiary
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) TextPrimary else TextSecondary
                                    )
                                )
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextTertiary,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Custom Range Slider / Buttons
            AnimatedVisibility(visible = selectedPeriod == UsagePeriodType.CUSTOM) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .background(CharcoalGrey.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text = "Days to look back: $customDaysBack days",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Measuring from ${customDaysBack} days ago until right now",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(2, 3, 5, 14, 21, 60).forEach { days ->
                            val isSel = customDaysBack == days
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) ElectricEmerald else CharcoalGrey,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { customDaysBack = days }
                            ) {
                                Text(
                                    text = "${days}d",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSel) DarkObsidian else TextPrimary
                                    ),
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Network Interface Selection
            Text(
                text = "NETWORK INTERFACE",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = ElectricEmerald
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NetworkInterfaceFilter.values().forEach { net ->
                    val isSelected = selectedNetwork == net
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) ElectricEmerald.copy(alpha = 0.15f) else CharcoalGrey,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) ElectricEmerald else CharcoalGrey
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedNetwork = net }
                            .testTag("network_option_${net.name.lowercase()}")
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = when (net) {
                                    NetworkInterfaceFilter.ALL -> Icons.Default.Language
                                    NetworkInterfaceFilter.MOBILE -> Icons.Default.SignalCellularAlt
                                    NetworkInterfaceFilter.WIFI -> Icons.Default.Wifi
                                },
                                contentDescription = net.shortLabel,
                                tint = if (isSelected) ElectricEmerald else TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = net.shortLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) TextPrimary else TextSecondary,
                                    fontSize = 11.sp
                                ),
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. Range Preview Card
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = CharcoalGrey,
                border = androidx.compose.foundation.BorderStroke(1.dp, CharcoalGrey.copy(alpha = 0.8f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Date Range",
                        tint = ElectricEmerald,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Time Window:",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextTertiary)
                        )
                        Text(
                            text = previewFilter.getFormattedRange(now),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "Active on: ${selectedNetwork.label}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = ElectricEmerald,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Save as Default Setting Option
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = CharcoalGrey.copy(alpha = 0.4f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { saveAsDefault = !saveAsDefault }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Save as Default (Settings)",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "Set up once: applies automatically across all services",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        )
                    }
                    Switch(
                        checked = saveAsDefault,
                        onCheckedChange = { saveAsDefault = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DarkObsidian,
                            checkedTrackColor = ElectricEmerald,
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = CharcoalGrey
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        selectedPeriod = UsagePeriodType.DAILY
                        selectedNetwork = NetworkInterfaceFilter.ALL
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CharcoalGrey),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Reset Today", color = TextSecondary)
                }

                Button(
                    onClick = {
                        onApplyFilter(previewFilter, saveAsDefault)
                        onDismiss()
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricEmerald),
                    modifier = Modifier
                        .weight(1.5f)
                        .testTag("apply_usage_filter_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Apply",
                        tint = DarkObsidian,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Apply Filter",
                        fontWeight = FontWeight.Bold,
                        color = DarkObsidian
                    )
                }
            }
        }
    }
}
