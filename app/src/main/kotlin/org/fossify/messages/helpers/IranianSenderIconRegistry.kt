package org.fossify.messages.helpers

/**
 * Registry for trusted Iranian SMS senders that can have a recognizable icon.
 * This is intentionally separate from [IranianBankRegistry]: an icon does not
 * imply that the message is a bank transaction.
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
        SenderInfo("SHAHR_BANK", "بانک شهر", Category.BANK, listOf("BANKSHAHR", "SHAHRBANK", "BANK SHAHR", "Bank Shahr", "بانک شهر"), "bank_shahr"),
        SenderInfo("SEPAH_BANK", "بانک سپه", Category.BANK, listOf("BANKSEPAH", "SEPAHBANK", "BANK SEPAH", "Bank Sepah", "بانک سپه", "SEPAH BANK", "SEPAH  BANK"), "bank_sepah"),
        SenderInfo("TEJARAT_BANK", "بانک تجارت", Category.BANK, listOf("BANKTEJARAT", "TEJARATBANK", "BANK TEJARAT", "TejaratBank", "بانک تجارت"), "bank_tejarat"),
        SenderInfo("MELLI_BANK", "بانک ملی ایران", Category.BANK, listOf("BANKMELLI", "BANKMELLIIRAN", "BANK MELLI", "Bank Melli", "بانک ملی", "بانک ملی ایران", "+98700717"), "bank_melli"),
        SenderInfo("MCI", "همراه اول", Category.TELECOM, listOf("MCI", "IR-MCI", "HAMRAHE AVAL", "HAMRAHEAVAL", "همراه اول", "همراه‌اول"), "sender_mci"),
        SenderInfo("IRANCELL", "ایرانسل", Category.TELECOM, listOf("IRANCELL", ".IRANCELL.", "IrancelleTo", "IRANCELLTO", "ایرانسل"), "sender_irancell"),
        SenderInfo("MOKHABERAT", "مخابرات ایران", Category.TELECOM, listOf("MOKHABERAT", "TCI", "TCT", "MOKHABERATIRAN", "مخابرات", "شرکت مخابرات ایران"), "sender_mokhaberat"),
        SenderInfo("GAS_KURDISTAN", "گاز کردستان", Category.UTILITY, listOf("+984040102020", "گاز کردستان", "گاز", "شرکت گاز", "شرکت ملی گاز ایران"), "sender_gas"),
        SenderInfo("ELECTRICITY_KURDISTAN", "برق کردستان", Category.UTILITY, listOf("+98404014013900", "برق کردستان", "برق", "توانیر", "شرکت توانیر"), "sender_electricity"),
        SenderInfo("WATER_IRAN", "آب و فاضلاب", Category.UTILITY, listOf("آبفا", "آب و فاضلاب", "شرکت آب و فاضلاب", "شرکت آبفا", "آبفای کردستان"), "sender_water"),
        SenderInfo("SAKHD", "ساخد", Category.GOVERNMENT, listOf("+9860009621", "ساخد", "سازمان خدمات درمانی نیروهای مسلح", "سازمان تامین خدمات درمانی نیروهای مسلح"), "sender_sakhd"),
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
