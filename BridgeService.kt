package com.callbridge.audio

import android.app.*
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.callbridge.R
import com.callbridge.signaling.SignalingClient
import com.callbridge.signaling.SignalMessage

class BridgeService : Service() {

    inner class LocalBinder : Binder() { fun getService() = this@BridgeService }
    private val binder = LocalBinder()

    private var signalingClient: SignalingClient? = null
    private val audioBridge = AudioBridge()

    override fun onBind(intent: Intent): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, buildNotification())

        val role = intent?.getStringExtra("role") ?: "slave"
        val roomCode = intent?.getStringExtra("roomCode") ?: ""
        val serverUrl = intent?.getStringExtra("serverUrl") ?: "wss://callbridge-relay.fly.dev"

        signalingClient = SignalingClient(serverUrl, onMessage = { msg ->
            handleSignal(msg)
        }, onConnected = {
            signalingClient?.send(SignalMessage(type = "join", room = roomCode, role = role))
        })
        signalingClient?.connect()

        audioBridge.start()

        return START_STICKY
    }

    private fun handleSignal(msg: SignalMessage) {
        // Signaling handled per peer in PeerManager
    }

    private fun buildNotification(): Notification {
        val channelId = "bridge_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "CallBridge", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("CallBridge Active")
            .setContentText("Bridging calls…")
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        audioBridge.stop()
        signalingClient?.disconnect()
    }
}
