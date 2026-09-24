package com.example.data.api

data class DataBundleDisplayPlan(
    val id: String,
    val name: String, // Branded display name, e.g. "FlowSaver™ 1GB Value"
    val rawName: String, // Original name e.g. "1GB (SME)"
    val network: String,
    val price: Double,
    val wholesalePrice: Double = price,
    val description: String,
    val validityText: String,
    val validityTab: String, // "Daily", "Weekly", "Monthly", "2-Months", "All"
    val category: String, // "SME Bundles", "CG Bundles", "Broadband Router Bundles", "Social Bundles", "Gifting"
    val brandedCategory: String, // "FlowSaver™ (SME)", "FlowTurbo™ (CG)", "FlowBroadband™ (Router)", "FlowSocial™ (Pass)", "FlowDirect™ (Telco)"
    val subBadge: String, // e.g. "⚡ SME Rate • Zero Auto-Renew"
    val infoBreakdown: String,
    val ussdCode: String = "*323#",
    val isHot: Boolean = false,
    val hasAutoRenewProtection: Boolean = true
)

object PairgateCatalogHelper {

    private val catalogCache = java.util.concurrent.ConcurrentHashMap<String, List<DataBundleDisplayPlan>>()

    fun clearCache() {
        catalogCache.clear()
    }

    fun normalizeDataVolumeKey(name: String): String {
        val clean = name.lowercase().replace(" ", "").replace("(", "").replace(")", "").replace("-", "")
        val match = Regex("""(\d+(\.\d+)?)(gb|mb|tb)""").find(clean) ?: return clean
        val numStr = match.groupValues[1]
        val unit = match.groupValues[3]
        val num = numStr.toDoubleOrNull() ?: return clean
        val formattedNum = if (num % 1.0 == 0.0) num.toInt().toString() else num.toString()
        return "$formattedNum$unit"
    }

    /**
     * Resolves unique branded package name and badging based on package category.
     * Renames SME -> FlowSaver™, CG -> FlowTurbo™, Broadband -> FlowBroadband™, etc.
     */
    fun getBrandedPlanDetails(
        planName: String,
        categoryStr: String
    ): Triple<String, String, String> {
        val raw = planName.trim()
        val volMatch = Regex("""(\d+(\.\d+)?)\s*(GB|MB|TB)""", RegexOption.IGNORE_CASE).find(raw)
        val displayVol = volMatch?.value?.uppercase()?.replace(" ", "") ?: normalizeDataVolumeKey(raw).uppercase()

        return when (categoryStr) {
            "SME Bundles" -> Triple(
                "FlowSaver™ $displayVol Value",
                "FlowSaver™ (SME)",
                "SME Wholesale"
            )
            "CG Bundles" -> Triple(
                "FlowTurbo™ $displayVol Corporate",
                "FlowTurbo™ (CG)",
                "Corporate Quota"
            )
            "Broadband Router Bundles" -> Triple(
                "FlowBroadband™ $displayVol Router",
                "FlowBroadband™ (Router)",
                "5G / MiFi Router"
            )
            "Social Bundles" -> Triple(
                "FlowSocial™ $displayVol Pass",
                "FlowSocial™ (Pass)",
                "Social & Streaming Pass"
            )
            "Gifting" -> Triple(
                "FlowDirect™ $displayVol Official",
                "FlowDirect™ (Telco)",
                "Direct Bundle"
            )
            else -> Triple(
                "FlowDirect™ $displayVol",
                "FlowDirect™",
                "Direct Bundle"
            )
        }
    }

    /**
     * Primary catalog builder supporting dynamic selective markup per category:
     * - SME: Higher markup (e.g. +8.0%) since wholesale is low
     * - CG: Selective markup (e.g. +6.0%)
     * - Direct / Gifting: Capped at official telco face value (profit via Pairgate cashback)
     * - Broadband Router: Selective markup (e.g. +4.0%)
     * - Social: Selective markup (e.g. +5.0%)
     *
     * Merges live API plans with verified catalog plans so SME, CG, and Broadband are NEVER missing.
     */
    fun buildCatalogForNetwork(
        network: String,
        livePlans: List<PairgateDataPlanItem>,
        generalMarkup: Double = PairgateVerifiedPlans.MARKUP_PERCENT,
        smeMarkup: Double = 8.0,
        cgMarkup: Double = 6.0,
        directMarkup: Double = 0.0,
        broadbandMarkup: Double = 4.0,
        socialMarkup: Double = 5.0,
        pricingStrategy: String = "SMART_TELCO_CAP",
        discountPercent: Double = 0.0
    ): List<DataBundleDisplayPlan> {
        val netUpper = network.uppercase()
        val cacheKey = "${netUpper}_${livePlans.hashCode()}_${generalMarkup}_${smeMarkup}_${cgMarkup}_${directMarkup}_${broadbandMarkup}_${socialMarkup}_${pricingStrategy}_${discountPercent}"
        catalogCache[cacheKey]?.let { return it }

        fun mapItemToDisplay(plan: PairgateDataPlanItem): DataBundleDisplayPlan {
            val days = plan.getEffectiveDurationDays()
            val validityTab = when {
                days >= 45 -> "2-Months"
                days in 15..44 -> "Monthly"
                days in 4..14 -> "Weekly"
                else -> "Daily"
            }
            val validityText = when {
                days == 1 -> "Valid for 1 Day (24 Hrs)"
                days == 2 -> "Valid for 2 Days (48 Hrs)"
                days in 3..6 -> "Valid for $days Days"
                days == 7 -> "Valid for 7 Days (1 Week)"
                days == 14 -> "Valid for 14 Days (2 Weeks)"
                days == 30 -> "Valid for 30 Days (1 Month)"
                days == 60 -> "Valid for 60 Days (2 Months)"
                days == 90 -> "Valid for 90 Days (3 Months)"
                days == 365 -> "Valid for 365 Days (1 Year)"
                else -> "Valid for ${plan.getEffectiveValidity()}"
            }
            val effCat = plan.getEffectiveCategory()
            val effName = plan.getEffectiveName().lowercase()
            val categoryStr = when {
                effCat.contains("BROADBAND", ignoreCase = true) || effName.contains("router") || effName.contains("broadband") || effName.contains("hynetflex") -> "Broadband Router Bundles"
                effCat.contains("SOCIAL", ignoreCase = true) || effName.contains("social") || effName.contains("pulse") || effName.contains("whatsapp") || effName.contains("tiktok") -> "Social Bundles"
                effCat.contains("SME", ignoreCase = true) || effName.contains("(sme)") || effName.contains("sme") -> "SME Bundles"
                effCat.contains("CG", ignoreCase = true) || effCat.contains("Corporate", ignoreCase = true) || effName.contains("(cg)") || effName.contains("cg") || effName.contains("corporate") -> "CG Bundles"
                effCat.contains("Gift", ignoreCase = true) || effName.contains("gift") -> "Gifting"
                else -> "Gifting"
            }

            val retailPrice = PairgateVerifiedPlans.getRetailPriceForCategory(
                category = categoryStr,
                wholesale = plan.getEffectivePrice(),
                generalMarkupPercent = generalMarkup,
                smeMarkupPercent = smeMarkup,
                cgMarkupPercent = cgMarkup,
                directMarkupPercent = directMarkup,
                broadbandMarkupPercent = broadbandMarkup,
                socialMarkupPercent = socialMarkup,
                pricingStrategy = pricingStrategy,
                discountPercent = discountPercent
            )

            val (brandedTitle, brandedCat, subBadge) = getBrandedPlanDetails(plan.getEffectiveName(), categoryStr)

            val ussd = when (netUpper) {
                "MTN" -> "*312*4*7#"
                "AIRTEL" -> "*323#"
                "GLO" -> "*323#"
                "9MOBILE" -> "*323#"
                else -> "*323#"
            }

            return DataBundleDisplayPlan(
                id = plan.getEffectivePlanId(),
                name = brandedTitle,
                rawName = plan.getEffectiveName(),
                network = netUpper,
                price = retailPrice,
                wholesalePrice = plan.getEffectivePrice(),
                description = "Get $brandedTitle for ₦${retailPrice.toInt()}. $validityText",
                validityText = validityText,
                validityTab = validityTab,
                category = categoryStr,
                brandedCategory = brandedCat,
                subBadge = subBadge,
                infoBreakdown = "Plan ID: ${plan.getEffectivePlanId()} • Allowance: ${plan.getEffectiveName()}",
                ussdCode = ussd,
                isHot = plan.isHotPlan(),
                hasAutoRenewProtection = true
            )
        }

        // 1. Map live plans from API matching provider and confirmed available
        val matchingLive = livePlans.filter { 
            it.getEffectiveProvider().equals(netUpper, ignoreCase = true) && it.isAvailable() 
        }
        val mappedLive = matchingLive.map { mapItemToDisplay(it) }

        // 2. Fallback only if device is offline or live plans are not yet returned
        val verifiedForNetwork = PairgateVerifiedPlans.ALL_PLANS
            .filter { it.getEffectiveProvider().equals(netUpper, ignoreCase = true) && it.isAvailable() }
            .map { mapItemToDisplay(it) }

        // 3. Selection: When live plans are fetched from Pairgate API, strictly use ONLY the genuine live plans!
        val combined = if (mappedLive.isNotEmpty()) {
            mappedLive.toMutableList()
        } else {
            verifiedForNetwork.toMutableList()
        }

        // Deduplicate cleanly by ID and Provider+Category+Volume signature
        val seenIds = mutableSetOf<String>()
        val seenSignatures = mutableSetOf<String>()
        val result = mutableListOf<DataBundleDisplayPlan>()

        for (item in combined) {
            val cleanId = item.id.trim().lowercase()
            // Strict filter: decommissioned plans (e.g. 23, 24) or invalid prices
            if (cleanId == "23" || cleanId == "24" || item.id.trim() == "23" || item.id.trim() == "24") {
                continue
            }
            if (item.price <= 0.0) {
                continue
            }
            // Strict filter: MTN SME decommissioned package IDs on upstream carrier
            if (item.network.equals("MTN", ignoreCase = true) && item.category.contains("SME", ignoreCase = true)) {
                if (cleanId in listOf("19", "20", "21", "22", "23", "24", "26")) {
                    continue
                }
            }
            // Strict filter: Inactive / unavailable indicators in plan name
            val rawLower = item.rawName.lowercase()
            if (rawLower.contains("inactive") || rawLower.contains("unavailable") ||
                rawLower.contains("disabled") || rawLower.contains("decommissioned") ||
                rawLower.contains("out of stock") || rawLower.contains("out_of_stock") ||
                rawLower.contains("deprecated") || rawLower.contains("carrier inactive")) {
                continue
            }
            val sig = "${item.network.uppercase()}_${item.category.uppercase()}_${normalizeDataVolumeKey(item.rawName)}"
            if (cleanId.isNotBlank() && seenIds.contains(cleanId)) {
                continue
            }
            if (seenSignatures.contains(sig)) {
                continue
            }
            if (cleanId.isNotBlank()) seenIds.add(cleanId)
            seenSignatures.add(sig)
            result.add(item)
        }

        // Sort by price ascending
        val sorted = result.sortedBy { it.price }
        catalogCache[cacheKey] = sorted
        return sorted
    }

    /**
     * Backward-compatible overload
     */
    fun buildCatalogForNetwork(
        network: String,
        livePlans: List<PairgateDataPlanItem>,
        markupPercent: Double = PairgateVerifiedPlans.MARKUP_PERCENT,
        pricingStrategy: String = "SMART_TELCO_CAP",
        discountPercent: Double = 0.0
    ): List<DataBundleDisplayPlan> {
        return buildCatalogForNetwork(
            network = network,
            livePlans = livePlans,
            generalMarkup = markupPercent,
            smeMarkup = 8.0,
            cgMarkup = 6.0,
            directMarkup = if (pricingStrategy == "FLAT_MARKUP") markupPercent else 0.0,
            broadbandMarkup = 4.0,
            socialMarkup = 5.0,
            pricingStrategy = pricingStrategy,
            discountPercent = discountPercent
        )
    }
}
