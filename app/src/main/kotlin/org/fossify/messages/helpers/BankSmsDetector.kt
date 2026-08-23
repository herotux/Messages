package org.fossify.messages.helpers

/**
 * Detects an Iranian bank from an incoming SMS without network access.
 *
 * The detector is intentionally conservative:
 * - verified sender IDs are HIGH confidence;
 * - a valid IBAN or valid 16-digit card number is HIGH confidence;
 * - an explicit, unambiguous bank name is MEDIUM confidence.
 *
 * Weak aliases and arbitrary numeric fragments are not enough to identify a bank.
 */
object BankSmsDetector {
    enum class Confidence {
        HIGH,
        MEDIUM,
    }

    enum class Reason {
        VERIFIED_SENDER,
        IBAN,
        CARD_NUMBER,
        EXPLICIT_BANK_NAME,
    }

    data class Detection(
        val bank: IranianBankRegistry.BankInfo,
        val confidence: Confidence,
        val reason: Reason,
    )

    private data class BankNameCandidate(
        val bank: IranianBankRegistry.BankInfo,
        val alias: String,
    )

    fun detect(sender: String, body: String): Detection? {
        IranianBankRegistry.findBySmsSender(sender)?.let {
            return Detection(it, Confidence.HIGH, Reason.VERIFIED_SENDER)
        }

        val normalizedBody = normalizeDigits(body)

        findIban(normalizedBody)?.let { iban ->
            if (IranianBankRegistry.isValidIban(iban)) {
                IranianBankRegistry.findByIban(iban)?.let {
                    return Detection(it, Confidence.HIGH, Reason.IBAN)
                }
            }
        }

        findCardNumbers(normalizedBody).forEach { cardNumber ->
            if (IranianBankRegistry.isValidCardNumber(cardNumber)) {
                IranianBankRegistry.findByCard(cardNumber)?.let {
                    return Detection(it, Confidence.HIGH, Reason.CARD_NUMBER)
                }
            }
        }

        findExplicitBankName(normalizedBody)?.let {
            return Detection(it.bank, Confidence.MEDIUM, Reason.EXPLICIT_BANK_NAME)
        }

        return null
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
                bank.aliases
                    .filter { alias -> alias.length >= 3 }
                    .map { alias -> BankNameCandidate(bank, alias) }
            }
            .sortedByDescending { candidate -> candidate.alias.length }

        val matches = candidates.filter { candidate ->
            body.contains(candidate.alias, ignoreCase = true)
        }.toList()
        if (matches.isEmpty()) return null

        val bestLength = matches.first().alias.length
        val best = matches.takeWhile { candidate -> candidate.alias.length == bestLength }
        val distinctBanks = best.map { candidate -> candidate.bank.id }.distinct()
        val bestCandidate = best.firstOrNull() ?: return null
        return bestCandidate.takeIf { distinctBanks.size == 1 }
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
