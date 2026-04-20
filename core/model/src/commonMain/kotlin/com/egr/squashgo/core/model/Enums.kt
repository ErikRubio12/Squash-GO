package com.egr.squashgo.core.model

enum class MatchType {
    CASUAL,
    RANKED,
}

enum class ChallengeStatus {
    PENDING,
    ACCEPTED,
    DECLINED,
    CANCELLED,
    EXPIRED,
    COMPLETED,
}

enum class MatchStatus {
    IN_PROGRESS,
    RESULT_SUBMITTED,
    CONFIRMED,
    DISPUTED,
    CANCELLED,
}

enum class Tier {
    BRONZE,
    SILVER,
    GOLD,
    PLATINUM,
    DIAMOND,
    MASTER,
}

enum class DisputeResolution {
    SCORE_ACCEPTED,
    SCORE_CORRECTED,
    MATCH_CANCELLED,
}
