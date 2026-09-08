package org.fossify.messages.helpers

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/** Pure validation helpers for theme readability. */
object ThemeValidator {
    private const val WHITE = 0xFFFFFFFF.toInt()
    private const val BLACK = 0xFF000000.toInt()

    data class ContrastIssue(
        val name: String,
        val foreground: Int,
        val background: Int,
        val ratio: Double,
        val requiredRatio: Double = 4.5
    )

    data class Report(val issues: List<ContrastIssue>) {
        val isValid: Boolean get() = issues.isEmpty()
    }

    fun validate(colors: ThemeManager.ThemeColors, minimumRatio: Double = 4.5): Report {
        val checks = listOf(
            Triple("Primary text / background", colors.textPrimary, colors.background),
            Triple("Secondary text / background", colors.textSecondary, colors.background),
            Triple("Incoming text / bubble", colors.textPrimary, colors.incomingBubble),
            Triple("Outgoing text / bubble", bestTextColor(colors.outgoingBubble), colors.outgoingBubble),
        )
        return Report(checks.mapNotNull { (name, foreground, background) ->
            val ratio = contrastRatio(foreground, background)
            if (ratio + 1e-9 < minimumRatio) ContrastIssue(name, foreground, background, ratio, minimumRatio) else null
        })
    }

    fun bestTextColor(background: Int): Int =
        if (contrastRatio(WHITE, background) >= contrastRatio(BLACK, background)) WHITE else BLACK

    fun contrastRatio(foreground: Int, background: Int): Double {
        val a = relativeLuminance(foreground)
        val b = relativeLuminance(background)
        return (max(a, b) + 0.05) / (min(a, b) + 0.05)
    }

    private fun relativeLuminance(color: Int): Double {
        fun channel(value: Int): Double {
            val s = value / 255.0
            return if (s <= 0.03928) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(red(color)) +
            0.7152 * channel(green(color)) +
            0.0722 * channel(blue(color))
    }

    private fun red(color: Int) = color ushr 16 and 0xFF
    private fun green(color: Int) = color ushr 8 and 0xFF
    private fun blue(color: Int) = color and 0xFF
}
