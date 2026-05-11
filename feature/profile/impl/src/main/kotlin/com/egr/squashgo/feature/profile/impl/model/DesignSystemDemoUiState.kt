package com.egr.squashgo.feature.profile.impl.model

/**
 * One-shot result of a `fetchFromRemote()` attempt on the Design System demo screen.
 * The screen consumes it via [androidx.compose.runtime.LaunchedEffect] to display a
 * Snackbar, then asks the ViewModel to clear it.
 */
sealed interface DesignSystemFetchResult {
    data class Success(val brandName: String) : DesignSystemFetchResult
    data object NoActivePalette : DesignSystemFetchResult
    data class Error(val reason: String?) : DesignSystemFetchResult
}
