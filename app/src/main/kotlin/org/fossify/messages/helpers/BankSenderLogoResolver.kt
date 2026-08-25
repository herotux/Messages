package org.fossify.messages.helpers

import android.content.Context

/** Pure sender-id -> drawable mapping. Provider queries are kept out of the adapter. */
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
}
