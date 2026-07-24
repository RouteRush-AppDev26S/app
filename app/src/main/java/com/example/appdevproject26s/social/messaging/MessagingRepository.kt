package com.example.appdevproject26s.social.messaging

import com.example.appdevproject26s.network.WebSocketManager
import com.google.gson.Gson
import io.reactivex.disposables.Disposable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessagingRepository @Inject constructor(
    private val chatApiService: ChatApiService,
    private val webSocketManager: WebSocketManager
) {
    private val gson = Gson()

    suspend fun getChats(): Result<List<ChatResponse>> {
        return try {
            val chats = chatApiService.getMyChats()
            Result.success(chats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createDirectChat(username: String): Result<ChatResponse> {
        return try {
            val chat = chatApiService.createDirectChat(CreateDirectChatRequest(username))
            Result.success(chat)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createGroupChat(name: String, usernames: List<String>): Result<ChatResponse> {
        return try {
            val chat = chatApiService.createGroupChat(CreateGroupChatRequest(name, usernames))
            Result.success(chat)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMessages(chatId: Long): Result<List<ChatMessageResponse>> {
        return try {
            val messages = chatApiService.getMessages(chatId)
            Result.success(messages)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun postMessage(chatId: Long, content: String?, routeId: Long? = null, pinId: Long? = null) {
        val request = PostMessageRequest(content, routeId, pinId)
        val jsonPayload = gson.toJson(request)
        webSocketManager.sendMessage("/app/chat/$chatId", jsonPayload)
    }

    fun observeChat(chatId: Long, onMessageReceived: (ChatMessageResponse) -> Unit): Disposable {
        return webSocketManager.subscribeToChat("/topic/chat/$chatId") { jsonPayload ->
            try {
                val response = gson.fromJson(jsonPayload, ChatMessageResponse::class.java)
                onMessageReceived(response)
            } catch (e: Exception) {
                // Handle parsing error if needed
            }
        }
    }
}