package org.fossify.messages.helpers

/**
 * Converts a persisted ThemeDefinition into stable Homa semantic roles.
 *
 * The project is View/Material based rather than Compose based, so these roles
 * are the app's runtime equivalent of a Material 3 ColorScheme. Widgets consume
 * semantic roles through ThemeApplier instead of reaching into legacy palette
 * values directly.
 */
object ThemeResolver {
    fun resolve(theme: ThemeManager.ThemeDefinition): HomaThemeTokens {
        val colors = theme.colors
        return tokensFromColors(colors)
    }

    /**
     * Resolves colors for app-owned utility screens such as Settings.
     *
     * Theme definitions historically store conversation palettes, many of which
     * are intentionally dark. Applying those background/text colors verbatim to
     * Settings made a light UI inherit a dark surface and dark/low-contrast text.
     * Settings still uses the selected theme's brand colors, but its surfaces and
     * foregrounds follow the actual light/dark UI mode.
     */
    fun resolveSettings(theme: ThemeManager.ThemeDefinition, darkMode: Boolean): HomaThemeTokens {
        val colors = theme.colors
        val light = !darkMode

        val background = if (light) 0xFFFFFBFE.toInt() else 0xFF121212.toInt()
        val surface = if (light) 0xFFFFFBFE.toInt() else 0xFF1E1E1E.toInt()
        val surfaceVariant = if (light) 0xFFF3EDF7.toInt() else 0xFF2A2A2A.toInt()
        val textPrimary = if (light) 0xFF1D1B20.toInt() else 0xFFE6E1E5.toInt()
        val textSecondary = if (light) 0xFF49454F.toInt() else 0xFFCAC4D0.toInt()
        val divider = if (light) 0x1F1D1B20 else 0x33FFFFFF
        val toolbar = surface
        val onPrimary = contrastColor(colors.primary)
        val onSecondary = contrastColor(colors.accent)

        return HomaThemeTokens(
            primary = colors.primary,
            onPrimary = onPrimary,
            secondary = colors.accent,
            onSecondary = onSecondary,
            background = background,
            onBackground = textPrimary,
            surface = surface,
            onSurface = textPrimary,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = textSecondary,
            outline = if (light) 0xFF79747E.toInt() else 0xFF938F99.toInt(),
            error = 0xFFB3261E.toInt(),
            toolbar = toolbar,
            fab = colors.fab,
            incomingMessage = if (light) 0xFFE8DEF8.toInt() else 0xFF4A4458.toInt(),
            outgoingMessage = if (light) 0xFFD0BCFF.toInt() else 0xFF6750A4.toInt(),
            messageText = textPrimary,
            messageSecondaryText = textSecondary,
            unreadIndicator = colors.accent,
            selectedItem = withAlpha(colors.primary, 0x24),
            link = colors.accent,
            divider = divider
        )
    }

    private fun tokensFromColors(colors: ThemeManager.ThemeColors): HomaThemeTokens {
        val onPrimary = contrastColor(colors.primary)
        val onSecondary = contrastColor(colors.accent)
        val onBackground = contrastColor(colors.background)
        val onSurface = contrastColor(colors.surface)
        val onSurfaceVariant = colors.textSecondary

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
            onSurfaceVariant = onSurfaceVariant,
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

    private fun withAlpha(color: Int, alpha: Int): Int =
        (color and 0x00FFFFFF) or ((alpha and 0xFF) shl 24)

    /** Returns a readable Material foreground for a solid background color. */
    internal fun contrastColor(background: Int): Int {
        // Keep the resolver platform-independent so its semantic mapping can be
        // exercised by JVM unit tests without relying on Android framework APIs.
        val red = (background shr 16) and 0xFF
        val green = (background shr 8) and 0xFF
        val blue = background and 0xFF
        val luminance = (0.299 * red + 0.587 * green + 0.114 * blue) / 255.0
        return if (luminance > 0.55) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
    }
}
