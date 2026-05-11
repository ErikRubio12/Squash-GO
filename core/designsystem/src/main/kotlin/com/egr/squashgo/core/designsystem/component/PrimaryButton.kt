package com.egr.squashgo.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Primary call-to-action button. Reads color from `MaterialTheme.colorScheme.primary`.
 *
 * Consumers pass only behavior and content — never colors. If the brand palette changes
 * at runtime via [com.egr.squashgo.core.designsystem.theme.ThemeController], every
 * `PrimaryButton` in the tree recomposes with the new color automatically.
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fillWidth: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = if (fillWidth) modifier.fillMaxWidth() else modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        Text(text = text)
    }
}
