package org.fossify.messages.helpers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeResolverTest {
    private val colors = ThemeManager.ThemeColors(
        primary = 0xFF112233.toInt(),
        accent = 0xFF445566.toInt(),
        background = 0xFF101010.toInt(),
        surface = 0xFF202020.toInt(),
        textPrimary = 0xFFFFFFFF.toInt(),
        textSecondary = 0xFFBBBBBB.toInt(),
        incomingBubble = 0xFF303030.toInt(),
        outgoingBubble = 0xFF405060.toInt(),
        toolbar = 0xFF223344.toInt(),
        tab = 0xFF334455.toInt(),
        fab = 0xFF556677.toInt(),
        divider = 0x33808080
    )

    private val theme = ThemeManager.ThemeDefinition(
        id = "test",
        nameFa = "آزمایشی",
        nameEn = "Test",
        lightColors = colors,
        darkColors = colors
    )

    @Test
    fun resolve_preservesExistingPaletteSemantics() {
        val tokens = ThemeResolver.resolve(theme)

        assertEquals(colors.primary, tokens.primary)
        assertEquals(colors.accent, tokens.secondary)
        assertEquals(colors.toolbar, tokens.toolbar)
        assertEquals(colors.fab, tokens.fab)
        assertEquals(colors.background, tokens.background)
        assertEquals(colors.surface, tokens.surface)
        assertEquals(colors.incomingBubble, tokens.incomingMessage)
        assertEquals(colors.outgoingBubble, tokens.outgoingMessage)
        assertEquals(colors.textPrimary, tokens.messageText)
        assertEquals(colors.textSecondary, tokens.messageSecondaryText)
        assertEquals(colors.divider, tokens.divider)
        assertEquals(colors.accent, tokens.link)
    }

    @Test
    fun resolve_createsDeterministicSelectedItemToken() {
        val first = ThemeResolver.resolve(theme).selectedItem
        val second = ThemeResolver.resolve(theme).selectedItem

        assertEquals(first, second)
        assertEquals(0x24112233, first)
    }

    @Test
    fun resolve_mapsMaterial3OnColorsFromBackgroundContrast() {
        val tokens = ThemeResolver.resolve(theme)

        assertEquals(0xFFFFFFFF.toInt(), tokens.onPrimary)
        assertEquals(0xFFFFFFFF.toInt(), tokens.onSecondary)
        assertEquals(0xFFFFFFFF.toInt(), tokens.onBackground)
        assertEquals(0xFFFFFFFF.toInt(), tokens.onSurface)
        assertEquals(colors.textSecondary, tokens.onSurfaceVariant)
        assertNotEquals(tokens.primary, tokens.onPrimary)
    }

    @Test
    fun resolve_usesDarkForegroundOnLightSemanticSurfaces() {
        val lightColors = colors.copy(
            primary = 0xFFE8F5E9.toInt(),
            accent = 0xFFFFF59D.toInt(),
            background = 0xFFFFFFFF.toInt(),
            surface = 0xFFF7F7F7.toInt()
        )
        val lightTheme = theme.copy(
            lightColors = lightColors,
            darkColors = lightColors
        )

        val tokens = ThemeResolver.resolve(lightTheme)

        assertEquals(0xFF000000.toInt(), tokens.onPrimary)
        assertEquals(0xFF000000.toInt(), tokens.onSecondary)
        assertEquals(0xFF000000.toInt(), tokens.onBackground)
        assertEquals(0xFF000000.toInt(), tokens.onSurface)
    }

    @Test
    fun resolve_handlesOpaqueAndTransparentArgbWithoutAndroidColorApis() {
        val transparentColors = colors.copy(
            primary = 0x00112233,
            background = 0x00FFFFFF,
            surface = 0x00000000
        )
        val transparentTheme = theme.copy(
            lightColors = transparentColors,
            darkColors = transparentColors
        )

        val tokens = ThemeResolver.resolve(transparentTheme)

        assertEquals(0xFFFFFFFF.toInt(), tokens.onPrimary)
        assertEquals(0xFF000000.toInt(), tokens.onBackground)
        assertEquals(0xFFFFFFFF.toInt(), tokens.onSurface)
    }

    @Test
    fun resolve_keepsErrorRoleStable() {
        assertEquals(0xFFB3261E.toInt(), ThemeResolver.resolve(theme).error)
    }

    @Test
    fun resolve_allBuiltInThemesProducesCompleteSemanticTokens() {
        ThemeManager.builtInThemes.forEach { builtIn ->
            val tokens = ThemeResolver.resolve(builtIn)
            assertEquals(builtIn.lightColors.primary, tokens.primary)
            assertEquals(builtIn.lightColors.accent, tokens.secondary)
            assertEquals(builtIn.lightColors.background, tokens.background)
            assertEquals(builtIn.lightColors.surface, tokens.surface)
            assertEquals(builtIn.lightColors.toolbar, tokens.toolbar)
            assertEquals(builtIn.lightColors.fab, tokens.fab)
            assertEquals(builtIn.lightColors.outgoingBubble, tokens.outgoingMessage)
            assertTrue("${builtIn.id}: incoming/outgoing bubbles remain distinct", colorDistance(tokens.incomingMessage, tokens.outgoingMessage) >= 48.0)
            assertTrue("${builtIn.id}: selected item keeps primary RGB", tokens.selectedItem and 0x00FFFFFF == tokens.primary and 0x00FFFFFF)
            assertEquals(0xFFB3261E.toInt(), tokens.error)
        }
    }

    @Test
    fun contrastColor_isCentralAndDeterministic() {
        assertEquals(0xFFFFFFFF.toInt(), ThemeResolver.contrastColor(0xFF000000.toInt()))
        assertEquals(0xFF000000.toInt(), ThemeResolver.contrastColor(0xFFFFFFFF.toInt()))
        assertEquals(
            ThemeResolver.contrastColor(0xFF336699.toInt()),
            ThemeResolver.contrastColor(0xFF336699.toInt())
        )
    }

    private fun colorDistance(first: Int, second: Int): Double {
        val dr = ((first ushr 16) and 0xFF) - ((second ushr 16) and 0xFF)
        val dg = ((first ushr 8) and 0xFF) - ((second ushr 8) and 0xFF)
        val db = (first and 0xFF) - (second and 0xFF)
        return kotlin.math.sqrt((dr * dr + dg * dg + db * db).toDouble())
    }
}
