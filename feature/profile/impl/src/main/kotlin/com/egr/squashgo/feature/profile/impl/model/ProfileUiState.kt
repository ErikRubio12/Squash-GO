package com.egr.squashgo.feature.profile.impl.model

data class CourtRow(
    val courtId: String,
    val name: String,
    val isPrimary: Boolean,
)

data class ProfileData(
    val displayName: String,
    val email: String,
    val tierLabel: String,
    val eloScore: Int,
    val rankedMatchesPlayed: Int,
    val isProvisional: Boolean,
    val courts: List<CourtRow>,
)

sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data class Ready(val data: ProfileData) : ProfileUiState
    data class Error(val kind: ProfileError) : ProfileUiState
}

enum class ProfileError {
    Network,
    NotAuthenticated,
}
