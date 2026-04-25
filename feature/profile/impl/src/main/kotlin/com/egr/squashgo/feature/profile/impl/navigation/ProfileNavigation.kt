package com.egr.squashgo.feature.profile.impl.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.egr.squashgo.feature.profile.api.ProfileRoute
import com.egr.squashgo.feature.profile.impl.ProfileScreen

fun NavGraphBuilder.profileGraph(navController: NavController) {
    composable<ProfileRoute> {
        ProfileScreen()
    }
}
