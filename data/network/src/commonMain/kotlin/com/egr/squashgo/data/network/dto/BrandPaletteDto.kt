package com.egr.squashgo.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Top-level row returned by `GET /rest/v1/brand_palettes`.
 *
 * The `palette` column is `jsonb` in Postgres; kotlinx-serialization sees it as a nested
 * object that maps 1:1 to [BrandPaletteContentDto].
 */
@Serializable
data class BrandPaletteRowDto(
    val id: String,
    val name: String,
    @SerialName("is_active") val isActive: Boolean,
    val palette: BrandPaletteContentDto,
)

/**
 * Server-side brand palette content. Every nested field is nullable / optional so a
 * partial palette (e.g. only the primary colors) is still a valid response — the mapper
 * merges missing fields with a local fallback palette before the resolver applies them.
 */
@Serializable
data class BrandPaletteContentDto(
    val name: String? = null,
    val light: BrandColorSetDto? = null,
    val dark: BrandColorSetDto? = null,
)

/**
 * Every color is optional so the server can ship partial overrides without breaking
 * deserialization. Missing colors fall back to the local default palette per-field, so a
 * typo in one field (or an intentional partial override) never voids the rest of the
 * payload.
 */
@Serializable
data class BrandColorSetDto(
    val primary: String? = null,
    @SerialName("on_primary") val onPrimary: String? = null,
    @SerialName("primary_container") val primaryContainer: String? = null,
    @SerialName("on_primary_container") val onPrimaryContainer: String? = null,
    val secondary: String? = null,
    @SerialName("on_secondary") val onSecondary: String? = null,
    val background: String? = null,
    @SerialName("on_background") val onBackground: String? = null,
    val surface: String? = null,
    @SerialName("on_surface") val onSurface: String? = null,
    val error: String? = null,
    @SerialName("on_error") val onError: String? = null,
)
