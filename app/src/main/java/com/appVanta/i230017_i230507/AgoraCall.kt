package com.appVanta.i230017_i230507

import android.content.Context
import android.view.SurfaceView
import android.view.ViewGroup
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import io.agora.rtc2.video.VideoCanvas

class AgoraCall(private val context: Context, private val video: Boolean, private val listener: Listener) {
    interface Listener {
        fun onJoined()
        fun onRemoteJoined(uid: Int)
        fun onRemoteLeft(uid: Int)
        fun onCallError(message: String)
    }

    private var rtcEngine: RtcEngine? = null

    private val mRtcEventHandler = object : IRtcEngineEventHandler() {
        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            listener.onJoined()
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            listener.onRemoteJoined(uid)
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            listener.onRemoteLeft(uid)
        }

        override fun onError(err: Int) {
            listener.onCallError("Agora Error code: $err")
        }
    }

    fun start(channel: String, container: ViewGroup?): Boolean {
        try {
            val config = RtcEngineConfig().apply {
                mContext = context.applicationContext
                mAppId = BuildConfig.AGORA_APP_ID
                mEventHandler = mRtcEventHandler
            }
            rtcEngine = RtcEngine.create(config)
            if (video) {
                rtcEngine?.enableVideo()
                rtcEngine?.startPreview()
                container?.let {
                    val surfaceView = SurfaceView(context)
                    it.addView(surfaceView)
                    rtcEngine?.setupLocalVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0))
                }
            } else {
                rtcEngine?.enableAudio()
            }
            rtcEngine?.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION)
            rtcEngine?.joinChannel("", channel, "", 0)
            return true
        } catch (e: Exception) {
            listener.onCallError(e.message ?: "Failed to start Agora engine")
            return false
        }
    }

    fun setRemoteVideo(uid: Int, container: ViewGroup) {
        if (!video) return
        val surfaceView = SurfaceView(context)
        container.removeAllViews()
        container.addView(surfaceView)
        rtcEngine?.setupRemoteVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, uid))
    }

    fun setMuted(muted: Boolean) {
        rtcEngine?.muteLocalAudioStream(muted)
    }

    fun setCameraEnabled(enabled: Boolean) {
        if (video) rtcEngine?.muteLocalVideoStream(!enabled)
    }

    fun switchCamera() {
        if (video) rtcEngine?.switchCamera()
    }

    fun setSpeaker(speaker: Boolean) {
        rtcEngine?.setEnableSpeakerphone(speaker)
    }

    fun leave() {
        try {
            rtcEngine?.leaveChannel()
            RtcEngine.destroy()
            rtcEngine = null
        } catch (_: Exception) {}
    }
}
