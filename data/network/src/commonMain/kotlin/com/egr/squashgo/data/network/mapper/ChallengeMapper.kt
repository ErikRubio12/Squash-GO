package com.egr.squashgo.data.network.mapper

import com.egr.squashgo.core.model.Challenge
import com.egr.squashgo.core.model.ChallengeStatus
import com.egr.squashgo.core.model.MatchType
import com.egr.squashgo.data.network.dto.ChallengeDto
import kotlinx.datetime.Instant

object ChallengeMapper {

    fun toDomain(dto: ChallengeDto): Challenge {
        return Challenge(
            id = dto.id,
            challengerId = dto.challengerId,
            challengedId = dto.challengedId,
            courtId = dto.courtId,
            matchType = when (dto.matchType) {
                "ranked" -> MatchType.RANKED
                else -> MatchType.CASUAL
            },
            message = dto.message,
            status = when (dto.status) {
                "pending" -> ChallengeStatus.PENDING
                "accepted" -> ChallengeStatus.ACCEPTED
                "declined" -> ChallengeStatus.DECLINED
                "cancelled" -> ChallengeStatus.CANCELLED
                "expired" -> ChallengeStatus.EXPIRED
                "completed" -> ChallengeStatus.COMPLETED
                else -> ChallengeStatus.PENDING
            },
            createdAt = Instant.parse(dto.createdAt),
            updatedAt = Instant.parse(dto.updatedAt),
            expiresAt = Instant.parse(dto.expiresAt),
        )
    }

    fun matchTypeToString(matchType: MatchType): String {
        return when (matchType) {
            MatchType.CASUAL -> "casual"
            MatchType.RANKED -> "ranked"
        }
    }
}
