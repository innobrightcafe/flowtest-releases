package com.example.data.api

import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

data class EmailDispatchResult(
    val success: Boolean,
    val methodUsed: String,
    val message: String,
    val error: String? = null
)

object SmtpEmailService {
    private const val TAG = "SmtpEmailService"
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val JSON_TYPE = "application/json; charset=utf-8".toMediaType()

    private fun getSecret(key: String): String {
        return try {
            val field = BuildConfig::class.java.getField(key)
            (field.get(null) as? String) ?: ""
        } catch (_: Throwable) {
            System.getenv(key) ?: ""
        }
    }

    /**
     * Attempts to send the 6-digit access code via configured SMTP (Gmail App Password / Custom SMTP)
     * or Resend API.
     */
    suspend fun sendVerificationCode(
        recipientEmail: String,
        code: String,
        customSmtpUser: String? = null,
        customSmtpPass: String? = null
    ): EmailDispatchResult = withContext(Dispatchers.IO) {
        val email = recipientEmail.trim()
        if (email.isBlank() || !email.contains("@")) {
            return@withContext EmailDispatchResult(
                success = false,
                methodUsed = "None",
                message = "Invalid email format",
                error = "Please provide a valid recipient email address."
            )
        }

        // Helper to check for real non-placeholder value
        fun isRealSecret(s: String): Boolean = s.isNotBlank() && 
                !s.startsWith("placeholder", ignoreCase = true) && 
                !s.startsWith("xxxx") && 
                !s.startsWith("default_", ignoreCase = true)

        // 1. Resolve SMTP credentials (from custom params or BuildConfig/Secrets)
        val smtpUser = listOfNotNull(
            customSmtpUser,
            getSecret("EMAIL_USER"),
            getSecret("GMAIL_USER"),
            getSecret("SMTP_USER"),
            getSecret("SMTP_EMAIL_ADDRESS")
        ).firstOrNull { isRealSecret(it) }?.trim() ?: ""

        val rawSmtpPass = listOfNotNull(
            customSmtpPass,
            getSecret("EMAIL_PASS"),
            getSecret("GMAIL_APP_PASSWORD"),
            getSecret("SMTP_PASS"),
            getSecret("SMTP_EMAIL_APP_PASSWORD")
        ).firstOrNull { isRealSecret(it) }?.trim() ?: ""

        // Gmail app passwords often contain spaces like "abcd efgh ijkl mnop", remove all whitespace
        val smtpPass = rawSmtpPass.replace("\\s+".toRegex(), "")

        val smtpHost = listOf(
            getSecret("EMAIL_HOST"),
            getSecret("SMTP_HOST")
        ).firstOrNull { isRealSecret(it) }?.trim() ?: "smtp.gmail.com"

        val smtpPortStr = listOf(
            getSecret("EMAIL_PORT"),
            getSecret("SMTP_PORT")
        ).firstOrNull { isRealSecret(it) }?.trim() ?: "465"
        val smtpPort = smtpPortStr.toIntOrNull() ?: 465

        // 2. Resolve Resend API Key
        val resendKey = listOf(
            getSecret("RESEND_API_KEY")
        ).firstOrNull { isRealSecret(it) }?.trim() ?: ""

        val resendFrom = listOf(
            getSecret("RESEND_FROM_EMAIL")
        ).firstOrNull { isRealSecret(it) }?.trim() ?: "FlowTest Security <onboarding@flowtest2026.com>"

        val subject = "Your FlowTest Verification Code: $code"
        val htmlBody = """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8">
              <style>
                body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #09090b; color: #ffffff; padding: 24px; }
                .card { background-color: #18181b; border: 1px solid #00f2fe; border-radius: 16px; padding: 32px; max-width: 480px; margin: 0 auto; text-align: center; }
                .brand { font-size: 14px; font-weight: 900; letter-spacing: 2px; color: #00f2fe; margin-bottom: 8px; }
                .title { font-size: 22px; font-weight: bold; margin-bottom: 12px; color: #ffffff; }
                .subtitle { font-size: 14px; color: #a1a1aa; line-height: 1.5; margin-bottom: 24px; }
                .code-box { background: #072a38; border: 2px dashed #00f2fe; border-radius: 12px; padding: 18px 24px; font-size: 36px; font-weight: 900; letter-spacing: 8px; color: #00f2fe; font-family: monospace; display: inline-block; margin-bottom: 24px; }
                .footer { font-size: 12px; color: #71717a; margin-top: 24px; border-top: 1px solid #27272a; padding-top: 16px; }
              </style>
            </head>
            <body>
              <div class="card">
                <div class="brand">FLOWTEST</div>
                <div class="title">Your Access Verification Code</div>
                <div class="subtitle">Use the 6-digit verification code below to authenticate your VTU account:</div>
                <div class="code-box">$code</div>
                <div class="subtitle">This code is valid for 10 minutes. If you did not request this code, you can safely ignore this email.</div>
                <div class="footer">
                  © 2026 FlowTest • flowtest2026.com • 256-Bit Encrypted Native Auth
                </div>
              </div>
            </body>
            </html>
        """.trimIndent()

        // Attempt 1: Gmail SMTP / Custom SMTP over SSL
        if (smtpUser.isNotBlank() && smtpPass.isNotBlank()) {
            Log.d(TAG, "Attempting SMTP dispatch via $smtpHost:$smtpPort for user $smtpUser")
            val smtpRes = sendViaSmtp(
                host = smtpHost,
                port = smtpPort,
                username = smtpUser,
                password = smtpPass,
                recipient = email,
                subject = subject,
                htmlContent = htmlBody
            )
            if (smtpRes.success) {
                return@withContext smtpRes
            } else {
                Log.w(TAG, "SMTP dispatch failed: ${smtpRes.error}. Checking fallback methods.")
            }
        }

        // Attempt 2: Resend API
        if (resendKey.isNotBlank()) {
            Log.d(TAG, "Attempting Resend API dispatch")
            val resendRes = sendViaResend(
                apiKey = resendKey,
                from = resendFrom,
                to = email,
                subject = subject,
                htmlContent = htmlBody
            )
            if (resendRes.success) {
                return@withContext resendRes
            }
        }

        // Attempt 3: If backend local relay endpoint is available
        val relayRes = sendViaBackendRelay(email, code)
        if (relayRes.success) {
            return@withContext relayRes
        }

        // If no credentials configured or all failed, return helpful error
        val errorMsg = if (smtpUser.isBlank() || smtpPass.isBlank()) {
            "SMTP credentials not found in secrets (GMAIL_USER / GMAIL_APP_PASSWORD or RESEND_API_KEY). Please add them to the AI Studio Secrets panel."
        } else {
            "SMTP delivery failed. Please verify your Gmail App Password has no restrictions."
        }

        return@withContext EmailDispatchResult(
            success = false,
            methodUsed = "None",
            message = errorMsg,
            error = errorMsg
        )
    }

    /**
     * Sends a secure 6-digit password reset code and token via custom branded email (SMTP or Resend API),
     * completely replacing the default Firebase Google link.
     */
    suspend fun sendPasswordResetCode(
        recipientEmail: String,
        code: String,
        resetToken: String,
        customSmtpUser: String? = null,
        customSmtpPass: String? = null
    ): EmailDispatchResult = withContext(Dispatchers.IO) {
        val email = recipientEmail.trim()
        if (email.isBlank() || !email.contains("@")) {
            return@withContext EmailDispatchResult(
                success = false,
                methodUsed = "None",
                message = "Invalid email format",
                error = "Please provide a valid recipient email address."
            )
        }

        fun isRealSecret(s: String): Boolean = s.isNotBlank() &&
                !s.startsWith("placeholder", ignoreCase = true) &&
                !s.startsWith("xxxx") &&
                !s.startsWith("default_", ignoreCase = true)

        val smtpUser = listOfNotNull(
            customSmtpUser,
            getSecret("EMAIL_USER"),
            getSecret("GMAIL_USER"),
            getSecret("SMTP_USER"),
            getSecret("SMTP_EMAIL_ADDRESS")
        ).firstOrNull { isRealSecret(it) }?.trim() ?: ""

        val rawSmtpPass = listOfNotNull(
            customSmtpPass,
            getSecret("EMAIL_PASS"),
            getSecret("GMAIL_APP_PASSWORD"),
            getSecret("SMTP_PASS"),
            getSecret("SMTP_EMAIL_APP_PASSWORD")
        ).firstOrNull { isRealSecret(it) }?.trim() ?: ""

        val smtpPass = rawSmtpPass.replace("\\s+".toRegex(), "")

        val smtpHost = listOf(
            getSecret("EMAIL_HOST"),
            getSecret("SMTP_HOST")
        ).firstOrNull { isRealSecret(it) }?.trim() ?: "smtp.gmail.com"

        val smtpPortStr = listOf(
            getSecret("EMAIL_PORT"),
            getSecret("SMTP_PORT")
        ).firstOrNull { isRealSecret(it) }?.trim() ?: "465"
        val smtpPort = smtpPortStr.toIntOrNull() ?: 465

        val resendKey = listOf(
            getSecret("RESEND_API_KEY")
        ).firstOrNull { isRealSecret(it) }?.trim() ?: ""

        val resendFrom = listOf(
            getSecret("RESEND_FROM_EMAIL")
        ).firstOrNull { isRealSecret(it) }?.trim() ?: "FlowTest Security <security@flowtest2026.com>"

        val subject = "FlowTest Password Reset Code: $code"
        val htmlBody = """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8">
              <style>
                body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #09090b; color: #ffffff; padding: 24px; }
                .card { background-color: #18181b; border: 1px solid #00f2fe; border-radius: 16px; padding: 32px; max-width: 480px; margin: 0 auto; text-align: center; }
                .brand { font-size: 14px; font-weight: 900; letter-spacing: 2px; color: #00f2fe; margin-bottom: 8px; }
                .title { font-size: 22px; font-weight: bold; margin-bottom: 12px; color: #ffffff; }
                .subtitle { font-size: 14px; color: #a1a1aa; line-height: 1.5; margin-bottom: 24px; }
                .code-box { background: #072a38; border: 2px dashed #00f2fe; border-radius: 12px; padding: 18px 24px; font-size: 36px; font-weight: 900; letter-spacing: 8px; color: #00f2fe; font-family: monospace; display: inline-block; margin-bottom: 24px; }
                .token-box { font-size: 11px; font-family: monospace; color: #71717a; background: #121214; padding: 8px 12px; border-radius: 6px; word-break: break-all; margin-bottom: 20px; }
                .footer { font-size: 12px; color: #71717a; margin-top: 24px; border-top: 1px solid #27272a; padding-top: 16px; }
              </style>
            </head>
            <body>
              <div class="card">
                <div class="brand">FLOWTEST SECURITY</div>
                <div class="title">Reset Account Password</div>
                <div class="subtitle">You requested to reset your password. Use the 6-digit verification code below in the FlowTest app along with your new password:</div>
                <div class="code-box">$code</div>
                <div class="token-box">Security Reset Token: $resetToken</div>
                <div class="subtitle">This code expires in 15 minutes. If you did not make this request, please change your password immediately or contact support.</div>
                <div class="footer">
                  © 2026 FlowTest • flowtest2026.com • 256-Bit Encrypted Native Auth
                </div>
              </div>
            </body>
            </html>
        """.trimIndent()

        // 1. Gmail / Custom SMTP
        if (smtpUser.isNotBlank() && smtpPass.isNotBlank()) {
            Log.d(TAG, "Attempting Password Reset SMTP dispatch via $smtpHost:$smtpPort for user $smtpUser")
            val smtpRes = sendViaSmtp(
                host = smtpHost,
                port = smtpPort,
                username = smtpUser,
                password = smtpPass,
                recipient = email,
                subject = subject,
                htmlContent = htmlBody
            )
            if (smtpRes.success) return@withContext smtpRes
        }

        // 2. Resend API
        if (resendKey.isNotBlank()) {
            Log.d(TAG, "Attempting Password Reset Resend API dispatch")
            val resendRes = sendViaResend(
                apiKey = resendKey,
                from = resendFrom,
                to = email,
                subject = subject,
                htmlContent = htmlBody
            )
            if (resendRes.success) return@withContext resendRes
        }

        // 3. Backend relay fallback
        val relayRes = sendViaBackendRelay(email, code)
        if (relayRes.success) return@withContext relayRes

        val errorMsg = if (smtpUser.isBlank() || smtpPass.isBlank()) {
            "Custom SMTP credentials or Resend API key not configured. Please add GMAIL_USER/GMAIL_APP_PASSWORD or RESEND_API_KEY in Secrets."
        } else {
            "Could not dispatch custom reset email. Please check SMTP connectivity."
        }

        return@withContext EmailDispatchResult(
            success = false,
            methodUsed = "None",
            message = errorMsg,
            error = errorMsg
        )
    }
    private fun sendViaSmtp(
        host: String,
        port: Int,
        username: String,
        password: String,
        recipient: String,
        subject: String,
        htmlContent: String
    ): EmailDispatchResult {
        val client = SmtpConnection(host, port)
        return try {
            client.connect()
            client.authenticateAndSend(
                username = username,
                password = password,
                recipient = recipient,
                subject = subject,
                htmlContent = htmlContent
            )
        } catch (e: Exception) {
            Log.e(TAG, "SMTP socket communication error: ${e.message}", e)
            EmailDispatchResult(
                success = false,
                methodUsed = "SMTP ($host:$port)",
                message = "SMTP Connection error: ${e.localizedMessage}",
                error = e.message
            )
        } finally {
            client.close()
        }
    }

    private class SmtpConnection(private val host: String, private val port: Int) {
        private var socket: Socket? = null
        private var reader: BufferedReader? = null
        private var writer: PrintWriter? = null

        fun connect() {
            if (port == 465) {
                val sslFactory = SSLSocketFactory.getDefault() as SSLSocketFactory
                val sslSocket = sslFactory.createSocket(host, port) as SSLSocket
                sslSocket.soTimeout = 12000
                sslSocket.startHandshake()
                socket = sslSocket
            } else {
                val plainSocket = Socket(host, port)
                plainSocket.soTimeout = 12000
                socket = plainSocket
            }

            val curSocket = socket ?: throw IllegalStateException("Socket failed to initialize")
            reader = BufferedReader(InputStreamReader(curSocket.getInputStream(), Charsets.UTF_8))
            writer = PrintWriter(OutputStreamWriter(curSocket.getOutputStream(), Charsets.UTF_8), true)
        }

        private fun readReply(): Pair<Int, String> {
            val curReader = reader ?: return Pair(0, "No reader")
            var lastLine = ""
            var code = 0
            while (true) {
                val line = curReader.readLine() ?: break
                lastLine = line
                if (line.length >= 3 && line.substring(0, 3).all { it.isDigit() }) {
                    code = line.substring(0, 3).toInt()
                    if (line.length == 3 || line[3] == ' ') {
                        break
                    }
                }
            }
            return Pair(code, lastLine)
        }

        private fun sendCommand(cmd: String, expectedCode: Int? = null): Boolean {
            val curWriter = writer ?: return false
            curWriter.print("$cmd\r\n")
            curWriter.flush()
            val (respCode, respText) = readReply()
            if (expectedCode != null && respCode != expectedCode) {
                Log.e(TAG, "SMTP Command failed: code $respCode msg $respText (expected $expectedCode)")
                return false
            }
            return true
        }

        fun authenticateAndSend(
            username: String,
            password: String,
            recipient: String,
            subject: String,
            htmlContent: String
        ): EmailDispatchResult {
            // Initial 220 banner
            val (initCode, initMsg) = readReply()
            if (initCode != 220) {
                return EmailDispatchResult(false, "SMTP", "SMTP Greeting failed: $initCode $initMsg", initMsg)
            }

            // EHLO
            if (!sendCommand("EHLO localhost", 250)) {
                sendCommand("HELO localhost", 250)
            }

            // STARTTLS if on port 587
            if (port == 587) {
                if (sendCommand("STARTTLS", 220)) {
                    val sslFactory = SSLSocketFactory.getDefault() as SSLSocketFactory
                    val sslSocket = sslFactory.createSocket(socket, host, port, true) as SSLSocket
                    sslSocket.soTimeout = 12000
                    sslSocket.startHandshake()
                    socket = sslSocket
                    reader = BufferedReader(InputStreamReader(sslSocket.getInputStream(), Charsets.UTF_8))
                    writer = PrintWriter(OutputStreamWriter(sslSocket.getOutputStream(), Charsets.UTF_8), true)
                    sendCommand("EHLO localhost", 250)
                }
            }

            // AUTH LOGIN
            if (!sendCommand("AUTH LOGIN", 334)) {
                return EmailDispatchResult(false, "SMTP", "AUTH LOGIN rejected by SMTP server", "AUTH failed")
            }

            val b64User = Base64.encodeToString(username.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            if (!sendCommand(b64User, 334)) {
                return EmailDispatchResult(false, "SMTP", "Username rejected by SMTP server", "Invalid Username")
            }

            val b64Pass = Base64.encodeToString(password.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            val curWriter = writer ?: return EmailDispatchResult(false, "SMTP", "Writer closed", "Writer error")
            curWriter.print("$b64Pass\r\n")
            curWriter.flush()

            val (authCode, authMsg) = readReply()
            if (authCode != 235) {
                return EmailDispatchResult(
                    false,
                    "SMTP",
                    "Gmail App Password authentication failed ($authCode: $authMsg). Please check your App Password in Secrets.",
                    authMsg
                )
            }

            // MAIL FROM
            if (!sendCommand("MAIL FROM:<$username>", 250)) {
                return EmailDispatchResult(false, "SMTP", "MAIL FROM rejected", "MAIL FROM failed")
            }

            // RCPT TO
            if (!sendCommand("RCPT TO:<$recipient>", 250)) {
                return EmailDispatchResult(false, "SMTP", "RCPT TO rejected for $recipient", "Recipient rejected")
            }

            // DATA
            if (!sendCommand("DATA", 354)) {
                return EmailDispatchResult(false, "SMTP", "DATA command rejected", "DATA rejected")
            }

            // RFC 822 / MIME Email Payload
            val dateStr = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US).format(Date())
            val msgId = "<" + System.currentTimeMillis() + "." + java.util.UUID.randomUUID() + "@flowtest2026.com>"

            val dataBuilder = StringBuilder()
            dataBuilder.append("From: \"FlowTest\" <$username>\r\n")
            dataBuilder.append("To: <$recipient>\r\n")
            dataBuilder.append("Date: $dateStr\r\n")
            dataBuilder.append("Message-ID: $msgId\r\n")
            dataBuilder.append("Subject: $subject\r\n")
            dataBuilder.append("MIME-Version: 1.0\r\n")
            dataBuilder.append("Content-Type: text/html; charset=UTF-8\r\n")
            dataBuilder.append("Content-Transfer-Encoding: 8bit\r\n")
            dataBuilder.append("\r\n")
            dataBuilder.append(htmlContent)
            dataBuilder.append("\r\n.\r\n")

            curWriter.print(dataBuilder.toString())
            curWriter.flush()

            val (dataCode, dataMsg) = readReply()
            if (dataCode != 250) {
                return EmailDispatchResult(false, "SMTP", "Message submission failed: $dataCode $dataMsg", dataMsg)
            }

            // QUIT
            sendCommand("QUIT")

            return EmailDispatchResult(
                success = true,
                methodUsed = "SMTP ($host)",
                message = "Access code email sent successfully via SMTP!"
            )
        }

        fun close() {
            try {
                writer?.close()
                reader?.close()
                socket?.close()
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Resend API email dispatch.
     */
    private fun sendViaResend(
        apiKey: String,
        from: String,
        to: String,
        subject: String,
        htmlContent: String
    ): EmailDispatchResult {
        return try {
            val json = JSONObject().apply {
                put("from", from)
                put("to", JSONArray().put(to))
                put("subject", subject)
                put("html", htmlContent)
            }

            val request = Request.Builder()
                .url("https://api.resend.com/emails")
                .addHeader("Authorization", "Bearer $apiKey")
                .post(json.toString().toRequestBody(JSON_TYPE))
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                EmailDispatchResult(
                    success = true,
                    methodUsed = "Resend API",
                    message = "Access code sent successfully via Resend API!"
                )
            } else {
                EmailDispatchResult(
                    success = false,
                    methodUsed = "Resend API",
                    message = "Resend API returned HTTP ${response.code}: $responseBody",
                    error = responseBody
                )
            }
        } catch (e: Exception) {
            EmailDispatchResult(
                success = false,
                methodUsed = "Resend API",
                message = "Resend API error: ${e.localizedMessage}",
                error = e.message
            )
        }
    }

    /**
     * Optional backend relay dispatch if server endpoint is running.
     */
    private fun sendViaBackendRelay(to: String, code: String): EmailDispatchResult {
        return try {
            val json = JSONObject().apply {
                put("email", to)
                put("code", code)
            }
            val request = Request.Builder()
                .url("http://10.0.2.2:3001/api/auth/send-code")
                .post(json.toString().toRequestBody(JSON_TYPE))
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                EmailDispatchResult(
                    success = true,
                    methodUsed = "Relay Server",
                    message = "Access code sent via server relay"
                )
            } else {
                EmailDispatchResult(false, "Relay Server", "Relay unreachable", "HTTP ${response.code}")
            }
        } catch (e: Exception) {
            EmailDispatchResult(false, "Relay Server", "Relay offline", e.message)
        }
    }
}
