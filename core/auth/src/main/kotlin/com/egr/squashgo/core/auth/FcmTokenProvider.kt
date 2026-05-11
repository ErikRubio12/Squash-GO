package com.egr.squashgo.core.auth

interface FcmTokenProvider {
    suspend fun currentToken(): String?
}
