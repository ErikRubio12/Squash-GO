package com.egr.squashgo.feature.discover.impl.model

import com.egr.squashgo.core.domain.repository.model.PlayerWithRating
import com.egr.squashgo.core.model.Court

sealed interface CourtDetailUiState {
    data object Loading : CourtDetailUiState
    data class Ready(
        val court: Court,
        val players: List<PlayerWithRating>,
    ) : CourtDetailUiState
    data object Error : CourtDetailUiState
}
