package org.fossify.messages.views

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.TextView
import kotlin.math.roundToInt

/**
 * Reusable Homa tab component.
 *
 * The selected state is intentionally self-contained so the tab presentation can
 * be replaced later without changing the folder-tab behavior or callers.
 */
class HomaPillTabView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : TextView(context, attrs) {

    private var selectedTab = false
    private var accentColor = Color.TRANSPARENT

    init {
        gravity = Gravity.CENTER
        textSize = 14f
        isSingleLine = true
        minHeight = dp(40)
        setPadding(dp(16), 0, dp(16), 0)
        includeFontPadding = false
        setBackgroundColor(Color.TRANSPARENT)
        setOnTouchListener { _, event ->
            when (event.actionMasked) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    animate().scaleX(0.97f).scaleY(0.97f).setDuration(70).start()
                    false
                }
                android.view.MotionEvent.ACTION_UP,
                android.view.MotionEvent.ACTION_CANCEL -> {
                    animate().scaleX(1f).scaleY(1f).setDuration(110).start()
                    false
                }
                else -> false
            }
        }
    }

    fun setPillState(selected: Boolean, color: Int) {
        selectedTab = selected
        accentColor = color
        alpha = if (selected) 1f else 0.82f
        typeface = if (selected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        background = if (selected) createSelectedBackground(color) else null
        if (!selected) setBackgroundColor(Color.TRANSPARENT)
        setTextColor(if (selected) contrastColor(color) else color)
    }

    fun isPillSelected(): Boolean = selectedTab

    private fun createSelectedBackground(color: Int): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dp(22).toFloat()
        setColor(color)
    }

    private fun contrastColor(color: Int): Int {
        val luminance = (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255.0
        return if (luminance > 0.62) Color.BLACK else Color.WHITE
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()
}
