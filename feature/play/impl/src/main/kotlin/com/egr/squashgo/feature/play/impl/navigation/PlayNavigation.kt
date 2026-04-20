package com.egr.squashgo.feature.play.impl.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.egr.squashgo.feature.play.api.ChallengeCreateRoute
import com.egr.squashgo.feature.play.api.ChallengesRoute
import com.egr.squashgo.feature.play.api.MatchConfirmRoute
import com.egr.squashgo.feature.play.api.MatchResultRoute
import com.egr.squashgo.feature.play.impl.ChallengeCreateScreen
import com.egr.squashgo.feature.play.impl.ChallengesScreen
import com.egr.squashgo.feature.play.impl.MatchConfirmScreen
import com.egr.squashgo.feature.play.impl.MatchResultScreen

fun NavGraphBuilder.playGraph(navController: NavController) {
    composable<ChallengeCreateRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<ChallengeCreateRoute>()
        ChallengeCreateScreen(
            challengedId = route.challengedId,
            courtId = route.courtId,
            onChallengeSent = { navController.popBackStack() },
            onBack = { navController.popBackStack() },
        )
    }

    composable<ChallengesRoute> {
        ChallengesScreen(
            onMatchResult = { matchId ->
                navController.navigate(MatchResultRoute(matchId))
            },
            onMatchConfirm = { matchId ->
                navController.navigate(MatchConfirmRoute(matchId))
            },
            onBack = { navController.popBackStack() },
        )
    }

    composable<MatchResultRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<MatchResultRoute>()
        MatchResultScreen(
            matchId = route.matchId,
            onResultSubmitted = { navController.popBackStack() },
            onBack = { navController.popBackStack() },
        )
    }

    composable<MatchConfirmRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<MatchConfirmRoute>()
        MatchConfirmScreen(
            matchId = route.matchId,
            onConfirmed = { navController.popBackStack() },
            onBack = { navController.popBackStack() },
        )
    }
}
