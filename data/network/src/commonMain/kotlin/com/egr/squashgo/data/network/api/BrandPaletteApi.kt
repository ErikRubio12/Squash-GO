package com.egr.squashgo.data.network.api

import com.egr.squashgo.data.network.dto.BrandPaletteRowDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

/**
 * Reads the active brand palette from Supabase.
 *
 * The `brand_palettes` table has a partial unique index so at most one row can have
 * `is_active = true` at any time. RLS exposes only that row for anonymous SELECT.
 *
 * Returning a list (instead of a single object) is the standard PostgREST contract:
 * the caller decides whether "no rows" is an error or a legitimate state. Here the
 * `SupabaseRemoteThemeSource` treats it as "no remote palette defined → fall back to
 * the local palette already in memory."
 */
class BrandPaletteApi(private val client: HttpClient) {

    suspend fun getActivePalette(): BrandPaletteRowDto? {
        val rows: List<BrandPaletteRowDto> = client.get("/rest/v1/brand_palettes") {
            parameter("select", "id,name,is_active,palette")
            parameter("is_active", "eq.true")
            parameter("limit", "1")
        }.body()
        return rows.firstOrNull()
    }
}
