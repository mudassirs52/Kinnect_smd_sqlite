package com.appVanta.i230017_i230507

import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView

class VideoCallActivity : BaseCallActivity() {

    override val videoCall = true

    private var muted = false
    private var cameraOn = true

    private lateinit var tvTimer: TextView
    private lateinit var local: FrameLayout
    private lateinit var remote: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_call)
        if (!initCall()) return

        tvTimer = findViewById(R.id.tvVideoTimer)
        local = findViewById(R.id.localContainer)
        remote = findViewById(R.id.remoteContainer)
        findViewById<TextView>(R.id.tvVideoName).text = peerName

        findViewById<TextView>(R.id.btnVideoEnd).setOnClickListener { hangUp("ended") }

        findViewById<TextView>(R.id.btnVideoMute).setOnClickListener { v ->
            muted = !muted
            agora?.setMuted(muted)
            v.alpha = if (muted) 0.5f else 1f
        }
        findViewById<TextView>(R.id.btnVideoCamera).setOnClickListener { v ->
            cameraOn = !cameraOn
            agora?.setCameraEnabled(cameraOn)
            local.visibility = if (cameraOn) android.view.View.VISIBLE else android.view.View.INVISIBLE
            v.alpha = if (cameraOn) 1f else 0.5f
        }
        findViewById<TextView>(R.id.btnVideoFlip).setOnClickListener { agora?.switchCamera() }

        beginCall()
    }

    override fun showStatus(text: String) {
        tvTimer.text = text
    }

    override fun localContainer(): ViewGroup? = local
    override fun remoteContainer(): ViewGroup? = remote
}