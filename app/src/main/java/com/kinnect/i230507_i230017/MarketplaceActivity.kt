package com.kinnect.i230507_i230017

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class MarketplaceActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_marketplace)

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
        findViewById<ImageView>(R.id.tabNotifications)?.setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }
        findViewById<ImageView>(R.id.tabMenu)?.setOnClickListener {
            startActivity(Intent(this, MenuActivity::class.java))
        }
    }

    private val removers = mutableListOf<() -> Unit>()

    override fun onStart() {
        super.onStart()
        UiUtils.bindBadges(this, removers)
    }

    override fun onStop() {
        super.onStop()
        removers.forEach { it() }
        removers.clear()
    }
}
