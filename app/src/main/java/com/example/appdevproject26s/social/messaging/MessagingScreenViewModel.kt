package com.example.appdevproject26s.social.messaging

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appdevproject26s.auth.AuthRepository
import com.example.appdevproject26s.modules.AppNotificationManager
import com.example.appdevproject26s.social.friends.FriendRepository
import com.example.appdevproject26s.social.friends.FriendshipResponse
import com.example.appdevproject26s.user.UserProfileResponse
import com.example.appdevproject26s.user.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.kevincianfarini.alchemist.scalar.toPower
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
    private val userRepository: UserRepository,
    private val unreadChatsStore: UnreadChatsStore,
    private val activeChatStore: ActiveChatStore,
    private val appNotificationManager: AppNotificationManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val chatIdArg: Long? = savedStateHandle.get<String>("chatId")?.toLongOrNull()

    val isLoggedIn: StateFlow<Boolean> = authRepo.isLoggedInFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )
    val currentUser = userRepository.currentUser

    private val _chats = MutableStateFlow<List<ChatResponse>>(emptyList())
    val chats: StateFlow<List<ChatResponse>> = _chats.asStateFlow()

    private val _selectedChat = MutableStateFlow<ChatResponse?>(null)
    val selectedChat: StateFlow<ChatResponse?> = _selectedChat.asStateFlow()

    val unreadChatIds: StateFlow<Set<Long>> = currentUser
        .flatMapLatest { user ->
            val userId = user?.id?.toString() ?: ""
            unreadChatsStore.getUnreadIdsFlowForUser(userId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptySet()
        )

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

    init {
        viewModelScope.launch {
            authRepo.isLoggedInFlow.collect { loggedIn ->
                if (loggedIn) {
                    fetchChats()
                    fetchFriends()
                } else {
                    _chats.value = emptyList()
                    _friends.value = emptyList()
                }
            }
        }
    }


    fun fetchChats() {
        viewModelScope.launch {
            messagingRepository.getChats().fold(
                onSuccess = { chatList ->
                    _chats.value = chatList
                    checkAndSelectIncomingChat()
                },
                onFailure = {
                    _chats.value = emptyList()
                }
            )
        }
    }

    private fun checkAndSelectIncomingChat() {
        if (chatIdArg != null && _selectedChat.value == null) {
            val matchingChat = _chats.value.find { it.id == chatIdArg }
            if (matchingChat != null) {
                selectChat(matchingChat)
            }
        }
    }

    fun selectChat(chat: ChatResponse?) {
        _selectedChat.value = chat
        activeChatStore.setActiveChatId(chat?.id)
        if (chat?.id != null) {
            val userId = currentUser.value?.id ?: return

            viewModelScope.launch {
                unreadChatsStore.removeUnreadChatId(userId = userId, chatId = chat.id)
                appNotificationManager.dismissMessageNotification(chat.id)
            }
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
        activeChatStore.setActiveChatId(null)
    }
}
