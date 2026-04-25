package com.egr.squashgo.feature.play.impl.model

import com.egr.squashgo.core.domain.repository.model.PlayerWithRating
import com.egr.squashgo.core.model.MatchType

sealed interface ChallengeCreateUiState {
    data object Loading : ChallengeCreateUiState
    data class Ready(
        val me: PlayerWithRating,
        val opponent: PlayerWithRating,
        val courtId: String?,
        val matchType: MatchType,
        val message: String,
        val isSending: Boolean,
        val error: ChallengeCreateError?,
    ) : ChallengeCreateUiState
    data object Sent : ChallengeCreateUiState
    data class Error(val error: ChallengeCreateError) : ChallengeCreateUiState
}

enum class ChallengeCreateError {
    Network,
    NotAuthenticated,
}
