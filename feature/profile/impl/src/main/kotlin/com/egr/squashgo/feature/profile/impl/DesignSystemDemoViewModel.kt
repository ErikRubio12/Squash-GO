package com.egr.squashgo.feature.profile.impl

import android.util.Log
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
import com.egr.squashgo.feature.profile.impl.model.DesignSystemFetchResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "DesignSystemDemoVM"

/**
 * Drives the Design System demo screen.
 *
 * Read access via [ThemeResolver] (current brand name); write access via [ThemeController]
 * (palette swap); async fetch via [RemoteThemeSource] (Supabase in production).
 *
 * Fallback contract: if the remote source returns `null` (no active row in Supabase) the
 * current palette stays in place and the screen surfaces a Snackbar. If the call throws
 * (network down, malformed JSON) the same fallback path applies.
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

    private val _lastFetch = mutableStateOf<DesignSystemFetchResult?>(null)
    val lastFetch: State<DesignSystemFetchResult?> = _lastFetch

    fun applyPalette(palette: BrandPalette) {
        themeController.applyPalette(palette)
    }

    fun reset() {
        themeController.reset()
    }

    /**
     * Fetches the active palette from the [RemoteThemeSource] and applies it. On no-row /
     * exception, the current palette stays in place and a result is emitted for the UI to
     * display.
     */
    fun fetchFromRemote() {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            _lastFetch.value = try {
                val palette = remoteThemeSource.fetchActivePalette()
                if (palette != null) {
                    themeController.applyPalette(palette)
                    DesignSystemFetchResult.Success(palette.name)
                } else {
                    DesignSystemFetchResult.NoActivePalette
                }
            } catch (cancel: kotlinx.coroutines.CancellationException) {
                throw cancel
            } catch (t: Throwable) {
                Log.w(TAG, "fetchFromRemote failed", t)
                DesignSystemFetchResult.Error(t.javaClass.simpleName)
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun consumeLastFetch() {
        _lastFetch.value = null
    }
}
