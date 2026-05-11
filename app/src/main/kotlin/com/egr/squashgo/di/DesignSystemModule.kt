package com.egr.squashgo.di

import com.egr.squashgo.core.designsystem.theme.RemoteStubThemeResolver
import com.egr.squashgo.core.designsystem.theme.ThemeController
import com.egr.squashgo.core.designsystem.theme.ThemeResolver
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides the active theme resolver to the rest of the app.
 *
 * The same [RemoteStubThemeResolver] singleton is bound under both [ThemeResolver]
 * (read-only) and [ThemeController] (mutation) so consumers depend only on the role they
 * need. Today this is a local stub that drives the design-system demo screen; replacing
 * it with a server-backed implementation later is a one-line change in this module.
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
