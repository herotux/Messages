package org.fossify.messages.helpers

import android.app.Activity
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import org.fossify.commons.extensions.baseConfig

/**
 * App-owned visual theme state. The selected app theme is intentionally independent
 * from the device light/dark setting.
 */
object AppThemeManager {
    private const val PREFS = "app_visual_theme"
    private const val KEY_BACKGROUND_IMAGE_URI = "background_image_uri"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getBackgroundImageUri(context: Context): Uri? {
        val value = prefs(context).getString(KEY_BACKGROUND_IMAGE_URI, null).orEmpty()
        return value.takeIf { it.isNotEmpty() }?.let(Uri::parse)
    }

    fun setBackgroundImageUri(context: Context, uri: Uri?) {
        prefs(context).edit().putString(KEY_BACKGROUND_IMAGE_URI, uri?.toString().orEmpty()).apply()
    }

    fun clearBackgroundImage(context: Context) {
        setBackgroundImageUri(context, null)
    }

    fun apply(activity: Activity) {
        val config = activity.baseConfig
        val primary = config.primaryColor
        val background = config.backgroundColor
        val contrast = contrastColor(primary)
        val status = darken(primary, 0.82f)

        activity.window.statusBarColor = status
        activity.window.navigationBarColor = background

        var flags = activity.window.decorView.systemUiVisibility
        flags = if (contrast == Color.DKGRAY) {
            flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        } else {
            flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            flags = if (contrast == Color.DKGRAY) {
                flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            } else {
                flags and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
            }
        }
        activity.window.decorView.systemUiVisibility = flags

        val imageUri = getBackgroundImageUri(activity)
        val backgroundDrawable = imageUri?.let { uri ->
            try {
                activity.contentResolver.openInputStream(uri)?.use { input ->
                    BitmapFactory.decodeStream(input)?.let { bitmap ->
                        BitmapDrawable(activity.resources, bitmap).apply {
                            gravity = Gravity.CENTER_CROP
                        }
                    }
                }
            } catch (_: Exception) {
                null
            }
        }
        activity.window.decorView.background = backgroundDrawable ?: ColorDrawable(background)

        applyToolbarColors(activity.window.decorView, primary, contrast)
    }

    private fun applyToolbarColors(view: View, primary: Int, contrast: Int) {
        if (view is Toolbar) {
            view.setBackgroundColor(primary)
            view.setTitleTextColor(contrast)
            view.setSubtitleTextColor(contrast)
            view.navigationIcon?.setTint(contrast)
            for (index in 0 until view.menu.size) {
                view.menu.getItem(index).icon?.setTint(contrast)
            }
        }

        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                applyToolbarColors(view.getChildAt(index), primary, contrast)
            }
        }
    }

    private fun contrastColor(color: Int): Int {
        val luminance = (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255.0
        return if (luminance > 0.58) Color.DKGRAY else Color.WHITE
    }

    private fun darken(color: Int, factor: Float): Int = Color.rgb(
        (Color.red(color) * factor).toInt().coerceIn(0, 255),
        (Color.green(color) * factor).toInt().coerceIn(0, 255),
        (Color.blue(color) * factor).toInt().coerceIn(0, 255)
    )
}
