package com.appVanta.i230017_i230507

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        loadUserInfo()

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
        findViewById<ImageView>(R.id.tabNotifications)?.setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }

        findViewById<View>(R.id.menu_profile_row)?.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
        findViewById<View>(R.id.shortcutMarketplace)?.setOnClickListener {
            startActivity(Intent(this, MarketplaceActivity::class.java))
        }
        findViewById<View>(R.id.shortcutFriends)?.setOnClickListener {
            startActivity(Intent(this, FriendsActivity::class.java))
        }

        findViewById<View>(R.id.menu_logout_row)?.setOnClickListener {
            SessionManager.logout(this)
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

    private fun loadUserInfo() {
        val name = RecentLoginStore.getName(this) ?: "Test User"
        val uid = "local_user_123"
        findViewById<TextView>(R.id.tvMenuName)?.text = name
        findViewById<TextView>(R.id.tvMenuInitials)?.let { tv ->
            Photos.bindAvatar(tv, name, uid, "")
        }
    }
}
