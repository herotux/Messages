package org.fossify.messages.helpers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
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
        colors = colors
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
    fun resolve_mapsMaterial3OnColorsFromReadableThemeText() {
        val tokens = ThemeResolver.resolve(theme)

        assertEquals(colors.textPrimary, tokens.onPrimary)
        assertEquals(colors.textPrimary, tokens.onSecondary)
        assertEquals(colors.textPrimary, tokens.onBackground)
        assertEquals(colors.textPrimary, tokens.onSurface)
        assertEquals(colors.textSecondary, tokens.onSurfaceVariant)
        assertNotEquals(tokens.primary, tokens.onPrimary)
    }

    @Test
    fun resolve_keepsErrorRoleStable() {
        assertEquals(0xFFB3261E.toInt(), ThemeResolver.resolve(theme).error)
    }
}
