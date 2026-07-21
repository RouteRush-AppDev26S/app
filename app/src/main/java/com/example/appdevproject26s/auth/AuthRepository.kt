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

private val Context.authDataStore by preferencesDataStore(name = "secure_auth_prefs")

@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authApiService: AuthApiService
) {
    private val secureManager = SecureTokenManager()

    private object PreferencesKeys {
        val ENCRYPTED_JWT_TOKEN = stringPreferencesKey("encrypted_jwt_token")
    }

    // Expose the decrypted token reactively
    val tokenFlow: Flow<String?> = context.authDataStore.data.map { preferences ->
        preferences[PreferencesKeys.ENCRYPTED_JWT_TOKEN]?.let { encryptedStr ->
            secureManager.decrypt(encryptedStr)
        }
    }

    suspend fun login(username: String, pass: String): Result<String> {
        return try {
            val request = LoginRequest(username = username, password = pass)
            val token = authApiService.login(request)

            saveToken(token)
            Result.success(token)
        } catch (e: retrofit2.HttpException) {
            // Extract the exact error message sent from your Spring Boot backend
            val errorBody = e.response()?.errorBody()?.string()
            val errorMessage = if (!errorBody.isNullOrBlank()) {
                errorBody.replace("\"", "") // Cleans up string quotes if returned as raw JSON text
            } else {
                e.localizedMessage ?: "Authentication failed"
            }
            Result.failure(Exception(errorMessage))
        } catch (e: Exception) {
            // Catches regular network drops, timeouts, etc.
            Result.failure(Exception("Network error: Check your connection"))
        }
    }

    suspend fun register(email: String, username: String, pass: String): Result<String> {
        return try {
            val request = RegisterRequest(email = email, username = username, password = pass)
            val token = authApiService.register(request)

            saveToken(token)
            Result.success(token)
        } catch (e: retrofit2.HttpException) {
            // Extract the exact error message sent from your Spring Boot backend
            val errorBody = e.response()?.errorBody()?.string()
            val errorMessage = if (!errorBody.isNullOrBlank()) {
                errorBody.replace("\"", "") // Cleans up string quotes if returned as raw JSON text
            } else {
                e.localizedMessage ?: "Authentication failed"
            }
            Result.failure(Exception(errorMessage))
        } catch (e: Exception) {
            // Catches regular network drops, timeouts, etc.
            Result.failure(Exception("Network error: Check your connection"))
        }
    }

    private suspend fun saveToken(token: String) {
        val encryptedToken = secureManager.encrypt(token)
        context.authDataStore.edit { preferences ->
            preferences[PreferencesKeys.ENCRYPTED_JWT_TOKEN] = encryptedToken
        }
    }

    suspend fun logout() {
        context.authDataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.ENCRYPTED_JWT_TOKEN)
        }
    }
}