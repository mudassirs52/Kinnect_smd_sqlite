package com.kinnect.i230507_i230017

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.kinnect.i230507_i230017.FriendManager.State
import org.json.JSONObject
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_other_profile)

        val uid = intent.getStringExtra("uid")
        if (uid == null) {
            finish()
            return
        }
        meUid = RecentLoginStore.getUserId(this)
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

        loadProfile()
    }

    private fun loadProfile() {
        val otherUid=intent.getStringExtra("uid")?:return
        DataRepository.read("profile_get",JSONObject().put("user_id",otherUid)) {data ->
            val u=data.optJSONObject("user")?:return@read
            val name=u.optString("name")
            findViewById<TextView>(R.id.tvTopName).text=name
            findViewById<TextView>(R.id.tvOtherName).text=name
            findViewById<TextView>(R.id.tvOtherBio).text=u.optString("bio")
            Photos.bindAvatar(findViewById(R.id.tvOtherInitials),name,otherUid,u.optString("profile_photo"))
            Photos.bindCover(findViewById(R.id.ivOtherCoverPhoto),u.optString("cover_photo"),R.drawable.image6)
            state=State.valueOf(u.optString("friend_state","NONE"));stateLoaded=true;bindButton()
        }
    }

    private fun bindButton() {
        if (!stateLoaded) return
        btnAdd.isEnabled = true
        when (state) {
            State.NONE -> {
                btnAdd.text = "Add Friend"
                btnAdd.setBackgroundResource(R.drawable.kn_pill_teal)
            }
            State.SENT, State.RECEIVED -> {
                btnAdd.text = if(state==State.RECEIVED) "Confirm" else "Requested"
                btnAdd.setBackgroundResource(R.drawable.kn_pill_gray)
            }
            State.FRIENDS -> {
                btnAdd.text = "Friends"
                btnAdd.setBackgroundResource(R.drawable.kn_pill_gray)
            }
        }
    }

    private fun onActionClick() {
        when (state) {
            State.NONE -> {
                FriendManager.sendRequest(this, otherUid) { ok ->
                    if (ok) {
                        state = State.SENT
                        bindButton()
                        toast("Friend request sent")
                    } else toast("Could not send request")
                }
            }
            State.RECEIVED -> {FriendManager.confirm(this,otherUid){ok ->if(ok){state=State.FRIENDS;bindButton()}}}
            State.SENT -> {
                FriendManager.cancelRequest(this, otherUid) { ok ->
                    if (ok) {
                        state = State.NONE
                        bindButton()
                        toast("Request canceled")
                    } else toast("Could not cancel request")
                }
            }
            State.FRIENDS -> {
                AlertDialog.Builder(this)
                    .setTitle("Unfriend $otherName?")
                    .setPositiveButton("Unfriend") { _, _ ->
                        FriendManager.unfriend(this, otherUid) { ok ->
                            if (ok) {
                                state = State.NONE
                                bindButton()
                                toast("Unfriended")
                            } else toast("Could not unfriend")
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
