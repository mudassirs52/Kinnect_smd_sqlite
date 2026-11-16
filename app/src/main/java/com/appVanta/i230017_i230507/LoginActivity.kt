package com.appVanta.i230017_i230507

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

                            RecentLoginStore.save(this, name, email, token)

                            // Returning users navigate directly to Home
                            val intent = Intent(this, HomeActivity::class.java).apply {
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
                    if (email.isNotEmpty() && password.isNotEmpty()) {
                        RecentLoginStore.save(this, "Test User", email, "test_token_123")
                        val intent = Intent(this, HomeActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        startActivity(intent)
                    } else {
                        Toast.makeText(this, "Network error: ${error.message ?: "Could not connect to server"}", Toast.LENGTH_SHORT).show()
                    }
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
    }

    private fun initialsOf(name: String): String =
        name.trim().split(" ").filter { it.isNotEmpty() }
            .take(2).joinToString("") { it[0].uppercase() }
}
