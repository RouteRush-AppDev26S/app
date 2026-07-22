package com.example.appdevproject26s.auth

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    @ApplicationContext private val context: Context
) : Interceptor {

    private val secureManager = SecureTokenManager()

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Fetch token synchronously without involving repositories or ViewModels
        val token = runBlocking {
            try {
                val encrypted = context.authDataStore.data.firstOrNull()?.get(AuthPreferencesKeys.ENCRYPTED_JWT_TOKEN)
                if (!encrypted.isNullOrBlank()) secureManager.decrypt(encrypted) else null
            } catch (e: Exception) {
                null
            }
        }

        val request = if (!token.isNullOrBlank()) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            originalRequest
        }

        return chain.proceed(request)
    }
}