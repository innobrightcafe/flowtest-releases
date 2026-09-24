package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.model.ElectricityAppliance
import com.example.data.model.ElectricityMeterConfig
import com.example.data.model.PresetElectricityAppliances
import com.example.util.AppNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

class ElectricityTrackerRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("electricity_tracker_prefs", Context.MODE_PRIVATE)

    private val _meterConfig = MutableStateFlow(loadMeterConfig())
    val meterConfig: StateFlow<ElectricityMeterConfig> = _meterConfig.asStateFlow()

    private val _appliances = MutableStateFlow(loadAppliances())
    val appliances: StateFlow<List<ElectricityAppliance>> = _appliances.asStateFlow()

    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    init {
        // Recalculate and trigger alert check on init
        repositoryScope.launch {
            checkAndTriggerLowBalanceAlert()
        }
    }

    // --- METER CONFIG MANAGEMENT ---

    fun updateMeterConfig(newConfig: ElectricityMeterConfig) {
        _meterConfig.value = newConfig
        saveMeterConfig(newConfig)
        checkAndTriggerLowBalanceAlert()
    }

    fun updateMeterNumberAndDisco(meterNumber: String, discoId: String, discoName: String, tariffBand: String, tariffRate: Double) {
        val current = _meterConfig.value
        val updated = current.copy(
            meterNumber = meterNumber.trim(),
            discoId = discoId,
            discoName = discoName,
            tariffBand = tariffBand,
            tariffRatePerKwh = tariffRate
        )
        updateMeterConfig(updated)
    }

    /**
     * User enters exact physical reading from CIU keypad to calibrate reading.
     */
    fun calibrateMeterBalance(exactKwh: Double) {
        val current = _meterConfig.value
        val updated = current.copy(
            currentBalanceKwh = exactKwh.coerceAtLeast(0.0),
            lastCalibratedTimestamp = System.currentTimeMillis()
        )
        updateMeterConfig(updated)
    }

    /**
     * Called when electricity token is purchased via Pairgate or manually entered.
     */
    fun addPurchasedToken(unitsKwh: Double, amountNaira: Double, tokenPin: String = "", meterNumber: String = "") {
        val current = _meterConfig.value
        val totalBurnKwh = calculateTotalDailyKwh()
        val currentEffectiveKwh = current.getEstimatedRemainingKwh(totalBurnKwh)
        val newBalance = currentEffectiveKwh + unitsKwh

        val updated = current.copy(
            meterNumber = if (meterNumber.isNotBlank()) meterNumber else current.meterNumber,
            currentBalanceKwh = newBalance,
            lastCalibratedTimestamp = System.currentTimeMillis(),
            lastRechargeAmountNaira = amountNaira,
            lastRechargeUnitsKwh = unitsKwh,
            lastRechargeToken = tokenPin
        )
        updateMeterConfig(updated)

        // Show toast or transaction notification
        AppNotificationManager.showTransactionNotification(
            context = context,
            title = "⚡ Electricity Token Credited!",
            message = "Added +${String.format(Locale.US, "%.1f", unitsKwh)} kWh to your meter. New estimated balance: ${String.format(Locale.US, "%.1f", newBalance)} kWh (~${String.format(Locale.US, "%.1f", updated.getEstimatedDaysRemaining(totalBurnKwh))} days left).",
            reference = if (tokenPin.isNotBlank()) tokenPin else "RECHARGE-METER",
            isSuccess = true
        )
    }

    fun updateLowBalanceAlertSettings(thresholdKwh: Double, isEnabled: Boolean) {
        val current = _meterConfig.value
        val updated = current.copy(
            lowBalanceAlertThresholdKwh = thresholdKwh,
            isLowBalanceAlertEnabled = isEnabled
        )
        updateMeterConfig(updated)
    }

    // --- APPLIANCE MANAGEMENT ---

    fun toggleAppliance(applianceId: String, isEnabled: Boolean) {
        val list = _appliances.value.toMutableList()
        val index = list.indexOfFirst { it.id == applianceId }
        if (index != -1) {
            list[index] = list[index].copy(isEnabled = isEnabled)
            _appliances.value = list
            saveAppliances(list)
            checkAndTriggerLowBalanceAlert()
        }
    }

    fun updateApplianceHours(applianceId: String, hours: Double) {
        val list = _appliances.value.toMutableList()
        val index = list.indexOfFirst { it.id == applianceId }
        if (index != -1) {
            list[index] = list[index].copy(hoursPerDay = hours.coerceIn(0.1, 24.0))
            _appliances.value = list
            saveAppliances(list)
            checkAndTriggerLowBalanceAlert()
        }
    }

    fun updateApplianceQuantity(applianceId: String, quantity: Int) {
        val list = _appliances.value.toMutableList()
        val index = list.indexOfFirst { it.id == applianceId }
        if (index != -1) {
            list[index] = list[index].copy(quantity = quantity.coerceIn(1, 100))
            _appliances.value = list
            saveAppliances(list)
            checkAndTriggerLowBalanceAlert()
        }
    }

    fun addAppliance(appliance: ElectricityAppliance) {
        val list = _appliances.value.toMutableList()
        list.add(0, appliance)
        _appliances.value = list
        saveAppliances(list)
        checkAndTriggerLowBalanceAlert()
    }

    fun deleteAppliance(applianceId: String) {
        val list = _appliances.value.toMutableList()
        list.removeAll { it.id == applianceId }
        _appliances.value = list
        saveAppliances(list)
        checkAndTriggerLowBalanceAlert()
    }

    fun resetToDefaultAppliances() {
        val defaults = PresetElectricityAppliances.getDefaultList()
        _appliances.value = defaults
        saveAppliances(defaults)
        checkAndTriggerLowBalanceAlert()
    }

    // --- CALCULATIONS ---

    fun calculateTotalDailyKwh(): Double {
        return _appliances.value.filter { it.isEnabled }.sumOf { it.dailyKwh }
    }

    fun calculateTotalConnectedWattage(): Int {
        return _appliances.value.filter { it.isEnabled }.sumOf { it.wattage * it.quantity }
    }

    fun calculateEstimatedRemainingKwh(): Double {
        val totalBurn = calculateTotalDailyKwh()
        return _meterConfig.value.getEstimatedRemainingKwh(totalBurn)
    }

    fun calculateEstimatedDaysRemaining(): Double {
        val totalBurn = calculateTotalDailyKwh()
        return _meterConfig.value.getEstimatedDaysRemaining(totalBurn)
    }

    // --- ALERT CHECKER ---

    fun checkAndTriggerLowBalanceAlert() {
        val config = _meterConfig.value
        if (!config.isLowBalanceAlertEnabled) return

        val totalBurn = calculateTotalDailyKwh()
        val remainingKwh = config.getEstimatedRemainingKwh(totalBurn)

        if (remainingKwh <= config.lowBalanceAlertThresholdKwh) {
            val now = System.currentTimeMillis()
            // Throttle alert to at most once every 6 hours
            if (now - config.lastAlertSentTimestamp > 6 * 3600 * 1000L) {
                val hoursRemaining = config.getEstimatedHoursRemaining(totalBurn)
                val daysRemaining = config.getEstimatedDaysRemaining(totalBurn)
                val timeRemainingStr = if (daysRemaining >= 1.0) {
                    String.format(Locale.US, "%.1f days", daysRemaining)
                } else {
                    String.format(Locale.US, "%.0f hours", hoursRemaining)
                }

                AppNotificationManager.showTransactionNotification(
                    context = context,
                    title = "⚠️ Low Electricity Meter Alert",
                    message = "Your meter has ~${String.format(Locale.US, "%.1f", remainingKwh)} kWh remaining ($timeRemainingStr left at current consumption). Top up your token to avoid darkness!",
                    reference = "METER-LOW-ALERT",
                    isSuccess = false
                )

                val updated = config.copy(lastAlertSentTimestamp = now)
                _meterConfig.value = updated
                saveMeterConfig(updated)
            }
        }
    }

    // --- INTERNAL PERSISTENCE ---

    private fun loadMeterConfig(): ElectricityMeterConfig {
        val meterNumber = prefs.getString("meter_number", "") ?: ""
        val discoId = prefs.getString("disco_id", "ikedc") ?: "ikedc"
        val discoName = prefs.getString("disco_name", "Ikeja Electric (IKEDC)") ?: "Ikeja Electric (IKEDC)"
        val meterType = prefs.getString("meter_type", "PREPAID") ?: "PREPAID"
        val tariffBand = prefs.getString("tariff_band", "Band A (20+ hrs)") ?: "Band A (20+ hrs)"
        val tariffRate = prefs.getFloat("tariff_rate", 209.50f).toDouble()
        val currentBalance = prefs.getFloat("current_balance_kwh", 100.0f).toDouble()
        val lastCalibrated = prefs.getLong("last_calibrated_ts", System.currentTimeMillis())
        val lastRechargeAmt = prefs.getFloat("last_recharge_amt", 0.0f).toDouble()
        val lastRechargeUnits = prefs.getFloat("last_recharge_units", 0.0f).toDouble()
        val lastRechargeToken = prefs.getString("last_recharge_token", "") ?: ""
        val lowAlertThreshold = prefs.getFloat("low_alert_threshold", 15.0f).toDouble()
        val isLowAlertEnabled = prefs.getBoolean("is_low_alert_enabled", true)
        val lastAlertSent = prefs.getLong("last_alert_sent_ts", 0L)

        return ElectricityMeterConfig(
            meterNumber = meterNumber,
            discoId = discoId,
            discoName = discoName,
            meterType = meterType,
            tariffBand = tariffBand,
            tariffRatePerKwh = tariffRate,
            currentBalanceKwh = currentBalance,
            lastCalibratedTimestamp = lastCalibrated,
            lastRechargeAmountNaira = lastRechargeAmt,
            lastRechargeUnitsKwh = lastRechargeUnits,
            lastRechargeToken = lastRechargeToken,
            lowBalanceAlertThresholdKwh = lowAlertThreshold,
            isLowBalanceAlertEnabled = isLowAlertEnabled,
            lastAlertSentTimestamp = lastAlertSent
        )
    }

    private fun saveMeterConfig(config: ElectricityMeterConfig) {
        prefs.edit()
            .putString("meter_number", config.meterNumber)
            .putString("disco_id", config.discoId)
            .putString("disco_name", config.discoName)
            .putString("meter_type", config.meterType)
            .putString("tariff_band", config.tariffBand)
            .putFloat("tariff_rate", config.tariffRatePerKwh.toFloat())
            .putFloat("current_balance_kwh", config.currentBalanceKwh.toFloat())
            .putLong("last_calibrated_ts", config.lastCalibratedTimestamp)
            .putFloat("last_recharge_amt", config.lastRechargeAmountNaira.toFloat())
            .putFloat("last_recharge_units", config.lastRechargeUnitsKwh.toFloat())
            .putString("last_recharge_token", config.lastRechargeToken)
            .putFloat("low_alert_threshold", config.lowBalanceAlertThresholdKwh.toFloat())
            .putBoolean("is_low_alert_enabled", config.isLowBalanceAlertEnabled)
            .putLong("last_alert_sent_ts", config.lastAlertSentTimestamp)
            .apply()
    }

    private fun loadAppliances(): List<ElectricityAppliance> {
        val jsonStr = prefs.getString("appliances_json", null)
        if (jsonStr.isNullOrBlank()) {
            val defaults = PresetElectricityAppliances.getDefaultList()
            saveAppliances(defaults)
            return defaults
        }

        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<ElectricityAppliance>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    ElectricityAppliance(
                        id = obj.optString("id", "app_${System.currentTimeMillis()}_$i"),
                        name = obj.optString("name", "Appliance"),
                        category = obj.optString("category", "Other"),
                        wattage = obj.optInt("wattage", 100),
                        quantity = obj.optInt("quantity", 1),
                        hoursPerDay = obj.optDouble("hoursPerDay", 4.0),
                        isEnabled = obj.optBoolean("isEnabled", true),
                        isCustom = obj.optBoolean("isCustom", false),
                        iconKey = obj.optString("iconKey", "default")
                    )
                )
            }
            if (list.isEmpty()) PresetElectricityAppliances.getDefaultList() else list
        } catch (e: Exception) {
            Log.e("ElectricityRepo", "Error parsing appliances JSON: ${e.message}")
            PresetElectricityAppliances.getDefaultList()
        }
    }

    private fun saveAppliances(list: List<ElectricityAppliance>) {
        try {
            val array = JSONArray()
            for (item in list) {
                val obj = JSONObject()
                obj.put("id", item.id)
                obj.put("name", item.name)
                obj.put("category", item.category)
                obj.put("wattage", item.wattage)
                obj.put("quantity", item.quantity)
                obj.put("hoursPerDay", item.hoursPerDay)
                obj.put("isEnabled", item.isEnabled)
                obj.put("isCustom", item.isCustom)
                obj.put("iconKey", item.iconKey)
                array.put(obj)
            }
            prefs.edit().putString("appliances_json", array.toString()).apply()
        } catch (e: Exception) {
            Log.e("ElectricityRepo", "Error saving appliances JSON: ${e.message}")
        }
    }
}
