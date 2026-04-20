package com.egr.squashgo.data.network.repository

import com.egr.squashgo.core.domain.repository.AuthRepository
import com.egr.squashgo.core.model.Session
import com.egr.squashgo.data.network.api.AuthApi
import com.egr.squashgo.data.network.mapper.AuthMapper

class SupabaseAuthRepository(
    private val authApi: AuthApi,
) : AuthRepository {

    override suspend fun sendMagicLink(email: String, redirectUrl: String) {
        authApi.sendMagicLink(email, redirectUrl)
    }

    override suspend fun verifyOtp(email: String, token: String): Session {
        return AuthMapper.toDomain(authApi.verifyOtp(email, token))
    }

    override suspend fun refreshToken(refreshToken: String): Session {
        return AuthMapper.toDomain(authApi.refreshToken(refreshToken))
    }
}