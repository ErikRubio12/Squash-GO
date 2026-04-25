package com.egr.squashgo.feature.discover.impl.model

import com.egr.squashgo.core.model.Court

sealed interface CourtListUiState {
    data object Loading : CourtListUiState
    data object Empty : CourtListUiState
    data class Success(val courts: List<Court>) : CourtListUiState
    data object Error : CourtListUiState
}
