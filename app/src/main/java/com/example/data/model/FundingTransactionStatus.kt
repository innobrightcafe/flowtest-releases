package com.example.data.model

/**
 * Reactive status state for the Wallet Funding flow.
 * Transitions between Idle -> Verifying -> Confirmed or Failed based on webhook & bank alert reconciliation.
 */
sealed class FundingTransactionStatus {
    object Idle : FundingTransactionStatus()
    object Verifying : FundingTransactionStatus()

    data class Confirmed(
        val amount: Double,
        val reference: String,
        val source: String = "MONIEPOINT WEBHOOK",
        val newBalance: Double = 0.0,
        val message: String = "Transfer Verified & Credited",
        val confirmationCode: String = "",
        val timestamp: Long = System.currentTimeMillis()
    ) : FundingTransactionStatus()

    data class Failed(
        val reason: String,
        val timestamp: Long = System.currentTimeMillis()
    ) : FundingTransactionStatus()
}
