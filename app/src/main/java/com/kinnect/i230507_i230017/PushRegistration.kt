package com.kinnect.i230507_i230017
import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import org.json.JSONObject
object PushRegistration {
    fun register(context: Context) {
        if(FirebaseApp.getApps(context).isEmpty())return
        FirebaseMessaging.getInstance().token.addOnSuccessListener {token ->
            context.getSharedPreferences("push",Context.MODE_PRIVATE).edit().putString("token",token).apply()
            DataRepository.mutate("fcm_register",JSONObject().put("fcm_token",token))
        }
    }
}
