package com.example.data.model

/**
 * Detailed verification information for electricity meters and utility accounts.
 * Required for utility verification and official proof of residence.
 */
data class MeterVerificationResult(
    val isValid: Boolean = false,
    val customerName: String = "",
    val meterNumber: String = "",
    val serviceAddress: String = "",
    val discoName: String = "",
    val meterType: String = "PREPAID",
    val tariffClass: String = "",
    val accountCode: String = "",
    val message: String = ""
)
