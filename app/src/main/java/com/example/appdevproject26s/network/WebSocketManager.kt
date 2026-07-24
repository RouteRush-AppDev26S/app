package com.example.appdevproject26s.network

import android.util.Log
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import ua.naiksoftware.stomp.Stomp
import ua.naiksoftware.stomp.StompClient
import ua.naiksoftware.stomp.dto.StompHeader
import ua.naiksoftware.stomp.dto.LifecycleEvent
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebSocketManager @Inject constructor() {
    private lateinit var stompClient: StompClient
    private val compositeDisposable = CompositeDisposable()
    var isConnected: Boolean = false
        private set

    fun connect(jwtToken: String? = null) {
        if (::stompClient.isInitialized && isConnected) return

        stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, BACKEND_WS_URL)

        val headers = mutableListOf<StompHeader>()
        if (!jwtToken.isNullOrEmpty()) {
            headers.add(StompHeader("Authorization", "Bearer $jwtToken"))
        }

        val lifecycleDisposable = stompClient.lifecycle()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { event ->
                when (event.type) {
                    LifecycleEvent.Type.OPENED -> {
                        isConnected = true
                        Log.d("WebSocketManager", "Connected successfully")
                    }
                    LifecycleEvent.Type.CLOSED,
                    LifecycleEvent.Type.ERROR -> {
                        isConnected = false
                        Log.e("WebSocketManager", "Connection dropped or errored", event.exception)
                    }
                    else -> {}
                }
            }
        compositeDisposable.add(lifecycleDisposable)
        stompClient.connect(headers)
    }

    fun subscribeToChat(destination: String, onMessageReceived: (String) -> Unit): Disposable {
        return stompClient.topic(destination)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ topicMessage ->
                onMessageReceived(topicMessage.payload)
            }, { error ->
                Log.e("WebSocketManager", "Error subscribing to $destination", error)
            })
    }

    fun sendMessage(destination: String, jsonPayload: String) {
        val sendDisposable = stompClient.send(destination, jsonPayload)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                Log.d("WebSocketManager", "Message sent to $destination")
            }, { error ->
                Log.e("WebSocketManager", "Failed to send message to $destination", error)
            })
        compositeDisposable.add(sendDisposable)
    }

    fun disconnect() {
        compositeDisposable.clear()
        if (::stompClient.isInitialized) {
            stompClient.disconnect()
        }
        isConnected = false
    }
}