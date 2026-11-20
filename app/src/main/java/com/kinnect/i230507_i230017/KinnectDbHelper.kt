package com.kinnect.i230507_i230017

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class StoryModel(
    val id: Int,
    val userId: String,
    val mediaUrl: String,
    val mediaType: String,
    val label: String,
    val audience: String,
    val expiresAt: Long,
    val createdAt: Long,
    val authorName: String = ""
)

data class LocalMsg(
    val clientMsgId: String,
    val senderId: String,
    val receiverId: String,
    val text: String,
    val mediaUrl: String,
    val mediaType: String,
    val edited: Boolean,
    val deleted: Boolean,
    val createdAt: Long
)

class KinnectDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "kinnect_offline_sync.db"
        private const val DATABASE_VERSION = 3

        const val TABLE_USERS = "users"
        const val TABLE_POSTS = "posts"
        const val TABLE_POST_IMAGES = "post_images"
        const val TABLE_STORIES = "stories"
        const val TABLE_FRIENDS = "friends"
        const val TABLE_FRIEND_REQUESTS = "friend_requests"
        const val TABLE_MESSAGES = "messages"
        const val TABLE_REACTIONS = "reactions"
        const val TABLE_COMMENTS = "comments"
        const val TABLE_PENDING_ACTIONS = "pending_actions"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE $TABLE_USERS (id TEXT PRIMARY KEY, name TEXT, username TEXT, profile_photo TEXT, bio TEXT)")
        db.execSQL("CREATE TABLE $TABLE_POSTS (id INTEGER PRIMARY KEY, user_id TEXT, author_name TEXT, text TEXT, created_at INTEGER, comment_count INTEGER)")
        db.execSQL("CREATE TABLE $TABLE_POST_IMAGES (id INTEGER PRIMARY KEY AUTOINCREMENT, post_id INTEGER, image_url TEXT, image_index INTEGER)")
        db.execSQL("CREATE TABLE $TABLE_STORIES (id INTEGER PRIMARY KEY, user_id TEXT, media_url TEXT, media_type TEXT, label TEXT, audience TEXT, expires_at INTEGER, created_at INTEGER, author_name TEXT)")
        db.execSQL("CREATE TABLE $TABLE_FRIENDS (id TEXT PRIMARY KEY, name TEXT, username TEXT, profile_photo TEXT)")
        db.execSQL("CREATE TABLE $TABLE_FRIEND_REQUESTS (id TEXT PRIMARY KEY, sender_id TEXT, receiver_id TEXT, status TEXT)")
        db.execSQL("CREATE TABLE $TABLE_MESSAGES (client_msg_id TEXT PRIMARY KEY, sender_id TEXT, receiver_id TEXT, text TEXT, media_url TEXT, media_type TEXT, edited INTEGER, deleted INTEGER, created_at INTEGER)")
        db.execSQL("CREATE TABLE $TABLE_REACTIONS (id INTEGER PRIMARY KEY AUTOINCREMENT, post_id INTEGER, user_id TEXT, reaction_type TEXT)")
        db.execSQL("CREATE TABLE $TABLE_COMMENTS (id INTEGER PRIMARY KEY, post_id INTEGER, user_id TEXT, parent_id INTEGER, text TEXT, image_url TEXT, created_at INTEGER)")
        db.execSQL("CREATE TABLE $TABLE_PENDING_ACTIONS (id INTEGER PRIMARY KEY AUTOINCREMENT, action_uuid TEXT UNIQUE, action_type TEXT, payload TEXT, created_at INTEGER, status TEXT, retry_count INTEGER)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_POSTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_POST_IMAGES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_STORIES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_FRIENDS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_FRIEND_REQUESTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_MESSAGES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_REACTIONS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_COMMENTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PENDING_ACTIONS")
        onCreate(db)
    }

    fun saveMessages(messages: List<LocalMsg>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (m in messages) {
                val values = ContentValues().apply {
                    put("client_msg_id", m.clientMsgId)
                    put("sender_id", m.senderId)
                    put("receiver_id", m.receiverId)
                    put("text", m.text)
                    put("media_url", m.mediaUrl)
                    put("media_type", m.mediaType)
                    put("edited", if (m.edited) 1 else 0)
                    put("deleted", if (m.deleted) 1 else 0)
                    put("created_at", m.createdAt)
                }
                db.insertWithOnConflict(TABLE_MESSAGES, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
            db.close()
        }
    }

    fun getCachedMessages(me: String, partnerId: String): List<LocalMsg> {
        val list = mutableListOf<LocalMsg>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_MESSAGES WHERE (sender_id = ? AND receiver_id = ?) OR (sender_id = ? AND receiver_id = ?) ORDER BY created_at ASC",
            arrayOf(me, partnerId, partnerId, me)
        )
        if (cursor.moveToFirst()) {
            do {
                list.add(
                    LocalMsg(
                        clientMsgId = cursor.getString(cursor.getColumnIndexOrThrow("client_msg_id")),
                        senderId = cursor.getString(cursor.getColumnIndexOrThrow("sender_id")),
                        receiverId = cursor.getString(cursor.getColumnIndexOrThrow("receiver_id")),
                        text = cursor.getString(cursor.getColumnIndexOrThrow("text")) ?: "",
                        mediaUrl = cursor.getString(cursor.getColumnIndexOrThrow("media_url")) ?: "",
                        mediaType = cursor.getString(cursor.getColumnIndexOrThrow("media_type")) ?: "",
                        edited = cursor.getInt(cursor.getColumnIndexOrThrow("edited")) == 1,
                        deleted = cursor.getInt(cursor.getColumnIndexOrThrow("deleted")) == 1,
                        createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"))
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return list
    }

    fun queueAction(actionUuid: String, actionType: String, payload: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("action_uuid", actionUuid)
            put("action_type", actionType)
            put("payload", payload)
            put("created_at", System.currentTimeMillis())
            put("status", "pending")
            put("retry_count", 0)
        }
        db.insertWithOnConflict(TABLE_PENDING_ACTIONS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        db.close()
    }

    fun getPendingActions(): List<PendingAction> {
        val list = mutableListOf<PendingAction>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_PENDING_ACTIONS WHERE status = 'pending' ORDER BY created_at ASC", null)
        if (cursor.moveToFirst()) {
            do {
                list.add(
                    PendingAction(
                        id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                        actionUuid = cursor.getString(cursor.getColumnIndexOrThrow("action_uuid")),
                        actionType = cursor.getString(cursor.getColumnIndexOrThrow("action_type")),
                        payload = cursor.getString(cursor.getColumnIndexOrThrow("payload")),
                        createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at")),
                        status = cursor.getString(cursor.getColumnIndexOrThrow("status")),
                        retryCount = cursor.getInt(cursor.getColumnIndexOrThrow("retry_count"))
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return list
    }

    fun markActionSynced(actionUuid: String) {
        val db = writableDatabase
        db.delete(TABLE_PENDING_ACTIONS, "action_uuid = ?", arrayOf(actionUuid))
        db.close()
    }

    fun incrementRetry(actionUuid: String) {
        val db = writableDatabase
        db.execSQL("UPDATE $TABLE_PENDING_ACTIONS SET retry_count = retry_count + 1 WHERE action_uuid = ?", arrayOf(actionUuid))
        db.close()
    }
}

data class PendingAction(
    val id: Int,
    val actionUuid: String,
    val actionType: String,
    val payload: String,
    val createdAt: Long,
    val status: String,
    val retryCount: Int
)
