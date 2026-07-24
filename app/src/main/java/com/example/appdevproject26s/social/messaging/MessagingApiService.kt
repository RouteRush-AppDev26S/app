package com.example.appdevproject26s.social.messaging

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ChatApiService {
    @GET("chats")
    suspend fun getMyChats(): List<ChatResponse>

    @POST("chats/direct")
    suspend fun createDirectChat(@Body request: CreateDirectChatRequest): ChatResponse

    @POST("chats/group")
    suspend fun createGroupChat(@Body request: CreateGroupChatRequest): ChatResponse

    @GET("chats/{id}/messages")
    suspend fun getMessages(@Path("id") chatId: Long): List<ChatMessageResponse>
}