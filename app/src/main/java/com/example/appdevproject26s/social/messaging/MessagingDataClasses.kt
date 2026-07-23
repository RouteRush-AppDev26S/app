package com.example.appdevproject26s.social.messaging

import com.example.appdevproject26s.route.Pin
import com.example.appdevproject26s.route.Route
import com.example.appdevproject26s.user.UserProfileResponse

data class ChatResponse(
    val id: Long,
    val name: String,
    val isGroup: Boolean,
    val createdAt: String,
)

data class Chat(
    val id: Long?,
    val name: String?,
    val isGroup: Boolean,
    val createdBy: UserProfileResponse?,
    val createdAt: String?
)

data class ChatMessage(
    val id: Long?,
    val chat: Chat?,
    val sender: UserProfileResponse?,
    val content: String?,
    val route: Route?,
    val pin: Pin?,
    val sentAt: String?
)

data class CreateDirectChatRequest(val username: String)
data class CreateGroupChatRequest(val name: String, val usernames: List<String>)
data class PostMessageRequest(val content: String?, val routeId: Long?, val pinId: Long?)