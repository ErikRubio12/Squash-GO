package com.egr.squashgo.data.network.di

import android.util.Log
import com.egr.squashgo.core.auth.SessionManager
import com.egr.squashgo.core.domain.repository.AuthRepository
import com.egr.squashgo.data.network.api.SupabaseApi
import com.egr.squashgo.core.config.SupabaseConfig
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val TAG = "NetworkModule"

    @Provides
    @Singleton
    fun provideHttpClient(
        config: SupabaseConfig,
        sessionManager: SessionManager,
        authRepository: Lazy<AuthRepository>,
    ): HttpClient {
        return SupabaseApi.createClient(
            supabaseUrl = config.url,
            supabaseAnonKey = config.anonKey,
            loadSessionTokens = {
                val access = sessionManager.accessToken
                val refresh = sessionManager.refreshToken
                if (access != null && refresh != null) access to refresh else null
            },
            refreshSession = { oldRefreshToken ->
                try {
                    val session = authRepository.get().refreshToken(oldRefreshToken)
                    sessionManager.updateSession(
                        accessToken = session.accessToken,
                        refreshToken = session.refreshToken,
                        userId = session.userId,
                        expiresInSeconds = session.expiresIn,
                    )
                    session.accessToken to session.refreshToken
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    Log.w(TAG, "token refresh failed; clearing session", e)
                    sessionManager.clearSession()
                    null
                }
            },
        )
    }
}
