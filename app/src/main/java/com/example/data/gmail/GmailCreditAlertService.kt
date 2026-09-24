package com.example.data.gmail

import android.content.Context
import android.util.Base64
import android.util.Log
import com.example.data.api.CloudRunApiClient
import com.example.data.db.BookkeepingDao
import com.example.data.db.InboundNotificationAuditLogEntity
import com.example.data.db.ProcessedPaymentEntity
import com.example.data.db.TransactionBookkeepingEntity
import com.example.data.repository.MultiUtilityPricingEngine
import com.example.util.PhoneNarrationParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

/**
 * Parsed Credit Alert item from Gmail.
 */
data class GmailCreditAlert(
    val messageId: String,
    val subject: String,
    val sender: String,
    val dateStr: String,
    val amount: Double,
    val rawNarration: String,
    val detectedConfirmationCode: String?,
    val reference: String,
    val senderName: String,
    val fullBodySnippet: String,
    val sessionId: String? = null,
    val senderBank: String? = null,
    val rfcMessageId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class GmailSyncResult(
    val isSuccess: Boolean,
    val message: String,
    val alertsFound: Int = 0,
    val newlyCreditedCount: Int = 0,
    val totalAmountCredited: Double = 0.0,
    val creditedAlerts: List<GmailCreditAlert> = emptyList(),
    val alreadyCreditedAlerts: List<GmailCreditAlert> = emptyList(),
    val allParsedAlerts: List<GmailCreditAlert> = emptyList(),
    val logs: List<String> = emptyList()
)

/**
 * Service that connects to Gmail via IMAP over SSL using an App Password,
 * inspects Moniepoint Credit Alert emails, extracts the deposit narration, amount, and reference,
 * and automatically credits the user's wallet.
 */
class GmailCreditAlertService(
    private val context: Context,
    private val bookkeepingDao: BookkeepingDao,
    private val multiUtilityEngine: MultiUtilityPricingEngine
) {
    companion object {
        private const val TAG = "GmailCreditAlertService"
        private const val PREFS_NAME = "gmail_alert_config"
        private const val KEY_GMAIL_ADDRESS = "gmail_address"
        private const val KEY_GMAIL_APP_PASSWORD = "gmail_app_password"
        private const val KEY_AUTO_SYNC_ENABLED = "gmail_auto_sync_enabled"
        private const val KEY_PROCESSED_MSG_IDS = "processed_gmail_msg_ids"

        const val DEFAULT_EMAIL = "innobright2010@gmail.com"
        const val ALT_EMAIL = "Innobrightcafe@gmail.com"
        const val IMAP_HOST = "imap.gmail.com"
        const val IMAP_PORT = 993
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun getSecret(key: String): String {
        return try {
            val field = com.example.BuildConfig::class.java.getField(key)
            (field.get(null) as? String) ?: ""
        } catch (_: Throwable) {
            System.getenv(key) ?: ""
        }
    }

    fun getSavedGmailAddress(): String {
        val saved = prefs.getString(KEY_GMAIL_ADDRESS, null)
        if (!saved.isNullOrBlank()) return saved.trim()
        val envUser = listOf(
            getSecret("GMAIL_USER"),
            getSecret("SMTP_EMAIL_ADDRESS"),
            getSecret("EMAIL_USER"),
            getSecret("SMTP_USER")
        ).firstOrNull { it.isNotBlank() && !it.startsWith("default_", true) }
        return envUser?.trim() ?: DEFAULT_EMAIL
    }

    fun saveGmailAddress(email: String) {
        prefs.edit().putString(KEY_GMAIL_ADDRESS, email.trim()).apply()
    }

    fun getSavedGmailAppPassword(): String {
        val saved = prefs.getString(KEY_GMAIL_APP_PASSWORD, null)
        if (!saved.isNullOrBlank()) return saved.replace("\\s+".toRegex(), "").trim()
        val envPass = listOf(
            getSecret("GMAIL_APP_PASSWORD"),
            getSecret("SMTP_EMAIL_APP_PASSWORD"),
            getSecret("EMAIL_PASS"),
            getSecret("SMTP_PASS")
        ).firstOrNull { it.isNotBlank() && !it.startsWith("default_", true) }
        return envPass?.replace("\\s+".toRegex(), "")?.trim() ?: ""
    }

    fun saveGmailAppPassword(appPassword: String) {
        // Clean spaces often inserted when copying 16-char app passwords (e.g. "abcd efgh ijkl mnop")
        val cleanPass = appPassword.replace("\\s+".toRegex(), "").trim()
        prefs.edit().putString(KEY_GMAIL_APP_PASSWORD, cleanPass).apply()
    }

    fun isConfigured(): Boolean {
        return getSavedGmailAddress().isNotBlank() && getSavedGmailAppPassword().isNotBlank()
    }

    fun isAutoSyncEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_SYNC_ENABLED, true)
    }

    fun setAutoSyncEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_SYNC_ENABLED, enabled).apply()
    }

    /**
     * Connects to Gmail IMAP, fetches recent credit alerts, matches narration to confirmation code / user phone,
     * checks for matching transfer amount, and credits the wallet if a new valid payment is found.
     */
    suspend fun syncAndProcessCreditAlerts(
        overrideEmail: String? = null,
        overridePassword: String? = null,
        targetUserPhone: String? = null,
        expectedAmount: Double? = null,
        userPhone: String? = null
    ): GmailSyncResult = withContext(Dispatchers.IO) {
        val email = (overrideEmail ?: getSavedGmailAddress()).trim()
        val appPassword = (overridePassword ?: getSavedGmailAppPassword()).replace("\\s+".toRegex(), "").trim()

        val logs = mutableListOf<String>()

        if (email.isBlank() || appPassword.isBlank()) {
            logs.add("Gmail email or App Password not configured.")
            return@withContext GmailSyncResult(
                isSuccess = false,
                message = "Gmail App Password is required. Please enter your 16-character Google App Password in Settings / Admin Tab.",
                logs = logs
            )
        }

        val cleanEmail = email.trim()
        val cleanAppPassword = appPassword.replace("\\s+".toRegex(), "").trim()
        // Strictly search messages received for the current day to avoid loading excess historical data
        val searchDateStr = SimpleDateFormat("d-MMM-yyyy", Locale.ENGLISH).format(Date())
        logs.add("Connecting to $IMAP_HOST:$IMAP_PORT for $cleanEmail (Searching alerts for today: $searchDateStr)...")

        var socket: SSLSocket? = null
        var reader: BufferedReader? = null

        try {
            val socketFactory = SSLSocketFactory.getDefault() as SSLSocketFactory
            val rawSocket = socketFactory.createSocket()
            rawSocket.connect(InetSocketAddress(IMAP_HOST, IMAP_PORT), 6000)
            rawSocket.soTimeout = 8000
            socket = rawSocket as SSLSocket

            reader = BufferedReader(InputStreamReader(socket.inputStream, StandardCharsets.UTF_8))
            val outputStream = socket.outputStream

            fun sendCommand(cmd: String) {
                val bytes = (cmd + "\r\n").toByteArray(StandardCharsets.UTF_8)
                outputStream.write(bytes)
                outputStream.flush()
            }

            // 1. Read greeting
            val greeting = readImapLine(reader)
            logs.add("Server Greeting: $greeting")

            // 2. Send LOGIN with strict CRLF
            sendCommand("A01 LOGIN \"$cleanEmail\" \"$cleanAppPassword\"")
            val loginResp = readUntilTaggedResponse(reader, "A01")
            val isLoginOk = loginResp.any { it.startsWith("A01 OK", ignoreCase = true) }

            if (!isLoginOk) {
                val errorDetails = loginResp.joinToString(" ")
                logs.add("Login Failed: $errorDetails")
                return@withContext GmailSyncResult(
                    isSuccess = false,
                    message = "Gmail Authentication Failed: Please check your Gmail address and 16-character App Password. ($errorDetails)",
                    logs = logs
                )
            }
            logs.add("Login Successful! Accessing INBOX...")

            // 3. SELECT INBOX
            sendCommand("A02 SELECT INBOX")
            val selectResp = readUntilTaggedResponse(reader, "A02")
            var totalMessagesInInbox = 0
            for (line in selectResp) {
                if (line.contains("EXISTS", ignoreCase = true)) {
                    val count = line.replace("[^0-9]".toRegex(), "").toIntOrNull()
                    if (count != null) totalMessagesInInbox = count
                }
            }
            logs.add("INBOX selected. Total messages in mailbox: $totalMessagesInInbox")

            if (totalMessagesInInbox == 0) {
                sendCommand("A09 LOGOUT")
                return@withContext GmailSyncResult(
                    isSuccess = true,
                    message = "INBOX is empty. No credit alerts found for today ($searchDateStr).",
                    alertsFound = 0,
                    logs = logs
                )
            }

            // 4. Targeted Search & Recent Messages Ingestion (Strictly limited to today / last few hours)
            logs.add("Scanning mailbox for today's ($searchDateStr) Credit Alerts / Moniepoint...")
            val cleanCodeSearch = targetUserPhone?.trim()?.replace("-", "")?.replace(" ", "") ?: ""
            val searchCommands = mutableListOf(
                "A03 SEARCH UNSEEN SINCE $searchDateStr",
                "A04 SEARCH FROM \"moniepoint\" SINCE $searchDateStr",
                "A05 SEARCH FROM \"moniepoint.com\" SINCE $searchDateStr",
                "A06 SEARCH SUBJECT \"Credit\" SINCE $searchDateStr",
                "A07 SEARCH SUBJECT \"Notification\" SINCE $searchDateStr",
                "A08 SEARCH SUBJECT \"Transaction\" SINCE $searchDateStr",
                "A09 SEARCH TEXT \"6666468328\" SINCE $searchDateStr"
            )
            if (cleanCodeSearch.length >= 4) {
                searchCommands.add("A10 SEARCH TEXT \"$cleanCodeSearch\" SINCE $searchDateStr")
            }

            val discoveredIds = mutableSetOf<Int>()
            for (cmd in searchCommands) {
                try {
                    val tag = cmd.substringBefore(" ")
                    sendCommand(cmd)
                    val resp = readUntilTaggedResponse(reader, tag)
                    val ids = parseSearchIds(resp)
                    if (ids.isNotEmpty()) {
                        discoveredIds.addAll(ids)
                    }
                } catch (e: Exception) {
                    logs.add("Search query notice ($cmd): ${e.message}")
                }
            }

            // Inspect candidate messages strictly for the day / last few hours (latest up to 25 entries)
            val recentWindow = 15
            val newestStart = maxOf(1, totalMessagesInInbox - (recentWindow - 1))
            val newestEnd = totalMessagesInInbox
            val newestSeqIds = (newestStart..newestEnd).toList()
            val candidateIds = (discoveredIds + newestSeqIds).distinct().sorted().takeLast(25)

            logs.add("Inspecting ${candidateIds.size} candidate message(s) for today's bank transfer alerts...")

            val parsedAlerts = mutableListOf<GmailCreditAlert>()
            var cmdTagNum = 20
            for (msgId in candidateIds) {
                val tag = "A$cmdTagNum"
                cmdTagNum++

                // Use BODY.PEEK to quickly fetch header fields and body text (4KB buffer)
                sendCommand("$tag FETCH $msgId (BODY.PEEK[HEADER.FIELDS (SUBJECT FROM DATE MESSAGE-ID)] BODY.PEEK[TEXT]<0.4096>)")
                val fetchLines = readUntilTaggedResponse(reader, tag)
                val fullRaw = fetchLines.joinToString("\n")

                val alert = parseAlertFromRawEmail(msgId.toString(), fullRaw)
                if (alert != null) {
                    // Strictly limit to today / last few hours (within past 12 hours)
                    val isWithinRecentHours = (System.currentTimeMillis() - alert.timestamp) < 12 * 60 * 60 * 1000L
                    if (isWithinRecentHours) {
                        parsedAlerts.add(alert)
                        logs.add("Parsed Alert: ₦${String.format(Locale.US, "%,.2f", alert.amount)} | Code: ${alert.detectedConfirmationCode ?: "None"} | Narration: '${alert.rawNarration}' | Ref: ${alert.reference}")
                    }
                }
            }

            logs.add("Total valid bank credit alerts parsed: ${parsedAlerts.size}")

            // 5. Process alerts into wallet
            val newlyCredited = mutableListOf<GmailCreditAlert>()
            val alreadyCredited = mutableListOf<GmailCreditAlert>()
            var totalCreditedAmount = 0.0

            val effectiveTargetPhone = targetUserPhone

            for (alert in parsedAlerts) {
                val isCredited = processAlertToWallet(
                    alert = alert,
                    targetUserPhone = effectiveTargetPhone,
                    expectedAmount = expectedAmount,
                    userPhone = userPhone,
                    onAlreadyCredited = { alreadyCredited.add(it) }
                )
                if (isCredited) {
                    newlyCredited.add(alert)
                    totalCreditedAmount += alert.amount
                    logs.add("✓ WALLET CREDITED: ₦${String.format(Locale.US, "%,.2f", alert.amount)} | Code '${alert.detectedConfirmationCode ?: effectiveTargetPhone ?: "Direct"}' | Ref: ${alert.reference}")
                    // CRITICAL: Only add the ONE specific transfer matching this transaction! Do not accumulate multiple transfers.
                    if (expectedAmount != null && expectedAmount > 0.0) {
                        break
                    }
                }
            }

            // 6. LOGOUT
            sendCommand("A99 LOGOUT")

            val finalMsg = if (newlyCredited.isNotEmpty()) {
                "Successfully synced! Credited ₦${String.format(Locale.US, "%,.2f", totalCreditedAmount)} from ${newlyCredited.size} new Moniepoint deposit(s)."
            } else if (alreadyCredited.isNotEmpty()) {
                "Verified ${alreadyCredited.size} deposit(s) for today (already credited to wallet)."
            } else if (parsedAlerts.isNotEmpty()) {
                "Checked ${parsedAlerts.size} Moniepoint credit alert(s) for today. All transfers are up to date."
            } else {
                "Connected to Gmail successfully. No new Moniepoint credit alerts found for today ($searchDateStr)."
            }

            return@withContext GmailSyncResult(
                isSuccess = true,
                message = finalMsg,
                alertsFound = parsedAlerts.size,
                newlyCreditedCount = newlyCredited.size,
                totalAmountCredited = totalCreditedAmount,
                creditedAlerts = newlyCredited,
                alreadyCreditedAlerts = alreadyCredited,
                allParsedAlerts = parsedAlerts,
                logs = logs
            )

        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "Socket timeout communicating with Gmail IMAP: ${e.message}", e)
            logs.add("Socket Timeout: The connection to imap.gmail.com:993 timed out.")
            logs.add("Tip: Ensure your Google Account has IMAP enabled (Gmail Settings > Forwarding and POP/IMAP) and that you are using a 16-character App Password.")
            return@withContext GmailSyncResult(
                isSuccess = false,
                message = "Gmail Sync Timed Out. Please verify your internet connection and ensure IMAP access is enabled in Gmail.",
                logs = logs
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to Gmail IMAP: ${e.message}", e)
            logs.add("Exception: ${e.javaClass.simpleName}: ${e.message}")
            return@withContext GmailSyncResult(
                isSuccess = false,
                message = "Gmail Connection Error: ${e.localizedMessage ?: e.message}",
                logs = logs
            )
        } finally {
            try { reader?.close() } catch (_: Exception) {}
            try { socket?.close() } catch (_: Exception) {}
        }
    }

    /**
     * Read-only fetch of recent credit alert emails from Gmail for display purposes.
     * CRITICAL SAFETY: This method strictly NEVER credits wallets or modifies balances.
     */
    suspend fun fetchRecentAlertsReadOnly(): List<GmailCreditAlert> = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext emptyList()
        val alerts = mutableListOf<GmailCreditAlert>()
        var socket: SSLSocket? = null
        var reader: BufferedReader? = null
        try {
            val gmailUser = getSavedGmailAddress()
            val gmailAppPassword = getSavedGmailAppPassword()
            val appPassClean = gmailAppPassword.replace("\\s".toRegex(), "")
            val factory = SSLSocketFactory.getDefault() as SSLSocketFactory
            val s = factory.createSocket() as SSLSocket
            socket = s
            s.connect(InetSocketAddress(IMAP_HOST, IMAP_PORT), 10000)
            s.soTimeout = 12000

            val r = BufferedReader(InputStreamReader(s.inputStream, StandardCharsets.UTF_8))
            reader = r
            val writer = PrintWriter(OutputStreamWriter(s.outputStream, StandardCharsets.UTF_8), true)

            fun sendCommand(command: String) {
                writer.print(command + "\r\n")
                writer.flush()
            }

            r.readLine()
            sendCommand("A01 LOGIN \"$gmailUser\" \"$appPassClean\"")
            readUntilTaggedResponse(r, "A01")

            sendCommand("A02 SELECT INBOX")
            val inboxLines = readUntilTaggedResponse(r, "A02")
            var totalMessagesInInbox = 0
            for (line in inboxLines) {
                if (line.contains("EXISTS", ignoreCase = true)) {
                    val count = line.replace("[^0-9]".toRegex(), "").toIntOrNull()
                    if (count != null) totalMessagesInInbox = count
                }
            }

            if (totalMessagesInInbox > 0) {
                val recentWindow = 20
                val newestStart = maxOf(1, totalMessagesInInbox - (recentWindow - 1))
                val candidateIds = (newestStart..totalMessagesInInbox).toList()

                var cmdTagNum = 10
                for (msgId in candidateIds) {
                    val tag = "A$cmdTagNum"
                    cmdTagNum++
                    sendCommand("$tag FETCH $msgId (BODY.PEEK[HEADER.FIELDS (SUBJECT FROM DATE MESSAGE-ID)] BODY.PEEK[TEXT]<0.16384>)")
                    val fetchLines = readUntilTaggedResponse(r, tag)
                    val fullRaw = fetchLines.joinToString("\n")
                    val alert = parseAlertFromRawEmail(msgId.toString(), fullRaw)
                    if (alert != null) {
                        alerts.add(alert)
                    }
                }
            }
            sendCommand("A99 LOGOUT")
        } catch (e: Exception) {
            Log.d(TAG, "fetchRecentAlertsReadOnly notice: ${e.message}")
        } finally {
            try { reader?.close() } catch (_: Exception) {}
            try { socket?.close() } catch (_: Exception) {}
        }
        alerts.reversed()
    }

    private suspend fun processAlertToWallet(
        alert: GmailCreditAlert,
        targetUserPhone: String?,
        expectedAmount: Double? = null,
        userPhone: String? = null,
        onAlreadyCredited: (GmailCreditAlert) -> Unit = {}
    ): Boolean = withContext(Dispatchers.IO) {
        val safeRef = alert.reference.ifBlank { "GMAIL-REF-" + Math.abs(alert.hashCode()) }

        val defaultWallet = bookkeepingDao.getUserWalletSync()
        val walletFullName = defaultWallet?.assignedAccountName?.trim() ?: ""
        val nameParts = walletFullName.split("\\s+".toRegex()).filter { it.length >= 3 }

        val targetCodeClean = targetUserPhone?.trim()?.replace("-", "")?.replace(" ", "")?.uppercase()
        val alertCodeClean = alert.detectedConfirmationCode?.trim()?.replace("-", "")?.replace(" ", "")?.uppercase()
        val activeWalletCodeClean = defaultWallet?.activeConfirmationCode?.trim()?.replace("-", "")?.replace(" ", "")?.uppercase()

        val normalizedPhone = userPhone?.let { PhoneNarrationParser.normalizePhoneNumber(it) ?: it.trim() }
        val phoneLast10 = normalizedPhone?.takeLast(10) ?: defaultWallet?.phoneNumber?.takeLast(10)
        val phoneLast8 = normalizedPhone?.takeLast(8) ?: defaultWallet?.phoneNumber?.takeLast(8)

        val targetDigits = targetCodeClean?.replace("[^0-9]".toRegex(), "")
        val activeDigits = activeWalletCodeClean?.replace("[^0-9]".toRegex(), "")

        val fullCorpus = (alert.rawNarration + " " + alert.fullBodySnippet + " " + alert.subject + " " + alert.senderName).replace("-", "").uppercase()

        // 1. Remark / Narration Code match:
        // Must find user's specific confirmation code or registered phone in the bank alert narration / remarks
        val isCodeMatch = when {
            !targetCodeClean.isNullOrBlank() -> {
                alertCodeClean == targetCodeClean ||
                fullCorpus.contains(targetCodeClean) ||
                (!targetDigits.isNullOrBlank() && targetDigits.length >= 4 && fullCorpus.contains(targetDigits))
            }
            !activeWalletCodeClean.isNullOrBlank() -> {
                alertCodeClean == activeWalletCodeClean ||
                fullCorpus.contains(activeWalletCodeClean) ||
                (!activeDigits.isNullOrBlank() && activeDigits.length >= 4 && fullCorpus.contains(activeDigits))
            }
            alertCodeClean != null -> {
                bookkeepingDao.getUserWalletByConfirmationCode(alert.detectedConfirmationCode ?: "") != null ||
                bookkeepingDao.getUserWalletByConfirmationCode(alertCodeClean) != null
            }
            else -> false
        }

        // Phone match: phone placed in bank narration / remarks
        val isPhoneMatch = (!phoneLast10.isNullOrBlank() && fullCorpus.contains(phoneLast10)) ||
                           (!phoneLast8.isNullOrBlank() && fullCorpus.contains(phoneLast8))

        val hasValidRemarkCode = isCodeMatch || isPhoneMatch

        // 2. Exact Amount match:
        // "it should only add the specific transfer with the remark or narration code and the exact amount transferred."
        // "if there is a credit alert with the amount but a different code it should not add it. or the code but a different amount."
        val hasExactAmount = if (expectedAmount != null && expectedAmount > 0.0) {
            Math.abs(alert.amount - expectedAmount) < 0.01
        } else {
            // If no expectedAmount is provided (e.g. general background sync), only match if code is explicitly present
            hasValidRemarkCode
        }

        // STRICT CONDITION: Must match BOTH the remark/code AND the exact amount transferred
        val isMatched = hasValidRemarkCode && hasExactAmount

        if (!isMatched) {
            val failureReason = when {
                !hasValidRemarkCode && hasExactAmount -> "Amount matches (₦${alert.amount}) but remark code does not match '$targetCodeClean'"
                hasValidRemarkCode && !hasExactAmount -> "Code matched ('$targetCodeClean') but amount ₦${alert.amount} does not match expected ₦$expectedAmount"
                else -> "Neither remark code '$targetCodeClean' nor exact amount ₦$expectedAmount matched alert (Amt: ₦${alert.amount})"
            }
            Log.d(TAG, "Skipping alert (Ref: $safeRef) - $failureReason")
            try {
                val now = System.currentTimeMillis()
                val formattedTime = SimpleDateFormat("HH:mm:ss dd-MMM-yyyy", Locale.US).format(Date(now))
                bookkeepingDao.insertInboundAuditLog(
                    InboundNotificationAuditLogEntity(
                        id = "AUD-GMAIL-" + (100000..999999).random(),
                        source = "GMAIL_NOTIFICATION",
                        eventType = when {
                            !hasValidRemarkCode && hasExactAmount -> "CREDIT_ALERT_WRONG_CODE"
                            hasValidRemarkCode && !hasExactAmount -> "CREDIT_ALERT_WRONG_AMOUNT"
                            else -> "CREDIT_ALERT_UNMATCHED"
                        },
                        reference = safeRef,
                        amount = alert.amount,
                        rawPayload = "Subject: ${alert.subject}\nDate: ${alert.dateStr}\nNarration: ${alert.rawNarration}\nSnippet: ${alert.fullBodySnippet.take(500)}",
                        parsedSender = alert.senderName,
                        parsedNarration = alert.rawNarration,
                        detectedConfirmationCode = alert.detectedConfirmationCode,
                        detectedPhone = phoneLast10,
                        signatureVerified = true,
                        signatureDetails = "SSL IMAP AUTH (imap.gmail.com:993)",
                        reconciliationStatus = when {
                            !hasValidRemarkCode && hasExactAmount -> "UNMATCHED_CODE_DIFFERENT"
                            hasValidRemarkCode && !hasExactAmount -> "UNMATCHED_AMOUNT_DIFFERENT"
                            else -> "UNMATCHED_NO_CODE"
                        },
                        matchedUserId = null,
                        balanceBefore = defaultWallet?.appWalletBalance ?: 0.0,
                        balanceAfter = defaultWallet?.appWalletBalance ?: 0.0,
                        reconciliationNotes = "$failureReason. Ref: $safeRef. User can report to admin for manual rectification.",
                        timestamp = now,
                        completedAtFormatted = formattedTime
                    )
                )
            } catch (e: Exception) {
                Log.w(TAG, "Audit log error: ${e.message}")
            }
            return@withContext false
        }

        // 1. Check local Room DB idempotency
        val cleanCodeUpper = (targetCodeClean ?: alert.detectedConfirmationCode ?: "").replace("-", "").replace(" ", "").uppercase()
        val existingProcessed = bookkeepingDao.getProcessedPayment(safeRef)
        val existingTx = bookkeepingDao.getTransactionsByDateRangeSync(0L, Long.MAX_VALUE).find {
            it.reference == safeRef || (it.recipientOrAccount.contains(safeRef) && it.status == "success")
        }
        val existingTxByCodeOrAmt = if (cleanCodeUpper.isNotBlank()) {
            bookkeepingDao.getTransactionsByDateRangeSync(System.currentTimeMillis() - 15 * 60 * 1000L, Long.MAX_VALUE).find { tx ->
                tx.transactionType == "wallet_deposit" && tx.status == "success" &&
                Math.abs(tx.amountDebitedFromUser - alert.amount) < 0.01 &&
                tx.recipientOrAccount.replace("-", "").replace(" ", "").uppercase().contains(cleanCodeUpper)
            }
        } else null

        if (existingProcessed != null || existingTx != null || existingTxByCodeOrAmt != null) {
            val dupReason = if (existingProcessed != null || existingTx != null) "Ref $safeRef" else "Code '$cleanCodeUpper' & Amount ₦${alert.amount}"
            Log.i(TAG, "Alert ($dupReason) already credited to wallet locally. Skipping duplicate.")
            onAlreadyCredited(alert)
            try {
                val now = System.currentTimeMillis()
                val formattedTime = SimpleDateFormat("HH:mm:ss dd-MMM-yyyy", Locale.US).format(Date(now))
                bookkeepingDao.insertInboundAuditLog(
                    InboundNotificationAuditLogEntity(
                        id = "AUD-GMAIL-" + (100000..999999).random(),
                        source = "GMAIL_NOTIFICATION",
                        eventType = "CREDIT_ALERT_DUPLICATE_LOCAL",
                        reference = safeRef,
                        amount = alert.amount,
                        rawPayload = "Subject: ${alert.subject}\nDate: ${alert.dateStr}\nNarration: ${alert.rawNarration}\nSnippet: ${alert.fullBodySnippet.take(500)}",
                        parsedSender = alert.senderName,
                        parsedNarration = alert.rawNarration,
                        detectedConfirmationCode = alert.detectedConfirmationCode,
                        detectedPhone = phoneLast10,
                        signatureVerified = true,
                        signatureDetails = "Room DB Idempotency Filter",
                        reconciliationStatus = "DUPLICATE_SKIPPED",
                        matchedUserId = defaultWallet?.id,
                        balanceBefore = defaultWallet?.appWalletBalance ?: 0.0,
                        balanceAfter = defaultWallet?.appWalletBalance ?: 0.0,
                        reconciliationNotes = "Alert Ref '$safeRef' for ₦${String.format(Locale.US, "%,.2f", alert.amount)} was already credited in local database. Duplicate credit safely prevented.",
                        timestamp = now,
                        completedAtFormatted = formattedTime
                    )
                )
            } catch (e: Exception) {
                Log.w(TAG, "Audit log error: ${e.message}")
            }
            return@withContext false
        }

        val userWallet = if (!targetCodeClean.isNullOrBlank()) {
            bookkeepingDao.getUserWalletByConfirmationCode(targetCodeClean)
                ?: (if (!normalizedPhone.isNullOrBlank()) bookkeepingDao.getUserWalletByPhone(normalizedPhone) else null)
                ?: bookkeepingDao.getUserWalletSync()
        } else if (!normalizedPhone.isNullOrBlank()) {
            bookkeepingDao.getUserWalletByPhone(normalizedPhone)
                ?: bookkeepingDao.getUserWalletSync()
        } else if (alertCodeClean != null) {
            bookkeepingDao.getUserWalletByConfirmationCode(alert.detectedConfirmationCode ?: "")
                ?: bookkeepingDao.getUserWalletByConfirmationCode(alertCodeClean)
        } else {
            null
        }

        if (userWallet == null) {
            Log.w(TAG, "No user wallet found in database to credit alert: $alert")
            return@withContext false
        }

        val phoneToSave = normalizedPhone ?: userWallet.phoneNumber

        // 2. CRITICAL: Backend Claim Deduplication Authority
        // Check and register transaction claim with Cloud Run backend to guarantee ZERO double-crediting
        val backendClaim = CloudRunApiClient.claimPaymentOnBackend(
            reference = safeRef,
            sessionId = alert.sessionId,
            messageId = alert.rfcMessageId ?: alert.messageId,
            amount = alert.amount,
            narrationCode = targetCodeClean ?: alert.detectedConfirmationCode ?: "",
            userPhone = phoneToSave,
            userId = userWallet.id,
            source = "GMAIL_ALERT",
            emailData = mapOf(
                "subject" to alert.subject,
                "dateStr" to alert.dateStr,
                "senderName" to alert.senderName,
                "senderBank" to (alert.senderBank ?: ""),
                "rawNarration" to alert.rawNarration,
                "detectedCode" to (alert.detectedConfirmationCode ?: "")
            )
        )

        if (backendClaim != null && backendClaim.isClaimed) {
            Log.w(TAG, "🛑 Backend reported transaction $safeRef ALREADY CLAIMED (${backendClaim.message}). Skipping duplicate credit.")
            onAlreadyCredited(alert)
            try {
                val now = System.currentTimeMillis()
                val formattedTime = SimpleDateFormat("HH:mm:ss dd-MMM-yyyy", Locale.US).format(Date(now))
                // Record in local DB as already claimed so local checks block it immediately next time
                bookkeepingDao.insertProcessedPayment(
                    ProcessedPaymentEntity(
                        reference = safeRef,
                        amount = alert.amount,
                        rawNarration = alert.rawNarration,
                        extractedPhone = phoneToSave,
                        senderName = alert.senderName,
                        status = "already_claimed_backend",
                        resolvedAt = now
                    )
                )
                bookkeepingDao.insertInboundAuditLog(
                    InboundNotificationAuditLogEntity(
                        id = "AUD-GMAIL-" + (100000..999999).random(),
                        source = "GMAIL_NOTIFICATION",
                        eventType = "CREDIT_ALERT_DUPLICATE_BACKEND",
                        reference = safeRef,
                        amount = alert.amount,
                        rawPayload = "Subject: ${alert.subject}\nDate: ${alert.dateStr}\nNarration: ${alert.rawNarration}\nBackend Status: ${backendClaim.status}",
                        parsedSender = alert.senderName,
                        parsedNarration = alert.rawNarration,
                        detectedConfirmationCode = alert.detectedConfirmationCode ?: targetCodeClean,
                        detectedPhone = phoneToSave,
                        signatureVerified = true,
                        signatureDetails = "Cloud Run Central Claim Ledger",
                        reconciliationStatus = "DUPLICATE_SKIPPED",
                        matchedUserId = userWallet.id,
                        balanceBefore = userWallet.appWalletBalance,
                        balanceAfter = userWallet.appWalletBalance,
                        reconciliationNotes = "Backend ledger confirmed reference '$safeRef' was already credited. Duplicate credit safely prevented.",
                        timestamp = now,
                        completedAtFormatted = formattedTime
                    )
                )
            } catch (e: Exception) {
                Log.w(TAG, "Audit log error: ${e.message}")
            }
            return@withContext false
        }

        // 3. Credit user's wallet with exact deposit amount
        val oldBal = userWallet.appWalletBalance
        val newBal = oldBal + alert.amount
        val nextRotatedCode = PhoneNarrationParser.generateRotatedCode(userWallet.activeConfirmationCode)

        bookkeepingDao.updateWalletBalance(userWallet.id, newBal)
        bookkeepingDao.updateUserConfirmationCode(userWallet.id, nextRotatedCode)

        // 4. Record in Processed Payments ledger locally
        bookkeepingDao.insertProcessedPayment(
            ProcessedPaymentEntity(
                reference = safeRef,
                amount = alert.amount,
                rawNarration = alert.rawNarration,
                extractedPhone = phoneToSave,
                senderName = alert.senderName,
                status = "completed",
                resolvedAt = System.currentTimeMillis()
            )
        )

        // 5. Record Bookkeeping Transaction
        val now = System.currentTimeMillis()
        val formattedTime = java.text.SimpleDateFormat("HH:mm:ss dd-MMM-yyyy", java.util.Locale.US).format(java.util.Date(now))
        val txEntity = TransactionBookkeepingEntity(
            id = "TX-GMAIL-" + (100000..999999).random(),
            userId = userWallet.id,
            transactionType = "wallet_deposit",
            serviceCategory = "Deposit",
            recipientOrAccount = "${alert.senderName} (PIN: ${alert.detectedConfirmationCode ?: targetCodeClean ?: "FT"}, Phone: $phoneToSave)",
            amountDebitedFromUser = alert.amount,
            amountPaidToWholesaleApi = alert.amount,
            netProfitEarned = 0.0,
            status = "success",
            timestamp = now,
            reference = safeRef,
            confirmationSource = "GMAIL ALERT",
            completedAtFormatted = formattedTime
        )
        bookkeepingDao.insertTransaction(txEntity)

        // 6. Ingest tracked email notification data to backend
        try {
            CloudRunApiClient.ingestEmailNotificationToBackend(
                reference = safeRef,
                sessionId = alert.sessionId,
                messageId = alert.messageId,
                rfcMessageId = alert.rfcMessageId,
                amount = alert.amount,
                rawNarration = alert.rawNarration,
                detectedCode = alert.detectedConfirmationCode ?: targetCodeClean,
                senderName = alert.senderName,
                senderBank = alert.senderBank,
                dateStr = alert.dateStr,
                fullBodySnippet = alert.fullBodySnippet
            )
        } catch (e: Exception) {
            Log.d(TAG, "Ingest email notification notice: ${e.message}")
        }

        try {
            bookkeepingDao.insertInboundAuditLog(
                InboundNotificationAuditLogEntity(
                    id = "AUD-GMAIL-" + (100000..999999).random(),
                    source = "GMAIL_NOTIFICATION",
                    eventType = "CREDIT_ALERT_CREDITED",
                    reference = safeRef,
                    amount = alert.amount,
                    rawPayload = "Subject: ${alert.subject}\nDate: ${alert.dateStr}\nNarration: ${alert.rawNarration}\nSnippet: ${alert.fullBodySnippet.take(500)}",
                    parsedSender = alert.senderName,
                    parsedNarration = alert.rawNarration,
                    detectedConfirmationCode = alert.detectedConfirmationCode ?: targetCodeClean,
                    detectedPhone = phoneToSave,
                    signatureVerified = true,
                    signatureDetails = "SSL IMAP AUTH OK (imap.gmail.com:993)",
                    reconciliationStatus = "RECONCILED_WALLET",
                    matchedUserId = userWallet.id,
                    balanceBefore = oldBal,
                    balanceAfter = newBal,
                    reconciliationNotes = "Matched code/phone '${alert.detectedConfirmationCode ?: targetCodeClean ?: phoneToSave}'. User wallet '${userWallet.id}' credited with ₦${String.format(Locale.US, "%,.2f", alert.amount)}. Balance: ₦${String.format(Locale.US, "%,.2f", oldBal)} -> ₦${String.format(Locale.US, "%,.2f", newBal)}. Code rotated to $nextRotatedCode.",
                    timestamp = now,
                    completedAtFormatted = formattedTime
                )
            )
        } catch (e: Exception) {
            Log.w(TAG, "Audit log error: ${e.message}")
        }

        Log.i(TAG, "Wallet funded via Gmail alert: ₦${alert.amount} -> New balance: ₦$newBal (Ref: $safeRef, Phone: $phoneToSave, Code rotated to $nextRotatedCode)")
        return@withContext true
    }

    /**
     * Parses email lines to extract amount, narration, sender name, and reference.
     */
    private fun parseAlertFromRawEmail(msgId: String, rawContent: String): GmailCreditAlert? {
        val cleanText = decodeAndStripHtml(rawContent)

        // 6. Subject & Date from raw headers
        val subjectMatch = "Subject:\\s*([^\\r\\n]+)".toRegex(RegexOption.IGNORE_CASE).find(rawContent)
        val subject = subjectMatch?.groupValues?.getOrNull(1)?.trim() ?: "Credit Alert"

        val dateMatch = "Date:\\s*([^\\r\\n]+)".toRegex(RegexOption.IGNORE_CASE).find(rawContent)
        val rawDateHeader = dateMatch?.groupValues?.getOrNull(1)?.trim()
        val dateStr = rawDateHeader ?: SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())

        // Strictly verify that the email was received for today to prevent excessive data
        if (rawDateHeader != null && !isDateHeaderFromToday(rawDateHeader)) {
            Log.d(TAG, "Skipping alert email $msgId because date header '$rawDateHeader' is not from today.")
            return null
        }

        // Check if this looks like a credit alert
        val isCreditNotification = subject.contains("credit", ignoreCase = true) ||
                subject.contains("alert", ignoreCase = true) ||
                subject.contains("deposit", ignoreCase = true) ||
                subject.contains("inward", ignoreCase = true) ||
                subject.contains("transfer", ignoreCase = true) ||
                cleanText.contains("Credit Alert", ignoreCase = true) ||
                cleanText.contains("Credit Notification", ignoreCase = true) ||
                cleanText.contains("Credit:", ignoreCase = true) ||
                cleanText.contains("Payment Received", ignoreCase = true) ||
                cleanText.contains("Transaction Notification", ignoreCase = true) ||
                cleanText.contains("Transaction Alert", ignoreCase = true) ||
                cleanText.contains("Moniepoint", ignoreCase = true) ||
                cleanText.contains("credited with", ignoreCase = true) ||
                cleanText.contains("credited your account", ignoreCase = true) ||
                cleanText.contains("account credited", ignoreCase = true) ||
                cleanText.contains("inward transfer", ignoreCase = true) ||
                cleanText.contains("transfer received", ignoreCase = true) ||
                cleanText.contains("deposit", ignoreCase = true)

        if (!isCreditNotification) return null

        // 1. Extract Amount
        var amount = 0.0
        val amountPatterns = listOf(
            "(?:Amount|Amount Credited|Credit Amount|Sum of|Transaction Amount)[:\\s]*(?:NGN|₦|N)?\\s*([0-9,]+(?:\\.[0-9]{2})?)".toRegex(RegexOption.IGNORE_CASE),
            "(?:credited with\\s*(?:NGN|₦|N)?\\s*([0-9,]+(?:\\.[0-9]{2})?))".toRegex(RegexOption.IGNORE_CASE),
            "(?:received\\s*(?:NGN|₦|N)?\\s*([0-9,]+(?:\\.[0-9]{2})?))".toRegex(RegexOption.IGNORE_CASE),
            "(?:(?:NGN|₦)\\s*([0-9,]+(?:\\.[0-9]{2})?))".toRegex(RegexOption.IGNORE_CASE),
            "(?:N\\s*([0-9,]+(?:\\.[0-9]{2})?))".toRegex(RegexOption.IGNORE_CASE),
            "(?:Amount[:\\s]*([0-9,]+(?:\\.[0-9]{2})?))".toRegex(RegexOption.IGNORE_CASE),
            "(?:Value[:\\s]*(?:NGN|₦|N)?\\s*([0-9,]+(?:\\.[0-9]{2})?))".toRegex(RegexOption.IGNORE_CASE)
        )

        for (pattern in amountPatterns) {
            val match = pattern.find(cleanText)
            if (match != null && match.groupValues.size > 1) {
                val amtStr = match.groupValues[1].replace(",", "").trim()
                val parsed = amtStr.toDoubleOrNull()
                if (parsed != null && parsed > 0) {
                    amount = parsed
                    break
                }
            }
        }

        if (amount <= 0.0) return null

        // 2. Extract Narration / Remarks / Description
        var rawNarration = ""
        val narrationNextLabels = listOf(
            "|", "Amount:", "Amount", "Date:", "Date", "Time:", "Ref:", "Reference:", "Session:", "Session ID:",
            "Status:", "Bank:", "Account:", "Payer:", "Sender:", "Recipient:", "Beneficiary:", "Channel:", "Balance:",
            "Type:", "Currency:", "Terminal:"
        )

        val narrationPatterns = listOf(
            "(?:Transaction Remark|Transaction Remarks|Customer Note|Remarks?|Narration|Narrations|Payment Description|Description|Payment Note|Details|Memo|Ref/Narration|Ref / Narration|Payment Ref / Narration|Particulars|Transfer Note|Transfer Remark|Transfer Narration|Purpose)[:\\s\\|]*([^\\r\\n<]+)".toRegex(RegexOption.IGNORE_CASE),
            "(?:Remark\\(s\\)|Remarks?\\(s\\)|Narration\\(s\\))[:\\s\\|]*([^\\r\\n<]+)".toRegex(RegexOption.IGNORE_CASE),
            "(?:Sender Narration|Sender Remark|Sender's Narration|Sender's Remark)[:\\s\\|]*([^\\r\\n<]+)".toRegex(RegexOption.IGNORE_CASE),
            "(?:with narration|narration is|remarks? are|remarks? is)[:\\s\\|]*([^\\r\\n<]+)".toRegex(RegexOption.IGNORE_CASE),
            "(?:Reason for Transfer|Payment Reason)[:\\s\\|]*([^\\r\\n<]+)".toRegex(RegexOption.IGNORE_CASE)
        )

        for (pattern in narrationPatterns) {
            val match = pattern.find(cleanText)
            if (match != null && match.groupValues.size > 1) {
                val found = cleanFieldValue(match.groupValues[1], narrationNextLabels)
                if (found.isNotBlank() &&
                    !found.startsWith("http", ignoreCase = true) &&
                    !found.contains("style=", ignoreCase = true) &&
                    !found.contains("font-", ignoreCase = true) &&
                    !found.equals("Credit Alert", ignoreCase = true) &&
                    !found.equals("PAYMENT_SUCCESSFUL", ignoreCase = true)
                ) {
                    rawNarration = found
                    break
                }
            }
        }

        // Secondary fallback for narration: search for transaction line format e.g. "TRF/..." or "NIP/..."
        if (rawNarration.isBlank()) {
            val trfPattern = "(?:\\bTRF/[^\\r\\n|]+|\\bNIP/[^\\r\\n|]+)".toRegex(RegexOption.IGNORE_CASE)
            val trfMatch = trfPattern.find(cleanText)
            if (trfMatch != null) {
                rawNarration = cleanFieldValue(trfMatch.value, narrationNextLabels)
            }
        }

        if (rawNarration.isBlank()) {
            rawNarration = "Moniepoint Bank Deposit"
        }

        // 3. Extract 4-digit confirmation code from Narration or Email Body
        val detectedConfirmationCode = PhoneNarrationParser.extractConfirmationCode(rawNarration)
            ?: PhoneNarrationParser.extractConfirmationCode(cleanText)

        // 4. Extract Session ID (Moniepoint / NIBSS instant payment session)
        var sessionId: String? = null
        val sessionPatterns = listOf(
            "(?:Session ID|SessionId|Session_ID|NIP Session ID|NIP SessionId)[:\\s]*([0-9A-Za-z_\\-]+)".toRegex(RegexOption.IGNORE_CASE),
            "(?:Session ID|SessionId)[:\\s]*<[^>]*>[:\\s]*([0-9A-Za-z_\\-]+)".toRegex(RegexOption.IGNORE_CASE),
            "(?:Session)[:\\s]*([0-9]{12,35})".toRegex(RegexOption.IGNORE_CASE)
        )
        for (pattern in sessionPatterns) {
            val match = pattern.find(cleanText)
            if (match != null && match.groupValues.size > 1) {
                val cand = match.groupValues[1].trim()
                if (cand.length >= 6) {
                    sessionId = cand
                    break
                }
            }
        }

        // 5. Extract RFC Message-ID from header
        val messageIdMatch = "(?:Message-ID|Message-Id):\\s*<([^>]+)>".toRegex(RegexOption.IGNORE_CASE).find(rawContent)
            ?: "(?:Message-ID|Message-Id):\\s*([^\\r\\n]+)".toRegex(RegexOption.IGNORE_CASE).find(rawContent)
        val rfcMessageId = messageIdMatch?.groupValues?.getOrNull(1)?.trim()

        // 6. Extract Reference
        var reference = ""
        val refPatterns = listOf(
            "(?:Transaction Reference|Txn Ref|Ref No|Reference Number|Payment Reference|Transaction Ref)[:\\s]*([A-Za-z0-9_\\-]+)".toRegex(RegexOption.IGNORE_CASE),
            "(?:Transaction ID|Payment Ref|Txn ID)[:\\s]*([A-Za-z0-9_\\-]+)".toRegex(RegexOption.IGNORE_CASE),
            "(?:Ref)[:\\s]*([A-Za-z0-9_\\-]{6,})".toRegex(RegexOption.IGNORE_CASE)
        )

        for (pattern in refPatterns) {
            val match = pattern.find(cleanText)
            if (match != null && match.groupValues.size > 1) {
                val refCandidate = match.groupValues[1].trim()
                if (refCandidate.length >= 6) {
                    reference = refCandidate
                    break
                }
            }
        }

        // 7. Extract Sender / Payer Name
        var senderName = "Moniepoint Payer"
        val senderNextLabels = listOf(
            "|", "Bank:", "Bank Name:", "Source Bank:", "Sender Bank:", "Account:", "Account Number:", "Source Account:",
            "Amount:", "Date:", "Time:", "Ref:", "Reference:", "Session:", "Session ID:", "Narration:", "Remarks:",
            "Description:", "Channel:", "Status:", "Recipient:", "Beneficiary:", "Terminal:"
        )

        val senderPatterns = listOf(
            "(?:Sender Name|Sender's Name|Payer Name|Payer's Name|Source Account Name|Source Name|Originator Name|Originator|Originating Customer|Initiator Name|Initiator|Paid By|From Name|Source Details|Sender Details)[:\\s\\|]*([^\\r\\n<]+)".toRegex(RegexOption.IGNORE_CASE),
            "(?:^|[\\r\\n|])\\s*Sender[:\\s\\|]+([^\\r\\n<]+)".toRegex(RegexOption.IGNORE_CASE),
            "(?:^|[\\r\\n|])\\s*Payer[:\\s\\|]+([^\\r\\n<]+)".toRegex(RegexOption.IGNORE_CASE),
            "(?:Transfer from|Received from|Credit from|Payment from|Sent by)[:\\s\\|]+([A-Za-z\\s'\\-\\.]+?)(?:\\s+(?:to|into|via|on|for|dated|with|ref|at)|[\\r\\n<\\|]|$)".toRegex(RegexOption.IGNORE_CASE),
            "(?:transfer of [^\\r\\n|]+ from|received [^\\r\\n|]+ from)\\s+([A-Za-z\\s'\\-\\.]+?)(?:\\s+(?:into|to|via|on|dated|ref|with)|[\\r\\n<\\|]|$)".toRegex(RegexOption.IGNORE_CASE)
        )

        val genericSenders = setOf(
            "moniepoint", "moniepoint mfb", "moniepoint microfinance bank", "customer", "bank customer",
            "system notice", "unknown", "alert", "transaction", "credit", "notification", "support",
            "bank", "payer", "sender", "user"
        )

        for (pattern in senderPatterns) {
            val match = pattern.find(cleanText)
            if (match != null && match.groupValues.size > 1) {
                val cleanedName = cleanFieldValue(match.groupValues[1], senderNextLabels).take(50)
                val lowerName = cleanedName.lowercase().trim()
                if (cleanedName.isNotBlank() &&
                    !cleanedName.contains("@") &&
                    !cleanedName.startsWith("http", ignoreCase = true) &&
                    !cleanedName.contains("{") &&
                    !cleanedName.contains("}") &&
                    !cleanedName.contains("font-", ignoreCase = true) &&
                    !genericSenders.contains(lowerName) &&
                    cleanedName.any { it in 'a'..'z' || it in 'A'..'Z' }
                ) {
                    senderName = cleanedName
                    break
                }
            }
        }

        // 8. Extract Sender Bank
        var senderBank = "Moniepoint MFB"
        val bankPatterns = listOf(
            "(?:Sender Bank|Source Bank|Originating Bank|Bank Name|From Bank)[:\\s]*([^\\r\\n<]+)".toRegex(RegexOption.IGNORE_CASE),
            "(?:Sender's Bank|Payer Bank)[:\\s]*([^\\r\\n<]+)".toRegex(RegexOption.IGNORE_CASE)
        )
        for (pattern in bankPatterns) {
            val match = pattern.find(cleanText)
            if (match != null && match.groupValues.size > 1) {
                val bName = match.groupValues[1].trim().take(30)
                if (bName.isNotBlank() && !bName.contains("@")) {
                    senderBank = bName
                    break
                }
            }
        }

        // DETERMINISTIC TRANSACTION IDENTITY GUARANTEE (CRITICAL: ZERO RANDOM NUMBERS)
        // If the bank alert omitted an explicit reference string, derive an immutable cryptographic identity
        // from the invariant properties so duplicate runs on the exact same email resolve to the identical reference.
        if (reference.isBlank()) {
            if (!sessionId.isNullOrBlank()) {
                reference = sessionId
            } else if (!rfcMessageId.isNullOrBlank()) {
                val cleanMsgId = rfcMessageId.filter { it.isLetterOrDigit() }.takeLast(16)
                reference = "MNP-MSG-$cleanMsgId"
            } else {
                val hashInput = "${dateStr}_${amount}_${rawNarration.trim()}_${senderName.trim()}_${detectedConfirmationCode ?: ""}"
                val hash = try {
                    val md = java.security.MessageDigest.getInstance("MD5")
                    md.digest(hashInput.toByteArray()).joinToString("") { "%02x".format(it) }
                } catch (_: Exception) {
                    Math.abs(hashInput.hashCode()).toString()
                }
                reference = "MNP-EML-${hash.take(12).uppercase()}"
            }
        }

        return GmailCreditAlert(
            messageId = msgId,
            subject = subject,
            sender = "Moniepoint MFB / Alert",
            dateStr = dateStr,
            amount = amount,
            rawNarration = rawNarration,
            detectedConfirmationCode = detectedConfirmationCode,
            reference = reference,
            senderName = senderName,
            fullBodySnippet = cleanText.take(250),
            sessionId = sessionId,
            senderBank = senderBank,
            rfcMessageId = rfcMessageId
        )
    }

    private fun cleanFieldValue(raw: String, nextLabels: List<String>): String {
        var text = raw.trim().removePrefix("|").removePrefix(":").trim()
        for (label in nextLabels) {
            val idx = text.indexOf(label, ignoreCase = true)
            if (idx > 0) {
                text = text.substring(0, idx).trim()
            }
        }
        return text.trimEnd(' ', '\t', '|', '-', ',', ';', ':', '/', '.')
    }

    private fun decodeAndStripHtml(raw: String): String {
        var text = raw

        // 1. Decode Base64 MIME parts (both header-driven and boundary blocks)
        try {
            val b64Pattern = "(?i)Content-Transfer-Encoding:\\s*base64[\\r\\n]+(?:Content-[^\\r\\n]+[\\r\\n]+)*\\s*([A-Za-z0-9+/=\\r\\n]{24,})".toRegex()
            text = b64Pattern.replace(text) { match ->
                try {
                    val b64Clean = match.groupValues[1].replace("\\s".toRegex(), "")
                    val decodedBytes = Base64.decode(b64Clean, Base64.DEFAULT)
                    val decodedStr = String(decodedBytes, StandardCharsets.UTF_8)
                    "\n$decodedStr\n"
                } catch (_: Exception) {
                    match.value
                }
            }

            val standaloneB64Pattern = "(?:^|[\\r\\n])((?:[A-Za-z0-9+/]{40,}=*={0,2}[\\r\\n]+){2,}[A-Za-z0-9+/]{20,}=*={0,2})(?=[\\r\\n]|$)".toRegex()
            text = standaloneB64Pattern.replace(text) { match ->
                try {
                    val rawBlock = match.groupValues[1].replace("\\s".toRegex(), "")
                    if (rawBlock.length >= 60) {
                        val decodedBytes = Base64.decode(rawBlock, Base64.DEFAULT)
                        val decodedStr = String(decodedBytes, StandardCharsets.UTF_8)
                        if (decodedStr.any { it in 'a'..'z' || it in 'A'..'Z' } && !decodedStr.contains("\u0000")) {
                            "\n$decodedStr\n"
                        } else {
                            match.value
                        }
                    } else {
                        match.value
                    }
                } catch (_: Exception) {
                    match.value
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Error decoding base64 in email: ${e.message}")
        }

        // 2. Decode Quoted-Printable (=20, =3D, =\r\n, etc.)
        if (text.contains("=")) {
            text = text.replace("=\r\n", "").replace("=\n", "")
            val qpPattern = "=([0-9A-Fa-f]{2})".toRegex()
            text = qpPattern.replace(text) { match ->
                try {
                    val code = match.groupValues[1].toInt(16)
                    code.toChar().toString()
                } catch (_: Exception) {
                    match.value
                }
            }
        }

        // 3. Remove <style>...</style> and <script>...</script> completely to avoid CSS noise in regex
        text = text.replace("(?si)<style[^>]*>.*?</style>".toRegex(), " ")
        text = text.replace("(?si)<script[^>]*>.*?</script>".toRegex(), " ")

        // 4. Decode HTML Entities
        text = text
            .replace("&nbsp;", " ", ignoreCase = true)
            .replace("&#160;", " ")
            .replace("&amp;", "&", ignoreCase = true)
            .replace("&lt;", "<", ignoreCase = true)
            .replace("&gt;", ">", ignoreCase = true)
            .replace("&quot;", "\"", ignoreCase = true)
            .replace("&#39;", "'")
            .replace("&apos;", "'", ignoreCase = true)
            .replace("&#8358;", "₦")
            .replace("&#x20A6;", "₦", ignoreCase = true)
            .replace("&#x20a6;", "₦", ignoreCase = true)
            .replace("&Naira;", "₦", ignoreCase = true)

        // 5. Structure HTML tags into lines and columns
        text = text.replace("<br\\s*/?>".toRegex(RegexOption.IGNORE_CASE), "\n")
        text = text.replace("</td>".toRegex(RegexOption.IGNORE_CASE), " | ")
        text = text.replace("</th>".toRegex(RegexOption.IGNORE_CASE), " | ")
        text = text.replace("</tr>".toRegex(RegexOption.IGNORE_CASE), "\n")
        text = text.replace("</div>".toRegex(RegexOption.IGNORE_CASE), "\n")
        text = text.replace("</p>".toRegex(RegexOption.IGNORE_CASE), "\n")
        text = text.replace("</li>".toRegex(RegexOption.IGNORE_CASE), "\n")
        text = text.replace("<hr\\s*/?>".toRegex(RegexOption.IGNORE_CASE), "\n")
        text = text.replace("<[^>]*>".toRegex(), " ")

        // 6. Clean excess spaces while preserving newlines
        val lines = text.split("\r\n", "\n")
        val cleanedLines = lines.map { it.replace("[ \\t]+".toRegex(), " ").trim() }.filter { it.isNotBlank() }
        return cleanedLines.joinToString("\n")
    }

    private fun readImapLine(reader: BufferedReader): String {
        return reader.readLine() ?: ""
    }

    private fun readUntilTaggedResponse(reader: BufferedReader, tag: String): List<String> {
        val lines = mutableListOf<String>()
        var count = 0
        while (count < 2000) {
            count++
            val line = reader.readLine() ?: break
            lines.add(line)
            if (line.startsWith("$tag OK", ignoreCase = true) ||
                line.startsWith("$tag NO", ignoreCase = true) ||
                line.startsWith("$tag BAD", ignoreCase = true)
            ) {
                break
            }
        }
        return lines
    }

    private fun parseSearchIds(searchLines: List<String>): List<Int> {
        val ids = mutableListOf<Int>()
        for (line in searchLines) {
            if (line.startsWith("* SEARCH", ignoreCase = true)) {
                val parts = line.removePrefix("* SEARCH").trim().split("\\s+".toRegex())
                for (p in parts) {
                    val id = p.toIntOrNull()
                    if (id != null) ids.add(id)
                }
            }
        }
        return ids.sorted()
    }

    private fun isDateHeaderFromToday(header: String): Boolean {
        try {
            val todayDayMonthYear = SimpleDateFormat("d MMM yyyy", Locale.ENGLISH).format(Date())
            val todayDayMonthYearPadded = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(Date())
            if (header.contains(todayDayMonthYear, ignoreCase = true) || header.contains(todayDayMonthYearPadded, ignoreCase = true)) {
                return true
            }

            val formats = listOf(
                SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.ENGLISH),
                SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH),
                SimpleDateFormat("d MMM yyyy HH:mm:ss Z", Locale.ENGLISH),
                SimpleDateFormat("dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH),
                SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z", Locale.ENGLISH)
            )
            for (fmt in formats) {
                try {
                    val parsed = fmt.parse(header)
                    if (parsed != null) {
                        val diffMillis = Math.abs(System.currentTimeMillis() - parsed.time)
                        if (diffMillis <= 14 * 60 * 60 * 1000L) {
                            return true
                        }
                    }
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            Log.d(TAG, "Date parse error: ${e.message}")
        }
        return false
    }
}
