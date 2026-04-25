package com.egr.squashgo.ui.model

sealed interface BootstrapState {
    data object Loading : BootstrapState
    data object NeedsLogin : BootstrapState
    data object NeedsOnboarding : BootstrapState
    data object Ready : BootstrapState
}
