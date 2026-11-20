package com.kinnect.i230507_i230017

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class FriendsActivity : AppCompatActivity() {

    private var me = ""
    private var users = mapOf<String, UserLite>()
    private var allFriends = mapOf<String, Set<String>>()
    private var incoming = mapOf<String, Long>()
    private var sent = setOf<String>()
    private val refreshListener:()->Unit={loadFriends()}
    private val removers = mutableListOf<() -> Unit>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        me = RecentLoginStore.getUserId(this)
        setContentView(R.layout.activity_friends)

        wireNavigation()

        findViewById<TextView>(R.id.chipSuggestions).setOnClickListener { showTab(false) }
        findViewById<TextView>(R.id.chipFriends).setOnClickListener { showTab(true) }
        showTab(false)
    }

    override fun onStart() {
        super.onStart()
        UiUtils.bindBadges(this, removers)
        DataRepository.listeners.add(refreshListener)
        loadFriends()
    }

    override fun onStop() {
        super.onStop()
        DataRepository.listeners.remove(refreshListener)
        removers.forEach { it() }
        removers.clear()
    }

    private fun wireNavigation() {
        findViewById<ImageView>(R.id.btnSearch)?.setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }
        findViewById<ImageView>(R.id.btnMessenger)?.setOnClickListener {
            startActivity(Intent(this, ChatsActivity::class.java))
        }
        findViewById<ImageView>(R.id.tabHome)?.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
        }
        findViewById<ImageView>(R.id.tabMarket)?.setOnClickListener {
            startActivity(Intent(this, MarketplaceActivity::class.java))
        }
        findViewById<ImageView>(R.id.tabNotifications)?.setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }
        findViewById<ImageView>(R.id.tabMenu)?.setOnClickListener {
            startActivity(Intent(this, MenuActivity::class.java))
        }
    }

    private fun showTab(friends: Boolean) {
        findViewById<View>(R.id.sectionSuggestions).visibility = if (friends) View.GONE else View.VISIBLE
        findViewById<View>(R.id.sectionFriends).visibility = if (friends) View.VISIBLE else View.GONE
        val on = getColor(R.color.kn_teal)
        val off = getColor(R.color.kn_text)
        findViewById<TextView>(R.id.chipSuggestions).setTextColor(if (friends) off else on)
        findViewById<TextView>(R.id.chipFriends).setTextColor(if (friends) on else off)
    }

    private fun render() {
        val mine = allFriends[me] ?: emptySet()
        renderRequests(mine)
        renderSuggestions(mine)
        renderFriends(mine)
    }

    private fun loadFriends() {
        DataRepository.read("friends_get") {data ->
            renderPeople(R.id.requestsContainer,data.optJSONArray("requests")?:org.json.JSONArray(),true)
            renderPeople(R.id.friendsContainer,data.optJSONArray("friends")?:org.json.JSONArray(),false)
            findViewById<TextView>(R.id.tvRequestCount).text="Friend requests (${data.optJSONArray("requests")?.length()?:0})"
        }
        DataRepository.read("search",org.json.JSONObject().put("q","").put("filter","everyone")) {data ->
            val all=data.optJSONArray("users")?:org.json.JSONArray();val suggestions=org.json.JSONArray()
            for(i in 0 until all.length())if(all.getJSONObject(i).optString("friend_state")=="NONE")suggestions.put(all.getJSONObject(i))
            renderPeople(R.id.suggestionsContainer,suggestions,false)
        }
    }
    private fun renderPeople(containerId: Int,people: org.json.JSONArray,requests: Boolean) {
        val box=findViewById<LinearLayout>(containerId);box.removeAllViews()
        for(i in 0 until people.length()) {
            val u=people.getJSONObject(i);val id=u.getString("id")
            val row=layoutInflater.inflate(if(requests)R.layout.item_friend_request else R.layout.item_suggestion,box,false)
            row.findViewById<TextView>(R.id.tvName).text=u.optString("name")
            Photos.bindAvatar(row.findViewById(R.id.tvAvatar),u.optString("name"),id,u.optString("profile_photo"))
            row.setOnClickListener{startActivity(Intent(this,OtherProfileActivity::class.java).putExtra("uid",id))}
            if(requests){
                row.findViewById<TextView>(R.id.btnConfirm).setOnClickListener{FriendManager.confirm(this,id){loadFriends()}}
                row.findViewById<TextView>(R.id.btnDelete).setOnClickListener{FriendManager.cancelRequest(this,id){loadFriends()}}
            }else{
                row.findViewById<TextView>(R.id.tvSub).text="@"+u.optString("username")
                val btn=row.findViewById<TextView>(R.id.btnAction)
                val state=u.optString("friend_state")
                btn.text=when(state){"FRIENDS"->"Message";"SENT"->"Requested";else->"Add friend"}
                UiUtils.stylePill(btn,state!="SENT")
                btn.setOnClickListener{when(state){
                    "FRIENDS"->startActivity(Intent(this,ChatActivity::class.java).putExtra("uid",id))
                    "SENT"->FriendManager.cancelRequest(this,id){loadFriends()}
                    else->FriendManager.sendRequest(this,id){loadFriends()}
                }}
                if(state=="FRIENDS")row.setOnLongClickListener{DataRepository.mutate("close_friend",org.json.JSONObject().put("other_id",id));Toast.makeText(this,"Added to Close friends",Toast.LENGTH_SHORT).show();true}
            };box.addView(row)
        }
    }
    private fun renderRequests(mine: Set<String>){}
    private fun renderSuggestions(mine: Set<String>){}
    private fun renderFriends(mine: Set<String>){}
}
