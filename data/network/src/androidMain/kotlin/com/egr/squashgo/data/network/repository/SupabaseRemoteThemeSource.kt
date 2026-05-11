package com.egr.squashgo.data.network.repository

import com.egr.squashgo.core.designsystem.sync.RemoteThemeSource
import com.egr.squashgo.core.designsystem.tokens.BrandPalette
import com.egr.squashgo.core.designsystem.tokens.SamplePalettes
import com.egr.squashgo.data.network.api.BrandPaletteApi
import com.egr.squashgo.data.network.mapper.BrandPaletteMapper

/**
 * Production [RemoteThemeSource] backed by Supabase.
 *
 * Returns the currently active [BrandPalette] from the `brand_palettes` table, or `null`
 * if no row has `is_active = true` (the caller treats this as a legitimate fallback
 * state, not an error).
 *
 * Partial JSON payloads are tolerated: any field missing or malformed in the server
 * response is filled from [defaultFallback] field-by-field, so a one-field override on
 * the server still produces a usable palette on the device. Only network failures and
 * top-level JSON-shape errors propagate as exceptions.
 */
class SupabaseRemoteThemeSource(
    private val brandPaletteApi: BrandPaletteApi,
    private val defaultFallback: BrandPalette = SamplePalettes.SquashGoDefault,
) : RemoteThemeSource {

    override suspend fun fetchActivePalette(): BrandPalette? {
        val row = brandPaletteApi.getActivePalette() ?: return null
        return BrandPaletteMapper.toDomain(row, fallback = defaultFallback)
    }
}
