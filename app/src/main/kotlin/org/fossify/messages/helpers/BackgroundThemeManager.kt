package org.fossify.messages.helpers

import android.app.Activity
import android.view.ViewGroup

/**
 * Backwards-compatible facade for the old background-only theme API.
 * New code should use ThemeManager directly.
 */
object BackgroundThemeManager {
    const val NONE = ThemeManager.DEFAULT_ID
    const val AURORA = ThemeManager.AURORA_ID
    const val OCEAN = ThemeManager.OCEAN_ID
    const val SUNSET = ThemeManager.SUNSET_ID
    const val FOREST = ThemeManager.FOREST_ID
    const val VIOLET = ThemeManager.VIOLET_ID
    const val MIDNIGHT = ThemeManager.MIDNIGHT_ID

    data class Theme(
        val id: String,
        val drawable: Int,
        val titleFa: String,
        val titleEn: String
    )

    val themes: List<Theme>
        get() = ThemeManager.builtInThemes.map {
            Theme(it.id, it.backgroundDrawable, it.nameFa, it.nameEn)
        }

    fun selectedId(activity: Activity): String = ThemeManager.selectedThemeId(activity)

    fun select(activity: Activity, id: String) {
        if (ThemeManager.selectBuiltIn(activity, id)) {
            apply(activity)
        }
    }

    fun apply(activity: Activity) {
        ThemeManager.applyBackground(activity)
    }

    fun applyToRoot(root: ViewGroup, themeId: String) {
        val theme = ThemeManager.findBuiltIn(themeId) ?: return
        if (theme.backgroundDrawable != 0) {
            root.setBackgroundResource(theme.backgroundDrawable)
        } else {
            root.background = null
        }
    }
}
