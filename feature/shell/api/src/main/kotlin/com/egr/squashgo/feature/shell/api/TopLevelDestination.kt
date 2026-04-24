package com.egr.squashgo.feature.shell.api

sealed interface TopLevelDestination {
    data object Discover : TopLevelDestination
    data object Play : TopLevelDestination
    data object Activity : TopLevelDestination
    data object Profile : TopLevelDestination
}
