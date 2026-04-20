package com.egr.squashgo.feature.discover.impl.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.egr.squashgo.feature.activity.api.MatchHistoryRoute
import com.egr.squashgo.feature.discover.api.CourtDetailRoute
import com.egr.squashgo.feature.discover.api.CourtListRoute
import com.egr.squashgo.feature.discover.impl.CourtDetailScreen
import com.egr.squashgo.feature.discover.impl.CourtListScreen
import com.egr.squashgo.feature.play.api.ChallengeCreateRoute
import com.egr.squashgo.feature.play.api.ChallengesRoute
import com.egr.squashgo.feature.profile.api.ProfileRoute

fun NavGraphBuilder.discoverGraph(navController: NavController) {
    composable<CourtListRoute> {
        CourtListScreen(
            onCourtClick = { courtId ->
                navController.navigate(CourtDetailRoute(courtId))
            },
            onProfileClick = {
                navController.navigate(ProfileRoute)
            },
            onChallengesClick = {
                navController.navigate(ChallengesRoute)
            },
            onHistoryClick = {
                navController.navigate(MatchHistoryRoute)
            },
        )
    }

    composable<CourtDetailRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<CourtDetailRoute>()
        CourtDetailScreen(
            courtId = route.courtId,
            onPlayerClick = { playerId ->
                navController.navigate(ChallengeCreateRoute(playerId, route.courtId))
            },
            onBack = { navController.popBackStack() },
        )
    }
}
