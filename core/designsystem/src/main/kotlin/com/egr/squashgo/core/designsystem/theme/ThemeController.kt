package com.egr.squashgo.core.designsystem.theme

import com.egr.squashgo.core.designsystem.tokens.BrandPalette

/**
 * Mutation contract for theme tokens.
 *
 * Separated from [ThemeResolver] so consumers that only read (like [SquashGoTheme] and
 * widgets) cannot accidentally trigger a theme switch. Only the demo screen and any future
 * sync engine should depend on this contract.
 */
interface ThemeController {
    fun applyPalette(palette: BrandPalette)
    fun reset()
}
