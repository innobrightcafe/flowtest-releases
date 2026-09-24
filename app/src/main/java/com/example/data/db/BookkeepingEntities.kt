package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users_wallets")
data class UserWalletEntity(
    @PrimaryKey val id: String = "usr_default_1",
    val email: String = "",
    val phoneNumber: String = "",
    val appWalletBalance: Double = 0.0,
    val assignedBank: String = "Moniepoint MFB",
    val assignedAccountNumber: String = "6666468328",
    val assignedAccountName: String = "FlowTest",
    val activeConfirmationCode: String = "FT01"
)

@Entity(tableName = "pricing_configs")
data class PricingConfigEntity(
    @PrimaryKey val id: String, // e.g. "data_mtn_sme_1gb", "airtime_mtn", "utility_ikedc", "cable_dstv", "sms_default"
    val serviceType: String, // "data", "airtime", "electricity", "cable_tv", "sms", "vpn"
    val providerId: String, // "mtn", "airtel", "glo", "9mobile", "dstv", "gotv", "ikedc", "ekedc", "httpsms"
    val planId: String? = null, // e.g. "mtn_sme_1gb", null for non-data
    val wholesaleCost: Double = 0.0, // used for Data packages (e.g., 230.0) or SMS cost (e.g. 2.50)
    val userRetailPrice: Double = 0.0, // used for Data packages (e.g., 280.0) or SMS price (e.g. 4.50)
    val percentageMarkup: Double = 0.0, // used for airtime/utilities (e.g., 1.5%)
    val flatConvenienceFee: Double = 0.0 // used for cable/utilities (e.g., 100.0)
)

@Entity(tableName = "client_virtual_accounts")
data class ClientAccountEntity(
    @PrimaryKey val id: String, // e.g. "acc_10928"
    val customerName: String,
    val customerEmail: String,
    val customerPhone: String,
    val bankName: String = "Moniepoint MFB",
    val accountNumber: String = "6666468328",
    val accountName: String = "FlowTest",
    val reference: String,
    val totalFunded: Double = 0.0,
    val status: String = "ACTIVE", // "ACTIVE", "SUSPENDED"
    val role: String = "USER", // "USER", "ADMIN"
    val walletBalance: Double = 0.0,
    val userPin: String = "1234",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "transactions_bookkeeping")
data class TransactionBookkeepingEntity(
    @PrimaryKey val id: String, // e.g. "TX-89201"
    val userId: String = "usr_default_1",
    val transactionType: String, // "wallet_deposit", "data_purchase", "airtime_vending", "utility_bill", "cable_subscription", "sms_broadcast", "vpn_subscription"
    val serviceCategory: String, // "Data", "Airtime", "Utilities", "Cable TV", "SMS", "VPN", "Deposit"
    val recipientOrAccount: String,
    val amountDebitedFromUser: Double,
    val amountPaidToWholesaleApi: Double,
    val netProfitEarned: Double, // Formula: amountDebitedFromUser - amountPaidToWholesaleApi
    val status: String, // "pending", "success", "failed"
    val timestamp: Long = System.currentTimeMillis(),
    val reference: String,
    val confirmationSource: String = "PAIRGATE API",
    val completedAtFormatted: String = ""
)

/**
 * Pending Orders linked to customer phone number for narration-based bank payment matching.
 */
@Entity(tableName = "pending_orders")
data class PendingOrderEntity(
    @PrimaryKey val id: String, // e.g. "ORD-109283"
    val phoneNumber: String, // Cleaned 11-digit string e.g. "08168290134"
    val customerName: String = "Valued Customer",
    val serviceType: String, // "data", "airtime", "utility", "cable_tv", "wallet_funding", "vpn"
    val network: String = "", // "MTN", "AIRTEL", "GLO", "9MOBILE"
    val planId: String = "",
    val planName: String = "",
    val retailPrice: Double = 0.0,
    val wholesaleCost: Double = 0.0,
    val narrationCode: String, // The 11-digit phone number the customer MUST put in bank narration
    val status: String = "pending", // "pending", "completed", "cancelled", "underpaid"
    val bankTransactionRef: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Idempotency Tracker: Ensures no duplicate execution of the same bank transaction reference.
 */
@Entity(tableName = "processed_payments")
data class ProcessedPaymentEntity(
    @PrimaryKey val reference: String, // Bank transaction reference (Idempotency key)
    val amount: Double,
    val rawNarration: String,
    val extractedPhone: String? = null,
    val senderName: String? = null,
    val status: String = "completed", // "completed", "unresolved", "underpaid", "duplicate"
    val orderId: String? = null,
    val resolvedAt: Long = System.currentTimeMillis()
)

/**
 * Admin Unresolved Payments Tracker: Captures bank transfers with missing/invalid narration,
 * underpayment, or unmatched orders so the admin can resolve manually via Dashboard.
 */
@Entity(tableName = "unresolved_payments")
data class UnresolvedPaymentEntity(
    @PrimaryKey val id: String, // e.g. "UNRES-10928"
    val bankReference: String,
    val amount: Double,
    val rawNarration: String,
    val senderName: String = "Bank Customer",
    val detectedPhone: String? = null,
    val failureReason: String, // "Missing/Invalid Phone in Narration", "No Matching Pending Order", "Underpayment Detected"
    val status: String = "UNRESOLVED", // "UNRESOLVED", "RESOLVED_MANUAL_CREDIT", "RESOLVED_ORDER_PUSHED", "REFUNDED"
    val resolutionNotes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Saved Beneficiary / Recipient for one-tap repeat transfers and utility top-ups.
 */
@Entity(tableName = "saved_recipients")
data class SavedRecipientEntity(
    @PrimaryKey val id: String, // e.g. "REC-91823"
    val name: String, // e.g. "Sunday Okafor", "Mom Airtime", "Home Prepaid Meter"
    val recipientType: String, // "transfer", "airtime", "data", "utility", "cable", "betting"
    val identifier: String, // 10-digit NUBAN, phone number, meter number, smartcard number
    val institutionOrProvider: String, // "Moniepoint", "Kuda", "MTN", "Airtel", "IKEDC", "DSTV"
    val bankAccountName: String? = null, // e.g. "SUNDAY OKAFOR"
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Detailed Request & Reconciliation Audit Log for Inbound Notifications:
 * Moniepoint Webhooks, Pairgate Webhooks, and Gmail Credit Alerts.
 */
@Entity(tableName = "inbound_notification_audit_logs")
data class InboundNotificationAuditLogEntity(
    @PrimaryKey val id: String, // e.g. "INB-109283"
    val source: String, // "MONIEPOINT_WEBHOOK", "PAIRGATE_WEBHOOK", "GMAIL_NOTIFICATION"
    val eventType: String = "PAYMENT_SUCCESSFUL",
    val reference: String,
    val amount: Double,
    val rawPayload: String,
    val parsedSender: String,
    val parsedNarration: String,
    val detectedConfirmationCode: String? = null,
    val detectedPhone: String? = null,
    val signatureVerified: Boolean = true,
    val signatureDetails: String = "VERIFIED_OK",
    val reconciliationStatus: String, // "RECONCILED_WALLET", "RECONCILED_ORDER", "DUPLICATE", "UNMATCHED", "FAILED_SIGNATURE", "TEST_SIMULATION"
    val matchedUserId: String? = null,
    val balanceBefore: Double = 0.0,
    val balanceAfter: Double = 0.0,
    val reconciliationNotes: String,
    val timestamp: Long = System.currentTimeMillis(),
    val completedAtFormatted: String = ""
)

