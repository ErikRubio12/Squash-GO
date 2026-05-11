package com.egr.squashgo.feature.shell.impl

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.egr.squashgo.core.auth.FcmTokenProvider
import com.egr.squashgo.core.auth.SessionManager
import com.egr.squashgo.core.domain.repository.AuthRepository
import com.egr.squashgo.core.domain.repository.PushTokenRepository
import com.egr.squashgo.feature.shell.impl.model.BootstrapState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class AppBootstrapViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val authRepository: AuthRepository,
    private val pushTokenRepository: PushTokenRepository,
    private val fcmTokenProvider: FcmTokenProvider,
) : ViewModel() {

    private val _state = MutableStateFlow<BootstrapState>(BootstrapState.Loading)
    val state: StateFlow<BootstrapState> = _state.asStateFlow()

    init {
        bootstrap()
    }

    fun logout() {
        viewModelScope.launch {
            // DELETE the device token BEFORE clearing the session — once the
            // session is gone we lose the auth context the RLS policy needs.
            try {
                fcmTokenProvider.currentToken()?.let { token ->
                    pushTokenRepository.deleteByToken(token)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                Log.w(TAG, "push_token delete on logout failed", e)
            }
            sessionManager.clearSession()
            _state.value = BootstrapState.NeedsLogin
        }
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

    private fun loggedInState(): BootstrapState {
        val state = if (sessionManager.isOnboarded) {
            BootstrapState.Ready
        } else {
            BootstrapState.NeedsOnboarding
        }
        registerPushTokenIfPossible()
        return state
    }

    // Fire-and-forget. Failure to register a push token is not a blocker for
    // anything else in the app, so we don't propagate or surface errors here.
    private fun registerPushTokenIfPossible() {
        val playerId = sessionManager.currentUserId ?: return
        viewModelScope.launch {
            try {
                val token = fcmTokenProvider.currentToken() ?: return@launch
                pushTokenRepository.upsert(
                    playerId = playerId,
                    token = token,
                    platform = "android",
                    locale = Locale.getDefault().language,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                Log.w(TAG, "push_token upsert failed", e)
            }
        }
    }

    private companion object {
        const val TAG = "AppBootstrap"
    }
}