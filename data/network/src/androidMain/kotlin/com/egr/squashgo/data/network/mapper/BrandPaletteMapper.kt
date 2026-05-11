package com.egr.squashgo.data.network.mapper

import androidx.compose.ui.graphics.Color
import com.egr.squashgo.core.designsystem.tokens.BrandColorSet
import com.egr.squashgo.core.designsystem.tokens.BrandPalette
import com.egr.squashgo.data.network.dto.BrandColorSetDto
import com.egr.squashgo.data.network.dto.BrandPaletteContentDto
import com.egr.squashgo.data.network.dto.BrandPaletteRowDto

/**
 * Converts a [BrandPaletteRowDto] (server-side hex strings, every field optional) into a
 * [BrandPalette] (Compose [Color] instances, every field required).
 *
 * **Field-level fallback contract:**
 * - If a color is missing from the JSON, use the corresponding color from `fallback`.
 * - If a color is present but malformed (e.g. `"not-a-color"`), also use `fallback`.
 * - If `light` or `dark` is missing entirely, use the entire corresponding set from `fallback`.
 * - If `name` is missing, use `fallback`'s name.
 *
 * Net effect: any subset of valid fields in Supabase is enough to render. The remote can
 * ship a one-field override ("just change the primary color"); the rest of the palette
 * is filled with the app's built-in default.
 *
 * Lives in `androidMain` because [Color] is a Compose type and only exists on Android.
 */
object BrandPaletteMapper {

    fun toDomain(row: BrandPaletteRowDto, fallback: BrandPalette): BrandPalette =
        toDomain(row.palette, fallback)

    fun toDomain(content: BrandPaletteContentDto, fallback: BrandPalette): BrandPalette =
        BrandPalette(
            name = content.name ?: fallback.name,
            light = content.light.toDomain(fallback.light),
            dark = content.dark.toDomain(fallback.dark),
        )

    private fun BrandColorSetDto?.toDomain(fallback: BrandColorSet): BrandColorSet {
        if (this == null) return fallback
        return BrandColorSet(
            primary = primary.parseOr(fallback.primary),
            onPrimary = onPrimary.parseOr(fallback.onPrimary),
            primaryContainer = primaryContainer.parseOr(fallback.primaryContainer),
            onPrimaryContainer = onPrimaryContainer.parseOr(fallback.onPrimaryContainer),
            secondary = secondary.parseOr(fallback.secondary),
            onSecondary = onSecondary.parseOr(fallback.onSecondary),
            background = background.parseOr(fallback.background),
            onBackground = onBackground.parseOr(fallback.onBackground),
            surface = surface.parseOr(fallback.surface),
            onSurface = onSurface.parseOr(fallback.onSurface),
            error = error.parseOr(fallback.error),
            onError = onError.parseOr(fallback.onError),
        )
    }

    /**
     * Parses a hex color string (`#RRGGBB` or `#AARRGGBB`, with or without `#`) into a
     * Compose [Color]. Returns [fallback] for null or malformed input — caller never has
     * to handle exceptions from a single bad field.
     */
    private fun String?.parseOr(fallback: Color): Color {
        if (this == null) return fallback
        val cleaned = trimStart('#')
        if (cleaned.length != 6 && cleaned.length != 8) return fallback
        val withAlpha = if (cleaned.length == 6) "FF$cleaned" else cleaned
        return try {
            Color(withAlpha.toLong(radix = 16).toInt())
        } catch (_: NumberFormatException) {
            fallback
        }
    }
}
