package com.egr.squashgo.feature.play.impl.model

import com.egr.squashgo.core.domain.repository.model.PlayerWithRating
import com.egr.squashgo.core.model.Challenge

sealed interface ExpiryDisplay {
    data object WithinHour : ExpiryDisplay
    data class Hours(val count: Int) : ExpiryDisplay
    data class Days(val count: Int) : ExpiryDisplay
}

sealed interface ChallengeDetailUiState {
    data object Loading : ChallengeDetailUiState
    data class Ready(
        val challenge: Challenge,
        val challenger: PlayerWithRating,
        val expiry: ExpiryDisplay,
        val isActing: Boolean,
        val actionError: Boolean,
    ) : ChallengeDetailUiState
    data object Done : ChallengeDetailUiState
    data class Error(val error: ChallengeDetailError) : ChallengeDetailUiState
}

enum class ChallengeDetailError {
    Network,
    NotFound,
    NotAuthenticated,
}
