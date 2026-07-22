package com.example.appdevproject26s.social.friends

enum class FriendshipStatus {
    PENDING,
    ACCEPTED
}

data class FriendshipResponse(
    val id: Long,
    val otherUserId: Long,
    val otherUsername: String,
    val status: FriendshipStatus,
    val createdAt: String // Serialized from backend Instant (e.g., ISO-8601 String)
)

data class AddFriendRequest(
    val friendUsername: String
)

data class SendRequestRequest(
    val username: String
)