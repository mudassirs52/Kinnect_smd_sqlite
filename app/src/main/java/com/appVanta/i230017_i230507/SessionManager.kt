package com.appVanta.i230017_i230507

import android.content.Context
import android.content.Intent
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley

object SessionManager {

    fun goOnline() {
        // Local fallback: no-op
    }

    fun logout(context: Context) {
        val token = RecentLoginStore.getToken(context)
        if (!token.isNullOrEmpty()) {
            val url = "${ApiClient.BASE_URL}logout.php"
            val queue = Volley.newRequestQueue(context)
            val request = object : StringRequest(Method.POST, url,
                Response.Listener { _ ->
                    // Logout success on server
                },
                Response.ErrorListener { _ ->
                    // Network failure during logout, still proceed locally
                }
            ) {
                override fun getHeaders(): Map<String, String> {
                    return mapOf("Authorization" to "Bearer $token")
                }
                override fun getParams(): Map<String, String> {
                    return mapOf("token" to token)
                }
            }
            queue.add(request)
        }

        performLogout(context)
    }

    private fun performLogout(context: Context) {
        RecentLoginStore.clear(context)
        val intent = Intent(context, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        context.startActivity(intent)
    }
}
