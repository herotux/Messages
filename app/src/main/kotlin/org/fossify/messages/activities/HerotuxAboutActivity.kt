package org.fossify.messages.activities

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.fossify.commons.extensions.getProperBackgroundColor
import org.fossify.commons.extensions.getProperPrimaryColor
import org.fossify.commons.extensions.getProperTextColor
import org.fossify.commons.extensions.updateTextColors
import org.fossify.messages.BuildConfig
import org.fossify.messages.helpers.ThemeManager
import java.util.UUID

class HerotuxAboutActivity : SimpleActivity() {
    companion object { private const val EXTRA_CONTACT_PAGE = "contact_page"; private const val EXTRA_THEME_BUILDER = "theme_builder"; private const val EXTRA_THEME_ID = "theme_id" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        when {
            intent.getBooleanExtra(EXTRA_THEME_BUILDER, false) -> renderThemeBuilder()
            intent.getBooleanExtra(EXTRA_CONTACT_PAGE, false) -> renderContactPage()
            else -> renderAboutPage()
        }
    }

    private fun renderThemeBuilder() {
        val editingId = intent.getStringExtra(EXTRA_THEME_ID)
        val existing = editingId?.let { ThemeManager.find(this, it) }
        val source = existing?.colors ?: ThemeManager.colors(this)
        data class Field(val key: String, val fa: String, var color: Int)
        val fields = mutableListOf(
            Field("primary", "رنگ اصلی", source.primary), Field("accent", "رنگ تأکیدی", source.accent),
            Field("background", "پس‌زمینه", source.background), Field("surface", "سطح کارت‌ها", source.surface),
            Field("toolbar", "نوار ابزار", source.toolbar), Field("incoming", "حباب دریافتی", source.incomingBubble),
            Field("outgoing", "حباب ارسالی", source.outgoingBubble), Field("text", "متن اصلی", source.textPrimary),
            Field("secondary", "متن ثانویه", source.textSecondary), Field("fab", "دکمه شناور", source.fab)
        )
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL; setPadding(dp(20), dp(20), dp(20), dp(28)) }
        val toolbar = Toolbar(this).apply {
            title = if (existing == null) "ساخت تم جدید" else "ویرایش تم"
            navigationIcon = getDrawable(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
            setNavigationOnClickListener { finish() }
        }
        root.addView(toolbar, LinearLayout.LayoutParams(-1, dp(56)))
        val scroll = ScrollView(this).apply { isFillViewport = true; isVerticalScrollBarEnabled = false }
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dp(10), 0, 0) }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val name = EditText(this).apply { hint = "نام تم"; setText(existing?.nameFa ?: ""); textSize = 16f }
        content.addView(name, LinearLayout.LayoutParams(-1, dp(58)))
        val preview = MaterialCardView(this).apply { radius = dp(20).toFloat(); cardElevation = 0f }
        val previewBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(16), dp(16), dp(16)) }
        val previewTitle = TextView(this).apply { text = "پیش‌نمایش گفتگو"; textSize = 17f }
        val incoming = TextView(this).apply { text = "سلام 👋 این یک پیام دریافتی است"; textSize = 15f; setPadding(dp(12), dp(10), dp(12), dp(10)) }
        val outgoing = TextView(this).apply { text = "سلام! تم جدید آماده است 😊"; textSize = 15f; setPadding(dp(12), dp(10), dp(12), dp(10)); gravity = Gravity.END }
        previewBox.addView(previewTitle)
        previewBox.addView(incoming, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, dp(12), dp(28), dp(6)) })
        previewBox.addView(outgoing, LinearLayout.LayoutParams(-1, -2).apply { setMargins(dp(28), dp(6), 0, 0) })
        preview.addView(previewBox)
        content.addView(preview, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, dp(16), 0, dp(10)) })

        fun get(key: String) = fields.first { it.key == key }.color
        fun refresh() {
            preview.setCardBackgroundColor(get("background")); previewTitle.setTextColor(get("text")); incoming.setTextColor(get("text")); outgoing.setTextColor(if (isLight(get("outgoing"))) Color.BLACK else Color.WHITE)
            incoming.backgroundTintList = ColorStateList.valueOf(get("incoming")); outgoing.backgroundTintList = ColorStateList.valueOf(get("outgoing"))
        }
        fields.forEach { field ->
            val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(5), 0, dp(5)) }
            row.addView(TextView(this).apply { text = field.fa; textSize = 15f }, LinearLayout.LayoutParams(0, dp(52), 1f))
            val swatch = View(this).apply { background = swatch(field.color) }
            row.addView(swatch, LinearLayout.LayoutParams(dp(58), dp(42)))
            row.setOnClickListener { chooseThemeColor(field, swatch) { refresh() } }
            content.addView(row)
        }
        content.addView(TextView(this).apply {
            text = if (existing == null) "ذخیره تم" else "ذخیره تغییرات"
            textSize = 16f; gravity = Gravity.CENTER; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE); background = GradientDrawable().apply { cornerRadius = dp(14).toFloat(); setColor(ThemeManager.colors(this@HerotuxAboutActivity).primary) }
            setPadding(0, dp(14), 0, dp(14)); isClickable = true
            setOnClickListener {
                val themeName = name.text.toString().trim().ifEmpty { "تم من" }
                val c = { key: String -> get(key) }
                val colors = ThemeManager.ThemeColors(c("primary"), c("accent"), c("background"), c("surface"), c("text"), c("secondary"), c("incoming"), c("outgoing"), c("toolbar"), c("accent"), c("fab"))
                val id = editingId ?: "user_" + UUID.randomUUID().toString()
                ThemeManager.saveUserTheme(this@HerotuxAboutActivity, ThemeManager.ThemeDefinition(id, themeName, themeName, ThemeManager.ThemeSource.USER, colors = colors))
                ThemeManager.select(this@HerotuxAboutActivity, id)
                finish()
            }
        }, LinearLayout.LayoutParams(-1, dp(52)).apply { setMargins(0, dp(16), 0, dp(10)) })
        setContentView(root)
        if (!org.fossify.messages.extensions.config.useEnglish) applyPersianFont(root)
        refresh()
    }

    private fun chooseThemeColor(field: Any, swatchView: View, changed: () -> Unit) {
        @Suppress("UNCHECKED_CAST") val f = field as dynamic
        val input = EditText(this).apply { hint = "#RRGGBB"; setText(String.format("#%06X", 0xFFFFFF and f.color)); selectAll() }
        MaterialAlertDialogBuilder(this).setTitle(f.fa).setView(input).setPositiveButton("اعمال") { _, _ ->
            runCatching { Color.parseColor(input.text.toString().trim()) }.onSuccess { color -> f.color = color; swatchView.background = swatch(color); changed() }
        }.setNegativeButton("لغو", null).show()
    }

    private fun swatch(color: Int) = GradientDrawable().apply { shape = GradientDrawable.RECTANGLE; cornerRadius = dp(12).toFloat(); setColor(color) }
    private fun isLight(color: Int): Boolean = (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255.0 > 0.55

    private fun renderAboutPage() {
        val backgroundColor = getProperBackgroundColor(); val textColor = getProperTextColor(); val primaryColor = getProperPrimaryColor(); val horizontalPadding = dp(24)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(backgroundColor) }
        val toolbar = Toolbar(this).apply { title = "درباره هما"; setTitleTextColor(textColor); navigationIcon = getDrawable(androidx.appcompat.R.drawable.abc_ic_ab_back_material); setNavigationOnClickListener { finish() }; elevation = dp(2).toFloat() }
        root.addView(toolbar, LinearLayout.LayoutParams(-1, dp(56)))
        val scroll = ScrollView(this).apply { isFillViewport = true; clipToPadding = false; setPadding(horizontalPadding, dp(24), horizontalPadding, dp(32)) }
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL }
        content.addView(TextView(this).apply { text = "هما"; textSize = 34f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); setTextColor(primaryColor); gravity = Gravity.CENTER }, LinearLayout.LayoutParams(-1, -2))
        content.addView(TextView(this).apply { text = "پیام‌رسانک SMS و MMS"; textSize = 18f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); setTextColor(textColor); gravity = Gravity.CENTER; setPadding(0, dp(6), 0, 0) }, LinearLayout.LayoutParams(-1, -2))
        content.addView(TextView(this).apply { text = "توسعه‌یافته توسط HEROTUX"; textSize = 15f; setTextColor(textColor); gravity = Gravity.CENTER; alpha = 0.82f; setPadding(0, dp(4), 0, 0) }, LinearLayout.LayoutParams(-1, -2))
        val card = MaterialCardView(this).apply { radius = dp(18).toFloat(); cardElevation = dp(2).toFloat(); setContentPadding(dp(20), dp(18), dp(20), dp(18)); strokeWidth = dp(1); strokeColor = primaryColor; setCardBackgroundColor(backgroundColor) }
        val cardText = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        cardText.addView(infoText("درباره هما", 17f, true, textColor)); cardText.addView(infoText("هما یک برنامه سبک و حریم‌خصوصی‌محور برای مدیریت پیامک و MMS است.", 15f, false, textColor)); cardText.addView(infoText("نسخه ${BuildConfig.VERSION_NAME}", 14f, false, textColor)); card.addView(cardText)
        content.addView(card, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(28) })
        content.addView(TextView(this).apply { text = "اطلاعات تماس\nContact information"; textSize = 16f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); setTextColor(primaryColor); gravity = Gravity.CENTER; setPadding(dp(12), dp(22), dp(12), dp(12)); isClickable = true; isFocusable = true; setOnClickListener { startActivity(Intent(this@HerotuxAboutActivity, HerotuxAboutActivity::class.java).putExtra(EXTRA_CONTACT_PAGE, true)) } }, LinearLayout.LayoutParams(-1, -2))
        content.addView(TextView(this).apply { text = "herotux.github.io"; textSize = 14f; setTextColor(primaryColor); gravity = Gravity.CENTER; setPadding(dp(12), dp(8), dp(12), dp(12)); isClickable = true; isFocusable = true; setOnClickListener { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://herotux.github.io"))) } }, LinearLayout.LayoutParams(-1, -2))
        content.addView(TextView(this).apply { text = "HEROTUX · Software Development"; textSize = 13f; setTextColor(textColor); alpha = 0.62f; gravity = Gravity.CENTER; setPadding(0, dp(12), 0, 0) }, LinearLayout.LayoutParams(-1, -2))
        scroll.addView(content); root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f)); setContentView(root); updateTextColors(root)
    }

    private fun renderContactPage() {
        val backgroundColor = getProperBackgroundColor(); val textColor = getProperTextColor(); val primaryColor = getProperPrimaryColor()
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(backgroundColor) }
        val toolbar = Toolbar(this).apply { title = "اطلاعات تماس"; setTitleTextColor(textColor); navigationIcon = getDrawable(androidx.appcompat.R.drawable.abc_ic_ab_back_material); setNavigationOnClickListener { finish() } }
        root.addView(toolbar, LinearLayout.LayoutParams(-1, dp(56)))
        val scroll = ScrollView(this).apply { setPadding(dp(20), dp(20), dp(20), dp(28)) }
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL; textDirection = View.TEXT_DIRECTION_RTL }
        content.addView(TextView(this).apply { text = "ارتباط با هما"; textSize = 24f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); setTextColor(textColor); gravity = Gravity.CENTER }, LinearLayout.LayoutParams(-1, -2))
        content.addView(TextView(this).apply { text = "برای پشتیبانی، پیشنهادها و گزارش مشکلات با ما در تماس باشید."; textSize = 14f; setTextColor(textColor); alpha = 0.75f; gravity = Gravity.CENTER; setPadding(0, dp(8), 0, dp(24)) }, LinearLayout.LayoutParams(-1, -2))
        contactCard(content, "ایمیل", "thefreetux@gmail.com", primaryColor, textColor) { startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:thefreetux@gmail.com"))) }
        contactCard(content, "تلفن پشتیبانی", "09375647544", primaryColor, textColor) { startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:09375647544"))) }
        contactCard(content, "وب‌سایت", "herotux.github.io", primaryColor, textColor) { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://herotux.github.io"))) }
        scroll.addView(content); root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f)); setContentView(root); updateTextColors(root)
    }

    private fun contactCard(root: LinearLayout, label: String, value: String, primaryColor: Int, textColor: Int, onClick: () -> Unit) {
        val card = MaterialCardView(this).apply { radius = dp(18).toFloat(); cardElevation = 0f; strokeWidth = dp(1); strokeColor = primaryColor; setCardBackgroundColor(getProperBackgroundColor()); isClickable = true; isFocusable = true; setOnClickListener { onClick() } }
        val text = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(14), dp(18), dp(14)) }
        text.addView(TextView(this).apply { this.text = label; textSize = 13f; setTextColor(textColor); alpha = 0.7f })
        text.addView(TextView(this).apply { this.text = value; textSize = 17f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); setTextColor(primaryColor); setPadding(0, dp(4), 0, 0) })
        card.addView(text); root.addView(card, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })
    }

    private fun infoText(text: String, size: Float, bold: Boolean, color: Int): TextView = TextView(this).apply { this.text = text; textSize = size; setTextColor(color); if (bold) typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); setPadding(0, dp(5), 0, dp(5)) }
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
