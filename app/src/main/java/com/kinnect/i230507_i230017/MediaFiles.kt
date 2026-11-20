package com.kinnect.i230507_i230017

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.net.URL
import java.security.MessageDigest

object MediaFiles {
    private val executor=java.util.concurrent.Executors.newFixedThreadPool(2)
    private val locks=java.util.concurrent.ConcurrentHashMap<String,Any>()
    fun url(path: String): String = if(path.startsWith("uploads/")) ApiClient.BASE_URL+path else path
    fun diskFile(path: String): File {
        if(path.startsWith("file:"))return File(requireNotNull(Uri.parse(path).path))
        val key=MessageDigest.getInstance("SHA-256").digest(url(path).toByteArray()).joinToString(""){"%02x".format(it)}
        val extension=path.substringBefore('?').substringAfterLast('.',"bin").takeIf{it.matches(Regex("[A-Za-z0-9]{1,8}"))}?:"bin"
        val dir=File(DataRepository.context.filesDir,"media").apply{mkdirs()};val dest=File(dir,"$key.$extension")
        synchronized(locks.getOrPut(key){Any()}) {
            if(!dest.exists()) {
                val connection=URL(url(path)).openConnection();connection.connectTimeout=8000;connection.readTimeout=15000
                val tmp=File(dir,"$key.part")
                try {connection.getInputStream().use{input->tmp.outputStream().use{input.copyTo(it)}};check(tmp.renameTo(dest))}
                finally{tmp.delete()}
            }
        }
        return dest
    }
    fun loadFile(path: String,done: (File?)->Unit={}) {
        executor.execute {val result=try{diskFile(path)}catch(e:Exception){null};android.os.Handler(android.os.Looper.getMainLooper()).post{done(result)}}
    }
    private fun decode(file: File): Bitmap? {
        val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true};BitmapFactory.decodeFile(file.absolutePath,bounds)
        var sample=1;while(maxOf(bounds.outWidth,bounds.outHeight)/sample>1600)sample*=2
        return BitmapFactory.decodeFile(file.absolutePath,BitmapFactory.Options().apply{inSampleSize=sample})
    }
    fun bitmap(path: String): Bitmap? = try {
        when {
            path.startsWith("file:") -> decode(diskFile(path))
            path.startsWith("content:") -> DataRepository.context.contentResolver.openInputStream(Uri.parse(path))?.use{BitmapFactory.decodeStream(it)}
            path.startsWith("uploads/") || path.startsWith("https://") || path.startsWith("http://") -> {
                decode(diskFile(path))
            }
            else -> StoryUtils.decodeBase64Bitmap(path)
        }
    }catch(e:Exception){null}
}
