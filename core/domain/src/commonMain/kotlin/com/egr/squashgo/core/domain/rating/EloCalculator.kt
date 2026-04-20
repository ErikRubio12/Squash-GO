package com.egr.squashgo.core.domain.rating

import kotlin.math.pow
import kotlin.math.roundToInt

object EloCalculator {

    private const val K_BOTH_PROVISIONAL = 60
    private const val K_ONE_PROVISIONAL = 40
    private const val K_ESTABLISHED = 32
    private const val ELO_FLOOR = 400
    private const val PROVISIONAL_THRESHOLD = 5

    fun calculateNewRatings(
        winnerElo: Int,
        loserElo: Int,
        winnerMatchesPlayed: Int,
        loserMatchesPlayed: Int,
    ): Pair<Int, Int> {
        val kFactor = determineKFactor(winnerMatchesPlayed, loserMatchesPlayed)
        val expectedWinner = expectedScore(winnerElo, loserElo)
        val expectedLoser = 1.0 - expectedWinner

        val newWinnerElo = (winnerElo + kFactor * (1.0 - expectedWinner)).roundToInt()
            .coerceAtLeast(ELO_FLOOR)
        val newLoserElo = (loserElo + kFactor * (0.0 - expectedLoser)).roundToInt()
            .coerceAtLeast(ELO_FLOOR)

        return newWinnerElo to newLoserElo
    }

    fun determineKFactor(playerAMatchesPlayed: Int, playerBMatchesPlayed: Int): Int {
        val aProvisional = playerAMatchesPlayed < PROVISIONAL_THRESHOLD
        val bProvisional = playerBMatchesPlayed < PROVISIONAL_THRESHOLD
        return when {
            aProvisional && bProvisional -> K_BOTH_PROVISIONAL
            aProvisional || bProvisional -> K_ONE_PROVISIONAL
            else -> K_ESTABLISHED
        }
    }

    private fun expectedScore(ratingA: Int, ratingB: Int): Double {
        return 1.0 / (1.0 + 10.0.pow((ratingB - ratingA) / 400.0))
    }
}
