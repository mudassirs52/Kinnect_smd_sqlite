package com.kinnect.i230507_i230017

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.MediaStore
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

data class UserLite(
    val uid: String,
    val name: String,
    val username: String,
    val bio: String,
    val createdAt: Long,
    val profilePhoto: String = "",
    val coverPhoto: String = ""
)

object UiUtils {
    private val avatarColors = intArrayOf(
        R.color.kn_indigo, R.color.kn_purple, R.color.kn_green, R.color.kn_brown,
        R.color.kn_maroon, R.color.kn_blue, R.color.kn_teal
    )

    fun initialsOf(name: String): String =
        name.trim().split(" ").filter { it.isNotEmpty() }
            .take(2).joinToString("") { it[0].uppercase() }

    fun tintAvatar(view: View, uid: String) {
        val res = avatarColors[(uid.hashCode() and 0x7fffffff) % avatarColors.size]
        view.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(view.context, res))
    }

    fun stylePill(view: TextView, primary: Boolean) {
        view.setBackgroundResource(if (primary) R.drawable.kn_pill_teal else R.drawable.kn_pill_gray)
        view.setTextColor(ContextCompat.getColor(view.context, if (primary) R.color.white else R.color.kn_text))
    }

    fun timeAgo(ts: Long): String {
        if (ts <= 0) return ""
        val min = (System.currentTimeMillis() - ts) / 60000
        return when {
            min < 1 -> "now"
            min < 60 -> "${min}m"
            min < 1440 -> "${min / 60}h"
            min < 10080 -> "${min / 1440}d"
            else -> "${min / 10080}w"
        }
    }

    fun bindBadges(activity: AppCompatActivity, removers: MutableList<() -> Unit>) {
        val badgeMsg = activity.findViewById<TextView>(R.id.tvUnreadMessageBadge)
        badgeMsg?.visibility = View.GONE
        val badgeNotif = activity.findViewById<TextView>(R.id.tvUnreadNotifBadge)
        badgeNotif?.visibility = View.GONE
    }

    fun loadPhoto(context: Context, path: String, imageView: ImageView, circleCrop: Boolean = false) {
        if (path.isEmpty()) return
        val bmp = if (path.startsWith("http") || path.startsWith("content") || path.startsWith("file")) {
            try {
                MediaStore.Images.Media.getBitmap(context.contentResolver, Uri.parse(path))
            } catch (e: Exception) {
                null
            }
        } else {
            ImageUtils.base64ToBytes(path)?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
        }
        if (bmp != null) imageView.setImageBitmap(bmp)
    }

    fun loadPhotoInto(context: Context, path: String, targetView: View, circleCrop: Boolean = false) {
        if (path.isEmpty()) return
        val bmp = if (path.startsWith("http") || path.startsWith("content") || path.startsWith("file")) {
            try {
                MediaStore.Images.Media.getBitmap(context.contentResolver, Uri.parse(path))
            } catch (e: Exception) {
                null
            }
        } else {
            ImageUtils.base64ToBytes(path)?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
        }
        if (bmp != null) targetView.background = BitmapDrawable(targetView.resources, bmp)
    }
}

object AvatarUtils {
    fun initials(name: String): String = UiUtils.initialsOf(name)
}
