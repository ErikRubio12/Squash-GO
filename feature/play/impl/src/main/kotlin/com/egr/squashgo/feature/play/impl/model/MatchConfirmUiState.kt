package com.egr.squashgo.feature.play.impl.model

import com.egr.squashgo.core.model.Match

sealed interface MatchConfirmUiState {
    data object Loading : MatchConfirmUiState

    data class Ready(
        val match: Match,
        val playerAName: String,
        val playerBName: String,
        val submitterName: String,
        val youAreA: Boolean,
        val isConfirming: Boolean,
        val confirmError: Boolean,
        val isDisputing: Boolean,
        val disputeError: Boolean,
        val disputeDialogOpen: Boolean,
        val disputeReason: String,
    ) : MatchConfirmUiState

    data object Done : MatchConfirmUiState

    data class Error(val error: MatchConfirmError) : MatchConfirmUiState
}

enum class MatchConfirmError {
    Network,
    NotAuthenticated,
    NotParticipant,
    NothingToConfirm,
    OwnSubmission,
}
