package com.kinnect.i230507_i230017

import android.Manifest
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class PhotoPickerActivity : AppCompatActivity() {

    private class Media(val uri: Uri, val video: Boolean, val durationMs: Long, val date: Long)

    private val all = ArrayList<Media>()
    private var shown: List<Media> = emptyList()
    private val selected = ArrayList<Uri>()
    private var multi = true
    private var filter = 0
    private var max = 10
    private lateinit var adapter: GalleryAdapter

    private val askPermission =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
            if (r.values.all { it }) loadMedia() else {
                Toast.makeText(this, "Gallery permission is needed", Toast.LENGTH_LONG).show()
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_picker)

        max = intent.getIntExtra("max", 10)

        findViewById<TextView>(R.id.tvCancelPhotoPicker).setOnClickListener { finish() }
        findViewById<TextView>(R.id.tvNextPhotoPicker).setOnClickListener {
            if (selected.isEmpty()) {
                Toast.makeText(this, "Select at least one photo", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            setResult(
                RESULT_OK,
                Intent().putStringArrayListExtra("uris", ArrayList(selected.map { it.toString() }))
            )
            finish()
        }
        findViewById<TextView>(R.id.btnSelectMultiple).setOnClickListener {
            multi = !multi
            (it as TextView).text = if (multi) "SELECT MULTIPLE" else "SELECT ONE"
            if (!multi && selected.size > 1) {
                val keep = selected.last()
                selected.clear(); selected.add(keep)
                adapter.notifyDataSetChanged(); updateNext()
            }
        }

        findViewById<View>(R.id.tabGallery).setOnClickListener { setFilter(0) }
        findViewById<View>(R.id.tabPhoto).setOnClickListener { setFilter(1) }
        findViewById<View>(R.id.tabVideo).setOnClickListener { setFilter(2) }

        adapter = GalleryAdapter()
        findViewById<RecyclerView>(R.id.rvGallery).apply {
            layoutManager = GridLayoutManager(this@PhotoPickerActivity, 4)
            adapter = this@PhotoPickerActivity.adapter
        }
        updateNext()

        val perms = if (Build.VERSION.SDK_INT >= 33)
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
        else arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)

        if (perms.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED })
            loadMedia() else askPermission.launch(perms)
    }

    private fun loadMedia() {
        Thread {
            val list = ArrayList<Media>()
            list += query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, false)
            list += query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true)
            list.sortByDescending { it.date }
            runOnUiThread {
                all.clear(); all.addAll(list)
                setFilter(0)
                if (all.isEmpty()) Toast.makeText(this, "No media found", Toast.LENGTH_SHORT).show()
            }
        }.start()
    }

    private fun query(uri: Uri, video: Boolean): List<Media> {
        val out = ArrayList<Media>()
        val proj = if (video)
            arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DATE_ADDED, MediaStore.Video.VideoColumns.DURATION)
        else arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DATE_ADDED)
        contentResolver.query(uri, proj, null, null, "${MediaStore.MediaColumns.DATE_ADDED} DESC")?.use { c ->
            var n = 0
            while (c.moveToNext() && n < 150) {
                val id = c.getLong(0)
                val date = c.getLong(1)
                val dur = if (video) c.getLong(2) else 0L
                out.add(Media(ContentUris.withAppendedId(uri, id), video, dur, date))
                n++
            }
        }
        return out
    }

    private fun setFilter(f: Int) {
        filter = f
        shown = when (f) {
            1 -> all.filter { !it.video }
            2 -> all.filter { it.video }
            else -> all.toList()
        }
        adapter.notifyDataSetChanged()

        val teal = ContextCompat.getColor(this, R.color.kn_teal)
        val grey = ContextCompat.getColor(this, R.color.kn_text2)
        val tabs = listOf(
            Triple(R.id.indGallery, R.id.txtGallery, 0),
            Triple(R.id.indPhoto, R.id.txtPhoto, 1),
            Triple(R.id.indVideo, R.id.txtVideo, 2)
        )
        for ((ind, txt, i) in tabs) {
            findViewById<View>(ind).visibility = if (i == f) View.VISIBLE else View.INVISIBLE
            findViewById<TextView>(txt).setTextColor(if (i == f) teal else grey)
        }
    }

    private fun updateNext() {
        findViewById<TextView>(R.id.tvNextPhotoPicker).text = "Next (${selected.size})"
    }

    private fun onTap(m: Media) {
        if (m.video) {
            Toast.makeText(this, "Videos can't be added to posts", Toast.LENGTH_SHORT).show()
            return
        }
        findViewById<ImageView>(R.id.previewImage).setImageURI(m.uri)
        if (multi) {
            if (selected.contains(m.uri)) selected.remove(m.uri)
            else if (selected.size < max) selected.add(m.uri)
            else Toast.makeText(this, "Up to $max photos", Toast.LENGTH_SHORT).show()
        } else {
            selected.clear(); selected.add(m.uri)
        }
        adapter.notifyDataSetChanged()
        updateNext()
    }

    private inner class GalleryAdapter : RecyclerView.Adapter<GalleryAdapter.VH>() {

        inner class VH(val root: FrameLayout, val iv: ImageView, val badge: TextView, val dur: TextView) :
            RecyclerView.ViewHolder(root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val dp = resources.displayMetrics.density
            val size = resources.displayMetrics.widthPixels / 4
            val root = FrameLayout(this@PhotoPickerActivity).apply {
                layoutParams = RecyclerView.LayoutParams(size, size)
                setPadding(2, 2, 2, 2)
            }
            val iv = ImageView(this@PhotoPickerActivity).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
            root.addView(iv, FrameLayout.LayoutParams(-1, -1))

            val badge = TextView(this@PhotoPickerActivity).apply {
                setBackgroundResource(R.drawable.kn_circle)
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                textSize = 12f
                setTypeface(typeface, Typeface.BOLD)
            }
            root.addView(
                badge,
                FrameLayout.LayoutParams((22 * dp).toInt(), (22 * dp).toInt(), Gravity.TOP or Gravity.END)
                    .apply { setMargins(0, (6 * dp).toInt(), (6 * dp).toInt(), 0) }
            )

            val dur = TextView(this@PhotoPickerActivity).apply {
                setTextColor(Color.WHITE)
                textSize = 12f
                setTypeface(typeface, Typeface.BOLD)
                setShadowLayer(4f, 0f, 0f, Color.BLACK)
            }
            root.addView(
                dur,
                FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM or Gravity.END)
                    .apply { setMargins(0, 0, (6 * dp).toInt(), (6 * dp).toInt()) }
            )
            return VH(root, iv, badge, dur)
        }

        override fun getItemCount() = shown.size

        override fun onBindViewHolder(h: VH, position: Int) {
            val m = shown[position]
            h.iv.setImageURI(m.uri)

            val idx = selected.indexOf(m.uri)
            if (idx >= 0) {
                h.badge.text = (idx + 1).toString()
                h.badge.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(this@PhotoPickerActivity, R.color.kn_teal))
            } else {
                h.badge.text = ""
                h.badge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#66FFFFFF"))
            }

            if (m.video) {
                val s = m.durationMs / 1000
                h.dur.visibility = View.VISIBLE
                h.dur.text = "%d:%02d".format(s / 60, s % 60)
            } else h.dur.visibility = View.GONE

            h.root.setOnClickListener { onTap(m) }
        }
    }
}
