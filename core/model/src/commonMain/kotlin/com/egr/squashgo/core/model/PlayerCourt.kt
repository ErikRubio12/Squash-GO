package com.egr.squashgo.core.model

data class PlayerCourt(
    val playerId: String,
    val courtId: String,
    val isPrimary: Boolean = false,
)
