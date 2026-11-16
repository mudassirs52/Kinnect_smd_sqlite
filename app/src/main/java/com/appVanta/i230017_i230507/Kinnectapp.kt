package com.appVanta.i230017_i230507

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class KinnectApp : Application(), Application.ActivityLifecycleCallbacks {

    private var startedCount = 0
    private var stopListening: (() -> Unit)? = null
    private var lastShownCallId = ""
    private var askedNotificationPermission = false

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityStarted(activity: Activity) {
        startedCount++
        askNotificationPermission(activity)
    }

    override fun onActivityStopped(activity: Activity) {
        startedCount--
        if (startedCount <= 0) {
            startedCount = 0
            detach()
        }
    }

    private fun askNotificationPermission(activity: Activity) {
        if (askedNotificationPermission || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (activity !is HomeActivity) return
        askedNotificationPermission = true
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 2001)
        }
    }

    private fun detach() {
        stopListening?.invoke()
        stopListening = null
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}
