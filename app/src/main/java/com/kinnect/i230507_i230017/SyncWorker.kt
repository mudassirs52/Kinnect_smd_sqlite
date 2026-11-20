package com.kinnect.i230507_i230017

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.json.JSONObject
import org.json.JSONArray

class SyncWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        synchronized(DataRepository) {
            val db=DataRepository.db
            val owner=RecentLoginStore.getUserId(applicationContext)
            if(owner.isEmpty())return Result.success()
            for(a in db.actions()) {
                if(isStopped)return Result.retry()
                if(a.user!=owner || a.status=="failed")continue
                if(RecentLoginStore.getUserId(applicationContext)!=owner)return Result.success()
                try {
                    val latest=db.actions().firstOrNull{it.uuid==a.uuid}?:continue
                    val params=JSONObject(latest.payload.toString());params.remove("_local_time")
                    var files=JSONArray(latest.files.toString())
                    if(a.endpoint=="message_send" && files.length()>0) {
                        val uploaded=DataRepository.request("message_upload",JSONObject().put("action_uuid",a.uuid+"-upload"),files)
                        params.put("media_url",uploaded.getString("media_url"));files=JSONArray()
                    }
                    val response=DataRepository.request(a.endpoint,params,files)
                    val remote=response.optString("post_id",response.optString("comment_id",""))
                    if(remote.isNotEmpty())db.replaceReference("local_"+a.uuid,remote)
                    // Save the authoritative result before removing the optimistic action.
                    val refresh=when(a.endpoint) {
                        "message_send","message_edit","message_delete","screenshot_report" -> "messages_get"
                        "posts_create","reactions_toggle" -> "posts_get"
                        "comments_create","comment_like","comment_delete" -> "comments_get"
                        "stories_upload","story_delete" -> "stories_get"
                        "profile_update","profile_upload_image" -> "profile_get"
                        "friend_send","friend_accept","friend_delete","friend_remove" -> "friends_get"
                        else -> ""
                    }
                    if(refresh.isNotEmpty()) {
                        val query=JSONObject()
                        if(refresh=="messages_get")query.put("other_user_id",params.optString("receiver_id",params.optString("other_user_id",params.optString("partner_id"))))
                        if(refresh=="comments_get")query.put("post_id",params.optString("post_id"))
                        val fresh=DataRepository.request(refresh,query,get=true)
                        if(RecentLoginStore.getUserId(applicationContext)!=owner)return Result.success()
                        db.cache(owner+":"+refresh+":"+query.toString(),fresh)
                    }
                    db.acknowledge(a.uuid);DataRepository.changed()
                }catch(e:DataRepository.ApiException) {
                    val permanent=e.status in 400..499 && e.status !in listOf(401,408,429)
                    db.failure(a.uuid,e.message?:"Server rejected action",permanent);DataRepository.changed()
                    if(!permanent)return Result.retry()
                }catch(e:Exception) {db.failure(a.uuid,e.message?:"Connection failed",false);return Result.retry()}
            }
            return Result.success()
        }
    }
}
