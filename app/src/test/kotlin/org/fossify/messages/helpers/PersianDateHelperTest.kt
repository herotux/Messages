package org.fossify.messages.helpers

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class PersianDateHelperTest {
    @Test
    fun convertsNowruzToPersianNewYear() {
        val timestamp = Instant.parse("2026-03-21T00:00:00Z").toEpochMilli()
        assertEquals("1405/1/1", PersianDateHelper.format(timestamp, includeTime = false))
    }

    @Test
    fun convertsDigitsToPersianDigits() {
        assertEquals("۱۴۰۵/۱/۱", PersianDateHelper.toPersianDigits("1405/1/1"))
    }
}
