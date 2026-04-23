package com.egr.squashgo.feature.onboarding.impl

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.egr.squashgo.core.auth.SessionManager
import com.egr.squashgo.core.domain.repository.PlayerRepository
import com.egr.squashgo.core.model.Player
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileSetupViewModel @Inject constructor(
    private val playerRepository: PlayerRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileSetupUiState>(ProfileSetupUiState.Loading)
    val uiState: StateFlow<ProfileSetupUiState> = _uiState

    private val _displayName = MutableStateFlow("")
    val displayName: StateFlow<String> = _displayName

    private var currentPlayer: Player? = null

    init {
        loadPlayer()
    }

    fun onDisplayNameChange(value: String) {
        _displayName.value = value
        if (_uiState.value is ProfileSetupUiState.Error) {
            _uiState.value = ProfileSetupUiState.Editing
        }
    }

    fun retryLoad() {
        loadPlayer()
    }

    fun save() {
        val player = currentPlayer ?: return
        val trimmed = _displayName.value.trim()
        if (trimmed.length !in DISPLAY_NAME_MIN..DISPLAY_NAME_MAX) {
            _uiState.value = ProfileSetupUiState.Error(ProfileSetupError.InvalidLength)
            return
        }

        viewModelScope.launch {
            _uiState.value = ProfileSetupUiState.Saving
            try {
                playerRepository.updatePlayer(player.copy(displayName = trimmed))
                _uiState.value = ProfileSetupUiState.Saved
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                Log.e(TAG, "updatePlayer failed", e)
                _uiState.value = ProfileSetupUiState.Error(ProfileSetupError.Network)
            }
        }
    }

    private fun loadPlayer() {
        val userId = sessionManager.currentUserId
        if (userId.isNullOrBlank()) {
            _uiState.value = ProfileSetupUiState.Error(ProfileSetupError.NotAuthenticated)
            return
        }

        viewModelScope.launch {
            _uiState.value = ProfileSetupUiState.Loading
            try {
                val player = playerRepository.getPlayer(userId)
                currentPlayer = player
                if (player.displayName.isNotBlank()) {
                    _uiState.value = ProfileSetupUiState.AlreadyComplete
                } else {
                    _displayName.value = ""
                    _uiState.value = ProfileSetupUiState.Editing
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                Log.e(TAG, "getPlayer failed", e)
                _uiState.value = ProfileSetupUiState.Error(ProfileSetupError.Network)
            }
        }
    }

    companion object {
        const val DISPLAY_NAME_MIN = 3
        const val DISPLAY_NAME_MAX = 20
        private const val TAG = "ProfileSetupViewModel"
    }
}

sealed interface ProfileSetupUiState {
    data object Loading : ProfileSetupUiState
    data object Editing : ProfileSetupUiState
    data object Saving : ProfileSetupUiState
    data object Saved : ProfileSetupUiState
    data object AlreadyComplete : ProfileSetupUiState
    data class Error(val error: ProfileSetupError) : ProfileSetupUiState
}

enum class ProfileSetupError {
    InvalidLength,
    Network,
    NotAuthenticated,
}