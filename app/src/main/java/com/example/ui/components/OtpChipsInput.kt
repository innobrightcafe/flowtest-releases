package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkObsidian
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ElectricEmerald
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningRed

/**
 * 21st.dev & UI/UX Pro Max Segmented Chip Code Input with Integrated One-Tap Paste.
 *
 * Displays:
 *  - 6 individual animated digit chips with active focus glow and filled states
 *  - A stylish "PASTE" button placed directly beside the chips
 *  - Smart clipboard parser: extracts 6-digit numeric sequences from any copied text
 *  - Keyboard interaction via underlying transparent BasicTextField
 */
@Composable
fun OtpChipsInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    codeLength: Int = 6,
    isError: Boolean = false,
    errorMessage: String? = null,
    onComplete: ((String) -> Unit)? = null,
    testTagPrefix: String = "otp_chip"
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val clipboardManager = LocalClipboardManager.current
    var pasteFeedback by remember { mutableStateOf<String?>(null) }

    // Pulsing cursor animation for currently focused chip
    val infiniteTransition = rememberInfiniteTransition(label = "otp_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    // Clear paste feedback after 2.5 seconds
    LaunchedEffect(pasteFeedback) {
        if (pasteFeedback != null) {
            kotlinx.coroutines.delay(2500L)
            pasteFeedback = null
        }
    }

    // Auto-focus when initialized
    LaunchedEffect(Unit) {
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    // Helper to safely extract 6 digits from clipboard
    fun handlePaste() {
        val clipText = clipboardManager.getText()?.text ?: ""
        if (clipText.isBlank()) {
            pasteFeedback = "Clipboard empty"
            return
        }

        // Try to find consecutive 6 digits
        val sixDigitRegex = Regex("\\b\\d{6}\\b")
        val match = sixDigitRegex.find(clipText)?.value

        val extracted = if (match != null) {
            match
        } else {
            // Otherwise extract all numeric digits up to codeLength
            val digitsOnly = clipText.filter { it.isDigit() }
            if (digitsOnly.length >= codeLength) {
                digitsOnly.take(codeLength)
            } else {
                digitsOnly
            }
        }

        if (extracted.isNotBlank()) {
            val finalCode = extracted.take(codeLength)
            onValueChange(finalCode)
            pasteFeedback = "Code pasted!"
            if (finalCode.length == codeLength) {
                focusManager.clearFocus()
                onComplete?.invoke(finalCode)
            }
        } else {
            pasteFeedback = "No numbers copied"
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Main Container: 6 Chips + Paste Button
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            // Hidden BasicTextField capturing raw keyboard input
            BasicTextField(
                value = value,
                onValueChange = { newVal ->
                    val filtered = newVal.filter { it.isDigit() }.take(codeLength)
                    onValueChange(filtered)
                    if (filtered.length == codeLength) {
                        focusManager.clearFocus()
                        onComplete?.invoke(filtered)
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        if (value.length == codeLength) {
                            onComplete?.invoke(value)
                        }
                    }
                ),
                cursorBrush = SolidColor(Color.Transparent),
                modifier = Modifier
                    .size(1.dp)
                    .focusRequester(focusRequester)
                    .testTag("${testTagPrefix}_input")
            )

            // Chips Row with Paste Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // The 6 Segmented Chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            focusRequester.requestFocus()
                        }
                ) {
                    for (i in 0 until codeLength) {
                        val isFocused = (i == value.length && value.length < codeLength)
                        val isFilled = i < value.length
                        val digitChar = if (isFilled) value[i].toString() else ""

                        val borderColor by animateColorAsState(
                            targetValue = when {
                                isError -> WarningRed
                                isFilled -> ElectricEmerald.copy(alpha = 0.85f)
                                isFocused -> CyberCyan
                                else -> DarkCardBorder
                            },
                            animationSpec = tween(200),
                            label = "border_color_$i"
                        )

                        val backgroundColor by animateColorAsState(
                            targetValue = when {
                                isFocused -> DarkSurfaceElevated
                                isFilled -> DarkSurface
                                else -> DarkObsidian.copy(alpha = 0.6f)
                            },
                            animationSpec = tween(200),
                            label = "bg_color_$i"
                        )

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = backgroundColor,
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isFocused || isFilled) 1.5.dp else 1.dp,
                                color = borderColor
                            ),
                            modifier = Modifier
                                .size(width = 40.dp, height = 48.dp)
                                .testTag("${testTagPrefix}_$i")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (isFilled) {
                                    Text(
                                        text = digitChar,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.Monospace,
                                            color = TextPrimary
                                        ),
                                        textAlign = TextAlign.Center
                                    )
                                } else if (isFocused) {
                                    // Animated active cursor indicator
                                    Box(
                                        modifier = Modifier
                                            .width(2.dp)
                                            .height(20.dp)
                                            .background(
                                                CyberCyan.copy(alpha = pulseAlpha),
                                                RoundedCornerShape(1.dp)
                                            )
                                    )
                                } else {
                                    // Placeholder subtle dot
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .background(
                                                TextMuted.copy(alpha = 0.35f),
                                                RoundedCornerShape(2.5.dp)
                                            )
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // The Beside "PASTE" Button Chip
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CyberCyan.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { handlePaste() }
                        .testTag("otp_paste_btn")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = "Paste OTP code from clipboard",
                            tint = CyberCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "PASTE",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = CyberCyan,
                                fontSize = 11.sp,
                                letterSpacing = 0.6.sp
                            )
                        )
                    }
                }
            }
        }

        // Paste Toast Feedback or Error feedback
        if (pasteFeedback != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = ElectricEmerald,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = pasteFeedback ?: "",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = ElectricEmerald,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp
                    )
                )
            }
        } else if (isError && errorMessage != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = WarningRed,
                    fontSize = 11.sp
                )
            )
        }
    }
}
