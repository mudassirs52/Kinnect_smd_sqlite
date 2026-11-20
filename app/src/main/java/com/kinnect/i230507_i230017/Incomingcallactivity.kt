package com.kinnect.i230507_i230017

import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class IncomingCallActivity : AppCompatActivity() {

    private var ringtone: Ringtone? = null
    private var removeObserver: (() -> Unit)? = null
    private val ui = Handler(Looper.getMainLooper())
    private var handled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_incoming_call)

        val call = intent.readCall() ?: run {
            finish()
            return
        }

        val tvAvatar = findViewById<TextView>(R.id.tvIncomingAvatar)
        tvAvatar.text = AvatarUtils.initials(call.callerName)
        Photos.bindUserAvatar(tvAvatar, call.callerName, call.callerUid)

        findViewById<TextView>(R.id.tvIncomingName).text = call.callerName
        findViewById<TextView>(R.id.tvIncomingLabel).text =
            if (call.isVideo()) "Incoming video call" else "Incoming voice call"

        startRinging()

        removeObserver = CallManager.observe(call.callId) { c ->
            if (!handled && c != null && c.status in CallManager.FINAL) {
                handled = true
                finish()
            }
        }
        ui.postDelayed({
            if (!handled) {
                handled = true
                finish()
            }
        }, CallManager.RING_TIMEOUT_MS)

        findViewById<TextView>(R.id.btnAccept).setOnClickListener {
            if (handled) return@setOnClickListener
            handled = true
            stopRinging()
            CallManager.accept(
                call,
                onDone = {
                    val target = if (call.isVideo()) VideoCallActivity::class.java
                    else VoiceCallActivity::class.java
                    startActivity(Intent(this, target).putCall(call))
                    finish()
                },
                onError = { msg ->
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                    finish()
                }
            )
        }

        findViewById<TextView>(R.id.btnDecline).setOnClickListener {
            if (handled) return@setOnClickListener
            handled = true
            CallManager.finish(call.callId, "declined")
            finish()
        }
    }

    private fun startRinging() {
        try {
            ringtone = RingtoneManager.getRingtone(
                this, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) ringtone?.isLooping = true
            ringtone?.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopRinging() {
        try {
            ringtone?.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        CallManager.activeCallId=null
        stopRinging()
        ui.removeCallbacksAndMessages(null)
        removeObserver?.invoke()
        super.onDestroy()
    }
}
