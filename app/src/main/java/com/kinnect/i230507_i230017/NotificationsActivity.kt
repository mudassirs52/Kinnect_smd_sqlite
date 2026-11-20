package com.kinnect.i230507_i230017

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class NotificationsActivity : AppCompatActivity() {

    private var me = ""
    private var users = mapOf<String, UserLite>()
    private var incoming = mapOf<String, Long>()
    private var items = listOf<Triple<String, String, Long>>()
    private val removers = mutableListOf<() -> Unit>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        me = RecentLoginStore.getUserId(this)
        setContentView(R.layout.activity_notifications)

        findViewById<ImageView>(R.id.btnSearch)?.setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }
        findViewById<ImageView>(R.id.btnMessenger)?.setOnClickListener {
            startActivity(Intent(this, ChatsActivity::class.java))
        }
        findViewById<ImageView>(R.id.tabHome)?.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
        }
        findViewById<ImageView>(R.id.tabFriends)?.setOnClickListener {
            startActivity(Intent(this, FriendsActivity::class.java))
        }
        findViewById<ImageView>(R.id.tabMarket)?.setOnClickListener {
            startActivity(Intent(this, MarketplaceActivity::class.java))
        }
        findViewById<ImageView>(R.id.tabMenu)?.setOnClickListener {
            startActivity(Intent(this, MenuActivity::class.java))
        }
    }

    override fun onStart() {
        super.onStart()
        UiUtils.bindBadges(this, removers)
        render()
    }

    override fun onStop() {
        super.onStop()
        removers.forEach { it() }
        removers.clear()
    }

    private fun render() {
        DataRepository.read("notifications_get") {data ->
            val box=findViewById<LinearLayout>(R.id.notifNewContainer);box.removeAllViews()
            val arr=data.optJSONArray("notifications")?:org.json.JSONArray()
            findViewById<View>(R.id.tvNotifEmpty).visibility=if(arr.length()==0)View.VISIBLE else View.GONE
            for(i in 0 until arr.length()){val n=arr.getJSONObject(i);box.addView(TextView(this).apply{text=n.optString("sender_name")+" · "+n.optString("text");setPadding(24,24,24,24);setOnClickListener{startActivity(Intent(this@NotificationsActivity,if(n.optString("type")=="friend_request")FriendsActivity::class.java else ChatActivity::class.java).putExtra("uid",n.optString("sender_id")))}})}
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
