package com.egr.squashgo.core.model

import kotlinx.datetime.Instant

data class Court(
    val id: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val numberOfCourts: Int? = null,
    val phoneNumber: String? = null,
    val website: String? = null,
    val isVerified: Boolean = true,
    val createdAt: Instant,
)
