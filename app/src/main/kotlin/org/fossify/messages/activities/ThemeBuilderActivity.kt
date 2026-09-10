package org.fossify.messages.activities

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.fossify.messages.BuildConfig
import org.fossify.messages.helpers.ThemeFileManager
import org.fossify.messages.helpers.ThemeManager
import java.util.UUID

/** Standalone editor for creating and editing Herotux themes. */
class ThemeBuilderActivity : SimpleActivity() {
    private data class ThemeField(val key: String, val label: String, var color: Int)

    companion object {
        const val EXTRA_THEME_ID = "theme_id"
    }

    private var pendingExportTheme: ThemeManager.ThemeDefinition? = null

    private val importThemeFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@registerForActivityResult
        runCatching {
            contentResolver.openInputStream(uri)?.use { it.reader().readText() } ?: error("فایل قابل خواندن نیست")
        }.mapCatching {
            ThemeFileManager.import(this, it).getOrThrow()
        }.onSuccess { theme ->
            if (ThemeFileManager.saveImported(this, theme)) {
                ThemeManager.select(this, theme.id)
                showThemeImported(theme)
            } else {
                showThemeError("ذخیره تم واردشده انجام نشد")
            }
        }.onFailure { showThemeError(it.message ?: "فایل تم معتبر نیست") }
    }

    private val createThemeFile = registerForActivityResult(ActivityResultContracts.CreateDocument(ThemeFileManager.MIME_TYPE)) { uri ->
        val theme = pendingExportTheme ?: return@registerForActivityResult
        pendingExportTheme = null
        if (uri == null) return@registerForActivityResult
        runCatching {
            contentResolver.openOutputStream(uri)?.use { output ->
                output.write(ThemeFileManager.export(theme).toByteArray(Charsets.UTF_8))
            } ?: error("فایل قابل ایجاد نیست")
        }.onFailure { showThemeError(it.message ?: "ذخیره فایل تم انجام نشد") }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        renderThemeBuilder()
    }

    private fun renderThemeBuilder() {
        val editingId = intent.getStringExtra(EXTRA_THEME_ID)
        val existing = editingId?.let { ThemeManager.find(this, it) }
        val source = existing?.colors ?: ThemeManager.colors(this)
        val fields = mutableListOf(
            ThemeField("primary", "رنگ اصلی", source.primary),
            ThemeField("accent", "رنگ تأکیدی", source.accent),
            ThemeField("background", "پس‌زمینه", source.background),
            ThemeField("surface", "سطح کارت‌ها", source.surface),
            ThemeField("toolbar", "نوار ابزار", source.toolbar),
            ThemeField("incoming", "حباب دریافتی", source.incomingBubble),
            ThemeField("outgoing", "حباب ارسالی", source.outgoingBubble),
            ThemeField("text", "متن اصلی", source.textPrimary),
            ThemeField("secondary", "متن ثانویه", source.textSecondary),
            ThemeField("fab", "دکمه شناور", source.fab)
        )

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(source.background)
        }
        val toolbar = Toolbar(this).apply {
            title = if (existing == null) "ساخت تم جدید" else "ویرایش تم"
            navigationIcon = getDrawable(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
            setNavigationOnClickListener { finish() }
        }
        root.addView(toolbar, LinearLayout.LayoutParams(-1, dp(56)))

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            isVerticalScrollBarEnabled = false
            setPadding(dp(20), dp(10), dp(20), dp(28))
        }
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val fileActions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val import = TextView(this).apply {
            text = "وارد کردن .homa-theme"
            textSize = 14f
            gravity = Gravity.CENTER
            isClickable = true
            setPadding(dp(8), dp(10), dp(8), dp(10))
            background = makeSwatch(source.surface)
            setOnClickListener { importThemeFile.launch(arrayOf(ThemeFileManager.MIME_TYPE, "application/octet-stream", "*/*")) }
        }
        fileActions.addView(import, LinearLayout.LayoutParams(0, dp(48), 1f))
        content.addView(fileActions, LinearLayout.LayoutParams(-1, dp(48)).apply { setMargins(0, 0, 0, dp(8)) })

        val name = EditText(this).apply {
            hint = "نام تم"
            setText(existing?.nameFa ?: "")
            textSize = 16f
        }
        content.addView(name, LinearLayout.LayoutParams(-1, dp(58)))

        val preview = MaterialCardView(this).apply { radius = dp(20).toFloat(); cardElevation = 0f }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        val title = TextView(this).apply { text = "پیش‌نمایش گفتگو"; textSize = 17f }
        val incoming = TextView(this).apply {
            text = "سلام 👋 این یک پیام دریافتی است"
            textSize = 15f
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        val outgoing = TextView(this).apply {
            text = "سلام! تم جدید آماده است 😊"
            textSize = 15f
            gravity = Gravity.END
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        box.addView(title)
        box.addView(incoming, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, dp(12), dp(28), dp(6)) })
        box.addView(outgoing, LinearLayout.LayoutParams(-1, -2).apply { setMargins(dp(28), dp(6), 0, 0) })
        preview.addView(box)
        content.addView(preview, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, dp(16), 0, dp(10)) })

        fun get(key: String) = fields.first { it.key == key }.color
        fun refresh() {
            preview.setCardBackgroundColor(get("background"))
            title.setTextColor(get("text"))
            incoming.setTextColor(get("text"))
            outgoing.setTextColor(if (isLight(get("outgoing"))) Color.BLACK else Color.WHITE)
            incoming.backgroundTintList = ColorStateList.valueOf(get("incoming"))
            outgoing.backgroundTintList = ColorStateList.valueOf(get("outgoing"))
        }

        fields.forEach { field ->
            val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(5), 0, dp(5)) }
            row.addView(TextView(this).apply { text = field.label; textSize = 15f }, LinearLayout.LayoutParams(0, dp(52), 1f))
            val swatch = View(this).apply { background = makeSwatch(field.color) }
            row.addView(swatch, LinearLayout.LayoutParams(dp(58), dp(42)))
            row.setOnClickListener { chooseThemeColor(field, swatch) { refresh() } }
            content.addView(row)
        }

        val save = TextView(this).apply {
            text = if (existing == null) "ذخیره تم" else "ذخیره تغییرات"
            textSize = 16f
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setPadding(0, dp(14), 0, dp(14))
            background = makeSwatch(source.primary)
            isClickable = true
            setOnClickListener {
                val themeName = name.text.toString().trim().ifEmpty { "تم من" }
                val c = { key: String -> get(key) }
                val colors = ThemeManager.ThemeColors(
                    c("primary"), c("accent"), c("background"), c("surface"), c("text"),
                    c("secondary"), c("incoming"), c("outgoing"), c("toolbar"), c("accent"), c("fab")
                )
                val id = editingId ?: "user_" + UUID.randomUUID().toString()
                ThemeManager.saveUserTheme(
                    this@ThemeBuilderActivity,
                    ThemeManager.ThemeDefinition(id, themeName, themeName, ThemeManager.ThemeSource.USER, colors = colors)
                )
                ThemeManager.select(this@ThemeBuilderActivity, id)
                finish()
            }
        }
        content.addView(save, LinearLayout.LayoutParams(-1, dp(52)).apply { setMargins(0, dp(16), 0, 0) })

        if (existing != null) {
            val export = TextView(this).apply {
                text = "خروجی فایل .homa-theme"
                textSize = 15f
                gravity = Gravity.CENTER
                setPadding(0, dp(12), 0, dp(12))
                isClickable = true
                setOnClickListener { exportThemeFile(existing) }
            }
            val share = TextView(this).apply {
                text = "اشتراک‌گذاری تم"
                textSize = 15f
                gravity = Gravity.CENTER
                setPadding(0, dp(12), 0, dp(12))
                isClickable = true
                setOnClickListener { shareThemeFile(existing) }
            }
            content.addView(export, LinearLayout.LayoutParams(-1, dp(48)).apply { topMargin = dp(8) })
            content.addView(share, LinearLayout.LayoutParams(-1, dp(48)))
        }

        setContentView(root)
        refresh()
    }

    private fun chooseThemeColor(field: ThemeField, swatch: View, changed: () -> Unit) {
        val input = EditText(this).apply {
            hint = "#RRGGBB"
            setText(String.format("#%06X", 0xFFFFFF and field.color))
            selectAll()
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(field.label)
            .setView(input)
            .setPositiveButton("اعمال") { _, _ ->
                runCatching { Color.parseColor(input.text.toString().trim()) }
                    .onSuccess { field.color = it; swatch.background = makeSwatch(it); changed() }
            }
            .setNegativeButton("لغو", null)
            .show()
    }

    private fun exportThemeFile(theme: ThemeManager.ThemeDefinition) {
        pendingExportTheme = theme
        createThemeFile.launch("${theme.nameEn.ifBlank { "theme" }}${ThemeFileManager.FILE_EXTENSION}")
    }

    private fun shareThemeFile(theme: ThemeManager.ThemeDefinition) {
        val cacheFile = runCatching {
            val safeName = theme.nameEn.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "theme" }
            java.io.File(cacheDir, "$safeName${ThemeFileManager.FILE_EXTENSION}").apply {
                writeText(ThemeFileManager.export(theme), Charsets.UTF_8)
            }
        }.getOrNull()
        if (cacheFile == null) {
            showThemeError("ساخت فایل اشتراک‌گذاری انجام نشد")
            return
        }
        val uri = androidx.core.content.FileProvider.getUriForFile(this, "${BuildConfig.APPLICATION_ID}.provider", cacheFile)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = ThemeFileManager.MIME_TYPE
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(send, "اشتراک‌گذاری تم"))
    }

    private fun showThemeImported(theme: ThemeManager.ThemeDefinition) {
        MaterialAlertDialogBuilder(this)
            .setTitle("تم وارد شد")
            .setMessage("«${theme.nameFa}» به کتابخانه تم اضافه و فعال شد.")
            .setPositiveButton("باشه", null)
            .show()
    }

    private fun showThemeError(message: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle("خطا در فایل تم")
            .setMessage(message)
            .setPositiveButton("باشه", null)
            .show()
    }

    private fun makeSwatch(color: Int): android.graphics.drawable.GradientDrawable =
        android.graphics.drawable.GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(14).toFloat()
            setStroke(dp(1), 0x33000000)
        }

    private fun isLight(color: Int): Boolean {
        val r = Color.red(color) / 255f
        val g = Color.green(color) / 255f
        val b = Color.blue(color) / 255f
        val luminance = 0.2126f * r + 0.7152f * g + 0.0722f * b
        return luminance > 0.55f
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
