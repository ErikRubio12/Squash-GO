package com.egr.squashgo.data.network.repository

import com.egr.squashgo.core.domain.repository.MatchRepository
import com.egr.squashgo.core.model.Match
import com.egr.squashgo.core.model.MatchScore
import com.egr.squashgo.data.network.api.EdgeFunctionApi
import com.egr.squashgo.data.network.api.MatchApi
import com.egr.squashgo.data.network.dto.SubmitResultRequest
import com.egr.squashgo.data.network.mapper.MatchMapper
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.hours

class SupabaseMatchRepository(
    private val matchApi: MatchApi,
    private val edgeFunctionApi: EdgeFunctionApi,
    private val currentUserId: () -> String,
) : MatchRepository {

    override suspend fun getMatch(matchId: String): Match {
        return MatchMapper.toDomain(matchApi.getMatch(matchId))
    }

    override suspend fun getMatchesForPlayer(playerId: String): List<Match> {
        return matchApi.getMatchesForPlayer(playerId).map(MatchMapper::toDomain)
    }

    override suspend fun submitResult(matchId: String, score: MatchScore, winnerId: String): Match {
        val now = Clock.System.now()
        val autoAcceptAt = now.plus(72.hours)
        val request = SubmitResultRequest(
            submittedById = currentUserId(),
            score = MatchMapper.scoreToDto(score),
            winnerId = winnerId,
            submittedAt = now.toString(),
            autoAcceptAt = autoAcceptAt.toString(),
        )
        return MatchMapper.toDomain(matchApi.submitResult(matchId, request))
    }

    override suspend fun confirmMatch(matchId: String): Match {
        edgeFunctionApi.confirmMatch(matchId)
        return getMatch(matchId)
    }

    override suspend fun disputeMatch(matchId: String, reason: String): Match {
        return getMatch(matchId)
    }
}
