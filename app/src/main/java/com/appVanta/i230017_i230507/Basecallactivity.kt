package com.appVanta.i230017_i230507

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/** Voice and video call screens extend this and only provide their own UI. */
abstract class BaseCallActivity : AppCompatActivity(), AgoraCall.Listener {

    protected abstract val videoCall: Boolean
    protected abstract fun showStatus(text: String)   // "Calling…", then the running timer
    protected abstract fun localContainer(): ViewGroup?
    protected abstract fun remoteContainer(): ViewGroup?
    protected open fun onConnected() {}

    protected lateinit var call: CallInfo
    protected var peerName = ""
    protected var isCaller = false
    protected var agora: AgoraCall? = null

    private val ui = Handler(Looper.getMainLooper())
    private var removeObserver: (() -> Unit)? = null
    private var connectedAt = 0L
    private var remoteUid = 0
    private var finished = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.all { it }) {
            joinCall()
        } else {
            toast("Microphone and camera permissions are needed for calls")
            hangUp("ended")
        }
    }

    private val tick = object : Runnable {
        override fun run() {
            val s = (SystemClock.elapsedRealtime() - connectedAt) / 1000
            showStatus("%02d:%02d".format(s / 60, s % 60))
            ui.postDelayed(this, 1000)
        }
    }

    /** Call from onCreate after setContentView. Returns false if the screen cannot continue. */
    protected fun initCall(): Boolean {
        val c = intent.readCall()
        val me = "local_user_123"
        if (c == null) {
            Log.e("KinnectCall", "Call screen opened without call data (callInfo=$c)")
            toast("Call details are missing. Start the call from a chat.")
            finish()
            return false
        }
        if (BuildConfig.AGORA_APP_ID.isEmpty()) {
            toast("Agora App ID is missing. Add AGORA_APP_ID to local.properties.")
            finish()
            return false
        }
        call = c
        isCaller = c.callerUid == me
        peerName = if (isCaller) c.calleeName else c.callerName

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = hangUp("ended")
        })
        return true
    }

    /** Asks for permissions if needed, then joins the Agora channel. */
    protected fun beginCall() {
        val needed = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (videoCall) needed += Manifest.permission.CAMERA
        val missing = needed.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) joinCall() else permissionLauncher.launch(missing.toTypedArray())
    }

    private fun joinCall() {
        Log.d("KinnectCall", "joinCall id=${call.callId} channel=${call.channel} video=$videoCall caller=$isCaller")
        CallManager.activeCallId = call.callId
        val engine = AgoraCall(this, videoCall, this)
        agora = engine
        if (!engine.start(call.channel, localContainer())) return

        removeObserver = CallManager.observe(call.callId) { c -> if (c != null) onCallChanged(c) }
        showStatus(if (isCaller) "Calling…" else "Connecting…")

        // Nobody answered in time
        if (isCaller) {
            ui.postDelayed({ if (connectedAt == 0L) hangUp("missed") }, CallManager.RING_TIMEOUT_MS)
        }
    }

    private fun onCallChanged(c: CallInfo) {
        if (finished) return
        Log.d("KinnectCall", "call status is now ${c.status}")
        if (c.status == "accepted" && isCaller && connectedAt == 0L) showStatus("Connecting…")
        if (c.status in CallManager.FINAL) {
            // The other side ended it; just close this screen
            finished = true
            cleanup()
            finish()
        }
    }

    // ---------- AgoraCall.Listener ----------
    override fun onJoined() {}

    override fun onRemoteJoined(uid: Int) {
        if (connectedAt != 0L) return
        remoteUid = uid
        connectedAt = SystemClock.elapsedRealtime()
        if (videoCall) remoteContainer()?.let { agora?.setRemoteVideo(uid, it) }
        ui.post(tick)
        onConnected()
    }

    override fun onRemoteLeft(uid: Int) {
        if (uid == remoteUid) hangUp("ended")
    }

    override fun onCallError(message: String) {
        Log.e("KinnectCall", "call error: $message")
        toast(message)
        hangUp("ended")
    }

    // ---------- Ending ----------
    protected fun hangUp(status: String) {
        if (finished) return
        Log.d("KinnectCall", "hangUp status=$status")
        finished = true
        val duration = if (connectedAt > 0) (SystemClock.elapsedRealtime() - connectedAt) / 1000 else 0L
        CallManager.finish(call.callId, status, duration)
        cleanup()
        finish()
    }

    private fun cleanup() {
        ui.removeCallbacksAndMessages(null)
        removeObserver?.invoke()
        removeObserver = null
        agora?.leave()
        agora = null
        CallManager.activeCallId = null
    }

    override fun onDestroy() {
        if (::call.isInitialized && !finished) hangUp("ended")
        super.onDestroy()
    }

    protected fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}