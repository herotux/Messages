package org.fossify.messages.helpers

import android.content.Context
import android.graphics.Color
import org.json.JSONArray
import org.json.JSONObject

/** Persistent storage for user-created themes. */
object ThemeStorage {
    private const val PREFS = "messages_theme"
    private const val KEY_USER_THEMES = "user_themes"

    fun load(context: Context): List<ThemeManager.ThemeDefinition> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_USER_THEMES, null) ?: return emptyList()

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
        themes.forEach { array.put(toJson(it)) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_USER_THEMES, array.toString())
            .apply()
    }

    private fun toJson(theme: ThemeManager.ThemeDefinition): JSONObject = JSONObject().apply {
        put("id", theme.id)
        put("nameFa", theme.nameFa)
        put("nameEn", theme.nameEn)
        put("version", theme.version)
        put("backgroundType", theme.backgroundType.name)
        put("gradientAngle", theme.gradientAngle)
        put("gradientColors", JSONArray().apply { theme.gradientColors.forEach { put(hex(it)) } })
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
            primary = Color.parseColor(item.getString("primary")),
            accent = Color.parseColor(item.getString("accent")),
            background = Color.parseColor(item.getString("background")),
            surface = Color.parseColor(item.getString("surface")),
            textPrimary = Color.parseColor(item.getString("textPrimary")),
            textSecondary = Color.parseColor(item.getString("textSecondary")),
            incomingBubble = Color.parseColor(item.getString("incomingBubble")),
            outgoingBubble = Color.parseColor(item.getString("outgoingBubble")),
            toolbar = Color.parseColor(item.getString("toolbar")),
            tab = Color.parseColor(item.getString("tab")),
            fab = Color.parseColor(item.getString("fab")),
            divider = Color.parseColor(item.optString("divider", "#33808080"))
        )
        val type = runCatching { ThemeManager.BackgroundType.valueOf(item.optString("backgroundType", ThemeManager.BackgroundType.SOLID.name)) }
            .getOrDefault(ThemeManager.BackgroundType.SOLID)
        val gradientColors = item.optJSONArray("gradientColors")?.let { array ->
            buildList {
                for (i in 0 until array.length()) runCatching { Color.parseColor(array.getString(i)) }.getOrNull()?.let(::add)
            }
        } ?: emptyList()
        return ThemeManager.ThemeDefinition(
            id = item.getString("id"),
            nameFa = item.optString("nameFa", item.optString("nameEn", "Custom")),
            nameEn = item.optString("nameEn", item.optString("nameFa", "Custom")),
            source = ThemeManager.ThemeSource.USER,
            colors = colors,
            version = item.optInt("version", 1).coerceAtLeast(1),
            backgroundType = type,
            gradientColors = gradientColors,
            gradientAngle = item.optInt("gradientAngle", 0)
        )
    }

    private fun hex(color: Int): String = String.format("#%08X", color)
}
