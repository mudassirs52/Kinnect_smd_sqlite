package com.appVanta.i230017_i230507

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
            Photos.bindAvatar(tvInitials, etName.text.toString(), "local_user_123", "", tint = false)
            uploadImage(uri, "profile")
        }
    }

    private val pickCoverLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            coverPhotoUri = uri
            Photos.bindCover(findViewById(R.id.coverPhotoPreview), "", R.drawable.bg_cover_sky)
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
        val token = RecentLoginStore.getToken(this) ?: return
        val url = "${ApiClient.BASE_URL}profile_get.php"

        val request = object : StringRequest(Method.GET, url,
            Response.Listener { response ->
                try {
                    val json = JSONObject(response)
                    if (json.getString("status") == "success") {
                        val u = json.getJSONObject("user")
                        etName.setText(u.getString("name"))
                        etUsername.setText(u.optString("username", ""))
                        etBio.setText(u.optString("bio", ""))
                        etEmail.setText(u.optString("email", ""))
                        val name = u.getString("name")
                        Photos.bindAvatar(tvInitials, name, "local_user_123", u.optString("profile_photo", ""), tint = false)
                    }
                } catch (_: Exception) {}
            },
            Response.ErrorListener {}
        ) {
            override fun getHeaders(): Map<String, String> = mapOf("Authorization" to "Bearer $token")
        }
        Volley.newRequestQueue(this).add(request)
    }

    private fun uploadImage(uri: Uri, type: String) {
        val inputStream = contentResolver.openInputStream(uri)
        val bytes = inputStream?.readBytes()
        if (bytes == null) {
            toast("Could not read image")
            return
        }

        val url = "${ApiClient.BASE_URL}profile_upload_image.php"
        val token = RecentLoginStore.getToken(this) ?: return
        val fileName = "${type}_${System.currentTimeMillis()}.jpg"

        val params = mapOf("type" to type)
        val byteData = mapOf("file" to VolleyMultipartRequest.DataPart(fileName, bytes, "image/jpeg"))

        val request = VolleyMultipartRequest(
            Request.Method.POST,
            url,
            mapOf("Authorization" to "Bearer $token"),
            params,
            byteData,
            Response.Listener { response ->
                try {
                    val json = JSONObject(String(response.data))
                    if (json.getString("status") == "success") {
                        toast("${type.replaceFirstChar { it.uppercase() }} photo updated")
                    } else {
                        toast(json.getString("message"))
                    }
                } catch (e: Exception) {
                    toast("Upload error: ${e.message}")
                }
            },
            Response.ErrorListener { error ->
                toast("Network error: ${error.message}")
            }
        )
        Volley.newRequestQueue(this).add(request)
        toast("Uploading $type photo...")
    }

    private fun save() {
        val name = etName.text.toString().trim()
        val username = etUsername.text.toString().trim().lowercase()
        val bio = etBio.text.toString().trim()

        if (name.isEmpty()) {
            etName.error = "Enter your name"
            return
        }

        btnSave.isEnabled = false
        val token = RecentLoginStore.getToken(this)
        if (token.isNullOrEmpty()) {
            RecentLoginStore.save(this, name, etEmail.text.toString().trim())
            finish()
            return
        }

        val url = "${ApiClient.BASE_URL}profile_update.php"
        val request = object : StringRequest(Method.POST, url,
            Response.Listener { response ->
                try {
                    val json = JSONObject(response)
                    if (json.getString("status") == "success") {
                        RecentLoginStore.save(this, name, etEmail.text.toString().trim())
                        if (firstTime) {
                            val intent = Intent(this, HomeActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                            startActivity(intent)
                        } else {
                            toast("Profile updated successfully")
                            finish()
                        }
                    } else {
                        toast(json.getString("message"))
                        btnSave.isEnabled = true
                    }
                } catch (e: Exception) {
                    toast("Error: ${e.message}")
                    btnSave.isEnabled = true
                }
            },
            Response.ErrorListener { error ->
                toast("Network error: ${error.message}")
                btnSave.isEnabled = true
            }
        ) {
            override fun getHeaders(): Map<String, String> = mapOf("Authorization" to "Bearer $token")
            override fun getParams(): Map<String, String> = mapOf(
                "name" to name,
                "username" to username,
                "bio" to bio
            )
        }
        Volley.newRequestQueue(this).add(request)
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}
