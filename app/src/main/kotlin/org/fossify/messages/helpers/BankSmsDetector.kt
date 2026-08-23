package org.fossify.messages.helpers

/**
 * Conservative offline detector for Iranian bank transaction SMS messages.
 * A card/IBAN/name alone is not enough when the message looks promotional.
 */
object BankSmsDetector {
    enum class Confidence { HIGH, MEDIUM }
    enum class Reason { VERIFIED_SENDER, IBAN, CARD_NUMBER, TRANSACTION_CONTEXT, EXPLICIT_BANK_NAME }

    data class Detection(
        val bank: IranianBankRegistry.BankInfo,
        val confidence: Confidence,
        val reason: Reason,
        val score: Int,
        val reasons: List<Reason>,
    )

    private data class BankNameCandidate(val bank: IranianBankRegistry.BankInfo, val alias: String)

    private val transactionTerms = listOf(
        "واریز", "واریزی", "برداشت", "برداشتی", "انتقال", "انتقالی", "پرداخت", "پرداختی",
        "مبلغ", "مانده", "موجودی", "تراکنش", "شماره پیگیری", "پیگیری", "کد پیگیری",
        "شماره مرجع", "مرجع", "رسید", "خرید", "شارژ", "اعتبار", "بستانکار", "بدهکار"
    )
    private val transactionStructure = listOf(
        Regex("(?i)(?:مبلغ|amount)\\s*[:：]"), Regex("(?i)(?:مانده|موجودی|balance)\\s*[:：]"),
        Regex("(?i)(?:حساب|شماره حساب|card|کارت)\\s*[:：]"), Regex("(?i)(?:زمان|تاریخ|تاریخ تراکنش)\\s*[:：]"),
        Regex("(?i)(?:شماره پیگیری|کد پیگیری|شماره مرجع|مرجع)\\s*[:：]")
    )
    private val nonTransactionTerms = listOf(
        "پویش", "کمک", "حمایت", "خیریه", "جامعه هدف", "آسیب دیده", "جنگ تحمیلی",
        "مشارکت", "کمک های نقدی", "درگاه", "کد دستوری", "تبلیغ", "تخفیف", "قرعه کشی",
        "برنده", "فروش ویژه", "اهدای", "نذری", "اطعام", "مهمانی", "نیکوکاری",
        "فروشگاه", "خدمتتون ارسال", "اطلاع دهید"
    )

    fun detect(sender: String, body: String): Detection? {
        // A verified bank sender is sufficient because the sender itself identifies the bank.
        IranianBankRegistry.findBySmsSender(sender)?.let {
            return Detection(it, Confidence.HIGH, Reason.VERIFIED_SENDER, 100, listOf(Reason.VERIFIED_SENDER))
        }

        val normalizedBody = normalizeDigits(body)
        val negativeHits = nonTransactionTerms.count { normalizedBody.contains(it, ignoreCase = true) }
        val termHits = transactionTerms.count { normalizedBody.contains(it, ignoreCase = true) }
        val structureHits = transactionStructure.count { it.containsMatchIn(normalizedBody) }

        // Card numbers and IBANs are common in merchant/payment-request messages.
        // They are supporting evidence only and MUST NOT identify a bank by themselves.
        // Without a verified sender or explicit bank name, an unknown conversation is not a bank.
        val bankMatch = findExplicitBankName(normalizedBody)
        if (bankMatch == null) return null

        if (termHits < 2 && structureHits < 2) return null
        if (negativeHits > 0) return null

        // If a card/IBAN is present, it can support the explicit bank identity, but the
        // bank name + transaction context remains the required identity signal.
        val score = termHits * 20 + structureHits * 25 + 30
        if (score < 70) return null

        return Detection(
            bankMatch.bank,
            if (score >= 100) Confidence.HIGH else Confidence.MEDIUM,
            Reason.TRANSACTION_CONTEXT,
            score,
            listOf(Reason.TRANSACTION_CONTEXT, Reason.EXPLICIT_BANK_NAME)
        )
    }

    private fun findExplicitBankName(body: String): BankNameCandidate? {
        val candidates = IranianBankRegistry.allBanks().flatMap { bank ->
            bank.aliases.filter { it.length >= 3 }.map { BankNameCandidate(bank, it) }
        }.sortedByDescending { it.alias.length }
        val matches = candidates.filter { hasExplicitInstitutionName(body, it.alias) }.toList()
        if (matches.isEmpty()) return null
        val bestLength = matches.maxOf { it.alias.length }
        val best = matches.filter { it.alias.length == bestLength }
        val distinctBanks = best.map { it.bank.id }.distinct()
        return best.firstOrNull()?.takeIf { distinctBanks.size == 1 }
    }

    private fun hasExplicitInstitutionName(body: String, alias: String): Boolean {
        val escapedAlias = Regex.escape(alias.trim())
        return Regex("(?:بانک|بانکِ|موسسه اعتباری|موسسه|مؤسسه اعتباری|مؤسسه)\\s+$escapedAlias(?=$|[^آ-ی])")
            .containsMatchIn(body)
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
