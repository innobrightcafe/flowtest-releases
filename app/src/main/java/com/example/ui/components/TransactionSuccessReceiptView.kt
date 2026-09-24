package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.util.PdfReceiptGenerator
import com.example.util.TransactionReceiptData
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class PurchaseSuccessReceipt(
    val serviceTitle: String,
    val providerOrType: String,
    val recipient: String,
    val amountPaid: Double,
    val reference: String,
    val newBalance: Double,
    val message: String = "Transaction processed successfully.",
    val bonusVpnTime: String = "+1 Hour Unlimited VPN Time",
    val bonusPoints: Int = 15,
    val timestamp: String = "Just Now",
    val balanceBefore: Double = 0.0,
    val isTransfer: Boolean = false,
    val isPending: Boolean = false,
    val status: String = if (isPending) "PENDING" else "SUCCESS",
    val tokenPin: String? = null,
    val meterUnits: String? = null,
    val customerName: String? = null,
    val meterNumber: String? = null,
    val serviceAddress: String? = null,
    val discoName: String? = null,
    val meterType: String? = null,
    val tariffClass: String? = null
)

private val NeonPurple = Color(0xFFA855F7)
private val NeonGold = Color(0xFFFFD700)
private val CyberBlue = Color(0xFF00F0FF)

// Data holder for gaming particles
private data class GameParticle(
    val initialX: Float,
    val initialY: Float,
    val speedX: Float,
    val speedY: Float,
    val size: Float,
    val color: Color,
    val isStar: Boolean,
    val rotationSpeed: Float
)

@Composable
fun TransactionSuccessReceiptView(
    receipt: PurchaseSuccessReceipt,
    onReturnHome: () -> Unit,
    onMakeAnotherPurchase: (() -> Unit)? = null,
    onSaveRecipient: ((name: String, type: String, identifier: String, provider: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var isRefCopied by remember { mutableStateOf(false) }
    var isRecipientSaved by remember { mutableStateOf(false) }
    var showSaveRecipientDialog by remember { mutableStateOf(false) }
    var customRecipientName by remember { mutableStateOf(receipt.recipient.substringBefore("(").trim()) }

    // Infinite animations for gaming effects
    val infiniteTransition = rememberInfiniteTransition(label = "gaming_effects")
    
    // Rotating radar ring
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_rotation"
    )

    // Pulsing energy badge
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // Shimmer glow alpha
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_alpha"
    )

    // Particle time counter
    val particleProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particles"
    )

    // XP Bar entry fill animation
    var xpBarAnimated by remember { mutableStateOf(false) }
    val animatedXpProgress by animateFloatAsState(
        targetValue = if (xpBarAnimated) 0.82f else 0.1f,
        animationSpec = tween(durationMillis = 1400, delayMillis = 300, easing = FastOutSlowInEasing),
        label = "xp_bar"
    )
    LaunchedEffect(Unit) {
        xpBarAnimated = true
    }

    // Generate random particles once
    val particles = remember {
        val colors = listOf(CyberCyan, ElectricEmerald, NeonGold, NeonPurple, CyberBlue, Color.White)
        List(35) {
            GameParticle(
                initialX = Random.nextFloat(),
                initialY = Random.nextFloat(),
                speedX = (Random.nextFloat() - 0.5f) * 0.4f,
                speedY = -(Random.nextFloat() * 0.5f + 0.3f),
                size = Random.nextFloat() * 8f + 4f,
                color = colors[Random.nextInt(colors.size)],
                isStar = Random.nextBoolean(),
                rotationSpeed = (Random.nextFloat() - 0.5f) * 720f
            )
        }
    }

    val isPending = receipt.isPending ||
            receipt.status.equals("PENDING", ignoreCase = true) ||
            receipt.status.contains("PENDING", ignoreCase = true) ||
            receipt.message.contains("pending", ignoreCase = true) ||
            receipt.message.contains("reconciliation", ignoreCase = true) ||
            receipt.message.contains("scheduled", ignoreCase = true) ||
            receipt.message.contains("queued", ignoreCase = true)

    val isElectricityService = remember(receipt) {
        receipt.serviceTitle.contains("Electric", ignoreCase = true) ||
                receipt.providerOrType.contains("Electric", ignoreCase = true) ||
                !receipt.meterNumber.isNullOrBlank() ||
                !receipt.discoName.isNullOrBlank() ||
                receipt.serviceTitle.contains("Token", ignoreCase = true)
    }

    val resolvedTokenPin = remember(receipt) {
        receipt.tokenPin ?: if (isElectricityService) {
            val regex = Regex("\\b\\d{4}-\\d{4}-\\d{4}-\\d{4}(-\\d{4})?\\b|\\b\\d{20}\\b")
            regex.find(receipt.message)?.value
        } else null
    }

    val receiptPdfData = remember(receipt, isPending, resolvedTokenPin) {
        TransactionReceiptData(
            transactionId = receipt.reference,
            reference = receipt.reference,
            title = receipt.serviceTitle,
            serviceType = receipt.providerOrType,
            recipient = receipt.recipient,
            amountPaid = receipt.amountPaid,
            senderName = "FlowTest User",
            senderAccount = "FlowTest Wallet",
            companyName = "FlowTest",
            status = if (isPending) "PENDING RECONCILIATION" else "SUCCESSFUL",
            balanceBefore = if (receipt.balanceBefore > 0) receipt.balanceBefore else null,
            balanceAfter = receipt.newBalance,
            narration = receipt.message,
            bonusInfo = "${receipt.bonusVpnTime} & +${receipt.bonusPoints} XP/Tokens",
            tokenPin = resolvedTokenPin ?: receipt.tokenPin,
            meterUnits = receipt.meterUnits,
            customerName = receipt.customerName,
            meterNumber = receipt.meterNumber,
            serviceAddress = receipt.serviceAddress,
            discoName = receipt.discoName,
            meterType = receipt.meterType,
            tariffClass = receipt.tariffClass
        )
    }

    Box(modifier = modifier.fillMaxWidth()) {
        // Floating Gaming Particles Canvas Overlay
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
        ) {
            val width = size.width
            val height = size.height

            particles.forEach { p ->
                val currentY = ((p.initialY + p.speedY * particleProgress) % 1f + 1f) % 1f * height
                val currentX = ((p.initialX + p.speedX * particleProgress) % 1f + 1f) % 1f * width
                val alpha = (sin(particleProgress * Math.PI.toFloat() * 2 + p.initialX * 10f) * 0.3f + 0.7f).coerceIn(0.2f, 1f)
                val rot = (p.rotationSpeed * particleProgress) % 360f

                rotate(rot, pivot = Offset(currentX, currentY)) {
                    if (p.isStar) {
                        drawDiamond(Offset(currentX, currentY), p.size, p.color.copy(alpha = alpha))
                    } else {
                        drawCircle(
                            color = p.color.copy(alpha = alpha),
                            radius = p.size / 2f,
                            center = Offset(currentX, currentY)
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = true,
            enter = fadeIn() + scaleIn(initialScale = 0.92f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ==========================================
                // 1. GAMING LEVEL-UP / VICTORY BADGE
                // ==========================================
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    // Outer Rotating Radar Ring
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .rotate(ringRotation)
                            .border(
                                width = 2.dp,
                                color = if (isPending) GlowingAmber else CyberCyan,
                                shape = CircleShape
                            )
                    )

                    // Middle Pulsing Energy Glow
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(if (isPending) GlowingAmber.copy(alpha = 0.25f) else CyberCyan.copy(alpha = 0.2f))
                    )

                    // Inner Victory Trophy / Shield Core
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceElevated)
                            .border(2.dp, if (isPending) GlowingAmber else CyberCyan, CircleShape)
                            .shadow(12.dp, CircleShape, spotColor = if (isPending) GlowingAmber else CyberCyan),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPending) Icons.Default.HourglassTop else Icons.Default.Check,
                            contentDescription = if (isPending) "Pending Reconciliation" else "Victory Success",
                            tint = if (isPending) GlowingAmber else CyberCyan,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Gaming Title Banner
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF0F172A).copy(alpha = 0.9f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isPending) GlowingAmber else CyberCyan
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("★", color = if (isPending) GlowingAmber else NeonGold, fontSize = 13.sp, fontWeight = FontWeight.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isPending) "ORDER PENDING" else "MISSION ACCOMPLISHED",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.8.sp,
                                color = Color.White,
                                fontSize = 11.sp
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("★", color = if (isPending) GlowingAmber else NeonGold, fontSize = 13.sp, fontWeight = FontWeight.Black)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (isPending) "Pending / Processing" else "Transaction Fulfilled!",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 0.5.sp,
                        fontSize = 20.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = if (isPending) "[STATUS: PENDING]" else "[0x99_OK • INSTANTLY SECURED & DELIVERED]",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = if (isPending) GlowingAmber.copy(alpha = 0.9f) else CyberCyan.copy(alpha = 0.85f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                // ==========================================
                // 2. GAMING REWARDS & XP LOOT BOX CARD
                // ==========================================
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .border(
                            1.5.dp,
                            CyberCyan,
                            RoundedCornerShape(18.dp)
                        ),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        // Level & XP Bar Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = NeonPurple.copy(alpha = 0.25f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonPurple)
                                ) {
                                    Text(
                                        text = "LVL 8 VIP",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Black,
                                            color = NeonPurple,
                                            fontSize = 9.sp
                                        ),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "PLAYER REWARDS UNLOCKED",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        letterSpacing = 1.sp,
                                        fontSize = 10.sp
                                    )
                                )
                            }

                            Text(
                                text = "+${receipt.bonusPoints * 10 + 100} XP",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    color = NeonGold,
                                    fontSize = 11.sp
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Animated XP Progress Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF1E293B))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(animatedXpProgress)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(CyberCyan)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // 3-Loot Buff Badges
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Buff 1: VPN Shield
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF1E1B4B),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.5f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("🛡️", fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = receipt.bonusVpnTime.take(12),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFFA5B4FC),
                                            fontSize = 9.sp
                                        ),
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "VIP Shield",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = TextMuted,
                                            fontSize = 8.sp
                                        )
                                    )
                                }
                            }

                            // Buff 2: Reward Tokens
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF2E1065),
                                border = androidx.compose.foundation.BorderStroke(1.dp, NeonPurple.copy(alpha = 0.5f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("⚡", fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "+${receipt.bonusPoints} Tokens",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Black,
                                            color = NeonPurple,
                                            fontSize = 9.sp
                                        ),
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "FlowCoins",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = TextMuted,
                                            fontSize = 8.sp
                                        )
                                    )
                                }
                            }

                            // Buff 3: Speed Multiplier
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF064E3B),
                                border = androidx.compose.foundation.BorderStroke(1.dp, ElectricEmerald.copy(alpha = 0.5f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("🚀", fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "2.5x Turbo",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Black,
                                            color = ElectricEmerald,
                                            fontSize = 9.sp
                                        ),
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "Route Boost",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = TextMuted,
                                            fontSize = 8.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ==========================================
                // 3. UPDATED ENERGY / WALLET BALANCE HUD
                // ==========================================
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            1.5.dp,
                            CyberCyan,
                            RoundedCornerShape(16.dp)
                        ),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0C1B2A))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "CURRENT ENERGY POOL (WALLET)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    color = CyberCyan,
                                    letterSpacing = 1.sp,
                                    fontSize = 9.sp
                                )
                            )
                            Text(
                                text = "₦${String.format("%,.2f", receipt.newBalance)}",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    fontSize = 20.sp
                                )
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isPending) GlowingAmber.copy(alpha = 0.18f) else ElectricEmerald.copy(alpha = 0.18f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isPending) GlowingAmber.copy(alpha = 0.6f) else ElectricEmerald.copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(if (isPending) GlowingAmber else ElectricEmerald)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isPending) "PENDING" else "SYNCED",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        color = if (isPending) GlowingAmber else ElectricEmerald,
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ==========================================
                // ⚡ PREPAID ELECTRICITY TOKEN PIN & DOWNLOAD INSTRUCTION
                // ==========================================
                if (isElectricityService) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .border(
                                2.dp,
                                Brush.horizontalGradient(listOf(ElectricEmerald, CyberCyan, GlowingAmber)),
                                RoundedCornerShape(20.dp)
                            ),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF042118))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text("⚡", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "PREPAID ELECTRICITY TOKEN",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        color = ElectricEmerald,
                                        letterSpacing = 1.2.sp,
                                        fontSize = 13.sp
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Prominent instruction to download the PDF to get the token
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = ElectricEmerald.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, ElectricEmerald.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = null,
                                        tint = ElectricEmerald,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Purchase successful! Download your official PDF receipt to get your 20-digit meter token and verified proof of address.",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.5.sp,
                                            lineHeight = 15.sp
                                        )
                                    )
                                }
                            }

                            if (!resolvedTokenPin.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(10.dp))

                                // Large Bold Token Display
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF031410),
                                    border = BorderStroke(1.5.dp, ElectricEmerald.copy(alpha = 0.8f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = resolvedTokenPin,
                                            style = MaterialTheme.typography.headlineMedium.copy(
                                                fontWeight = FontWeight.Black,
                                                fontFamily = FontFamily.Monospace,
                                                color = Color.White,
                                                letterSpacing = 2.sp,
                                                fontSize = 22.sp
                                            ),
                                            textAlign = TextAlign.Center
                                        )
                                        if (!receipt.meterUnits.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Estimated Units: ${receipt.meterUnits}",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = CyberCyan,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Copy PIN Button
                                var isPinCopied by remember { mutableStateOf(false) }
                                Button(
                                    onClick = {
                                        val cleanDigits = resolvedTokenPin.replace("-", "").trim()
                                        clipboardManager.setText(AnnotatedString(cleanDigits))
                                        isPinCopied = true
                                        Toast.makeText(context, "Meter Token PIN copied!", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isPinCopied) ElectricEmerald else CyberCyan,
                                        contentColor = DarkObsidian
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(42.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPinCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isPinCopied) "COPIED TO CLIPBOARD" else "COPY TOKEN PIN",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Download PDF button to get token & address
                            Button(
                                onClick = {
                                    PdfReceiptGenerator.downloadReceiptPdf(context, receiptPdfData) { file ->
                                        PdfReceiptGenerator.openOrShareReceipt(context, file)
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ElectricEmerald,
                                    contentColor = DarkObsidian
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "DOWNLOAD PDF RECEIPT (GET TOKEN)",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.5.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Crucial Meter Notice: The system cannot recharge the meter directly!
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = GlowingAmber.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, GlowingAmber.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text("⚠️", fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "The system cannot recharge your meter directly. You must manually enter the 20-digit PIN from your PDF receipt into your prepaid meter keypad, then press the Enter (↵) key to load electricity.",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = GlowingAmber,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            lineHeight = 15.sp
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                // =======================================================
                // 🏛️ VERIFIED UTILITY PREMISES & PROOF OF ADDRESS CARD
                // =======================================================
                val hasUtilityVerification = !receipt.customerName.isNullOrBlank() ||
                        !receipt.serviceAddress.isNullOrBlank() ||
                        !receipt.meterNumber.isNullOrBlank()

                if (hasUtilityVerification) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .border(
                                1.5.dp,
                                Brush.horizontalGradient(listOf(ElectricEmerald, CyberCyan)),
                                RoundedCornerShape(18.dp)
                            ),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF041913))
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
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = "Verified Utility",
                                        tint = ElectricEmerald,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "METER & SERVICE DETAILS",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Black,
                                            color = ElectricEmerald,
                                            letterSpacing = 1.sp,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                                Surface(
                                    color = ElectricEmerald.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "OFFICIAL RECORD",
                                        color = ElectricEmerald,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 9.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Customer Name
                            if (!receipt.customerName.isNullOrBlank()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text("Registered Customer", color = TextSecondary, fontSize = 11.sp)
                                    Text(
                                        text = receipt.customerName,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.fillMaxWidth(0.65f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                            }

                            // Meter Number
                            val displayedMeter = receipt.meterNumber ?: receipt.recipient
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Meter / Account No.", color = TextSecondary, fontSize = 11.sp)
                                Text(
                                    text = displayedMeter,
                                    color = CyberCyan,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Service / Premises Address
                            if (!receipt.serviceAddress.isNullOrBlank()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text("Service Address", color = TextSecondary, fontSize = 11.sp)
                                    Text(
                                        text = receipt.serviceAddress,
                                        color = Color(0xFFE2E8F0),
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.fillMaxWidth(0.68f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                            }

                            // Distribution Company & Tariff
                            if (!receipt.discoName.isNullOrBlank() || !receipt.tariffClass.isNullOrBlank()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("DISCO & Tariff", color = TextSecondary, fontSize = 11.sp)
                                    Text(
                                        text = "${receipt.discoName ?: receipt.providerOrType} • ${receipt.tariffClass ?: "Band A Residential"}",
                                        color = TextMuted,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.fillMaxWidth(0.65f)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                // ==========================================
                // 4. SCI-FI HUD TELEMETRY BREAKDOWN CARD
                // ==========================================
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // Section Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "┌ TELEMETRY READOUT",
                                fontFamily = FontFamily.Monospace,
                                color = TextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "SECURED ┐",
                                fontFamily = FontFamily.Monospace,
                                color = TextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Service & Network Node
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Service Package", color = TextSecondary, fontSize = 12.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TelcoLogo(network = receipt.providerOrType, size = 18.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${receipt.providerOrType.uppercase()} • ${receipt.serviceTitle}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Recipient Target
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Target Recipient", color = TextSecondary, fontSize = 12.sp)
                            Text(
                                text = receipt.recipient,
                                color = CyberCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        if (!receipt.customerName.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Verified Beneficiary", color = TextSecondary, fontSize = 12.sp)
                                Text(
                                    text = receipt.customerName,
                                    color = ElectricEmerald,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.fillMaxWidth(0.65f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Credits Debited
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Credits Debited", color = TextSecondary, fontSize = 12.sp)
                            Text(
                                text = "₦${String.format("%,.2f", receipt.amountPaid)}",
                                color = GlowingAmber,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Reference Hash + Copy Action
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Cipher Ref", color = TextSecondary, fontSize = 12.sp)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = DarkSurfaceElevated,
                                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        clipboardManager.setText(AnnotatedString(receipt.reference))
                                        isRefCopied = true
                                        Toast.makeText(context, "Cipher Ref copied to clipboard", Toast.LENGTH_SHORT).show()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = receipt.reference.take(16) + if (receipt.reference.length > 16) "..." else "",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        color = if (isRefCopied) ElectricEmerald else CyberCyan,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = if (isRefCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                        contentDescription = "Copy",
                                        tint = if (isRefCopied) ElectricEmerald else CyberCyan,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Node Status
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Status Telemetry", color = TextSecondary, fontSize = 12.sp)
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isPending) GlowingAmber.copy(alpha = 0.15f) else ElectricEmerald.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = if (isPending) "PENDING" else "FULFILLED // 100% ONLINE",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isPending) GlowingAmber else ElectricEmerald,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                if (isPending) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, GlowingAmber.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF261D08))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.HourglassTop,
                                contentDescription = "Pending",
                                tint = GlowingAmber,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Status: Pending / Processing",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = GlowingAmber,
                                    fontSize = 12.5.sp
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ==========================================
                // 5. OFFICIAL HOLO-RECEIPT (PDF)
                // ==========================================
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, CyberCyan.copy(alpha = 0.35f), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2027))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = "PDF Receipt",
                                    tint = Color(0xFFFF5252),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Official Holo-Receipt (PDF)",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 12.sp
                                    )
                                )
                            }

                            Text(
                                text = "FlowTest VIP",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextMuted,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Download PDF Button
                            Button(
                                onClick = {
                                    PdfReceiptGenerator.downloadReceiptPdf(context, receiptPdfData)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated, contentColor = CyberCyan),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Download, contentDescription = "Download", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            // Share PDF Button
                            Button(
                                onClick = {
                                    PdfReceiptGenerator.shareReceiptPdf(context, receiptPdfData)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ==========================================
                // 6. SAVE ALLY TO SQUAD / BENEFICIARY
                // ==========================================
                if (onSaveRecipient != null && receipt.recipient.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isRecipientSaved) ElectricEmerald.copy(alpha = 0.12f) else DarkSurface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isRecipientSaved) ElectricEmerald.copy(alpha = 0.5f) else DarkCardBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val isElectricity = !receipt.meterNumber.isNullOrBlank() || receipt.serviceTitle.contains("Electricity", ignoreCase = true)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (isRecipientSaved) Icons.Default.BookmarkAdded else if (isElectricity) Icons.Default.ElectricMeter else Icons.Default.BookmarkAdd,
                                    contentDescription = "Save Recipient",
                                    tint = if (isRecipientSaved) ElectricEmerald else GlowingAmber,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = if (isRecipientSaved) (if (isElectricity) "Meter Saved for Later!" else "Beneficiary Saved!") else (if (isElectricity) "Save Meter Number" else "Save Beneficiary"),
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isRecipientSaved) ElectricEmerald else Color.White,
                                            fontSize = 11.sp
                                        )
                                    )
                                    Text(
                                        text = if (isRecipientSaved) "Saved to your list for 1-tap recharges" else "Save for 1-tap rapid recharges",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = TextSecondary,
                                            fontSize = 9.sp
                                        )
                                    )
                                }
                            }

                            if (!isRecipientSaved) {
                                Button(
                                    onClick = { showSaveRecipientDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = GlowingAmber, contentColor = DarkObsidian),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text(if (isElectricity) "SAVE METER" else "SAVE", fontSize = 10.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ==========================================
                // 7. HIGH-ENERGY ACTION BUTTONS
                // ==========================================
                // Primary: Claim Rewards & Return
                Button(
                    onClick = onReturnHome,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberCyan,
                        contentColor = DarkObsidian
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .shadow(8.dp, RoundedCornerShape(14.dp), spotColor = CyberCyan)
                ) {
                    Icon(
                        imageVector = Icons.Default.SportsEsports,
                        contentDescription = "Claim",
                        tint = DarkObsidian,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CLAIM REWARDS & RETURN",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Black,
                            color = DarkObsidian,
                            letterSpacing = 1.sp,
                            fontSize = 13.sp
                        )
                    )
                }

                if (onMakeAnotherPurchase != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onMakeAnotherPurchase,
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Replay, contentDescription = "Re-deploy", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "RAPID RE-DEPLOY (BUY AGAIN)",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = CyberCyan,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }

    // Modal to customize recipient name/nickname before saving
    if (showSaveRecipientDialog) {
        val isMeter = !receipt.meterNumber.isNullOrBlank() || receipt.serviceTitle.contains("Electricity", ignoreCase = true)
        AlertDialog(
            onDismissRequest = { showSaveRecipientDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isMeter) Icons.Default.ElectricBolt else Icons.Default.BookmarkAdd,
                        contentDescription = null,
                        tint = GlowingAmber
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isMeter) "Save Electricity Meter" else "Save Ally / Beneficiary", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column {
                    Text(
                        text = if (isMeter) {
                            "Save this meter number and customer details for 1-tap recharges."
                        } else {
                            "Save this recipient details for instant 1-tap re-use in future transactions."
                        },
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 12.sp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = customRecipientName,
                        onValueChange = { customRecipientName = it },
                        label = { Text(if (isMeter) "Customer Name / Label" else "Recipient Name / Nickname") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan, unfocusedBorderColor = DarkCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isMeter) "Meter Number: ${receipt.meterNumber ?: receipt.recipient}" else "Identifier: ${receipt.recipient}",
                        style = MaterialTheme.typography.labelSmall.copy(color = CyberCyan, fontSize = 11.sp)
                    )
                    Text(
                        text = "Service: ${receipt.providerOrType.uppercase()} • ${receipt.serviceTitle}",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 10.sp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanIdentifier = (receipt.meterNumber ?: receipt.recipient).trim()
                        val serviceCategory = if (isMeter) {
                            "electricity"
                        } else if (receipt.isTransfer || receipt.serviceTitle.contains("Transfer", ignoreCase = true)) {
                            "transfer"
                        } else if (receipt.serviceTitle.contains("Airtime", ignoreCase = true)) {
                            "airtime"
                        } else if (receipt.serviceTitle.contains("Data", ignoreCase = true)) {
                            "data"
                        } else if (receipt.serviceTitle.contains("Cable", ignoreCase = true)) {
                            "cable"
                        } else {
                            "other"
                        }

                        onSaveRecipient?.invoke(
                            customRecipientName.ifBlank { if (isMeter) receipt.customerName ?: "Electricity Meter" else "Beneficiary" },
                            serviceCategory,
                            cleanIdentifier,
                            receipt.discoName ?: receipt.providerOrType
                        )
                        isRecipientSaved = true
                        showSaveRecipientDialog = false
                        val successMsg = if (isMeter) "Meter '${cleanIdentifier}' saved successfully!" else "Recipient '${customRecipientName}' saved successfully!"
                        Toast.makeText(context, successMsg, Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(if (isMeter) "Save Meter" else "Save Beneficiary", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveRecipientDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            },
            containerColor = DarkSurfaceElevated
        )
    }
}

// Custom DrawScope helper for drawing diamond star particles
private fun DrawScope.drawDiamond(center: Offset, size: Float, color: Color) {
    val path = Path().apply {
        moveTo(center.x, center.y - size)
        lineTo(center.x + size * 0.7f, center.y)
        lineTo(center.x, center.y + size)
        lineTo(center.x - size * 0.7f, center.y)
        close()
    }
    drawPath(path, color = color)
}
