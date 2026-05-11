package com.egr.squashgo.core.designsystem.sync

import com.egr.squashgo.core.designsystem.tokens.BrandPalette

/**
 * Contract for an asynchronous source of brand palettes.
 *
 * This is the boundary between the design system (which knows how to *apply* tokens) and
 * whatever knows how to *fetch* them. Implementations could be:
 *  - a local stub (today) for demos and tests
 *  - an HTTP client hitting a Supabase `brand_palettes` endpoint (next step)
 *  - a websocket / Supabase Realtime subscription pushing updates
 *  - a CMS-backed service
 *
 * Replacing this interface's binding in `DesignSystemModule.kt` is the only change
 * needed to swap a local stub for a real network-backed source. Nothing else in the app
 * depends on *how* the palette is delivered.
 */
interface RemoteThemeSource {
    suspend fun fetchActivePalette(): BrandPalette
}
