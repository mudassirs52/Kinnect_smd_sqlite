package com.appVanta.i230017_i230507

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ImageButton
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File

class CameraActivity : AppCompatActivity() {

    private var pendingUri: Uri? = null

    private val takePhoto = registerForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) pendingUri?.let { openEditor(it, "image") }
    }

    private val takeVideo = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == RESULT_OK) pendingUri?.let { openEditor(it, "video") }
    }

    private val pickMedia = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            val type = if (contentResolver.getType(it)?.startsWith("video") == true) "video" else "image"
            openEditor(it, type)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        findViewById<ImageButton>(R.id.camera_close)?.setOnClickListener { finish() }

        val shutter = findViewById<View>(R.id.btn_shutter)
        shutter.setOnClickListener {
            val uri = newCaptureUri("jpg")
            pendingUri = uri
            takePhoto.launch(uri)
        }
        shutter.setOnLongClickListener {
            val uri = newCaptureUri("mp4")
            pendingUri = uri
            val i = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
                .putExtra(MediaStore.EXTRA_OUTPUT, uri)
                .putExtra(MediaStore.EXTRA_DURATION_LIMIT, 10)
                .putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0)
                .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            takeVideo.launch(i)
            true
        }

        findViewById<View>(R.id.btn_gallery)?.setOnClickListener {
            pickMedia.launch(arrayOf("image/*", "video/*"))
        }
    }

    private fun newCaptureUri(ext: String): Uri {
        val f = File(cacheDir, "cap_${System.currentTimeMillis()}.$ext")
        return FileProvider.getUriForFile(this, "$packageName.fileprovider", f)
    }

    private fun openEditor(uri: Uri, type: String) {
        val i = Intent(this, StoryEditorActivity::class.java)
        i.data = uri
        i.putExtra("type", type)
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(i)
    }
}