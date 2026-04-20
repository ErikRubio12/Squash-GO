package com.egr.squashgo.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MagicLinkRequest(
    val email: String,
)

@Serializable
data class VerifyOtpRequest(
    val email: String,
    val token: String,
    val type: String = "magiclink",
)

@Serializable
data class SessionDto(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("token_type") val tokenType: String = "bearer",
    @SerialName("expires_in") val expiresIn: Long,
    val user: AuthUserDto,
)

@Serializable
data class RefreshTokenRequest(
    @SerialName("refresh_token") val refreshToken: String,
)

@Serializable
data class AuthUserDto(
    val id: String,
    val email: String? = null,
)

@Serializable
data class ConfirmMatchRequest(
    @SerialName("match_id") val matchId: String,
)

@Serializable
data class ConfirmMatchResponse(
    val success: Boolean,
    @SerialName("winner_new_elo") val winnerNewElo: Int? = null,
    @SerialName("loser_new_elo") val loserNewElo: Int? = null,
)
