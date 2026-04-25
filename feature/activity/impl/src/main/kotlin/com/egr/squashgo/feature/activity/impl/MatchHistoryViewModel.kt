package com.egr.squashgo.feature.activity.impl

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.egr.squashgo.core.auth.SessionManager
import com.egr.squashgo.core.domain.repository.MatchRepository
import com.egr.squashgo.core.domain.repository.PlayerRepository
import com.egr.squashgo.core.model.Match
import com.egr.squashgo.core.model.MatchStatus
import com.egr.squashgo.feature.activity.impl.model.MatchHistoryError
import com.egr.squashgo.feature.activity.impl.model.MatchHistoryUiState
import com.egr.squashgo.feature.activity.impl.model.MatchRow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MatchHistoryViewModel @Inject constructor(
    private val matchRepository: MatchRepository,
    private val playerRepository: PlayerRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow<MatchHistoryUiState>(MatchHistoryUiState.Loading)
    val uiState: StateFlow<MatchHistoryUiState> = _uiState

    init {
        load()
    }

    fun refresh() {
        load()
    }

    private fun load() {
        val userId = sessionManager.currentUserId
        if (userId.isNullOrBlank()) {
            _uiState.value = MatchHistoryUiState.Error(MatchHistoryError.NotAuthenticated)
            return
        }

        viewModelScope.launch {
            _uiState.value = MatchHistoryUiState.Loading
            try {
                val confirmed = matchRepository.getMatchesForPlayer(userId)
                    .filter { it.status == MatchStatus.CONFIRMED }
                    .sortedByDescending { it.playedAt ?: it.confirmedAt ?: it.createdAt }

                if (confirmed.isEmpty()) {
                    _uiState.value = MatchHistoryUiState.Empty
                    return@launch
                }

                val opponentIds = confirmed
                    .map { opponentIdFor(it, userId) }
                    .distinct()
                val opponentNamesById = if (opponentIds.isEmpty()) {
                    emptyMap()
                } else {
                    playerRepository.getPlayersWithRatings(opponentIds)
                        .associate { it.player.id to it.player.displayName }
                }

                val rows = confirmed.map { match ->
                    buildRow(
                        match = match,
                        userId = userId,
                        opponentName = opponentNamesById[opponentIdFor(match, userId)],
                    )
                }
                _uiState.value = MatchHistoryUiState.Ready(rows = rows)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                Log.e(TAG, "load match history failed", e)
                _uiState.value = MatchHistoryUiState.Error(MatchHistoryError.Network)
            }
        }
    }

    private fun opponentIdFor(match: Match, userId: String): String =
        if (match.playerAId == userId) match.playerBId else match.playerAId

    private fun buildRow(match: Match, userId: String, opponentName: String?): MatchRow {
        val isPlayerA = match.playerAId == userId
        val didWin = match.winnerId == userId
        val (userGames, opponentGames) = gamesWon(match, isPlayerA)
        return MatchRow(
            matchId = match.id,
            opponentName = opponentName,
            didWin = didWin,
            userGames = userGames,
            opponentGames = opponentGames,
            playedAt = match.playedAt ?: match.confirmedAt,
        )
    }

    private fun gamesWon(match: Match, isPlayerA: Boolean): Pair<Int, Int> {
        val games = match.score?.games ?: return 0 to 0
        var aWins = 0
        var bWins = 0
        games.forEach { game ->
            when {
                game.playerAScore > game.playerBScore -> aWins++
                game.playerBScore > game.playerAScore -> bWins++
            }
        }
        return if (isPlayerA) aWins to bWins else bWins to aWins
    }

    private companion object {
        const val TAG = "MatchHistoryVM"
    }
}
