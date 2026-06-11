package com.callbridge.signaling

import com.google.gson.Gson
import okhttp3.*
import kotlinx.coroutines.*

class SignalingClient(
    private val serverUrl: String,
    private val onMessage: (SignalMessage) -> Unit,
    private val onConnected: () -> Unit
) {
    private val client = OkHttpClient()
    private val gson = Gson()
    private var webSocket: WebSocket? = null

    fun connect() {
        val request = Request.Builder().url(serverUrl).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) { onConnected() }
            override fun onMessage(ws: WebSocket, text: String) {
                try { onMessage(gson.fromJson(text, SignalMessage::class.java)) } catch (_: Exception) {}
            }
            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {}
        })
    }

    fun send(msg: SignalMessage) { webSocket?.send(gson.toJson(msg)) }

    fun disconnect() { webSocket?.close(1000, "bye"); client.dispatcher.executorService.shutdown() }
}
