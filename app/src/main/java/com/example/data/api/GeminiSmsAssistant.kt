package com.example.data.api

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
import java.util.concurrent.TimeUnit

data class CountryCodeInfo(
    val countryCode: String,
    val countryName: String,
    val flagEmoji: String,
    val dialPrefix: String,
    val exampleFormat: String
)

data class CountryCodeSuggestion(
    val country: CountryCodeInfo,
    val normalizedNumber: String,
    val formattedDisplay: String
)

object GeminiSmsAssistant {

    private const val TAG = "GeminiSmsAssistant"
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val GEMINI_ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    val SUPPORTED_COUNTRIES = listOf(
        CountryCodeInfo("NG", "Nigeria", "🇳🇬", "+234", "0813 754 5370"),
        CountryCodeInfo("US", "United States", "🇺🇸", "+1", "(555) 234-5678"),
        CountryCodeInfo("GB", "United Kingdom", "🇬🇧", "+44", "07911 123456"),
        CountryCodeInfo("GH", "Ghana", "🇬🇭", "+233", "024 123 4567"),
        CountryCodeInfo("KE", "Kenya", "🇰🇪", "+254", "0712 345678"),
        CountryCodeInfo("ZA", "South Africa", "🇿🇦", "+27", "082 123 4567"),
        CountryCodeInfo("AE", "United Arab Emirates", "🇦🇪", "+971", "050 123 4567"),
        CountryCodeInfo("IN", "India", "🇮🇳", "+91", "98765 43210"),
        CountryCodeInfo("DE", "Germany", "🇩🇪", "+49", "0151 12345678")
    )

    /**
     * Auto-detects and suggests international country code from local user phone input
     */
    fun detectCountryCode(input: String): CountryCodeSuggestion? {
        val clean = input.trim()
        if (clean.length < 3) return null

        val digitsOnly = clean.replace(Regex("[^0-9]"), "")

        // 1. Direct international prefix check
        if (clean.startsWith("+234") || digitsOnly.startsWith("234")) {
            val localPart = digitsOnly.removePrefix("234")
            return CountryCodeSuggestion(
                country = SUPPORTED_COUNTRIES[0], // Nigeria
                normalizedNumber = "+234$localPart",
                formattedDisplay = "+234 $localPart"
            )
        }

        if (clean.startsWith("+1") || (clean.startsWith("1") && digitsOnly.length == 11)) {
            val localPart = digitsOnly.removePrefix("1")
            return CountryCodeSuggestion(
                country = SUPPORTED_COUNTRIES[1], // USA
                normalizedNumber = "+1$localPart",
                formattedDisplay = "+1 $localPart"
            )
        }

        if (clean.startsWith("+44") || digitsOnly.startsWith("44")) {
            val localPart = digitsOnly.removePrefix("44")
            return CountryCodeSuggestion(
                country = SUPPORTED_COUNTRIES[2], // UK
                normalizedNumber = "+44$localPart",
                formattedDisplay = "+44 $localPart"
            )
        }

        if (clean.startsWith("+233") || digitsOnly.startsWith("233")) {
            val localPart = digitsOnly.removePrefix("233")
            return CountryCodeSuggestion(
                country = SUPPORTED_COUNTRIES[3], // Ghana
                normalizedNumber = "+233$localPart",
                formattedDisplay = "+233 $localPart"
            )
        }

        if (clean.startsWith("+254") || digitsOnly.startsWith("254")) {
            val localPart = digitsOnly.removePrefix("254")
            return CountryCodeSuggestion(
                country = SUPPORTED_COUNTRIES[4], // Kenya
                normalizedNumber = "+254$localPart",
                formattedDisplay = "+254 $localPart"
            )
        }

        if (clean.startsWith("+27") || digitsOnly.startsWith("27")) {
            val localPart = digitsOnly.removePrefix("27")
            return CountryCodeSuggestion(
                country = SUPPORTED_COUNTRIES[5], // South Africa
                normalizedNumber = "+27$localPart",
                formattedDisplay = "+27 $localPart"
            )
        }

        // 2. Local number heuristics
        if (digitsOnly.startsWith("080") || digitsOnly.startsWith("081") || digitsOnly.startsWith("070") ||
            digitsOnly.startsWith("090") || digitsOnly.startsWith("091") || digitsOnly.startsWith("80") ||
            digitsOnly.startsWith("81") || digitsOnly.startsWith("70") || digitsOnly.startsWith("90") || digitsOnly.startsWith("91")
        ) {
            val trimmedLeadingZero = if (digitsOnly.startsWith("0")) digitsOnly.substring(1) else digitsOnly
            return CountryCodeSuggestion(
                country = SUPPORTED_COUNTRIES[0], // Nigeria
                normalizedNumber = "+234$trimmedLeadingZero",
                formattedDisplay = "+234 $trimmedLeadingZero (Nigeria 🇳🇬)"
            )
        }

        if (digitsOnly.startsWith("024") || digitsOnly.startsWith("054") || digitsOnly.startsWith("055") || digitsOnly.startsWith("020")) {
            val trimmed = if (digitsOnly.startsWith("0")) digitsOnly.substring(1) else digitsOnly
            return CountryCodeSuggestion(
                country = SUPPORTED_COUNTRIES[3], // Ghana
                normalizedNumber = "+233$trimmed",
                formattedDisplay = "+233 $trimmed (Ghana 🇬🇭)"
            )
        }

        if (digitsOnly.startsWith("07") && digitsOnly.length == 10) {
            val trimmed = digitsOnly.substring(1)
            return CountryCodeSuggestion(
                country = SUPPORTED_COUNTRIES[4], // Kenya
                normalizedNumber = "+254$trimmed",
                formattedDisplay = "+254 $trimmed (Kenya 🇰🇪)"
            )
        }

        return null
    }

    /**
     * AI-Powered SMS message composition and refinement with character & cost optimization
     */
    suspend fun composeSmsWithAi(
        rawTopicOrDraft: String,
        tone: String = "Formal Business",
        recipientName: String = "Valued Customer",
        senderName: String = "FlowTest"
    ): String = withContext(Dispatchers.IO) {
        val cleanInput = rawTopicOrDraft.trim()
        if (cleanInput.isBlank()) {
            return@withContext "Dear $recipientName, thank you for connecting with $senderName. Please let us know if you need any assistance."
        }

        val geminiApiKey: String? = try {
            val key = BuildConfig.GEMINI_API_KEY
            if (key.isNotBlank() && !key.startsWith("default_", ignoreCase = true)) key else null
        } catch (e: Throwable) { null }

        if (!geminiApiKey.isNullOrBlank()) {
            try {
                val systemPrompt = """
                    You are a professional SMS copywriter. Write a concise, high-converting SMS message based on the user's prompt.
                    Rules:
                    1. Keep total message strictly under 150 characters (1 SMS segment).
                    2. Tone: $tone.
                    3. Recipient: $recipientName.
                    4. Sender Signature: $senderName.
                    5. Output ONLY the plain SMS text with no markdown, quotes, explanations or extra preamble.
                """.trimIndent()

                val payload = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("parts", JSONArray().apply {
                                put(JSONObject().put("text", "$systemPrompt\n\nPrompt: $cleanInput"))
                            })
                        })
                    })
                    put("generationConfig", JSONObject().apply {
                        put("temperature", 0.7)
                        put("maxOutputTokens", 120)
                    })
                }

                val request = Request.Builder()
                    .url("$GEMINI_ENDPOINT?key=$geminiApiKey")
                    .post(payload.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val respBody = response.body?.string()
                    if (!respBody.isNullOrBlank()) {
                        val json = JSONObject(respBody)
                        val candidates = json.optJSONArray("candidates")
                        if (candidates != null && candidates.length() > 0) {
                            val parts = candidates.getJSONObject(0)
                                .optJSONObject("content")
                                ?.optJSONArray("parts")
                            if (parts != null && parts.length() > 0) {
                                val generatedText = parts.getJSONObject(0).optString("text", "").trim()
                                if (generatedText.isNotBlank()) {
                                    return@withContext generatedText.replace("\"", "")
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Gemini direct call failed, using intelligent template engine: ${e.message}")
            }
        }

        // Smart Offline / Fallback Template Engine
        return@withContext when (tone.lowercase()) {
            "formal business", "formal" -> {
                "Dear $recipientName, regarding: $cleanInput. We appreciate your partnership with $senderName. Contact us for any inquiries."
            }
            "quick reminder", "reminder" -> {
                "Friendly reminder for $recipientName: $cleanInput. Kindly take necessary action. Thank you - $senderName."
            }
            "promo & sales", "promo" -> {
                "🔥 Exclusive Deal for $recipientName! $cleanInput. Claim your discount today with $senderName. T&Cs apply."
            }
            "urgent notice", "urgent" -> {
                "🚨 URGENT: $cleanInput. Immediate attention is required for account safety. - $senderName"
            }
            "2fa / otp code", "otp" -> {
                val code = (100000..999999).random()
                "Your $senderName security verification code is $code. Valid for 10 mins. Do not disclose to anyone."
            }
            else -> {
                "Hi $recipientName, $cleanInput. Sent via $senderName Cloud Messenger."
            }
        }.take(160)
    }

    /**
     * Generates 3 contextual smart replies for an ongoing conversation
     */
    suspend fun generateSmartReplies(
        conversationContext: String
    ): List<String> = withContext(Dispatchers.IO) {
        val geminiApiKey: String? = try {
            val key = BuildConfig.GEMINI_API_KEY
            if (key.isNotBlank() && !key.startsWith("default_", ignoreCase = true)) key else null
        } catch (e: Throwable) { null }

        if (!geminiApiKey.isNullOrBlank() && conversationContext.isNotBlank()) {
            try {
                val prompt = """
                    Based on this last SMS received: "$conversationContext"
                    Provide exactly 3 short, natural, one-sentence reply suggestions suitable for SMS.
                    Format output as a JSON array of strings only. Example: ["Understood, thank you!", "I will call you shortly.", "Please send the details."]
                """.trimIndent()

                val payload = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("parts", JSONArray().apply {
                                put(JSONObject().put("text", prompt))
                            })
                        })
                    })
                }

                val request = Request.Builder()
                    .url("$GEMINI_ENDPOINT?key=$geminiApiKey")
                    .post(payload.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val respBody = response.body?.string()
                    if (!respBody.isNullOrBlank()) {
                        val json = JSONObject(respBody)
                        val text = json.optJSONArray("candidates")
                            ?.getJSONObject(0)
                            ?.optJSONObject("content")
                            ?.optJSONArray("parts")
                            ?.getJSONObject(0)
                            ?.optString("text") ?: ""

                        val cleanJson = text.substringAfter("[").substringBeforeLast("]")
                        if (cleanJson.isNotBlank()) {
                            val items = cleanJson.split(",")
                                .map { it.trim().replace("\"", "").replace("'", "") }
                                .filter { it.isNotBlank() }
                            if (items.isNotEmpty()) {
                                return@withContext items.take(3)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Smart replies API call failed: ${e.message}")
            }
        }

        // Default instant smart replies
        val ctxLower = conversationContext.lowercase()
        return@withContext when {
            ctxLower.contains("price") || ctxLower.contains("cost") || ctxLower.contains("pay") -> listOf(
                "I've received the pricing, will confirm payment 👍",
                "Please send the bank account details.",
                "Can we discuss a discount for bulk orders?"
            )
            ctxLower.contains("when") || ctxLower.contains("time") || ctxLower.contains("ready") -> listOf(
                "Will be ready in about 15 minutes.",
                "I'll confirm the exact time shortly.",
                "Let's schedule for this afternoon."
            )
            ctxLower.contains("code") || ctxLower.contains("otp") || ctxLower.contains("verify") -> listOf(
                "Code received and verified successfully.",
                "Didn't receive the code, please resend.",
                "Thank you for the quick verification."
            )
            else -> listOf(
                "Received, thank you for the update! 👍",
                "Sounds good, I'll follow up shortly.",
                "Got it! Let me review and get back to you."
            )
        }
    }
}
