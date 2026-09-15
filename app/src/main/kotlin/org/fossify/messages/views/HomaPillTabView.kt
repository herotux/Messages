package org.fossify.messages.views

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

/** A small, reusable pill tab presentation component. */
class HomaPillTabView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    init {
        gravity = android.view.Gravity.CENTER
        maxLines = 1
    }

    fun setPillStyle(selected: Boolean, color: Int) {
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = height.coerceAtLeast(1) / 2f
            setColor(if (selected) color else Color.TRANSPARENT)
        }
        setTextColor(if (selected) contrastColor(color) else color)
    }

    private fun contrastColor(color: Int): Int {
        val r = Color.red(color) / 255f
        val g = Color.green(color) / 255f
        val b = Color.blue(color) / 255f
        val luminance = 0.2126f * r + 0.7152f * g + 0.0722f * b
        return if (luminance > 0.55f) Color.BLACK else Color.WHITE
    }

    companion object {
        fun applyTo(view: android.widget.TextView, selected: Boolean, color: Int) {
            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = view.height.coerceAtLeast(1) / 2f
                setColor(if (selected) color else Color.TRANSPARENT)
            }
            view.background = drawable
            val r = Color.red(color) / 255f
            val g = Color.green(color) / 255f
            val b = Color.blue(color) / 255f
            val luminance = 0.2126f * r + 0.7152f * g + 0.0722f * b
            view.setTextColor(if (selected && luminance <= 0.55f || !selected) {
                if (selected) Color.WHITE else color
            } else Color.BLACK)
        }
    }
}
