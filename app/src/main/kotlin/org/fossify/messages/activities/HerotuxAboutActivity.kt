package org.fossify.messages.activities

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import com.google.android.material.card.MaterialCardView
import org.fossify.commons.extensions.getProperBackgroundColor
import org.fossify.commons.extensions.getProperPrimaryColor
import org.fossify.commons.extensions.getProperTextColor
import org.fossify.commons.extensions.updateTextColors
import org.fossify.messages.BuildConfig
import org.fossify.messages.helpers.ThemeManager

class HerotuxAboutActivity : SimpleActivity() {
    companion object {
        private const val EXTRA_CONTACT_PAGE = "contact_page"
        private const val EXTRA_THEME_BUILDER = "theme_builder"
        private const val EXTRA_THEME_ID = "theme_id"
    }

    private var themeFilter = 0
    private var themeQuery = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        when {
            intent.getBooleanExtra(EXTRA_THEME_BUILDER, false) -> {
                val id = intent.getStringExtra(EXTRA_THEME_ID)
                if (id == null) renderThemeLibrary() else startActivity(Intent(this, ThemeBuilderActivity::class.java).putExtra(ThemeBuilderActivity.EXTRA_THEME_ID, id).also { finish() })
            }
            intent.getBooleanExtra(EXTRA_CONTACT_PAGE, false) -> renderContactPage()
            else -> renderAboutPage()
        }
    }

    private fun renderThemeLibrary() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutDirection = if (config.useEnglish) View.LAYOUT_DIRECTION_LTR else View.LAYOUT_DIRECTION_RTL; setBackgroundColor(getProperBackgroundColor()) }
        val toolbar = Toolbar(this).apply { title = tr("کتابخانه تم", "Theme library"); setTitleTextColor(getProperTextColor()); navigationIcon = getDrawable(androidx.appcompat.R.drawable.abc_ic_ab_back_material); setNavigationOnClickListener { finish() } }
        root.addView(toolbar, LinearLayout.LayoutParams(-1, dp(56)))
        val scroll = ScrollView(this).apply { isFillViewport = true; isVerticalScrollBarEnabled = false }
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(12), dp(18), dp(28)) }
        scroll.addView(content); root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f)); setContentView(root)

        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        actions.addView(libButton(tr("＋ ساخت تم", "+ Create theme")) { startActivity(Intent(this@HerotuxAboutActivity, ThemeBuilderActivity::class.java).putExtra("create_theme", true)) }, LinearLayout.LayoutParams(0, dp(46), 1f))
        actions.addView(libButton(tr("مرتب‌سازی", "Sort")) { showThemeSortDialog() }, LinearLayout.LayoutParams(0, dp(46), 1f).apply { marginStart = dp(8) })
        content.addView(actions)

        val active = ThemeManager.activeTheme(this)
        val activeCard = MaterialCardView(this).apply { radius = dp(18).toFloat(); strokeWidth = dp(2); strokeColor = active.colors.accent; setCardBackgroundColor(active.colors.surface) }
        val activeBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(14), dp(16), dp(14)) }
        activeBox.addView(TextView(this).apply { text = tr("تم فعال", "Active theme"); textSize = 12f; setTextColor(active.colors.accent); typeface = Typeface.DEFAULT_BOLD })
        activeBox.addView(TextView(this).apply { text = if (config.useEnglish) active.nameEn else active.nameFa; textSize = 20f; setTextColor(active.colors.textPrimary); typeface = Typeface.DEFAULT_BOLD; setPadding(0, dp(4), 0, dp(4)) })
        activeBox.addView(TextView(this).apply { text = "✓ ${tr("در حال استفاده", "Currently applied")}  ·  ${active.id}"; textSize = 12f; setTextColor(active.colors.textSecondary) })
        activeCard.addView(activeBox); activeCard.setOnClickListener { showThemeActions(active) }
        content.addView(activeCard, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) })

        val search = EditText(this).apply { hint = tr("جستجوی نام تم…", "Search themes…"); singleLine = true; setPadding(dp(14), 0, dp(14), 0); setBackgroundColor(active.colors.surface) }
        content.addView(search, LinearLayout.LayoutParams(-1, dp(48)).apply { bottomMargin = dp(8) })
        val filters = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf(tr("همه", "All"), tr("ساخته من", "My themes"), tr("واردشده", "Imported"), tr("پسندیده", "Favorites")).forEachIndexed { i, label -> filters.addView(libButton(label) { themeFilter = i; renderLibraryCards(content, search.text.toString()) }, LinearLayout.LayoutParams(0, dp(42), 1f).apply { if (i > 0) marginStart = dp(4) }) }
        content.addView(filters, LinearLayout.LayoutParams(-1, dp(44)))
        search.addTextChangedListener(object : android.text.TextWatcher { override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) = Unit; override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) { themeQuery = s?.toString().orEmpty(); renderLibraryCards(content, themeQuery) }; override fun afterTextChanged(s: android.text.Editable?) = Unit })
        renderLibraryCards(content, "")
        if (!config.useEnglish) applyPersianFont(root)
    }

    private fun renderLibraryCards(content: LinearLayout, query: String) {
        while (content.childCount > 4) content.removeViewAt(4)
        val all = ThemeManager.queryThemes(this, query, ThemeManager.librarySort(this))
        val filtered = when (themeFilter) { 1 -> all.filter { it.source == ThemeManager.ThemeSource.USER }; 2 -> all.filter { it.source == ThemeManager.ThemeSource.IMPORTED }; 3 -> all.filter { ThemeManager.isFavorite(this, it.id) }; else -> all }
        val groups = listOf(ThemeManager.ThemeSource.BUILT_IN to tr("تم‌های آماده", "Built-in themes"), ThemeManager.ThemeSource.USER to tr("تم‌های من", "My themes"), ThemeManager.ThemeSource.IMPORTED to tr("تم‌های واردشده", "Imported themes"), ThemeManager.ThemeSource.COMMUNITY to tr("تم‌های جامعه", "Community themes"))
        var count = 0
        groups.forEach { (source, title) -> val group = filtered.filter { it.source == source }; if (group.isNotEmpty()) { content.addView(section(title)); group.forEach { theme -> content.addView(themeCard(theme), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) }); count++ } } }
        if (count == 0) content.addView(section(tr("تمی پیدا نشد", "No themes found")))
    }

    private fun themeCard(theme: ThemeManager.ThemeDefinition): MaterialCardView {
        val selected = ThemeManager.selectedThemeId(this) == theme.id
        return MaterialCardView(this).apply {
            radius = dp(18).toFloat(); strokeWidth = if (selected) dp(2) else dp(1); strokeColor = if (selected) theme.colors.accent else getProperTextColor(); setCardBackgroundColor(theme.colors.surface)
            setOnClickListener { ThemeManager.select(this@HerotuxAboutActivity, theme.id); renderThemeLibrary() }; setOnLongClickListener { showThemeActions(theme); true }
            val box = LinearLayout(this@HerotuxAboutActivity).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12), dp(12), dp(12), dp(12)) }
            box.addView(TextView(this@HerotuxAboutActivity).apply { text = if (config.useEnglish) theme.nameEn else theme.nameFa; textSize = 17f; setTextColor(theme.colors.textPrimary); typeface = Typeface.DEFAULT_BOLD })
            box.addView(TextView(this@HerotuxAboutActivity).apply { text = "Aa   ${theme.nameEn}"; textSize = 13f; setTextColor(theme.colors.textPrimary); setBackgroundColor(theme.colors.background); setPadding(dp(10), dp(10), dp(10), dp(10)) }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(6) })
            box.addView(TextView(this@HerotuxAboutActivity).apply { text = "● ${tr("اصلی", "Primary")}   ● ${tr("تأکیدی", "Accent")}   ● ${tr("حباب", "Bubble")}"; textSize = 12f; setTextColor(theme.colors.textSecondary); setPadding(0, dp(8), 0, 0) })
            box.addView(TextView(this@HerotuxAboutActivity).apply { text = if (selected) "✓ ${tr("فعال", "Active")}" else if (ThemeManager.isFavorite(this@HerotuxAboutActivity, theme.id)) "★ ${tr("پسندیده", "Favorite")}" else ""; textSize = 12f; setTextColor(theme.colors.accent) })
            addView(box)
        }
    }

    private fun showThemeSortDialog() {
        val sorts = arrayOf(tr("پسندیده‌ها اول", "Favorites first"), tr("نام صعودی", "Name A–Z"), tr("نام نزولی", "Name Z–A"), tr("پیش‌فرض", "Default"))
        val values = arrayOf(ThemeManager.ThemeSort.FAVORITES_FIRST, ThemeManager.ThemeSort.NAME_ASC, ThemeManager.ThemeSort.NAME_DESC, ThemeManager.ThemeSort.DEFAULT)
        val current = ThemeManager.librarySort(this); val checked = values.indexOf(current).coerceAtLeast(0)
        AlertDialog.Builder(this).setTitle(tr("مرتب‌سازی تم‌ها", "Sort themes")).setSingleChoiceItems(sorts, checked) { dialog, which -> ThemeManager.setLibrarySort(this, values[which]); dialog.dismiss(); renderThemeLibrary() }.setNegativeButton(tr("لغو", "Cancel"), null).show()
    }

    private fun showThemeActions(theme: ThemeManager.ThemeDefinition) {
        val builtIn = theme.source == ThemeManager.ThemeSource.BUILT_IN
        val items = if (builtIn) arrayOf(tr("اعمال", "Apply"), tr("تکثیر", "Duplicate"), tr("پسندیده", "Favorite")) else arrayOf(tr("اعمال", "Apply"), tr("ویرایش", "Edit"), tr("تکثیر", "Duplicate"), tr("پسندیده", "Favorite"))
        AlertDialog.Builder(this).setTitle(if (config.useEnglish) theme.nameEn else theme.nameFa).setItems(items) { _, which -> when (which) { 0 -> { ThemeManager.select(this, theme.id); renderThemeLibrary() }; 1 -> if (builtIn) duplicate(theme) else startActivity(Intent(this, ThemeBuilderActivity::class.java).putExtra(ThemeBuilderActivity.EXTRA_THEME_ID, theme.id)); 2 -> if (builtIn) toggleFavorite(theme) else duplicate(theme); 3 -> if (!builtIn) toggleFavorite(theme) } }.show()
    }

    private fun duplicate(theme: ThemeManager.ThemeDefinition) { val copy = theme.copy(id = "user_${java.util.UUID.randomUUID()}", source = ThemeManager.ThemeSource.USER, nameFa = "${theme.nameFa} (کپی)", nameEn = "${theme.nameEn} (Copy)"); ThemeManager.saveUserTheme(this, copy); ThemeManager.select(this, copy.id); renderThemeLibrary() }
    private fun toggleFavorite(theme: ThemeManager.ThemeDefinition) { ThemeManager.toggleFavorite(this, theme.id); renderThemeLibrary() }
    private fun section(text: String) = TextView(this).apply { this.text = text; textSize = 15f; typeface = Typeface.DEFAULT_BOLD; setTextColor(getProperTextColor()); setPadding(0, dp(14), 0, dp(4)) }
    private fun libButton(text: String, onClick: () -> Unit) = TextView(this).apply { this.text = text; textSize = 13f; gravity = Gravity.CENTER; isClickable = true; setOnClickListener { onClick() }; setTextColor(getProperPrimaryColor()); setBackgroundColor(getProperBackgroundColor()); setPadding(dp(6), 0, dp(6), 0) }
    private fun tr(fa: String, en: String) = if (config.useEnglish) en else fa

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
        content.addView(infoText("herotux.github.io", 14f, false, primary).apply { gravity = Gravity.CENTER; setOnClickListener { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://herotux.github.io"))) } }, LinearLayout.LayoutParams(-1, -2)); content.addView(infoText("HEROTUX · Software Development", 13f, false, text).apply { gravity = Gravity.CENTER; alpha = 0.62f }, LinearLayout.LayoutParams(-1, -2)); scroll.addView(content); root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f)); setContentView(root); updateTextColors(root)
    }

    private fun renderContactPage() {
        val bg = getProperBackgroundColor(); val text = getProperTextColor(); val primary = getProperPrimaryColor(); val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(bg) }
        val toolbar = Toolbar(this).apply { title = "اطلاعات تماس"; setTitleTextColor(text); navigationIcon = getDrawable(androidx.appcompat.R.drawable.abc_ic_ab_back_material); setNavigationOnClickListener { finish() } }; root.addView(toolbar, LinearLayout.LayoutParams(-1, dp(56)))
        val scroll = ScrollView(this).apply { setPadding(dp(20), dp(20), dp(20), dp(28)) }; val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL; textDirection = View.TEXT_DIRECTION_RTL }
        content.addView(infoText("ارتباط با هما", 24f, true, text).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(-1, -2)); content.addView(infoText("برای پشتیبانی، پیشنهادها و گزارش مشکلات با ما در تماس باشید.", 14f, false, text).apply { gravity = Gravity.CENTER; alpha = 0.75f }, LinearLayout.LayoutParams(-1, -2)); contactCard(content, "ایمیل", "thefreetux@gmail.com", primary, text) { startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:thefreetux@gmail.com"))) }; contactCard(content, "تلفن پشتیبانی", "09375647544", primary, text) { startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:09375647544"))) }; contactCard(content, "وب‌سایت", "herotux.github.io", primary, text) { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://herotux.github.io"))) }; scroll.addView(content); root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f)); setContentView(root); updateTextColors(root)
    }

    private fun contactCard(root: LinearLayout, label: String, value: String, primary: Int, text: Int, onClick: () -> Unit) { val card = MaterialCardView(this).apply { radius = dp(18).toFloat(); cardElevation = 0f; strokeWidth = dp(1); strokeColor = primary; setCardBackgroundColor(getProperBackgroundColor()); isClickable = true; setOnClickListener { onClick() } }; val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(14), dp(18), dp(14)) }; content.addView(infoText(label, 13f, false, text)); content.addView(infoText(value, 17f, true, primary)); card.addView(content); root.addView(card, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) }) }
    private fun infoText(value: String, size: Float, bold: Boolean, color: Int) = TextView(this).apply { text = value; textSize = size; setTextColor(color); if (bold) typeface = Typeface.DEFAULT_BOLD; setPadding(0, dp(5), 0, dp(5)) }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}