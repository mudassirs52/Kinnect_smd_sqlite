package com.appVanta.i230017_i230507

import android.content.Context
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

object FriendManager {

    enum class State { NONE, SENT, RECEIVED, FRIENDS }

    fun sendRequest(context: Context, other: String, onDone: (Boolean) -> Unit = {}) {
        val token = RecentLoginStore.getToken(context) ?: return onDone(false)
        val url = "${ApiClient.BASE_URL}friend_send.php"
        val request = object : StringRequest(Method.POST, url,
            Response.Listener { res ->
                try {
                    onDone(JSONObject(res).getString("status") == "success")
                } catch (e: Exception) { onDone(false) }
            },
            Response.ErrorListener { onDone(false) }
        ) {
            override fun getHeaders(): Map<String, String> = mapOf("Authorization" to "Bearer $token")
            override fun getParams(): Map<String, String> = mapOf("receiver_id" to other)
        }
        Volley.newRequestQueue(context).add(request)
    }

    fun cancelRequest(context: Context, other: String, onDone: (Boolean) -> Unit = {}) {
        val token = RecentLoginStore.getToken(context) ?: return onDone(false)
        val url = "${ApiClient.BASE_URL}friend_delete.php"
        val request = object : StringRequest(Method.POST, url,
            Response.Listener { res ->
                try {
                    onDone(JSONObject(res).getString("status") == "success")
                } catch (e: Exception) { onDone(false) }
            },
            Response.ErrorListener { onDone(false) }
        ) {
            override fun getHeaders(): Map<String, String> = mapOf("Authorization" to "Bearer $token")
            override fun getParams(): Map<String, String> = mapOf("other_id" to other)
        }
        Volley.newRequestQueue(context).add(request)
    }

    fun confirm(context: Context, other: String, onDone: (Boolean) -> Unit = {}) {
        val token = RecentLoginStore.getToken(context) ?: return onDone(false)
        val url = "${ApiClient.BASE_URL}friend_accept.php"
        val request = object : StringRequest(Method.POST, url,
            Response.Listener { res ->
                try {
                    onDone(JSONObject(res).getString("status") == "success")
                } catch (e: Exception) { onDone(false) }
            },
            Response.ErrorListener { onDone(false) }
        ) {
            override fun getHeaders(): Map<String, String> = mapOf("Authorization" to "Bearer $token")
            override fun getParams(): Map<String, String> = mapOf("sender_id" to other)
        }
        Volley.newRequestQueue(context).add(request)
    }

    fun unfriend(context: Context, other: String, onDone: (Boolean) -> Unit = {}) {
        val token = RecentLoginStore.getToken(context) ?: return onDone(false)
        val url = "${ApiClient.BASE_URL}friend_remove.php"
        val request = object : StringRequest(Method.POST, url,
            Response.Listener { res ->
                try {
                    onDone(JSONObject(res).getString("status") == "success")
                } catch (e: Exception) { onDone(false) }
            },
            Response.ErrorListener { onDone(false) }
        ) {
            override fun getHeaders(): Map<String, String> = mapOf("Authorization" to "Bearer $token")
            override fun getParams(): Map<String, String> = mapOf("other_id" to other)
        }
        Volley.newRequestQueue(context).add(request)
    }
}
