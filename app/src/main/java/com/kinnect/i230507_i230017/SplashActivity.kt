package com.kinnect.i230507_i230017

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
        val token = RecentLoginStore.getToken(this)
        val target = when { token.isNullOrEmpty() -> LoginActivity::class.java; !RecentLoginStore.profileComplete(this) -> EditProfileActivity::class.java; else -> HomeActivity::class.java }
        val intent = Intent(this, target).apply {
            putExtra("firstTime", !RecentLoginStore.profileComplete(this@SplashActivity))
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
