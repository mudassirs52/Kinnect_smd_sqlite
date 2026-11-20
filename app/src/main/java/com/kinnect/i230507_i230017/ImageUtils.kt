package com.kinnect.i230507_i230017

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import android.util.LruCache
import java.io.ByteArrayOutputStream

object ImageUtils {

    // Sizes used across the app
    const val AVATAR = 150      // profile pictures
    const val POST = 800        // post / chat / comment images
    const val STORY = 900       // story images
    const val COVER = 800       // cover photos

    private val cache = object : LruCache<String, Bitmap>(
        (Runtime.getRuntime().maxMemory() / 1024 / 8).toInt()
    ) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount / 1024
    }

    /** Uri (gallery or camera) -> resized, rotated, compressed Base64 string. Null on failure. */
    fun uriToBase64(context: Context, uri: Uri, maxSize: Int = POST, quality: Int = 70): String? {
        return try {
            // 1. Read only the dimensions
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            // 2. Decode a down-sampled copy so big photos don't cause OutOfMemory
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= maxSize && bounds.outHeight / (sample * 2) >= maxSize) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            var bitmap = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            } ?: return null

            // 3. Fix rotation from EXIF
            val rotation = context.contentResolver.openInputStream(uri)?.use { readRotation(it) } ?: 0f
            if (rotation != 0f) {
                val m = Matrix().apply { postRotate(rotation) }
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, true)
            }

            // 4. Final scale to maxSize
            val scale = maxSize.toFloat() / maxOf(bitmap.width, bitmap.height)
            if (scale < 1f) {
                bitmap = Bitmap.createScaledBitmap(
                    bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true
                )
            }

            bitmapToBase64(bitmap, quality)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /** Decodes a down-sampled, correctly rotated Bitmap for on-screen previews. Null on failure. */
    fun decodeSampled(context: Context, uri: Uri, reqSize: Int): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= reqSize && bounds.outHeight / (sample * 2) >= reqSize) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            var bmp = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            } ?: return null
            val rotation = context.contentResolver.openInputStream(uri)?.use { readRotation(it) } ?: 0f
            if (rotation != 0f) {
                val m = Matrix().apply { postRotate(rotation) }
                bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
            }
            bmp
        } catch (e: Exception) {
            null
        }
    }

    fun bitmapToBase64(bitmap: Bitmap, quality: Int = 70): String {
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    // Names used by the chat code, so both features share this single ImageUtils
    fun encode(context: Context, uri: Uri, maxSide: Int = 800, quality: Int = 70): String? =
        uriToBase64(context, uri, maxSide, quality)

    fun toBitmap(b64: String): Bitmap? = base64ToBitmap(b64)

    /** Base64 string -> Bitmap (cached). Null if empty or invalid. */
    fun base64ToBitmap(base64: String?): Bitmap? {
        if (base64.isNullOrEmpty()) return null
        val key = "${base64.length}:${base64.hashCode()}"
        cache.get(key)?.let { return it }
        return try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.also { cache.put(key, it) }
        } catch (e: Exception) {
            null
        }
    }

    /** Raw file bytes (used for short videos) -> Base64. */
    fun bytesToBase64(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)

    fun base64ToBytes(base64: String): ByteArray = Base64.decode(base64, Base64.DEFAULT)

    private fun readRotation(input: java.io.InputStream): Float {
        return try {
            when (ExifInterface(input).getAttributeInt(
                ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
            )) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } catch (e: Exception) {
            0f
        }
    }
}