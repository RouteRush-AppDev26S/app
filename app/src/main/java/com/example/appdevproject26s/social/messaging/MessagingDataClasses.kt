package com.example.appdevproject26s.social.messaging

data class ChatResponse(
    val id: Long,
    val name: String,
    val isGroup: Boolean,
    val createdAt: String,
)

data class ChatMessageResponse(
    val id: Long?,
    val senderId: Long?,
    val senderUsername: String?,
    val content: String?,
    val routeId: Long?,
    val pinId: Long?,
    val sentAt: String?
)

data class CreateDirectChatRequest(val username: String)
data class CreateGroupChatRequest(val name: String, val usernames: List<String>)
data class PostMessageRequest(val content: String?, val routeId: Long?, val pinId: Long?)