package com.egr.squashgo.core.domain.repository

interface PushTokenRepository {
    suspend fun upsert(playerId: String, token: String, platform: String, locale: String?)
    suspend fun deleteByToken(token: String)
}
