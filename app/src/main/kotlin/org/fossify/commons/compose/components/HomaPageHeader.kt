package org.fossify.commons.compose.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.fossify.commons.compose.theme.HomaDesignTokens

/**
 * Shared edge-to-edge page header.
 *
 * The surface itself can draw behind the status bar while its content receives
 * the status-bar inset through [statusBarsPadding].
 */
@Composable
fun HomaPageHeader(
    title: String,
    modifier: Modifier = Modifier,
    navigation: @Composable RowScope.() -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = HomaDesignTokens.Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            navigation()
            Text(
                text = title,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = HomaDesignTokens.Spacing.sm),
                style = MaterialTheme.typography.titleLarge,
            )
            actions()
        }
    }
}
