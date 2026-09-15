package org.fossify.messages.views

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.widget.TextView
import kotlin.math.roundToInt

/**
 * Reusable Homa pill tab component.
 *
 * The visual state is centralized here so the presentation can be replaced later
 * without changing folder-tab behavior or callers.
 */
class HomaPillTabView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : TextView(context, attrs) {

    init {
        configureBase(this)
        setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    animate().scaleX(0.97f).scaleY(0.97f).setDuration(70).start()
                    false
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    animate().scaleX(1f).scaleY(1f).setDuration(110).start()
                    false
                }
                else -> false
            }
        }
    }

    fun setPillState(selected: Boolean, color: Int) {
        applyState(this, selected, color)
    }

    companion object {
        fun applyTo(view: TextView, selected: Boolean, color: Int) {
            configureBase(view)
            applyState(view, selected, color)
        }

        private fun configureBase(view: TextView) {
            view.gravity = Gravity.CENTER
            view.textSize = 14f
            view.isSingleLine = true
            view.minHeight = dp(view, 40)
            view.setPadding(dp(view, 16), 0, dp(view, 16), 0)
            view.includeFontPadding = false
        }

        private fun applyState(view: TextView, selected: Boolean, color: Int) {
            view.animate().cancel()
            view.alpha = if (selected) 1f else 0.82f
            view.typeface = if (selected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            view.background = if (selected) createSelectedBackground(view, color) else null
            if (!selected) view.setBackgroundColor(Color.TRANSPARENT)
            view.setTextColor(if (selected) contrastColor(color) else color)
        }

        private fun createSelectedBackground(view: TextView, color: Int): GradientDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(view, 22).toFloat()
            setColor(color)
        }

        private fun contrastColor(color: Int): Int {
            val luminance = (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255.0
            return if (luminance > 0.62) Color.BLACK else Color.WHITE
        }

        private fun dp(view: TextView, value: Int): Int = (value * view.resources.displayMetrics.density).roundToInt()
    }
}
