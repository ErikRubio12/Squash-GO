package com.egr.squashgo.core.domain.repository

import com.egr.squashgo.core.model.Session

interface AuthRepository {
    suspend fun sendMagicLink(email: String, redirectUrl: String)
    suspend fun verifyOtp(email: String, token: String): Session
    suspend fun refreshToken(refreshToken: String): Session
}