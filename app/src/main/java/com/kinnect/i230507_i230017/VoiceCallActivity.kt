package com.kinnect.i230507_i230017

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

class VoiceCallActivity : BaseCallActivity() {

    override val videoCall = false

    private var muted = false
    private var speakerOn = false
    private var tvTimer: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_voice_call)
        if (!initCall()) return

        (findViewById<View>(R.id.tvCallName) as? TextView)?.text = peerName
        val tvAvatar = findViewById<View>(R.id.tvCallAvatar) as? TextView
        if (tvAvatar != null) {
            tvAvatar.text = AvatarUtils.initials(peerName)
            val peerUid = if (isCaller) call.calleeUid else call.callerUid
            UserCache.get(peerUid) { u ->
                Photos.bindAvatar(tvAvatar, peerName, peerUid, u.profilePhoto)
            }
        }
        tvTimer = findViewById<View>(R.id.tvCallTimer) as? TextView

        findViewById<View>(R.id.btn_end_call)?.setOnClickListener { hangUp("ended") }

        findViewById<View>(R.id.btnMute)?.setOnClickListener { v ->
            muted = !muted
            agora?.setMuted(muted)
            v.alpha = if (muted) 0.5f else 1f
        }
        findViewById<View>(R.id.btnSpeaker)?.setOnClickListener { v ->
            speakerOn = !speakerOn
            agora?.setSpeaker(speakerOn)
            v.alpha = if (speakerOn) 1f else 0.6f
        }
        findViewById<View>(R.id.btnSpeaker)?.alpha = 0.6f

        beginCall()
    }

    override fun showStatus(text: String) {
        tvTimer?.text = text
    }

    override fun localContainer(): ViewGroup? = null
    override fun remoteContainer(): ViewGroup? = null
}