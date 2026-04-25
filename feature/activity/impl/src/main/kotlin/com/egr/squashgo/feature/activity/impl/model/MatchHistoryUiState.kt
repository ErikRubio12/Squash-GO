package com.egr.squashgo.feature.activity.impl.model

import kotlinx.datetime.Instant

data class MatchRow(
    val matchId: String,
    val opponentName: String?,
    val didWin: Boolean,
    val userGames: Int,
    val opponentGames: Int,
    val playedAt: Instant?,
)

sealed interface MatchHistoryUiState {
    data object Loading : MatchHistoryUiState
    data object Empty : MatchHistoryUiState
    data class Ready(val rows: List<MatchRow>) : MatchHistoryUiState
    data class Error(val kind: MatchHistoryError) : MatchHistoryUiState
}

enum class MatchHistoryError {
    Network,
    NotAuthenticated,
}
