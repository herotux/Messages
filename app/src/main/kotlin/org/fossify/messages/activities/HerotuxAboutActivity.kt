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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.fossify.commons.extensions.getProperBackgroundColor
import org.fossify.commons.extensions.getProperPrimaryColor
import org.fossify.commons.extensions.getProperTextColor
import org.fossify.commons.extensions.updateTextColors
import org.fossify.messages.BuildConfig
import org.fossify.messages.helpers.ThemeFileManager
import org.fossify.messages.helpers.ThemeManager
import java.util.UUID

class HerotuxAboutActivity : SimpleActivity() {
    private data class ThemeField(val key: String, val label: String, var color: Int)

    companion object {
        private const val EXTRA_CONTACT_PAGE = "contact_page"
        private const val EXTRA_THEME_BUILDER = "theme_builder"
        private const val EXTRA_THEME_ID = "theme_id"
    }

    private val importThemeFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@registerForActivityResult
        runCatching {
            contentResolver.openInputStream(uri)?.use { it.reader().readText() }
                ?: error("فایل قابل خواندن نیست")
        }.mapCatching { raw -> ThemeFileManager.import(this, raw).getOrThrow() }
            .onSuccess { theme ->
                if (ThemeFileManager.saveImported(this, theme)) {
                    ThemeManager.select(this, theme.id)
                    showThemeImported(theme)
                } else showThemeError("ذخیره تم واردشده انجام نشد")
            }
            .onFailure { showThemeError(it.message ?: "فایل تم معتبر نیست") }
    }

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
        val fields = mutableListOf(
            ThemeField("primary", "رنگ اصلی", source.primary), ThemeField("accent", "رنگ تأکیدی", source.accent),
            ThemeField("background", "پس‌زمینه", source.background), ThemeField("surface", "سطح کارت‌ها", source.surface),
            ThemeField("toolbar", "نوار ابزار", source.toolbar), ThemeField("incoming", "حباب دریافتی", source.incomingBubble),
            ThemeField("outgoing", "حباب ارسالی", source.outgoingBubble), ThemeField("text", "متن اصلی", source.textPrimary),
            ThemeField("secondary", "متن ثانویه", source.textSecondary), ThemeField("fab", "دکمه شناور", source.fab)
        )
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL; setBackgroundColor(ThemeManager.colors(this@HerotuxAboutActivity).background) }
        val toolbar = Toolbar(this).apply { title = if (existing == null) "ساخت تم جدید" else "ویرایش تم"; navigationIcon = getDrawable(androidx.appcompat.R.drawable.abc_ic_ab_back_material); setNavigationOnClickListener { finish() } }
        root.addView(toolbar, LinearLayout.LayoutParams(-1, dp(56)))
        val scroll = ScrollView(this).apply { isFillViewport = true; isVerticalScrollBarEnabled = false; setPadding(dp(20), dp(10), dp(20), dp(28)) }
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(content); root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        val name = EditText(this).apply { hint = "نام تم"; setText(existing?.nameFa ?: ""); textSize = 16f }
        content.addView(name, LinearLayout.LayoutParams(-1, dp(58)))

        val preview = MaterialCardView(this).apply { radius = dp(20).toFloat(); cardElevation = 0f }
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(16), dp(16), dp(16)) }
        val title = TextView(this).apply { text = "پیش‌نمایش گفتگو"; textSize = 17f }
        val incoming = TextView(this).apply { text = "سلام 👋 این یک پیام دریافتی است"; textSize = 15f; setPadding(dp(12), dp(10), dp(12), dp(10)) }
        val outgoing = TextView(this).apply { text = "سلام! تم جدید آماده است 😊"; textSize = 15f; gravity = Gravity.END; setPadding(dp(12), dp(10), dp(12), dp(10)) }
        box.addView(title); box.addView(incoming, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, dp(12), dp(28), dp(6)) }); box.addView(outgoing, LinearLayout.LayoutParams(-1, -2).apply { setMargins(dp(28), dp(6), 0, 0) }); preview.addView(box)
        content.addView(preview, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, dp(16), 0, dp(10)) })

        fun get(key: String) = fields.first { it.key == key }.color
        fun refresh() {
            preview.setCardBackgroundColor(get("background")); title.setTextColor(get("text")); incoming.setTextColor(get("text")); outgoing.setTextColor(if (isLight(get("outgoing"))) Color.BLACK else Color.WHITE)
            incoming.backgroundTintList = ColorStateList.valueOf(get("incoming")); outgoing.backgroundTintList = ColorStateList.valueOf(get("outgoing"))
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
            text = if (existing == null) "ذخیره تم" else "ذخیره تغییرات"; textSize = 16f; gravity = Gravity.CENTER; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE); setPadding(0, dp(14), 0, dp(14)); background = makeSwatch(ThemeManager.colors(this@HerotuxAboutActivity).primary); isClickable = true
            setOnClickListener {
                val themeName = name.text.toString().trim().ifEmpty { "تم من" }; val c = { key: String -> get(key) }
                val colors = ThemeManager.ThemeColors(c("primary"), c("accent"), c("background"), c("surface"), c("text"), c("secondary"), c("incoming"), c("outgoing"), c("toolbar"), c("accent"), c("fab"))
                val id = editingId ?: "user_" + UUID.randomUUID().toString()
                ThemeManager.saveUserTheme(this@HerotuxAboutActivity, ThemeManager.ThemeDefinition(id, themeName, themeName, ThemeManager.ThemeSource.USER, colors = colors)); ThemeManager.select(this@HerotuxAboutActivity, id); finish()
            }
        }
        content.addView(save, LinearLayout.LayoutParams(-1, dp(52)).apply { setMargins(0, dp(16), 0, 0) })
        setContentView(root); refresh()
    }

    private fun chooseThemeColor(field: ThemeField, swatch: View, changed: () -> Unit) {
        val input = EditText(this).apply { hint = "#RRGGBB"; setText(String.format("#%06X", 0xFFFFFF and field.color)); selectAll() }
        MaterialAlertDialogBuilder(this).setTitle(field.label).setView(input).setPositiveButton("اعمال") { _, _ -> runCatching { Color.parseColor(input.text.toString().trim()) }.onSuccess { field.color = it; swatch.background = makeSwatch(it); changed() } }.setNegativeButton("لغو", null).show()
    }

    private fun makeSwatch(color: Int) = GradientDrawable().apply { shape = GradientDrawable.RECTANGLE; cornerRadius = dp(12).toFloat(); setColor(color) }
    private fun isLight(color: Int) = (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255.0 > 0.55

    private fun showThemeImported(theme: ThemeManager.ThemeDefinition) {
        MaterialAlertDialogBuilder(this)
            .setTitle("تم وارد شد")
            .setMessage("«${theme.nameFa}» به کتابخانه تم اضافه و فعال شد.")
            .setPositiveButton("باشه", null)
            .show()
    }

    private fun showThemeError(message: String) {
        MaterialAlertDialogBuilder(this).setTitle("خطا در فایل تم").setMessage(message).setPositiveButton("باشه", null).show()
    }

    private fun renderAboutPage() {
        val bg = getProperBackgroundColor(); val text = getProperTextColor(); val primary = getProperPrimaryColor()
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(bg) }
        val toolbar = Toolbar(this).apply { title = "درباره هما"; setTitleTextColor(text); navigationIcon = getDrawable(androidx.appcompat.R.drawable.abc_ic_ab_back_material); setNavigationOnClickListener { finish() }; elevation = dp(2).toFloat() }
        root.addView(toolbar, LinearLayout.LayoutParams(-1, dp(56)))
        val scroll = ScrollView(this).apply { isFillViewport = true; clipToPadding = false; setPadding(dp(24), dp(24), dp(24), dp(32)) }
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL }
        content.addView(infoText("هما", 34f, true, primary), LinearLayout.LayoutParams(-1, -2)); content.addView(infoText("پیام‌رسانک SMS و MMS", 18f, true, text), LinearLayout.LayoutParams(-1, -2)); content.addView(infoText("توسعه‌یافته توسط HEROTUX", 15f, false, text), LinearLayout.LayoutParams(-1, -2))
        val card = MaterialCardView(this).apply { radius = dp(18).toFloat(); cardElevation = dp(2).toFloat(); setContentPadding(dp(20), dp(18), dp(20), dp(18)); strokeWidth = dp(1); strokeColor = primary; setCardBackgroundColor(bg) }
        val cardText = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }; cardText.addView(infoText("درباره هما", 17f, true, text)); cardText.addView(infoText("هما یک برنامه سبک و حریم‌خصوصی‌محور برای مدیریت پیامک و MMS است.", 15f, false, text)); cardText.addView(infoText("نسخه ${BuildConfig.VERSION_NAME}", 14f, false, text)); card.addView(cardText); content.addView(card, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(28) })
        content.addView(infoText("اطلاعات تماس\nContact information", 16f, true, primary).apply { gravity = Gravity.CENTER; setPadding(dp(12), dp(22), dp(12), dp(12)); isClickable = true; setOnClickListener { startActivity(Intent(this@HerotuxAboutActivity, HerotuxAboutActivity::class.java).putExtra(EXTRA_CONTACT_PAGE, true)) } }, LinearLayout.LayoutParams(-1, -2))
        content.addView(infoText("herotux.github.io", 14f, false, primary).apply { gravity = Gravity.CENTER; setOnClickListener { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://herotux.github.io"))) } }, LinearLayout.LayoutParams(-1, -2))
        content.addView(infoText("HEROTUX · Software Development", 13f, false, text).apply { gravity = Gravity.CENTER; alpha = 0.62f }, LinearLayout.LayoutParams(-1, -2)); scroll.addView(content); root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f)); setContentView(root); updateTextColors(root)
    }

    private fun renderContactPage() {
        val bg = getProperBackgroundColor(); val text = getProperTextColor(); val primary = getProperPrimaryColor()
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(bg) }; val toolbar = Toolbar(this).apply { title = "اطلاعات تماس"; setTitleTextColor(text); navigationIcon = getDrawable(androidx.appcompat.R.drawable.abc_ic_ab_back_material); setNavigationOnClickListener { finish() } }; root.addView(toolbar, LinearLayout.LayoutParams(-1, dp(56)))
        val scroll = ScrollView(this).apply { setPadding(dp(20), dp(20), dp(20), dp(28)) }; val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL; textDirection = View.TEXT_DIRECTION_RTL }
        content.addView(infoText("ارتباط با هما", 24f, true, text).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(-1, -2)); content.addView(infoText("برای پشتیبانی، پیشنهادها و گزارش مشکلات با ما در تماس باشید.", 14f, false, text).apply { gravity = Gravity.CENTER; alpha = 0.75f }, LinearLayout.LayoutParams(-1, -2))
        contactCard(content, "ایمیل", "thefreetux@gmail.com", primary, text) { startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:thefreetux@gmail.com"))) }; contactCard(content, "تلفن پشتیبانی", "09375647544", primary, text) { startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:09375647544"))) }; contactCard(content, "وب‌سایت", "herotux.github.io", primary, text) { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://herotux.github.io"))) }
        scroll.addView(content); root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f)); setContentView(root); updateTextColors(root)
    }

    private fun contactCard(root: LinearLayout, label: String, value: String, primary: Int, text: Int, onClick: () -> Unit) {
        val card = MaterialCardView(this).apply { radius = dp(18).toFloat(); cardElevation = 0f; strokeWidth = dp(1); strokeColor = primary; setCardBackgroundColor(getProperBackgroundColor()); isClickable = true; setOnClickListener { onClick() } }
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(14), dp(18), dp(14)) }; content.addView(infoText(label, 13f, false, text)); content.addView(infoText(value, 17f, true, primary)); card.addView(content); root.addView(card, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })
    }

    private fun infoText(value: String, size: Float, bold: Boolean, color: Int) = TextView(this).apply { text = value; textSize = size; setTextColor(color); if (bold) typeface = Typeface.DEFAULT_BOLD; setPadding(0, dp(5), 0, dp(5)) }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}