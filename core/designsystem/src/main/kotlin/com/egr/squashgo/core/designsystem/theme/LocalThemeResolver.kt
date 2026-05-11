package com.egr.squashgo.core.designsystem.theme

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.egr.squashgo.core.designsystem.tokens.SamplePalettes
import com.egr.squashgo.core.designsystem.tokens.ThemeTokens

/**
 * Default [ThemeResolver] that returns a fixed local palette. Used as the fallback in the
 * [LocalThemeResolverComposition] when `:app` has not provided a different implementation
 * (for example: Compose previews, unit tests, library standalone builds).
 */
class LocalThemeResolver : ThemeResolver {
    private val _tokens = mutableStateOf(
        ThemeTokens.fromBrandPalette(SamplePalettes.SquashGoDefault),
    )
    override val tokens: State<ThemeTokens> = _tokens
}
