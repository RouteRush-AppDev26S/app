package com.example.appdevproject26s

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import com.example.appdevproject26s.stats.AnalyticsRecorder
import com.example.appdevproject26s.stats.DashboardScreen
import com.example.appdevproject26s.stats.HeatmapScreen
import com.example.appdevproject26s.ui.theme.AppDevProject26STheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject // <-- ADDED THIS IMPORT

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var navigatedChatIdState by mutableStateOf<Long?>(null)

    @Inject
    lateinit var analyticsRecorder: AnalyticsRecorder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Optional: Start analytics recorder if required by your project
        analyticsRecorder.start(lifecycleScope)

        handleNotificationIntent(intent)

        setContent {
            AppDevProject26STheme {
                NavigationAppRootComposable(
                    navigationChatId = navigatedChatIdState,
                    onNavigated = { navigatedChatIdState = null }
                )
            }
        }
    }

    private fun handleNotificationIntent(intent: Intent?) {
        // FIXED syntax error here: added ?. before takeIf
        val chatId = intent?.getLongExtra("EXTRA_CHAT_ID", -1L)?.takeIf { it != -1L }
        if (chatId != null) {
            navigatedChatIdState = chatId
        }
    }
}