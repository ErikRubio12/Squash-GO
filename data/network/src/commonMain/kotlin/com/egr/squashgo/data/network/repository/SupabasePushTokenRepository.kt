package com.egr.squashgo.data.network.repository

import com.egr.squashgo.core.domain.repository.PushTokenRepository
import com.egr.squashgo.data.network.api.PushTokenApi
import com.egr.squashgo.data.network.dto.UpsertPushTokenRequest

class SupabasePushTokenRepository(
    private val api: PushTokenApi,
) : PushTokenRepository {

    override suspend fun upsert(
        playerId: String,
        token: String,
        platform: String,
        locale: String?,
    ) {
        api.upsert(
            UpsertPushTokenRequest(
                playerId = playerId,
                token = token,
                platform = platform,
                locale = locale,
            ),
        )
    }

    override suspend fun deleteByToken(token: String) {
        api.deleteByToken(token)
    }
}
