package com.example.appdevproject26s.auth

import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): String // Returns the token string directly

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): String // Returns the token string directly
}