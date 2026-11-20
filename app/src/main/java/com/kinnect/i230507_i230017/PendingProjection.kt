package com.kinnect.i230507_i230017

import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object PendingProjection {
    fun apply(endpoint: String,params: JSONObject,source: JSONObject): JSONObject {
        val json=JSONObject(source.toString());val me=RecentLoginStore.getUserId(DataRepository.context)
        val key=when(endpoint){"messages_get"->"messages";"posts_get"->"posts";"comments_get"->"comments";"stories_get"->"stories";else->""}
        val arr=json.optJSONArray(key)?:JSONArray();val list=(0 until arr.length()).map{arr.getJSONObject(it)}.toMutableList()
        for(a in DataRepository.db.actions().filter{it.user==me}) {
            val p=a.payload;val state=if(a.status=="failed")"Failed to sync" else "Pending"
            val base=JSONObject(p.toString()).put("id","local_"+a.uuid).put("user_id",me).put("created_at",p.optLong("_local_time")).put("sync_state",state)
            fun localFile(i: Int)=Uri.fromFile(File(a.files.getJSONObject(i).getString("path"))).toString()
            if(a.endpoint.startsWith("friend_")) {
                val other=p.optString("receiver_id",p.optString("sender_id",p.optString("other_id")))
                val after=when(a.endpoint){"friend_send"->"SENT";"friend_accept"->"FRIENDS";else->"NONE"}
                val users=json.optJSONArray("users")
                if(users!=null)for(i in 0 until users.length())users.getJSONObject(i).let{if(it.optString("id")==other)it.put("friend_state",after).put("sync_state",state)}
                json.optJSONObject("user")?.let{if(it.optString("id")==other)it.put("friend_state",after).put("sync_state",state)}
                if(endpoint=="friends_get") {
                    var person: JSONObject?=null
                    for(section in listOf("friends","requests","sent")) {
                        val people=json.optJSONArray(section)?:JSONArray();val retained=JSONArray()
                        for(i in 0 until people.length()){val u=people.getJSONObject(i);if(u.optString("id")==other)person=u else retained.put(u)}
                        json.put(section,retained)
                    }
                    if(after!="NONE") {
                        val u=person?:DataRepository.db.user(other)?:JSONObject().put("id",other).put("name","Kinnect user")
                        u.put("friend_state",after).put("sync_state",state)
                        val section=if(after=="FRIENDS")"friends" else "sent"
                        (json.optJSONArray(section)?:JSONArray().also{json.put(section,it)}).put(u)
                    }
                }
            }
            if (endpoint == "profile_get") {
                val uid = params.optString("user_id", params.optString("uid", me))
                if (uid == me && a.endpoint in listOf("profile_update", "profile_upload_image")) {
                    val user = json.optJSONObject("user") ?: JSONObject().put("id", me)
                        .put("name", RecentLoginStore.getName(DataRepository.context)).also { json.put("user", it) }
                    if (a.endpoint == "profile_upload_image" && a.files.length() > 0) {
                        user.put(if (p.optString("type") == "cover") "cover_photo" else "profile_photo", localFile(0))
                    } else if (a.endpoint == "profile_update") {
                        for (field in listOf("name", "username", "bio")) if (p.has(field)) user.put(field, p.get(field))
                    }
                }
            }
            when {
                endpoint=="messages_get" && a.endpoint=="message_send" && p.optString("receiver_id")==params.optString("other_user_id") -> {
                    if(list.none{it.optString("client_msg_id")==a.uuid})list.add(base.put("client_msg_id",a.uuid).put("sender_id",me).put("edited",false).put("deleted",false).put("media_url",if(a.files.length()>0)localFile(0) else p.optString("media_url")))
                }
                endpoint=="messages_get" && a.endpoint in listOf("message_edit","message_delete") -> list.find{it.optString("client_msg_id")==p.optString("client_msg_id")}?.apply {
                    put("sync_state",state);if(a.endpoint=="message_edit"){put("text",p.optString("new_text"));put("edited",true)}else{put("deleted",true);put("text","")}
                }
                endpoint=="posts_get" && a.endpoint=="posts_create" -> {
                    val images=JSONArray();for(i in 0 until a.files.length())images.put(localFile(i))
                    list.add(0,base.put("author_name",RecentLoginStore.getName(DataRepository.context)).put("images",images))
                }
                endpoint=="posts_get" && a.endpoint=="reactions_toggle" -> list.find{it.optString("id")==p.optString("post_id")}?.apply{put("my_reaction",p.optString("reaction_type"));put("sync_state",state)}
                endpoint=="comments_get" && a.endpoint=="comments_create" && p.optString("post_id")==params.optString("post_id") -> list.add(base.put("image_url",if(a.files.length()>0)localFile(0) else ""))
                endpoint=="comments_get" && a.endpoint=="comment_like" -> list.find{it.optString("id")==p.optString("comment_id")}?.put("liked",p.optString("liked")=="1")
                endpoint=="comments_get" && a.endpoint=="comment_delete" -> list.removeAll{it.optString("id")==p.optString("comment_id") || it.optString("parent_id")==p.optString("comment_id")}
                endpoint=="stories_get" && a.endpoint=="stories_upload" -> if(a.files.length()>0)list.add(base.put("media_url",localFile(0)).put("expires_at",p.optLong("_local_time")+StoryUtils.DAY_MS).put("author_name",RecentLoginStore.getName(DataRepository.context)))
                endpoint=="stories_get" && a.endpoint=="story_delete" -> list.removeAll{it.optString("id")==p.optString("story_id")}
            }
        }
        if(key.isNotEmpty()) {val out=JSONArray();for(v in list)if(key!="stories" || v.optLong("expires_at")>System.currentTimeMillis())out.put(v);json.put(key,out)}
        return json
    }
}
