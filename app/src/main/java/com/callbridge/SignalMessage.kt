package com.callbridge.signaling

data class SignalMessage(
    val type: String,       // "join", "offer", "answer", "ice", "leave"
    val room: String = "",
    val role: String = "",  // "master" or "slave"
    val sdp: String = "",
    val candidate: String = "",
    val sdpMid: String = "",
    val sdpMLineIndex: Int = 0,
    val from: String = ""
)
