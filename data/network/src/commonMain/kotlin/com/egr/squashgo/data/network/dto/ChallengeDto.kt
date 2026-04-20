package com.egr.squashgo.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChallengeDto(
    val id: String,
    @SerialName("challenger_id") val challengerId: String,
    @SerialName("challenged_id") val challengedId: String,
    @SerialName("court_id") val courtId: String? = null,
    @SerialName("match_type") val matchType: String,
    val message: String? = null,
    val status: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("expires_at") val expiresAt: String,
)

@Serializable
data class CreateChallengeRequest(
    @SerialName("challenger_id") val challengerId: String,
    @SerialName("challenged_id") val challengedId: String,
    @SerialName("court_id") val courtId: String? = null,
    @SerialName("match_type") val matchType: String,
    val message: String? = null,
)

@Serializable
data class UpdateChallengeRequest(
    val status: String,
)
