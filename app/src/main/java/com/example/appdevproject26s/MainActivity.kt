package com.example.appdevproject26s

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.appdevproject26s.gamification.achievements.AchievementScreen
import com.example.appdevproject26s.gamification.challenges.ChallengeScreen
import com.example.appdevproject26s.gamification.leaderboard.LeaderboardScreen
import com.example.appdevproject26s.network.SessionManager
import com.example.appdevproject26s.profile.ProfileScreen
import com.example.appdevproject26s.route.HomeScreen
import com.example.appdevproject26s.route.RouteScreen
import com.example.appdevproject26s.social.friends.FriendsScreen
import com.example.appdevproject26s.social.messaging.MessagingScreen
import com.example.appdevproject26s.ui.theme.AppDevProject26STheme
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var sessionManager: SessionManager

    private var navigatedChatIdState by mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleNotificationIntent(intent)

        setContent {
            AppDevProject26STheme {
                NavigationApp(
                    navigationChatId = navigatedChatIdState,
                    onNavigated = { navigatedChatIdState = null }
                )

            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                sessionManager.startSessionObservation()
            }
        }
    }

    private fun handleNotificationIntent(intent: Intent?) {
        val chatId = intent?.getLongExtra("EXTRA_CHAT_ID", -1L).takeIf { it != -1L }
        if (chatId != null) {
            navigatedChatIdState = chatId
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sessionManager.terminateSession()
    }
}

@Composable
fun NavigationApp(
    navigationChatId: Long?,
    onNavigated: () -> Unit
) {
    val navController = rememberNavController()

    LaunchedEffect(navigationChatId) {
        if (navigationChatId != null) {
            navController.navigate("messaging/$navigationChatId")
            onNavigated()
        }
    }

    NavHost(navController = navController, startDestination = "home") {
        composable("home") { HomeScreen(navController) }
        composable( route = "route" ) { RouteScreen(navController) }
        composable(route = "messaging/{chatId}") { MessagingScreen(navController) }
        composable( route = "messaging" ) { MessagingScreen(navController) }
        composable("friends") { FriendsScreen(navController = navController) }
        composable("profile") { ProfileScreen(navController = navController) }
        composable("leaderboard") { LeaderboardScreen(navController) }
        composable("challenge") { ChallengeScreen(navController) }
        composable("achievements") { AchievementScreen(navController) }
        composable("settings") { SettingsScreen(navController) }
    }
}