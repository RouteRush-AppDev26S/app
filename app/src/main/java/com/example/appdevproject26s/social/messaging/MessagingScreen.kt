package com.example.appdevproject26s.social.messaging

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    var showNewChatDialog by remember { mutableStateOf(false) }
    val selectedChat by messagingViewModel.selectedChat.collectAsState()
    val isLoggedIn by messagingViewModel.isLoggedIn.collectAsState()
    val currentUser by messagingViewModel.currentUser.collectAsState()

    val onBackClick: (() -> Unit)? =
        if (selectedChat != null) {
            { messagingViewModel.selectChat(null) }
        } else {
            null
        }


    ScreenScaffold(
        navController = navController,
        title = selectedChat?.name ?: stringResource(R.string.messages_title),
        showBackButton = true,
        onBackClick = onBackClick
    ) { paddingValues ->

        if (!isLoggedIn) {
            LoginPrompt(
                feature = "messaging",
                onNavigateToLogin = { navController.navigate("profile") }
            )

        } else {

            if (selectedChat != null) {
                val currentChat = selectedChat!!
                val chatId = currentChat.id ?: 0L
                val messages by messagingViewModel.messages.collectAsState(initial = emptyList())

                // Chat Detail View
                ChatDetailContent(
                    messages = messages,
                    onSendMessage = messagingViewModel::sendMessage,
                    myUsername = currentUser?.username ?: "",
                )
            } else {

                val chats = messagingViewModel.chats.collectAsState().value

                Box(modifier = Modifier.fillMaxSize()) {
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
                                ChatCard(chat) {
                                    messagingViewModel.selectChat(chat)
                                }
                            }
                        }
                    }

                    FloatingActionButton(
                        onClick = {
                            messagingViewModel.fetchFriends() // Refresh friends list
                            showNewChatDialog = true
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "New Chat")
                    }

                    if (showNewChatDialog) {
                        NewChatDialog(
                            viewModel = messagingViewModel,
                            onDismiss = { showNewChatDialog = false },
                            onChatSelected = { chat ->
                                showNewChatDialog = false
                                // TODO: Navigate to chat details thread screen using chat.id
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatCard(
    chat: ChatResponse,
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

@Composable
fun NewChatDialog(
    viewModel: MessagingScreenViewModel,
    onDismiss: () -> Unit,
    onChatSelected: (ChatResponse) -> Unit
) {
    val friends by viewModel.friends.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    // Filter friends based on search query matching their username
    val filteredFriends = friends.filter { friendship ->
        val otherUsername = friendship.otherUsername
        otherUsername.contains(searchQuery, ignoreCase = true)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Direct Chat") },
        text = {
            Column {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    label = { Text("Search friends") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    singleLine = true
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 250.dp)
                ) {
                    items(filteredFriends) { friendship ->
                        val username = friendship?.otherUsername ?: "Unknown"
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    viewModel.createDirectChat(username) { createdChat ->
                                        onChatSelected(createdChat)
                                    }
                                }
                        ) {
                            Text(
                                text = username,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}