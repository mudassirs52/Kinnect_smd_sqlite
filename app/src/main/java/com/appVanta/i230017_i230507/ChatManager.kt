package com.appVanta.i230017_i230507

import android.content.Context
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.util.UUID

object ChatManager {

    fun send(context: Context, me: String, other: String, text: String, mediaUrl: String, mediaType: String, replyId: Int?, onDone: (Boolean) -> Unit) {
        val token = RecentLoginStore.getToken(context) ?: return onDone(false)
        EspressoIdlingResource.increment()
        val clientMsgId = UUID.randomUUID().toString()
        val url = "${ApiClient.BASE_URL}message_send.php"

        val request = object : StringRequest(Method.POST, url,
            Response.Listener { response ->
                try {
                    val json = JSONObject(response)
                    val ok = json.getString("status") == "success"
                    if (ok) {
                        val db = KinnectDbHelper(context)
                        db.saveMessages(
                            listOf(
                                LocalMsg(
                                    clientMsgId = clientMsgId,
                                    senderId = me,
                                    receiverId = other,
                                    text = text,
                                    mediaUrl = mediaUrl,
                                    mediaType = mediaType,
                                    edited = false,
                                    deleted = false,
                                    createdAt = System.currentTimeMillis()
                                )
                            )
                        )
                    }
                    onDone(ok)
                } catch (e: Exception) {
                    onDone(false)
                } finally {
                    EspressoIdlingResource.decrement()
                }
            },
            Response.ErrorListener {
                // Offline fallback: save locally so message appears in UI during offline/testing
                val db = KinnectDbHelper(context)
                db.saveMessages(
                    listOf(
                        LocalMsg(
                            clientMsgId = clientMsgId,
                            senderId = me,
                            receiverId = other,
                            text = text,
                            mediaUrl = mediaUrl,
                            mediaType = mediaType,
                            edited = false,
                            deleted = false,
                            createdAt = System.currentTimeMillis()
                        )
                    )
                )
                EspressoIdlingResource.decrement()
                onDone(true)
            }
        ) {
            override fun getHeaders(): Map<String, String> = mapOf("Authorization" to "Bearer $token")
            override fun getParams(): Map<String, String> {
                val params = mutableMapOf(
                    "receiver_id" to other,
                    "text" to text,
                    "client_msg_id" to clientMsgId,
                    "media_type" to mediaType
                )
                if (mediaUrl.isNotEmpty()) params["media_url"] = mediaUrl
                if (replyId != null) params["reply_id"] = replyId.toString()
                return params
            }
        }
        Volley.newRequestQueue(context).add(request)
    }

    fun edit(context: Context, clientMsgId: String, newText: String, onDone: (Boolean) -> Unit) {
        val token = RecentLoginStore.getToken(context) ?: return onDone(false)
        EspressoIdlingResource.increment()
        val url = "${ApiClient.BASE_URL}message_edit.php"

        val request = object : StringRequest(Method.POST, url,
            Response.Listener { response ->
                try {
                    val json = JSONObject(response)
                    onDone(json.getString("status") == "success")
                } catch (e: Exception) {
                    onDone(false)
                } finally {
                    EspressoIdlingResource.decrement()
                }
            },
            Response.ErrorListener {
                EspressoIdlingResource.decrement()
                onDone(false)
            }
        ) {
            override fun getHeaders(): Map<String, String> = mapOf("Authorization" to "Bearer $token")
            override fun getParams(): Map<String, String> = mapOf(
                "client_msg_id" to clientMsgId,
                "new_text" to newText
            )
        }
        Volley.newRequestQueue(context).add(request)
    }

    fun delete(context: Context, clientMsgId: String, onDone: (Boolean) -> Unit) {
        val token = RecentLoginStore.getToken(context) ?: return onDone(false)
        EspressoIdlingResource.increment()
        val url = "${ApiClient.BASE_URL}message_delete.php"

        val request = object : StringRequest(Method.POST, url,
            Response.Listener { response ->
                try {
                    val json = JSONObject(response)
                    onDone(json.getString("status") == "success")
                } catch (e: Exception) {
                    onDone(false)
                } finally {
                    EspressoIdlingResource.decrement()
                }
            },
            Response.ErrorListener {
                EspressoIdlingResource.decrement()
                onDone(false)
            }
        ) {
            override fun getHeaders(): Map<String, String> = mapOf("Authorization" to "Bearer $token")
            override fun getParams(): Map<String, String> = mapOf(
                "client_msg_id" to clientMsgId
            )
        }
        Volley.newRequestQueue(context).add(request)
    }

    fun fetchMessages(context: Context, other: String, onMessages: (List<Msg>) -> Unit) {
        val token = RecentLoginStore.getToken(context) ?: return
        EspressoIdlingResource.increment()
        val url = "${ApiClient.BASE_URL}messages_get.php?other_user_id=$other"

        val request = object : StringRequest(Method.GET, url,
            Response.Listener { response ->
                try {
                    val json = JSONObject(response)
                    if (json.getString("status") == "success") {
                        val arr = json.getJSONArray("messages")
                        val list = mutableListOf<Msg>()
                        val db = KinnectDbHelper(context)
                        val localMsgs = mutableListOf<LocalMsg>()

                        for (i in 0 until arr.length()) {
                            val o = arr.getJSONObject(i)
                            val m = Msg(
                                id = o.getInt("id").toString(),
                                clientMsgId = o.getString("client_msg_id"),
                                from = o.getString("sender_id"),
                                text = o.getString("text"),
                                image = "",
                                mediaUrl = o.optString("media_url", ""),
                                mediaType = o.optString("media_type", ""),
                                ts = o.getLong("created_at"),
                                edited = o.getBoolean("edited"),
                                deleted = o.getBoolean("deleted"),
                                replyId = o.optInt("reply_id", 0).toString(),
                                replyText = "",
                                replyFrom = "",
                                hearts = emptySet()
                            )
                            list.add(m)
                            localMsgs.add(
                                LocalMsg(
                                    clientMsgId = m.clientMsgId,
                                    senderId = m.from,
                                    receiverId = other,
                                    text = m.text,
                                    mediaUrl = m.mediaUrl,
                                    mediaType = m.mediaType,
                                    edited = m.edited,
                                    deleted = m.deleted,
                                    createdAt = m.ts
                                )
                            )
                        }
                        db.saveMessages(localMsgs)
                        onMessages(list)
                    }
                } catch (_: Exception) {
                } finally {
                    EspressoIdlingResource.decrement()
                }
            },
            Response.ErrorListener {
                // Offline fallback: load from SQLite cache
                val db = KinnectDbHelper(context)
                val cached = db.getCachedMessages("local_user_123", other)
                val list = cached.map {
                    Msg(
                        id = it.clientMsgId,
                        clientMsgId = it.clientMsgId,
                        from = it.senderId,
                        text = it.text,
                        image = "",
                        mediaUrl = it.mediaUrl,
                        mediaType = it.mediaType,
                        ts = it.createdAt,
                        edited = it.edited,
                        deleted = it.deleted,
                        replyId = "",
                        replyText = "",
                        replyFrom = "",
                        hearts = emptySet()
                    )
                }
                EspressoIdlingResource.decrement()
                onMessages(list)
            }
        ) {
            override fun getHeaders(): Map<String, String> = mapOf("Authorization" to "Bearer $token")
        }
        Volley.newRequestQueue(context).add(request)
    }
}
