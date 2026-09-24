package com.example.ui.components.admin

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ui.viewmodel.VpnViewModel
import com.example.ui.components.FlowButtonLoadingLine
import com.example.ui.theme.*
import kotlinx.coroutines.launch

data class AdvertSuggestion(
    val id: String,
    val title: String,
    val body: String,
    val category: String,
    val tag: String,
    val iconEmoji: String,
    val actionUrl: String = "https://flowtest.app"
)

data class BroadcastHistoryItem(
    val id: String,
    val title: String,
    val audience: String,
    val hasPush: Boolean,
    val hasEmail: Boolean,
    val timestamp: String,
    val status: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAdvertsOffersTab(
    viewModel: VpnViewModel,
    onShowStatusMsg: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Pre-made suggestions matching user request and Image 3
    val suggestions = remember {
        listOf(
            AdvertSuggestion(
                id = "sug_rewards_coins",
                title = "You're one tap away from rewards! 💛",
                body = "Why just buy when you can earn too? 👀💛\nPurchase airtime or data above N1000 on FlowTest App and collect bonus reward coins for awesome rewards.\nLet's stack those coins! 🪙🔥",
                category = "Rewards & Coins",
                tag = "COINS OFFER",
                iconEmoji = "🪙"
            ),
            AdvertSuggestion(
                id = "sug_weekend_data",
                title = "⚡ Weekend Data Rush: Get Extra 1GB Free!",
                body = "Recharge your weekend browsing! Top up any 2GB or 5GB data bundle today and instantly get 2 Hours bonus VPN high-speed browsing time. Valid this weekend only! 🚀🎉",
                category = "Data Promo",
                tag = "MEGA DATA",
                iconEmoji = "⚡"
            ),
            AdvertSuggestion(
                id = "sug_cashback_alert",
                title = "💰 Instant 5% Cashback on Airtime & Utilities",
                body = "Special mid-week reward! Pay your electricity bills, recharge airtime, or subscribe cable TV today and receive instant 5% cashback straight into your wallet! ✨💳",
                category = "Cashback",
                tag = "5% CASHBACK",
                iconEmoji = "💰"
            ),
            AdvertSuggestion(
                id = "sug_vpn_night_pass",
                title = "🛡️ Free Unlimited VPN Night Pass Active",
                body = "Surfing tonight? Enjoy ultra-low ping gaming and zero-buffer streaming with our secure encrypted servers. Protect your identity with zero logs! 🔒⚡",
                category = "Security & VPN",
                tag = "VPN PASS",
                iconEmoji = "🛡️"
            ),
            AdvertSuggestion(
                id = "sug_bulk_sms",
                title = "📢 Grow Your Business: Bulk SMS at ₦1.80/unit!",
                body = "Reach thousands of customers in seconds. Fast delivery with real-time delivery reports. Top up your SMS credits today and enjoy wholesale rates! 📈💼",
                category = "Business & SMS",
                tag = "BULK SMS",
                iconEmoji = "📢"
            )
        )
    }

    var selectedSuggestionId by remember { mutableStateOf("sug_rewards_coins") }
    var advertTitleInput by remember { mutableStateOf(suggestions[0].title) }
    var advertBodyInput by remember { mutableStateOf(suggestions[0].body) }
    var targetAudience by remember { mutableStateOf("All Registered Users") }
    var sendNativePush by remember { mutableStateOf(true) }
    var sendEmailNotification by remember { mutableStateOf(true) }
    var isBroadcasting by remember { mutableStateOf(false) }

    var broadcastHistory by remember {
        mutableStateOf(
            listOf(
                BroadcastHistoryItem(
                    id = "hist_1",
                    title = "You're one tap away from rewards! 💛",
                    audience = "All Users",
                    hasPush = true,
                    hasEmail = true,
                    timestamp = "Just Now",
                    status = "DELIVERED"
                ),
                BroadcastHistoryItem(
                    id = "hist_2",
                    title = "⚡ Weekend Data Rush: Get Extra 1GB Free!",
                    audience = "Data Buyers",
                    hasPush = true,
                    hasEmail = true,
                    timestamp = "Yesterday",
                    status = "DELIVERED"
                )
            )
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header Card
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = DarkSurface,
            border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(GlowingAmber.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Campaign,
                                contentDescription = null,
                                tint = GlowingAmber,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "ADVERT & OFFERS BROADCASTER",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.8.sp,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = "Dispatch Native Push & Email Adverts to Users",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                    Surface(
                        color = ElectricEmerald.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "LIVE SYSTEM",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = ElectricEmerald,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Suggestion Templates Strip (with Image 3 style)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CAMPAIGN TEMPLATE SUGGESTIONS",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = CyberCyan,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                )
                Text(
                    text = "Tap to Auto-fill",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                suggestions.forEach { sug ->
                    val isSelected = selectedSuggestionId == sug.id
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) Color(0xFF132738) else DarkSurface,
                        border = BorderStroke(
                            if (isSelected) 1.5.dp else 1.dp,
                            if (isSelected) CyberCyan else DarkCardBorder
                        ),
                        modifier = Modifier
                            .width(230.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                selectedSuggestionId = sug.id
                                advertTitleInput = sug.title
                                advertBodyInput = sug.body
                            }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(sug.iconEmoji, fontSize = 20.sp)
                                Surface(
                                    color = if (isSelected) CyberCyan.copy(alpha = 0.2f) else DarkSurfaceElevated,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = sug.tag,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (isSelected) CyberCyan else GlowingAmber,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp
                                        ),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = sug.title,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    fontSize = 12.sp
                                ),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = sug.category,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }
            }
        }

        // Custom Notification Form
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = DarkSurface,
            border = BorderStroke(1.dp, DarkCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "EDIT BROADCAST CONTENT",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )

                // Title Input
                OutlinedTextField(
                    value = advertTitleInput,
                    onValueChange = { advertTitleInput = it },
                    label = { Text("Notification Title / Subject") },
                    placeholder = { Text("e.g. You're one tap away from rewards! 💛") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = DarkCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = DarkSurfaceElevated,
                        unfocusedContainerColor = DarkSurfaceElevated
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("advert_title_input")
                )

                // Body Message Input
                OutlinedTextField(
                    value = advertBodyInput,
                    onValueChange = { advertBodyInput = it },
                    label = { Text("Advert Message / Offer Body") },
                    placeholder = { Text("Enter the offer details, reward points, validity or instructions...") },
                    minLines = 4,
                    maxLines = 8,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = DarkCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = DarkSurfaceElevated,
                        unfocusedContainerColor = DarkSurfaceElevated
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("advert_body_input")
                )

                // Target Audience
                Text(
                    text = "Target Audience",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("All Registered Users", "Active Data Buyers", "New Signups", "VIP Resellers").forEach { aud ->
                        val isSelected = targetAudience == aud
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) CyberCyan.copy(alpha = 0.2f) else DarkSurfaceElevated,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) CyberCyan else DarkCardBorder
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { targetAudience = aud }
                        ) {
                            Text(
                                text = aud,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isSelected) CyberCyan else TextSecondary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 11.sp
                                ),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Delivery Channels Selection
                Text(
                    text = "Delivery Channels",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Native App Push Toggle
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (sendNativePush) Color(0xFF103248) else DarkSurfaceElevated,
                        border = BorderStroke(
                            1.dp,
                            if (sendNativePush) CyberCyan else DarkCardBorder
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { sendNativePush = !sendNativePush }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = sendNativePush,
                                onCheckedChange = { sendNativePush = it },
                                colors = CheckboxDefaults.colors(checkedColor = CyberCyan)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "Native App Push",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = "Android Notification",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextMuted,
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        }
                    }

                    // Email Notification Toggle
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (sendEmailNotification) Color(0xFF103248) else DarkSurfaceElevated,
                        border = BorderStroke(
                            1.dp,
                            if (sendEmailNotification) CyberCyan else DarkCardBorder
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { sendEmailNotification = !sendEmailNotification }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = sendEmailNotification,
                                onCheckedChange = { sendEmailNotification = it },
                                colors = CheckboxDefaults.colors(checkedColor = CyberCyan)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "Email Alert",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = "Dispatched via API",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextMuted,
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Live Android 14 Notification Preview (Matching Image 3)
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = DarkSurface,
            border = BorderStroke(1.dp, GlowingAmber.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            tint = GlowingAmber,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "LIVE NOTIFICATION PREVIEW (USER SCREEN)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = GlowingAmber,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        )
                    }
                    Text(
                        text = "Android 14 / One UI",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 10.sp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Simulated System Notification Banner
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF161B22),
                    border = BorderStroke(1.dp, Color(0xFF30363D)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(GlowingAmber),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Campaign,
                                        contentDescription = null,
                                        tint = DarkObsidian,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "FlowTest Rewards",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("•", color = TextMuted, fontSize = 10.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "now",
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 10.sp)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Title
                        Text(
                            text = advertTitleInput.ifBlank { "You're one tap away from rewards! 💛" },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 13.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Body (Multilined with emojis)
                        Text(
                            text = advertBodyInput.ifBlank { "Why just buy when you can earn too? 👀💛\nPurchase airtime or data..." },
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFFC9D1D9),
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Action Pills
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = GlowingAmber.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, GlowingAmber.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "Claim Offer",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = GlowingAmber,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF21262D)
                            ) {
                                Text(
                                    text = "Open App",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Action Trigger Button
        Button(
            onClick = {
                if (advertTitleInput.isBlank() || advertBodyInput.isBlank()) {
                    Toast.makeText(context, "Please enter both title and body text", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (!sendNativePush && !sendEmailNotification) {
                    Toast.makeText(context, "Please select at least one delivery channel", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                isBroadcasting = true
                coroutineScope.launch {
                    viewModel.broadcastAdvertOffer(
                        title = advertTitleInput.trim(),
                        body = advertBodyInput.trim(),
                        sendPush = sendNativePush,
                        sendEmail = sendEmailNotification,
                        targetAudience = targetAudience
                    ) { success, msg ->
                        isBroadcasting = false
                        if (success) {
                            onShowStatusMsg("Broadcast sent successfully: $msg")
                            Toast.makeText(context, "Broadcast sent to Native Push & Email!", Toast.LENGTH_LONG).show()
                            // Add to history
                            broadcastHistory = listOf(
                                BroadcastHistoryItem(
                                    id = "hist_${System.currentTimeMillis()}",
                                    title = advertTitleInput.trim(),
                                    audience = targetAudience,
                                    hasPush = sendNativePush,
                                    hasEmail = sendEmailNotification,
                                    timestamp = "Just Now",
                                    status = "DELIVERED"
                                )
                            ) + broadcastHistory
                        } else {
                            Toast.makeText(context, "Broadcast notice: $msg", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            },
            enabled = !isBroadcasting,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = GlowingAmber,
                contentColor = DarkObsidian
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("broadcast_advert_submit_btn")
        ) {
            if (isBroadcasting) {
                FlowButtonLoadingLine(
                    color = DarkObsidian,
                    width = 44.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Dispatching Push & Email...",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Broadcast Advert & Offer to Users",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black)
                )
            }
        }

        // Broadcast History Log
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = DarkSurface,
            border = BorderStroke(1.dp, DarkCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "RECENT CAMPAIGN BROADCASTS",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                broadcastHistory.take(5).forEach { item ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = DarkSurfaceElevated,
                        border = BorderStroke(1.dp, DarkCardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${item.audience} • ${item.timestamp}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = TextMuted,
                                            fontSize = 10.sp
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    if (item.hasPush) {
                                        Surface(color = CyberCyan.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                                            Text("PUSH", color = CyberCyan, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    if (item.hasEmail) {
                                        Surface(color = GlowingAmber.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                                            Text("EMAIL", color = GlowingAmber, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                        }
                                    }
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = ElectricEmerald.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = item.status,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = ElectricEmerald,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
