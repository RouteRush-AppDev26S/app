package com.example.appdevproject26s.network

import android.util.Log
import com.example.appdevproject26s.auth.AuthRepository
import com.example.appdevproject26s.social.messaging.InboxManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    private val authRepository: AuthRepository,
    private val webSocketManager: WebSocketManager,
    private val inboxManager: InboxManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        scope.launch {
            authRepository.isLoggedInFlow.combine(authRepository.tokenFlow) { isLoggedIn, token ->
                Pair(isLoggedIn, token)
            }.collectLatest { (isLoggedIn, token) ->
                if (isLoggedIn && !token.isNullOrBlank()) {
                    if (!webSocketManager.isConnected) {
                        webSocketManager.connect(jwtToken = token)
                    }
                    inboxManager.startListening()
                } else {
                    terminateSession()
                }
            }
        }
    }

    fun terminateSession() {
        inboxManager.stopListening()
        if (webSocketManager.isConnected) {
            webSocketManager.disconnect()
        }
    }
}