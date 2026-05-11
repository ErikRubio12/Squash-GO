package com.egr.squashgo.data.network.di

import com.egr.squashgo.data.network.api.AuthApi
import com.egr.squashgo.data.network.api.BrandPaletteApi
import com.egr.squashgo.data.network.api.ChallengeApi
import com.egr.squashgo.data.network.api.CourtApi
import com.egr.squashgo.data.network.api.EdgeFunctionApi
import com.egr.squashgo.data.network.api.MatchApi
import com.egr.squashgo.data.network.api.PlayerApi
import com.egr.squashgo.data.network.api.PushTokenApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApiModule {

    @Provides
    @Singleton
    fun provideAuthApi(client: HttpClient): AuthApi = AuthApi(client)

    @Provides
    @Singleton
    fun provideCourtApi(client: HttpClient): CourtApi = CourtApi(client)

    @Provides
    @Singleton
    fun providePlayerApi(client: HttpClient): PlayerApi = PlayerApi(client)

    @Provides
    @Singleton
    fun provideChallengeApi(client: HttpClient): ChallengeApi = ChallengeApi(client)

    @Provides
    @Singleton
    fun provideMatchApi(client: HttpClient): MatchApi = MatchApi(client)

    @Provides
    @Singleton
    fun provideEdgeFunctionApi(client: HttpClient): EdgeFunctionApi = EdgeFunctionApi(client)

    @Provides
    @Singleton
    fun providePushTokenApi(client: HttpClient): PushTokenApi = PushTokenApi(client)

    @Provides
    @Singleton
    fun provideBrandPaletteApi(client: HttpClient): BrandPaletteApi = BrandPaletteApi(client)
}
