package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.BuildConfig
import com.example.ui.theme.*
import com.example.util.AppUpdateInfo
import com.example.util.AppUpdateManager
import com.example.util.UpdateStatus

@Composable
fun AppUpdateDialog(
    status: UpdateStatus,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var permissionNote by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = {
            if (status !is UpdateStatus.Downloading) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = status !is UpdateStatus.Downloading,
            dismissOnClickOutside = status !is UpdateStatus.Downloading,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .border(
                    BorderStroke(
                        1.2.dp,
                        Brush.verticalGradient(
                            listOf(CyberCyan, DarkCardBorder, ElectricEmerald.copy(alpha = 0.5f))
                        )
                    ),
                    RoundedCornerShape(24.dp)
                ),
            color = DarkSurfaceElevated,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (status) {
                    is UpdateStatus.Checking -> {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(CyberCyan.copy(alpha = 0.12f))
                                .border(1.5.dp, CyberCyan.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Checking Updates",
                                tint = CyberCyan,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Checking for Updates...",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Connecting to FlowTest Cloud to verify release integrity.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        FlowLoadingLine(
                            modifier = Modifier.fillMaxWidth(0.75f),
                            color = CyberCyan,
                            trackColor = DarkSurfaceElevated,
                            height = 4.dp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        androidx.compose.material3.TextButton(
                            onClick = onDismiss
                        ) {
                            Text(
                                text = "Cancel & Continue",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                            )
                        }
                    }

                    is UpdateStatus.Available -> {
                        val info = status.info
                        FlowtestEmblem(
                            size = 64.dp,
                            primaryColor = CyberCyan,
                            glowAlpha = 0.35f,
                            animateRotationOnLoad = true
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = CyberCyan.copy(alpha = 0.15f),
                                border = BorderStroke(0.8.dp, CyberCyan)
                            ) {
                                Text(
                                    text = "NEW VERSION AVAILABLE",
                                    color = CyberCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "FlowTest v${info.latestVersionName}",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Black,
                                color = TextPrimary
                            )
                        )

                        Text(
                            text = "Current: v${BuildConfig.VERSION_NAME} • Build ${BuildConfig.VERSION_CODE}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextMuted,
                                fontFamily = FontFamily.Monospace
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Release Notes Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkObsidian),
                            border = BorderStroke(0.8.dp, DarkCardBorder)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.NewReleases,
                                        contentDescription = null,
                                        tint = GlowingAmber,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "What's New in this Release",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = GlowingAmber,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = info.releaseNotes,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextSecondary,
                                        fontSize = 11.5.sp,
                                        lineHeight = 16.sp
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Safe Update Guarantee Note
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ElectricEmerald.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = ElectricEmerald,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Zero Data Loss: Wallet balance, PIN, and settings are 100% preserved. No uninstall required.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = ElectricEmerald,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Google Play Protect Notice
                        GooglePlayProtectNoticeCard()

                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = {
                                AppUpdateManager.startBackgroundDownload(context, info)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberCyan,
                                contentColor = DarkObsidian
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "DOWNLOAD & UPDATE NOW",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                )
                            )
                        }

                        if (!info.isMandatory) {
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(
                                onClick = onDismiss,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Remind Me Later",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = TextMuted,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }

                    is UpdateStatus.Downloading -> {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(CyberCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = "Downloading",
                                tint = CyberCyan,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Downloading Update...",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "FlowTest v${status.info.latestVersionName} is updating in the background.",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        FlowLoadingLine(
                            progress = status.progressPercent / 100f,
                            modifier = Modifier.fillMaxWidth(),
                            color = CyberCyan,
                            trackColor = DarkObsidian,
                            height = 6.dp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${status.progressPercent}% Completed",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = CyberCyan,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            val mbDownloaded = status.downloadedBytes / (1024.0 * 1024.0)
                            val mbTotal = status.totalBytes / (1024.0 * 1024.0)
                            Text(
                                text = String.format("%.1f MB / %.1f MB", mbDownloaded, mbTotal),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextMuted,
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "You can safely close this screen or switch apps; download will finish in background.",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextMuted,
                                textAlign = TextAlign.Center,
                                fontSize = 10.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                            border = BorderStroke(0.8.dp, DarkCardBorder)
                        ) {
                            Text("RUN IN BACKGROUND")
                        }
                    }

                    is UpdateStatus.ReadyToInstall -> {
                        val info = status.info
                        val apkFile = status.apkFile

                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(ElectricEmerald.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Ready",
                                tint = ElectricEmerald,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = ElectricEmerald.copy(alpha = 0.15f),
                            border = BorderStroke(0.8.dp, ElectricEmerald)
                        ) {
                            Text(
                                text = "UPDATE READY TO INSTALL",
                                color = ElectricEmerald,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Upgrade to v${info.latestVersionName}",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                color = TextPrimary
                            )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "The update package is verified and ready. Tap below to install over your existing installation. You will NOT lose your login or data!",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                textAlign = TextAlign.Center,
                                lineHeight = 16.sp
                            )
                        )

                        permissionNote?.let { note ->
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = note,
                                color = GlowingAmber,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Google Play Protect Notice
                        GooglePlayProtectNoticeCard()

                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = {
                                val result = AppUpdateManager.installUpdate(context, apkFile)
                                result.onFailure { error ->
                                    permissionNote = error.message
                                    Toast.makeText(context, error.message ?: "Installation error", Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberCyan,
                                contentColor = DarkObsidian
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "INSTALL UPDATE (KEEP ALL DATA)",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = {
                                try {
                                    val downloadsIntent = Intent(android.app.DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(downloadsIntent)
                                } catch (_: Exception) {
                                    Toast.makeText(context, "APK is available in your device Downloads folder", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                            border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("VIEW SAVED APK IN DOWNLOADS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Install Later",
                                style = MaterialTheme.typography.labelMedium.copy(color = TextMuted)
                            )
                        }
                    }

                    is UpdateStatus.UpToDate -> {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(ElectricEmerald.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = "Up to Date",
                                tint = ElectricEmerald,
                                modifier = Modifier.size(34.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "You're on the Latest Version!",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = TextPrimary
                            )
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "FlowTest v${status.currentVersionName} (Build ${BuildConfig.VERSION_CODE}) is currently running with the latest military-grade VPN protocols and zero-trust banking.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                textAlign = TextAlign.Center,
                                lineHeight = 16.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Google Play Protect Notice
                        GooglePlayProtectNoticeCard()

                        Spacer(modifier = Modifier.height(14.dp))

                        // Interactive self-test installer verification
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkObsidian),
                            border = BorderStroke(0.8.dp, DarkCardBorder)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "DEVICE UPDATE COMPATIBILITY",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = CyberCyan,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 9.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Want to test that your phone allows updating without uninstalling? Tap below to test the in-app installer.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextMuted,
                                        fontSize = 10.5.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedButton(
                                    onClick = {
                                        AppUpdateManager.createSelfTestUpdate(context)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                                    border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.6f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "VERIFY IN-APP INSTALLER ON THIS PHONE",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = onDismiss,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberCyan,
                                contentColor = DarkObsidian
                            )
                        ) {
                            Text("DONE", fontWeight = FontWeight.Black)
                        }
                    }

                    is UpdateStatus.Error -> {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF5252).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = "Error",
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Update Check Notice",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = status.message,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Alternative Direct Download via Browser
                        OutlinedButton(
                            onClick = {
                                try {
                                    val browserIntent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://github.com/innobrightcafe/flowtest-releases/releases/latest/download/FlowTest.apk")
                                    )
                                    context.startActivity(browserIntent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Could not launch browser: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                            border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.6f))
                        ) {
                            Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("DOWNLOAD APK DIRECTLY IN BROWSER", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Google Play Protect Notice
                        GooglePlayProtectNoticeCard()

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("CLOSE")
                            }
                            Button(
                                onClick = { AppUpdateManager.checkForUpdates(context) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian)
                            ) {
                                Text("RETRY CHECK", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    UpdateStatus.Idle -> {
                        // Not showing anything
                    }
                }
            }
        }
    }
}

/**
 * Floating in-app pill banner that appears when an update has finished downloading in background.
 */
@Composable
fun AppUpdateFloatingBanner(
    status: UpdateStatus,
    onOpenDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = status is UpdateStatus.ReadyToInstall || status is UpdateStatus.Downloading,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(
                    BorderStroke(
                        1.dp,
                        if (status is UpdateStatus.ReadyToInstall) ElectricEmerald else CyberCyan
                    ),
                    RoundedCornerShape(16.dp)
                ),
            color = DarkSurfaceElevated,
            tonalElevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                if (status is UpdateStatus.ReadyToInstall)
                                    ElectricEmerald.copy(alpha = 0.15f)
                                else CyberCyan.copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (status is UpdateStatus.ReadyToInstall)
                                Icons.Default.Bolt
                            else Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = if (status is UpdateStatus.ReadyToInstall)
                                ElectricEmerald
                            else CyberCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = if (status is UpdateStatus.ReadyToInstall)
                                "⚡ Update Downloaded & Ready"
                            else "Downloading Update...",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Black,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = if (status is UpdateStatus.ReadyToInstall)
                                "Tap to install without losing any data"
                            else "${(status as UpdateStatus.Downloading).progressPercent}% completed in background",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (status is UpdateStatus.ReadyToInstall)
                                    ElectricEmerald
                                else CyberCyan,
                                fontSize = 10.sp
                            )
                        )
                    }
                }

                Button(
                    onClick = onOpenDialog,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberCyan,
                        contentColor = DarkObsidian
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (status is UpdateStatus.ReadyToInstall) "INSTALL" else "VIEW",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

/**
 * High-visibility security notice explaining why Google Play Protect alerts appear for direct APKs
 * and how users should undermine/bypass the alert to complete updates safely.
 */
@Composable
fun GooglePlayProtectNoticeCard(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = GlowingAmber.copy(alpha = 0.12f)),
        border = BorderStroke(1.dp, GlowingAmber.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(GlowingAmber.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = GlowingAmber,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Google Play Protect Notice",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Black,
                                color = GlowingAmber,
                                fontSize = 11.5.sp
                            )
                        )
                        Text(
                            text = "Undermine / bypass the Google warning",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextMuted,
                                fontSize = 9.sp
                            )
                        )
                    }
                }
                Surface(
                    color = GlowingAmber.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "SAFE & DIRECT",
                        color = GlowingAmber,
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Because this app is installed directly from FlowTest and is not on Google Play Store, Android & Google Play Protect may show a warning: \"Unrecognized app\" or \"Blocked by Play Protect\". Please ignore / undermine this Google alert:",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextPrimary.copy(alpha = 0.9f),
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                color = DarkObsidian,
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(0.6.dp, DarkCardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Text("1.", color = GlowingAmber, fontWeight = FontWeight.Bold, fontSize = 9.sp, modifier = Modifier.width(14.dp))
                        Text("When the Google popup appears, tap \"More details\" (or small down arrow ⌵).", color = TextSecondary, fontSize = 9.sp, lineHeight = 12.sp)
                    }
                    Row(verticalAlignment = Alignment.Top) {
                        Text("2.", color = GlowingAmber, fontWeight = FontWeight.Bold, fontSize = 9.sp, modifier = Modifier.width(14.dp))
                        Text("Tap \"Install anyway\" (or \"Keep app\") to proceed.", color = TextSecondary, fontSize = 9.sp, lineHeight = 12.sp)
                    }
                    Row(verticalAlignment = Alignment.Top) {
                        Text("3.", color = ElectricEmerald, fontWeight = FontWeight.Bold, fontSize = 9.sp, modifier = Modifier.width(14.dp))
                        Text("100% safe: All your funds, VPN tokens, and data stay intact.", color = TextSecondary, fontSize = 9.sp, lineHeight = 12.sp)
                    }
                }
            }
        }
    }
}
