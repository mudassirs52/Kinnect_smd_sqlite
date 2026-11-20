package com.kinnect.i230507_i230017
import android.content.Context
import org.json.JSONObject

object ChatManager {
    fun send(context: Context,me: String,other: String,text: String,mediaUrl: String,mediaType: String,replyId: Int?,onDone:(Boolean)->Unit) {
        val p=JSONObject().put("receiver_id",other).put("text",text).put("media_url",mediaUrl).put("media_type",mediaType)
        replyId?.let{p.put("reply_id",it)}
        DataRepository.mutate("message_send",p,callback=onDone)
    }
    fun edit(context: Context,clientMsgId: String,newText: String,onDone:(Boolean)->Unit) {
        DataRepository.mutate("message_edit",JSONObject().put("client_msg_id",clientMsgId).put("new_text",newText).put("partner_id",ChatActivity.activeChatUid?:""),callback=onDone)
    }
    fun delete(context: Context,clientMsgId: String,onDone:(Boolean)->Unit) {
        DataRepository.mutate("message_delete",JSONObject().put("client_msg_id",clientMsgId).put("partner_id",ChatActivity.activeChatUid?:""),callback=onDone)
    }
    fun fetchMessages(context: Context,other: String,onMessages:(List<Msg>)->Unit) {
        DataRepository.read("messages_get",JSONObject().put("other_user_id",other)) {json ->
            val arr=json.optJSONArray("messages")?:return@read
            onMessages((0 until arr.length()).map {i -> val o=arr.getJSONObject(i)
                o.optString("media_url").takeIf{it.isNotEmpty() && it!="null"}?.let{MediaFiles.loadFile(it)}
                Msg(o.optString("id"),o.getString("client_msg_id"),o.getString("sender_id"),o.optString("text"),"",o.optString("media_url"),o.optString("media_type"),o.optLong("created_at"),o.optBoolean("edited"),o.optBoolean("deleted"),o.optString("reply_id"),"","",emptySet(),o.optString("sync_state"))
            })
        }
    }
}
