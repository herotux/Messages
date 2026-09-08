package org.fossify.messages.helpers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeValidatorTest {
    private val white = 0xFFFFFFFF.toInt()
    private val black = 0xFF000000.toInt()

    @Test
    fun contrastRatio_blackOnWhite_is21() {
        assertEquals(21.0, ThemeValidator.contrastRatio(black, white), 0.001)
    }

    @Test
    fun bestTextColor_prefersWhiteOnDarkBackground() {
        assertEquals(white, ThemeValidator.bestTextColor(0xFF141414.toInt()))
    }

    @Test
    fun bestTextColor_prefersBlackOnLightBackground() {
        assertEquals(black, ThemeValidator.bestTextColor(0xFFF0F0F0.toInt()))
    }

    @Test
    fun validate_detectsLowContrastPairs() {
        val colors = ThemeManager.ThemeColors(
            primary = 0xFF0000FF.toInt(),
            accent = 0xFF0000FF.toInt(),
            background = white,
            surface = white,
            textPrimary = 0xFFD3D3D3.toInt(),
            textSecondary = 0xFFD3D3D3.toInt(),
            incomingBubble = white,
            outgoingBubble = white,
            toolbar = 0xFF0000FF.toInt(),
            tab = 0xFF0000FF.toInt(),
            fab = 0xFF0000FF.toInt()
        )

        val report = ThemeValidator.validate(colors)
        assertTrue(report.issues.isNotEmpty())
    }
}
