# CallBridge — SIM + Internet Call Merger

Bridge a real SIM phone call with internet callers. One phone is the **master** (makes the SIM call + runs the bridge). Any number of **slave** phones join via the internet app.

```
SIM caller (no app needed)
       ↕  cellular
 ┌─────────────┐
 │ MASTER phone │  ← runs the bridge inside the app
 └─────────────┘
       ↕  internet (WebRTC)
 ┌─────────────┐   ┌─────────────┐
 │ SLAVE phone │   │ SLAVE phone │
 └─────────────┘   └─────────────┘
```

---

## How it works

1. Master dials a SIM call normally via Android dialer
2. Master starts the bridge — the app captures microphone audio (which includes the SIM caller's voice via speakerphone) and streams it to all slaves via WebRTC
3. Slave phones connect by entering an 8-character room code
4. Slaves hear the SIM caller; the SIM caller hears the slaves via the master's speaker

---

## Project structure

```
CallBridge/
├── app/                        # Android app (Kotlin)
│   └── src/main/java/com/callbridge/
│       ├── MainActivity.kt         — role picker (master / slave)
│       ├── master/
│       │   └── MasterActivity.kt   — dial SIM + start bridge
│       ├── slave/
│       │   └── SlaveActivity.kt    — join bridge via room code
│       ├── audio/
│       │   ├── AudioBridge.kt      — PCM capture + mixing
│       │   ├── PeerManager.kt      — WebRTC peer connection
│       │   └── BridgeService.kt    — foreground service (keeps bridge alive)
│       └── signaling/
│           ├── SignalingClient.kt  — WebSocket client
│           └── SignalMessage.kt    — message types
└── signaling-server/           # Node.js relay (deploy once, free)
    ├── server.js
    ├── package.json
    └── fly.toml
```

---

## Setup

### 1. Deploy the signaling server (one time, free)

```bash
cd signaling-server
npm install

# Option A: fly.io (free tier, recommended)
brew install flyctl          # or curl -L https://fly.io/install.sh | sh
fly auth signup
fly launch --name callbridge-relay
fly deploy
# Your server is now at: wss://callbridge-relay.fly.dev

# Option B: run locally for testing
node server.js
# Use: ws://YOUR_LOCAL_IP:8080
```

### 2. Build the Android app

Open `CallBridge/` in Android Studio (Hedgehog or newer).

Required: Android SDK 34, Kotlin 1.9, Java 17.

```bash
./gradlew assembleDebug
# APK at: app/build/outputs/apk/debug/app-debug.apk
```

### 3. Install on both phones

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 4. Using the app

**On the master phone:**
1. Open CallBridge → tap "Master phone"
2. Enter the phone number you want to SIM-call → tap "Dial SIM call"
3. Once the SIM call connects, go back to the app
4. Tap "Start bridge" (optionally enter your signaling server URL)
5. The 8-character room code appears — share it with your slaves

**On each slave phone:**
1. Open CallBridge → tap "Slave phone"
2. Enter the room code from the master
3. Tap "Join bridge"
4. You will hear everything — the SIM caller and the master

---

## Permissions required

| Permission | Why |
|---|---|
| `RECORD_AUDIO` | Capture mic / call audio |
| `CALL_PHONE` | Dial the SIM call |
| `READ_PHONE_STATE` | Detect active call state |
| `MODIFY_AUDIO_SETTINGS` | Route audio through speaker |
| `INTERNET` | WebRTC + signaling |
| `FOREGROUND_SERVICE` | Keep bridge alive in background |

---

## Known limitations & next steps

### Current limitations
- **Android only** — iOS restricts call audio capture in third-party apps
- Audio quality depends on speakerphone + mic proximity; for clean audio, a headset on the master phone improves isolation
- The free STUN servers work for most networks; behind strict corporate NAT a TURN server is needed

### Improving audio isolation (advanced)
On Android 10+, use `AudioPlaybackCaptureConfiguration` to capture call audio directly instead of through the microphone. This requires the `CAPTURE_AUDIO_OUTPUT` permission (needs device owner or root on most phones):

```kotlin
val config = AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
    .addMatchingUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
    .build()
val recorder = AudioRecord.Builder()
    .setAudioPlaybackCaptureConfig(config)
    .build()
```

### Adding a free TURN server
Edit `PeerManager.kt` and uncomment the TURN server lines. Free options:
- `openrelay.metered.ca` (OpenRelay, free tier)
- Self-host `coturn` on any $5/month VPS

---

## Architecture diagram

```
Master phone
├── AudioRecord (VOICE_COMMUNICATION source)
│   └── Captures mic + SIM caller audio
├── AudioTrack (VOICE_COMMUNICATION usage)
│   └── Plays slave audio → SIM caller hears it
├── BridgeService (foreground, keeps alive)
│   ├── SignalingClient → wss://relay
│   └── PeerManager × N (one per slave)
│       ├── RTCPeerConnection
│       ├── Sends: local mic PCM → slave
│       └── Receives: slave PCM → AudioTrack

Slave phone
├── BridgeService
│   ├── SignalingClient → wss://relay
│   └── PeerManager (connects to master)
│       ├── RTCPeerConnection
│       ├── Receives: master audio → speaker
│       └── Sends: slave mic → master
```
