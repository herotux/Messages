package org.fossify.messages.helpers

/**
 * Semantic, app-owned design tokens derived from the active visual theme.
 *
 * These tokens intentionally describe meaning rather than individual widgets,
 * so future screens and features can depend on stable Homa semantics instead
 * of reaching into ThemeManager's storage model.
 */
data class HomaThemeTokens(
    val primary: Int,
    val onPrimary: Int,
    val secondary: Int,
    val onSecondary: Int,
    val background: Int,
    val onBackground: Int,
    val surface: Int,
    val onSurface: Int,
    val surfaceVariant: Int,
    val onSurfaceVariant: Int,
    val outline: Int,
    val error: Int,
    val incomingMessage: Int,
    val outgoingMessage: Int,
    val messageText: Int,
    val messageSecondaryText: Int,
    val unreadIndicator: Int,
    val selectedItem: Int,
    val link: Int,
    val divider: Int
)
