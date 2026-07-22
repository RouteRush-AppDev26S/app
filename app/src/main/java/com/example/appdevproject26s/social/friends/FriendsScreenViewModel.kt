package com.example.appdevproject26s.social.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appdevproject26s.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface FriendsUiState {
    data object Loading : FriendsUiState
    data class Success(
        val friends: List<FriendshipResponse>,
        val pendingRequests: List<FriendshipResponse>
    ) : FriendsUiState
    data class Error(val message: String) : FriendsUiState
}

@HiltViewModel
class FriendsScreenViewModel @Inject constructor(
    private val friendRepository: FriendRepository,
    authRepository: AuthRepository
) : ViewModel() {

    val isLoggedIn: StateFlow<Boolean> = authRepository.isLoggedInFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )
    private val _uiState = MutableStateFlow<FriendsUiState>(FriendsUiState.Loading)
    val uiState: StateFlow<FriendsUiState> = _uiState.asStateFlow()

    private val _addFriendInput = MutableStateFlow("")
    val addFriendInput = _addFriendInput.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = FriendsUiState.Loading

            val friendsResult = friendRepository.getFriends()
            val pendingResult = friendRepository.getPendingRequests()

            if (friendsResult.isSuccess && pendingResult.isSuccess) {
                _uiState.value = FriendsUiState.Success(
                    friends = friendsResult.getOrDefault(emptyList()),
                    pendingRequests = pendingResult.getOrDefault(emptyList())
                )
            } else {
                val errorMsg = friendsResult.exceptionOrNull()?.localizedMessage
                    ?: pendingResult.exceptionOrNull()?.localizedMessage
                    ?: "Failed to load data"
                _uiState.value = FriendsUiState.Error(errorMsg)
            }
        }
    }

    fun updateAddFriendInput(username: String) {
        _addFriendInput.value = username
    }

    fun sendFriendRequest() {
        val username = _addFriendInput.value.trim()
        if (username.isBlank()) {
            _actionMessage.value = "Username cannot be empty"
            return
        }

        viewModelScope.launch {
            friendRepository.sendRequest(username).fold(
                onSuccess = {
                    _actionMessage.value = "Successfully added $username!"
                    _addFriendInput.value = ""
                    loadData() // Refresh list
                },
                onFailure = { error ->
                    _actionMessage.value = error.localizedMessage ?: "Failed to add friend"
                }
            )
        }
    }

    fun acceptRequest(friendshipId: Long) {
        viewModelScope.launch {
            friendRepository.acceptRequest(friendshipId).fold(
                onSuccess = {
                    _actionMessage.value = "Friend request accepted!"
                    loadData()
                },
                onFailure = { error ->
                    _actionMessage.value = error.localizedMessage ?: "Failed to accept request"
                }
            )
        }
    }

    fun declineOrCancel(friendshipId: Long) {
        viewModelScope.launch {
            friendRepository.declineOrCancel(friendshipId).fold(
                onSuccess = {
                    _actionMessage.value = "Friendship updated successfully."
                    loadData()
                },
                onFailure = { error ->
                    _actionMessage.value = error.localizedMessage ?: "Failed to remove friendship"
                }
            )
        }
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }
}