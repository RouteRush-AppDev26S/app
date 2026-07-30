package com.example.appdevproject26s

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import com.example.appdevproject26s.auth.AuthRepository
import com.example.appdevproject26s.gamification.achievements.AchievementScreen
import com.example.appdevproject26s.gamification.challenges.ChallengeScreen
import com.example.appdevproject26s.gamification.leaderboard.LeaderboardScreen
import com.example.appdevproject26s.modules.AppNotificationManager
import com.example.appdevproject26s.network.WebSocketManager
import com.example.appdevproject26s.profile.ProfileScreen
import com.example.appdevproject26s.route.HomeScreen
import com.example.appdevproject26s.route.RouteScreen
import com.example.appdevproject26s.social.friends.FriendsScreen
import com.example.appdevproject26s.social.messaging.ActiveChatStore
import com.example.appdevproject26s.social.messaging.MessagingRepository
import com.example.appdevproject26s.social.messaging.MessagingScreen
import com.example.appdevproject26s.social.messaging.UnreadChatsStore
import com.example.appdevproject26s.ui.theme.AppDevProject26STheme
import com.example.appdevproject26s.user.UserRepository
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.disposables.Disposable
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

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var messagingRepository: MessagingRepository

    @Inject
    lateinit var unreadChatsStore: UnreadChatsStore

    @Inject
    lateinit var activeChatStore: ActiveChatStore

    @Inject
    lateinit var appNotificationManager: AppNotificationManager

    private var globalInboxSubscription: Disposable? = null

    private var navigatedChatIdState by mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialChatId = intent?.getLongExtra("EXTRA_CHAT_ID", -1L).takeIf { it != -1L }
        if (initialChatId != null) {
            navigatedChatIdState = initialChatId
        }

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
                authRepository.isLoggedInFlow.combine(authRepository.tokenFlow) { isLoggedIn, token ->
                    Pair(isLoggedIn, token)
                }.collectLatest { (isLoggedIn, token) ->
                    if (isLoggedIn && !token.isNullOrBlank()) {
                        // User is verified and logged in: Connect socket
                        if (!webSocketManager.isConnected) {
                            webSocketManager.connect(jwtToken = token)
                            Log.d("TOKEN", token)
                        }
                        startGlobalInboxListener()
                    } else {
                        // User is logged out or session expired: clear inbox subscription, Disconnect socket
                        globalInboxSubscription?.dispose()
                        globalInboxSubscription = null
                        if (webSocketManager.isConnected) {
                            webSocketManager.disconnect()
                        }
                    }
                }
            }
        }
    }

    private fun handleNotificationIntent(intent: Intent) {
        val chatId = intent.getLongExtra("EXTRA_CHAT_ID", -1L).takeIf { it != -1L }
        Log.d("NOTIFICATION_DEBUG", "onNewIntent with chatId: $chatId")
        if (chatId != null) {
            navigatedChatIdState = chatId
        }
    }

    private fun startGlobalInboxListener() {
        if (globalInboxSubscription != null) return

        lifecycleScope.launch {
            userRepository.getCurrentUser().fold(
                onSuccess = { user ->
                    val currentUserId = user.id ?: return@fold

                    globalInboxSubscription = messagingRepository.observeInbox(currentUserId) { newMessage ->
                        val chatId = newMessage.chatId
                        val senderId = newMessage.senderId

                        if (chatId != null) {
                            val isCurrentltViewingChat = (activeChatStore.activeChatId.value == chatId)

                            if (senderId != currentUserId && !isCurrentltViewingChat ) {
                                appNotificationManager.showMessageNotification(
                                    senderName = newMessage.senderUsername ?: "unknown",
                                    messageText = newMessage.content ?: "",
                                    chatId = chatId
                                    )
                                lifecycleScope.launch {
                                    unreadChatsStore.addUnreadChatId(chatId)
                                }
                            }
                        }
                    }
                },
                onFailure = { error ->
                    Log.e("WEBSOCKET_DEBUG", "-> Failed to setup global inbox: ${error.message}")
                }
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        globalInboxSubscription?.dispose()
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