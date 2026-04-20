package com.egr.squashgo.data.network.repository

import com.egr.squashgo.core.domain.repository.ChallengeRepository
import com.egr.squashgo.core.model.Challenge
import com.egr.squashgo.core.model.MatchType
import com.egr.squashgo.data.network.api.ChallengeApi
import com.egr.squashgo.data.network.dto.CreateChallengeRequest
import com.egr.squashgo.data.network.mapper.ChallengeMapper

class SupabaseChallengeRepository(
    private val challengeApi: ChallengeApi,
    private val currentUserId: () -> String,
) : ChallengeRepository {

    override suspend fun sendChallenge(
        challengedId: String,
        courtId: String?,
        matchType: MatchType,
        message: String?,
    ): Challenge {
        val request = CreateChallengeRequest(
            challengerId = currentUserId(),
            challengedId = challengedId,
            courtId = courtId,
            matchType = ChallengeMapper.matchTypeToString(matchType),
            message = message,
        )
        return ChallengeMapper.toDomain(challengeApi.createChallenge(request))
    }

    override suspend fun getIncomingChallenges(playerId: String): List<Challenge> {
        return challengeApi.getIncomingChallenges(playerId).map(ChallengeMapper::toDomain)
    }

    override suspend fun getOutgoingChallenges(playerId: String): List<Challenge> {
        return challengeApi.getOutgoingChallenges(playerId).map(ChallengeMapper::toDomain)
    }

    override suspend fun acceptChallenge(challengeId: String): Challenge {
        return ChallengeMapper.toDomain(challengeApi.updateChallengeStatus(challengeId, "accepted"))
    }

    override suspend fun declineChallenge(challengeId: String): Challenge {
        return ChallengeMapper.toDomain(challengeApi.updateChallengeStatus(challengeId, "declined"))
    }

    override suspend fun cancelChallenge(challengeId: String): Challenge {
        return ChallengeMapper.toDomain(challengeApi.updateChallengeStatus(challengeId, "cancelled"))
    }
}
