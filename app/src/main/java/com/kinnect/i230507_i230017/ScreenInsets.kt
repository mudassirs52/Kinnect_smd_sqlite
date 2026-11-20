package com.kinnect.i230507_i230017

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.WeakHashMap

/** Keep the original screen spacing on Android versions that enforce edge-to-edge. */
object ScreenInsets {
    private val installed = WeakHashMap<View, Boolean>()

    fun install(activity: Activity) {
        if (Build.VERSION.SDK_INT < 35) return
        // These screens already manage their own system-bar/keyboard insets.
        if (activity is ChatActivity || activity is MainActivity) return
        val content = activity.findViewById<ViewGroup>(android.R.id.content) ?: return
        val root = content.getChildAt(0) ?: return
        if (root.fitsSystemWindows || installed.containsKey(root)) return
        installed[root] = true
        val left = root.paddingLeft
        val top = root.paddingTop
        val right = root.paddingRight
        val bottom = root.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.setPadding(left + bars.left, top + bars.top, right + bars.right, bottom + bars.bottom)
            WindowInsetsCompat.CONSUMED
        }
        ViewCompat.requestApplyInsets(root)
    }
}
