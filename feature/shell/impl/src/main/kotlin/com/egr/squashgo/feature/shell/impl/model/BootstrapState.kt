package com.egr.squashgo.feature.shell.impl.model

sealed interface BootstrapState {
    data object Loading : BootstrapState
    data object NeedsLogin : BootstrapState
    data object NeedsOnboarding : BootstrapState
    data object Ready : BootstrapState
}
