package com.egr.squashgo.feature.profile.impl

import androidx.compose.runtime.State
import androidx.lifecycle.ViewModel
import com.egr.squashgo.core.designsystem.theme.ThemeController
import com.egr.squashgo.core.designsystem.theme.ThemeResolver
import com.egr.squashgo.core.designsystem.tokens.BrandPalette
import com.egr.squashgo.core.designsystem.tokens.SamplePalettes
import com.egr.squashgo.core.designsystem.tokens.ThemeTokens
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Drives the Design System demo screen.
 *
 * Read access via [ThemeResolver] (for displaying the current brand name); write access
 * via [ThemeController] (to swap palettes from the buttons). Hilt provides the same
 * underlying singleton under both interfaces — see `app/di/DesignSystemModule.kt`.
 */
@HiltViewModel
class DesignSystemDemoViewModel @Inject constructor(
    themeResolver: ThemeResolver,
    private val themeController: ThemeController,
) : ViewModel() {

    val tokens: State<ThemeTokens> = themeResolver.tokens

    val palettes: List<BrandPalette> = SamplePalettes.all

    fun applyPalette(palette: BrandPalette) {
        themeController.applyPalette(palette)
    }

    fun reset() {
        themeController.reset()
    }
}
