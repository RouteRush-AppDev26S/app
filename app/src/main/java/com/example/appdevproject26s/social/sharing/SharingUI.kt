package com.example.appdevproject26s.social.sharing

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.appdevproject26s.map.MapScreenViewModel

@Composable
fun PinSharingDialog(
    viewModel: MapScreenViewModel,
    onDismiss: () -> Unit,
) {
    val friends by viewModel.friends.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val pinNote by viewModel.pinNote.collectAsState()
    val isSharing by viewModel.isSharingPin.collectAsState()

    val friendsToSharePinWith by viewModel.friendsToSharePinWith.collectAsState()

    // Filter friends based on search query matching their username
    val filteredFriends = friends.filter { friendship ->
        val otherUsername = friendship.otherUsername
        otherUsername.contains(searchQuery, ignoreCase = true)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Share Pin") },
        text = {
            if (isSharing) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (friends.isEmpty()) {
                Text("No friends available to share with.")
            } else {
                Column {
                    OutlinedTextField(
                        value = pinNote,
                        onValueChange = { viewModel.updatePinNote(it) },
                        label = { Text("Add a note (optional)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        singleLine = false,
                        maxLines = 3
                    )

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
                            val isChecked = friendsToSharePinWith.contains(username)

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        viewModel.toggleSharePinWithUser(username)
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
                                        onCheckedChange = {
                                            viewModel.toggleSharePinWithUser(
                                                username
                                            )
                                        }
                                    )
                                }
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

                Button(
                    onClick = {
                        if (friendsToSharePinWith.isNotEmpty()) {
                            viewModel.sharePinWithFriends()
                        }
                    },
                    enabled = friendsToSharePinWith.size > 0

                ) {
                    Text("Share")
                }
            }
        }
    )
}