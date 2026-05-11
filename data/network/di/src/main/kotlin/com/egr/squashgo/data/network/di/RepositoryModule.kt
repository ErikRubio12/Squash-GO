package com.egr.squashgo.data.network.di

import com.egr.squashgo.core.auth.SessionManager
import com.egr.squashgo.core.domain.repository.AuthRepository
import com.egr.squashgo.core.domain.repository.ChallengeRepository
import com.egr.squashgo.core.domain.repository.CourtRepository
import com.egr.squashgo.core.domain.repository.MatchRepository
import com.egr.squashgo.core.domain.repository.PlayerRepository
import com.egr.squashgo.core.domain.repository.PushTokenRepository
import com.egr.squashgo.core.domain.repository.RatingRepository
import com.egr.squashgo.data.network.api.AuthApi
import com.egr.squashgo.data.network.api.ChallengeApi
import com.egr.squashgo.data.network.api.CourtApi
import com.egr.squashgo.data.network.api.EdgeFunctionApi
import com.egr.squashgo.data.network.api.MatchApi
import com.egr.squashgo.data.network.api.PlayerApi
import com.egr.squashgo.data.network.api.PushTokenApi
import com.egr.squashgo.data.network.repository.SupabaseAuthRepository
import com.egr.squashgo.data.network.repository.SupabaseChallengeRepository
import com.egr.squashgo.data.network.repository.SupabaseCourtRepository
import com.egr.squashgo.data.network.repository.SupabaseMatchRepository
import com.egr.squashgo.data.network.repository.SupabasePlayerRepository
import com.egr.squashgo.data.network.repository.SupabasePushTokenRepository
import com.egr.squashgo.data.network.repository.SupabaseRatingRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideAuthRepository(authApi: AuthApi): AuthRepository =
        SupabaseAuthRepository(authApi)

    @Provides
    @Singleton
    fun provideCourtRepository(courtApi: CourtApi, playerApi: PlayerApi): CourtRepository =
        SupabaseCourtRepository(courtApi, playerApi)

    @Provides
    @Singleton
    fun providePlayerRepository(playerApi: PlayerApi, client: HttpClient): PlayerRepository =
        SupabasePlayerRepository(playerApi, client)

    @Provides
    @Singleton
    fun provideChallengeRepository(
        challengeApi: ChallengeApi,
        sessionManager: SessionManager,
    ): ChallengeRepository =
        SupabaseChallengeRepository(
            challengeApi,
            currentUserId = { sessionManager.currentUserId ?: error("Not logged in") },
        )

    @Provides
    @Singleton
    fun provideMatchRepository(
        matchApi: MatchApi,
        edgeFunctionApi: EdgeFunctionApi,
        sessionManager: SessionManager,
    ): MatchRepository =
        SupabaseMatchRepository(
            matchApi,
            edgeFunctionApi,
            currentUserId = { sessionManager.currentUserId ?: error("Not logged in") },
        )

    @Provides
    @Singleton
    fun provideRatingRepository(playerApi: PlayerApi, matchApi: MatchApi): RatingRepository =
        SupabaseRatingRepository(playerApi, matchApi)

    @Provides
    @Singleton
    fun providePushTokenRepository(pushTokenApi: PushTokenApi): PushTokenRepository =
        SupabasePushTokenRepository(pushTokenApi)
}
