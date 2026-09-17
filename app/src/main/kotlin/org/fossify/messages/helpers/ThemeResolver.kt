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

    /**
     * Incoming and outgoing bubbles are semantic surfaces, not just two nearby
     * shades of the same theme color. If a custom theme makes them too similar,
     * move the incoming bubble toward the neutral surface/background pair.
     */
    private fun resolveIncomingBubble(
        configured: Int,
        outgoing: Int,
        surface: Int,
        background: Int,
    ): Int {
        if (colorDistance(configured, outgoing) >= 48 && contrastRatio(configured, outgoing) >= 1.22) {
            return configured
        }
        val candidate = mix(surface, background, 0.5f)
        return if (colorDistance(candidate, outgoing) >= 48 && contrastRatio(candidate, outgoing) >= 1.22) {
            candidate
        } else {
            mix(surface, background, 0.25f)
        }
    }

    private fun mix(first: Int, second: Int, secondWeight: Float): Int {
        val weight = secondWeight.coerceIn(0f, 1f)
        val inverse = 1f - weight
        val red = (android.graphics.Color.red(first) * inverse + android.graphics.Color.red(second) * weight).toInt()
        val green = (android.graphics.Color.green(first) * inverse + android.graphics.Color.green(second) * weight).toInt()
        val blue = (android.graphics.Color.blue(first) * inverse + android.graphics.Color.blue(second) * weight).toInt()
        return android.graphics.Color.rgb(red, green, blue)
    }

    private fun colorDistance(first: Int, second: Int): Double {
        val dr = android.graphics.Color.red(first) - android.graphics.Color.red(second)
        val dg = android.graphics.Color.green(first) - android.graphics.Color.green(second)
        val db = android.graphics.Color.blue(first) - android.graphics.Color.blue(second)
        return kotlin.math.sqrt((dr * dr + dg * dg + db * db).toDouble())
    }

    private fun contrastRatio(first: Int, second: Int): Double {
        val l1 = relativeLuminance(first)
        val l2 = relativeLuminance(second)
        return (maxOf(l1, l2) + 0.05) / (minOf(l1, l2) + 0.05)
    }

    private fun relativeLuminance(color: Int): Double {
        fun channel(value: Int): Double {
            val normalized = value / 255.0
            return if (normalized <= 0.03928) normalized / 12.92 else ((normalized + 0.055) / 1.055).let { it * it * it }
        }
        return 0.2126 * channel(android.graphics.Color.red(color)) +
            0.7152 * channel(android.graphics.Color.green(color)) +
            0.0722 * channel(android.graphics.Color.blue(color))
    }

    internal fun contrastColor(background: Int): Int {
        val red = (background shr 16) and 0xFF
        val green = (background shr 8) and 0xFF
        val blue = background and 0xFF
        val luminance = (0.299 * red + 0.587 * green + 0.114 * blue) / 255.0
        return if (luminance > 0.55) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
    }
}
