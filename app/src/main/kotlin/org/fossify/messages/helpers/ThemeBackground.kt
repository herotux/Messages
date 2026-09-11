package org.fossify.messages.helpers

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.View
import androidx.core.content.ContextCompat

/** Creates and applies the drawable used for a theme's background. */
object ThemeBackground {
    fun apply(view: View, type: ThemeManager.BackgroundType, solidColor: Int, colors: List<Int>, angle: Int, wallpaperUri: String? = null) {
        when {
            type == ThemeManager.BackgroundType.WALLPAPER && !wallpaperUri.isNullOrBlank() -> {
                val drawable = loadWallpaper(view.context, wallpaperUri)
                if (drawable != null) {
                    view.background = drawable
                    return
                }
            }
            type == ThemeManager.BackgroundType.LINEAR_GRADIENT && colors.size >= 2 -> {
                view.background = GradientDrawable(orientation(angle), colors.toIntArray()).apply { cornerRadius = 0f }
                return
            }
        }
        view.setBackgroundColor(solidColor)
    }

    private fun loadWallpaper(context: Context, rawUri: String): Drawable? = runCatching {
        context.contentResolver.openInputStream(Uri.parse(rawUri))?.use { input ->
            Drawable.createFromStream(input, rawUri)
        }
    }.getOrNull()?.also { drawable ->
        drawable.setTintList(null)
        ContextCompat.getDrawable(context, android.R.color.transparent)
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
