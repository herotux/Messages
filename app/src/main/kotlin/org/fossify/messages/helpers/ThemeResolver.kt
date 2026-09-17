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
     * Conversation themes can intentionally use dark backgrounds even when the
     * application is currently in light mode. Settings must not inherit those
     * conversation surfaces. It keeps the selected theme's primary/accent as
     * branding, while its surfaces and foregrounds follow the actual UI mode.
     */
    fun resolveSettings(theme: ThemeManager.ThemeDefinition, darkMode: Boolean): HomaThemeTokens {
        val colors = theme.colors
        val light = !darkMode

        val background = if (light) blend(colors.primary, 0xFFFFFBFE.toInt(), 0.035f) else 0xFF121212.toInt()
        val surface = if (light) 0xFFFFFBFE.toInt() else 0xFF1E1E1E.toInt()
        val surfaceVariant = if (light) blend(colors.primary, 0xFFFFFFFF.toInt(), 0.065f) else 0xFF2A2A2A.toInt()
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
            incomingMessage = if (light) blend(colors.primary, 0xFFFFFFFF.toInt(), 0.82f) else 0xFF4A4458.toInt(),
            outgoingMessage = if (light) blend(colors.primary, 0xFFFFFFFF.toInt(), 0.70f) else 0xFF6750A4.toInt(),
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

    private fun blend(foreground: Int, background: Int, amount: Float): Int {
        val a = amount.coerceIn(0f, 1f)
        val fr = (foreground shr 16) and 0xFF
        val fg = (foreground shr 8) and 0xFF
        val fb = foreground and 0xFF
        val br = (background shr 16) and 0xFF
        val bg = (background shr 8) and 0xFF
        val bb = background and 0xFF
        val r = (br + ((fr - br) * a)).toInt().coerceIn(0, 255)
        val g = (bg + ((fg - bg) * a)).toInt().coerceIn(0, 255)
        val b = (bb + ((fb - bb) * a)).toInt().coerceIn(0, 255)
        return 0xFF000000.toInt() or (r shl 16) or (g shl 8) or b
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
