package com.example.appdevproject26s

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.example.appdevproject26s.stats.AnalyticsRecorder
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