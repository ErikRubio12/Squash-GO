package com.egr.squashgo.feature.activity.impl.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.egr.squashgo.feature.activity.api.MatchHistoryRoute
import com.egr.squashgo.feature.activity.impl.MatchHistoryScreen

fun NavGraphBuilder.activityGraph(navController: NavController) {
    composable<MatchHistoryRoute> {
        MatchHistoryScreen(
            onBack = { navController.popBackStack() },
        )
    }
}
