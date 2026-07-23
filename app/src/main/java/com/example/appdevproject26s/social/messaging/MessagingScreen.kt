package com.example.appdevproject26s.social.messaging

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
    ) { paddingValues ->
        val isLoggedIn by messagingViewModel.isLoggedIn.collectAsState()

        if (!isLoggedIn) {
            LoginPrompt(
                feature = "messaging",
                onNavigateToLogin = { navController.navigate("profile") }
            )

        } else {
            val chats = messagingViewModel.chats.collectAsState().value

            if (chats.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No chats found",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyColumn() {
                    items(items = chats, key = { it.id ?: 0L }) { chat ->
                        ChatCard(chat) { }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatCard(
    chat: Chat,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick)
    ) {
        Text(
            text = chat.name ?: "Chat #${chat.id}",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp)
        )
    }
}