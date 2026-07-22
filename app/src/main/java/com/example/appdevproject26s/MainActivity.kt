package com.example.appdevproject26s

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.appdevproject26s.profile.ProfileScreen
import com.example.appdevproject26s.social.friends.FriendsScreen
import com.example.appdevproject26s.social.messaging.MessagingScreen
import com.example.appdevproject26s.ui.theme.AppDevProject26STheme

import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.HiltAndroidApp

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppDevProject26STheme {
                NavigationApp()
            }
        }
    }
}

@Composable
fun NavigationApp(modifier: Modifier = Modifier) {

    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") { HomeScreen(navController) }
        composable( route = "route" ) { RouteScreen(navController) }
        composable( route = "messaging" ) { MessagingScreen(navController) }
        composable("friends") { FriendsScreen(navController = navController) }
        composable("profile") { ProfileScreen(navController = navController) }
        composable("leaderboard") { LeaderboardScreen(navController) }
        composable("challenge") { ChallengeScreen(navController) }
        composable("settings") { SettingsScreen(navController) }
    }
}