package org.fossify.messages.helpers

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.ProgressBar
import androidx.appcompat.widget.Toolbar

/**
 * Applies the app-owned visual theme to an Activity.
 *
 * ThemeManager is the single source of truth for the selected visual theme;
 * legacy Fossify baseConfig colors are intentionally not used here because
 * they can override the Material 3 semantic palette on every Activity resume.
 */
object AppThemeManager {
    private const val PREFS = "app_visual_theme"
    private const val KEY_BACKGROUND_IMAGE_URI = "background_image_uri"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getBackgroundImageUri(context: Context): Uri? =
        prefs(context).getString(KEY_BACKGROUND_IMAGE_URI, null).orEmpty()
            .takeIf { it.isNotEmpty() }?.let(Uri::parse)

    fun setBackgroundImageUri(context: Context, uri: Uri?) {
        prefs(context).edit().putString(KEY_BACKGROUND_IMAGE_URI, uri?.toString().orEmpty()).apply()
    }

    fun clearBackgroundImage(context: Context) = setBackgroundImageUri(context, null)

    fun apply(activity: Activity) {
        val theme = ThemeManager.themeForActivity(activity)
        val colors = theme.colors
        val primary = colors.primary
        val background = colors.background
        val accent = colors.accent
        val contrast = contrastColor(primary)

        activity.window.statusBarColor = darken(primary, 0.82f)
        activity.window.navigationBarColor = background

        var flags = activity.window.decorView.systemUiVisibility
        flags = if (contrast == Color.DKGRAY) flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        else flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            flags = if (isLight(background)) flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            else flags and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
        }
        activity.window.decorView.systemUiVisibility = flags

        val imageDrawable = getBackgroundImageUri(activity)?.let { uri ->
            try {
                activity.contentResolver.openInputStream(uri)?.use { input ->
                    BitmapFactory.decodeStream(input)?.let { bitmap ->
                        BitmapDrawable(activity.resources, bitmap).apply { gravity = Gravity.CENTER }
                    }
                }
            } catch (_: Exception) { null }
        }
        activity.window.decorView.background = imageDrawable ?: ColorDrawable(background)
        applyViewTheme(activity.window.decorView, primary, accent, contrast)
    }

    private fun applyViewTheme(view: View, primary: Int, accent: Int, contrast: Int) {
        if (view is Toolbar) {
            view.setBackgroundColor(primary)
            view.setTitleTextColor(contrast)
            view.setSubtitleTextColor(contrast)
            view.navigationIcon?.setTint(contrast)
            for (index in 0 until view.menu.size()) view.menu.getItem(index).icon?.setTint(contrast)
        }

        val tint = ColorStateList.valueOf(accent)
        when (view) {
            is Button -> view.backgroundTintList = tint
            is EditText -> view.backgroundTintList = tint
            is CompoundButton -> view.buttonTintList = tint
            is ProgressBar -> view.progressTintList = tint
        }

        if (view is ViewGroup) {
            for (index in 0 until view.childCount) applyViewTheme(view.getChildAt(index), primary, accent, contrast)
        }
    }

    private fun contrastColor(color: Int): Int {
        return if (isLight(color)) Color.DKGRAY else Color.WHITE
    }

    private fun isLight(color: Int): Boolean {
        val luminance = (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255.0
        return luminance > 0.58
    }

    private fun darken(color: Int, factor: Float): Int = Color.rgb(
        (Color.red(color) * factor).toInt().coerceIn(0, 255),
        (Color.green(color) * factor).toInt().coerceIn(0, 255),
        (Color.blue(color) * factor).toInt().coerceIn(0, 255)
    )
}
