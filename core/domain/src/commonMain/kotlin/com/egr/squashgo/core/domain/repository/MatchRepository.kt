package com.egr.squashgo.core.domain.repository

import com.egr.squashgo.core.model.Match
import com.egr.squashgo.core.model.MatchScore

interface MatchRepository {
    suspend fun getMatch(matchId: String): Match
    suspend fun getMatchesForPlayer(playerId: String): List<Match>
    suspend fun submitResult(matchId: String, score: MatchScore, winnerId: String): Match
    suspend fun confirmMatch(matchId: String): Match
    suspend fun disputeMatch(matchId: String, reason: String): Match
}
