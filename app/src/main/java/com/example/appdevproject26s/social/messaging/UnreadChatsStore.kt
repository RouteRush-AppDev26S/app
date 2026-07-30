package com.example.appdevproject26s.social.messaging

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UnreadChatsStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private val UNREAD_CHATS_KEY = stringSetPreferencesKey("unread_chat_ids")

    // Expose the unread IDs as a reactive Flow of Set<Long>
    val unreadIdsFlow: Flow<Set<Long>> = dataStore.data
        .map { preferences ->
            val stringSet = preferences[UNREAD_CHATS_KEY] ?: emptySet()
            stringSet.mapNotNull { it.toLongOrNull() }.toSet()
        }

    suspend fun addUnreadChatId(chatId: Long) {
        dataStore.edit { preferences ->
            val currentSet = preferences[UNREAD_CHATS_KEY] ?: emptySet()
            preferences[UNREAD_CHATS_KEY] = currentSet + chatId.toString()
        }
    }

    suspend fun removeUnreadChatId(chatId: Long) {
        dataStore.edit { preferences ->
            val currentSet = preferences[UNREAD_CHATS_KEY] ?: emptySet()
            preferences[UNREAD_CHATS_KEY] = currentSet - chatId.toString()
        }
    }

    suspend fun clearAll() {
        dataStore.edit { preferences ->
            preferences.remove(UNREAD_CHATS_KEY)
        }
    }
}