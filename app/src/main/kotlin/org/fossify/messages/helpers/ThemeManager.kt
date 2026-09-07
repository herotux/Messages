package org.fossify.messages.helpers

import android.app.Activity
import android.content.Context
import android.graphics.Color

/**
 * Central theme definition used by built-in, custom and (later) imported/community themes.
 *
 * Stage 1 intentionally keeps the existing background-theme UI compatible while introducing
 * a single model that can grow into the full theme engine without changing callers again.
 */
object ThemeManager {
    private const val PREFS = "messages_theme"
    private const val KEY_THEME_ID = "selected_theme_id"

    const val DEFAULT_ID = "none"
    const val AURORA_ID = "aurora"
    const val OCEAN_ID = "ocean"
    const val SUNSET_ID = "sunset"
    const val FOREST_ID = "forest"
    const val VIOLET_ID = "violet"
    const val MIDNIGHT_ID = "midnight"

    enum class ThemeSource {
        BUILT_IN,
        USER,
        IMPORTED,
        COMMUNITY
    }

    data class ThemeColors(
        val primary: Int = Color.TRANSPARENT,
        val accent: Int = Color.TRANSPARENT,
        val background: Int = Color.TRANSPARENT,
        val surface: Int = Color.TRANSPARENT,
        val textPrimary: Int = Color.TRANSPARENT,
        val textSecondary: Int = Color.TRANSPARENT,
        val incomingBubble: Int = Color.TRANSPARENT,
        val outgoingBubble: Int = Color.TRANSPARENT,
        val toolbar: Int = Color.TRANSPARENT,
        val tab: Int = Color.TRANSPARENT,
        val fab: Int = Color.TRANSPARENT
    )

    data class ThemeDefinition(
        val id: String,
        val nameFa: String,
        val nameEn: String,
        val source: ThemeSource = ThemeSource.BUILT_IN,
        val backgroundDrawable: Int = 0,
        val colors: ThemeColors = ThemeColors(),
        val version: Int = 1
    )

    /**
     * Existing presets are preserved exactly as selectable built-in themes.
     * Their color palette will be populated in the next phase.
     */
    val builtInThemes: List<ThemeDefinition> = listOf(
        ThemeDefinition(DEFAULT_ID, "بدون پس‌زمینه", "No background"),
        ThemeDefinition(AURORA_ID, "شفق قطبی", "Aurora", backgroundDrawable = org.fossify.messages.R.drawable.bg_theme_aurora),
        ThemeDefinition(OCEAN_ID, "اقیانوس", "Ocean", backgroundDrawable = org.fossify.messages.R.drawable.bg_theme_ocean),
        ThemeDefinition(SUNSET_ID, "غروب", "Sunset", backgroundDrawable = org.fossify.messages.R.drawable.bg_theme_sunset),
        ThemeDefinition(FOREST_ID, "جنگل", "Forest", backgroundDrawable = org.fossify.messages.R.drawable.bg_theme_forest),
        ThemeDefinition(VIOLET_ID, "بنفش", "Violet", backgroundDrawable = org.fossify.messages.R.drawable.bg_theme_violet),
        ThemeDefinition(MIDNIGHT_ID, "نیمه‌شب", "Midnight", backgroundDrawable = org.fossify.messages.R.drawable.bg_theme_midnight)
    )

    fun selectedThemeId(context: Context): String = context
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getString(KEY_THEME_ID, DEFAULT_ID)
        ?: DEFAULT_ID

    fun selectBuiltIn(context: Context, id: String): Boolean {
        if (builtInThemes.none { it.id == id }) return false
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME_ID, id)
            .apply()
        return true
    }

    fun findBuiltIn(id: String): ThemeDefinition? = builtInThemes.firstOrNull { it.id == id }

    fun applyBackground(activity: Activity) {
        val theme = findBuiltIn(selectedThemeId(activity)) ?: findBuiltIn(DEFAULT_ID) ?: return
        val content = activity.findViewById<android.view.ViewGroup>(android.R.id.content) ?: return
        val root = if (content.childCount == 1 && content.getChildAt(0) is android.view.ViewGroup) {
            content.getChildAt(0) as android.view.ViewGroup
        } else {
            content
        }

        if (theme.backgroundDrawable != 0) {
            root.setBackgroundResource(theme.backgroundDrawable)
        } else {
            root.background = null
        }
    }
}
