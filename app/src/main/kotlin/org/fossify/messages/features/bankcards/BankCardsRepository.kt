package org.fossify.messages.features.bankcards

import android.content.Context
import android.graphics.Color
import org.fossify.messages.extensions.getMessagesDB
import org.fossify.messages.helpers.IranianBankRegistry
import org.fossify.messages.models.BankAccount
import java.util.Locale

/** The only Messages-specific bridge used by the standalone bank-card UI. */
class BankCardsRepository(private val context: Context) {
    fun getCards(): List<BankCard> = runCatching {
        context.getMessagesDB().BankAccountsDao().getAll().mapNotNull { account -> runCatching { toUi(account) }.getOrNull() }
    }.getOrDefault(emptyList())

    fun save(existing: BankCard?, cardNumber: String, holderName: String, iban: String): Result<Unit> = runCatching {
        val normalizedCard = normalizeCard(cardNumber)
        val bank = IranianBankRegistry.findByCard(normalizedCard) ?: error("Unknown bank")
        val normalizedIban = normalizeIban(iban)
        if (!IranianBankRegistry.isValidCardNumber(normalizedCard)) error("Invalid card")
        if (normalizedIban.isNotBlank() && !IranianBankRegistry.isValidIban(normalizedIban)) error("Invalid IBAN")
        val now = System.currentTimeMillis()
        val entity = BankAccount(id = existing?.id ?: 0, bankId = bank.id.name, cardNumber = normalizedCard, holderName = holderName.trim(), iban = normalizedIban, createdAt = existing?.createdAt ?: now, updatedAt = now)
        val dao = context.getMessagesDB().BankAccountsDao()
        if (existing == null) dao.insert(entity) else dao.update(entity)
    }

    fun delete(card: BankCard): Result<Unit> = runCatching {
        context.getMessagesDB().BankAccountsDao().delete(BankAccount(id = card.id, bankId = card.bankId, cardNumber = card.cardNumber, holderName = card.holderName, iban = card.iban, createdAt = card.createdAt, updatedAt = card.updatedAt))
    }

    fun reorder(cards: List<BankCard>): Result<Unit> = runCatching {
        val dao = context.getMessagesDB().BankAccountsDao()
        cards.forEachIndexed { index, card -> dao.update(BankAccount(id = card.id, bankId = card.bankId, cardNumber = card.cardNumber, holderName = card.holderName, iban = card.iban, createdAt = card.createdAt, updatedAt = System.currentTimeMillis() + cards.size - index)) }
    }

    fun detect(cardNumber: String): BankVisual? = runCatching {
        val bank = IranianBankRegistry.findByCard(normalizeCard(cardNumber)) ?: return@runCatching null
        val bankId = bank.id.name
        val color = when (bankId) {
            "MELLAT" -> Color.rgb(165, 30, 45)
            "MELLI" -> Color.rgb(20, 75, 135)
            "TEJARAT" -> Color.rgb(0, 112, 175)
            "SADERAT" -> Color.rgb(0, 92, 155)
            "SEPAH" -> Color.rgb(205, 160, 35)
            "PASARGAD" -> Color.rgb(32, 65, 105)
            "PARSIAN" -> Color.rgb(20, 115, 110)
            "SAMAN" -> Color.rgb(20, 110, 145)
            "SHAHR" -> Color.rgb(80, 55, 125)
            else -> Color.rgb(70, 80, 95)
        }
        BankVisual(id = bankId, persianName = bank.persianName, englishName = bank.englishName, logoResourceName = bank.logoResourceName, color = color)
    }.getOrNull()

    fun normalizeCard(value: String): String = normalizeDigits(value).filter(Char::isDigit)
    // Keep the canonical value unformatted: callers that copy/share/display the account data must receive 16 contiguous digits.
    fun formatCard(value: String): String = normalizeCard(value)
    fun normalizeIban(value: String): String = normalizeDigits(value).replace(" ", "").replace("-", "").uppercase(Locale.US)
    fun formatIban(value: String): String = normalizeIban(value).chunked(4).joinToString(" ")
    fun validCard(value: String): Boolean = runCatching { IranianBankRegistry.isValidCardNumber(normalizeCard(value)) }.getOrDefault(false)
    fun validIban(value: String): Boolean = value.isBlank() || runCatching { IranianBankRegistry.isValidIban(normalizeIban(value)) }.getOrDefault(false)

    private fun normalizeDigits(value: String): String = buildString(value.length) { value.forEach { c -> append(when (c) { in '۰'..'۹' -> ('0'.code + c.code - '۰'.code).toChar(); in '٠'..'٩' -> ('0'.code + c.code - '٠'.code).toChar(); else -> c }) } }
    private fun toUi(account: BankAccount): BankCard { val normalizedCard = normalizeCard(account.cardNumber); return BankCard(id = account.id, bankId = account.bankId, cardNumber = normalizedCard, holderName = account.holderName.orEmpty(), iban = normalizeIban(account.iban.orEmpty()), createdAt = account.createdAt, updatedAt = account.updatedAt, visual = detect(normalizedCard)) }
}

data class BankCard(val id: Long, val bankId: String, val cardNumber: String, val holderName: String, val iban: String, val createdAt: Long, val updatedAt: Long, val visual: BankVisual?)
data class BankVisual(val id: String, val persianName: String, val englishName: String, val logoResourceName: String?, val color: Int)