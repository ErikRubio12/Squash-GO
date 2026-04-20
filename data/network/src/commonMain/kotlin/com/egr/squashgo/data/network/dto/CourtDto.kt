package com.egr.squashgo.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CourtDto(
    val id: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    @SerialName("number_of_courts") val numberOfCourts: Int? = null,
    @SerialName("phone_number") val phoneNumber: String? = null,
    val website: String? = null,
    @SerialName("is_verified") val isVerified: Boolean = true,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class CourtWithPlayersDto(
    val id: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    @SerialName("number_of_courts") val numberOfCourts: Int? = null,
    @SerialName("phone_number") val phoneNumber: String? = null,
    val website: String? = null,
    @SerialName("is_verified") val isVerified: Boolean = true,
    @SerialName("created_at") val createdAt: String,
    @SerialName("player_courts") val playerCourts: List<PlayerCourtJoinDto> = emptyList(),
)

@Serializable
data class PlayerCourtJoinDto(
    @SerialName("player_id") val playerId: String,
    val players: PlayerDto? = null,
)
