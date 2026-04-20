package com.egr.squashgo.data.network.mapper

import com.egr.squashgo.core.model.Rating
import com.egr.squashgo.core.model.RatingHistory
import com.egr.squashgo.data.network.dto.RatingDto
import com.egr.squashgo.data.network.dto.RatingHistoryDto
import kotlinx.datetime.Instant

object RatingMapper {

    fun toDomain(dto: RatingDto): Rating {
        return Rating(
            playerId = dto.playerId,
            eloScore = dto.eloScore,
            rankedMatchesPlayed = dto.rankedMatchesPlayed,
            isProvisional = dto.isProvisional,
            updatedAt = Instant.parse(dto.updatedAt),
        )
    }

    fun historyToDomain(dto: RatingHistoryDto): RatingHistory {
        return RatingHistory(
            id = dto.id,
            playerId = dto.playerId,
            matchId = dto.matchId,
            eloBefore = dto.eloBefore,
            eloAfter = dto.eloAfter,
            kFactor = dto.kFactor,
            createdAt = Instant.parse(dto.createdAt),
        )
    }
}
