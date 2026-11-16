package com.appVanta.i230017_i230507

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.graphics.Typeface
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ChatsActivity : AppCompatActivity() {

    private data class ChatRow(
        val uid: String,
        val lastText: String,
        val lastFrom: String,
        val lastTs: Long,
        val unread: Int
    )

    private var me = "local_user_123"
    private var users = mapOf<String, UserLite>()
    private var online = mapOf<String, Boolean>()
    private var friends = setOf<String>()
    private var chats = listOf<ChatRow>()
    private val removers = mutableListOf<() -> Unit>()

    private lateinit var search: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chats)

        search = findViewById(R.id.chatsSearch)

        findViewById<ImageView>(R.id.btnBack)?.setOnClickListener { finish() }

        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                render()
            }
        })
    }

    override fun onStart() {
        super.onStart()
        // Local fallback: no-op for observation
    }

    override fun onStop() {
        super.onStop()
        removers.forEach { it() }
        removers.clear()
    }

    private fun render() {
        renderFriends()
        renderChats(search.text.toString().trim().lowercase())
    }

    private fun renderFriends() {
        val scroll = findViewById<View>(R.id.activeFriendsScroll)
        val box = findViewById<LinearLayout>(R.id.activeFriendsContainer)
        box.removeAllViews()

        val list = friends.mapNotNull { users[it] }
            .sortedWith(compareByDescending<UserLite> { online[it.uid] == true }.thenBy { it.name.lowercase() })

        scroll.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE

        for (u in list) {
            val item = layoutInflater.inflate(R.layout.item_active_friend, box, false)
            val avatar = item.findViewById<TextView>(R.id.tvAvatar)
            Photos.bindAvatar(avatar, u.name, u.uid, u.profilePhoto)
            item.findViewById<View>(R.id.onlineDot).visibility =
                if (online[u.uid] == true) View.VISIBLE else View.GONE
            item.findViewById<TextView>(R.id.tvName).text = u.name.split(" ").first()
            item.setOnClickListener { openChat(u.uid) }
            box.addView(item)
        }
    }

    private fun renderChats(q: String) {
        val box = findViewById<LinearLayout>(R.id.chat_list_container)
        box.removeAllViews()

        val rows = chats.mapNotNull { c -> users[c.uid]?.let { it to c } }
            .filter { q.isEmpty() || it.first.name.lowercase().contains(q) }

        val empty = findViewById<TextView>(R.id.tvChatsEmpty)
        if (rows.isEmpty()) {
            empty.visibility = View.VISIBLE
            empty.text = if (q.isEmpty()) "No conversations yet. Tap a friend above to start chatting." else "No chats match your search"
            return
        }
        empty.visibility = View.GONE

        for ((u, c) in rows) {
            val row = layoutInflater.inflate(R.layout.item_chat_row, box, false)
            val avatar = row.findViewById<TextView>(R.id.tvAvatar)
            Photos.bindAvatar(avatar, u.name, u.uid, u.profilePhoto)
            row.findViewById<View>(R.id.onlineDot).visibility =
                if (online[u.uid] == true) View.VISIBLE else View.GONE
            row.findViewById<TextView>(R.id.tvName).text = u.name

            val prefix = if (c.lastFrom == me) "You: " else ""
            val preview = row.findViewById<TextView>(R.id.tvPreview)
            preview.text = "$prefix${c.lastText} · ${ChatUtils.listTime(c.lastTs)}"
            val unread = c.unread > 0
            preview.setTypeface(null, if (unread) Typeface.BOLD else Typeface.NORMAL)
            preview.setTextColor(getColor(if (unread) R.color.kn_text else R.color.kn_text2))
            row.findViewById<View>(R.id.unreadDot).visibility = if (unread) View.VISIBLE else View.GONE

            row.setOnClickListener { openChat(u.uid) }
            box.addView(row)
        }
    }

    private fun openChat(uid: String) {
        startActivity(Intent(this, ChatActivity::class.java).putExtra("uid", uid))
    }
}
