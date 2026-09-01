package org.fossify.messages.activities

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.res.ResourcesCompat
import androidx.core.os.LocaleListCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider
import org.fossify.commons.dialogs.SecurityDialog
import org.fossify.commons.helpers.FONT_TYPE_CUSTOM
import org.fossify.commons.helpers.FONT_TYPE_SYSTEM_DEFAULT
import org.fossify.commons.helpers.FontHelper
import org.fossify.commons.helpers.SHOW_ALL_TABS
import org.fossify.messages.R
import org.fossify.messages.extensions.config
import org.fossify.messages.helpers.FILE_SIZE_100_KB
import org.fossify.messages.helpers.FILE_SIZE_1_MB
import org.fossify.messages.helpers.FILE_SIZE_200_KB
import org.fossify.messages.helpers.FILE_SIZE_2_MB
import org.fossify.messages.helpers.FILE_SIZE_300_KB
import org.fossify.messages.helpers.FILE_SIZE_600_KB
import org.fossify.messages.helpers.FILE_SIZE_NONE
import org.fossify.messages.helpers.LOCK_SCREEN_NOTHING
import org.fossify.messages.helpers.LOCK_SCREEN_SENDER
import org.fossify.messages.helpers.LOCK_SCREEN_SENDER_MESSAGE

/**
 * Standalone Messages settings UI. Presentation is local to Messages and does not
 * depend on Commons settings layouts or text rendering.
 */
class SettingsActivity : SimpleActivity() {
    companion object {
        private const val EXTRA_PAGE = "settings_page"
        private const val GENERAL = "general"
        private const val APPEARANCE = "appearance"
        private const val MESSAGES = "messages"
        private const val NOTIFICATIONS = "notifications"
        private const val CONVERSATIONS = "conversations"
        private const val BANKS = "banks"
        private const val PRIVACY = "privacy"
        private const val ABOUT = "about"
    }

    private val pickCustomFont = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@registerForActivityResult
        runCatching {
            val name = resolveFontFileName(uri)
            val data = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@runCatching
            if (data.isNotEmpty() && FontHelper.saveFontData(this, data, name)) {
                config.fontType = FONT_TYPE_CUSTOM
                config.fontName = name
                FontHelper.clearCache()
                recreate()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        render(intent.getStringExtra(EXTRA_PAGE))
    }

    private fun english(): Boolean = config.useEnglish
    private fun t(fa: String, en: String): String = if (english()) en else fa

    private fun render(page: String?) {
        val surface = color(com.google.android.material.R.attr.colorSurface)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = if (english()) View.LAYOUT_DIRECTION_LTR else View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(surface)
        }
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            val bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            view.setPadding(view.paddingLeft, top, view.paddingRight, bottom)
            insets
        }
        ViewCompat.requestApplyInsets(root)
        val toolbar = MaterialToolbar(this).apply {
            title = pageTitle(page)
            setBackgroundColor(surface)
            setTitleTextColor(color(com.google.android.material.R.attr.colorOnSurface))
            elevation = 0f
            if (page != null) {
                navigationIcon = getDrawable(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
                setNavigationOnClickListener { finish() }
            }
        }
        root.addView(toolbar, LinearLayout.LayoutParams(-1, dp(64)))
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            isVerticalScrollBarEnabled = false
            setPadding(dp(20), dp(14), dp(20), dp(28))
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = if (english()) View.LAYOUT_DIRECTION_LTR else View.LAYOUT_DIRECTION_RTL
            textDirection = if (english()) View.TEXT_DIRECTION_LTR else View.TEXT_DIRECTION_RTL
        }
        scroll.addView(content, LinearLayout.LayoutParams(-1, -2))
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        when (page) {
            GENERAL -> general(content)
            APPEARANCE -> appearance(content)
            MESSAGES -> messages(content)
            NOTIFICATIONS -> notifications(content)
            CONVERSATIONS -> conversations(content)
            BANKS -> banks(content)
            PRIVACY -> privacy(content)
            ABOUT -> about(content)
            else -> home(content)
        }
        if (!english()) applyPersianFont(root)
    }

    private fun home(root: LinearLayout) {
        category(root, "عمومی", "زبان، تقویم و تنظیمات پایه", GENERAL, android.R.drawable.ic_menu_manage, "General", "Language, calendar and basic options")
        category(root, "ظاهر", "تم، فونت و اندازه متن", APPEARANCE, android.R.drawable.ic_menu_view, "Appearance", "Theme, font and text size")
        category(root, "پیام‌ها", "ارسال SMS و MMS و گزارش تحویل", MESSAGES, android.R.drawable.ic_dialog_email, "Messages", "SMS, MMS and delivery reports")
        category(root, "اعلان‌ها", "اعلان‌ها و نمایش روی صفحه قفل", NOTIFICATIONS, android.R.drawable.ic_dialog_info, "Notifications", "Notifications and lock-screen display")
        category(root, "گفتگوها", "آرشیو و سطل بازیافت", CONVERSATIONS, android.R.drawable.ic_menu_sort_by_size, "Conversations", "Archive and recycle bin")
        category(root, "بانک و تراکنش", "کارت‌ها و تشخیص بانک", BANKS, android.R.drawable.ic_menu_save, "Banking", "Cards and bank detection")
        category(root, "حریم خصوصی و امنیت", "قفل برنامه و مجوزها", PRIVACY, android.R.drawable.ic_lock_lock, "Privacy & security", "App lock and permissions")
        category(root, "درباره برنامه", "اطلاعات نسخه و پروژه", ABOUT, android.R.drawable.ic_menu_info_details, "About", "Version and project information")
    }

    private fun category(root: LinearLayout, fa: String, faSummary: String, page: String, icon: Int, en: String, enSummary: String) {
        val card = MaterialCardView(this).apply {
            radius = dp(20).toFloat()
            cardElevation = 0f
            strokeWidth = dp(1)
            strokeColor = color(com.google.android.material.R.attr.colorOutlineVariant)
            setCardBackgroundColor(color(com.google.android.material.R.attr.colorSurfaceVariant))
            setOnClickListener { startActivity(Intent(this@SettingsActivity, SettingsActivity::class.java).putExtra(EXTRA_PAGE, page)) }
        }
        val line = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = if (english()) View.LAYOUT_DIRECTION_LTR else View.LAYOUT_DIRECTION_RTL
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }
        line.addView(AppCompatImageView(this).apply {
            setImageResource(icon)
            imageTintList = ColorStateList.valueOf(color(androidx.appcompat.R.attr.colorPrimary))
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }, LinearLayout.LayoutParams(dp(48), dp(48)))
        val texts = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12), 0, dp(8), 0) }
        texts.addView(label(t(fa, en), 17f, true))
        texts.addView(label(t(faSummary, enSummary), 13f, false, color(com.google.android.material.R.attr.colorOnSurfaceVariant)))
        line.addView(texts, LinearLayout.LayoutParams(0, -2, 1f))
        line.addView(TextView(this).apply { text = if (english()) "›" else "‹"; textSize = 28f; gravity = Gravity.CENTER; setTextColor(color(com.google.android.material.R.attr.colorOnSurfaceVariant)) }, LinearLayout.LayoutParams(dp(28), dp(44)))
        card.addView(line)
        root.addView(card, margins(0, 10))
    }

    private fun general(root: LinearLayout) {
        section(root, t("عمومی", "General"))
        row(root, t("زبان برنامه", "App language"), if (english()) "English" else "فارسی") { chooseLanguage() }
        if (Build.VERSION.SDK_INT >= 33) row(root, t("زبان سیستم Android", "Android system language"), resources.configuration.locales[0].displayLanguage) {
            runCatching { startActivity(Intent(Settings.ACTION_APP_LOCALE_SETTINGS).setData(Uri.parse("package:$packageName"))) }
        }
        toggle(root, t("تقویم شمسی", "Persian calendar"), t("نمایش تاریخ‌ها با تقویم جلالی", "Display dates using the Jalali calendar"), config.usePersianCalendar) {
            config.usePersianCalendar = it
            recreate()
        }
        row(root, t("تاریخ و ساعت", "Date & time"), t("استفاده از تنظیمات سیستم", "Use system settings")) { startActivity(Intent(Settings.ACTION_DATE_SETTINGS)) }
    }

    private fun appearance(root: LinearLayout) {
        section(root, t("ظاهر", "Appearance"))
        val dark = getSharedPreferences("messages_settings_ui", MODE_PRIVATE).getBoolean("dark_mode", false)
        toggle(root, t("حالت تاریک", "Dark mode"), t("تم تیره برای رابط کاربری", "Use a dark interface"), dark) {
            getSharedPreferences("messages_settings_ui", MODE_PRIVATE).edit().putBoolean("dark_mode", it).apply()
            AppCompatDelegate.setDefaultNightMode(if (it) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)
        }
        row(root, t("فونت برنامه", "App font"), fontLabel()) { chooseFont() }
        row(root, t("اندازه متن", "Text size"), fontSizeLabel()) { chooseFontSize() }
        slider(root, t("اندازه متن گفتگو", "Conversation text size"), config.fontSize.toFloat().coerceIn(1f, 4f)) { config.fontSize = it.toInt() }
        toggle(root, t("شمارنده کاراکتر", "Character counter"), t("نمایش تعداد کاراکتر هنگام نوشتن", "Show character count while typing"), config.showCharacterCounter) { config.showCharacterCounter = it }
        toggle(root, t("نویسه‌های ساده", "Simple characters"), t("استفاده از نویسه‌های ساده‌تر", "Use simpler characters"), config.useSimpleCharacters) { config.useSimpleCharacters = it }
    }

    private fun messages(root: LinearLayout) {
        section(root, t("پیام‌ها", "Messages"))
        toggle(root, t("ارسال با Enter", "Send with Enter"), t("با زدن Enter پیام ارسال شود", "Send a message when Enter is pressed"), config.sendOnEnter) { config.sendOnEnter = it }
        toggle(root, t("گزارش تحویل", "Delivery reports"), t("گزارش دریافت پیام توسط گیرنده", "Show delivery reports"), config.enableDeliveryReports) { config.enableDeliveryReports = it }
        toggle(root, t("MMS برای پیام طولانی", "MMS for long messages"), t("پیام‌های طولانی به MMS تبدیل شوند", "Convert long messages to MMS"), config.sendLongMessageMMS) { config.sendLongMessageMMS = it }
        toggle(root, t("MMS برای پیام گروهی", "MMS for group messages"), t("پیام‌های گروهی با MMS ارسال شوند", "Send group messages as MMS"), config.sendGroupMessageMMS) { config.sendGroupMessageMMS = it }
        row(root, t("محدودیت حجم MMS", "MMS size limit"), mmsLimitLabel()) { chooseMmsLimit() }
    }

    private fun notifications(root: LinearLayout) {
        section(root, t("اعلان‌ها", "Notifications"))
        row(root, t("تنظیمات اعلان Messages", "Messages notification settings"), t("صدا، لرزش و رفتار اعلان", "Sound, vibration and notification behavior")) { openAppNotificationSettings() }
        row(root, t("مجوز اعلان‌ها", "Notification permission"), t("مدیریت مجوزهای اعلان", "Manage notification permission")) { openAppNotificationSettings() }
        row(root, t("نمایش روی صفحه قفل", "Lock-screen display"), lockScreenLabel()) { chooseLockScreen() }
    }

    private fun conversations(root: LinearLayout) {
        section(root, t("گفتگوها", "Conversations"))
        toggle(root, t("نگه داشتن گفتگوهای آرشیوشده", "Keep archived conversations"), t("گفتگوهای جدید دوباره از آرشیو خارج نشوند", "New messages do not unarchive conversations"), config.keepConversationsArchived) { config.keepConversationsArchived = it }
        toggle(root, t("سطل بازیافت", "Recycle bin"), t("حذف گفتگوها ابتدا به سطل بازیافت منتقل شود", "Deleted conversations go to the recycle bin first"), config.useRecycleBin) { config.useRecycleBin = it }
        row(root, t("سطل بازیافت", "Recycle bin"), t("مدیریت گفتگوهای حذف‌شده", "Manage deleted conversations")) { runCatching { startActivity(Intent(this, RecycleBinConversationsActivity::class.java)) } }
        row(root, t("گفتگوهای آرشیوشده", "Archived conversations"), t("مشاهده گفتگوهای آرشیوشده", "View archived conversations")) { runCatching { startActivity(Intent(this, ArchivedConversationsActivity::class.java)) } }
    }

    private fun banks(root: LinearLayout) {
        section(root, t("بانک و تراکنش", "Banking"))
        row(root, t("حساب‌ها و کارت‌های بانکی", "Bank accounts & cards"), t("افزودن، ویرایش و حذف کارت‌ها", "Add, edit and delete cards")) { startActivity(Intent(this, BankCardsActivity::class.java)) }
        row(root, t("تشخیص بانک", "Bank detection"), t("بانک‌های پشتیبانی‌شده", "Supported banks")) {
            val names = org.fossify.messages.helpers.IranianBankRegistry.allBanks().map { if (english()) it.englishName else it.persianName }.toTypedArray()
            MaterialAlertDialogBuilder(this).setTitle(t("بانک‌های پشتیبانی‌شده", "Supported banks")).setItems(names, null).show()
        }
    }

    private fun privacy(root: LinearLayout) {
        section(root, t("حریم خصوصی و امنیت", "Privacy & security"))
        row(root, t("قفل برنامه", "App lock"), if (config.isAppPasswordProtectionOn) t("فعال", "Enabled") else t("غیرفعال", "Disabled")) { openAppLock() }
        row(root, t("محتوای پیام روی صفحه قفل", "Lock-screen message content"), lockScreenLabel()) { chooseLockScreen() }
        row(root, t("مجوزها", "Permissions"), t("مدیریت دسترسی‌های Messages", "Manage Messages permissions")) { openAppDetails() }
    }

    private fun about(root: LinearLayout) {
        section(root, "Messages")
        row(root, t("درباره برنامه", "About"), t("اطلاعات نسخه و پروژه", "Version and project information")) { startActivity(Intent(this, HerotuxAboutActivity::class.java)) }
        row(root, t("تنظیمات سیستم Android", "Android system settings"), t("اطلاعات برنامه، مجوزها و حافظه", "App info, permissions and storage")) { openAppDetails() }
    }

    private fun chooseLanguage() {
        val labels = arrayOf("فارسی", "English")
        val selected = if (config.useEnglish) 1 else 0
        MaterialAlertDialogBuilder(this).setTitle(t("زبان برنامه", "App language")).setSingleChoiceItems(labels, selected) { dialog, which ->
            val english = which == 1
            config.useEnglish = english
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(if (english) "en" else "fa"))
            dialog.dismiss()
            recreate()
        }.show()
    }

    private fun chooseFont() {
        val labels = arrayOf(t("فونت پیش‌فرض سیستم", "System default font"), t("فونت سفارشی", "Custom font"))
        val selected = if (config.fontType == FONT_TYPE_CUSTOM) 1 else 0
        MaterialAlertDialogBuilder(this).setTitle(t("فونت برنامه", "App font")).setSingleChoiceItems(labels, selected) { dialog, which ->
            if (which == 0) {
                config.fontType = FONT_TYPE_SYSTEM_DEFAULT
                config.fontName = ""
                FontHelper.clearCache()
                dialog.dismiss()
                recreate()
            } else {
                dialog.dismiss()
                pickCustomFont.launch(arrayOf("font/*", "application/octet-stream"))
            }
        }.show()
    }

    private fun resolveFontFileName(uri: Uri): String {
        val fallback = "custom_font_${System.currentTimeMillis()}.ttf"
        val name = runCatching { contentResolver.query(uri, arrayOf("_display_name"), null, null, null)?.use { if (it.moveToFirst()) it.getString(0) else null } }.getOrNull()
        return name?.substringAfterLast('/')?.replace(Regex("[^A-Za-z0-9._-]"), "_")?.ifBlank { fallback } ?: fallback
    }

    private fun openAppLock() {
        val tab = if (config.isAppPasswordProtectionOn) config.appProtectionType else SHOW_ALL_TABS
        SecurityDialog(activity = this, requiredHash = config.appPasswordHash, showTabIndex = tab) { hash, type, success ->
            if (!success) return@SecurityDialog
            val wasEnabled = config.isAppPasswordProtectionOn
            config.isAppPasswordProtectionOn = !wasEnabled
            config.appPasswordHash = if (wasEnabled) "" else hash
            config.appProtectionType = type
            render(PRIVACY)
        }
    }

    private fun chooseFontSize() {
        val values = arrayOf(t("کوچک", "Small"), t("متوسط", "Medium"), t("بزرگ", "Large"), t("خیلی بزرگ", "Very large"))
        val selected = (config.fontSize - 1).coerceIn(0, 3)
        MaterialAlertDialogBuilder(this).setTitle(t("اندازه متن", "Text size")).setSingleChoiceItems(values, selected) { dialog, which -> config.fontSize = which + 1; dialog.dismiss(); render(APPEARANCE) }.show()
    }

    private fun chooseLockScreen() {
        val values = arrayOf(t("فرستنده و متن پیام", "Sender and message"), t("فقط فرستنده", "Sender only"), t("هیچ‌چیز", "Nothing"))
        val selected = when (config.lockScreenVisibilitySetting) { LOCK_SCREEN_SENDER -> 1; LOCK_SCREEN_NOTHING -> 2; else -> 0 }
        MaterialAlertDialogBuilder(this).setTitle(t("نمایش روی صفحه قفل", "Lock-screen display")).setSingleChoiceItems(values, selected) { dialog, which ->
            config.lockScreenVisibilitySetting = when (which) { 1 -> LOCK_SCREEN_SENDER; 2 -> LOCK_SCREEN_NOTHING; else -> LOCK_SCREEN_SENDER_MESSAGE }
            dialog.dismiss()
            render(NOTIFICATIONS)
        }.show()
    }

    private fun chooseMmsLimit() {
        val labels = arrayOf("بدون محدودیت", "2 MB", "1 MB", "600 KB", "300 KB", "200 KB", "100 KB")
        val values = arrayOf(FILE_SIZE_NONE, FILE_SIZE_2_MB, FILE_SIZE_1_MB, FILE_SIZE_600_KB, FILE_SIZE_300_KB, FILE_SIZE_200_KB, FILE_SIZE_100_KB)
        val selected = values.indexOf(config.mmsFileSizeLimit).coerceAtLeast(0)
        MaterialAlertDialogBuilder(this).setTitle(t("محدودیت حجم MMS", "MMS size limit")).setSingleChoiceItems(labels, selected) { dialog, which -> config.mmsFileSizeLimit = values[which]; dialog.dismiss(); render(MESSAGES) }.show()
    }

    private fun fontSizeLabel() = when (config.fontSize) { 1 -> t("کوچک", "Small"); 2 -> t("متوسط", "Medium"); 3 -> t("بزرگ", "Large"); else -> t("خیلی بزرگ", "Very large") }
    private fun fontLabel() = if (config.fontType == FONT_TYPE_CUSTOM) t("سفارشی: ${config.fontName}", "Custom: ${config.fontName}") else t("پیش‌فرض سیستم", "System default")
    private fun lockScreenLabel() = when (config.lockScreenVisibilitySetting) { LOCK_SCREEN_SENDER -> t("فقط فرستنده", "Sender only"); LOCK_SCREEN_NOTHING -> t("هیچ‌چیز", "Nothing"); else -> t("فرستنده و متن پیام", "Sender and message") }
    private fun mmsLimitLabel() = when (config.mmsFileSizeLimit) { FILE_SIZE_100_KB -> "100 KB"; FILE_SIZE_200_KB -> "200 KB"; FILE_SIZE_300_KB -> "300 KB"; FILE_SIZE_600_KB -> "600 KB"; FILE_SIZE_1_MB -> "1 MB"; FILE_SIZE_2_MB -> "2 MB"; else -> t("بدون محدودیت", "Unlimited") }

    private fun row(root: LinearLayout, title: String, summary: String, action: () -> Unit) {
        val card = MaterialCardView(this).apply { radius = dp(16).toFloat(); cardElevation = 0f; setCardBackgroundColor(color(com.google.android.material.R.attr.colorSurfaceVariant)); setOnClickListener { action() } }
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(14), dp(18), dp(14)) }
        content.addView(label(title, 16f, true)); content.addView(label(summary, 13f, false, color(com.google.android.material.R.attr.colorOnSurfaceVariant)))
        card.addView(content); root.addView(card, margins(0, 8))
    }

    private fun toggle(root: LinearLayout, title: String, summary: String, checked: Boolean, changed: (Boolean) -> Unit) {
        val card = MaterialCardView(this).apply { radius = dp(16).toFloat(); cardElevation = 0f; setCardBackgroundColor(color(com.google.android.material.R.attr.colorSurfaceVariant)) }
        val line = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(12), dp(8), dp(10), dp(8)) }
        val texts = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        texts.addView(label(title, 16f, true)); texts.addView(label(summary, 12f, false, color(com.google.android.material.R.attr.colorOnSurfaceVariant)))
        line.addView(texts, LinearLayout.LayoutParams(0, -2, 1f))
        line.addView(MaterialSwitch(this).apply { isChecked = checked; setOnCheckedChangeListener { _, value -> changed(value) } }, LinearLayout.LayoutParams(dp(64), -2))
        card.addView(line); root.addView(card, margins(0, 8))
    }

    private fun slider(root: LinearLayout, title: String, value: Float, changed: (Float) -> Unit) {
        val card = MaterialCardView(this).apply { radius = dp(16).toFloat(); cardElevation = 0f; setCardBackgroundColor(color(com.google.android.material.R.attr.colorSurfaceVariant)) }
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(14), dp(18), dp(8)) }
        content.addView(label(title, 16f, true))
        content.addView(Slider(this).apply { valueFrom = 1f; valueTo = 4f; stepSize = 1f; this.value = value; addOnChangeListener { _, v, _ -> changed(v) } })
        card.addView(content); root.addView(card, margins(0, 8))
    }

    private fun section(root: LinearLayout, title: String) = root.addView(label(title, 13f, true, color(androidx.appcompat.R.attr.colorPrimary)), margins(4, 10))
    private fun label(textValue: String, size: Float, bold: Boolean, textColor: Int = color(com.google.android.material.R.attr.colorOnSurface)) = TextView(this).apply { text = textValue; textSize = size; setTextColor(textColor); if (bold) typeface = Typeface.DEFAULT_BOLD }
    private fun pageTitle(page: String?) = when (page) { GENERAL -> t("عمومی", "General"); APPEARANCE -> t("ظاهر", "Appearance"); MESSAGES -> t("پیام‌ها", "Messages"); NOTIFICATIONS -> t("اعلان‌ها", "Notifications"); CONVERSATIONS -> t("گفتگوها", "Conversations"); BANKS -> t("بانک و تراکنش", "Banking"); PRIVACY -> t("حریم خصوصی و امنیت", "Privacy & security"); ABOUT -> t("درباره برنامه", "About"); else -> t("تنظیمات", "Settings") }
    private fun openAppNotificationSettings() { startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)) }
    private fun openAppDetails() { startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))) }
    private fun color(attr: Int): Int { val value = TypedValue(); theme.resolveAttribute(attr, value, true); return if (value.resourceId != 0) getColor(value.resourceId) else value.data }
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
    private fun margins(top: Int = 0, bottom: Int = 0) = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, dp(top), 0, dp(bottom)) }

    private fun applyPersianFont(view: View) {
        val typeface = ResourcesCompat.getFont(this, R.font.vazirmatn_regular) ?: return
        if (view is TextView) view.typeface = typeface
        if (view is android.view.ViewGroup) for (i in 0 until view.childCount) applyPersianFont(view.getChildAt(i))
    }
}
