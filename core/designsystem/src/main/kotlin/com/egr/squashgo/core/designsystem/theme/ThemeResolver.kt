package com.egr.squashgo.core.designsystem.theme

import androidx.compose.runtime.State
import com.egr.squashgo.core.designsystem.tokens.ThemeTokens

/**
 * Read contract for the resolved theme tokens consumed by [SquashGoTheme].
 *
 * This is the boundary between **how tokens are produced** (local default, remote server,
 * test override, client-specific bootstrap) and **how tokens are consumed** (the design
 * system + features). Consumers must only depend on this interface.
 *
 * State is exposed as Compose [State] so the theme recomposes automatically when tokens
 * change without any explicit lifecycle plumbing.
 */
interface ThemeResolver {
    val tokens: State<ThemeTokens>
}
