package com.appVanta.i230017_i230507

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

data class PostModel(
    val id: Int,
    val userId: String,
    val authorName: String,
    val text: String,
    val createdAt: Long,
    val images: List<String>,
    val commentCount: Int
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
        private const val DATABASE_NAME = "kinnect_cache.db"
        private const val DATABASE_VERSION = 3

        const val TABLE_STORIES = "stories"
        const val TABLE_POSTS = "posts"
        const val TABLE_POST_IMAGES = "post_images"
        const val TABLE_MESSAGES = "messages"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE $TABLE_STORIES (id INTEGER PRIMARY KEY, user_id TEXT, media_url TEXT, media_type TEXT, label TEXT, audience TEXT, expires_at INTEGER, created_at INTEGER, author_name TEXT)")
        db.execSQL("CREATE TABLE $TABLE_POSTS (id INTEGER PRIMARY KEY, user_id TEXT, author_name TEXT, text TEXT, created_at INTEGER, comment_count INTEGER)")
        db.execSQL("CREATE TABLE $TABLE_POST_IMAGES (id INTEGER PRIMARY KEY AUTOINCREMENT, post_id INTEGER, image_url TEXT, image_index INTEGER)")
        db.execSQL("CREATE TABLE $TABLE_MESSAGES (client_msg_id TEXT PRIMARY KEY, sender_id TEXT, receiver_id TEXT, text TEXT, media_url TEXT, media_type TEXT, edited INTEGER, deleted INTEGER, created_at INTEGER)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_STORIES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_POSTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_POST_IMAGES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_MESSAGES")
        onCreate(db)
    }

    fun saveStories(stories: List<StoryModel>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(TABLE_STORIES, null, null)
            for (story in stories) {
                val values = ContentValues().apply {
                    put("id", story.id)
                    put("user_id", story.userId)
                    put("media_url", story.mediaUrl)
                    put("media_type", story.mediaType)
                    put("label", story.label)
                    put("audience", story.audience)
                    put("expires_at", story.expiresAt)
                    put("created_at", story.createdAt)
                    put("author_name", story.authorName)
                }
                db.insert(TABLE_STORIES, null, values)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
            db.close()
        }
    }

    fun getValidStories(): List<StoryModel> {
        val list = mutableListOf<StoryModel>()
        val db = readableDatabase
        val currentTime = System.currentTimeMillis()
        val cursor = db.rawQuery("SELECT * FROM $TABLE_STORIES WHERE expires_at > ? ORDER BY created_at DESC", arrayOf(currentTime.toString()))
        if (cursor.moveToFirst()) {
            do {
                list.add(
                    StoryModel(
                        id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                        userId = cursor.getString(cursor.getColumnIndexOrThrow("user_id")),
                        mediaUrl = cursor.getString(cursor.getColumnIndexOrThrow("media_url")),
                        mediaType = cursor.getString(cursor.getColumnIndexOrThrow("media_type")),
                        label = cursor.getString(cursor.getColumnIndexOrThrow("label")),
                        audience = cursor.getString(cursor.getColumnIndexOrThrow("audience")),
                        expiresAt = cursor.getLong(cursor.getColumnIndexOrThrow("expires_at")),
                        createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at")),
                        authorName = cursor.getString(cursor.getColumnIndexOrThrow("author_name")) ?: ""
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return list
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
}
