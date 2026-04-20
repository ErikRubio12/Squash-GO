package com.egr.squashgo.core.model

import kotlinx.datetime.Instant

data class Dispute(
    val id: String,
    val matchId: String,
    val raisedById: String,
    val reason: String,
    val adminNotes: String? = null,
    val resolution: DisputeResolution? = null,
    val resolvedAt: Instant? = null,
    val createdAt: Instant,
)
