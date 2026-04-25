package com.egr.squashgo.data.network.repository

import com.egr.squashgo.core.domain.repository.CourtRepository
import com.egr.squashgo.core.domain.repository.model.CourtWithPlayers
import com.egr.squashgo.core.domain.repository.model.PlayerWithRating
import com.egr.squashgo.core.model.Court
import com.egr.squashgo.data.network.api.CourtApi
import com.egr.squashgo.data.network.api.PlayerApi
import com.egr.squashgo.data.network.dto.CourtDto
import com.egr.squashgo.data.network.mapper.CourtMapper
import com.egr.squashgo.data.network.mapper.PlayerMapper
import com.egr.squashgo.data.network.mapper.RatingMapper

class SupabaseCourtRepository(
    private val courtApi: CourtApi,
    private val playerApi: PlayerApi,
) : CourtRepository {

    override suspend fun getCourts(): List<Court> {
        return courtApi.getCourts().map(CourtMapper::toDomain)
    }

    override suspend fun getCourtById(courtId: String): Court {
        return CourtMapper.toDomain(courtApi.getCourtById(courtId))
    }

    override suspend fun getCourtWithPlayers(courtId: String): CourtWithPlayers {
        val courtDto = courtApi.getCourtWithPlayers(courtId)
        val court = CourtMapper.toDomain(
            CourtDto(
                id = courtDto.id,
                name = courtDto.name,
                address = courtDto.address,
                latitude = courtDto.latitude,
                longitude = courtDto.longitude,
                numberOfCourts = courtDto.numberOfCourts,
                phoneNumber = courtDto.phoneNumber,
                website = courtDto.website,
                isVerified = courtDto.isVerified,
                createdAt = courtDto.createdAt,
            )
        )
        val players = courtDto.playerCourts.mapNotNull { join ->
            val playerDto = join.players ?: return@mapNotNull null
            val player = PlayerMapper.toDomain(playerDto)
            val ratingDto = playerApi.getRating(player.id)
            val rating = RatingMapper.toDomain(ratingDto)
            PlayerWithRating(player, rating)
        }
        return CourtWithPlayers(court, players)
    }
}
