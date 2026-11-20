package com.kinnect.i230507_i230017

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

        val name = RecentLoginStore.getName(this) ?: "Kinnect user"
        val uid = RecentLoginStore.getUserId(this)
        findViewById<TextView>(R.id.tvProfileTopName)?.text = name
        findViewById<TextView>(R.id.tvProfileName)?.text = name
        Photos.bindAvatar(findViewById(R.id.tvProfileInitials), name, uid, "", tint = false)
        Photos.bindCover(findViewById<ImageView>(R.id.ivCoverPhoto), "", R.drawable.image5)
        findViewById<TextView>(R.id.tvProfileFriends)?.text = "0 friends"
    }

    override fun onStart() {
        super.onStart()
        DataRepository.read("profile_get") {data ->
            val u=data.optJSONObject("user")?:return@read
            findViewById<TextView>(R.id.tvProfileTopName)?.text=u.optString("name")
            findViewById<TextView>(R.id.tvProfileName)?.text=u.optString("name")
            findViewById<TextView>(R.id.tvProfileFriends)?.text="${u.optInt("friend_count")} friends"
            Photos.bindAvatar(findViewById(R.id.tvProfileInitials),u.optString("name"),u.optString("id"),u.optString("profile_photo"),false)
            Photos.bindCover(findViewById(R.id.ivCoverPhoto),u.optString("cover_photo"),R.drawable.image5)
        }
    }

    override fun onStop() {
        super.onStop()
        removers.forEach { it() }
        removers.clear()
    }
}
