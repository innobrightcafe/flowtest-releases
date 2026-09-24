package com.example.data.util

import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import java.util.UUID

data class DeviceContact(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val countryCode: String = "+234",
    val flagEmoji: String = "🇳🇬",
    val isOnFeedApp: Boolean = false,
    val isOnline: Boolean = false,
    val statusBio: String = "Available for VTU & Data",
    val avatarBgColorHex: Long = 0xFF00E5FF
)

object ContactsHelper {

    // Official support desk on the network for instant P2P Flow Chat
    val DEFAULT_FEED_COMMUNITY_USERS = listOf(
        DeviceContact(
            id = "usr_flowtest_support",
            name = "FlowTest Support",
            phoneNumber = "08168290134",
            countryCode = "+234",
            flagEmoji = "🇳🇬",
            isOnFeedApp = true,
            isOnline = true,
            statusBio = "⚡ Official 24/7 FlowTest Support & Billing Helpdesk",
            avatarBgColorHex = 0xFF00E5FF
        )
    )

    fun hasContactPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun cleanPhoneNumber(rawNumber: String): String {
        return rawNumber.replace(" ", "")
            .replace("-", "")
            .replace("(", "")
            .replace(")", "")
            .trim()
    }

    fun isKnownFeedAppNumber(phoneNumber: String, registeredNumbers: Set<String> = emptySet()): Boolean {
        val clean = cleanPhoneNumber(phoneNumber)
        if (clean.isBlank()) return false
        val last10 = clean.takeLast(10)

        // Check against pre-seeded users
        val matchPreseeded = DEFAULT_FEED_COMMUNITY_USERS.any {
            val preseededClean = cleanPhoneNumber(it.phoneNumber)
            preseededClean == clean || preseededClean.takeLast(10) == last10
        }
        if (matchPreseeded) return true

        // Check against dynamic registered app numbers
        return registeredNumbers.any {
            val regClean = cleanPhoneNumber(it)
            regClean == clean || regClean.takeLast(10) == last10
        }
    }

    fun getDeviceContacts(context: Context, registeredFeedNumbers: Set<String> = emptySet()): List<DeviceContact> {
        val contactsList = mutableListOf<DeviceContact>()
        if (!hasContactPermission(context)) {
            return DEFAULT_FEED_COMMUNITY_USERS
        }

        try {
            val cursor: Cursor? = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone._ID,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )

            cursor?.use {
                val idIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone._ID)
                val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                val seenNumbers = mutableSetOf<String>()

                while (it.moveToNext()) {
                    val id = if (idIdx != -1) it.getString(idIdx) else UUID.randomUUID().toString()
                    val name = if (nameIdx != -1) it.getString(nameIdx) ?: "Contact" else "Contact"
                    val number = if (numberIdx != -1) it.getString(numberIdx) ?: "" else ""

                    val clean = cleanPhoneNumber(number)
                    if (clean.isNotBlank() && clean.length >= 7 && !seenNumbers.contains(clean)) {
                        seenNumbers.add(clean)
                        val (code, flag) = deriveCountryCodeAndFlag(clean)
                        val onFeedApp = isKnownFeedAppNumber(clean, registeredFeedNumbers)
                        contactsList.add(
                            DeviceContact(
                                id = id,
                                name = name,
                                phoneNumber = clean,
                                countryCode = code,
                                flagEmoji = flag,
                                isOnFeedApp = onFeedApp,
                                isOnline = onFeedApp && (clean.hashCode() % 2 == 0),
                                statusBio = if (onFeedApp) "⚡ Available on Feed App • Flow Chat" else "SMS Contact",
                                avatarBgColorHex = deriveColor(clean)
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            return DEFAULT_FEED_COMMUNITY_USERS
        }

        // Merge preseeded feed contacts if not already in phonebook
        val finalMerged = contactsList.toMutableList()
        for (feedUser in DEFAULT_FEED_COMMUNITY_USERS) {
            val clean = cleanPhoneNumber(feedUser.phoneNumber)
            if (!seenContains(finalMerged, clean)) {
                finalMerged.add(0, feedUser)
            }
        }

        return finalMerged
    }

    private fun seenContains(list: List<DeviceContact>, cleanPhone: String): Boolean {
        val last10 = cleanPhone.takeLast(10)
        return list.any { cleanPhoneNumber(it.phoneNumber).takeLast(10) == last10 }
    }

    private fun deriveColor(phone: String): Long {
        val colors = listOf(0xFF00E5FF, 0xFF00E676, 0xFFFFAB00, 0xFF7C4DFF, 0xFFFF4081, 0xFF2979FF, 0xFF00B0FF)
        val idx = Math.abs(phone.hashCode()) % colors.size
        return colors[idx]
    }

    private fun deriveCountryCodeAndFlag(phone: String): Pair<String, String> {
        return when {
            phone.startsWith("+234") || phone.startsWith("08") || phone.startsWith("07") || phone.startsWith("09") -> Pair("+234", "🇳🇬")
            phone.startsWith("+1") -> Pair("+1", "🇺🇸")
            phone.startsWith("+44") -> Pair("+44", "🇬🇧")
            phone.startsWith("+233") -> Pair("+233", "🇬🇭")
            phone.startsWith("+254") -> Pair("+254", "🇰🇪")
            phone.startsWith("+27") -> Pair("+27", "🇿🇦")
            else -> Pair("+234", "🌍")
        }
    }

    fun getFallbackContacts(): List<DeviceContact> {
        return DEFAULT_FEED_COMMUNITY_USERS
    }
}
