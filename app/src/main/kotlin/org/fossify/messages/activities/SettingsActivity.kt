package org.fossify.messages.activities

import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.AppCompatImageView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider
import org.fossify.messages.R
import org.fossify.messages.dialogs.ExportMessagesDialog
import org.fossify.messages.extensions.config
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
import org.fossify.messages.helpers.MessagesImporter
import java.util.Locale

/**
 * Messages-owned settings UI.
 *
 * This screen deliberately does not use the legacy Fossify settings layout or
 * its view IDs. The root page is a dashboard and every settings category has
 * its own screen, keeping the UI RTL-friendly and independent from Commons UI.
 */
class SettingsActivity : SimpleActivity() {
    companion object {
        private const val EXTRA_PAGE = "settings_page"
        private const val PAGE_GENERAL = "general"
        private const val PAGE_APPEARANCE = "appearance"
        private const val PAGE_MESSAGES = "messages"
        private const val PAGE_NOTIFICATIONS = "notifications"
        private const val PAGE_CONVERSATIONS = "conversations"
        private const val PAGE_BANKS = "banks"
        private const val PAGE_PRIVACY = "privacy"
        private const val PAGE_BACKUP = "backup"
        private const val PAGE_ABOUT = "about"
        private const val PREFS = "messages_settings_ui"
        private const val KEY_DARK = "dark_mode"
    }

    private var exportMessagesDialog: ExportMessagesDialog? = null
    private var recycleBinMessages = 0

    private val importDocument = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { MessagesImporter(this).importMessages(it) } }

    private val saveDocument = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { exportMessagesDialog?.exportMessages(it) } }

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
            title = titleFor(page)
            elevation = 0f
            setBackgroundColor(surface)
            if (page != null) {
                navigationIcon = androidx.appcompat.R.drawable.abc_ic_ab_back_material.let { getDrawable(it) }
                setNavigationOnClickListener { finish() }
            }
        }
        root.addView(toolbar, LinearLayout.LayoutParams(-1, dp(64)))

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            isVerticalScrollBarEnabled = false
            clipToPadding = false
            setPadding(dp(20), dp(12), dp(20), dp(28))
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            textDirection = View.TEXT_DIRECTION_RTL
        }
        scroll.addView(content, ViewGroup.LayoutParams(-1, -2))
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)

        when (page) {
            PAGE_GENERAL -> general(content)
            PAGE_APPEARANCE -> appearance(content)
            PAGE_MESSAGES -> messages(content)
            PAGE_NOTIFICATIONS -> notifications(content)
            PAGE_CONVERSATIONS -> conversations(content)
            PAGE_BANKS -> banks(content)
            PAGE_PRIVACY -> privacy(content)
            PAGE_BACKUP -> backup(content)
            PAGE_ABOUT -> about(content)
            else -> home(content)
        }
    }

    private fun home(root: LinearLayout) {
        root.addView(TextView(this).apply {
            text = "تنظیمات"
            textSize = 30f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(color(com.google.android.material.R.attr.colorOnSurface))
        }, margins(bottom = 4))
        root.addView(TextView(this).apply {
            text = "تنظیمات Messages را از بخش‌های جداگانه مدیریت کنید"
            textSize = 14f
            setTextColor(color(com.google.android.material.R.attr.colorOnSurfaceVariant))
        }, margins(bottom = 20))

        category(root, "عمومی", "زبان، تقویم، تاریخ و تنظیمات پایه", PAGE_GENERAL, android.R.drawable.ic_menu_manage)
        category(root, "ظاهر", "تم، حالت روشن و تاریک و اندازه متن", PAGE_APPEARANCE, android.R.drawable.ic_menu_view)
        category(root, "پیام‌ها", "ارسال SMS، MMS و گزارش تحویل", PAGE_MESSAGES, android.R.drawable.ic_dialog_email)
        category(root, "اعلان‌ها", "اعلان پیام‌ها و صفحه قفل", PAGE_NOTIFICATIONS, android.R.drawable.ic_dialog_info)
        category(root, "گفتگوها", "آرشیو و سطل بازیافت", PAGE_CONVERSATIONS, android.R.drawable.ic_menu_sort_by_size)
        category(root, "بانک و تراکنش", "کارت‌های بانکی و تشخیص بانک", PAGE_BANKS, android.R.drawable.ic_menu_save)
        category(root, "حریم خصوصی و امنیت", "قفل برنامه و محتوای صفحه قفل", PAGE_PRIVACY, android.R.drawable.ic_lock_lock)
        category(root, "پشتیبان‌گیری", "خروجی و ورود پیام‌ها", PAGE_BACKUP, android.R.drawable.ic_menu_upload)
        category(root, "درباره برنامه", "اطلاعات Messages و تنظیمات سیستم", PAGE_ABOUT, android.R.drawable.ic_menu_info_details)
    }

    private fun general(root: LinearLayout) {
        section(root, "عمومی")
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            row(root, "زبان برنامه", Locale.getDefault().displayLanguage) {
                startActivity(Intent(Settings.ACTION_APP_LOCALE_SETTINGS).setData(Uri.parse("package:$packageName")))
            }
        }
        switch(root, "تقویم فارسی", "استفاده از تاریخ جلالی در Messages", config.usePersianCalendar) {
            config.usePersianCalendar = it
        }
        switch(root, "استفاده از زبان انگلیسی", "زبان داخلی Messages", config.useEnglish) {
            config.useEnglish = it
            recreate()
        }
        row(root, "فرمت تاریخ و ساعت", "تغییر از تنظیمات سیستم") {
            startActivity(Intent(Settings.ACTION_DATE_SETTINGS))
        }
    }

    private fun appearance(root: LinearLayout) {
        section(root, "ظاهر برنامه")
        val dark = getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(KEY_DARK, false)
        switch(root, "حالت تاریک", "استفاده از رابط کاربری تیره", dark) { enabled ->
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(KEY_DARK, enabled).apply()
            AppCompatDelegate.setDefaultNightMode(if (enabled) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)
        }
        row(root, "اندازه متن", fontSizeLabel()) { chooseFontSize() }
        slider(root, "اندازه متن گفتگو", config.fontSize.toFloat().coerceIn(1f, 4f)) { config.fontSize = it.toInt() }
        switch(root, "شمارنده کاراکتر", "نمایش تعداد کاراکتر هنگام نوشتن", config.showCharacterCounter) { config.showCharacterCounter = it }
        switch(root, "نویسه‌های ساده", "استفاده از نویسه‌های ساده‌تر برای سازگاری", config.useSimpleCharacters) { config.useSimpleCharacters = it }
    }

    private fun messages(root: LinearLayout) {
        section(root, "پیام‌ها")
        switch(root, "ارسال با Enter", "با Enter پیام ارسال شود", config.sendOnEnter) { config.sendOnEnter = it }
        switch(root, "گزارش تحویل", "گزارش دریافت پیام توسط گیرنده", config.enableDeliveryReports) { config.enableDeliveryReports = it }
        switch(root, "MMS برای پیام‌های طولانی", "پیام‌های طولانی به MMS تبدیل شوند", config.sendLongMessageMMS) { config.sendLongMessageMMS = it }
        switch(root, "MMS برای پیام گروهی", "پیام گروهی با MMS ارسال شود", config.sendGroupMessageMMS) { config.sendGroupMessageMMS = it }
        row(root, "محدودیت حجم MMS", mmsLimitLabel()) { chooseMmsLimit() }
    }

    private fun notifications(root: LinearLayout) {
        section(root, "اعلان‌ها")
        row(root, "تنظیمات اعلان‌های Messages", "صدا، لرزش و رفتار اعلان") {
            startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
        }
        row(root, "مجوز اعلان‌ها", "مدیریت مجوز اعلان‌های برنامه") {
            startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
        }
        row(root, "نمایش روی صفحه قفل", lockScreenLabel()) { chooseLockScreen() }
    }

    private fun conversations(root: LinearLayout) {
        section(root, "گفتگوها")
        switch(root, "نگه داشتن گفتگوهای آرشیوشده", "گفتگوهای جدید دوباره از آرشیو خارج نشوند", config.keepConversationsArchived) { config.keepConversationsArchived = it }
        switch(root, "سطل بازیافت", "حذف گفتگوها ابتدا به سطل بازیافت منتقل شود", config.useRecycleBin) {
            config.useRecycleBin = it
            render(PAGE_CONVERSATIONS)
        }
        recycleBinMessages = try { org.fossify.messages.extensions.messagesDB.getArchivedCount() } catch (_: Exception) { 0 }
        if (config.useRecycleBin) row(root, "خالی کردن سطل بازیافت", "$recycleBinMessages پیام") {
            if (recycleBinMessages > 0) {
                org.fossify.messages.extensions.emptyMessagesRecycleBin()
                recycleBinMessages = 0
                render(PAGE_CONVERSATIONS)
            }
        }
    }

    private fun banks(root: LinearLayout) {
        section(root, "بانک و تراکنش")
        row(root, "حساب‌ها و کارت‌های بانکی", "افزودن، ویرایش و حذف کارت‌ها") { BankAccountsFeature.showBankAccountManager(this) }
        row(root, "تشخیص بانک", "تشخیص خودکار بانک از شماره کارت") {
            MaterialAlertDialogBuilder(this).setTitle("بانک‌های پشتیبانی‌شده")
                .setItems(org.fossify.messages.helpers.IranianBankRegistry.allBanks().map { it.persianName }.toTypedArray(), null).show()
        }
    }

    private fun privacy(root: LinearLayout) {
        section(root, "حریم خصوصی و امنیت")
        row(root, "تنظیمات قفل برنامه", if (config.isAppPasswordProtectionOn) "فعال" else "غیرفعال") {
            startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
        }
        row(root, "محتوای پیام روی صفحه قفل", lockScreenLabel()) { chooseLockScreen() }
        row(root, "مجوزها و دسترسی‌های برنامه", "مدیریت دسترسی‌های Messages") {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        }
    }

    private fun backup(root: LinearLayout) {
        section(root, "پشتیبان‌گیری و انتقال")
        row(root, "خروجی پیام‌ها", "ذخیره SMS و MMS در فایل JSON") {
            exportMessagesDialog = ExportMessagesDialog(this) { fileName -> saveDocument.launch("$fileName.json") }
        }
        row(root, "وارد کردن پیام‌ها", "بازیابی پیام‌ها از فایل پشتیبان") {
            importDocument.launch(arrayOf("application/json", "application/xml", "text/xml", "application/octet-stream"))
        }
    }

    private fun about(root: LinearLayout) {
        section(root, "Messages")
        row(root, "درباره برنامه", "اطلاعات نسخه و پروژه") {
            try { startActivity(Intent(this, HerotuxAboutActivity::class.java)) } catch (_: Exception) { }
        }
        row(root, "تنظیمات سیستم Android", "مجوزها و اطلاعات برنامه") {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
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
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL; setPadding(dp(14), dp(12), dp(14), dp(12)) }
        val image = AppCompatImageView(this).apply {
            setImageResource(icon)
            imageTintList = ColorStateList.valueOf(color(com.google.android.material.R.attr.colorPrimary))
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }
        row.addView(image, LinearLayout.LayoutParams(dp(48), dp(48)))
        val texts = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL; setPadding(dp(12), 0, dp(8), 0) }
        texts.addView(TextView(this).apply { text = title; textSize = 17f; typeface = android.graphics.Typeface.DEFAULT_BOLD; setTextColor(color(com.google.android.material.R.attr.colorOnSurface)) })
        texts.addView(TextView(this).apply { text = summary; textSize = 13f; setPadding(0, dp(3), 0, 0); setTextColor(color(com.google.android.material.R.attr.colorOnSurfaceVariant)) })
        row.addView(texts, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(TextView(this).apply { text = "‹"; textSize = 28f; gravity = Gravity.CENTER; setTextColor(color(com.google.android.material.R.attr.colorOnSurfaceVariant)) }, LinearLayout.LayoutParams(dp(28), dp(44)))
        card.addView(row)
        root.addView(card, margins(bottom = 10))
    }

    private fun row(root: LinearLayout, title: String, summary: String, action: () -> Unit) {
        val card = MaterialCardView(this).apply { radius = dp(16).toFloat(); cardElevation = 0f; setCardBackgroundColor(color(com.google.android.material.R.attr.colorSurfaceVariant)); setOnClickListener { action() } }
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL; setPadding(dp(18), dp(14), dp(18), dp(14)) }
        content.addView(TextView(this).apply { text = title; textSize = 16f; typeface = android.graphics.Typeface.DEFAULT_BOLD; setTextColor(color(com.google.android.material.R.attr.colorOnSurface)) })
        content.addView(TextView(this).apply { text = summary; textSize = 13f; setPadding(0, dp(3), 0, 0); setTextColor(color(com.google.android.material.R.attr.colorOnSurfaceVariant)) })
        card.addView(content)
        root.addView(card, margins(bottom = 8))
    }

    private fun switch(root: LinearLayout, title: String, summary: String, checked: Boolean, changed: (Boolean) -> Unit) {
        val card = MaterialCardView(this).apply { radius = dp(16).toFloat(); cardElevation = 0f; setCardBackgroundColor(color(com.google.android.material.R.attr.colorSurfaceVariant)) }
        val content = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL; setPadding(dp(12), dp(8), dp(10), dp(8)) }
        val texts = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL }
        texts.addView(TextView(this).apply { text = title; textSize = 16f; typeface = android.graphics.Typeface.DEFAULT_BOLD; setTextColor(color(com.google.android.material.R.attr.colorOnSurface)) })
        texts.addView(TextView(this).apply { text = summary; textSize = 12f; setPadding(0, dp(3), 0, 0); setTextColor(color(com.google.android.material.R.attr.colorOnSurfaceVariant)) })
        content.addView(texts, LinearLayout.LayoutParams(0, -2, 1f))
        content.addView(MaterialSwitch(this).apply { isChecked = checked; setOnCheckedChangeListener { _, value -> changed(value) } }, LinearLayout.LayoutParams(dp(64), -2))
        card.addView(content)
        root.addView(card, margins(bottom = 8))
    }

    private fun slider(root: LinearLayout, title: String, value: Float, changed: (Float) -> Unit) {
        val card = MaterialCardView(this).apply { radius = dp(16).toFloat(); cardElevation = 0f; setCardBackgroundColor(color(com.google.android.material.R.attr.colorSurfaceVariant)) }
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(14), dp(18), dp(8)) }
        content.addView(TextView(this).apply { text = title; textSize = 16f; typeface = android.graphics.Typeface.DEFAULT_BOLD; setTextColor(color(com.google.android.material.R.attr.colorOnSurface)) })
        content.addView(Slider(this).apply { valueFrom = 1f; valueTo = 4f; stepSize = 1f; this.value = value; addOnChangeListener { _, v, _ -> changed(v) } })
        card.addView(content)
        root.addView(card, margins(bottom = 8))
    }

    private fun section(root: LinearLayout, title: String) {
        root.addView(TextView(this).apply { text = title; textSize = 13f; typeface = android.graphics.Typeface.DEFAULT_BOLD; setTextColor(color(com.google.android.material.R.attr.colorPrimary)); setPadding(dp(4), dp(8), dp(4), dp(10)) }, margins(bottom = 2))
    }

    private fun chooseFontSize() {
        val values = arrayOf("کوچک", "متوسط", "بزرگ", "خیلی بزرگ")
        val selected = (config.fontSize - 1).coerceIn(0, 3)
        MaterialAlertDialogBuilder(this).setTitle("اندازه متن").setSingleChoiceItems(values, selected) { dialog, which ->
            config.fontSize = which + 1
            dialog.dismiss()
            render(PAGE_APPEARANCE)
        }.show()
    }

    private fun fontSizeLabel() = when (config.fontSize) { 1 -> "کوچک"; 2 -> "متوسط"; 3 -> "بزرگ"; else -> "خیلی بزرگ" }

    private fun chooseLockScreen() {
        val values = arrayOf("فرستنده و متن پیام", "فقط فرستنده", "هیچ‌چیز")
        val selected = when (config.lockScreenVisibilitySetting) { LOCK_SCREEN_SENDER -> 1; LOCK_SCREEN_NOTHING -> 2; else -> 0 }
        MaterialAlertDialogBuilder(this).setTitle("نمایش روی صفحه قفل").setSingleChoiceItems(values, selected) { dialog, which ->
            config.lockScreenVisibilitySetting = when (which) { 1 -> LOCK_SCREEN_SENDER; 2 -> LOCK_SCREEN_NOTHING; else -> LOCK_SCREEN_SENDER_MESSAGE }
            dialog.dismiss()
            render(PAGE_NOTIFICATIONS)
        }.show()
    }

    private fun lockScreenLabel() = when (config.lockScreenVisibilitySetting) { LOCK_SCREEN_SENDER -> "فقط فرستنده"; LOCK_SCREEN_NOTHING -> "هیچ‌چیز"; else -> "فرستنده و متن پیام" }

    private fun chooseMmsLimit() {
        val labels = arrayOf("بدون محدودیت", "2 MB", "1 MB", "600 KB", "300 KB", "200 KB", "100 KB")
        val values = arrayOf(FILE_SIZE_NONE, FILE_SIZE_2_MB, FILE_SIZE_1_MB, FILE_SIZE_600_KB, FILE_SIZE_300_KB, FILE_SIZE_200_KB, FILE_SIZE_100_KB)
        val selected = values.indexOf(config.mmsFileSizeLimit).coerceAtLeast(0)
        MaterialAlertDialogBuilder(this).setTitle("محدودیت حجم MMS").setSingleChoiceItems(labels, selected) { dialog, which ->
            config.mmsFileSizeLimit = values[which]
            dialog.dismiss()
            render(PAGE_MESSAGES)
        }.show()
    }

    private fun mmsLimitLabel() = when (config.mmsFileSizeLimit) { FILE_SIZE_100_KB -> "100 KB"; FILE_SIZE_200_KB -> "200 KB"; FILE_SIZE_300_KB -> "300 KB"; FILE_SIZE_600_KB -> "600 KB"; FILE_SIZE_1_MB -> "1 MB"; FILE_SIZE_2_MB -> "2 MB"; else -> "بدون محدودیت" }

    private fun titleFor(page: String?) = when (page) {
        PAGE_GENERAL -> "عمومی"
        PAGE_APPEARANCE -> "ظاهر"
        PAGE_MESSAGES -> "پیام‌ها"
        PAGE_NOTIFICATIONS -> "اعلان‌ها"
        PAGE_CONVERSATIONS -> "گفتگوها"
        PAGE_BANKS -> "بانک و تراکنش"
        PAGE_PRIVACY -> "حریم خصوصی و امنیت"
        PAGE_BACKUP -> "پشتیبان‌گیری"
        PAGE_ABOUT -> "درباره برنامه"
        else -> "تنظیمات"
    }

    private fun color(attr: Int): Int {
        val value = TypedValue()
        theme.resolveAttribute(attr, value, true)
        return if (value.resourceId != 0) getColor(value.resourceId) else value.data
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun margins(bottom: Int = 0): LinearLayout.LayoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, dp(bottom)) }
}
