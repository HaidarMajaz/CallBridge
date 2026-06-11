package com.callbridge.slave

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.callbridge.R
import com.callbridge.audio.BridgeService
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class SlaveActivity : AppCompatActivity() {
    private var bound = false
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) { bound = true }
        override fun onServiceDisconnected(name: ComponentName) { bound = false }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_slave)

        val etCode = findViewById<TextInputEditText>(R.id.etRoomCode)
        val etServer = findViewById<TextInputEditText>(R.id.etServerUrl)
        val btnJoin = findViewById<MaterialButton>(R.id.btnJoin)

        btnJoin.setOnClickListener {
            val code = etCode.text.toString().trim().uppercase()
            if (code.length != 8) { Toast.makeText(this,"Enter the 8-character room code",Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1); return@setOnClickListener
            }
            val serverUrl = etServer.text.toString().trim().ifBlank { "wss://callbridge-relay.fly.dev" }
            val intent = Intent(this, BridgeService::class.java).apply {
                putExtra("role", "slave"); putExtra("roomCode", code); putExtra("serverUrl", serverUrl)
            }
            ContextCompat.startForegroundService(this, intent)
            bindService(intent, connection, BIND_AUTO_CREATE)
            Toast.makeText(this, "Joining room $code…", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() { super.onDestroy(); if (bound) unbindService(connection) }
}
