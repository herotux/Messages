package org.fossify.messages.helpers

import android.content.Context
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.util.UUID

/** Versioned .homa-theme import/export format. */
object ThemeFileManager {
    const val FILE_EXTENSION = ".homa-theme"
    const val MIME_TYPE = "application/json"
    const val SCHEMA = "homa-theme"
    const val CURRENT_VERSION = 1

    private val gson = GsonBuilder().setPrettyPrinting().create()

    fun export(theme: ThemeManager.ThemeDefinition): String = JsonObject().apply {
        addProperty("schema", SCHEMA)
        addProperty("version", CURRENT_VERSION)
        add("theme", JsonObject().apply {
            addProperty("id", theme.id)
            addProperty("nameFa", theme.nameFa)
            addProperty("nameEn", theme.nameEn)
            add("colors", JsonObject().apply {
                addProperty("primary", hex(theme.colors.primary))
                addProperty("accent", hex(theme.colors.accent))
                addProperty("background", hex(theme.colors.background))
                addProperty("surface", hex(theme.colors.surface))
                addProperty("textPrimary", hex(theme.colors.textPrimary))
                addProperty("textSecondary", hex(theme.colors.textSecondary))
                addProperty("incomingBubble", hex(theme.colors.incomingBubble))
                addProperty("outgoingBubble", hex(theme.colors.outgoingBubble))
                addProperty("toolbar", hex(theme.colors.toolbar))
                addProperty("tab", hex(theme.colors.tab))
                addProperty("fab", hex(theme.colors.fab))
                addProperty("divider", hex(theme.colors.divider))
            })
        })
    }.let(gson::toJson)

    fun import(context: Context, raw: String): Result<ThemeManager.ThemeDefinition> =
        importTheme(raw, ThemeManager.allThemes(context).map { it.id }.toSet())

    /** Pure parser used by JVM tests and by callers that already have theme IDs. */
    fun importTheme(raw: String, existingIds: Set<String> = emptySet()): Result<ThemeManager.ThemeDefinition> = runCatching {
        val root = JsonParser.parseString(raw).asJsonObject
        require(root.get("schema")?.asString == SCHEMA) { "فرمت فایل تم معتبر نیست" }
        val formatVersion = root.get("version")?.asInt ?: 0
        require(formatVersion in 1..CURRENT_VERSION) { "نسخه فایل تم پشتیبانی نمی‌شود" }
        val item = root.getAsJsonObject("theme") ?: error("اطلاعات تم وجود ندارد")
        val colors = item.getAsJsonObject("colors") ?: error("رنگ‌های تم وجود ندارد")
        val originalId = item.get("id")?.asString?.trim().orEmpty()
        require(originalId.isNotBlank()) { "شناسه تم وجود ندارد" }
        val id = if (originalId !in existingIds) originalId else "imported_${UUID.randomUUID()}"
        ThemeManager.ThemeDefinition(
            id = id,
            nameFa = item.get("nameFa")?.asString?.trim().orEmpty().ifBlank { "تم واردشده" },
            nameEn = item.get("nameEn")?.asString?.trim().orEmpty().ifBlank { "Imported theme" },
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

    fun saveImported(context: Context, theme: ThemeManager.ThemeDefinition): Boolean =
        ThemeManager.saveImportedTheme(context, theme)

    private fun parseColor(colors: JsonObject, key: String, default: String? = null): Int {
        val value = colors.get(key)?.asString?.trim() ?: default.orEmpty()
        require(value.matches(Regex("#[0-9A-Fa-f]{6}([0-9A-Fa-f]{2})?"))) { "رنگ نامعتبر برای $key" }
        val hex = value.substring(1).toLong(16).toInt()
        return if (value.length == 7) (0xFF000000.toInt() or hex) else hex
    }

    private fun hex(color: Int): String = String.format("#%08X", color)
}
