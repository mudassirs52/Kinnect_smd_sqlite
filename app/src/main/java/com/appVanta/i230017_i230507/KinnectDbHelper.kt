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

class KinnectDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "kinnect_cache.db"
        private const val DATABASE_VERSION = 2

        const val TABLE_STORIES = "stories"
        const val TABLE_POSTS = "posts"
        const val TABLE_POST_IMAGES = "post_images"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE $TABLE_STORIES (id INTEGER PRIMARY KEY, user_id TEXT, media_url TEXT, media_type TEXT, label TEXT, audience TEXT, expires_at INTEGER, created_at INTEGER, author_name TEXT)")
        db.execSQL("CREATE TABLE $TABLE_POSTS (id INTEGER PRIMARY KEY, user_id TEXT, author_name TEXT, text TEXT, created_at INTEGER, comment_count INTEGER)")
        db.execSQL("CREATE TABLE $TABLE_POST_IMAGES (id INTEGER PRIMARY KEY AUTOINCREMENT, post_id INTEGER, image_url TEXT, image_index INTEGER)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_STORIES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_POSTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_POST_IMAGES")
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

    fun savePosts(posts: List<PostModel>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(TABLE_POSTS, null, null)
            db.delete(TABLE_POST_IMAGES, null, null)
            for (post in posts) {
                val pValues = ContentValues().apply {
                    put("id", post.id)
                    put("user_id", post.userId)
                    put("author_name", post.authorName)
                    put("text", post.text)
                    put("created_at", post.createdAt)
                    put("comment_count", post.commentCount)
                }
                db.insert(TABLE_POSTS, null, pValues)

                for ((index, imgUrl) in post.images.withIndex()) {
                    val imgValues = ContentValues().apply {
                        put("post_id", post.id)
                        put("image_url", imgUrl)
                        put("image_index", index)
                    }
                    db.insert(TABLE_POST_IMAGES, null, imgValues)
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
            db.close()
        }
    }

    fun getCachedPosts(): List<PostModel> {
        val posts = mutableListOf<PostModel>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_POSTS ORDER BY created_at DESC", null)
        if (cursor.moveToFirst()) {
            do {
                val postId = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
                val userId = cursor.getString(cursor.getColumnIndexOrThrow("user_id"))
                val authorName = cursor.getString(cursor.getColumnIndexOrThrow("author_name"))
                val text = cursor.getString(cursor.getColumnIndexOrThrow("text"))
                val createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"))
                val commentCount = cursor.getInt(cursor.getColumnIndexOrThrow("comment_count"))

                // Fetch images for this post
                val images = mutableListOf<String>()
                val imgCursor = db.rawQuery("SELECT image_url FROM $TABLE_POST_IMAGES WHERE post_id = ? ORDER BY image_index ASC", arrayOf(postId.toString()))
                if (imgCursor.moveToFirst()) {
                    do {
                        images.add(imgCursor.getString(imgCursor.getColumnIndexOrThrow("image_url")))
                    } while (imgCursor.moveToNext())
                }
                imgCursor.close()

                posts.add(PostModel(postId, userId, authorName, text, createdAt, images, commentCount))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return posts
    }
}
