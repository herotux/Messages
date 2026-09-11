package org.fossify.messages.helpers

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeScheduleManagerTest {
    @Test
    fun dayPeriod_normalSchedule_usesDayBetweenTransitions() {
        assertTrue(ThemeScheduleManager.isDayPeriodAtMinutes(7 * 60, 19 * 60, 7 * 60))
        assertTrue(ThemeScheduleManager.isDayPeriodAtMinutes(7 * 60, 19 * 60, 12 * 60))
        assertFalse(ThemeScheduleManager.isDayPeriodAtMinutes(7 * 60, 19 * 60, 19 * 60))
        assertFalse(ThemeScheduleManager.isDayPeriodAtMinutes(7 * 60, 19 * 60, 23 * 60))
    }

    @Test
    fun dayPeriod_overnightSchedule_usesDayAcrossMidnight() {
        assertTrue(ThemeScheduleManager.isDayPeriodAtMinutes(19 * 60, 7 * 60, 23 * 60))
        assertTrue(ThemeScheduleManager.isDayPeriodAtMinutes(19 * 60, 7 * 60, 6 * 60))
        assertFalse(ThemeScheduleManager.isDayPeriodAtMinutes(19 * 60, 7 * 60, 12 * 60))
    }

    @Test
    fun dayPeriod_equalTransitions_isDeterministicallyDay() {
        assertTrue(ThemeScheduleManager.isDayPeriodAtMinutes(8 * 60, 8 * 60, 0))
        assertTrue(ThemeScheduleManager.isDayPeriodAtMinutes(8 * 60, 8 * 60, 23 * 60 + 59))
    }
}
