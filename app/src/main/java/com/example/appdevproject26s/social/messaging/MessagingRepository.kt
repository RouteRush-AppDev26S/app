package com.example.appdevproject26s.social.messaging

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessagingRepository @Inject constructor(
    private val chatApiService: ChatApiService
) {

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

    suspend fun postMessage(chatId: Long, content: String?, routeId: Long? = null, pinId: Long? = null): Result<ChatMessageResponse> {
        return try {
            val message = chatApiService.postMessage(chatId, PostMessageRequest(content, routeId, pinId))
            Result.success(message)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}