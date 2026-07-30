package com.example.appdevproject26s.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
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

    val savedUsernameFlow: Flow<String?> = context.authDataStore.data.map { preferences ->
        preferences[AuthPreferencesKeys.SAVED_USERNAME]
    }

    val isLoggedInFlow: Flow<Boolean> = tokenFlow.map { token ->
        if (token.isNullOrBlank()) {
            false
        } else if (!isTokenExpired(token)) {
            true
        } else {
            clearToken()
            false
        }
    }

    suspend fun login(username: String, pass: String): Result<String> {
        return try {
            val request = LoginRequest(username = username, password = pass)
            val token = authApiService.login(request)

            saveTokenAndUsername(token, username)
            Result.success(token)
        } catch (e : Exception) {
            Result.failure(parseHttpError(e, "Authentication failed"))
        }
    }

    suspend fun register(email: String, username: String, pass: String): Result<String> {
        return try {
            val request = RegisterRequest(email = email, username = username, password = pass)
            val token = authApiService.register(request)

            saveTokenAndUsername(token, username)
            Result.success(token)
        } catch (e: Exception) {
            Result.failure(parseHttpError(e, "Registration failed"))
        }
    }

    private suspend fun saveTokenAndUsername(token: String, username: String) {
        val encryptedToken = secureManager.encrypt(token)
        context.authDataStore.edit { preferences ->
            preferences[AuthPreferencesKeys.ENCRYPTED_JWT_TOKEN] = encryptedToken
            preferences[AuthPreferencesKeys.SAVED_USERNAME] = username
        }
    }

    suspend fun clearToken() {
        context.authDataStore.edit { preferences ->
            preferences.remove(AuthPreferencesKeys.ENCRYPTED_JWT_TOKEN)
        }
    }

    suspend fun logout() {
        context.authDataStore.edit { preferences ->
            preferences.remove(AuthPreferencesKeys.ENCRYPTED_JWT_TOKEN)
            preferences.remove(AuthPreferencesKeys.SAVED_USERNAME)
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