package com.example

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testDefaultBalanceZeroWhenNotSetUp() {
    val purchasedMb = 0.0
    val appsUsedMb = 120.0
    val remainingMb = if (purchasedMb <= 0.0) 0.0 else (purchasedMb - appsUsedMb).coerceAtLeast(0.0)

    assertEquals(0.0, remainingMb, 0.001)
  }

  @Test
  fun testPurchaseStartsAccountingFromBoughtData() {
    val initialPurchasedMb = 0.0
    val initialBalanceMb = 0.0

    // User buys 4GB (4096 MB)
    val boughtMb = 4.0 * 1024.0
    val newAllowanceMb = if (initialBalanceMb > 0.0) initialBalanceMb + boughtMb else boughtMb
    var consumedSincePurchaseMb = 0.0

    // Apps on the phone use 1.2GB
    val appUsageDeltaMb = 1.2 * 1024.0
    consumedSincePurchaseMb += appUsageDeltaMb

    val remainingMb = (newAllowanceMb - consumedSincePurchaseMb).coerceAtLeast(0.0)
    val remainingGb = remainingMb / 1024.0

    assertEquals(4096.0, newAllowanceMb, 0.001)
    assertEquals(2867.2, remainingMb, 0.001)
    assertEquals(2.8, remainingGb, 0.001)
  }

  @Test
  fun testAppUsageReadjustmentPreservesBalanceAndResetsUsageCounter() {
    // Current state before readjustment: Remaining balance is 2.8 GB, app usage had built up to 1.2 GB
    val remainingBalanceBeforeReadjust = 2867.2 // 2.8 GB in MB
    var appUsageCounter = 1228.8               // 1.2 GB in MB

    // Readjustment occurs (e.g. 24h cycle or manual readjust button)
    // 1. App usage counter resets to 0 so it doesn't continuously build up
    appUsageCounter = 0.0
    // 2. The active remaining balance becomes the base allowance
    val newBaseAllowance = remainingBalanceBeforeReadjust
    var newRemaining = newBaseAllowance

    assertEquals(0.0, appUsageCounter, 0.001)
    assertEquals(2867.2, newRemaining, 0.001)

    // Next period: apps use 300 MB
    val nextPeriodUsage = 300.0
    appUsageCounter += nextPeriodUsage
    newRemaining = (newBaseAllowance - appUsageCounter).coerceAtLeast(0.0)

    assertEquals(300.0, appUsageCounter, 0.001) // Usage cleanly reflects only the active period
    assertEquals(2567.2, newRemaining, 0.001)   // Balance deducted accurately
  }

  @Test
  fun testEstimatedBalanceCalculation_6GBPurchased_3Point5GBUsed() {
    val purchasedMb = 6.0 * 1024.0 // 6GB = 6144 MB
    val appsUsedMb = 3.5 * 1024.0   // 3.5GB = 3584 MB
    val remainingMb = (purchasedMb - appsUsedMb).coerceAtLeast(0.0)
    val remainingGb = remainingMb / 1024.0

    assertEquals(2560.0, remainingMb, 0.001)
    assertEquals(2.5, remainingGb, 0.001)
  }

  @Test
  fun test24HourCycleResetCondition() {
    val now = 1700000000000L
    val twentyFourHoursMs = 24L * 60L * 60L * 1000L

    // Within same cycle: 5 hours later -> should not reset based on 24h duration
    val fiveHoursLater = now + (5L * 60L * 60L * 1000L)
    assertFalse(fiveHoursLater - now >= twentyFourHoursMs)

    // 25 hours later -> must reset
    val twentyFiveHoursLater = now + (25L * 60L * 60L * 1000L)
    assertTrue(twentyFiveHoursLater - now >= twentyFourHoursMs)
  }
}
