package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.api.HttpSmsService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("FlowTest", appName)
  }

  @Test
  fun `verify phone number normalization`() {
    assertEquals("+2348137545370", HttpSmsService.normalizePhoneNumber("+2348137545370"))
    assertEquals("+2348103462171", HttpSmsService.normalizePhoneNumber("08103462171"))
    assertEquals("+2348103462171", HttpSmsService.normalizePhoneNumber("2348103462171"))
  }

  @Test
  fun `verify sms metrics calculation`() {
    val (chars, words, pages) = HttpSmsService.calculateSmsMetrics("FlowTest Security: Your verification code is 492019. Valid for 10 minutes.")
    assertTrue(chars > 0)
    assertTrue(words > 0)
    assertEquals(1, pages)
  }

  @Test
  fun `verify sms dispatch metrics and parameter validation`() = runBlocking {
    val result = HttpSmsService.dispatchSms(
      recipients = listOf("+2348103462171"),
      content = "FlowTest Security: Your verification code is 492019. Valid for 10 minutes.",
      senderId = "+2348137545370"
    )
    assertEquals(1, result.recipientCount)
    assertEquals(1, result.pageCount)
    assertTrue(result.charCount > 0)
  }
}

