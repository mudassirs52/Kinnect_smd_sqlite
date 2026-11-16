package com.appVanta.i230017_i230507

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

    fun toggle(postId: String, kind: String) {
        // Local fallback: no-op
    }
}

object UserCache {
    private val cache = HashMap<String, UserLite>()

    fun get(uid: String, cb: (UserLite) -> Unit) {
        cache[uid]?.let { cb(it); return }
        val u = UserLite(uid, "Test User", "", "", 0L)
        cache[uid] = u
        cb(u)
    }
}

object ImageCache {
    private val cache = object : LruCache<String, Bitmap>(24 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount / 1024
    }

    fun load(key: String, path: String, iv: ImageView) {
        iv.tag = key
        val hit = cache.get(key)
        if (hit != null) {
            iv.setImageBitmap(hit)
            return
        }
        iv.setImageDrawable(null)
    }
}
