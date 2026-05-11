package com.egr.squashgo.di

import com.egr.squashgo.core.designsystem.sync.RemoteThemeSource
import com.egr.squashgo.core.designsystem.theme.RemoteStubThemeResolver
import com.egr.squashgo.core.designsystem.theme.ThemeController
import com.egr.squashgo.core.designsystem.theme.ThemeResolver
import com.egr.squashgo.data.SimulatedRemoteThemeSource
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides the active theme resolver and remote source to the rest of the app.
 *
 * The same [RemoteStubThemeResolver] singleton is bound under both [ThemeResolver]
 * (read-only) and [ThemeController] (mutation) so consumers depend only on the role they
 * need. The [RemoteThemeSource] is the contract for *where palettes come from* — today
 * a local simulation, tomorrow an HTTP / Supabase / websocket impl. Swap one line here to
 * change it; nothing else in the app needs to know.
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
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RemoteThemeSourceModule {

    @Binds
    @Singleton
    abstract fun bindRemoteThemeSource(
        simulated: SimulatedRemoteThemeSource,
    ): RemoteThemeSource
}
