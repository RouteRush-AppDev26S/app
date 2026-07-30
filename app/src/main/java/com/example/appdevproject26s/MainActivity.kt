package com.example.appdevproject26s

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.appdevproject26s.network.SessionManager
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
                NavigationAppRootComposable(
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