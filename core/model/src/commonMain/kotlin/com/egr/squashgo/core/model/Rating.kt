package com.egr.squashgo.core.model

import kotlinx.datetime.Instant

data class Rating(
    val playerId: String,
    val eloScore: Int,
    val rankedMatchesPlayed: Int,
    val isProvisional: Boolean,
    val updatedAt: Instant,
)

data class RatingHistory(
    val id: String,
    val playerId: String,
    val matchId: String,
    val eloBefore: Int,
    val eloAfter: Int,
    val kFactor: Int,
    val createdAt: Instant,
)
