package com.callbridge.master

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.callbridge.R
import com.callbridge.audio.BridgeService
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class MasterActivity : AppCompatActivity() {
    private var bound = false
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) { bound = true }
        override fun onServiceDisconnected(name: ComponentName) { bound = false }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_master)

        val etPhone = findViewById<TextInputEditText>(R.id.etPhoneNumber)
        val etServer = findViewById<TextInputEditText>(R.id.etServerUrl)
        val tvRoomCode = findViewById<TextView>(R.id.tvRoomCode)
        val btnDial = findViewById<MaterialButton>(R.id.btnDial)
        val btnBridge = findViewById<MaterialButton>(R.id.btnStartBridge)

        btnDial.setOnClickListener {
            val number = etPhone.text.toString().trim()
            if (number.isBlank()) { Toast.makeText(this,"Enter a phone number",Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CALL_PHONE), 1); return@setOnClickListener
            }
            startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:$number")))
        }

        btnBridge.setOnClickListener {
            val perms = arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CALL_PHONE)
            val missing = perms.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
            if (missing.isNotEmpty()) { ActivityCompat.requestPermissions(this, missing.toTypedArray(), 2); return@setOnClickListener }
            val serverUrl = etServer.text.toString().trim().ifBlank { "wss://callbridge-relay.fly.dev" }
            val intent = Intent(this, BridgeService::class.java).apply {
                putExtra("role", "master"); putExtra("serverUrl", serverUrl)
            }
            ContextCompat.startForegroundService(this, intent)
            bindService(intent, connection, BIND_AUTO_CREATE)
            val code = (1..8).map { ('A'..'Z').random() }.joinToString("")
            tvRoomCode.text = "Room code: $code"
            Toast.makeText(this, "Bridge started! Share code: $code", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() { super.onDestroy(); if (bound) unbindService(connection) }
}
