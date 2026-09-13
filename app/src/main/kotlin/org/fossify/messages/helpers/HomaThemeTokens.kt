package org.fossify.messages.helpers

/**
 * Semantic, app-owned design tokens derived from the active visual theme.
 *
 * These tokens describe meaning rather than individual widgets, while retaining
 * the existing theme palette exactly during the migration to the central applier.
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
    val toolbar: Int,
    val fab: Int,
    val incomingMessage: Int,
    val outgoingMessage: Int,
    val messageText: Int,
    val messageSecondaryText: Int,
    val unreadIndicator: Int,
    val selectedItem: Int,
    val link: Int,
    val divider: Int
)
