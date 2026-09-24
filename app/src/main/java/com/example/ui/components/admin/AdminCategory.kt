package com.example.ui.components.admin

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

enum class AdminCategory(
    val title: String,
    val shortLabel: String,
    val icon: ImageVector,
    val description: String
) {
    FINANCIALS(
        title = "Financials & Ledger",
        shortLabel = "Ledger",
        icon = Icons.Default.AccountBalanceWallet,
        description = "Corporate Moniepoint Ledger, Balances, Unresolved & Pending Orders"
    ),
    PRICING(
        title = "Pricing & Margins",
        shortLabel = "Pricing",
        icon = Icons.AutoMirrored.Filled.TrendingUp,
        description = "Reseller Margins, Wholesale Markups, User Discounts & Combos"
    ),
    CLIENTS(
        title = "Client Accounts",
        shortLabel = "Clients",
        icon = Icons.Default.SupervisorAccount,
        description = "Client Virtual NUBANs, KYC Status & Inbound Deposits"
    ),
    SMS_GATEWAY(
        title = "SMS Gateway",
        shortLabel = "SMS",
        icon = Icons.Default.Sms,
        description = "HttpSMS Gateway, Pricing & Real-Time DLR Delivery Stream"
    ),
    SECURITY_API(
        title = "API & Security",
        shortLabel = "Security",
        icon = Icons.Default.Security,
        description = "FlowTest Package Backbone, Cloud Run, Webhook Live Audit & Passcode"
    ),
    AUDIT_LOGS(
        title = "Audit & History",
        shortLabel = "Logs",
        icon = Icons.AutoMirrored.Filled.ReceiptLong,
        description = "Live VTU Vending Logs, Webhook Payloads & Diagnostic Alerts"
    ),
    BROADCAST_OFFERS(
        title = "Adverts & Offers",
        shortLabel = "Offers",
        icon = Icons.Default.Campaign,
        description = "Broadcast Custom Adverts, Rewards & Flash Offers via Push & Email"
    )
}
