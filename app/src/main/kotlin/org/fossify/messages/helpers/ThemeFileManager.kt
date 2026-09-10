package org.fossify.messages.helpers

import android.content.Context
import org.json.JSONObject
import java.util.UUID

/** Versioned .homa-theme import/export format. */
object ThemeFileManager {
    const val FILE_EXTENSION = ".homa-theme"
    const val MIME_TYPE = "application/json"
    const val SCHEMA = "homa-theme"
    const val CURRENT_VERSION = 1

    fun export(theme: ThemeManager.ThemeDefinition): String = JSONObject().apply {
        put("schema", SCHEMA)
        put("version", CURRENT_VERSION)
        put("theme", JSONObject().apply {
            put("id", theme.id)
            put("nameFa", theme.nameFa)
            put("nameEn", theme.nameEn)
            put("colors", JSONObject().apply {
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
            })
        })
    }.toString(2)

    fun import(context: Context, raw: String): Result<ThemeManager.ThemeDefinition> =
        importTheme(raw, ThemeManager.allThemes(context).map { it.id }.toSet())

    /** Pure parser used by JVM tests and by callers that already have theme IDs. */
    fun importTheme(raw: String, existingIds: Set<String> = emptySet()): Result<ThemeManager.ThemeDefinition> = runCatching {
        val root = JSONObject(raw)
        require(root.optString("schema") == SCHEMA) { "فرمت فایل تم معتبر نیست" }
        val formatVersion = root.optInt("version", 0)
        require(formatVersion in 1..CURRENT_VERSION) { "نسخه فایل تم پشتیبانی نمی‌شود" }
        val item = root.getJSONObject("theme")
        val colors = item.getJSONObject("colors")
        val originalId = item.optString("id").trim()
        require(originalId.isNotBlank()) { "شناسه تم وجود ندارد" }
        val id = if (originalId !in existingIds) originalId else "imported_${UUID.randomUUID()}"
        ThemeManager.ThemeDefinition(
            id = id,
            nameFa = item.optString("nameFa", item.optString("nameEn", "تم واردشده")).trim().ifBlank { "تم واردشده" },
            nameEn = item.optString("nameEn", item.optString("nameFa", "Imported theme")).trim().ifBlank { "Imported theme" },
            source = ThemeManager.ThemeSource.IMPORTED,
            colors = ThemeManager.ThemeColors(
                primary = parseColor(colors, "primary"),
                accent = parseColor(colors, "accent"),
                background = parseColor(colors, "background"),
                surface = parseColor(colors, "surface"),
                textPrimary = parseColor(colors, "textPrimary"),
                textSecondary = parseColor(colors, "textSecondary"),
                incomingBubble = parseColor(colors, "incomingBubble"),
                outgoingBubble = parseColor(colors, "outgoingBubble"),
                toolbar = parseColor(colors, "toolbar"),
                tab = parseColor(colors, "tab"),
                fab = parseColor(colors, "fab"),
                divider = parseColor(colors, "divider", "#33808080")
            ),
            version = formatVersion
        )
    }

    fun saveImported(context: Context, theme: ThemeManager.ThemeDefinition): Boolean {
        if (theme.source != ThemeManager.ThemeSource.IMPORTED) return false
        val existing = ThemeStorage.load(context).filterNot { it.id == theme.id }
        ThemeStorage.save(context, existing + theme)
        return true
    }

    private fun parseColor(colors: JSONObject, key: String, default: String? = null): Int {
        val value = colors.optString(key, default ?: "").trim()
        require(value.matches(Regex("#[0-9A-Fa-f]{6}([0-9A-Fa-f]{2})?"))) { "رنگ نامعتبر برای $key" }
        val hex = value.substring(1).toLong(16).toInt()
        return if (value.length == 7) (0xFF000000.toInt() or hex) else hex
    }

    private fun hex(color: Int): String = String.format("#%08X", color)
}
