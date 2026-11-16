package com.appVanta.i230017_i230507

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

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
            RecentLoginStore.save(this, email.substringBefore("@").replaceFirstChar { it.uppercase() }, email)
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    private fun initialsOf(name: String): String =
        name.trim().split(" ").filter { it.isNotEmpty() }
            .take(2).joinToString("") { it[0].uppercase() }
}
