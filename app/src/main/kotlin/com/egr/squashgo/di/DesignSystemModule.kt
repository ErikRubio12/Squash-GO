package com.egr.squashgo.di

import com.egr.squashgo.core.designsystem.sync.RemoteThemeSource
import com.egr.squashgo.core.designsystem.theme.RemoteStubThemeResolver
import com.egr.squashgo.core.designsystem.theme.ThemeController
import com.egr.squashgo.core.designsystem.theme.ThemeResolver
import com.egr.squashgo.data.network.api.BrandPaletteApi
import com.egr.squashgo.data.network.repository.SupabaseRemoteThemeSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Composition root for the design-system runtime.
 *
 * Two singletons are wired here:
 *
 *  - [RemoteStubThemeResolver] — exposed under both [ThemeResolver] (read) and
 *    [ThemeController] (mutation) so consumers depend only on the role they need. This is
 *    the in-memory state holder that drives all `MaterialTheme.colorScheme.*` reads.
 *
 *  - [RemoteThemeSource] — production binding is [SupabaseRemoteThemeSource], which hits
 *    the `brand_palettes` table in Supabase. To run without a backend (e.g. for E2E
 *    tests, dev mode, or demos before the migration has been applied), swap the provider
 *    below for `SimulatedRemoteThemeSource` — that class lives in `app/data/` exactly so
 *    a per-client `:app` can choose its source without touching shared code.
 */
@Module
@InstallIn(SingletonComponent::class)
object DesignSystemModule {

    @Provides
    @Singleton
    fun provideRemoteStubThemeResolver(): RemoteStubThemeResolver = RemoteStubThemeResolver()

    @Provides
    @Singleton
    fun provideThemeResolver(stub: RemoteStubThemeResolver): ThemeResolver = stub

    @Provides
    @Singleton
    fun provideThemeController(stub: RemoteStubThemeResolver): ThemeController = stub

    @Provides
    @Singleton
    fun provideRemoteThemeSource(
        brandPaletteApi: BrandPaletteApi,
    ): RemoteThemeSource = SupabaseRemoteThemeSource(brandPaletteApi)
}
