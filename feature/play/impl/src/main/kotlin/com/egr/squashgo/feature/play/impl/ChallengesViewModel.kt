package com.egr.squashgo.feature.play.impl

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.egr.squashgo.core.auth.SessionManager
import com.egr.squashgo.core.domain.repository.ChallengeRepository
import com.egr.squashgo.core.domain.repository.PlayerRepository
import com.egr.squashgo.feature.play.impl.model.ChallengeCard
import com.egr.squashgo.feature.play.impl.model.ChallengesError
import com.egr.squashgo.feature.play.impl.model.ChallengesUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChallengesViewModel @Inject constructor(
    private val challengeRepository: ChallengeRepository,
    private val playerRepository: PlayerRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChallengesUiState>(ChallengesUiState.Loading)
    val uiState: StateFlow<ChallengesUiState> = _uiState

    init {
        load()
    }

    fun refresh() {
        load()
    }

    fun cancelChallenge(challengeId: String) {
        val current = _uiState.value as? ChallengesUiState.Ready ?: return
        if (current.cancellingId != null) return

        viewModelScope.launch {
            _uiState.update { current.copy(cancellingId = challengeId, cancelError = false) }
            try {
                challengeRepository.cancelChallenge(challengeId)
                val next = current.copy(
                    outgoing = current.outgoing.filterNot { it.challenge.id == challengeId },
                    cancellingId = null,
                    cancelError = false,
                )
                _uiState.value = next
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                Log.e(TAG, "cancelChallenge failed", e)
                _uiState.update {
                    (_uiState.value as? ChallengesUiState.Ready ?: current).copy(
                        cancellingId = null,
                        cancelError = true,
                    )
                }
            }
        }
    }

    fun dismissCancelError() {
        val current = _uiState.value as? ChallengesUiState.Ready ?: return
        if (!current.cancelError) return
        _uiState.update { current.copy(cancelError = false) }
    }

    private fun load() {
        val userId = sessionManager.currentUserId
        if (userId.isNullOrBlank()) {
            _uiState.value = ChallengesUiState.Error(ChallengesError.NotAuthenticated)
            return
        }

        viewModelScope.launch {
            _uiState.value = ChallengesUiState.Loading
            try {
                val (incoming, outgoing) = coroutineScope {
                    val incomingDeferred = async { challengeRepository.getIncomingChallenges(userId) }
                    val outgoingDeferred = async { challengeRepository.getOutgoingChallenges(userId) }
                    incomingDeferred.await() to outgoingDeferred.await()
                }

                val otherIds = buildSet {
                    incoming.forEach { add(it.challengerId) }
                    outgoing.forEach { add(it.challengedId) }
                }.toList()

                val playersById = if (otherIds.isEmpty()) {
                    emptyMap()
                } else {
                    playerRepository.getPlayersWithRatings(otherIds)
                        .associateBy { it.player.id }
                }

                _uiState.value = ChallengesUiState.Ready(
                    incoming = incoming.map { ChallengeCard(it, playersById[it.challengerId]) },
                    outgoing = outgoing.map { ChallengeCard(it, playersById[it.challengedId]) },
                    cancellingId = null,
                    cancelError = false,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                Log.e(TAG, "load challenges failed", e)
                _uiState.value = ChallengesUiState.Error(ChallengesError.Network)
            }
        }
    }

    private companion object {
        const val TAG = "ChallengesVM"
    }
}
