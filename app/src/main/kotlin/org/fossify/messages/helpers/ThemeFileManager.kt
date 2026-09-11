package org.fossify.messages.helpers

import android.content.Context
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.util.UUID

/** Versioned .homa-theme import/export format. */
object ThemeFileManager {
    const val FILE_EXTENSION = ".homa-theme"
    const val MIME_TYPE = "application/json"
    const val SCHEMA = "homa-theme"
    const val CURRENT_VERSION = 3
    private val gson = GsonBuilder().setPrettyPrinting().create()

    fun export(theme: ThemeManager.ThemeDefinition): String = JsonObject().apply {
        addProperty("schema", SCHEMA); addProperty("version", CURRENT_VERSION)
        add("theme", JsonObject().apply {
            addProperty("id", theme.id); addProperty("nameFa", theme.nameFa); addProperty("nameEn", theme.nameEn)
            addProperty("backgroundType", theme.backgroundType.name); addProperty("gradientAngle", theme.gradientAngle)
            addProperty("wallpaperUri", theme.wallpaperUri)
            add("gradientColors", JsonArray().apply { theme.gradientColors.forEach { add(hex(it)) } })
            add("colors", JsonObject().apply {
                addProperty("primary", hex(theme.colors.primary)); addProperty("accent", hex(theme.colors.accent)); addProperty("background", hex(theme.colors.background)); addProperty("surface", hex(theme.colors.surface))
                addProperty("textPrimary", hex(theme.colors.textPrimary)); addProperty("textSecondary", hex(theme.colors.textSecondary)); addProperty("incomingBubble", hex(theme.colors.incomingBubble)); addProperty("outgoingBubble", hex(theme.colors.outgoingBubble))
                addProperty("toolbar", hex(theme.colors.toolbar)); addProperty("tab", hex(theme.colors.tab)); addProperty("fab", hex(theme.colors.fab)); addProperty("divider", hex(theme.colors.divider))
            })
        })
    }.let(gson::toJson)

    fun import(context: Context, raw: String): Result<ThemeManager.ThemeDefinition> = importTheme(raw, ThemeManager.allThemes(context).map { it.id }.toSet())

    fun importTheme(raw: String, existingIds: Set<String> = emptySet()): Result<ThemeManager.ThemeDefinition> = runCatching {
        val root = JsonParser.parseString(raw).asJsonObject
        require(root.get("schema")?.asString == SCHEMA) { "فرمت فایل تم معتبر نیست" }
        val formatVersion = root.get("version")?.asInt ?: 0
        require(formatVersion in 1..CURRENT_VERSION) { "نسخه فایل تم پشتیبانی نمی‌شود" }
        val item = root.getAsJsonObject("theme") ?: error("اطلاعات تم وجود ندارد")
        val colors = item.getAsJsonObject("colors") ?: error("رنگ‌های تم وجود ندارد")
        val originalId = item.get("id")?.asString?.trim().orEmpty(); require(originalId.isNotBlank()) { "شناسه تم وجود ندارد" }
        val id = if (originalId !in existingIds) originalId else "imported_${UUID.randomUUID()}"
        val backgroundType = runCatching { ThemeManager.BackgroundType.valueOf(item.get("backgroundType")?.asString ?: ThemeManager.BackgroundType.SOLID.name) }.getOrDefault(ThemeManager.BackgroundType.SOLID)
        val gradientColors = item.getAsJsonArray("gradientColors")?.mapNotNull { runCatching { parseColorValue(it.asString) }.getOrNull() } ?: emptyList()
        val wallpaperUri = item.get("wallpaperUri")?.takeIf { !it.isJsonNull }?.asString?.trim()?.takeIf { it.isNotBlank() }
        require(backgroundType != ThemeManager.BackgroundType.LINEAR_GRADIENT || gradientColors.size >= 2) { "رنگ‌های گرادیان کامل نیستند" }
        require(backgroundType != ThemeManager.BackgroundType.WALLPAPER || !wallpaperUri.isNullOrBlank()) { "تصویر پس‌زمینه تم وجود ندارد" }
        ThemeManager.ThemeDefinition(
            id = id, nameFa = item.get("nameFa")?.asString?.trim().orEmpty().ifBlank { "تم واردشده" }, nameEn = item.get("nameEn")?.asString?.trim().orEmpty().ifBlank { "Imported theme" },
            source = ThemeManager.ThemeSource.IMPORTED,
            colors = ThemeManager.ThemeColors(parseColor(colors, "primary"), parseColor(colors, "accent"), parseColor(colors, "background"), parseColor(colors, "surface"), parseColor(colors, "textPrimary"), parseColor(colors, "textSecondary"), parseColor(colors, "incomingBubble"), parseColor(colors, "outgoingBubble"), parseColor(colors, "toolbar"), parseColor(colors, "tab"), parseColor(colors, "fab"), parseColor(colors, "divider", "#33808080")),
            version = formatVersion, backgroundType = backgroundType, gradientColors = gradientColors, gradientAngle = normalizeAngle(item.get("gradientAngle")?.asInt ?: 0), wallpaperUri = wallpaperUri
        )
    }

    fun saveImported(context: Context, theme: ThemeManager.ThemeDefinition): Boolean = ThemeManager.saveImportedTheme(context, theme)
    private fun parseColor(colors: JsonObject, key: String, default: String? = null): Int = parseColorValue(colors.get(key)?.asString?.trim() ?: default.orEmpty(), key)
    private fun parseColorValue(value: String, key: String = "color"): Int { require(value.matches(Regex("#[0-9A-Fa-f]{6}([0-9A-Fa-f]{2})?"))) { "رنگ نامعتبر برای $key" }; val hex = value.substring(1).toLong(16).toInt(); return if (value.length == 7) (0xFF000000.toInt() or hex) else hex }
    private fun normalizeAngle(value: Int): Int { val normalized = ((value % 360) + 360) % 360; return ((normalized + 22) / 45 * 45) % 360 }
    private fun hex(color: Int): String = String.format("#%08X", color)
}
