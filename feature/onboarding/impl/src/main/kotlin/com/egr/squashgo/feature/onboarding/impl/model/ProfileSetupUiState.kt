package com.egr.squashgo.feature.onboarding.impl.model

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
