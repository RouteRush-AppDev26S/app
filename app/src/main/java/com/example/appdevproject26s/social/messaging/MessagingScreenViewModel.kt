package com.example.appdevproject26s.social.messaging

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
import kotlin.collections.emptyList

@HiltViewModel
class MessagingScreenViewModel @Inject constructor(
    private val messagingRepository: MessagingRepository,
    private val authRepo: AuthRepository

) : ViewModel() {

    val isLoggedIn: StateFlow<Boolean> = authRepo.isLoggedInFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )
    private val _chats = MutableStateFlow<List<Chat>>(emptyList())
    val chats: StateFlow<List<Chat>> = _chats.asStateFlow()

    init {
        fetchChats()
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
}
