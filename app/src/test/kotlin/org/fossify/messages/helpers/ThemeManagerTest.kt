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
    fun builtInThemes_haveCompleteColorPalettes() {
        ThemeManager.builtInThemes.forEach { theme ->
            assertCompletePalette(theme.id, theme.lightColors)
            assertCompletePalette(theme.id, theme.darkColors)
        }
    }

    @Test
    fun builtInThemes_haveStableIdentityMetadata() {
        ThemeManager.builtInThemes.forEach { theme ->
            assertTrue(theme.id.isNotBlank())
            assertTrue(theme.nameFa.isNotBlank())
            assertTrue(theme.nameEn.isNotBlank())
            assertEquals(ThemeManager.ThemeSource.BUILT_IN, theme.source)
        }
    }

    @Test
    fun customThemeDefinition_defaultsToSolidBackground() {
        val colors = ThemeManager.builtInThemes.first().lightColors
        val theme = ThemeManager.ThemeDefinition(
            id = "test",
            nameFa = "آزمایشی",
            nameEn = "Test",
            lightColors = colors,
            darkColors = colors
        )
        assertEquals(ThemeManager.ThemeSource.BUILT_IN, theme.source)
        assertEquals(ThemeManager.BackgroundType.SOLID, theme.backgroundType)
        assertTrue(theme.gradientColors.isEmpty())
    }

    private fun assertCompletePalette(id: String, colors: ThemeManager.ThemeColors) {
        assertTrue("$id: primary", colors.primary != 0)
        assertTrue("$id: accent", colors.accent != 0)
        assertTrue("$id: background", colors.background != 0)
        assertTrue("$id: surface", colors.surface != 0)
        assertTrue("$id: textPrimary", colors.textPrimary != 0)
        assertTrue("$id: textSecondary", colors.textSecondary != 0)
        assertTrue("$id: incomingBubble", colors.incomingBubble != 0)
        assertTrue("$id: outgoingBubble", colors.outgoingBubble != 0)
        assertTrue("$id: toolbar", colors.toolbar != 0)
        assertTrue("$id: tab", colors.tab != 0)
        assertTrue("$id: fab", colors.fab != 0)
        assertTrue("$id: divider", colors.divider != 0)
    }
}
