package org.fossify.messages.helpers

/** Converts a persisted ThemeDefinition into stable Homa semantic UI tokens. */
object ThemeResolver {
    fun resolve(theme: ThemeManager.ThemeDefinition): HomaThemeTokens {
        val colors = theme.colors
        val onPrimary = colors.textPrimary
        val onBackground = colors.textPrimary
        val onSurface = colors.textPrimary
        val onSurfaceVariant = colors.textSecondary
        val selectedItem = withAlpha(colors.primary, 0x24)
        val unreadIndicator = colors.accent
        val error = 0xFFB3261E.toInt()

        return HomaThemeTokens(
            primary = colors.primary,
            onPrimary = onPrimary,
            secondary = colors.accent,
            onSecondary = onPrimary,
            background = colors.background,
            onBackground = onBackground,
            surface = colors.surface,
            onSurface = onSurface,
            surfaceVariant = colors.surface,
            onSurfaceVariant = onSurfaceVariant,
            outline = colors.divider,
            error = error,
            incomingMessage = colors.incomingBubble,
            outgoingMessage = colors.outgoingBubble,
            messageText = colors.textPrimary,
            messageSecondaryText = colors.textSecondary,
            unreadIndicator = unreadIndicator,
            selectedItem = selectedItem,
            link = colors.accent,
            divider = colors.divider
        )
    }

    private fun withAlpha(color: Int, alpha: Int): Int =
        (color and 0x00FFFFFF) or ((alpha and 0xFF) shl 24)
}
