package com.egr.squashgo.data.network.mapper

import com.egr.squashgo.core.model.Session
import com.egr.squashgo.data.network.dto.SessionDto

object AuthMapper {

    fun toDomain(dto: SessionDto): Session {
        return Session(
            accessToken = dto.accessToken,
            refreshToken = dto.refreshToken,
            expiresIn = dto.expiresIn,
            userId = dto.user.id,
            email = dto.user.email,
        )
    }
}