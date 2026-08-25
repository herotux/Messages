package org.fossify.messages.helpers

object IranianBankRegistry {
    enum class BankId { MELLI, MELLAT, TEJARAT, SEPAH, MASKAN, MEHR_IRAN, TOSEE_TAAVON }
    data class BankInfo(val id: BankId, val persianName: String, val englishName: String, val cardPrefixes: List<String> = emptyList(), val aliases: List<String> = emptyList(), val logoResourceName: String? = null)
    private val banks = listOf(
        BankInfo(BankId.MELLI, "بانک ملی ایران", "Melli Bank", listOf("603799", "636214"), listOf("ملی", "بانک ملی", "بانک ملی ایران"), "bank_melli"),
        BankInfo(BankId.MELLAT, "بانک ملت", "Mellat Bank", listOf("610433", "991975"), listOf("ملت", "بانک ملت"), "bank_mellat"),
        BankInfo(BankId.TEJARAT, "بانک تجارت", "Tejarat Bank", listOf("627353", "585983"), listOf("تجارت", "بانک تجارت"), "bank_tejarat"),
        BankInfo(BankId.SEPAH, "بانک سپه", "Sepah Bank", listOf("589210", "627381", "639599", "636949", "639370"), listOf("سپه", "بانک سپه"), "bank_sepah"),
        BankInfo(BankId.MASKAN, "بانک مسکن", "Maskan Bank", listOf("628023"), listOf("مسکن", "بانک مسکن"), "bank_maskan"),
        BankInfo(BankId.MEHR_IRAN, "بانک قرض الحسنه مهر ایران", "Mehr Iran Bank", listOf("606373"), listOf("مهر", "مهر ایران", "بانک مهر ایران", "بانک قرض الحسنه مهر ایران"), "bank_mehr_iran"),
        BankInfo(BankId.TOSEE_TAAVON, "بانک توسعه تعاون", "Tosee Taavon Bank", listOf("502908"), listOf("توسعه تعاون", "بانک توسعه تعاون"), "bank_tosee_taavon")
    )
    private val byId = banks.associateBy { it.id }
    fun allBanks() = banks
    fun findById(id: BankId) = byId[id]
    fun findBySmsSender(sender: String): BankInfo? = when (sender.trim().uppercase().replace(" ", "").replace("-", "")) {
        "SEPAHBANK", "BANKSEPAH" -> findById(BankId.SEPAH)
        "TEJARATBANK", "BANKTEJARAT", "TEJRATBANK" -> findById(BankId.TEJARAT)
        "BANKMELLI", "BANKMELLIIRAN", "MELLIBANK" -> findById(BankId.MELLI)
        "MASKANBANK", "BANKMASKAN" -> findById(BankId.MASKAN)
        else -> null
    }
    fun findByCard(card: String): BankInfo? = banks.asSequence().flatMap { b -> b.cardPrefixes.map { it to b }.asSequence() }.filter { card.filter(Char::isDigit).startsWith(it.first) }.maxByOrNull { it.first.length }?.second
    fun isValidCardNumber(card: String): Boolean {
        val d = card.filter(Char::isDigit); if (d.length != 16) return false
        var sum = 0; d.forEachIndexed { i, c -> var n = c - '0'; if (i % 2 == 0) { n *= 2; if (n > 9) n -= 9 }; sum += n }; return sum % 10 == 0
    }
}
