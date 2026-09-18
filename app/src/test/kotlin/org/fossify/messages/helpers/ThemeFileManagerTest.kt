package org.fossify.messages.helpers

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeFileManagerTest {
    private val light = ThemeManager.ThemeColors(
        primary = 0xFF388E3C.toInt(), accent = 0xFF4CAF50.toInt(), background = 0xFFF4FBFB.toInt(), surface = 0xFFE4F7F6.toInt(),
        textPrimary = 0xFF101818.toInt(), textSecondary = 0xFF536666.toInt(), incomingBubble = 0xFFD5EFEE.toInt(), outgoingBubble = 0xFFBDEBE9.toInt(),
        toolbar = 0xFF00A6A6.toInt(), tab = 0xFF5DD6D3.toInt(), fab = 0xFF00A6A6.toInt(), divider = 0x33808080
    )

    private val dark = ThemeManager.ThemeColors(
        primary = 0xFF5DD6D3.toInt(), accent = 0xFF00A6A6.toInt(), background = 0xFF0D2424.toInt(), surface = 0xFF123B3B.toInt(),
        textPrimary = 0xFFF2FFFF.toInt(), textSecondary = 0xFFA9C4C4.toInt(), incomingBubble = 0xFF1A4848.toInt(), outgoingBubble = 0xFF0E5556.toInt(),
        toolbar = 0xFF123B3B.toInt(), tab = 0xFF0E5556.toInt(), fab = 0xFF5DD6D3.toInt(), divider = 0x33808080
    )

    private fun theme(id: String = "user_test") = ThemeManager.ThemeDefinition(
        id = id, nameFa = "تم آزمایشی", nameEn = "Test Theme", source = ThemeManager.ThemeSource.USER,
        lightColors = light, darkColors = dark
    )

    @Test
    fun export_contains_versioned_dual_palette_schema() {
        val root = JsonParser.parseString(ThemeFileManager.export(theme())).asJsonObject
        val item = root.getAsJsonObject("theme")
        assertEquals(ThemeFileManager.CURRENT_VERSION, root.get("version").asInt)
        assertTrue(item.has("light"))
        assertTrue(item.has("dark"))
        assertEquals(light.primary.toString(), light.primary.toString())
    }

    @Test
    fun export_import_round_trip_preserves_both_palettes() {
        val original = theme()
        val imported = ThemeFileManager.importTheme(ThemeFileManager.export(original)).getOrThrow()
        assertEquals(original.id, imported.id)
        assertEquals(original.nameFa, imported.nameFa)
        assertEquals(original.nameEn, imported.nameEn)
        assertEquals(original.lightColors, imported.lightColors)
        assertEquals(original.darkColors, imported.darkColors)
        assertEquals(ThemeManager.ThemeSource.IMPORTED, imported.source)
        assertEquals(ThemeFileManager.CURRENT_VERSION, imported.version)
    }

    @Test
    fun legacy_single_palette_import_is_migrated_to_both_modes() {
        val root = JsonParser.parseString(ThemeFileManager.export(theme())).asJsonObject
        val item = root.getAsJsonObject("theme")
        item.remove("light")
        item.remove("dark")
        item.add("colors", JsonParser.parseString(ThemeFileManager.export(theme())).asJsonObject.getAsJsonObject("theme").getAsJsonObject("light"))
        val imported = ThemeFileManager.importTheme(root.toString()).getOrThrow()
        assertEquals(imported.lightColors, imported.darkColors)
    }

    @Test
    fun gradient_round_trip_preserves_angle_and_colors() {
        val original = theme().copy(backgroundType = ThemeManager.BackgroundType.LINEAR_GRADIENT, gradientColors = listOf(0xFF112233.toInt(), 0xFF445566.toInt(), 0xFF778899.toInt()), gradientAngle = 91)
        val imported = ThemeFileManager.importTheme(ThemeFileManager.export(original)).getOrThrow()
        assertEquals(ThemeManager.BackgroundType.LINEAR_GRADIENT, imported.backgroundType)
        assertEquals(original.gradientColors, imported.gradientColors)
        assertEquals(90, imported.gradientAngle)
    }

    @Test
    fun embedded_wallpaper_is_preserved_on_import() {
        val root = JsonParser.parseString(ThemeFileManager.export(theme())).asJsonObject
        val item = root.getAsJsonObject("theme")
        item.addProperty("backgroundType", ThemeManager.BackgroundType.WALLPAPER.name)
        item.addProperty("wallpaperBase64", "aGVsbG8=")
        item.add("wallpaperUri", com.google.gson.JsonNull.INSTANCE)
        val imported = ThemeFileManager.importTheme(root.toString()).getOrThrow()
        assertEquals(ThemeManager.BackgroundType.WALLPAPER, imported.backgroundType)
        assertEquals("aGVsbG8=", imported.embeddedWallpaperBase64)
    }

    @Test
    fun import_rejects_wrong_schema() {
        assertTrue(ThemeFileManager.importTheme("{\"schema\":\"other\",\"version\":1}").isFailure)
    }

    @Test
    fun import_rejects_unsupported_version() {
        val raw = ThemeFileManager.export(theme()).replace("\"version\": ${ThemeFileManager.CURRENT_VERSION}", "\"version\": 99")
        assertTrue(ThemeFileManager.importTheme(raw).isFailure)
    }

    @Test
    fun duplicate_id_gets_new_import_id() {
        val imported = ThemeFileManager.importTheme(ThemeFileManager.export(theme()), setOf("user_test")).getOrThrow()
        assertTrue(imported.id.startsWith("imported_"))
    }
}
