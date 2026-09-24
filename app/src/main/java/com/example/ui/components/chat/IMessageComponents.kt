package com.example.ui.components.chat

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.ui.viewmodel.VpnViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

// Flow Chat & SMS Color Tokens
val FlowChatCyan = CyberCyan
val FlowChatCyanDark = Color(0xFF00B0D0)
val SmsBlue = Color(0xFF007AFF)
val SmsBlueDark = Color(0xFF0056B3)
val ChatBubbleGray = Color(0xFF262629)
val ChatBubbleGrayLight = Color(0xFF3A3A3C)

// Backward compatibility aliases
val IMessageBlue = SmsBlue
val IMessageBlueDark = SmsBlueDark
val IMessageGreen = SmsBlue
val IMessageGreenDark = SmsBlueDark
val IMessageGray = ChatBubbleGray
val IMessageGrayLight = ChatBubbleGrayLight
val WhatsAppGreen = FlowChatCyan

enum class ChatSendMode {
    FLOW_CHAT,   // Flow Chat Peer-to-Peer Data/VPN Messaging (Primary CyberCyan) - ₦0.00
    SMS;         // Carrier Mobile SMS via Gateway (System Blue) - ₦4.00/SMS

    companion object {
        val FREE_CHAT = FLOW_CHAT
        val CARRIER_SMS = SMS
    }
}

/**
 * Flow Chat & SMS Chat Bubble with Primary Cyan gradient (Flow Chat), Blue gradient (SMS),
 * or Slate Gray (Incoming), rounded corners with speech tail, tapback reactions,
 * audio note player, reply quote preview, and delivery status.
 */
@Composable
fun IMessageBubble(
    message: VpnViewModel.SmsMessageItem,
    recipientName: String,
    onReactionSelect: (emoji: String) -> Unit,
    onReplyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isOutgoing = message.isOutgoing
    val isDirect = message.isDirectMessage
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var showReactionMenu by remember { mutableStateOf(false) }

    val timeFormatted = remember(message.timestamp) {
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        sdf.format(Date(message.timestamp))
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start
    ) {
        // Floating Tapback Reaction Menu if open
        AnimatedVisibility(
            visible = showReactionMenu,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            TapbackReactionMenu(
                onSelectEmoji = { emoji ->
                    onReactionSelect(emoji)
                    showReactionMenu = false
                },
                onReply = {
                    onReplyClick()
                    showReactionMenu = false
                },
                onCopy = {
                    clipboard.setText(AnnotatedString(message.text))
                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    showReactionMenu = false
                },
                onDismiss = { showReactionMenu = false }
            )
        }

        Box(
            modifier = Modifier.padding(
                start = if (isOutgoing) 48.dp else 0.dp,
                end = if (isOutgoing) 0.dp else 48.dp
            )
        ) {
            Column(
                horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start
            ) {
                // Main Bubble Container
                Box(
                    modifier = Modifier
                        .clip(
                            RoundedCornerShape(
                                topStart = 18.dp,
                                topEnd = 18.dp,
                                bottomStart = if (isOutgoing) 18.dp else 4.dp,
                                bottomEnd = if (isOutgoing) 4.dp else 18.dp
                            )
                        )
                        .background(
                            when {
                                isOutgoing && isDirect -> Brush.verticalGradient(
                                    listOf(FlowChatCyan, FlowChatCyanDark)
                                )
                                isOutgoing && !isDirect -> Brush.verticalGradient(
                                    listOf(SmsBlue, SmsBlueDark)
                                )
                                else -> Brush.verticalGradient(
                                    listOf(ChatBubbleGray, Color(0xFF1E1E20))
                                )
                            }
                        )
                        .border(
                            1.dp,
                            if (isOutgoing) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.08f),
                            RoundedCornerShape(
                                topStart = 18.dp,
                                topEnd = 18.dp,
                                bottomStart = if (isOutgoing) 18.dp else 4.dp,
                                bottomEnd = if (isOutgoing) 4.dp else 18.dp
                            )
                        )
                        .pointerInput(message.id) {
                            detectTapGestures(
                                onTap = { showReactionMenu = !showReactionMenu },
                                onLongPress = { showReactionMenu = true }
                            )
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Column {
                        // Reply quote preview if message replies to something
                        if (!message.replyToText.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.Black.copy(alpha = 0.25f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(3.dp)
                                            .height(26.dp)
                                            .background(
                                                if (isOutgoing) Color.White.copy(alpha = 0.8f) else CyberCyan,
                                                RoundedCornerShape(2.dp)
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = "Replying to:",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White.copy(alpha = 0.7f)
                                            )
                                        )
                                        Text(
                                            text = message.replyToText!!,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontSize = 11.sp,
                                                color = Color.White
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        // Message Type Body: Voice, Image, Gift, or Standard Text
                        when (message.messageType) {
                            "VOICE" -> {
                                VoiceMessagePlayer(
                                    durationSec = if (message.voiceDurationSec > 0) message.voiceDurationSec else 12,
                                    isOutgoing = isOutgoing
                                )
                            }
                            "IMAGE" -> {
                                ImageAttachmentCard(
                                    caption = message.text,
                                    mediaUrl = message.mediaDescription,
                                    isOutgoing = isOutgoing
                                )
                            }
                            "DOCUMENT" -> {
                                DocumentAttachmentCard(
                                    fileName = message.text,
                                    mediaUrl = message.mediaDescription,
                                    isOutgoing = isOutgoing
                                )
                            }
                            "GIFT" -> {
                                GiftCouponCard(
                                    title = message.text,
                                    isOutgoing = isOutgoing
                                )
                            }
                            else -> {
                                Text(
                                    text = message.text,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = if (isOutgoing && isDirect) DarkObsidian else Color.White,
                                        fontWeight = if (isOutgoing && isDirect) FontWeight.Medium else FontWeight.Normal,
                                        fontSize = 14.5.sp,
                                        lineHeight = 20.sp,
                                        letterSpacing = 0.15.sp
                                    )
                                )
                            }
                        }
                    }
                }

                // Attached Reaction Badge pill if message has reactions
                if (message.reactions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = DarkSurfaceElevated,
                        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .offset(y = (-6).dp)
                            .clickable { showReactionMenu = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            message.reactions.forEach { reaction ->
                                Text(text = reaction, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Timestamp & Delivery status receipt
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
        ) {
            Text(
                text = timeFormatted,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = TextMuted,
                    fontSize = 9.5.sp
                )
            )

            if (isOutgoing) {
                Spacer(modifier = Modifier.width(4.dp))
                if (isDirect) {
                    // Flow Chat Delivery Receipts
                    val (receiptText, receiptColor) = when (message.status) {
                        "READ" -> "Read ✓✓" to CyberCyan
                        "DELIVERED" -> "Delivered ✓✓" to Color(0xFF64D2FF)
                        "SENT" -> "Sent ✓" to TextMuted
                        "SENDING" -> "Sending..." to TextMuted
                        else -> "Failed" to CoralRed
                    }
                    Text(
                        text = receiptText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = receiptColor,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                } else {
                    // Carrier SMS Delivery Receipts
                    val (smsText, smsColor) = when (message.status) {
                        "DELIVERED" -> "Delivered (SMS) ✓" to SmsBlue
                        "SENT" -> "Sent via SMS ✓" to TextMuted
                        "SENDING" -> "Sending SMS..." to TextMuted
                        else -> "Failed" to CoralRed
                    }
                    Text(
                        text = smsText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = smsColor,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }
    }
}

/**
 * Floating iOS Tapback Reaction Bar: ❤️, 👍, 👎, 😂, ‼️, ❓, ⚡, 🔥 + Reply & Copy
 */
@Composable
fun TapbackReactionMenu(
    onSelectEmoji: (String) -> Unit,
    onReply: () -> Unit,
    onCopy: () -> Unit,
    onDismiss: () -> Unit
) {
    val emojis = listOf("❤️", "👍", "👎", "😂", "‼️", "❓", "⚡", "🔥")

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFF1C1C1E),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
        shadowElevation = 8.dp,
        modifier = Modifier
            .padding(bottom = 6.dp)
            .testTag("tapback_reaction_menu")
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                emojis.forEach { emoji ->
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable { onSelectEmoji(emoji) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = emoji, fontSize = 18.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onReply() }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Reply, contentDescription = "Reply", tint = CyberCyan, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reply", color = CyberCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onCopy() }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = "Copy", tint = TextSecondary, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy", color = TextSecondary, fontSize = 11.sp)
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onDismiss() }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = TextMuted, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("Done", color = TextMuted, fontSize = 11.sp)
                }
            }
        }
    }
}

/**
 * WhatsApp / iMessage Interactive Voice Note Player
 */
@Composable
fun VoiceMessagePlayer(
    durationSec: Int,
    isOutgoing: Boolean
) {
    var isPlaying by remember { mutableStateOf(false) }
    var currentProgress by remember { mutableStateOf(0f) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            val totalSteps = durationSec * 10
            for (step in 1..totalSteps) {
                delay(100L)
                currentProgress = step.toFloat() / totalSteps
            }
            isPlaying = false
            currentProgress = 0f
        }
    }

    Row(
        modifier = Modifier
            .widthIn(min = 190.dp, max = 250.dp)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { isPlaying = !isPlaying },
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.2f))
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            // Simulated audio waveform bars
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val barHeights = listOf(8, 14, 22, 10, 18, 26, 12, 20, 14, 24, 16, 10, 18, 12, 22, 8, 16, 12)
                barHeights.forEachIndexed { index, h ->
                    val fraction = (index + 1).toFloat() / barHeights.size
                    val isPassed = fraction <= currentProgress
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(h.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (isPassed) Color.White else Color.White.copy(alpha = 0.4f)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "0:${String.format("%02d", (currentProgress * durationSec).toInt())}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 10.sp
                    )
                )
                Text(
                    text = "0:${String.format("%02d", durationSec)}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 10.sp
                    )
                )
            }
        }
    }
}

/**
 * Image Attachment Card for Photos in Chat backed by Cloudflare R2 / local cache
 */
@Composable
fun ImageAttachmentCard(
    caption: String,
    mediaUrl: String? = null,
    isOutgoing: Boolean
) {
    Column(
        modifier = Modifier
            .widthIn(max = 240.dp)
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.linearGradient(
                        listOf(DarkSurfaceElevated, Color(0xFF1A2634))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Filled.Image,
                    contentDescription = "Photo",
                    tint = CyberCyan,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (!mediaUrl.isNullOrBlank() && mediaUrl.startsWith("http")) "☁️ R2 Cloud Photo" else "Flow Photo",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 11.sp
                    )
                )
            }
        }

        if (caption.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = caption,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color.White,
                    fontSize = 13.sp
                )
            )
        }
    }
}

/**
 * Document Attachment Card for PDFs and docs backed by Cloudflare R2
 */
@Composable
fun DocumentAttachmentCard(
    fileName: String,
    mediaUrl: String? = null,
    isOutgoing: Boolean
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = DarkSurfaceElevated,
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
        modifier = Modifier
            .widthIn(max = 240.dp)
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFE53935).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Description,
                    contentDescription = "Document",
                    tint = Color(0xFFE53935),
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fileName,
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (!mediaUrl.isNullOrBlank() && mediaUrl.startsWith("http")) "☁️ Cloudflare R2 Encrypted" else "Flow Document",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = FlowChatCyan,
                        fontSize = 9.sp
                    )
                )
            }
        }
    }
}

/**
 * Gift Coupon Card in Chat
 */
@Composable
fun GiftCouponCard(
    title: String,
    isOutgoing: Boolean
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF2E2606),
        border = androidx.compose.foundation.BorderStroke(1.dp, GlowingAmber),
        modifier = Modifier
            .widthIn(max = 250.dp)
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(GlowingAmber),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.CardGiftcard,
                    contentDescription = "Gift",
                    tint = DarkObsidian,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "GIFT VOUCHER",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = GlowingAmber
                    )
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 12.sp
                    )
                )
            }
        }
    }
}

/**
 * Animated iOS 3-Dot Typing Indicator Bubble
 */
@Composable
fun TypingIndicatorBubble(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    val dot1 by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "d1"
    )
    val dot2 by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "d2"
    )
    val dot3 by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "d3"
    )

    Row(
        modifier = modifier
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = 4.dp,
                bottomEnd = 18.dp
            ),
            color = IMessageGray,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier.padding(start = 6.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(7.dp).scale(dot1).clip(CircleShape).background(Color.White.copy(alpha = dot1)))
                Box(modifier = Modifier.size(7.dp).scale(dot2).clip(CircleShape).background(Color.White.copy(alpha = dot2)))
                Box(modifier = Modifier.size(7.dp).scale(dot3).clip(CircleShape).background(Color.White.copy(alpha = dot3)))
            }
        }
    }
}

/**
 * WebRTC Audio Call Dialog with CleanSignal™ HD DSP
 */
@Composable
fun AudioCallDialog(
    recipientName: String,
    recipientPhone: String,
    onDismiss: () -> Unit,
    onSwitchToVideo: () -> Unit = {}
) {
    WebRtcAudioCallDialog(
        recipientName = recipientName,
        recipientPhone = recipientPhone,
        onDismiss = onDismiss,
        onSwitchToVideo = onSwitchToVideo
    )
}

/**
 * WebRTC Video Call Dialog with CameraX & CleanSignal™
 */
@Composable
fun VideoCallDialog(
    recipientName: String,
    recipientPhone: String = "",
    onDismiss: () -> Unit
) {
    WebRtcVideoCallDialog(
        recipientName = recipientName,
        recipientPhone = recipientPhone,
        onDismiss = onDismiss
    )
}

/**
 * iOS '+' Action Sheet for Attachments (Camera, Photos, Audio Memo, Gift Data, Location)
 */
@Composable
fun IosAttachmentSheet(
    onSendPhoto: (String) -> Unit,
    onSendVoice: () -> Unit,
    onSendGift: (String) -> Unit,
    onSendLocation: () -> Unit,
    onSendDocument: (() -> Unit)? = null,
    onCloudBackup: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = DarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Flow Media & Cloud Backup",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = TextMuted)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                AttachmentItem(
                    icon = Icons.Filled.PhotoCamera,
                    label = "Camera",
                    bg = CyberCyan,
                    onClick = {
                        onSendPhoto("📷 Live Snapshot from Flow Camera")
                        onDismiss()
                    }
                )
                AttachmentItem(
                    icon = Icons.Filled.PhotoLibrary,
                    label = "Photos",
                    bg = Color(0xFF5856D6),
                    onClick = {
                        onSendPhoto("🖼️ Data Reseller Screenshot")
                        onDismiss()
                    }
                )
                AttachmentItem(
                    icon = Icons.Filled.Description,
                    label = "Document",
                    bg = Color(0xFFE53935),
                    onClick = {
                        onSendDocument?.invoke() ?: onSendPhoto("📄 Contract_Invoice_2026.pdf")
                        onDismiss()
                    }
                )
                AttachmentItem(
                    icon = Icons.Filled.CloudUpload,
                    label = "R2 Backup",
                    bg = Color(0xFFF6821F), // Cloudflare Orange
                    onClick = {
                        onCloudBackup?.invoke()
                        onDismiss()
                    }
                )
                AttachmentItem(
                    icon = Icons.Filled.CardGiftcard,
                    label = "Gift Data",
                    bg = GlowingAmber,
                    onClick = {
                        onSendGift("🎁 1GB High-Speed SME Data Gift")
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
fun AttachmentItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    bg: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(bg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = DarkObsidian,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}
