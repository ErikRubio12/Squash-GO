package com.egr.squashgo.feature.onboarding.impl

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.egr.squashgo.core.auth.SessionManager
import com.egr.squashgo.core.domain.exception.AuthException
import com.egr.squashgo.core.domain.repository.AuthRepository
import com.egr.squashgo.feature.onboarding.impl.model.LoginError
import com.egr.squashgo.feature.onboarding.impl.model.LoginUiState
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
        if (_uiState.value is LoginUiState.Error) {
            _uiState.value = LoginUiState.Idle
        }
    }

    fun sendMagicLink() {
        val emailValue = _email.value.trim()
        if (emailValue.isBlank()) return
        if (!EMAIL_REGEX.matches(emailValue)) {
            _uiState.value = LoginUiState.Error(LoginError.InvalidEmail)
            return
        }

        viewModelScope.launch {
            _uiState.value = LoginUiState.SendingLink
            try {
                authRepository.sendMagicLink(emailValue, REDIRECT_URL)
                _uiState.value = LoginUiState.LinkSent
            } catch (e: AuthException) {
                Log.e(TAG, "sendMagicLink failed", e)
                _uiState.value = LoginUiState.Error(e.toLoginError())
            }
        }
    }

    fun handleDeepLinkTokens(
        accessToken: String,
        refreshToken: String,
        expiresIn: Long,
    ) {
        val userId = extractUserIdFromJwt(accessToken).orEmpty()
        sessionManager.updateSession(
            accessToken = accessToken,
            refreshToken = refreshToken,
            userId = userId,
            expiresInSeconds = expiresIn,
        )
        _uiState.value = LoginUiState.Success
    }

    private fun extractUserIdFromJwt(accessToken: String): String? = try {
        val payload = accessToken.split(".")[1]
        val decoded = android.util.Base64.decode(payload, android.util.Base64.URL_SAFE)
        val json = String(decoded)
        Regex("\"sub\"\\s*:\\s*\"([^\"]+)\"").find(json)?.groupValues?.get(1)
    } catch (_: Exception) {
        null
    }

    private fun AuthException.toLoginError(): LoginError = when (this) {
        is AuthException.InvalidEmail -> LoginError.InvalidEmail
        is AuthException.RateLimited -> LoginError.RateLimited
        is AuthException.Network -> LoginError.Network
        is AuthException.Server -> LoginError.Server
        is AuthException.Unknown -> LoginError.Unknown
    }

    companion object {
        const val REDIRECT_URL = "squashgo://auth-callback"
        private const val TAG = "LoginViewModel"
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    }
}

