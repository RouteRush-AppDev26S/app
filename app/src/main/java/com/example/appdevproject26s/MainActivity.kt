package com.example.appdevproject26s

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.appdevproject26s.auth.AuthRepository
import com.example.appdevproject26s.gamification.achievements.AchievementScreen
import com.example.appdevproject26s.gamification.challenges.ChallengeScreen
import com.example.appdevproject26s.gamification.leaderboard.LeaderboardScreen
import com.example.appdevproject26s.network.WebSocketManager
import com.example.appdevproject26s.profile.ProfileScreen
import com.example.appdevproject26s.route.HomeScreen
import com.example.appdevproject26s.route.RouteScreen
import com.example.appdevproject26s.social.friends.FriendsScreen
import com.example.appdevproject26s.social.messaging.MessagingScreen
import com.example.appdevproject26s.ui.theme.AppDevProject26STheme

import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var webSocketManager: WebSocketManager

    @Inject
    lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppDevProject26STheme {
                NavigationApp()
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                authRepository.isLoggedInFlow.combine(authRepository.tokenFlow) { isLoggedIn, token ->
                    Pair(isLoggedIn, token)
                }.collectLatest { (isLoggedIn, token) ->
                    if (isLoggedIn && !token.isNullOrBlank()) {
                        // User is verified and logged in: Connect socket
                        if (!webSocketManager.isConnected) {
                            webSocketManager.connect(jwtToken = token)
                            Log.d("TOKEN", token)
                        }
                    } else {
                        // User is logged out or session expired: Disconnect socket
                        if (webSocketManager.isConnected) {
                            webSocketManager.disconnect()
                        }
                    }
                }
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
        composable("achievements") { AchievementScreen(navController) }
        composable("settings") { SettingsScreen(navController) }
    }
}