package com.example.data.util

import androidx.compose.ui.graphics.Color

/**
 * Utility for automatic telco carrier detection, normalization, and network-mismatch validation
 * across all Nigerian mobile numbers (MTN, GLO, Airtel, 9Mobile).
 */
object NigerianCarrierDetector {

    // Prefix mapping for Nigerian Telco Carriers
    private val MTN_PREFIXES = listOf(
        "0803", "0806", "0703", "0706", "0813", "0816", "0810", "0814",
        "0903", "0906", "0913", "0916", "07025", "07026", "0704"
    )

    private val GLO_PREFIXES = listOf(
        "0805", "0807", "0705", "0815", "0811", "0905", "0915"
    )

    private val AIRTEL_PREFIXES = listOf(
        "0802", "0808", "0708", "0812", "0701", "0902", "0901", "0904", "0907", "0912"
    )

    private val NINEMOBILE_PREFIXES = listOf(
        "0809", "0817", "0818", "0909", "0908"
    )

    /**
     * Normalizes any input phone number into standard 11-digit local format: "0803XXXXXXX".
     */
    fun normalizePhoneNumber(rawPhone: String): String {
        var clean = rawPhone.replace(Regex("[^0-9+]"), "").trim()
        if (clean.startsWith("+234")) {
            clean = "0" + clean.substring(4)
        } else if (clean.startsWith("234") && clean.length >= 13) {
            clean = "0" + clean.substring(3)
        } else if (clean.length == 10 && (clean.startsWith("8") || clean.startsWith("7") || clean.startsWith("9"))) {
            clean = "0$clean"
        }
        return clean
    }

    /**
     * Detects the carrier from a phone number string.
     * Returns "MTN", "GLO", "AIRTEL", "9MOBILE", or null if undetermined.
     */
    fun detectCarrier(phone: String): String? {
        val clean = normalizePhoneNumber(phone)
        if (clean.length < 4) return null

        // Check 5-digit prefixes first (e.g. 07025, 07026)
        if (clean.length >= 5) {
            val prefix5 = clean.substring(0, 5)
            if (MTN_PREFIXES.contains(prefix5)) return "MTN"
        }

        val prefix4 = clean.substring(0, 4)
        return when {
            MTN_PREFIXES.contains(prefix4) -> "MTN"
            GLO_PREFIXES.contains(prefix4) -> "GLO"
            AIRTEL_PREFIXES.contains(prefix4) -> "AIRTEL"
            NINEMOBILE_PREFIXES.contains(prefix4) -> "9MOBILE"
            else -> null
        }
    }

    /**
     * Checks if the user's selected network matches the carrier detected from the phone number.
     * Returns true if there is a definite mismatch.
     */
    fun isMismatch(selectedNetwork: String, phone: String): Boolean {
        val detected = detectCarrier(phone) ?: return false
        val selectedNormalized = normalizeNetworkSlug(selectedNetwork)
        return !selectedNormalized.equals(normalizeNetworkSlug(detected), ignoreCase = true)
    }

    /**
     * Standardizes network strings into canonical lowercase tokens: "mtn", "glo", "airtel", "9mobile".
     */
    fun normalizeNetworkSlug(network: String): String {
        val lower = network.lowercase().trim()
        return when {
            lower.contains("mtn") -> "mtn"
            lower.contains("glo") -> "glo"
            lower.contains("airtel") -> "airtel"
            lower.contains("9mobile") || lower.contains("etisalat") -> "9mobile"
            else -> lower
        }
    }

    /**
     * Formats user-friendly display name: "MTN", "GLO", "Airtel", "9mobile".
     */
    fun getDisplayName(network: String): String {
        return when (normalizeNetworkSlug(network)) {
            "mtn" -> "MTN"
            "glo" -> "GLO"
            "airtel" -> "Airtel"
            "9mobile" -> "9mobile"
            else -> network.uppercase()
        }
    }

    /**
     * Returns theme color associated with the carrier.
     */
    fun getCarrierColor(network: String): Color {
        return when (normalizeNetworkSlug(network)) {
            "mtn" -> Color(0xFFFFCC00)
            "glo" -> Color(0xFF00A859)
            "airtel" -> Color(0xFFFF0000)
            "9mobile" -> Color(0xFF006848)
            else -> Color(0xFF00E5FF)
        }
    }

    /**
     * Returns warning description if there is a mismatch.
     */
    fun getMismatchWarning(selectedNetwork: String, phone: String): String? {
        val detected = detectCarrier(phone) ?: return false.let { null }
        if (isMismatch(selectedNetwork, phone)) {
            val selectedName = getDisplayName(selectedNetwork)
            val detectedName = getDisplayName(detected)
            return "Recipient line ($phone) is $detectedName, but $selectedName is currently selected! Dispatches will fail or be rejected."
        }
        return null
    }
}
