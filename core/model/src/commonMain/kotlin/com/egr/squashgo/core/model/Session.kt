package com.egr.squashgo.core.model

data class Session(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
    val userId: String,
    val email: String?,
)