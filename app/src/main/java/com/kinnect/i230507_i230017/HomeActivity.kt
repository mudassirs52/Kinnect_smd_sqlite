package com.kinnect.i230507_i230017

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
    private val localNetwork = LocalNetworkPermission(this)

    private var friendIds: Set<String> = emptySet()
    private lateinit var feed: PostAdapter
    private val nameCache = HashMap<String, String>()
    private val fetching = HashSet<String>()
    private val removers = mutableListOf<() -> Unit>()
    private val refreshListener: () -> Unit = { loadUserInfo();renderFeed();renderStories() }
    private val handler = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            renderStories()
            renderFeed()
            handler.postDelayed(this, 15000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        if (Build.VERSION.SDK_INT >= 33 && LocalNetworkPermission.granted(this)) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        loadUserInfo()
        if (!LocalNetworkPermission.granted(this)) {
            localNetwork.ensure { renderFeed(); renderStories() }
        }

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
        loadUserInfo()
        val me = RecentLoginStore.getUserId(this)
        UiUtils.bindBadges(this, removers)
        DataRepository.listeners.add(refreshListener)
        renderFeed();renderStories()
        handler.postDelayed(tick, 15000)
    }

    override fun onStop() {
        super.onStop()
        removers.forEach { it() }
        removers.clear()
        DataRepository.listeners.remove(refreshListener)
        handler.removeCallbacks(tick)
        if (::feed.isInitialized) feed.release()
    }

    private fun loadUserInfo() {
        val uid = RecentLoginStore.getUserId(this)
        val name = RecentLoginStore.getName(this) ?: "Kinnect user"
        val first = name.trim().split(" ").firstOrNull() ?: name
        findViewById<TextView>(R.id.ivMyProfileIcon)?.let { tv ->
            Photos.bindUserAvatar(tv, name, uid)
        }
        findViewById<TextView>(R.id.storyMeAvatar)?.let { tv ->
            Photos.bindUserAvatar(tv, name, uid)
        }
        findViewById<TextView>(R.id.tvWhatsOnYourMind)?.text = "What's on your mind, $first?"
    }

    private fun renderFeed() {
        if (!::feed.isInitialized) return
        DataRepository.read("posts_get") {json ->
            val arr=json.optJSONArray("posts")?:return@read
            feed.submit((0 until arr.length()).map {i -> val p=arr.getJSONObject(i);val images=p.optJSONArray("images")?:org.json.JSONArray()
                PostAdapter.Post(p.getString("id"),p.getString("user_id"),p.optString("text"),p.optLong("created_at"),images.length(),p.optString("author_name"),(0 until images.length()).map{images.getString(it)},p.optString("my_reaction"),p.optString("sync_state"))
            })
        }
    }

    private fun renderStories() {
        DataRepository.read("stories_get") { json ->
            val stories = json.optJSONArray("stories") ?: return@read
            val container = findViewById<LinearLayout>(R.id.storiesContainer)
            val me = RecentLoginStore.getUserId(this)
            // Show each owner's newest story, keeping your own story first.
            val latestByOwner = (0 until stories.length()).map { stories.getJSONObject(it) }
                .groupBy { it.getString("user_id") }
                .mapValues { (_, items) -> items.maxBy { it.optLong("created_at") } }
            val entries = latestByOwner.values.sortedWith(
                compareByDescending<org.json.JSONObject> { it.getString("user_id") == me }
                    .thenByDescending { it.optLong("created_at") }
            )
            container.removeAllViews()
            for (story in entries) {
                val owner = story.getString("user_id")
                val name = if (owner == me) "Your story" else story.optString("author_name")
                container.addView(storyCard(owner, name, story))
            }
        }
    }

    private fun storyCard(owner: String, name: String, story: org.json.JSONObject): View {
        val dp = resources.displayMetrics.density
        val card = CardView(this)
        card.layoutParams = LinearLayout.LayoutParams((78 * dp).toInt(), (160 * dp).toInt())
            .apply { marginStart = (6 * dp).toInt() }
        card.radius = 12 * dp
        card.cardElevation = 0f
        card.setCardBackgroundColor(Color.parseColor("#2F3B4B"))
        card.setOnClickListener {
            startActivity(
                Intent(this@HomeActivity, StoryViewerActivity::class.java).putExtra("uid", owner)
            )
        }

        val frame = FrameLayout(this)

        val iv = ImageView(this)
        iv.scaleType = ImageView.ScaleType.CENTER_CROP
        val path = story.optString("media_url")
        val key = "story_" + story.getString("id")
        if (story.optString("media_type") == "video") {
            ImageCache.loadVideoThumbnail(key, path, iv)
        } else {
            ImageCache.load(key, path, iv)
        }
        frame.addView(iv, FrameLayout.LayoutParams(-1, -1))

        val avatar = TextView(this).apply {
            setBackgroundResource(R.drawable.kn_circle)
            UiUtils.tintAvatar(this, owner)
            gravity = Gravity.CENTER
            text = UiUtils.initialsOf(name.ifEmpty { "?" })
            setTextColor(Color.WHITE)
            textSize = 10f
            setTypeface(typeface, Typeface.BOLD)
        }
        Photos.bindUserAvatar(avatar, story.optString("author_name", name), owner)
        frame.addView(
            avatar,
            FrameLayout.LayoutParams((32 * dp).toInt(), (32 * dp).toInt())
                .apply { setMargins((8 * dp).toInt(), (8 * dp).toInt(), 0, 0) }
        )

        val tv = TextView(this).apply {
            text = name
            setTextColor(Color.WHITE)
            textSize = 10f
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            setShadowLayer(4f, 0f, 0f, Color.BLACK)
        }
        frame.addView(
            tv,
            FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM).apply {
                bottomMargin = (14 * dp).toInt()
                marginStart = (4 * dp).toInt()
                marginEnd = (4 * dp).toInt()
            }
        )

        card.addView(frame)
        return card
    }
}
