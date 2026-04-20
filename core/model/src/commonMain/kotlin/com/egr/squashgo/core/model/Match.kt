package com.egr.squashgo.core.model

import kotlinx.datetime.Instant

data class Match(
    val id: String,
    val challengeId: String,
    val playerAId: String,
    val playerBId: String,
    val courtId: String? = null,
    val matchType: MatchType,
    val status: MatchStatus,
    val submittedById: String? = null,
    val score: MatchScore? = null,
    val winnerId: String? = null,
    val playedAt: Instant? = null,
    val submittedAt: Instant? = null,
    val confirmedAt: Instant? = null,
    val autoAcceptAt: Instant? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class MatchScore(
    val games: List<GameScore>,
)

data class GameScore(
    val playerAScore: Int,
    val playerBScore: Int,
)
