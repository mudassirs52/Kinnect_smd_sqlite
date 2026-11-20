package com.kinnect.i230507_i230017

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

    private var me = ""
    private val handler = Handler(Looper.getMainLooper())
    private var pending: Runnable? = null
    private var loading = false
    private var query = ""
    private var filter = "everyone"
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
        me = RecentLoginStore.getUserId(this)
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

        val root=findViewById<LinearLayout>(R.id.resultsContainer).parent as android.view.ViewGroup
        val filters=LinearLayout(this)
        for(label in listOf("Everyone","Friends","Requests"))filters.addView(TextView(this).apply{text=label;setTextColor(getColor(R.color.kn_teal));setPadding(24,20,24,20);setOnClickListener{filter=label.lowercase();runSearch(query)}})
        root.addView(filters,0)
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
        DataRepository.read("search",JSONObject().put("q",q).put("filter",filter)) {json ->
            if(q!=query)return@read
            loading=false
            val arr=json.optJSONArray("users")?:org.json.JSONArray()
            results=(0 until arr.length()).map {i->val u=arr.getJSONObject(i);SearchUser(u.getString("id"),u.optString("name"),u.optString("username"),u.optString("profile_photo"),u.optString("friend_state","NONE"))}
            renderResults()
        }
        loading=false
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
                btnAdd.visibility = View.VISIBLE
                tvStatus.visibility = View.VISIBLE
                tvStatus.text = if(user.friendState=="RECEIVED") "Confirm" else "Requested"
            }
            "FRIENDS" -> {
                btnAdd.visibility = View.VISIBLE
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
            "RECEIVED" -> {FriendManager.confirm(this,user.id){ok ->if(ok){user.friendState="FRIENDS";updateButtonState(btnAdd,tvStatus,user)}}}
            "SENT" -> {
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
