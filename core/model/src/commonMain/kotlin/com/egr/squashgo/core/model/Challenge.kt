package com.egr.squashgo.core.model

import kotlinx.datetime.Instant

data class Challenge(
    val id: String,
    val challengerId: String,
    val challengedId: String,
    val courtId: String? = null,
    val matchType: MatchType,
    val message: String? = null,
    val status: ChallengeStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
    val expiresAt: Instant,
)
