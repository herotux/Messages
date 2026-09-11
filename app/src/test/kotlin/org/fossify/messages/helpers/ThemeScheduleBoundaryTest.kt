package org.fossify.messages.helpers

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeScheduleBoundaryTest {
    @Test
    fun normalSchedule_boundaries_areHalfOpen() {
        assertTrue(ThemeScheduleManager.isDayPeriodAtMinutes(0, 1439, 0))
        assertTrue(ThemeScheduleManager.isDayPeriodAtMinutes(0, 1439, 1438))
        assertFalse(ThemeScheduleManager.isDayPeriodAtMinutes(0, 1439, 1439))
    }

    @Test
    fun overnightSchedule_boundaries_includeMidnight() {
        assertTrue(ThemeScheduleManager.isDayPeriodAtMinutes(22 * 60, 7 * 60, 22 * 60))
        assertTrue(ThemeScheduleManager.isDayPeriodAtMinutes(22 * 60, 7 * 60, 0))
        assertTrue(ThemeScheduleManager.isDayPeriodAtMinutes(22 * 60, 7 * 60, 6 * 60 + 59))
        assertFalse(ThemeScheduleManager.isDayPeriodAtMinutes(22 * 60, 7 * 60, 7 * 60))
    }

    @Test
    fun outOfRangeMinutes_areClampedBeforeEvaluation() {
        assertTrue(ThemeScheduleManager.isDayPeriodAtMinutes(-1, 60, 0))
        assertFalse(ThemeScheduleManager.isDayPeriodAtMinutes(0, 2000, 1439))
    }
}
