package org.fossify.messages.helpers

/**
 * Offline registry for Iranian bank identification.
 *
 * Cross-checked against:
 * - masihgh/iranian-bank-list (banks.json)
 * - IR-Banks/ir-banks-info (banks.ts, sheba.ts)
 *
 * No network access and no third-party dependency are used at runtime.
 */
object IranianBankRegistry {
    enum class BankId {
        MELLI, MELLAT, TEJARAT, SADERAT, SEPAH, REFAH, MASKAN, KESHAVARZI,
        SANAT_MADAN, POST, TOSEE_SADERAT, TOSEE_TAAVON, TOSEE, PARSIAN,
        PASARGAD, KARAFARIN, SAMAN, BLUBANK, EGHTESAD_NOVIN, SARMAYEH, SINA,
        MEHR_IRAN, SHAHR, GARDESHGARI, DEY, IRAN_ZAMIN, RESALAT, MELAL,
        KHAVAR_MIANEH, IRAN_VENEZUELA, CENTRAL_BANK,
    }

    data class BankInfo(
        val id: BankId,
        val persianName: String,
        val englishName: String,
        val ibanCode: String?,
        val cardPrefixes: List<String>,
        val legacyIbanCodes: List<String> = emptyList(),
        val aliases: List<String> = emptyList(),
        /** Android drawable name; assets are bundled separately. */
        val logoResourceName: String? = null,
    )

    private val banks = listOf(
        BankInfo(BankId.MELLI, "بانک ملی ایران", "Melli Bank", "017", listOf("603799", "636214"), listOf("062"), listOf("ملی", "بانک ملی", "آینده", "بانک آینده"), "bank_melli"),
        BankInfo(BankId.MELLAT, "بانک ملت", "Mellat Bank", "012", listOf("610433", "991975"), aliases = listOf("ملت", "بانک ملت"), logoResourceName = "bank_mellat"),
        BankInfo(BankId.TEJARAT, "بانک تجارت", "Tejarat Bank", "018", listOf("627353", "585983"), aliases = listOf("تجارت", "بانک تجارت"), logoResourceName = "bank_tejarat"),
        BankInfo(BankId.SADERAT, "بانک صادرات ایران", "Saderat Bank", "019", listOf("603769"), aliases = listOf("صادرات", "بانک صادرات"), logoResourceName = "bank_saderat"),
        BankInfo(BankId.SEPAH, "بانک سپه", "Sepah Bank", "015", listOf("589210", "627381", "639599", "636949", "639370"), aliases = listOf("سپه", "انصار", "قوامین", "حکمت ایرانیان", "مهر اقتصاد"), logoResourceName = "bank_sepah"),
        BankInfo(BankId.REFAH, "بانک رفاه کارگران", "Refah Bank", "013", listOf("589463"), aliases = listOf("رفاه", "رفاه کارگران"), logoResourceName = "bank_refah"),
        BankInfo(BankId.MASKAN, "بانک مسکن", "Maskan Bank", "014", listOf("628023"), aliases = listOf("مسکن", "بانک مسکن"), logoResourceName = "bank_maskan"),
        BankInfo(BankId.KESHAVARZI, "بانک کشاورزی", "Keshavarzi Bank", "016", listOf("603770", "639217"), aliases = listOf("کشاورزی"), logoResourceName = "bank_keshavarzi"),
        BankInfo(BankId.SANAT_MADAN, "بانک صنعت و معدن", "Sanat-o-Madan Bank", "011", listOf("627961"), aliases = listOf("صنعت و معدن"), logoResourceName = "bank_sanat_madan"),
        BankInfo(BankId.POST, "پست بانک ایران", "Post Bank", "021", listOf("627760"), aliases = listOf("پست بانک"), logoResourceName = "bank_post"),
        BankInfo(BankId.TOSEE_SADERAT, "بانک توسعه صادرات ایران", "Export Development Bank", "020", listOf("627648", "207177"), aliases = listOf("توسعه صادرات"), logoResourceName = "bank_tosee_saderat"),
        BankInfo(BankId.TOSEE_TAAVON, "بانک توسعه تعاون", "Tosee Taavon Bank", "022", listOf("502908"), aliases = listOf("توسعه تعاون"), logoResourceName = "bank_tosee_taavon"),
        BankInfo(BankId.TOSEE, "موسسه اعتباری توسعه", "Tosee Credit Institution", "051", listOf("628157"), aliases = listOf("توسعه"), logoResourceName = "bank_tosee"),
        BankInfo(BankId.PARSIAN, "بانک پارسیان", "Parsian Bank", "054", listOf("622106", "639194", "627884"), aliases = listOf("پارسیان"), logoResourceName = "bank_parsian"),
        BankInfo(BankId.PASARGAD, "بانک پاسارگاد", "Pasargad Bank", "057", listOf("502229", "639347"), aliases = listOf("پاسارگاد"), logoResourceName = "bank_pasargad"),
        BankInfo(BankId.KARAFARIN, "بانک کارآفرین", "Karafarin Bank", "053", listOf("627488", "502910"), aliases = listOf("کارآفرین"), logoResourceName = "bank_karafarin"),
        BankInfo(BankId.SAMAN, "بانک سامان", "Saman Bank", "056", listOf("621986"), aliases = listOf("سامان"), logoResourceName = "bank_saman"),
        BankInfo(BankId.BLUBANK, "بلوبانک", "BluBank", "056", listOf("62198618", "62198619"), aliases = listOf("بلو", "بلوبانک"), logoResourceName = "bank_blu"),
        BankInfo(BankId.EGHTESAD_NOVIN, "بانک اقتصاد نوین", "Eghtesad Novin Bank", "055", listOf("627412"), aliases = listOf("اقتصاد نوین"), logoResourceName = "bank_eghtesad_novin"),
        BankInfo(BankId.SARMAYEH, "بانک سرمایه", "Sarmayeh Bank", "058", listOf("639607"), aliases = listOf("سرمایه"), logoResourceName = "bank_sarmayeh"),
        BankInfo(BankId.SINA, "بانک سینا", "Sina Bank", "059", listOf("639346"), aliases = listOf("سینا"), logoResourceName = "bank_sina"),
        BankInfo(BankId.MEHR_IRAN, "بانک قرض الحسنه مهر ایران", "Mehr Iran Bank", "060", listOf("606373"), aliases = listOf("مهر ایران", "مهر"), logoResourceName = "bank_mehr_iran"),
        BankInfo(BankId.SHAHR, "بانک شهر", "Shahr Bank", "061", listOf("502806", "504706"), aliases = listOf("شهر"), logoResourceName = "bank_shahr"),
        BankInfo(BankId.GARDESHGARI, "بانک گردشگری", "Gardeshgari Bank", "064", listOf("505416", "505426"), aliases = listOf("گردشگری"), logoResourceName = "bank_gardeshgari"),
        BankInfo(BankId.DEY, "بانک دی", "Dey Bank", "066", listOf("502938"), aliases = listOf("دی"), logoResourceName = "bank_dey"),
        BankInfo(BankId.IRAN_ZAMIN, "بانک ایران زمین", "Iran Zamin Bank", "069", listOf("505785"), aliases = listOf("ایران زمین"), logoResourceName = "bank_iran_zamin"),
        BankInfo(BankId.RESALAT, "بانک قرض الحسنه رسالت", "Resalat Bank", "070", listOf("504172"), aliases = listOf("رسالت"), logoResourceName = "bank_resalat"),
        BankInfo(BankId.MELAL, "موسسه اعتباری ملل", "Melal Credit Institution", "075", listOf("606256"), aliases = listOf("ملل"), logoResourceName = "bank_melal"),
        BankInfo(BankId.KHAVAR_MIANEH, "بانک خاورمیانه", "Middle East Bank", "078", listOf("585947"), aliases = listOf("خاورمیانه"), logoResourceName = "bank_khavar_mianeh"),
        BankInfo(BankId.IRAN_VENEZUELA, "بانک ایران و ونزوئلا", "Iran-Venezuela Bank", "095", emptyList(), aliases = listOf("ایران و ونزوئلا"), logoResourceName = "bank_iran_venezuela"),
        BankInfo(BankId.CENTRAL_BANK, "بانک مرکزی جمهوری اسلامی ایران", "Central Bank of Iran", "010", listOf("636795"), aliases = listOf("بانک مرکزی"), logoResourceName = "bank_central"),
    )

    private val byId = banks.associateBy { it.id }
    private val knownNonBankSenders = setOf("V.REFAH", "V.MASKAN")

    fun allBanks(): List<BankInfo> = banks
    fun findById(id: BankId): BankInfo? = byId[id]

    /** Longest-prefix matching is required for 62198618/19 (Blu) vs 621986 (Saman). */
    fun findByCard(cardNumber: String): BankInfo? {
        val normalized = normalizeDigits(cardNumber).filter(Char::isDigit)
        if (normalized.length < 6) return null
        return banks.asSequence()
            .flatMap { bank -> bank.cardPrefixes.asSequence().map { it to bank } }
            .filter { (prefix, _) -> normalized.startsWith(prefix) }
            .maxByOrNull { (prefix, _) -> prefix.length }
            ?.second
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
        val normalized = normalizeIban(iban) ?: return null
        val code = normalized.substring(4, 7)
        return banks.firstOrNull { it.ibanCode == code || code in it.legacyIbanCodes }
    }

    fun isValidIban(iban: String): Boolean {
        val normalized = normalizeIban(iban) ?: return false
        val rearranged = normalized.substring(4) + normalized.substring(0, 4)
        var remainder = 0
        for (char in rearranged) {
            val value = if (char.isDigit()) char - '0' else char - 'A' + 10
            remainder = (remainder * 10 + value) % 97
        }
        return remainder == 1
    }

    /** Conservative: sender IDs are added only after explicit verification. */
    fun findBySmsSender(sender: String): BankInfo? {
        val normalized = normalizeSender(sender)
        if (normalized.isEmpty() || normalized in knownNonBankSenders) return null
        return null
    }

    fun isKnownNonBankSender(sender: String): Boolean = normalizeSender(sender) in knownNonBankSenders

    private fun normalizeIban(value: String): String? {
        val normalized = normalizeDigits(value).uppercase().replace(" ", "").replace("-", "")
        return normalized.takeIf { it.matches(Regex("IR\\d{24}")) }
    }

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
