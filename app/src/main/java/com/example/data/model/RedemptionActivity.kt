package com.example.data.model

enum class RedemptionCategory {
    VPN_TIME,
    CASH,
    NETWORK_CREDIT,
    PRO_UPGRADE
}

data class RedemptionActivity(
    val id: String,
    val title: String,            // e.g. "1 Day VPN", "₦100 Cash", "MTN ₦120", "Airtel ₦120"
    val subtitle: String,         // e.g. "24h VPN Time", "Direct to Wallet", "Airtime Top-up"
    val category: RedemptionCategory,
    val pointsCost: Int,          // e.g. 50, 90, 120, 180, 250, 350
    val rewardValue: Double,      // Minutes for VPN (e.g. 1440.0), Naira for Cash/Airtime (e.g. 120.0)
    val network: String = "",     // "MTN", "AIRTEL", "GLO", "9MOBILE"
    val isActive: Boolean = true, // Admin can toggle active / inactive. If inactive, it fades!
    val iconType: String = "vpn"  // "vpn", "cash", "mtn", "airtel", "glo", "9mobile", "pro"
)
