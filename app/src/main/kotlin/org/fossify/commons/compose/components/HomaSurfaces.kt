package org.fossify.commons.compose.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.fossify.commons.compose.theme.HomaDesignTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomaBottomSheet(onDismissRequest: () -> Unit, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismissRequest, modifier = modifier, windowInsets = WindowInsets.navigationBars, dragHandle = { BottomSheetDefaults.DragHandle() }, shape = RoundedCornerShape(topStart = HomaDesignTokens.Shape.extraLarge, topEnd = HomaDesignTokens.Shape.extraLarge), containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(HomaDesignTokens.Spacing.md), content = content)
    }
}

@Composable
fun HomaDialog(onDismissRequest: () -> Unit, title: String, text: String? = null, confirm: @Composable () -> Unit, dismiss: (@Composable () -> Unit)? = null) {
    AlertDialog(onDismissRequest = onDismissRequest, title = { Text(title) }, text = text?.let { { Text(it) } }, confirmButton = confirm, dismissButton = dismiss, shape = RoundedCornerShape(HomaDesignTokens.Shape.extraLarge))
}

@Composable
fun HomaSurface(modifier: Modifier = Modifier, content: @Composable () -> Unit) = Surface(modifier = modifier, color = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface, content = content)
