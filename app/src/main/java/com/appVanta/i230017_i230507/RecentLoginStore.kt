package com.appVanta.i230017_i230507

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object RecentLoginStore {
    private const val PREFS = "recent_login_secure"

    private fun getPrefs(context: Context) = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    fun save(context: Context, name: String, email: String, token: String = "") {
        getPrefs(context).edit()
            .putString("name", name)
            .putString("email", email)
            .putString("token", token)
            .apply()
    }

    fun getName(context: Context): String? =
        getPrefs(context).getString("name", null)

    fun getEmail(context: Context): String? =
        getPrefs(context).getString("email", null)

    fun getToken(context: Context): String? =
        getPrefs(context).getString("token", null)

    fun clear(context: Context) {
        getPrefs(context).edit().clear().apply()
    }
}
