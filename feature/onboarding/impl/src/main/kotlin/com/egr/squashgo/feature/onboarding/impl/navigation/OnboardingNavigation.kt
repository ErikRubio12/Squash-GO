package com.egr.squashgo.feature.onboarding.impl.navigation

import android.net.Uri
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.egr.squashgo.feature.onboarding.api.HomeCourtPickerRoute
import com.egr.squashgo.feature.onboarding.api.LoginRoute
import com.egr.squashgo.feature.onboarding.api.ProfileSetupRoute
import com.egr.squashgo.feature.onboarding.impl.HomeCourtPickerScreen
import com.egr.squashgo.feature.onboarding.impl.LoginScreen
import com.egr.squashgo.feature.onboarding.impl.ProfileSetupScreen

fun NavGraphBuilder.onboardingGraph(
    navController: NavController,
    deepLinkUri: Uri?,
    onOnboardingComplete: () -> Unit,
) {
    composable<LoginRoute> {
        LoginScreen(
            deepLinkUri = deepLinkUri,
            onLoginSuccess = {
                navController.navigate(ProfileSetupRoute) {
                    popUpTo(LoginRoute) { inclusive = true }
                }
            },
        )
    }

    composable<ProfileSetupRoute> {
        ProfileSetupScreen(
            onProfileSaved = {
                navController.navigate(HomeCourtPickerRoute) {
                    popUpTo(ProfileSetupRoute) { inclusive = true }
                }
            },
        )
    }

    composable<HomeCourtPickerRoute> {
        HomeCourtPickerScreen(
            onCourtsSaved = onOnboardingComplete,
        )
    }
}
