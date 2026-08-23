package org.fossify.messages.helpers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IranianBankRegistryTest {
    @Test
    fun cardPrefixLookupUsesLongestPrefix() {
        assertEquals(
            IranianBankRegistry.BankId.BLUBANK,
            IranianBankRegistry.findByCard("6219861900000000")?.id,
        )
        assertEquals(
            IranianBankRegistry.BankId.BLUBANK,
            IranianBankRegistry.findByCard("6219861800000000")?.id,
        )
        assertEquals(
            IranianBankRegistry.BankId.SAMAN,
            IranianBankRegistry.findByCard("6219860099999999")?.id,
        )
    }

    @Test
    fun cardLookupNormalizesPersianDigits() {
        assertEquals(
            IranianBankRegistry.BankId.MELLI,
            IranianBankRegistry.findByCard("۶۰۳۷۹۹۰۰۰۰۰۰۰۰۰۰")?.id,
        )
    }

    @Test
    fun cardLuhnValidationWorks() {
        assertTrue(IranianBankRegistry.isValidCardNumber("6037997512345675"))
        assertFalse(IranianBankRegistry.isValidCardNumber("6037997512345676"))
    }

    @Test
    fun ibanLookupUsesBankCode() {
        assertEquals(
            IranianBankRegistry.BankId.MELLAT,
            IranianBankRegistry.findByIban("IR000120000000000000000000")?.id,
        )
        assertNull(IranianBankRegistry.findByIban("123"))
    }

    @Test
    fun knownNonBankSendersAreNotBanks() {
        assertTrue(IranianBankRegistry.isKnownNonBankSender("v.refah"))
        assertTrue(IranianBankRegistry.isKnownNonBankSender("V.MASKAN"))
        assertNull(IranianBankRegistry.findBySmsSender("V.REFAH"))
    }
}
