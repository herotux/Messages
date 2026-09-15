package org.fossify.commons.compose.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import org.fossify.commons.compose.theme.HomaDesignTokens

@Composable
fun HomaButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) =
    Button(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(HomaDesignTokens.Shape.medium)) { Text(text) }

@Composable
fun HomaCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) =
    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(HomaDesignTokens.Shape.large), elevation = CardDefaults.cardElevation()) {
        Column(Modifier.padding(HomaDesignTokens.Spacing.md)) { content() }
    }

@Composable
fun HomaListItem(headline: String, modifier: Modifier = Modifier, supporting: String? = null, leading: (@Composable () -> Unit)? = null, trailing: (@Composable () -> Unit)? = null) {
    ListItem(
        modifier = modifier,
        headlineContent = { Text(headline) },
        supportingContent = supporting?.let { { Text(it) } },
        leadingContent = leading,
        trailingContent = trailing,
    )
}

@Composable
fun HomaTextField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, singleLine: Boolean = true) =
    OutlinedTextField(value = value, onValueChange = onValueChange, label = { Text(label) }, modifier = modifier.fillMaxWidth(), singleLine = singleLine, shape = RoundedCornerShape(HomaDesignTokens.Shape.medium))

@Composable
fun HomaChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) =
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) }, modifier = modifier)

@Composable
fun HomaEmptyState(title: String, modifier: Modifier = Modifier, message: String? = null) {
    Column(modifier.fillMaxWidth().padding(HomaDesignTokens.Spacing.xl), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(HomaDesignTokens.Spacing.sm)) {
        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(40.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        message?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
    }
}

@Composable
fun HomaLoading(modifier: Modifier = Modifier) = Row(modifier.fillMaxWidth().padding(HomaDesignTokens.Spacing.xl), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator() }
