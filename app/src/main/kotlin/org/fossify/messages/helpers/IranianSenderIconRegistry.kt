package org.fossify.messages.helpers

/**
 * Registry for trusted Iranian SMS senders that are not necessarily banks.
 *
 * This is intentionally separate from [IranianBankRegistry]: a sender can have
 * a recognizable icon without being classified as a bank or having its card
 * numbers treated as banking evidence.
 */
object IranianSenderIconRegistry {
    enum class Category { BANK, TELECOM, UTILITY, GOVERNMENT, BUSINESS }

    data class SenderInfo(
        val id: String,
        val displayName: String,
        val category: Category,
        val senderAliases: List<String>,
        val logoResourceName: String? = null,
    )

    private val senders = listOf(
        SenderInfo(
            id = "SHAHR_BANK",
            displayName = "بانک شهر",
            category = Category.BANK,
            senderAliases = listOf("BANKSHAHR", "SHAHRBANK", "BANK SHAHR", "Bank Shahr", "بانک شهر"),
            logoResourceName = "bank_shahr",
        ),
        SenderInfo(
            id = "IRANCELL",
            displayName = "ایرانسل",
            category = Category.TELECOM,
            senderAliases = listOf("IRANCELL", ".IRANCELL.", "IrancelleTo"),
            logoResourceName = "sender_irancell",
        ),
        SenderInfo(
            id = "MOKHABERAT",
            displayName = "مخابرات ایران",
            category = Category.TELECOM,
            senderAliases = listOf("MOKHABERAT", "مخابرات"),
            logoResourceName = "sender_mokhaberat",
        ),
        SenderInfo(
            id = "GAS_KURDISTAN",
            displayName = "گاز کردستان",
            category = Category.UTILITY,
            senderAliases = listOf("+984040102020", "گاز کردستان"),
            logoResourceName = "sender_gas",
        ),
        SenderInfo(
            id = "ELECTRICITY_KURDISTAN",
            displayName = "برق کردستان",
            category = Category.UTILITY,
            senderAliases = listOf("+98404014013900", "برق کردستان"),
            logoResourceName = "sender_electricity",
        ),
        SenderInfo(
            id = "SAKHD",
            displayName = "ساخد",
            category = Category.GOVERNMENT,
            senderAliases = listOf("+9860009621", "ساخد"),
            logoResourceName = "sender_sakhd",
        ),
    )

    private val aliases = senders.flatMap { sender ->
        sender.senderAliases.map { normalize(it) to sender }
    }.toMap()

    fun find(sender: String?): SenderInfo? {
        if (sender.isNullOrBlank()) return null
        return aliases[normalize(sender)]
    }

    fun all(): List<SenderInfo> = senders

    private fun normalize(value: String): String = value
        .trim()
        .replace("\u200C", "")
        .replace(" ", "")
        .uppercase()
}
