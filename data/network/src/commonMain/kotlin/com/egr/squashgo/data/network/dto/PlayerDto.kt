package com.egr.squashgo.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PlayerDto(
    val id: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("availability_note") val availabilityNote: String? = null,
    @SerialName("is_inactive") val isInactive: Boolean = false,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
data class UpdatePlayerRequest(
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("availability_note") val availabilityNote: String? = null,
)
