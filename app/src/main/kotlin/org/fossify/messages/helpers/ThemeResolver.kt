package org.fossify.messages.helpers

/** Converts a selected ThemeDefinition into the runtime semantic palette. */
object ThemeResolver {
    fun resolve(theme: ThemeManager.ThemeDefinition, darkMode: Boolean): HomaThemeTokens =
        tokensFromColors(theme.colorsForMode(darkMode))

    fun resolve(theme: ThemeManager.ThemeDefinition, context: android.content.Context): HomaThemeTokens =
        resolve(theme, ThemeManager.contextForDarkMode(context))

    /** Compatibility entry point: Settings uses the same selected theme palette, never a global palette. */
    @Deprecated("Use resolve(theme, darkMode)")
    fun resolveSettings(theme: ThemeManager.ThemeDefinition, darkMode: Boolean): HomaThemeTokens =
        resolve(theme, darkMode)

    private fun tokensFromColors(colors: ThemeManager.ThemeColors): HomaThemeTokens {
        val onPrimary = contrastColor(colors.primary)
        val onSecondary = contrastColor(colors.accent)
        val onBackground = contrastColor(colors.background)
        val onSurface = contrastColor(colors.surface)
        return HomaThemeTokens(
            primary = colors.primary,
            onPrimary = onPrimary,
            secondary = colors.accent,
            onSecondary = onSecondary,
            background = colors.background,
            onBackground = onBackground,
            surface = colors.surface,
            onSurface = onSurface,
            surfaceVariant = colors.surface,
            onSurfaceVariant = colors.textSecondary,
            outline = colors.divider,
            error = 0xFFB3261E.toInt(),
            toolbar = colors.toolbar,
            fab = colors.fab,
            incomingMessage = colors.incomingBubble,
            outgoingMessage = colors.outgoingBubble,
            messageText = colors.textPrimary,
            messageSecondaryText = colors.textSecondary,
            unreadIndicator = colors.accent,
            selectedItem = withAlpha(colors.primary, 0x24),
            link = colors.accent,
            divider = colors.divider
        )
    }

    private fun withAlpha(color: Int, alpha: Int): Int = (color and 0x00FFFFFF) or ((alpha and 0xFF) shl 24)

    internal fun contrastColor(background: Int): Int {
        val red = (background shr 16) and 0xFF
        val green = (background shr 8) and 0xFF
        val blue = background and 0xFF
        val luminance = (0.299 * red + 0.587 * green + 0.114 * blue) / 255.0
        return if (luminance > 0.55) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
    }
}
