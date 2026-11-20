package com.kinnect.i230507_i230017

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
        applicationContext.getSharedPreferences("push",MODE_PRIVATE).edit().putString("token",token).apply()
        if(RecentLoginStore.getToken(applicationContext)!=null)DataRepository.mutate("fcm_register",org.json.JSONObject().put("fcm_token",token))
    }
}
