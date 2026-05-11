package com.egr.squashgo.core.designsystem.theme

import androidx.compose.runtime.compositionLocalOf

/**
 * Compose entry point for the active [ThemeResolver].
 *
 * Default value is a [LocalThemeResolver] so any Composable can be previewed / tested in
 * isolation without DI plumbing. The `:app` module overrides this at the root via
 * `CompositionLocalProvider(LocalThemeResolverComposition provides hiltProvidedResolver)`
 * so the production app uses the Hilt-managed singleton (a [RemoteStubThemeResolver]
 * today, a real network-backed resolver in the future).
 */
val LocalThemeResolverComposition = compositionLocalOf<ThemeResolver> { LocalThemeResolver() }
