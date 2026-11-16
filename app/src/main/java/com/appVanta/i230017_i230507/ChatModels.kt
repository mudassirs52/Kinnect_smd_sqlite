package com.appVanta.i230017_i230507

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class Msg(
    val id: String,
    val clientMsgId: String,
    val from: String,
    val text: String,
    val image: String,
    val mediaUrl: String,
    val mediaType: String,
    val ts: Long,
    val edited: Boolean,
    val deleted: Boolean,
    val replyId: String,
    val replyText: String,
    val replyFrom: String,
    val hearts: Set<String>
)

object ChatUtils {
    const val EDIT_WINDOW_MS = 5 * 60 * 1000L

    fun chatId(a: String, b: String): String = if (a < b) "${a}_$b" else "${b}_$a"

    private fun sameDay(a: Calendar, b: Calendar) =
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

    fun timeLabel(ts: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = ts }
        val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(ts))
        return if (sameDay(cal, Calendar.getInstance())) {
            "TODAY $time"
        } else {
            SimpleDateFormat("EEE d MMM", Locale.getDefault()).format(Date(ts)).uppercase() + " " + time
        }
    }

    fun listTime(ts: Long): String {
        if (ts <= 0) return ""
        val diff = System.currentTimeMillis() - ts
        return when {
            diff < 60_000 -> "now"
            diff < 3_600_000 -> "${diff / 60_000}m"
            diff < 86_400_000 -> "${diff / 3_600_000}h"
            diff < 604_800_000 -> SimpleDateFormat("EEE", Locale.getDefault()).format(Date(ts))
            else -> SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(ts))
        }
    }
}
