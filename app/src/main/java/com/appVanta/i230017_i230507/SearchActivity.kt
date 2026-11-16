package com.appVanta.i230017_i230507

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SearchActivity : AppCompatActivity() {

    private var me = "local_user_123"
    private val handler = Handler(Looper.getMainLooper())
    private var pending: Runnable? = null
    private var searchId = 0
    private var loading = false
    private var query = ""
    private var results = listOf<UserLite>()
    private val removers = mutableListOf<() -> Unit>()

    private lateinit var input: EditText
    private lateinit var clear: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)

        input = findViewById(R.id.search_input)
        clear = findViewById(R.id.search_clear)

        findViewById<ImageButton>(R.id.search_back)?.setOnClickListener { finish() }
        clear.setOnClickListener { input.setText("") }

        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                onQueryChanged(s?.toString()?.trim() ?: "")
            }
        })
        input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                saveRecent(query)
                true
            } else {
                false
            }
        }

        showRecent()
    }

    override fun onStart() {
        super.onStart()
        // Local fallback: no-op
    }

    override fun onStop() {
        super.onStop()
        removers.forEach { it() }
        removers.clear()
    }

    override fun onDestroy() {
        super.onDestroy()
        pending?.let { handler.removeCallbacks(it) }
    }

    private fun onQueryChanged(q: String) {
        query = q
        clear.visibility = if (q.isEmpty()) View.GONE else View.VISIBLE
        pending?.let { handler.removeCallbacks(it) }
        searchId++

        if (q.isEmpty()) {
            loading = false
            results = emptyList()
            findViewById<View>(R.id.sectionResults).visibility = View.GONE
            showRecent()
            return
        }

        findViewById<View>(R.id.sectionRecent).visibility = View.GONE
        findViewById<View>(R.id.sectionResults).visibility = View.VISIBLE
        loading = true
        renderResults()

        val r = Runnable { runSearch(q) }
        pending = r
        handler.postDelayed(r, 250)
    }

    private fun runSearch(q: String) {
        loading = false
        results = emptyList()
        renderResults()
    }

    private fun renderResults(errorText: String? = null) {
        if (query.isEmpty()) return

        val box = findViewById<LinearLayout>(R.id.resultsContainer)
        val status = findViewById<TextView>(R.id.tvSearchStatus)
        box.removeAllViews()

        if (results.isEmpty()) {
            status.visibility = View.VISIBLE
            status.text = when {
                errorText != null -> errorText
                loading -> "Searching..."
                else -> "No people found for \"$query\""
            }
            return
        }
        status.visibility = View.GONE
    }

    private fun prefs() = getSharedPreferences("recent_searches", MODE_PRIVATE)

    private fun recents(): List<String> =
        (prefs().getString("list_$me", "") ?: "").split("\n").filter { it.isNotEmpty() }

    private fun storeRecents(list: List<String>) {
        prefs().edit().putString("list_$me", list.joinToString("\n")).apply()
    }

    private fun saveRecent(q: String) {
        val t = q.trim()
        if (t.isEmpty()) return
        storeRecents((listOf(t) + recents().filter { !it.equals(t, ignoreCase = true) }).take(8))
    }

    private fun showRecent() {
        val box = findViewById<LinearLayout>(R.id.recentContainer) ?: return
        box.removeAllViews()
        val list = recents()
        findViewById<View>(R.id.sectionRecent).visibility = if (list.isEmpty()) View.GONE else View.VISIBLE

        for (item in list) {
            val row = layoutInflater.inflate(R.layout.item_recent_search, box, false)
            row.findViewById<TextView>(R.id.tvQuery).text = item
            row.setOnClickListener {
                input.setText(item)
                input.setSelection(item.length)
            }
            row.findViewById<View>(R.id.btnRemove).setOnClickListener {
                storeRecents(recents().filter { it != item })
                showRecent()
            }
            box.addView(row)
        }
    }
}
