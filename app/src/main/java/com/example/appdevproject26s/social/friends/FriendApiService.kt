package com.example.appdevproject26s.social.friends

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface FriendApiService {

    @GET("friendships")
    suspend fun getFriends(): List<FriendshipResponse>

    @GET("friendships/pending")
    suspend fun getPendingRequests(): List<FriendshipResponse>

    @POST("friendships")
    suspend fun sendRequest(
        @Body request: SendRequestRequest
    ): FriendshipResponse

    @POST("friendships/{id}/accept")
    suspend fun acceptRequest(
        @Path("id") friendshipId: Long
    ): FriendshipResponse

    @DELETE("friendships/{id}")
    suspend fun declineOrCancel(
        @Path("id") friendshipId: Long
    )
}