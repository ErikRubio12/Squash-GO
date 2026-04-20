package com.egr.squashgo.data.network.mapper

import com.egr.squashgo.core.model.Player
import com.egr.squashgo.data.network.dto.PlayerDto
import kotlinx.datetime.Instant

object PlayerMapper {

    fun toDomain(dto: PlayerDto): Player {
        return Player(
            id = dto.id,
            displayName = dto.displayName,
            email = "", // email not returned by PostgREST for other players
            avatarUrl = dto.avatarUrl,
            availabilityNote = dto.availabilityNote,
            isInactive = dto.isInactive,
            createdAt = Instant.parse(dto.createdAt),
            updatedAt = Instant.parse(dto.updatedAt),
        )
    }
}
