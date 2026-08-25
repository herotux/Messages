package org.fossify.messages.helpers

import android.content.Context
import android.content.res.Resources
import android.provider.Telephony
import org.fossify.messages.R

/** Lightweight sender-id -> drawable mapping. No provider query is performed here. */
object BankSenderLogoResolver {
    private val aliases = mapOf(
        "SEPAHBANK" to "bank_sepah",
        "SEPAH BANK" to "bank_sepah",
        "TEJRATBANK" to "bank_tejarat",
        "TEJARATBANK" to "bank_tejarat",
        "BANKMELLI" to "bank_melli",
        "BANK MELLI" to "bank_melli",
        "BANKMASKAN" to "bank_maskan",
        "MASKANBANK" to "bank_maskan",
        "BANKMELLAT" to "bank_mellat",
        "BANKMEHR" to "bank_mehr_iran",
        "MEHRIRAN" to "bank_mehr_iran",
        "TOSEE TAAAVON" to "bank_tosee_taavon",
        "TOSEE TAAVON" to "bank_tosee_taavon"
    )

    fun normalize(value: String): String = value
        .trim()
        .uppercase()
        .replace("ك", "ک")
        .replace("ي", "ی")
        .replace("‌", "")
        .replace(" ", "")
        .replace("-", "")

    fun resourceName(sender: String): String? {
        val normalized = normalize(sender)
        return aliases[normalized]
            ?: when {
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
        return Resources.getSystem().displayMetrics.let {
            context.resources.getIdentifier(name, "drawable", context.packageName)
        }
    }

    fun findLatestBankSender(context: Context, threadId: Long): String? {
        if (threadId <= 0L) return null
        return try {
            context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                arrayOf(Telephony.Sms.ADDRESS),
                "${Telephony.Sms.THREAD_ID}=?",
                arrayOf(threadId.toString()),
                "${Telephony.Sms.DATE} DESC"
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val sender = cursor.getString(0).orEmpty()
                    if (resourceName(sender) != null) return@use sender
                }
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}
