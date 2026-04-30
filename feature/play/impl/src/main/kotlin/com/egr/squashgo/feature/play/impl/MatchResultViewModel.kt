package com.egr.squashgo.feature.play.impl

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.egr.squashgo.core.auth.SessionManager
import com.egr.squashgo.core.domain.match.ScoreValidator
import com.egr.squashgo.core.domain.repository.MatchRepository
import com.egr.squashgo.core.domain.repository.PlayerRepository
import com.egr.squashgo.core.model.MatchScore
import com.egr.squashgo.core.model.MatchStatus
import com.egr.squashgo.feature.play.impl.model.GameInput
import com.egr.squashgo.feature.play.impl.model.MatchResultError
import com.egr.squashgo.feature.play.impl.model.MatchResultUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MatchResultViewModel @Inject constructor(
    private val matchRepository: MatchRepository,
    private val playerRepository: PlayerRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow<MatchResultUiState>(MatchResultUiState.Loading)
    val uiState: StateFlow<MatchResultUiState> = _uiState

    private var loadedMatchId: String? = null

    fun load(matchId: String) {
        if (loadedMatchId == matchId && _uiState.value !is MatchResultUiState.Error) return
        loadedMatchId = matchId

        val userId = sessionManager.currentUserId
        if (userId.isNullOrBlank()) {
            _uiState.value = MatchResultUiState.Error(MatchResultError.NotAuthenticated)
            return
        }

        viewModelScope.launch {
            _uiState.value = MatchResultUiState.Loading
            try {
                val match = matchRepository.getMatch(matchId)

                if (match.status != MatchStatus.IN_PROGRESS) {
                    _uiState.value = MatchResultUiState.Error(MatchResultError.AlreadySubmitted)
                    return@launch
                }

                val youAreA = match.playerAId == userId
                if (!youAreA && match.playerBId != userId) {
                    _uiState.value = MatchResultUiState.Error(MatchResultError.NotParticipant)
                    return@launch
                }

                val players = playerRepository
                    .getPlayersWithRatings(listOf(match.playerAId, match.playerBId))
                    .associateBy { it.player.id }
                val opponentId = if (youAreA) match.playerBId else match.playerAId

                _uiState.value = MatchResultUiState.Ready(
                    match = match,
                    youName = players[userId]?.player?.displayName.orEmpty(),
                    opponentName = players[opponentId]?.player?.displayName.orEmpty(),
                    youAreA = youAreA,
                    games = List(MAX_GAMES) { GameInput() },
                    isSubmitting = false,
                    showInvalidScoreMessage = false,
                    submitError = false,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                Log.e(TAG, "load match failed", e)
                _uiState.value = MatchResultUiState.Error(MatchResultError.Network)
            }
        }
    }

    fun retry() {
        val id = loadedMatchId ?: return
        loadedMatchId = null
        load(id)
    }

    fun updateScore(index: Int, isPlayerA: Boolean, raw: String) {
        val current = _uiState.value as? MatchResultUiState.Ready ?: return
        if (index !in current.games.indices) return
        val sanitized = raw.filter(Char::isDigit).take(MAX_SCORE_DIGITS)
        val games = current.games.toMutableList()
        val existing = games[index]
        games[index] = if (isPlayerA) {
            existing.copy(playerAScore = sanitized)
        } else {
            existing.copy(playerBScore = sanitized)
        }
        _uiState.update {
            current.copy(
                games = games,
                showInvalidScoreMessage = false,
                submitError = false,
            )
        }
    }

    fun submit() {
        val current = _uiState.value as? MatchResultUiState.Ready ?: return
        if (current.isSubmitting) return

        val games = current.games
            .filterNot { it.isBlank }
            .mapNotNull { it.toGameScoreOrNull() }
        val score = MatchScore(games)

        val validation = ScoreValidator.validateMatchScore(score)
        if (validation is ScoreValidator.ValidationResult.Invalid) {
            _uiState.update { current.copy(showInvalidScoreMessage = true, submitError = false) }
            return
        }

        val winnerId = ScoreValidator.getWinnerId(
            score = score,
            playerAId = current.match.playerAId,
            playerBId = current.match.playerBId,
        )
        if (winnerId == null) {
            _uiState.update { current.copy(showInvalidScoreMessage = true, submitError = false) }
            return
        }

        viewModelScope.launch {
            _uiState.update { current.copy(isSubmitting = true, submitError = false) }
            try {
                matchRepository.submitResult(current.match.id, score, winnerId)
                _uiState.value = MatchResultUiState.Done
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                Log.e(TAG, "submit match result failed", e)
                _uiState.update {
                    (_uiState.value as? MatchResultUiState.Ready ?: current).copy(
                        isSubmitting = false,
                        submitError = true,
                    )
                }
            }
        }
    }

    private companion object {
        const val TAG = "MatchResultVM"
        const val MAX_GAMES = 5
        const val MAX_SCORE_DIGITS = 2
    }
}
