package com.appVanta.i230017_i230507

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class HomeActivity : AppCompatActivity() {

    private var friendIds: Set<String> = emptySet()
    private lateinit var feed: PostAdapter
    private val nameCache = HashMap<String, String>()
    private val fetching = HashSet<String>()
    private val removers = mutableListOf<() -> Unit>()
    private val handler = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            renderStories()
            handler.postDelayed(this, 60_000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        loadUserInfo()

        // Feed
        feed = PostAdapter(this)
        findViewById<RecyclerView>(R.id.rvFeed).apply {
            layoutManager = LinearLayoutManager(this@HomeActivity)
            isNestedScrollingEnabled = false
            adapter = feed
        }

        // Top bar navigation
        findViewById<ImageView>(R.id.btnSearch)?.setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }
        findViewById<ImageView>(R.id.btnMessenger)?.setOnClickListener {
            startActivity(Intent(this, ChatsActivity::class.java))
        }

        // Tab bar navigation
        findViewById<ImageView>(R.id.tabFriends)?.setOnClickListener {
            startActivity(Intent(this, FriendsActivity::class.java))
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

        // Create post / story
        findViewById<TextView>(R.id.tvWhatsOnYourMind)?.setOnClickListener {
            startActivity(Intent(this, CreatePostActivity::class.java))
        }
        findViewById<View>(R.id.cardCreateStory)?.setOnClickListener {
            startActivity(Intent(this, CameraActivity::class.java))
        }
        findViewById<TextView>(R.id.ivMyProfileIcon)?.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }

    override fun onStart() {
        super.onStart()
        SessionManager.goOnline()
        val me = "local_user_123"
        UiUtils.bindBadges(this, removers)
        handler.postDelayed(tick, 60_000)
    }

    override fun onStop() {
        super.onStop()
        removers.forEach { it() }
        removers.clear()
        handler.removeCallbacks(tick)
        if (::feed.isInitialized) feed.release()
    }

    private fun loadUserInfo() {
        val uid = "local_user_123"
        val name = RecentLoginStore.getName(this) ?: "Test User"
        val first = name.trim().split(" ").firstOrNull() ?: name
        findViewById<TextView>(R.id.ivMyProfileIcon)?.let { tv ->
            Photos.bindAvatar(tv, name, uid, "")
        }
        findViewById<TextView>(R.id.storyMeAvatar)?.let { tv ->
            Photos.bindAvatar(tv, name, uid, "")
        }
        findViewById<TextView>(R.id.tvWhatsOnYourMind)?.text = "What's on your mind, $first?"
    }

    private fun renderFeed() {
        if (!::feed.isInitialized) return
        feed.submit(emptyList())
    }

    private fun renderStories() {
        // Local fallback: no-op
    }
}
