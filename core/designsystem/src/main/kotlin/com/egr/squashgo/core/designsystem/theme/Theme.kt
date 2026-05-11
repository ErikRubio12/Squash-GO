package com.egr.squashgo.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue

/**
 * Root theme wrapper for the SquashGo app.
 *
 * Reads tokens from the active [ThemeResolver] (provided via [LocalThemeResolverComposition])
 * and maps them to a Material 3 [androidx.compose.material3.ColorScheme]. Features and
 * widgets never see the resolver — they only see `MaterialTheme.colorScheme.*`.
 *
 * Source of tokens (local defaults, remote stub, real network resolver) is decided by the
 * `:app` module's composition root via a Hilt-provided implementation.
 */
@Composable
fun SquashGoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val resolver = LocalThemeResolverComposition.current
    val tokens by resolver.tokens
    MaterialTheme(
        colorScheme = if (darkTheme) tokens.dark else tokens.light,
        content = content,
    )
}
