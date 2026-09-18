package org.fossify.commons.compose.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.fossify.commons.compose.theme.HomaDesignTokens

/** Shared bank-card structure; bank branding is supplied by the caller. */
@Composable
fun HomaBankCard(
    bankName: String,
    cardNumber: String,
    ownerName: String? = null,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    brandContent: (@Composable () -> Unit)? = null,
    actions: (@Composable () -> Unit)? = null,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(HomaDesignTokens.Shape.extraLarge),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(Modifier.padding(HomaDesignTokens.Spacing.md)) {
            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text(bankName, style = MaterialTheme.typography.titleMedium)
                    ownerName?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                }
                brandContent?.invoke()
            }
            Text(
                text = cardNumber,
                modifier = Modifier.padding(top = HomaDesignTokens.Spacing.lg),
                style = MaterialTheme.typography.titleLarge,
            )
            if (selected || actions != null) {
                Row(Modifier.fillMaxWidth().padding(top = HomaDesignTokens.Spacing.sm)) { actions?.invoke() }
            }
        }
    }
}
