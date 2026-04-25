package com.egr.squashgo.feature.play.impl.model

import com.egr.squashgo.core.domain.repository.model.PlayerWithRating
import com.egr.squashgo.core.model.Challenge

data class ChallengeCard(
    val challenge: Challenge,
    val other: PlayerWithRating?,
)

sealed interface ChallengesUiState {
    data object Loading : ChallengesUiState
    data class Ready(
        val incoming: List<ChallengeCard>,
        val outgoing: List<ChallengeCard>,
        val cancellingId: String?,
        val cancelError: Boolean,
    ) : ChallengesUiState
    data class Error(val error: ChallengesError) : ChallengesUiState
}

enum class ChallengesError {
    Network,
    NotAuthenticated,
}
