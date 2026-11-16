package com.appVanta.i230017_i230507

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    private var routed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        Handler(Looper.getMainLooper()).postDelayed({
            if (!isFinishing && !routed) {
                routed = true
                route()
            }
        }, 5000)
    }

    private fun route() {
        val email = RecentLoginStore.getEmail(this)
        val target = if (!email.isNullOrEmpty()) HomeActivity::class.java else LoginActivity::class.java
        val intent = Intent(this, target).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
