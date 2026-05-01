package com.egr.squashgo.feature.play.impl

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.egr.squashgo.core.auth.SessionManager
import com.egr.squashgo.core.domain.repository.MatchRepository
import com.egr.squashgo.core.domain.repository.PlayerRepository
import com.egr.squashgo.core.model.MatchStatus
import com.egr.squashgo.feature.play.impl.model.MatchConfirmError
import com.egr.squashgo.feature.play.impl.model.MatchConfirmUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MatchConfirmViewModel @Inject constructor(
    private val matchRepository: MatchRepository,
    private val playerRepository: PlayerRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow<MatchConfirmUiState>(MatchConfirmUiState.Loading)
    val uiState: StateFlow<MatchConfirmUiState> = _uiState

    private var loadedMatchId: String? = null

    fun load(matchId: String) {
        if (loadedMatchId == matchId && _uiState.value !is MatchConfirmUiState.Error) return
        loadedMatchId = matchId

        val userId = sessionManager.currentUserId
        if (userId.isNullOrBlank()) {
            _uiState.value = MatchConfirmUiState.Error(MatchConfirmError.NotAuthenticated)
            return
        }

        viewModelScope.launch {
            _uiState.value = MatchConfirmUiState.Loading
            try {
                val match = matchRepository.getMatch(matchId)

                if (match.status != MatchStatus.RESULT_SUBMITTED) {
                    _uiState.value = MatchConfirmUiState.Error(MatchConfirmError.NothingToConfirm)
                    return@launch
                }

                val youAreA = match.playerAId == userId
                if (!youAreA && match.playerBId != userId) {
                    _uiState.value = MatchConfirmUiState.Error(MatchConfirmError.NotParticipant)
                    return@launch
                }

                if (match.submittedById == userId) {
                    _uiState.value = MatchConfirmUiState.Error(MatchConfirmError.OwnSubmission)
                    return@launch
                }

                val players = playerRepository
                    .getPlayersWithRatings(listOf(match.playerAId, match.playerBId))
                    .associateBy { it.player.id }

                val playerAName = players[match.playerAId]?.player?.displayName.orEmpty()
                val playerBName = players[match.playerBId]?.player?.displayName.orEmpty()
                val submitterName = match.submittedById
                    ?.let { players[it]?.player?.displayName }
                    .orEmpty()

                _uiState.value = MatchConfirmUiState.Ready(
                    match = match,
                    playerAName = playerAName,
                    playerBName = playerBName,
                    submitterName = submitterName,
                    youAreA = youAreA,
                    isConfirming = false,
                    confirmError = false,
                    isDisputing = false,
                    disputeError = false,
                    disputeDialogOpen = false,
                    disputeReason = "",
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                Log.e(TAG, "load match failed", e)
                _uiState.value = MatchConfirmUiState.Error(MatchConfirmError.Network)
            }
        }
    }

    fun retry() {
        val id = loadedMatchId ?: return
        loadedMatchId = null
        load(id)
    }

    fun confirm() {
        val current = _uiState.value as? MatchConfirmUiState.Ready ?: return
        if (current.isConfirming || current.isDisputing) return

        viewModelScope.launch {
            _uiState.update { current.copy(isConfirming = true, confirmError = false) }
            try {
                matchRepository.confirmMatch(current.match.id)
                _uiState.value = MatchConfirmUiState.Done
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                Log.e(TAG, "confirm match failed", e)
                _uiState.update {
                    (_uiState.value as? MatchConfirmUiState.Ready ?: current).copy(
                        isConfirming = false,
                        confirmError = true,
                    )
                }
            }
        }
    }

    fun openDisputeDialog() {
        val current = _uiState.value as? MatchConfirmUiState.Ready ?: return
        if (current.isConfirming || current.isDisputing) return
        _uiState.update {
            current.copy(disputeDialogOpen = true, disputeReason = "", disputeError = false)
        }
    }

    fun dismissDisputeDialog() {
        val current = _uiState.value as? MatchConfirmUiState.Ready ?: return
        if (current.isDisputing) return
        _uiState.update { current.copy(disputeDialogOpen = false) }
    }

    fun updateDisputeReason(reason: String) {
        val current = _uiState.value as? MatchConfirmUiState.Ready ?: return
        _uiState.update {
            current.copy(disputeReason = reason.take(MAX_REASON_LENGTH), disputeError = false)
        }
    }

    fun submitDispute() {
        val current = _uiState.value as? MatchConfirmUiState.Ready ?: return
        if (current.isDisputing) return
        val reason = current.disputeReason.trim()
        if (reason.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { current.copy(isDisputing = true, disputeError = false) }
            try {
                matchRepository.disputeMatch(current.match.id, reason)
                _uiState.value = MatchConfirmUiState.Done
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                Log.e(TAG, "dispute match failed", e)
                _uiState.update {
                    (_uiState.value as? MatchConfirmUiState.Ready ?: current).copy(
                        isDisputing = false,
                        disputeError = true,
                    )
                }
            }
        }
    }

    private companion object {
        const val TAG = "MatchConfirmVM"
        const val MAX_REASON_LENGTH = 240
    }
}
