package com.example.ui.components

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.ui.theme.*
import com.example.util.AppUpdateManager
import com.example.util.UpdateStatus

@Composable
fun AppUpdateSettingsCard(
    onOpenFullDialog: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val updateStatus by AppUpdateManager.updateStatus.collectAsStateWithLifecycle()
    val isAutoUpdateEnabled by AppUpdateManager.isAutoUpdateEnabled.collectAsStateWithLifecycle()
    val lastCheckTime by AppUpdateManager.lastCheckedTimestamp.collectAsStateWithLifecycle()

    var showDialog by remember { mutableStateOf(false) }

    if (showDialog && updateStatus !is UpdateStatus.Idle) {
        AppUpdateDialog(
            status = updateStatus,
            onDismiss = { showDialog = false }
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(
                BorderStroke(
                    1.2.dp,
                    if (updateStatus is UpdateStatus.ReadyToInstall)
                        ElectricEmerald.copy(alpha = 0.8f)
                    else CyberCyan.copy(alpha = 0.5f)
                ),
                RoundedCornerShape(20.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (updateStatus is UpdateStatus.ReadyToInstall)
                                    ElectricEmerald.copy(alpha = 0.15f)
                                else CyberCyan.copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (updateStatus is UpdateStatus.ReadyToInstall)
                                Icons.Default.Bolt
                            else Icons.Default.SystemUpdate,
                            contentDescription = "Updates",
                            tint = if (updateStatus is UpdateStatus.ReadyToInstall)
                                ElectricEmerald
                            else CyberCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "APP UPDATES & VERSION CONTROL",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = TextPrimary,
                                letterSpacing = 0.5.sp
                            )
                        )
                        Text(
                            text = "Update directly without uninstalling or losing data",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (updateStatus is UpdateStatus.ReadyToInstall)
                        ElectricEmerald.copy(alpha = 0.15f)
                    else CyberCyan.copy(alpha = 0.15f),
                    border = BorderStroke(
                        0.8.dp,
                        if (updateStatus is UpdateStatus.ReadyToInstall)
                            ElectricEmerald
                        else CyberCyan
                    )
                ) {
                    Text(
                        text = "v${BuildConfig.VERSION_NAME}",
                        color = if (updateStatus is UpdateStatus.ReadyToInstall)
                            ElectricEmerald
                        else CyberCyan,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Current Version & Status Info Box
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = DarkObsidian,
                border = BorderStroke(0.8.dp, DarkCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "CURRENT BUILD",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextMuted,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "FlowTest v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp
                                )
                            )
                        }

                        // Status Badge
                        when (updateStatus) {
                            is UpdateStatus.ReadyToInstall -> {
                                Surface(
                                    color = ElectricEmerald.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(0.8.dp, ElectricEmerald)
                                ) {
                                    Text(
                                        text = "⚡ READY TO INSTALL",
                                        color = ElectricEmerald,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            is UpdateStatus.Available -> {
                                Surface(
                                    color = GlowingAmber.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(0.8.dp, GlowingAmber)
                                ) {
                                    Text(
                                        text = "UPDATE AVAILABLE",
                                        color = GlowingAmber,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            is UpdateStatus.Downloading -> {
                                Surface(
                                    color = CyberCyan.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(0.8.dp, CyberCyan)
                                ) {
                                    Text(
                                        text = "DOWNLOADING...",
                                        color = CyberCyan,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            else -> {
                                Surface(
                                    color = ElectricEmerald.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "UP TO DATE",
                                        color = ElectricEmerald,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (updateStatus is UpdateStatus.Downloading) {
                        val dl = updateStatus as UpdateStatus.Downloading
                        Spacer(modifier = Modifier.height(10.dp))
                        FlowLoadingLine(
                            progress = dl.progressPercent / 100f,
                            modifier = Modifier.fillMaxWidth(),
                            color = CyberCyan,
                            trackColor = DarkSurfaceElevated,
                            height = 6.dp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Downloading v${dl.info.latestVersionName} in background...",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 10.sp)
                            )
                            Text(
                                text = "${dl.progressPercent}%",
                                style = MaterialTheme.typography.labelSmall.copy(color = CyberCyan, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Automatic Background Update Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkObsidian)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = null,
                        tint = if (isAutoUpdateEnabled) ElectricEmerald else TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Auto-Update When Connected to Internet",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 11.5.sp
                            )
                        )
                        Text(
                            text = "Triggers automatic download in background over Wi-Fi/Data",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextMuted,
                                fontSize = 9.5.sp
                            )
                        )
                    }
                }

                Switch(
                    checked = isAutoUpdateEnabled,
                    onCheckedChange = { AppUpdateManager.setAutoUpdateEnabled(context, it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = DarkObsidian,
                        checkedTrackColor = ElectricEmerald,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = DarkCardBorder
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            when (updateStatus) {
                is UpdateStatus.ReadyToInstall -> {
                    val ready = updateStatus as UpdateStatus.ReadyToInstall
                    Button(
                        onClick = {
                            val res = AppUpdateManager.installUpdate(context, ready.apkFile)
                            res.onFailure {
                                Toast.makeText(context, it.message ?: "Installer launch error", Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberCyan,
                            contentColor = DarkObsidian
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Bolt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "INSTALL UPDATE NOW (KEEP ALL DATA)",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                }

                is UpdateStatus.Available -> {
                    val avail = updateStatus as UpdateStatus.Available
                    Button(
                        onClick = {
                            AppUpdateManager.startBackgroundDownload(context, avail.info)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberCyan,
                            contentColor = DarkObsidian
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "DOWNLOAD v${avail.info.latestVersionName} IN BACKGROUND",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                }

                is UpdateStatus.Downloading -> {
                    OutlinedButton(
                        onClick = { showDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                        border = BorderStroke(1.dp, CyberCyan)
                    ) {
                        Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("VIEW DOWNLOAD PROGRESS", fontWeight = FontWeight.Bold)
                    }
                }

                else -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                AppUpdateManager.checkForUpdates(context)
                                showDialog = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberCyan,
                                contentColor = DarkObsidian
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "CHECK UPDATES",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black)
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                AppUpdateManager.createSelfTestUpdate(context)
                                showDialog = true
                            },
                            modifier = Modifier
                                .weight(1.1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                            border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.6f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "TEST INSTALLER",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Google Play Protect Notice
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = GlowingAmber.copy(alpha = 0.12f)),
                border = BorderStroke(1.dp, GlowingAmber.copy(alpha = 0.45f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
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
                        Text(
                            text = "Google Play Protect Notice",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Black,
                                color = GlowingAmber,
                                fontSize = 11.5.sp
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Because this app is installed directly and is not on Google Play Store, Google may show an alert: \"Unrecognized app\" or \"Blocked by Play Protect\". You should undermine / bypass this alert by tapping \"More details\" -> \"Install anyway\". 100% safe with zero data loss!",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextPrimary.copy(alpha = 0.9f),
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        )
                    )
                }
            }
        }
    }
}
