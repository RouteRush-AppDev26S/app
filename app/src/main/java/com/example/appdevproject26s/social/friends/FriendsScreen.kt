package com.example.appdevproject26s.social.friends

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.appdevproject26s.ScreenScaffold
import com.example.appdevproject26s.social.LoginPrompt

@Composable
fun FriendsScreen(
    navController: NavController,
    viewModel: FriendsScreenViewModel = hiltViewModel()
) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val inputUsername by viewModel.addFriendInput.collectAsState()
    val actionMessage by viewModel.actionMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(actionMessage) {
        actionMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearActionMessage()
        }
    }

    ScreenScaffold(
        navController = navController,
        title = "My Friends",
        snackbarHostState = snackbarHostState,
    ) { openDrawer ->
        if (!isLoggedIn) {
            LoginPrompt(
                feature = "friends",
                onNavigateToLogin = { navController.navigate("profile") }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Add Friend Input Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = inputUsername,
                        onValueChange = viewModel::updateAddFriendInput,
                        label = { Text("Friend's Username") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Button(
                        onClick = { viewModel.sendFriendRequest() }
                    ) {
                        Icon(imageVector = Icons.Default.PersonAdd, contentDescription = "Add")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // States Handling
                when (val state = uiState) {
                    is FriendsUiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    is FriendsUiState.Error -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = state.message, color = MaterialTheme.colorScheme.error)
                        }
                    }

                    is FriendsUiState.Success -> {
                        Log.d("FRIENDS", state.pendingRequests.isEmpty().toString() )
                        if (state.friends.isEmpty() && state.pendingRequests.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No friends added yet.",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                // Pending Requests Section
                                if (state.pendingRequests.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = "Pending Requests",
                                            style = MaterialTheme.typography.titleMedium,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    }
                                    items(state.pendingRequests) { request ->
                                        FriendCard(
                                            friendship = request,
                                            onAccept = { viewModel.acceptRequest(request.id) },
                                            onDeclineOrCancel = { viewModel.declineOrCancel(request.id) }
                                        )
                                    }
                                }

                                // Established Friends Section
                                if (state.friends.isNotEmpty()) {
                                    item {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "My Friends",
                                            style = MaterialTheme.typography.titleMedium,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    }
                                    items(state.friends) { friend ->
                                        FriendCard(
                                            friendship = friend,
                                            onAccept = null,
                                            onDeclineOrCancel = { viewModel.declineOrCancel(friend.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FriendCard(
    friendship: FriendshipResponse,
    onAccept: (() -> Unit)?,
    onDeclineOrCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(text = friendship.otherUsername, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Status: ${friendship.status}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (friendship.status == FriendshipStatus.PENDING)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onAccept != null) {
                    IconButton(onClick = onAccept) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Accept",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                IconButton(onClick = onDeclineOrCancel) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove/Decline",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}