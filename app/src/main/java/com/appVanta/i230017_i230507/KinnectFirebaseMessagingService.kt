package com.appVanta.i230017_i230507

import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class KinnectFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        sendTokenToServer(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val data = remoteMessage.data
        val type = data["type"] ?: "message"
        val senderUid = data["sender_uid"] ?: ""
        val senderName = data["sender_name"] ?: "Kinnect User"
        val text = data["text"] ?: ""

        when (type) {
            "message" -> KinnectNotificationHelper.showMessageNotification(applicationContext, senderUid, senderName, text, false)
            "friend_request" -> KinnectNotificationHelper.showFriendRequestNotification(applicationContext, senderUid, senderName)
            "screenshot" -> KinnectNotificationHelper.showMessageNotification(applicationContext, senderUid, senderName, "Took a screenshot", false)
        }
    }

    private fun sendTokenToServer(token: String) {
        val authToken = RecentLoginStore.getToken(applicationContext) ?: return
        val url = "${ApiClient.BASE_URL}fcm_register.php"

        val request = object : StringRequest(Method.POST, url,
            Response.Listener { _ -> },
            Response.ErrorListener {}
        ) {
            override fun getHeaders(): Map<String, String> = mapOf("Authorization" to "Bearer $authToken")
            override fun getParams(): Map<String, String> = mapOf("fcm_token" to token)
        }
        Volley.newRequestQueue(applicationContext).add(request)
    }
}
