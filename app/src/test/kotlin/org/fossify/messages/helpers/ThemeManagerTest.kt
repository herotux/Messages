package org.fossify.messages.helpers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeManagerTest {
    @Test
    fun builtInThemes_haveUniqueIds() {
        val ids = ThemeManager.builtInThemes.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun builtInThemes_coverExpectedPresets() {
        assertNotNull(ThemeManager.findBuiltIn(ThemeManager.DEFAULT_ID))
        assertNotNull(ThemeManager.findBuiltIn(ThemeManager.AURORA_ID))
        assertNotNull(ThemeManager.findBuiltIn(ThemeManager.OCEAN_ID))
        assertNotNull(ThemeManager.findBuiltIn(ThemeManager.SUNSET_ID))
        assertNotNull(ThemeManager.findBuiltIn(ThemeManager.FOREST_ID))
        assertNotNull(ThemeManager.findBuiltIn(ThemeManager.VIOLET_ID))
        assertNotNull(ThemeManager.findBuiltIn(ThemeManager.MIDNIGHT_ID))
    }

    @Test
    fun customThemeDefinition_defaultsToSolidBackground() {
        val theme = ThemeManager.ThemeDefinition(
            id = "test",
            nameFa = "آزمایشی",
            nameEn = "Test",
            colors = ThemeManager.builtInThemes.first().colors
        )
        assertEquals(ThemeManager.ThemeSource.BUILT_IN, theme.source)
        assertEquals(ThemeManager.BackgroundType.SOLID, theme.backgroundType)
        assertTrue(theme.gradientColors.isEmpty())
    }
}
