package com.example.appdevproject26s.social.messaging

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appdevproject26s.auth.AuthRepository
import com.example.appdevproject26s.social.friends.FriendRepository
import com.example.appdevproject26s.social.friends.FriendshipResponse
import com.example.appdevproject26s.user.UserProfileResponse
import com.example.appdevproject26s.user.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
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

    private val _currentUser = MutableStateFlow<UserProfileResponse?>(null)
    val currentUser: StateFlow<UserProfileResponse?> = _currentUser.asStateFlow()

    private val _chats = MutableStateFlow<List<ChatResponse>>(emptyList())
    val chats: StateFlow<List<ChatResponse>> = _chats.asStateFlow()

    private val _selectedChat = MutableStateFlow<ChatResponse?>(null)
    val selectedChat: StateFlow<ChatResponse?> = _selectedChat.asStateFlow()

    private val _unreadChatIds = MutableStateFlow<Set<Long>>(emptySet())
    val unreadChatIds: StateFlow<Set<Long>> = _unreadChatIds.asStateFlow()

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
                callbackFlow {
                    if (chat?.id == null) {
                        trySend(emptyList())
                        awaitClose {}
                        return@callbackFlow
                    }

                    var currentMessages = listOf<ChatMessageResponse>()

                    // Fetch initial message history via REST
                    messagingRepository.getMessages(chat.id).fold(
                        onSuccess = { initialList ->
                            currentMessages = initialList
                            trySend(currentMessages)
                        },
                        onFailure = {
                            currentMessages = emptyList()
                            trySend(currentMessages)
                        }
                    )

                    // then subscribe to chat channel
                    val subscription = messagingRepository.observeChat(chat.id) { incomingMessage ->
                        currentMessages = currentMessages + incomingMessage
                        trySend(currentMessages)
                    }

                    awaitClose {
                        subscription.dispose()
                    }
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    fun sendMessage(text: String) {
        val chatId = _selectedChat.value?.id ?: return
        messagingRepository.postMessage(chatId, text)
    }

    private var inboxSubscription: Disposable? = null

    init {
        viewModelScope.launch {
            authRepo.isLoggedInFlow.collect { loggedIn ->
                if (loggedIn) {
                    fetchCurrentUser()
                    fetchChats()
                    fetchFriends()
                } else {
                    inboxSubscription?.dispose()
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
                onSuccess = { user ->
                    _currentUser.value = user
                    setupInboxListener(user.id)
                            },
                onFailure = { error ->
                    _currentUser.value = null
                }
            )
        }
    }

    private fun setupInboxListener(userId: Long) {
        inboxSubscription?.dispose()

        inboxSubscription = messagingRepository.observeInbox(userId) { newMessage ->
            val currentSelectedChatId = _selectedChat.value?.id

            // If a message arrives for a chat the user is NOT currently looking at, mark it unread
            if (newMessage.chatId != null && newMessage.chatId != currentSelectedChatId) {
                _unreadChatIds.value += newMessage.chatId
            }

            // Refresh the chat list so it reorders to the top with the latest preview
            fetchChats()
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
        if (chat?.id != null) {
            _unreadChatIds.value = _unreadChatIds.value - chat.id
        }
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

    override fun onCleared() {
        super.onCleared()
        inboxSubscription?.dispose()
    }
}
