package com.appVanta.i230017_i230507

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class StoryEditorActivity : AppCompatActivity() {

    private lateinit var uri: Uri
    private var type = "image"
    private var audience = "story"
    private var label = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_story_editor)

        val data = intent.data
        if (data == null) { finish(); return }
        uri = data
        type = intent.getStringExtra("type") ?: "image"

        val img = findViewById<ImageView>(R.id.imgPreview)
        val vid = findViewById<VideoView>(R.id.videoPreview)
        if (type == "video") {
            img.visibility = View.GONE
            vid.visibility = View.VISIBLE
            vid.setVideoURI(uri)
            vid.setOnPreparedListener { it.isLooping = true; vid.start() }
        } else {
            img.setImageBitmap(StoryUtils.decodeBitmap(this, uri, 1080))
        }

        findViewById<ImageButton>(R.id.story_editor_close)?.setOnClickListener { finish() }
        findViewById<View>(R.id.tool_text)?.setOnClickListener { askLabel() }
        findViewById<View>(R.id.pillStory)?.setOnClickListener { setAudience("story") }
        findViewById<View>(R.id.pillClose)?.setOnClickListener { pickCloseFriends() }
        findViewById<TextView>(R.id.story_editor_share)?.setOnClickListener { share(it) }

        setAudience("story")
    }

    private fun setAudience(a: String) {
        audience = a
        val on = ColorStateList.valueOf(ContextCompatTeal())
        val off = ColorStateList.valueOf(Color.parseColor("#2A2A2A"))
        findViewById<View>(R.id.pillStory)?.backgroundTintList = if (a == "story") on else off
        findViewById<View>(R.id.pillClose)?.backgroundTintList = if (a == "close") on else off
    }

    private fun ContextCompatTeal() = ContextCompat.getColor(this, R.color.kn_teal)

    private fun askLabel() {
        val et = EditText(this).apply { setText(label); hint = "Add text" }
        AlertDialog.Builder(this)
            .setTitle("Text label")
            .setView(et)
            .setPositiveButton("OK") { _, _ ->
                label = et.text.toString().trim()
                val tv = findViewById<TextView>(R.id.tvLabel)
                tv.text = label
                tv.visibility = if (label.isEmpty()) View.GONE else View.VISIBLE
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun pickCloseFriends() {
        Toast.makeText(this, "Close friends feature", Toast.LENGTH_SHORT).show()
    }

    private fun share(v: View) {
        Toast.makeText(this, "Shared story", Toast.LENGTH_SHORT).show()
        finish()
    }
}
