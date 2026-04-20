package com.egr.squashgo.data.network.repository

import com.egr.squashgo.core.domain.repository.RatingRepository
import com.egr.squashgo.core.model.Rating
import com.egr.squashgo.core.model.RatingHistory
import com.egr.squashgo.data.network.api.MatchApi
import com.egr.squashgo.data.network.api.PlayerApi
import com.egr.squashgo.data.network.mapper.RatingMapper

class SupabaseRatingRepository(
    private val playerApi: PlayerApi,
    private val matchApi: MatchApi,
) : RatingRepository {

    override suspend fun getRating(playerId: String): Rating {
        return RatingMapper.toDomain(playerApi.getRating(playerId))
    }

    override suspend fun getRatingHistory(playerId: String): List<RatingHistory> {
        return matchApi.getRatingHistory(playerId).map(RatingMapper::historyToDomain)
    }
}
