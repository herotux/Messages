package org.fossify.messages.helpers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeFileManagerGradientTest {
    private val colors = ThemeManager.ThemeColors(
        primary = 0xFF1565C0.toInt(),
        accent = 0xFF42A5F5.toInt(),
        background = 0xFF0D1B2A.toInt(),
        surface = 0xFF1B263B.toInt(),
        textPrimary = 0xFFFFFFFF.toInt(),
        textSecondary = 0xFFB0BEC5.toInt(),
        incomingBubble = 0xFF263850.toInt(),
        outgoingBubble = 0xFF1565C0.toInt(),
        toolbar = 0xFF1565C0.toInt(),
        tab = 0xFF42A5F5.toInt(),
        fab = 0xFF1E88E5.toInt()
    )

    @Test
    fun gradientRoundTripPreservesBackgroundSettings() {
        val original = ThemeManager.ThemeDefinition(
            id = "gradient-test",
            nameFa = "گرادیان تست",
            nameEn = "Gradient Test",
            source = ThemeManager.ThemeSource.USER,
            colors = colors,
            backgroundType = ThemeManager.BackgroundType.LINEAR_GRADIENT,
            gradientColors = listOf(0xFF0D47A1.toInt(), 0xFF00ACC1.toInt()),
            gradientAngle = 135
        )

        val raw = ThemeFileManager.export(original)
        val imported = ThemeFileManager.importTheme(raw).getOrThrow()

        assertEquals(ThemeManager.BackgroundType.LINEAR_GRADIENT, imported.backgroundType)
        assertEquals(listOf(0xFF0D47A1.toInt(), 0xFF00ACC1.toInt()), imported.gradientColors)
        assertEquals(135, imported.gradientAngle)
        assertTrue(raw.contains("\"version\": ${ThemeFileManager.CURRENT_VERSION}"))
    }

    @Test
    fun legacyVersionOneFileStillImportsAsSolid() {
        val raw = """
            {
              "schema":"homa-theme",
              "version":1,
              "theme":{
                "id":"legacy",
                "nameFa":"قدیمی",
                "nameEn":"Legacy",
                "colors":{
                  "primary":"#FF388E3C",
                  "accent":"#FF4CAF50",
                  "background":"#FF161616",
                  "surface":"#FF242424",
                  "textPrimary":"#FFFFFFFF",
                  "textSecondary":"#FFBDBDBD",
                  "incomingBubble":"#FF2A2A2A",
                  "outgoingBubble":"#FF388E3C",
                  "toolbar":"#FF388E3C",
                  "tab":"#FF388E3C",
                  "fab":"#FF4CAF50",
                  "divider":"#33808080"
                }
              }
            }
        """.trimIndent()

        val imported = ThemeFileManager.importTheme(raw).getOrThrow()
        assertEquals(ThemeManager.BackgroundType.SOLID, imported.backgroundType)
        assertTrue(imported.gradientColors.isEmpty())
    }
}
