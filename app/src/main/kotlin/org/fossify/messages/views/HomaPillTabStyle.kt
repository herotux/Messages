package org.fossify.messages.views

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.widget.TextView

object HomaPillTabStyle {
    fun apply(view: TextView, selected: Boolean, color: Int) {
        view.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = view.height.coerceAtLeast(1) / 2f
            setColor(if (selected) color else Color.TRANSPARENT)
        }
        val r = Color.red(color) / 255f
        val g = Color.green(color) / 255f
        val b = Color.blue(color) / 255f
        val luminance = 0.2126f * r + 0.7152f * g + 0.0722f * b
        view.setTextColor(if (selected) {
            if (luminance > 0.55f) Color.BLACK else Color.WHITE
        } else color)
    }
}
