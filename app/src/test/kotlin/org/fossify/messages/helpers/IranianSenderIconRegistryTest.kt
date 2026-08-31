package org.fossify.messages.helpers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class IranianSenderIconRegistryTest {
    @Test fun bankShahrHasSeparateSenderIconEntry() {
        val info = IranianSenderIconRegistry.find("Bank Shahr")
        assertNotNull(info)
        assertEquals(IranianSenderIconRegistry.Category.BANK, info?.category)
        assertEquals("bank_shahr", info?.logoResourceName)
    }

    @Test fun iranCellAndMokhaberatAreServiceSendersNotBanks() {
        assertEquals(IranianSenderIconRegistry.Category.TELECOM, IranianSenderIconRegistry.find("IrancelleTo")?.category)
        assertEquals(IranianSenderIconRegistry.Category.TELECOM, IranianSenderIconRegistry.find("Mokhaberat")?.category)
    }

    @Test fun kurdistanUtilitySendersHaveTheirOwnCategory() {
        assertEquals("sender_gas", IranianSenderIconRegistry.find("+984040102020")?.logoResourceName)
        assertEquals("sender_electricity", IranianSenderIconRegistry.find("+98404014013900")?.logoResourceName)
    }

    @Test fun sakhdDoesNotStealMokhaberatSender() {
        assertEquals("SAKHD", IranianSenderIconRegistry.find("+9860009621")?.id)
        assertEquals("MOKHABERAT", IranianSenderIconRegistry.find("Mokhaberat")?.id)
    }

    @Test fun unknownSenderHasNoIcon() {
        assertNull(IranianSenderIconRegistry.find("random-person"))
    }
}
