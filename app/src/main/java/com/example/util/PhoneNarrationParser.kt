package com.example.util

import java.util.Locale

sealed class NarrationIdentifier {
    data class ConfirmationCode(val code: String) : NarrationIdentifier()
    data class PhoneNumber(val phone: String) : NarrationIdentifier()
    data object None : NarrationIdentifier()
}

/**
 * Utility for parsing bank transfer narrations / remarks / descriptions.
 * Extracts dynamic auto-generated confirmation codes (e.g. "FT-8492", "FT01") as well as sender phone numbers.
 */
object PhoneNarrationParser {

    /**
     * Generates or rotates a collision-resistant confirmation code (e.g. "FT-8492" or "FT8492")
     */
    fun generateRotatedCode(currentCode: String? = null, counter: Int? = null): String {
        if (counter != null && counter > 0) {
            val num = ((counter - 1) % 9000) + 1000
            return String.format(Locale.US, "FT-%04d", num)
        }
        val randomNum = (1000..9999).random()
        return String.format(Locale.US, "FT-%04d", randomNum)
    }

    /**
     * Extracts a confirmation code (e.g. "FT-8492", "FT8492", "FT 8492", "FT01", "8492") from bank transfer narration.
     */
    fun extractConfirmationCode(narration: String?): String? {
        if (narration.isNullOrBlank()) return null

        // 1. Match FT followed by 2 to 6 alphanumeric chars/digits with optional separator (e.g. FT-8492, FT8492, FT 8492, FT_8492, FT01)
        val regex = Regex("""\bFT[-_\s]?([0-9A-Z]{2,6})\b""", RegexOption.IGNORE_CASE)
        val match = regex.find(narration)
        if (match != null) {
            val suffix = match.groupValues[1].uppercase()
            return "FT-$suffix"
        }

        // 2. Check compact string without spaces/symbols for FT prefix
        val cleaned = narration.replace(Regex("""[\s\-_/\\.:,;*#]"""), "").uppercase()
        val compactMatch = Regex("""FT([0-9A-Z]{2,6})""").find(cleaned)
        if (compactMatch != null) {
            return "FT-${compactMatch.groupValues[1]}"
        }

        // 3. Match PIN, CODE, REF, or TX followed by 4-to-6 digit numerical code (e.g. "PIN 8492", "CODE: 8492", "REF 49201")
        val pinMatch = Regex("""\b(?:PIN|CODE|REF|TX|ID)[-:\s]+([0-9]{4,6})\b""", RegexOption.IGNORE_CASE).find(narration)
        if (pinMatch != null) {
            return "FT-${pinMatch.groupValues[1]}"
        }

        // 4. Match standalone 4-to-6 digit numerical code in narration/remarks
        val standaloneMatch = Regex("""\b([0-9]{4,6})\b""").find(narration)
        if (standaloneMatch != null) {
            return "FT-${standaloneMatch.groupValues[1]}"
        }

        return null
    }

    /**
     * Extracts strictly the Confirmation Code or Phone Number from narration.
     */
    fun extractIdentifierFromNarration(narration: String?): NarrationIdentifier {
        if (narration.isNullOrBlank()) return NarrationIdentifier.None

        // 1. Check for FT Confirmation Code first
        val confCode = extractConfirmationCode(narration)
        if (confCode != null) {
            return NarrationIdentifier.ConfirmationCode(confCode)
        }

        // 2. Check for 11-digit or 10-digit Nigerian Phone Number
        val phoneMatch = Regex("""\b(0[789][01]\d{8}|234[789][01]\d{8}|[789][01]\d{8})\b""").find(narration)
        if (phoneMatch != null) {
            val norm = normalizePhoneNumber(phoneMatch.value)
            if (norm != null) {
                return NarrationIdentifier.PhoneNumber(norm)
            }
        }

        return NarrationIdentifier.None
    }

    /**
     * Checks if a narration matches a given confirmation code or phone number.
     */
    fun narrationMatchesTarget(narration: String?, targetCode: String?, targetPhone: String?): Boolean {
        if (narration.isNullOrBlank()) return false
        val cleanNarr = narration.uppercase()
        val cleanNarrNoHyphen = cleanNarr.replace("-", "").replace(" ", "")

        if (!targetCode.isNullOrBlank()) {
            val cleanCode = targetCode.trim().uppercase()
            val codeNoHyphen = cleanCode.replace("-", "").replace(" ", "")
            val codeDigits = cleanCode.replace("[^0-9]".toRegex(), "")

            if (cleanNarr.contains(cleanCode) || cleanNarrNoHyphen.contains(codeNoHyphen)) return true
            if (codeDigits.length >= 4 && cleanNarr.contains(codeDigits)) return true
        }

        if (!targetPhone.isNullOrBlank()) {
            val normPhone = normalizePhoneNumber(targetPhone)
            if (normPhone != null) {
                val last10 = if (normPhone.length == 11) normPhone.substring(1) else normPhone
                if (cleanNarr.contains(normPhone) || cleanNarr.contains(last10)) return true
            }
        }

        return false
    }

    /**
     * Normalizes any Nigerian phone format to standard 11 digits: "08031234567"
     */
    fun normalizePhoneNumber(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val digitsOnly = raw.replace(Regex("[^0-9]"), "")

        // Case 1: 13 digits starting with 234 (e.g. 2348031234567)
        if (digitsOnly.length == 13 && digitsOnly.startsWith("234")) {
            return "0" + digitsOnly.substring(3)
        }

        // Case 2: 11 digits starting with 070, 080, 081, 090, 091, 071, etc.
        if (digitsOnly.length == 11 && digitsOnly.startsWith("0")) {
            return digitsOnly
        }

        // Case 3: 10 digits missing leading 0 (e.g. 8031234567, 8168290134, 7012345678, 9012345678)
        if (digitsOnly.length == 10 && (digitsOnly.startsWith("7") || digitsOnly.startsWith("8") || digitsOnly.startsWith("9"))) {
            return "0$digitsOnly"
        }

        // Case 4: 14 digits with 00234
        if (digitsOnly.length == 14 && digitsOnly.startsWith("00234")) {
            return "0" + digitsOnly.substring(5)
        }

        return if (digitsOnly.length >= 11) digitsOnly.take(11) else null
    }
}
