package com.kinnect.i230507_i230017

import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import java.util.WeakHashMap
import java.util.concurrent.Executors

/**
 * One place that shows Base64 profile pictures and covers.
 *
 *   Photos.bindAvatar(textView, name, uid, user.profilePhoto)
 *   Photos.bindCover(imageView, user.coverPhoto, R.drawable.image6)
 *
 * Self-contained: it decodes the Base64 itself, so it needs nothing else from ImageUtils.
 */
object Photos {

    private val avatarBindings = WeakHashMap<TextView, Any>()

    /** Fetch the saved user photo; initials remain the fallback when no photo exists. */
    fun bindUserAvatar(tv: TextView, name: String, uid: String, tint: Boolean = true) {
        val binding = Any()
        avatarBindings[tv] = binding
        bindAvatar(tv, name, uid, null, tint)
        UserCache.get(uid) { user ->
            if (avatarBindings[tv] === binding) {
                bindAvatar(tv, user.name.ifEmpty { name }, uid, user.profilePhoto, tint)
            }
        }
    }

    private const val TAG = "Photos"
    private val exec = Executors.newFixedThreadPool(3)

    // The tint a view had before we put a photo in its background (so we can restore it)
    private val originalTint = WeakHashMap<View, ColorStateList?>()

    /**
     * Circular avatar. Shows initials on a coloured circle until the photo is decoded,
     * and permanently when there is no photo.
     * Pass tint = false to keep the layout's own colour (own profile, Edit profile).
     */
    fun bindAvatar(tv: TextView, name: String, uid: String, photoB64: String?, tint: Boolean = true) {
        if (photoB64.isNullOrEmpty()) {
            if (tv.tag != null) {                       // we had replaced the background with a photo
                tv.setBackgroundResource(R.drawable.kn_circle)
                tv.backgroundTintList = originalTint[tv]
                tv.tag = null
            }
            tv.text = UiUtils.initialsOf(name)
            if (tint) UiUtils.tintAvatar(tv, uid)
            return
        }

        val key = "$uid:${photoB64.length}:${photoB64.hashCode()}"
        if (tv.tag == key) return                        // this photo is already showing
        if (!originalTint.containsKey(tv)) originalTint[tv] = tv.backgroundTintList
        tv.tag = key

        // Clear the previous user's photo while this binding loads.
        tv.setBackgroundResource(R.drawable.kn_circle)
        tv.backgroundTintList = originalTint[tv]
        tv.text = UiUtils.initialsOf(name)
        if (tint) UiUtils.tintAvatar(tv, uid)

        exec.execute {
            val bmp = MediaFiles.bitmap(photoB64)
            val square = bmp?.let { cropSquare(it) }
            tv.post {
                if (tv.tag != key) return@post           // view was rebound to something else
                if (square == null) {
                    tv.tag = null // Allow a later refresh to retry a failed download.
                    Log.w(TAG, "Could not load the profile photo of $uid")
                    return@post
                }
                val drawable = RoundedBitmapDrawableFactory.create(tv.resources, square)
                drawable.isCircular = true
                tv.backgroundTintList = null
                tv.background = drawable
                tv.text = ""
            }
        }
    }

    /** Cover image, centre-cropped. Shows [fallbackRes] when there is no cover. */
    fun bindCover(iv: ImageView, photoB64: String?, fallbackRes: Int) {
        if (photoB64.isNullOrEmpty()) {
            iv.tag = null
            iv.scaleType = ImageView.ScaleType.CENTER_CROP
            iv.setImageResource(fallbackRes)
            return
        }
        val key = "${photoB64.length}:${photoB64.hashCode()}"
        if (iv.tag == key) return
        iv.tag = key

        exec.execute {
            val bmp = MediaFiles.bitmap(photoB64)
            iv.post {
                if (iv.tag != key) return@post
                if (bmp == null) {
                    Log.w(TAG, "Could not decode a cover photo (is it valid Base64 JPEG?)")
                    iv.setImageResource(fallbackRes)
                } else {
                    iv.scaleType = ImageView.ScaleType.CENTER_CROP
                    iv.setImageBitmap(bmp)
                }
            }
        }
    }

    private fun decode(b64: String): Bitmap? = try {
        val clean = b64.substringAfter("base64,").trim()
        val bytes = Base64.decode(clean, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (e: Exception) {
        Log.w(TAG, "decode failed: ${e.message}")
        null
    }

    private fun cropSquare(b: Bitmap): Bitmap {
        val side = minOf(b.width, b.height)
        if (side == b.width && side == b.height) return b
        return Bitmap.createBitmap(b, (b.width - side) / 2, (b.height - side) / 2, side, side)
    }
}