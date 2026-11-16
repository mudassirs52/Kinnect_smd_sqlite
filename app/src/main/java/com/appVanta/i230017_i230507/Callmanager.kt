package com.appVanta.i230017_i230507

import android.content.Intent

data class CallInfo(
    var callId: String = "",
    var callerUid: String = "",
    var callerName: String = "",
    var calleeUid: String = "",
    var calleeName: String = "",
    var type: String = "voice",      // "voice" or "video"
    var status: String = "ringing",  // ringing | accepted | declined | ended | missed
    var channel: String = "",
    var createdAt: Long = 0L,
    var acceptedAt: Long = 0L,
    var endedAt: Long = 0L
)

fun CallInfo.isVideo(): Boolean = type == "video"

fun Intent.putCall(call: CallInfo): Intent {
    putExtra("callId", call.callId)
    putExtra("callerUid", call.callerUid)
    putExtra("callerName", call.callerName)
    putExtra("calleeUid", call.calleeUid)
    putExtra("calleeName", call.calleeName)
    putExtra("type", call.type)
    putExtra("channel", call.channel)
    return this
}

fun Intent.readCall(): CallInfo? {
    val id = getStringExtra("callId") ?: return null
    return CallInfo(
        callId = id,
        callerUid = getStringExtra("callerUid") ?: "",
        callerName = getStringExtra("callerName") ?: "",
        calleeUid = getStringExtra("calleeUid") ?: "",
        calleeName = getStringExtra("calleeName") ?: "",
        type = getStringExtra("type") ?: "voice",
        channel = getStringExtra("channel") ?: ""
    )
}

object CallManager {

    const val RING_TIMEOUT_MS = 45_000L
    val FINAL = setOf("declined", "ended", "missed")

    /** Id of the call this device is currently in (null when idle). */
    @Volatile
    var activeCallId: String? = null

    fun start(
        peerUid: String,
        peerName: String,
        type: String,
        onReady: (CallInfo) -> Unit,
        onError: (String) -> Unit
    ) {
        val me = "local_user_123"
        val id = "call_${System.currentTimeMillis()}"
        val channel = BuildConfig.AGORA_CHANNEL.ifEmpty { id }
        onReady(
            CallInfo(
                callId = id,
                callerUid = me,
                callerName = "Test User",
                calleeUid = peerUid,
                calleeName = peerName,
                type = type,
                channel = channel,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    fun accept(call: CallInfo, onDone: () -> Unit, onError: (String) -> Unit) {
        onDone()
    }

    fun finish(callId: String, wantedStatus: String, durationSec: Long = 0) {
        // Local fallback: no-op
    }

    fun observe(callId: String, onChange: (CallInfo?) -> Unit): () -> Unit {
        return {}
    }

    fun observeIncoming(onCall: (CallInfo) -> Unit): () -> Unit {
        return {}
    }
}
