package com.kinnect.i230507_i230017

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

/** Android 17 protects LAN connections separately from internet access. */
class LocalNetworkPermission(private val activity: ComponentActivity) {
    private var onGranted: (() -> Unit)? = null
    private val launcher = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val callback = onGranted
        onGranted = null
        if (granted) {
            DataRepository.schedule()
            callback?.invoke()
        } else {
            Toast.makeText(activity,
                "Allow Nearby devices access to connect to your laptop server.",
                Toast.LENGTH_LONG).show()
        }
    }

    fun ensure(ready: () -> Unit = {}) {
        if (granted(activity)) ready()
        else {
            onGranted = ready
            launcher.launch(PERMISSION)
        }
    }

    companion object {
        private const val PERMISSION = "android.permission.ACCESS_LOCAL_NETWORK"

        fun granted(context: Context): Boolean =
            !required() || ContextCompat.checkSelfPermission(context, PERMISSION) ==
                PackageManager.PERMISSION_GRANTED

        private fun required(): Boolean {
            if (Build.VERSION.SDK_INT < 37) return false
            val host = Uri.parse(ApiClient.BASE_URL).host?.lowercase() ?: return false
            val octets = host.split('.').map { it.toIntOrNull() }
            val privateIpv4 = octets.size == 4 && (
                octets[0] == 10 || octets[0] == 127 ||
                (octets[0] == 192 && octets[1] == 168) ||
                (octets[0] == 172 && (octets[1] ?: -1) in 16..31)
            )
            return privateIpv4 || host == "localhost" || host.endsWith(".local") ||
                (!host.contains('.') && !host.contains(':')) || host == "::1" ||
                host.startsWith("fe80:") ||
                (host.contains(':') && (host.startsWith("fc") || host.startsWith("fd")))
        }
    }
}
