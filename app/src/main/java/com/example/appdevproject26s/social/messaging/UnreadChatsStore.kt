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
    fun getUnreadIdsFlowForUser(userId: String): Flow<Set<Long>> {
        return dataStore.data.map { preferences ->
            val entries = preferences[UNREAD_CHATS_KEY] ?: emptySet()
            entries.mapNotNull { entry ->
                val parts = entry.split(":")
                if (parts.size == 2 && parts[0] == userId) {
                    parts[1].toLongOrNull()
                } else {
                    null
                }
            }.toSet()

        }
    }
    suspend fun addUnreadChatId(userId: String, chatId: Long) {
        dataStore.edit { preferences ->
            val currentEntries = preferences[UNREAD_CHATS_KEY] ?: emptySet()
            val newEntry = "$userId:$chatId"
            preferences[UNREAD_CHATS_KEY] = currentEntries + newEntry
        }
    }

    suspend fun removeUnreadChatId(userId: Long, chatId: Long) {
        dataStore.edit { preferences ->
            val currentSet = preferences[UNREAD_CHATS_KEY] ?: emptySet()
            val targetEntry = "$userId:$chatId"
            preferences[UNREAD_CHATS_KEY] = currentSet - targetEntry
        }
    }

    suspend fun clearAll() {
        dataStore.edit { preferences ->
            preferences.remove(UNREAD_CHATS_KEY)
        }
    }
}