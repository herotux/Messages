package org.fossify.messages.helpers

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.net.Uri
import android.view.ViewGroup
import org.fossify.messages.R

/** Central theme definition and persisted theme state for Homa. */
object ThemeManager {
    private const val PREFS = "messages_theme"
    private const val KEY_THEME_ID = "selected_theme_id"
    private const val KEY_FAVORITES = "favorite_theme_ids"
    private const val KEY_LIBRARY_SORT = "library_sort"
    private const val KEY_RECENT_THEMES = "recent_theme_ids"
    private const val KEY_BACKGROUND_IMAGE_URI = "background_image_uri"
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

    /** Theme identity owns both complete appearance palettes. */
    data class ThemeDefinition(
        val id: String, val nameFa: String, val nameEn: String,
        val source: ThemeSource = ThemeSource.BUILT_IN,
        val backgroundDrawable: Int = 0,
        val lightColors: ThemeColors,
        val darkColors: ThemeColors,
        val version: Int = 1,
        val backgroundType: BackgroundType = BackgroundType.SOLID,
        val gradientColors: List<Int> = emptyList(), val gradientAngle: Int = 0,
        val wallpaperUri: String? = null,
        val embeddedWallpaperBase64: String? = null
    ) {
        @Deprecated("Use lightColors/darkColors")
        val colors: ThemeColors get() = lightColors
        fun colorsForMode(darkMode: Boolean): ThemeColors = if (darkMode) darkColors else lightColors

        /** Compatibility constructor for older editor/import code. */
        constructor(
            id: String, nameFa: String, nameEn: String,
            source: ThemeSource = ThemeSource.BUILT_IN, backgroundDrawable: Int = 0,
            colors: ThemeColors, version: Int = 1,
            backgroundType: BackgroundType = BackgroundType.SOLID,
            gradientColors: List<Int> = emptyList(), gradientAngle: Int = 0,
            wallpaperUri: String? = null, embeddedWallpaperBase64: String? = null
        ) : this(id, nameFa, nameEn, source, backgroundDrawable, colors, colors, version,
            backgroundType, gradientColors, gradientAngle, wallpaperUri, embeddedWallpaperBase64)
    }

    fun contextForThemeFiles(): Context? = applicationContext
    fun contextForDarkMode(context: Context): Boolean =
        (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    fun colors(context: Context): ThemeColors = current(context).colorsForMode(contextForDarkMode(context))

    private fun c(value: String): Int {
        val hex = value.trim().removePrefix("#")
        val normalized = when (hex.length) { 3 -> hex.map { "$it$it" }.joinToString(""); 6, 8 -> hex; else -> throw IllegalArgumentException("Unsupported theme color: $value") }
        return (if (normalized.length == 6) "FF$normalized" else normalized).toLong(16).toInt()
    }

    private val defaultLight = ThemeColors(c("#00A6A6"),c("#5DD6D3"),c("#F4FBFB"),c("#E4F7F6"),c("#123737"),c("#527070"),c("#D5EFEE"),c("#BDEBE9"),c("#FFFFFF"),c("#00A6A6"),c("#00A6A6"))
    private val defaultDark = ThemeColors(c("#5DD6D3"),c("#00A6A6"),c("#0D2424"),c("#123B3B"),c("#E8FAF9"),c("#A9C9C8"),c("#1A4848"),c("#0E5556"),c("#123B3B"),c("#5DD6D3"),c("#00A6A6"))
    private val auroraLight = ThemeColors(c("#625BCE"),c("#8178E8"),c("#FAF9FF"),c("#F0EEFF"),c("#211E2E"),c("#66627A"),c("#EAE7FF"),c("#DCD7FF"),c("#F0EEFF"),c("#625BCE"),c("#625BCE"))
    private val auroraDark = ThemeColors(c("#B8B0FF"),c("#9188FF"),c("#17152A"),c("#24213D"),c("#F5F2FF"),c("#CAC5E5"),c("#302C4D"),c("#5149A8"),c("#24213D"),c("#B8B0FF"),c("#9188FF"))
    private val oceanLight = ThemeColors(c("#0277B5"),c("#03A9F4"),c("#F5FBFE"),c("#E5F5FC"),c("#102832"),c("#526D78"),c("#D7EDF6"),c("#C5E6F4"),c("#E5F5FC"),c("#0277B5"),c("#0277B5"))
    private val oceanDark = ThemeColors(c("#63C5F4"),c("#29B6F6"),c("#071A24"),c("#102D3A"),c("#EAF8FF"),c("#B8D5E2"),c("#173846"),c("#075A88"),c("#102D3A"),c("#63C5F4"),c("#29B6F6"))
    private val sunsetLight = ThemeColors(c("#D84A00"),c("#FF9800"),c("#FFF9F5"),c("#FFF0E6"),c("#352017"),c("#765C4E"),c("#FFE3D1"),c("#FFD4BD"),c("#FFF0E6"),c("#D84A00"),c("#D84A00"))
    private val sunsetDark = ThemeColors(c("#FFB15C"),c("#FFB52E"),c("#21150F"),c("#382219"),c("#FFF4EC"),c("#E6C7B4"),c("#432A1D"),c("#9E351A"),c("#382219"),c("#FFB15C"),c("#FFB52E"))
    private val forestLight = ThemeColors(c("#2E7D32"),c("#43A047"),c("#F6FBF6"),c("#E8F4E9"),c("#17251A"),c("#59705D"),c("#D9ECDD"),c("#CBE6CE"),c("#E8F4E9"),c("#2E7D32"),c("#2E7D32"))
    private val forestDark = ThemeColors(c("#81C784"),c("#66BB6A"),c("#0E1B11"),c("#19301D"),c("#EFF8F0"),c("#BFD8C2"),c("#203A25"),c("#28652E"),c("#19301D"),c("#81C784"),c("#66BB6A"))
    private val violetLight = ThemeColors(c("#7B1FA2"),c("#AB47BC"),c("#FCF8FE"),c("#F3E8F5"),c("#291A2D"),c("#705D75"),c("#EAD9EE"),c("#DFC8E5"),c("#F3E8F5"),c("#7B1FA2"),c("#7B1FA2"))
    private val violetDark = ThemeColors(c("#D38AE8"),c("#CE93D8"),c("#1B101F"),c("#321D38"),c("#FCEFFF"),c("#D8C1DE"),c("#3B2342"),c("#64207B"),c("#321D38"),c("#D38AE8"),c("#CE93D8"))
    private val midnightLight = ThemeColors(c("#455A64"),c("#607D8B"),c("#F7FAFB"),c("#EAF0F2"),c("#172126"),c("#58666D"),c("#DCE5E8"),c("#CFDCE1"),c("#EAF0F2"),c("#455A64"),c("#455A64"))
    private val midnightDark = ThemeColors(c("#90A4AE"),c("#B0BEC5"),c("#080B0D"),c("#151A1E"),c("#F5F7F8"),c("#AAB6BD"),c("#1D252A"),c("#35474F"),c("#151A1E"),c("#90A4AE"),c("#B0BEC5"))

    val builtInThemes: List<ThemeDefinition> = listOf(
        ThemeDefinition(DEFAULT_ID,"بدون پس‌زمینه","No background",lightColors=defaultLight,darkColors=defaultDark),
        ThemeDefinition(AURORA_ID,"شفق قطبی","Aurora",backgroundDrawable=R.drawable.bg_theme_aurora,lightColors=auroraLight,darkColors=auroraDark),
        ThemeDefinition(OCEAN_ID,"اقیانوس","Ocean",backgroundDrawable=R.drawable.bg_theme_ocean,lightColors=oceanLight,darkColors=oceanDark),
        ThemeDefinition(SUNSET_ID,"غروب","Sunset",backgroundDrawable=R.drawable.bg_theme_sunset,lightColors=sunsetLight,darkColors=sunsetDark),
        ThemeDefinition(FOREST_ID,"جنگل","Forest",backgroundDrawable=R.drawable.bg_theme_forest,lightColors=forestLight,darkColors=forestDark),
        ThemeDefinition(VIOLET_ID,"بنفش","Violet",backgroundDrawable=R.drawable.bg_theme_violet,lightColors=violetLight,darkColors=violetDark),
        ThemeDefinition(MIDNIGHT_ID,"نیمه‌شب","Midnight",backgroundDrawable=R.drawable.bg_theme_midnight,lightColors=midnightLight,darkColors=midnightDark)
    )

    fun allThemes(context: Context): List<ThemeDefinition> { applicationContext=context.applicationContext; val seen=HashSet<String>(); return (builtInThemes+ThemeStorage.load(context)).filter{it.id.isNotBlank()&&seen.add(it.id)} }
    fun customThemes(context: Context)=ThemeStorage.load(context).filter{it.source!=ThemeSource.BUILT_IN}
    fun userThemes(context: Context)=customThemes(context).filter{it.source==ThemeSource.USER}
    fun importedThemes(context: Context)=customThemes(context).filter{it.source==ThemeSource.IMPORTED}
    fun communityThemes(context: Context)=customThemes(context).filter{it.source==ThemeSource.COMMUNITY}
    fun libraryThemes(context: Context)=linkedMapOf(ThemeSource.BUILT_IN to builtInThemes,ThemeSource.USER to userThemes(context),ThemeSource.IMPORTED to importedThemes(context),ThemeSource.COMMUNITY to communityThemes(context))
    fun selectedThemeId(context: Context)=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getString(KEY_THEME_ID,DEFAULT_ID)?.takeIf{it.isNotBlank()}?:DEFAULT_ID
    fun activeTheme(context: Context)=current(context)
    fun current(context: Context)=allThemes(context).firstOrNull{it.id==selectedThemeId(context)}?:builtInThemes.first()
    fun getBackgroundImageUri(context: Context): Uri?=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getString(KEY_BACKGROUND_IMAGE_URI,null).orEmpty().takeIf{it.isNotBlank()}?.let(Uri::parse)
    fun setBackgroundImageUri(context: Context,uri: Uri?){context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putString(KEY_BACKGROUND_IMAGE_URI,uri?.toString().orEmpty()).apply()}
    fun clearBackgroundImage(context: Context)=setBackgroundImageUri(context,null)
    fun select(context: Context,id:String):Boolean{if(allThemes(context).none{it.id==id})return false;val p=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);val r=p.getString(KEY_RECENT_THEMES,"")?.split(',')?.filter{it.isNotBlank()}?.toMutableList()?:mutableListOf();r.remove(id);r.add(0,id);p.edit().putString(KEY_THEME_ID,id).putString(KEY_RECENT_THEMES,r.distinct().take(MAX_RECENT_THEMES).joinToString(",")).apply();return true}
    fun selectBuiltIn(context: Context,id:String)=if(builtInThemes.none{it.id==id})false else select(context,id)
    fun findBuiltIn(id:String)=builtInThemes.firstOrNull{it.id==id}
    fun find(context:Context,id:String)=allThemes(context).firstOrNull{it.id==id}
    fun isFavorite(context:Context,id:String)=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getStringSet(KEY_FAVORITES,emptySet())?.contains(id)==true
    fun setFavorite(context:Context,id:String,favorite:Boolean){if(find(context,id)==null)return;val p=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);val f=p.getStringSet(KEY_FAVORITES,emptySet())?.toMutableSet()?:mutableSetOf();if(favorite)f.add(id)else f.remove(id);p.edit().putStringSet(KEY_FAVORITES,f).apply()}
    fun toggleFavorite(context:Context,id:String):Boolean{val n=!isFavorite(context,id);setFavorite(context,id,n);return n}
    fun favoriteThemes(context:Context)=allThemes(context).filter{isFavorite(context,it.id)}
    fun librarySort(context:Context)=ThemeSort.entries.getOrElse(context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getInt(KEY_LIBRARY_SORT,ThemeSort.FAVORITES_FIRST.ordinal)){ThemeSort.FAVORITES_FIRST}
    fun setLibrarySort(context:Context,sort:ThemeSort){context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putInt(KEY_LIBRARY_SORT,sort.ordinal).apply()}
    fun searchThemes(context:Context,query:String)=queryThemes(context,query,librarySort(context))
    fun queryThemes(context:Context,query:String,sort:ThemeSort=librarySort(context)):List<ThemeDefinition>{val q=query.trim();val m=if(q.isEmpty())allThemes(context)else allThemes(context).filter{it.nameFa.contains(q,true)||it.nameEn.contains(q,true)||it.id.contains(q,true)};return sortThemes(context,m,sort)}
    fun sortThemes(context:Context,themes:List<ThemeDefinition>,sort:ThemeSort)=when(sort){ThemeSort.DEFAULT->themes;ThemeSort.FAVORITES_FIRST->themes.sortedWith(compareByDescending<ThemeDefinition>{isFavorite(context,it.id)}.thenBy{it.nameEn.lowercase()}.thenBy{it.id});ThemeSort.NAME_ASC->themes.sortedWith(compareBy<ThemeDefinition>{it.nameEn.lowercase()}.thenBy{it.id});ThemeSort.NAME_DESC->themes.sortedWith(compareByDescending<ThemeDefinition>{it.nameEn.lowercase()}.thenBy{it.id});ThemeSort.NEWEST->themes.asReversed();ThemeSort.BUILT_IN_FIRST->themes.sortedWith(compareBy<ThemeDefinition>{if(it.source==ThemeSource.BUILT_IN)0 else 1}.thenBy{it.nameEn.lowercase()});ThemeSort.CUSTOM_FIRST->themes.sortedWith(compareBy<ThemeDefinition>{if(it.source==ThemeSource.BUILT_IN)1 else 0}.thenBy{it.nameEn.lowercase()});ThemeSort.LAST_USED->{val r=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getString(KEY_RECENT_THEMES,"")?.split(',')?.filter{it.isNotBlank()}.orEmpty();val rank=r.withIndex().associate{it.value to it.index};themes.sortedWith(compareBy<ThemeDefinition>{rank[it.id]?:Int.MAX_VALUE}.thenBy{it.nameEn.lowercase()})}}
    fun saveUserTheme(context:Context,theme:ThemeDefinition){val id=theme.id.trim();if(id.isBlank()||builtInThemes.any{it.id==id})return;ThemeStorage.save(context,ThemeStorage.load(context).filterNot{it.id==id}+theme.copy(id=id,source=ThemeSource.USER,backgroundDrawable=0,embeddedWallpaperBase64=null))}
    fun saveImportedTheme(context:Context,theme:ThemeDefinition):Boolean{if(theme.source!=ThemeSource.IMPORTED||theme.id.isBlank()||builtInThemes.any{it.id==theme.id})return false;val t=if(!theme.embeddedWallpaperBase64.isNullOrBlank())ThemeFileManager.materializeEmbeddedWallpaper(context,theme).getOrElse{return false}else theme;ThemeStorage.save(context,ThemeStorage.load(context).filterNot{it.id==t.id}+t.copy(backgroundDrawable=0,embeddedWallpaperBase64=null));return true}
    fun deleteCustomTheme(context:Context,id:String):Boolean{val s=ThemeStorage.load(context);if(s.none{it.id==id})return false;ThemeStorage.save(context,s.filterNot{it.id==id});setFavorite(context,id,false);if(selectedThemeId(context)==id)select(context,DEFAULT_ID);return true}
    fun deleteUserTheme(context:Context,id:String)=deleteCustomTheme(context,id)
    fun themeForActivity(activity:Activity):ThemeDefinition{val threadId=activity.intent?.getLongExtra(THREAD_ID,0L)?:0L;if(threadId!=0L)ConversationThemeManager.getTheme(activity,threadId)?.let{return it};return current(activity)}
    fun applyBackground(activity:Activity){val threadId=activity.intent?.getLongExtra(THREAD_ID,0L)?:0L;if(threadId==0L)return;val theme=themeForActivity(activity);val colors=theme.colorsForMode(contextForDarkMode(activity));val content=activity.findViewById<ViewGroup>(android.R.id.content)?:return;val root=if(content.childCount==1&&content.getChildAt(0)is ViewGroup)content.getChildAt(0)as ViewGroup else content;val legacy=if(theme.backgroundType==BackgroundType.SOLID&&theme.wallpaperUri==null)getBackgroundImageUri(activity)else null;if(theme.backgroundDrawable!=0)root.setBackgroundResource(theme.backgroundDrawable)else ThemeBackground.apply(root,theme.backgroundType,colors.background,theme.gradientColors,theme.gradientAngle,theme.wallpaperUri?:legacy?.toString())}
}
