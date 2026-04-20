package com.egr.squashgo.core.model

import kotlinx.datetime.Instant

data class Player(
    val id: String,
    val displayName: String,
    val email: String,
    val avatarUrl: String? = null,
    val availabilityNote: String? = null,
    val isInactive: Boolean = false,
    val createdAt: Instant,
    val updatedAt: Instant,
)
