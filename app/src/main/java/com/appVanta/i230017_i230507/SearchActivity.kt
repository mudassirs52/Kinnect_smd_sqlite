package com.appVanta.i230017_i230507

import android.content.Intent
import android.net.Uri
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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class SearchActivity : AppCompatActivity() {

    private var me = "local_user_123"
    private val handler = Handler(Looper.getMainLooper())
    private var pending: Runnable? = null
    private var loading = false
    private var query = ""
    private var results = listOf<SearchUser>()

    data class SearchUser(
        val id: String,
        val name: String,
        val username: String,
        val profilePhoto: String,
        var friendState: String // NONE, SENT, RECEIVED, FRIENDS
    )

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

    override fun onDestroy() {
        super.onDestroy()
        pending?.let { handler.removeCallbacks(it) }
    }

    private fun onQueryChanged(q: String) {
        query = q
        clear.visibility = if (q.isEmpty()) View.GONE else View.VISIBLE
        pending?.let { handler.removeCallbacks(it) }

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
        val token = RecentLoginStore.getToken(this)
        if (token.isNullOrEmpty()) {
            loading = false
            renderResults("Offline / Not authenticated")
            return
        }

        val url = "${ApiClient.BASE_URL}search.php?q=${Uri.encode(q)}&filter=everyone"
        val request = object : StringRequest(Method.GET, url,
            Response.Listener { response ->
                loading = false
                try {
                    val json = JSONObject(response)
                    if (json.getString("status") == "success") {
                        val arr = json.getJSONArray("users")
                        val list = mutableListOf<SearchUser>()
                        for (i in 0 until arr.length()) {
                            val o = arr.getJSONObject(i)
                            list.add(
                                SearchUser(
                                    id = o.getString("id"),
                                    name = o.getString("name"),
                                    username = o.optString("username", ""),
                                    profilePhoto = o.optString("profile_photo", ""),
                                    friendState = o.optString("friend_state", "NONE")
                                )
                            )
                        }
                        results = list
                        renderResults()
                    } else {
                        renderResults(json.getString("message"))
                    }
                } catch (e: Exception) {
                    renderResults("Parsing error: ${e.message}")
                }
            },
            Response.ErrorListener { _ ->
                loading = false
                renderResults("Network error (Offline Mode)")
            }
        ) {
            override fun getHeaders(): Map<String, String> = mapOf("Authorization" to "Bearer $token")
        }
        Volley.newRequestQueue(this).add(request)
    }

    private fun renderResults(errorText: String? = null) {
        val box = findViewById<LinearLayout>(R.id.resultsContainer) ?: return
        val status = findViewById<TextView>(R.id.tvSearchStatus) ?: return
        box.removeAllViews()

        if (loading) {
            status.visibility = View.VISIBLE
            status.text = "Searching..."
            return
        }

        if (results.isEmpty()) {
            status.visibility = View.VISIBLE
            status.text = errorText ?: "No people found for \"$query\""
            return
        }

        status.visibility = View.GONE

        for (user in results) {
            val row = layoutInflater.inflate(R.layout.item_search_person, box, false)
            val avatar = row.findViewById<TextView>(R.id.tvAvatar)
            Photos.bindAvatar(avatar, user.name, user.id, user.profilePhoto)
            row.findViewById<TextView>(R.id.tvName).text = user.name
            row.findViewById<TextView>(R.id.tvSub).text = if (user.username.isNotEmpty()) "@${user.username}" else user.id

            val btnAdd = row.findViewById<ImageView>(R.id.btnAdd)
            val tvStatus = row.findViewById<TextView>(R.id.tvStatus)

            updateButtonState(btnAdd, tvStatus, user)

            btnAdd.setOnClickListener {
                onActionButtonClick(user, btnAdd, tvStatus)
            }

            row.setOnClickListener {
                startActivity(Intent(this, OtherProfileActivity::class.java).putExtra("uid", user.id))
            }
            box.addView(row)
        }
    }

    private fun updateButtonState(btnAdd: ImageView, tvStatus: TextView, user: SearchUser) {
        when (user.friendState) {
            "NONE" -> {
                btnAdd.visibility = View.VISIBLE
                tvStatus.visibility = View.GONE
                btnAdd.setImageResource(R.drawable.ic_person_add)
            }
            "SENT", "RECEIVED" -> {
                btnAdd.visibility = View.GONE
                tvStatus.visibility = View.VISIBLE
                tvStatus.text = "Requested"
            }
            "FRIENDS" -> {
                btnAdd.visibility = View.GONE
                tvStatus.visibility = View.VISIBLE
                tvStatus.text = "Friends"
            }
        }
    }

    private fun onActionButtonClick(user: SearchUser, btnAdd: ImageView, tvStatus: TextView) {
        when (user.friendState) {
            "NONE" -> {
                FriendManager.sendRequest(this, user.id) { ok ->
                    if (ok) {
                        user.friendState = "SENT"
                        updateButtonState(btnAdd, tvStatus, user)
                        toast("Friend request sent")
                    } else toast("Failed to send request")
                }
            }
            "SENT", "RECEIVED" -> {
                FriendManager.cancelRequest(this, user.id) { ok ->
                    if (ok) {
                        user.friendState = "NONE"
                        updateButtonState(btnAdd, tvStatus, user)
                        toast("Request canceled")
                    } else toast("Failed to cancel request")
                }
            }
            "FRIENDS" -> {
                AlertDialog.Builder(this)
                    .setTitle("Unfriend ${user.name}?")
                    .setPositiveButton("Unfriend") { _, _ ->
                        FriendManager.unfriend(this, user.id) { ok ->
                            if (ok) {
                                user.friendState = "NONE"
                                updateButtonState(btnAdd, tvStatus, user)
                                toast("Unfriended")
                            } else toast("Failed to unfriend")
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
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

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
