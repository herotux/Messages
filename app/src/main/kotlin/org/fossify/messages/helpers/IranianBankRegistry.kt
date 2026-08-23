package org.fossify.messages.helpers

/**
 * Offline registry for Iranian bank identification.
 *
 * Data sources cross-checked against:
 * - masihgh/iranian-bank-list (banks.json)
 * - IR-Banks/ir-banks-info (banks.ts, sheba.ts)
 *
 * This class deliberately contains no network access and no third-party dependency.
 */
object IranianBankRegistry {
    enum class BankId {
        MELLI,
        MELLAT,
        TEJARAT,
        SADERAT,
        SEPAH,
        REFAH,
        MASKAN,
        KESHAVARZI,
        SANAT_MADAN,
        POST,
        TOSEE_SADERAT,
        TOSEE_TAAVON,
        TOSEE,
        PARSIAN,
        PASARGAD,
        KARAFARIN,
        SAMAN,
        EGHTESAD_NOVIN,
        SARMAYEH,
        SINA,
        MEHR_IRAN,
        SHAHR,
        GARDESHGARI,
        DEY,
        IRAN_ZAMIN,
        RESALAT,
        MELAL,
        KHAVAR_MIANEH,
        IRAN_VENEZUELA,
        CENTRAL_BANK,
        AYANDEH_FORMER,
        UNKNOWN,
    }

    data class BankInfo(
        val id: BankId,
        val persianName: String,
        val englishName: String,
        val ibanCode: String?,
        val cardPrefixes: List<String>,
        val aliases: List<String> = emptyList(),
        /** Android drawable name. Kept as a name so the registry is independent of R. */
        val logoResourceName: String? = null,
    )

    private val banks = listOf(
        BankInfo(BankId.MELLI, "بانک ملی ایران", "Melli Bank", "017", listOf("603799"), listOf("ملی", "بانک ملی"), "bank_melli"),
        BankInfo(BankId.MELLAT, "بانک ملت", "Mellat Bank", "012", listOf("610433", "991975"), listOf("ملت", "بانک ملت"), "bank_mellat"),
        BankInfo(BankId.TEJARAT, "بانک تجارت", "Tejarat Bank", "018", listOf("627353", "585983"), listOf("تجارت", "بانک تجارت"), "bank_tejarat"),
        BankInfo(BankId.SADERAT, "بانک صادرات ایران", "Saderat Bank", "019", listOf("603769"), listOf("صادرات", "بانک صادرات"), "bank_saderat"),
        BankInfo(BankId.SEPAH, "بانک سپه", "Sepah Bank", "015", listOf("589210", "627381", "639599", "636949", "639370", "502229"), listOf("سپه", "انصار", "قوامین", "حکمت ایرانیان", "مهر اقتصاد"), "bank_sepah"),
        BankInfo(BankId.REFAH, "بانک رفاه کارگران", "Refah Bank", "013", listOf("589463"), listOf("رفاه", "رفاه کارگران"), "bank_refah"),
        BankInfo(BankId.MASKAN, "بانک مسکن", "Maskan Bank", "014", listOf("628023"), listOf("مسکن", "بانک مسکن"), "bank_maskan"),
        BankInfo(BankId.KESHAVARZI, "بانک کشاورزی", "Keshavarzi Bank", "016", listOf("603770", "639217"), listOf("کشاورزی"), "bank_keshavarzi"),
        BankInfo(BankId.SANAT_MADAN, "بانک صنعت و معدن", "Sanat-o-Madan Bank", "011", listOf("627961"), listOf("صنعت و معدن"), "bank_sanat_madan"),
        BankInfo(BankId.POST, "پست بانک ایران", "Post Bank", "021", listOf("627760"), listOf("پست بانک"), "bank_post"),
        BankInfo(BankId.TOSEE_SADERAT, "بانک توسعه صادرات ایران", "Export Development Bank", "020", listOf("627648", "207177"), listOf("توسعه صادرات"), "bank_tosee_saderat"),
        BankInfo(BankId.TOSEE_TAAVON, "بانک توسعه تعاون", "Tosee Taavon Bank", "022", listOf("502908"), listOf("توسعه تعاون"), "bank_tosee_taavon"),
        BankInfo(BankId.TOSEE, "موسسه اعتباری توسعه", "Tosee Credit Institution", "051", listOf("628157"), listOf("توسعه"), "bank_tosee"),
        BankInfo(BankId.PARSIAN, "بانک پارسیان", "Parsian Bank", "054", listOf("622106", "639194", "627884"), listOf("پارسیان"), "bank_parsian"),
        BankInfo(BankId.PASARGAD, "بانک پاسارگاد", "Pasargad Bank", "057", listOf("502229", "639347"), listOf("پاسارگاد"), "bank_pasargad"),
        BankInfo(BankId.KARAFARIN, "بانک کارآفرین", "Karafarin Bank", "053", listOf("627488", "502910"), listOf("کارآفرین"), "bank_karafarin"),
        BankInfo(BankId.SAMAN, "بانک سامان", "Saman Bank", "056", listOf("621986"), listOf("سامان"), "bank_saman"),
        BankInfo(BankId.EGHTESAD_NOVIN, "بانک اقتصاد نوین", "Eghtesad Novin Bank", "055", listOf("627412"), listOf("اقتصاد نوین"), "bank_eghtesad_novin"),
        BankInfo(BankId.SARMAYEH, "بانک سرمایه", "Sarmayeh Bank", "058", listOf("639607"), listOf("سرمایه"), "bank_sarmayeh"),
        BankInfo(BankId.SINA, "بانک سینا", "Sina Bank", "059", listOf("639346"), listOf("سینا"), "bank_sina"),
        BankInfo(BankId.MEHR_IRAN, "بانک قرض الحسنه مهر ایران", "Mehr Iran Bank", "060", listOf("606373"), listOf("مهر ایران", "مهر"), "bank_mehr_iran"),
        BankInfo(BankId.SHAHR, "بانک شهر", "Shahr Bank", "061", listOf("502806", "504706"), listOf("شهر"), "bank_shahr"),
        // Ayandeh was resolved into Melli in 1404; the former card prefix is retained as history.
        BankInfo(BankId.AYANDEH_FORMER, "بانک آینده (سابق)", "Ayandeh Bank (former)", "062", listOf("636214"), listOf("آینده", "بانک آینده"), "bank_melli"),
        BankInfo(BankId.GARDESHGARI, "بانک گردشگری", "Gardeshgari Bank", "064", listOf("505416", "505426"), listOf("گردشگری"), "bank_gardeshgari"),
        BankInfo(BankId.DEY, "بانک دی", "Dey Bank", "066", listOf("502938"), listOf("دی"), "bank_dey"),
        BankInfo(BankId.IRAN_ZAMIN, "بانک ایران زمین", "Iran Zamin Bank", "069", listOf("505785"), listOf("ایران زمین"), "bank_iran_zamin"),
        BankInfo(BankId.RESALAT, "بانک قرض الحسنه رسالت", "Resalat Bank", "070", listOf("504172"), listOf("رسالت"), "bank_resalat"),
        BankInfo(BankId.MELAL, "موسسه اعتباری ملل", "Melal Credit Institution", "075", listOf("606256"), listOf("ملل"), "bank_melal"),
        BankInfo(BankId.KHAVAR_MIANEH, "بانک خاورمیانه", "Middle East Bank", "078", listOf("585947"), listOf("خاورمیانه"), "bank_khavar_mianeh"),
        BankInfo(BankId.IRAN_VENEZUELA, "بانک ایران و ونزوئلا", "Iran-Venezuela Bank", "095", emptyList(), listOf("ایران و ونزوئلا"), "bank_iran_venezuela"),
        BankInfo(BankId.CENTRAL_BANK, "بانک مرکزی جمهوری اسلامی ایران", "Central Bank of Iran", "010", listOf("636795"), listOf("بانک مرکزی"), "bank_central"),
    )

    private val byId = banks.associateBy { it.id }

    // Special cases that require more than the normal six-digit BIN.
    // The generic 621986 prefix belongs to Saman; 62198618/19 belong to Blu.
    private val specialCardPrefixes = listOf(
        "62198619" to BankInfo(BankId.SAMAN, "بلوبانک", "BluBank", "056", listOf("62198619"), listOf("بلوبانک", "بلو"), "bank_saman"),
        "62198618" to BankInfo(BankId.SAMAN, "بلوبانک", "BluBank", "056", listOf("62198618"), listOf("بلوبانک", "بلو"), "bank_saman"),
    )

    /** Sender names that are explicitly known to be non-bank institutional senders. */
    private val knownNonBankSenders = setOf(
        "V.REFAH",
        "V.MASKAN",
    )

    fun allBanks(): List<BankInfo> = banks

    fun findById(id: BankId): BankInfo? = byId[id]

    /** Finds the most specific matching card prefix. */
    fun findByCard(cardNumber: String): BankInfo? {
        val normalized = normalizeDigits(cardNumber).filter(Char::isDigit)
        if (normalized.length < 6) return null

        specialCardPrefixes.firstOrNull { normalized.startsWith(it.first) }?.let { return it.second }

        return banks
            .asSequence()
            .filter { bank -> bank.cardPrefixes.any { normalized.startsWith(it) } }
            .sortedByDescending { bank -> bank.cardPrefixes.maxOfOrNull { it.length } ?: 0 }
            .firstOrNull()
    }

    fun isValidCardNumber(cardNumber: String): Boolean {
        val digits = normalizeDigits(cardNumber).filter(Char::isDigit)
        if (digits.length != 16 || digits.all { it == '0' }) return false

        var sum = 0
        for (i in digits.indices) {
            var value = digits[i] - '0'
            if (i % 2 == 0) {
                value *= 2
                if (value > 9) value -= 9
            }
            sum += value
        }
        return sum % 10 == 0
    }

    fun findByIban(iban: String): BankInfo? {
        val normalized = normalizeDigits(iban)
            .uppercase()
            .replace(" ", "")
            .replace("-", "")
        if (!normalized.startsWith("IR") || normalized.length != 26) return null

        val code = normalized.substring(4, 7)
        return banks.firstOrNull { it.ibanCode == code }
    }

    fun isValidIban(iban: String): Boolean {
        val normalized = normalizeDigits(iban)
            .uppercase()
            .replace(" ", "")
            .replace("-", "")
        if (!normalized.matches(Regex("IR\\d{24}"))) return false

        val rearranged = normalized.substring(4) + normalized.substring(0, 4)
        var remainder = 0
        for (char in rearranged) {
            val value = if (char.isDigit()) char - '0' else char - 'A' + 10
            remainder = (remainder * 10 + value) % 97
        }
        return remainder == 1
    }

    fun findBySmsSender(sender: String): BankInfo? {
        val normalized = normalizeSender(sender)
        if (normalized.isEmpty() || normalized in knownNonBankSenders) return null

        // Sender mapping is deliberately conservative. Add only verified sender IDs here;
        // card/IBAN data must never be used as evidence that an arbitrary SMS sender is a bank.
        return null
    }

    fun isKnownNonBankSender(sender: String): Boolean = normalizeSender(sender) in knownNonBankSenders

    private fun normalizeSender(value: String): String = value.trim().uppercase().replace(" ", "")

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
