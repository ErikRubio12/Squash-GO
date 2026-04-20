package com.egr.squashgo.core.domain.rating

import com.egr.squashgo.core.model.Tier

data class TierDivision(
    val tier: Tier,
    val division: Int?, // 1-5 within tier, null for Master
)

object TierMapper {

    private const val TIER_SIZE = 200
    private const val DIVISION_SIZE = 40
    private const val BRONZE_FLOOR = 800

    fun fromElo(elo: Int): TierDivision {
        if (elo >= 1800) return TierDivision(Tier.MASTER, division = null)

        val clampedElo = elo.coerceAtLeast(BRONZE_FLOOR)
        val tierIndex = ((clampedElo - BRONZE_FLOOR) / TIER_SIZE).coerceIn(0, 4)
        val tier = when (tierIndex) {
            0 -> Tier.BRONZE
            1 -> Tier.SILVER
            2 -> Tier.GOLD
            3 -> Tier.PLATINUM
            4 -> Tier.DIAMOND
            else -> Tier.BRONZE
        }

        val tierFloor = BRONZE_FLOOR + (tierIndex * TIER_SIZE)
        val divisionIndex = ((clampedElo - tierFloor) / DIVISION_SIZE).coerceIn(0, 4)
        // Division 5 = lowest, Division 1 = highest
        val division = 5 - divisionIndex

        return TierDivision(tier, division)
    }

    fun displayString(tierDivision: TierDivision): String {
        val tierName = tierDivision.tier.name.lowercase()
            .replaceFirstChar { it.uppercase() }
        return if (tierDivision.division != null) {
            "$tierName ${tierDivision.division}"
        } else {
            tierName
        }
    }

    fun displayStringForRating(elo: Int, isProvisional: Boolean): String {
        if (isProvisional) return "Calibrating"
        return displayString(fromElo(elo))
    }
}
