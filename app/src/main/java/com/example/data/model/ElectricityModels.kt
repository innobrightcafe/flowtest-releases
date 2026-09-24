package com.example.data.model

import java.util.Locale

/**
 * Data models for Electricity Consumption & Smart Meter Tracker.
 */
data class ElectricityAppliance(
    val id: String,
    val name: String,
    val category: String, // "Cooling", "Refrigeration", "Entertainment", "Lighting", "Kitchen", "Pumping", "Other"
    val wattage: Int, // in Watts (e.g., 1100 for 1.5HP Inverter AC)
    val quantity: Int = 1,
    val hoursPerDay: Double = 6.0,
    val isEnabled: Boolean = true,
    val isCustom: Boolean = false,
    val iconKey: String = "default" // "ac", "fridge", "tv", "fan", "bulb", "pump", "kettle", "microwave", "pc", "iron"
) {
    /**
     * Daily consumption in Kilowatt-hours (kWh)
     * Formula: (Wattage * Quantity * Hours) / 1000
     */
    val dailyKwh: Double
        get() = if (!isEnabled) 0.0 else (wattage.toDouble() * quantity * hoursPerDay) / 1000.0

    /**
     * Monthly consumption in kWh (approx 30 days)
     */
    val monthlyKwh: Double
        get() = dailyKwh * 30.0

    /**
     * Daily cost in Naira based on the given tariff rate
     */
    fun dailyCostNaira(tariffRate: Double): Double = dailyKwh * tariffRate

    /**
     * Monthly cost in Naira based on the given tariff rate
     */
    fun monthlyCostNaira(tariffRate: Double): Double = monthlyKwh * tariffRate
}

data class ElectricityMeterConfig(
    val meterNumber: String = "",
    val discoId: String = "ikedc",
    val discoName: String = "Ikeja Electric (IKEDC)",
    val meterType: String = "PREPAID", // "PREPAID" or "POSTPAID"
    val tariffBand: String = "Band A (20+ hrs)", // "Band A (20+ hrs)", "Band B (16-20 hrs)", "Band C", "Custom"
    val tariffRatePerKwh: Double = 209.50, // Nigerian NERC Band A standard ~₦209.50/kWh
    val currentBalanceKwh: Double = 100.0, // Last recorded or calibrated kWh balance
    val lastCalibratedTimestamp: Long = System.currentTimeMillis(),
    val lastRechargeAmountNaira: Double = 0.0,
    val lastRechargeUnitsKwh: Double = 0.0,
    val lastRechargeToken: String = "",
    val lowBalanceAlertThresholdKwh: Double = 15.0, // Default trigger alert when <= 15 kWh
    val isLowBalanceAlertEnabled: Boolean = true,
    val lastAlertSentTimestamp: Long = 0L
) {
    /**
     * Computes the real-time estimated remaining kWh balance taking into account
     * the elapsed time since last calibration and active appliances consumption burn rate.
     */
    fun getEstimatedRemainingKwh(totalDailyBurnKwh: Double): Double {
        if (currentBalanceKwh <= 0.0) return 0.0
        val now = System.currentTimeMillis()
        val elapsedMs = (now - lastCalibratedTimestamp).coerceAtLeast(0L)
        val elapsedDays = elapsedMs.toDouble() / (24.0 * 3600.0 * 1000.0)
        val consumedSinceCalibration = totalDailyBurnKwh * elapsedDays
        return (currentBalanceKwh - consumedSinceCalibration).coerceAtLeast(0.0)
    }

    /**
     * Estimates days of power remaining at current household burn rate.
     */
    fun getEstimatedDaysRemaining(totalDailyBurnKwh: Double): Double {
        val remainingKwh = getEstimatedRemainingKwh(totalDailyBurnKwh)
        if (totalDailyBurnKwh <= 0.001) return 999.0
        return (remainingKwh / totalDailyBurnKwh).coerceAtLeast(0.0)
    }

    /**
     * Estimates hours of power remaining.
     */
    fun getEstimatedHoursRemaining(totalDailyBurnKwh: Double): Double {
        return getEstimatedDaysRemaining(totalDailyBurnKwh) * 24.0
    }

    /**
     * Estimated monetary value of remaining units.
     */
    fun getEstimatedValueNaira(totalDailyBurnKwh: Double): Double {
        return getEstimatedRemainingKwh(totalDailyBurnKwh) * tariffRatePerKwh
    }

    /**
     * Formats remaining balance nicely with 1 decimal place.
     */
    fun formatRemainingBalance(totalDailyBurnKwh: Double): String {
        val bal = getEstimatedRemainingKwh(totalDailyBurnKwh)
        return String.format(Locale.US, "%.1f", bal)
    }
}

/**
 * Standard preset Nigerian & global household appliances with realistic wattages.
 */
object PresetElectricityAppliances {
    fun getDefaultList(): List<ElectricityAppliance> = listOf(
        ElectricityAppliance(
            id = "app_ac_inverter",
            name = "1.5HP Inverter AC",
            category = "Cooling",
            wattage = 1100,
            quantity = 1,
            hoursPerDay = 6.0,
            isEnabled = true,
            iconKey = "ac"
        ),
        ElectricityAppliance(
            id = "app_refrigerator",
            name = "Double-Door Refrigerator",
            category = "Refrigeration",
            wattage = 200,
            quantity = 1,
            hoursPerDay = 18.0,
            isEnabled = true,
            iconKey = "fridge"
        ),
        ElectricityAppliance(
            id = "app_deep_freezer",
            name = "Chest Deep Freezer",
            category = "Refrigeration",
            wattage = 250,
            quantity = 1,
            hoursPerDay = 12.0,
            isEnabled = false,
            iconKey = "fridge"
        ),
        ElectricityAppliance(
            id = "app_ceiling_fans",
            name = "Ceiling Fans",
            category = "Cooling",
            wattage = 75,
            quantity = 3,
            hoursPerDay = 10.0,
            isEnabled = true,
            iconKey = "fan"
        ),
        ElectricityAppliance(
            id = "app_smart_tv",
            name = "50-inch Smart LED TV",
            category = "Entertainment",
            wattage = 90,
            quantity = 1,
            hoursPerDay = 6.0,
            isEnabled = true,
            iconKey = "tv"
        ),
        ElectricityAppliance(
            id = "app_soundbar",
            name = "Home Theater / Soundbar",
            category = "Entertainment",
            wattage = 80,
            quantity = 1,
            hoursPerDay = 4.0,
            isEnabled = true,
            iconKey = "tv"
        ),
        ElectricityAppliance(
            id = "app_led_bulbs",
            name = "LED Light Bulbs",
            category = "Lighting",
            wattage = 9,
            quantity = 8,
            hoursPerDay = 8.0,
            isEnabled = true,
            iconKey = "bulb"
        ),
        ElectricityAppliance(
            id = "app_water_pump",
            name = "Water Borehole Pump (1HP)",
            category = "Pumping",
            wattage = 750,
            quantity = 1,
            hoursPerDay = 1.0,
            isEnabled = true,
            iconKey = "pump"
        ),
        ElectricityAppliance(
            id = "app_microwave",
            name = "Microwave Oven",
            category = "Kitchen",
            wattage = 1200,
            quantity = 1,
            hoursPerDay = 0.5,
            isEnabled = true,
            iconKey = "microwave"
        ),
        ElectricityAppliance(
            id = "app_electric_kettle",
            name = "Electric Kettle",
            category = "Kitchen",
            wattage = 1500,
            quantity = 1,
            hoursPerDay = 0.3,
            isEnabled = true,
            iconKey = "kettle"
        ),
        ElectricityAppliance(
            id = "app_pressing_iron",
            name = "Electric Pressing Iron",
            category = "Kitchen",
            wattage = 1000,
            quantity = 1,
            hoursPerDay = 0.5,
            isEnabled = true,
            iconKey = "iron"
        ),
        ElectricityAppliance(
            id = "app_laptop_pc",
            name = "Laptop / Workstation",
            category = "Entertainment",
            wattage = 65,
            quantity = 1,
            hoursPerDay = 8.0,
            isEnabled = true,
            iconKey = "pc"
        ),
        ElectricityAppliance(
            id = "app_wifi_router",
            name = "Wi-Fi Router & Decoder",
            category = "Entertainment",
            wattage = 20,
            quantity = 1,
            hoursPerDay = 24.0,
            isEnabled = true,
            iconKey = "pc"
        )
    )
}
