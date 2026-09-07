package org.fossify.messages.helpers

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.view.ViewGroup
import org.fossify.messages.R

/**
 * Central theme definition for built-in, user, imported and community themes.
 *
 * Stage 2 introduces a real palette while keeping the existing background theme
 * selection compatible with the old Settings UI.
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
        val primary: Int,
        val accent: Int,
        val background: Int,
        val surface: Int,
        val textPrimary: Int,
        val textSecondary: Int,
        val incomingBubble: Int,
        val outgoingBubble: Int,
        val toolbar: Int,
        val tab: Int,
        val fab: Int,
        val divider: Int = 0x33808080
    )

    data class ThemeDefinition(
        val id: String,
        val nameFa: String,
        val nameEn: String,
        val source: ThemeSource = ThemeSource.BUILT_IN,
        val backgroundDrawable: Int = 0,
        val colors: ThemeColors,
        val version: Int = 1
    )

    private fun c(value: String): Int = Color.parseColor(value)

    private val defaultColors = ThemeColors(
        primary = c("#388E3C"), accent = c("#4CAF50"), background = c("#161616"),
        surface = c("#242424"), textPrimary = c("#FFFFFF"), textSecondary = c("#BDBDBD"),
        incomingBubble = c("#2A2A2A"), outgoingBubble = c("#388E3C"),
        toolbar = c("#388E3C"), tab = c("#388E3C"), fab = c("#4CAF50")
    )
    private val auroraColors = ThemeColors(
        primary = c("#6C63FF"), accent = c("#8B80FF"), background = c("#17152A"),
        surface = c("#24213D"), textPrimary = c("#FFFFFF"), textSecondary = c("#C9C5E8"),
        incomingBubble = c("#302C4D"), outgoingBubble = c("#5B54C7"),
        toolbar = c("#5B54C7"), tab = c("#8B80FF"), fab = c("#6C63FF")
    )
    private val oceanColors = ThemeColors(
        primary = c("#0288D1"), accent = c("#03A9F4"), background = c("#071A24"),
        surface = c("#102D3A"), textPrimary = c("#FFFFFF"), textSecondary = c("#B8D5E2"),
        incomingBubble = c("#173846"), outgoingBubble = c("#0277BD"),
        toolbar = c("#0277BD"), tab = c("#03A9F4"), fab = c("#0288D1")
    )
    private val sunsetColors = ThemeColors(
        primary = c("#E65100"), accent = c("#FF9800"), background = c("#21150F"),
        surface = c("#382219"), textPrimary = c("#FFFFFF"), textSecondary = c("#E6C7B4"),
        incomingBubble = c("#432A1D"), outgoingBubble = c("#D84315"),
        toolbar = c("#D84315"), tab = c("#FF9800"), fab = c("#E65100")
    )
    private val forestColors = ThemeColors(
        primary = c("#2E7D32"), accent = c("#66BB6A"), background = c("#0E1B11"),
        surface = c("#19301D"), textPrimary = c("#FFFFFF"), textSecondary = c("#BFD8C2"),
        incomingBubble = c("#203A25"), outgoingBubble = c("#2E7D32"),
        toolbar = c("#2E7D32"), tab = c("#66BB6A"), fab = c("#43A047")
    )
    private val violetColors = ThemeColors(
        primary = c("#7B1FA2"), accent = c("#AB47BC"), background = c("#1B101F"),
        surface = c("#321D38"), textPrimary = c("#FFFFFF"), textSecondary = c("#D8C1DE"),
        incomingBubble = c("#3B2342"), outgoingBubble = c("#7B1FA2"),
        toolbar = c("#7B1FA2"), tab = c("#AB47BC"), fab = c("#9C27B0")
    )
    private val midnightColors = ThemeColors(
        primary = c("#607D8B"), accent = c("#90A4AE"), background = c("#080B0D"),
        surface = c("#151A1E"), textPrimary = c("#F5F7F8"), textSecondary = c("#AAB6BD"),
        incomingBubble = c("#1D252A"), outgoingBubble = c("#455A64"),
        toolbar = c("#263238"), tab = c("#90A4AE"), fab = c("#607D8B")
    )

    val builtInThemes: List<ThemeDefinition> = listOf(
        ThemeDefinition(DEFAULT_ID, "بدون پس‌زمینه", "No background", colors = defaultColors),
        ThemeDefinition(AURORA_ID, "شفق قطبی", "Aurora", backgroundDrawable = R.drawable.bg_theme_aurora, colors = auroraColors),
        ThemeDefinition(OCEAN_ID, "اقیانوس", "Ocean", backgroundDrawable = R.drawable.bg_theme_ocean, colors = oceanColors),
        ThemeDefinition(SUNSET_ID, "غروب", "Sunset", backgroundDrawable = R.drawable.bg_theme_sunset, colors = sunsetColors),
        ThemeDefinition(FOREST_ID, "جنگل", "Forest", backgroundDrawable = R.drawable.bg_theme_forest, colors = forestColors),
        ThemeDefinition(VIOLET_ID, "بنفش", "Violet", backgroundDrawable = R.drawable.bg_theme_violet, colors = violetColors),
        ThemeDefinition(MIDNIGHT_ID, "نیمه‌شب", "Midnight", backgroundDrawable = R.drawable.bg_theme_midnight, colors = midnightColors)
    )

    fun selectedThemeId(context: Context): String = context
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getString(KEY_THEME_ID, DEFAULT_ID)
        ?: DEFAULT_ID

    fun current(context: Context): ThemeDefinition =
        findBuiltIn(selectedThemeId(context)) ?: findBuiltIn(DEFAULT_ID)!!

    fun colors(context: Context): ThemeColors = current(context).colors

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
        val theme = current(activity)
        val content = activity.findViewById<ViewGroup>(android.R.id.content) ?: return
        val root = if (content.childCount == 1 && content.getChildAt(0) is ViewGroup) {
            content.getChildAt(0) as ViewGroup
        } else {
            content
        }

        if (theme.backgroundDrawable != 0) {
            root.setBackgroundResource(theme.backgroundDrawable)
        } else {
            root.setBackgroundColor(theme.colors.background)
        }
    }
}
