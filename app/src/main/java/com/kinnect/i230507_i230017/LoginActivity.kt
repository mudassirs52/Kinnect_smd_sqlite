package com.kinnect.i230507_i230017

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class LoginActivity : AppCompatActivity() {
    private val localNetwork = LocalNetworkPermission(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)

        val recentName = RecentLoginStore.getName(this)
        val recentEmail = RecentLoginStore.getEmail(this)
        val card = findViewById<View>(R.id.cardRecent)
        val label = findViewById<View>(R.id.tvRecentLabel)
        if (recentName != null && recentEmail != null) {
            findViewById<TextView>(R.id.tvRecentName).text = recentName
            findViewById<TextView>(R.id.tvRecentInitials).text = initialsOf(recentName)
            card.setOnClickListener {
                etEmail.setText(recentEmail)
                etPassword.requestFocus()
            }
        } else {
            card.visibility = View.GONE
            label.visibility = View.GONE
        }

        findViewById<TextView>(R.id.btnCreateAccount).setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }

        btnLogin.setOnClickListener {
            if (!LocalNetworkPermission.granted(this)) {
                localNetwork.ensure { btnLogin.performClick() }
                return@setOnClickListener
            }
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            EspressoIdlingResource.increment()
            val url = "${ApiClient.BASE_URL}login.php"
            val queue = Volley.newRequestQueue(this)
            val request = object : StringRequest(Method.POST, url,
                Response.Listener { response ->
                    try {
                        val json = JSONObject(response)
                        if (json.getString("status") == "success") {
                            val token = json.getString("token")
                            val userObj = json.getJSONObject("user")
                            val name = "${userObj.getString("first_name")} ${userObj.getString("last_name")}"

                            RecentLoginStore.save(this, name, email, token, userObj.getString("id"), json.getLong("expires_at"), userObj.optBoolean("profile_complete"))
                            DataRepository.schedule()
                            PushRegistration.register(this)

                            // Returning users navigate directly to Home
                            val intent = Intent(this, if(userObj.optBoolean("profile_complete")) HomeActivity::class.java else EditProfileActivity::class.java).putExtra("firstTime",true).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                            startActivity(intent)
                        } else {
                            Toast.makeText(this, json.getString("message"), Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this, "Parsing error: ${e.message}", Toast.LENGTH_SHORT).show()
                    } finally {
                        EspressoIdlingResource.decrement()
                    }
                },
                Response.ErrorListener { error ->
                    val serverMessage = error.networkResponse?.data?.let { bytes ->
                        runCatching { JSONObject(String(bytes, Charsets.UTF_8)).optString("message") }.getOrNull()
                    }?.takeIf { it.isNotBlank() }
                    val message = serverMessage ?: if (!LocalNetworkPermission.granted(this))
                        "Allow Nearby devices access to connect to your laptop server."
                    else "Could not connect to the server. Keep Apache and MySQL running."
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                    EspressoIdlingResource.decrement()
                }
            ) {
                override fun getParams(): Map<String, String> {
                    return mapOf(
                        "email" to email,
                        "password" to password
                    )
                }
            }
            queue.add(request)
        }
        if (!LocalNetworkPermission.granted(this)) localNetwork.ensure()
    }

    private fun initialsOf(name: String): String =
        name.trim().split(" ").filter { it.isNotEmpty() }
            .take(2).joinToString("") { it[0].uppercase() }
}
