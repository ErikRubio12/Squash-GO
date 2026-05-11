package com.egr.squashgo.core.designsystem.tokens

import androidx.compose.ui.graphics.Color

/**
 * Brand-level color input — what a white-label client provides.
 *
 * Concept: replaces per-component remote keys (e.g. `session_filters_button_confirm_color`)
 * with a single typed contract that the design system knows how to interpret.
 *
 * A client never tells the app what color a specific button should be. A client tells the
 * app what its brand looks like. The design system decides where that brand shows up.
 */
data class BrandPalette(
    val name: String,
    val light: BrandColorSet,
    val dark: BrandColorSet,
)

data class BrandColorSet(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val onSecondary: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val error: Color,
    val onError: Color,
)
