package com.example.appdevproject26s.social.messaging

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appdevproject26s.auth.AuthRepository
import com.example.appdevproject26s.social.friends.FriendRepository
import com.example.appdevproject26s.social.friends.FriendshipResponse
import com.example.appdevproject26s.user.UserProfileResponse
import com.example.appdevproject26s.user.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MessagingScreenViewModel @Inject constructor(
    authRepo: AuthRepository,
    private val messagingRepository: MessagingRepository,
    private val friendRepository: FriendRepository,
    private val userRepository: UserRepository

) : ViewModel() {

    val isLoggedIn: StateFlow<Boolean> = authRepo.isLoggedInFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    private val _messageRefreshTrigger = MutableSharedFlow<Unit>(replay = 0)

    private val _currentUser = MutableStateFlow<UserProfileResponse?>(null)
    val currentUser: StateFlow<UserProfileResponse?> = _currentUser.asStateFlow()

    private val _chats = MutableStateFlow<List<ChatResponse>>(emptyList())
    val chats: StateFlow<List<ChatResponse>> = _chats.asStateFlow()

    private val _selectedChat = MutableStateFlow<ChatResponse?>(null)
    val selectedChat: StateFlow<ChatResponse?> = _selectedChat.asStateFlow()

    private val _friends = MutableStateFlow<List<FriendshipResponse>>(emptyList())
    val friends: StateFlow<List<FriendshipResponse>> = _friends.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _createGroupChatUsernames = MutableStateFlow<List<String>>(emptyList())
    val createGroupChatUsernames: StateFlow<List<String>> = _createGroupChatUsernames.asStateFlow()

    private val _inputGroupChatName = MutableStateFlow<String>("")
    val inputGroupChatName: StateFlow<String> = _inputGroupChatName.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val messages: StateFlow<List<ChatMessageResponse>> =
        _selectedChat
            .flatMapLatest { chat ->
                flow {
                    if (chat?.id != null) {
                        // Emit initial load
                        fetchMessages(chat.id)

                        _messageRefreshTrigger.collect {
                            fetchMessages(chat.id)
                        }
                    } else {
                        emit(emptyList())
                    }
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    private suspend fun kotlinx.coroutines.flow.FlowCollector<List<ChatMessageResponse>>.fetchMessages(chatId: Long) {
        messagingRepository.getMessages(chatId).fold(
            onSuccess = { emit(it) },
            onFailure = { emit(emptyList()) }
        )
    }

    init {
        viewModelScope.launch {
            authRepo.isLoggedInFlow.collect { loggedIn ->
                if (loggedIn) {
                    fetchCurrentUser()
                    fetchChats()
                    fetchFriends()
                } else {
                    _currentUser.value = null
                    _chats.value = emptyList()
                    _friends.value = emptyList()
                }
            }
        }
    }

    private fun fetchCurrentUser() {
        viewModelScope.launch {
            userRepository.getCurrentUser().fold(
                onSuccess = { _currentUser.value = it },
                onFailure = { _currentUser.value = null }
            )
        }
    }

    fun fetchChats() {
        viewModelScope.launch {
            messagingRepository.getChats().fold(
                onSuccess = { chatList ->
                    _chats.value = chatList
                },
                onFailure = {
                    _chats.value = emptyList()
                }
            )
        }
    }

    fun selectChat(chat: ChatResponse?) {
        _selectedChat.value = chat
    }

    fun fetchFriends() {
        viewModelScope.launch {
            friendRepository.getFriends().fold(
                onSuccess = { _friends.value = it },
                onFailure = { _friends.value = emptyList() }
            )
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun createDirectChat(username: String, onChatCreated: (ChatResponse) -> Unit) {
        viewModelScope.launch {
            messagingRepository.createDirectChat(username).fold(
                onSuccess = { chat ->
                    fetchChats() // Refresh chat list
                    onChatCreated(chat)
                },
                onFailure = { /* Handle error if needed */ }
            )
        }
    }

    fun createGroupChat(name: String, usernames: List<String>, onChatCreated: (ChatResponse) -> Unit) {
        viewModelScope.launch {
            messagingRepository.createGroupChat(name, usernames).fold(
                onSuccess = { chat ->
                    fetchChats() // Refresh chat list
                    onChatCreated(chat)
                },
                onFailure = { /* Handle error if needed */ }
            )
        }
    }

    fun sendMessage(text: String) {
        val chatId = _selectedChat.value?.id ?: return
        viewModelScope.launch {
            messagingRepository.postMessage(chatId, text).fold(
                onSuccess = {
                    _messageRefreshTrigger.emit(Unit)
                },
                onFailure = { /* Handle error if needed */ }
            )
        }
    }

    fun updateInputGroupChatName(it: String) {
        _inputGroupChatName.value = it
    }

    fun toggleUsernameForGroupChat(username: String) {
        val currentList = _createGroupChatUsernames.value
        _createGroupChatUsernames.value = if (currentList.contains(username)) {
            currentList - username
        } else {
            currentList + username
        }
    }

}
