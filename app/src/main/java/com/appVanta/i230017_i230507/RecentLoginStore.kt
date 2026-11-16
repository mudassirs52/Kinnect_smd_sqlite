package com.appVanta.i230017_i230507

import android.content.Context

object RecentLoginStore {
    private const val PREFS = "recent_login"

    fun save(context: Context, name: String, email: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("name", name).putString("email", email).apply()
    }

    fun getName(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("name", null)

    fun getEmail(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("email", null)
}