package com.example.appdevproject26s.social.messaging

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.appdevproject26s.R
import com.example.appdevproject26s.ScreenScaffold
import com.example.appdevproject26s.social.LoginPrompt

@Composable
fun MessagingScreen(
    navController: NavController,
    messagingViewModel: MessagingScreenViewModel = hiltViewModel(),
) {
    ScreenScaffold(
        navController = navController,
        title = stringResource(R.string.messages_title),
        showBackButton = true
    ) {
        val isLoggedIn by messagingViewModel.isLoggedIn.collectAsState()

        if (!isLoggedIn) {
            LoginPrompt(
                feature = "messaging",
                onNavigateToLogin = { navController.navigate("profile") }
            )

        } else {
            val chats = messagingViewModel.chats.collectAsState().value

            LazyColumn() {
                items(items = chats) { chat ->
                    Text(chat)
                }
            }
        }
    }
}
