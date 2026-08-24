package org.fossify.messages.helpers

/**
 * Exact alphanumeric sender IDs observed in real Iranian bank SMS samples.
 * Sender IDs are checked before body heuristics so a bank SMS can receive its
 * logo even when the body does not contain a Persian bank name.
 */
object BankSenderAliases {
    private val aliases = mapOf(
        "SEPAHBANK" to IranianBankRegistry.BankId.SEPAH,
        "BANKSEPAH" to IranianBankRegistry.BankId.SEPAH,
        "TEJARATBANK" to IranianBankRegistry.BankId.TEJARAT,
        "BANKTEJARAT" to IranianBankRegistry.BankId.TEJARAT,
        "BANKMELLI" to IranianBankRegistry.BankId.MELLI,
        "BANKMELLIIRAN" to IranianBankRegistry.BankId.MELLI,
        "MELLIBANK" to IranianBankRegistry.BankId.MELLI,
        "MASKANBANK" to IranianBankRegistry.BankId.MASKAN,
        "BANKMASKAN" to IranianBankRegistry.BankId.MASKAN,
    )

    fun find(sender: String): IranianBankRegistry.BankInfo? {
        val normalized = sender.trim()
            .uppercase()
            .replace(" ", "")
            .replace("-", "")
        return aliases[normalized]?.let(IranianBankRegistry::findById)
    }
}
