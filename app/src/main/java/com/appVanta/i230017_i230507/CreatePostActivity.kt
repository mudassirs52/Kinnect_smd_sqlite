package com.appVanta.i230017_i230507

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File

class CreatePostActivity : AppCompatActivity() {

    private val images = ArrayList<Uri>()
    private var cameraUri: Uri? = null

    private val picker =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
            if (r.resultCode == RESULT_OK) {
                val list = r.data?.getStringArrayListExtra("uris") ?: return@registerForActivityResult
                images.clear()
                list.forEach { images.add(Uri.parse(it)) }
                renderThumbs()
            }
        }

    private val takePhoto = registerForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) cameraUri?.let { images.add(it); renderThumbs() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_post)

        val me = "local_user_123"

        UserCache.get(me) { u ->
            findViewById<TextView>(R.id.tvPostName).text = u.name
            findViewById<TextView>(R.id.tvPostAvatar)?.let { tv ->
                Photos.bindAvatar(tv, u.name, u.uid, u.profilePhoto)
            }
        }

        findViewById<TextView>(R.id.btnClosePost).setOnClickListener { finish() }

        findViewById<TextView>(R.id.tvOptionPhotoPicker).setOnClickListener {
            picker.launch(Intent(this, PhotoPickerActivity::class.java).putExtra("max", 10))
        }
        findViewById<TextView>(R.id.tvOptionCamera).setOnClickListener {
            val f = File(cacheDir, "post_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", f)
            cameraUri = uri
            takePhoto.launch(uri)
        }

        findViewById<Button>(R.id.btnSubmitPost).setOnClickListener { submit(me, it as Button) }
    }

    private fun renderThumbs() {
        val dp = resources.displayMetrics.density
        val box = findViewById<LinearLayout>(R.id.selectedImages)
        box.removeAllViews()
        findViewById<View>(R.id.selectedScroll).visibility =
            if (images.isEmpty()) View.GONE else View.VISIBLE

        for (uri in images.toList()) {
            val frame = FrameLayout(this)
            val iv = ImageView(this).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
            iv.setImageURI(uri)
            frame.addView(iv, FrameLayout.LayoutParams((88 * dp).toInt(), (88 * dp).toInt()))

            val x = TextView(this).apply {
                text = "✕"
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                setBackgroundResource(R.drawable.kn_circle)
                backgroundTintList = ColorStateList.valueOf(
                    Color.parseColor("#99000000")
                )
                textSize = 12f
                setOnClickListener { images.remove(uri); renderThumbs() }
            }
            frame.addView(
                x,
                FrameLayout.LayoutParams((24 * dp).toInt(), (24 * dp).toInt(), Gravity.TOP or Gravity.END)
                    .apply { setMargins(0, (4 * dp).toInt(), (4 * dp).toInt(), 0) }
            )
            box.addView(frame, LinearLayout.LayoutParams(-2, -2).apply { marginEnd = (8 * dp).toInt() })
        }
    }

    private fun submit(me: String, btn: Button) {
        val text = findViewById<EditText>(R.id.etPostText).text.toString().trim()
        if (text.isEmpty() && images.isEmpty()) {
            Toast.makeText(this, "Write something or add a photo", Toast.LENGTH_SHORT).show()
            return
        }
        btn.isEnabled = false
        Toast.makeText(this, "Posted", Toast.LENGTH_SHORT).show()
        finish()
    }
}
