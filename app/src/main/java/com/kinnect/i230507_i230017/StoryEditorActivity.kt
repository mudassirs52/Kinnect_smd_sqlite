package com.kinnect.i230507_i230017

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
import com.android.volley.NetworkResponse
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.Volley
import org.json.JSONObject

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
        findViewById<View>(R.id.pillClose)?.setOnClickListener { setAudience("close") }
        findViewById<TextView>(R.id.story_editor_share)?.setOnClickListener { uploadStory() }

        setAudience("story")
    }

    private fun setAudience(a: String) {
        audience = a
        val on = ColorStateList.valueOf(ContextCompatTeal())
        val off = ColorStateList.valueOf(Color.parseColor("#2A2A2A"))
        findViewById<View>(R.id.pillStory)?.backgroundTintList = if (a == "story") on else off
        findViewById<View>(R.id.pillClose)?.backgroundTintList = if (a == "close") on else off
        if (a == "close") {
            Toast.makeText(this, "Audience: Close Friends", Toast.LENGTH_SHORT).show()
        }
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

    private fun uploadStory() {
        DataRepository.mutate("stories_upload",JSONObject().put("audience",audience).put("label",label).put("media_type",type),listOf(uri)) {ok ->
            if(ok){Toast.makeText(this,"Story saved for sharing",Toast.LENGTH_SHORT).show();finish()}
        }
    }
}
