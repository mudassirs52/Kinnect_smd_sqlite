package com.appVanta.i230017_i230507

import android.content.Context
import android.content.Intent
import android.widget.Toast

fun Context.startCallWith(peerUid: String, peerName: String, video: Boolean) {
    val type = if (video) "video" else "voice"
    if (CallManager.activeCallId != null) {
        Toast.makeText(this, "You are already in a call", Toast.LENGTH_SHORT).show()
        return
    }
    CallManager.start(
        peerUid, peerName, type,
        onReady = { call ->
            val target = if (video) VideoCallActivity::class.java else VoiceCallActivity::class.java
            startActivity(Intent(this, target).putCall(call))
        },
        onError = { Toast.makeText(this, it, Toast.LENGTH_LONG).show() }
    )
}
