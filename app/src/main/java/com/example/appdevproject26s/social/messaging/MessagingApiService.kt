package com.example.appdevproject26s.social.messaging

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ChatApiService {
    @GET("chats")
    suspend fun getMyChats(): List<Chat>

    @POST("chats/direct")
    suspend fun createDirectChat(@Body request: CreateDirectChatRequest): Chat

    @POST("chats/group")
    suspend fun createGroupChat(@Body request: CreateGroupChatRequest): Chat

    @GET("chats/{id}/messages")
    suspend fun getMessages(@Path("id") chatId: Long): List<ChatMessage>

    @POST("chats/{id}/messages")
    suspend fun postMessage(
        @Path("id") chatId: Long,
        @Body request: PostMessageRequest
    ): ChatMessage
}