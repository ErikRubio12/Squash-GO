package com.egr.squashgo.core.designsystem.sync

import com.egr.squashgo.core.designsystem.tokens.BrandPalette

/**
 * Contract for an asynchronous source of brand palettes.
 *
 * This is the boundary between the design system (which knows how to *apply* tokens) and
 * whatever knows how to *fetch* them. Implementations include:
 *  - [com.egr.squashgo.data.network.repository.SupabaseRemoteThemeSource] (production)
 *  - `SimulatedRemoteThemeSource` in `:app/data/` (fixture / dev mode)
 *  - in the future: a websocket / Supabase Realtime subscription, a CMS, etc.
 *
 * Replacing this interface's binding in `DesignSystemModule` is the only change needed
 * to swap one source for another. Nothing else in the app depends on *how* the palette
 * is delivered.
 *
 * @return the currently active palette, or `null` if the source has no palette configured
 *   (caller falls back to the palette already in memory).
 * @throws Throwable if the source is unreachable or the response is malformed.
 */
interface RemoteThemeSource {
    suspend fun fetchActivePalette(): BrandPalette?
}
