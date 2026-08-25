package org.fossify.messages.helpers

object BankSmsDetector {
    data class Detection(val bank: IranianBankRegistry.BankInfo)

    fun detect(sender: String, body: String): Detection? {
        IranianBankRegistry.findBySmsSender(sender)?.let { return Detection(it) }
        val normalized = body.replace('ك', 'ک').replace('ي', 'ی')
        val bank = IranianBankRegistry.allBanks().sortedByDescending { it.aliases.maxOfOrNull(String::length) ?: 0 }
            .firstOrNull { b -> b.aliases.any { alias -> Regex("(?:بانک|موسسه|مؤسسه)\\s+${Regex.escape(alias)}(?=$|[^آ-ی])").containsMatchIn(normalized) } }
            ?: return null
        val transactionWords = listOf("واریز", "برداشت", "انتقال", "پرداخت", "مبلغ", "مانده", "موجودی", "تراکنش", "شماره پیگیری", "رسید", "رمز", "کد ورود")
        if (transactionWords.count { normalized.contains(it) } < 2 && !normalized.contains("مشتری گرامی")) return null
        return Detection(bank)
    }
}
