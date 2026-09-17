package org.fossify.messages.helpers

import android.content.Context
import android.net.Uri
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

/** Versioned .homa-theme import/export format. */
object ThemeFileManager {
    const val FILE_EXTENSION = ".homa-theme"
    const val MIME_TYPE = "application/json"
    const val SCHEMA = "homa-theme"
    const val CURRENT_VERSION = 5
    private const val WALLPAPER_FILE_PREFIX = "homa_wallpaper_"
    private const val MAX_EMBEDDED_WALLPAPER_BYTES = 12 * 1024 * 1024
    private val gson = GsonBuilder().setPrettyPrinting().create()

    fun export(theme: ThemeManager.ThemeDefinition): String = exportInternal(theme, ThemeManager.contextForThemeFiles())
    fun export(context: Context, theme: ThemeManager.ThemeDefinition): String = exportInternal(theme, context)

    private fun exportInternal(theme: ThemeManager.ThemeDefinition, context: Context?): String = JsonObject().apply {
        addProperty("schema", SCHEMA)
        addProperty("version", CURRENT_VERSION)
        add("theme", JsonObject().apply {
            addProperty("id", theme.id)
            addProperty("nameFa", theme.nameFa)
            addProperty("nameEn", theme.nameEn)
            addProperty("backgroundType", theme.backgroundType.name)
            addProperty("gradientAngle", normalizeAngle(theme.gradientAngle))
            addProperty("wallpaperUri", theme.wallpaperUri)
            val embeddedWallpaper = when {
                theme.backgroundType != ThemeManager.BackgroundType.WALLPAPER -> null
                !theme.embeddedWallpaperBase64.isNullOrBlank() -> theme.embeddedWallpaperBase64
                context != null -> readWallpaperBytes(context, theme.wallpaperUri)?.let { bytes ->
                    require(bytes.size <= MAX_EMBEDDED_WALLPAPER_BYTES) { "حجم تصویر پس‌زمینه بیش از حد مجاز است" }
                    ThemeBase64.encode(bytes)
                }
                else -> null
            }
            if (embeddedWallpaper != null) addProperty("wallpaperBase64", embeddedWallpaper)
            if (theme.backgroundType == ThemeManager.BackgroundType.WALLPAPER && context != null && embeddedWallpaper == null) {
                require(false) { "تصویر پس‌زمینه قابل خواندن نیست" }
            }
            add("gradientColors", JsonArray().apply { theme.gradientColors.forEach { add(hex(it)) } })
            add("light", paletteObject(theme.lightColors))
            add("dark", paletteObject(theme.darkColors))
        })
    }.let(gson::toJson)

    fun import(context: Context, raw: String): Result<ThemeManager.ThemeDefinition> =
        importTheme(raw, ThemeManager.allThemes(context).map { it.id }.toSet())

    fun importTheme(raw: String, existingIds: Set<String> = emptySet()): Result<ThemeManager.ThemeDefinition> = runCatching {
        val root = JsonParser.parseString(raw).asJsonObject
        require(root.get("schema")?.asString == SCHEMA) { "فرمت فایل تم معتبر نیست" }
        val formatVersion = root.get("version")?.asInt ?: 0
        require(formatVersion in 1..CURRENT_VERSION) { "نسخه فایل تم پشتیبانی نمی‌شود" }
        val item = root.getAsJsonObject("theme") ?: error("اطلاعات تم وجود ندارد")
        val legacyColors = item.getAsJsonObject("colors")
        val lightObject = item.getAsJsonObject("light") ?: legacyColors ?: error("رنگ‌های تم وجود ندارد")
        val darkObject = item.getAsJsonObject("dark") ?: lightObject
        val originalId = item.get("id")?.asString?.trim().orEmpty()
        require(originalId.isNotBlank()) { "شناسه تم وجود ندارد" }
        val id = if (originalId !in existingIds) originalId else "imported_${UUID.randomUUID()}"
        val backgroundType = runCatching {
            ThemeManager.BackgroundType.valueOf(item.get("backgroundType")?.asString ?: ThemeManager.BackgroundType.SOLID.name)
        }.getOrDefault(ThemeManager.BackgroundType.SOLID)
        val gradientColors = item.getAsJsonArray("gradientColors")?.mapNotNull {
            runCatching { parseColorValue(it.asString) }.getOrNull()
        } ?: emptyList()
        val wallpaperUri = item.get("wallpaperUri")?.takeIf { !it.isJsonNull }?.asString?.trim()?.takeIf { it.isNotBlank() }
        val wallpaperBase64 = item.get("wallpaperBase64")?.takeIf { !it.isJsonNull }?.asString?.trim()?.takeIf { it.isNotBlank() }
        require(backgroundType != ThemeManager.BackgroundType.LINEAR_GRADIENT || gradientColors.size >= 2) { "رنگ‌های گرادیان کامل نیستند" }
        require(backgroundType != ThemeManager.BackgroundType.WALLPAPER || !wallpaperUri.isNullOrBlank() || !wallpaperBase64.isNullOrBlank()) { "تصویر پس‌زمینه تم وجود ندارد" }
        ThemeManager.ThemeDefinition(
            id = id,
            nameFa = item.get("nameFa")?.asString?.trim().orEmpty().ifBlank { "تم واردشده" },
            nameEn = item.get("nameEn")?.asString?.trim().orEmpty().ifBlank { "Imported theme" },
            source = ThemeManager.ThemeSource.IMPORTED,
            lightColors = parsePalette(lightObject),
            darkColors = parsePalette(darkObject),
            version = formatVersion,
            backgroundType = backgroundType,
            gradientColors = gradientColors,
            gradientAngle = normalizeAngle(item.get("gradientAngle")?.asInt ?: 0),
            wallpaperUri = wallpaperUri,
            embeddedWallpaperBase64 = wallpaperBase64
        )
    }

    fun materializeEmbeddedWallpaper(context: Context, theme: ThemeManager.ThemeDefinition): Result<ThemeManager.ThemeDefinition> = runCatching {
        val encoded = theme.embeddedWallpaperBase64 ?: return@runCatching theme
        val bytes = decodeWallpaperBase64(encoded)
        val safeId = theme.id.replace(Regex("[^A-Za-z0-9._-]"), "_").take(80).ifBlank { "theme" }
        val file = File(context.filesDir, "$WALLPAPER_FILE_PREFIX${safeId}_${UUID.randomUUID()}.img")
        file.outputStream().use { it.write(bytes) }
        theme.copy(wallpaperUri = Uri.fromFile(file).toString(), embeddedWallpaperBase64 = null)
    }

    fun saveImported(context: Context, theme: ThemeManager.ThemeDefinition): Boolean = ThemeManager.saveImportedTheme(context, theme)

    private fun paletteObject(colors: ThemeManager.ThemeColors): JsonObject = JsonObject().apply {
        addProperty("primary", hex(colors.primary))
        addProperty("accent", hex(colors.accent))
        addProperty("background", hex(colors.background))
        addProperty("surface", hex(colors.surface))
        addProperty("textPrimary", hex(colors.textPrimary))
        addProperty("textSecondary", hex(colors.textSecondary))
        addProperty("incomingBubble", hex(colors.incomingBubble))
        addProperty("outgoingBubble", hex(colors.outgoingBubble))
        addProperty("toolbar", hex(colors.toolbar))
        addProperty("tab", hex(colors.tab))
        addProperty("fab", hex(colors.fab))
        addProperty("divider", hex(colors.divider))
    }

    private fun parsePalette(colors: JsonObject): ThemeManager.ThemeColors = ThemeManager.ThemeColors(
        parseColor(colors, "primary"), parseColor(colors, "accent"), parseColor(colors, "background"), parseColor(colors, "surface"),
        parseColor(colors, "textPrimary"), parseColor(colors, "textSecondary"), parseColor(colors, "incomingBubble"), parseColor(colors, "outgoingBubble"),
        parseColor(colors, "toolbar"), parseColor(colors, "tab"), parseColor(colors, "fab"), parseColor(colors, "divider", "#33808080")
    )

    private fun readWallpaperBytes(context: Context, wallpaperUri: String?): ByteArray? = wallpaperUri?.let { uri ->
        runCatching {
            context.contentResolver.openInputStream(Uri.parse(uri))?.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var total = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    if (total > MAX_EMBEDDED_WALLPAPER_BYTES) return@use null
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
        }.getOrNull()
    }

    private fun decodeWallpaperBase64(encoded: String): ByteArray = runCatching {
        ThemeBase64.decode(encoded)
    }.getOrElse { error("داده تصویر پس‌زمینه نامعتبر است") }.also {
        require(it.isNotEmpty() && it.size <= MAX_EMBEDDED_WALLPAPER_BYTES) { "داده تصویر پس‌زمینه نامعتبر یا بیش از حد بزرگ است" }
    }

    private fun parseColor(colors: JsonObject, key: String, default: String? = null): Int =
        parseColorValue(colors.get(key)?.asString?.trim() ?: default.orEmpty(), key)

    private fun parseColorValue(value: String, key: String = "color"): Int {
        require(value.matches(Regex("#[0-9A-Fa-f]{6}([0-9A-Fa-f]{2})?"))) { "رنگ نامعتبر برای $key" }
        val hex = value.substring(1).toLong(16).toInt()
        return if (value.length == 7) (0xFF000000.toInt() or hex) else hex
    }

    private fun normalizeAngle(value: Int): Int {
        val normalized = ((value % 360) + 360) % 360
        return ((normalized + 22) / 45 * 45) % 360
    }

    private fun hex(color: Int): String = String.format("#%08X", color)
}
