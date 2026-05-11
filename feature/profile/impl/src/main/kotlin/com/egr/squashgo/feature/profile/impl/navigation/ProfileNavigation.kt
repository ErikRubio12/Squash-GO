package com.egr.squashgo.feature.profile.impl.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.egr.squashgo.feature.profile.api.DesignSystemDemoRoute
import com.egr.squashgo.feature.profile.api.ProfileRoute
import com.egr.squashgo.feature.profile.impl.DesignSystemDemoScreen
import com.egr.squashgo.feature.profile.impl.ProfileScreen

fun NavGraphBuilder.profileGraph(
    navController: NavController,
    onLogout: () -> Unit,
) {
    composable<ProfileRoute> {
        ProfileScreen(
            onLogout = onLogout,
            onOpenDesignSystemDemo = { navController.navigate(DesignSystemDemoRoute) },
        )
    }
    composable<DesignSystemDemoRoute> {
        DesignSystemDemoScreen(
            onBack = { navController.popBackStack() },
        )
    }
}