package com.egr.squashgo.ui.model

sealed interface BootstrapState {
    data object Loading : BootstrapState
    data object LoggedIn : BootstrapState
    data object LoggedOut : BootstrapState
}
