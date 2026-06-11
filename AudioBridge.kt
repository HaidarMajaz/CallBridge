package com.callbridge.audio

import android.media.*
import kotlinx.coroutines.*

class AudioBridge {
    private val sampleRate = 16000
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate,
        AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)

    private var recorder: AudioRecord? = null
    private var track: AudioTrack? = null
    private var recording = false
    private var scope = CoroutineScope(Dispatchers.IO)

    var onAudioData: ((ByteArray) -> Unit)? = null

    fun start() {
        recording = true
        recorder = AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION, sampleRate,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize)

        track = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            .setAudioFormat(AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM).build()

        recorder?.startRecording()
        track?.play()

        scope.launch {
            val buf = ByteArray(bufferSize)
            while (recording) {
                val read = recorder?.read(buf, 0, buf.size) ?: -1
                if (read > 0) onAudioData?.invoke(buf.copyOf(read))
            }
        }
    }

    fun playAudio(data: ByteArray) { track?.write(data, 0, data.size) }

    fun stop() {
        recording = false
        scope.cancel()
        recorder?.stop(); recorder?.release(); recorder = null
        track?.stop(); track?.release(); track = null
    }
}
