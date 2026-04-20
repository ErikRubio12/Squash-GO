package com.egr.squashgo.core.domain.repository

import com.egr.squashgo.core.model.Challenge
import com.egr.squashgo.core.model.MatchType

interface ChallengeRepository {
    suspend fun sendChallenge(
        challengedId: String,
        courtId: String?,
        matchType: MatchType,
        message: String?,
    ): Challenge

    suspend fun getIncomingChallenges(playerId: String): List<Challenge>
    suspend fun getOutgoingChallenges(playerId: String): List<Challenge>
    suspend fun acceptChallenge(challengeId: String): Challenge
    suspend fun declineChallenge(challengeId: String): Challenge
    suspend fun cancelChallenge(challengeId: String): Challenge
}
