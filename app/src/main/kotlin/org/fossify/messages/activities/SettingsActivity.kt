package org.fossify.messages.activities

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider
import org.fossify.commons.activities.ManageBlockedNumbersActivity
import org.fossify.commons.dialogs.ChangeDateTimeFormatDialog
import org.fossify.commons.dialogs.ConfirmationDialog
import org.fossify.commons.dialogs.FeatureLockedDialog
import org.fossify.commons.dialogs.SecurityDialog
import org.fossify.commons.extensions.getBlockedNumbers
import org.fossify.commons.extensions.getFontSizeText
import org.fossify.commons.extensions.isOrWasThankYouInstalled
import org.fossify.commons.extensions.launchChangeAppLanguageIntent
import org.fossify.commons.extensions.launchCustomizeNotificationsIntent
import org.fossify.commons.extensions.startCustomizationActivity
import org.fossify.commons.extensions.toast
import org.fossify.commons.helpers.FONT_SIZE_EXTRA_LARGE
import org.fossify.commons.helpers.FONT_SIZE_LARGE
import org.fossify.commons.helpers.FONT_SIZE_MEDIUM
import org.fossify.commons.helpers.FONT_SIZE_SMALL
import org.fossify.commons.helpers.PROTECTION_FINGERPRINT
import org.fossify.commons.helpers.SHOW_ALL_TABS
import org.fossify.messages.R
import org.fossify.messages.extensions.config
import org.fossify.messages.extensions.emptyMessagesRecycleBin
import org.fossify.messages.extensions.messagesDB
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
import org.fossify.messages.helpers.refreshConversations
import org.fossify.messages.helpers.IranianBankRegistry
import java.util.Locale

/**
 * Messages-owned settings UI.
 *
 * This intentionally does not use the old Fossify settings layout, preference
 * rows, MyTextView/MyMaterialSwitch or MyAppBarLayout.  The screen is a small
 * Material 3 navigation hub and every settings group has its own screen.
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
    }

    private var blockedNumbersAtPause = -1
    private var recycleBinMessages = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        render(intent.getStringExtra(EXTRA_PAGE))
    }

    override fun onResume() {
        super.onResume()
        if (!isFinishing) render(intent.getStringExtra(EXTRA_PAGE), preserveScroll = true)
    }

    override fun onPause() {
        super.onPause()
        blockedNumbersAtPause = getBlockedNumbers().hashCode()
    }

    private fun render(page: String?, preserveScroll: Boolean = false) {
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            setPadding(dp(20), 0, dp(20), dp(28))
            isVerticalScrollBarEnabled = false
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(16), 0, 0)
        }
        scroll.addView(content, ViewGroup.LayoutParams(-1, -2))

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(resolveColor(com.google.android.material.R.attr.colorSurface))
        }
        val toolbar = MaterialToolbar(this).apply {
            title = pageTitle(page)
            setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
            setNavigationOnClickListener { finish() }
            elevation = 0f
            setBackgroundColor(resolveColor(com.google.android.material.R.attr.colorSurface))
        }
        root.addView(toolbar, LinearLayout.LayoutParams(-1, dp(64)))
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)

        if (page == null) buildHome(content) else when (page) {
            PAGE_GENERAL -> buildGeneral(content)
            PAGE_APPEARANCE -> buildAppearance(content)
            PAGE_MESSAGES -> buildMessages(content)
            PAGE_NOTIFICATIONS -> buildNotifications(content)
            PAGE_CONVERSATIONS -> buildConversations(content)
            PAGE_BANKS -> buildBanks(content)
            PAGE_PRIVACY -> buildPrivacy(content)
            PAGE_BACKUP -> buildBackup(content)
            PAGE_ABOUT -> buildAbout(content)
            else -> buildHome(content)
        }
    }

    private fun buildHome(root: LinearLayout) {
        val title = TextView(this).apply {
            text = "تنظیمات"
            textSize = 30f
            setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurface))
            gravity = Gravity.START
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        root.addView(title, marginParams(bottom = 6))
        root.addView(TextView(this).apply {
            text = "همه تنظیمات برنامه را در بخش‌های جداگانه مدیریت کنید"
            textSize = 14f
            setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurfaceVariant))
        }, marginParams(bottom = 20))

        settingCard(root, "عمومی", "زبان، تاریخ، اندازه متن و موارد پایه", PAGE_GENERAL, android.R.drawable.ic_menu_manage)
        settingCard(root, "ظاهر", "تم، رنگ، اندازه متن و نمایش رابط کاربری", PAGE_APPEARANCE, android.R.drawable.ic_menu_view)
        settingCard(root, "پیام‌ها", "ارسال SMS، MMS و گزارش تحویل", PAGE_MESSAGES, android.R.drawable.ic_dialog_email)
        settingCard(root, "اعلان‌ها", "اعلان پیام‌ها و نمایش روی صفحه قفل", PAGE_NOTIFICATIONS, android.R.drawable.ic_dialog_info)
        settingCard(root, "گفتگوها", "آرشیو، سطل بازیافت و نمایش پوشه‌ها", PAGE_CONVERSATIONS, android.R.drawable.ic_menu_sort_by_size)
        settingCard(root, "بانک و تراکنش", "کارت‌های بانکی و تشخیص پیامک‌های بانکی", PAGE_BANKS, android.R.drawable.ic_menu_save)
        settingCard(root, "حریم خصوصی و امنیت", "قفل برنامه و محتوای پیام روی صفحه قفل", PAGE_PRIVACY, android.R.drawable.ic_lock_lock)
        settingCard(root, "پشتیبان‌گیری", "خروجی گرفتن و وارد کردن پیام‌ها", PAGE_BACKUP, android.R.drawable.ic_menu_upload)
        settingCard(root, "درباره برنامه", "نسخه و اطلاعات Messages", PAGE_ABOUT, android.R.drawable.ic_menu_info_details)
    }

    private fun buildGeneral(root: LinearLayout) {
        section(root, "تنظیمات عمومی")
        actionRow(root, "زبان برنامه", Locale.getDefault().displayLanguage) { if (android.os.Build.VERSION.SDK_INT >= 33) launchChangeAppLanguageIntent() }
        switchRow(root, "استفاده از زبان انگلیسی", "تغییر زبان داخلی برنامه", config.useEnglish) { config.useEnglish = it; recreate() }
        actionRow(root, "فرمت تاریخ و ساعت", "تغییر نحوه نمایش تاریخ و زمان") { ChangeDateTimeFormatDialog(this) { refreshConversations() } }
        actionRow(root, "شماره‌های مسدودشده", "مدیریت شماره‌های مسدود") {
            if (isOrWasThankYouInstalled()) startActivity(Intent(this, ManageBlockedNumbersActivity::class.java)) else FeatureLockedDialog(this) { }
        }
        actionRow(root, "کلمات مسدودشده", "مدیریت فیلتر کلمات") {
            if (isOrWasThankYouInstalled()) startActivity(Intent(this, ManageBlockedKeywordsActivity::class.java)) else FeatureLockedDialog(this) { }
        }
    }

    private fun buildAppearance(root: LinearLayout) {
        section(root, "ظاهر")
        actionRow(root, "رنگ و تم برنامه", "انتخاب رنگ اصلی و ظاهر روشن/تیره") { startCustomizationActivity() }
        actionRow(root, "اندازه متن", getFontSizeText()) {
            val values = listOf(
                FONT_SIZE_SMALL to "کوچک", FONT_SIZE_MEDIUM to "متوسط",
                FONT_SIZE_LARGE to "بزرگ", FONT_SIZE_EXTRA_LARGE to "خیلی بزرگ"
            )
            MaterialAlertDialogBuilder(this).setTitle("اندازه متن")
                .setSingleChoiceItems(values.map { it.second }.toTypedArray(), values.indexOfFirst { it.first == config.fontSize }) { d, which ->
                    config.fontSize = values[which].first
                    d.dismiss()
                    render(PAGE_APPEARANCE)
                }.show()
        }
        sliderRow(root, "اندازه متن گفتگو", config.fontSize.toFloat().coerceIn(1f, 4f)) { config.fontSize = it.toInt() }
        switchRow(root, "نمایش شمارنده کاراکتر", "نمایش تعداد کاراکتر هنگام نوشتن پیام", config.showCharacterCounter) { config.showCharacterCounter = it }
        switchRow(root, "کاراکترهای ساده", "استفاده از نویسه‌های ساده‌تر برای سازگاری", config.useSimpleCharacters) { config.useSimpleCharacters = it }
    }

    private fun buildMessages(root: LinearLayout) {
        section(root, "ارسال و دریافت")
        switchRow(root, "ارسال با Enter", "با زدن Enter پیام ارسال شود", config.sendOnEnter) { config.sendOnEnter = it }
        switchRow(root, "گزارش تحویل", "گزارش دریافت شدن پیام توسط گیرنده", config.enableDeliveryReports) { config.enableDeliveryReports = it }
        switchRow(root, "ارسال پیام طولانی به‌صورت MMS", "پیام‌های طولانی به MMS تبدیل شوند", config.sendLongMessageMMS) { config.sendLongMessageMMS = it }
        switchRow(root, "پیام گروهی به‌صورت MMS", "پیام گروهی با MMS ارسال شود", config.sendGroupMessageMMS) { config.sendGroupMessageMMS = it }
        actionRow(root, "محدودیت حجم MMS", mmsLimitText()) { chooseMmsLimit() }
    }

    private fun buildNotifications(root: LinearLayout) {
        section(root, "اعلان‌ها")
        actionRow(root, "تنظیمات اعلان‌ها", "صدا، لرزش و رفتار اعلان پیام") { launchCustomizeNotificationsIntent() }
        actionRow(root, "نمایش روی صفحه قفل", lockScreenText()) { chooseLockScreen() }
        if (android.os.Build.VERSION.SDK_INT >= 33) actionRow(root, "مجوز اعلان‌ها", "مدیریت مجوز اعلان‌های Messages") {
            startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
        }
    }

    private fun buildConversations(root: LinearLayout) {
        section(root, "گفتگوها")
        switchRow(root, "نمایش پوشه‌ها", "تب‌ها و پوشه‌های گفتگو در صفحه اصلی", config.showFolders) { config.showFolders = it }
        switchRow(root, "نگه داشتن گفتگوهای آرشیوشده", "گفتگوهای جدید دوباره از آرشیو خارج نشوند", config.keepConversationsArchived) { config.keepConversationsArchived = it }
        switchRow(root, "استفاده از سطل بازیافت", "حذف گفتگوها ابتدا به سطل بازیافت منتقل شود", config.useRecycleBin) { config.useRecycleBin = it; render(PAGE_CONVERSATIONS) }
        if (config.useRecycleBin) {
            ensureRecycleBinCount()
            actionRow(root, "خالی کردن سطل بازیافت", "$recycleBinMessages پیام") {
                if (recycleBinMessages == 0) toast("سطل بازیافت خالی است") else ConfirmationDialog(this, "", R.string.empty_recycle_bin_messages_confirmation, org.fossify.commons.R.string.yes, org.fossify.commons.R.string.no) {
                    emptyMessagesRecycleBin(); recycleBinMessages = 0; render(PAGE_CONVERSATIONS)
                }
            }
        }
    }

    private fun buildBanks(root: LinearLayout) {
        section(root, "بانک و تراکنش")
        actionRow(root, "حساب‌ها و کارت‌های بانکی", "افزودن، ویرایش و حذف کارت‌های بانکی") {
            BankAccountsFeature.showBankAccounts(this)
        }
        actionRow(root, "تشخیص بانک", "تشخیص خودکار بانک از شماره کارت و پیامک") { showBankList() }
        actionRow(root, "اپراتورها", "همراه اول، ایرانسل، رایتل و مخابرات") { showOperatorInfo() }
    }

    private fun buildPrivacy(root: LinearLayout) {
        section(root, "امنیت")
        switchRow(root, "قفل برنامه", "محافظت از کل برنامه با رمز یا اثر انگشت", config.isAppPasswordProtectionOn) {
            if (it) configureProtection() else config.isAppPasswordProtectionOn = false
            render(PAGE_PRIVACY)
        }
        actionRow(root, "محتوای پیام روی صفحه قفل", lockScreenText()) { chooseLockScreen() }
    }

    private fun buildBackup(root: LinearLayout) {
        section(root, "پشتیبان‌گیری و انتقال")
        actionRow(root, "خروجی پیام‌ها", "ذخیره پیام‌ها در فایل JSON") { exportMessages() }
        actionRow(root, "وارد کردن پیام‌ها", "بازیابی پیام‌ها از JSON/XML") {
            registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) MessagesImporter(this).importMessages(uri) }.launch(arrayOf("application/json", "application/xml", "text/xml", "application/octet-stream"))
        }
    }

    private fun buildAbout(root: LinearLayout) {
        section(root, "Messages")
        actionRow(root, "درباره برنامه", "اطلاعات نسخه و پروژه") { startActivity(Intent(this, HerotuxAboutActivity::class.java)) }
        actionRow(root, "تنظیمات Android", "مجوزها و اطلاعات برنامه در سیستم") { startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))) }
    }

    private fun settingCard(root: LinearLayout, title: String, subtitle: String, page: String, icon: Int) {
        val card = MaterialCardView(this).apply {
            radius = dp(20).toFloat(); cardElevation = 0f; strokeWidth = dp(1)
            strokeColor = resolveColor(com.google.android.material.R.attr.colorOutlineVariant)
            setCardBackgroundColor(resolveColor(com.google.android.material.R.attr.colorSurfaceContainer))
            setOnClickListener { startActivity(Intent(this@SettingsActivity, SettingsActivity::class.java).putExtra(EXTRA_PAGE, page)) }
        }
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(16), dp(14), dp(16), dp(14)) }
        val image = AppCompatImageView(this).apply { setImageResource(icon); imageTintList = android.content.res.ColorStateList.valueOf(resolveColor(com.google.android.material.R.attr.colorPrimary)); setPadding(dp(8), dp(8), dp(8), dp(8)); background = circleBackground() }
        row.addView(image, LinearLayout.LayoutParams(dp(48), dp(48)))
        val texts = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL; textDirection = View.TEXT_DIRECTION_RTL; setPadding(dp(14), 0, dp(8), 0) }
        texts.addView(TextView(this).apply { text = title; textSize = 17f; setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurface)); typeface = android.graphics.Typeface.DEFAULT_BOLD }, LinearLayout.LayoutParams(-1, -2))
        texts.addView(TextView(this).apply { text = subtitle; textSize = 13f; setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurfaceVariant)); setPadding(0, dp(3), 0, 0) }, LinearLayout.LayoutParams(-1, -2))
        row.addView(texts, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(TextView(this).apply { text = "‹"; textSize = 28f; setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurfaceVariant)) }, LinearLayout.LayoutParams(dp(28), dp(40)))
        card.addView(row); root.addView(card, marginParams(top = 0, bottom = 12))
    }

    private fun actionRow(root: LinearLayout, title: String, summary: String, action: () -> Unit) {
        val card = MaterialCardView(this).apply {
            radius = dp(16).toFloat(); cardElevation = 0f; strokeWidth = 0
            setCardBackgroundColor(resolveColor(com.google.android.material.R.attr.colorSurfaceContainerLow)); setOnClickListener { action() }
        }
        val row = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL; textDirection = View.TEXT_DIRECTION_RTL; setPadding(dp(18), dp(15), dp(18), dp(15)) }
        row.addView(TextView(this).apply { text = title; textSize = 16f; setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurface)); typeface = android.graphics.Typeface.DEFAULT_BOLD })
        row.addView(TextView(this).apply { text = summary; textSize = 13f; setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurfaceVariant)); setPadding(0, dp(4), 0, 0) })
        card.addView(row); root.addView(card, marginParams(bottom = 8))
    }

    private fun switchRow(root: LinearLayout, title: String, summary: String, checked: Boolean, changed: (Boolean) -> Unit) {
        val card = MaterialCardView(this).apply { radius = dp(16).toFloat(); cardElevation = 0f; strokeWidth = 0; setCardBackgroundColor(resolveColor(com.google.android.material.R.attr.colorSurfaceContainerLow)) }
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL; setPadding(dp(16), dp(10), dp(10), dp(10)) }
        val texts = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL; textDirection = View.TEXT_DIRECTION_RTL }
        texts.addView(TextView(this).apply { text = title; textSize = 16f; setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurface)); typeface = android.graphics.Typeface.DEFAULT_BOLD })
        texts.addView(TextView(this).apply { text = summary; textSize = 12f; setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurfaceVariant)); setPadding(0, dp(3), 0, 0) })
        row.addView(texts, LinearLayout.LayoutParams(0, -2, 1f))
        val sw = MaterialSwitch(this).apply { isChecked = checked; setOnCheckedChangeListener { _, value -> changed(value) } }
        row.addView(sw, LinearLayout.LayoutParams(dp(64), -2))
        card.addView(row); root.addView(card, marginParams(bottom = 8))
    }

    private fun sliderRow(root: LinearLayout, title: String, value: Float, changed: (Float) -> Unit) {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(14), dp(18), dp(8)) }
        box.addView(TextView(this).apply { text = title; textSize = 16f; setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurface)); typeface = android.graphics.Typeface.DEFAULT_BOLD })
        val slider = Slider(this).apply { valueFrom = 1f; valueTo = 4f; stepSize = 1f; this.value = value; addOnChangeListener { _, v, _ -> changed(v) } }
        box.addView(slider)
        root.addView(box, marginParams(bottom = 8))
    }

    private fun section(root: LinearLayout, title: String) { root.addView(TextView(this).apply { text = title; textSize = 13f; setTextColor(resolveColor(com.google.android.material.R.attr.colorPrimary)); typeface = android.graphics.Typeface.DEFAULT_BOLD; setPadding(dp(4), dp(8), dp(4), dp(10)) }, marginParams(bottom = 2)) }

    private fun pageTitle(page: String?) = when (page) {
        PAGE_GENERAL -> "عمومی"; PAGE_APPEARANCE -> "ظاهر"; PAGE_MESSAGES -> "پیام‌ها"; PAGE_NOTIFICATIONS -> "اعلان‌ها"; PAGE_CONVERSATIONS -> "گفتگوها"; PAGE_BANKS -> "بانک و تراکنش"; PAGE_PRIVACY -> "حریم خصوصی و امنیت"; PAGE_BACKUP -> "پشتیبان‌گیری"; PAGE_ABOUT -> "درباره برنامه"; else -> "تنظیمات"
    }

    private fun chooseLockScreen() {
        val values = arrayOf("فرستنده و متن پیام", "فقط فرستنده", "هیچ‌چیز")
        val selected = when (config.lockScreenVisibilitySetting) { LOCK_SCREEN_SENDER -> 1; LOCK_SCREEN_NOTHING -> 2; else -> 0 }
        MaterialAlertDialogBuilder(this).setTitle("نمایش روی صفحه قفل").setSingleChoiceItems(values, selected) { d, which ->
            config.lockScreenVisibilitySetting = when (which) { 1 -> LOCK_SCREEN_SENDER; 2 -> LOCK_SCREEN_NOTHING; else -> LOCK_SCREEN_SENDER_MESSAGE }; d.dismiss(); render(PAGE_NOTIFICATIONS)
        }.show()
    }

    private fun lockScreenText() = when (config.lockScreenVisibilitySetting) { LOCK_SCREEN_SENDER -> "فقط فرستنده"; LOCK_SCREEN_NOTHING -> "هیچ‌چیز"; else -> "فرستنده و متن پیام" }

    private fun chooseMmsLimit() {
        val values = arrayOf("بدون محدودیت", "2 MB", "1 MB", "600 KB", "300 KB", "200 KB", "100 KB")
        val raw = arrayOf(FILE_SIZE_NONE, FILE_SIZE_2_MB, FILE_SIZE_1_MB, FILE_SIZE_600_KB, FILE_SIZE_300_KB, FILE_SIZE_200_KB, FILE_SIZE_100_KB)
        val selected = raw.indexOf(config.mmsFileSizeLimit).coerceAtLeast(0)
        MaterialAlertDialogBuilder(this).setTitle("محدودیت حجم MMS").setSingleChoiceItems(values, selected) { d, which -> config.mmsFileSizeLimit = raw[which]; d.dismiss(); render(PAGE_MESSAGES) }.show()
    }

    private fun mmsLimitText() = when (config.mmsFileSizeLimit) { FILE_SIZE_100_KB -> "100 KB"; FILE_SIZE_200_KB -> "200 KB"; FILE_SIZE_300_KB -> "300 KB"; FILE_SIZE_600_KB -> "600 KB"; FILE_SIZE_1_MB -> "1 MB"; FILE_SIZE_2_MB -> "2 MB"; else -> "بدون محدودیت" }

    private fun configureProtection() {
        val tab = if (config.isAppPasswordProtectionOn) config.appProtectionType else SHOW_ALL_TABS
        SecurityDialog(activity = this, requiredHash = config.appPasswordHash, showTabIndex = tab) { hash, type, success ->
            if (success) { config.isAppPasswordProtectionOn = true; config.appPasswordHash = hash; config.appProtectionType = type; render(PAGE_PRIVACY) }
        }
    }

    private fun showBankList() {
        val banks = IranianBankRegistry.banks
        val labels = banks.map { it.persianName }.toTypedArray()
        MaterialAlertDialogBuilder(this).setTitle("بانک‌های پشتیبانی‌شده").setItems(labels, null).show()
    }

    private fun showOperatorInfo() {
        MaterialAlertDialogBuilder(this).setTitle("اپراتورها").setMessage("همراه اول\nایرانسل\nرایتل\nمخابرات ایران").setPositiveButton("باشه", null).show()
    }

    private fun ensureRecycleBinCount() { recycleBinMessages = messagesDB.getArchivedCount() }

    private fun exportMessages() { toast("برای خروجی گرفتن از پیام‌ها از ابزار Export استفاده کنید") }

    private fun circleBackground() = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(resolveColor(com.google.android.material.R.attr.colorSecondaryContainer)) }
    private fun marginParams(top: Int = 0, bottom: Int = 0) = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(top); bottomMargin = dp(bottom) }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun resolveColor(attr: Int): Int { val a = theme.obtainStyledAttributes(intArrayOf(attr)); val color = a.getColor(0, Color.TRANSPARENT); a.recycle(); return color }
}
