package org.fossify.messages.helpers

/** Exact sender profiles observed in real Iranian bank SMS samples. */
object IranianBankSenderProfiles {
    private val normalizedSenders = mapOf(
        "+989820004747" to IranianBankRegistry.BankId.RESALAT,
        "98500014747" to IranianBankRegistry.BankId.RESALAT,
        "RESALATBANK" to IranianBankRegistry.BankId.RESALAT,
        "+989820003501" to IranianBankRegistry.BankId.SHAHR,
        "+9820003501" to IranianBankRegistry.BankId.SHAHR,
        "+9820003502" to IranianBankRegistry.BankId.SHAHR,
        "MASKANBANK" to IranianBankRegistry.BankId.MASKAN,
        "BANKMASKAN" to IranianBankRegistry.BankId.MASKAN,

        // Bank Melli: exact observed phone and human-readable sender variants.
        "+9830009417" to IranianBankRegistry.BankId.MELLI,
        "+98100041415001" to IranianBankRegistry.BankId.MELLI,
        "+98700717" to IranianBankRegistry.BankId.MELLI,
        "BMIR" to IranianBankRegistry.BankId.MELLI,
        "BMI.IR" to IranianBankRegistry.BankId.MELLI,
        "BANKMELLI" to IranianBankRegistry.BankId.MELLI,
        "BANKMELLIIRAN" to IranianBankRegistry.BankId.MELLI,
        "BANK MELLI" to IranianBankRegistry.BankId.MELLI,

        // Bank Sepah: Android may preserve spaces/separators in the sender.
        "BANKSEPAH" to IranianBankRegistry.BankId.SEPAH,
        "SEPAHBANK" to IranianBankRegistry.BankId.SEPAH,
        "SEPAH BANK" to IranianBankRegistry.BankId.SEPAH,
        "30001557" to IranianBankRegistry.BankId.SEPAH,
        "671557" to IranianBankRegistry.BankId.SEPAH,
        "1557" to IranianBankRegistry.BankId.SEPAH,
        "بانکسپه" to IranianBankRegistry.BankId.SEPAH,

        // Bank Tejarat sender variants.
        "TEJARATBANK" to IranianBankRegistry.BankId.TEJARAT,
        "BANKTEJARAT" to IranianBankRegistry.BankId.TEJARAT,
        "TEJARAT BANK" to IranianBankRegistry.BankId.TEJARAT,
    )

    fun find(sender: String): IranianBankRegistry.BankInfo? {
        val normalized = normalize(sender)
        val id = normalizedSenders[normalized] ?: return null
        return IranianBankRegistry.findById(id)
    }

    /**
     * Canonicalizes Android SMS sender IDs before lookup.
     *
     * Keep '+' for international phone-number senders. For alphanumeric
     * sender IDs, discard separators/punctuation and normalize case so that
     * values such as "SEPAH BANK", "SEPAH-BANK" and "SEPAHBANK" all resolve
     * to the same canonical key. The same applies to "Bank Melli".
     */
    private fun normalize(value: String): String = buildString(value.length) {
        normalizeDigits(value.trim()).uppercase().forEachIndexed { index, char ->
            if (char == '+' && index == 0) {
                append(char)
            } else if (char.isLetterOrDigit()) {
                append(char)
            }
        }
    }

    private fun normalizeDigits(value: String): String = buildString(value.length) {
        value.forEach { char ->
            append(when (char) {
                in '۰'..'۹' -> ('0'.code + (char - '۰')).toChar()
                in '٠'..'٩' -> ('0'.code + (char - '٠')).toChar()
                else -> char
            })
        }
    }
}
