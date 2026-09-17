package org.fossify.messages.helpers

object ThemeResolver {
    fun resolve(theme: ThemeManager.ThemeDefinition): HomaThemeTokens =
        ThemeManager.contextForThemeFiles()?.let { resolve(theme, it) } ?: resolve(theme, false)

    fun resolve(theme: ThemeManager.ThemeDefinition, darkMode: Boolean): HomaThemeTokens =
        tokensFromColors(theme.colorsForMode(darkMode))

    fun resolve(theme: ThemeManager.ThemeDefinition, context: android.content.Context): HomaThemeTokens =
        resolve(theme, ThemeManager.contextForDarkMode(context))

    @Deprecated("Use resolve(theme, darkMode)")
    fun resolveSettings(theme: ThemeManager.ThemeDefinition, darkMode: Boolean): HomaThemeTokens = resolve(theme, darkMode)

    private fun tokensFromColors(colors: ThemeManager.ThemeColors): HomaThemeTokens {
        return HomaThemeTokens(
            primary = colors.primary,
            onPrimary = contrastColor(colors.primary),
            secondary = colors.accent,
            onSecondary = contrastColor(colors.accent),
            background = colors.background,
            onBackground = contrastColor(colors.background),
            surface = colors.surface,
            onSurface = contrastColor(colors.surface),
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
            selectedItem = (colors.primary and 0x00FFFFFF) or 0x24000000,
            link = colors.accent,
            divider = colors.divider
        )
    }

    internal fun contrastColor(background: Int): Int {
        val red = (background shr 16) and 0xFF
        val green = (background shr 8) and 0xFF
        val blue = background and 0xFF
        val luminance = (0.299 * red + 0.587 * green + 0.114 * blue) / 255.0
        return if (luminance > 0.55) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
    }
}
