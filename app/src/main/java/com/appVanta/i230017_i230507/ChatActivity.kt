package com.appVanta.i230017_i230507

import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.graphics.Bitmap
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ChatActivity : AppCompatActivity() {

    companion object {
        var activeChatUid: String? = null
    }

    private var me = ""
    private var other = ""
    private var chatId = ""
    private var otherUser: UserLite? = null
    private var isFriend = false
    private var online = false
    private var replyTo: Msg? = null
    private var editing: Msg? = null
    private var lastTs = 0L
    private val msgs = HashMap<String, Msg>()
    private val rows = HashMap<String, View>()
    private val bitmaps = HashMap<String, Bitmap>()
    private val removers = mutableListOf<() -> Unit>()

    private lateinit var input: EditText
    private lateinit var btnSend: ImageView
    private lateinit var container: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var replyBar: View
    private lateinit var tvReplyLabel: TextView

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) sendImage(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.chatRoot)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        val myId = "local_user_123"
        val otherId = intent.getStringExtra("uid")
        if (otherId == null || myId == otherId) {
            finish()
            return
        }
        me = myId
        other = otherId
        chatId = ChatUtils.chatId(me, other)

        input = findViewById(R.id.chatInput)
        btnSend = findViewById(R.id.btnSend)
        container = findViewById(R.id.messagesContainer)
        scroll = findViewById(R.id.chatScroll)
        replyBar = findViewById(R.id.replyBar)
        tvReplyLabel = findViewById(R.id.tvReplyLabel)

        findViewById<ImageButton>(R.id.chat_back).setOnClickListener { finish() }
        findViewById<View>(R.id.btn_voice_call).setOnClickListener {
            startCallWith(other, otherUser?.name ?: "Kinnect user", video = false)
        }
        findViewById<View>(R.id.btn_video_call).setOnClickListener {
            startCallWith(other, otherUser?.name ?: "Kinnect user", video = true)
        }
        findViewById<ImageView>(R.id.btnGallery).setOnClickListener { pickImage.launch("image/*") }
        findViewById<TextView>(R.id.btnCancelReply).setOnClickListener { cancelBar() }
        btnSend.setOnClickListener { onSendClicked() }

        findViewById<View>(R.id.btnChatInfo)?.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Chat Security & Privacy")
                .setMessage("Screenshots taken in this chat notify the other user and add a system message (\"Name took a screenshot\") to the conversation.")
                .setPositiveButton("Simulate Screenshot") { _, _ ->
                    handleScreenshot()
                }
                .setNegativeButton("Close", null)
                .show()
        }

        registerScreenshotObserver()

        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val hasText = !s.isNullOrBlank()
                btnSend.setImageResource(if (hasText) R.drawable.ic_send else R.drawable.ic_thumb_up)
            }
        })
    }

    private var lastScreenshotTime = 0L
    private var screenshotObserver: ContentObserver? = null

    private fun registerScreenshotObserver() {
        if (Build.VERSION.SDK_INT >= 34) {
            try {
                registerScreenCaptureCallback(mainExecutor) {
                    handleScreenshot()
                }
            } catch (_: Exception) {}
        } else {
            val handler = Handler(Looper.getMainLooper())
            val observer = object : ContentObserver(handler) {
                override fun onChange(selfChange: Boolean, uri: Uri?) {
                    super.onChange(selfChange, uri)
                    if (uri != null && uri.toString().contains("media/external/images/media")) {
                        try {
                            contentResolver.query(
                                uri,
                                arrayOf(MediaStore.Images.Media.DATA),
                                null,
                                null,
                                null
                            )?.use { cursor ->
                                if (cursor.moveToFirst()) {
                                    val path = cursor.getString(0) ?: ""
                                    if (path.contains("screenshot", ignoreCase = true) ||
                                        path.contains("Screenshots", ignoreCase = true)
                                    ) {
                                        handleScreenshot()
                                    }
                                }
                            }
                        } catch (_: Exception) {}
                    }
                }
            }
            try {
                contentResolver.registerContentObserver(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    true,
                    observer
                )
                screenshotObserver = observer
            } catch (_: Exception) {}
        }
    }

    private fun handleScreenshot() {
        val now = System.currentTimeMillis()
        if (now - lastScreenshotTime < 3000L) return
        lastScreenshotTime = now
        ChatManager.send(me, other, "Someone took a screenshot", "", null)
        Toast.makeText(this, "Security notification sent to chat", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        screenshotObserver?.let {
            try { contentResolver.unregisterContentObserver(it) } catch (_: Exception) {}
        }
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        activeChatUid = other
    }

    override fun onPause() {
        super.onPause()
        if (activeChatUid == other) activeChatUid = null
    }

    override fun onStart() {
        super.onStart()
        if (chatId.isEmpty()) return

        container.removeAllViews()
        msgs.clear()
        rows.clear()
        lastTs = 0L
    }

    override fun onStop() {
        super.onStop()
        removers.forEach { it() }
        removers.clear()
    }

    private fun bindHeader() {
        val u = otherUser ?: return
        findViewById<TextView>(R.id.tvChatName).text = u.name
        findViewById<TextView>(R.id.tvIntroName).text = u.name
        findViewById<TextView>(R.id.tvChatStatus).text = if (online) "Active now" else "Offline"
        findViewById<View>(R.id.chatOnlineDot).visibility = if (online) View.VISIBLE else View.GONE
        findViewById<TextView>(R.id.tvIntroSub).text = when {
            isFriend -> "You're friends on Kinnect"
            u.username.isNotEmpty() -> "@${u.username}"
            else -> ""
        }
        for (id in listOf(R.id.chatAvatar, R.id.tvIntroAvatar)) {
            val avatar = findViewById<TextView>(id)
            avatar.text = UiUtils.initialsOf(u.name)
            UiUtils.tintAvatar(avatar, u.uid)
        }
    }

    private fun firstName(): String = otherUser?.name?.split(" ")?.firstOrNull() ?: "They"

    private fun addMessage(m: Msg) {
        msgs[m.id] = m
        if (m.ts > 0 && (lastTs == 0L || m.ts - lastTs > 30 * 60 * 1000L)) {
            container.addView(timeText(m.ts))
        }
        if (m.ts > 0) lastTs = m.ts

        val row = layoutInflater.inflate(R.layout.item_message, container, false)
        rows[m.id] = row
        bind(row, m)
        container.addView(row)

        if (m.from != me) markRead()
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
    }

    private fun updateMessage(m: Msg) {
        msgs[m.id] = m
        rows[m.id]?.let { bind(it, m) }
    }

    private fun bind(row: View, m: Msg) {
        val mine = m.from == me
        (row as LinearLayout).gravity = if (mine) Gravity.END else Gravity.START

        val label = row.findViewById<TextView>(R.id.tvReplyLabel)
        val quote = row.findViewById<TextView>(R.id.tvQuote)
        val image = row.findViewById<ImageView>(R.id.ivMsgImage)
        val bubble = row.findViewById<TextView>(R.id.tvBubble)
        val meta = row.findViewById<LinearLayout>(R.id.rowMeta)
        val edited = row.findViewById<TextView>(R.id.tvEdited)
        val heart = row.findViewById<ImageView>(R.id.ivHeart)

        row.setOnLongClickListener {
            showOptions(m)
            true
        }

        val isSystem = m.text.endsWith(" took a screenshot")
        if (isSystem) {
            label.visibility = View.GONE
            quote.visibility = View.GONE
            image.visibility = View.GONE
            meta.visibility = View.GONE
            bubble.visibility = View.VISIBLE
            bubble.text = "“${m.text}”"
            bubble.setBackgroundResource(R.drawable.kn_pill_gray)
            bubble.setTextColor(getColor(R.color.kn_text2))
            bubble.setTypeface(null, Typeface.ITALIC)
            (row as LinearLayout).gravity = Gravity.CENTER
            return
        }

        if (m.deleted) {
            label.visibility = View.GONE
            quote.visibility = View.GONE
            image.visibility = View.GONE
            meta.visibility = View.GONE
            bubble.visibility = View.VISIBLE
            bubble.text = if (mine) "You removed a message" else "${firstName()} removed a message"
            bubble.setBackgroundResource(R.drawable.kn_pill_outline)
            bubble.setTextColor(getColor(R.color.kn_text2))
            bubble.setTypeface(null, Typeface.ITALIC)
            return
        }

        bubble.setTypeface(null, Typeface.NORMAL)

        if (m.replyId.isNotEmpty()) {
            label.visibility = View.VISIBLE
            label.text = replyLabel(m)
            quote.visibility = View.VISIBLE
            quote.text = m.replyText.ifEmpty { "Photo" }
        } else {
            label.visibility = View.GONE
            quote.visibility = View.GONE
        }

        if (m.image.isNotEmpty()) {
            val bmp = bitmaps[m.id] ?: ImageUtils.toBitmap(m.image)?.also { bitmaps[m.id] = it }
            if (bmp != null) {
                image.setImageBitmap(bmp)
                image.setBackgroundResource(R.drawable.kn_bubble_in)
                image.clipToOutline = true
                image.visibility = View.VISIBLE
            } else {
                image.visibility = View.GONE
            }
        } else {
            image.visibility = View.GONE
        }

        if (m.text.isNotEmpty()) {
            bubble.visibility = View.VISIBLE
            bubble.text = m.text
            bubble.setBackgroundResource(if (mine) R.drawable.kn_bubble_out else R.drawable.kn_bubble_in)
            bubble.setTextColor(getColor(if (mine) R.color.white else R.color.kn_text))
        } else {
            bubble.visibility = View.GONE
        }

        edited.visibility = if (m.edited) View.VISIBLE else View.GONE
        heart.visibility = if (m.hearts.isNotEmpty()) View.VISIBLE else View.GONE
        meta.visibility = if (m.edited || m.hearts.isNotEmpty()) View.VISIBLE else View.GONE
    }

    private fun replyLabel(m: Msg): String {
        val name = firstName()
        return when {
            m.from == me && m.replyFrom == me -> "You replied to yourself"
            m.from == me -> "You replied to $name"
            m.replyFrom == me -> "$name replied to you"
            else -> "$name replied to themselves"
        }
    }

    private fun showOptions(m: Msg) {
        if (m.deleted) return
        val canChange = m.from == me && System.currentTimeMillis() - m.ts < ChatUtils.EDIT_WINDOW_MS

        val labels = ArrayList<String>()
        val actions = ArrayList<() -> Unit>()

        labels += "Reply"
        actions += { startReply(m) }

        labels += if (me in m.hearts) "Remove heart" else "React with heart"
        actions += { ChatManager.toggleHeart(me, other, m) }

        if (canChange) {
            if (m.text.isNotEmpty()) {
                labels += "Edit"
                actions += { startEdit(m) }
            }
            labels += "Delete"
            actions += { confirmDelete(m) }
        }

        AlertDialog.Builder(this)
            .setItems(labels.toTypedArray()) { _, i -> actions[i]() }
            .show()
    }

    private fun startReply(m: Msg) {
        editing = null
        replyTo = m
        val who = if (m.from == me) "yourself" else firstName()
        tvReplyLabel.text = "Replying to $who"
        replyBar.visibility = View.VISIBLE
        showKeyboard()
    }

    private fun startEdit(m: Msg) {
        replyTo = null
        editing = m
        tvReplyLabel.text = "Editing message"
        replyBar.visibility = View.VISIBLE
        input.setText(m.text)
        input.setSelection(m.text.length)
        btnSend.setImageResource(R.drawable.ic_send)
        showKeyboard()
    }

    private fun confirmDelete(m: Msg) {
        AlertDialog.Builder(this)
            .setTitle("Delete this message?")
            .setPositiveButton("Delete") { _, _ ->
                ChatManager.delete(me, other, m) { ok -> if (!ok) toast("You can't delete this message anymore") }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun clearBar() {
        replyTo = null
        editing = null
        replyBar.visibility = View.GONE
    }

    private fun cancelBar() {
        if (editing != null) input.setText("")
        clearBar()
    }

    private fun onSendClicked() {
        val text = input.text.toString().trim()
        val edit = editing

        if (edit != null) {
            if (text.isEmpty()) {
                toast("Message can't be empty")
                return
            }
            if (text != edit.text) {
                ChatManager.edit(me, other, edit, text) { ok -> if (!ok) toast("You can't edit this message anymore") }
            }
            input.setText("")
            clearBar()
            return
        }

        ChatManager.send(me, other, text.ifEmpty { "👍" }, "", replyTo) { ok -> if (!ok) toast("Message not sent") }
        input.setText("")
        clearBar()
    }

    private fun sendImage(uri: Uri) {
        Thread {
            val b64 = ImageUtils.encode(this, uri)
            runOnUiThread {
                if (b64 == null) {
                    toast("Could not read this image")
                } else {
                    ChatManager.send(me, other, "", b64, replyTo) { ok -> if (!ok) toast("Image not sent") }
                    clearBar()
                }
            }
        }.start()
    }

    private fun markRead() {
        // Local fallback: no-op
    }

    private fun showKeyboard() {
        input.requestFocus()
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun timeText(ts: Long) = TextView(this).apply {
        text = ChatUtils.timeLabel(ts)
        gravity = Gravity.CENTER
        setTextColor(getColor(R.color.kn_text2))
        textSize = 11f
        setTypeface(typeface, Typeface.BOLD)
        setPadding(0, (14 * resources.displayMetrics.density).toInt(), 0, (8 * resources.displayMetrics.density).toInt())
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun startCallWith(peerUid: String, peerName: String, video: Boolean) {
        val type = if (video) "video" else "voice"
        CallManager.start(peerUid, peerName, type, { call ->
            val target = if (video) VideoCallActivity::class.java else VoiceCallActivity::class.java
            startActivity(Intent(this, target).putCall(call))
        }, { msg ->
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        })
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}
