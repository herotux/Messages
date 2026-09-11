package org.fossify.messages.helpers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeFileManagerTest {
    private fun theme(id: String = "user_test") = ThemeManager.ThemeDefinition(
        id = id,
        nameFa = "تم آزمایشی",
        nameEn = "Test Theme",
        source = ThemeManager.ThemeSource.USER,
        colors = ThemeManager.ThemeColors(
            primary = 0xFF388E3C.toInt(),
            accent = 0xFF4CAF50.toInt(),
            background = 0xFF161616.toInt(),
            surface = 0xFF242424.toInt(),
            textPrimary = 0xFFFFFFFF.toInt(),
            textSecondary = 0xFFBDBDBD.toInt(),
            incomingBubble = 0xFF2A2A2A.toInt(),
            outgoingBubble = 0xFF388E3C.toInt(),
            toolbar = 0xFF388E3C.toInt(),
            tab = 0xFF388E3C.toInt(),
            fab = 0xFF4CAF50.toInt(),
            divider = 0x33808080
        )
    )

    @Test
    fun export_contains_versioned_schema() {
        val raw = ThemeFileManager.export(theme())
        assertTrue(raw.contains("\"schema\": \"homa-theme\""))
        assertTrue(raw.contains("\"version\": ${ThemeFileManager.CURRENT_VERSION}"))
        assertTrue(raw.contains("\"colors\""))
    }

    @Test
    fun export_import_round_trip_preserves_theme_data() {
        val original = theme()
        val imported = ThemeFileManager.importTheme(ThemeFileManager.export(original)).getOrThrow()
        assertEquals(original.id, imported.id)
        assertEquals(original.nameFa, imported.nameFa)
        assertEquals(original.nameEn, imported.nameEn)
        assertEquals(original.colors, imported.colors)
        assertEquals(ThemeManager.ThemeSource.IMPORTED, imported.source)
        assertEquals(ThemeFileManager.CURRENT_VERSION, imported.version)
    }

    @Test
    fun import_rejects_wrong_schema() {
        val result = ThemeFileManager.importTheme("{\"schema\":\"other\",\"version\":1}")
        assertTrue(result.isFailure)
    }

    @Test
    fun import_rejects_unsupported_version() {
        val raw = ThemeFileManager.export(theme()).replace(
            "\"version\": ${ThemeFileManager.CURRENT_VERSION}",
            "\"version\": 99"
        )
        assertTrue(ThemeFileManager.importTheme(raw).isFailure)
    }

    @Test
    fun duplicate_id_gets_new_import_id() {
        val imported = ThemeFileManager.importTheme(ThemeFileManager.export(theme()), setOf("user_test")).getOrThrow()
        assertTrue(imported.id.startsWith("imported_"))
    }
}
