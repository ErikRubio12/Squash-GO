package com.egr.squashgo.feature.shell.impl

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.egr.squashgo.feature.activity.api.MatchHistoryRoute
import com.egr.squashgo.feature.discover.api.CourtListRoute
import com.egr.squashgo.feature.play.api.ChallengesRoute
import com.egr.squashgo.feature.profile.api.ProfileRoute
import com.egr.squashgo.feature.shell.api.TopLevelDestination

@Composable
fun MainShellScaffold(
    navController: NavController,
    content: @Composable (PaddingValues) -> Unit,
) {
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val selectedDestination = currentBackStackEntry?.destination?.toTopLevelDestination()

    Scaffold(
        bottomBar = {
            if (selectedDestination != null) {
                NavigationBar {
                    TopLevelTabs.forEach { tab ->
                        NavigationBarItem(
                            selected = tab.destination == selectedDestination,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = stringResource(tab.contentDescriptionRes),
                                )
                            },
                            label = { Text(stringResource(tab.labelRes)) },
                        )
                    }
                }
            }
        },
        content = content,
    )
}

private data class TopLevelTab(
    val destination: TopLevelDestination,
    val route: Any,
    val icon: ImageVector,
    val labelRes: Int,
    val contentDescriptionRes: Int,
)

private val TopLevelTabs: List<TopLevelTab> = listOf(
    TopLevelTab(
        destination = TopLevelDestination.Discover,
        route = CourtListRoute,
        icon = Icons.Default.LocationOn,
        labelRes = R.string.shell_tab_discover,
        contentDescriptionRes = R.string.shell_tab_discover_cd,
    ),
    TopLevelTab(
        destination = TopLevelDestination.Play,
        route = ChallengesRoute,
        icon = Icons.Default.PlayArrow,
        labelRes = R.string.shell_tab_play,
        contentDescriptionRes = R.string.shell_tab_play_cd,
    ),
    TopLevelTab(
        destination = TopLevelDestination.Activity,
        route = MatchHistoryRoute,
        icon = Icons.AutoMirrored.Filled.List,
        labelRes = R.string.shell_tab_activity,
        contentDescriptionRes = R.string.shell_tab_activity_cd,
    ),
    TopLevelTab(
        destination = TopLevelDestination.Profile,
        route = ProfileRoute,
        icon = Icons.Default.Person,
        labelRes = R.string.shell_tab_profile,
        contentDescriptionRes = R.string.shell_tab_profile_cd,
    ),
)

private fun NavDestination.toTopLevelDestination(): TopLevelDestination? = when {
    hasRoute<CourtListRoute>() -> TopLevelDestination.Discover
    hasRoute<ChallengesRoute>() -> TopLevelDestination.Play
    hasRoute<MatchHistoryRoute>() -> TopLevelDestination.Activity
    hasRoute<ProfileRoute>() -> TopLevelDestination.Profile
    else -> null
}
