package com.example.appdevproject26s

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.appdevproject26s.gamification.achievements.AchievementScreen
import com.example.appdevproject26s.gamification.challenges.ChallengeScreen
import com.example.appdevproject26s.gamification.leaderboard.LeaderboardScreen
import com.example.appdevproject26s.profile.ProfileScreen
import com.example.appdevproject26s.route.MapScreen
import com.example.appdevproject26s.route.RouteScreen
import com.example.appdevproject26s.social.friends.FriendsScreen
import com.example.appdevproject26s.social.messaging.MessagingScreen
import com.example.appdevproject26s.stats.DashboardScreen
import com.example.appdevproject26s.stats.HeatmapScreen

@Composable
fun NavigationAppRootComposable(
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
        composable("home") { MapScreen(navController) }
        composable( route = "route" ) { RouteScreen(navController) }
        composable(route = "messaging/{chatId}") { MessagingScreen(navController) }
        composable( route = "messaging" ) { MessagingScreen(navController) }
        composable("friends") { FriendsScreen(navController = navController) }
        composable("profile") { ProfileScreen(navController = navController) }
        composable("leaderboard") { LeaderboardScreen(navController) }
        composable("challenge") { ChallengeScreen(navController) }
        composable("achievements") { AchievementScreen(navController) }
        composable("settings") { SettingsScreen(navController) }
        composable("statistics") { DashboardScreen(navController) }
        composable("heatmap") { HeatmapScreen(navController) }
    }
}