package com.kinnect.i230507_i230017

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONObject

/** SQLiteOpenHelper + ContentValues + bound queries + Cursor, as in the lecture. */
class OfflineStore(context: Context) : SQLiteOpenHelper(context, "kinnect_v1.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE snapshots (cache_key TEXT PRIMARY KEY, payload TEXT NOT NULL)")
        for (table in listOf("messages", "posts", "stories", "users", "friends", "comments"))
            db.execSQL("CREATE TABLE $table (id TEXT PRIMARY KEY, payload TEXT NOT NULL)")
        db.execSQL("CREATE TABLE pending_actions (id INTEGER PRIMARY KEY AUTOINCREMENT, action_uuid TEXT UNIQUE NOT NULL, user_id TEXT NOT NULL, endpoint TEXT NOT NULL, payload TEXT NOT NULL, files TEXT NOT NULL, status TEXT NOT NULL DEFAULT 'pending', attempts INTEGER NOT NULL DEFAULT 0, last_error TEXT NOT NULL DEFAULT '')")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) { /* Future migrations must preserve the outbox. */ }
    fun snapshot(key: String): JSONObject? = readableDatabase.rawQuery("SELECT payload FROM snapshots WHERE cache_key=?", arrayOf(key)).use {
        if (it.moveToFirst()) JSONObject(it.getString(0)) else null
    }
    fun cache(key: String, data: JSONObject) {
        val db = writableDatabase; db.beginTransaction()
        try {
            db.insertWithOnConflict("snapshots", null, ContentValues().apply { put("cache_key", key); put("payload", data.toString()) }, SQLiteDatabase.CONFLICT_REPLACE)
            for (table in listOf("messages", "posts", "stories", "users", "friends", "comments")) {
                val arr = data.optJSONArray(table) ?: continue
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val id = if (table == "messages") obj.getString("client_msg_id") else obj.getString("id")
                    db.insertWithOnConflict(table, null, ContentValues().apply { put("id", id); put("payload", obj.toString()) }, SQLiteDatabase.CONFLICT_REPLACE)
                }
            }
            data.optJSONObject("user")?.let { u -> db.insertWithOnConflict("users", null, ContentValues().apply { put("id", u.getString("id")); put("payload", u.toString()) }, SQLiteDatabase.CONFLICT_REPLACE) }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }
    fun user(id: String): JSONObject? = readableDatabase.rawQuery("SELECT payload FROM users WHERE id=?", arrayOf(id)).use { if(it.moveToFirst()) JSONObject(it.getString(0)) else null }
    fun enqueue(uuid: String, user: String, endpoint: String, payload: JSONObject, files: JSONArray) {
        writableDatabase.insertOrThrow("pending_actions", null, ContentValues().apply {
            put("action_uuid", uuid); put("user_id", user); put("endpoint", endpoint)
            put("payload", payload.toString()); put("files", files.toString())
        })
    }
    data class Action(val uuid: String, val user: String, val endpoint: String, val payload: JSONObject, val files: JSONArray, val status: String)
    fun actions(): List<Action> = readableDatabase.rawQuery("SELECT action_uuid,user_id,endpoint,payload,files,status FROM pending_actions ORDER BY id", null).use { c ->
        buildList { while (c.moveToNext()) add(Action(c.getString(0), c.getString(1), c.getString(2), JSONObject(c.getString(3)), JSONArray(c.getString(4)), c.getString(5))) }
    }
    fun replaceReference(localId: String,remoteId: String) {
        for(a in actions()) {
            var changed=false
            for(field in listOf("post_id","comment_id","parent_id","reply_id"))if(a.payload.optString(field)==localId){a.payload.put(field,remoteId);changed=true}
            if(changed)writableDatabase.update("pending_actions",ContentValues().apply{put("payload",a.payload.toString())},"action_uuid=?",arrayOf(a.uuid))
        }
    }
    fun acknowledge(uuid: String) { writableDatabase.delete("pending_actions", "action_uuid=?", arrayOf(uuid)) }
    fun failure(uuid: String, message: String, permanent: Boolean) {
        writableDatabase.execSQL("UPDATE pending_actions SET attempts=attempts+1,last_error=?,status=? WHERE action_uuid=?", arrayOf(message, if(permanent) "failed" else "pending", uuid))
    }
    fun clear() {
        val db=writableDatabase; db.beginTransaction()
        try { for(t in listOf("snapshots","messages","posts","stories","users","friends","comments","pending_actions")) db.delete(t,null,null); db.setTransactionSuccessful() }
        finally { db.endTransaction() }
    }
    fun expireStories() {
        readableDatabase.rawQuery("SELECT id,payload FROM stories",null).use { c -> while(c.moveToNext()) if(JSONObject(c.getString(1)).optLong("expires_at") <= System.currentTimeMillis()) writableDatabase.delete("stories","id=?",arrayOf(c.getString(0))) }
        readableDatabase.rawQuery("SELECT cache_key,payload FROM snapshots WHERE cache_key LIKE ?",arrayOf("%:stories_get:%")).use {c ->
            while(c.moveToNext()) {
                val data=JSONObject(c.getString(1));val stories=data.optJSONArray("stories")?:continue;val live=JSONArray()
                for(i in 0 until stories.length()){val story=stories.getJSONObject(i);if(story.optLong("expires_at")>System.currentTimeMillis())live.put(story)}
                if(live.length()!=stories.length())writableDatabase.update("snapshots",ContentValues().apply{put("payload",data.put("stories",live).toString())},"cache_key=?",arrayOf(c.getString(0)))
            }
        }
    }
}
