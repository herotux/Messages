package org.fossify.messages.activities

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.fossify.messages.helpers.ThemeManager
import java.util.UUID

/** User theme editor with live preview. */
class ThemeBuilderActivity : SimpleActivity() {
    private data class Field(val key: String, val fa: String, val en: String, var color: Int)
    private val fields = mutableListOf<Field>()
    private lateinit var preview: MaterialCardView
    private lateinit var previewTitle: TextView
    private lateinit var previewIncoming: TextView
    private lateinit var previewOutgoing: TextView
    private var editingId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        editingId = intent.getStringExtra("theme_id")
        val existing = editingId?.let { ThemeManager.find(this, it) }
        val colors = existing?.colors ?: ThemeManager.colors(this)
        fields.clear()
        fields += Field("primary", "رنگ اصلی", "Primary", colors.primary)
        fields += Field("accent", "رنگ تأکیدی", "Accent", colors.accent)
        fields += Field("background", "پس‌زمینه", "Background", colors.background)
        fields += Field("surface", "سطح کارت‌ها", "Surface", colors.surface)
        fields += Field("toolbar", "نوار ابزار", "Toolbar", colors.toolbar)
        fields += Field("incoming", "حباب دریافتی", "Incoming bubble", colors.incomingBubble)
        fields += Field("outgoing", "حباب ارسالی", "Outgoing bubble", colors.outgoingBubble)
        fields += Field("text", "متن اصلی", "Text", colors.textPrimary)
        fields += Field("secondary", "متن ثانویه", "Text secondary", colors.textSecondary)
        fields += Field("fab", "دکمه شناور", "FAB", colors.fab)
        build(existing)
    }

    private fun build(existing: ThemeManager.ThemeDefinition?) {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(20), dp(20), dp(20), dp(28))
        }
        root.addView(TextView(this).apply {
            text = if (existing == null) "ساخت تم جدید" else "ویرایش تم"
            textSize = 24f
            setTextColor(ThemeManager.colors(this@ThemeBuilderActivity).textPrimary)
        }, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(14) })

        val name = EditText(this).apply {
            hint = "نام تم"
            setText(existing?.nameFa ?: "")
            textSize = 16f
            setSingleLine(true)
        }
        root.addView(name, LinearLayout.LayoutParams(-1, dp(58)))

        preview = MaterialCardView(this).apply { radius = dp(20).toFloat(); cardElevation = 0f }
        val previewBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(16), dp(16), dp(16)) }
        previewTitle = TextView(this).apply { text = "پیش‌نمایش گفتگو"; textSize = 17f }
        previewIncoming = TextView(this).apply { text = "سلام 👋 این یک پیام دریافتی است"; textSize = 15f; setPadding(dp(12), dp(10), dp(12), dp(10)) }
        previewOutgoing = TextView(this).apply { text = "سلام! تم جدید آماده است 😊"; textSize = 15f; setPadding(dp(12), dp(10), dp(12), dp(10)); gravity = Gravity.END }
        previewBox.addView(previewTitle)
        previewBox.addView(previewIncoming, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, dp(12), dp(28), dp(6)) })
        previewBox.addView(previewOutgoing, LinearLayout.LayoutParams(-1, -2).apply { setMargins(dp(28), dp(6), 0, 0) })
        preview.addView(previewBox)
        root.addView(preview, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, dp(18), 0, dp(12)) })

        fields.forEach { field ->
            val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(6), 0, dp(6)) }
            row.addView(TextView(this).apply { text = field.fa; textSize = 15f }, LinearLayout.LayoutParams(0, dp(52), 1f))
            val swatch = swatch(field.color)
            row.addView(swatch, LinearLayout.LayoutParams(dp(58), dp(42)))
            row.setOnClickListener { chooseColor(field, swatch) }
            root.addView(row)
        }

        root.addView(Button(this).apply {
            text = if (existing == null) "ذخیره تم" else "ذخیره تغییرات"
            setOnClickListener {
                val themeName = name.text.toString().trim().ifEmpty { "تم من" }
                val c = { key: String -> fields.first { it.key == key }.color }
                val colors = ThemeManager.ThemeColors(
                    c("primary"), c("accent"), c("background"), c("surface"), c("text"), c("secondary"),
                    c("incoming"), c("outgoing"), c("toolbar"), c("accent"), c("fab"), c("accent")
                )
                val id = editingId ?: "user_" + UUID.randomUUID().toString()
                ThemeManager.saveUserTheme(this@ThemeBuilderActivity, ThemeManager.ThemeDefinition(id, themeName, themeName, ThemeManager.ThemeSource.USER, colors = colors))
                ThemeManager.select(this@ThemeBuilderActivity, id)
                finish()
            }
        }, LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(16) })

        setContentView(root)
        updatePreview()
    }

    private fun swatch(color: Int): View = View(this).apply {
        background = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            cornerRadius = dp(12).toFloat()
            setColor(color)
        }
    }

    private fun chooseColor(field: Field, swatch: View) {
        val input = EditText(this).apply {
            hint = "#RRGGBB"
            setText(String.format("#%06X", 0xFFFFFF and field.color))
            selectAll()
            setSingleLine(true)
        }
        MaterialAlertDialogBuilder(this).setTitle(field.fa).setView(input)
            .setPositiveButton("اعمال") { _, _ ->
                val parsed = runCatching { Color.parseColor(input.text.toString().trim()) }.getOrNull() ?: return@setPositiveButton
                field.color = parsed
                swatch.background = android.graphics.drawable.GradientDrawable().apply { cornerRadius = dp(12).toFloat(); setColor(parsed) }
                updatePreview()
            }.setNegativeButton("لغو", null).show()
    }

    private fun updatePreview() {
        val get = { key: String -> fields.first { it.key == key }.color }
        preview.setCardBackgroundColor(get("background"))
        previewTitle.setTextColor(get("text"))
        previewIncoming.setTextColor(get("text"))
        previewIncoming.backgroundTintList = ColorStateList.valueOf(get("incoming"))
        previewOutgoing.setTextColor(if (isLight(get("outgoing"))) Color.BLACK else Color.WHITE)
        previewOutgoing.backgroundTintList = ColorStateList.valueOf(get("outgoing"))
    }

    private fun isLight(color: Int): Boolean = (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255.0 > 0.55
}
