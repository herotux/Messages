package org.fossify.messages.helpers

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.view.ViewGroup
import org.fossify.messages.R

/** Central theme definition for built-in and user-created themes. */
object ThemeManager {
    private const val PREFS = "messages_theme"
    private const val KEY_THEME_ID = "selected_theme_id"
    private const val KEY_FAVORITES = "favorite_theme_ids"
    private const val KEY_LIBRARY_SORT = "library_sort"
    private const val KEY_RECENT_THEMES = "recent_theme_ids"
    private const val MAX_RECENT_THEMES = 12
    private var applicationContext: Context? = null

    const val DEFAULT_ID = "none"
    const val AURORA_ID = "aurora"
    const val OCEAN_ID = "ocean"
    const val SUNSET_ID = "sunset"
    const val FOREST_ID = "forest"
    const val VIOLET_ID = "violet"
    const val MIDNIGHT_ID = "midnight"

    enum class ThemeSource { BUILT_IN, USER, IMPORTED, COMMUNITY }
    enum class ThemeSort { DEFAULT, FAVORITES_FIRST, NAME_ASC, NAME_DESC, NEWEST, BUILT_IN_FIRST, CUSTOM_FIRST, LAST_USED }
    enum class BackgroundType { SOLID, LINEAR_GRADIENT, WALLPAPER }

    data class ThemeColors(
        val primary: Int, val accent: Int, val background: Int, val surface: Int,
        val textPrimary: Int, val textSecondary: Int, val incomingBubble: Int,
        val outgoingBubble: Int, val toolbar: Int, val tab: Int, val fab: Int,
        val divider: Int = 0x33808080
    )

    data class ThemeDefinition(
        val id: String, val nameFa: String, val nameEn: String,
        val source: ThemeSource = ThemeSource.BUILT_IN,
        val backgroundDrawable: Int = 0, val colors: ThemeColors, val version: Int = 1,
        val backgroundType: BackgroundType = BackgroundType.SOLID,
        val gradientColors: List<Int> = emptyList(), val gradientAngle: Int = 0,
        val wallpaperUri: String? = null,
        /** Temporary payload used while importing an embedded wallpaper. */
        val embeddedWallpaperBase64: String? = null
    )

    fun contextForThemeFiles(): Context? = applicationContext
    private fun c(value: String): Int = Color.parseColor(value)
    private val defaultColors = ThemeColors(c("#388E3C"), c("#4CAF50"), c("#161616"), c("#242424"), c("#FFFFFF"), c("#BDBDBD"), c("#2A2A2A"), c("#388E3C"), c("#388E3C"), c("#388E3C"), c("#4CAF50"))
    private val auroraColors = ThemeColors(c("#6C63FF"), c("#8B80FF"), c("#17152A"), c("#24213D"), c("#FFFFFF"), c("#C9C5E8"), c("#302C4D"), c("#5B54C7"), c("#5B54C7"), c("#8B80FF"), c("#6C63FF"))
    private val oceanColors = ThemeColors(c("#0288D1"), c("#03A9F4"), c("#071A24"), c("#102D3A"), c("#FFFFFF"), c("#B8D5E2"), c("#173846"), c("#0277BD"), c("#0277BD"), c("#03A9F4"), c("#0288D1"))
    private val sunsetColors = ThemeColors(c("#E65100"), c("#FF9800"), c("#21150F"), c("#382219"), c("#FFFFFF"), c("#E6C7B4"), c("#432A1D"), c("#D84315"), c("#D84315"), c("#FF9800"), c("#E65100"))
    private val forestColors = ThemeColors(c("#2E7D32"), c("#66BB6A"), c("#0E1B11"), c("#19301D"), c("#FFFFFF"), c("#BFD8C2"), c("#203A25"), c("#2E7D32"), c("#2E7D32"), c("#66BB6A"), c("#43A047"))
    private val violetColors = ThemeColors(c("#7B1FA2"), c("#AB47BC"), c("#1B101F"), c("#321D38"), c("#FFFFFF"), c("#D8C1DE"), c("#3B2342"), c("#7B1FA2"), c("#7B1FA2"), c("#AB47BC"), c("#9C27B0"))
    private val midnightColors = ThemeColors(c("#607D8B"), c("#90A4AE"), c("#080B0D"), c("#151A1E"), c("#F5F7F8"), c("#AAB6BD"), c("#1D252A"), c("#455A64"), c("#263238"), c("#90A4AE"), c("#607D8B"))

    val builtInThemes: List<ThemeDefinition> = listOf(
        ThemeDefinition(DEFAULT_ID, "بدون پس‌زمینه", "No background", colors = defaultColors),
        ThemeDefinition(AURORA_ID, "شفق قطبی", "Aurora", backgroundDrawable = R.drawable.bg_theme_aurora, colors = auroraColors),
        ThemeDefinition(OCEAN_ID, "اقیانوس", "Ocean", backgroundDrawable = R.drawable.bg_theme_ocean, colors = oceanColors),
        ThemeDefinition(SUNSET_ID, "غروب", "Sunset", backgroundDrawable = R.drawable.bg_theme_sunset, colors = sunsetColors),
        ThemeDefinition(FOREST_ID, "جنگل", "Forest", backgroundDrawable = R.drawable.bg_theme_forest, colors = forestColors),
        ThemeDefinition(VIOLET_ID, "بنفش", "Violet", backgroundDrawable = R.drawable.bg_theme_violet, colors = violetColors),
        ThemeDefinition(MIDNIGHT_ID, "نیمه‌شب", "Midnight", backgroundDrawable = R.drawable.bg_theme_midnight, colors = midnightColors)
    )

    fun allThemes(context: Context): List<ThemeDefinition> {
        applicationContext = context.applicationContext
        val seen = HashSet<String>()
        return (builtInThemes + ThemeStorage.load(context)).filter { it.id.isNotBlank() && seen.add(it.id) }
    }

    fun customThemes(context: Context): List<ThemeDefinition> = ThemeStorage.load(context).filter { it.source != ThemeSource.BUILT_IN }
    fun userThemes(context: Context): List<ThemeDefinition> = customThemes(context).filter { it.source == ThemeSource.USER }
    fun importedThemes(context: Context): List<ThemeDefinition> = customThemes(context).filter { it.source == ThemeSource.IMPORTED }
    fun communityThemes(context: Context): List<ThemeDefinition> = customThemes(context).filter { it.source == ThemeSource.COMMUNITY }
    fun libraryThemes(context: Context): Map<ThemeSource, List<ThemeDefinition>> = linkedMapOf(ThemeSource.BUILT_IN to builtInThemes, ThemeSource.USER to userThemes(context), ThemeSource.IMPORTED to importedThemes(context), ThemeSource.COMMUNITY to communityThemes(context))
    fun selectedThemeId(context: Context): String = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_THEME_ID, DEFAULT_ID)?.takeIf { it.isNotBlank() } ?: DEFAULT_ID
    fun activeTheme(context: Context): ThemeDefinition = current(context)
    fun current(context: Context): ThemeDefinition = allThemes(context).firstOrNull { it.id == selectedThemeId(context) } ?: builtInThemes.first()
    fun colors(context: Context): ThemeColors = current(context).colors

    fun select(context: Context, id: String): Boolean {
        if (allThemes(context).none { it.id == id }) return false
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val recent = prefs.getString(KEY_RECENT_THEMES, "")?.split(',')?.filter { it.isNotBlank() }?.toMutableList() ?: mutableListOf()
        recent.remove(id)
        recent.add(0, id)
        prefs.edit().putString(KEY_THEME_ID, id).putString(KEY_RECENT_THEMES, recent.distinct().take(MAX_RECENT_THEMES).joinToString(",")).apply()
        return true
    }

    fun selectBuiltIn(context: Context, id: String): Boolean = if (builtInThemes.none { it.id == id }) false else select(context, id)
    fun findBuiltIn(id: String): ThemeDefinition? = builtInThemes.firstOrNull { it.id == id }
    fun find(context: Context, id: String): ThemeDefinition? = allThemes(context).firstOrNull { it.id == id }
    fun isFavorite(context: Context, id: String): Boolean = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getStringSet(KEY_FAVORITES, emptySet())?.contains(id) == true

    fun setFavorite(context: Context, id: String, favorite: Boolean) {
        if (find(context, id) == null) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val favorites = prefs.getStringSet(KEY_FAVORITES, emptySet())?.toMutableSet() ?: mutableSetOf()
        if (favorite) favorites.add(id) else favorites.remove(id)
        prefs.edit().putStringSet(KEY_FAVORITES, favorites).apply()
    }

    fun toggleFavorite(context: Context, id: String): Boolean { val next = !isFavorite(context, id); setFavorite(context, id, next); return next }
    fun favoriteThemes(context: Context): List<ThemeDefinition> = allThemes(context).filter { isFavorite(context, it.id) }
    fun librarySort(context: Context): ThemeSort = ThemeSort.entries.getOrElse(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_LIBRARY_SORT, ThemeSort.FAVORITES_FIRST.ordinal)) { ThemeSort.FAVORITES_FIRST }
    fun setLibrarySort(context: Context, sort: ThemeSort) { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt(KEY_LIBRARY_SORT, sort.ordinal).apply() }

    fun searchThemes(context: Context, query: String): List<ThemeDefinition> {
        val q = query.trim()
        val matches = if (q.isEmpty()) allThemes(context) else allThemes(context).filter { it.nameFa.contains(q, true) || it.nameEn.contains(q, true) || it.id.contains(q, true) }
        return sortThemes(context, matches, librarySort(context))
    }

    fun sortThemes(context: Context, themes: List<ThemeDefinition>, sort: ThemeSort): List<ThemeDefinition> = when (sort) {
        ThemeSort.DEFAULT -> themes
        ThemeSort.FAVORITES_FIRST -> themes.sortedWith(compareByDescending<ThemeDefinition> { isFavorite(context, it.id) }.thenBy { it.nameEn.lowercase() }.thenBy { it.id })
        ThemeSort.NAME_ASC -> themes.sortedWith(compareBy<ThemeDefinition> { it.nameEn.lowercase() }.thenBy { it.id })
        ThemeSort.NAME_DESC -> themes.sortedWith(compareByDescending<ThemeDefinition> { it.nameEn.lowercase() }.thenBy { it.id })
        ThemeSort.NEWEST -> themes.asReversed()
        ThemeSort.BUILT_IN_FIRST -> themes.sortedWith(compareBy<ThemeDefinition> { if (it.source == ThemeSource.BUILT_IN) 0 else 1 }.thenBy { it.nameEn.lowercase() })
        ThemeSort.CUSTOM_FIRST -> themes.sortedWith(compareBy<ThemeDefinition> { if (it.source == ThemeSource.BUILT_IN) 1 else 0 }.thenBy { it.nameEn.lowercase() })
        ThemeSort.LAST_USED -> {
            val recent = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_RECENT_THEMES, "")?.split(',')?.filter { it.isNotBlank() }.orEmpty()
            val rank = recent.withIndex().associate { it.value to it.index }
            themes.sortedWith(compareBy<ThemeDefinition> { rank[it.id] ?: Int.MAX_VALUE }.thenBy { it.nameEn.lowercase() })
        }
    }

    fun queryThemes(context: Context, query: String, sort: ThemeSort = librarySort(context)): List<ThemeDefinition> {
        val q = query.trim()
        val matches = if (q.isEmpty()) allThemes(context) else allThemes(context).filter { it.nameFa.contains(q, true) || it.nameEn.contains(q, true) || it.id.contains(q, true) }
        return sortThemes(context, matches, sort)
    }

    fun saveUserTheme(context: Context, theme: ThemeDefinition) {
        val safeId = theme.id.trim()
        if (safeId.isBlank() || builtInThemes.any { it.id == safeId }) return
        val users = ThemeStorage.load(context).filterNot { it.id == safeId }
        ThemeStorage.save(context, users + theme.copy(id = safeId, source = ThemeSource.USER, backgroundDrawable = 0, embeddedWallpaperBase64 = null))
    }

    fun saveImportedTheme(context: Context, theme: ThemeDefinition): Boolean {
        if (theme.source != ThemeSource.IMPORTED || theme.id.isBlank() || builtInThemes.any { it.id == theme.id }) return false
        val storedTheme = if (!theme.embeddedWallpaperBase64.isNullOrBlank()) ThemeFileManager.materializeEmbeddedWallpaper(context, theme).getOrElse { return false } else theme
        val existing = ThemeStorage.load(context).filterNot { it.id == storedTheme.id }
        ThemeStorage.save(context, existing + storedTheme.copy(backgroundDrawable = 0, embeddedWallpaperBase64 = null))
        return true
    }

    fun deleteCustomTheme(context: Context, id: String): Boolean {
        val stored = ThemeStorage.load(context)
        if (stored.none { it.id == id }) return false
        ThemeStorage.save(context, stored.filterNot { it.id == id })
        setFavorite(context, id, false)
        if (selectedThemeId(context) == id) select(context, DEFAULT_ID)
        return true
    }

    fun deleteUserTheme(context: Context, id: String): Boolean = deleteCustomTheme(context, id)

    /** Returns the global theme unless this Activity carries a conversation thread override. */
    fun themeForActivity(activity: Activity): ThemeDefinition {
        val threadId = activity.intent?.getLongExtra(THREAD_ID, 0L) ?: 0L
        if (threadId != 0L) ConversationThemeManager.getTheme(activity, threadId)?.let { return it }
        return current(activity)
    }

    fun applyBackground(activity: Activity) {
        val theme = themeForActivity(activity)
        val content = activity.findViewById<ViewGroup>(android.R.id.content) ?: return
        val root = if (content.childCount == 1 && content.getChildAt(0) is ViewGroup) content.getChildAt(0) as ViewGroup else content
        if (theme.backgroundDrawable != 0) root.setBackgroundResource(theme.backgroundDrawable)
        else ThemeBackground.apply(root, theme.backgroundType, theme.colors.background, theme.gradientColors, theme.gradientAngle, theme.wallpaperUri)
    }
}
