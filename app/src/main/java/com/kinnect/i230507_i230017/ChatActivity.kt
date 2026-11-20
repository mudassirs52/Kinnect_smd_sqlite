package com.kinnect.i230507_i230017

import android.database.ContentObserver
import android.content.Intent
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
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.Volley
import org.json.JSONObject

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

    private val refreshListener: () -> Unit = { loadMessages() }
    private var captureCallback: android.app.Activity.ScreenCaptureCallback? = null
    private var legacyCaptureObserver: ContentObserver? = null
    private var captureSince = 0L
    private var lastCaptureId = ""
    private val capturePermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if(granted && activeChatUid==other) watchLegacyCaptures()
    }
    private val handler = Handler(Looper.getMainLooper())
    private val pollRunnable = object : Runnable {
        override fun run() {
            loadMessages()
            DataRepository.read("presence_get",JSONObject().put("ids",other)){j->val online=j.optJSONArray("users")?.optJSONObject(0)?.optBoolean("online")==true
                findViewById<TextView>(R.id.tvChatStatus).text=if(online)"Active now" else "Offline"
                findViewById<View>(R.id.chatOnlineDot).visibility=if(online)View.VISIBLE else View.GONE
            }
            handler.postDelayed(this, 3000)
        }
    }

    private val pickMedia = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) uploadAndSendMedia(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        me = RecentLoginStore.getUserId(this)
        setContentView(R.layout.activity_chat)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.chatRoot)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        val myId = RecentLoginStore.getUserId(this)
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
        findViewById<ImageView>(R.id.btnGallery).setOnClickListener { pickMedia.launch("*/*") }
        findViewById<TextView>(R.id.btnCancelReply).setOnClickListener { cancelBar() }
        btnSend.setOnClickListener { onSendClicked() }

        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val hasText = !s.isNullOrBlank()
                btnSend.setImageResource(if (hasText) R.drawable.ic_send else R.drawable.ic_thumb_up)
            }
        })
    }

    override fun onResume() {
        super.onResume()
        activeChatUid = other
        DataRepository.listeners.add(refreshListener)
        registerCapture()
        handler.post(pollRunnable)
    }

    override fun onPause() {
        super.onPause()
        DataRepository.listeners.remove(refreshListener)
        unregisterCapture()
        if (activeChatUid == other) activeChatUid = null
        handler.removeCallbacks(pollRunnable)
    }

    override fun onStart() {
        super.onStart()
        if (chatId.isEmpty()) return
        loadMessages()
    }

    private fun loadMessages() {
        ChatManager.fetchMessages(this, other) { newMsgs ->
            container.removeAllViews()
            msgs.clear()
            rows.clear()
            lastTs = 0L
            for (m in newMsgs.sortedBy { it.ts }) {
                addMessage(m)
            }
        }
    }

    private fun uploadAndSendMedia(uri: Uri) {
        val mime=contentResolver.getType(uri)?:"application/octet-stream"
        val type=when {mime.startsWith("image/")->"image";mime.startsWith("video/")->"video";else->"file"}
        DataRepository.mutate("message_send",JSONObject().put("receiver_id",other).put("text","").put("media_type",type),listOf(uri)) {ok -> if(ok)loadMessages() else toast("Could not save attachment")}
    }

    private fun addMessage(m: Msg) {
        msgs[m.clientMsgId] = m
        if (m.ts > 0 && (lastTs == 0L || m.ts - lastTs > 30 * 60 * 1000L)) {
            container.addView(timeText(m.ts))
        }
        if (m.ts > 0) lastTs = m.ts

        val row = layoutInflater.inflate(R.layout.item_message, container, false)
        rows[m.clientMsgId] = row
        bind(row, m)
        container.addView(row)
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
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

        if (m.deleted) {
            label.visibility = View.GONE
            quote.visibility = View.GONE
            image.visibility = View.GONE
            meta.visibility = View.GONE
            bubble.visibility = View.VISIBLE
            bubble.text = if (mine) "You removed a message" else "Message removed"
            bubble.setBackgroundResource(R.drawable.kn_pill_outline)
            bubble.setTextColor(getColor(R.color.kn_text2))
            bubble.setTypeface(null, Typeface.ITALIC)
            return
        }

        bubble.setTypeface(null, Typeface.NORMAL)

        if (m.mediaUrl.isNotEmpty() && m.mediaType == "image") {
            image.visibility = View.VISIBLE
            ImageCache.load("msg_${m.clientMsgId}", m.mediaUrl, image)
        } else if (m.mediaUrl.isNotEmpty()) {
            bubble.visibility = View.VISIBLE
            bubble.text = "Open ${m.mediaType}"
            bubble.setOnClickListener {
                MediaFiles.loadFile(m.mediaUrl){file ->
                    if(file==null)toast("Connect to download this attachment")
                    else if(!isDestroyed)try{
                        val uri=androidx.core.content.FileProvider.getUriForFile(this,"$packageName.fileprovider",file)
                        val mime=android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension)?:"application/octet-stream"
                        startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri,mime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
                    }catch(e:Exception){toast("No app is available to open this file")}
                }
            }
        }

        if (m.text.isNotEmpty()) {
            bubble.visibility = View.VISIBLE
            bubble.text = m.text
            bubble.setBackgroundResource(if (mine) R.drawable.kn_bubble_out else R.drawable.kn_bubble_in)
            bubble.setTextColor(getColor(if (mine) R.color.white else R.color.kn_text))
        } else if (m.mediaUrl.isEmpty()) {
            bubble.visibility = View.GONE
        }

        edited.text = m.syncState.ifEmpty { if(m.edited) "Edited" else "" }
        edited.visibility = if (m.edited || m.syncState.isNotEmpty()) View.VISIBLE else View.GONE
        meta.visibility = if (m.edited || m.syncState.isNotEmpty()) View.VISIBLE else View.GONE
    }

    private fun showOptions(m: Msg) {
        if (m.deleted) return
        val canChange = m.from == me && System.currentTimeMillis() - m.ts < ChatUtils.EDIT_WINDOW_MS

        val labels = ArrayList<String>()
        val actions = ArrayList<() -> Unit>()

        labels += "Reply"
        actions += { startReply(m) }

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
        tvReplyLabel.text = "Replying to message"
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
                ChatManager.delete(this, m.clientMsgId) { ok ->
                    if (ok) loadMessages() else toast("You can't delete this message after 5 minutes")
                }
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
                ChatManager.edit(this, edit.clientMsgId, text) { ok ->
                    if (ok) loadMessages() else toast("You can't edit this message after 5 minutes")
                }
            }
            input.setText("")
            clearBar()
            return
        }

        if (text.isEmpty()) return

        ChatManager.send(this, me, other, text, "", "text", replyTo?.id?.toIntOrNull()) { ok ->
            if (ok) loadMessages() else toast("Message not sent")
        }
        input.setText("")
        clearBar()
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

    private fun registerCapture() {
        if(Build.VERSION.SDK_INT>=34) {
            val callback=android.app.Activity.ScreenCaptureCallback {
                DataRepository.mutate("screenshot_report",JSONObject().put("receiver_id",other))
            };captureCallback=callback;registerScreenCaptureCallback(mainExecutor,callback)
        } else {
            val permission=if(Build.VERSION.SDK_INT>=33)android.Manifest.permission.READ_MEDIA_IMAGES else android.Manifest.permission.READ_EXTERNAL_STORAGE
            if(androidx.core.content.ContextCompat.checkSelfPermission(this,permission)==android.content.pm.PackageManager.PERMISSION_GRANTED)watchLegacyCaptures()
            else capturePermission.launch(permission)
        }
        DataRepository.read("profile_get",JSONObject().put("user_id",other)) {data ->
            val u=data.optJSONObject("user")?:return@read
            findViewById<TextView>(R.id.tvChatName).text=u.optString("name")
            findViewById<TextView>(R.id.tvChatStatus).text=if(u.optBoolean("online"))"Active now" else "Offline"
            findViewById<View>(R.id.chatOnlineDot).visibility=if(u.optBoolean("online"))View.VISIBLE else View.GONE
            Photos.bindAvatar(findViewById(R.id.chatAvatar),u.optString("name"),other,u.optString("profile_photo"))
            otherUser=UserLite(other,u.optString("name"),u.optString("username"),u.optString("bio"),u.optLong("created_at"),u.optString("profile_photo"),u.optString("cover_photo"))
        }
    }
    private fun watchLegacyCaptures() {
        if(legacyCaptureObserver!=null)return
        captureSince=System.currentTimeMillis()
        val observer=object:ContentObserver(handler){
            override fun onChange(selfChange: Boolean) {
                DataRepository.io.execute {
                    try {
                        contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,arrayOf(MediaStore.Images.Media._ID,MediaStore.Images.Media.DISPLAY_NAME,MediaStore.Images.Media.DATE_ADDED),null,null,"${MediaStore.Images.Media.DATE_ADDED} DESC")?.use{cursor ->
                            if(cursor.moveToFirst()) {
                                val id=cursor.getString(0);val name=cursor.getString(1)?:"";val time=cursor.getLong(2)*1000
                                if(id!=lastCaptureId && time>=captureSince-1000 && name.contains("screenshot",true) && activeChatUid==other) {
                                    lastCaptureId=id;DataRepository.mutate("screenshot_report",JSONObject().put("receiver_id",other))
                                }
                            }
                        }
                    }catch(_:Exception){}
                }
            }
        }
        legacyCaptureObserver=observer
        contentResolver.registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,true,observer)
    }
    private fun unregisterCapture(){
        if(Build.VERSION.SDK_INT>=34)captureCallback?.let{unregisterScreenCaptureCallback(it)}
        captureCallback=null
        legacyCaptureObserver?.let{contentResolver.unregisterContentObserver(it)};legacyCaptureObserver=null
    }
    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}
