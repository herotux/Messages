package org.fossify.messages.helpers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IranianBankRegistryTest {
    @Test
    fun cardPrefixLookupUsesLongestPrefix() {
        assertEquals(IranianBankRegistry.BankId.BLUBANK, IranianBankRegistry.findByCard("6219861900000000")?.id)
        assertEquals(IranianBankRegistry.BankId.BLUBANK, IranianBankRegistry.findByCard("6219861800000000")?.id)
        assertEquals(IranianBankRegistry.BankId.SAMAN, IranianBankRegistry.findByCard("6219860099999999")?.id)
    }

    @Test
    fun cardLookupNormalizesPersianDigits() {
        assertEquals(IranianBankRegistry.BankId.MELLI, IranianBankRegistry.findByCard("۶۰۳۷۹۹۰۰۰۰۰۰۰۰۰۶")?.id)
    }

    @Test
    fun cardLuhnValidationWorks() {
        assertTrue(IranianBankRegistry.isValidCardNumber("6037990000000006"))
        assertFalse(IranianBankRegistry.isValidCardNumber("6037990000000007"))
    }

    @Test
    fun ibanLookupAndValidationUseBankCode() {
        val iban = "IR700120000000000000000000"
        assertEquals(IranianBankRegistry.BankId.MELLAT, IranianBankRegistry.findByIban(iban)?.id)
        assertTrue(IranianBankRegistry.isValidIban(iban))
        assertNull(IranianBankRegistry.findByIban("123"))
    }

    @Test
    fun knownNonBankSendersAreNotBanks() {
        assertTrue(IranianBankRegistry.isKnownNonBankSender("v.refah"))
        assertTrue(IranianBankRegistry.isKnownNonBankSender("V.MASKAN"))
        assertNull(IranianBankRegistry.findBySmsSender("V.REFAH"))
    }

    @Test
    fun verifiedSmsSenderLookupWorks() {
        assertEquals(IranianBankRegistry.BankId.MELLAT, IranianBankRegistry.findBySmsSender("200033")?.id)
        assertEquals(IranianBankRegistry.BankId.MELLAT, IranianBankRegistry.findBySmsSender("BankMellat")?.id)
        assertEquals(IranianBankRegistry.BankId.MELLI, IranianBankRegistry.findBySmsSender("۳۰۰۰۱۷")?.id)
        assertEquals(IranianBankRegistry.BankId.SAMAN, IranianBankRegistry.findBySmsSender("20000")?.id)
    }

    @Test
    fun banksWithBundledLogosExposeTheirDrawableName() {
        assertEquals("bank_melli", IranianBankRegistry.findById(IranianBankRegistry.BankId.MELLI)?.logoResourceName)
        assertEquals("bank_mehr_iran", IranianBankRegistry.findById(IranianBankRegistry.BankId.MEHR_IRAN)?.logoResourceName)
        assertNotNull(IranianBankRegistry.findById(IranianBankRegistry.BankId.MELLAT)?.logoResourceName)
    }

    @Test
    fun bankSmsDetectorRecognizesVerifiedSender() {
        val detection = BankSmsDetector.detect("200033", "تراکنش با موفقیت انجام شد")
        assertEquals(IranianBankRegistry.BankId.MELLAT, detection?.bank?.id)
        assertEquals(BankSmsDetector.Confidence.HIGH, detection?.confidence)
        assertEquals(BankSmsDetector.Reason.VERIFIED_SENDER, detection?.reason)
    }

    @Test
    fun bankSmsDetectorRecognizesValidCardWithPersianDigits() {
        val detection = BankSmsDetector.detect("1000", "مبلغ از کارت ۶۰۳۷-۹۹۰۰-۰۰۰۰-۰۰۰۶ کسر شد")
        assertEquals(IranianBankRegistry.BankId.MELLI, detection?.bank?.id)
        assertEquals(BankSmsDetector.Confidence.HIGH, detection?.confidence)
        assertEquals(BankSmsDetector.Reason.CARD_NUMBER, detection?.reason)
    }

    @Test
    fun bankSmsDetectorRecognizesValidIban() {
        val detection = BankSmsDetector.detect("1000", "شماره شبا IR70 0120 0000 0000 0000 0000 00")
        assertEquals(IranianBankRegistry.BankId.MELLAT, detection?.bank?.id)
        assertEquals(BankSmsDetector.Confidence.HIGH, detection?.confidence)
        assertEquals(BankSmsDetector.Reason.IBAN, detection?.reason)
    }

    @Test
    fun bankSmsDetectorRequiresTransactionContextForExplicitBankName() {
        val detection = BankSmsDetector.detect(
            "1000",
            "بانک ملت\nمبلغ: 6000000\nمانده: 6035959\nزمان: 1405/05/29\nواریز"
        )
        assertEquals(IranianBankRegistry.BankId.MELLAT, detection?.bank?.id)
        assertEquals(BankSmsDetector.Confidence.HIGH, detection?.confidence)
        assertEquals(BankSmsDetector.Reason.TRANSACTION_CONTEXT, detection?.reason)
    }

    @Test
    fun bankSmsDetectorRejectsBankNameWithoutTransactionContext() {
        assertNull(BankSmsDetector.detect("1000", "من امروز به بانک ملت مراجعه کردم"))
    }

    @Test
    fun bankSmsDetectorRejectsPromotionalMessageContainingBankName() {
        val body = "هموطن گرامی برای مشارکت در پویش بنای مهربانی و حمایت از جامعه هدف سازمان بهزیستی، کمک های نقدی خود را از طریق درگاه واریز نمائید. بانک مهر ایران"
        assertNull(BankSmsDetector.detect("1000", body))
    }

    @Test
    fun bankSmsDetectorDoesNotTrustUnknownSenderAlone() {
        assertNull(BankSmsDetector.detect("IR-MELLAT", "تراکنش انجام شد"))
    }

    @Test
    fun bankSmsDetectorDoesNotUseInvalidCard() {
        assertNull(BankSmsDetector.detect("1000", "کارت ۶۰۳۷۹۹۰۰۰۰۰۰۰۰۰۷"))
    }
}
