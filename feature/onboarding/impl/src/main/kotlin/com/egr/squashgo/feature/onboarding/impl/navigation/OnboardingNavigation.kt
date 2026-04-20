package com.egr.squashgo.feature.onboarding.impl.navigation

import android.net.Uri
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.egr.squashgo.feature.onboarding.api.LoginRoute
import com.egr.squashgo.feature.onboarding.impl.LoginScreen

fun NavGraphBuilder.onboardingGraph(
    deepLinkUri: Uri?,
    onLoginSuccess: () -> Unit,
) {
    composable<LoginRoute> {
        LoginScreen(
            deepLinkUri = deepLinkUri,
            onLoginSuccess = onLoginSuccess,
        )
    }
}
