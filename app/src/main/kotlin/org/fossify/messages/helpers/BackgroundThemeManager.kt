package org.fossify.messages.helpers

import android.app.Activity
import android.content.Intent
import android.view.ViewGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.fossify.messages.R
import org.fossify.messages.activities.HerotuxAboutActivity

/**
 * Backwards-compatible facade for the old background-only theme API.
 * It now also exposes user themes to the existing Appearance screen.
 */
object BackgroundThemeManager {
    const val NONE = ThemeManager.DEFAULT_ID
    const val AURORA = ThemeManager.AURORA_ID
    const val OCEAN = ThemeManager.OCEAN_ID
    const val SUNSET = ThemeManager.SUNSET_ID
    const val FOREST = ThemeManager.FOREST_ID
    const val VIOLET = ThemeManager.VIOLET_ID
    const val MIDNIGHT = ThemeManager.MIDNIGHT_ID

    private const val CREATE_USER_THEME = "__create_user_theme__"
    private const val USER_PREFIX = "user_"
    private const val EXTRA_THEME_BUILDER = "theme_builder"
    private const val EXTRA_THEME_ID = "theme_id"

    data class Theme(
        val id: String,
        val drawable: Int,
        val titleFa: String,
        val titleEn: String
    )

    val themes: List<Theme>
        get() = ThemeManager.builtInThemes.map {
            Theme(it.id, it.backgroundDrawable, it.nameFa, it.nameEn)
        } + Theme(
            CREATE_USER_THEME,
            R.drawable.bg_theme_custom_placeholder,
            "＋ ساخت تم جدید",
            "+ Create new theme"
        ) + ThemeStoragePreview.themes()

    fun selectedId(activity: Activity): String = ThemeManager.selectedThemeId(activity)

    fun select(activity: Activity, id: String) {
        when {
            id == CREATE_USER_THEME -> openBuilder(activity, null)
            id.startsWith(USER_PREFIX) -> showUserThemeActions(activity, id)
            ThemeManager.selectBuiltIn(activity, id) -> apply(activity)
        }
    }

    private fun showUserThemeActions(activity: Activity, id: String) {
        val theme = ThemeManager.find(activity, id) ?: return
        MaterialAlertDialogBuilder(activity)
            .setTitle(theme.nameFa)
            .setItems(arrayOf("اعمال تم", "ویرایش", "حذف")) { dialog, which ->
                when (which) {
                    0 -> {
                        ThemeManager.select(activity, id)
                        apply(activity)
                    }
                    1 -> openBuilder(activity, id)
                    2 -> confirmDelete(activity, id, theme.nameFa)
                }
                dialog.dismiss()
            }
            .show()
    }

    private fun confirmDelete(activity: Activity, id: String, name: String) {
        MaterialAlertDialogBuilder(activity)
            .setTitle("حذف تم")
            .setMessage("تم «$name» حذف شود؟")
            .setNegativeButton("لغو", null)
            .setPositiveButton("حذف") { _, _ ->
                ThemeManager.deleteUserTheme(activity, id)
                apply(activity)
            }
            .show()
    }

    private fun openBuilder(activity: Activity, id: String?) {
        activity.startActivity(Intent(activity, HerotuxAboutActivity::class.java).apply {
            putExtra(EXTRA_THEME_BUILDER, true)
            if (id != null) putExtra(EXTRA_THEME_ID, id)
        })
    }

    fun apply(activity: Activity) {
        ThemeManager.applyBackground(activity)
    }

    fun applyToRoot(root: ViewGroup, themeId: String) {
        val theme = ThemeManager.find(root.context, themeId) ?: return
        if (theme.backgroundDrawable != 0) {
            root.setBackgroundResource(theme.backgroundDrawable)
        } else {
            root.setBackgroundColor(theme.colors.background)
        }
    }

    /** Lightweight adapter used by the existing Appearance carousel. */
    private object ThemeStoragePreview {
        fun themes(): List<Theme> = emptyList()
    }
}
