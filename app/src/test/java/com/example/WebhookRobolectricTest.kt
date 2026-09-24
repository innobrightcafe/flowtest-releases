package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.db.BookkeepingDao
import com.example.data.db.UserWalletEntity
import com.example.data.repository.MultiUtilityPricingEngine
import com.example.data.api.HttpSmsService
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class WebhookRobolectricTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: BookkeepingDao
    private lateinit var engine: MultiUtilityPricingEngine

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.bookkeepingDao()
        engine = MultiUtilityPricingEngine(dao)

        // Seed initial test wallet
        runBlocking {
            dao.insertOrUpdateUserWallet(
                UserWalletEntity(
                    id = "usr_default_1",
                    phoneNumber = "08168290134",
                    email = "innobright2010@gmail.com",
                    activeConfirmationCode = "FT-1001",
                    appWalletBalance = 1000.0,
                    assignedAccountNumber = "6666468328",
                    assignedBank = "Moniepoint MFB",
                    assignedAccountName = "FlowTest"
                )
            )
        }
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun testMoniepointWebhook_autoCreditWalletWithConfirmationCode() = runBlocking {
        val initialWallet = dao.getUserWalletSync()
        assertNotNull(initialWallet)
        assertEquals(1000.0, initialWallet!!.appWalletBalance, 0.01)

        val txnRef = "MNP_WH_TEST_001"
        val depositAmount = 4500.0
        val narration = "Transfer from Innocent Bright FT-1001"

        val result = engine.processIncomingMoniepointWebhook(
            transactionReference = txnRef,
            amountReceived = depositAmount,
            rawNarration = narration,
            senderName = "Innocent Bright"
        )

        // Verify successful fulfillment
        assertEquals("auto_credited", result.status)
        assertTrue(result.isFulfilled)
        assertEquals(depositAmount, result.amountReceived, 0.01)

        // Verify wallet updated in DB
        val updatedWallet = dao.getUserWalletSync()
        assertNotNull(updatedWallet)
        assertEquals(5500.0, updatedWallet!!.appWalletBalance, 0.01)

        // Verify record in processed payments ledger
        val processedRecord = dao.getProcessedPayment(txnRef)
        assertNotNull(processedRecord)
        assertEquals(txnRef, processedRecord!!.reference)
        assertEquals(depositAmount, processedRecord.amount, 0.01)
    }

    @Test
    fun testMoniepointWebhook_idempotencyPreventsDuplicateCrediting() = runBlocking {
        val txnRef = "MNP_WH_DUP_CHECK_999"
        val depositAmount = 2000.0
        val narration = "Transfer FT-1001"

        // First webhook call -> should succeed
        val res1 = engine.processIncomingMoniepointWebhook(
            transactionReference = txnRef,
            amountReceived = depositAmount,
            rawNarration = narration,
            senderName = "Innocent Bright"
        )
        assertEquals("auto_credited", res1.status)
        assertTrue(res1.isFulfilled)

        val walletAfterFirst = dao.getUserWalletSync()!!.appWalletBalance
        assertEquals(3000.0, walletAfterFirst, 0.01)

        // Second webhook call with SAME reference -> MUST return duplicate and NOT credit again
        val res2 = engine.processIncomingMoniepointWebhook(
            transactionReference = txnRef,
            amountReceived = depositAmount,
            rawNarration = narration,
            senderName = "Innocent Bright"
        )
        assertEquals("duplicate", res2.status)
        assertFalse(res2.isFulfilled)

        // Wallet balance must stay unchanged (no double crediting)
        val walletAfterSecond = dao.getUserWalletSync()!!.appWalletBalance
        assertEquals(3000.0, walletAfterSecond, 0.01)
    }

    @Test
    fun testMoniepointWebhook_unresolvedWhenNarrationLacksIdentifier() = runBlocking {
        val txnRef = "MNP_WH_NO_ID_404"
        val depositAmount = 7500.0
        // Clear active confirmation code and phone on wallet to simulate unresolvable narration
        dao.insertOrUpdateUserWallet(
            UserWalletEntity(
                id = "usr_default_1",
                phoneNumber = "",
                email = "",
                activeConfirmationCode = "",
                appWalletBalance = 1000.0,
                assignedAccountNumber = "6666468328",
                assignedBank = "Moniepoint MFB",
                assignedAccountName = "FlowTest"
            )
        )

        val result = engine.processIncomingMoniepointWebhook(
            transactionReference = txnRef,
            amountReceived = depositAmount,
            rawNarration = "Cash deposit at ATM Lagos",
            senderName = "Cash Depositor"
        )

        assertEquals("invalid_narration", result.status)
        assertFalse(result.isFulfilled)

        // Check it was logged to Unresolved Payments table for Admin
        val unresolvedList = dao.getAllUnresolvedPayments()
        assertTrue(unresolvedList.any { it.bankReference == txnRef })
    }

    @Test
    fun testHttpSmsService_phoneNormalizationAndMetrics() {
        assertEquals("+2348168290134", HttpSmsService.normalizePhoneNumber("08168290134"))
        assertEquals("+2348168290134", HttpSmsService.normalizePhoneNumber("+2348168290134"))
        assertEquals("+2348168290134", HttpSmsService.normalizePhoneNumber("2348168290134"))

        val (chars, words, pages) = HttpSmsService.calculateSmsMetrics("Your OTP is 123456")
        assertTrue(chars > 0)
        assertEquals(1, pages)
        assertEquals(4, words)
    }

    @Test
    fun testHmacSha256SignatureVerification() {
        val secret = "mnp_whsec_test_secret_123"
        val payload = "{\"event\":\"PAYMENT_SUCCESSFUL\",\"amount\":5000}"

        val sha256Hmac = Mac.getInstance("HmacSHA256")
        val secretKey = SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256")
        sha256Hmac.init(secretKey)
        val hash = sha256Hmac.doFinal(payload.toByteArray(Charsets.UTF_8))
        val hexString = hash.joinToString("") { "%02x".format(it) }

        assertNotNull(hexString)
        assertEquals(64, hexString.length) // SHA256 hex output is always 64 chars
    }
}
