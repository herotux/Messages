package org.fossify.messages.helpers

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.util.Locale

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

    @Test
    fun formatsEnglishMonthAbbreviationWithLatinDigits() {
        val timestamp = Instant.parse("2026-08-21T00:00:00Z").toEpochMilli()
        assertEquals("30 mor 1405", PersianDateHelper.formatMonthName(timestamp, Locale.ENGLISH))
    }

    @Test
    fun formatsPersianMonthNameWithPersianDigits() {
        val timestamp = Instant.parse("2026-08-21T00:00:00Z").toEpochMilli()
        assertEquals("۳۰ مرداد ۱۴۰۵", PersianDateHelper.formatMonthName(timestamp, Locale("fa")))
    }
}
