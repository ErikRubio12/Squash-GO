package com.egr.squashgo.feature.play.impl.model

import com.egr.squashgo.core.model.GameScore
import com.egr.squashgo.core.model.Match

sealed interface MatchResultUiState {
    data object Loading : MatchResultUiState

    data class Ready(
        val match: Match,
        val youName: String,
        val opponentName: String,
        val youAreA: Boolean,
        val games: List<GameInput>,
        val isSubmitting: Boolean,
        val showInvalidScoreMessage: Boolean,
        val submitError: Boolean,
    ) : MatchResultUiState

    data object Done : MatchResultUiState

    data class Error(val error: MatchResultError) : MatchResultUiState
}

data class GameInput(
    val playerAScore: String = "",
    val playerBScore: String = "",
) {
    val isBlank: Boolean
        get() = playerAScore.isBlank() && playerBScore.isBlank()

    fun toGameScoreOrNull(): GameScore? {
        val a = playerAScore.toIntOrNull() ?: return null
        val b = playerBScore.toIntOrNull() ?: return null
        return GameScore(a, b)
    }
}

enum class MatchResultError {
    Network,
    NotAuthenticated,
    NotParticipant,
    AlreadySubmitted,
}
