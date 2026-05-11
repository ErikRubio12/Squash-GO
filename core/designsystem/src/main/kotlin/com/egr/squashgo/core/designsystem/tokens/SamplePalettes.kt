package com.egr.squashgo.core.designsystem.tokens

import androidx.compose.ui.graphics.Color

/**
 * Sample brand palettes used to demonstrate runtime theme replacement.
 *
 * In a real product, these would arrive from a server endpoint or be defined by a per-client
 * `:app` module. They live here as sample data so the design-system demo screen can show
 * the resolver mechanism without depending on a backend.
 */
object SamplePalettes {

    /** SquashGo Vancouver — the current default. */
    val SquashGoDefault: BrandPalette = BrandPalette(
        name = "SquashGo Vancouver",
        light = BrandColorSet(
            primary = Color(0xFF1B5E20),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFA5D6A7),
            onPrimaryContainer = Color(0xFF0A2E0F),
            secondary = Color(0xFF4CAF50),
            onSecondary = Color.White,
            background = Color(0xFFFAFAFA),
            onBackground = Color(0xFF1A1A1A),
            surface = Color.White,
            onSurface = Color(0xFF1A1A1A),
            error = Color(0xFFB00020),
            onError = Color.White,
        ),
        dark = BrandColorSet(
            primary = Color(0xFF81C784),
            onPrimary = Color(0xFF0A2E0F),
            primaryContainer = Color(0xFF1B5E20),
            onPrimaryContainer = Color(0xFFA5D6A7),
            secondary = Color(0xFF66BB6A),
            onSecondary = Color(0xFF0A2E0F),
            background = Color(0xFF121212),
            onBackground = Color(0xFFE0E0E0),
            surface = Color(0xFF1E1E1E),
            onSurface = Color(0xFFE0E0E0),
            error = Color(0xFFCF6679),
            onError = Color.Black,
        ),
    )

    /** Hypothetical Toronto client — blue-leaning brand. */
    val TorontoClient: BrandPalette = BrandPalette(
        name = "Toronto Squash Club",
        light = BrandColorSet(
            primary = Color(0xFF0D47A1),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFBBDEFB),
            onPrimaryContainer = Color(0xFF002171),
            secondary = Color(0xFF1976D2),
            onSecondary = Color.White,
            background = Color(0xFFF5F7FA),
            onBackground = Color(0xFF14202E),
            surface = Color.White,
            onSurface = Color(0xFF14202E),
            error = Color(0xFFD32F2F),
            onError = Color.White,
        ),
        dark = BrandColorSet(
            primary = Color(0xFF82B1FF),
            onPrimary = Color(0xFF002171),
            primaryContainer = Color(0xFF0D47A1),
            onPrimaryContainer = Color(0xFFBBDEFB),
            secondary = Color(0xFF64B5F6),
            onSecondary = Color(0xFF002171),
            background = Color(0xFF0F1620),
            onBackground = Color(0xFFE0E6EE),
            surface = Color(0xFF18202C),
            onSurface = Color(0xFFE0E6EE),
            error = Color(0xFFFF8A80),
            onError = Color.Black,
        ),
    )

    /** Hypothetical Bronze tournament event — warm amber palette. */
    val BronzeEvent: BrandPalette = BrandPalette(
        name = "Bronze Tournament",
        light = BrandColorSet(
            primary = Color(0xFF8D5524),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFFFD8B4),
            onPrimaryContainer = Color(0xFF3E1F00),
            secondary = Color(0xFFCD853F),
            onSecondary = Color.White,
            background = Color(0xFFFFF8F0),
            onBackground = Color(0xFF2A1A0A),
            surface = Color(0xFFFFFCF7),
            onSurface = Color(0xFF2A1A0A),
            error = Color(0xFFB00020),
            onError = Color.White,
        ),
        dark = BrandColorSet(
            primary = Color(0xFFE6B07A),
            onPrimary = Color(0xFF3E1F00),
            primaryContainer = Color(0xFF8D5524),
            onPrimaryContainer = Color(0xFFFFD8B4),
            secondary = Color(0xFFD2A06A),
            onSecondary = Color(0xFF3E1F00),
            background = Color(0xFF1A120A),
            onBackground = Color(0xFFE8DCC8),
            surface = Color(0xFF231811),
            onSurface = Color(0xFFE8DCC8),
            error = Color(0xFFCF6679),
            onError = Color.Black,
        ),
    )

    /** All sample palettes, exposed for the demo screen to iterate. */
    val all: List<BrandPalette> = listOf(SquashGoDefault, TorontoClient, BronzeEvent)
}
