package com.egr.squashgo.core.designsystem.theme

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.egr.squashgo.core.designsystem.tokens.BrandPalette
import com.egr.squashgo.core.designsystem.tokens.SamplePalettes
import com.egr.squashgo.core.designsystem.tokens.ThemeTokens

/**
 * [ThemeResolver] implementation that simulates a server-driven theme source.
 *
 * In a real product, an equivalent implementation would:
 *  1. Subscribe to a remote endpoint (REST poll, websocket, push) returning [BrandPalette]s.
 *  2. Cache the latest tokens locally for offline / first-launch.
 *  3. Emit a new value on the [tokens] state; Compose recomposes automatically.
 *
 * This stub keeps the surface identical but mutates the state in-process via
 * [applyPalette]. The demo screen drives that mutation from buttons.
 *
 * Single class implements both [ThemeResolver] (read) and [ThemeController] (write). DI
 * exposes the same instance under both interfaces; consumers ask for the role they need.
 */
class RemoteStubThemeResolver(
    initialPalette: BrandPalette = SamplePalettes.SquashGoDefault,
) : ThemeResolver, ThemeController {

    private val defaultTokens = ThemeTokens.fromBrandPalette(initialPalette)
    private val _tokens = mutableStateOf(defaultTokens)

    override val tokens: State<ThemeTokens> = _tokens

    override fun applyPalette(palette: BrandPalette) {
        _tokens.value = ThemeTokens.fromBrandPalette(palette)
    }

    override fun reset() {
        _tokens.value = defaultTokens
    }
}
