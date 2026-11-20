package com.kinnect.i230507_i230017

import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

class YourStoryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_your_story)

        findViewById<android.view.View>(R.id.btn_add_to_story).setOnClickListener{startActivity(android.content.Intent(this,CameraActivity::class.java))}
        findViewById<android.view.View>(R.id.btn_save_story).setOnClickListener{startActivity(android.content.Intent(this,StoryViewerActivity::class.java).putExtra("uid",RecentLoginStore.getUserId(this)))}
        findViewById<ImageButton>(R.id.your_story_close)?.setOnClickListener { finish() }
    }
}
