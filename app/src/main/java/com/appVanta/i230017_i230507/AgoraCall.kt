package com.appVanta.i230017_i230507

import android.content.Context
import android.view.ViewGroup

class AgoraCall(context: Context, video: Boolean, listener: Listener) {
    interface Listener {
        fun onJoined()
        fun onRemoteJoined(uid: Int)
        fun onRemoteLeft(uid: Int)
        fun onCallError(message: String)
    }

    fun start(channel: String, container: ViewGroup?): Boolean = true
    fun setRemoteVideo(uid: Int, container: ViewGroup) {}
    fun setMuted(muted: Boolean) {}
    fun setCameraEnabled(enabled: Boolean) {}
    fun switchCamera() {}
    fun setSpeaker(speaker: Boolean) {}
    fun leave() {}
}
