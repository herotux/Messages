package org.fossify.messages.helpers

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Persistent storage for user-created themes. */
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
                if (theme.id.isBlank()) continue
                result.putIfAbsent(theme.id, theme)
            }
            result.values.toList()
        }.getOrDefault(emptyList())
    }

    fun save(context: Context, themes: List<ThemeManager.ThemeDefinition>) {
        val array = JSONArray()
        val seen = HashSet<String>()
        themes.forEach { theme ->
            if (theme.id.isNotBlank() && seen.add(theme.id)) array.put(toJson(theme))
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_USER_THEMES, array.toString()).apply()
    }

    private fun toJson(theme: ThemeManager.ThemeDefinition): JSONObject = JSONObject().apply {
        put("id", theme.id)
        put("nameFa", theme.nameFa)
        put("nameEn", theme.nameEn)
        put("source", theme.source.name)
        put("version", theme.version)
        put("backgroundType", theme.backgroundType.name)
        put("gradientAngle", theme.gradientAngle)
        put("gradientColors", JSONArray().apply { theme.gradientColors.forEach { put(hex(it)) } })
        put("wallpaperUri", theme.wallpaperUri ?: JSONObject.NULL)
        put("primary", hex(theme.colors.primary))
        put("accent", hex(theme.colors.accent))
        put("background", hex(theme.colors.background))
        put("surface", hex(theme.colors.surface))
        put("textPrimary", hex(theme.colors.textPrimary))
        put("textSecondary", hex(theme.colors.textSecondary))
        put("incomingBubble", hex(theme.colors.incomingBubble))
        put("outgoingBubble", hex(theme.colors.outgoingBubble))
        put("toolbar", hex(theme.colors.toolbar))
        put("tab", hex(theme.colors.tab))
        put("fab", hex(theme.colors.fab))
        put("divider", hex(theme.colors.divider))
    }

    private fun fromJson(item: JSONObject): ThemeManager.ThemeDefinition {
        val colors = ThemeManager.ThemeColors(
            parseColor(item.getString("primary")),
            parseColor(item.getString("accent")),
            parseColor(item.getString("background")),
            parseColor(item.getString("surface")),
            parseColor(item.getString("textPrimary")),
            parseColor(item.getString("textSecondary")),
            parseColor(item.getString("incomingBubble")),
            parseColor(item.getString("outgoingBubble")),
            parseColor(item.getString("toolbar")),
            parseColor(item.getString("tab")),
            parseColor(item.getString("fab")),
            parseColor(item.optString("divider", "#33808080"))
        )
        val type = runCatching {
            ThemeManager.BackgroundType.valueOf(item.optString("backgroundType", ThemeManager.BackgroundType.SOLID.name))
        }.getOrDefault(ThemeManager.BackgroundType.SOLID)
        val source = runCatching {
            ThemeManager.ThemeSource.valueOf(item.optString("source", ThemeManager.ThemeSource.USER.name))
        }.getOrDefault(ThemeManager.ThemeSource.USER)
        val gradientColors = item.optJSONArray("gradientColors")?.let { array ->
            buildList {
                for (i in 0 until array.length()) {
                    runCatching { parseColor(array.getString(i)) }.getOrNull()?.let(::add)
                }
            }
        } ?: emptyList()
        val wallpaperUri = item.optString("wallpaperUri", "").takeIf { it.isNotBlank() && it != "null" }
        return ThemeManager.ThemeDefinition(
            id = item.getString("id").trim(),
            nameFa = item.optString("nameFa", item.optString("nameEn", "Custom")),
            nameEn = item.optString("nameEn", item.optString("nameFa", "Custom")),
            source = source,
            colors = colors,
            version = item.optInt("version", 1).coerceAtLeast(1),
            backgroundType = type,
            gradientColors = gradientColors,
            gradientAngle = normalizeAngle(item.optInt("gradientAngle", 0)),
            wallpaperUri = wallpaperUri
        )
    }

    /** Parses #RGB, #RRGGBB and #AARRGGBB without relying on Android graphics classes. */
    private fun parseColor(value: String): Int {
        val hex = value.trim().removePrefix("#")
        val normalized = when (hex.length) {
            3 -> hex.map { "$it$it" }.joinToString("")
            6, 8 -> hex
            else -> throw IllegalArgumentException("Unsupported theme color: $value")
        }
        val argb = if (normalized.length == 6) "FF$normalized" else normalized
        return argb.toLong(16).toInt()
    }

    private fun normalizeAngle(value: Int): Int {
        val normalized = ((value % 360) + 360) % 360
        return ((normalized + 22) / 45 * 45) % 360
    }

    private fun hex(color: Int): String = String.format("#%08X", color)
}
