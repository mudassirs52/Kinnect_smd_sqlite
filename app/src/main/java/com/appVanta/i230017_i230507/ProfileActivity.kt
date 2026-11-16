package com.appVanta.i230017_i230507

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ProfileActivity : AppCompatActivity() {

    private val removers = mutableListOf<() -> Unit>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        findViewById<TextView>(R.id.ivProfileBack)?.setOnClickListener { finish() }

        findViewById<Button>(R.id.btnAddStory)?.setOnClickListener {
            startActivity(Intent(this, YourStoryActivity::class.java))
        }
        findViewById<Button>(R.id.btnEditProfile)?.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        val name = RecentLoginStore.getName(this) ?: "Test User"
        val uid = "local_user_123"
        findViewById<TextView>(R.id.tvProfileTopName)?.text = name
        findViewById<TextView>(R.id.tvProfileName)?.text = name
        Photos.bindAvatar(findViewById(R.id.tvProfileInitials), name, uid, "", tint = false)
        Photos.bindCover(findViewById<ImageView>(R.id.ivCoverPhoto), "", R.drawable.image5)
        findViewById<TextView>(R.id.tvProfileFriends)?.text = "0 friends"
    }

    override fun onStart() {
        super.onStart()
        // Local fallback: no-op
    }

    override fun onStop() {
        super.onStop()
        removers.forEach { it() }
        removers.clear()
    }
}
