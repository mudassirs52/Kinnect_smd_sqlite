package com.kinnect.i230507_i230017

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity

/** Screen 05. Launch with extra "postId". Same reaction removes it, a different one replaces it. */
class ReactionPickerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reaction_picker)

        val postId = intent.getStringExtra("postId")

        val map = mapOf(
            R.id.react_like to "like",
            R.id.react_love to "love",
            R.id.react_haha to "haha",
            R.id.react_wow to "wow",
            R.id.react_sad to "sad",
            R.id.react_angry to "angry"
        )
        for ((id, kind) in map) {
            findViewById<View>(id)?.setOnClickListener {
                if (postId != null) Reactions.toggle(postId, kind)
                finish()
            }
        }

        // tap outside the bar to dismiss
        findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
            ?.setOnClickListener { finish() }
    }
}