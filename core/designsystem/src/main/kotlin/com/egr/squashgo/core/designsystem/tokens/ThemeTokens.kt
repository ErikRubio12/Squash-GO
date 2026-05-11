package com.egr.squashgo.core.designsystem.tokens

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme

/**
 * The internal, design-system-resolved theme contract.
 *
 * Features and widgets consume `MaterialTheme.colorScheme.*`. They do not see [BrandPalette]
 * or any remote keys. This indirection is the whole point of the proposal: features depend
 * on a stable, typed M3 contract; the design system decides how to fill it.
 *
 * The `fromBrandPalette` mapper is where future logic could go to:
 *  - derive container/variant slots from a smaller brand input
 *  - apply contrast adjustments
 *  - merge defaults with client overrides
 */
data class ThemeTokens(
    val brandName: String,
    val light: ColorScheme,
    val dark: ColorScheme,
) {
    companion object {
        fun fromBrandPalette(palette: BrandPalette): ThemeTokens = ThemeTokens(
            brandName = palette.name,
            light = palette.light.toLightColorScheme(),
            dark = palette.dark.toDarkColorScheme(),
        )
    }
}

private fun BrandColorSet.toLightColorScheme(): ColorScheme = lightColorScheme(
    primary = primary,
    onPrimary = onPrimary,
    primaryContainer = primaryContainer,
    onPrimaryContainer = onPrimaryContainer,
    secondary = secondary,
    onSecondary = onSecondary,
    background = background,
    onBackground = onBackground,
    surface = surface,
    onSurface = onSurface,
    error = error,
    onError = onError,
)

private fun BrandColorSet.toDarkColorScheme(): ColorScheme = darkColorScheme(
    primary = primary,
    onPrimary = onPrimary,
    primaryContainer = primaryContainer,
    onPrimaryContainer = onPrimaryContainer,
    secondary = secondary,
    onSecondary = onSecondary,
    background = background,
    onBackground = onBackground,
    surface = surface,
    onSurface = onSurface,
    error = error,
    onError = onError,
)
