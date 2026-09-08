package org.fossify.messages.helpers

import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeValidatorTest {
    @Test
    fun contrastRatio_blackOnWhite_is21() {
        assertEquals(21.0, ThemeValidator.contrastRatio(Color.BLACK, Color.WHITE), 0.001)
    }

    @Test
    fun bestTextColor_prefersWhiteOnDarkBackground() {
        assertEquals(Color.WHITE, ThemeValidator.bestTextColor(Color.rgb(20, 20, 20)))
    }

    @Test
    fun bestTextColor_prefersBlackOnLightBackground() {
        assertEquals(Color.BLACK, ThemeValidator.bestTextColor(Color.rgb(240, 240, 240)))
    }

    @Test
    fun validate_detectsLowContrastPairs() {
        val colors = ThemeManager.ThemeColors(
            primary = Color.BLUE,
            accent = Color.BLUE,
            background = Color.WHITE,
            surface = Color.WHITE,
            textPrimary = Color.LTGRAY,
            textSecondary = Color.LTGRAY,
            incomingBubble = Color.WHITE,
            outgoingBubble = Color.WHITE,
            toolbar = Color.BLUE,
            tab = Color.BLUE,
            fab = Color.BLUE
        )

        val report = ThemeValidator.validate(colors)
        assertTrue(report.issues.isNotEmpty())
    }
}
