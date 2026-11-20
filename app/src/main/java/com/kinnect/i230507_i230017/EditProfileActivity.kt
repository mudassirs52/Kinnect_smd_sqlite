package com.kinnect.i230507_i230017

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class EditProfileActivity : AppCompatActivity() {

    private var firstTime = false
    private var profilePhotoUri: Uri? = null
    private var coverPhotoUri: Uri? = null

    private lateinit var etName: EditText
    private lateinit var etUsername: EditText
    private lateinit var etWebsite: EditText
    private lateinit var etBio: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var etGender: EditText
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button
    private lateinit var tvInitials: TextView

    private val pickProfileLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            profilePhotoUri = uri
            Photos.bindAvatar(tvInitials, etName.text.toString(), RecentLoginStore.getUserId(this), uri.toString(), tint = false)
            uploadImage(uri, "profile")
        }
    }

    private val pickCoverLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            coverPhotoUri = uri
            Photos.bindCover(findViewById(R.id.coverPhotoPreview), uri.toString(), R.drawable.bg_cover_sky)
            uploadImage(uri, "cover")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        firstTime = intent.getBooleanExtra("firstTime", false)

        etName = findViewById(R.id.edit_name)
        etUsername = findViewById(R.id.edit_username)
        etWebsite = findViewById(R.id.edit_website)
        etBio = findViewById(R.id.edit_bio)
        etEmail = findViewById(R.id.edit_email)
        etPhone = findViewById(R.id.edit_phone)
        etGender = findViewById(R.id.edit_gender)
        btnSave = findViewById(R.id.edit_profile_save)
        btnCancel = findViewById(R.id.edit_profile_back)
        tvInitials = findViewById(R.id.tvEditInitials)

        findViewById<View>(R.id.btnEditProfilePic)?.setOnClickListener {
            pickProfileLauncher.launch("image/*")
        }
        findViewById<View>(R.id.btnEditCover)?.setOnClickListener {
            pickCoverLauncher.launch("image/*")
        }

        if (firstTime) btnCancel.visibility = View.INVISIBLE

        btnCancel.setOnClickListener { finish() }
        btnSave.setOnClickListener { save() }

        loadProfile()
    }

    private fun loadProfile() {
        DataRepository.read("profile_get") {data ->
            val u=data.optJSONObject("user")?:return@read
            etName.setText(u.optString("name"));etUsername.setText(u.optString("username"));etBio.setText(u.optString("bio"));etEmail.setText(u.optString("email"))
            if (profilePhotoUri == null) Photos.bindAvatar(tvInitials,u.optString("name"),u.optString("id"),u.optString("profile_photo"),false)
            if (coverPhotoUri == null) Photos.bindCover(findViewById(R.id.coverPhotoPreview),u.optString("cover_photo"),R.drawable.bg_cover_sky)
        }
    }

    private fun uploadImage(uri: Uri, type: String) {
        DataRepository.mutate("profile_upload_image",JSONObject().put("type",type),listOf(uri)) {ok -> if(ok)toast("Photo saved for syncing")}
    }

    private fun save() {
        val name = etName.text.toString().trim()
        val username = etUsername.text.toString().trim().lowercase()
        val bio = etBio.text.toString().trim()

        if (name.isEmpty()) {
            etName.error = "Enter your name"
            return
        }

        if(!username.matches(Regex("[a-z0-9_]{3,40}"))){etUsername.error="Use 3-40 letters, digits or underscores";return}
        btnSave.isEnabled=false
        val params=JSONObject().put("name",name).put("username",username).put("bio",bio)
        if(firstTime) {
            DataRepository.io.execute {
                try {
                    val result=DataRepository.request("profile_update",params.put("action_uuid",java.util.UUID.randomUUID().toString()))
                    DataRepository.db.cache(RecentLoginStore.getUserId(this)+":profile_get:{}",result)
                    runOnUiThread {RecentLoginStore.save(this,name,etEmail.text.toString());RecentLoginStore.completeProfile(this)
                        startActivity(Intent(this,HomeActivity::class.java).apply{flags=Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK});finish()}
                }catch(e:Exception){runOnUiThread{toast(e.message?:"Connect to finish setting up your profile");btnSave.isEnabled=true}}
            }
        }else DataRepository.mutate("profile_update",params) {ok -> if(ok){RecentLoginStore.save(this,name,etEmail.text.toString());finish()}else btnSave.isEnabled=true}
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}
