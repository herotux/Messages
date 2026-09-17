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
        val incomingBubble = resolveIncomingBubble(
            configured = colors.incomingBubble,
            outgoing = colors.outgoingBubble,
            surface = colors.surface,
            background = colors.background,
        )
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
            incomingMessage = incomingBubble,
            outgoingMessage = colors.outgoingBubble,
            messageText = colors.textPrimary,
            messageSecondaryText = colors.textSecondary,
            unreadIndicator = colors.accent,
            selectedItem = (colors.primary and 0x00FFFFFF) or 0x24000000,
            link = colors.accent,
            divider = colors.divider
        )
    }

    /** Incoming and outgoing bubbles are semantic surfaces and must remain visually distinct. */
    private fun resolveIncomingBubble(
        configured: Int,
        outgoing: Int,
        surface: Int,
        background: Int,
    ): Int {
        if (colorDistance(configured, outgoing) >= MIN_BUBBLE_DISTANCE) return configured

        val candidates = listOf(
            mix(surface, background, 0.5f),
            background,
            surface,
            mix(surface, background, 0.75f),
            mix(surface, background, 0.25f),
            mix(outgoing, contrastColor(outgoing), 0.65f),
        )
        return candidates.firstOrNull { colorDistance(it, outgoing) >= MIN_BUBBLE_DISTANCE }
            ?: contrastColor(outgoing)
    }

    private const val MIN_BUBBLE_DISTANCE = 48.0

    private fun red(color: Int): Int = (color ushr 16) and 0xFF
    private fun green(color: Int): Int = (color ushr 8) and 0xFF
    private fun blue(color: Int): Int = color and 0xFF

    private fun rgb(red: Int, green: Int, blue: Int): Int =
        (0xFF shl 24) or
            ((red.coerceIn(0, 255)) shl 16) or
            ((green.coerceIn(0, 255)) shl 8) or
            blue.coerceIn(0, 255)

    private fun mix(first: Int, second: Int, secondWeight: Float): Int {
        val weight = secondWeight.coerceIn(0f, 1f)
        val inverse = 1f - weight
        return rgb(
            (red(first) * inverse + red(second) * weight).toInt(),
            (green(first) * inverse + green(second) * weight).toInt(),
            (blue(first) * inverse + blue(second) * weight).toInt(),
        )
    }

    private fun colorDistance(first: Int, second: Int): Double {
        val dr = red(first) - red(second)
        val dg = green(first) - green(second)
        val db = blue(first) - blue(second)
        return kotlin.math.sqrt((dr * dr + dg * dg + db * db).toDouble())
    }

    internal fun contrastColor(background: Int): Int {
        val red = (background ushr 16) and 0xFF
        val green = (background ushr 8) and 0xFF
        val blue = background and 0xFF
        val luminance = (0.299 * red + 0.587 * green + 0.114 * blue) / 255.0
        return if (luminance > 0.55) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
    }
}
