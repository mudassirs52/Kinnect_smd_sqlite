package com.appVanta.i230017_i230507

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class EditProfileActivity : AppCompatActivity() {

    private var firstTime = false
    private var oldUsernameLower = ""
    private var profilePhotoUri = ""
    private var coverPhotoUri = ""

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
            val b64 = ImageUtils.uriToBase64(this, uri, ImageUtils.AVATAR)
            if (b64 != null) {
                profilePhotoUri = b64
                Photos.bindAvatar(tvInitials, etName.text.toString(), "local_user_123", b64, tint = false)
            } else {
                toast("Could not read image")
            }
        }
    }

    private val pickCoverLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val b64 = ImageUtils.uriToBase64(this, uri, ImageUtils.COVER)
            if (b64 != null) {
                coverPhotoUri = b64
                Photos.bindCover(findViewById(R.id.coverPhotoPreview), b64, R.drawable.bg_cover_sky)
            } else {
                toast("Could not read image")
            }
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
        val name = RecentLoginStore.getName(this) ?: "Test User"
        etName.setText(name)
        etEmail.setText(RecentLoginStore.getEmail(this) ?: "")
        Photos.bindAvatar(tvInitials, name, "local_user_123", profilePhotoUri, tint = false)
    }

    private fun save() {
        val name = etName.text.toString().trim()
        val username = etUsername.text.toString().trim().lowercase()

        if (name.isEmpty()) {
            etName.error = "Enter your name"
            return
        }

        btnSave.isEnabled = false
        RecentLoginStore.save(this, name, etEmail.text.toString().trim())

        if (firstTime) {
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        } else {
            toast("Profile saved")
            finish()
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}
