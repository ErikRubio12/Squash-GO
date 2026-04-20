package com.egr.squashgo.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RatingDto(
    @SerialName("player_id") val playerId: String,
    @SerialName("elo_score") val eloScore: Int,
    @SerialName("ranked_matches_played") val rankedMatchesPlayed: Int,
    @SerialName("is_provisional") val isProvisional: Boolean,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
data class RatingHistoryDto(
    val id: String,
    @SerialName("player_id") val playerId: String,
    @SerialName("match_id") val matchId: String,
    @SerialName("elo_before") val eloBefore: Int,
    @SerialName("elo_after") val eloAfter: Int,
    @SerialName("k_factor") val kFactor: Int,
    @SerialName("created_at") val createdAt: String,
)
