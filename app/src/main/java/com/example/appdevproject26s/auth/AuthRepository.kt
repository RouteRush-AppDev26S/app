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
    @ApplicationContext private val context: Context
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

    suspend fun login(email: String, pass: String): Result<String> {
        return try {
            val mockToken = "mock_jwt_token_12345"
            saveToken(mockToken)
            Result.success(mockToken)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(email: String, pass: String): Result<String> {
        return try {
            val mockToken = "mock_jwt_token_12345"
            saveToken(mockToken)
            Result.success(mockToken)
        } catch (e: Exception) {
            Result.failure(e)
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