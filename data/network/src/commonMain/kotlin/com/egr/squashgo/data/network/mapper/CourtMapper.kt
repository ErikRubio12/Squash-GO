package com.egr.squashgo.data.network.mapper

import com.egr.squashgo.core.domain.repository.CourtWithPlayers
import com.egr.squashgo.core.domain.repository.PlayerWithRating
import com.egr.squashgo.core.model.Court
import com.egr.squashgo.core.model.Rating
import com.egr.squashgo.data.network.dto.CourtDto
import com.egr.squashgo.data.network.dto.CourtWithPlayersDto
import kotlinx.datetime.Instant

object CourtMapper {

    fun toDomain(dto: CourtDto): Court {
        return Court(
            id = dto.id,
            name = dto.name,
            address = dto.address,
            latitude = dto.latitude,
            longitude = dto.longitude,
            numberOfCourts = dto.numberOfCourts,
            phoneNumber = dto.phoneNumber,
            website = dto.website,
            isVerified = dto.isVerified,
            createdAt = Instant.parse(dto.createdAt),
        )
    }

    fun toDomainWithPlayers(
        dto: CourtWithPlayersDto,
        ratingsMap: Map<String, Rating>,
    ): CourtWithPlayers {
        val court = Court(
            id = dto.id,
            name = dto.name,
            address = dto.address,
            latitude = dto.latitude,
            longitude = dto.longitude,
            numberOfCourts = dto.numberOfCourts,
            phoneNumber = dto.phoneNumber,
            website = dto.website,
            isVerified = dto.isVerified,
            createdAt = Instant.parse(dto.createdAt),
        )
        val players = dto.playerCourts.mapNotNull { join ->
            val playerDto = join.players ?: return@mapNotNull null
            val player = PlayerMapper.toDomain(playerDto)
            val rating = ratingsMap[player.id] ?: return@mapNotNull null
            PlayerWithRating(player, rating)
        }
        return CourtWithPlayers(court, players)
    }
}
