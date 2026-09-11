package org.fossify.messages.activities

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.fossify.messages.BuildConfig
import org.fossify.messages.extensions.config
import org.fossify.messages.helpers.ThemeBackground
import org.fossify.messages.helpers.ThemeFileManager
import org.fossify.messages.helpers.ThemeManager
import org.fossify.messages.helpers.ThemeValidator
import java.util.UUID

/** Theme library and standalone editor for creating and editing Herotux themes. */
class ThemeBuilderActivity : SimpleActivity() {
    private data class ThemeField(val key: String, val label: String, var color: Int)

    companion object {
        const val EXTRA_THEME_ID = "theme_id"
        private const val EXTRA_CREATE = "create_theme"
    }

    private var pendingExportTheme: ThemeManager.ThemeDefinition? = null
    private var themeFilter = 0
    private var editingWallpaperUri: String? = null

    private val importThemeFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@registerForActivityResult
        runCatching { contentResolver.openInputStream(uri)?.use { it.reader().readText() } ?: error("فایل قابل خواندن نیست") }
            .mapCatching { ThemeFileManager.import(this, it).getOrThrow() }
            .onSuccess { theme ->
                if (ThemeFileManager.saveImported(this, theme)) { ThemeManager.select(this, theme.id); renderThemeLibrary() }
                else showThemeError("ذخیره تم واردشده انجام نشد")
            }
            .onFailure { showThemeError(it.message ?: "فایل تم معتبر نیست") }
    }

    private val wallpaperPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@registerForActivityResult
        runCatching { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        editingWallpaperUri = uri.toString()
        renderThemeBuilder()
    }

    private val createThemeFile = registerForActivityResult(ActivityResultContracts.CreateDocument(ThemeFileManager.MIME_TYPE)) { uri ->
        val theme = pendingExportTheme ?: return@registerForActivityResult
        pendingExportTheme = null
        if (uri == null) return@registerForActivityResult
        runCatching { contentResolver.openOutputStream(uri)?.use { it.write(ThemeFileManager.export(this, theme).toByteArray(Charsets.UTF_8)) } ?: error("فایل قابل ایجاد نیست") }
            .onFailure { showThemeError(it.message ?: "ذخیره فایل تم انجام نشد") }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val editingId = intent.getStringExtra(EXTRA_THEME_ID)
        if (editingId != null || intent.getBooleanExtra(EXTRA_CREATE, false)) renderThemeBuilder() else renderThemeLibrary()
    }

    private fun english(): Boolean = config.useEnglish
    private fun t(fa: String, en: String): String = if (english()) en else fa

    private fun openEditor(themeId: String? = null) {
        startActivity(Intent(this, ThemeBuilderActivity::class.java).apply {
            putExtra(EXTRA_CREATE, themeId == null)
            if (themeId != null) putExtra(EXTRA_THEME_ID, themeId)
        })
    }

    private fun openPreview(theme: ThemeManager.ThemeDefinition) {
        startActivity(Intent(this, ThemePreviewActivity::class.java).putExtra(ThemePreviewActivity.EXTRA_THEME_ID, theme.id))
    }

    private fun attrColor(attr: Int): Int {
        val value = TypedValue()
        theme.resolveAttribute(attr, value, true)
        return if (value.resourceId != 0) getColor(value.resourceId) else value.data
    }

    private fun renderThemeLibrary() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutDirection = if (english()) View.LAYOUT_DIRECTION_LTR else View.LAYOUT_DIRECTION_RTL; setBackgroundColor(attrColor(com.google.android.material.R.attr.colorSurface)) }
        val toolbar = Toolbar(this).apply { title = t("کتابخانه تم", "Theme library"); setTitleTextColor(attrColor(com.google.android.material.R.attr.colorOnSurface)); navigationIcon = getDrawable(androidx.appcompat.R.drawable.abc_ic_ab_back_material); setNavigationOnClickListener { finish() } }
        root.addView(toolbar, LinearLayout.LayoutParams(-1, dp(56)))
        val scroll = ScrollView(this).apply { isFillViewport = true; isVerticalScrollBarEnabled = false }
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(12), dp(18), dp(28)) }
        scroll.addView(content); root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f)); setContentView(root)
        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        actions.addView(actionButton(t("＋ ساخت تم", "+ Create theme")) { openEditor() }, LinearLayout.LayoutParams(0, dp(48), 1f))
        actions.addView(actionButton(t("مرتب‌سازی", "Sort")) { showSortDialog() }, LinearLayout.LayoutParams(0, dp(48), 1f).apply { marginStart = dp(5) })
        actions.addView(actionButton(t("وارد کردن", "Import")) { importThemeFile.launch(arrayOf(ThemeFileManager.MIME_TYPE, "application/octet-stream", "*/*")) }, LinearLayout.LayoutParams(0, dp(48), 1f).apply { marginStart = dp(5) })
        content.addView(actions)
        val search = EditText(this).apply { hint = t("جستجوی نام تم…", "Search themes…"); setSingleLine(true); setPadding(dp(14), 0, dp(14), 0); setBackgroundColor(attrColor(com.google.android.material.R.attr.colorSurfaceVariant)) }
        content.addView(search, LinearLayout.LayoutParams(-1, dp(48)).apply { topMargin = dp(12) })
        val filterRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        listOf(t("همه", "All"), t("ساخته من", "My themes"), t("واردشده", "Imported"), t("پسندیده", "Favorites")).forEachIndexed { index, label -> filterRow.addView(actionButton(label) { themeFilter = index; renderThemeCards(content, search.text.toString()) }, LinearLayout.LayoutParams(0, dp(42), 1f).apply { if (index > 0) marginStart = dp(5) }) }
        content.addView(filterRow, LinearLayout.LayoutParams(-1, dp(46)).apply { topMargin = dp(8) })
        search.addTextChangedListener(object : android.text.TextWatcher { override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit; override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { renderThemeCards(content, s?.toString().orEmpty()) }; override fun afterTextChanged(s: android.text.Editable?) = Unit })
        renderThemeCards(content, "")
    }

    private fun showSortDialog() {
        val sorts = ThemeManager.ThemeSort.entries
        val labels = sorts.map { sort ->
            when (sort) {
                ThemeManager.ThemeSort.DEFAULT -> t("پیش‌فرض", "Default")
                ThemeManager.ThemeSort.FAVORITES_FIRST -> t("پسندیده‌ها اول", "Favorites first")
                ThemeManager.ThemeSort.NAME_ASC -> t("نام: الف تا ی", "Name: A–Z")
                ThemeManager.ThemeSort.NAME_DESC -> t("نام: ی تا الف", "Name: Z–A")
                ThemeManager.ThemeSort.NEWEST -> t("جدیدترین", "Newest")
                ThemeManager.ThemeSort.BUILT_IN_FIRST -> t("تم‌های آماده اول", "Built-in first")
                ThemeManager.ThemeSort.CUSTOM_FIRST -> t("تم‌های شخصی اول", "Custom first")
                ThemeManager.ThemeSort.LAST_USED -> t("آخرین استفاده", "Last used")
            }
        }.toTypedArray()
        val selected = sorts.indexOf(ThemeManager.librarySort(this)).coerceAtLeast(0)
        MaterialAlertDialogBuilder(this).setTitle(t("مرتب‌سازی کتابخانه", "Sort library")).setSingleChoiceItems(labels, selected) { dialog, which ->
            ThemeManager.setLibrarySort(this, sorts[which]); dialog.dismiss(); renderThemeLibrary()
        }.setNegativeButton(t("لغو", "Cancel"), null).show()
    }

    private fun renderThemeCards(content: LinearLayout, query: String) {
        while (content.childCount > 3) content.removeViewAt(3)
        val all = ThemeManager.queryThemes(this, query, ThemeManager.librarySort(this))
        val themes = when (themeFilter) { 1 -> all.filter { it.source == ThemeManager.ThemeSource.USER }; 2 -> all.filter { it.source == ThemeManager.ThemeSource.IMPORTED }; 3 -> all.filter { ThemeManager.isFavorite(this, it.id) }; else -> all }
        val groups = listOf(ThemeManager.ThemeSource.BUILT_IN to t("تم‌های آماده", "Built-in themes"), ThemeManager.ThemeSource.USER to t("تم‌های من", "My themes"), ThemeManager.ThemeSource.IMPORTED to t("تم‌های واردشده", "Imported themes"), ThemeManager.ThemeSource.COMMUNITY to t("تم‌های جامعه", "Community themes"))
        var count = 0
        groups.forEach { (source, title) -> val group = themes.filter { it.source == source }; if (group.isEmpty()) return@forEach; content.addView(sectionTitle(title), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(16) }); group.forEach { theme -> content.addView(themeCard(theme), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) }); count++ } }
        if (count == 0) content.addView(sectionTitle(t("تمی پیدا نشد", "No themes found")))
    }

    private fun themeCard(theme: ThemeManager.ThemeDefinition): MaterialCardView = MaterialCardView(this).apply {
        val selected = ThemeManager.selectedThemeId(this@ThemeBuilderActivity) == theme.id
        radius = dp(18).toFloat(); strokeWidth = if (selected) dp(2) else dp(1); strokeColor = if (selected) theme.colors.accent else attrColor(com.google.android.material.R.attr.colorOutlineVariant); setCardBackgroundColor(theme.colors.surface)
        setOnClickListener { ThemeManager.select(this@ThemeBuilderActivity, theme.id); renderThemeLibrary() }; setOnLongClickListener { showThemeActions(theme); true }
        val box = LinearLayout(this@ThemeBuilderActivity).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12), dp(12), dp(12), dp(12)) }
        val header = LinearLayout(this@ThemeBuilderActivity).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        header.addView(TextView(this@ThemeBuilderActivity).apply { text = if (english()) theme.nameEn else theme.nameFa; textSize = 17f; setTextColor(theme.colors.textPrimary); typeface = Typeface.DEFAULT_BOLD }, LinearLayout.LayoutParams(0, dp(40), 1f))
        header.addView(TextView(this@ThemeBuilderActivity).apply { text = "◉"; textSize = 20f; gravity = Gravity.CENTER; setTextColor(theme.colors.accent); contentDescription = t("پیش‌نمایش", "Preview"); setOnClickListener { openPreview(theme) } }, LinearLayout.LayoutParams(dp(44), dp(40)))
        header.addView(TextView(this@ThemeBuilderActivity).apply { text = if (ThemeManager.isFavorite(this@ThemeBuilderActivity, theme.id)) "★" else "☆"; textSize = 24f; gravity = Gravity.CENTER; setTextColor(theme.colors.accent); setOnClickListener { ThemeManager.toggleFavorite(this@ThemeBuilderActivity, theme.id); renderThemeLibrary() } }, LinearLayout.LayoutParams(dp(44), dp(40)))
        box.addView(header)
        val preview = LinearLayout(this@ThemeBuilderActivity).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12), dp(10), dp(12), dp(10)) }
        ThemeBackground.apply(preview, theme.backgroundType, theme.colors.background, theme.gradientColors, theme.gradientAngle, theme.wallpaperUri)
        preview.addView(TextView(this@ThemeBuilderActivity).apply { text = "Aa  ${theme.nameEn}"; textSize = 13f; setTextColor(theme.colors.textPrimary) })
        preview.addView(TextView(this@ThemeBuilderActivity).apply { text = t("پیام دریافتی", "Incoming message"); textSize = 12f; setTextColor(theme.colors.textPrimary); setBackgroundColor(theme.colors.incomingBubble); setPadding(dp(8), dp(6), dp(8), dp(6)) }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(6) })
        preview.addView(TextView(this@ThemeBuilderActivity).apply { text = t("پیام ارسالی", "Outgoing message"); textSize = 12f; setTextColor(if (isLight(theme.colors.outgoingBubble)) Color.BLACK else Color.WHITE); setBackgroundColor(theme.colors.outgoingBubble); gravity = Gravity.END; setPadding(dp(8), dp(6), dp(8), dp(6)) }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(5) })
        box.addView(preview)
        val source = when (theme.source) { ThemeManager.ThemeSource.BUILT_IN -> t("آماده", "Built-in"); ThemeManager.ThemeSource.USER -> t("ساخته من", "My theme"); ThemeManager.ThemeSource.IMPORTED -> t("واردشده", "Imported"); ThemeManager.ThemeSource.COMMUNITY -> t("جامعه", "Community") }
        box.addView(TextView(this@ThemeBuilderActivity).apply { text = if (selected) "✓ $source · ${t("فعال", "Active")}" else source; textSize = 12f; setTextColor(theme.colors.textSecondary); setPadding(0, dp(8), 0, 0) })
        addView(box)
    }

    private fun showThemeActions(theme: ThemeManager.ThemeDefinition) {
        val builtIn = theme.source == ThemeManager.ThemeSource.BUILT_IN
        val items = if (builtIn) arrayOf(t("اعمال", "Apply"), t("پیش‌نمایش", "Preview"), t("تکثیر", "Duplicate"), if (ThemeManager.isFavorite(this, theme.id)) t("حذف از پسندیده‌ها", "Remove favorite") else t("افزودن به پسندیده‌ها", "Add to favorites")) else arrayOf(t("اعمال", "Apply"), t("پیش‌نمایش", "Preview"), t("ویرایش", "Edit"), t("تکثیر", "Duplicate"), t("خروجی", "Export"), t("اشتراک‌گذاری", "Share"), if (ThemeManager.isFavorite(this, theme.id)) t("حذف از پسندیده‌ها", "Remove favorite") else t("افزودن به پسندیده‌ها", "Add to favorites"), t("حذف", "Delete"))
        MaterialAlertDialogBuilder(this).setTitle(if (english()) theme.nameEn else theme.nameFa).setItems(items) { _, which ->
            if (builtIn) when (which) { 0 -> { ThemeManager.select(this, theme.id); renderThemeLibrary() }; 1 -> openPreview(theme); 2 -> duplicateTheme(theme); 3 -> { ThemeManager.toggleFavorite(this, theme.id); renderThemeLibrary() } }
            else when (which) { 0 -> { ThemeManager.select(this, theme.id); renderThemeLibrary() }; 1 -> openPreview(theme); 2 -> openEditor(theme.id); 3 -> duplicateTheme(theme); 4 -> exportThemeFile(theme); 5 -> shareThemeFile(theme); 6 -> { ThemeManager.toggleFavorite(this, theme.id); renderThemeLibrary() }; 7 -> confirmDeleteTheme(theme) }
        }.show()
    }

    private fun duplicateTheme(theme: ThemeManager.ThemeDefinition) { val copy = theme.copy(id = "user_${UUID.randomUUID()}", source = ThemeManager.ThemeSource.USER, nameFa = theme.nameFa + " (کپی)", nameEn = theme.nameEn + " (Copy)"); ThemeManager.saveUserTheme(this, copy); ThemeManager.select(this, copy.id); renderThemeLibrary() }
    private fun confirmDeleteTheme(theme: ThemeManager.ThemeDefinition) { MaterialAlertDialogBuilder(this).setTitle(t("حذف تم؟", "Delete theme?")).setMessage(t("تم «${theme.nameFa}» حذف شود؟", "Delete “${theme.nameEn}”?" )).setNegativeButton(t("لغو", "Cancel"), null).setPositiveButton(t("حذف", "Delete")) { _, _ -> ThemeManager.deleteCustomTheme(this, theme.id); renderThemeLibrary() }.show() }
    private fun exportThemeFile(theme: ThemeManager.ThemeDefinition) { pendingExportTheme = theme; createThemeFile.launch("${theme.nameEn.ifBlank { "theme" }}${ThemeFileManager.FILE_EXTENSION}") }
    private fun shareThemeFile(theme: ThemeManager.ThemeDefinition) { val file = runCatching { java.io.File(cacheDir, "${theme.id}${ThemeFileManager.FILE_EXTENSION}").apply { writeText(ThemeFileManager.export(this@ThemeBuilderActivity, theme), Charsets.UTF_8) } }.getOrNull() ?: return showThemeError("ساخت فایل اشتراک‌گذاری انجام نشد"); val uri = androidx.core.content.FileProvider.getUriForFile(this, "${BuildConfig.APPLICATION_ID}.provider", file); startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = ThemeFileManager.MIME_TYPE; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }, t("اشتراک‌گذاری تم", "Share theme"))) }
    private fun sectionTitle(text: String) = TextView(this).apply { this.text = text; textSize = 15f; typeface = Typeface.DEFAULT_BOLD; setTextColor(attrColor(com.google.android.material.R.attr.colorOnSurface)); setPadding(0, dp(4), 0, dp(4)) }
    private fun actionButton(text: String, onClick: () -> Unit) = TextView(this).apply { this.text = text; textSize = 13f; gravity = Gravity.CENTER; isClickable = true; setOnClickListener { onClick() }; setTextColor(attrColor(androidx.appcompat.R.attr.colorPrimary)); setBackgroundColor(attrColor(com.google.android.material.R.attr.colorSurfaceVariant)); setPadding(dp(8), 0, dp(8), 0) }

    private fun renderThemeBuilder() {
        val editingId = intent.getStringExtra(EXTRA_THEME_ID)
        val existing = editingId?.let { ThemeManager.find(this, it) }
        val source = existing?.colors ?: ThemeManager.colors(this)
        if (editingWallpaperUri == null) editingWallpaperUri = existing?.wallpaperUri
        val fields = mutableListOf(ThemeField("primary", "رنگ اصلی", source.primary), ThemeField("accent", "رنگ تأکیدی", source.accent), ThemeField("background", "پس‌زمینه", source.background), ThemeField("surface", "سطح کارت‌ها", source.surface), ThemeField("toolbar", "نوار ابزار", source.toolbar), ThemeField("incoming", "حباب دریافتی", source.incomingBubble), ThemeField("outgoing", "حباب ارسالی", source.outgoingBubble), ThemeField("text", "متن اصلی", source.textPrimary), ThemeField("secondary", "متن ثانویه", source.textSecondary), ThemeField("fab", "دکمه شناور", source.fab))
        var backgroundType = when { !editingWallpaperUri.isNullOrBlank() -> ThemeManager.BackgroundType.WALLPAPER; existing != null -> existing.backgroundType; else -> ThemeManager.BackgroundType.SOLID }
        var gradientStart = existing?.gradientColors?.getOrNull(0) ?: source.background
        var gradientEnd = existing?.gradientColors?.getOrNull(1) ?: source.primary
        var gradientAngle = existing?.gradientAngle ?: 0
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL }
        val toolbar = Toolbar(this).apply { title = if (existing == null) "ساخت تم جدید" else "ویرایش تم"; navigationIcon = getDrawable(androidx.appcompat.R.drawable.abc_ic_ab_back_material); setNavigationOnClickListener { finish() } }
        root.addView(toolbar, LinearLayout.LayoutParams(-1, dp(56)))
        val scroll = ScrollView(this).apply { isFillViewport = true; isVerticalScrollBarEnabled = false; setPadding(dp(20), dp(10), dp(20), dp(28)) }
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }; scroll.addView(content); root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        val fileActions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val import = TextView(this).apply { text = "وارد کردن .homa-theme"; textSize = 14f; gravity = Gravity.CENTER; isClickable = true; setPadding(dp(8), dp(10), dp(8), dp(10)); background = makeSwatch(source.surface); setOnClickListener { importThemeFile.launch(arrayOf(ThemeFileManager.MIME_TYPE, "application/octet-stream", "*/*")) } }
        fileActions.addView(import, LinearLayout.LayoutParams(0, dp(48), 1f)); content.addView(fileActions, LinearLayout.LayoutParams(-1, dp(48)).apply { setMargins(0, 0, 0, dp(8)) })
        val name = EditText(this).apply { hint = "نام تم"; setText(existing?.nameFa ?: ""); textSize = 16f }; content.addView(name, LinearLayout.LayoutParams(-1, dp(58)))
        val preview = MaterialCardView(this).apply { radius = dp(20).toFloat(); cardElevation = 0f }
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(16), dp(16), dp(16)) }
        val title = TextView(this).apply { text = "پیش‌نمایش گفتگو"; textSize = 17f }
        val incoming = TextView(this).apply { text = "سلام 👋 این یک پیام دریافتی است"; textSize = 15f; setPadding(dp(12), dp(10), dp(12), dp(10)) }
        val outgoing = TextView(this).apply { text = "سلام! تم جدید آماده است 😊"; textSize = 15f; gravity = Gravity.END; setPadding(dp(12), dp(10), dp(12), dp(10)) }
        box.addView(title); box.addView(incoming, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, dp(12), dp(28), dp(6)) }); box.addView(outgoing, LinearLayout.LayoutParams(-1, -2).apply { setMargins(dp(28), dp(6), 0, 0) }); preview.addView(box); content.addView(preview, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, dp(16), 0, dp(10)) })
        fun get(key: String) = fields.first { it.key == key }.color
        fun buildColors() = ThemeManager.ThemeColors(get("primary"), get("accent"), get("background"), get("surface"), get("text"), get("secondary"), get("incoming"), get("outgoing"), get("toolbar"), get("accent"), get("fab"))
        val refresh: () -> Unit = { val bg = if (backgroundType == ThemeManager.BackgroundType.LINEAR_GRADIENT) listOf(gradientStart, gradientEnd) else emptyList(); ThemeBackground.apply(box, backgroundType, get("background"), bg, gradientAngle, editingWallpaperUri); title.setTextColor(get("text")); incoming.setTextColor(get("text")); outgoing.setTextColor(if (isLight(get("outgoing"))) Color.BLACK else Color.WHITE); incoming.backgroundTintList = ColorStateList.valueOf(get("incoming")); outgoing.backgroundTintList = ColorStateList.valueOf(get("outgoing")) }
        lateinit var backgroundButton: TextView
        backgroundButton = actionButton(backgroundLabel(backgroundType, gradientAngle, editingWallpaperUri)) {
            val choices = arrayOf("رنگ ساده", "گرادیان خطی", "تصویر از گالری")
            MaterialAlertDialogBuilder(this).setTitle("پس‌زمینه تم").setItems(choices) { _, which -> when (which) { 0 -> { backgroundType = ThemeManager.BackgroundType.SOLID; editingWallpaperUri = null; refreshBackgroundButton(backgroundButton, backgroundType, gradientAngle, editingWallpaperUri); refresh() }; 1 -> showGradientDialog(gradientStart, gradientEnd, gradientAngle) { start, end, angle -> backgroundType = ThemeManager.BackgroundType.LINEAR_GRADIENT; gradientStart = start; gradientEnd = end; gradientAngle = angle; editingWallpaperUri = null; refreshBackgroundButton(backgroundButton, backgroundType, gradientAngle, editingWallpaperUri); refresh() }; 2 -> { backgroundType = ThemeManager.BackgroundType.WALLPAPER; wallpaperPicker.launch(arrayOf("image/*")) } } }.show()
        }
        content.addView(backgroundButton, LinearLayout.LayoutParams(-1, dp(48)).apply { bottomMargin = dp(8) })
        fields.forEach { field -> val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(5), 0, dp(5)) }; row.addView(TextView(this).apply { text = field.label; textSize = 15f }, LinearLayout.LayoutParams(0, dp(52), 1f)); val swatch = View(this).apply { background = makeSwatch(field.color) }; row.addView(swatch, LinearLayout.LayoutParams(dp(58), dp(42))); row.setOnClickListener { chooseThemeColor(field, swatch) { refresh() } }; content.addView(row) }
        val save = TextView(this).apply { text = if (existing == null) "ذخیره تم" else "ذخیره تغییرات"; textSize = 16f; gravity = Gravity.CENTER; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE); setPadding(0, dp(14), 0, dp(14)); background = makeSwatch(source.primary); isClickable = true; setOnClickListener { val themeName = name.text.toString().trim().ifEmpty { "تم من" }; val colors = buildColors(); val report = ThemeValidator.validate(colors); if (!report.isValid) { val details = report.issues.joinToString("\n") { issue -> "• ${issue.name}: ${String.format("%.2f", issue.ratio)}:1 (حداقل ${String.format("%.1f", issue.requiredRatio)}:1)" }; MaterialAlertDialogBuilder(this@ThemeBuilderActivity).setTitle("خوانایی تم نیاز به بررسی دارد").setMessage("کنتراست بعضی ترکیب‌های متن و پس‌زمینه پایین است:\n\n$details\n\nمی‌خواهید با همین رنگ‌ها ذخیره شود؟").setNegativeButton("اصلاح رنگ‌ها", null).setPositiveButton("ذخیره با همین رنگ‌ها") { _, _ -> saveTheme(themeName, colors, editingId, backgroundType, gradientStart, gradientEnd, gradientAngle, editingWallpaperUri) }.show(); return@setOnClickListener }; saveTheme(themeName, colors, editingId, backgroundType, gradientStart, gradientEnd, gradientAngle, editingWallpaperUri) } }
        content.addView(save, LinearLayout.LayoutParams(-1, dp(52)).apply { setMargins(0, dp(16), 0, 0) })
        if (existing != null) { content.addView(TextView(this).apply { text = "پیش‌نمایش حرفه‌ای"; textSize = 15f; gravity = Gravity.CENTER; setPadding(0, dp(12), 0, dp(12)); setOnClickListener { openPreview(existing) } }, LinearLayout.LayoutParams(-1, dp(48)).apply { topMargin = dp(8) }); content.addView(TextView(this).apply { text = "خروجی فایل .homa-theme"; textSize = 15f; gravity = Gravity.CENTER; setPadding(0, dp(12), 0, dp(12)); setOnClickListener { exportThemeFile(existing) } }, LinearLayout.LayoutParams(-1, dp(48)).apply { topMargin = dp(8) }); content.addView(TextView(this).apply { text = "اشتراک‌گذاری تم"; textSize = 15f; gravity = Gravity.CENTER; setPadding(0, dp(12), 0, dp(12)); setOnClickListener { shareThemeFile(existing) } }, LinearLayout.LayoutParams(-1, dp(48))) }
        setContentView(root); refreshBackgroundButton(backgroundButton, backgroundType, gradientAngle, editingWallpaperUri); refresh()
    }

    private fun showGradientDialog(start: Int, end: Int, angle: Int, onApply: (Int, Int, Int) -> Unit) { val startInput = EditText(this).apply { hint = "رنگ شروع #RRGGBB"; setText(String.format("#%06X", 0xFFFFFF and start)); selectAll() }; val endInput = EditText(this).apply { hint = "رنگ پایان #RRGGBB"; setText(String.format("#%06X", 0xFFFFFF and end)); selectAll() }; val spinner = Spinner(this); val angles = listOf(0, 45, 90, 135, 180, 225, 270, 315); spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, angles.map { "${it}°" }); spinner.setSelection(angles.indexOf(angle).coerceAtLeast(0)); val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(8), dp(4), dp(8), 0) }; layout.addView(startInput); layout.addView(endInput); layout.addView(TextView(this).apply { text = "زاویه گرادیان"; setPadding(0, dp(8), 0, dp(2)) }); layout.addView(spinner); MaterialAlertDialogBuilder(this).setTitle("تنظیم گرادیان").setView(layout).setPositiveButton("اعمال") { _, _ -> runCatching { Color.parseColor(startInput.text.toString().trim()) to Color.parseColor(endInput.text.toString().trim()) }.onSuccess { (a, b) -> onApply(a, b, angles[spinner.selectedItemPosition]) }.onFailure { showThemeError("رنگ گرادیان نامعتبر است") } }.setNegativeButton("لغو", null).show() }
    private fun backgroundLabel(type: ThemeManager.BackgroundType, angle: Int, wallpaperUri: String?): String = when (type) { ThemeManager.BackgroundType.LINEAR_GRADIENT -> "پس‌زمینه: گرادیان خطی · ${angle}°"; ThemeManager.BackgroundType.WALLPAPER -> "پس‌زمینه: تصویر ${if (wallpaperUri.isNullOrBlank()) "انتخاب نشده" else "✓"}"; else -> "پس‌زمینه: رنگ ساده" }
    private fun refreshBackgroundButton(button: TextView, type: ThemeManager.BackgroundType, angle: Int, wallpaperUri: String?) { button.text = backgroundLabel(type, angle, wallpaperUri) }
    private fun saveTheme(themeName: String, colors: ThemeManager.ThemeColors, editingId: String?, backgroundType: ThemeManager.BackgroundType, gradientStart: Int, gradientEnd: Int, gradientAngle: Int, wallpaperUri: String?) { val id = editingId ?: "user_${UUID.randomUUID()}"; val gradientColors = if (backgroundType == ThemeManager.BackgroundType.LINEAR_GRADIENT) listOf(gradientStart, gradientEnd) else emptyList(); ThemeManager.saveUserTheme(this, ThemeManager.ThemeDefinition(id, themeName, themeName, ThemeManager.ThemeSource.USER, colors = colors, backgroundType = backgroundType, gradientColors = gradientColors, gradientAngle = gradientAngle, wallpaperUri = wallpaperUri)); ThemeManager.select(this, id); finish() }
    private fun chooseThemeColor(field: ThemeField, swatch: View, changed: () -> Unit) { val input = EditText(this).apply { hint = "#RRGGBB"; setText(String.format("#%06X", 0xFFFFFF and field.color)); selectAll() }; MaterialAlertDialogBuilder(this).setTitle(field.label).setView(input).setPositiveButton("اعمال") { _, _ -> runCatching { Color.parseColor(input.text.toString().trim()) }.onSuccess { field.color = it; swatch.background = makeSwatch(it); changed() } }.setNegativeButton("لغو", null).show() }
    private fun showThemeError(message: String) { MaterialAlertDialogBuilder(this).setTitle("خطا در فایل تم").setMessage(message).setPositiveButton("باشه", null).show() }
    private fun makeSwatch(color: Int): android.graphics.drawable.GradientDrawable = android.graphics.drawable.GradientDrawable().apply { setColor(color); cornerRadius = dp(14).toFloat(); setStroke(dp(1), 0x33000000) }
    private fun isLight(color: Int): Boolean { val r = Color.red(color) / 255f; val g = Color.green(color) / 255f; val b = Color.blue(color) / 255f; return 0.2126f * r + 0.7152f * g + 0.0722f * b > 0.55f }
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
