package com.example.appdevproject26s

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlinx.coroutines.launch

@Composable
fun NavDrawer(
    navController: NavController,
    drawerState: DrawerState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val drawerStateScope = rememberCoroutineScope()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val startDestinationRoute = navController.graph.startDestinationRoute ?: "home"


    ModalNavigationDrawer(
        modifier = modifier,
        drawerState = drawerState,
        drawerContent = {

            ModalDrawerSheet {
                NavigationDrawerItem(
                    label = { Text(text = stringResource(R.string.home_title)) },
                    selected = currentRoute == "home",
                    icon = {
                        Icon(
                            Icons.Default.Map,
                            contentDescription = stringResource(R.string.home_title)
                        )
                    },
                    onClick = {
                        drawerStateScope.launch { drawerState.close() }
                        // clear back stack. home is the root
                        navController.navigate("home") {
                            popUpTo(startDestinationRoute) { saveState = true }
                            launchSingleTop = true
                        }
                    }
                )
                NavigationDrawerItem(
                    label = { Text(text = stringResource(R.string.route_title)) },
                    selected = currentRoute == "route",
                    icon = {
                        Icon(
                            Icons.Default.Route,
                            contentDescription = stringResource(R.string.route_title)
                        )
                    },
                    onClick = {
                        drawerStateScope.launch { drawerState.close() }
                        navController.navigate("route") {
                            popUpTo("home") { saveState = true }
                            launchSingleTop = true
                        }
                    }
                )
                NavigationDrawerItem(
                    label = { Text(text = stringResource(R.string.messages_title)) },
                    selected = currentRoute == "messaging",
                    icon = {
                        Icon(
                            Icons.AutoMirrored.Filled.Message,
                            contentDescription = stringResource(R.string.messages_title)
                        )
                    },
                    onClick = {
                        drawerStateScope.launch { drawerState.close() }
                        navController.navigate("messaging") {
                            popUpTo("home") { saveState = true }
                            launchSingleTop = true
                        }
                    }
                )
                NavigationDrawerItem(
                    label = { Text(text = stringResource(R.string.friends_title)) },
                    selected = currentRoute == "friends",
                    icon = {
                        Icon(
                            Icons.Default.People,
                            contentDescription = stringResource(R.string.friends_title)
                        )
                    },
                    onClick = {
                        drawerStateScope.launch { drawerState.close() }
                        navController.navigate("friends") {
                            popUpTo("home") { saveState = true }
                            launchSingleTop = true
                        }
                    }
                )
                HorizontalDivider()
                NavigationDrawerItem(
                    label = { Text(text = stringResource(R.string.profile_title)) },
                    selected = currentRoute == "profile",
                    icon = {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = stringResource(R.string.profile_title)
                        )
                    },
                    onClick = {
                        drawerStateScope.launch { drawerState.close() }
                        navController.navigate("profile") {
                            popUpTo("home") { saveState = true }
                            launchSingleTop = true
                        }
                    }
                )
                NavigationDrawerItem(
                    label = { Text(text = stringResource(R.string.settings_title)) },
                    selected = currentRoute == "settings",
                    icon = {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings_title)
                        )
                    },
                    onClick = {
                        drawerStateScope.launch { drawerState.apply { close() } }
                        navController.navigate("settings") {
                            launchSingleTop = true
                        }
                    }
                )

            }


        },
        gesturesEnabled = drawerState.currentValue != DrawerValue.Closed // enables swipe to close draw only
    ) {
        content()
    }


}

