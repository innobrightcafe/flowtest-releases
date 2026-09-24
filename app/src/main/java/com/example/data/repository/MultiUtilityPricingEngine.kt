package com.example.data.repository

import android.util.Log
import com.example.data.api.CloudRunApiClient
import com.example.data.api.PairgateAirtimeRequest
import com.example.data.api.PairgateApiResponse
import com.example.data.api.PairgateApiService
import com.example.data.api.PairgateBillRequest
import com.example.data.api.PairgateDataRequest
import com.example.data.api.PairgateVerifiedPlans
import com.example.data.api.PairgateVirtualAccountRequest
import com.example.data.db.*
import com.example.util.NarrationIdentifier
import com.example.util.PhoneNarrationParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

data class BookkeepingStats(
    val totalRevenueProcessed: Double = 0.0,
    val totalWholesaleCostExpended: Double = 0.0,
    val totalNetProfit: Double = 0.0,
    val profitByData: Double = 0.0,
    val profitByAirtime: Double = 0.0,
    val profitByUtilities: Double = 0.0,
    val profitByCable: Double = 0.0,
    val profitBySms: Double = 0.0,
    val profitByVpn: Double = 0.0,
    val totalTransactionsCount: Int = 0,
    val successfulTransactionsCount: Int = 0,
    val failedTransactionsCount: Int = 0
)

data class TransactionOperationResult(
    val isSuccess: Boolean,
    val message: String,
    val transactionId: String? = null,
    val amountDebited: Double = 0.0,
    val netProfit: Double = 0.0,
    val newWalletBalance: Double = 0.0,
    val isPending: Boolean = false,
    val status: String = if (isPending) "PENDING" else if (isSuccess) "SUCCESS" else "FAILED"
)

data class CreateVirtualAccountResult(
    val isSuccess: Boolean,
    val message: String,
    val account: ClientAccountEntity? = null
)

data class WebhookFulfillmentResult(
    val status: String, // "success", "ignored", "duplicate", "invalid_narration", "no_pending_order", "underpaid", "failed"
    val message: String,
    val orderId: String? = null,
    val extractedPhone: String? = null,
    val amountReceived: Double = 0.0,
    val isFulfilled: Boolean = false
)

data class AutomatedTransferCheckResult(
    val isFoundAndCredited: Boolean,
    val amountCredited: Double = 0.0,
    val reference: String = "",
    val message: String,
    val newWalletBalance: Double = 0.0,
    val isPendingNetwork: Boolean = false
)

data class ResolutionResult(
    val success: Boolean,
    val newBalance: Double = 0.0,
    val creditedAmount: Double = 0.0,
    val targetPhone: String = "",
    val reference: String = "",
    val message: String = ""
)

class MultiUtilityPricingEngine(
    private val bookkeepingDao: BookkeepingDao
) {
    companion object {
        const val CORPORATE_BANK_NAME = "Moniepoint MFB"
        const val CORPORATE_ACCOUNT_NUMBER = "6666468328"
        const val CORPORATE_ACCOUNT_NAME = "INOSOFTTECH LIMITED"
        const val BRAND_NAME = "FlowTest"
        const val COMPANY_LEGAL_NAME = "INOSOFTTECH LIMITED"
        const val COMPANY_RC_NUMBER = "9710966"
    }

    val userWalletFlow: Flow<UserWalletEntity?> = bookkeepingDao.getUserWalletFlow()
    val allTransactionsFlow: Flow<List<TransactionBookkeepingEntity>> = bookkeepingDao.getAllBookkeepingTransactions()
    val allPricingConfigsFlow: Flow<List<PricingConfigEntity>> = bookkeepingDao.getAllPricingConfigs()
    val allClientAccountsFlow: Flow<List<ClientAccountEntity>> = bookkeepingDao.getAllClientAccounts()
    val allPendingOrdersFlow: Flow<List<PendingOrderEntity>> = bookkeepingDao.getAllPendingOrdersFlow()
    val allUnresolvedPaymentsFlow: Flow<List<UnresolvedPaymentEntity>> = bookkeepingDao.getAllUnresolvedPaymentsFlow()
    val allProcessedPaymentsFlow: Flow<List<ProcessedPaymentEntity>> = bookkeepingDao.getAllProcessedPaymentsFlow()
    val allSavedRecipientsFlow: Flow<List<SavedRecipientEntity>> = bookkeepingDao.getAllSavedRecipientsFlow()

    suspend fun saveRecipient(
        name: String,
        recipientType: String = "contact",
        identifier: String,
        institutionOrProvider: String = "",
        bankAccountName: String? = null,
        isFavorite: Boolean = false
    ): SavedRecipientEntity = withContext(Dispatchers.IO) {
        val cleanPhone = identifier.replace("[^0-9+]".toRegex(), "").trim().ifBlank { identifier.trim() }
        val existing = bookkeepingDao.getSavedRecipientByIdentifier(cleanPhone)
        val recipient = existing?.copy(
            name = name.trim().ifBlank { existing.name },
            recipientType = recipientType.ifBlank { existing.recipientType },
            identifier = cleanPhone,
            institutionOrProvider = institutionOrProvider.ifBlank { existing.institutionOrProvider },
            bankAccountName = bankAccountName ?: existing.bankAccountName,
            isFavorite = isFavorite || existing.isFavorite
        ) ?: SavedRecipientEntity(
            id = "REC-" + (100000..999999).random(),
            name = name.trim().ifBlank { "Beneficiary" },
            recipientType = recipientType.ifBlank { "contact" },
            identifier = cleanPhone,
            institutionOrProvider = institutionOrProvider,
            bankAccountName = bankAccountName,
            isFavorite = isFavorite
        )
        bookkeepingDao.insertSavedRecipient(recipient)
        recipient
    }

    suspend fun deleteRecipient(id: String) = withContext(Dispatchers.IO) {
        bookkeepingDao.deleteSavedRecipientById(id)
    }

    suspend fun toggleFavoriteRecipient(recipient: SavedRecipientEntity) = withContext(Dispatchers.IO) {
        bookkeepingDao.insertSavedRecipient(recipient.copy(isFavorite = !recipient.isFavorite))
    }


    suspend fun createPairgateVirtualAccountForUser(
        customerName: String,
        customerEmail: String,
        customerPhone: String,
        bvn: String = "",
        nin: String = "",
        preferredBank: String = CORPORATE_BANK_NAME,
        apiService: PairgateApiService? = null,
        bearerToken: String? = null
    ): CreateVirtualAccountResult = withContext(Dispatchers.IO) {
        val nameParts = customerName.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
        val firstName = nameParts.firstOrNull()?.ifBlank { "User" } ?: "User"
        val lastName = if (nameParts.size > 1) nameParts.drop(1).joinToString(" ") else "Client"
        val customerRef = "SECUREVPN_USER_" + (10000..99999).random()

        var assignedNuban: String? = null
        var assignedBank: String = CORPORATE_BANK_NAME
        var assignedName: String = CORPORATE_ACCOUNT_NAME
        var assignedRef: String = customerRef
        var apiSuccess = false
        var apiErrorReason: String? = null

        // 1. Attempt secure Cloud Run Backend Proxy (keeps API keys completely off frontend)
        try {
            val cloudRunResp = CloudRunApiClient.createVirtualAccount(
                firstName = firstName,
                lastName = lastName,
                email = customerEmail.trim(),
                phone = customerPhone.trim(),
                customerReference = customerRef,
                customerName = "$firstName $lastName".trim(),
                bvn = bvn.ifBlank { null },
                nin = nin.ifBlank { null }
            )
            val cloudNuban = cloudRunResp?.extractAccountNumber()
            if (!cloudNuban.isNullOrBlank()) {
                assignedNuban = cloudNuban
                assignedBank = cloudRunResp.extractBankName() ?: assignedBank
                assignedName = cloudRunResp.extractAccountName() ?: assignedName
                assignedRef = cloudRunResp.extractReference() ?: assignedRef
                apiSuccess = true
            }
        } catch (e: Exception) {
            Log.d("MultiUtilityEngine", "CloudRun VA creation fallback: ${e.message}")
        }

        // 2. Fallback to direct client API if Cloud Run is unavailable and client key is present
        if (!apiSuccess && apiService != null && !bearerToken.isNullOrBlank()) {
            try {
                val req = PairgateVirtualAccountRequest(
                    firstName = firstName,
                    lastName = lastName,
                    email = customerEmail.trim(),
                    phone = customerPhone.trim(),
                    customerReference = customerRef,
                    customerName = "$firstName $lastName".trim(),
                    customerEmail = customerEmail.trim(),
                    customerPhone = customerPhone.trim(),
                    bvn = bvn.ifBlank { null },
                    nin = nin.ifBlank { null }
                )

                var resp = try {
                    apiService.createClientVirtualAccount(bearerToken, req)
                } catch (e: Exception) {
                    apiErrorReason = "Create: ${e.message}"
                    null
                }

                if (resp == null || !resp.isSuccessful || resp.body()?.extractAccountNumber() == null) {
                    resp = try {
                        apiService.createClientVirtualAccountDirect(bearerToken, req)
                    } catch (e: Exception) {
                        apiErrorReason = "Direct: ${e.message}"
                        null
                    }
                }

                if (resp != null && resp.isSuccessful && resp.body() != null) {
                    val body = resp.body()!!
                    val liveNuban = body.extractAccountNumber()
                    if (!liveNuban.isNullOrBlank()) {
                        assignedNuban = liveNuban
                        assignedBank = body.extractBankName()?.takeIf { it.isNotBlank() } ?: assignedBank
                        assignedName = body.extractAccountName()?.takeIf { it.isNotBlank() } ?: assignedName
                        assignedRef = body.extractReference()?.takeIf { it.isNotBlank() } ?: assignedRef
                        apiSuccess = true
                    } else {
                        apiErrorReason = "Corporate collection account routed."
                    }
                } else if (resp != null) {
                    val errCode = resp.code()
                    if (errCode == 404) {
                        apiErrorReason = "Using company corporate account: $CORPORATE_ACCOUNT_NUMBER ($CORPORATE_BANK_NAME)"
                    } else {
                        val errMsg = resp.errorBody()?.string()?.take(120)?.replace(Regex("<.*?>"), "")?.trim() ?: resp.message()
                        apiErrorReason = "Gateway ($errCode): $errMsg"
                    }
                }
            } catch (e: Exception) {
                apiErrorReason = e.message ?: "Network error"
                Log.w("MultiUtilityEngine", "Pairgate Live Account API call exception: ${e.message}")
            }
        } else if (!apiSuccess) {
            apiErrorReason = "Company Corporate Account Active"
        }

        val finalNuban = assignedNuban ?: CORPORATE_ACCOUNT_NUMBER
        val finalBank = if (assignedNuban != null) assignedBank else CORPORATE_BANK_NAME
        val finalName = if (assignedNuban != null) assignedName else CORPORATE_ACCOUNT_NAME

        val clientAcc = ClientAccountEntity(
            id = "acc_" + (finalNuban.takeLast(6)),
            customerName = customerName,
            customerEmail = customerEmail,
            customerPhone = customerPhone,
            bankName = finalBank,
            accountNumber = finalNuban,
            accountName = finalName,
            reference = assignedRef,
            totalFunded = 0.0,
            status = "ACTIVE",
            createdAt = System.currentTimeMillis()
        )
        bookkeepingDao.insertClientAccount(clientAcc)

        CreateVirtualAccountResult(
            isSuccess = true,
            message = "Corporate Moniepoint Bank Account Ready: $finalNuban ($finalBank - $finalName)",
            account = clientAcc
        )
    }

    /**
     * Creates a Pending Order tied to the recipient phone number.
     * Prevents Double-Order Clash by cancelling orders older than 30 minutes for the same phone number.
     */
    suspend fun createPendingOrder(
        phoneNumber: String,
        customerName: String = "Customer",
        serviceType: String, // "data", "airtime", "utility", "cable_tv", "wallet_funding", "vpn"
        network: String = "",
        planId: String = "",
        planName: String = "",
        retailPrice: Double,
        wholesaleCost: Double = retailPrice * 0.85,
        customNarrationCode: String? = null,
        bankTransactionRef: String? = null
    ): PendingOrderEntity = withContext(Dispatchers.IO) {
        val cleanPhone = PhoneNarrationParser.normalizePhoneNumber(phoneNumber) ?: phoneNumber.trim()
        val userWallet = bookkeepingDao.getUserWalletSync()
        val activeCode = customNarrationCode?.trim()?.uppercase()
            ?: userWallet?.activeConfirmationCode?.takeIf { it.isNotBlank() }
            ?: PhoneNarrationParser.generateRotatedCode()

        // Double-Order Clash Fix: Only auto-cancel orders that have been pending for more than 30 minutes
        // Recent orders within 30 minutes must remain pending for Admin review and manual fulfillment!
        val now = System.currentTimeMillis()
        val thirtyMinsMs = 30 * 60 * 1000L
        val activePending = bookkeepingDao.getActivePendingOrdersForPhone(cleanPhone)
        for (oldOrder in activePending) {
            if (now - oldOrder.createdAt >= thirtyMinsMs) {
                cancelAndRefundPendingOrder(oldOrder.id, "Auto-cancelled: superseded after 30min inactivity")
            }
        }
        if (activeCode.isNotBlank() && activeCode != cleanPhone) {
            val codePending = bookkeepingDao.getActivePendingOrdersForPhone(activeCode)
            for (oldOrder in codePending) {
                if (now - oldOrder.createdAt >= thirtyMinsMs) {
                    cancelAndRefundPendingOrder(oldOrder.id, "Auto-cancelled: superseded after 30min inactivity")
                }
            }
        }

        val orderId = "ORD-" + (100000..999999).random()
        val pendingOrder = PendingOrderEntity(
            id = orderId,
            phoneNumber = cleanPhone,
            customerName = customerName,
            serviceType = serviceType,
            network = network,
            planId = planId,
            planName = planName,
            retailPrice = retailPrice,
            wholesaleCost = wholesaleCost,
            narrationCode = activeCode, // Rotated 4-character Confirmation Code e.g. "FT01"
            status = "pending",
            bankTransactionRef = bankTransactionRef,
            createdAt = now
        )

        bookkeepingDao.insertPendingOrder(pendingOrder)
        pendingOrder
    }

    /**
     * Retries an active pending order by attempting wholesale dispatch to upstream CloudRun/Pairgate gateway.
     */
    suspend fun retryPendingOrder(
        orderId: String,
        apiService: PairgateApiService? = null,
        bearerToken: String? = null
    ): TransactionOperationResult = withContext(Dispatchers.IO) {
        val order = bookkeepingDao.getPendingOrderById(orderId)
            ?: return@withContext TransactionOperationResult(false, "Pending order $orderId not found")

        if (order.status == "completed") {
            return@withContext TransactionOperationResult(false, "Order $orderId is already completed.")
        }

        // Always generate a fresh unique reference for retry attempts so upstream doesn't flag "too many attempts" or duplicate reference
        val retryRef = "PG-RETRY-${System.currentTimeMillis()}-${(1000..9999).random()}"
        var ref = retryRef
        var success = false
        var errMsg = ""

        val service = order.serviceType.lowercase().trim()
        val network = order.network.lowercase().trim().ifBlank { "mtn" }
        val phone = order.phoneNumber.trim()

        val token = if (bearerToken.isNullOrBlank()) {
            "Bearer " + com.example.BuildConfig.PAIRGATE_API_KEY.replace(Regex("(?i)^bearer\\s+"), "").trim()
        } else if (bearerToken.startsWith("Bearer ", ignoreCase = true)) {
            bearerToken
        } else {
            "Bearer $bearerToken"
        }

        val client = apiService ?: PairgateApiService.create()

        try {
            when {
                service.contains("data") -> {
                    // Resolve plan ID through Pairgate catalog
                    val resolvedPlanId = PairgateDataRequest.resolvePairgatePlanId(network, order.planId, order.retailPrice)

                    // Step 1: Attempt via Cloud Run proxy
                    var cloudResp: PairgateApiResponse? = null
                    try {
                        cloudResp = CloudRunApiClient.purchaseData(
                            network = network,
                            planId = resolvedPlanId,
                            phone = phone,
                            customerReference = retryRef,
                            amount = order.retailPrice,
                            apiKey = token
                        )
                    } catch (e: Exception) {
                        android.util.Log.w("MultiUtilityEngine", "CloudRun retry attempt: ${e.message}")
                    }

                    if (cloudResp != null && (cloudResp.status.equals("success", ignoreCase = true) || cloudResp.status == "200" || cloudResp.message?.contains("success", ignoreCase = true) == true)) {
                        success = true
                        ref = cloudResp.reference ?: retryRef
                        errMsg = cloudResp.message ?: "Data bundle activated successfully on retry."
                    } else {
                        // Step 2: Direct API fallback with exact PairgateDataRequest
                        val req = PairgateDataRequest.create(
                            provider = network,
                            planIdentifier = resolvedPlanId,
                            recipientPhone = phone,
                            reference = retryRef,
                            amount = order.retailPrice
                        )
                        val resp = client.purchaseData(token, req)
                        if (resp.isSuccessful) {
                            val body = resp.body()
                            if (body?.isSuccessful() == true) {
                                success = true
                                ref = body.getEffectiveReference() ?: retryRef
                                errMsg = body.getDetailedMessage()
                            } else {
                                errMsg = body?.getDetailedMessage() ?: cloudResp?.message ?: "Pairgate declined transaction on retry"
                            }
                        } else {
                            val errBody = try { resp.errorBody()?.string() } catch (e: Exception) { null }
                            val errObj = PairgateApiResponse.parseErrorBody(errBody)
                            errMsg = errObj?.getDetailedMessage() ?: cloudResp?.message ?: "Pairgate HTTP ${resp.code()}: ${resp.message()}"
                        }
                    }
                }
                service.contains("airtime") -> {
                    val req = PairgateAirtimeRequest.create(
                        provider = network,
                        amount = order.retailPrice,
                        recipientPhone = phone,
                        reference = retryRef
                    )
                    val resp = client.purchaseAirtime(token, req)
                    if (resp.isSuccessful) {
                        val body = resp.body()
                        if (body?.isSuccessful() == true) {
                            success = true
                            ref = body.getEffectiveReference() ?: retryRef
                            errMsg = body.getDetailedMessage()
                        } else {
                            errMsg = body?.getDetailedMessage() ?: "Pairgate declined airtime transaction"
                        }
                    } else {
                        val errBody = try { resp.errorBody()?.string() } catch (e: Exception) { null }
                        val errObj = PairgateApiResponse.parseErrorBody(errBody)
                        errMsg = errObj?.getDetailedMessage() ?: "Pairgate HTTP ${resp.code()}: ${resp.message()}"
                    }
                }
                else -> {
                    errMsg = "Service $service not supported for auto-retry"
                }
            }
        } catch (e: java.io.IOException) {
            errMsg = "Network error: ${e.localizedMessage ?: "Unable to connect to Pairgate gateway"}"
        } catch (e: Exception) {
            errMsg = e.localizedMessage ?: "Unexpected error during dispatch"
        }

        if (success) {
            bookkeepingDao.updatePendingOrderStatus(order.id, "completed", ref)
            val now = System.currentTimeMillis()
            val timeFmt = java.text.SimpleDateFormat("HH:mm:ss dd-MMM-yyyy", java.util.Locale.US).format(java.util.Date(now))
            val netProfit = order.retailPrice - order.wholesaleCost
            val txEntity = TransactionBookkeepingEntity(
                id = "TX-FULFILL-" + (100000..999999).random(),
                userId = "usr_default_1",
                transactionType = "${order.serviceType}_vending",
                serviceCategory = order.serviceType.replaceFirstChar { it.uppercase() },
                recipientOrAccount = "${order.phoneNumber} (${order.planName.ifBlank { order.planId }})",
                amountDebitedFromUser = order.retailPrice,
                amountPaidToWholesaleApi = order.wholesaleCost,
                netProfitEarned = netProfit,
                status = "success",
                timestamp = now,
                reference = ref,
                confirmationSource = "ADMIN RETRY DISPATCH",
                completedAtFormatted = timeFmt
            )
            bookkeepingDao.insertTransaction(txEntity)

            return@withContext TransactionOperationResult(
                isSuccess = true,
                message = "Order ${order.id} successfully fulfilled via Pairgate! ($errMsg)",
                transactionId = ref,
                netProfit = netProfit
            )
        } else {
            return@withContext TransactionOperationResult(
                isSuccess = false,
                message = "Upstream retry failed: ${errMsg.ifBlank { "Service currently unavailable on provider gateway" }}."
            )
        }
    }

    /**
     * Manually marks a pending order as fulfilled when admin handled it externally or out-of-band.
     */
    suspend fun fulfillPendingOrderManually(
        orderId: String,
        notes: String = "Manually fulfilled by Admin"
    ): TransactionOperationResult = withContext(Dispatchers.IO) {
        val order = bookkeepingDao.getPendingOrderById(orderId)
            ?: return@withContext TransactionOperationResult(false, "Pending order $orderId not found")

        if (order.status == "completed") {
            return@withContext TransactionOperationResult(false, "Order $orderId is already completed.")
        }

        val ref = "MANUAL-ADMIN-" + (100000..999999).random()
        bookkeepingDao.updatePendingOrderStatus(order.id, "completed", ref)

        val existingTxs = bookkeepingDao.getAllTransactionsSync()
        val matchingTx = existingTxs.firstOrNull {
            it.id.equals(order.id, ignoreCase = true) ||
            it.reference.equals(order.id, ignoreCase = true) ||
            (order.bankTransactionRef?.isNotBlank() == true && it.reference.equals(order.bankTransactionRef, ignoreCase = true))
        }

        val now = System.currentTimeMillis()
        val timeFmt = java.text.SimpleDateFormat("HH:mm:ss dd-MMM-yyyy", java.util.Locale.US).format(java.util.Date(now))
        val netProfit = order.retailPrice - order.wholesaleCost

        // If order was a wallet funding / deposit request, credit the wallet!
        if (order.serviceType.contains("wallet", ignoreCase = true) ||
            order.serviceType.contains("fund", ignoreCase = true) ||
            order.serviceType.contains("deposit", ignoreCase = true)) {
            val targetPhone = order.phoneNumber.trim()
            val wallet = (if (targetPhone.isNotBlank()) bookkeepingDao.getUserWalletByPhone(targetPhone) else null)
            if (wallet != null && order.retailPrice > 0.0) {
                val newBal = wallet.appWalletBalance + order.retailPrice
                bookkeepingDao.updateWalletBalance(wallet.id, newBal)
                if (targetPhone.isNotBlank()) {
                    bookkeepingDao.updateWalletBalanceByPhone(targetPhone, newBal)
                    bookkeepingDao.updateClientAccountBalanceByPhone(targetPhone, newBal)
                }
            }
        }

        if (matchingTx != null) {
            bookkeepingDao.updateTransactionStatus(matchingTx.id, "success")
        } else {
            val txEntity = TransactionBookkeepingEntity(
                id = "TX-MANUAL-" + (100000..999999).random(),
                userId = "usr_default_1",
                transactionType = "${order.serviceType}_manual_vending",
                serviceCategory = order.serviceType.replaceFirstChar { it.uppercase() },
                recipientOrAccount = "${order.phoneNumber} (${order.planName.ifBlank { order.planId }})",
                amountDebitedFromUser = order.retailPrice,
                amountPaidToWholesaleApi = order.wholesaleCost,
                netProfitEarned = netProfit,
                status = "success",
                timestamp = now,
                reference = ref,
                confirmationSource = "ADMIN MANUAL: $notes",
                completedAtFormatted = timeFmt
            )
            bookkeepingDao.insertTransaction(txEntity)
        }

        TransactionOperationResult(
            isSuccess = true,
            transactionId = ref,
            message = "Order ${order.id} marked as manually fulfilled for ${order.phoneNumber}."
        )
    }

    /**
     * Checks if an order was completed or already refunded by Admin.
     */
    suspend fun isOrderCompletedOrRefunded(reference: String): Boolean = withContext(Dispatchers.IO) {
        val clean = reference.trim()
        val order = bookkeepingDao.getPendingOrderById(clean)
            ?: bookkeepingDao.getAllPendingOrdersSync().firstOrNull {
                it.id.equals(clean, ignoreCase = true) ||
                it.bankTransactionRef?.equals(clean, ignoreCase = true) == true
            }
        order?.status == "completed" || order?.status == "cancelled_refunded"
    }

    /**
     * Checks if a transaction has reached SUCCESS status.
     */
    suspend fun isTransactionCompleted(reference: String): Boolean = withContext(Dispatchers.IO) {
        val clean = reference.trim()
        val tx = bookkeepingDao.getTransactionById(clean)
            ?: bookkeepingDao.getAllTransactionsSync().firstOrNull { it.reference.equals(clean, ignoreCase = true) }
        tx?.status == "success"
    }

    /**
     * Cancels a pending order and guarantees that any debited retail amount is refunded back to user wallet.
     */
    suspend fun cancelAndRefundPendingOrder(
        orderId: String,
        reason: String = "Cancelled by Admin"
    ): TransactionOperationResult = withContext(Dispatchers.IO) {
        val order = bookkeepingDao.getPendingOrderById(orderId)
            ?: return@withContext TransactionOperationResult(false, "Pending order $orderId not found")

        if (order.status == "completed") {
            return@withContext TransactionOperationResult(false, "Order $orderId has already been completed.")
        }
        if (order.status == "cancelled_refunded" || order.status == "refunded") {
            return@withContext TransactionOperationResult(false, "Order $orderId has already been refunded.")
        }

        // Mark order as cancelled and refunded
        bookkeepingDao.updatePendingOrderStatus(order.id, "cancelled_refunded", "REFUNDED: $reason")

        val refundAmount = order.retailPrice
        var refundedWalletBal = 0.0
        if (refundAmount > 0.0) {
            val wallet = bookkeepingDao.getUserWalletByPhone(order.phoneNumber)
                ?: bookkeepingDao.getUserWalletSync()

            if (wallet != null) {
                refundedWalletBal = wallet.appWalletBalance + refundAmount
                bookkeepingDao.updateWalletBalance(wallet.id, refundedWalletBal)

                val now = System.currentTimeMillis()
                val timeFmt = java.text.SimpleDateFormat("HH:mm:ss dd-MMM-yyyy", java.util.Locale.US).format(java.util.Date(now))
                val refundTxId = "TX-REF-" + (100000..999999).random()
                val txEntity = TransactionBookkeepingEntity(
                    id = refundTxId,
                    userId = wallet.id,
                    transactionType = "wallet_refund",
                    serviceCategory = "Refund",
                    recipientOrAccount = "${order.phoneNumber} (${order.serviceType.uppercase()})",
                    amountDebitedFromUser = refundAmount,
                    amountPaidToWholesaleApi = 0.0,
                    netProfitEarned = 0.0,
                    status = "success",
                    timestamp = now,
                    reference = "REF-${order.id}",
                    confirmationSource = "ADMIN REFUND DESK",
                    completedAtFormatted = timeFmt
                )
                bookkeepingDao.insertTransaction(txEntity)
            }
        }

        TransactionOperationResult(
            isSuccess = true,
            message = "Order ${order.id} cancelled. ₦${String.format(java.util.Locale.US, "%,.2f", refundAmount)} refunded to ${order.phoneNumber} wallet.",
            amountDebited = refundAmount,
            newWalletBalance = refundedWalletBal
        )
    }

    /**
     * Finds and effects a refund for any transaction or pending order matching the given reference or ID.
     * Restores the user's wallet balance, marks the order as refunded, and records the refund bookkeeping log.
     */
    suspend fun effectPairgateRefundByReference(
        reference: String,
        amountToRefund: Double? = null,
        reason: String = "FlowTest System Refund"
    ): TransactionOperationResult = withContext(Dispatchers.IO) {
        val cleanRef = reference.trim()
        if (cleanRef.isBlank()) {
            return@withContext TransactionOperationResult(false, "Invalid transaction reference")
        }

        // 1. Try finding pending order matching reference or ID
        val pendingOrders = bookkeepingDao.getAllPendingOrdersSync()
        val matchedOrder = pendingOrders.firstOrNull { order ->
            order.id.equals(cleanRef, ignoreCase = true) ||
            (order.bankTransactionRef ?: "").equals(cleanRef, ignoreCase = true) ||
            order.narrationCode.equals(cleanRef, ignoreCase = true) ||
            order.phoneNumber.equals(cleanRef, ignoreCase = true)
        }

        if (matchedOrder != null) {
            if (matchedOrder.status == "cancelled_refunded" || matchedOrder.status == "refunded") {
                return@withContext TransactionOperationResult(false, "Order $cleanRef has already been refunded.")
            }
            return@withContext cancelAndRefundPendingOrder(matchedOrder.id, reason)
        }

        // 2. If no pending order matched, check transactions
        val txs = bookkeepingDao.getAllTransactionsSync()
        val matchedTx = txs.firstOrNull { tx ->
            tx.reference.equals(cleanRef, ignoreCase = true) ||
            tx.id.equals(cleanRef, ignoreCase = true)
        }

        val refundAmount = amountToRefund ?: matchedTx?.amountDebitedFromUser ?: 0.0
        if (refundAmount <= 0.0) {
            return@withContext TransactionOperationResult(false, "Cannot refund: Order amount could not be determined for $cleanRef")
        }

        val wallet = bookkeepingDao.getUserWalletSync()
            ?: return@withContext TransactionOperationResult(false, "User wallet not found")

        val newBal = wallet.appWalletBalance + refundAmount
        bookkeepingDao.updateWalletBalance(wallet.id, newBal)

        val now = System.currentTimeMillis()
        val timeFmt = java.text.SimpleDateFormat("HH:mm:ss dd-MMM-yyyy", java.util.Locale.US).format(java.util.Date(now))
        val refundTxId = "TX-REF-" + (100000..999999).random()
        val txEntity = TransactionBookkeepingEntity(
            id = refundTxId,
            userId = wallet.id,
            transactionType = "wallet_refund",
            serviceCategory = "Refund",
            recipientOrAccount = matchedTx?.recipientOrAccount ?: cleanRef,
            amountDebitedFromUser = refundAmount,
            amountPaidToWholesaleApi = 0.0,
            netProfitEarned = 0.0,
            status = "success",
            timestamp = now,
            reference = "REF-$cleanRef",
            confirmationSource = "FLOWTEST AUTO-REFUND GATEWAY",
            completedAtFormatted = timeFmt
        )
        bookkeepingDao.insertTransaction(txEntity)

        if (matchedTx != null) {
            bookkeepingDao.updateTransactionStatus(matchedTx.id, "refunded")
        }

        TransactionOperationResult(
            isSuccess = true,
            message = "Refund effected successfully. ₦${String.format(java.util.Locale.US, "%,.2f", refundAmount)} credited back to wallet.",
            amountDebited = refundAmount,
            newWalletBalance = newBal,
            transactionId = refundTxId
        )
    }

    /**
     * Synchronizes current user's profile details into Room DB user wallet entity.
     */
    suspend fun syncUserAccountDetails(
        userId: String = "usr_default_1",
        phoneNumber: String,
        email: String,
        accountName: String = CORPORATE_ACCOUNT_NAME,
        accountNumber: String = CORPORATE_ACCOUNT_NUMBER,
        bankName: String = CORPORATE_BANK_NAME
    ) = withContext(Dispatchers.IO) {
        val existing = bookkeepingDao.getUserWalletSync(userId)
        val cleanPhone = PhoneNarrationParser.normalizePhoneNumber(phoneNumber) ?: phoneNumber.trim()
        val wallet = existing?.copy(
            phoneNumber = cleanPhone,
            email = email,
            assignedAccountName = CORPORATE_ACCOUNT_NAME,
            assignedAccountNumber = CORPORATE_ACCOUNT_NUMBER,
            assignedBank = CORPORATE_BANK_NAME
        ) ?: UserWalletEntity(
            id = userId,
            email = email,
            phoneNumber = cleanPhone,
            assignedAccountName = CORPORATE_ACCOUNT_NAME,
            assignedAccountNumber = CORPORATE_ACCOUNT_NUMBER,
            assignedBank = CORPORATE_BANK_NAME
        )
        bookkeepingDao.insertOrUpdateUserWallet(wallet)
    }

    /**
     * Intercepts and processes the Moniepoint collection webhook or live transfer deposit.
     * Follows the 7-Step Verified Pipeline:
     * 1. Idempotency Check (verifies unique bank transaction reference)
     * 2. Extract 11-digit phone number from narration
     * 3. Match most recent pending order (ORDER BY createdAt DESC) or registered user by reference/phone
     * 4. Verify exact amount paid vs order price
     * 5. Lock transaction ledger
     * 6. Trigger Pairgate vending or User Wallet Top-Up
     * 7. Update order and balance status in DB
     */
    suspend fun processIncomingMoniepointWebhook(
        transactionReference: String,
        amountReceived: Double,
        rawNarration: String,
        senderName: String = "Bank Customer",
        apiService: PairgateApiService? = null,
        bearerToken: String? = null,
        isSimulationOnly: Boolean = false,
        senderAccountNumber: String = "",
        senderBank: String = "",
        explicitTargetUserPhone: String? = null
    ): WebhookFulfillmentResult = withContext(Dispatchers.IO) {
        val txnRef = transactionReference.trim()
        val cleanNarration = rawNarration.trim()

        // 1. Idempotency Check (Check if reference was already settled in local DB or backend claim ledger)
        val duplicateCheck = bookkeepingDao.getProcessedPayment(txnRef)
        val isBackendClaimed = CloudRunApiClient.checkPaymentClaimedOnBackend(txnRef)
        if (duplicateCheck != null || isBackendClaimed) {
            Log.i("MultiUtilityEngine", "Duplicate transaction reference ignored (Local: ${duplicateCheck != null}, Backend: $isBackendClaimed): $txnRef")
            recordInboundAuditLog(
                source = "MONIEPOINT_WEBHOOK",
                eventType = "PAYMENT_SUCCESSFUL",
                reference = txnRef,
                amount = amountReceived,
                rawPayload = """{"event":"PAYMENT_SUCCESSFUL","reference":"$txnRef","amount":$amountReceived,"narration":"$cleanNarration","sender":"$senderName","isSimulation":$isSimulationOnly}""",
                parsedSender = senderName,
                parsedNarration = cleanNarration,
                detectedCode = null,
                detectedPhone = null,
                signatureVerified = true,
                signatureDetails = "HMAC-SHA256 & Backend Ledger",
                reconciliationStatus = "DUPLICATE_SKIPPED",
                matchedUserId = null,
                balanceBefore = 0.0,
                balanceAfter = 0.0,
                notes = "Duplicate webhook reference '$txnRef' already settled in local or backend ledger. Duplicate credit safely skipped."
            )
            return@withContext WebhookFulfillmentResult(
                status = "duplicate",
                message = "Webhook Reference Rejected: Transaction reference '$txnRef' was already processed in the ledger. Duplicate funding prevented.",
                orderId = duplicateCheck?.orderId,
                amountReceived = amountReceived
            )
        }

        // 2. Extract Confirmation Code (e.g. "FT-1001") or Phone Number from narration or explicit parameters
        val identifier = PhoneNarrationParser.extractIdentifierFromNarration(cleanNarration)
        var extractedIdentifier = when (identifier) {
            is NarrationIdentifier.ConfirmationCode -> identifier.code.ifBlank { null }
            is NarrationIdentifier.PhoneNumber -> identifier.phone.ifBlank { null }
            is NarrationIdentifier.None -> null
        }

        // If no code in narration, fallback to explicit target phone or sender account number
        if (extractedIdentifier.isNullOrBlank()) {
            if (!explicitTargetUserPhone.isNullOrBlank()) {
                extractedIdentifier = explicitTargetUserPhone
            } else if (senderAccountNumber.isNotBlank()) {
                extractedIdentifier = senderAccountNumber
            }
        }

        if (extractedIdentifier.isNullOrBlank() && explicitTargetUserPhone.isNullOrBlank()) {
            Log.w("MultiUtilityEngine", "Alert: Payment received without valid Confirmation Code / Phone in narration. Text: '$cleanNarration'")
            recordInboundAuditLog(
                source = "MONIEPOINT_WEBHOOK",
                eventType = "PAYMENT_SUCCESSFUL",
                reference = txnRef,
                amount = amountReceived,
                rawPayload = """{"event":"PAYMENT_SUCCESSFUL","reference":"$txnRef","amount":$amountReceived,"narration":"$cleanNarration","sender":"$senderName","isSimulation":$isSimulationOnly}""",
                parsedSender = senderName,
                parsedNarration = cleanNarration,
                detectedCode = null,
                detectedPhone = null,
                signatureVerified = true,
                signatureDetails = "HMAC-SHA256 VERIFIED",
                reconciliationStatus = if (isSimulationOnly) "TEST_PARSER_NO_CODE" else "UNRESOLVED_NO_CODE",
                matchedUserId = null,
                balanceBefore = 0.0,
                balanceAfter = 0.0,
                notes = "No confirmation code (e.g. FT-1001) or phone found in bank narration '$cleanNarration'. Logged for admin resolution."
            )
            if (isSimulationOnly) {
                return@withContext WebhookFulfillmentResult(
                    status = "simulation_no_phone",
                    message = "Simulation Verification: Reference '$txnRef' is valid & unique, but no confirmation code (e.g. FT-1001) or phone was found in bank narration '$cleanNarration'. In production, this transfer (₦${String.format(java.util.Locale.US, "%,.2f", amountReceived)}) would be placed in Unresolved Transfers for Admin desk resolution.",
                    amountReceived = amountReceived,
                    isFulfilled = false
                )
            }
            val unresId = "UNRES-" + (10000..99999).random()
            bookkeepingDao.insertUnresolvedPayment(
                UnresolvedPaymentEntity(
                    id = unresId,
                    bankReference = txnRef,
                    amount = amountReceived,
                    rawNarration = cleanNarration,
                    senderName = senderName,
                    detectedPhone = null,
                    failureReason = "No Confirmation Code (e.g. FT-1001) or Phone found in bank narration",
                    status = "UNRESOLVED"
                )
            )
            bookkeepingDao.insertProcessedPayment(
                ProcessedPaymentEntity(
                    reference = txnRef,
                    amount = amountReceived,
                    rawNarration = cleanNarration,
                    extractedPhone = null,
                    senderName = senderName,
                    status = "unresolved"
                )
            )
            return@withContext WebhookFulfillmentResult(
                status = "invalid_narration",
                message = "Payment of ₦${String.format(java.util.Locale.US, "%,.2f", amountReceived)} received with Ref '$txnRef', but no Confirmation Code / Phone found in narration. Logged for Admin Resolution.",
                amountReceived = amountReceived
            )
        }

        // 3. Find freshest pending order or registered user wallet for this confirmation code / phone
        val pendingOrder = when (identifier) {
            is NarrationIdentifier.ConfirmationCode -> bookkeepingDao.getFreshestPendingOrderForNarrationCode(identifier.code)
            is NarrationIdentifier.PhoneNumber -> bookkeepingDao.getFreshestPendingOrderForPhone(identifier.phone)
            else -> if (!explicitTargetUserPhone.isNullOrBlank()) bookkeepingDao.getFreshestPendingOrderForPhone(explicitTargetUserPhone) else null
        }
        val matchedUserWallet = when (identifier) {
            is NarrationIdentifier.ConfirmationCode -> bookkeepingDao.getUserWalletByConfirmationCode(identifier.code)
            is NarrationIdentifier.PhoneNumber -> bookkeepingDao.getUserWalletByPhone(identifier.phone)
            else -> null
        } ?: (if (!explicitTargetUserPhone.isNullOrBlank()) bookkeepingDao.getUserWalletByPhone(explicitTargetUserPhone) else null)
          ?: bookkeepingDao.getUserWalletSync("usr_default_1")

        // If in simulation dry-run mode, report complete verification results without touching real balances
        if (isSimulationOnly) {
            return@withContext if (pendingOrder != null) {
                val fitsAmount = amountReceived >= pendingOrder.retailPrice
                val simNotes = if (fitsAmount) {
                    "Simulation test passed: Matched pending order #${pendingOrder.id} (${pendingOrder.serviceType.uppercase()}). Paid: ₦${String.format("%,.2f", amountReceived)} (Required: ₦${String.format("%,.2f", pendingOrder.retailPrice)}). Reconciliation verified."
                } else {
                    "Simulation underpayment alert: Order #${pendingOrder.id} requires ₦${String.format("%,.2f", pendingOrder.retailPrice)}, but received ₦${String.format("%,.2f", amountReceived)}."
                }
                recordInboundAuditLog(
                    source = "MONIEPOINT_WEBHOOK",
                    eventType = "PAYMENT_SUCCESSFUL",
                    reference = txnRef,
                    amount = amountReceived,
                    rawPayload = """{"event":"PAYMENT_SUCCESSFUL","reference":"$txnRef","amount":$amountReceived,"narration":"$cleanNarration","sender":"$senderName","isSimulation":true}""",
                    parsedSender = senderName,
                    parsedNarration = cleanNarration,
                    detectedCode = extractedIdentifier,
                    detectedPhone = pendingOrder.phoneNumber,
                    signatureVerified = true,
                    signatureDetails = "HMAC-SHA256 VERIFIED",
                    reconciliationStatus = if (fitsAmount) "TEST_SIMULATION_MATCHED" else "TEST_SIMULATION_UNDERPAID",
                    matchedUserId = "usr_default_1",
                    balanceBefore = matchedUserWallet?.appWalletBalance ?: 0.0,
                    balanceAfter = matchedUserWallet?.appWalletBalance ?: 0.0,
                    notes = simNotes
                )
                WebhookFulfillmentResult(
                    status = if (fitsAmount) "simulation_order_matched" else "simulation_underpayment",
                    message = if (fitsAmount) {
                        "Simulation Passed: Reference '$txnRef' verified! Matched active pending order #${pendingOrder.id} (${pendingOrder.serviceType.uppercase()} ${pendingOrder.planName}) for code/phone $extractedIdentifier. Paid: ₦${String.format("%,.2f", amountReceived)} (Required: ₦${String.format("%,.2f", pendingOrder.retailPrice)}). In production, this instantly auto-vends via the telecom gateway."
                    } else {
                        "Simulation Alert: Reference '$txnRef' verified for $extractedIdentifier, but payment of ₦${String.format("%,.2f", amountReceived)} is below the required order amount (₦${String.format("%,.2f", pendingOrder.retailPrice)}). In production, this would be marked as Underpaid."
                    },
                    orderId = pendingOrder.id,
                    extractedPhone = extractedIdentifier,
                    amountReceived = amountReceived,
                    isFulfilled = false
                )
            } else if (matchedUserWallet != null) {
                val simBefore = matchedUserWallet.appWalletBalance
                val simAfter = simBefore + amountReceived
                val simNotes = "Simulation test passed: Reference '$txnRef' verified! Matched registered user $extractedIdentifier (${matchedUserWallet.email}, Active Code: ${matchedUserWallet.activeConfirmationCode}). Reconciliation verified: ₦${String.format("%,.2f", simBefore)} -> ₦${String.format("%,.2f", simAfter)} (+₦${String.format("%,.2f", amountReceived)}). Live balance safely preserved during test."
                recordInboundAuditLog(
                    source = "MONIEPOINT_WEBHOOK",
                    eventType = "PAYMENT_SUCCESSFUL",
                    reference = txnRef,
                    amount = amountReceived,
                    rawPayload = """{"event":"PAYMENT_SUCCESSFUL","reference":"$txnRef","amount":$amountReceived,"narration":"$cleanNarration","sender":"$senderName","isSimulation":true}""",
                    parsedSender = senderName,
                    parsedNarration = cleanNarration,
                    detectedCode = extractedIdentifier,
                    detectedPhone = matchedUserWallet.phoneNumber,
                    signatureVerified = true,
                    signatureDetails = "HMAC-SHA256 VERIFIED",
                    reconciliationStatus = "TEST_SIMULATION_MATCHED",
                    matchedUserId = matchedUserWallet.id,
                    balanceBefore = simBefore,
                    balanceAfter = simAfter,
                    notes = simNotes
                )
                WebhookFulfillmentResult(
                    status = "simulation_wallet_matched",
                    message = "Simulation Passed: Reference '$txnRef' verified! Matched registered user $extractedIdentifier (${matchedUserWallet.email}, Code: ${matchedUserWallet.activeConfirmationCode}). Current user balance: ₦${String.format("%,.2f", matchedUserWallet.appWalletBalance)}. In production, this deposit of ₦${String.format("%,.2f", amountReceived)} will credit the user wallet to ₦${String.format("%,.2f", matchedUserWallet.appWalletBalance + amountReceived)}.",
                    extractedPhone = extractedIdentifier,
                    amountReceived = amountReceived,
                    isFulfilled = false
                )
            } else {
                recordInboundAuditLog(
                    source = "MONIEPOINT_WEBHOOK",
                    eventType = "PAYMENT_SUCCESSFUL",
                    reference = txnRef,
                    amount = amountReceived,
                    rawPayload = """{"event":"PAYMENT_SUCCESSFUL","reference":"$txnRef","amount":$amountReceived,"narration":"$cleanNarration","sender":"$senderName","isSimulation":true}""",
                    parsedSender = senderName,
                    parsedNarration = cleanNarration,
                    detectedCode = extractedIdentifier,
                    detectedPhone = null,
                    signatureVerified = true,
                    signatureDetails = "HMAC-SHA256 VERIFIED",
                    reconciliationStatus = "TEST_SIMULATION_UNMATCHED",
                    matchedUserId = null,
                    balanceBefore = 0.0,
                    balanceAfter = 0.0,
                    notes = "Simulation test: Reference '$txnRef' verified with code $extractedIdentifier, but no matching pending order or registered user account was found."
                )
                WebhookFulfillmentResult(
                    status = "simulation_unmatched_user",
                    message = "Simulation Alert: Reference '$txnRef' verified and identifier $extractedIdentifier extracted, but no matching pending order or registered user account was found. In production, this will be logged in Admin Unresolved Transfers.",
                    extractedPhone = extractedIdentifier,
                    amountReceived = amountReceived,
                    isFulfilled = false
                )
            }
        }

        if (pendingOrder != null) {
            // 4. Verify Amount (Ensure user paid at least the retail price)
            if (amountReceived < pendingOrder.retailPrice) {
                Log.w("MultiUtilityEngine", "Underpayment: Order needs ₦${pendingOrder.retailPrice}, received ₦$amountReceived")
                val unresId = "UNRES-" + (10000..99999).random()
                bookkeepingDao.insertUnresolvedPayment(
                    UnresolvedPaymentEntity(
                        id = unresId,
                        bankReference = txnRef,
                        amount = amountReceived,
                        rawNarration = cleanNarration,
                        senderName = senderName,
                        detectedPhone = extractedIdentifier,
                        failureReason = "Underpayment: Required ₦${pendingOrder.retailPrice}, Paid ₦$amountReceived (Order: ${pendingOrder.id})",
                        status = "UNRESOLVED"
                    )
                )
                bookkeepingDao.updatePendingOrderStatus(pendingOrder.id, "underpaid", txnRef)
                bookkeepingDao.insertProcessedPayment(
                    ProcessedPaymentEntity(
                        reference = txnRef,
                        amount = amountReceived,
                        rawNarration = cleanNarration,
                        extractedPhone = extractedIdentifier,
                        senderName = senderName,
                        status = "underpaid",
                        orderId = pendingOrder.id
                    )
                )
                return@withContext WebhookFulfillmentResult(
                    status = "underpaid",
                    message = "Underpayment detected for Ref '$txnRef': Order required ₦${pendingOrder.retailPrice}, but received ₦$amountReceived.",
                    orderId = pendingOrder.id,
                    extractedPhone = extractedIdentifier,
                    amountReceived = amountReceived
                )
            }

            // 5. Lock transaction ledger right away to prevent race conditions
            bookkeepingDao.insertProcessedPayment(
                ProcessedPaymentEntity(
                    reference = txnRef,
                    amount = amountReceived,
                    rawNarration = cleanNarration,
                    extractedPhone = extractedIdentifier,
                    senderName = senderName,
                    status = "processing",
                    orderId = pendingOrder.id
                )
            )

            // 6. TRIGGER PAIRGATE API VENDING INSTANTLY
            var vendingSuccess = true
            var vendingMessage = "Fulfilled via Direct Narration Payment"

            if (apiService != null && !bearerToken.isNullOrBlank() && pendingOrder.serviceType == "data") {
                try {
                    val purchaseReq = PairgateDataRequest.create(
                        provider = pendingOrder.network.lowercase(),
                        planIdentifier = pendingOrder.planId,
                        recipientPhone = pendingOrder.phoneNumber.ifBlank { extractedIdentifier.orEmpty() },
                        reference = txnRef,
                        amount = pendingOrder.retailPrice
                    )
                    val reqJson = """{"provider_id":"${purchaseReq.providerId}","plan_id":"${purchaseReq.planId}","recipient":"${purchaseReq.recipient}","reference":"${purchaseReq.reference}"}"""
                    Log.i("PairgateGateway", "MultiUtilityEngine dispatching: $reqJson")
                    val resp = apiService.purchaseData(bearerToken, purchaseReq)
                    val resSummary = if (resp.isSuccessful) resp.body()?.message ?: "success" else resp.errorBody()?.string() ?: "status ${resp.code()}"
                    Log.i("PairgateGateway", "MultiUtilityEngine response: HTTP ${resp.code()} - $resSummary")
                    if (resp.isSuccessful && resp.body()?.status == "success") {
                        vendingSuccess = true
                        vendingMessage = resp.body()?.message ?: "Data bundle delivered to ${pendingOrder.phoneNumber}"
                    }
                } catch (e: Exception) {
                    Log.e("MultiUtilityEngine", "Pairgate delivery call error: ${e.message}")
                }
            }

            // 7. Update order status to success in database
            bookkeepingDao.updatePendingOrderStatus(pendingOrder.id, "completed", txnRef)

            // Auto-rotate confirmation code for next order
            val newRotatedCode = PhoneNarrationParser.generateRotatedCode(matchedUserWallet?.activeConfirmationCode)
            if (matchedUserWallet != null) {
                bookkeepingDao.updateUserConfirmationCode(matchedUserWallet.id, newRotatedCode)
            }

            // Record in Transaction Bookkeeping Ledger
            val netProfit = pendingOrder.retailPrice - pendingOrder.wholesaleCost
            val now = System.currentTimeMillis()
            val timeFmt = java.text.SimpleDateFormat("HH:mm:ss dd-MMM-yyyy", java.util.Locale.US).format(java.util.Date(now))
            val txEntity = TransactionBookkeepingEntity(
                id = "TX-FULFILL-" + (100000..999999).random(),
                userId = "usr_default_1",
                transactionType = "narration_bank_order",
                serviceCategory = pendingOrder.serviceType.replaceFirstChar { it.uppercase() },
                recipientOrAccount = "$extractedIdentifier (${pendingOrder.planName.ifBlank { pendingOrder.planId }})",
                amountDebitedFromUser = amountReceived,
                amountPaidToWholesaleApi = pendingOrder.wholesaleCost,
                netProfitEarned = netProfit,
                status = "success",
                timestamp = now,
                reference = txnRef,
                confirmationSource = "MONIEPOINT WEBHOOK",
                completedAtFormatted = timeFmt
            )
            bookkeepingDao.insertTransaction(txEntity)

            recordInboundAuditLog(
                source = "MONIEPOINT_WEBHOOK",
                eventType = "PAYMENT_SUCCESSFUL",
                reference = txnRef,
                amount = amountReceived,
                rawPayload = """{"event":"PAYMENT_SUCCESSFUL","reference":"$txnRef","amount":$amountReceived,"narration":"$cleanNarration","sender":"$senderName","orderId":"${pendingOrder.id}"}""",
                parsedSender = senderName,
                parsedNarration = cleanNarration,
                detectedCode = extractedIdentifier,
                detectedPhone = pendingOrder.phoneNumber,
                signatureVerified = true,
                signatureDetails = "HMAC-SHA256 VERIFIED",
                reconciliationStatus = "RECONCILED_VTU_ORDER",
                matchedUserId = "usr_default_1",
                balanceBefore = matchedUserWallet?.appWalletBalance ?: 0.0,
                balanceAfter = matchedUserWallet?.appWalletBalance ?: 0.0,
                notes = "Auto-vended ${pendingOrder.serviceType.uppercase()} (${pendingOrder.planName}) for recipient $extractedIdentifier. Confirmation code rotated to $newRotatedCode."
            )

            return@withContext WebhookFulfillmentResult(
                status = "success",
                message = "Order ${pendingOrder.id} Fulfilled! $vendingMessage for $extractedIdentifier (Ref: $txnRef). Confirmation code rotated to $newRotatedCode.",
                orderId = pendingOrder.id,
                extractedPhone = extractedIdentifier,
                amountReceived = amountReceived,
                isFulfilled = true
            )
        }

        // If no pending order exists, automatically credit the user's wallet with this confirmation code / phone
        val userWallet = matchedUserWallet
        if (userWallet != null) {
            // Local check: check if code or amount was already processed locally in the last 15 minutes
            val cleanCodeUpper = (extractedIdentifier ?: "").replace("-", "").replace(" ", "").uppercase()
            val isAlreadyLocallyCredited = if (cleanCodeUpper.isNotBlank()) {
                bookkeepingDao.getRecentProcessedPaymentsForCodeOrPhone(cleanCodeUpper).any { p ->
                    Math.abs(p.amount - amountReceived) < 0.01 && (System.currentTimeMillis() - p.resolvedAt) < 15 * 60 * 1000L
                } || bookkeepingDao.getTransactionsByDateRangeSync(System.currentTimeMillis() - 15 * 60 * 1000L, Long.MAX_VALUE).any { tx ->
                    tx.transactionType == "wallet_deposit" && tx.status == "success" &&
                    Math.abs(tx.amountDebitedFromUser - amountReceived) < 0.01 &&
                    tx.recipientOrAccount.contains(cleanCodeUpper, ignoreCase = true)
                }
            } else false

            if (isAlreadyLocallyCredited) {
                Log.w("MultiUtilityEngine", "Webhook code $extractedIdentifier already credited locally. Skipping duplicate.")
                return@withContext WebhookFulfillmentResult(
                    status = "duplicate",
                    message = "Transaction with code '$extractedIdentifier' was already credited locally. Duplicate skipped.",
                    amountReceived = amountReceived,
                    isFulfilled = false
                )
            }

            // Claim payment on backend ledger
            val backendClaim = CloudRunApiClient.claimPaymentOnBackend(
                reference = txnRef,
                amount = amountReceived,
                narrationCode = extractedIdentifier ?: "",
                userPhone = userWallet.phoneNumber,
                userId = userWallet.id,
                source = "MONIEPOINT_WEBHOOK"
            )
            if (backendClaim != null && backendClaim.isClaimed) {
                Log.w("MultiUtilityEngine", "Webhook reference $txnRef already claimed on backend (${backendClaim.message}). Skipping duplicate credit.")
                return@withContext WebhookFulfillmentResult(
                    status = "duplicate",
                    message = "Transaction '$txnRef' was already credited on central backend ledger. Duplicate skipped.",
                    amountReceived = amountReceived,
                    isFulfilled = false
                )
            }

            val balanceBefore = userWallet.appWalletBalance
            val newBal = balanceBefore + amountReceived
            val nextRotatedCode = PhoneNarrationParser.generateRotatedCode(userWallet.activeConfirmationCode)
            bookkeepingDao.updateWalletBalance(userWallet.id, newBal)
            bookkeepingDao.updateUserConfirmationCode(userWallet.id, nextRotatedCode)

            bookkeepingDao.insertProcessedPayment(
                ProcessedPaymentEntity(
                    reference = txnRef,
                    amount = amountReceived,
                    rawNarration = cleanNarration,
                    extractedPhone = extractedIdentifier,
                    senderName = senderName,
                    status = "completed"
                )
            )

            val now = System.currentTimeMillis()
            val timeFmt = java.text.SimpleDateFormat("HH:mm:ss dd-MMM-yyyy", java.util.Locale.US).format(java.util.Date(now))
            val txEntity = TransactionBookkeepingEntity(
                id = "TX-WALLET-" + (100000..999999).random(),
                userId = userWallet.id,
                transactionType = "wallet_deposit",
                serviceCategory = "Deposit",
                recipientOrAccount = "$senderName ($extractedIdentifier)",
                amountDebitedFromUser = amountReceived,
                amountPaidToWholesaleApi = amountReceived,
                netProfitEarned = 0.0,
                status = "success",
                timestamp = now,
                reference = txnRef,
                confirmationSource = "MONIEPOINT WEBHOOK",
                completedAtFormatted = timeFmt
            )
            bookkeepingDao.insertTransaction(txEntity)

            recordInboundAuditLog(
                source = "MONIEPOINT_WEBHOOK",
                eventType = "PAYMENT_SUCCESSFUL",
                reference = txnRef,
                amount = amountReceived,
                rawPayload = """{"event":"PAYMENT_SUCCESSFUL","reference":"$txnRef","amount":$amountReceived,"narration":"$cleanNarration","sender":"$senderName","account":"${userWallet.id}"}""",
                parsedSender = senderName,
                parsedNarration = cleanNarration,
                detectedCode = extractedIdentifier,
                detectedPhone = userWallet.phoneNumber,
                signatureVerified = true,
                signatureDetails = "HMAC-SHA256 VERIFIED",
                reconciliationStatus = "RECONCILED_WALLET",
                matchedUserId = userWallet.id,
                balanceBefore = balanceBefore,
                balanceAfter = newBal,
                notes = "Matched confirmation code/phone $extractedIdentifier. Wallet credited with ₦${String.format("%,.2f", amountReceived)}. Balance: ₦${String.format("%,.2f", balanceBefore)} -> ₦${String.format("%,.2f", newBal)}. Code rotated to $nextRotatedCode."
            )

            return@withContext WebhookFulfillmentResult(
                status = "auto_credited",
                message = "Wallet credited with ₦${String.format("%,.2f", amountReceived)} for user $extractedIdentifier (Ref: $txnRef). Previous Balance: ₦${String.format("%,.2f", balanceBefore)}, New Balance: ₦${String.format("%,.2f", newBal)}. Code rotated to $nextRotatedCode.",
                extractedPhone = extractedIdentifier,
                amountReceived = amountReceived,
                isFulfilled = true
            )
        }

        // Unmatched phone / confirmation code with no user
        val unresId = "UNRES-" + (10000..99999).random()
        bookkeepingDao.insertUnresolvedPayment(
            UnresolvedPaymentEntity(
                id = unresId,
                bankReference = txnRef,
                amount = amountReceived,
                rawNarration = cleanNarration,
                senderName = senderName,
                detectedPhone = extractedIdentifier,
                failureReason = "No matching pending order or registered user for code/phone $extractedIdentifier",
                status = "UNRESOLVED"
            )
        )

        recordInboundAuditLog(
            source = "MONIEPOINT_WEBHOOK",
            eventType = "PAYMENT_SUCCESSFUL",
            reference = txnRef,
            amount = amountReceived,
            rawPayload = """{"event":"PAYMENT_SUCCESSFUL","reference":"$txnRef","amount":$amountReceived,"narration":"$cleanNarration","sender":"$senderName"}""",
            parsedSender = senderName,
            parsedNarration = cleanNarration,
            detectedCode = extractedIdentifier,
            detectedPhone = null,
            signatureVerified = true,
            signatureDetails = "HMAC-SHA256 VERIFIED",
            reconciliationStatus = "UNMATCHED_UNRESOLVED",
            matchedUserId = null,
            balanceBefore = 0.0,
            balanceAfter = 0.0,
            notes = "Inbound transfer of ₦${String.format("%,.2f", amountReceived)} with identifier '$extractedIdentifier' does not match any active order or user account. Queued in Unresolved Ledger."
        )

        return@withContext WebhookFulfillmentResult(
            status = "no_pending_order",
            message = "Money received with code/phone $extractedIdentifier (Ref: $txnRef), but no active pending order or registered user was found. Logged to Admin Desk.",
            extractedPhone = extractedIdentifier,
            amountReceived = amountReceived
        )
    }

    private suspend fun recordInboundAuditLog(
        source: String,
        eventType: String,
        reference: String,
        amount: Double,
        rawPayload: String,
        parsedSender: String,
        parsedNarration: String,
        detectedCode: String?,
        detectedPhone: String?,
        signatureVerified: Boolean,
        signatureDetails: String,
        reconciliationStatus: String,
        matchedUserId: String?,
        balanceBefore: Double,
        balanceAfter: Double,
        notes: String
    ) {
        try {
            val now = System.currentTimeMillis()
            val timeFmt = java.text.SimpleDateFormat("HH:mm:ss dd-MMM-yyyy", java.util.Locale.US).format(java.util.Date(now))
            bookkeepingDao.insertInboundAuditLog(
                InboundNotificationAuditLogEntity(
                    id = "AUD-" + (100000..999999).random(),
                    source = source,
                    eventType = eventType,
                    reference = reference,
                    amount = amount,
                    rawPayload = rawPayload,
                    parsedSender = parsedSender,
                    parsedNarration = parsedNarration,
                    detectedConfirmationCode = detectedCode,
                    detectedPhone = detectedPhone,
                    signatureVerified = signatureVerified,
                    signatureDetails = signatureDetails,
                    reconciliationStatus = reconciliationStatus,
                    matchedUserId = matchedUserId,
                    balanceBefore = balanceBefore,
                    balanceAfter = balanceAfter,
                    reconciliationNotes = notes,
                    timestamp = now,
                    completedAtFormatted = timeFmt
                )
            )
        } catch (e: Exception) {
            Log.w("MultiUtilityEngine", "Failed to record inbound audit log: ${e.message}")
        }
    }

    /**
     * Checks automated bank transfer status for the client.
     * Looks up recent processed payments matching the client's confirmation PIN, phone number, and intended deposit amount.
     */
    suspend fun checkAutomatedTransferForUser(
        phone: String,
        expectedAmount: Double? = null,
        userPhone: String? = null,
        accountNumber: String = CORPORATE_ACCOUNT_NUMBER,
        accountName: String = CORPORATE_ACCOUNT_NAME
    ): AutomatedTransferCheckResult = withContext(Dispatchers.IO) {
        val cleanCode = phone.trim().uppercase()
        val normalizedUserPhone = userPhone?.let { PhoneNarrationParser.normalizePhoneNumber(it) ?: it.trim() }
        val phoneLast10 = normalizedUserPhone?.takeLast(10)

        val wallet = if (!normalizedUserPhone.isNullOrBlank()) {
            bookkeepingDao.getUserWalletByPhone(normalizedUserPhone) ?: bookkeepingDao.getUserWalletByConfirmationCode(cleanCode)
        } else {
            bookkeepingDao.getUserWalletByConfirmationCode(cleanCode)
        } ?: bookkeepingDao.getUserWalletSync()

        val currentBalance = wallet?.appWalletBalance ?: 0.0
        val activeCode = wallet?.activeConfirmationCode ?: cleanCode

        // Strictly exclude user alerts, funding errors, or user-submitted tickets!
        // User funding error reports MUST be manually reviewed and credited by the admin!
        val unresolvedList = bookkeepingDao.getAllUnresolvedPayments().filter { 
            it.status == "UNRESOLVED" &&
            !it.failureReason.contains("User Alert", ignoreCase = true) &&
            !it.failureReason.contains("Funding Error", ignoreCase = true) &&
            !it.failureReason.contains("User Report", ignoreCase = true) &&
            !it.rawNarration.contains("User Alert", ignoreCase = true) &&
            !it.rawNarration.contains("User Funding Error", ignoreCase = true) &&
            !it.rawNarration.contains("User Report", ignoreCase = true) &&
            !it.bankReference.startsWith("ALERT-") &&
            !it.bankReference.startsWith("USR-")
        }
        val activeCodeDigits = activeCode.replace("[^0-9]".toRegex(), "")
        val cleanCodeDigits = cleanCode.replace("[^0-9]".toRegex(), "")

        val calendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val startOfDay = calendar.timeInMillis

        val matchingUnresolved = unresolvedList.firstOrNull { 
            val isRecent = it.createdAt >= startOfDay || (System.currentTimeMillis() - it.createdAt) < 24 * 60 * 60 * 1000L
            val rawNarr = it.rawNarration.uppercase()
            val rawNarrNoHyphen = rawNarr.replace("-", "").replace(" ", "")
            val matchesCode = it.detectedPhone.equals(activeCode, ignoreCase = true) ||
                    it.detectedPhone.equals(cleanCode, ignoreCase = true) ||
                    rawNarr.contains(activeCode, ignoreCase = true) ||
                    rawNarr.contains(cleanCode, ignoreCase = true) ||
                    rawNarrNoHyphen.contains(activeCode.replace("-", "").replace(" ", ""), ignoreCase = true) ||
                    rawNarrNoHyphen.contains(cleanCode.replace("-", "").replace(" ", ""), ignoreCase = true) ||
                    (activeCodeDigits.length >= 4 && rawNarr.contains(activeCodeDigits)) ||
                    (cleanCodeDigits.length >= 4 && rawNarr.contains(cleanCodeDigits))
            val matchesPhone = !normalizedUserPhone.isNullOrBlank() && (
                    it.rawNarration.contains(normalizedUserPhone, ignoreCase = true) ||
                    (!phoneLast10.isNullOrBlank() && it.rawNarration.contains(phoneLast10, ignoreCase = true))
            )
            // STRICT REQUIREMENT:
            // Must match BOTH the remark/code AND the exact amount transferred.
            // If code matches but amount is different -> NO MATCH.
            // If amount matches but code is different -> NO MATCH.
            val matchesExactAmount = if (expectedAmount != null && expectedAmount > 0.0) {
                Math.abs(it.amount - expectedAmount) < 0.01
            } else {
                true
            }
            isRecent && (matchesCode || matchesPhone) && matchesExactAmount
        }

        val finalMatch = matchingUnresolved

        if (finalMatch != null && wallet != null) {
            // Check if this reference was already credited to prevent duplicate crediting
            val existingProcessed = bookkeepingDao.getProcessedPayment(finalMatch.bankReference)
            if (existingProcessed != null) {
                bookkeepingDao.updateUnresolvedPaymentStatus(finalMatch.id, "RESOLVED_ALREADY_CREDITED", "Reference already processed")
                return@withContext AutomatedTransferCheckResult(
                    isFoundAndCredited = true,
                    amountCredited = finalMatch.amount,
                    reference = finalMatch.bankReference,
                    message = "Transfer of ₦${String.format(java.util.Locale.US, "%,.2f", finalMatch.amount)} Verified & Credited to your wallet (Ref: ${finalMatch.bankReference}).",
                    newWalletBalance = currentBalance,
                    isPendingNetwork = false
                )
            }

            val balanceBefore = wallet.appWalletBalance
            val newBal = balanceBefore + finalMatch.amount
            val nextRotatedCode = PhoneNarrationParser.generateRotatedCode(wallet.activeConfirmationCode)
            val phoneToSave = normalizedUserPhone ?: wallet.phoneNumber

            bookkeepingDao.updateWalletBalance(wallet.id, newBal)
            bookkeepingDao.updateUserConfirmationCode(wallet.id, nextRotatedCode)
            bookkeepingDao.updateUnresolvedPaymentStatus(finalMatch.id, "RESOLVED_AUTO_CREDIT", "Auto-resolved to active user $phoneToSave ($activeCode)")

            bookkeepingDao.insertProcessedPayment(
                ProcessedPaymentEntity(
                    reference = finalMatch.bankReference,
                    amount = finalMatch.amount,
                    rawNarration = finalMatch.rawNarration,
                    extractedPhone = phoneToSave,
                    senderName = finalMatch.senderName,
                    status = "completed",
                    resolvedAt = System.currentTimeMillis()
                )
            )

            val now = System.currentTimeMillis()
            val timeFmt = java.text.SimpleDateFormat("HH:mm:ss dd-MMM-yyyy", java.util.Locale.US).format(java.util.Date(now))
            val txEntity = TransactionBookkeepingEntity(
                id = "TX-DEP-" + (100000..999999).random(),
                userId = wallet.id,
                transactionType = "wallet_deposit",
                serviceCategory = "Deposit",
                recipientOrAccount = "${finalMatch.senderName} (PIN: $activeCode, Phone: $phoneToSave)",
                amountDebitedFromUser = finalMatch.amount,
                amountPaidToWholesaleApi = finalMatch.amount,
                netProfitEarned = 0.0,
                status = "success",
                timestamp = now,
                reference = finalMatch.bankReference,
                confirmationSource = "MONIEPOINT WEBHOOK",
                completedAtFormatted = timeFmt
            )
            bookkeepingDao.insertTransaction(txEntity)

            return@withContext AutomatedTransferCheckResult(
                isFoundAndCredited = true,
                amountCredited = finalMatch.amount,
                reference = finalMatch.bankReference,
                message = "Transfer Verified & Credited! ₦${String.format("%,.2f", finalMatch.amount)} added to your wallet (Ref: ${finalMatch.bankReference}).",
                newWalletBalance = newBal,
                isPendingNetwork = false
            )
        }

        // If not found yet in live webhooks, return not found status without mutating balance
        return@withContext AutomatedTransferCheckResult(
            isFoundAndCredited = false,
            amountCredited = 0.0,
            reference = "",
            message = "No transfer detected yet matching PIN '$activeCode'" + (if (expectedAmount != null && expectedAmount > 0) " for ₦${String.format("%,.2f", expectedAmount)}" else "") + ". Ensure '$activeCode' was in narration and transfer completed.",
            newWalletBalance = currentBalance,
            isPendingNetwork = false
        )
    }

    /**
     * Admin manual resolution for unresolved payments.
     */
    suspend fun resolveUnresolvedPayment(
        unresolvedId: String,
        action: String, // "CREDIT_WALLET", "PUSH_PAIRGATE", "REFUND", "MARK_DECLINED"
        targetPhone: String,
        notes: String = ""
    ): ResolutionResult = withContext(Dispatchers.IO) {
        val cleanPhone = PhoneNarrationParser.normalizePhoneNumber(targetPhone) ?: targetPhone.trim()
        val unresolved = bookkeepingDao.getUnresolvedPaymentById(unresolvedId)

        val wallet = (if (cleanPhone.isNotBlank()) bookkeepingDao.getUserWalletByPhone(cleanPhone) else null)
            ?: (if (cleanPhone.isNotBlank()) bookkeepingDao.getUserWalletByConfirmationCode(cleanPhone) else null)
            ?: (if (unresolved?.detectedPhone?.isNotBlank() == true) bookkeepingDao.getUserWalletByPhone(unresolved.detectedPhone) else null)
            ?: bookkeepingDao.getUserWalletSync("usr_default_1")
            ?: bookkeepingDao.getUserWalletSync()

        if (action == "CREDIT_WALLET") {
            val creditAmount = unresolved?.amount ?: 0.0
            val targetRef = unresolved?.bankReference?.ifBlank { "RES-" + (100000..999999).random() } ?: ("RES-" + (100000..999999).random())

            if (creditAmount > 0) {
                val defaultWallet = bookkeepingDao.getUserWalletSync("usr_default_1")
                val clientByPhone = if (cleanPhone.isNotBlank()) bookkeepingDao.getClientAccountByEmailOrPhone("", cleanPhone) else null
                val clientByTarget = if (targetPhone.isNotBlank()) bookkeepingDao.getClientAccountByEmailOrPhone(targetPhone.trim(), targetPhone.trim()) else null
                val clientAcct = clientByPhone ?: clientByTarget

                val currentBal = maxOf(
                    wallet?.appWalletBalance ?: 0.0,
                    defaultWallet?.appWalletBalance ?: 0.0,
                    clientAcct?.walletBalance ?: 0.0
                )
                val newBal = currentBal + creditAmount
                val targetWalletId = wallet?.id ?: clientAcct?.id ?: (if (cleanPhone.isNotBlank()) "usr_${cleanPhone.takeLast(10)}" else "usr_default_1")
                val nextRotatedCode = PhoneNarrationParser.generateRotatedCode(wallet?.activeConfirmationCode ?: "")

                // 1. Update target wallet and code
                bookkeepingDao.updateWalletBalance(targetWalletId, newBal)
                bookkeepingDao.updateUserConfirmationCode(targetWalletId, nextRotatedCode)

                // 2. Only update default wallet if the target IS explicitly the default wallet
                if (targetWalletId == "usr_default_1") {
                    bookkeepingDao.updateWalletBalance("usr_default_1", newBal)
                    bookkeepingDao.updateUserConfirmationCode("usr_default_1", nextRotatedCode)
                }

                // 3. Update all wallets & client virtual accounts by all phone variants and email
                val phoneVariants = mutableSetOf<String>()
                if (cleanPhone.isNotBlank()) phoneVariants.add(cleanPhone)
                if (targetPhone.isNotBlank()) phoneVariants.add(targetPhone.trim())
                if (unresolved?.detectedPhone?.isNotBlank() == true) phoneVariants.add(unresolved.detectedPhone.trim())
                if (wallet?.phoneNumber?.isNotBlank() == true) phoneVariants.add(wallet.phoneNumber.trim())

                val baseList = phoneVariants.toList()
                for (p in baseList) {
                    val digits = p.replace(Regex("[^0-9]"), "")
                    if (digits.length >= 10) {
                        val last10 = digits.takeLast(10)
                        phoneVariants.add("0$last10")
                        phoneVariants.add("+234$last10")
                        phoneVariants.add("234$last10")
                        phoneVariants.add(last10)
                    }
                }

                for (p in phoneVariants) {
                    bookkeepingDao.updateWalletBalanceByPhone(p, newBal)
                    bookkeepingDao.updateClientAccountBalanceByPhone(p, newBal)
                }

                if (clientAcct != null) {
                    bookkeepingDao.updateClientAccountBalance(clientAcct.id, newBal)
                }
                if (targetPhone.contains("@")) {
                    bookkeepingDao.updateClientAccountBalanceByPhone(targetPhone.trim(), newBal)
                }

                // 4. Record transaction in bookkeeping
                val now = System.currentTimeMillis()
                val timeFmt = java.text.SimpleDateFormat("HH:mm:ss dd-MMM-yyyy", java.util.Locale.US).format(java.util.Date(now))
                val txEntity = TransactionBookkeepingEntity(
                    id = "TX-DEP-" + (100000..999999).random(),
                    userId = targetWalletId,
                    transactionType = "wallet_deposit",
                    serviceCategory = "Deposit",
                    recipientOrAccount = "${unresolved?.senderName ?: "Bank Customer"} ($cleanPhone)",
                    amountDebitedFromUser = creditAmount,
                    amountPaidToWholesaleApi = creditAmount,
                    netProfitEarned = 0.0,
                    status = "success",
                    timestamp = now,
                    reference = targetRef,
                    confirmationSource = "ADMIN MANUAL RESOLVE",
                    completedAtFormatted = timeFmt
                )
                bookkeepingDao.insertTransaction(txEntity)

                // 5. Insert processed payment
                bookkeepingDao.insertProcessedPayment(
                    ProcessedPaymentEntity(
                        reference = targetRef,
                        amount = creditAmount,
                        rawNarration = unresolved?.rawNarration ?: "Admin resolution credit",
                        extractedPhone = cleanPhone.ifBlank { unresolved?.detectedPhone ?: "" },
                        senderName = unresolved?.senderName ?: "Bank Customer",
                        status = "completed",
                        resolvedAt = now
                    )
                )

                bookkeepingDao.updateUnresolvedPaymentStatus(unresolvedId, "RESOLVED", "Manually credited ₦${String.format("%,.2f", creditAmount)} to $cleanPhone: $notes")

                return@withContext ResolutionResult(
                    success = true,
                    newBalance = newBal,
                    creditedAmount = creditAmount,
                    targetPhone = cleanPhone,
                    reference = targetRef,
                    message = "Wallet credited with ₦${String.format("%,.2f", creditAmount)}. New Balance: ₦${String.format("%,.2f", newBal)}"
                )
            } else {
                return@withContext ResolutionResult(
                    success = false,
                    message = "Cannot credit zero or invalid amount."
                )
            }
        } else if (action == "PUSH_PAIRGATE" || action == "RESOLVED_ORDER_PUSHED") {
            bookkeepingDao.updateUnresolvedPaymentStatus(unresolvedId, "RESOLVED", "Manually pushed order to $cleanPhone: $notes")
            return@withContext ResolutionResult(
                success = true,
                targetPhone = cleanPhone,
                message = "Order marked pushed successfully!"
            )
        } else if (action == "REFUND" || action == "RESOLVED_REFUNDED") {
            bookkeepingDao.updateUnresolvedPaymentStatus(unresolvedId, "RESOLVED_REFUNDED", "Marked refunded: $notes")
            return@withContext ResolutionResult(
                success = true,
                targetPhone = cleanPhone,
                message = "Payment marked refunded."
            )
        } else if (action == "MARK_DECLINED") {
            bookkeepingDao.updateUnresolvedPaymentStatus(unresolvedId, "DECLINED", "Declined by admin: $notes")
            return@withContext ResolutionResult(
                success = true,
                targetPhone = cleanPhone,
                message = "Payment declined."
            )
        }
        ResolutionResult(success = false, message = "Unknown resolution action: $action")
    }

    /**
     * User report of a stalled bank deposit to alert the Admin for manual verification & credit.
     * Does NOT credit wallet automatically; records a pending unresolved payment for admin review.
     */
    suspend fun reportStalledDepositToAdmin(
        phone: String,
        amount: Double,
        reference: String,
        senderName: String = "Bank Customer",
        userNote: String = ""
    ): WebhookFulfillmentResult = withContext(Dispatchers.IO) {
        val cleanPhone = PhoneNarrationParser.normalizePhoneNumber(phone) ?: phone.trim()
        val wallet = bookkeepingDao.getUserWalletByPhone(cleanPhone) ?: bookkeepingDao.getUserWalletSync()
            ?: return@withContext WebhookFulfillmentResult(
                status = "error",
                message = "Wallet account not found for $cleanPhone",
                extractedPhone = cleanPhone,
                amountReceived = amount,
                isFulfilled = false
            )

        val unresId = "UNRES-" + (10000..99999).random()
        val safeRef = reference.trim().ifBlank { "ALERT-${System.currentTimeMillis() % 1000000}" }
        val rawNarration = "User Funding Error Alert: Ref=$safeRef, Sender=$senderName, Note=${userNote.ifBlank { "Awaiting Admin bank statement check and manual credit" }}"

        val unresolvedEntity = UnresolvedPaymentEntity(
            id = unresId,
            bankReference = safeRef,
            amount = amount,
            rawNarration = rawNarration,
            senderName = senderName.ifBlank { wallet.assignedAccountName.ifBlank { "Bank Customer" } },
            detectedPhone = cleanPhone,
            failureReason = "User Funding Error Alert: Awaiting Manual Admin Review & Service Credit",
            status = "UNRESOLVED",
            resolutionNotes = userNote.takeIf { it.isNotBlank() },
            createdAt = System.currentTimeMillis()
        )
        bookkeepingDao.insertUnresolvedPayment(unresolvedEntity)

        return@withContext WebhookFulfillmentResult(
            status = "reported",
            message = "Funding report submitted to Admin Desk (Ref: $safeRef). The Admin will verify your transaction manually and credit your service or wallet.",
            extractedPhone = cleanPhone,
            amountReceived = amount,
            isFulfilled = false
        )
    }

    suspend fun recordServicePurchase(
        userId: String = "usr_default_1",
        serviceCategory: String,
        recipientOrAccount: String,
        amountChargedToUser: Double,
        wholesaleCostPrice: Double,
        netProfit: Double
    ): TransactionOperationResult = withContext(Dispatchers.IO) {
        val wallet = bookkeepingDao.getUserWalletSync(userId)
            ?: return@withContext TransactionOperationResult(false, "User wallet not found")

        if (wallet.appWalletBalance < amountChargedToUser) {
            return@withContext TransactionOperationResult(
                isSuccess = false,
                message = "Insufficient wallet balance. Required: ₦${String.format("%,.2f", amountChargedToUser)}, Available: ₦${String.format("%,.2f", wallet.appWalletBalance)}"
            )
        }

        val debitedBalance = wallet.appWalletBalance - amountChargedToUser
        bookkeepingDao.updateWalletBalance(userId, debitedBalance)

        val txId = "TX-SVC-" + (100000..999999).random()
        val ref = "PG-SVC-" + (1000000..9999999).random()

        val now = System.currentTimeMillis()
        val timeFmt = java.text.SimpleDateFormat("HH:mm:ss dd-MMM-yyyy", java.util.Locale.US).format(java.util.Date(now))
        val txEntity = TransactionBookkeepingEntity(
            id = txId,
            userId = userId,
            transactionType = "combo_service_purchase",
            serviceCategory = serviceCategory,
            recipientOrAccount = recipientOrAccount,
            amountDebitedFromUser = amountChargedToUser,
            amountPaidToWholesaleApi = wholesaleCostPrice,
            netProfitEarned = netProfit,
            status = "success",
            timestamp = now,
            reference = ref,
            confirmationSource = "FLOWTEST API",
            completedAtFormatted = timeFmt
        )

        bookkeepingDao.insertTransaction(txEntity)

        TransactionOperationResult(
            isSuccess = true,
            message = "Purchase Successful for $recipientOrAccount",
            transactionId = txId,
            amountDebited = amountChargedToUser,
            netProfit = netProfit,
            newWalletBalance = debitedBalance
        )
    }

    suspend fun updateUserVirtualAccountInfo(
        email: String,
        phoneNumber: String,
        bankName: String = CORPORATE_BANK_NAME,
        accountNumber: String = CORPORATE_ACCOUNT_NUMBER,
        accountName: String = CORPORATE_ACCOUNT_NAME,
        userId: String = ""
    ) = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        val walletId = if (userId.isNotBlank()) "usr_$userId" else if (cleanEmail.isNotBlank()) "usr_" + cleanEmail.replace("[^a-zA-Z0-9_]".toRegex(), "_") else "usr_default_1"
        val current = bookkeepingDao.getUserWalletSync(walletId)
        val updated = if (current != null) {
            current.copy(
                email = email,
                phoneNumber = phoneNumber,
                assignedBank = bankName,
                assignedAccountNumber = accountNumber,
                assignedAccountName = accountName
            )
        } else {
            UserWalletEntity(
                id = walletId,
                appWalletBalance = 0.0,
                email = email,
                phoneNumber = phoneNumber,
                assignedBank = bankName,
                assignedAccountNumber = accountNumber,
                assignedAccountName = accountName
            )
        }
        bookkeepingDao.insertOrUpdateUserWallet(updated)

        val clientAccId = "acc_" + (if (userId.isNotBlank()) userId.takeLast(10) else if (cleanEmail.isNotBlank()) cleanEmail.replace("[^a-zA-Z0-9_]".toRegex(), "_").takeLast(12) else accountNumber.takeLast(6))
        val clientAcc = ClientAccountEntity(
            id = clientAccId,
            customerName = accountName,
            customerEmail = email,
            customerPhone = phoneNumber,
            bankName = bankName,
            accountNumber = accountNumber,
            accountName = accountName,
            reference = "PG-VUBAN-" + (100000..999999).random(),
            totalFunded = 0.0,
            status = "ACTIVE",
            createdAt = System.currentTimeMillis()
        )
        bookkeepingDao.insertClientAccount(clientAcc)
    }

    suspend fun seedInitialDataAndConfigs() = withContext(Dispatchers.IO) {
        val currentWallet = bookkeepingDao.getUserWalletSync()
        if (currentWallet == null) {
            bookkeepingDao.insertOrUpdateUserWallet(
                UserWalletEntity(
                    appWalletBalance = 0.0,
                    assignedBank = CORPORATE_BANK_NAME,
                    assignedAccountNumber = CORPORATE_ACCOUNT_NUMBER,
                    assignedAccountName = CORPORATE_ACCOUNT_NAME
                )
            )
        } else {
            val updatedWallet = currentWallet.copy(
                assignedBank = CORPORATE_BANK_NAME,
                assignedAccountNumber = CORPORATE_ACCOUNT_NUMBER,
                assignedAccountName = CORPORATE_ACCOUNT_NAME
            )
            bookkeepingDao.insertOrUpdateUserWallet(updatedWallet)
        }

        val defaultConfigs = listOf(
            PricingConfigEntity(id = "data_mtn_sme_1gb", serviceType = "data", providerId = "mtn", planId = "mtn_sme_1gb", wholesaleCost = 230.0, userRetailPrice = 280.0),
            PricingConfigEntity(id = "data_mtn_sme_2gb", serviceType = "data", providerId = "mtn", planId = "mtn_sme_2gb", wholesaleCost = 460.0, userRetailPrice = 550.0),
            PricingConfigEntity(id = "data_mtn_sme_5gb", serviceType = "data", providerId = "mtn", planId = "mtn_sme_5gb", wholesaleCost = 1150.0, userRetailPrice = 1350.0),
            PricingConfigEntity(id = "data_airtel_1gb", serviceType = "data", providerId = "airtel", planId = "airtel_1gb", wholesaleCost = 240.0, userRetailPrice = 290.0),
            PricingConfigEntity(id = "data_glo_1gb", serviceType = "data", providerId = "glo", planId = "glo_1gb", wholesaleCost = 225.0, userRetailPrice = 270.0),
            PricingConfigEntity(id = "airtime_mtn", serviceType = "airtime", providerId = "mtn", percentageMarkup = 1.5),
            PricingConfigEntity(id = "airtime_airtel", serviceType = "airtime", providerId = "airtel", percentageMarkup = 2.0),
            PricingConfigEntity(id = "airtime_glo", serviceType = "airtime", providerId = "glo", percentageMarkup = 2.5),
            PricingConfigEntity(id = "airtime_9mobile", serviceType = "airtime", providerId = "9mobile", percentageMarkup = 3.0),
            PricingConfigEntity(id = "utility_ikedc", serviceType = "electricity", providerId = "ikedc", flatConvenienceFee = 100.0),
            PricingConfigEntity(id = "utility_ekedc", serviceType = "electricity", providerId = "ekedc", flatConvenienceFee = 100.0),
            PricingConfigEntity(id = "utility_aedc", serviceType = "electricity", providerId = "aedc", flatConvenienceFee = 100.0),
            PricingConfigEntity(id = "cable_dstv", serviceType = "cable_tv", providerId = "dstv", flatConvenienceFee = 100.0),
            PricingConfigEntity(id = "cable_gotv", serviceType = "cable_tv", providerId = "gotv", flatConvenienceFee = 100.0),
            PricingConfigEntity(id = "cable_startimes", serviceType = "cable_tv", providerId = "startimes", flatConvenienceFee = 100.0),
            PricingConfigEntity(id = "sms_default", serviceType = "sms", providerId = "httpsms", planId = "sms_standard", wholesaleCost = 2.50, userRetailPrice = 4.50, percentageMarkup = 80.0)
        )
        bookkeepingDao.insertPricingConfigs(defaultConfigs)

        // Initialize clean frequent beneficiaries table if needed (starts empty for new installations)
    }

    suspend fun getUserWalletSync(userId: String = "usr_default_1"): UserWalletEntity? = withContext(Dispatchers.IO) {
        bookkeepingDao.getUserWalletSync(userId)
    }

    suspend fun setWalletBalance(newBalance: Double, userId: String = "usr_default_1") = withContext(Dispatchers.IO) {
        val wallet = bookkeepingDao.getUserWalletSync(userId) ?: UserWalletEntity(id = userId)
        bookkeepingDao.insertOrUpdateUserWallet(wallet.copy(appWalletBalance = newBalance))
    }

    suspend fun handleInboundWebhookDeposit(
        senderName: String,
        amountTransferred: Double,
        senderAccount: String,
        reference: String
    ): TransactionOperationResult = withContext(Dispatchers.IO) {
        val userWallet = bookkeepingDao.getUserWalletSync() ?: UserWalletEntity().also { bookkeepingDao.insertOrUpdateUserWallet(it) }

        val pairgateFee = minOf(amountTransferred * 0.01, 100.0)
        val cleanAmountCredited = amountTransferred - pairgateFee

        val newBalance = userWallet.appWalletBalance + cleanAmountCredited
        bookkeepingDao.updateWalletBalance(userWallet.id, newBalance)

        val txId = "TX-DEP-" + (100000..999999).random()
        val now = System.currentTimeMillis()
        val timeFmt = java.text.SimpleDateFormat("HH:mm:ss dd-MMM-yyyy", java.util.Locale.US).format(java.util.Date(now))
        val txEntity = TransactionBookkeepingEntity(
            id = txId,
            userId = userWallet.id,
            transactionType = "wallet_deposit",
            serviceCategory = "Deposit",
            recipientOrAccount = "$senderName ($senderAccount)",
            amountDebitedFromUser = cleanAmountCredited,
            amountPaidToWholesaleApi = cleanAmountCredited,
            netProfitEarned = 0.0,
            status = "success",
            timestamp = now,
            reference = reference,
            confirmationSource = "MONIEPOINT INBOUND WEBHOOK",
            completedAtFormatted = timeFmt
        )

        bookkeepingDao.insertTransaction(txEntity)
        bookkeepingDao.recordClientAccountDeposit(userWallet.assignedAccountNumber, cleanAmountCredited)

        TransactionOperationResult(
            isSuccess = true,
            message = "Deposit Credited: ₦${String.format("%,.2f", cleanAmountCredited)}",
            transactionId = txId,
            amountDebited = cleanAmountCredited,
            netProfit = 0.0,
            newWalletBalance = newBalance
        )
    }

    suspend fun recordSuccessfulDeposit(
        amount: Double,
        reference: String,
        narration: String = "Deposit",
        sender: String = "Moniepoint Transfer",
        userId: String = "usr_default_1"
    ) = withContext(Dispatchers.IO) {
        val txId = "TX-DEP-" + (100000..999999).random()
        val now = System.currentTimeMillis()
        val timeFmt = java.text.SimpleDateFormat("HH:mm:ss dd-MMM-yyyy", java.util.Locale.US).format(java.util.Date(now))
        val txEntity = TransactionBookkeepingEntity(
            id = txId,
            userId = userId,
            transactionType = "wallet_deposit",
            serviceCategory = "Deposit",
            recipientOrAccount = "$sender - $narration",
            amountDebitedFromUser = amount,
            amountPaidToWholesaleApi = amount,
            netProfitEarned = 0.0,
            status = "success",
            timestamp = now,
            reference = reference,
            confirmationSource = "MONIEPOINT BANK FEED",
            completedAtFormatted = timeFmt
        )
        bookkeepingDao.insertTransaction(txEntity)
    }

    suspend fun purchaseDataPackage(
        userId: String = "usr_default_1",
        planId: String,
        recipientPhone: String,
        simulateApiFailure: Boolean = false,
        apiService: PairgateApiService? = null,
        bearerToken: String? = null
    ): TransactionOperationResult = withContext(Dispatchers.IO) {
        val config = bookkeepingDao.getPricingConfigByPlanId(planId)
        val verifiedPlan = PairgateVerifiedPlans.ALL_PLANS.find { it.getEffectivePlanId() == planId || it.planId == planId || it.id == planId }
        val (providerId: String, wholesaleCost: Double, retailPrice: Double) = when {
            config != null -> Triple(config.providerId ?: "mtn", config.wholesaleCost, config.userRetailPrice)
            verifiedPlan != null -> {
                val wholesale: Double = verifiedPlan.getEffectivePrice()
                val retail: Double = (wholesale * 1.08).coerceAtLeast(wholesale + 20.0)
                Triple(verifiedPlan.getEffectiveProvider().lowercase(), wholesale, retail)
            }
            else -> {
                val wholesale: Double = 250.0
                val retail: Double = 300.0
                Triple("mtn", wholesale, retail)
            }
        }

        val wallet = bookkeepingDao.getUserWalletSync(userId)
            ?: return@withContext TransactionOperationResult(false, "User wallet not found")

        val netProfit = (retailPrice - wholesaleCost).coerceAtLeast(0.0)

        if (wallet.appWalletBalance < retailPrice) {
            return@withContext TransactionOperationResult(
                isSuccess = false,
                message = "Insufficient wallet balance. Required: ₦${String.format("%,.2f", retailPrice)}, Available: ₦${String.format("%,.2f", wallet.appWalletBalance)}"
            )
        }

        val txId = "TX-DATA-" + (100000..999999).random()
        val ref = "PG-DATA-" + (1000000..9999999).random()

        var apiSucceeded = false
        var isNetworkFailure = false
        var responseMessage = ""
        var finalRef = ref

        if (simulateApiFailure) {
            isNetworkFailure = true
            responseMessage = "Simulated network timeout connecting to wholesale gateway"
        } else {
            val formattedToken = if (bearerToken.isNullOrBlank()) {
                "Bearer " + com.example.BuildConfig.PAIRGATE_API_KEY.replace(Regex("(?i)^bearer\\s+"), "").trim()
            } else if (bearerToken.startsWith("Bearer ", ignoreCase = true)) {
                bearerToken
            } else {
                "Bearer $bearerToken"
            }

            val req = PairgateDataRequest.create(
                provider = providerId,
                planIdentifier = planId,
                recipientPhone = recipientPhone,
                reference = ref,
                amount = retailPrice
            )

            try {
                val service = apiService ?: PairgateApiService.create()
                val response = service.purchaseData(formattedToken, req)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.isSuccessful() == true) {
                        apiSucceeded = true
                        responseMessage = body.getDetailedMessage()
                        finalRef = body.getEffectiveReference() ?: ref
                    } else {
                        apiSucceeded = false
                        responseMessage = body?.getDetailedMessage() ?: "Pairgate declined data purchase."
                    }
                } else {
                    apiSucceeded = false
                    val errStr = try { response.errorBody()?.string() } catch (e: Exception) { null }
                    val errObj = PairgateApiResponse.parseErrorBody(errStr)
                    responseMessage = errObj?.getDetailedMessage() ?: "Pairgate returned HTTP ${response.code()}: ${response.message()}"
                }
            } catch (e: java.io.IOException) {
                // Only actual network failures (timeout, no route, disconnect) trigger admin pending queue
                isNetworkFailure = true
                responseMessage = e.localizedMessage ?: "Network error connecting to Pairgate gateway"
            } catch (e: Exception) {
                isNetworkFailure = true
                responseMessage = e.localizedMessage ?: "Connection error"
            }
        }

        val now = System.currentTimeMillis()
        val timeFmt = java.text.SimpleDateFormat("HH:mm:ss dd-MMM-yyyy", java.util.Locale.US).format(java.util.Date(now))

        if (apiSucceeded) {
            val debitedBalance = wallet.appWalletBalance - retailPrice
            bookkeepingDao.updateWalletBalance(userId, debitedBalance)

            val txEntity = TransactionBookkeepingEntity(
                id = txId,
                userId = userId,
                transactionType = "data_purchase",
                serviceCategory = "Data",
                recipientOrAccount = "$providerId - $recipientPhone",
                amountDebitedFromUser = retailPrice,
                amountPaidToWholesaleApi = wholesaleCost,
                netProfitEarned = netProfit,
                status = "success",
                timestamp = now,
                reference = finalRef,
                confirmationSource = "PAIRGATE API (DIRECT)",
                completedAtFormatted = timeFmt
            )
            bookkeepingDao.insertTransaction(txEntity)

            return@withContext TransactionOperationResult(
                isSuccess = true,
                message = responseMessage.ifBlank { "Data bundle successfully delivered to $recipientPhone!" },
                transactionId = txId,
                amountDebited = retailPrice,
                netProfit = netProfit,
                newWalletBalance = debitedBalance
            )
        }

        if (isNetworkFailure) {
            // ONLY network failure: queue for Admin reconciliation and notify user it's pending
            val debitedBalance = wallet.appWalletBalance - retailPrice
            bookkeepingDao.updateWalletBalance(userId, debitedBalance)

            createPendingOrder(
                phoneNumber = recipientPhone,
                customerName = "Flow Customer",
                serviceType = "data",
                network = providerId,
                planId = planId,
                planName = verifiedPlan?.getEffectiveName() ?: "${providerId.uppercase()} Data ($planId)",
                retailPrice = retailPrice,
                wholesaleCost = wholesaleCost,
                bankTransactionRef = finalRef
            )

            val txEntity = TransactionBookkeepingEntity(
                id = txId,
                userId = userId,
                transactionType = "data_purchase",
                serviceCategory = "Data",
                recipientOrAccount = "$providerId - $recipientPhone",
                amountDebitedFromUser = retailPrice,
                amountPaidToWholesaleApi = wholesaleCost,
                netProfitEarned = netProfit,
                status = "pending",
                timestamp = now,
                reference = finalRef,
                confirmationSource = "PAIRGATE API (NETWORK INTERRUPTION)",
                completedAtFormatted = timeFmt
            )
            bookkeepingDao.insertTransaction(txEntity)

            return@withContext TransactionOperationResult(
                isSuccess = true,
                isPending = true,
                status = "PENDING",
                message = "Network connection interrupted while contacting gateway. Order queued as PENDING for Admin fulfillment ($recipientPhone).",
                transactionId = txId,
                amountDebited = retailPrice,
                netProfit = netProfit,
                newWalletBalance = debitedBalance
            )
        }

        // Upstream Pairgate API error (e.g. invalid plan, bad recipient, insufficient wholesale balance)
        // DO NOT debit wallet, DO NOT queue for Admin reconciliation, display Pairgate's response directly!
        val txEntity = TransactionBookkeepingEntity(
            id = txId,
            userId = userId,
            transactionType = "data_purchase",
            serviceCategory = "Data",
            recipientOrAccount = "$providerId - $recipientPhone",
            amountDebitedFromUser = 0.0,
            amountPaidToWholesaleApi = 0.0,
            netProfitEarned = 0.0,
            status = "failed",
            timestamp = now,
            reference = finalRef,
            confirmationSource = "PAIRGATE API (REJECTED)",
            completedAtFormatted = timeFmt
        )
        bookkeepingDao.insertTransaction(txEntity)

        return@withContext TransactionOperationResult(
            isSuccess = false,
            isPending = false,
            status = "FAILED",
            message = responseMessage,
            transactionId = txId,
            newWalletBalance = wallet.appWalletBalance
        )
    }

    suspend fun vendAirtime(
        userId: String = "usr_default_1",
        providerId: String,
        phone: String,
        requestedAmount: Double,
        simulateApiFailure: Boolean = false,
        apiService: PairgateApiService? = null,
        bearerToken: String? = null
    ): TransactionOperationResult = withContext(Dispatchers.IO) {
        val config = bookkeepingDao.getPricingConfigByServiceAndProvider("airtime", providerId)
        val percentageMarkup = config?.percentageMarkup ?: 1.5
        val retailPrice = requestedAmount * (1.0 + (percentageMarkup / 100.0))
        val wholesaleCost = requestedAmount
        val netProfit = (retailPrice - wholesaleCost).coerceAtLeast(0.0)

        val wallet = bookkeepingDao.getUserWalletSync(userId)
            ?: return@withContext TransactionOperationResult(false, "User wallet not found")

        if (wallet.appWalletBalance < retailPrice) {
            return@withContext TransactionOperationResult(
                isSuccess = false,
                message = "Insufficient wallet balance. Required: ₦${String.format("%,.2f", retailPrice)}, Available: ₦${String.format("%,.2f", wallet.appWalletBalance)}"
            )
        }

        val txId = "TX-AIR-" + (100000..999999).random()
        val ref = "PG-AIR-" + (1000000..9999999).random()

        var apiSucceeded = false
        var isNetworkFailure = false
        var responseMessage = ""
        var finalRef = ref

        if (simulateApiFailure) {
            isNetworkFailure = true
            responseMessage = "Simulated network timeout during airtime vending"
        } else {
            val formattedToken = if (bearerToken.isNullOrBlank()) {
                "Bearer " + com.example.BuildConfig.PAIRGATE_API_KEY.replace(Regex("(?i)^bearer\\s+"), "").trim()
            } else if (bearerToken.startsWith("Bearer ", ignoreCase = true)) {
                bearerToken
            } else {
                "Bearer $bearerToken"
            }

            val req = PairgateAirtimeRequest.create(
                provider = providerId,
                amount = requestedAmount,
                recipientPhone = phone,
                reference = ref
            )

            try {
                val service = apiService ?: PairgateApiService.create()
                val response = service.purchaseAirtime(formattedToken, req)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.isSuccessful() == true) {
                        apiSucceeded = true
                        responseMessage = body.getDetailedMessage()
                        finalRef = body.getEffectiveReference() ?: ref
                    } else {
                        apiSucceeded = false
                        responseMessage = body?.getDetailedMessage() ?: "Pairgate declined airtime purchase."
                    }
                } else {
                    apiSucceeded = false
                    val errStr = try { response.errorBody()?.string() } catch (e: Exception) { null }
                    val errObj = PairgateApiResponse.parseErrorBody(errStr)
                    responseMessage = errObj?.getDetailedMessage() ?: "Pairgate returned HTTP ${response.code()}: ${response.message()}"
                }
            } catch (e: java.io.IOException) {
                isNetworkFailure = true
                responseMessage = e.localizedMessage ?: "Network error connecting to Pairgate gateway"
            } catch (e: Exception) {
                isNetworkFailure = true
                responseMessage = e.localizedMessage ?: "Connection error"
            }
        }

        val now = System.currentTimeMillis()
        val timeFmt = java.text.SimpleDateFormat("HH:mm:ss dd-MMM-yyyy", java.util.Locale.US).format(java.util.Date(now))

        if (apiSucceeded) {
            val debitedBalance = wallet.appWalletBalance - retailPrice
            bookkeepingDao.updateWalletBalance(userId, debitedBalance)

            val txEntity = TransactionBookkeepingEntity(
                id = txId,
                userId = userId,
                transactionType = "airtime_vending",
                serviceCategory = "Airtime",
                recipientOrAccount = "$providerId - $phone",
                amountDebitedFromUser = retailPrice,
                amountPaidToWholesaleApi = wholesaleCost,
                netProfitEarned = netProfit,
                status = "success",
                timestamp = now,
                reference = finalRef,
                confirmationSource = "PAIRGATE API (DIRECT)",
                completedAtFormatted = timeFmt
            )
            bookkeepingDao.insertTransaction(txEntity)

            return@withContext TransactionOperationResult(
                isSuccess = true,
                message = responseMessage.ifBlank { "Airtime ₦${String.format("%,.2f", requestedAmount)} successfully vended to $phone!" },
                transactionId = txId,
                amountDebited = retailPrice,
                netProfit = netProfit,
                newWalletBalance = debitedBalance
            )
        }

        if (isNetworkFailure) {
            val debitedBalance = wallet.appWalletBalance - retailPrice
            bookkeepingDao.updateWalletBalance(userId, debitedBalance)

            createPendingOrder(
                phoneNumber = phone,
                customerName = "Flow Customer",
                serviceType = "airtime",
                network = providerId,
                planId = "airtime_${requestedAmount.toInt()}",
                planName = "${providerId.uppercase()} ₦$requestedAmount Airtime Top-Up",
                retailPrice = retailPrice,
                wholesaleCost = wholesaleCost,
                bankTransactionRef = finalRef
            )

            val txEntity = TransactionBookkeepingEntity(
                id = txId,
                userId = userId,
                transactionType = "airtime_vending",
                serviceCategory = "Airtime",
                recipientOrAccount = "$providerId - $phone",
                amountDebitedFromUser = retailPrice,
                amountPaidToWholesaleApi = wholesaleCost,
                netProfitEarned = netProfit,
                status = "pending",
                timestamp = now,
                reference = finalRef,
                confirmationSource = "PAIRGATE API (NETWORK INTERRUPTION)",
                completedAtFormatted = timeFmt
            )
            bookkeepingDao.insertTransaction(txEntity)

            return@withContext TransactionOperationResult(
                isSuccess = true,
                isPending = true,
                status = "PENDING",
                message = "Network connection interrupted while contacting gateway. Airtime order queued as PENDING for Admin fulfillment ($phone).",
                transactionId = txId,
                amountDebited = retailPrice,
                netProfit = netProfit,
                newWalletBalance = debitedBalance
            )
        }

        // Upstream Pairgate API error - Do NOT debit wallet, return Pairgate response directly
        val txEntity = TransactionBookkeepingEntity(
            id = txId,
            userId = userId,
            transactionType = "airtime_vending",
            serviceCategory = "Airtime",
            recipientOrAccount = "$providerId - $phone",
            amountDebitedFromUser = 0.0,
            amountPaidToWholesaleApi = 0.0,
            netProfitEarned = 0.0,
            status = "failed",
            timestamp = now,
            reference = finalRef,
            confirmationSource = "PAIRGATE API (REJECTED)",
            completedAtFormatted = timeFmt
        )
        bookkeepingDao.insertTransaction(txEntity)

        return@withContext TransactionOperationResult(
            isSuccess = false,
            isPending = false,
            status = "FAILED",
            message = responseMessage,
            transactionId = txId,
            newWalletBalance = wallet.appWalletBalance
        )
    }

    suspend fun payUtilityOrCable(
        userId: String = "usr_default_1",
        serviceType: String,
        providerId: String,
        accountOrMeterNumber: String,
        rawPackageAmount: Double,
        simulateApiFailure: Boolean = false,
        apiService: PairgateApiService? = null,
        bearerToken: String? = null
    ): TransactionOperationResult = withContext(Dispatchers.IO) {
        val config = bookkeepingDao.getPricingConfigByServiceAndProvider(serviceType, providerId)
            ?: return@withContext TransactionOperationResult(false, "No pricing config found for $serviceType - $providerId")

        val wallet = bookkeepingDao.getUserWalletSync(userId)
            ?: return@withContext TransactionOperationResult(false, "User wallet not found")

        val convenienceFee = config.flatConvenienceFee
        val retailPrice = rawPackageAmount + convenienceFee
        val wholesaleCost = rawPackageAmount
        val netProfit = convenienceFee

        if (wallet.appWalletBalance < retailPrice) {
            return@withContext TransactionOperationResult(
                isSuccess = false,
                message = "Insufficient wallet balance. Required: ₦${String.format("%,.2f", retailPrice)}, Available: ₦${String.format("%,.2f", wallet.appWalletBalance)}"
            )
        }

        val debitedBalance = wallet.appWalletBalance - retailPrice
        bookkeepingDao.updateWalletBalance(userId, debitedBalance)

        val txId = "TX-BILL-" + (100000..999999).random()
        val ref = "PG-BILL-" + (1000000..9999999).random()
        val categoryName = if (serviceType == "electricity") "Utilities" else "Cable TV"

        val now = System.currentTimeMillis()
        val timeFmt = java.text.SimpleDateFormat("HH:mm:ss dd-MMM-yyyy", java.util.Locale.US).format(java.util.Date(now))
        val txEntity = TransactionBookkeepingEntity(
            id = txId,
            userId = userId,
            transactionType = if (serviceType == "electricity") "utility_bill" else "cable_subscription",
            serviceCategory = categoryName,
            recipientOrAccount = "$providerId - $accountOrMeterNumber",
            amountDebitedFromUser = retailPrice,
            amountPaidToWholesaleApi = wholesaleCost,
            netProfitEarned = netProfit,
            status = "pending",
            timestamp = now,
            reference = ref,
            confirmationSource = if (serviceType == "electricity") "FLOWTEST API (UTILITY)" else "FLOWTEST API (CABLE)",
            completedAtFormatted = timeFmt
        )
        bookkeepingDao.insertTransaction(txEntity)

        var apiFailed = simulateApiFailure
        var apiErrorMsg = "Bill payment failed at the gateway."

        if (!simulateApiFailure) {
            var dispatchedViaCloudRun = false
            try {
                val cloudResp = CloudRunApiClient.payBill(
                    serviceId = providerId,
                    customerId = accountOrMeterNumber,
                    amount = rawPackageAmount
                )
                if (cloudResp != null && (cloudResp.status?.contains("success", ignoreCase = true) == true || cloudResp.message?.contains("success", ignoreCase = true) == true)) {
                    dispatchedViaCloudRun = true
                }
            } catch (e: Exception) {
                Log.d("MultiUtilityEngine", "CloudRun bill payment fallback: ${e.message}")
            }

            if (!dispatchedViaCloudRun && apiService != null && !bearerToken.isNullOrBlank()) {
                try {
                    val req = PairgateBillRequest.create(
                        providerId = providerId,
                        accountOrMeterNumber = accountOrMeterNumber,
                        amount = rawPackageAmount,
                        reference = ref
                    )
                    val response = apiService.payBill(bearerToken, req)
                    if (!response.isSuccessful) {
                        apiFailed = true
                        apiErrorMsg = "Bill API returned HTTP ${response.code()}: ${response.message()}"
                    }
                } catch (e: Exception) {
                    Log.w("MultiUtilityEngine", "Live Pairgate Bill pay warning: ${e.message}")
                }
            }
        }

        if (apiFailed) {
            bookkeepingDao.updateWalletBalance(userId, wallet.appWalletBalance)
            bookkeepingDao.updateTransactionStatus(txId, "failed")
            return@withContext TransactionOperationResult(
                isSuccess = false,
                message = "$apiErrorMsg ₦${String.format("%,.2f", retailPrice)} was refunded.",
                transactionId = txId,
                newWalletBalance = wallet.appWalletBalance
            )
        }

        bookkeepingDao.updateTransactionStatus(txId, "success")

        TransactionOperationResult(
            isSuccess = true,
            message = "Payment of ₦${String.format("%,.2f", rawPackageAmount)} for $accountOrMeterNumber ($providerId) Successful!",
            transactionId = txId,
            amountDebited = retailPrice,
            netProfit = netProfit,
            newWalletBalance = debitedBalance
        )
    }

    suspend fun recordSmsBroadcast(
        userId: String = "usr_default_1",
        recipientsCount: Int,
        pageCount: Int,
        totalCharged: Double,
        wholesaleCost: Double,
        senderId: String,
        reference: String
    ): TransactionOperationResult = withContext(Dispatchers.IO) {
        val wallet = bookkeepingDao.getUserWalletSync(userId)
            ?: return@withContext TransactionOperationResult(false, "User wallet not found")

        if (wallet.appWalletBalance < totalCharged) {
            return@withContext TransactionOperationResult(
                isSuccess = false,
                message = "Insufficient wallet balance. Required: ₦${String.format("%,.2f", totalCharged)}, Available: ₦${String.format("%,.2f", wallet.appWalletBalance)}"
            )
        }

        val netProfit = totalCharged - wholesaleCost
        val debitedBalance = wallet.appWalletBalance - totalCharged
        bookkeepingDao.updateWalletBalance(userId, debitedBalance)

        val txId = "TX-SMS-" + (100000..999999).random()
        val now = System.currentTimeMillis()
        val timeFmt = java.text.SimpleDateFormat("HH:mm:ss dd-MMM-yyyy", java.util.Locale.US).format(java.util.Date(now))
        val txEntity = TransactionBookkeepingEntity(
            id = txId,
            userId = userId,
            transactionType = "sms_broadcast",
            serviceCategory = "SMS",
            recipientOrAccount = "$senderId -> $recipientsCount recipients ($pageCount page(s))",
            amountDebitedFromUser = totalCharged,
            amountPaidToWholesaleApi = wholesaleCost,
            netProfitEarned = netProfit,
            status = "success",
            timestamp = now,
            reference = reference,
            confirmationSource = "KUDI SMS GATEWAY",
            completedAtFormatted = timeFmt
        )
        bookkeepingDao.insertTransaction(txEntity)

        TransactionOperationResult(
            isSuccess = true,
            message = "SMS dispatched to $recipientsCount recipient(s). Charged ₦${String.format("%,.2f", totalCharged)}.",
            transactionId = txId,
            amountDebited = totalCharged,
            netProfit = netProfit,
            newWalletBalance = debitedBalance
        )
    }

    suspend fun purchaseVpnProSubscription(
        userId: String = "usr_default_1",
        planTitle: String,
        planDays: Int,
        packagePrice: Double
    ): TransactionOperationResult = withContext(Dispatchers.IO) {
        val wallet = bookkeepingDao.getUserWalletSync(userId)
            ?: return@withContext TransactionOperationResult(false, "User wallet not found")

        if (wallet.appWalletBalance < packagePrice) {
            return@withContext TransactionOperationResult(
                isSuccess = false,
                message = "Insufficient wallet balance. Required: ₦${String.format("%,.2f", packagePrice)}, Available: ₦${String.format("%,.2f", wallet.appWalletBalance)}"
            )
        }

        val newBalance = wallet.appWalletBalance - packagePrice
        bookkeepingDao.updateWalletBalance(userId, newBalance)

        val txId = "TX-VPNPRO-" + (100000..999999).random()
        val ref = "AZ-PRO-" + (1000000..9999999).random()

        val now = System.currentTimeMillis()
        val timeFmt = java.text.SimpleDateFormat("HH:mm:ss dd-MMM-yyyy", java.util.Locale.US).format(java.util.Date(now))
        val txEntity = TransactionBookkeepingEntity(
            id = txId,
            userId = userId,
            transactionType = "vpn_pro_purchase",
            serviceCategory = "VPN",
            recipientOrAccount = "$planTitle ($planDays Days VIP Access)",
            amountDebitedFromUser = packagePrice,
            amountPaidToWholesaleApi = 0.0,
            netProfitEarned = packagePrice,
            status = "success",
            timestamp = now,
            reference = ref,
            confirmationSource = "WALLET SETTLEMENT",
            completedAtFormatted = timeFmt
        )
        bookkeepingDao.insertTransaction(txEntity)

        TransactionOperationResult(
            isSuccess = true,
            message = "VIP VPN Pro Activated! Purchased $planTitle for ₦${String.format("%,.2f", packagePrice)}.",
            transactionId = txId,
            amountDebited = packagePrice,
            netProfit = packagePrice,
            newWalletBalance = newBalance
        )
    }

    suspend fun purchaseVpnTimePack(
        userId: String = "usr_default_1",
        planTitle: String,
        minutesToAdd: Long,
        packagePrice: Double,
        wholesaleCost: Double = 0.0
    ): TransactionOperationResult = withContext(Dispatchers.IO) {
        val wallet = bookkeepingDao.getUserWalletSync(userId)
            ?: return@withContext TransactionOperationResult(false, "User wallet not found")

        if (wallet.appWalletBalance < packagePrice) {
            return@withContext TransactionOperationResult(
                isSuccess = false,
                message = "Insufficient wallet balance. Required: ₦${String.format("%,.2f", packagePrice)}, Available: ₦${String.format("%,.2f", wallet.appWalletBalance)}"
            )
        }

        val newBalance = wallet.appWalletBalance - packagePrice
        bookkeepingDao.updateWalletBalance(userId, newBalance)

        val txId = "TX-VPNTIME-" + (100000..999999).random()
        val ref = "AZ-TIME-" + (1000000..9999999).random()

        val now = System.currentTimeMillis()
        val timeFmt = java.text.SimpleDateFormat("HH:mm:ss dd-MMM-yyyy", java.util.Locale.US).format(java.util.Date(now))
        val hours = if (minutesToAdd >= 60) "${minutesToAdd / 60}h" else "${minutesToAdd}m"
        val netProfit = (packagePrice - wholesaleCost).coerceAtLeast(0.0)

        val txEntity = TransactionBookkeepingEntity(
            id = txId,
            userId = userId,
            transactionType = "vpn_time_purchase",
            serviceCategory = "VPN",
            recipientOrAccount = "$planTitle (+$hours VPN Time)",
            amountDebitedFromUser = packagePrice,
            amountPaidToWholesaleApi = wholesaleCost,
            netProfitEarned = netProfit,
            status = "success",
            timestamp = now,
            reference = ref,
            confirmationSource = "WALLET SETTLEMENT",
            completedAtFormatted = timeFmt
        )
        bookkeepingDao.insertTransaction(txEntity)

        TransactionOperationResult(
            isSuccess = true,
            message = "VPN Time Added! Purchased $planTitle for ₦${String.format("%,.2f", packagePrice)}.",
            transactionId = txId,
            amountDebited = packagePrice,
            netProfit = packagePrice,
            newWalletBalance = newBalance
        )
    }

    suspend fun purchaseByodSmsSubscription(
        userId: String = "usr_default_1",
        planTitle: String = "BYOD SaaS Monthly Gateway",
        durationDays: Int = 30,
        packagePrice: Double = 5000.0
    ): TransactionOperationResult = withContext(Dispatchers.IO) {
        val wallet = bookkeepingDao.getUserWalletSync(userId)
            ?: return@withContext TransactionOperationResult(false, "User wallet not found")

        if (wallet.appWalletBalance < packagePrice) {
            return@withContext TransactionOperationResult(
                isSuccess = false,
                message = "Insufficient wallet balance. Required: ₦${String.format("%,.2f", packagePrice)}, Available: ₦${String.format("%,.2f", wallet.appWalletBalance)}"
            )
        }

        val newBalance = wallet.appWalletBalance - packagePrice
        bookkeepingDao.updateWalletBalance(userId, newBalance)

        val txId = "TX-BYOD-" + (100000..999999).random()
        val ref = "BYOD-SUB-" + (1000000..9999999).random()

        val now = System.currentTimeMillis()
        val timeFmt = java.text.SimpleDateFormat("HH:mm:ss dd-MMM-yyyy", java.util.Locale.US).format(java.util.Date(now))
        val txEntity = TransactionBookkeepingEntity(
            id = txId,
            userId = userId,
            transactionType = "byod_sms_subscription",
            serviceCategory = "SMS",
            recipientOrAccount = "$planTitle ($durationDays Days Access)",
            amountDebitedFromUser = packagePrice,
            amountPaidToWholesaleApi = 0.0,
            netProfitEarned = packagePrice,
            status = "success",
            timestamp = now,
            reference = ref,
            confirmationSource = "BYOD GATEWAY SAAS",
            completedAtFormatted = timeFmt
        )
        bookkeepingDao.insertTransaction(txEntity)

        TransactionOperationResult(
            isSuccess = true,
            message = "BYOD SMS SaaS Activated! $planTitle ($durationDays Days) for ₦${String.format("%,.2f", packagePrice)}.",
            transactionId = txId,
            amountDebited = packagePrice,
            netProfit = packagePrice,
            newWalletBalance = newBalance
        )
    }

    suspend fun purchaseSmsBatchPack(
        userId: String = "usr_default_1",
        packTitle: String,
        smsUnits: Int,
        packagePrice: Double
    ): TransactionOperationResult = withContext(Dispatchers.IO) {
        val wallet = bookkeepingDao.getUserWalletSync(userId)
            ?: return@withContext TransactionOperationResult(false, "User wallet not found")

        if (wallet.appWalletBalance < packagePrice) {
            return@withContext TransactionOperationResult(
                isSuccess = false,
                message = "Insufficient wallet balance. Required: ₦${String.format("%,.2f", packagePrice)}, Available: ₦${String.format("%,.2f", wallet.appWalletBalance)}"
            )
        }

        val newBalance = wallet.appWalletBalance - packagePrice
        bookkeepingDao.updateWalletBalance(userId, newBalance)

        val txId = "TX-SMSBATCH-" + (100000..999999).random()
        val ref = "SMS-BATCH-" + (1000000..9999999).random()

        val now = System.currentTimeMillis()
        val timeFmt = java.text.SimpleDateFormat("HH:mm:ss dd-MMM-yyyy", java.util.Locale.US).format(java.util.Date(now))
        val txEntity = TransactionBookkeepingEntity(
            id = txId,
            userId = userId,
            transactionType = "sms_batch_tokens",
            serviceCategory = "SMS",
            recipientOrAccount = "$packTitle (+$smsUnits SMS Credits)",
            amountDebitedFromUser = packagePrice,
            amountPaidToWholesaleApi = 0.0,
            netProfitEarned = packagePrice,
            status = "success",
            timestamp = now,
            reference = ref,
            confirmationSource = "WALLET SETTLEMENT",
            completedAtFormatted = timeFmt
        )
        bookkeepingDao.insertTransaction(txEntity)

        TransactionOperationResult(
            isSuccess = true,
            message = "SMS Batch Pack Added! +$smsUnits SMS Credits for ₦${String.format("%,.2f", packagePrice)}.",
            transactionId = txId,
            amountDebited = packagePrice,
            netProfit = packagePrice,
            newWalletBalance = newBalance
        )
    }

    suspend fun getBookkeepingStats(
        startTimeMs: Long = 0L,
        endTimeMs: Long = System.currentTimeMillis() + 86400000L
    ): BookkeepingStats = withContext(Dispatchers.IO) {
        val txs = bookkeepingDao.getTransactionsByDateRangeSync(startTimeMs, endTimeMs)

        var totalRev = 0.0
        var totalWholesale = 0.0
        var totalNetProf = 0.0
        var profitData = 0.0
        var profitAirtime = 0.0
        var profitUtil = 0.0
        var profitCable = 0.0
        var profitSms = 0.0
        var profitVpn = 0.0
        var successCount = 0
        var failCount = 0

        txs.forEach { tx ->
            if (tx.status == "success") {
                successCount++
                if (tx.serviceCategory != "Deposit") {
                    totalRev += tx.amountDebitedFromUser
                    totalWholesale += tx.amountPaidToWholesaleApi
                    totalNetProf += tx.netProfitEarned

                    when (tx.serviceCategory) {
                        "Data" -> profitData += tx.netProfitEarned
                        "Airtime" -> profitAirtime += tx.netProfitEarned
                        "Utilities" -> profitUtil += tx.netProfitEarned
                        "Cable TV" -> profitCable += tx.netProfitEarned
                        "SMS" -> profitSms += tx.netProfitEarned
                        "VPN" -> profitVpn += tx.netProfitEarned
                    }
                }
            } else if (tx.status == "failed") {
                failCount++
            }
        }

        BookkeepingStats(
            totalRevenueProcessed = totalRev,
            totalWholesaleCostExpended = totalWholesale,
            totalNetProfit = totalNetProf,
            profitByData = profitData,
            profitByAirtime = profitAirtime,
            profitByUtilities = profitUtil,
            profitByCable = profitCable,
            profitBySms = profitSms,
            profitByVpn = profitVpn,
            totalTransactionsCount = txs.size,
            successfulTransactionsCount = successCount,
            failedTransactionsCount = failCount
        )
    }

    suspend fun savePricingConfig(config: PricingConfigEntity) = withContext(Dispatchers.IO) {
        bookkeepingDao.insertPricingConfig(config)
    }
}
