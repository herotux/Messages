package org.fossify.messages.helpers

import android.util.Log

/** Exact sender profiles observed in real Iranian bank SMS samples. */
object IranianBankSenderProfiles {
    private const val TAG = "IranianBankSender"

    private val normalizedSenders = mapOf(
        "+989820004747" to IranianBankRegistry.BankId.RESALAT,
        "98500014747" to IranianBankRegistry.BankId.RESALAT,
        "RESALATBANK" to IranianBankRegistry.BankId.RESALAT,
        "+989820003501" to IranianBankRegistry.BankId.SHAHR,
        "+9820003501" to IranianBankRegistry.BankId.SHAHR,
        "+9820003502" to IranianBankRegistry.BankId.SHAHR,
        "MASKANBANK" to IranianBankRegistry.BankId.MASKAN,
        "BANKMASKAN" to IranianBankRegistry.BankId.MASKAN,
        "+9830009417" to IranianBankRegistry.BankId.MELLI,
        "+98100041415001" to IranianBankRegistry.BankId.MELLI,
        "+98700717" to IranianBankRegistry.BankId.MELLI,
        "BMIR" to IranianBankRegistry.BankId.MELLI,
        "BMI.IR" to IranianBankRegistry.BankId.MELLI,
        "BANKMELLI" to IranianBankRegistry.BankId.MELLI,
        "BANKMELLIIRAN" to IranianBankRegistry.BankId.MELLI,
        "BANK MELLI" to IranianBankRegistry.BankId.MELLI,
        "بانک ملی" to IranianBankRegistry.BankId.MELLI,
        "BANKSEPAH" to IranianBankRegistry.BankId.SEPAH,
        "SEPAHBANK" to IranianBankRegistry.BankId.SEPAH,
        "SEPAH BANK" to IranianBankRegistry.BankId.SEPAH,
        "30001557" to IranianBankRegistry.BankId.SEPAH,
        "671557" to IranianBankRegistry.BankId.SEPAH,
        "1557" to IranianBankRegistry.BankId.SEPAH,
        "بانکسپه" to IranianBankRegistry.BankId.SEPAH,
        "بانک سپه" to IranianBankRegistry.BankId.SEPAH,
        "TEJARATBANK" to IranianBankRegistry.BankId.TEJARAT,
        "BANKTEJARAT" to IranianBankRegistry.BankId.TEJARAT,
        "TEJARAT BANK" to IranianBankRegistry.BankId.TEJARAT,
        "بانک تجارت" to IranianBankRegistry.BankId.TEJARAT,
        "MELLATBANK" to IranianBankRegistry.BankId.MELLAT,
        "BANKMELLAT" to IranianBankRegistry.BankId.MELLAT,
        "MELLAT BANK" to IranianBankRegistry.BankId.MELLAT,
        "بانک ملت" to IranianBankRegistry.BankId.MELLAT,
        "TTBANK" to IranianBankRegistry.BankId.TOSEE_TAAVON,
        "TOSEE TAAVON" to IranianBankRegistry.BankId.TOSEE_TAAVON,
        "TOSEE TAVON" to IranianBankRegistry.BankId.TOSEE_TAAVON,
        "BANKTOSEE TAAVON" to IranianBankRegistry.BankId.TOSEE_TAAVON,
        "BANKTOSEETAAVON" to IranianBankRegistry.BankId.TOSEE_TAAVON,
        "بانک توسعه تعاون" to IranianBankRegistry.BankId.TOSEE_TAAVON,
        "توسعه تعاون" to IranianBankRegistry.BankId.TOSEE_TAAVON,
    )

    private val tracedBanks = setOf(
        IranianBankRegistry.BankId.SEPAH,
        IranianBankRegistry.BankId.MELLI,
        IranianBankRegistry.BankId.TEJARAT,
        IranianBankRegistry.BankId.MELLAT,
        IranianBankRegistry.BankId.TOSEE_TAAVON,
    )

    fun find(sender: String): IranianBankRegistry.BankInfo? {
        val normalized = normalize(sender)
        val id = normalizedSenders[normalized]
        if (id in tracedBanks) {
            safeLog("BANK_SENDER_MATCH raw=${sender.take(80)} normalized=$normalized bankId=$id")
        } else if (id == null && (sender.contains("MELLAT", true) || sender.contains("TEJARAT", true) || sender.contains("SEPAH", true) || sender.contains("MELLI", true) || sender.contains("TOSEE", true) || sender.contains("توسعه") || sender.contains("ملت") || sender.contains("تجارت") || sender.contains("سپه") || sender.contains("ملی"))) {
            safeLog("BANK_SENDER_MISS raw=${sender.take(80)} normalized=$normalized")
        }
        return id?.let(IranianBankRegistry::findById)
    }

    private fun safeLog(message: String) {
        try {
            Log.d(TAG, message)
        } catch (_: Throwable) {
            // android.util.Log is not implemented by plain JVM unit-test stubs.
        }
    }

    private fun normalize(value: String): String = buildString(value.length) {
        normalizeDigits(value.trim()).uppercase().forEachIndexed { index, char ->
            if (char == '+' && index == 0) append(char)
            else if (char.isLetterOrDigit()) append(char)
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
