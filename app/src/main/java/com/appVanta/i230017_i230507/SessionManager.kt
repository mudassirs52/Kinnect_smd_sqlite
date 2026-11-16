package com.appVanta.i230017_i230507

import android.content.Context
import android.content.Intent

object SessionManager {

    fun goOnline() {
        // Local fallback: no-op
    }

    fun logout(context: Context) {
        performLogout(context)
    }

    private fun performLogout(context: Context) {
        val intent = Intent(context, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        context.startActivity(intent)
    }
}
