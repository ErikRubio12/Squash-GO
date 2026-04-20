package com.egr.squashgo.di

import com.egr.squashgo.BuildConfig
import com.egr.squashgo.core.auth.SessionManager
import com.egr.squashgo.data.network.api.SupabaseApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideHttpClient(sessionManager: SessionManager): HttpClient {
        return SupabaseApi.createClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseAnonKey = BuildConfig.SUPABASE_ANON_KEY,
            tokenProvider = { sessionManager.accessToken },
        )
    }
}
