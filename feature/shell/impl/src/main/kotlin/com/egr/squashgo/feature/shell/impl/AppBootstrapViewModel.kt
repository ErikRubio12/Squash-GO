package com.egr.squashgo.feature.shell.impl

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.egr.squashgo.core.auth.SessionManager
import com.egr.squashgo.core.domain.repository.AuthRepository
import com.egr.squashgo.feature.shell.impl.model.BootstrapState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppBootstrapViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<BootstrapState>(BootstrapState.Loading)
    val state: StateFlow<BootstrapState> = _state.asStateFlow()

    init {
        bootstrap()
    }

    fun logout() {
        sessionManager.clearSession()
        _state.value = BootstrapState.NeedsLogin
    }

    private fun bootstrap() {
        val refreshToken = sessionManager.refreshToken
        if (refreshToken.isNullOrBlank()) {
            _state.value = BootstrapState.NeedsLogin
            return
        }

        if (!sessionManager.isAccessTokenExpired()) {
            _state.value = loggedInState()
            return
        }

        viewModelScope.launch {
            try {
                val session = authRepository.refreshToken(refreshToken)
                sessionManager.updateSession(
                    accessToken = session.accessToken,
                    refreshToken = session.refreshToken,
                    userId = session.userId,
                    expiresInSeconds = session.expiresIn,
                )
                _state.value = loggedInState()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                Log.w(TAG, "refreshToken failed; clearing session", e)
                sessionManager.clearSession()
                _state.value = BootstrapState.NeedsLogin
            }
        }
    }

    private fun loggedInState(): BootstrapState =
        if (sessionManager.isOnboarded) BootstrapState.Ready else BootstrapState.NeedsOnboarding

    private companion object {
        const val TAG = "AppBootstrap"
    }
}