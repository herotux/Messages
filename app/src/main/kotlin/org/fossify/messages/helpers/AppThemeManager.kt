package org.fossify.messages.helpers

import android.app.Activity
import android.content.Context
import android.net.Uri

/**
 * Backwards-compatible facade for legacy callers.
 *
 * ThemeManager owns theme state and ThemeApplier owns runtime UI application.
 * Keeping this facade avoids breaking older call sites while preventing a
 * second, competing theme engine from surviving in the app.
 */
object AppThemeManager {
    private const val PREFS = "messages_theme"
    private const val KEY_BACKGROUND_IMAGE_URI = "background_image_uri"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getBackgroundImageUri(context: Context): Uri? =
        prefs(context).getString(KEY_BACKGROUND_IMAGE_URI, null).orEmpty()
            .takeIf { it.isNotEmpty() }
            ?.let(Uri::parse)

    fun setBackgroundImageUri(context: Context, uri: Uri?) {
        prefs(context).edit().putString(KEY_BACKGROUND_IMAGE_URI, uri?.toString().orEmpty()).apply()
    }

    fun clearBackgroundImage(context: Context) = setBackgroundImageUri(context, null)

    /** Delegates all runtime visual application to the central ThemeApplier. */
    fun apply(activity: Activity) = ThemeApplier.apply(activity)
}
