package com.appVanta.i230017_i230507

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

    class Post(val id: String, val uid: String, val text: String, val ts: Long, val imageCount: Int)

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
        h.myReaction = null

        h.name.text = ""
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
        h.time.text = UiUtils.timeAgo(p.ts) + " · 🌐"

        h.text.text = p.text
        h.text.visibility = if (p.text.isEmpty()) View.GONE else View.VISIBLE

        buildGrid(h.grid, p)

        h.like.setOnClickListener {
            Reactions.toggle(p.id, "like")
        }
        h.like.setOnLongClickListener {
            showReactionPicker()
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
        val iv = ImageView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (220 * ctx.resources.displayMetrics.density).toInt())
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundColor(Color.parseColor("#E0E0E0"))
        }
        ImageCache.load("post_${p.id}_0", "postImages/${p.id}/0", iv)
        grid.addView(iv)
    }

    private fun showReactionPicker() {
        val intent = Intent(ctx, ReactionPickerActivity::class.java)
        ctx.startActivity(intent)
    }
}
