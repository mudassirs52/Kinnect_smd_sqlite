package com.appVanta.i230017_i230507

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.appVanta.i230017_i230507.FriendManager.State
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OtherProfileActivity : AppCompatActivity() {

    private lateinit var otherUid: String
    private lateinit var meUid: String
    private lateinit var btnAdd: Button
    private var otherName = ""
    private var state = State.NONE
    private var stateLoaded = false
    private var otherFriends = setOf<String>()
    private var myFriends = setOf<String>()
    private val removers = mutableListOf<() -> Unit>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_other_profile)

        val me = "local_user_123"
        val uid = intent.getStringExtra("uid")
        if (uid == null) {
            finish()
            return
        }
        if (uid == me) {
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
            return
        }
        meUid = me
        otherUid = uid

        btnAdd = findViewById(R.id.btn_add_friend_other)
        btnAdd.isEnabled = false
        btnAdd.setOnClickListener { onActionClick() }

        findViewById<ImageButton>(R.id.other_profile_back)?.setOnClickListener { finish() }
        findViewById<Button>(R.id.btn_message_other)?.setOnClickListener {
            startActivity(Intent(this, ChatActivity::class.java).putExtra("uid", otherUid))
        }

        findViewById<View>(R.id.rowOtherWork).visibility = View.GONE
        findViewById<View>(R.id.rowOtherLives).visibility = View.GONE
    }

    override fun onStart() {
        super.onStart()
        if (!::otherUid.isInitialized) return
        // Local fallback: no-op for observation
    }

    override fun onStop() {
        super.onStop()
        removers.forEach { it() }
        removers.clear()
    }

    private fun bindUser(u: UserLite) {
        otherName = u.name
        findViewById<TextView>(R.id.tvTopName).text = u.name
        findViewById<TextView>(R.id.tvOtherName).text = u.name
        Photos.bindAvatar(findViewById(R.id.tvOtherInitials), u.name, u.uid, u.profilePhoto)
        Photos.bindCover(findViewById(R.id.ivOtherCoverPhoto), u.coverPhoto, R.drawable.image6)
        findViewById<TextView>(R.id.tvOtherBio).text = u.bio
        if (u.createdAt > 0) {
            val joined = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(u.createdAt))
            findViewById<TextView>(R.id.tvOtherJoined).text = "Joined $joined"
        }
    }

    private fun bindStats() {
        val mutual = otherFriends.filter { it in myFriends }
        findViewById<TextView>(R.id.tvOtherStats).text = "${otherFriends.size} friends · ${mutual.size} mutual"
    }

    private fun bindButton() {
        if (!stateLoaded) return
        btnAdd.isEnabled = true
        when (state) {
            State.NONE -> {
                btnAdd.text = "Add friend"
                btnAdd.setBackgroundResource(R.drawable.kn_pill_teal)
            }
            State.SENT -> {
                btnAdd.text = "Cancel request"
                btnAdd.setBackgroundResource(R.drawable.kn_pill_gray)
            }
            State.RECEIVED -> {
                btnAdd.text = "Respond to request"
                btnAdd.setBackgroundResource(R.drawable.kn_pill_teal)
            }
            State.FRIENDS -> {
                btnAdd.text = "Friends"
                btnAdd.setBackgroundResource(R.drawable.kn_pill_gray)
            }
        }
    }

    private fun onActionClick() {
        when (state) {
            State.NONE -> FriendManager.sendRequest(otherUid) { ok -> if (!ok) toast("Could not send request") }
            State.SENT -> FriendManager.cancelRequest(otherUid) { ok -> if (!ok) toast("Could not cancel request") }
            State.RECEIVED -> {
                AlertDialog.Builder(this)
                    .setTitle("Respond to friend request")
                    .setPositiveButton("Confirm") { _, _ -> FriendManager.confirm(otherUid) {} }
                    .setNegativeButton("Delete") { _, _ -> FriendManager.deleteRequest(otherUid) {} }
                    .show()
            }
            State.FRIENDS -> {
                AlertDialog.Builder(this)
                    .setTitle("Unfriend $otherName?")
                    .setPositiveButton("Unfriend") { _, _ -> FriendManager.unfriend(otherUid) {} }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
