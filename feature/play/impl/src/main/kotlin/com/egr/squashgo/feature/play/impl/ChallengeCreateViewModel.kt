package com.egr.squashgo.feature.play.impl

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.egr.squashgo.core.auth.SessionManager
import com.egr.squashgo.core.domain.repository.ChallengeRepository
import com.egr.squashgo.core.domain.repository.PlayerRepository
import com.egr.squashgo.core.model.MatchType
import com.egr.squashgo.feature.play.impl.model.ChallengeCreateError
import com.egr.squashgo.feature.play.impl.model.ChallengeCreateUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChallengeCreateViewModel @Inject constructor(
    private val playerRepository: PlayerRepository,
    private val challengeRepository: ChallengeRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChallengeCreateUiState>(ChallengeCreateUiState.Loading)
    val uiState: StateFlow<ChallengeCreateUiState> = _uiState

    private var loadedKey: String? = null

    fun load(challengedId: String, courtId: String?) {
        val key = "$challengedId|${courtId.orEmpty()}"
        if (loadedKey == key && _uiState.value is ChallengeCreateUiState.Ready) return
        loadedKey = key

        val userId = sessionManager.currentUserId
        if (userId.isNullOrBlank()) {
            _uiState.value = ChallengeCreateUiState.Error(ChallengeCreateError.NotAuthenticated)
            return
        }

        viewModelScope.launch {
            _uiState.value = ChallengeCreateUiState.Loading
            try {
                val results = playerRepository.getPlayersWithRatings(listOf(userId, challengedId))
                val me = results.firstOrNull { it.player.id == userId }
                val opponent = results.firstOrNull { it.player.id == challengedId }
                if (me == null || opponent == null) {
                    _uiState.value = ChallengeCreateUiState.Error(ChallengeCreateError.Network)
                    return@launch
                }
                _uiState.value = ChallengeCreateUiState.Ready(
                    me = me,
                    opponent = opponent,
                    courtId = courtId,
                    matchType = MatchType.CASUAL,
                    message = "",
                    isSending = false,
                    error = null,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                Log.e(TAG, "load challenge participants failed", e)
                _uiState.value = ChallengeCreateUiState.Error(ChallengeCreateError.Network)
            }
        }
    }

    fun retry() {
        val key = loadedKey ?: return
        val (challengedId, courtIdPart) = key.split("|", limit = 2).let { it[0] to it[1] }
        loadedKey = null
        load(challengedId, courtIdPart.ifEmpty { null })
    }

    fun onMatchTypeChange(matchType: MatchType) {
        val current = _uiState.value as? ChallengeCreateUiState.Ready ?: return
        _uiState.update { current.copy(matchType = matchType, error = null) }
    }

    fun onMessageChange(value: String) {
        val current = _uiState.value as? ChallengeCreateUiState.Ready ?: return
        val trimmed = if (value.length > MAX_MESSAGE_LENGTH) value.take(MAX_MESSAGE_LENGTH) else value
        _uiState.update { current.copy(message = trimmed, error = null) }
    }

    fun send() {
        val current = _uiState.value as? ChallengeCreateUiState.Ready ?: return
        if (current.isSending) return

        viewModelScope.launch {
            _uiState.value = current.copy(isSending = true, error = null)
            try {
                challengeRepository.sendChallenge(
                    challengedId = current.opponent.player.id,
                    courtId = current.courtId,
                    matchType = current.matchType,
                    message = current.message.trim().ifBlank { null },
                )
                _uiState.value = ChallengeCreateUiState.Sent
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                Log.e(TAG, "sendChallenge failed", e)
                _uiState.value = current.copy(
                    isSending = false,
                    error = ChallengeCreateError.Network,
                )
            }
        }
    }

    companion object {
        const val MAX_MESSAGE_LENGTH = 200
        private const val TAG = "ChallengeCreateVM"
    }
}

