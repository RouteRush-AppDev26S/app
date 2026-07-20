package com.example.appdevproject26s.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.collections.emptyList

@HiltViewModel
class MessagingScreenViewModel @Inject constructor(
    private val repository: MessagingRepository

) : ViewModel() {
    private val _chats = MutableStateFlow<List<String>>(emptyList())
    val chats = _chats.asStateFlow()

    init {
        loadChats()
    }

    private fun loadChats() {
        viewModelScope.launch {
            _chats.value = repository.getChats()
        }
    }
}
