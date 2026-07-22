package com.example.appdevproject26s.auth

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.authDataStore by preferencesDataStore(name = "secure_auth_prefs")

object AuthPreferencesKeys {
    val ENCRYPTED_JWT_TOKEN = stringPreferencesKey("encrypted_jwt_token")
}