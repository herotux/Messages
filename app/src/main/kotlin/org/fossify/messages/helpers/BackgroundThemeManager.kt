package org.fossify.messages.helpers

import android.app.Activity
import android.view.ViewGroup
import org.fossify.messages.R

object BackgroundThemeManager {
    private const val PREFS = "messages_background_theme"
    private const val KEY_THEME = "selected_theme"

    const val NONE = "none"
    const val AURORA = "aurora"
    const val OCEAN = "ocean"
    const val SUNSET = "sunset"
    const val FOREST = "forest"
    const val VIOLET = "violet"
    const val MIDNIGHT = "midnight"

    data class Theme(val id: String, val drawable: Int, val titleFa: String, val titleEn: String)

    val themes = listOf(
        Theme(NONE, 0, "بدون پس‌زمینه", "No background"),
        Theme(AURORA, R.drawable.bg_theme_aurora, "شفق قطبی", "Aurora"),
        Theme(OCEAN, R.drawable.bg_theme_ocean, "اقیانوس", "Ocean"),
        Theme(SUNSET, R.drawable.bg_theme_sunset, "غروب", "Sunset"),
        Theme(FOREST, R.drawable.bg_theme_forest, "جنگل", "Forest"),
        Theme(VIOLET, R.drawable.bg_theme_violet, "بنفش", "Violet"),
        Theme(MIDNIGHT, R.drawable.bg_theme_midnight, "نیمه‌شب", "Midnight")
    )

    fun selectedId(activity: Activity): String = activity.getSharedPreferences(PREFS, Activity.MODE_PRIVATE)
        .getString(KEY_THEME, NONE) ?: NONE

    fun select(activity: Activity, id: String) {
        if (themes.none { it.id == id }) return
        activity.getSharedPreferences(PREFS, Activity.MODE_PRIVATE).edit().putString(KEY_THEME, id).apply()
        apply(activity)
    }

    fun apply(activity: Activity) {
        val selected = selectedId(activity)
        if (selected == NONE) return
        val content = activity.findViewById<ViewGroup>(android.R.id.content) ?: return
        val root = if (content.childCount == 1 && content.getChildAt(0) is ViewGroup) content.getChildAt(0) as ViewGroup else content
        applyToRoot(root, selected)
    }

    fun applyToRoot(root: ViewGroup, themeId: String) {
        val theme = themes.firstOrNull { it.id == themeId } ?: return
        if (theme.drawable != 0) root.setBackgroundResource(theme.drawable)
    }
}
