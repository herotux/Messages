package org.fossify.commons.compose.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Homa UI foundation tokens.
 *
 * These tokens define how the UI is constructed. Active colors and visual
 * appearance still come from the application's ThemeManager/Material theme.
 */
object HomaDesignTokens {
    object Spacing {
        val xxs: Dp = 4.dp
        val xs: Dp = 8.dp
        val sm: Dp = 12.dp
        val md: Dp = 16.dp
        val lg: Dp = 20.dp
        val xl: Dp = 24.dp
        val xxl: Dp = 32.dp
        val xxxl: Dp = 40.dp
    }

    object Shape {
        val small: Dp = 8.dp
        val medium: Dp = 12.dp
        val large: Dp = 16.dp
        val extraLarge: Dp = 24.dp
        val pill: Dp = 999.dp
    }

    object Component {
        val minTouchTarget: Dp = 48.dp
        val topBarHeight: Dp = 64.dp
        val listItemMinHeight: Dp = 56.dp
        val cardPadding: Dp = 16.dp
        val sectionSpacing: Dp = 24.dp
    }
}
