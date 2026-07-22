package com.example.appdevproject26s.social.friends

import com.example.appdevproject26s.auth.parseHttpError
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FriendRepository @Inject constructor(
    private val friendApiService: FriendApiService
) {

    suspend fun getFriends(): Result<List<FriendshipResponse>> {
        return try {
            val friendships = friendApiService.getFriends()
            Result.success(friendships)
        } catch (e: Exception) {
            Result.failure(parseHttpError(e, "Failed to load friends"))
        }
    }

    suspend fun getPendingRequests(): Result<List<FriendshipResponse>> {
        return try {
            val requests = friendApiService.getPendingRequests()
            Result.success(requests)
        } catch (e: Exception) {
            Result.failure(parseHttpError(e, "Failed to load pending requests"))
        }
    }

    suspend fun sendRequest(username: String): Result<FriendshipResponse> {
        return try {
            val request = SendRequestRequest(username = username)
            val friendship = friendApiService.sendRequest(request)
            Result.success(friendship)
        } catch (e: Exception) {
            Result.failure(parseHttpError(e, "Failed to send friend request"))
        }
    }

    suspend fun acceptRequest(friendshipId: Long): Result<FriendshipResponse> {
        return try {
            val friendship = friendApiService.acceptRequest(friendshipId)
            Result.success(friendship)
        } catch (e: Exception) {
            Result.failure(parseHttpError(e, "Failed to accept friend request"))
        }
    }

    suspend fun declineOrCancel(friendshipId: Long): Result<Unit> {
        return try {
            friendApiService.declineOrCancel(friendshipId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(parseHttpError(e, "Failed to remove friendship"))
        }
    }
}