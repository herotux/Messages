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
        "+9830009417" to IranianBankRegistry.BankId.MELLI,
        "+98100041415001" to IranianBankRegistry.BankId.MELLI,
        "BMIR" to IranianBankRegistry.BankId.MELLI,
        "BMI.IR" to IranianBankRegistry.BankId.MELLI,
        "BANKMELLI" to IranianBankRegistry.BankId.MELLI,
        "BANKMELLIIRAN" to IranianBankRegistry.BankId.MELLI,
        "+989820003501" to IranianBankRegistry.BankId.SHAHR,
    )

    fun find(sender: String): IranianBankRegistry.BankInfo? {
        val normalized = normalize(sender)
        val id = normalizedSenders[normalized] ?: return null
        return IranianBankRegistry.findById(id)
    }

    private fun normalize(value: String): String = value
        .trim()
        .replace(" ", "")
        .replace("-", "")
        .uppercase()
        .let { input ->
            buildString(input.length) {
                input.forEach { char ->
                    append(when (char) {
                        in '۰'..'۹' -> ('0'.code + (char - '۰')).toChar()
                        in '٠'..'٩' -> ('0'.code + (char - '٠')).toChar()
                        else -> char
                    })
                }
            }
        }
}
