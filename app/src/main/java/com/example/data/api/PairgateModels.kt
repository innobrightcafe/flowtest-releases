package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PairgateAirtimeRequest(
    @Json(name = "provider_id") val providerId: String = "mtn",
    @Json(name = "amount") val amount: Double = 0.0,
    @Json(name = "recipient") val recipient: String = "",
    @Json(name = "reference") val reference: String = ""
) {
    companion object {
        fun create(
            provider: String,
            amount: Double,
            recipientPhone: String,
            reference: String
        ): PairgateAirtimeRequest {
            val cleanSlug = when {
                provider.contains("mtn", ignoreCase = true) -> "mtn"
                provider.contains("glo", ignoreCase = true) -> "glo"
                provider.contains("airtel", ignoreCase = true) -> "airtel"
                provider.contains("9mobile", ignoreCase = true) || provider.contains("etisalat", ignoreCase = true) -> "9mobile"
                else -> provider.lowercase().trim()
            }
            var cleanPhone = recipientPhone.replace(Regex("[^0-9+]"), "").trim()
            if (cleanPhone.startsWith("+234")) {
                cleanPhone = "0" + cleanPhone.substring(4)
            } else if (cleanPhone.startsWith("234") && cleanPhone.length == 13) {
                cleanPhone = "0" + cleanPhone.substring(3)
            } else if (cleanPhone.length == 10 && (cleanPhone.startsWith("8") || cleanPhone.startsWith("7") || cleanPhone.startsWith("9"))) {
                cleanPhone = "0$cleanPhone"
            }

            return PairgateAirtimeRequest(
                providerId = cleanSlug,
                amount = amount,
                recipient = cleanPhone,
                reference = reference
            )
        }
    }
}

@JsonClass(generateAdapter = true)
data class PairgateDataRequest(
    @Json(name = "provider_id") val providerId: String = "mtn",
    @Json(name = "network") val network: String = providerId,
    @Json(name = "network_id") val networkId: Int? = when (providerId.lowercase()) {
        "mtn" -> 1
        "glo" -> 2
        "9mobile", "etisalat" -> 3
        "airtel" -> 4
        else -> 1
    },
    @Json(name = "plan_id") val planId: String = "20",
    @Json(name = "plan_type") val planType: String? = null,
    @Json(name = "type") val type: String? = planType,
    @Json(name = "recipient") val recipient: String = "",
    @Json(name = "phone") val phone: String = recipient,
    @Json(name = "reference") val reference: String = "",
    @Json(name = "auto_renew") val autoRenew: Int = 0,
    @Json(name = "autorenew") val autorenew: Int = 0,
    @Json(name = "disable_autorenew") val disableAutorenew: Boolean = true
) {
    companion object {
        fun create(
            provider: String,
            planIdentifier: String,
            recipientPhone: String,
            reference: String,
            amount: Double = 0.0,
            categoryOverride: String? = null
        ): PairgateDataRequest {
            val cleanSlug = when {
                provider.contains("mtn", ignoreCase = true) -> "mtn"
                provider.contains("glo", ignoreCase = true) -> "glo"
                provider.contains("airtel", ignoreCase = true) -> "airtel"
                provider.contains("9mobile", ignoreCase = true) || provider.contains("etisalat", ignoreCase = true) -> "9mobile"
                else -> provider.lowercase().trim()
            }
            var cleanPhone = recipientPhone.replace(Regex("[^0-9+]"), "").trim()
            if (cleanPhone.startsWith("+234")) {
                cleanPhone = "0" + cleanPhone.substring(4)
            } else if (cleanPhone.startsWith("234") && cleanPhone.length == 13) {
                cleanPhone = "0" + cleanPhone.substring(3)
            } else if (cleanPhone.length == 10 && (cleanPhone.startsWith("8") || cleanPhone.startsWith("7") || cleanPhone.startsWith("9"))) {
                cleanPhone = "0$cleanPhone"
            }

            var pId = planIdentifier.trim()
            if (pId.startsWith("plan_", ignoreCase = true)) {
                pId = pId.substring(5).trim()
            }

            val finalPlanCode = resolvePairgatePlanId(cleanSlug, pId, amount, categoryOverride)

            val netId = when (cleanSlug) {
                "mtn" -> 1
                "glo" -> 2
                "9mobile" -> 3
                "airtel" -> 4
                else -> 1
            }

            // Determine if this is an SME plan, CG, or Gifting
            val verifiedItem = PairgateVerifiedPlans.findPlanById(finalPlanCode)
            val effectivePlanType = categoryOverride ?: verifiedItem?.planCategory ?: when {
                cleanSlug == "mtn" && finalPlanCode.toIntOrNull() in 19..26 -> "SME"
                cleanSlug == "mtn" && finalPlanCode.toIntOrNull() in 14..18 -> "CG"
                cleanSlug == "mtn" && finalPlanCode.toIntOrNull() in 260..290 -> "DIRECT"
                cleanSlug == "mtn" -> "GIFTING"
                cleanSlug == "airtel" && finalPlanCode.toIntOrNull() in 89..103 -> "CG"
                cleanSlug == "airtel" && finalPlanCode.toIntOrNull() in 104..120 -> "SME"
                cleanSlug == "airtel" -> "GIFTING"
                cleanSlug == "glo" && finalPlanCode.toIntOrNull() in 59..71 -> "CG"
                cleanSlug == "glo" -> "GIFTING"
                cleanSlug == "9mobile" && finalPlanCode.toIntOrNull() in 128..140 -> "SME"
                cleanSlug == "9mobile" -> "GIFTING"
                else -> "SME"
            }

            return PairgateDataRequest(
                providerId = cleanSlug,
                network = cleanSlug,
                networkId = netId,
                planId = finalPlanCode,
                planType = effectivePlanType,
                type = effectivePlanType,
                recipient = cleanPhone,
                phone = cleanPhone,
                reference = reference,
                autoRenew = 0,
                autorenew = 0,
                disableAutorenew = true
            )
        }

        fun resolvePairgatePlanId(
            cleanSlug: String,
            planIdentifier: String,
            amount: Double = 0.0,
            categoryOverride: String? = null
        ): String {
            val pId = planIdentifier.trim()
            val lower = pId.lowercase()
            val catLower = (categoryOverride ?: "").lowercase()
            val isSme = lower.contains("sme") || catLower.contains("sme")
            val isCg = lower.contains("cg") || catLower.contains("cg")

            // 1. If it is already a valid numeric ID from Pairgate, verify and preserve it!
            if (pId.matches(Regex("""^\d+$"""))) {
                if (cleanSlug == "mtn" && (pId == "23" || pId == "24")) {
                    // Plan 23 & 24 (MTN SME 5GB) are decommissioned/unavailable on upstream carrier.
                    // Safely route to live 100% deliverable MTN 5GB CG plan 18!
                    return "18"
                }
                val verified = PairgateVerifiedPlans.findPlanById(pId)
                if (verified != null) {
                    if (cleanSlug == "mtn" && verified.getEffectivePlanId() == "24") {
                        return "18"
                    }
                    return pId
                }
                if (pId.toInt() > 10) {
                    if (cleanSlug == "mtn" && (pId == "23" || pId == "24")) {
                        return "18"
                    }
                    return pId
                }
            }

            // 2. Intercept placeholder/dummy IDs (1, 2, 5, 10, etc.) and volume strings
            val is500Mb = lower.contains("500mb") || lower.contains("500 mb")
            val is1Gb = (lower.contains("1gb") || lower.contains("1.0gb") || lower.contains("1 gb") || pId == "1") && !lower.contains("1.5") && !lower.contains("10") && !lower.contains("15")
            val is1_5Gb = lower.contains("1.5gb") || lower.contains("1.5 gb")
            val is2Gb = lower.contains("2gb") || lower.contains("2.0gb") || lower.contains("2 gb") || pId == "2"
            val is3Gb = lower.contains("3gb") || lower.contains("3.0gb") || lower.contains("3 gb")
            val is4Gb = lower.contains("4gb") || lower.contains("4.0gb") || lower.contains("4 gb")
            val is5Gb = lower.contains("5gb") || lower.contains("5.0gb") || lower.contains("5 gb") || pId == "5" || pId == "24"
            val is10Gb = lower.contains("10gb") || lower.contains("10.0gb") || lower.contains("10 gb") || pId == "10"

            return when (cleanSlug) {
                "mtn" -> when {
                    isSme && is500Mb -> "14" // Fallback to live CG 500MB
                    isSme && is1Gb -> "15" // Fallback to live CG 1GB
                    isSme && is2Gb -> "16" // Fallback to live CG 2GB
                    isSme && is3Gb -> "17" // Fallback to live CG 3GB
                    isSme && is4Gb -> "18" // Upstream MTN has no 4GB SME; safely fulfilled via 5GB CG (18)
                    isSme && is5Gb -> "18" // MTN 5GB CG Plan ID on Pairgate (Plan 24 SME is unavailable upstream)
                    isSme && is10Gb -> "278" // MTN 10GB CG Plan ID on Pairgate
                    is500Mb -> "14"
                    is1Gb -> "15"
                    is2Gb -> "16"
                    is3Gb -> "17"
                    is4Gb -> if (isSme) "18" else "271"
                    is5Gb -> "18" // Always use 18 (CG) for MTN 5GB because plan 24 (SME) is unavailable upstream!
                    is10Gb -> if (isCg) "278" else "26"
                    pId.matches(Regex("""^\d+$""")) && (pId.toIntOrNull() ?: 0) > 10 -> if (pId == "24" || pId == "23") "18" else pId
                    else -> "15"
                }
                "airtel" -> when {
                    is500Mb -> "89"
                    is1Gb -> "90"
                    is1_5Gb -> "91"
                    is2Gb -> "92"
                    is3Gb -> "93"
                    is4Gb -> "95"
                    is5Gb -> "96"
                    is10Gb -> "98"
                    pId.matches(Regex("""^\d+$""")) && (pId.toIntOrNull() ?: 0) > 10 -> pId
                    else -> "90"
                }
                "glo" -> when {
                    lower.contains("200mb") -> "59"
                    is500Mb -> "60"
                    is1Gb -> "61"
                    is1_5Gb -> "62"
                    is2Gb -> "64"
                    is3Gb -> "65"
                    is5Gb -> "68"
                    is10Gb -> "71"
                    pId.matches(Regex("""^\d+$""")) && (pId.toIntOrNull() ?: 0) > 10 -> pId
                    else -> "61"
                }
                "9mobile" -> when {
                    is500Mb -> "128"
                    is1Gb -> "129"
                    is1_5Gb -> "130"
                    is2Gb -> "131"
                    is3Gb -> "132"
                    is4Gb -> "133"
                    is5Gb -> "135"
                    is10Gb -> "137"
                    pId.matches(Regex("""^\d+$""")) && (pId.toIntOrNull() ?: 0) > 10 -> pId
                    else -> "129"
                }
                else -> if (pId.matches(Regex("""^\d+$""")) && pId != "1" && pId != "2" && pId != "5" && pId != "10") pId else "15"
            }
        }
    }
}

@JsonClass(generateAdapter = true)
data class PairgateDataPlanItem(
    @Json(name = "id") val id: Any? = null,
    @Json(name = "item_id") val itemId: Any? = null,
    @Json(name = "itemId") val itemIdCamel: Any? = null,
    @Json(name = "item_code") val itemCode: Any? = null,
    @Json(name = "plan_id") val planId: Any? = null,
    @Json(name = "planId") val planIdCamel: Any? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "plan_name") val planName: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "price") val price: Any? = null,
    @Json(name = "amount") val amount: Any? = null,
    @Json(name = "cost") val cost: Any? = null,
    @Json(name = "reseller_price") val resellerPrice: Any? = null,
    @Json(name = "provider_id") val providerId: String? = null,
    @Json(name = "network") val network: String? = null,
    @Json(name = "plan_category") val planCategory: String? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "category") val category: String? = null,
    @Json(name = "validity") val validity: Any? = null,
    @Json(name = "duration") val duration: Any? = null,
    @Json(name = "data_volume") val dataVolume: String? = null,
    @Json(name = "size") val size: String? = null,
    @Json(name = "status") val status: String? = "ACTIVE",
    @Json(name = "state") val state: String? = null,
    @Json(name = "available") val available: Any? = null,
    @Json(name = "is_available") val isAvailableJson: Any? = null,
    @Json(name = "active") val active: Any? = null,
    @Json(name = "is_active") val isActive: Any? = null,
    @Json(name = "enabled") val enabled: Any? = null,
    @Json(name = "in_stock") val inStock: Any? = null,
    @Json(name = "stock") val stock: Any? = null,
    @Json(name = "disabled") val disabled: Any? = null
) {
    fun isAvailable(): Boolean {
        // 1. Must have valid plan id and positive price
        val effId = getEffectivePlanId().trim()
        if (effId.isBlank()) return false
        if (getEffectivePrice() <= 0.0) return false
        // Plans 23 and 24 (MTN SME 5GB) are decommissioned / unavailable on upstream carrier
        if (effId == "23" || effId == "24") return false

        // Inactive / decommissioned indicators in name or description
        val nameLower = getEffectiveName().lowercase()
        if (nameLower.contains("carrier inactive") || nameLower.contains("inactive") ||
            nameLower.contains("unavailable") || nameLower.contains("disabled") ||
            nameLower.contains("decommissioned") || nameLower.contains("out of stock") ||
            nameLower.contains("out_of_stock") || nameLower.contains("deprecated")) {
            return false
        }

        // Upstream Carrier Rule: MTN SME plans (IDs 19..26) are suspended on upstream carrier
        val prov = getEffectiveProvider().uppercase()
        val cat = getEffectiveCategory().uppercase()
        if (prov == "MTN" && cat == "SME") {
            if (effId in listOf("19", "20", "21", "22", "23", "24", "26")) {
                return false
            }
            if (active != true && active != "1" && active != "true" && available != true && available != "true") {
                return false
            }
        }

        // 2. Strict status and state checks
        val s = (status?.toString() ?: state?.toString() ?: "").trim().uppercase()
        if (s.isNotBlank()) {
            if (s == "0" || s == "INACTIVE" || s == "DISABLED" || s == "UNAVAILABLE" ||
                s == "OUT_OF_STOCK" || s == "OUT OF STOCK" || s == "FALSE" || s == "OFF" ||
                s == "PENDING" || s == "SUSPENDED" || s == "CLOSED" || s == "DEPRECATED" ||
                s == "BLOCKED" || s == "EXPIRED") return false
        }

        fun isExplicitlyUnavailable(v: Any?): Boolean {
            if (v == null) return false
            val str = v.toString().trim().lowercase()
            return str == "false" || str == "0" || str == "no" || str == "disabled" || str == "inactive" || str == "off" || str == "unavailable" || str == "pending" || str == "out_of_stock" || str == "out of stock"
        }

        fun isExplicitlyDisabled(v: Any?): Boolean {
            if (v == null) return false
            val str = v.toString().trim().lowercase()
            return str == "true" || str == "1" || str == "yes" || str == "disabled" || str == "inactive"
        }

        if (isExplicitlyUnavailable(available)) return false
        if (isExplicitlyUnavailable(isAvailableJson)) return false
        if (isExplicitlyUnavailable(active)) return false
        if (isExplicitlyUnavailable(isActive)) return false
        if (isExplicitlyUnavailable(enabled)) return false
        if (isExplicitlyUnavailable(inStock)) return false
        if (isExplicitlyDisabled(disabled)) return false

        if (stock != null) {
            val stockNum = stock.toString().replace(Regex("[^0-9.-]"), "").toDoubleOrNull()
            if (stockNum != null && stockNum <= 0.0) return false
        }

        return true
    }

    fun getEffectivePlanId(): String {
        fun cleanId(v: Any?): String {
            if (v == null) return ""
            if (v is Number) {
                if (v.toDouble() == v.toLong().toDouble()) return v.toLong().toString()
            }
            var s = v.toString().trim()
            if (s.endsWith(".0") && s.matches(Regex("""^\d+\.0$"""))) {
                s = s.substringBefore(".0")
            }
            return s
        }
        val cand = cleanId(itemId).ifBlank {
            cleanId(itemIdCamel).ifBlank {
                cleanId(itemCode).ifBlank {
                    cleanId(planId).ifBlank {
                        cleanId(planIdCamel).ifBlank {
                            cleanId(id)
                        }
                    }
                }
            }
        }
        return cand
    }

    fun getEffectiveName(): String {
        return name?.trim()
            ?: description?.trim()
            ?: planName?.trim()
            ?: title?.trim()
            ?: "Data Bundle ${getEffectivePlanId()}"
    }

    fun getEffectivePrice(): Double {
        fun parse(v: Any?): Double? = when (v) {
            is Number -> v.toDouble()
            is String -> v.replace(Regex("[^0-9.]"), "").toDoubleOrNull()
            else -> null
        }
        return parse(price) ?: parse(amount) ?: parse(cost) ?: parse(resellerPrice) ?: 0.0
    }

    fun getEffectiveProvider(): String {
        val raw = providerId ?: network ?: ""
        return when {
            raw.contains("mtn", ignoreCase = true) -> "MTN"
            raw.contains("airtel", ignoreCase = true) -> "Airtel"
            raw.contains("glo", ignoreCase = true) -> "Glo"
            raw.contains("9mobile", ignoreCase = true) || raw.contains("etisalat", ignoreCase = true) -> "9mobile"
            getEffectiveName().contains("mtn", ignoreCase = true) -> "MTN"
            getEffectiveName().contains("airtel", ignoreCase = true) -> "Airtel"
            getEffectiveName().contains("glo", ignoreCase = true) -> "Glo"
            getEffectiveName().contains("9mobile", ignoreCase = true) -> "9mobile"
            else -> raw.ifBlank { "MTN" }.uppercase()
        }
    }

    fun getEffectiveCategory(): String {
        val cat = planCategory ?: type ?: category ?: ""
        val n = getEffectiveName().lowercase()
        return when {
            cat.contains("sme", ignoreCase = true) || n.contains("sme") -> "SME"
            cat.contains("gift", ignoreCase = true) || n.contains("gifting") -> "Gifting"
            cat.contains("corp", ignoreCase = true) || cat.contains("cg", ignoreCase = true) || n.contains("corporate") || n.contains("cg") -> "Corporate"
            cat.contains("dir", ignoreCase = true) || n.contains("direct") -> "Direct"
            else -> cat.ifBlank { "Standard" }
        }
    }

    /**
     * Extracts plan duration in days directly from the Pairgate `duration` field (integer/number or string)
     * with graceful backward-compatible fallback to `validity`, `description`, or plan name.
     */
    fun getEffectiveDurationDays(): Int {
        when (duration) {
            is Number -> return duration.toInt()
            is String -> {
                val digits = duration.replace(Regex("[^0-9]"), "").toIntOrNull()
                if (digits != null && digits > 0) return digits
            }
            else -> {}
        }

        when (validity) {
            is Number -> return validity.toInt()
            is String -> {
                val digits = validity.replace(Regex("[^0-9]"), "").toIntOrNull()
                if (digits != null && digits > 0) return digits
            }
            else -> {}
        }

        val textToSearch = "${description ?: ""} ${name ?: ""} ${planName ?: ""}".lowercase()
        return when {
            textToSearch.contains("365 day") || textToSearch.contains("1 year") || textToSearch.contains("yearly") -> 365
            textToSearch.contains("90 day") || textToSearch.contains("3 month") -> 90
            textToSearch.contains("60 day") || textToSearch.contains("2 month") -> 60
            textToSearch.contains("30 day") || textToSearch.contains("month") || textToSearch.contains("30days") -> 30
            textToSearch.contains("14 day") || textToSearch.contains("2 week") -> 14
            textToSearch.contains("7 day") || textToSearch.contains("1 week") || textToSearch.contains("7days") -> 7
            textToSearch.contains("2 day") || textToSearch.contains("48 hr") -> 2
            textToSearch.contains("1 day") || textToSearch.contains("daily") || textToSearch.contains("24 hr") -> 1
            else -> 30
        }
    }

    fun getEffectiveKey(): String = "${getEffectiveProvider().uppercase()}_${getEffectivePlanId()}"

    fun isHotPlan(): Boolean {
        val n = getEffectiveName().lowercase()
        val p = getEffectivePlanId()
        val cat = getEffectiveCategory()
        val isHotVolume = n.contains("1gb") || n.contains("1.0gb") || n.contains("1.5gb") || 
                          n.contains("2gb") || n.contains("2.0gb") || n.contains("3gb") || 
                          n.contains("5gb") || n.contains("10gb") || n.contains("special")
        val isPopularCategory = cat == "SME" || cat == "CG" || cat == "Corporate"
        return (isHotVolume && (isPopularCategory || getEffectiveDurationDays() == 30)) || 
               p in listOf("14", "15", "16", "17", "18", "19", "20", "21", "22", "24", "26", 
                          "89", "90", "91", "92", "93", "95", "98", "59", "60", "61", "64", "65", 
                          "68", "71", "128", "129", "131", "132", "135", "137")
    }

    fun getEffectiveValidity(): String {
        val days = getEffectiveDurationDays()
        return when (days) {
            1 -> "1 Day"
            else -> "$days Days"
        }
    }

    fun getValidityCategory(): String {
        val days = getEffectiveDurationDays()
        return when {
            days <= 2 -> "Daily"
            days in 3..14 -> "Weekly"
            days in 15..90 -> "Monthly"
            days > 90 -> "Yearly"
            else -> "Monthly"
        }
    }
}

@JsonClass(generateAdapter = true)
data class RecentDataPurchase(
    @Json(name = "network") val network: String = "MTN",
    @Json(name = "plan_id") val planId: String = "20",
    @Json(name = "plan_name") val planName: String = "MTN 1.0GB SME (30 Days)",
    @Json(name = "recipient_phone") val recipientPhone: String = "",
    @Json(name = "amount_naira") val amountNaira: Double = 280.0,
    @Json(name = "validity") val validity: String = "30 Days",
    @Json(name = "category") val category: String = "SME",
    @Json(name = "timestamp") val timestamp: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class PairgateDataPlansResponse(
    @Json(name = "status") val status: Any? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "data") val data: Any? = null,
    @Json(name = "plans") val plans: List<PairgateDataPlanItem>? = null
) {
    fun extractPlansList(): List<PairgateDataPlanItem> {
        val result = mutableListOf<PairgateDataPlanItem>()

        if (!plans.isNullOrEmpty()) {
            result.addAll(plans)
        }

        when (val d = data) {
            is List<*> -> {
                for (item in d) {
                    if (item is Map<*, *>) {
                        val parsed = parseMapToItem(item)
                        if (parsed != null && parsed.getEffectivePlanId().isNotBlank()) {
                            result.add(parsed)
                        }
                    }
                }
            }
            is Map<*, *> -> {
                for ((key, value) in d) {
                    val keyStr = key?.toString() ?: ""
                    if (value is List<*>) {
                        for (item in value) {
                            if (item is Map<*, *>) {
                                val parsed = parseMapToItem(item, defaultProvider = keyStr)
                                if (parsed != null && parsed.getEffectivePlanId().isNotBlank()) {
                                    result.add(parsed)
                                }
                            }
                        }
                    } else if (value is Map<*, *>) {
                        val parsed = parseMapToItem(value, defaultProvider = keyStr)
                        if (parsed != null && parsed.getEffectivePlanId().isNotBlank()) {
                            result.add(parsed)
                        }
                    }
                }
            }
            else -> {}
        }

        return result
    }

    private fun parseMapToItem(map: Map<*, *>, defaultProvider: String? = null): PairgateDataPlanItem? {
        val pId = map["plan_id"] ?: map["item_id"] ?: map["itemId"] ?: map["id"] ?: map["planId"] ?: map["code"] ?: map["item_code"] ?: map["variation_code"] ?: return null
        val name = map["name"]?.toString() ?: map["plan_name"]?.toString() ?: map["title"]?.toString()
        val description = map["description"]?.toString() ?: map["desc"]?.toString()
        val price = map["price"] ?: map["amount"] ?: map["cost"] ?: map["reseller_price"]
        val provider = map["provider"]?.toString() ?: map["provider_id"]?.toString() ?: map["provider_name"]?.toString() ?: map["network"]?.toString() ?: defaultProvider
        val category = map["plan_type"]?.toString() ?: map["plan_category"]?.toString() ?: map["type"]?.toString() ?: map["category"]?.toString()
        val duration = map["duration"] ?: map["validity"]
        val validity = map["validity"] ?: map["duration"]
        val volume = map["data_volume"]?.toString() ?: map["size"]?.toString()
        val status = map["status"]?.toString() ?: map["state"]?.toString() ?: map["plan_status"]?.toString() ?: "ACTIVE"
        val state = map["state"]?.toString()
        val available = map["available"] ?: map["is_available"] ?: map["isAvailable"] ?: map["active"] ?: map["is_active"] ?: map["enabled"]
        val active = map["active"] ?: map["is_active"]
        val isActive = map["is_active"] ?: map["isActive"]
        val enabled = map["enabled"] ?: map["is_enabled"]
        val inStock = map["in_stock"] ?: map["inStock"] ?: map["is_in_stock"]
        val stock = map["stock"] ?: map["quantity"]
        val disabled = map["disabled"] ?: map["is_disabled"]

        return PairgateDataPlanItem(
            id = pId,
            itemId = map["item_id"] ?: pId,
            itemIdCamel = map["itemId"] ?: pId,
            itemCode = map["item_code"] ?: pId,
            planId = map["plan_id"] ?: pId,
            name = name,
            description = description,
            price = price,
            providerId = provider,
            network = provider,
            planCategory = category,
            validity = validity,
            duration = duration,
            dataVolume = volume,
            status = status,
            state = state,
            available = available,
            isAvailableJson = map["is_available"] ?: map["isAvailable"],
            active = active,
            isActive = isActive,
            enabled = enabled,
            inStock = inStock,
            stock = stock,
            disabled = disabled
        )
    }
}

object PairgateVerifiedPlans {
    const val MARKUP_PERCENT = 3.5

    /**
     * Determines whether a plan wholesale price matches standard Nigerian Telco direct/gifting retail denominations
     * (e.g., ₦500, ₦1000, ₦1500, ₦1800, ₦2000, ₦2500, ₦3000, ₦3500, ₦4000, ₦5000, etc.)
     */
    fun isOfficialTelcoFaceValue(wholesale: Double): Boolean {
        val w = wholesale.toInt()
        val standardTelcoDenominations = setOf(
            50, 75, 100, 200, 300, 350, 500, 600, 750, 800, 900, 1000,
            1200, 1500, 1800, 2000, 2500, 3000, 3500, 4000, 4500, 5000,
            6000, 6500, 7500, 8000, 9000, 10000, 11000, 14500, 16000, 18000,
            24000, 30000, 35000, 40000, 75000, 90000, 225000
        )
        return standardTelcoDenominations.contains(w)
    }

    /**
     * Computes the intelligent retail price with selective dynamic markups per category:
     * - SME bundles: Higher margin (e.g. +8.0%) since wholesale is low (₦285 for 1GB vs ₦1,000 retail)
     * - CG bundles: Selective margin (e.g. +6.0%)
     * - Broadband Router bundles: Selective margin (e.g. +4.0%)
     * - Social bundles: Selective margin (e.g. +5.0%)
     * - Direct / Gifting bundles: Zero added margin (capped strictly at official telco face value).
     *   The reseller earns profit from Pairgate's automated wholesale cashback/rebates!
     */
    fun getRetailPriceForCategory(
        category: String,
        wholesale: Double,
        generalMarkupPercent: Double = MARKUP_PERCENT,
        smeMarkupPercent: Double = 8.0,
        cgMarkupPercent: Double = 6.0,
        directMarkupPercent: Double = 0.0,
        broadbandMarkupPercent: Double = 4.0,
        socialMarkupPercent: Double = 5.0,
        pricingStrategy: String = "SMART_TELCO_CAP",
        discountPercent: Double = 0.0
    ): Double {
        if (wholesale <= 0.0) return 0.0

        val catLower = category.lowercase()
        val isSme = catLower.contains("sme")
        val isCg = catLower.contains("cg") || catLower.contains("corporate")
        val isBroadband = catLower.contains("broadband") || catLower.contains("router") || catLower.contains("hynetflex")
        val isSocial = catLower.contains("social")
        val isDirectOrGifting = catLower.contains("gift") || catLower.contains("direct") || (!isSme && !isCg && isOfficialTelcoFaceValue(wholesale))

        val effMarkup = when {
            isSme -> smeMarkupPercent
            isCg -> cgMarkupPercent
            isBroadband -> broadbandMarkupPercent
            isSocial -> socialMarkupPercent
            isDirectOrGifting -> directMarkupPercent
            else -> generalMarkupPercent
        }

        return when (pricingStrategy) {
            "SMART_TELCO_CAP" -> {
                if (isDirectOrGifting && effMarkup <= 0.0) {
                    // Strictly cap at official telco face value so user never pays more than telco.
                    // Reseller receives pairgate system cashback on backend!
                    wholesale
                } else {
                    val markup = wholesale * (effMarkup / 100.0)
                    Math.ceil((wholesale + markup) / 5.0) * 5.0
                }
            }
            "COMPETITIVE_DISCOUNT" -> {
                if (isDirectOrGifting) {
                    val effectiveDiscount = if (discountPercent > 0.0) discountPercent else 1.0
                    val discounted = wholesale * (1.0 - (effectiveDiscount / 100.0))
                    (Math.floor(discounted / 5.0) * 5.0).coerceAtMost(wholesale - 5.0).coerceAtLeast(wholesale * 0.95)
                } else {
                    val markup = wholesale * (effMarkup / 100.0)
                    Math.ceil((wholesale + markup) / 5.0) * 5.0
                }
            }
            "FLAT_MARKUP" -> {
                val markup = wholesale * (effMarkup / 100.0)
                Math.ceil((wholesale + markup) / 5.0) * 5.0
            }
            else -> {
                if (isDirectOrGifting && effMarkup <= 0.0) wholesale else {
                    val markup = wholesale * (effMarkup / 100.0)
                    Math.ceil((wholesale + markup) / 5.0) * 5.0
                }
            }
        }
    }

    /**
     * Backward-compatible overload
     */
    fun getRetailPrice(
        wholesale: Double,
        markupPercent: Double = MARKUP_PERCENT,
        isGiftingOrDirect: Boolean = false,
        pricingStrategy: String = "SMART_TELCO_CAP",
        discountPercent: Double = 0.0
    ): Double {
        val cat = if (isGiftingOrDirect) "Gifting" else "Data Bundles"
        return getRetailPriceForCategory(
            category = cat,
            wholesale = wholesale,
            generalMarkupPercent = markupPercent,
            smeMarkupPercent = markupPercent,
            cgMarkupPercent = markupPercent,
            directMarkupPercent = if (pricingStrategy == "FLAT_MARKUP") markupPercent else 0.0,
            broadbandMarkupPercent = markupPercent,
            socialMarkupPercent = markupPercent,
            pricingStrategy = pricingStrategy,
            discountPercent = discountPercent
        )
    }

    fun findPlanById(planId: String): PairgateDataPlanItem? {
        val clean = planId.trim().lowercase()
        return ALL_PLANS.find { (it.getEffectivePlanId().lowercase() == clean || it.id?.toString()?.lowercase() == clean) && it.isAvailable() }
    }

    val ALL_PLANS: List<PairgateDataPlanItem> = listOf(
        // ======================= MTN (CG) =======================
        PairgateDataPlanItem(id = "14", planId = "14", itemId = "14", name = "500MB (CG)", price = 285.0, providerId = "MTN", network = "MTN", planCategory = "CG", type = "CG", validity = "7 Days", duration = 7, dataVolume = "500MB"),
        PairgateDataPlanItem(id = "15", planId = "15", itemId = "15", name = "1GB (CG)", price = 425.0, providerId = "MTN", network = "MTN", planCategory = "CG", type = "CG", validity = "7 Days", duration = 7, dataVolume = "1GB"),
        PairgateDataPlanItem(id = "16", planId = "16", itemId = "16", name = "2GB (CG)", price = 850.0, providerId = "MTN", network = "MTN", planCategory = "CG", type = "CG", validity = "30 Days", duration = 30, dataVolume = "2GB"),
        PairgateDataPlanItem(id = "17", planId = "17", itemId = "17", name = "3GB (CG)", price = 1275.0, providerId = "MTN", network = "MTN", planCategory = "CG", type = "CG", validity = "30 Days", duration = 30, dataVolume = "3GB"),
        PairgateDataPlanItem(id = "18", planId = "18", itemId = "18", name = "5GB (CG)", price = 1600.0, providerId = "MTN", network = "MTN", planCategory = "CG", type = "CG", validity = "30 Days", duration = 30, dataVolume = "5GB"),
        // ======================= MTN (GIFTING) =======================
        PairgateDataPlanItem(id = "364", planId = "364", itemId = "364", name = "75MB (GIFTING)", price = 80.0, providerId = "MTN", network = "MTN", planCategory = "GIFTING", type = "GIFTING", validity = "1 Day", duration = 1, dataVolume = "75MB"),
        PairgateDataPlanItem(id = "302", planId = "302", itemId = "302", name = "110MB", price = 100.0, providerId = "MTN", network = "MTN", planCategory = "GIFTING", type = "GIFTING", validity = "1 Day", duration = 1, dataVolume = "110MB"),
        PairgateDataPlanItem(id = "332", planId = "332", itemId = "332", name = "230MB", price = 200.0, providerId = "MTN", network = "MTN", planCategory = "GIFTING", type = "GIFTING", validity = "1 Day", duration = 1, dataVolume = "230MB"),
        PairgateDataPlanItem(id = "301", planId = "301", itemId = "301", name = "500MB", price = 350.0, providerId = "MTN", network = "MTN", planCategory = "GIFTING", type = "GIFTING", validity = "1 Day", duration = 1, dataVolume = "500MB"),
        PairgateDataPlanItem(id = "333", planId = "333", itemId = "333", name = "600MB + 2 Mins", price = 500.0, providerId = "MTN", network = "MTN", planCategory = "GIFTING", type = "GIFTING", validity = "7 Days", duration = 7, dataVolume = "600MB"),
        PairgateDataPlanItem(id = "316", planId = "316", itemId = "316", name = "500MB", price = 500.0, providerId = "MTN", network = "MTN", planCategory = "GIFTING", type = "GIFTING", validity = "7 Days", duration = 7, dataVolume = "500MB"),
        PairgateDataPlanItem(id = "299", planId = "299", itemId = "299", name = "500MB + 1GB Youtube", price = 500.0, providerId = "MTN", network = "MTN", planCategory = "GIFTING", type = "GIFTING", validity = "7 Days", duration = 7, dataVolume = "500MB"),
        PairgateDataPlanItem(id = "300", planId = "300", itemId = "300", name = "1GB Daily Data + 1.5 Mins", price = 500.0, providerId = "MTN", network = "MTN", planCategory = "GIFTING", type = "GIFTING", validity = "1 Day", duration = 1, dataVolume = "1GB"),
        PairgateDataPlanItem(id = "285", planId = "285", itemId = "285", name = "1.5GB", price = 600.0, providerId = "MTN", network = "MTN", planCategory = "GIFTING", type = "GIFTING", validity = "2 Days", duration = 2, dataVolume = "1.5GB"),
        PairgateDataPlanItem(id = "274", planId = "274", itemId = "274", name = "2.5GB", price = 750.0, providerId = "MTN", network = "MTN", planCategory = "GIFTING", type = "GIFTING", validity = "1 Day", duration = 1, dataVolume = "2.5GB"),
        PairgateDataPlanItem(id = "297", planId = "297", itemId = "297", name = "2GB", price = 750.0, providerId = "MTN", network = "MTN", planCategory = "GIFTING", type = "GIFTING", validity = "2 Days", duration = 2, dataVolume = "2GB"),
        PairgateDataPlanItem(id = "295", planId = "295", itemId = "295", name = "1GB + 1GB Youtube Night", price = 800.0, providerId = "MTN", network = "MTN", planCategory = "GIFTING", type = "GIFTING", validity = "7 Days", duration = 7, dataVolume = "1GB"),
        PairgateDataPlanItem(id = "298", planId = "298", itemId = "298", name = "2.5GB", price = 900.0, providerId = "MTN", network = "MTN", planCategory = "GIFTING", type = "GIFTING", validity = "2 Days", duration = 2, dataVolume = "2.5GB"),
        PairgateDataPlanItem(id = "272", planId = "272", itemId = "272", name = "3.5GB", price = 1000.0, providerId = "MTN", network = "MTN", planCategory = "GIFTING", type = "GIFTING", validity = "1 Day", duration = 1, dataVolume = "3.5GB"),
        PairgateDataPlanItem(id = "283", planId = "283", itemId = "283", name = "3.2GB", price = 1000.0, providerId = "MTN", network = "MTN", planCategory = "GIFTING", type = "GIFTING", validity = "2 Days", duration = 2, dataVolume = "3.2GB"),
        PairgateDataPlanItem(id = "281", planId = "281", itemId = "281", name = "1.5GB", price = 1000.0, providerId = "MTN", network = "MTN", planCategory = "GIFTING", type = "GIFTING", validity = "7 Days", duration = 7, dataVolume = "1.5GB"),
        PairgateDataPlanItem(id = "315", planId = "315", itemId = "315", name = "1GB", price = 1000.0, providerId = "MTN", network = "MTN", planCategory = "GIFTING", type = "GIFTING", validity = "30 Days", duration = 30, dataVolume = "1GB"),
        PairgateDataPlanItem(id = "271", planId = "271", itemId = "271", name = "4GB", price = 1200.0, providerId = "MTN", network = "MTN", planCategory = "GIFTING", type = "GIFTING", validity = "2 Days", duration = 2, dataVolume = "4GB"),
        PairgateDataPlanItem(id = "296", planId = "296", itemId = "296", name = "2GB + 2 Mins", price = 1500.0, providerId = "MTN", network = "MTN", planCategory = "GIFTING", type = "GIFTING", validity = "30 Days", duration = 30, dataVolume = "2GB"),
        PairgateDataPlanItem(id = "318", planId = "318", itemId = "318", name = "3.5GB", price = 1500.0, providerId = "MTN", network = "MTN", planCategory = "GIFTING", type = "GIFTING", validity = "7 Days", duration = 7, dataVolume = "3.5GB"),
        PairgateDataPlanItem(id = "293", planId = "293", itemId = "293", name = "1.8GB Xtra Bundle", price = 1500.0, providerId = "MTN", network = "MTN", planCategory = "GIFTING", type = "GIFTING", validity = "30 Days", duration = 30, dataVolume = "1.8GB"),
        PairgateDataPlanItem(id = "270", planId = "270", itemId = "270", name = "5.5GB", price = 1500.0, providerId = "MTN", network = "MTN", planCategory = "GIFTING", type = "GIFTING", validity = "2 Days", duration = 2, dataVolume = "5.5GB"),
        PairgateDataPlanItem(id = "323", planId = "323", itemId = "323", name = "7GB", price = 1800.0, providerId = "MTN", network = "MTN", planCategory = "GIFTING", type = "GIFTING", validity = "2 Days", duration = 2, dataVolume = "7GB"),
        PairgateDataPlanItem(id = "331", planId = "331", itemId = "331", name = "2.7GB + 2 Mins", price = 2000.0, providerId = "MTN", network = "MTN", planCategory = "GIFTING", type = "GIFTING", validity = "30 Days", duration = 30, dataVolume = "2.7GB"),
        PairgateDataPlanItem(id = "329", planId = "329", itemId = "329", name = "3GB", price = 2000.0, providerId = "MTN", network = "MTN", planCategory = "GIFTING", type = "GIFTING", validity = "30 Days", duration = 30, dataVolume = "3GB"),
        PairgateDataPlanItem(id = "319", planId = "319", itemId = "319", name = "5GB", price = 2500.0, providerId = "MTN", network = "MTN", planCategory = "GIFTING", type = "GIFTING", validity = "30 Days", duration = 30, dataVolume = "5GB"),
        PairgateDataPlanItem(id = "320", planId = "320", itemId = "320", name = "6GB", price = 2500.0, providerId = "MTN", network = "MTN", planCategory = "GIFTING", type = "GIFTING", validity = "7 Days", duration = 7, dataVolume = "6GB"),
        PairgateDataPlanItem(id = "292", planId = "292", itemId = "292", name = "3.5GB + 5 Mins", price = 2500.0, providerId = "MTN", network = "MTN", planCategory = "GIFTING", type = "GIFTING", validity = "30 Days", duration = 30, dataVolume = "3.5GB"),
        PairgateDataPlanItem(id = "330", planId = "330", itemId = "330", name = "6.75GB", price = 3000.0, providerId = "MTN", network = "MTN", planCategory = "GIFTING", type = "GIFTING", validity = "30 Days", duration = 30, dataVolume = "6.75GB"),
        PairgateDataPlanItem(id = "294", planId = "294", itemId = "294", name = "2.7GB Xtra Bundle", price = 3000.0, providerId = "MTN", network = "MTN", planCategory = "GIFTING", type = "GIFTING", validity = "30 Days", duration = 30, dataVolume = "2.7GB"),
        PairgateDataPlanItem(id = "286", planId = "286", itemId = "286", name = "7GB", price = 3500.0, providerId = "MTN", network = "MTN", planCategory = "GIFTING", type = "GIFTING", validity = "30 Days", duration = 30, dataVolume = "7GB"),
        PairgateDataPlanItem(id = "290", planId = "290", itemId = "290", name = "11GB", price = 3500.0, providerId = "MTN", network = "MTN", planCategory = "GIFTING", type = "GIFTING", validity = "7 Days", duration = 7, dataVolume = "11GB"),
        PairgateDataPlanItem(id = "327", planId = "327", itemId = "327", name = "15GB", price = 4000.0, providerId = "MTN", network = "MTN", planCategory = "GIFTING", type = "GIFTING", validity = "7 Days", duration = 7, dataVolume = "15GB"),
        PairgateDataPlanItem(id = "291", planId = "291", itemId = "291", name = "10GB + 10mins", price = 4500.0, providerId = "MTN", network = "MTN", planCategory = "GIFTING", type = "GIFTING", validity = "30 Days", duration = 30, dataVolume = "10GB"),
        PairgateDataPlanItem(id = "326", planId = "326", itemId = "326", name = "12.5GB", price = 4500.0, providerId = "MTN", network = "MTN", planCategory = "GIFTING", type = "GIFTING", validity = "14 Days", duration = 14, dataVolume = "12.5GB"),
        PairgateDataPlanItem(id = "273", planId = "273", itemId = "273", name = "20GB", price = 5000.0, providerId = "MTN", network = "MTN", planCategory = "GIFTING", type = "GIFTING", validity = "7 Days", duration = 7, dataVolume = "20GB"),
        PairgateDataPlanItem(id = "321", planId = "321", itemId = "321", name = "14.5GB", price = 5000.0, providerId = "MTN", network = "MTN", planCategory = "GIFTING", type = "GIFTING", validity = "30 Days", duration = 30, dataVolume = "14.5GB"),
        PairgateDataPlanItem(id = "325", planId = "325", itemId = "325", name = "18GB", price = 6000.0, providerId = "MTN", network = "MTN", planCategory = "GIFTING", type = "GIFTING", validity = "14 Days", duration = 14, dataVolume = "18GB"),
        PairgateDataPlanItem(id = "289", planId = "289", itemId = "289", name = "16.5GB", price = 6500.0, providerId = "MTN", network = "MTN", planCategory = "GIFTING", type = "GIFTING", validity = "30 Days", duration = 30, dataVolume = "16.5GB"),
        PairgateDataPlanItem(id = "287", planId = "287", itemId = "287", name = "20GB", price = 7500.0, providerId = "MTN", network = "MTN", planCategory = "GIFTING", type = "GIFTING", validity = "30 Days", duration = 30, dataVolume = "20GB"),
        PairgateDataPlanItem(id = "328", planId = "328", itemId = "328", name = "28GB", price = 8000.0, providerId = "MTN", network = "MTN", planCategory = "GIFTING", type = "GIFTING", validity = "14 Days", duration = 14, dataVolume = "28GB"),
        PairgateDataPlanItem(id = "334", planId = "334", itemId = "334", name = "30GB Hynetflex Router Only Data Plan", price = 9000.0, providerId = "MTN", network = "MTN", planCategory = "BROADBAND", type = "BROADBAND", validity = "30 Days", duration = 30, dataVolume = "30GB"),
        PairgateDataPlanItem(id = "324", planId = "324", itemId = "324", name = "40GB", price = 10000.0, providerId = "MTN", network = "MTN", planCategory = "GIFTING", type = "GIFTING", validity = "14 Days", duration = 14, dataVolume = "40GB"),
        PairgateDataPlanItem(id = "269", planId = "269", itemId = "269", name = "34GB", price = 10000.0, providerId = "MTN", network = "MTN", planCategory = "GIFTING", type = "GIFTING", validity = "30 Days", duration = 30, dataVolume = "34GB"),
        PairgateDataPlanItem(id = "279", planId = "279", itemId = "279", name = "36GB", price = 11000.0, providerId = "MTN", network = "MTN", planCategory = "GIFTING", type = "GIFTING", validity = "30 Days", duration = 30, dataVolume = "36GB"),
        PairgateDataPlanItem(id = "276", planId = "276", itemId = "276", name = "60GB Hynetflex Router Only", price = 14500.0, providerId = "MTN", network = "MTN", planCategory = "BROADBAND", type = "BROADBAND", validity = "30 Days", duration = 30, dataVolume = "60GB"),
        PairgateDataPlanItem(id = "288", planId = "288", itemId = "288", name = "65GB", price = 16000.0, providerId = "MTN", network = "MTN", planCategory = "GIFTING", type = "GIFTING", validity = "30 Days", duration = 30, dataVolume = "65GB"),
        PairgateDataPlanItem(id = "278", planId = "278", itemId = "278", name = "75GB", price = 18000.0, providerId = "MTN", network = "MTN", planCategory = "GIFTING", type = "GIFTING", validity = "30 Days", duration = 30, dataVolume = "75GB"),
        PairgateDataPlanItem(id = "284", planId = "284", itemId = "284", name = "120GB + 5GB Youtube/ms Teams / Zoom", price = 24000.0, providerId = "MTN", network = "MTN", planCategory = "GIFTING", type = "GIFTING", validity = "30 Days", duration = 30, dataVolume = "120GB"),
        PairgateDataPlanItem(id = "322", planId = "322", itemId = "322", name = "150GB + 2GB Daily 5g Router", price = 30000.0, providerId = "MTN", network = "MTN", planCategory = "BROADBAND", type = "BROADBAND", validity = "30 Days", duration = 30, dataVolume = "150GB"),
        PairgateDataPlanItem(id = "280", planId = "280", itemId = "280", name = "165GB", price = 35000.0, providerId = "MTN", network = "MTN", planCategory = "GIFTING", type = "GIFTING", validity = "30 Days", duration = 30, dataVolume = "165GB"),
        PairgateDataPlanItem(id = "277", planId = "277", itemId = "277", name = "150GB", price = 40000.0, providerId = "MTN", network = "MTN", planCategory = "GIFTING", type = "GIFTING", validity = "60 Days", duration = 60, dataVolume = "150GB"),
        PairgateDataPlanItem(id = "275", planId = "275", itemId = "275", name = "450GB", price = 75000.0, providerId = "MTN", network = "MTN", planCategory = "GIFTING", type = "GIFTING", validity = "90 Days", duration = 90, dataVolume = "450GB"),
        PairgateDataPlanItem(id = "282", planId = "282", itemId = "282", name = "480GB", price = 90000.0, providerId = "MTN", network = "MTN", planCategory = "GIFTING", type = "GIFTING", validity = "90 Days", duration = 90, dataVolume = "480GB"),
        PairgateDataPlanItem(id = "317", planId = "317", itemId = "317", name = "1.5TB Data", price = 225000.0, providerId = "MTN", network = "MTN", planCategory = "GIFTING", type = "GIFTING", validity = "365 Days", duration = 365, dataVolume = "1.5TB"),
        // ======================= AIRTEL (CG) =======================
        PairgateDataPlanItem(id = "89", planId = "89", itemId = "89", name = "500MB (CG)", price = 500.0, providerId = "AIRTEL", network = "AIRTEL", planCategory = "CG", type = "CG", validity = "7 Days", duration = 7, dataVolume = "500MB"),
        PairgateDataPlanItem(id = "90", planId = "90", itemId = "90", name = "1GB (CG)", price = 830.0, providerId = "AIRTEL", network = "AIRTEL", planCategory = "CG", type = "CG", validity = "7 Days", duration = 7, dataVolume = "1GB"),
        PairgateDataPlanItem(id = "91", planId = "91", itemId = "91", name = "1.5GB (CG)", price = 1000.0, providerId = "AIRTEL", network = "AIRTEL", planCategory = "CG", type = "CG", validity = "7 Days", duration = 7, dataVolume = "1.5GB"),
        PairgateDataPlanItem(id = "92", planId = "92", itemId = "92", name = "2GB (CG)", price = 1500.0, providerId = "AIRTEL", network = "AIRTEL", planCategory = "CG", type = "CG", validity = "30 Days", duration = 30, dataVolume = "2GB"),
        PairgateDataPlanItem(id = "94", planId = "94", itemId = "94", name = "3.5GB (CG)", price = 1500.0, providerId = "AIRTEL", network = "AIRTEL", planCategory = "CG", type = "CG", validity = "7 Days", duration = 7, dataVolume = "3.5GB"),
        PairgateDataPlanItem(id = "93", planId = "93", itemId = "93", name = "3GB (CG)", price = 2000.0, providerId = "AIRTEL", network = "AIRTEL", planCategory = "CG", type = "CG", validity = "30 Days", duration = 30, dataVolume = "3GB"),
        PairgateDataPlanItem(id = "95", planId = "95", itemId = "95", name = "4GB (CG)", price = 2500.0, providerId = "AIRTEL", network = "AIRTEL", planCategory = "CG", type = "CG", validity = "30 Days", duration = 30, dataVolume = "4GB"),
        PairgateDataPlanItem(id = "96", planId = "96", itemId = "96", name = "6GB (CG)", price = 2500.0, providerId = "AIRTEL", network = "AIRTEL", planCategory = "CG", type = "CG", validity = "7 Days", duration = 7, dataVolume = "6GB"),
        PairgateDataPlanItem(id = "97", planId = "97", itemId = "97", name = "8GB (CG)", price = 3000.0, providerId = "AIRTEL", network = "AIRTEL", planCategory = "CG", type = "CG", validity = "30 Days", duration = 30, dataVolume = "8GB"),
        PairgateDataPlanItem(id = "98", planId = "98", itemId = "98", name = "10GB (CG)", price = 3000.0, providerId = "AIRTEL", network = "AIRTEL", planCategory = "CG", type = "CG", validity = "7 Days", duration = 7, dataVolume = "10GB"),
        PairgateDataPlanItem(id = "99", planId = "99", itemId = "99", name = "10GB (CG)", price = 4000.0, providerId = "AIRTEL", network = "AIRTEL", planCategory = "CG", type = "CG", validity = "30 Days", duration = 30, dataVolume = "10GB"),
        PairgateDataPlanItem(id = "100", planId = "100", itemId = "100", name = "13GB (CG)", price = 5000.0, providerId = "AIRTEL", network = "AIRTEL", planCategory = "CG", type = "CG", validity = "30 Days", duration = 30, dataVolume = "13GB"),
        PairgateDataPlanItem(id = "101", planId = "101", itemId = "101", name = "18GB (CG)", price = 5000.0, providerId = "AIRTEL", network = "AIRTEL", planCategory = "CG", type = "CG", validity = "7 Days", duration = 7, dataVolume = "18GB"),
        PairgateDataPlanItem(id = "102", planId = "102", itemId = "102", name = "18GB (CG)", price = 6000.0, providerId = "AIRTEL", network = "AIRTEL", planCategory = "CG", type = "CG", validity = "30 Days", duration = 30, dataVolume = "18GB"),
        PairgateDataPlanItem(id = "103", planId = "103", itemId = "103", name = "25GB (CG)", price = 8000.0, providerId = "AIRTEL", network = "AIRTEL", planCategory = "CG", type = "CG", validity = "30 Days", duration = 30, dataVolume = "25GB"),
        // ======================= AIRTEL (SME) =======================
        PairgateDataPlanItem(id = "airtel_sme_500mb", planId = "airtel_sme_500mb", itemId = "89", name = "500MB (SME)", price = 150.0, providerId = "AIRTEL", network = "AIRTEL", planCategory = "SME", type = "SME", validity = "30 Days", duration = 30, dataVolume = "500MB"),
        PairgateDataPlanItem(id = "airtel_sme_1gb", planId = "airtel_sme_1gb", itemId = "90", name = "1GB (SME)", price = 290.0, providerId = "AIRTEL", network = "AIRTEL", planCategory = "SME", type = "SME", validity = "30 Days", duration = 30, dataVolume = "1GB"),
        PairgateDataPlanItem(id = "airtel_sme_2gb", planId = "airtel_sme_2gb", itemId = "92", name = "2GB (SME)", price = 580.0, providerId = "AIRTEL", network = "AIRTEL", planCategory = "SME", type = "SME", validity = "30 Days", duration = 30, dataVolume = "2GB"),
        PairgateDataPlanItem(id = "airtel_sme_5gb", planId = "airtel_sme_5gb", itemId = "96", name = "5GB (SME)", price = 1450.0, providerId = "AIRTEL", network = "AIRTEL", planCategory = "SME", type = "SME", validity = "30 Days", duration = 30, dataVolume = "5GB"),
        PairgateDataPlanItem(id = "airtel_sme_10gb", planId = "airtel_sme_10gb", itemId = "99", name = "10GB (SME)", price = 2900.0, providerId = "AIRTEL", network = "AIRTEL", planCategory = "SME", type = "SME", validity = "30 Days", duration = 30, dataVolume = "10GB"),
        // ======================= AIRTEL (BROADBAND ROUTER) =======================
        PairgateDataPlanItem(id = "airtel_bb_40gb", planId = "203", itemId = "203", name = "40GB Router / MiFi Broadband", price = 10000.0, providerId = "AIRTEL", network = "AIRTEL", planCategory = "BROADBAND", type = "BROADBAND", validity = "30 Days", duration = 30, dataVolume = "40GB"),
        PairgateDataPlanItem(id = "airtel_bb_75gb", planId = "202", itemId = "202", name = "75GB Router Ultra Broadband", price = 15000.0, providerId = "AIRTEL", network = "AIRTEL", planCategory = "BROADBAND", type = "BROADBAND", validity = "30 Days", duration = 30, dataVolume = "75GB"),
        PairgateDataPlanItem(id = "airtel_bb_120gb", planId = "201", itemId = "201", name = "120GB Router Unlimited Broadband", price = 20000.0, providerId = "AIRTEL", network = "AIRTEL", planCategory = "BROADBAND", type = "BROADBAND", validity = "30 Days", duration = 30, dataVolume = "120GB"),
        PairgateDataPlanItem(id = "airtel_bb_200gb", planId = "213", itemId = "213", name = "200GB Router Mega Broadband", price = 30000.0, providerId = "AIRTEL", network = "AIRTEL", planCategory = "BROADBAND", type = "BROADBAND", validity = "30 Days", duration = 30, dataVolume = "200GB"),
        // ======================= AIRTEL (GIFTING) =======================
        PairgateDataPlanItem(id = "212", planId = "212", itemId = "212", name = "250MB Night Plan (12 - 5 Am)", price = 50.0, providerId = "AIRTEL", network = "AIRTEL", planCategory = "GIFTING", type = "GIFTING", validity = "1 Day", duration = 1, dataVolume = "250MB"),
        PairgateDataPlanItem(id = "198", planId = "198", itemId = "198", name = "75MB Daily Plan", price = 75.0, providerId = "AIRTEL", network = "AIRTEL", planCategory = "GIFTING", type = "GIFTING", validity = "1 Day", duration = 1, dataVolume = "75MB"),
        PairgateDataPlanItem(id = "189", planId = "189", itemId = "189", name = "110MB Plan", price = 100.0, providerId = "AIRTEL", network = "AIRTEL", planCategory = "GIFTING", type = "GIFTING", validity = "1 Day", duration = 1, dataVolume = "110MB"),
        PairgateDataPlanItem(id = "211", planId = "211", itemId = "211", name = "200MB Social Plan Platforms", price = 100.0, providerId = "AIRTEL", network = "AIRTEL", planCategory = "GIFTING", type = "GIFTING", validity = "2 Days", duration = 2, dataVolume = "200MB"),
        PairgateDataPlanItem(id = "210", planId = "210", itemId = "210", name = "1GB Social Plan Platforms", price = 300.0, providerId = "AIRTEL", network = "AIRTEL", planCategory = "GIFTING", type = "GIFTING", validity = "3 Days", duration = 3, dataVolume = "1GB"),
        PairgateDataPlanItem(id = "362", planId = "362", itemId = "362", name = "1GB Binge Plan", price = 500.0, providerId = "AIRTEL", network = "AIRTEL", planCategory = "GIFTING", type = "GIFTING", validity = "1 Day", duration = 1, dataVolume = "1GB"),
        PairgateDataPlanItem(id = "215", planId = "215", itemId = "215", name = "2GB Binge Plan + Youtube & Social Plan Data", price = 600.0, providerId = "AIRTEL", network = "AIRTEL", planCategory = "GIFTING", type = "GIFTING", validity = "2 Days", duration = 2, dataVolume = "2GB"),
        PairgateDataPlanItem(id = "197", planId = "197", itemId = "197", name = "3GB Binge Plan + Youtube & Social Plan Data", price = 750.0, providerId = "AIRTEL", network = "AIRTEL", planCategory = "GIFTING", type = "GIFTING", validity = "2 Days", duration = 2, dataVolume = "3GB"),
        PairgateDataPlanItem(id = "209", planId = "209", itemId = "209", name = "1GB Plan", price = 800.0, providerId = "AIRTEL", network = "AIRTEL", planCategory = "GIFTING", type = "GIFTING", validity = "7 Days", duration = 7, dataVolume = "1GB"),
        PairgateDataPlanItem(id = "196", planId = "196", itemId = "196", name = "1.5GB Weekly Plan + Youtube & Social Plan", price = 1000.0, providerId = "AIRTEL", network = "AIRTEL", planCategory = "GIFTING", type = "GIFTING", validity = "7 Days", duration = 7, dataVolume = "1.5GB"),
        PairgateDataPlanItem(id = "204", planId = "204", itemId = "204", name = "4GB Binge Plan + Youtube & Social Plan Data", price = 1000.0, providerId = "AIRTEL", network = "AIRTEL", planCategory = "GIFTING", type = "GIFTING", validity = "2 Days", duration = 2, dataVolume = "4GB"),
        PairgateDataPlanItem(id = "216", planId = "216", itemId = "216", name = "6GB Binge Plan + Youtube & Social Plan Data", price = 1500.0, providerId = "AIRTEL", network = "AIRTEL", planCategory = "GIFTING", type = "GIFTING", validity = "2 Days", duration = 2, dataVolume = "6GB"),
        PairgateDataPlanItem(id = "195", planId = "195", itemId = "195", name = "2GB Plan", price = 1500.0, providerId = "AIRTEL", network = "AIRTEL", planCategory = "GIFTING", type = "GIFTING", validity = "30 Days", duration = 30, dataVolume = "2GB"),
        PairgateDataPlanItem(id = "208", planId = "208", itemId = "208", name = "4GB Weekly Plan + Youtube & Social Plan", price = 1500.0, providerId = "AIRTEL", network = "AIRTEL", planCategory = "GIFTING", type = "GIFTING", validity = "7 Days", duration = 7, dataVolume = "4GB"),
        PairgateDataPlanItem(id = "363", planId = "363", itemId = "363", name = "6GB Weekly Plan + Youtube & Social Plan Data", price = 2000.0, providerId = "AIRTEL", network = "AIRTEL", planCategory = "GIFTING", type = "GIFTING", validity = "7 Days", duration = 7, dataVolume = "6GB"),
        PairgateDataPlanItem(id = "207", planId = "207", itemId = "207", name = "3GB Monthly Plan", price = 2000.0, providerId = "AIRTEL", network = "AIRTEL", planCategory = "GIFTING", type = "GIFTING", validity = "30 Days", duration = 30, dataVolume = "3GB"),
        PairgateDataPlanItem(id = "200", planId = "200", itemId = "200", name = "8GB Weekly Plan + Youtube & Social Plan", price = 2500.0, providerId = "AIRTEL", network = "AIRTEL", planCategory = "GIFTING", type = "GIFTING", validity = "7 Days", duration = 7, dataVolume = "8GB"),
        PairgateDataPlanItem(id = "194", planId = "194", itemId = "194", name = "4GB Monthly Plan + Youtube & Social Plan", price = 2500.0, providerId = "AIRTEL", network = "AIRTEL", planCategory = "GIFTING", type = "GIFTING", validity = "30 Days", duration = 30, dataVolume = "4GB"),
        PairgateDataPlanItem(id = "193", planId = "193", itemId = "193", name = "10GB Weekly Plan + Youtube & Social Plan", price = 3000.0, providerId = "AIRTEL", network = "AIRTEL", planCategory = "GIFTING", type = "GIFTING", validity = "7 Days", duration = 7, dataVolume = "10GB"),
        PairgateDataPlanItem(id = "192", planId = "192", itemId = "192", name = "8GB Monthly Plan + Youtube & Social Plan", price = 3000.0, providerId = "AIRTEL", network = "AIRTEL", planCategory = "GIFTING", type = "GIFTING", validity = "30 Days", duration = 30, dataVolume = "8GB"),
        PairgateDataPlanItem(id = "214", planId = "214", itemId = "214", name = "10GB Monthly Plan + Youtube & Social Plan", price = 4000.0, providerId = "AIRTEL", network = "AIRTEL", planCategory = "GIFTING", type = "GIFTING", validity = "30 Days", duration = 30, dataVolume = "10GB"),
        PairgateDataPlanItem(id = "206", planId = "206", itemId = "206", name = "20GB Weekly Plan + Youtube & Social Plan", price = 5000.0, providerId = "AIRTEL", network = "AIRTEL", planCategory = "GIFTING", type = "GIFTING", validity = "7 Days", duration = 7, dataVolume = "20GB"),
        PairgateDataPlanItem(id = "191", planId = "191", itemId = "191", name = "13GB Monthly Plan + Youtube & Social Plan", price = 5000.0, providerId = "AIRTEL", network = "AIRTEL", planCategory = "GIFTING", type = "GIFTING", validity = "30 Days", duration = 30, dataVolume = "13GB"),
        PairgateDataPlanItem(id = "205", planId = "205", itemId = "205", name = "18GB Monthly Plan + Youtube & Social Plan", price = 6000.0, providerId = "AIRTEL", network = "AIRTEL", planCategory = "GIFTING", type = "GIFTING", validity = "30 Days", duration = 30, dataVolume = "18GB"),
        PairgateDataPlanItem(id = "199", planId = "199", itemId = "199", name = "25GB Monthly Plan + Youtube & Social Plan", price = 8000.0, providerId = "AIRTEL", network = "AIRTEL", planCategory = "GIFTING", type = "GIFTING", validity = "30 Days", duration = 30, dataVolume = "25GB"),
        PairgateDataPlanItem(id = "203", planId = "203", itemId = "203", name = "35GB Monthly Plan + Youtube & Social Plan", price = 10000.0, providerId = "AIRTEL", network = "AIRTEL", planCategory = "GIFTING", type = "GIFTING", validity = "30 Days", duration = 30, dataVolume = "35GB"),
        PairgateDataPlanItem(id = "202", planId = "202", itemId = "202", name = "60GB Monthly Plan + Youtube & Social Plan", price = 15000.0, providerId = "AIRTEL", network = "AIRTEL", planCategory = "GIFTING", type = "GIFTING", validity = "30 Days", duration = 30, dataVolume = "60GB"),
        PairgateDataPlanItem(id = "201", planId = "201", itemId = "201", name = "100GB Monthly Plan + Youtube & Social Plan", price = 20000.0, providerId = "AIRTEL", network = "AIRTEL", planCategory = "GIFTING", type = "GIFTING", validity = "30 Days", duration = 30, dataVolume = "100GB"),
        PairgateDataPlanItem(id = "213", planId = "213", itemId = "213", name = "160GB Monthly Plan", price = 30000.0, providerId = "AIRTEL", network = "AIRTEL", planCategory = "GIFTING", type = "GIFTING", validity = "30 Days", duration = 30, dataVolume = "160GB"),
        PairgateDataPlanItem(id = "190", planId = "190", itemId = "190", name = "210GB Data", price = 40000.0, providerId = "AIRTEL", network = "AIRTEL", planCategory = "GIFTING", type = "GIFTING", validity = "30 Days", duration = 30, dataVolume = "210GB"),
        // ======================= GLO (CG) =======================
        PairgateDataPlanItem(id = "59", planId = "59", itemId = "59", name = "200MB (CG)", price = 92.0, providerId = "GLO", network = "GLO", planCategory = "CG", type = "CG", validity = "14 Days", duration = 14, dataVolume = "200MB"),
        PairgateDataPlanItem(id = "60", planId = "60", itemId = "60", name = "500MB (CG)", price = 215.0, providerId = "GLO", network = "GLO", planCategory = "CG", type = "CG", validity = "30 Days", duration = 30, dataVolume = "500MB"),
        PairgateDataPlanItem(id = "61", planId = "61", itemId = "61", name = "1GB (CG)", price = 375.0, providerId = "GLO", network = "GLO", planCategory = "CG", type = "CG", validity = "3 Days", duration = 3, dataVolume = "1GB"),
        PairgateDataPlanItem(id = "62", planId = "62", itemId = "62", name = "1GB CG", price = 395.0, providerId = "GLO", network = "GLO", planCategory = "CG", type = "CG", validity = "7 Days", duration = 7, dataVolume = "1GB"),
        PairgateDataPlanItem(id = "63", planId = "63", itemId = "63", name = "1GB CG", price = 445.0, providerId = "GLO", network = "GLO", planCategory = "CG", type = "CG", validity = "30 Days", duration = 30, dataVolume = "1GB"),
        PairgateDataPlanItem(id = "64", planId = "64", itemId = "64", name = "2GB CG", price = 890.0, providerId = "GLO", network = "GLO", planCategory = "CG", type = "CG", validity = "30 Days", duration = 30, dataVolume = "2GB"),
        PairgateDataPlanItem(id = "65", planId = "65", itemId = "65", name = "3GB CG", price = 1125.0, providerId = "GLO", network = "GLO", planCategory = "CG", type = "CG", validity = "3 Days", duration = 3, dataVolume = "3GB"),
        PairgateDataPlanItem(id = "66", planId = "66", itemId = "66", name = "3GB CG", price = 1185.0, providerId = "GLO", network = "GLO", planCategory = "CG", type = "CG", validity = "7 Days", duration = 7, dataVolume = "3GB"),
        PairgateDataPlanItem(id = "67", planId = "67", itemId = "67", name = "3GB CG", price = 1335.0, providerId = "GLO", network = "GLO", planCategory = "CG", type = "CG", validity = "30 Days", duration = 30, dataVolume = "3GB"),
        PairgateDataPlanItem(id = "68", planId = "68", itemId = "68", name = "5GB CG", price = 1875.0, providerId = "GLO", network = "GLO", planCategory = "CG", type = "CG", validity = "3 Days", duration = 3, dataVolume = "5GB"),
        PairgateDataPlanItem(id = "69", planId = "69", itemId = "69", name = "5GB CG", price = 1975.0, providerId = "GLO", network = "GLO", planCategory = "CG", type = "CG", validity = "7 Days", duration = 7, dataVolume = "5GB"),
        PairgateDataPlanItem(id = "70", planId = "70", itemId = "70", name = "5GB CG", price = 2225.0, providerId = "GLO", network = "GLO", planCategory = "CG", type = "CG", validity = "30 Days", duration = 30, dataVolume = "5GB"),
        PairgateDataPlanItem(id = "71", planId = "71", itemId = "71", name = "10GB CG", price = 4450.0, providerId = "GLO", network = "GLO", planCategory = "CG", type = "CG", validity = "30 Days", duration = 30, dataVolume = "10GB"),
        // ======================= GLO (SME) =======================
        PairgateDataPlanItem(id = "glo_sme_500mb", planId = "glo_sme_500mb", itemId = "60", name = "500MB (SME)", price = 140.0, providerId = "GLO", network = "GLO", planCategory = "SME", type = "SME", validity = "30 Days", duration = 30, dataVolume = "500MB"),
        PairgateDataPlanItem(id = "glo_sme_1gb", planId = "glo_sme_1gb", itemId = "63", name = "1GB (SME)", price = 280.0, providerId = "GLO", network = "GLO", planCategory = "SME", type = "SME", validity = "30 Days", duration = 30, dataVolume = "1GB"),
        PairgateDataPlanItem(id = "glo_sme_2gb", planId = "glo_sme_2gb", itemId = "64", name = "2GB (SME)", price = 560.0, providerId = "GLO", network = "GLO", planCategory = "SME", type = "SME", validity = "30 Days", duration = 30, dataVolume = "2GB"),
        PairgateDataPlanItem(id = "glo_sme_5gb", planId = "glo_sme_5gb", itemId = "70", name = "5GB (SME)", price = 1400.0, providerId = "GLO", network = "GLO", planCategory = "SME", type = "SME", validity = "30 Days", duration = 30, dataVolume = "5GB"),
        PairgateDataPlanItem(id = "glo_sme_10gb", planId = "glo_sme_10gb", itemId = "71", name = "10GB (SME)", price = 2800.0, providerId = "GLO", network = "GLO", planCategory = "SME", type = "SME", validity = "30 Days", duration = 30, dataVolume = "10GB"),
        // ======================= GLO (BROADBAND ROUTER) =======================
        PairgateDataPlanItem(id = "glo_bb_42gb", planId = "258", itemId = "258", name = "42GB Router / MiFi Broadband", price = 10000.0, providerId = "GLO", network = "GLO", planCategory = "BROADBAND", type = "BROADBAND", validity = "30 Days", duration = 30, dataVolume = "42GB"),
        PairgateDataPlanItem(id = "glo_bb_107gb", planId = "254", itemId = "254", name = "107GB Router Mega Broadband", price = 20000.0, providerId = "GLO", network = "GLO", planCategory = "BROADBAND", type = "BROADBAND", validity = "30 Days", duration = 30, dataVolume = "107GB"),
        PairgateDataPlanItem(id = "glo_bb_220gb", planId = "234", itemId = "234", name = "220GB Router Ultra Broadband", price = 40000.0, providerId = "GLO", network = "GLO", planCategory = "BROADBAND", type = "BROADBAND", validity = "30 Days", duration = 30, dataVolume = "220GB"),
        PairgateDataPlanItem(id = "glo_bb_310gb", planId = "240", itemId = "240", name = "310GB Router Mega Broadband (60 Days)", price = 50000.0, providerId = "GLO", network = "GLO", planCategory = "BROADBAND", type = "BROADBAND", validity = "60 Days", duration = 60, dataVolume = "310GB"),
        PairgateDataPlanItem(id = "glo_bb_1000gb", planId = "236", itemId = "236", name = "1000GB (1TB) Router 1-Year Broadband", price = 150000.0, providerId = "GLO", network = "GLO", planCategory = "BROADBAND", type = "BROADBAND", validity = "365 Days", duration = 365, dataVolume = "1000GB"),
        // ======================= GLO (GIFTING) =======================
        PairgateDataPlanItem(id = "220", planId = "220", itemId = "220", name = "135MB Social Bundle", price = 50.0, providerId = "GLO", network = "GLO", planCategory = "GIFTING", type = "GIFTING", validity = "3 Days", duration = 3, dataVolume = "135MB"),
        PairgateDataPlanItem(id = "255", planId = "255", itemId = "255", name = "45MB", price = 50.0, providerId = "GLO", network = "GLO", planCategory = "GIFTING", type = "GIFTING", validity = "1 Day", duration = 1, dataVolume = "45MB"),
        PairgateDataPlanItem(id = "222", planId = "222", itemId = "222", name = "350MB Night Plan", price = 60.0, providerId = "GLO", network = "GLO", planCategory = "GIFTING", type = "GIFTING", validity = "1 Day", duration = 1, dataVolume = "350MB"),
        PairgateDataPlanItem(id = "219", planId = "219", itemId = "219", name = "335MB Social Bundle", price = 100.0, providerId = "GLO", network = "GLO", planCategory = "GIFTING", type = "GIFTING", validity = "7 Days", duration = 7, dataVolume = "335MB"),
        PairgateDataPlanItem(id = "268", planId = "268", itemId = "268", name = "125MB (120MB + 5MB Night)", price = 100.0, providerId = "GLO", network = "GLO", planCategory = "GIFTING", type = "GIFTING", validity = "1 Day", duration = 1, dataVolume = "125MB"),
        PairgateDataPlanItem(id = "228", planId = "228", itemId = "228", name = "245MB Campus Booster (240MB + 5MB Night)", price = 100.0, providerId = "GLO", network = "GLO", planCategory = "GIFTING", type = "GIFTING", validity = "1 Day", duration = 1, dataVolume = "245MB"),
        PairgateDataPlanItem(id = "256", planId = "256", itemId = "256", name = "300MB (myg Social Bundle)", price = 100.0, providerId = "GLO", network = "GLO", planCategory = "GIFTING", type = "GIFTING", validity = "1 Day", duration = 1, dataVolume = "300MB"),
        PairgateDataPlanItem(id = "221", planId = "221", itemId = "221", name = "750MB Night Plan", price = 120.0, providerId = "GLO", network = "GLO", planCategory = "GIFTING", type = "GIFTING", validity = "1 Day", duration = 1, dataVolume = "750MB"),
        PairgateDataPlanItem(id = "267", planId = "267", itemId = "267", name = "275MB (250MB + 25MB Night)", price = 200.0, providerId = "GLO", network = "GLO", planCategory = "GIFTING", type = "GIFTING", validity = "2 Days", duration = 2, dataVolume = "275MB"),
        PairgateDataPlanItem(id = "227", planId = "227", itemId = "227", name = "525MB Campus Booster (500MB + 25MB Night)", price = 200.0, providerId = "GLO", network = "GLO", planCategory = "GIFTING", type = "GIFTING", validity = "2 Days", duration = 2, dataVolume = "525MB"),
        PairgateDataPlanItem(id = "252", planId = "252", itemId = "252", name = "875MB Weekend", price = 200.0, providerId = "GLO", network = "GLO", planCategory = "GIFTING", type = "GIFTING", validity = "2 Days", duration = 2, dataVolume = "875MB"),
        PairgateDataPlanItem(id = "218", planId = "218", itemId = "218", name = "1.1GB Social Bundle Nights", price = 300.0, providerId = "GLO", network = "GLO", planCategory = "GIFTING", type = "GIFTING", validity = "10 Days", duration = 10, dataVolume = "1.1GB"),
        PairgateDataPlanItem(id = "253", planId = "253", itemId = "253", name = "1GB (my G Social Bundle)", price = 300.0, providerId = "GLO", network = "GLO", planCategory = "GIFTING", type = "GIFTING", validity = "3 Days", duration = 3, dataVolume = "1GB"),
        PairgateDataPlanItem(id = "243", planId = "243", itemId = "243", name = "2.5GB", price = 500.0, providerId = "GLO", network = "GLO", planCategory = "GIFTING", type = "GIFTING", validity = "2 Days", duration = 2, dataVolume = "2.5GB"),
        PairgateDataPlanItem(id = "361", planId = "361", itemId = "361", name = "Collabo Package (450MB Data + 10 Minutes Voice)", price = 500.0, providerId = "GLO", network = "GLO", planCategory = "GIFTING", type = "GIFTING", validity = "7 Days", duration = 7, dataVolume = "450MB"),
        PairgateDataPlanItem(id = "244", planId = "244", itemId = "244", name = "2GB Special", price = 500.0, providerId = "GLO", network = "GLO", planCategory = "GIFTING", type = "GIFTING", validity = "1 Day", duration = 1, dataVolume = "2GB"),
        PairgateDataPlanItem(id = "251", planId = "251", itemId = "251", name = "1.5GB (my G Social Bundle)", price = 500.0, providerId = "GLO", network = "GLO", planCategory = "GIFTING", type = "GIFTING", validity = "7 Days", duration = 7, dataVolume = "1.5GB"),
        PairgateDataPlanItem(id = "266", planId = "266", itemId = "266", name = "1.55GB (550MB + 1GB Night)", price = 500.0, providerId = "GLO", network = "GLO", planCategory = "GIFTING", type = "GIFTING", validity = "7 Days", duration = 7, dataVolume = "1.55GB"),
        PairgateDataPlanItem(id = "217", planId = "217", itemId = "217", name = "1.8GB Social Bundle Nights", price = 500.0, providerId = "GLO", network = "GLO", planCategory = "GIFTING", type = "GIFTING", validity = "15 Days", duration = 15, dataVolume = "1.8GB"),
        PairgateDataPlanItem(id = "226", planId = "226", itemId = "226", name = "2.1GB Campus Booster (1.1GB + 1GB Night)", price = 500.0, providerId = "GLO", network = "GLO", planCategory = "GIFTING", type = "GIFTING", validity = "7 Days", duration = 7, dataVolume = "2.1GB"),
        PairgateDataPlanItem(id = "238", planId = "238", itemId = "238", name = "3.55GB Special Plan", price = 600.0, providerId = "GLO", network = "GLO", planCategory = "GIFTING", type = "GIFTING", validity = "2 Days", duration = 2, dataVolume = "3.55GB"),
        PairgateDataPlanItem(id = "245", planId = "245", itemId = "245", name = "1.1GB", price = 750.0, providerId = "GLO", network = "GLO", planCategory = "GIFTING", type = "GIFTING", validity = "14 Days", duration = 14, dataVolume = "1.1GB"),
        PairgateDataPlanItem(id = "237", planId = "237", itemId = "237", name = "5.1GB Special Plan", price = 1000.0, providerId = "GLO", network = "GLO", planCategory = "GIFTING", type = "GIFTING", validity = "2 Days", duration = 2, dataVolume = "5.1GB"),
        PairgateDataPlanItem(id = "360", planId = "360", itemId = "360", name = "Collabo Package (1.25GB Data + 20 Minutes Voice)", price = 1000.0, providerId = "GLO", network = "GLO", planCategory = "GIFTING", type = "GIFTING", validity = "7 Days", duration = 7, dataVolume = "1.25GB"),
        PairgateDataPlanItem(id = "265", planId = "265", itemId = "265", name = "2.6GB", price = 1000.0, providerId = "GLO", network = "GLO", planCategory = "GIFTING", type = "GIFTING", validity = "30 Days", duration = 30, dataVolume = "2.6GB"),
        PairgateDataPlanItem(id = "225", planId = "225", itemId = "225", name = "4.2GB Campus Booster (2.2GB + 2GB Night)", price = 1000.0, providerId = "GLO", network = "GLO", planCategory = "GIFTING", type = "GIFTING", validity = "30 Days", duration = 30, dataVolume = "4.2GB"),
        PairgateDataPlanItem(id = "249", planId = "249", itemId = "249", name = "3.5GB (my G Social Bundle)", price = 1000.0, providerId = "GLO", network = "GLO", planCategory = "GIFTING", type = "GIFTING", validity = "30 Days", duration = 30, dataVolume = "3.5GB"),
        PairgateDataPlanItem(id = "246", planId = "246", itemId = "246", name = "3.7GB (1.7GB + 2GB Night)", price = 1000.0, providerId = "GLO", network = "GLO", planCategory = "GIFTING", type = "GIFTING", validity = "7 Days", duration = 7, dataVolume = "3.7GB"),
        PairgateDataPlanItem(id = "248", planId = "248", itemId = "248", name = "5.2GB (2.2GB + 3GB Night)", price = 1500.0, providerId = "GLO", network = "GLO", planCategory = "GIFTING", type = "GIFTING", validity = "30 Days", duration = 30, dataVolume = "5.2GB"),
        PairgateDataPlanItem(id = "250", planId = "250", itemId = "250", name = "6GB Special (4GB + 2GB Night)", price = 1500.0, providerId = "GLO", network = "GLO", planCategory = "GIFTING", type = "GIFTING", validity = "7 Days", duration = 7, dataVolume = "6GB"),
        PairgateDataPlanItem(id = "359", planId = "359", itemId = "359", name = "Collabo Package (1.85GB Data + 30 Minutes Voice)", price = 1500.0, providerId = "GLO", network = "GLO", planCategory = "GIFTING", type = "GIFTING", validity = "14 Days", duration = 14, dataVolume = "1.85GB"),
        PairgateDataPlanItem(id = "247", planId = "247", itemId = "247", name = "9GB (6.5GB + 2.5GB Night)", price = 2000.0, providerId = "GLO", network = "GLO", planCategory = "GIFTING", type = "GIFTING", validity = "7 Days", duration = 7, dataVolume = "9GB"),
        PairgateDataPlanItem(id = "224", planId = "224", itemId = "224", name = "10GB Campus Booster (6.5GB + 3.5GB Night)", price = 2000.0, providerId = "GLO", network = "GLO", planCategory = "GIFTING", type = "GIFTING", validity = "30 Days", duration = 30, dataVolume = "10GB"),
        PairgateDataPlanItem(id = "232", planId = "232", itemId = "232", name = "Always-on N2000", price = 2000.0, providerId = "GLO", network = "GLO", planCategory = "GIFTING", type = "GIFTING", validity = "15 Days", duration = 15, dataVolume = ""),
        PairgateDataPlanItem(id = "358", planId = "358", itemId = "358", name = "Collabo Package (3GB Data + 40 Minutes Voice)", price = 2000.0, providerId = "GLO", network = "GLO", planCategory = "GIFTING", type = "GIFTING", validity = "30 Days", duration = 30, dataVolume = "3GB"),
        PairgateDataPlanItem(id = "264", planId = "264", itemId = "264", name = "6.25GB (3.25GB + 3GB Night)", price = 2000.0, providerId = "GLO", network = "GLO", planCategory = "GIFTING", type = "GIFTING", validity = "30 Days", duration = 30, dataVolume = "6.25GB"),
        PairgateDataPlanItem(id = "357", planId = "357", itemId = "357", name = "Collabo Package (4GB Data + 50 Minutes Voice)", price = 2500.0, providerId = "GLO", network = "GLO", planCategory = "GIFTING", type = "GIFTING", validity = "30 Days", duration = 30, dataVolume = "4GB"),
        PairgateDataPlanItem(id = "263", planId = "263", itemId = "263", name = "7.25GB (4.25GB + 3GB Night)", price = 2500.0, providerId = "GLO", network = "GLO", planCategory = "GIFTING", type = "GIFTING", validity = "30 Days", duration = 30, dataVolume = "7.25GB"),
        PairgateDataPlanItem(id = "356", planId = "356", itemId = "356", name = "Collabo Package (7.5GB Data + 60 Minutes Voice)", price = 3000.0, providerId = "GLO", network = "GLO", planCategory = "GIFTING", type = "GIFTING", validity = "30 Days", duration = 30, dataVolume = "7.5GB"),
        PairgateDataPlanItem(id = "262", planId = "262", itemId = "262", name = "10.5GB (8.5GB +2GB Night)", price = 3000.0, providerId = "GLO", network = "GLO", planCategory = "GIFTING", type = "GIFTING", validity = "30 Days", duration = 30, dataVolume = "10.5GB"),
        PairgateDataPlanItem(id = "231", planId = "231", itemId = "231", name = "Always-on N3500", price = 3500.0, providerId = "GLO", network = "GLO", planCategory = "GIFTING", type = "GIFTING", validity = "30 Days", duration = 30, dataVolume = ""),
        PairgateDataPlanItem(id = "261", planId = "261", itemId = "261", name = "12.5GB", price = 4000.0, providerId = "GLO", network = "GLO", planCategory = "GIFTING", type = "GIFTING", validity = "30 Days", duration = 30, dataVolume = "12.5GB"),
        PairgateDataPlanItem(id = "223", planId = "223", itemId = "223", name = "32GB For 30 Days Campus Booster (29GB + 3GB Night)", price = 5000.0, providerId = "GLO", network = "GLO", planCategory = "GIFTING", type = "GIFTING", validity = "30 Days", duration = 30, dataVolume = "32GB"),
        PairgateDataPlanItem(id = "355", planId = "355", itemId = "355", name = "Collabo Package (12.5GB Data + 100 Minutes Voice)", price = 5000.0, providerId = "GLO", network = "GLO", planCategory = "GIFTING", type = "GIFTING", validity = "30 Days", duration = 30, dataVolume = "12.5GB"),
        PairgateDataPlanItem(id = "260", planId = "260", itemId = "260", name = "17GB (14.5GB + 2.5GB Night)", price = 5000.0, providerId = "GLO", network = "GLO", planCategory = "GIFTING", type = "GIFTING", validity = "30 Days", duration = 30, dataVolume = "17GB"),
        PairgateDataPlanItem(id = "230", planId = "230", itemId = "230", name = "Always-on N5000", price = 5000.0, providerId = "GLO", network = "GLO", planCategory = "GIFTING", type = "GIFTING", validity = "30 Days", duration = 30, dataVolume = ""),
        PairgateDataPlanItem(id = "241", planId = "241", itemId = "241", name = "20.5GB", price = 6000.0, providerId = "GLO", network = "GLO", planCategory = "GIFTING", type = "GIFTING", validity = "30 Days", duration = 30, dataVolume = "20.5GB"),
        PairgateDataPlanItem(id = "229", planId = "229", itemId = "229", name = "Alwavs-on N7000", price = 7000.0, providerId = "GLO", network = "GLO", planCategory = "GIFTING", type = "GIFTING", validity = "30 Days", duration = 30, dataVolume = ""),
        PairgateDataPlanItem(id = "259", planId = "259", itemId = "259", name = "28GB", price = 8000.0, providerId = "GLO", network = "GLO", planCategory = "GIFTING", type = "GIFTING", validity = "30 Days", duration = 30, dataVolume = "28GB"),
        PairgateDataPlanItem(id = "258", planId = "258", itemId = "258", name = "42GB (38GB + 4GB Night)", price = 10000.0, providerId = "GLO", network = "GLO", planCategory = "GIFTING", type = "GIFTING", validity = "30 Days", duration = 30, dataVolume = "42GB"),
        PairgateDataPlanItem(id = "257", planId = "257", itemId = "257", name = "64GB", price = 15000.0, providerId = "GLO", network = "GLO", planCategory = "GIFTING", type = "GIFTING", validity = "30 Days", duration = 30, dataVolume = "64GB"),
        PairgateDataPlanItem(id = "254", planId = "254", itemId = "254", name = "107GB", price = 20000.0, providerId = "GLO", network = "GLO", planCategory = "GIFTING", type = "GIFTING", validity = "30 Days", duration = 30, dataVolume = "107GB"),
        PairgateDataPlanItem(id = "235", planId = "235", itemId = "235", name = "135GB Mega Plan", price = 25000.0, providerId = "GLO", network = "GLO", planCategory = "GIFTING", type = "GIFTING", validity = "30 Days", duration = 30, dataVolume = "135GB"),
        PairgateDataPlanItem(id = "242", planId = "242", itemId = "242", name = "165GB Mega", price = 30000.0, providerId = "GLO", network = "GLO", planCategory = "GIFTING", type = "GIFTING", validity = "30 Days", duration = 30, dataVolume = "165GB"),
        PairgateDataPlanItem(id = "234", planId = "234", itemId = "234", name = "220GB Mega Plan", price = 40000.0, providerId = "GLO", network = "GLO", planCategory = "GIFTING", type = "GIFTING", validity = "30 Days", duration = 30, dataVolume = "220GB"),
        PairgateDataPlanItem(id = "240", planId = "240", itemId = "240", name = "310GB Mega Plan", price = 50000.0, providerId = "GLO", network = "GLO", planCategory = "GIFTING", type = "GIFTING", validity = "60 Days", duration = 60, dataVolume = "310GB"),
        PairgateDataPlanItem(id = "239", planId = "239", itemId = "239", name = "355GB Mega Plan", price = 60000.0, providerId = "GLO", network = "GLO", planCategory = "GIFTING", type = "GIFTING", validity = "60 Days", duration = 60, dataVolume = "355GB"),
        PairgateDataPlanItem(id = "233", planId = "233", itemId = "233", name = "475GB Mega Plan", price = 75000.0, providerId = "GLO", network = "GLO", planCategory = "GIFTING", type = "GIFTING", validity = "90 Days", duration = 90, dataVolume = "475GB"),
        PairgateDataPlanItem(id = "236", planId = "236", itemId = "236", name = "1000GB Mega Plan", price = 150000.0, providerId = "GLO", network = "GLO", planCategory = "GIFTING", type = "GIFTING", validity = "365 Days", duration = 365, dataVolume = "1000GB"),
        // ======================= 9MOBILE (SME) =======================
        PairgateDataPlanItem(id = "128", planId = "128", itemId = "128", name = "500MB (SME)", price = 260.0, providerId = "9MOBILE", network = "9MOBILE", planCategory = "SME", type = "SME", validity = "30 Days", duration = 30, dataVolume = "500MB"),
        PairgateDataPlanItem(id = "129", planId = "129", itemId = "129", name = "1GB (SME)", price = 515.0, providerId = "9MOBILE", network = "9MOBILE", planCategory = "SME", type = "SME", validity = "30 Days", duration = 30, dataVolume = "1GB"),
        PairgateDataPlanItem(id = "130", planId = "130", itemId = "130", name = "1.5GB (SME)", price = 750.0, providerId = "9MOBILE", network = "9MOBILE", planCategory = "SME", type = "SME", validity = "30 Days", duration = 30, dataVolume = "1.5GB"),
        PairgateDataPlanItem(id = "131", planId = "131", itemId = "131", name = "2GB (SME)", price = 1030.0, providerId = "9MOBILE", network = "9MOBILE", planCategory = "SME", type = "SME", validity = "30 Days", duration = 30, dataVolume = "2GB"),
        PairgateDataPlanItem(id = "132", planId = "132", itemId = "132", name = "3GB (SME)", price = 1545.0, providerId = "9MOBILE", network = "9MOBILE", planCategory = "SME", type = "SME", validity = "30 Days", duration = 30, dataVolume = "3GB"),
        PairgateDataPlanItem(id = "133", planId = "133", itemId = "133", name = "4GB (SME)", price = 2060.0, providerId = "9MOBILE", network = "9MOBILE", planCategory = "SME", type = "SME", validity = "30 Days", duration = 30, dataVolume = "4GB"),
        PairgateDataPlanItem(id = "134", planId = "134", itemId = "134", name = "4.5GB (SME)", price = 2180.0, providerId = "9MOBILE", network = "9MOBILE", planCategory = "SME", type = "SME", validity = "30 Days", duration = 30, dataVolume = "4.5GB"),
        PairgateDataPlanItem(id = "135", planId = "135", itemId = "135", name = "5GB (SME)", price = 2575.0, providerId = "9MOBILE", network = "9MOBILE", planCategory = "SME", type = "SME", validity = "30 Days", duration = 30, dataVolume = "5GB"),
        PairgateDataPlanItem(id = "136", planId = "136", itemId = "136", name = "7.5GB (SME)", price = 3700.0, providerId = "9MOBILE", network = "9MOBILE", planCategory = "SME", type = "SME", validity = "30 Days", duration = 30, dataVolume = "7.5GB"),
        PairgateDataPlanItem(id = "137", planId = "137", itemId = "137", name = "10GB (SME)", price = 5150.0, providerId = "9MOBILE", network = "9MOBILE", planCategory = "SME", type = "SME", validity = "30 Days", duration = 30, dataVolume = "10GB"),
        PairgateDataPlanItem(id = "138", planId = "138", itemId = "138", name = "11GB (SME)", price = 5665.0, providerId = "9MOBILE", network = "9MOBILE", planCategory = "SME", type = "SME", validity = "30 Days", duration = 30, dataVolume = "11GB"),
        PairgateDataPlanItem(id = "139", planId = "139", itemId = "139", name = "15GB (SME)", price = 7725.0, providerId = "9MOBILE", network = "9MOBILE", planCategory = "SME", type = "SME", validity = "30 Days", duration = 30, dataVolume = "15GB"),
        PairgateDataPlanItem(id = "140", planId = "140", itemId = "140", name = "20GB (SME)", price = 10300.0, providerId = "9MOBILE", network = "9MOBILE", planCategory = "SME", type = "SME", validity = "30 Days", duration = 30, dataVolume = "20GB"),
        // ======================= 9MOBILE (GIFTING) =======================
        PairgateDataPlanItem(id = "188", planId = "188", itemId = "188", name = "40MB", price = 50.0, providerId = "9MOBILE", network = "9MOBILE", planCategory = "GIFTING", type = "GIFTING", validity = "1 Day", duration = 1, dataVolume = "40MB"),
        PairgateDataPlanItem(id = "187", planId = "187", itemId = "187", name = "83MB + 50MB Social", price = 100.0, providerId = "9MOBILE", network = "9MOBILE", planCategory = "GIFTING", type = "GIFTING", validity = "1 Day", duration = 1, dataVolume = "83MB"),
        PairgateDataPlanItem(id = "178", planId = "178", itemId = "178", name = "150MB + 100MB Night Data", price = 150.0, providerId = "9MOBILE", network = "9MOBILE", planCategory = "GIFTING", type = "GIFTING", validity = "1 Day", duration = 1, dataVolume = "150MB"),
        PairgateDataPlanItem(id = "177", planId = "177", itemId = "177", name = "200MB Social Plan", price = 200.0, providerId = "9MOBILE", network = "9MOBILE", planCategory = "GIFTING", type = "GIFTING", validity = "7 Days", duration = 7, dataVolume = "200MB"),
        PairgateDataPlanItem(id = "391", planId = "391", itemId = "391", name = "250MB", price = 200.0, providerId = "9MOBILE", network = "9MOBILE", planCategory = "GIFTING", type = "GIFTING", validity = "1 Day", duration = 1, dataVolume = "250MB"),
        PairgateDataPlanItem(id = "186", planId = "186", itemId = "186", name = "650MB + 100MB Socials", price = 500.0, providerId = "9MOBILE", network = "9MOBILE", planCategory = "GIFTING", type = "GIFTING", validity = "7 Days", duration = 7, dataVolume = "650MB"),
        PairgateDataPlanItem(id = "185", planId = "185", itemId = "185", name = "2GB", price = 1000.0, providerId = "9MOBILE", network = "9MOBILE", planCategory = "GIFTING", type = "GIFTING", validity = "30 Days", duration = 30, dataVolume = "2GB"),
        PairgateDataPlanItem(id = "181", planId = "181", itemId = "181", name = "2.3GB", price = 1200.0, providerId = "9MOBILE", network = "9MOBILE", planCategory = "GIFTING", type = "GIFTING", validity = "30 Days", duration = 30, dataVolume = "2.3GB"),
        PairgateDataPlanItem(id = "396", planId = "396", itemId = "396", name = "3.2GB", price = 1500.0, providerId = "9MOBILE", network = "9MOBILE", planCategory = "GIFTING", type = "GIFTING", validity = "30 Days", duration = 30, dataVolume = "3.2GB"),
        PairgateDataPlanItem(id = "397", planId = "397", itemId = "397", name = "3.4GB", price = 1500.0, providerId = "9MOBILE", network = "9MOBILE", planCategory = "GIFTING", type = "GIFTING", validity = "7 Days", duration = 7, dataVolume = "3.4GB"),
        PairgateDataPlanItem(id = "180", planId = "180", itemId = "180", name = "4.5GB", price = 2000.0, providerId = "9MOBILE", network = "9MOBILE", planCategory = "GIFTING", type = "GIFTING", validity = "30 Days", duration = 30, dataVolume = "4.5GB"),
        PairgateDataPlanItem(id = "179", planId = "179", itemId = "179", name = "5.2GB", price = 2500.0, providerId = "9MOBILE", network = "9MOBILE", planCategory = "GIFTING", type = "GIFTING", validity = "30 Days", duration = 30, dataVolume = "5.2GB"),
        PairgateDataPlanItem(id = "184", planId = "184", itemId = "184", name = "6.2GB", price = 3000.0, providerId = "9MOBILE", network = "9MOBILE", planCategory = "GIFTING", type = "GIFTING", validity = "30 Days", duration = 30, dataVolume = "6.2GB"),
        PairgateDataPlanItem(id = "183", planId = "183", itemId = "183", name = "8.4GB", price = 4000.0, providerId = "9MOBILE", network = "9MOBILE", planCategory = "GIFTING", type = "GIFTING", validity = "30 Days", duration = 30, dataVolume = "8.4GB"),
        PairgateDataPlanItem(id = "182", planId = "182", itemId = "182", name = "11.4GB", price = 5000.0, providerId = "9MOBILE", network = "9MOBILE", planCategory = "GIFTING", type = "GIFTING", validity = "30 Days", duration = 30, dataVolume = "11.4GB"),
        // ======================= 9MOBILE (CG) =======================
        PairgateDataPlanItem(id = "9mobile_cg_500mb", planId = "9mobile_cg_500mb", itemId = "128", name = "500MB (CG)", price = 260.0, providerId = "9MOBILE", network = "9MOBILE", planCategory = "CG", type = "CG", validity = "30 Days", duration = 30, dataVolume = "500MB"),
        PairgateDataPlanItem(id = "9mobile_cg_1gb", planId = "9mobile_cg_1gb", itemId = "129", name = "1GB (CG)", price = 515.0, providerId = "9MOBILE", network = "9MOBILE", planCategory = "CG", type = "CG", validity = "30 Days", duration = 30, dataVolume = "1GB"),
        PairgateDataPlanItem(id = "9mobile_cg_2gb", planId = "9mobile_cg_2gb", itemId = "131", name = "2GB (CG)", price = 1030.0, providerId = "9MOBILE", network = "9MOBILE", planCategory = "CG", type = "CG", validity = "30 Days", duration = 30, dataVolume = "2GB"),
        PairgateDataPlanItem(id = "9mobile_cg_5gb", planId = "9mobile_cg_5gb", itemId = "135", name = "5GB (CG)", price = 2575.0, providerId = "9MOBILE", network = "9MOBILE", planCategory = "CG", type = "CG", validity = "30 Days", duration = 30, dataVolume = "5GB"),
        // ======================= 9MOBILE (BROADBAND ROUTER) =======================
        PairgateDataPlanItem(id = "9mobile_bb_30gb", planId = "398", itemId = "398", name = "30GB Router / MiFi Broadband", price = 10000.0, providerId = "9MOBILE", network = "9MOBILE", planCategory = "BROADBAND", type = "BROADBAND", validity = "30 Days", duration = 30, dataVolume = "30GB"),
        PairgateDataPlanItem(id = "9mobile_bb_60gb", planId = "399", itemId = "399", name = "60GB Router Mega Broadband", price = 18000.0, providerId = "9MOBILE", network = "9MOBILE", planCategory = "BROADBAND", type = "BROADBAND", validity = "30 Days", duration = 30, dataVolume = "60GB"),
        PairgateDataPlanItem(id = "9mobile_bb_100gb", planId = "400", itemId = "400", name = "100GB Router Ultra Broadband", price = 27500.0, providerId = "9MOBILE", network = "9MOBILE", planCategory = "BROADBAND", type = "BROADBAND", validity = "60 Days", duration = 60, dataVolume = "100GB"),
    )
}

@JsonClass(generateAdapter = true)
data class PairgateBillRequest(
    @Json(name = "provider_id") val providerId: String = "",
    @Json(name = "service_id") val serviceId: String = "",
    @Json(name = "biller_id") val billerId: String = "",
    @Json(name = "disco_name") val discoName: String = "",
    @Json(name = "customer_id") val customerId: String = "",
    @Json(name = "meter_number") val meterNumber: String = "",
    @Json(name = "smartcard_number") val smartcardNumber: String = "",
    @Json(name = "iuc_number") val iucNumber: String = "",
    @Json(name = "recipient") val recipient: String = "",
    @Json(name = "phone") val phone: String = "",
    @Json(name = "phone_number") val phoneNumber: String = "",
    @Json(name = "amount") val amount: Double = 0.0,
    @Json(name = "package") val packagePlan: String = "",
    @Json(name = "plan_id") val planId: String = "",
    @Json(name = "variation_code") val variationCode: String = "",
    @Json(name = "meter_type") val meterType: String = "prepaid",
    @Json(name = "reference") val reference: String = "",
    @Json(name = "request_id") val requestId: String = "",
    @Json(name = "ref") val ref: String = ""
) {
    companion object {
        fun create(
            providerId: String,
            accountOrMeterNumber: String,
            amount: Double,
            reference: String,
            packagePlan: String = "",
            phone: String = ""
        ): PairgateBillRequest {
            val cleanSlug = providerId.lowercase().trim()
            val cleanAccount = accountOrMeterNumber.replace(Regex("[^0-9]"), "").trim()
            return PairgateBillRequest(
                providerId = cleanSlug,
                serviceId = cleanSlug,
                billerId = cleanSlug,
                discoName = cleanSlug,
                customerId = cleanAccount,
                meterNumber = cleanAccount,
                smartcardNumber = cleanAccount,
                iucNumber = cleanAccount,
                recipient = cleanAccount,
                phone = phone.ifBlank { cleanAccount },
                phoneNumber = phone.ifBlank { cleanAccount },
                amount = amount,
                packagePlan = packagePlan,
                planId = packagePlan,
                variationCode = packagePlan,
                meterType = "prepaid",
                reference = reference,
                requestId = reference,
                ref = reference
            )
        }
    }
}

@JsonClass(generateAdapter = true)
data class PairgateVirtualAccountRequest(
    @Json(name = "first_name") val firstName: String = "",
    @Json(name = "last_name") val lastName: String = "",
    @Json(name = "email") val email: String = "",
    @Json(name = "phone") val phone: String = "",
    @Json(name = "customer_reference") val customerReference: String = "",
    @Json(name = "customer_name") val customerName: String? = null,
    @Json(name = "customer_email") val customerEmail: String? = null,
    @Json(name = "customer_phone") val customerPhone: String? = null,
    @Json(name = "bvn") val bvn: String? = null,
    @Json(name = "nin") val nin: String? = null
)

@JsonClass(generateAdapter = true)
data class PairgateVirtualAccountItem(
    @Json(name = "id") val id: String = "acc_1",
    @Json(name = "customer_name") val customerName: String = "",
    @Json(name = "customer_email") val customerEmail: String = "",
    @Json(name = "customer_phone") val customerPhone: String = "",
    @Json(name = "bank_name") val bankName: String = "Moniepoint Microfinance Bank",
    @Json(name = "account_number") val accountNumber: String = "",
    @Json(name = "account_name") val accountName: String = "",
    @Json(name = "reference") val reference: String = "",
    @Json(name = "balance") val balance: Double = 0.0,
    @Json(name = "status") val status: String = "ACTIVE",
    @Json(name = "created_at") val createdAt: String = "2026-08-19"
)

@JsonClass(generateAdapter = true)
data class PairgateVirtualAccountResponse(
    @Json(name = "status") val status: String? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "data") val data: Any? = null,
    @Json(name = "bankDetails") val bankDetails: Map<String, Any?>? = null,
    @Json(name = "account_number") val accountNumber: String? = null,
    @Json(name = "bank_name") val bankName: String? = null,
    @Json(name = "account_name") val accountName: String? = null,
    @Json(name = "reference") val reference: String? = null
) {
    private fun getDataMap(): Map<String, Any?>? {
        return when (data) {
            is Map<*, *> -> {
                @Suppress("UNCHECKED_CAST")
                data as Map<String, Any?>
            }
            else -> null
        }
    }

    fun extractAccountNumber(): String? {
        val map = getDataMap()
        return accountNumber
            ?: map?.get("account_number")?.toString()
            ?: map?.get("accountNumber")?.toString()
            ?: map?.get("nuban")?.toString()
            ?: bankDetails?.get("accountNumber")?.toString()
            ?: bankDetails?.get("account_number")?.toString()
    }

    fun extractBankName(): String? {
        val map = getDataMap()
        return bankName
            ?: map?.get("bank_name")?.toString()
            ?: map?.get("bankName")?.toString()
            ?: map?.get("bank")?.toString()
            ?: bankDetails?.get("bank")?.toString()
            ?: bankDetails?.get("bank_name")?.toString()
    }

    fun extractAccountName(): String? {
        val map = getDataMap()
        return accountName
            ?: map?.get("account_name")?.toString()
            ?: map?.get("accountName")?.toString()
            ?: map?.get("customer_name")?.toString()
            ?: bankDetails?.get("accountName")?.toString()
            ?: bankDetails?.get("account_name")?.toString()
    }

    fun extractReference(): String? {
        val map = getDataMap()
        return reference
            ?: map?.get("customer_reference")?.toString()
            ?: map?.get("reference")?.toString()
            ?: map?.get("ref")?.toString()
            ?: map?.get("id")?.toString()
    }
}

@JsonClass(generateAdapter = true)
data class PairgateAdminAccountsListResponse(
    @Json(name = "status") val status: String,
    @Json(name = "message") val message: String? = null,
    @Json(name = "total_accounts") val totalAccounts: Int? = 0,
    @Json(name = "accounts") val accounts: List<PairgateVirtualAccountItem>? = null
)

@JsonClass(generateAdapter = true)
data class PairgateAdminProfileResponse(
    @Json(name = "status") val status: String? = null,
    @Json(name = "reseller_name") val resellerName: String? = null,
    @Json(name = "business_name") val businessName: String? = null,
    @Json(name = "email") val email: String? = null,
    @Json(name = "phone") val phone: String? = null,
    @Json(name = "tier_level") val tierLevel: String? = null,
    @Json(name = "balance") val balance: Any? = null,
    @Json(name = "wallet_balance") val walletBalance: Any? = null,
    @Json(name = "data") val data: Map<String, Any?>? = null,
    @Json(name = "currency") val currency: String? = "NGN",
    @Json(name = "settlement_bank") val settlementBank: String? = null,
    @Json(name = "settlement_account_number") val settlementAccountNumber: String? = null,
    @Json(name = "settlement_account_name") val settlementAccountName: String? = null
) {
    fun getEffectiveBalance(): Double? {
        fun parseVal(v: Any?): Double? = when (v) {
            is Number -> v.toDouble()
            is String -> v.toDoubleOrNull()
            is Map<*, *> -> {
                val b = v["balance"] ?: v["wallet_balance"] ?: v["available_balance"] ?: v["user_balance"]
                when (b) {
                    is Number -> b.toDouble()
                    is String -> b.toDoubleOrNull()
                    else -> null
                }
            }
            else -> null
        }
        return parseVal(balance)
            ?: parseVal(walletBalance)
            ?: parseVal(data?.get("balance"))
            ?: parseVal(data?.get("wallet_balance"))
            ?: parseVal(data?.get("available_balance"))
            ?: parseVal(data?.get("user_balance"))
    }

    fun extractSettlementBank(): String? {
        val raw = settlementBank
            ?: data?.get("settlement_bank")?.toString()
            ?: data?.get("settlementBank")?.toString()
            ?: data?.get("bank_name")?.toString()
            ?: data?.get("bankName")?.toString()
            ?: data?.get("bank")?.toString()
        return sanitizeBankName(raw)
    }

    companion object {
        fun sanitizeBankName(raw: String?): String? {
            if (raw.isNullOrBlank()) return null
            val trimmed = raw.trim()
            val commonBanks = listOf(
                "Moniepoint Microfinance Bank",
                "Moniepoint MFB",
                "Providus Bank",
                "Wema Bank",
                "SafeHaven MFB",
                "PalmPay",
                "OPay",
                "Kuda Bank",
                "First Bank",
                "GTBank",
                "Access Bank",
                "Zenith Bank",
                "United Bank for Africa",
                "Sterling Bank",
                "Fidelity Bank"
            )
            for (bank in commonBanks) {
                val doubleBank = "$bank $bank"
                val dashBank = "$bank - $bank"
                val dotBank = "$bank • $bank"
                val slashBank = "$bank / $bank"
                if (trimmed.contains(doubleBank, ignoreCase = true) || 
                    trimmed.contains(dashBank, ignoreCase = true) || 
                    trimmed.contains(dotBank, ignoreCase = true) ||
                    trimmed.contains(slashBank, ignoreCase = true)) {
                    return bank
                }
            }
            val words = trimmed.split("\\s+".toRegex()).filter { it.isNotBlank() }
            if (words.size >= 2 && words.size % 2 == 0) {
                val half = words.size / 2
                val firstHalf = words.subList(0, half).joinToString(" ")
                val secondHalf = words.subList(half, words.size).joinToString(" ")
                if (firstHalf.equals(secondHalf, ignoreCase = true)) {
                    return firstHalf
                }
            }
            return trimmed
        }
    }

    fun extractSettlementAccountNumber(): String? {
        return settlementAccountNumber
            ?: data?.get("settlement_account_number")?.toString()
            ?: data?.get("settlementAccountNumber")?.toString()
            ?: data?.get("account_number")?.toString()
            ?: data?.get("accountNumber")?.toString()
            ?: data?.get("nuban")?.toString()
    }

    fun extractSettlementAccountName(): String? {
        return settlementAccountName
            ?: data?.get("settlement_account_name")?.toString()
            ?: data?.get("settlementAccountName")?.toString()
            ?: data?.get("account_name")?.toString()
            ?: data?.get("accountName")?.toString()
            ?: data?.get("beneficiary_name")?.toString()
    }
}

@JsonClass(generateAdapter = true)
data class PairgateElectricityPurchaseRequest(
    @Json(name = "provider_id") val providerId: String,
    @Json(name = "amount") val amount: Int,
    @Json(name = "meter_number") val meterNumber: String,
    @Json(name = "meter_type") val meterType: Int, // 1 = prepaid, 2 = postpaid
    @Json(name = "reference") val reference: String
)

@JsonClass(generateAdapter = true)
data class PairgateCablePurchaseRequest(
    @Json(name = "provider_id") val providerId: String,
    @Json(name = "plan_id") val planId: String,
    @Json(name = "smartcard") val smartcard: String,
    @Json(name = "reference") val reference: String
)

@JsonClass(generateAdapter = true)
data class PairgateApiResponse(
    @Json(name = "status") val status: String? = null,
    @Json(name = "code") val code: Any? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "reference") val reference: String? = null,
    @Json(name = "errors") val errors: Any? = null,
    @Json(name = "data") val data: Map<String, Any?>? = null,
    @Json(name = "token") val token: Any? = null,
    @Json(name = "token_pin") val tokenPin: Any? = null,
    @Json(name = "tokenPin") val tokenPinCamel: Any? = null,
    @Json(name = "pin") val pin: Any? = null,
    @Json(name = "meter_token") val meterToken: Any? = null,
    @Json(name = "main_token") val mainToken: Any? = null,
    @Json(name = "creditToken") val creditToken: Any? = null,
    @Json(name = "units") val units: Any? = null,
    @Json(name = "meter_units") val meterUnits: Any? = null,
    @Json(name = "kwh") val kwh: Any? = null,
    @Json(name = "address") val address: Any? = null,
    @Json(name = "service_address") val serviceAddress: Any? = null,
    @Json(name = "customer_name") val customerName: Any? = null,
    @Json(name = "customerName") val customerNameCamel: Any? = null,
    @Json(name = "name") val name: Any? = null,
    @Json(name = "meter_number") val meterNumber: Any? = null,
    @Json(name = "tariff_class") val tariffClass: Any? = null
) {
    fun isSuccessful(): Boolean {
        val s = status?.lowercase() ?: ""
        val c = code?.toString() ?: ""
        if (s == "error" || s == "failed" || s == "fail") return false
        return s == "success" || s == "true" || c == "200" || (data?.get("status") == true) || (data?.get("status")?.toString()?.equals("success", ignoreCase = true) == true)
    }

    fun isProcessing(): Boolean {
        val s = status?.lowercase() ?: ""
        val m = message?.lowercase() ?: ""
        val ds = data?.get("status")?.toString()?.lowercase() ?: ""
        return s == "processing" || s == "pending" || ds == "processing" || ds == "pending" || m.contains("processing") || m.contains("pending")
    }

    fun isFailedOrRefunded(): Boolean {
        val s = status?.lowercase() ?: ""
        val m = message?.lowercase() ?: ""
        val ds = data?.get("status")?.toString()?.lowercase() ?: ""
        return s == "failed" || s == "refunded" || s == "reversed" || s == "error" ||
                ds == "failed" || ds == "refunded" || ds == "reversed" ||
                m.contains("refund", ignoreCase = true) || m.contains("fail", ignoreCase = true) || m.contains("revers", ignoreCase = true)
    }

    fun getDetailedMessage(): String {
        val baseMsg = message?.trim()
        val errMap = errors as? Map<*, *>
        if (errMap != null && errMap.isNotEmpty()) {
            val errList = errMap.values.joinToString("; ") { v ->
                when (v) {
                    is List<*> -> v.filterNotNull().joinToString(", ")
                    else -> v.toString()
                }
            }
            if (errList.isNotBlank()) {
                return if (!baseMsg.isNullOrBlank()) "$baseMsg ($errList)" else errList
            }
        }
        if (!baseMsg.isNullOrBlank()) return baseMsg
        return "Pairgate response: ${status ?: "Unknown"}"
    }

    companion object {
        fun parseErrorBody(errorJson: String?): PairgateApiResponse? {
            if (errorJson.isNullOrBlank()) return null
            return try {
                val moshi = com.squareup.moshi.Moshi.Builder()
                    .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
                    .build()
                moshi.adapter(PairgateApiResponse::class.java).fromJson(errorJson)
            } catch (e: Exception) {
                PairgateApiResponse(
                    status = "error",
                    message = errorJson.take(200)
                )
            }
        }
    }

    fun getEffectiveReference(): String? {
        return reference
            ?: data?.get("reference_code")?.toString()
            ?: data?.get("reference")?.toString()
            ?: data?.get("ref")?.toString()
    }

    private fun sanitizeTokenString(raw: Any?): String? {
        if (raw == null) return null
        val str = when (raw) {
            is Double -> {
                if (raw > 1e12) java.math.BigDecimal.valueOf(raw).toPlainString() else raw.toLong().toString()
            }
            is Long -> raw.toString()
            is Number -> java.math.BigDecimal(raw.toString()).toPlainString()
            else -> raw.toString().trim()
        }
        if (str.isBlank() || str.equals("null", ignoreCase = true)) return null

        val digitsOnly = str.replace(Regex("[^0-9]"), "")
        if (digitsOnly.length == 20) {
            return digitsOnly
        }
        if (digitsOnly.length in 16..24) {
            return digitsOnly
        }
        if (str.length in 16..30) return str
        return null
    }

    fun getMeterToken(): String? {
        // 1. Direct model root fields
        val direct = sanitizeTokenString(token)
            ?: sanitizeTokenString(tokenPin)
            ?: sanitizeTokenString(tokenPinCamel)
            ?: sanitizeTokenString(pin)
            ?: sanitizeTokenString(meterToken)
            ?: sanitizeTokenString(mainToken)
            ?: sanitizeTokenString(creditToken)
        if (direct != null) return direct

        // 2. Data dictionary lookup
        if (data != null) {
            val fromData = sanitizeTokenString(data["token"])
                ?: sanitizeTokenString(data["token_pin"])
                ?: sanitizeTokenString(data["tokenPin"])
                ?: sanitizeTokenString(data["pin"])
                ?: sanitizeTokenString(data["meter_token"])
                ?: sanitizeTokenString(data["main_token"])
                ?: sanitizeTokenString(data["creditToken"])
                ?: sanitizeTokenString(data["token_code"])
                ?: sanitizeTokenString(data["tokenCode"])
                ?: sanitizeTokenString(data["tokenNumber"])
                ?: sanitizeTokenString(data["tokens"])
            if (fromData != null) return fromData

            // Deep inspect sub-maps
            for (key in listOf("data", "details", "token_data", "result", "payload", "receipt")) {
                val sub = data[key] as? Map<*, *>
                if (sub != null) {
                    val subToken = sanitizeTokenString(sub["token"])
                        ?: sanitizeTokenString(sub["token_pin"])
                        ?: sanitizeTokenString(sub["tokenPin"])
                        ?: sanitizeTokenString(sub["pin"])
                        ?: sanitizeTokenString(sub["meter_token"])
                        ?: sanitizeTokenString(sub["creditToken"])
                    if (subToken != null) return subToken
                }
            }
        }

        // 3. Fallback: Search all message and stringified data for 20-digit pattern
        val allText = (message ?: "") + " " + (data?.toString() ?: "")
        val matchDashes = Regex("""\b(\d{4}[-\s]\d{4}[-\s]\d{4}[-\s]\d{4}[-\s]\d{4})\b""").find(allText)
        if (matchDashes != null) return matchDashes.value.replace(" ", "-")

        val match20 = Regex("""\b(\d{20})\b""").find(allText)
        if (match20 != null) return match20.value

        return null
    }

    fun getMeterUnits(): String? {
        val direct = units?.toString() ?: meterUnits?.toString() ?: kwh?.toString()
        if (!direct.isNullOrBlank() && direct != "null") return direct

        return data?.get("units")?.toString()
            ?: data?.get("meter_units")?.toString()
            ?: data?.get("kwh")?.toString()
            ?: data?.get("unit")?.toString()
            ?: data?.get("units_credited")?.toString()
    }

    fun getServiceAddress(): String? {
        val direct = address?.toString() ?: serviceAddress?.toString()
        if (!direct.isNullOrBlank() && direct != "null") return direct

        return data?.get("service_address")?.toString()
            ?: data?.get("address")?.toString()
            ?: data?.get("serviceAddress")?.toString()
            ?: data?.get("location")?.toString()
            ?: data?.get("customer_address")?.toString()
    }

    fun getCustomerName(): String? {
        val direct = customerName?.toString() ?: customerNameCamel?.toString() ?: name?.toString()
        if (!direct.isNullOrBlank() && direct != "null") return direct

        return data?.get("customer_name")?.toString()
            ?: data?.get("customerName")?.toString()
            ?: data?.get("name")?.toString()
            ?: data?.get("consumer_name")?.toString()
            ?: data?.get("beneficiary_name")?.toString()
    }
}

@JsonClass(generateAdapter = true)
data class PairgateBalanceResponse(
    @Json(name = "status") val status: String? = null,
    @Json(name = "balance") val balance: Any? = null,
    @Json(name = "wallet_balance") val walletBalance: Any? = null,
    @Json(name = "data") val data: Map<String, Any?>? = null,
    @Json(name = "currency") val currency: String? = "NGN",
    @Json(name = "message") val message: String? = null
) {
    fun getEffectiveBalance(): Double? {
        fun parseVal(v: Any?): Double? = when (v) {
            is Number -> v.toDouble()
            is String -> v.toDoubleOrNull()
            is Map<*, *> -> {
                val b = v["balance"] ?: v["wallet_balance"] ?: v["available_balance"] ?: v["user_balance"]
                when (b) {
                    is Number -> b.toDouble()
                    is String -> b.toDoubleOrNull()
                    else -> null
                }
            }
            else -> null
        }
        return parseVal(balance)
            ?: parseVal(walletBalance)
            ?: parseVal(data?.get("balance"))
            ?: parseVal(data?.get("wallet_balance"))
            ?: parseVal(data?.get("available_balance"))
            ?: parseVal(data?.get("user_balance"))
    }
}

@JsonClass(generateAdapter = true)
data class PairgateWebhookEvent(
    @param:Json(name = "event") val event: String = "virtual_account.deposit",
    @param:Json(name = "reference") val reference: String = "",
    @param:Json(name = "amount") val amount: Double = 0.0,
    @param:Json(name = "fee") val fee: Double = 0.0,
    @param:Json(name = "currency") val currency: String = "NGN",
    @param:Json(name = "customer_email") val customerEmail: String = "",
    @param:Json(name = "customer_phone") val customerPhone: String = "",
    @param:Json(name = "account_number") val accountNumber: String = "",
    @param:Json(name = "bank_name") val bankName: String = "Moniepoint Microfinance Bank",
    @param:Json(name = "sender_name") val senderName: String = "",
    @param:Json(name = "sender_bank") val senderBank: String = "",
    @param:Json(name = "session_id") val sessionId: String = "",
    @param:Json(name = "status") val status: String = "SUCCESS",
    @param:Json(name = "timestamp") val timestamp: Long = System.currentTimeMillis()
)

data class PairgateWebhookLog(
    val id: String = java.util.UUID.randomUUID().toString(),
    val event: String,
    val reference: String,
    val amount: Double,
    val signatureVerified: Boolean,
    val calculatedSignature: String,
    val incomingSignature: String,
    val payloadJson: String,
    val status: String,
    val timestamp: Long = System.currentTimeMillis()
)

object PairgateWebhookSecurity {
    fun generateSecureWebhookSecret(prefix: String = "mnp_whsec_"): String {
        val secureRandom = java.security.SecureRandom()
        val randomBytes = ByteArray(24) // 24 bytes = 48 hex characters (192 bits of entropy)
        secureRandom.nextBytes(randomBytes)
        val hex = randomBytes.joinToString("") { "%02x".format(it) }
        return "$prefix$hex"
    }

    fun computeHmacSha512(data: String, secretKey: String): String {
        return try {
            val algorithm = "HmacSHA512"
            val secretKeySpec = javax.crypto.spec.SecretKeySpec(secretKey.toByteArray(Charsets.UTF_8), algorithm)
            val mac = javax.crypto.Mac.getInstance(algorithm)
            mac.init(secretKeySpec)
            val hash = mac.doFinal(data.toByteArray(Charsets.UTF_8))
            hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }

    fun computeHmacSha256(data: String, secretKey: String): String {
        return try {
            val algorithm = "HmacSHA256"
            val secretKeySpec = javax.crypto.spec.SecretKeySpec(secretKey.toByteArray(Charsets.UTF_8), algorithm)
            val mac = javax.crypto.Mac.getInstance(algorithm)
            mac.init(secretKeySpec)
            val hash = mac.doFinal(data.toByteArray(Charsets.UTF_8))
            hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }

    fun extractSignatureFromHeaders(headers: Map<String, String>): String? {
        val candidateKeys = listOf(
            "moniepoint-signature",
            "x-moniepoint-signature",
            "x-signature",
            "signature",
            "x-hub-signature-256",
            "x-moniepoint-hash"
        )
        for (key in candidateKeys) {
            val entry = headers.entries.find { it.key.equals(key, ignoreCase = true) }
            if (entry != null && entry.value.isNotBlank()) {
                return entry.value.trim()
            }
        }
        return null
    }

    fun verifySignature(
        rawPayload: String,
        providedSignature: String,
        secretKey: String
    ): Boolean {
        if (providedSignature.isBlank() || secretKey.isBlank()) return false
        val computed512 = computeHmacSha512(rawPayload, secretKey)
        val computed256 = computeHmacSha256(rawPayload, secretKey)

        val cleanProvided = providedSignature.trim().lowercase()
        return java.security.MessageDigest.isEqual(
            cleanProvided.toByteArray(Charsets.UTF_8),
            computed512.lowercase().toByteArray(Charsets.UTF_8)
        ) || java.security.MessageDigest.isEqual(
            cleanProvided.toByteArray(Charsets.UTF_8),
            computed256.lowercase().toByteArray(Charsets.UTF_8)
        )
    }

    fun verifyWithDiagnostics(
        rawPayload: String,
        providedSignature: String,
        secretKey: String
    ): WebhookSignatureVerificationResult {
        if (secretKey.isBlank()) {
            return WebhookSignatureVerificationResult(
                isVerified = false,
                matchedAlgorithm = "NONE",
                computed256 = "",
                computed512 = "",
                providedSignature = providedSignature,
                diagnostics = "Rejection: Webhook secret key is empty or not configured."
            )
        }
        if (providedSignature.isBlank()) {
            val calc256 = computeHmacSha256(rawPayload, secretKey)
            val calc512 = computeHmacSha512(rawPayload, secretKey)
            return WebhookSignatureVerificationResult(
                isVerified = false,
                matchedAlgorithm = "NONE",
                computed256 = calc256,
                computed512 = calc512,
                providedSignature = "",
                diagnostics = "Rejection: Missing signature header (moniepoint-signature). Expected HMAC-SHA256: $calc256"
            )
        }

        val calc256 = computeHmacSha256(rawPayload, secretKey)
        val calc512 = computeHmacSha512(rawPayload, secretKey)
        val cleanProvided = providedSignature.trim().lowercase()

        val isMatch256 = java.security.MessageDigest.isEqual(
            cleanProvided.toByteArray(Charsets.UTF_8),
            calc256.lowercase().toByteArray(Charsets.UTF_8)
        )
        val isMatch512 = java.security.MessageDigest.isEqual(
            cleanProvided.toByteArray(Charsets.UTF_8),
            calc512.lowercase().toByteArray(Charsets.UTF_8)
        )

        return if (isMatch256) {
            WebhookSignatureVerificationResult(
                isVerified = true,
                matchedAlgorithm = "HMAC-SHA256",
                computed256 = calc256,
                computed512 = calc512,
                providedSignature = providedSignature,
                diagnostics = "Verified: Valid Moniepoint HMAC-SHA256 signature match."
            )
        } else if (isMatch512) {
            WebhookSignatureVerificationResult(
                isVerified = true,
                matchedAlgorithm = "HMAC-SHA512",
                computed256 = calc256,
                computed512 = calc512,
                providedSignature = providedSignature,
                diagnostics = "Verified: Valid Moniepoint HMAC-SHA512 signature match."
            )
        } else {
            WebhookSignatureVerificationResult(
                isVerified = false,
                matchedAlgorithm = "MISMATCH",
                computed256 = calc256,
                computed512 = calc512,
                providedSignature = providedSignature,
                diagnostics = "Security Alert: Tampered signature detected. Received: '$providedSignature', Expected HMAC-SHA256: '$calc256'."
            )
        }
    }
}

data class WebhookSignatureVerificationResult(
    val isVerified: Boolean,
    val matchedAlgorithm: String,
    val computed256: String,
    val computed512: String,
    val providedSignature: String,
    val diagnostics: String
)

@JsonClass(generateAdapter = true)
data class PairgateElectricityVerifyRequest(
    @Json(name = "provider_id") val providerId: String,
    @Json(name = "meter_number") val meterNumber: String,
    @Json(name = "meter_type") val meterType: Int = 1
)

@JsonClass(generateAdapter = true)
data class PairgateCableVerifyRequest(
    @Json(name = "provider_id") val providerId: String,
    @Json(name = "smartcard") val smartcard: String
)

@JsonClass(generateAdapter = true)
data class PairgateElectricityVerifyResponse(
    @Json(name = "code") val code: Int? = null,
    @Json(name = "status") val status: Any? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "customer_name") val rootCustomerName: String? = null,
    @Json(name = "name") val rootName: String? = null,
    @Json(name = "address") val rootAddress: String? = null,
    @Json(name = "data") val data: Map<String, Any?>? = null
) {
    fun isSuccessful(): Boolean {
        if (code != null && code != 200) return false
        if (status is String && status.equals("error", ignoreCase = true)) return false
        if (status is Boolean && !status) return false
        val dataStatus = data?.get("status")
        if (dataStatus is Boolean && !dataStatus) return false
        val name = extractCustomerName()
        if (name.isBlank() || name.equals("Unknown Customer", ignoreCase = true)) return false
        return true
    }

    fun extractCustomerName(): String {
        return rootCustomerName?.takeIf { it.isNotBlank() }
            ?: rootName?.takeIf { it.isNotBlank() }
            ?: data?.get("customer_name")?.toString()?.takeIf { it.isNotBlank() }
            ?: data?.get("name")?.toString()?.takeIf { it.isNotBlank() }
            ?: data?.get("Customer_Name")?.toString()?.takeIf { it.isNotBlank() }
            ?: ""
    }

    fun extractAddress(): String {
        return rootAddress?.takeIf { it.isNotBlank() }
            ?: data?.get("address")?.toString()?.takeIf { it.isNotBlank() }
            ?: data?.get("customer_address")?.toString()?.takeIf { it.isNotBlank() }
            ?: data?.get("Address")?.toString()?.takeIf { it.isNotBlank() }
            ?: ""
    }

    fun extractTariff(): String {
        return data?.get("tariff")?.toString()?.takeIf { it.isNotBlank() }
            ?: data?.get("tariff_class")?.toString()?.takeIf { it.isNotBlank() }
            ?: data?.get("tariff_code")?.toString()?.takeIf { it.isNotBlank() }
            ?: ""
    }

    fun extractAccountNumber(): String {
        return data?.get("account_number")?.toString()?.takeIf { it.isNotBlank() }
            ?: data?.get("meter_number")?.toString()?.takeIf { it.isNotBlank() }
            ?: ""
    }
}

object PairgateDisCoUtils {
    /**
     * Maps human-readable DisCo names or codes to Pairgate's verified provider slugs:
     * - "ikedc" -> Ikeja Electricity - IKEDC (PHCN)
     * - "eko" -> Eko Electricity - EKEDC (PHCN)
     * - "aedc" -> Abuja electricity - AEDC
     * - "ibedc" -> Ibadan Electricity - IBEDC
     * - "enugu" -> Enugu Electricity - EEDC
     * - "kedco" -> Kano Electricity - KEDCO
     * - "ph" -> PortHarcourt Electricity - PHEDC
     * - "benin" -> Benin Electricity - BEDC
     * - "kaduna" -> Kaduna Electricity - KAEDC
     * - "jedc" -> Jos Electricity - JEDC
     * - "yola" -> Yola Electricity - YEDC
     * - "aba" -> Aba Electricity - ABA
     */
    fun resolveSlug(serviceId: String): String {
        val clean = serviceId.lowercase().trim()
        return when {
            clean.contains("ikedc") || clean.contains("ikeja") -> "ikedc"
            clean.contains("ekedc") || clean.contains("eko") -> "eko"
            clean.contains("aedc") || clean.contains("abuja") -> "aedc"
            clean.contains("ibedc") || clean.contains("ibadan") -> "ibedc"
            clean.contains("enugu") || clean.contains("eedc") -> "enugu"
            clean.contains("kedco") || clean.contains("kano") -> "kedco"
            clean.contains("phed") || clean.contains("port harcourt") || clean.contains("ph") -> "ph"
            clean.contains("bedc") || clean.contains("benin") -> "benin"
            clean.contains("kaedc") || clean.contains("kaduna") -> "kaduna"
            clean.contains("jedc") || clean.contains("jed") || clean.contains("jos") -> "jedc"
            clean.contains("yedc") || clean.contains("yola") -> "yola"
            clean.contains("aba") -> "aba"
            else -> clean.replace(Regex("[^a-z0-9]"), "")
        }
    }

    fun getDisplayName(serviceId: String): String {
        return when (resolveSlug(serviceId)) {
            "ikedc" -> "Ikeja Electric (IKEDC)"
            "eko" -> "Eko Electric (EKEDC)"
            "aedc" -> "Abuja Electric (AEDC)"
            "ibedc" -> "Ibadan Electric (IBEDC)"
            "enugu" -> "Enugu Electric (EEDC)"
            "kedco" -> "Kano Electric (KEDCO)"
            "ph" -> "Port Harcourt Electric (PHEDC)"
            "benin" -> "Benin Electric (BEDC)"
            "kaduna" -> "Kaduna Electric (KAEDC)"
            "jedc" -> "Jos Electric (JEDC)"
            "yola" -> "Yola Electric (YEDC)"
            "aba" -> "Aba Power (ABA)"
            else -> serviceId
        }
    }

    fun parseMeterTypeInt(type: String): Int {
        val clean = type.lowercase().trim()
        return if (clean.contains("postpaid") || clean == "2") 2 else 1
    }
}
