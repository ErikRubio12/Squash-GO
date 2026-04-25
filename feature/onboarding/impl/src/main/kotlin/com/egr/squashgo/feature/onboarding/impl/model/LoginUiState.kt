package com.egr.squashgo.feature.onboarding.impl.model

sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object SendingLink : LoginUiState
    data object LinkSent : LoginUiState
    data object Verifying : LoginUiState
    data object Success : LoginUiState
    data class Error(val error: LoginError) : LoginUiState
}

enum class LoginError {
    InvalidEmail,
    Network,
    RateLimited,
    Server,
    Unknown,
}
