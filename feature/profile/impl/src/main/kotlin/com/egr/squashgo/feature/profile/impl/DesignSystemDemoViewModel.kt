package com.egr.squashgo.feature.profile.impl

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.egr.squashgo.core.designsystem.sync.RemoteThemeSource
import com.egr.squashgo.core.designsystem.theme.ThemeController
import com.egr.squashgo.core.designsystem.theme.ThemeResolver
import com.egr.squashgo.core.designsystem.tokens.BrandPalette
import com.egr.squashgo.core.designsystem.tokens.SamplePalettes
import com.egr.squashgo.core.designsystem.tokens.ThemeTokens
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the Design System demo screen.
 *
 * Read access via [ThemeResolver] (current brand name); write access via [ThemeController]
 * (palette swap); async fetch via [RemoteThemeSource] (simulated remote source today, real
 * HTTP / Supabase impl tomorrow). Hilt provides the same resolver singleton under both
 * interfaces — see `app/di/DesignSystemModule.kt`.
 */
@HiltViewModel
class DesignSystemDemoViewModel @Inject constructor(
    themeResolver: ThemeResolver,
    private val themeController: ThemeController,
    private val remoteThemeSource: RemoteThemeSource,
) : ViewModel() {

    val tokens: State<ThemeTokens> = themeResolver.tokens

    val palettes: List<BrandPalette> = SamplePalettes.all

    private val _isSyncing = mutableStateOf(false)
    val isSyncing: State<Boolean> = _isSyncing

    fun applyPalette(palette: BrandPalette) {
        themeController.applyPalette(palette)
    }

    fun reset() {
        themeController.reset()
    }

    /**
     * Simulates a server-driven palette update: fetches the next active palette from the
     * remote source (with latency) and applies it. The same code path will drive a real
     * HTTP / Supabase fetch when [RemoteThemeSource] is rebound in DI.
     */
    fun fetchFromRemote() {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val palette = remoteThemeSource.fetchActivePalette()
                themeController.applyPalette(palette)
            } finally {
                _isSyncing.value = false
            }
        }
    }
}
