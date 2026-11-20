package com.kinnect.i230507_i230017
import android.content.Context
import org.json.JSONObject
object FriendManager {
    enum class State { NONE,SENT,RECEIVED,FRIENDS }
    fun sendRequest(context: Context,other: String,onDone:(Boolean)->Unit={})=DataRepository.mutate("friend_send",JSONObject().put("receiver_id",other),callback=onDone)
    fun cancelRequest(context: Context,other: String,onDone:(Boolean)->Unit={})=DataRepository.mutate("friend_delete",JSONObject().put("other_id",other),callback=onDone)
    fun confirm(context: Context,other: String,onDone:(Boolean)->Unit={})=DataRepository.mutate("friend_accept",JSONObject().put("sender_id",other),callback=onDone)
    fun unfriend(context: Context,other: String,onDone:(Boolean)->Unit={})=DataRepository.mutate("friend_remove",JSONObject().put("other_id",other),callback=onDone)
}
