package com.kinnect.i230507_i230017

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream

object StoryUtils {
    const val DAY_MS = 24L * 60 * 60 * 1000
    const val MAX_VIDEO_BYTES = 5 * 1024 * 1024

    fun decodeBitmap(ctx: Context, uri: Uri, maxSide: Int): Bitmap? {
        val cr = ctx.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0) return null
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
        val raw = cr.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return null
        val rotation = try {
            cr.openInputStream(uri)?.use { s ->
                when (ExifInterface(s).getAttributeInt(ExifInterface.TAG_ORIENTATION, 1)) {
                    6 -> 90; 3 -> 180; 8 -> 270; else -> 0
                }
            } ?: 0
        } catch (e: Exception) { 0 }
        val scale = minOf(1f, maxSide.toFloat() / maxOf(raw.width, raw.height))
        val m = Matrix().apply { postScale(scale, scale); postRotate(rotation.toFloat()) }
        return Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, m, true)
    }

    fun scaleTo(bmp: Bitmap, maxSide: Int): Bitmap {
        val s = minOf(1f, maxSide.toFloat() / maxOf(bmp.width, bmp.height))
        return Bitmap.createScaledBitmap(bmp, (bmp.width * s).toInt().coerceAtLeast(1), (bmp.height * s).toInt().coerceAtLeast(1), true)
    }

    fun toBase64(bmp: Bitmap, quality: Int): String {
        val out = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, quality, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    fun videoFrame(ctx: Context, uri: Uri): Bitmap? {
        val r = MediaMetadataRetriever()
        return try {
            r.setDataSource(ctx, uri)
            r.getFrameAtTime(0)
        } catch (e: Exception) { null } finally { r.release() }
    }

    /** Returns null if the video is larger than MAX_VIDEO_BYTES. */
    fun readVideoBase64(ctx: Context, uri: Uri): String? {
        val out = ByteArrayOutputStream()
        ctx.contentResolver.openInputStream(uri)?.use { s ->
            val buf = ByteArray(16 * 1024)
            var total = 0
            while (true) {
                val n = s.read(buf)
                if (n < 0) break
                total += n
                if (total > MAX_VIDEO_BYTES) return null
                out.write(buf, 0, n)
            }
        } ?: return null
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    fun decodeBase64Bitmap(b64: String): Bitmap? = try {
        val bytes = Base64.decode(b64, Base64.NO_WRAP)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (e: Exception) { null }

    fun deleteStory(owner: String, id: String) {
        DataRepository.mutate("story_delete",org.json.JSONObject().put("story_id",id))
    }
}
