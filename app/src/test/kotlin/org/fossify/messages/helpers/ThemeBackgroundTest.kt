package org.fossify.messages.helpers

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeBackgroundTest {
    @Test
    fun normalizeAngle_snapsToNearest45Degrees() {
        assertEquals(0, ThemeBackground.normalizeAngle(0))
        assertEquals(0, ThemeBackground.normalizeAngle(22))
        assertEquals(45, ThemeBackground.normalizeAngle(23))
        assertEquals(315, ThemeBackground.normalizeAngle(-46))
        assertEquals(0, ThemeBackground.normalizeAngle(360))
        assertEquals(0, ThemeBackground.normalizeAngle(359))
    }
}
