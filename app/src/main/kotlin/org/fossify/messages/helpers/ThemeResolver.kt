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
