package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object CloudRunApiClient {
    private const val TAG = "CloudRunApiClient"

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(12, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    // Configured Cloud Run endpoints with primary cloud host
    fun getBaseUrls(customWebhookUrl: String? = null): List<String> {
        val configuredUrl = try {
            BuildConfig.CLOUDRUN_BACKEND_URL.takeIf { it.isNotBlank() && it.startsWith("http") }
        } catch (e: Exception) {
            null
        }

        val customOrigin = customWebhookUrl?.trim()?.let { url ->
            try {
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    val uri = java.net.URI(url)
                    val portPart = if (uri.port != -1 && uri.port != 80 && uri.port != 443) ":${uri.port}" else ""
                    "${uri.scheme}://${uri.host}$portPart"
                } else null
            } catch (_: Exception) {
                null
            }
        }

        return listOfNotNull(
            customOrigin,
            configuredUrl,
            "https://ais-dev-b2z7pcf46edgzljhlc7sdi-28824110157.europe-west1.run.app",
            "https://ais-pre-b2z7pcf46edgzljhlc7sdi-28824110157.europe-west1.run.app"
        ).distinct()
    }

    private suspend fun executeGet(path: String): Pair<Int, String>? = withContext(Dispatchers.IO) {
        for (baseUrl in getBaseUrls()) {
            val cleanBase = baseUrl.trimEnd('/')
            val cleanPath = if (path.startsWith("/")) path else "/$path"
            val fullUrl = "$cleanBase$cleanPath"
            try {
                val req = Request.Builder()
                    .url(fullUrl)
                    .get()
                    .build()
                val resp = client.newCall(req).execute()
                val bodyStr = resp.body?.string() ?: ""
                if (resp.isSuccessful) {
                    return@withContext Pair(resp.code, bodyStr)
                }
            } catch (e: Exception) {
                Log.d(TAG, "GET $fullUrl failed: ${e.message}")
            }
        }
        null
    }

    private suspend fun executePost(path: String, jsonObject: JSONObject): Pair<Int, String>? = withContext(Dispatchers.IO) {
        val body = jsonObject.toString().toRequestBody(JSON_MEDIA_TYPE)
        for (baseUrl in getBaseUrls()) {
            val cleanBase = baseUrl.trimEnd('/')
            val cleanPath = if (path.startsWith("/")) path else "/$path"
            val fullUrl = "$cleanBase$cleanPath"
            for (attempt in 1..2) {
                try {
                    val req = Request.Builder()
                        .url(fullUrl)
                        .post(body)
                        .build()
                    val resp = client.newCall(req).execute()
                    val bodyStr = resp.body?.string() ?: ""
                    if (resp.isSuccessful || bodyStr.isNotBlank()) {
                        return@withContext Pair(resp.code, bodyStr)
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "POST $fullUrl failed (attempt $attempt): ${e.message}")
                    if (attempt == 1) {
                        kotlinx.coroutines.delay(800)
                    }
                }
            }
        }
        null
    }

    /**
     * Proxies Pairgate wallet balance query via Cloud Run backend securely.
     */
    suspend fun fetchPairgateBalance(): PairgateBalanceResponse? = withContext(Dispatchers.IO) {
        val result = executeGet("/api/v1/pairgate/balance") ?: executeGet("/api/pairgate/balance")
        if (result != null) {
            try {
                val adapter = moshi.adapter(PairgateBalanceResponse::class.java)
                return@withContext adapter.fromJson(result.second)
            } catch (e: Exception) {
                Log.w(TAG, "Error parsing balance response: ${e.message}")
            }
        }
        null
    }

    /**
     * Proxies Pairgate virtual account creation via Cloud Run backend securely.
     */
    suspend fun createVirtualAccount(
        firstName: String,
        lastName: String,
        email: String,
        phone: String,
        customerReference: String,
        customerName: String,
        bvn: String? = null,
        nin: String? = null
    ): PairgateVirtualAccountResponse? = withContext(Dispatchers.IO) {
        val json = JSONObject().apply {
            put("firstName", firstName)
            put("lastName", lastName)
            put("email", email)
            put("phone", phone)
            put("customerReference", customerReference)
            put("customerName", customerName)
            put("customerEmail", email)
            put("customerPhone", phone)
            if (!bvn.isNullOrBlank()) put("bvn", bvn)
            if (!nin.isNullOrBlank()) put("nin", nin)
        }

        val result = executePost("/api/v1/pairgate/virtual-account", json)
            ?: executePost("/api/pairgate/virtual-account", json)

        if (result != null) {
            try {
                val adapter = moshi.adapter(PairgateVirtualAccountResponse::class.java)
                return@withContext adapter.fromJson(result.second)
            } catch (e: Exception) {
                Log.w(TAG, "Error parsing VA response: ${e.message}")
            }
        }
        null
    }

    /**
     * Proxies Data bundle purchase via Cloud Run backend.
     */
    suspend fun purchaseData(
        network: String,
        planId: String,
        phone: String,
        customerReference: String,
        amount: Double,
        apiKey: String? = null,
        planType: String? = null
    ): PairgateApiResponse? = withContext(Dispatchers.IO) {
        val cleanSlug = when {
            network.contains("mtn", ignoreCase = true) -> "mtn"
            network.contains("glo", ignoreCase = true) -> "glo"
            network.contains("airtel", ignoreCase = true) -> "airtel"
            network.contains("9mobile", ignoreCase = true) || network.contains("etisalat", ignoreCase = true) -> "9mobile"
            else -> network.lowercase().trim()
        }
        val resolvedPlanId = PairgateDataRequest.resolvePairgatePlanId(cleanSlug, planId, amount, planType)
        val verifiedPlan = PairgateVerifiedPlans.findPlanById(resolvedPlanId) ?: PairgateVerifiedPlans.findPlanById(planId)
        val netId = when (cleanSlug) {
            "mtn" -> 1
            "glo" -> 2
            "9mobile" -> 3
            "airtel" -> 4
            else -> 1
        }
        val effectivePlanType = when {
            cleanSlug == "mtn" && (resolvedPlanId == "18" || resolvedPlanId.toIntOrNull() in 14..18) -> "CG"
            planType != null && planType.isNotBlank() -> planType
            verifiedPlan?.planCategory != null -> verifiedPlan.planCategory
            cleanSlug == "mtn" && resolvedPlanId.toIntOrNull() in 19..26 -> "SME"
            cleanSlug == "mtn" && resolvedPlanId.toIntOrNull() in 260..290 -> "DIRECT"
            cleanSlug == "mtn" -> "GIFTING"
            cleanSlug == "airtel" && resolvedPlanId.toIntOrNull() in 89..103 -> "CG"
            cleanSlug == "airtel" && resolvedPlanId.toIntOrNull() in 104..120 -> "SME"
            cleanSlug == "airtel" -> "GIFTING"
            cleanSlug == "glo" && resolvedPlanId.toIntOrNull() in 59..71 -> "CG"
            cleanSlug == "glo" -> "GIFTING"
            cleanSlug == "9mobile" && resolvedPlanId.toIntOrNull() in 128..140 -> "SME"
            cleanSlug == "9mobile" -> "GIFTING"
            else -> "SME"
        }

        val json = JSONObject().apply {
            put("network", cleanSlug)
            put("provider_id", cleanSlug)
            put("network_id", netId)
            put("plan_id", resolvedPlanId)
            put("plan_type", effectivePlanType)
            put("type", effectivePlanType)
            put("phone", phone)
            put("recipient", phone)
            put("customerReference", customerReference)
            put("reference", customerReference)
            put("amount", amount)
            put("auto_renew", 0)
            put("autorenew", 0)
            put("disable_autorenew", true)
            if (!apiKey.isNullOrBlank()) {
                put("apiKey", apiKey)
            }
        }

        val result = executePost("/api/v1/pairgate/purchase-data", json)
            ?: executePost("/api/pairgate/purchase-data", json)

        if (result != null) {
            try {
                val adapter = moshi.adapter(PairgateApiResponse::class.java)
                return@withContext adapter.fromJson(result.second)
            } catch (e: Exception) {
                Log.w(TAG, "Error parsing data purchase response: ${e.message}")
            }
        }
        null
    }

    /**
     * Proxies fetching strictly active Pairgate data plans via Cloud Run backend.
     */
    suspend fun fetchPairgateDataPlans(providerId: String? = null): List<PairgateDataPlanItem> = withContext(Dispatchers.IO) {
        val query = if (!providerId.isNullOrBlank()) "?provider_id=$providerId" else ""
        val result = executeGet("/api/v1/pairgate/plans$query")
            ?: executeGet("/api/pairgate/plans$query")

        if (result != null) {
            try {
                val adapter = moshi.adapter(PairgateDataPlansResponse::class.java)
                val response = adapter.fromJson(result.second)
                val rawItems = response?.extractPlansList() ?: emptyList()
                return@withContext rawItems.filter { it.isAvailable() }
            } catch (e: Exception) {
                Log.w(TAG, "Error parsing plans from CloudRun: ${e.message}")
            }
        }
        emptyList()
    }

    /**
     * Proxies Airtime purchase via Cloud Run backend.
     */
    suspend fun purchaseAirtime(
        network: String,
        amount: Double,
        phone: String,
        customerReference: String
    ): PairgateApiResponse? = withContext(Dispatchers.IO) {
        val json = JSONObject().apply {
            put("network", network)
            put("amount", amount)
            put("phone", phone)
            put("customerReference", customerReference)
        }

        val result = executePost("/api/v1/pairgate/purchase-airtime", json)
            ?: executePost("/api/pairgate/purchase-airtime", json)

        if (result != null) {
            try {
                val adapter = moshi.adapter(PairgateApiResponse::class.java)
                return@withContext adapter.fromJson(result.second)
            } catch (e: Exception) {
                Log.w(TAG, "Error parsing airtime purchase response: ${e.message}")
            }
        }
        null
    }

    /**
     * Executes live electricity purchase directly via Cloud Run and Pairgate backend.
     */
    suspend fun purchaseElectricity(
        providerId: String,
        meterNumber: String,
        meterType: String,
        amount: Int,
        reference: String
    ): PairgateApiResponse? = withContext(Dispatchers.IO) {
        val cleanSlug = PairgateDisCoUtils.resolveSlug(providerId)
        val cleanMeter = meterNumber.replace(Regex("[^0-9]"), "").trim()
        val typeInt = PairgateDisCoUtils.parseMeterTypeInt(meterType)
        val json = JSONObject().apply {
            put("provider_id", cleanSlug)
            put("service_id", cleanSlug)
            put("meter_number", cleanMeter)
            put("customer_id", cleanMeter)
            put("meter_type", typeInt)
            put("type", if (typeInt == 2) "postpaid" else "prepaid")
            put("amount", amount)
            put("reference", reference)
        }

        val result = executePost("/api/v1/pairgate/electricity/purchase", json)
            ?: executePost("/api/v1/pairgate/pay-bill", json)

        if (result != null && result.second.isNotBlank()) {
            val rawJson = result.second
            try {
                val adapter = moshi.adapter(PairgateApiResponse::class.java)
                val parsed = adapter.fromJson(rawJson)
                if (parsed != null) {
                    val tokenFound = parsed.getMeterToken()
                    if (tokenFound.isNullOrBlank()) {
                        // Inspect raw JSON directly for 20-digit token or token field
                        val regex20 = Regex("""\b(\d{20})\b""")
                        val match = regex20.find(rawJson)
                        if (match != null) {
                            return@withContext parsed.copy(token = match.value)
                        }
                    }
                    return@withContext parsed
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error parsing electricity purchase response: ${e.message}")
                // Fallback basic parse using JSONObject
                try {
                    val jo = org.json.JSONObject(rawJson)
                    val rawToken = jo.optString("token")
                        .ifBlank { jo.optString("token_pin") }
                        .ifBlank { jo.optJSONObject("data")?.optString("token") ?: "" }
                    val rawUnits = jo.optString("units")
                        .ifBlank { jo.optJSONObject("data")?.optString("units") ?: "" }
                    val rawAddr = jo.optString("address")
                        .ifBlank { jo.optJSONObject("data")?.optString("address") ?: "" }
                    return@withContext PairgateApiResponse(
                        status = jo.optString("status", "success"),
                        message = jo.optString("message", "Purchase successful"),
                        reference = jo.optString("reference", reference),
                        token = rawToken.ifBlank { null },
                        units = rawUnits.ifBlank { null },
                        address = rawAddr.ifBlank { null }
                    )
                } catch (_: Exception) {}
            }
        }
        null
    }

    /**
     * Queries transaction status and refund status from Cloud Run / Pairgate gateway proxy.
     */
    suspend fun queryPairgateTransactionStatus(referenceCode: String): PairgateApiResponse? = withContext(Dispatchers.IO) {
        val cleanRef = referenceCode.trim()
        if (cleanRef.isBlank()) return@withContext null
        val endpoints = listOf(
            "/api/v1/pairgate/transaction/status?reference_code=${java.net.URLEncoder.encode(cleanRef, "UTF-8")}",
            "/api/v1/pairgate/query/$cleanRef",
            "/api/pairgate/query/$cleanRef",
            "/api/v1/pairgate/status/$cleanRef",
            "/api/pairgate/status?reference=$cleanRef"
        )
        for (ep in endpoints) {
            val result = executeGet(ep)
            if (result != null && result.second.isNotBlank()) {
                try {
                    val adapter = moshi.adapter(PairgateApiResponse::class.java)
                    val parsed = adapter.fromJson(result.second)
                    if (parsed != null) return@withContext parsed
                } catch (e: Exception) {
                    Log.d(TAG, "Error parsing Pairgate query from $ep: ${e.message}")
                }
            }
        }
        null
    }

    /**
     * Proxies Utility / Cable TV bill payment via Cloud Run backend.
     */
    suspend fun payBill(
        serviceId: String,
        customerId: String,
        amount: Double,
        variationId: String? = null
    ): PairgateApiResponse? = withContext(Dispatchers.IO) {
        val cleanCust = customerId.replace(Regex("[^0-9]"), "").trim()
        val isElec = serviceId.contains("edc", ignoreCase = true) ||
                serviceId.contains("ikeja", ignoreCase = true) ||
                serviceId.contains("eko", ignoreCase = true) ||
                serviceId.contains("aedc", ignoreCase = true) ||
                serviceId.contains("ibedc", ignoreCase = true) ||
                serviceId.contains("enugu", ignoreCase = true) ||
                serviceId.contains("kedco", ignoreCase = true) ||
                serviceId.contains("bedc", ignoreCase = true) ||
                serviceId.contains("jedc", ignoreCase = true) ||
                serviceId.contains("yedc", ignoreCase = true) ||
                serviceId.contains("aba", ignoreCase = true)

        if (isElec) {
            return@withContext purchaseElectricity(
                providerId = serviceId,
                meterNumber = cleanCust,
                meterType = "prepaid",
                amount = amount.toInt(),
                reference = "FLOW_ELEC_${System.currentTimeMillis()}"
            )
        }

        val json = JSONObject().apply {
            put("service_id", serviceId)
            put("customer_id", customerId)
            put("amount", amount)
            if (!variationId.isNullOrBlank()) put("variation_id", variationId)
        }

        val result = executePost("/api/v1/pairgate/pay-bill", json)
            ?: executePost("/api/pairgate/pay-bill", json)

        if (result != null) {
            try {
                val adapter = moshi.adapter(PairgateApiResponse::class.java)
                return@withContext adapter.fromJson(result.second)
            } catch (e: Exception) {
                Log.w(TAG, "Error parsing bill pay response: ${e.message}")
            }
        }
        null
    }

    /**
     * Proxies customer / meter verification via Cloud Run & Pairgate backend,
     * returning rich structured utility details for verification receipts and proof of address.
     */
    suspend fun verifyCustomer(
        serviceId: String,
        customerId: String,
        type: String = "prepaid"
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val cleanCust = customerId.trim()
        if (cleanCust.length < 6) {
            return@withContext Pair(false, "Please enter at least 8 digits for account / meter verification.")
        }
        val detailed = verifyMeterDetailed(serviceId, cleanCust, type)
        if (detailed.isValid) {
            Pair(true, "${detailed.customerName} • ${detailed.serviceAddress}")
        } else {
            Pair(false, detailed.message.ifBlank { "Verification failed. Please check your meter number." })
        }
    }

    suspend fun verifyMeterDetailed(
        serviceId: String,
        customerId: String,
        type: String = "prepaid"
    ): com.example.data.model.MeterVerificationResult = withContext(Dispatchers.IO) {
        val cleanCust = customerId.trim()
        val discoClean = PairgateDisCoUtils.getDisplayName(serviceId)
        val providerSlug = PairgateDisCoUtils.resolveSlug(serviceId)
        val meterTypeInt = PairgateDisCoUtils.parseMeterTypeInt(type)

        if (cleanCust.length < 6) {
            return@withContext com.example.data.model.MeterVerificationResult(
                isValid = false,
                meterNumber = cleanCust,
                discoName = discoClean,
                message = "Please enter at least 8 digits on your meter card."
            )
        }

        var parsedName: String? = null
        var parsedAddress: String? = null
        var parsedTariff: String? = null
        var parsedAccount: String? = null
        var errorMessage: String? = null

        try {
            val json = JSONObject().apply {
                put("service_id", providerSlug)
                put("provider_id", providerSlug)
                put("customer_id", cleanCust)
                put("meter_number", cleanCust)
                put("type", if (meterTypeInt == 2) "postpaid" else "prepaid")
                put("meter_type", meterTypeInt)
            }
            val result = executePost("/api/v1/pairgate/verify-customer", json)
                ?: executePost("/api/pairgate/verify-customer", json)
            if (result != null && result.second.isNotBlank()) {
                val root = JSONObject(result.second)
                val isSuccess = root.optString("status").equals("success", ignoreCase = true)
                val innerData = root.optJSONObject("data")

                parsedName = root.optString("customer_name").takeIf { it.isNotBlank() }
                    ?: root.optString("name").takeIf { it.isNotBlank() }
                    ?: innerData?.optString("customer_name")?.takeIf { it.isNotBlank() }
                    ?: innerData?.optString("name")?.takeIf { it.isNotBlank() }

                parsedAddress = root.optString("address").takeIf { it.isNotBlank() }
                    ?: innerData?.optString("address")?.takeIf { it.isNotBlank() }
                    ?: innerData?.optString("customer_address")?.takeIf { it.isNotBlank() }

                parsedTariff = root.optString("tariff").takeIf { it.isNotBlank() }
                    ?: innerData?.optString("tariff")?.takeIf { it.isNotBlank() }
                    ?: innerData?.optString("tariff_class")?.takeIf { it.isNotBlank() }

                parsedAccount = root.optString("account_number").takeIf { it.isNotBlank() }
                    ?: innerData?.optString("account_number")?.takeIf { it.isNotBlank() }
                    ?: cleanCust

                if (!isSuccess || parsedName.isNullOrBlank() || parsedName.equals("Unknown Customer", ignoreCase = true)) {
                    val rawMsg = root.optString("message").takeIf { it.isNotBlank() }
                        ?: innerData?.optString("message")?.takeIf { it.isNotBlank() }
                        ?: "Meter number $cleanCust could not be verified by $discoClean on the system."
                    errorMessage = rawMsg.replace("Pairgate", "FlowTest", ignoreCase = true)
                        .replace("Pair gate", "FlowTest", ignoreCase = true)
                }
            } else {
                errorMessage = "Could not connect to verification gateway. Please check your network connection."
            }
        } catch (e: Exception) {
            Log.d(TAG, "verifyMeterDetailed error: ${e.message}")
            errorMessage = e.message?.replace("Pairgate", "FlowTest", ignoreCase = true) ?: "Verification request failed"
        }

        val isValidMeter = !parsedName.isNullOrBlank() && !parsedName.equals("Unknown Customer", ignoreCase = true)

        if (isValidMeter) {
            com.example.data.model.MeterVerificationResult(
                isValid = true,
                customerName = parsedName.trim(),
                meterNumber = cleanCust,
                serviceAddress = parsedAddress?.trim() ?: "",
                discoName = discoClean,
                meterType = if (meterTypeInt == 2) "POSTPAID" else "PREPAID",
                tariffClass = parsedTariff?.trim()?.takeIf { it.isNotBlank() } ?: "Active Meter",
                accountCode = parsedAccount ?: cleanCust,
                message = "Meter verified successfully on the system."
            )
        } else {
            val safeErr = errorMessage?.replace("Pairgate", "FlowTest", ignoreCase = true)
                ?.replace("Pair gate", "FlowTest", ignoreCase = true)
            com.example.data.model.MeterVerificationResult(
                isValid = false,
                customerName = "",
                meterNumber = cleanCust,
                serviceAddress = "",
                discoName = discoClean,
                meterType = if (meterTypeInt == 2) "POSTPAID" else "PREPAID",
                tariffClass = "",
                accountCode = cleanCust,
                message = safeErr ?: "Meter verification failed: Customer details not found on the system."
            )
        }
    }

    private fun normalizeDiscoTitle(serviceId: String): String {
        return when {
            serviceId.contains("ikeja", ignoreCase = true) || serviceId.contains("ikedc", ignoreCase = true) -> "IKEDC (Ikeja Electric)"
            serviceId.contains("eko", ignoreCase = true) || serviceId.contains("ekedc", ignoreCase = true) -> "EKEDC (Eko Electric)"
            serviceId.contains("abuja", ignoreCase = true) || serviceId.contains("aedc", ignoreCase = true) -> "AEDC (Abuja Electric)"
            serviceId.contains("ibadan", ignoreCase = true) || serviceId.contains("ibedc", ignoreCase = true) -> "IBEDC (Ibadan Electric)"
            serviceId.contains("enugu", ignoreCase = true) || serviceId.contains("eedc", ignoreCase = true) -> "EEDC (Enugu Electric)"
            serviceId.contains("kano", ignoreCase = true) || serviceId.contains("kedco", ignoreCase = true) -> "KEDCO (Kano Electric)"
            serviceId.contains("port", ignoreCase = true) || serviceId.contains("phed", ignoreCase = true) -> "PHED (Port Harcourt Electric)"
            serviceId.contains("benin", ignoreCase = true) || serviceId.contains("bedc", ignoreCase = true) -> "BEDC (Benin Electric)"
            serviceId.contains("kaduna", ignoreCase = true) || serviceId.contains("kaedco", ignoreCase = true) -> "KAEDCO (Kaduna Electric)"
            serviceId.contains("jos", ignoreCase = true) || serviceId.contains("jed", ignoreCase = true) -> "JED (Jos Electric)"
            serviceId.contains("yola", ignoreCase = true) || serviceId.contains("yedc", ignoreCase = true) -> "YEDC (Yola Electric)"
            serviceId.isNotBlank() -> serviceId
            else -> "IKEDC (Ikeja Electric)"
        }
    }

    /**
     * Proxies SMS dispatch via Cloud Run backend.
     */
    suspend fun sendSms(phone: String, message: String): Boolean = withContext(Dispatchers.IO) {
        val json = JSONObject().apply {
            put("phone", phone)
            put("message", message)
        }

        val result = executePost("/api/sms/send", json) ?: executePost("/api/v1/sms/send", json)
        result != null && (result.first in 200..299 || result.second.contains("success", ignoreCase = true))
    }

    /**
     * Fetches recent inbound webhooks from Cloud Run / backend Webhook Hub.
     */
    suspend fun fetchRecentWebhooks(customWebhookUrl: String? = null): List<JSONObject> = withContext(Dispatchers.IO) {
        val endpoints = listOf(
            "/api/v1/moniepoint/webhooks/recent",
            "/api/moniepoint/webhooks/recent",
            "/api/webhooks/recent",
            "/api/v1/webhooks/recent",
            "/webhooks/recent"
        )
        val allItems = mutableListOf<JSONObject>()
        val seenRefs = mutableSetOf<String>()
        val hosts = getBaseUrls(customWebhookUrl)

        for (baseUrl in hosts) {
            val cleanBase = baseUrl.trimEnd('/')
            for (ep in endpoints) {
                val fullUrl = "$cleanBase$ep"
                try {
                    val req = Request.Builder()
                        .url(fullUrl)
                        .get()
                        .build()
                    val resp = client.newCall(req).execute()
                    val bodyStr = resp.body?.string() ?: ""
                    if (resp.isSuccessful && bodyStr.isNotBlank()) {
                        val root = JSONObject(bodyStr)
                        val eventsArray = root.optJSONArray("events")
                            ?: root.optJSONArray("webhooks")
                            ?: root.optJSONArray("data")
                        if (eventsArray != null && eventsArray.length() > 0) {
                            for (i in 0 until eventsArray.length()) {
                                val item = eventsArray.optJSONObject(i) ?: continue
                                val ref = item.optString("reference", "").ifBlank { item.optString("id", "") }
                                if (ref.isNotBlank() && !seenRefs.contains(ref)) {
                                    seenRefs.add(ref)
                                    allItems.add(item)
                                } else if (ref.isBlank()) {
                                    allItems.add(item)
                                }
                            }
                            // Stop further endpoint checks for this specific host
                            break
                        }
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "GET $fullUrl failed: ${e.message}")
                }
            }
        }
        allItems
    }

    /**
     * Dispatches an inbound webhook payload to the Cloud Run Webhook Hub.
     */
    suspend fun dispatchWebhook(
        amount: Double,
        senderName: String,
        narration: String,
        reference: String,
        accountNumber: String = "6666468328"
    ): Boolean = withContext(Dispatchers.IO) {
        val json = JSONObject().apply {
            put("event", "PAYMENT_SUCCESSFUL")
            put("transactionReference", reference)
            put("merchantReference", reference)
            put("amount", amount)
            put("narration", narration)
            put("senderName", senderName)
            put("accountNumber", accountNumber)
        }
        val result = executePost("/api/webhook/moniepoint", json)
            ?: executePost("/api/v1/moniepoint/webhook", json)
            ?: executePost("/moniepoint-webhook", json)
        result != null && result.first in 200..299
    }

    /**
     * Queries Moniepoint transaction status / requery endpoint on Cloud Run.
     */
    suspend fun requeryMoniepointTransaction(reference: String): JSONObject? = withContext(Dispatchers.IO) {
        val cleanRef = reference.trim()
        if (cleanRef.isBlank()) return@withContext null
        val result = executeGet("/api/v1/moniepoint/requery/$cleanRef")
        if (result != null && result.second.isNotBlank()) {
            try {
                return@withContext JSONObject(result.second)
            } catch (e: Exception) {
                Log.d(TAG, "Error parsing requery response for $cleanRef: ${e.message}")
            }
        }
        null
    }

    /**
     * Atomically claims a transaction on the Cloud Run backend ledger.
     * Prevents double-crediting if two checks run simultaneously or across devices.
     * Returns isClaimed = true if the payment has ALREADY been claimed/credited previously.
     */
    suspend fun claimPaymentOnBackend(
        reference: String,
        sessionId: String? = null,
        messageId: String? = null,
        amount: Double,
        narrationCode: String,
        userPhone: String,
        userId: String = "usr_default_1",
        source: String = "GMAIL_ALERT",
        emailData: Map<String, String>? = null
    ): BackendClaimResult? = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("reference", reference.trim())
                if (!sessionId.isNullOrBlank()) put("sessionId", sessionId.trim())
                if (!messageId.isNullOrBlank()) put("messageId", messageId.trim())
                put("amount", amount)
                put("narrationCode", narrationCode.trim())
                put("userPhone", userPhone.trim())
                put("userId", userId)
                put("source", source)
                if (emailData != null) {
                    val emailObj = JSONObject()
                    emailData.forEach { (k, v) -> emailObj.put(k, v) }
                    put("emailData", emailObj)
                }
            }
            val result = executePost("/api/v1/payments/claim", json)
                ?: executePost("/api/payments/claim", json)

            if (result != null && result.second.isNotBlank()) {
                val parsed = JSONObject(result.second)
                val isClaimed = parsed.optBoolean("isClaimed", false)
                val status = parsed.optString("status", "")
                val msg = parsed.optString("message", "")
                val ref = parsed.optString("reference", reference)
                return@withContext BackendClaimResult(
                    isClaimed = isClaimed || status == "ALREADY_CLAIMED",
                    status = status,
                    message = msg,
                    reference = ref
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Backend claim check error: ${e.message}")
        }
        null
    }

    /**
     * Checks if a reference is already claimed on the backend.
     */
    suspend fun checkPaymentClaimedOnBackend(reference: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val cleanRef = reference.trim()
            if (cleanRef.isBlank()) return@withContext false
            val result = executeGet("/api/v1/payments/check-claim/$cleanRef")
                ?: executeGet("/api/payments/check-claim/$cleanRef")
            if (result != null && result.second.isNotBlank()) {
                val parsed = JSONObject(result.second)
                return@withContext parsed.optBoolean("isClaimed", false)
            }
        } catch (e: Exception) {
            Log.d(TAG, "Backend check claim error: ${e.message}")
        }
        false
    }

    /**
     * Ingests parsed email notification details into Cloud Run backend for audit and tracking.
     */
    suspend fun ingestEmailNotificationToBackend(
        reference: String,
        sessionId: String? = null,
        messageId: String? = null,
        rfcMessageId: String? = null,
        amount: Double,
        rawNarration: String,
        detectedCode: String? = null,
        senderName: String,
        senderBank: String? = null,
        dateStr: String,
        fullBodySnippet: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("reference", reference)
                if (!sessionId.isNullOrBlank()) put("sessionId", sessionId)
                if (!messageId.isNullOrBlank()) put("messageId", messageId)
                if (!rfcMessageId.isNullOrBlank()) put("rfcMessageId", rfcMessageId)
                put("amount", amount)
                put("rawNarration", rawNarration)
                if (!detectedCode.isNullOrBlank()) put("detectedCode", detectedCode)
                put("senderName", senderName)
                if (!senderBank.isNullOrBlank()) put("senderBank", senderBank)
                put("dateStr", dateStr)
                put("fullBodySnippet", fullBodySnippet.take(300))
            }
            val result = executePost("/api/v1/email-notifications/ingest", json)
                ?: executePost("/api/email-notifications/ingest", json)
            return@withContext result != null && result.first in 200..299
        } catch (e: Exception) {
            Log.d(TAG, "Ingest email notification to backend error: ${e.message}")
            false
        }
    }

    /**
     * Fetches recent email notifications tracked on the backend.
     */
    suspend fun fetchRecentEmailNotifications(): List<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val result = executeGet("/api/v1/email-notifications/recent")
                ?: executeGet("/api/email-notifications/recent")
            if (result != null && result.second.isNotBlank()) {
                val parsed = JSONObject(result.second)
                val list = mutableListOf<JSONObject>()
                val arr = parsed.optJSONArray("notifications")
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        list.add(arr.getJSONObject(i))
                    }
                }
                return@withContext list
            }
        } catch (e: Exception) {
            Log.d(TAG, "Fetch recent email notifications error: ${e.message}")
        }
        emptyList()
    }

    /**
     * Broadcasts promotional adverts and special offers via Cloud Run backend.
     * Triggers email delivery to active users and logs broadcast receipts.
     */
    suspend fun broadcastAdvertOffer(
        title: String,
        body: String,
        sendPush: Boolean = true,
        sendEmail: Boolean = true,
        targetAudience: String = "all",
        actionUrl: String? = null
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("title", title)
                put("body", body)
                put("sendPush", sendPush)
                put("sendEmail", sendEmail)
                put("targetAudience", targetAudience)
                if (!actionUrl.isNullOrBlank()) put("actionUrl", actionUrl)
            }
            val result = executePost("/api/v1/admin/broadcast-advert", json)
                ?: executePost("/api/admin/broadcast-advert", json)
            if (result != null && result.first in 200..299) {
                val parsed = try { JSONObject(result.second) } catch (_: Throwable) { null }
                val msg = parsed?.optString("message") ?: "Broadcast delivered successfully"
                return@withContext Pair(true, msg)
            } else {
                val err = result?.second ?: "Server connection failed"
                return@withContext Pair(false, err)
            }
        } catch (e: Exception) {
            Log.d(TAG, "Broadcast advert error: ${e.message}")
            Pair(false, e.message ?: "Failed to dispatch broadcast")
        }
    }

    /**
     * Synchronizes a registered user profile to the Cloud Run central registry.
     */
    suspend fun syncUserToCloud(
        id: String,
        name: String,
        email: String,
        phone: String,
        bankName: String,
        accountNumber: String,
        accountName: String,
        reference: String,
        totalFunded: Double,
        status: String,
        role: String,
        walletBalance: Double,
        userPin: String
    ): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("id", id)
                put("customerName", name)
                put("customerEmail", email)
                put("customerPhone", phone)
                put("bankName", bankName)
                put("accountNumber", accountNumber)
                put("accountName", accountName)
                put("reference", reference)
                put("totalFunded", totalFunded)
                put("status", status)
                put("role", role)
                put("walletBalance", walletBalance)
                put("userPin", userPin)
            }
            val result = executePost("/api/v1/users/sync", json) ?: executePost("/api/users/sync", json)
            if (result != null && result.first in 200..299) {
                return@withContext JSONObject(result.second)
            }
        } catch (e: Exception) {
            Log.d(TAG, "syncUserToCloud failed: ${e.message}")
        }
        null
    }

    /**
     * Fetches all registered users from the Cloud Run central registry for Admin Console.
     */
    suspend fun fetchAllUsersFromCloud(): List<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val result = executeGet("/api/v1/admin/users") ?: executeGet("/api/v1/users")
            if (result != null && result.first in 200..299) {
                val obj = JSONObject(result.second)
                val arr = obj.optJSONArray("users")
                val list = mutableListOf<JSONObject>()
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        list.add(arr.getJSONObject(i))
                    }
                }
                return@withContext list
            }
        } catch (e: Exception) {
            Log.d(TAG, "fetchAllUsersFromCloud failed: ${e.message}")
        }
        emptyList()
    }

    /**
     * Fetches the authoritative wallet balance of a user from the cloud.
     */
    suspend fun fetchUserBalanceFromCloud(identifier: String): Double? = withContext(Dispatchers.IO) {
        if (identifier.isBlank()) return@withContext null
        try {
            val encoded = java.net.URLEncoder.encode(identifier.trim(), "UTF-8")
            val result = executeGet("/api/v1/users/$encoded/balance") ?: executeGet("/api/v1/user/balance?identifier=$encoded")
            if (result != null && result.first in 200..299) {
                val obj = JSONObject(result.second)
                if (obj.optBoolean("success", false)) {
                    return@withContext obj.optDouble("walletBalance", 0.0)
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "fetchUserBalanceFromCloud failed: ${e.message}")
        }
        null
    }

    /**
     * Admin adjusts user wallet balance directly on the cloud backend.
     */
    suspend fun adminAdjustBalanceOnCloud(
        identifier: String,
        amountDelta: Double? = null,
        newBalance: Double? = null,
        reason: String = "",
        adminName: String = "Admin"
    ): Pair<Boolean, Double?> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("identifier", identifier.trim())
                if (amountDelta != null) put("amountDelta", amountDelta)
                if (newBalance != null) put("newBalance", newBalance)
                put("reason", reason)
                put("adminName", adminName)
            }
            val result = executePost("/api/v1/admin/users/adjust-balance", json) ?: executePost("/api/v1/admin/credit-user", json)
            if (result != null && result.first in 200..299) {
                val obj = JSONObject(result.second)
                val bal = if (obj.has("newBalance")) obj.optDouble("newBalance") else null
                return@withContext Pair(true, bal)
            }
        } catch (e: Exception) {
            Log.d(TAG, "adminAdjustBalanceOnCloud failed: ${e.message}")
        }
        Pair(false, null)
    }

    /**
     * Reports a user deposit or unresolved funding report to the central Admin Desk.
     */
    suspend fun reportDepositToAdminOnCloud(
        phone: String,
        amount: Double,
        reference: String,
        senderName: String,
        userNote: String,
        userId: String = ""
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("phone", phone.trim())
                put("amount", amount)
                put("reference", reference.trim())
                put("senderName", senderName.trim())
                put("userNote", userNote.trim())
                if (userId.isNotBlank()) put("userId", userId.trim())
            }
            val result = executePost("/api/v1/admin/notifications/report-deposit", json)
                ?: executePost("/api/notifications/deposit-alert", json)
            if (result != null && result.first in 200..299) {
                val obj = JSONObject(result.second)
                val msg = obj.optString("message", "Deposit alert reported to Admin Desk successfully")
                return@withContext Pair(true, msg)
            }
        } catch (e: Exception) {
            Log.d(TAG, "reportDepositToAdminOnCloud failed: ${e.message}")
        }
        Pair(false, "Failed to submit alert to Admin server")
    }

    /**
     * Fetches all inbound customer notifications, funding alerts, and reports for the Admin.
     */
    suspend fun fetchAdminNotificationsFromCloud(): List<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val result = executeGet("/api/v1/admin/notifications") ?: executeGet("/api/admin/notifications")
            if (result != null && result.first in 200..299) {
                val obj = JSONObject(result.second)
                val arr = obj.optJSONArray("notifications")
                val list = mutableListOf<JSONObject>()
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        list.add(arr.getJSONObject(i))
                    }
                }
                return@withContext list
            }
        } catch (e: Exception) {
            Log.d(TAG, "fetchAdminNotificationsFromCloud failed: ${e.message}")
        }
        emptyList()
    }

    /**
     * Admin resolves a customer notification, optionally crediting their wallet immediately.
     */
    suspend fun resolveAdminNotificationOnCloud(
        notificationId: String,
        action: String,
        notes: String = "",
        creditedAmount: Double? = null,
        targetPhone: String? = null
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("notificationId", notificationId)
                put("action", action)
                put("notes", notes)
                if (creditedAmount != null) put("creditedAmount", creditedAmount)
                if (!targetPhone.isNullOrBlank()) put("targetPhone", targetPhone)
            }
            val result = executePost("/api/v1/admin/notifications/resolve", json)
            if (result != null && result.first in 200..299) {
                val obj = JSONObject(result.second)
                val msg = obj.optString("message", "Notification resolved successfully")
                return@withContext Pair(true, msg)
            }
        } catch (e: Exception) {
            Log.d(TAG, "resolveAdminNotificationOnCloud failed: ${e.message}")
        }
        Pair(false, "Failed to resolve notification on server")
    }

    /**
     * Admin dispatches a message or announcement directly to a user or all users.
     */
    suspend fun adminSendMessageToUser(
        targetPhone: String,
        targetEmail: String = "",
        title: String,
        message: String,
        sendSms: Boolean = true,
        sender: String = "Admin Desk"
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("targetPhone", targetPhone.trim())
                put("targetEmail", targetEmail.trim())
                put("title", title.trim())
                put("message", message.trim())
                put("sendSms", sendSms)
                put("sender", sender.trim())
            }
            val result = executePost("/api/v1/admin/messages/send", json)
            if (result != null && result.first in 200..299) {
                val obj = JSONObject(result.second)
                val msg = obj.optString("message", "Message dispatched successfully")
                return@withContext Pair(true, msg)
            }
        } catch (e: Exception) {
            Log.d(TAG, "adminSendMessageToUser failed: ${e.message}")
        }
        Pair(false, "Failed to dispatch message to user")
    }

    /**
     * User checks for inbound messages and alerts from the Admin.
     */
    suspend fun fetchUserMessagesFromCloud(phone: String, email: String = ""): List<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val encodedPhone = java.net.URLEncoder.encode(phone.trim(), "UTF-8")
            val encodedEmail = java.net.URLEncoder.encode(email.trim(), "UTF-8")
            val result = executeGet("/api/v1/user/messages?phone=$encodedPhone&email=$encodedEmail")
            if (result != null && result.first in 200..299) {
                val obj = JSONObject(result.second)
                val arr = obj.optJSONArray("messages")
                val list = mutableListOf<JSONObject>()
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        list.add(arr.getJSONObject(i))
                    }
                }
                return@withContext list
            }
        } catch (e: Exception) {
            Log.d(TAG, "fetchUserMessagesFromCloud failed: ${e.message}")
        }
        emptyList()
    }
}

data class BackendClaimResult(
    val isClaimed: Boolean,
    val status: String,
    val message: String,
    val reference: String
)
