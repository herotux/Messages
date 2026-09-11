package org.fossify.messages.helpers

import android.graphics.drawable.GradientDrawable
import android.view.View

/** Creates the drawable used for a theme's background. */
object ThemeBackground {
    fun apply(view: View, type: ThemeManager.BackgroundType, solidColor: Int, colors: List<Int>, angle: Int) {
        if (type != ThemeManager.BackgroundType.LINEAR_GRADIENT || colors.size < 2) {
            view.setBackgroundColor(solidColor)
            return
        }
        val drawable = GradientDrawable(orientation(angle), colors.toIntArray()).apply {
            cornerRadius = 0f
        }
        view.background = drawable
    }

    private fun orientation(angle: Int): GradientDrawable.Orientation = when (((angle % 360) + 360) % 360) {
        0 -> GradientDrawable.Orientation.LEFT_RIGHT
        45 -> GradientDrawable.Orientation.BL_TR
        90 -> GradientDrawable.Orientation.BOTTOM_TOP
        135 -> GradientDrawable.Orientation.BR_TL
        180 -> GradientDrawable.Orientation.RIGHT_LEFT
        225 -> GradientDrawable.Orientation.TR_BL
        270 -> GradientDrawable.Orientation.TOP_BOTTOM
        else -> GradientDrawable.Orientation.TL_BR
    }
}
