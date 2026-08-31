package org.fossify.messages.activities

import android.content.Intent
import android.content.res.ColorStateList
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
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider
import org.fossify.commons.dialogs.SecurityDialog
import org.fossify.commons.helpers.FONT_TYPE_CUSTOM
import org.fossify.commons.helpers.FONT_TYPE_SYSTEM_DEFAULT
import org.fossify.commons.helpers.FontHelper
import org.fossify.commons.helpers.PROTECTION_FINGERPRINT
import org.fossify.commons.helpers.SHOW_ALL_TABS
import org.fossify.messages.helpers.BankAccountsFeature
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
import org.fossify.messages.extensions.config
import java.util.Locale
import kotlin.system.exitProcess

/**
 * Messages-owned settings UI.
 *
 * The UI is intentionally independent from the legacy Commons settings
 * screen. Each category is a separate instance of this activity.
 */
class SettingsActivity : SimpleActivity() {
    companion object {
        private const val EXTRA_PAGE = "settings_page"
        private const val PREFS = "messages_settings_ui"
        private const val KEY_DARK = "dark_mode"
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
            val fileName = resolveFontFileName(uri)
            val data = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@runCatching
            if (data.isEmpty()) return@runCatching
            if (FontHelper.saveFontData(this, data, fileName)) {
                config.fontType = FONT_TYPE_CUSTOM
                config.fontName = fileName
                FontHelper.clearCache()
                recreate()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        render(intent.getStringExtra(EXTRA_PAGE))
    }

    override fun onResume() {
        super.onResume()
        if (!isFinishing) render(intent.getStringExtra(EXTRA_PAGE))
    }

    private fun render(page: String?) {
        val surface = color(com.google.android.material.R.attr.colorSurface)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(surface)
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        val toolbar = MaterialToolbar(this).apply {
            title = pageTitle(page)
            setBackgroundColor(surface)
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
            clipToPadding = false
            setPadding(dp(20), dp(14), dp(20), dp(28))
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            textDirection = View.TEXT_DIRECTION_RTL
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
    }

    private fun home(root: LinearLayout) {
        titleBlock(root, "تنظیمات", "همه تنظیمات Messages در بخش‌های جداگانه")
        category(root, "عمومی", "زبان، تقویم و تنظیمات پایه", GENERAL, android.R.drawable.ic_menu_manage)
        category(root, "ظاهر", "تم، فونت و اندازه متن", APPEARANCE, android.R.drawable.ic_menu_view)
        category(root, "پیام‌ها", "ارسال SMS و MMS و گزارش تحویل", MESSAGES, android.R.drawable.ic_dialog_email)
        category(root, "اعلان‌ها", "اعلان‌ها و نمایش روی صفحه قفل", NOTIFICATIONS, android.R.drawable.ic_dialog_info)
        category(root, "گفتگوها", "آرشیو و سطل بازیافت", CONVERSATIONS, android.R.drawable.ic_menu_sort_by_size)
        category(root, "بانک و تراکنش", "کارت‌ها و تشخیص بانک", BANKS, android.R.drawable.ic_menu_save)
        category(root, "حریم خصوصی و امنیت", "قفل برنامه و مجوزها", PRIVACY, android.R.drawable.ic_lock_lock)
        category(root, "درباره برنامه", "اطلاعات برنامه و تنظیمات سیستم", ABOUT, android.R.drawable.ic_menu_info_details)
    }

    private fun general(root: LinearLayout) {
        section(root, "عمومی")
        row(root, "زبان برنامه", if (config.useEnglish) "English" else "فارسی") {
            chooseLanguage()
        }
        if (Build.VERSION.SDK_INT >= 33) {
            row(root, "زبان سیستم Android", Locale.getDefault().displayLanguage) {
                runCatching {
                    startActivity(Intent(Settings.ACTION_APP_LOCALE_SETTINGS).setData(Uri.parse("package:$packageName")))
                }
            }
        }
        toggle(root, "تقویم فارسی", "نمایش تاریخ‌ها با تقویم جلالی", config.usePersianCalendar) {
            config.usePersianCalendar = it
            // Date formatting is read when screens are created. Recreate so the
            // change is visible immediately instead of only after a cold start.
            recreate()
        }
        row(root, "تاریخ و ساعت", "استفاده از تنظیمات سیستم") {
            startActivity(Intent(Settings.ACTION_DATE_SETTINGS))
        }
    }

    private fun appearance(root: LinearLayout) {
        section(root, "ظاهر")
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        val dark = prefs.getBoolean(KEY_DARK, false)
        toggle(root, "حالت تاریک", "تم تیره برای رابط کاربری", dark) {
            prefs.edit().putBoolean(KEY_DARK, it).apply()
            AppCompatDelegate.setDefaultNightMode(if (it) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)
        }
        row(root, "فونت برنامه", fontLabel()) { chooseFont() }
        choice(root, "اندازه متن", fontSizeLabel()) { chooseFontSize() }
        slider(root, "اندازه متن گفتگو", config.fontSize.toFloat().coerceIn(1f, 4f)) {
            config.fontSize = it.toInt()
        }
        toggle(root, "شمارنده کاراکتر", "نمایش تعداد کاراکتر هنگام نوشتن", config.showCharacterCounter) {
            config.showCharacterCounter = it
        }
        toggle(root, "نویسه‌های ساده", "استفاده از نویسه‌های ساده‌تر", config.useSimpleCharacters) {
            config.useSimpleCharacters = it
        }
    }

    private fun messages(root: LinearLayout) {
        section(root, "پیام‌ها")
        toggle(root, "ارسال با Enter", "با زدن Enter پیام ارسال شود", config.sendOnEnter) { config.sendOnEnter = it }
        toggle(root, "گزارش تحویل", "گزارش دریافت پیام توسط گیرنده", config.enableDeliveryReports) { config.enableDeliveryReports = it }
        toggle(root, "MMS برای پیام طولانی", "پیام‌های طولانی به MMS تبدیل شوند", config.sendLongMessageMMS) { config.sendLongMessageMMS = it }
        toggle(root, "MMS برای پیام گروهی", "پیام‌های گروهی با MMS ارسال شوند", config.sendGroupMessageMMS) { config.sendGroupMessageMMS = it }
        choice(root, "محدودیت حجم MMS", mmsLimitLabel()) { chooseMmsLimit() }
    }

    private fun notifications(root: LinearLayout) {
        section(root, "اعلان‌ها")
        row(root, "تنظیمات اعلان Messages", "صدا، لرزش و رفتار اعلان") { openAppNotificationSettings() }
        row(root, "مجوز اعلان‌ها", "مدیریت مجوزهای اعلان") { openAppNotificationSettings() }
        choice(root, "نمایش روی صفحه قفل", lockScreenLabel()) { chooseLockScreen() }
    }

    private fun conversations(root: LinearLayout) {
        section(root, "گفتگوها")
        toggle(root, "نگه داشتن گفتگوهای آرشیوشده", "گفتگوهای جدید دوباره از آرشیو خارج نشوند", config.keepConversationsArchived) {
            config.keepConversationsArchived = it
        }
        toggle(root, "سطل بازیافت", "حذف گفتگوها ابتدا به سطل بازیافت منتقل شود", config.useRecycleBin) {
            config.useRecycleBin = it
        }
        row(root, "سطل بازیافت", "مدیریت گفتگوهای حذف‌شده") {
            runCatching { startActivity(Intent(this, RecycleBinConversationsActivity::class.java)) }
        }
        row(root, "گفتگوهای آرشیوشده", "مشاهده گفتگوهای آرشیوشده") {
            runCatching { startActivity(Intent(this, ArchivedConversationsActivity::class.java)) }
        }
    }

    private fun banks(root: LinearLayout) {
        section(root, "بانک و تراکنش")
        row(root, "حساب‌ها و کارت‌های بانکی", "افزودن، ویرایش و حذف کارت‌ها") {
            BankAccountsFeature.showBankAccountManager(this)
        }
        row(root, "تشخیص بانک", "بانک‌های پشتیبانی‌شده و تشخیص شماره کارت") {
            val names = org.fossify.messages.helpers.IranianBankRegistry.allBanks().map { it.persianName }.toTypedArray()
            MaterialAlertDialogBuilder(this).setTitle("بانک‌های پشتیبانی‌شده").setItems(names, null).show()
        }
    }

    private fun privacy(root: LinearLayout) {
        section(root, "حریم خصوصی و امنیت")
        row(root, "قفل برنامه", if (config.isAppPasswordProtectionOn) "فعال" else "غیرفعال") {
            openAppLock()
        }
        choice(root, "محتوای پیام روی صفحه قفل", lockScreenLabel()) { chooseLockScreen() }
        row(root, "مجوزها", "مدیریت دسترسی‌های Messages") { openAppDetails() }
    }

    private fun about(root: LinearLayout) {
        section(root, "Messages")
        row(root, "درباره برنامه", "اطلاعات نسخه و پروژه") {
            startActivity(Intent(this, HerotuxAboutActivity::class.java))
        }
        row(root, "تنظیمات سیستم Android", "اطلاعات برنامه، مجوزها و حافظه") { openAppDetails() }
    }

    private fun chooseLanguage() {
        val labels = arrayOf("فارسی", "English")
        val selected = if (config.useEnglish) 1 else 0
        MaterialAlertDialogBuilder(this)
            .setTitle("زبان برنامه")
            .setSingleChoiceItems(labels, selected) { dialog, which ->
                val english = which == 1
                if (english != config.useEnglish) {
                    config.useEnglish = english
                    dialog.dismiss()
                    // Fossify's legacy language setting intentionally restarts
                    // the process so every activity/resource gets the new locale.
                    exitProcess(0)
                } else {
                    dialog.dismiss()
                }
            }
            .show()
    }

    private fun chooseFont() {
        val labels = arrayOf("فونت پیش‌فرض سیستم", "فونت سفارشی")
        val selected = if (config.fontType == FONT_TYPE_CUSTOM) 1 else 0
        MaterialAlertDialogBuilder(this)
            .setTitle("فونت برنامه")
            .setSingleChoiceItems(labels, selected) { dialog, which ->
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
            }
            .show()
    }

    private fun resolveFontFileName(uri: Uri): String {
        val fallback = "custom_font_${System.currentTimeMillis()}.ttf"
        val name = runCatching {
            contentResolver.query(uri, arrayOf("_display_name"), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull()
        val raw = name?.substringAfterLast('/').orEmpty()
        if (raw.isBlank()) return fallback
        return raw.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { fallback }
    }

    private fun openAppLock() {
        val tabToShow = if (config.isAppPasswordProtectionOn) config.appProtectionType else SHOW_ALL_TABS
        SecurityDialog(
            activity = this,
            requiredHash = config.appPasswordHash,
            showTabIndex = tabToShow
        ) { hash, type, success ->
            if (!success) return@SecurityDialog
            val wasEnabled = config.isAppPasswordProtectionOn
            config.isAppPasswordProtectionOn = !wasEnabled
            config.appPasswordHash = if (wasEnabled) "" else hash
            config.appProtectionType = type
            if (config.isAppPasswordProtectionOn) {
                val message = if (config.appProtectionType == PROTECTION_FINGERPRINT) {
                    org.fossify.commons.R.string.fingerprint_setup_successfully
                } else {
                    org.fossify.commons.R.string.protection_setup_successfully
                }
                MaterialAlertDialogBuilder(this)
                    .setMessage(message)
                    .setPositiveButton(org.fossify.commons.R.string.ok, null)
                    .show()
            }
            render(PRIVACY)
        }
    }

    private fun category(root: LinearLayout, title: String, summary: String, page: String, icon: Int) {
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
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }
        val image = AppCompatImageView(this).apply {
            setImageResource(icon)
            imageTintList = ColorStateList.valueOf(color(com.google.android.material.R.attr.colorPrimary))
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }
        line.addView(image, LinearLayout.LayoutParams(dp(48), dp(48)))
        val texts = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL; setPadding(dp(12), 0, dp(8), 0) }
        texts.addView(label(title, 17f, true))
        texts.addView(label(summary, 13f, false, color(com.google.android.material.R.attr.colorOnSurfaceVariant)))
        line.addView(texts, LinearLayout.LayoutParams(0, -2, 1f))
        line.addView(TextView(this).apply { text = "‹"; textSize = 28f; gravity = Gravity.CENTER; setTextColor(color(com.google.android.material.R.attr.colorOnSurfaceVariant)) }, LinearLayout.LayoutParams(dp(28), dp(44)))
        card.addView(line)
        root.addView(card, margins(0, 10))
    }

    private fun row(root: LinearLayout, title: String, summary: String, action: () -> Unit) {
        val card = MaterialCardView(this).apply {
            radius = dp(16).toFloat()
            cardElevation = 0f
            setCardBackgroundColor(color(com.google.android.material.R.attr.colorSurfaceVariant))
            setOnClickListener { action() }
        }
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL; setPadding(dp(18), dp(14), dp(18), dp(14)) }
        content.addView(label(title, 16f, true))
        content.addView(label(summary, 13f, false, color(com.google.android.material.R.attr.colorOnSurfaceVariant)))
        card.addView(content)
        root.addView(card, margins(0, 8))
    }

    private fun choice(root: LinearLayout, title: String, summary: String, action: () -> Unit) = row(root, title, summary, action)

    private fun toggle(root: LinearLayout, title: String, summary: String, checked: Boolean, changed: (Boolean) -> Unit) {
        val card = MaterialCardView(this).apply {
            radius = dp(16).toFloat()
            cardElevation = 0f
            setCardBackgroundColor(color(com.google.android.material.R.attr.colorSurfaceVariant))
        }
        val line = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL; setPadding(dp(12), dp(8), dp(10), dp(8)) }
        val texts = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL }
        texts.addView(label(title, 16f, true))
        texts.addView(label(summary, 12f, false, color(com.google.android.material.R.attr.colorOnSurfaceVariant)))
        line.addView(texts, LinearLayout.LayoutParams(0, -2, 1f))
        line.addView(MaterialSwitch(this).apply { isChecked = checked; setOnCheckedChangeListener { _, value -> changed(value) } }, LinearLayout.LayoutParams(dp(64), -2))
        card.addView(line)
        root.addView(card, margins(0, 8))
    }

    private fun slider(root: LinearLayout, title: String, value: Float, changed: (Float) -> Unit) {
        val card = MaterialCardView(this).apply { radius = dp(16).toFloat(); cardElevation = 0f; setCardBackgroundColor(color(com.google.android.material.R.attr.colorSurfaceVariant)) }
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(14), dp(18), dp(8)) }
        content.addView(label(title, 16f, true))
        content.addView(Slider(this).apply { valueFrom = 1f; valueTo = 4f; stepSize = 1f; this.value = value; addOnChangeListener { _, v, _ -> changed(v) } })
        card.addView(content)
        root.addView(card, margins(0, 8))
    }

    private fun titleBlock(root: LinearLayout, title: String, subtitle: String) {
        root.addView(label(title, 30f, true, color(com.google.android.material.R.attr.colorOnSurface)), margins(0, 2))
        root.addView(label(subtitle, 14f, false, color(com.google.android.material.R.attr.colorOnSurfaceVariant)), margins(0, 20))
    }

    private fun section(root: LinearLayout, title: String) {
        root.addView(label(title, 13f, true, color(com.google.android.material.R.attr.colorPrimary)), margins(4, 10))
    }

    private fun label(textValue: String, size: Float, bold: Boolean, textColor: Int = color(com.google.android.material.R.attr.colorOnSurface)): TextView = TextView(this).apply {
        text = textValue
        textSize = size
        setTextColor(textColor)
        if (bold) typeface = android.graphics.Typeface.DEFAULT_BOLD
    }

    private fun chooseFontSize() {
        val values = arrayOf("کوچک", "متوسط", "بزرگ", "خیلی بزرگ")
        val selected = (config.fontSize - 1).coerceIn(0, 3)
        MaterialAlertDialogBuilder(this).setTitle("اندازه متن").setSingleChoiceItems(values, selected) { dialog, which ->
            config.fontSize = which + 1
            dialog.dismiss()
            render(APPEARANCE)
        }.show()
    }

    private fun chooseLockScreen() {
        val values = arrayOf("فرستنده و متن پیام", "فقط فرستنده", "هیچ‌چیز")
        val selected = when (config.lockScreenVisibilitySetting) { LOCK_SCREEN_SENDER -> 1; LOCK_SCREEN_NOTHING -> 2; else -> 0 }
        MaterialAlertDialogBuilder(this).setTitle("نمایش روی صفحه قفل").setSingleChoiceItems(values, selected) { dialog, which ->
            config.lockScreenVisibilitySetting = when (which) { 1 -> LOCK_SCREEN_SENDER; 2 -> LOCK_SCREEN_NOTHING; else -> LOCK_SCREEN_SENDER_MESSAGE }
            dialog.dismiss()
            render(NOTIFICATIONS)
        }.show()
    }

    private fun chooseMmsLimit() {
        val labels = arrayOf("بدون محدودیت", "2 MB", "1 MB", "600 KB", "300 KB", "200 KB", "100 KB")
        val values = arrayOf(FILE_SIZE_NONE, FILE_SIZE_2_MB, FILE_SIZE_1_MB, FILE_SIZE_600_KB, FILE_SIZE_300_KB, FILE_SIZE_200_KB, FILE_SIZE_100_KB)
        val selected = values.indexOf(config.mmsFileSizeLimit).coerceAtLeast(0)
        MaterialAlertDialogBuilder(this).setTitle("محدودیت حجم MMS").setSingleChoiceItems(labels, selected) { dialog, which ->
            config.mmsFileSizeLimit = values[which]
            dialog.dismiss()
            render(MESSAGES)
        }.show()
    }

    private fun fontSizeLabel() = when (config.fontSize) { 1 -> "کوچک"; 2 -> "متوسط"; 3 -> "بزرگ"; else -> "خیلی بزرگ" }
    private fun fontLabel() = if (config.fontType == FONT_TYPE_CUSTOM) "سفارشی: ${config.fontName}" else "پیش‌فرض سیستم"
    private fun lockScreenLabel() = when (config.lockScreenVisibilitySetting) { LOCK_SCREEN_SENDER -> "فقط فرستنده"; LOCK_SCREEN_NOTHING -> "هیچ‌چیز"; else -> "فرستنده و متن پیام" }
    private fun mmsLimitLabel() = when (config.mmsFileSizeLimit) { FILE_SIZE_100_KB -> "100 KB"; FILE_SIZE_200_KB -> "200 KB"; FILE_SIZE_300_KB -> "300 KB"; FILE_SIZE_600_KB -> "600 KB"; FILE_SIZE_1_MB -> "1 MB"; FILE_SIZE_2_MB -> "2 MB"; else -> "بدون محدودیت" }

    private fun openAppNotificationSettings() {
        startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
    }

    private fun openAppDetails() {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
    }

    private fun pageTitle(page: String?) = when (page) {
        GENERAL -> "عمومی"
        APPEARANCE -> "ظاهر"
        MESSAGES -> "پیام‌ها"
        NOTIFICATIONS -> "اعلان‌ها"
        CONVERSATIONS -> "گفتگوها"
        BANKS -> "بانک و تراکنش"
        PRIVACY -> "حریم خصوصی و امنیت"
        ABOUT -> "درباره برنامه"
        else -> "تنظیمات"
    }

    private fun color(attr: Int): Int {
        val value = TypedValue()
        theme.resolveAttribute(attr, value, true)
        return if (value.resourceId != 0) getColor(value.resourceId) else value.data
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun margins(top: Int = 0, bottom: Int = 0): LinearLayout.LayoutParams = LinearLayout.LayoutParams(-1, -2).apply {
        setMargins(0, dp(top), 0, dp(bottom))
    }
}
