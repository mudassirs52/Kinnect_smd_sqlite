package com.appVanta.i230017_i230507

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley

object PresenceManager {
    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false
    private lateinit var appContext: Context

    private val heartbeatRunnable = object : Runnable {
        override fun run() {
            sendHeartbeat()
            if (isRunning) {
                handler.postDelayed(this, 30_000) // every 30 seconds
            }
        }
    }

    fun start(context: Context) {
        if (isRunning) return
        appContext = context.applicationContext
        isRunning = true
        handler.post(heartbeatRunnable)
    }

    fun stop() {
        isRunning = false
        handler.removeCallbacks(heartbeatRunnable)
    }

    private fun sendHeartbeat() {
        val token = RecentLoginStore.getToken(appContext) ?: return
        val url = "${ApiClient.BASE_URL}heartbeat.php"

        val request = object : StringRequest(Method.POST, url,
            Response.Listener { _ -> },
            Response.ErrorListener {}
        ) {
            override fun getHeaders(): Map<String, String> = mapOf("Authorization" to "Bearer $token")
        }
        Volley.newRequestQueue(appContext).add(request)
    }
}
