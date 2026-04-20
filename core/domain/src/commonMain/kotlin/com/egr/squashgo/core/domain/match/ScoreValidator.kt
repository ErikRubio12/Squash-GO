package com.egr.squashgo.core.domain.match

import com.egr.squashgo.core.model.GameScore
import com.egr.squashgo.core.model.MatchScore

object ScoreValidator {

    private const val STANDARD_WIN_SCORE = 11
    private const val MIN_GAMES_TO_WIN = 3
    private const val MAX_GAMES = 5

    fun validateMatchScore(score: MatchScore): ValidationResult {
        val games = score.games

        if (games.isEmpty() || games.size > MAX_GAMES) {
            return ValidationResult.Invalid("Match must have 1 to $MAX_GAMES games")
        }

        for ((index, game) in games.withIndex()) {
            val gameResult = validateGameScore(game)
            if (gameResult is ValidationResult.Invalid) {
                return ValidationResult.Invalid("Game ${index + 1}: ${gameResult.reason}")
            }
        }

        val playerAWins = games.count { isGameWinner(it.playerAScore, it.playerBScore) }
        val playerBWins = games.count { isGameWinner(it.playerBScore, it.playerAScore) }

        if (playerAWins != MIN_GAMES_TO_WIN && playerBWins != MIN_GAMES_TO_WIN) {
            return ValidationResult.Invalid("One player must win $MIN_GAMES_TO_WIN games")
        }

        // No extra games after someone already won
        val maxWins = maxOf(playerAWins, playerBWins)
        if (maxWins > MIN_GAMES_TO_WIN) {
            return ValidationResult.Invalid("Match should end when a player reaches $MIN_GAMES_TO_WIN wins")
        }

        return ValidationResult.Valid
    }

    fun validateGameScore(game: GameScore): ValidationResult {
        val high = maxOf(game.playerAScore, game.playerBScore)
        val low = minOf(game.playerAScore, game.playerBScore)

        if (high < STANDARD_WIN_SCORE) {
            return ValidationResult.Invalid("Winner must reach at least $STANDARD_WIN_SCORE points")
        }

        // Standard win: 11-X where X <= 9
        if (high == STANDARD_WIN_SCORE && low <= STANDARD_WIN_SCORE - 2) {
            return ValidationResult.Valid
        }

        // Deuce scenario: must win by 2
        if (high > STANDARD_WIN_SCORE || (high == STANDARD_WIN_SCORE && low >= STANDARD_WIN_SCORE - 1)) {
            if (high - low != 2) {
                return ValidationResult.Invalid("In a close game, winner must lead by exactly 2 points")
            }
            return ValidationResult.Valid
        }

        return ValidationResult.Invalid("Invalid game score: ${game.playerAScore}-${game.playerBScore}")
    }

    fun getWinnerId(score: MatchScore, playerAId: String, playerBId: String): String? {
        val playerAWins = score.games.count { isGameWinner(it.playerAScore, it.playerBScore) }
        val playerBWins = score.games.count { isGameWinner(it.playerBScore, it.playerAScore) }
        return when {
            playerAWins == MIN_GAMES_TO_WIN -> playerAId
            playerBWins == MIN_GAMES_TO_WIN -> playerBId
            else -> null
        }
    }

    private fun isGameWinner(winnerScore: Int, loserScore: Int): Boolean {
        if (winnerScore < STANDARD_WIN_SCORE) return false
        if (winnerScore == STANDARD_WIN_SCORE && loserScore <= STANDARD_WIN_SCORE - 2) return true
        return winnerScore > loserScore && winnerScore - loserScore == 2 && loserScore >= STANDARD_WIN_SCORE - 1
    }

    sealed class ValidationResult {
        data object Valid : ValidationResult()
        data class Invalid(val reason: String) : ValidationResult()
    }
}
