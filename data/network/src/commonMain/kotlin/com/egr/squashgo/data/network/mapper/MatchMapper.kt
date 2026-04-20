package com.egr.squashgo.data.network.mapper

import com.egr.squashgo.core.model.GameScore
import com.egr.squashgo.core.model.Match
import com.egr.squashgo.core.model.MatchScore
import com.egr.squashgo.core.model.MatchStatus
import com.egr.squashgo.core.model.MatchType
import com.egr.squashgo.data.network.dto.GameScoreDto
import com.egr.squashgo.data.network.dto.MatchDto
import com.egr.squashgo.data.network.dto.MatchScoreDto
import kotlinx.datetime.Instant

object MatchMapper {

    fun toDomain(dto: MatchDto): Match {
        return Match(
            id = dto.id,
            challengeId = dto.challengeId,
            playerAId = dto.playerAId,
            playerBId = dto.playerBId,
            courtId = dto.courtId,
            matchType = when (dto.matchType) {
                "ranked" -> MatchType.RANKED
                else -> MatchType.CASUAL
            },
            status = when (dto.status) {
                "in_progress" -> MatchStatus.IN_PROGRESS
                "result_submitted" -> MatchStatus.RESULT_SUBMITTED
                "confirmed" -> MatchStatus.CONFIRMED
                "disputed" -> MatchStatus.DISPUTED
                "cancelled" -> MatchStatus.CANCELLED
                else -> MatchStatus.IN_PROGRESS
            },
            submittedById = dto.submittedById,
            score = dto.score?.let { scoreToDomain(it) },
            winnerId = dto.winnerId,
            playedAt = dto.playedAt?.let { Instant.parse(it) },
            submittedAt = dto.submittedAt?.let { Instant.parse(it) },
            confirmedAt = dto.confirmedAt?.let { Instant.parse(it) },
            autoAcceptAt = dto.autoAcceptAt?.let { Instant.parse(it) },
            createdAt = Instant.parse(dto.createdAt),
            updatedAt = Instant.parse(dto.updatedAt),
        )
    }

    fun scoreToDomain(dto: MatchScoreDto): MatchScore {
        return MatchScore(
            games = dto.games.map { GameScore(it.playerA, it.playerB) }
        )
    }

    fun scoreToDto(score: MatchScore): MatchScoreDto {
        return MatchScoreDto(
            games = score.games.map { GameScoreDto(it.playerAScore, it.playerBScore) }
        )
    }
}
