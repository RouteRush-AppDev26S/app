package com.example.appdevproject26s.social.messaging

import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.appdevproject26s.modules.AppNotificationManager
import com.example.appdevproject26s.profile.UserRepository
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InboxManager @Inject constructor(
    private val userRepository: UserRepository,
    private val messagingRepository: MessagingRepository,
    private val activeChatStore: ActiveChatStore,
    private val unreadChatsStore: UnreadChatsStore,
    private val appNotificationManager: AppNotificationManager
) {
    private var globalInboxSubscription: Disposable? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private fun isAppInForeGround(): Boolean {
        return ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
    }
    fun startListening() {
        if (globalInboxSubscription != null) return

        scope.launch {
            userRepository.getCurrentUser().fold(
                onSuccess = { user ->
                    val currentUserId = user.id ?: return@fold
                    val userIdString = currentUserId.toString()

                    globalInboxSubscription = messagingRepository.observeInbox(currentUserId) { newMessage ->
                        val chatId = newMessage.chatId
                        val senderId = newMessage.senderId

                        if (chatId != null && senderId != currentUserId) {
                            val isAppForeground = isAppInForeGround()
                            val isCurrentlyViewingChat = (activeChatStore.activeChatId.value == chatId)

                            if (!isAppForeground || !isCurrentlyViewingChat) {
                                appNotificationManager.showMessageNotification(
                                    senderName = newMessage.senderUsername ?: "unknown",
                                    messageText = newMessage.content ?: "",
                                    chatId = chatId
                                )
                                scope.launch {
                                    unreadChatsStore.addUnreadChatId(userId = userIdString, chatId = chatId)
                                }
                            }
                        }
                    }
                },
                onFailure = { error ->
                    Log.e("WEBSOCKET_DEBUG", "-> Failed to setup global inbox: ${error.message}")
                }
            )
        }
    }

    fun stopListening() {
        globalInboxSubscription?.dispose()
        globalInboxSubscription = null
    }
}