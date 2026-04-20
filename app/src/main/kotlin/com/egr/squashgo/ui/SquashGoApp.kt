package com.egr.squashgo.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.egr.squashgo.core.designsystem.theme.SquashGoTheme
import com.egr.squashgo.feature.discover.api.CourtListRoute
import com.egr.squashgo.feature.discover.impl.navigation.discoverGraph
import com.egr.squashgo.feature.activity.impl.navigation.activityGraph
import com.egr.squashgo.feature.onboarding.api.LoginRoute
import com.egr.squashgo.feature.onboarding.impl.navigation.onboardingGraph
import com.egr.squashgo.feature.play.impl.navigation.playGraph
import com.egr.squashgo.feature.profile.impl.navigation.profileGraph

@Composable
fun SquashGoApp(deepLinkUri: Uri? = null) {
    SquashGoTheme {
        val navController = rememberNavController()

        NavHost(
            navController = navController,
            startDestination = LoginRoute,
        ) {
            onboardingGraph(
                deepLinkUri = deepLinkUri,
                onLoginSuccess = {
                    navController.navigate(CourtListRoute) {
                        popUpTo(LoginRoute) { inclusive = true }
                    }
                },
            )

            discoverGraph(navController)
            playGraph(navController)
            profileGraph(navController)
            activityGraph(navController)
        }
    }
}
