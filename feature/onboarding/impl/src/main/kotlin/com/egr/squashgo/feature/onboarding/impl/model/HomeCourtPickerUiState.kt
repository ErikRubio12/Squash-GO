package com.egr.squashgo.feature.onboarding.impl.model

import com.egr.squashgo.core.model.Court

sealed interface HomeCourtPickerUiState {
    data object Loading : HomeCourtPickerUiState
    data class Ready(
        val filteredCourts: List<Court>,
        val selectedCourtIds: List<String>,
        val primaryCourtId: String?,
        val isSaving: Boolean,
        val validationError: HomeCourtPickerError?,
    ) : HomeCourtPickerUiState
    data object Saved : HomeCourtPickerUiState
    data object AlreadyComplete : HomeCourtPickerUiState
    data class Error(val error: HomeCourtPickerError) : HomeCourtPickerUiState
}

enum class HomeCourtPickerError {
    MinNotMet,
    Network,
    NotAuthenticated,
}
