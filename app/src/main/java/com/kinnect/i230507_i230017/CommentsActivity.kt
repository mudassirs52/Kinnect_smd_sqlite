package com.kinnect.i230507_i230017

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class CommentsActivity : AppCompatActivity() {

    class Comment(
        val id: String, val uid: String, val text: String,
        val ts: Long, val parentId: String, val hasImage: Boolean, val imageUrl: String="", val liked: Boolean=false, val syncState: String=""
    )

    class Row(val c: Comment?, val reply: Boolean, val moreFor: String?, val more: Int)

    private lateinit var postId: String
    private var me = ""
    private lateinit var adapter: CommentAdapter

    private var comments: List<Comment> = emptyList()
    private val expanded = HashSet<String>()
    private var replyTo: Comment? = null
    private var pendingImage: String? = null
    private var pendingUri: Uri? = null
    private val refreshListener: () -> Unit = {loadComments()}
    private val stops = ArrayList<() -> Unit>()

    private val pickImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri -> uri?.let { prepareImage(it) } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        me = RecentLoginStore.getUserId(this)
        setContentView(R.layout.activity_comments)

        postId = intent.getStringExtra("postId") ?: run { finish(); return }

        findViewById<TextView>(R.id.tvCommentsBack).setOnClickListener { finish() }

        adapter = CommentAdapter()
        findViewById<RecyclerView>(R.id.rvComments).apply {
            layoutManager = LinearLayoutManager(this@CommentsActivity)
            adapter = this@CommentsActivity.adapter
        }

        findViewById<View>(R.id.btnPickImage).setOnClickListener { pickImage.launch("image/*") }
        findViewById<View>(R.id.btnRemovePending).setOnClickListener { clearPending() }
        findViewById<View>(R.id.tvCancelReply).setOnClickListener { cancelReply() }
        findViewById<View>(R.id.btnSend).setOnClickListener { send() }
    }

    private fun loadComments() {
        DataRepository.read("comments_get",org.json.JSONObject().put("post_id",postId)) {data ->
            val arr=data.optJSONArray("comments")?:return@read
            comments=(0 until arr.length()).map{i->val c=arr.getJSONObject(i);Comment(c.optString("id"),c.optString("user_id"),c.optString("text"),c.optLong("created_at"),c.optString("parent_id").takeUnless{it=="null"||it=="0"}?:"",c.optString("image_url").isNotEmpty(),c.optString("image_url"),c.optBoolean("liked"),c.optString("sync_state"))};rebuild()
        }
    }
    override fun onResume(){super.onResume();DataRepository.listeners.add(refreshListener);loadComments()}
    override fun onPause(){DataRepository.listeners.remove(refreshListener);super.onPause()}
    override fun onDestroy() {
        DataRepository.listeners.remove(refreshListener)
        stops.forEach { it() }
        stops.clear()
        super.onDestroy()
    }

    private fun prepareImage(uri: Uri) {
        pendingUri=uri
        Thread {
            val b64 = StoryUtils.decodeBitmap(this, uri, 800)?.let { StoryUtils.toBase64(it, 70) }
            runOnUiThread {
                if (b64 == null) {
                    Toast.makeText(this, "Could not read this image", Toast.LENGTH_SHORT).show()
                } else {
                    pendingImage = b64
                    findViewById<View>(R.id.pendingImageRow).visibility = View.VISIBLE
                    UiUtils.loadPhoto(this, b64, findViewById(R.id.ivPending))
                }
            }
        }.start()
    }

    private fun clearPending() {
        pendingImage = null
        pendingUri = null
        findViewById<View>(R.id.pendingImageRow).visibility = View.GONE
    }

    private fun cancelReply() {
        replyTo = null
        findViewById<View>(R.id.replyingBar).visibility = View.GONE
    }

    private fun send() {
        val et = findViewById<EditText>(R.id.etComment)
        val text = et.text.toString().trim()
        val img = pendingImage
        if (text.isEmpty() && img == null) return

        DataRepository.mutate("comments_create",org.json.JSONObject().put("post_id",postId).put("text",text).put("parent_id",replyTo?.id?:""),listOfNotNull(pendingUri)) {ok ->
            if(ok){et.setText("");clearPending();cancelReply();loadComments()}
        }
    }

    private fun rebuild() {
        val rows = ArrayList<Row>()
        val top = comments.filter { it.parentId.isEmpty() }.sortedBy { it.ts }
        val replies = comments.filter { it.parentId.isNotEmpty() }.groupBy { it.parentId }

        for (c in top) {
            rows += Row(c, false, null, 0)
            val list = replies[c.id]?.sortedBy { it.ts } ?: emptyList()
            if (list.isNotEmpty()) {
                val exp = c.id in expanded
                val shown = if (exp) list else list.take(2)
                for (r in shown) rows += Row(r, true, null, 0)
                if (!exp && list.size > 2) {
                    rows += Row(null, false, c.id, list.size - 2)
                }
            }
        }
        adapter.items = rows
        adapter.notifyDataSetChanged()
    }

    inner class CommentAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        var items = listOf<Row>()
        var likes = mapOf<String, Set<String>>()

        override fun getItemViewType(position: Int) = if (items[position].moreFor != null) 1 else 0
        override fun getItemCount() = items.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inf = LayoutInflater.from(parent.context)
            return if (viewType == 1) {
                object : RecyclerView.ViewHolder(inf.inflate(R.layout.item_comment, parent, false)) {}
            } else {
                CommentVH(inf.inflate(R.layout.item_comment, parent, false))
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val r = items[position]
            if (r.moreFor != null) {
                holder.itemView.setOnClickListener {
                    expanded += r.moreFor
                    rebuild()
                }
                return
            }
            val c = r.c ?: return
            val h = holder as CommentVH
            h.bind(c, r.reply)
        }
    }

    inner class CommentVH(v: View) : RecyclerView.ViewHolder(v) {
        val avatar: TextView = v.findViewById(R.id.cAvatar)
        val name: TextView = v.findViewById(R.id.cName)
        val text: TextView = v.findViewById(R.id.cText)
        val time: TextView = v.findViewById(R.id.cTime)
        val like: TextView = v.findViewById(R.id.cLike)
        val image: ImageView = v.findViewById(R.id.cImage)

        fun bind(c: Comment, isReply: Boolean) {
            val lp = itemView.layoutParams as ViewGroup.MarginLayoutParams
            val dp = itemView.resources.displayMetrics.density
            lp.marginStart = ((if (isReply) 44 else 12) * dp).toInt()
            itemView.layoutParams = lp

            avatar.text = ""
            UiUtils.tintAvatar(avatar, c.uid)
            UserCache.get(c.uid) { u ->
                name.text = u.name
                Photos.bindAvatar(avatar, u.name, u.uid, u.profilePhoto)
            }

            text.text = c.text
            text.visibility = if (c.text.isEmpty()) View.GONE else View.VISIBLE
            time.text = UiUtils.timeAgo(c.ts)+if(c.syncState.isEmpty()) "" else " · ${c.syncState}"

            if (c.hasImage) {
                image.visibility = View.VISIBLE
                ImageCache.load("comment_${c.id}", c.imageUrl, image)
            } else {
                image.visibility = View.GONE
            }

            val liked = c.liked
            like.text = if (liked) "Liked" else "Like"
            like.setTextColor(ContextCompat.getColor(this@CommentsActivity, if (liked) R.color.kn_teal else R.color.kn_text2))
            like.setOnClickListener {
                DataRepository.mutate("comment_like",org.json.JSONObject().put("comment_id",c.id).put("post_id",postId).put("liked",if(liked) "0" else "1"))
            }

            itemView.findViewById<TextView>(R.id.cReply).setOnClickListener {replyTo=c;findViewById<View>(R.id.replyingBar).visibility=View.VISIBLE}
            itemView.setOnLongClickListener {
                if (c.uid == me) {
                    AlertDialog.Builder(this@CommentsActivity)
                        .setTitle("Delete comment?")
                        .setPositiveButton("Delete") { _, _ ->
                            DataRepository.mutate("comment_delete",org.json.JSONObject().put("comment_id",c.id).put("post_id",postId))
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
                true
            }
        }
    }
}
