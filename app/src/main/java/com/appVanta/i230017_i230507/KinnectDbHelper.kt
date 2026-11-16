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

class KinnectDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "kinnect_stories.db"
        private const val DATABASE_VERSION = 1

        const val TABLE_STORIES = "stories"
        const val COLUMN_ID = "id"
        const val COLUMN_USER_ID = "user_id"
        const val COLUMN_MEDIA_URL = "media_url"
        const val COLUMN_MEDIA_TYPE = "media_type"
        const val COLUMN_LABEL = "label"
        const val COLUMN_AUDIENCE = "audience"
        const val COLUMN_EXPIRES_AT = "expires_at"
        const val COLUMN_CREATED_AT = "created_at"
        const val COLUMN_AUTHOR_NAME = "author_name"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = ("CREATE TABLE $TABLE_STORIES ("
                + "$COLUMN_ID INTEGER PRIMARY KEY,"
                + "$COLUMN_USER_ID TEXT,"
                + "$COLUMN_MEDIA_URL TEXT,"
                + "$COLUMN_MEDIA_TYPE TEXT,"
                + "$COLUMN_LABEL TEXT,"
                + "$COLUMN_AUDIENCE TEXT,"
                + "$COLUMN_EXPIRES_AT INTEGER,"
                + "$COLUMN_CREATED_AT INTEGER,"
                + "$COLUMN_AUTHOR_NAME TEXT)")
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_STORIES")
        onCreate(db)
    }

    fun saveStories(stories: List<StoryModel>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(TABLE_STORIES, null, null)
            for (story in stories) {
                val values = ContentValues().apply {
                    put(COLUMN_ID, story.id)
                    put(COLUMN_USER_ID, story.userId)
                    put(COLUMN_MEDIA_URL, story.mediaUrl)
                    put(COLUMN_MEDIA_TYPE, story.mediaType)
                    put(COLUMN_LABEL, story.label)
                    put(COLUMN_AUDIENCE, story.audience)
                    put(COLUMN_EXPIRES_AT, story.expiresAt)
                    put(COLUMN_CREATED_AT, story.createdAt)
                    put(COLUMN_AUTHOR_NAME, story.authorName)
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
        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_STORIES WHERE $COLUMN_EXPIRES_AT > ? ORDER BY $COLUMN_CREATED_AT DESC",
            arrayOf(currentTime.toString())
        )
        if (cursor.moveToFirst()) {
            do {
                list.add(
                    StoryModel(
                        id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                        userId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER_ID)),
                        mediaUrl = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MEDIA_URL)),
                        mediaType = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MEDIA_TYPE)),
                        label = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LABEL)),
                        audience = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_AUDIENCE)),
                        expiresAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_EXPIRES_AT)),
                        createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT)),
                        authorName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_AUTHOR_NAME)) ?: ""
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return list
    }

    fun clearExpiredStories() {
        val db = writableDatabase
        val currentTime = System.currentTimeMillis()
        db.delete(TABLE_STORIES, "$COLUMN_EXPIRES_AT <= ?", arrayOf(currentTime.toString()))
        db.close()
    }
}
