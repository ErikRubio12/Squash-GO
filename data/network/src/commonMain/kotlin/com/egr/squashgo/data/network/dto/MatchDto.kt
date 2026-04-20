package com.egr.squashgo.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MatchDto(
    val id: String,
    @SerialName("challenge_id") val challengeId: String,
    @SerialName("player_a_id") val playerAId: String,
    @SerialName("player_b_id") val playerBId: String,
    @SerialName("court_id") val courtId: String? = null,
    @SerialName("match_type") val matchType: String,
    val status: String,
    @SerialName("submitted_by_id") val submittedById: String? = null,
    val score: MatchScoreDto? = null,
    @SerialName("winner_id") val winnerId: String? = null,
    @SerialName("played_at") val playedAt: String? = null,
    @SerialName("submitted_at") val submittedAt: String? = null,
    @SerialName("confirmed_at") val confirmedAt: String? = null,
    @SerialName("auto_accept_at") val autoAcceptAt: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
data class MatchScoreDto(
    val games: List<GameScoreDto>,
)

@Serializable
data class GameScoreDto(
    @SerialName("player_a") val playerA: Int,
    @SerialName("player_b") val playerB: Int,
)

@Serializable
data class SubmitResultRequest(
    val status: String = "result_submitted",
    @SerialName("submitted_by_id") val submittedById: String,
    val score: MatchScoreDto,
    @SerialName("winner_id") val winnerId: String,
    @SerialName("submitted_at") val submittedAt: String,
    @SerialName("auto_accept_at") val autoAcceptAt: String,
)

@Serializable
data class CreateMatchRequest(
    @SerialName("challenge_id") val challengeId: String,
    @SerialName("player_a_id") val playerAId: String,
    @SerialName("player_b_id") val playerBId: String,
    @SerialName("court_id") val courtId: String? = null,
    @SerialName("match_type") val matchType: String,
)
