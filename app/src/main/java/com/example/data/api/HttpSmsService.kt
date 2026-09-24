package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class HttpSmsSendRequest(
    @Json(name = "content") val content: String,
    @Json(name = "from") val from: String,
    @Json(name = "to") val to: String
)

@JsonClass(generateAdapter = true)
data class HttpSmsResponseData(
    @Json(name = "id") val id: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "timestamp") val timestamp: String? = null
)

@JsonClass(generateAdapter = true)
data class HttpSmsApiResponse(
    @Json(name = "status") val status: String = "success",
    @Json(name = "message") val message: String? = null,
    @Json(name = "data") val data: HttpSmsResponseData? = null
)

data class SmsDispatchResult(
    val isSuccess: Boolean,
    val message: String,
    val reference: String,
    val recipientCount: Int,
    val pageCount: Int,
    val charCount: Int,
    val wordCount: Int,
    val totalCostNaira: Double,
    val gatewayStatus: String
)

object HttpSmsService {

    private const val TAG = "HttpSmsService"
    private const val BASE_URL = "https://api.httpsms.com/v1/messages/send"

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val requestAdapter = moshi.adapter(HttpSmsSendRequest::class.java)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /**
     * Calculates GSM character count, word count, and standard SMS billing page units.
     * Standard GSM 7-bit allows 160 chars for page 1, and 153 chars per page thereafter.
     */
    fun calculateSmsMetrics(content: String): Triple<Int, Int, Int> {
        val clean = content.trim()
        val charCount = clean.length
        val wordCount = if (clean.isBlank()) 0 else clean.split(Regex("\\s+")).filter { it.isNotBlank() }.size
        val pageCount = when {
            charCount == 0 -> 1
            charCount <= 160 -> 1
            else -> ((charCount + 152) / 153)
        }
        return Triple(charCount, wordCount, pageCount)
    }

    /**
     * Formats phone numbers to international standard e.g. 08031234567 -> +2348031234567
     */
    fun normalizePhoneNumber(phone: String): String {
        val digits = phone.replace(Regex("[^0-9+]"), "")
        return when {
            digits.startsWith("+") -> digits
            digits.startsWith("0") -> "+234" + digits.substring(1)
            digits.startsWith("234") -> "+$digits"
            else -> "+234$digits"
        }
    }

    /**
     * Dispatches SMS message using HttpSMS REST Gateway with encryption & TLS socket.
     */
    suspend fun dispatchSms(
        recipients: List<String>,
        content: String,
        senderId: String = "+2348137545370",
        customApiKey: String? = null,
        pricePerSmsPage: Double = 4.50
    ): SmsDispatchResult = withContext(Dispatchers.IO) {
        val (charCount, wordCount, pageCount) = calculateSmsMetrics(content)
        val validRecipients = recipients
            .map { it.trim() }
            .filter { it.isNotBlank() && it.replace(Regex("[^0-9]"), "").length >= 10 }
            .distinct()

        val recipientCount = if (validRecipients.isEmpty()) 1 else validRecipients.size
        val totalCost = recipientCount * pageCount * pricePerSmsPage
        val ref = "SMS-" + (100000..999999).random()

        val apiKey = listOfNotNull(
            customApiKey?.takeIf { it.isNotBlank() && !it.startsWith("default_", ignoreCase = true) },
            try { BuildConfig.HTTPSMS_API_KEY.takeIf { it.isNotBlank() && !it.startsWith("default_", ignoreCase = true) } } catch (e: Exception) { null },
            System.getenv("HTTPSMS_API_KEY")?.takeIf { it.isNotBlank() && !it.startsWith("default_", ignoreCase = true) }
        ).firstOrNull() ?: ""

        val isLiveApiKeyConfigured = apiKey.isNotBlank() && !apiKey.contains("dummy", ignoreCase = true) && !apiKey.contains("token", ignoreCase = true)

        var liveSuccessCount = 0
        var lastGatewayStatus = if (isLiveApiKeyConfigured) "PENDING" else "API_KEY_REQUIRED"
        var errorDetail: String? = null

        val normalizedFrom = if (senderId.isNotBlank() && (senderId.startsWith("+") || senderId.startsWith("0") || senderId.all { it.isDigit() })) {
            normalizePhoneNumber(senderId)
        } else if (senderId.isNotBlank()) {
            senderId
        } else {
            "+2348137545370"
        }

        if (!isLiveApiKeyConfigured) {
            // Attempt Cloud Run Backend SMS Proxy (keeps API key secure on server)
            var cloudSuccessCount = 0
            for (phone in validRecipients) {
                val ok = CloudRunApiClient.sendSms(phone, content)
                if (ok) cloudSuccessCount++
            }
            if (cloudSuccessCount > 0) {
                return@withContext SmsDispatchResult(
                    isSuccess = true,
                    message = "SMS successfully dispatched via Cloud Run SMS Gateway to $cloudSuccessCount recipient(s).",
                    reference = ref,
                    recipientCount = recipientCount,
                    pageCount = pageCount,
                    charCount = charCount,
                    wordCount = wordCount,
                    totalCostNaira = totalCost,
                    gatewayStatus = "DELIVERED"
                )
            }

            return@withContext SmsDispatchResult(
                isSuccess = false,
                message = "HttpSMS API Key is missing. To send real SMS to $recipientCount recipient(s), enter your HttpSMS API Key in the SMS Settings tab or configure server credentials.",
                reference = ref,
                recipientCount = recipientCount,
                pageCount = pageCount,
                charCount = charCount,
                wordCount = wordCount,
                totalCostNaira = totalCost,
                gatewayStatus = "API_KEY_REQUIRED"
            )
        }

        for (phone in validRecipients) {
            val normalizedTo = normalizePhoneNumber(phone)
            val jsonPayload = requestAdapter.toJson(
                HttpSmsSendRequest(
                    content = content,
                    from = normalizedFrom,
                    to = normalizedTo
                )
            )

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val request = Request.Builder()
                .url(BASE_URL)
                .addHeader("x-api-key", apiKey)
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json")
                .post(jsonPayload.toRequestBody(mediaType))
                .build()

            try {
                httpClient.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string().orEmpty()
                    if (response.isSuccessful) {
                        liveSuccessCount++
                        lastGatewayStatus = "DELIVERED"
                        Log.d(TAG, "HttpSMS dispatched successfully to $normalizedTo: $responseBody")
                    } else {
                        Log.w(TAG, "HttpSMS API error ${response.code}: $responseBody")
                        lastGatewayStatus = "FAILED (${response.code})"
                        errorDetail = when (response.code) {
                            401 -> "Invalid HttpSMS API Key. Please verify your API key in SMS Settings."
                            404 -> "Phone number $normalizedFrom is not registered on your HttpSMS account. Ensure the HttpSMS Android app is installed on the phone with SIM $normalizedFrom and logged in."
                            400 -> "Bad request: $responseBody"
                            422 -> "Unprocessable: $responseBody"
                            429 -> "HttpSMS rate limit exceeded. Please wait a moment."
                            else -> "HttpSMS error (${response.code}): $responseBody"
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "HttpSMS connection error to $normalizedTo: ${e.message}")
                lastGatewayStatus = "CONNECTION_ERROR"
                errorDetail = "Network connection to HttpSMS failed: ${e.localizedMessage}"
            }
        }

        val isOverallSuccess = liveSuccessCount > 0
        val summaryMessage = if (isOverallSuccess) {
            "SMS successfully dispatched to $liveSuccessCount recipient(s) via SIM $normalizedFrom."
        } else {
            errorDetail ?: "Failed to dispatch SMS via HttpSMS gateway. Check API key and phone connection."
        }

        SmsDispatchResult(
            isSuccess = isOverallSuccess,
            message = summaryMessage,
            reference = ref,
            recipientCount = recipientCount,
            pageCount = pageCount,
            charCount = charCount,
            wordCount = wordCount,
            totalCostNaira = totalCost,
            gatewayStatus = lastGatewayStatus
        )
    }

    /**
     * Webhook signature validation for HttpSMS events.
     */
    fun verifyWebhookSignature(payload: String, incomingSignature: String, webhookSecret: String): Boolean {
        if (incomingSignature.isBlank()) return false
        val computed = PairgateWebhookSecurity.computeHmacSha256(payload, webhookSecret)
        return java.security.MessageDigest.isEqual(
            computed.toByteArray(Charsets.UTF_8),
            incomingSignature.toByteArray(Charsets.UTF_8)
        )
    }
}

@JsonClass(generateAdapter = true)
data class HttpSmsWebhookPayloadData(
    @Json(name = "id") val id: String? = null,
    @Json(name = "owner_id") val ownerId: String? = null,
    @Json(name = "contact_id") val contactId: String? = null,
    @Json(name = "content") val content: String? = null,
    @Json(name = "from") val from: String? = null,
    @Json(name = "to") val to: String? = null,
    @Json(name = "type") val type: String? = "sent",
    @Json(name = "status") val status: String? = "DELIVERED",
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null,
    @Json(name = "failure_reason") val failureReason: String? = null
)

@JsonClass(generateAdapter = true)
data class HttpSmsWebhookEvent(
    @Json(name = "event") val event: String,
    @Json(name = "timestamp") val timestamp: String? = null,
    @Json(name = "data") val data: HttpSmsWebhookPayloadData? = null
)

data class HttpSmsDeliveryLog(
    val messageId: String,
    val event: String,
    val recipient: String,
    val senderId: String,
    val status: String,
    val failureReason: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val signatureVerified: Boolean = true
)

data class HttpSmsDeliveryStats(
    val totalDispatched: Int = 0,
    val totalDelivered: Int = 0,
    val totalFailed: Int = 0,
    val totalPending: Int = 0
) {
    val successRatePercent: Double
        get() = if (totalDispatched == 0) 100.0 else (totalDelivered.toDouble() / totalDispatched.toDouble()) * 100.0
}
