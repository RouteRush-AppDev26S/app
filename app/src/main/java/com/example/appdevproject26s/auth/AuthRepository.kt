package com.example.appdevproject26s.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authApiService: AuthApiService
) {
    private val secureManager = SecureTokenManager()

    // Expose the decrypted token reactively
    val tokenFlow: Flow<String?> = context.authDataStore.data.map { preferences ->
        preferences[AuthPreferencesKeys.ENCRYPTED_JWT_TOKEN]?.let { encryptedStr ->
            secureManager.decrypt(encryptedStr)
        }
    }

    val isLoggedInFlow: Flow<Boolean> = tokenFlow.map { token ->
        if (token.isNullOrBlank()) {
            false
        } else {
            !isTokenExpired(token)
        }
    }

    suspend fun login(username: String, pass: String): Result<String> {
        return try {
            val request = LoginRequest(username = username, password = pass)
            val token = authApiService.login(request)

            saveToken(token)
            Result.success(token)
        } catch (e : Exception) {
            Result.failure(parseHttpError(e, "Authentication failed"))
        }
    }

    suspend fun register(email: String, username: String, pass: String): Result<String> {
        return try {
            val request = RegisterRequest(email = email, username = username, password = pass)
            val token = authApiService.register(request)

            saveToken(token)
            Result.success(token)
        } catch (e: Exception) {
            Result.failure(parseHttpError(e, "Registration failed"))
        }
    }

    private suspend fun saveToken(token: String) {
        val encryptedToken = secureManager.encrypt(token)
        context.authDataStore.edit { preferences ->
            preferences[AuthPreferencesKeys.ENCRYPTED_JWT_TOKEN] = encryptedToken
        }
    }

    suspend fun logout() {
        context.authDataStore.edit { preferences ->
            preferences.remove(AuthPreferencesKeys.ENCRYPTED_JWT_TOKEN)
        }
    }
}

private fun isTokenExpired(token: String): Boolean {
    return try {
        val parts = token.split(".")
        if (parts.size != 3) return true

        // Decode the payload (middle part)
        val payloadBytes = android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)
        val payloadString = String(payloadBytes, Charsets.UTF_8)

        val jsonObject = org.json.JSONObject(payloadString)
        val expirationTimeSeconds = jsonObject.optLong("exp", 0)
        val currentTimeSeconds = System.currentTimeMillis() / 1000

        currentTimeSeconds >= expirationTimeSeconds
    } catch (e: Exception) {
        true
    }
}