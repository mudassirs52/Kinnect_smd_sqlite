package com.appVanta.i230017_i230507

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class FriendsActivity : AppCompatActivity() {

    private var me = "local_user_123"
    private var users = mapOf<String, UserLite>()
    private var allFriends = mapOf<String, Set<String>>()
    private var incoming = mapOf<String, Long>()
    private var sent = setOf<String>()
    private val removers = mutableListOf<() -> Unit>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friends)

        wireNavigation()

        findViewById<TextView>(R.id.chipSuggestions).setOnClickListener { showTab(false) }
        findViewById<TextView>(R.id.chipFriends).setOnClickListener { showTab(true) }
        showTab(false)
    }

    override fun onStart() {
        super.onStart()
        UiUtils.bindBadges(this, removers)
        // Local fallback: no-op for database observation
    }

    override fun onStop() {
        super.onStop()
        removers.forEach { it() }
        removers.clear()
    }

    private fun wireNavigation() {
        findViewById<ImageView>(R.id.btnSearch)?.setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }
        findViewById<ImageView>(R.id.btnMessenger)?.setOnClickListener {
            startActivity(Intent(this, ChatsActivity::class.java))
        }
        findViewById<ImageView>(R.id.tabHome)?.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
        }
        findViewById<ImageView>(R.id.tabMarket)?.setOnClickListener {
            startActivity(Intent(this, MarketplaceActivity::class.java))
        }
        findViewById<ImageView>(R.id.tabNotifications)?.setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }
        findViewById<ImageView>(R.id.tabMenu)?.setOnClickListener {
            startActivity(Intent(this, MenuActivity::class.java))
        }
    }

    private fun showTab(friends: Boolean) {
        findViewById<View>(R.id.sectionSuggestions).visibility = if (friends) View.GONE else View.VISIBLE
        findViewById<View>(R.id.sectionFriends).visibility = if (friends) View.VISIBLE else View.GONE
        val on = getColor(R.color.kn_teal)
        val off = getColor(R.color.kn_text)
        findViewById<TextView>(R.id.chipSuggestions).setTextColor(if (friends) off else on)
        findViewById<TextView>(R.id.chipFriends).setTextColor(if (friends) on else off)
    }

    private fun render() {
        val mine = allFriends[me] ?: emptySet()
        renderRequests(mine)
        renderSuggestions(mine)
        renderFriends(mine)
    }

    private fun renderRequests(mine: Set<String>) {
        // Local fallback: no-op
    }

    private fun renderSuggestions(mine: Set<String>) {
        // Local fallback: no-op
    }

    private fun renderFriends(mine: Set<String>) {
        // Local fallback: no-op
    }
}
