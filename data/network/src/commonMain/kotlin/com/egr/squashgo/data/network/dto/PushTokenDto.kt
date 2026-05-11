package com.egr.squashgo.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpsertPushTokenRequest(
    @SerialName("player_id") val playerId: String,
    val token: String,
    val platform: String,
    val locale: String?,
)
