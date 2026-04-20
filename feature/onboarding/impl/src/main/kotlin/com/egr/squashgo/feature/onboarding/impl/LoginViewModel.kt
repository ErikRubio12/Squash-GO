package com.egr.squashgo.feature.onboarding.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.egr.squashgo.core.auth.SessionManager
import com.egr.squashgo.core.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email

    fun onEmailChange(value: String) {
        _email.value = value
    }

    fun sendMagicLink() {
        val emailValue = _email.value.trim()
        if (emailValue.isBlank()) return

        viewModelScope.launch {
            _uiState.value = LoginUiState.SendingLink
            try {
                authRepository.sendMagicLink(emailValue, REDIRECT_URL)
                _uiState.value = LoginUiState.LinkSent
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error(e.message ?: "Failed to send magic link")
            }
        }
    }

    fun handleAccessToken(accessToken: String) {
        sessionManager.updateSession(accessToken, userId = "")
        // Decode user ID from JWT payload
        try {
            val payload = accessToken.split(".")[1]
            val decoded = android.util.Base64.decode(payload, android.util.Base64.URL_SAFE)
            val json = String(decoded)
            val sub = Regex("\"sub\"\\s*:\\s*\"([^\"]+)\"").find(json)?.groupValues?.get(1)
            if (sub != null) {
                sessionManager.updateSession(accessToken, sub)
            }
        } catch (_: Exception) {
            // Token is still set, userId will be empty
        }
        _uiState.value = LoginUiState.Success
    }

    companion object {
        const val REDIRECT_URL = "squashgo://auth-callback"
    }
}

sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object SendingLink : LoginUiState
    data object LinkSent : LoginUiState
    data object Verifying : LoginUiState
    data object Success : LoginUiState
    data class Error(val message: String) : LoginUiState
}
