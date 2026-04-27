package com.egr.squashgo.core.config.android

import com.egr.squashgo.core.config.SupabaseConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppConfigModule {

    @Provides
    @Singleton
    fun provideSupabaseConfig(): SupabaseConfig = ConfigBootstrap.requireSupabaseConfig()
}