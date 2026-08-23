package org.fossify.messages.helpers

/**
 * Conservative offline detector for Iranian bank transaction SMS messages.
 *
 * A bank name by itself is never enough. Text-based detection requires
 * transaction context and multiple independent signals. Verified sender IDs,
 * valid IBANs and valid card numbers remain high-confidence signals.
 */
object BankSmsDetector {
    enum class Confidence { HIGH, MEDIUM }

    enum class Reason {
        VERIFIED_SENDER,
        IBAN,
        CARD_NUMBER,
        TRANSACTION_CONTEXT,
        EXPLICIT_BANK_NAME,
    }

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
        Regex("(?i)(?:مبلغ|amount)\\s*[:：]"),
        Regex("(?i)(?:مانده|موجودی|balance)\\s*[:：]"),
        Regex("(?i)(?:حساب|شماره حساب|card|کارت)\\s*[:：]"),
        Regex("(?i)(?:زمان|تاریخ|تاریخ تراکنش)\\s*[:：]"),
        Regex("(?i)(?:شماره پیگیری|کد پیگیری|شماره مرجع|مرجع)\\s*[:：]"),
    )

    private val nonTransactionTerms = listOf(
        "پویش", "کمک", "حمایت", "خیریه", "جامعه هدف", "آسیب دیده", "جنگ تحمیلی",
        "مشارکت", "کمک های نقدی", "درگاه", "کد دستوری", "تبلیغ", "تخفیف", "قرعه کشی",
        "برنده", "فروش ویژه"
    )

    fun detect(sender: String, body: String): Detection? {
        IranianBankRegistry.findBySmsSender(sender)?.let {
            return Detection(it, Confidence.HIGH, Reason.VERIFIED_SENDER, 100, listOf(Reason.VERIFIED_SENDER))
        }

        val normalizedBody = normalizeDigits(body)

        findIban(normalizedBody)?.let { iban ->
            if (IranianBankRegistry.isValidIban(iban)) {
                IranianBankRegistry.findByIban(iban)?.let {
                    return Detection(it, Confidence.HIGH, Reason.IBAN, 100, listOf(Reason.IBAN))
                }
            }
        }

        findCardNumbers(normalizedBody).forEach { cardNumber ->
            if (IranianBankRegistry.isValidCardNumber(cardNumber)) {
                IranianBankRegistry.findByCard(cardNumber)?.let {
                    return Detection(it, Confidence.HIGH, Reason.CARD_NUMBER, 100, listOf(Reason.CARD_NUMBER))
                }
            }
        }

        return detectFromTransactionText(normalizedBody)
    }

    private fun detectFromTransactionText(body: String): Detection? {
        val termHits = transactionTerms.count { body.contains(it, ignoreCase = true) }
        val structureHits = transactionStructure.count { it.containsMatchIn(body) }
        val negativeHits = nonTransactionTerms.count { body.contains(it, ignoreCase = true) }

        // Bank names without transaction context are never enough.
        if (termHits < 2 && structureHits < 2) return null

        // Promotional/charity messages are rejected unless they contain a real
        // transaction structure such as مبلغ/مانده/حساب/تاریخ with a value.
        if (negativeHits >= 2 && structureHits == 0) return null

        val bankMatch = findExplicitBankName(body) ?: return null
        val score = termHits * 20 + structureHits * 25 + 30 - negativeHits * 25
        if (score < 70) return null

        return Detection(
            bank = bankMatch.bank,
            confidence = if (score >= 100) Confidence.HIGH else Confidence.MEDIUM,
            reason = Reason.TRANSACTION_CONTEXT,
            score = score,
            reasons = listOf(Reason.TRANSACTION_CONTEXT, Reason.EXPLICIT_BANK_NAME),
        )
    }

    private fun findIban(body: String): String? =
        Regex("IR\\d{24}", RegexOption.IGNORE_CASE)
            .find(body.replace(" ", "").replace("-", ""))?.value

    private fun findCardNumbers(body: String): Sequence<String> =
        Regex("\\d{4}(?:[ -]?\\d{4}){3}").findAll(body).map { match ->
            match.value.filter(Char::isDigit)
        }

    private fun findExplicitBankName(body: String): BankNameCandidate? {
        val candidates = IranianBankRegistry.allBanks()
            .flatMap { bank ->
                bank.aliases.filter { it.length >= 3 }.map { BankNameCandidate(bank, it) }
            }
            .sortedByDescending { it.alias.length }

        val matches = candidates.filter { hasExplicitInstitutionName(body, it.alias) }.toList()
        if (matches.isEmpty()) return null

        val bestLength = matches.maxOf { it.alias.length }
        val best = matches.filter { it.alias.length == bestLength }
        val distinctBanks = best.map { it.bank.id }.distinct()
        return best.firstOrNull()?.takeIf { distinctBanks.size == 1 }
    }

    private fun hasExplicitInstitutionName(body: String, alias: String): Boolean {
        val escapedAlias = Regex.escape(alias.trim())
        val pattern = Regex(
            "(?:بانک|بانکِ|موسسه اعتباری|موسسه|مؤسسه اعتباری|مؤسسه)\\s+$escapedAlias(?=$|[^آ-ی])"
        )
        return pattern.containsMatchIn(body)
    }

    private fun normalizeDigits(value: String): String = buildString(value.length) {
        value.forEach { char ->
            append(
                when (char) {
                    in '۰'..'۹' -> ('0'.code + (char - '۰')).toChar()
                    in '٠'..'٩' -> ('0'.code + (char - '٠')).toChar()
                    else -> char
                }
            )
        }
    }
}
