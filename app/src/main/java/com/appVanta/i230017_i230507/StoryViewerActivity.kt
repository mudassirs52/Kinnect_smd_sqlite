package com.appVanta.i230017_i230507

import android.R as AndroidR
import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class StoryViewerActivity : AppCompatActivity() {

    private class Item(
        val label: String
    )

    private val items = ArrayList<Item>()
    private val bars = ArrayList<ProgressBar>()
    private var index = 0
    private var animator: ValueAnimator? = null
    private var stopSeen: (() -> Unit)? = null
    private var stopReacts: (() -> Unit)? = null
    private lateinit var owner: String
    private var me: String = "local_user_123"
    private val isOwner get() = owner == me

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_story_viewer)

        owner = intent.getStringExtra("uid") ?: run { finish(); return }

        findViewById<TextView>(R.id.btnCloseStory).setOnClickListener { finish() }
        findViewById<View>(R.id.zoneNext).setOnClickListener { show(index + 1) }
        findViewById<View>(R.id.zonePrev).setOnClickListener { show(index - 1) }

        findViewById<View>(R.id.barViewer).visibility = if (isOwner) View.GONE else View.VISIBLE
        findViewById<View>(R.id.barOwner).visibility = if (isOwner) View.VISIBLE else View.GONE
        findViewById<View>(R.id.btnMore).visibility = if (isOwner) View.GONE else View.VISIBLE

        findViewById<View>(R.id.rLike).setOnClickListener { react("like") }
        findViewById<View>(R.id.rLove).setOnClickListener { react("love") }
        findViewById<View>(R.id.rHaha).setOnClickListener { react("haha") }

        findViewById<View>(R.id.ownCreate).setOnClickListener {
            startActivity(Intent(this, CameraActivity::class.java))
        }
        findViewById<View>(R.id.ownMore).setOnClickListener { confirmDelete() }

        val avatar = findViewById<TextView>(R.id.storyAvatar)
        UiUtils.tintAvatar(avatar, owner)
        findViewById<TextView>(R.id.storyName).text = if (isOwner) "Your story" else "User"
        Photos.bindAvatar(avatar, "User", owner, "")

        items.add(Item(""))
        val container = findViewById<LinearLayout>(R.id.barsContainer)
        val pb = ProgressBar(this, null, AndroidR.attr.progressBarStyleHorizontal).apply {
            max = 1000
            progressTintList = ColorStateList.valueOf(Color.WHITE)
            progressBackgroundTintList = ColorStateList.valueOf(Color.parseColor("#55FFFFFF"))
        }
        container.addView(pb, LinearLayout.LayoutParams(0, -1, 1f))
        bars.add(pb)
        show(0)
    }

    private fun show(i: Int) {
        animator?.cancel()
        if (i < 0) { finish(); return }
        if (i >= items.size) { finish(); return }
        index = i

        for (j in bars.indices) {
            bars[j].progress = when {
                j < i -> 1000
                j > i -> 0
                else -> 0
            }
        }

        val item = items[i]
        val img = findViewById<ImageView>(R.id.storyImage)
        val vid = findViewById<VideoView>(R.id.storyVideo)
        val tvLabel = findViewById<TextView>(R.id.storyLabel)

        img.visibility = View.VISIBLE
        vid.visibility = View.GONE
        img.setImageResource(R.drawable.image1)

        tvLabel.text = item.label
        tvLabel.visibility = if (item.label.isEmpty()) View.GONE else View.VISIBLE

        val currentBar = bars[i]
        animator = ValueAnimator.ofInt(0, 1000).apply {
            duration = 5000
            interpolator = LinearInterpolator()
            addUpdateListener { currentBar.progress = it.animatedValue as Int }
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {}
                override fun onAnimationEnd(animation: Animator) { show(index + 1) }
                override fun onAnimationCancel(animation: Animator) {}
                override fun onAnimationRepeat(animation: Animator) {}
            })
            start()
        }
    }

    private fun react(kind: String) {
        Toast.makeText(this, "Reacted $kind", Toast.LENGTH_SHORT).show()
    }

    private fun confirmDelete() {
        AlertDialog.Builder(this)
            .setTitle("Delete story?")
            .setPositiveButton("Delete") { _, _ -> finish() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroy() {
        animator?.cancel()
        stopSeen?.invoke()
        stopReacts?.invoke()
        super.onDestroy()
    }
}
