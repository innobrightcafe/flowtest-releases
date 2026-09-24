package com.example.ui.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.MainActivity
import com.example.ui.theme.*
import com.example.util.CrashLogger
import java.io.File

/**
 * Dedicated, resilient Activity launched whenever an unhandled exception crashes FlowTest.
 * Displays user-friendly diagnostics, renders the crash log, and provides 1-tap options
 * to share the log file via Email or copy it to clipboard.
 */
class CrashReportActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val rawReport = intent.getStringExtra(EXTRA_CRASH_REPORT)
            ?: CrashLogger.getLatestCrashLog(this)
            ?: "No crash report recorded."
        val crashFilePath = intent.getStringExtra(EXTRA_CRASH_FILE)
        val crashFile = crashFilePath?.let { File(it) } ?: CrashLogger.getLatestCrashLogFile(this)

        setContent {
            FlowTestTheme(darkTheme = true) {
                CrashReportScreen(
                    reportText = rawReport,
                    crashFile = crashFile,
                    onShareEmail = {
                        CrashLogger.shareCrashLogViaEmail(this, rawReport, crashFile)
                    },
                    onCopyToClipboard = {
                        CrashLogger.copyToClipboard(this, rawReport)
                    },
                    onRestartApp = {
                        val launchIntent = Intent(this, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        }
                        startActivity(launchIntent)
                        finish()
                    },
                    onClose = {
                        finishAffinity()
                    }
                )
            }
        }
    }

    companion object {
        const val EXTRA_CRASH_REPORT = "extra_crash_report"
        const val EXTRA_CRASH_FILE = "extra_crash_file"
        const val EXTRA_IS_MAIN_THREAD = "extra_is_main_thread"
    }
}

@Composable
fun CrashReportScreen(
    reportText: String,
    crashFile: File?,
    onShareEmail: () -> Unit,
    onCopyToClipboard: () -> Unit,
    onRestartApp: () -> Unit,
    onClose: () -> Unit
) {
    val verticalScroll = rememberScrollState()
    val logScrollState = rememberScrollState()
    val logHScrollState = rememberScrollState()

    Scaffold(
        containerColor = DarkObsidian,
        contentColor = TextPrimary,
        bottomBar = {
            Surface(
                color = DarkSurfaceElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Button(
                        onClick = onShareEmail,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricEmerald,
                            contentColor = DarkObsidian
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Email Report",
                            tint = DarkObsidian,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "SHARE LOG VIA EMAIL",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.8.sp,
                                color = DarkObsidian
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onRestartApp,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Restart", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("RESTART", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = onClose,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMuted),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Text("CLOSE", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .statusBarsPadding()
                .verticalScroll(verticalScroll)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(WarningRed.copy(alpha = 0.15f))
                        .border(1.dp, WarningRed.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.BugReport,
                        contentDescription = "Crash Icon",
                        tint = WarningRed,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "FlowTest Stopped",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "Crash Log Saved to Device",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = WarningRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Explanation Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "An unexpected exception interrupted the application. All crash details, device state, and stack traces were automatically captured and saved to a local text file:",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DarkObsidian,
                        border = androidx.compose.foundation.BorderStroke(0.6.dp, DarkCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = crashFile?.absolutePath ?: "Internal: /crash_logs/latest_crash.txt",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = CyberCyan,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.5.sp
                            ),
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Tap 'Share Log via Email' below to email this diagnostic file directly to the developer for a fast fix.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = ElectricEmerald,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Diagnostic Meta Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CRASH LOG PREVIEW",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Black,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                )

                TextButton(
                    onClick = onCopyToClipboard,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = CyberCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Copy Log",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = CyberCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Scrollable Log Window
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = DarkObsidian,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    Text(
                        text = reportText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.5.sp,
                            color = Color(0xFFE2E8F0),
                            lineHeight = 15.sp
                        ),
                        modifier = Modifier
                            .verticalScroll(logScrollState)
                            .horizontalScroll(logHScrollState)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Technical summary chip
            Text(
                text = "Target Support Email: ${CrashLogger.SUPPORT_EMAIL} • FlowTest v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            )

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
