package com.kinnect.i230507_i230017

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView

/** The compact, anchored reaction bar from screen 05 of the UI reference. */
object ReactionPicker {
    fun show(anchor: View, current: String, selected: (String) -> Unit) {
        val context = anchor.context
        val density = context.resources.displayMetrics.density
        fun dp(value: Int) = (value * density).toInt()
        fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
            setColor(color); cornerRadius = dp(radius).toFloat()
        }
        val width = minOf(dp(336), context.resources.displayMetrics.widthPixels - dp(24))
        val height = dp(104)
        val root = FrameLayout(context)
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = rounded(Color.WHITE, 32)
            elevation = dp(6).toFloat()
            setPadding(dp(6), 0, dp(6), 0)
            clipChildren = false
            clipToPadding = false
        }
        root.clipChildren = false
        root.addView(row, FrameLayout.LayoutParams(-1, dp(56), Gravity.BOTTOM).apply {
            bottomMargin = dp(6); marginStart = dp(6); marginEnd = dp(6)
        })
        val popup = PopupWindow(root, width, height, true).apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            isOutsideTouchable = true
            inputMethodMode = PopupWindow.INPUT_METHOD_NOT_NEEDED
        }
        val icons = listOf(R.drawable.ic_thumbsup, R.drawable.ic_heart, R.drawable.ic_haha,
            R.drawable.ic_wow, R.drawable.ic_sad, R.drawable.ic_angry)
        val colors = listOf("#0B5F63", "#C52F50", "#C98B12", "#C98B12", "#C98B12", "#B44329")
        val images = ArrayList<ImageView>()
        val labels = ArrayList<TextView>()
        fun preview(index: Int) {
            images.forEachIndexed { i, image ->
                image.scaleX = if (i == index) 1.3f else 1f
                image.scaleY = if (i == index) 1.3f else 1f
                image.translationY = if (i == index) -dp(10).toFloat() else 0f
                labels[i].visibility = if (i == index) View.VISIBLE else View.INVISIBLE
            }
        }
        Reactions.keys.forEachIndexed { index, kind ->
            val cell = FrameLayout(context).apply {
                clipChildren = false
                isClickable = true
                isFocusable = true
                contentDescription = Reactions.label(kind)
            }
            val image = ImageView(context).apply {
                setImageResource(icons[index])
                if (index < 2) {
                    background = rounded(Color.parseColor(colors[index]), 24)
                    imageTintList = ColorStateList.valueOf(Color.WHITE)
                    setPadding(dp(8), dp(8), dp(8), dp(8))
                } else imageTintList = ColorStateList.valueOf(Color.parseColor(colors[index]))
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }
            cell.addView(image, FrameLayout.LayoutParams(dp(40), dp(40), Gravity.CENTER))
            images.add(image)
            val label = TextView(context).apply {
                text = Reactions.label(kind)
                textSize = 10f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                background = rounded(Color.parseColor("#222222"), 10)
                setPadding(dp(5), dp(2), dp(5), dp(2))
                visibility = View.INVISIBLE
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }
            labels.add(label)
            val cellWidth = (width - dp(24)) / 6
            root.addView(label, FrameLayout.LayoutParams(cellWidth, dp(22)).apply {
                leftMargin = dp(12) + index * cellWidth
                topMargin = dp(4)
            })
            row.addView(cell, LinearLayout.LayoutParams(0, -1, 1f))
            cell.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) preview(index)
                false
            }
            cell.setOnClickListener { popup.dismiss(); selected(kind) }
        }
        preview(Reactions.keys.indexOf(current))
        val location = IntArray(2)
        anchor.getLocationOnScreen(location)
        val visible = android.graphics.Rect()
        anchor.getWindowVisibleDisplayFrame(visible)
        val x = (location[0] - dp(6)).coerceIn(visible.left + dp(6), maxOf(visible.left + dp(6), visible.right - width - dp(6)))
        val y = maxOf(visible.top, location[1] - height + dp(6))
        popup.showAtLocation(anchor.rootView, Gravity.NO_GRAVITY, x, y)
        // Keep the post visible behind the picker, with the reference's dimmed backdrop.
        val container = root.rootView
        val params = container?.layoutParams as? WindowManager.LayoutParams
        if (container != null && params != null) {
            params.flags = params.flags or WindowManager.LayoutParams.FLAG_DIM_BEHIND
            params.dimAmount = 0.35f
            (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).updateViewLayout(container, params)
        }
    }
}

