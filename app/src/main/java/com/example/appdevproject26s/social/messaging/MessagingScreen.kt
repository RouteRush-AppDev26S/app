package com.example.appdevproject26s.social.messaging

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
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

    val screenTitle =
        if (selectedChat != null) {
            selectedChat?.name ?: "Chat #${selectedChat?.id}"
        } else {
            stringResource(R.string.messages_title)
        }

    ScreenScaffold(
        navController = navController,
        title = screenTitle,
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
                                messagingViewModel.selectChat(chat)
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

    val createGroupChatUsernames by viewModel.createGroupChatUsernames.collectAsState()
    val inputGroupChatName by viewModel.inputGroupChatName.collectAsState()

    // Filter friends based on search query matching their username
    val filteredFriends = friends.filter { friendship ->
        val otherUsername = friendship.otherUsername
        otherUsername.contains(searchQuery, ignoreCase = true)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (createGroupChatUsernames.size > 1) "New Group Chat" else "New Chat") },
        text = {
            Column {
                // 1. Show selected friends above search if any are selected
                if (createGroupChatUsernames.isNotEmpty()) {
                    Text("Selected (${createGroupChatUsernames.size}):", style = MaterialTheme.typography.labelMedium)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(createGroupChatUsernames) { username ->
                            FilterChip(
                                selected = true,
                                onClick = { viewModel.toggleUsernameForGroupChat(username) },
                                label = { Text(username) },
                                trailingIcon = {
                                    Icon(Icons.Default.Close, contentDescription = "Remove")
                                }
                            )
                        }
                    }
                }

                // 2. Show Group Chat Name Field if 2 or more users are selected
                if (createGroupChatUsernames.size > 1) {
                    OutlinedTextField(
                        value = inputGroupChatName,
                        onValueChange = { viewModel.updateInputGroupChatName(it) },
                        label = { Text("Group Chat Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

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
                        val username = friendship.otherUsername ?: "Unknown"
                        val isChecked = createGroupChatUsernames.contains(username)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    viewModel.toggleUsernameForGroupChat(username)
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = username,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { viewModel.toggleUsernameForGroupChat(username) }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }

                // Final Action Button to Create Chat (Direct or Group)
                Button(
                    onClick = {
                        if (createGroupChatUsernames.size == 1) {
                            // Direct Chat route
                            viewModel.createDirectChat(createGroupChatUsernames.first()) { chat ->
                                onChatSelected(chat)
                            }
                        } else if (createGroupChatUsernames.size > 1 && inputGroupChatName.isNotBlank()) {
                            // Group Chat route
                            viewModel.createGroupChat(inputGroupChatName, createGroupChatUsernames.toList()) { chat ->
                                onChatSelected(chat)
                            }
                        }
                    },
                    enabled = when {
                        createGroupChatUsernames.size == 1 -> true
                        createGroupChatUsernames.size > 1 -> inputGroupChatName.isNotBlank()
                        else -> false
                    }
                ) {
                    Text(if (createGroupChatUsernames.size > 1) "Create Group" else "Start Chat")
                }
            }
        }
    )
}