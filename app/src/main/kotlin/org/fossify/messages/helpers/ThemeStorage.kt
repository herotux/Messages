package org.fossify.messages.helpers

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Persistent storage for user-created themes. Version 2 stores independent Light/Dark palettes. */
object ThemeStorage {
    private const val PREFS = "messages_theme"
    private const val KEY_USER_THEMES = "user_themes"

    fun load(context: Context): List<ThemeManager.ThemeDefinition> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_USER_THEMES, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            val result = LinkedHashMap<String, ThemeManager.ThemeDefinition>()
            for (i in 0 until array.length()) {
                val theme = runCatching { fromJson(array.getJSONObject(i)) }.getOrNull() ?: continue
                if (theme.id.isNotBlank()) result.putIfAbsent(theme.id, theme)
            }
            result.values.toList()
        }.getOrDefault(emptyList())
    }

    fun save(context: Context, themes: List<ThemeManager.ThemeDefinition>) {
        val array = JSONArray(); val seen = HashSet<String>()
        themes.forEach { if (it.id.isNotBlank() && seen.add(it.id)) array.put(toJson(it)) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_USER_THEMES, array.toString()).apply()
    }

    private fun toJson(theme: ThemeManager.ThemeDefinition): JSONObject = JSONObject().apply {
        put("id", theme.id); put("nameFa", theme.nameFa); put("nameEn", theme.nameEn)
        put("source", theme.source.name); put("version", maxOf(2, theme.version))
        put("backgroundType", theme.backgroundType.name); put("gradientAngle", theme.gradientAngle)
        put("gradientColors", JSONArray().apply { theme.gradientColors.forEach { put(hex(it)) } })
        put("wallpaperUri", theme.wallpaperUri ?: JSONObject.NULL)
        put("light", colorsToJson(theme.lightColors)); put("dark", colorsToJson(theme.darkColors))
    }

    private fun colorsToJson(colors: ThemeManager.ThemeColors): JSONObject = JSONObject().apply {
        put("primary", hex(colors.primary)); put("accent", hex(colors.accent)); put("background", hex(colors.background)); put("surface", hex(colors.surface))
        put("textPrimary", hex(colors.textPrimary)); put("textSecondary", hex(colors.textSecondary)); put("incomingBubble", hex(colors.incomingBubble)); put("outgoingBubble", hex(colors.outgoingBubble))
        put("toolbar", hex(colors.toolbar)); put("tab", hex(colors.tab)); put("fab", hex(colors.fab)); put("divider", hex(colors.divider))
    }

    private fun fromJson(item: JSONObject): ThemeManager.ThemeDefinition {
        val lightObject = item.optJSONObject("light")
        val darkObject = item.optJSONObject("dark")
        // V1 migration: old themes had one top-level palette. Preserve their appearance in both modes
        // rather than silently changing a user's saved theme during the storage migration.
        val legacy = if (lightObject == null && darkObject == null) parseColors(item) else null
        val light = lightObject?.let(::parseColors) ?: legacy ?: parseColors(darkObject!!)
        val dark = darkObject?.let(::parseColors) ?: legacy ?: light
        val type = runCatching { ThemeManager.BackgroundType.valueOf(item.optString("backgroundType", ThemeManager.BackgroundType.SOLID.name)) }.getOrDefault(ThemeManager.BackgroundType.SOLID)
        val source = runCatching { ThemeManager.ThemeSource.valueOf(item.optString("source", ThemeManager.ThemeSource.USER.name)) }.getOrDefault(ThemeManager.ThemeSource.USER)
        val gradientColors = item.optJSONArray("gradientColors")?.let { a -> buildList { for (i in 0 until a.length()) runCatching { parseColor(a.getString(i)) }.getOrNull()?.let(::add) } } ?: emptyList()
        val wallpaperUri = item.optString("wallpaperUri", "").takeIf { it.isNotBlank() && it != "null" }
        return ThemeManager.ThemeDefinition(
            id=item.getString("id").trim(), nameFa=item.optString("nameFa",item.optString("nameEn","Custom")), nameEn=item.optString("nameEn",item.optString("nameFa","Custom")),
            source=source, lightColors=light, darkColors=dark, version=maxOf(2,item.optInt("version",1)), backgroundType=type,
            gradientColors=gradientColors, gradientAngle=normalizeAngle(item.optInt("gradientAngle",0)), wallpaperUri=wallpaperUri
        )
    }

    private fun parseColors(item: JSONObject): ThemeManager.ThemeColors = ThemeManager.ThemeColors(
        parseColor(item.getString("primary")), parseColor(item.getString("accent")), parseColor(item.getString("background")), parseColor(item.getString("surface")),
        parseColor(item.getString("textPrimary")), parseColor(item.getString("textSecondary")), parseColor(item.getString("incomingBubble")), parseColor(item.getString("outgoingBubble")),
        parseColor(item.getString("toolbar")), parseColor(item.getString("tab")), parseColor(item.getString("fab")), parseColor(item.optString("divider","#33808080"))
    )

    private fun parseColor(value: String): Int {
        val hex=value.trim().removePrefix("#")
        val normalized=when(hex.length){3->hex.map{"$it$it"}.joinToString("");6,8->hex;else->throw IllegalArgumentException("Unsupported theme color: $value")}
        return (if(normalized.length==6)"FF$normalized" else normalized).toLong(16).toInt()
    }
    private fun normalizeAngle(value:Int):Int { val n=((value%360)+360)%360; return ((n+22)/45*45)%360 }
    private fun hex(color:Int):String=String.format("#%08X",color)
}
