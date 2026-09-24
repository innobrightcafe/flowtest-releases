package com.example.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

object AppNotificationManager {

    private const val TAG = "AppNotificationManager"

    const val CHANNEL_TRANSACTIONS = "channel_vtu_transactions_v2"
    const val CHANNEL_SECURITY_VPN = "channel_vpn_security_v2"
    const val CHANNEL_SMS_ALERTS = "channel_sms_communications_v2"
    const val CHANNEL_DATA_SAVER = "channel_data_bandwidth_alerts_v2"
    const val CHANNEL_DIRECT_MESSAGES = "channel_direct_messages_chat_v2"
    const val CHANNEL_APP_UPDATES = "channel_app_updates_v2"
    const val CHANNEL_ADVERTS_OFFERS = "channel_adverts_offers_v2"

    private var channelsCreated = false

    fun initNotificationChannels(context: Context) {
        if (channelsCreated) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

                val defaultSoundUri = try {
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                } catch (_: Throwable) {
                    null
                }

                val audioAttributes = try {
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .build()
                } catch (_: Throwable) {
                    null
                }

                val directMsgChannel = NotificationChannel(
                    CHANNEL_DIRECT_MESSAGES,
                    "Direct Messages & Feed Chat",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Instant in-app peer-to-peer chat alerts, read receipts and community messages"
                    enableLights(true)
                    lightColor = Color.GREEN
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 200, 100, 200)
                    if (defaultSoundUri != null && audioAttributes != null) {
                        try { setSound(defaultSoundUri, audioAttributes) } catch (_: Throwable) {}
                    }
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    setShowBadge(true)
                }

                val txChannel = NotificationChannel(
                    CHANNEL_TRANSACTIONS,
                    "Wallet & VTU Transactions",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Real-time alerts for wallet deposits, airtime, data, and bill vending"
                    enableLights(true)
                    lightColor = Color.CYAN
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 250, 150, 250)
                    if (defaultSoundUri != null && audioAttributes != null) {
                        try { setSound(defaultSoundUri, audioAttributes) } catch (_: Throwable) {}
                    }
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    setShowBadge(true)
                }

                val vpnChannel = NotificationChannel(
                    CHANNEL_SECURITY_VPN,
                    "VPN & Zero-Trust Tunnel",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "VPN tunnel connection and protection status updates"
                    enableLights(true)
                    lightColor = Color.GREEN
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    setShowBadge(false)
                }

                val smsChannel = NotificationChannel(
                    CHANNEL_SMS_ALERTS,
                    "SMS Gateway & Messages",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "SMS delivery reports, bulk dispatch tracking and OTP codes"
                    enableLights(true)
                    lightColor = Color.YELLOW
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 200, 100, 200)
                    if (defaultSoundUri != null && audioAttributes != null) {
                        try { setSound(defaultSoundUri, audioAttributes) } catch (_: Throwable) {}
                    }
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    setShowBadge(true)
                }

                val dataChannel = NotificationChannel(
                    CHANNEL_DATA_SAVER,
                    "Data Management & Firewall",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Daily bandwidth consumption, firewall app freezing and quota alerts"
                    enableLights(true)
                    lightColor = Color.MAGENTA
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    setShowBadge(true)
                }

                val updateChannel = NotificationChannel(
                    CHANNEL_APP_UPDATES,
                    "App Updates & Upgrades",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Automatic background updates and system upgrade alerts"
                    enableLights(true)
                    lightColor = Color.CYAN
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 300, 150, 300)
                    if (defaultSoundUri != null && audioAttributes != null) {
                        try { setSound(defaultSoundUri, audioAttributes) } catch (_: Throwable) {}
                    }
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    setShowBadge(true)
                }

                val advertChannel = NotificationChannel(
                    CHANNEL_ADVERTS_OFFERS,
                    "Adverts, Offers & Rewards",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Promotions, bonus reward coins, cashback alerts and exclusive flash deals"
                    enableLights(true)
                    lightColor = Color.YELLOW
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 250, 150, 300)
                    if (defaultSoundUri != null && audioAttributes != null) {
                        try { setSound(defaultSoundUri, audioAttributes) } catch (_: Throwable) {}
                    }
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    setShowBadge(true)
                }

                manager.createNotificationChannels(
                    listOf(directMsgChannel, txChannel, vpnChannel, smsChannel, dataChannel, updateChannel, advertChannel)
                )
                Log.d(TAG, "Initialized all 7 notification channels successfully.")
            }
            channelsCreated = true
        } catch (e: Throwable) {
            Log.e(TAG, "Notification channel initialization error: ${e.message}")
            channelsCreated = true
        }
    }

    fun canPostNotification(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                Log.w(TAG, "POST_NOTIFICATIONS permission not granted.")
                return false
            }
        }
        val enabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        if (!enabled) {
            Log.w(TAG, "System notifications are disabled for this app.")
        }
        return enabled
    }

    fun getNotificationPermissionStatus(context: Context): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) "GRANTED" else "PERMISSION_REQUIRED"
        } else {
            if (NotificationManagerCompat.from(context).areNotificationsEnabled()) "GRANTED" else "DISABLED"
        }
    }

    fun openNotificationSettings(context: Context) {
        try {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                }
            } else {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch notification settings: ${e.message}", e)
        }
    }

    private fun getPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getActivity(context, 0, intent, flags)
    }

    @SuppressLint("MissingPermission")
    private fun notifySafely(context: Context, notificationId: Int, notification: Notification): Boolean {
        return try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
            Log.d(TAG, "System notification posted successfully (ID: $notificationId)")
            true
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: Cannot post notification without POST_NOTIFICATIONS permission", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to post notification: ${e.message}", e)
            false
        }
    }

    fun showTransactionNotification(
        context: Context,
        title: String,
        message: String,
        reference: String? = null,
        isSuccess: Boolean = true
    ): Boolean {
        initNotificationChannels(context)
        if (!canPostNotification(context)) return false

        val notificationId = (System.currentTimeMillis() % 100000).toInt()
        val pendingIntent = getPendingIntent(context)

        val finalBigText = when {
            reference.isNullOrBlank() -> message
            message.contains(reference, ignoreCase = true) || message.contains("Ref:", ignoreCase = true) || message.contains("Reference", ignoreCase = true) -> message
            else -> "$message\nRef ID: $reference"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_TRANSACTIONS)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(finalBigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setColor(if (isSuccess) 0xFF00E676.toInt() else 0xFFFF5252.toInt())
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        return notifySafely(context, notificationId, notification)
    }

    fun showElectricityTokenNotification(
        context: Context,
        title: String,
        message: String,
        reference: String,
        tokenPin: String?,
        discoName: String,
        meterNumber: String,
        amountNaira: Double,
        serviceAddress: String? = null,
        customerName: String? = null
    ): Boolean {
        initNotificationChannels(context)
        if (!canPostNotification(context)) return false

        val notificationId = (200000..999999).random()

        // Create an intent to launch MainActivity and automatically download/open PDF receipt with the token
        val downloadIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_ACTION_DOWNLOAD_RECEIPT_REF", reference)
            putExtra("EXTRA_ACTION_DOWNLOAD_RECEIPT_TOKEN", tokenPin ?: "")
            putExtra("EXTRA_ACTION_DOWNLOAD_RECEIPT_DISCO", discoName)
            putExtra("EXTRA_ACTION_DOWNLOAD_RECEIPT_METER", meterNumber)
            putExtra("EXTRA_ACTION_DOWNLOAD_RECEIPT_AMOUNT", amountNaira)
            putExtra("EXTRA_ACTION_DOWNLOAD_RECEIPT_ADDRESS", serviceAddress ?: "")
            putExtra("EXTRA_ACTION_DOWNLOAD_RECEIPT_CUSTOMER", customerName ?: "")
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(context, notificationId, downloadIntent, flags)

        val tokenLine = if (!tokenPin.isNullOrBlank()) "⚡ Token PIN: $tokenPin\n" else ""
        val addressLine = if (!serviceAddress.isNullOrBlank()) "Address: $serviceAddress\n" else ""
        val bigText = "$tokenLine" +
                "Meter: $meterNumber • $discoName\n" +
                "Amount: ₦${String.format(java.util.Locale.US, "%,.2f", amountNaira)}\n" +
                addressLine +
                "\n📥 Tap to download your official PDF receipt to get your token and utility verification."

        val notification = NotificationCompat.Builder(context, CHANNEL_TRANSACTIONS)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setContentTitle(title)
            .setContentText(if (!tokenPin.isNullOrBlank()) "Token: $tokenPin (Tap to download PDF)" else message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setColor(0xFF00E676.toInt())
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(
                R.drawable.ic_stat_notify,
                "📥 Download PDF (Get Token)",
                pendingIntent
            )
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        return notifySafely(context, notificationId, notification)
    }

    fun showVirtualAccountCreatedNotification(
        context: Context,
        bankName: String,
        accountNumber: String,
        accountName: String
    ): Boolean {
        initNotificationChannels(context)
        if (!canPostNotification(context)) return false

        val notificationId = 1001
        val pendingIntent = getPendingIntent(context)

        val notification = NotificationCompat.Builder(context, CHANNEL_TRANSACTIONS)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setContentTitle("🎉 Client Virtual Account Active!")
            .setContentText("$bankName • $accountNumber")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Your dedicated NUBAN deposit account is ready:\n• Bank: $bankName\n• Account No: $accountNumber\n• Account Name: $accountName\nTransfer funds anytime to fund your wallet instantly!"
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setColor(0xFF00E5FF.toInt())
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        return notifySafely(context, notificationId, notification)
    }

    fun showSmsDeliveredNotification(
        context: Context,
        recipient: String,
        pageCount: Int,
        unitsCharged: Double,
        status: String = "DELIVERED"
    ): Boolean {
        initNotificationChannels(context)
        if (!canPostNotification(context)) return false

        val notificationId = (System.currentTimeMillis() % 100000).toInt()
        val pendingIntent = getPendingIntent(context)

        val notification = NotificationCompat.Builder(context, CHANNEL_SMS_ALERTS)
            .setSmallIcon(R.drawable.ic_stat_chat)
            .setContentTitle("📨 SMS $status")
            .setContentText("Delivered to $recipient ($pageCount Page(s) • ₦$unitsCharged)")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "SMS Dispatch Completed:\n• Recipient: $recipient\n• Volume: $pageCount Page(s)\n• Charged: ₦${String.format("%.2f", unitsCharged)}\n• Status: $status (Encrypted via HttpSMS Gateway)"
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setColor(0xFF00E5FF.toInt())
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        return notifySafely(context, notificationId, notification)
    }

    fun showVpnStateNotification(
        context: Context,
        isConnected: Boolean,
        serverName: String,
        ipAddress: String
    ): Boolean {
        initNotificationChannels(context)
        if (!canPostNotification(context)) return false

        val notificationId = 2001
        val pendingIntent = getPendingIntent(context)

        val notification = NotificationCompat.Builder(context, CHANNEL_SECURITY_VPN)
            .setSmallIcon(R.drawable.ic_stat_shield)
            .setContentTitle(if (isConnected) "🛡️ Zero-Trust VPN Active" else "⚠️ VPN Disconnected")
            .setContentText(if (isConnected) "Connected to $serverName • IP: $ipAddress" else "Traffic is currently exposed to local ISP")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setColor(if (isConnected) 0xFF00E5FF.toInt() else 0xFFFFAB00.toInt())
            .setContentIntent(pendingIntent)
            .setOngoing(isConnected)
            .setAutoCancel(!isConnected)
            .build()

        return notifySafely(context, notificationId, notification)
    }

    fun showDataAlertNotification(
        context: Context,
        title: String,
        message: String
    ): Boolean {
        initNotificationChannels(context)
        if (!canPostNotification(context)) return false

        val notificationId = 3001
        val pendingIntent = getPendingIntent(context)

        val notification = NotificationCompat.Builder(context, CHANNEL_DATA_SAVER)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setColor(0xFF00E5FF.toInt())
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        return notifySafely(context, notificationId, notification)
    }

    fun showOtpNotification(
        context: Context,
        otpCode: String,
        channelName: String = "Email/SMS"
    ): Boolean {
        initNotificationChannels(context)
        if (!canPostNotification(context)) return false

        val notificationId = 4001
        val pendingIntent = getPendingIntent(context)

        val notification = NotificationCompat.Builder(context, CHANNEL_SMS_ALERTS)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setContentTitle("🔐 Security Verification Code")
            .setContentText("Your access code is $otpCode (Expires in 10 mins)")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Your 6-digit verification code is:\n\n$otpCode\n\nDispatched via $channelName. Never share this code with anyone."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setColor(0xFF00E5FF.toInt())
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        return notifySafely(context, notificationId, notification)
    }

    fun showDirectMessageNotification(
        context: Context,
        senderName: String,
        messageText: String,
        senderPhone: String
    ): Boolean {
        initNotificationChannels(context)
        if (!canPostNotification(context)) return false

        val notificationId = (senderPhone.hashCode() and 0x7FFFFFFF) % 100000 + 50000
        val pendingIntent = getPendingIntent(context)

        val notification = NotificationCompat.Builder(context, CHANNEL_DIRECT_MESSAGES)
            .setSmallIcon(R.drawable.ic_stat_chat)
            .setContentTitle("💬 $senderName")
            .setContentText(messageText)
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(messageText)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setColor(0xFF00E676.toInt())
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        return notifySafely(context, notificationId, notification)
    }

    fun showUpdateReadyNotification(
        context: Context,
        versionName: String,
        installPendingIntent: PendingIntent? = null
    ): Boolean {
        initNotificationChannels(context)
        if (!canPostNotification(context)) return false

        val notificationId = 88801
        val intentToUse = installPendingIntent ?: run {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("EXTRA_SHOW_UPDATE_DIALOG", true)
            }
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            PendingIntent.getActivity(context, notificationId, intent, flags)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_APP_UPDATES)
            .setSmallIcon(R.drawable.ic_stat_shield)
            .setContentTitle("⚡ FlowTest Update v$versionName Ready")
            .setContentText("Download completed. Tap to install update without losing any data.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "FlowTest v$versionName has been downloaded in the background.\n\nTap to install the upgrade immediately. All your account details, wallet balance, and settings will remain safe!"
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setColor(0xFF00E5FF.toInt())
            .setContentIntent(intentToUse)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        return notifySafely(context, notificationId, notification)
    }

    fun showUpdateAvailableNotification(
        context: Context,
        versionName: String,
        releaseNotes: String
    ): Boolean {
        initNotificationChannels(context)
        if (!canPostNotification(context)) return false

        val notificationId = 88802
        val updateIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_SHOW_UPDATE_DIALOG", true)
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(context, notificationId, updateIntent, flags)

        val notification = NotificationCompat.Builder(context, CHANNEL_APP_UPDATES)
            .setSmallIcon(R.drawable.ic_stat_shield)
            .setContentTitle("🚀 FlowTest v$versionName Available")
            .setContentText("A new performance and security update is available. Tap to upgrade.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "New FlowTest v$versionName is now available!\n\nRelease Notes:\n$releaseNotes\n\nTap to download and install."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setColor(0xFF00E5FF.toInt())
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        return notifySafely(context, notificationId, notification)
    }

    fun showAdvertOfferNotification(
        context: Context,
        title: String,
        body: String,
        actionUrl: String? = null
    ): Boolean {
        initNotificationChannels(context)
        if (!canPostNotification(context)) return false

        val notificationId = (77000..77999).random()
        val pendingIntent = getPendingIntent(context)

        val firstLine = body.lines().firstOrNull { it.isNotBlank() } ?: body

        val notification = NotificationCompat.Builder(context, CHANNEL_ADVERTS_OFFERS)
            .setSmallIcon(R.drawable.ic_stat_shield)
            .setContentTitle(title)
            .setContentText(firstLine)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(title)
                    .bigText(body)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_PROMO)
            .setColor(0xFFFFB300.toInt()) // Vibrant Amber Gold
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .addAction(R.drawable.ic_stat_shield, "Grab Offer", pendingIntent)
            .addAction(R.drawable.ic_stat_shield, "Open App", pendingIntent)
            .build()

        return notifySafely(context, notificationId, notification)
    }
}

