package org.fossify.commons.compose.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.fossify.commons.compose.theme.HomaDesignTokens

/** Shared Homa page heading without a toolbar strip or elevation. */
@Composable
fun HomaPageHeader(
    title: String,
    modifier: Modifier = Modifier,
    navigation: @Composable RowScope.() -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = HomaDesignTokens.Spacing.md, end = HomaDesignTokens.Spacing.md)
            .padding(top = 14.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        navigation()
        Text(
            text = title,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = HomaDesignTokens.Spacing.sm),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        actions()
    }
}
