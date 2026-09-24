package com.example.data.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class UsagePeriodType(val label: String, val shortLabel: String) {
    DAILY("Daily (Today)", "Today"),
    WEEKLY("Weekly (7 Days)", "7 Days"),
    MONTHLY("Monthly (30 Days)", "30 Days"),
    CUSTOM("Custom Range", "Custom");

    companion object {
        val TODAY = DAILY
        val LAST_7_DAYS = WEEKLY
        val LAST_30_DAYS = MONTHLY
    }
}

typealias UsagePeriod = UsagePeriodType

enum class NetworkInterfaceFilter(val label: String, val shortLabel: String) {
    ALL("All Networks (Cellular + Wi-Fi)", "All"),
    MOBILE("Cellular / SIM Data Only", "Mobile"),
    WIFI("Wi-Fi Only", "Wi-Fi")
}

data class DataUsageFilter(
    val periodType: UsagePeriodType = UsagePeriodType.DAILY,
    val networkFilter: NetworkInterfaceFilter = NetworkInterfaceFilter.ALL,
    val customStartTimestamp: Long? = null,
    val customEndTimestamp: Long? = null
) {
    val period: UsagePeriodType get() = periodType
    val networkType: NetworkInterfaceFilter get() = networkFilter

    fun getFormattedWindow(referenceTime: Long = System.currentTimeMillis()): String = getShortDateRange(referenceTime)

    fun copy(
        periodType: UsagePeriodType = this.periodType,
        networkFilter: NetworkInterfaceFilter = this.networkFilter,
        customStartTimestamp: Long? = this.customStartTimestamp,
        customEndTimestamp: Long? = this.customEndTimestamp,
        period: UsagePeriodType = periodType,
        networkType: NetworkInterfaceFilter = networkFilter
    ): DataUsageFilter = DataUsageFilter(
        periodType = period,
        networkFilter = networkType,
        customStartTimestamp = customStartTimestamp,
        customEndTimestamp = customEndTimestamp
    )
    fun getTimeInterval(referenceTime: Long = System.currentTimeMillis()): Pair<Long, Long> {
        val end = customEndTimestamp?.takeIf { it > 0L } ?: referenceTime
        val start = when (periodType) {
            UsagePeriodType.DAILY -> {
                val cal = Calendar.getInstance().apply {
                    timeInMillis = end
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                cal.timeInMillis
            }
            UsagePeriodType.WEEKLY -> {
                end - (7L * 24L * 60L * 60L * 1000L)
            }
            UsagePeriodType.MONTHLY -> {
                end - (30L * 24L * 60L * 60L * 1000L)
            }
            UsagePeriodType.CUSTOM -> {
                customStartTimestamp?.takeIf { it > 0L } ?: (end - (24L * 60L * 60L * 1000L))
            }
        }
        return Pair(start.coerceAtMost(end), end)
    }

    fun getFormattedRange(referenceTime: Long = System.currentTimeMillis()): String {
        val (start, end) = getTimeInterval(referenceTime)
        val dateFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
        return "${dateFormat.format(Date(start))} – ${dateFormat.format(Date(end))}"
    }

    fun getShortDateRange(referenceTime: Long = System.currentTimeMillis()): String {
        val (start, end) = getTimeInterval(referenceTime)
        val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
        return if (periodType == UsagePeriodType.DAILY) {
            "Today (${SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(end))})"
        } else {
            "${dateFormat.format(Date(start))} – ${dateFormat.format(Date(end))}"
        }
    }
}
