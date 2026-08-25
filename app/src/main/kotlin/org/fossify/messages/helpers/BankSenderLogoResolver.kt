package org.fossify.messages.helpers

import android.content.Context
import android.provider.Telephony

/** Resolves known SMS sender IDs to drawable names. Provider access stays off the UI thread. */
object BankSenderLogoResolver {
    private val aliases = mapOf(
        "SEPAHBANK" to "bank_sepah",
        "TEJRATBANK" to "bank_tejarat",
        "TEJARATBANK" to "bank_tejarat",
        "BANKMELLI" to "bank_melli",
        "BANKMASKAN" to "bank_maskan",
        "MASKANBANK" to "bank_maskan",
        "BANKMELLAT" to "bank_mellat",
        "BANKMEHR" to "bank_mehr_iran",
        "MEHRIRAN" to "bank_mehr_iran",
        "TOSEETAAVON" to "bank_tosee_taavon",
        "TOSEETAAVONBANK" to "bank_tosee_taavon"
    )

    fun normalize(value: String): String = value.trim().uppercase()
        .replace("ك", "ک").replace("ي", "ی")
        .replace("‌", "").replace(" ", "").replace("-", "")

    fun resourceName(sender: String): String? {
        val normalized = normalize(sender)
        return aliases[normalized] ?: when {
            normalized.contains("SEPAH") -> "bank_sepah"
            normalized.contains("TEJARAT") || normalized.contains("TEJRAT") -> "bank_tejarat"
            normalized.contains("MELLI") -> "bank_melli"
            normalized.contains("MELLAT") -> "bank_mellat"
            normalized.contains("MASKAN") -> "bank_maskan"
            normalized.contains("MEHR") -> "bank_mehr_iran"
            normalized.contains("TAAVON") -> "bank_tosee_taavon"
            else -> null
        }
    }

    fun drawableId(context: Context, sender: String): Int {
        val name = resourceName(sender) ?: return 0
        return context.resources.getIdentifier(name, "drawable", context.packageName)
    }

    fun findLatestBankSender(context: Context, threadId: Long): String? {
        val projection = arrayOf(Telephony.Sms.ADDRESS)
        return try {
            context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                projection,
                "${Telephony.Sms.THREAD_ID}=?",
                arrayOf(threadId.toString()),
                "${Telephony.Sms.DATE} DESC"
            )?.use { cursor ->
                val address = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
                while (cursor.moveToNext()) {
                    if (address >= 0) {
                        val sender = cursor.getString(address).orEmpty()
                        if (resourceName(sender) != null) return@use resourceName(sender)
                    }
                }
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}
