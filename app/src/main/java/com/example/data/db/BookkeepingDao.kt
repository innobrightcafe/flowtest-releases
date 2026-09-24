package com.example.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BookkeepingDao {

    // User Wallet Queries
    @Query("SELECT * FROM users_wallets WHERE id = :userId")
    fun getUserWalletFlow(userId: String = "usr_default_1"): Flow<UserWalletEntity?>

    @Query("SELECT * FROM users_wallets WHERE id = :userId")
    suspend fun getUserWalletSync(userId: String = "usr_default_1"): UserWalletEntity?

    @Query("SELECT * FROM users_wallets WHERE activeConfirmationCode = :identifier OR phoneNumber = :phoneNumber OR phoneNumber LIKE '%' || :last10Digits LIMIT 1")
    suspend fun getUserWalletByPhone(phoneNumber: String, last10Digits: String = phoneNumber.takeLast(10), identifier: String = phoneNumber): UserWalletEntity?

    @Query("SELECT * FROM users_wallets WHERE activeConfirmationCode = :code OR REPLACE(activeConfirmationCode, '-', '') = REPLACE(:code, '-', '') LIMIT 1")
    suspend fun getUserWalletByConfirmationCode(code: String): UserWalletEntity?

    @Query("UPDATE users_wallets SET activeConfirmationCode = :newCode WHERE id = :userId")
    suspend fun updateUserConfirmationCode(userId: String = "usr_default_1", newCode: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateUserWallet(userWallet: UserWalletEntity)

    @Query("UPDATE users_wallets SET appWalletBalance = :newBalance WHERE id = :userId")
    suspend fun updateWalletBalance(userId: String = "usr_default_1", newBalance: Double)

    @Query("UPDATE users_wallets SET appWalletBalance = :newBalance WHERE phoneNumber = :phone OR phoneNumber LIKE '%' || :last10Digits")
    suspend fun updateWalletBalanceByPhone(phone: String, newBalance: Double, last10Digits: String = phone.takeLast(10))

    @Query("UPDATE users_wallets SET appWalletBalance = 0.0")
    suspend fun resetAllUserBalancesToZero()

    @Query("UPDATE users_wallets SET appWalletBalance = appWalletBalance + :amount WHERE id = :userId")
    suspend fun creditUserWallet(userId: String = "usr_default_1", amount: Double)

    // Pricing Config Queries
    @Query("SELECT * FROM pricing_configs")
    fun getAllPricingConfigs(): Flow<List<PricingConfigEntity>>

    @Query("SELECT * FROM pricing_configs WHERE planId = :planId LIMIT 1")
    suspend fun getPricingConfigByPlanId(planId: String): PricingConfigEntity?

    @Query("SELECT * FROM pricing_configs WHERE serviceType = :serviceType AND providerId = :providerId LIMIT 1")
    suspend fun getPricingConfigByServiceAndProvider(serviceType: String, providerId: String): PricingConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPricingConfigs(configs: List<PricingConfigEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPricingConfig(config: PricingConfigEntity)

    // Client Virtual Accounts (Admin Tracking & User Management)
    @Query("SELECT * FROM client_virtual_accounts ORDER BY createdAt DESC")
    fun getAllClientAccounts(): Flow<List<ClientAccountEntity>>

    @Query("SELECT * FROM client_virtual_accounts ORDER BY createdAt DESC")
    suspend fun getAllClientAccountsSync(): List<ClientAccountEntity>

    @Query("SELECT * FROM client_virtual_accounts WHERE accountNumber = :accountNumber LIMIT 1")
    suspend fun getClientAccountByNumber(accountNumber: String): ClientAccountEntity?

    @Query("SELECT * FROM client_virtual_accounts WHERE id = :id LIMIT 1")
    suspend fun getClientAccountById(id: String): ClientAccountEntity?

    @Query("SELECT * FROM client_virtual_accounts WHERE (:email != '' AND LOWER(customerEmail) = LOWER(:email)) OR (:phone != '' AND (customerPhone = :phone OR customerPhone LIKE '%' || :last10Digits)) LIMIT 1")
    suspend fun getClientAccountByEmailOrPhone(email: String, phone: String, last10Digits: String = if (phone.length >= 10) phone.takeLast(10) else phone): ClientAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClientAccounts(accounts: List<ClientAccountEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClientAccount(account: ClientAccountEntity)

    @Query("UPDATE client_virtual_accounts SET totalFunded = totalFunded + :amount WHERE accountNumber = :accountNumber")
    suspend fun recordClientAccountDeposit(accountNumber: String, amount: Double)

    @Query("UPDATE client_virtual_accounts SET status = :status WHERE id = :id")
    suspend fun updateClientAccountStatus(id: String, status: String)

    @Query("UPDATE client_virtual_accounts SET role = :role WHERE id = :id")
    suspend fun updateClientAccountRole(id: String, role: String)

    @Query("UPDATE client_virtual_accounts SET walletBalance = :balance WHERE id = :id")
    suspend fun updateClientAccountBalance(id: String, balance: Double)

    @Query("UPDATE client_virtual_accounts SET walletBalance = :balance WHERE customerPhone = :phone OR customerPhone LIKE '%' || :last10Digits OR customerEmail = :phone")
    suspend fun updateClientAccountBalanceByPhone(phone: String, balance: Double, last10Digits: String = phone.takeLast(10))

    @Query("UPDATE client_virtual_accounts SET userPin = :pin WHERE id = :id")
    suspend fun updateClientAccountPin(id: String, pin: String)

    @Query("DELETE FROM client_virtual_accounts WHERE id = :id")
    suspend fun deleteClientAccount(id: String)

    @Query("DELETE FROM client_virtual_accounts WHERE id IN (:ids)")
    suspend fun deleteClientAccountsByIds(ids: List<String>)

    // Transactions & Bookkeeping Queries
    @Query("SELECT * FROM transactions_bookkeeping ORDER BY timestamp DESC")
    fun getAllBookkeepingTransactions(): Flow<List<TransactionBookkeepingEntity>>

    @Query("SELECT * FROM transactions_bookkeeping ORDER BY timestamp DESC")
    suspend fun getAllTransactionsSync(): List<TransactionBookkeepingEntity>

    @Query("SELECT * FROM transactions_bookkeeping WHERE timestamp >= :startTimeMs AND timestamp <= :endTimeMs ORDER BY timestamp DESC")
    fun getTransactionsByDateRange(startTimeMs: Long, endTimeMs: Long): Flow<List<TransactionBookkeepingEntity>>

    @Query("SELECT * FROM transactions_bookkeeping WHERE timestamp >= :startTimeMs AND timestamp <= :endTimeMs")
    suspend fun getTransactionsByDateRangeSync(startTimeMs: Long, endTimeMs: Long): List<TransactionBookkeepingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionBookkeepingEntity)

    @Query("SELECT * FROM transactions_bookkeeping WHERE id = :txId OR reference = :txId LIMIT 1")
    suspend fun getTransactionById(txId: String): TransactionBookkeepingEntity?

    @Query("DELETE FROM transactions_bookkeeping WHERE id = :txId")
    suspend fun deleteTransaction(txId: String)

    @Query("UPDATE transactions_bookkeeping SET status = :status WHERE id = :txId")
    suspend fun updateTransactionStatus(txId: String, status: String)

    @Query("SELECT COALESCE(SUM(amountDebitedFromUser), 0.0) FROM transactions_bookkeeping WHERE status = 'success'")
    fun getTotalRevenueProcessed(): Flow<Double>

    @Query("SELECT COALESCE(SUM(amountPaidToWholesaleApi), 0.0) FROM transactions_bookkeeping WHERE status = 'success'")
    fun getTotalWholesaleCostExpended(): Flow<Double>

    @Query("SELECT COALESCE(SUM(netProfitEarned), 0.0) FROM transactions_bookkeeping WHERE status = 'success'")
    fun getTotalNetProfitEarned(): Flow<Double>

    // Pending Orders (Narration-based Bank Transfer Matching)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingOrder(order: PendingOrderEntity)

    @Query("UPDATE pending_orders SET status = 'cancelled' WHERE (narrationCode = :phoneNumber OR phoneNumber = :phoneNumber) AND status = 'pending'")
    suspend fun cancelPendingOrdersForPhone(phoneNumber: String)

    @Query("SELECT * FROM pending_orders WHERE (narrationCode = :phoneNumber OR phoneNumber = :phoneNumber) AND status = 'pending' ORDER BY createdAt DESC LIMIT 1")
    suspend fun getFreshestPendingOrderForPhone(phoneNumber: String): PendingOrderEntity?

    @Query("SELECT * FROM pending_orders WHERE narrationCode = :code AND status = 'pending' ORDER BY createdAt DESC LIMIT 1")
    suspend fun getFreshestPendingOrderForNarrationCode(code: String): PendingOrderEntity?

    @Query("SELECT * FROM pending_orders ORDER BY createdAt DESC")
    fun getAllPendingOrdersFlow(): Flow<List<PendingOrderEntity>>

    @Query("SELECT * FROM pending_orders ORDER BY createdAt DESC")
    suspend fun getAllPendingOrdersSync(): List<PendingOrderEntity>

    @Query("UPDATE pending_orders SET status = :status, bankTransactionRef = :bankRef WHERE id = :orderId")
    suspend fun updatePendingOrderStatus(orderId: String, status: String, bankRef: String?)

    @Query("SELECT * FROM pending_orders WHERE id = :orderId LIMIT 1")
    suspend fun getPendingOrderById(orderId: String): PendingOrderEntity?

    @Query("UPDATE pending_orders SET status = :status WHERE id = :orderId")
    suspend fun updatePendingOrderStatusOnly(orderId: String, status: String)

    @Query("SELECT * FROM pending_orders WHERE (narrationCode = :phoneNumber OR phoneNumber = :phoneNumber) AND status = 'pending'")
    suspend fun getActivePendingOrdersForPhone(phoneNumber: String): List<PendingOrderEntity>

    // Idempotency Tracker (Processed Payments)
    @Query("SELECT * FROM processed_payments WHERE reference = :reference LIMIT 1")
    suspend fun getProcessedPayment(reference: String): ProcessedPaymentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProcessedPayment(payment: ProcessedPaymentEntity)

    @Query("SELECT * FROM processed_payments ORDER BY resolvedAt DESC")
    fun getAllProcessedPaymentsFlow(): Flow<List<ProcessedPaymentEntity>>

    @Query("SELECT * FROM processed_payments WHERE (extractedPhone = :phoneNumber OR rawNarration LIKE '%' || :phoneNumber || '%') ORDER BY resolvedAt DESC LIMIT 5")
    suspend fun getRecentProcessedPaymentsForPhone(phoneNumber: String): List<ProcessedPaymentEntity>

    @Query("SELECT * FROM processed_payments WHERE extractedPhone = :codeOrPhone OR rawNarration LIKE '%' || :codeOrPhone || '%' OR reference = :codeOrPhone ORDER BY resolvedAt DESC LIMIT 5")
    suspend fun getRecentProcessedPaymentsForCodeOrPhone(codeOrPhone: String): List<ProcessedPaymentEntity>

    @Query("SELECT * FROM transactions_bookkeeping WHERE (recipientOrAccount LIKE '%' || :phoneNumber || '%' OR reference LIKE '%' || :phoneNumber || '%') AND serviceCategory = 'Deposit' ORDER BY timestamp DESC LIMIT 5")
    suspend fun getRecentDepositsForPhone(phoneNumber: String): List<TransactionBookkeepingEntity>

    // Unresolved Payments (Admin Resolution Desk)
    @Query("SELECT * FROM unresolved_payments ORDER BY createdAt DESC")
    fun getAllUnresolvedPaymentsFlow(): Flow<List<UnresolvedPaymentEntity>>

    @Query("SELECT * FROM unresolved_payments ORDER BY createdAt DESC")
    suspend fun getAllUnresolvedPayments(): List<UnresolvedPaymentEntity>

    @Query("SELECT * FROM unresolved_payments WHERE id = :id LIMIT 1")
    suspend fun getUnresolvedPaymentById(id: String): UnresolvedPaymentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnresolvedPayment(unresolved: UnresolvedPaymentEntity)

    @Query("UPDATE unresolved_payments SET status = :status, resolutionNotes = :notes WHERE id = :id")
    suspend fun updateUnresolvedPaymentStatus(id: String, status: String, notes: String)

    @Query("DELETE FROM unresolved_payments WHERE id = :id")
    suspend fun deleteUnresolvedPayment(id: String)

    // Saved Recipients & Beneficiaries
    @Query("SELECT * FROM saved_recipients ORDER BY isFavorite DESC, createdAt DESC")
    fun getAllSavedRecipientsFlow(): Flow<List<SavedRecipientEntity>>

    @Query("SELECT * FROM saved_recipients WHERE recipientType = :type ORDER BY isFavorite DESC, createdAt DESC")
    fun getSavedRecipientsByTypeFlow(type: String): Flow<List<SavedRecipientEntity>>

    @Query("SELECT * FROM saved_recipients WHERE identifier = :identifier LIMIT 1")
    suspend fun getSavedRecipientByIdentifier(identifier: String): SavedRecipientEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedRecipient(recipient: SavedRecipientEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedRecipients(recipients: List<SavedRecipientEntity>)

    @Delete
    suspend fun deleteSavedRecipient(recipient: SavedRecipientEntity)

    @Query("DELETE FROM saved_recipients WHERE id = :id")
    suspend fun deleteSavedRecipientById(id: String)

    // Detailed Inbound Notification Audit Logs (Moniepoint, Pairgate, Gmail)
    @Query("SELECT * FROM inbound_notification_audit_logs ORDER BY timestamp DESC")
    fun getAllInboundAuditLogsFlow(): Flow<List<InboundNotificationAuditLogEntity>>

    @Query("SELECT * FROM inbound_notification_audit_logs ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentInboundAuditLogs(limit: Int = 50): List<InboundNotificationAuditLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInboundAuditLog(log: InboundNotificationAuditLogEntity)

    @Query("DELETE FROM inbound_notification_audit_logs")
    suspend fun clearAllInboundAuditLogs()
}
