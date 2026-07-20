package com.example.appdevproject26s.social

import javax.inject.Inject

class MessagingRepository @Inject constructor() {

    suspend fun getChats(): List<String> {
        // check local db -> make call to backend to check for update
        return listOf("Fetched Chat A", "Fetched Chat B", "Fetched Chat C", "Just placeholders")
    }
}