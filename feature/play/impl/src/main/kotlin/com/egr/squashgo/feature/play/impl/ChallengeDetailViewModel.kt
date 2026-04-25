package com.egr.squashgo.feature.play.impl

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.egr.squashgo.core.auth.SessionManager
import com.egr.squashgo.core.domain.repository.ChallengeRepository
import com.egr.squashgo.core.domain.repository.PlayerRepository
import com.egr.squashgo.core.model.Challenge
import com.egr.squashgo.feature.play.impl.model.ChallengeDetailError
import com.egr.squashgo.feature.play.impl.model.ChallengeDetailUiState
import com.egr.squashgo.feature.play.impl.model.ExpiryDisplay
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import javax.inject.Inject

@HiltViewModel
class ChallengeDetailViewModel @Inject constructor(
    private val challengeRepository: ChallengeRepository,
    private val playerRepository: PlayerRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChallengeDetailUiState>(ChallengeDetailUiState.Loading)
    val uiState: StateFlow<ChallengeDetailUiState> = _uiState

    private var loadedChallengeId: String? = null

    fun load(challengeId: String) {
        if (loadedChallengeId == challengeId && _uiState.value is ChallengeDetailUiState.Ready) return
        loadedChallengeId = challengeId

        val userId = sessionManager.currentUserId
        if (userId.isNullOrBlank()) {
            _uiState.value = ChallengeDetailUiState.Error(ChallengeDetailError.NotAuthenticated)
            return
        }

        viewModelScope.launch {
            _uiState.value = ChallengeDetailUiState.Loading
            try {
                val incoming = challengeRepository.getIncomingChallenges(userId)
                val challenge = incoming.firstOrNull { it.id == challengeId }
                    ?: run {
                        _uiState.value = ChallengeDetailUiState.Error(ChallengeDetailError.NotFound)
                        return@launch
                    }
                val challenger = playerRepository.getPlayersWithRatings(listOf(challenge.challengerId))
                    .firstOrNull()
                if (challenger == null) {
                    _uiState.value = ChallengeDetailUiState.Error(ChallengeDetailError.Network)
                    return@launch
                }
                _uiState.value = ChallengeDetailUiState.Ready(
                    challenge = challenge,
                    challenger = challenger,
                    expiry = computeExpiry(challenge),
                    isActing = false,
                    actionError = false,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                Log.e(TAG, "load challenge detail failed", e)
                _uiState.value = ChallengeDetailUiState.Error(ChallengeDetailError.Network)
            }
        }
    }

    fun retry() {
        val id = loadedChallengeId ?: return
        loadedChallengeId = null
        load(id)
    }

    fun accept() = act { id -> challengeRepository.acceptChallenge(id) }

    fun decline() = act { id -> challengeRepository.declineChallenge(id) }

    private fun act(action: suspend (String) -> Unit) {
        val current = _uiState.value as? ChallengeDetailUiState.Ready ?: return
        if (current.isActing) return
        viewModelScope.launch {
            _uiState.update { current.copy(isActing = true, actionError = false) }
            try {
                action(current.challenge.id)
                _uiState.value = ChallengeDetailUiState.Done
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                Log.e(TAG, "challenge action failed", e)
                _uiState.update {
                    (_uiState.value as? ChallengeDetailUiState.Ready ?: current).copy(
                        isActing = false,
                        actionError = true,
                    )
                }
            }
        }
    }

    private fun computeExpiry(challenge: Challenge): ExpiryDisplay {
        val remaining = challenge.expiresAt - Clock.System.now()
        val totalMinutes = remaining.inWholeMinutes
        return when {
            totalMinutes <= 0L -> ExpiryDisplay.WithinHour
            totalMinutes < 60L -> ExpiryDisplay.WithinHour
            totalMinutes < 24L * 60L -> ExpiryDisplay.Hours((totalMinutes / 60L).toInt())
            else -> ExpiryDisplay.Days((totalMinutes / (60L * 24L)).toInt())
        }
    }

    private companion object {
        const val TAG = "ChallengeDetailVM"
    }
}

