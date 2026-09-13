package org.fossify.messages.helpers

import android.app.Activity
import android.content.Context
import android.net.Uri

/**
 * Backwards-compatible facade for legacy callers.
 *
 * ThemeManager owns persisted theme state and ThemeApplier owns runtime UI
 * application. This class intentionally contains no independent theme state.
 */
object AppThemeManager {
    fun getBackgroundImageUri(context: Context): Uri? = ThemeManager.getBackgroundImageUri(context)

    fun setBackgroundImageUri(context: Context, uri: Uri?) = ThemeManager.setBackgroundImageUri(context, uri)

    fun clearBackgroundImage(context: Context) = ThemeManager.clearBackgroundImage(context)

    fun apply(activity: Activity) = ThemeApplier.apply(activity)
}
