package com.kinnect.i230507_i230017

import android.graphics.Bitmap
import android.graphics.Color
import android.util.LruCache
import android.widget.ImageView

object Reactions {
    val keys = listOf("like", "love", "haha", "wow", "sad", "angry")
    private val emojis = mapOf(
        "like" to "👍", "love" to "❤️", "haha" to "😆",
        "wow" to "😮", "sad" to "😢", "angry" to "😡"
    )
    private val labels = mapOf(
        "like" to "Like", "love" to "Love", "haha" to "Haha",
        "wow" to "Wow", "sad" to "Sad", "angry" to "Angry"
    )
    private val colors = mapOf(
        "like" to "#0B5F63", "love" to "#D6334A", "haha" to "#E8A317",
        "wow" to "#E8A317", "sad" to "#E8A317", "angry" to "#C2432A"
    )

    fun emoji(k: String) = emojis[k] ?: "👍"
    fun label(k: String) = labels[k] ?: "Like"
    fun color(k: String): Int = Color.parseColor(colors[k] ?: "#0B5F63")

    fun toggle(postId: String,kind: String) {
        DataRepository.mutate("reactions_toggle",org.json.JSONObject().put("post_id",postId).put("reaction_type",kind))
    }
}
object UserCache {
    fun get(uid: String,cb:(UserLite)->Unit) {
        val params = org.json.JSONObject()
        if (uid != RecentLoginStore.getUserId(DataRepository.context)) params.put("user_id", uid)
        DataRepository.read("profile_get",params) {data ->
            val u=data.optJSONObject("user")?:return@read
            cb(UserLite(uid,u.optString("name"),u.optString("username"),u.optString("bio"),u.optLong("created_at"),u.optString("profile_photo"),u.optString("cover_photo")))
        }
    }
}

object ImageCache {
    private val executor = java.util.concurrent.Executors.newFixedThreadPool(3)
    private val cache = object : LruCache<String, Bitmap>(24 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount / 1024
    }

    fun loadVideoThumbnail(key: String, path: String, iv: ImageView) {
        iv.tag = key
        cache.get(key)?.let { iv.setImageBitmap(it); return }
        iv.setImageDrawable(null)
        executor.execute {
            val retriever = android.media.MediaMetadataRetriever()
            val bitmap = try {
                retriever.setDataSource(MediaFiles.diskFile(path).absolutePath)
                if (android.os.Build.VERSION.SDK_INT >= 27) {
                    retriever.getScaledFrameAtTime(0, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 240, 480)
                } else {
                    retriever.getFrameAtTime(0)
                }
            } catch (_: Exception) { null }
            finally { retriever.release() }
            if (bitmap != null) {
                cache.put(key, bitmap)
                iv.post { if (iv.tag == key) iv.setImageBitmap(bitmap) }
            }
        }
    }

    fun load(key: String, path: String, iv: ImageView) {
        iv.tag = key
        val hit = cache.get(key)
        if (hit != null) {
            iv.setImageBitmap(hit)
            return
        }
        iv.setImageDrawable(null)
        executor.execute {
            val bitmap=MediaFiles.bitmap(path)
            if(bitmap!=null){cache.put(key,bitmap);iv.post{if(iv.tag==key)iv.setImageBitmap(bitmap)}}
        }
    }
}
