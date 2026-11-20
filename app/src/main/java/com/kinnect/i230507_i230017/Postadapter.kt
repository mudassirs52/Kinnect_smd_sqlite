package com.kinnect.i230507_i230017

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView

class PostAdapter(private val ctx: Context) : RecyclerView.Adapter<PostAdapter.VH>() {

    class Post(val id: String, val uid: String, val text: String, val ts: Long, val imageCount: Int, val authorName: String="", val images: List<String> = emptyList(), val reaction: String="", val syncState: String="")

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val avatar: TextView = v.findViewById(R.id.pAvatar)
        val name: TextView = v.findViewById(R.id.pName)
        val time: TextView = v.findViewById(R.id.pTime)
        val more: TextView = v.findViewById(R.id.pMore)
        val hide: TextView = v.findViewById(R.id.pHide)
        val text: TextView = v.findViewById(R.id.pText)
        val grid: LinearLayout = v.findViewById(R.id.pGrid)
        val like: TextView = v.findViewById(R.id.pLike)
        val comment: TextView = v.findViewById(R.id.pComment)
        val share: TextView = v.findViewById(R.id.pShare)

        var boundId = ""
        var myReaction: String? = null
        val stops = ArrayList<() -> Unit>()

        fun stop() {
            stops.forEach { it() }
            stops.clear()
        }
    }

    private var items: List<Post> = emptyList()
    private val hidden = HashSet<String>()
    private val holders = HashSet<VH>()

    fun submit(list: List<Post>) {
        items = list.filter { it.id !in hidden }
        notifyDataSetChanged()
    }

    fun release() {
        holders.forEach { it.stop() }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(ctx).inflate(R.layout.itempost, parent, false)
        return VH(v).also { holders.add(it) }
    }

    override fun getItemCount() = items.size

    override fun onViewRecycled(holder: VH) {
        holder.stop()
    }

    override fun onBindViewHolder(h: VH, position: Int) {
        val p = items[position]
        h.stop()
        h.boundId = p.id
        h.myReaction = p.reaction
        h.like.text = if (p.reaction.isEmpty()) "Like" else "${Reactions.emoji(p.reaction)} ${Reactions.label(p.reaction)}"
        h.like.setTextColor(if (p.reaction.isEmpty()) androidx.core.content.ContextCompat.getColor(ctx, R.color.kn_text2) else Reactions.color(p.reaction))

        h.name.text = p.authorName
        h.avatar.text = ""
        h.avatar.tag = null
        h.avatar.setBackgroundResource(R.drawable.kn_circle)
        UiUtils.tintAvatar(h.avatar, p.uid)
        UserCache.get(p.uid) { u ->
            if (h.boundId == p.id) {
                h.name.text = u.name
                Photos.bindAvatar(h.avatar, u.name, u.uid, u.profilePhoto)
            }
        }
        h.time.text = UiUtils.timeAgo(p.ts) + if(p.syncState.isEmpty()) " · 🌐" else " · ${p.syncState}"

        h.text.text = p.text
        h.text.visibility = if (p.text.isEmpty()) View.GONE else View.VISIBLE

        buildGrid(h.grid, p)

        h.like.setOnClickListener {
            Reactions.toggle(p.id, if(h.myReaction=="like") "" else "like")
        }
        h.like.setOnLongClickListener {
            showReactionPicker(h.like, p)
            true
        }
        h.comment.setOnClickListener {
            ctx.startActivity(Intent(ctx, CommentsActivity::class.java).putExtra("postId", p.id))
        }
        h.share.setOnClickListener {
            Toast.makeText(ctx, "Shared", Toast.LENGTH_SHORT).show()
        }

        h.more.setOnClickListener {
            AlertDialog.Builder(ctx)
                .setItems(arrayOf("Hide post", "Report")) { _, i ->
                    if (i == 0) {
                        hidden += p.id
                        submit(items)
                    }
                }
                .show()
        }
        h.hide.setOnClickListener {
            hidden += p.id
            submit(items)
        }
    }

    private fun buildGrid(grid: LinearLayout, p: Post) {
        grid.removeAllViews()
        if (p.imageCount <= 0) {
            grid.visibility = View.GONE
            return
        }
        grid.visibility = View.VISIBLE
        grid.orientation=LinearLayout.VERTICAL
        val shown=p.images.take(4)
        for(start in shown.indices.step(2)) {
            val row=LinearLayout(ctx)
            for(i in start until minOf(start+2,shown.size)) {
                val frame=android.widget.FrameLayout(ctx)
                val iv=ImageView(ctx).apply{scaleType=ImageView.ScaleType.CENTER_CROP}
                frame.addView(iv,android.widget.FrameLayout.LayoutParams(-1,-1))
                ImageCache.load("post_${p.id}_$i",shown[i],iv)
                if(i==3 && p.images.size>4)frame.addView(TextView(ctx).apply{text="+${p.images.size-4}";textSize=28f;gravity=android.view.Gravity.CENTER;setTextColor(Color.WHITE);setBackgroundColor(0x77000000)},android.widget.FrameLayout.LayoutParams(-1,-1))
                row.addView(frame,LinearLayout.LayoutParams(0,(180*ctx.resources.displayMetrics.density).toInt(),1f))
            };grid.addView(row)
        }
    }
    private fun showReactionPicker(anchor: View, p: Post) {
        ReactionPicker.show(anchor, p.reaction) { kind ->
            Reactions.toggle(p.id, if (p.reaction == kind) "" else kind)
        }
    }
}
