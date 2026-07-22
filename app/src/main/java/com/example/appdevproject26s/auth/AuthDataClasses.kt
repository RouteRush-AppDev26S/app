package com.example.appdevproject26s.auth

// Matches public record LoginRequest(String username, String password)
data class LoginRequest(
    val username: String, // Note: Backend expects username, not email, for login!
    val password: String
)

// Matches public record RegisterRequest(String email, String username, String password)
data class RegisterRequest(
    val email: String,
    val username: String,
    val password: String
)