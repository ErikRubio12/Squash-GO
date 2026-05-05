package com.egr.squashgo.ui

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.egr.squashgo.core.designsystem.theme.SquashGoTheme
import com.egr.squashgo.feature.discover.api.CourtListRoute
import com.egr.squashgo.feature.discover.impl.navigation.discoverGraph
import com.egr.squashgo.feature.activity.impl.navigation.activityGraph
import com.egr.squashgo.feature.onboarding.api.HomeCourtPickerRoute
import com.egr.squashgo.feature.onboarding.api.LoginRoute
import com.egr.squashgo.feature.onboarding.api.ProfileSetupRoute
import com.egr.squashgo.feature.onboarding.impl.navigation.onboardingGraph
import com.egr.squashgo.feature.play.impl.navigation.playGraph
import com.egr.squashgo.feature.profile.impl.navigation.profileGraph
import com.egr.squashgo.feature.shell.impl.AppBootstrapViewModel
import com.egr.squashgo.feature.shell.impl.MainShellScaffold
import com.egr.squashgo.feature.shell.impl.model.BootstrapState

@Composable
fun SquashGoApp(
    deepLinkUri: Uri? = null,
    bootstrapViewModel: AppBootstrapViewModel = hiltViewModel(),
) {
    SquashGoTheme {
        val bootstrapState by bootstrapViewModel.state.collectAsStateWithLifecycle()

        when (bootstrapState) {
            BootstrapState.Loading -> LoadingScreen()
            BootstrapState.Ready -> AppNavGraph(
                deepLinkUri = deepLinkUri,
                startDestination = CourtListRoute,
                onLogout = bootstrapViewModel::logout,
            )
            BootstrapState.NeedsOnboarding -> AppNavGraph(
                deepLinkUri = deepLinkUri,
                startDestination = ProfileSetupRoute,
                onLogout = bootstrapViewModel::logout,
            )
            BootstrapState.NeedsLogin -> AppNavGraph(
                deepLinkUri = deepLinkUri,
                startDestination = LoginRoute,
                onLogout = bootstrapViewModel::logout,
            )
        }
    }
}

@Composable
private fun AppNavGraph(
    deepLinkUri: Uri?,
    startDestination: Any,
    onLogout: () -> Unit,
) {
    val navController = rememberNavController()
    MainShellScaffold(navController = navController) { padding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(padding),
        ) {
            onboardingGraph(
                navController = navController,
                deepLinkUri = deepLinkUri,
                onOnboardingComplete = {
                    navController.navigate(CourtListRoute) {
                        popUpTo(HomeCourtPickerRoute) { inclusive = true }
                    }
                },
            )

            discoverGraph(navController)
            playGraph(navController)
            profileGraph(onLogout = onLogout)
            activityGraph(navController)
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
