package org.fossify.messages.activities

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.materialswitch.MaterialSwitch
import org.fossify.messages.plugins.PluginLicenseStore
import org.fossify.messages.plugins.PluginRegistry
import org.fossify.messages.plugins.SmsAutomationPlugin
import org.fossify.messages.plugins.SmsBackupProPlugin
import org.fossify.messages.plugins.SmsTemplatesPlugin
import java.text.DateFormat
import java.util.Date

class PluginStoreActivity : SimpleActivity() {
    private lateinit var content: LinearLayout
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); render() }
    private fun render() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(36), dp(20), dp(28)); setBackgroundColor(color(com.google.android.material.R.attr.colorSurface)); layoutDirection = View.LAYOUT_DIRECTION_RTL }
        root.addView(TextView(this).apply { text = "🔌 پلاگین‌های Messages"; textSize = 24f; setTextColor(color(com.google.android.material.R.attr.colorOnSurface)); gravity = Gravity.CENTER })
        root.addView(TextView(this).apply { text = "قابلیت‌های حرفه‌ای را جداگانه فعال و مدیریت کنید"; textSize = 14f; gravity = Gravity.CENTER; setTextColor(color(com.google.android.material.R.attr.colorOnSurfaceVariant)); setPadding(0, dp(6), 0, dp(20)) })
        val scroll = ScrollView(this); content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }; scroll.addView(content); root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f)); setContentView(root); PluginRegistry.all.forEach { addPlugin(it) }
    }
    private fun addPlugin(plugin: PluginRegistry.Plugin) {
        val licensed = PluginLicenseStore.isLicensed(this, plugin.id)
        val card = MaterialCardView(this).apply { radius = dp(22).toFloat(); cardElevation = 0f; strokeWidth = dp(1); strokeColor = color(com.google.android.material.R.attr.colorOutlineVariant); setCardBackgroundColor(color(com.google.android.material.R.attr.colorSurfaceVariant)) }
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(18), dp(18), dp(18)) }
        box.addView(TextView(this).apply { text = plugin.titleFa; textSize = 19f; setTextColor(color(com.google.android.material.R.attr.colorOnSurface)) })
        box.addView(TextView(this).apply { text = plugin.descriptionFa; textSize = 14f; setTextColor(color(com.google.android.material.R.attr.colorOnSurfaceVariant)); setPadding(0, dp(6), 0, dp(10)) })
        box.addView(TextView(this).apply { text = if (licensed) "فعال" else "Premium • آزمایشی ۷ روزه"; textSize = 13f; setTextColor(color(com.google.android.material.R.attr.colorPrimary)); setPadding(0, 0, 0, dp(10)) })
        if (!licensed) box.addView(Button(this).apply { text = "فعال‌سازی آزمایشی ۷ روزه"; setOnClickListener { PluginLicenseStore.activateTrial(this@PluginStoreActivity, plugin.id, 7); render() } }) else {
            val expiry = PluginLicenseStore.expiry(this, plugin.id)
            box.addView(TextView(this).apply { text = "اعتبار تا ${DateFormat.getDateInstance().format(Date(expiry))}"; textSize = 13f; setTextColor(color(com.google.android.material.R.attr.colorPrimary)); setPadding(0, 0, 0, dp(8)) })
            when (plugin.id) { PluginRegistry.SMS_AUTOMATION -> addAutomation(box); PluginRegistry.SMS_TEMPLATES -> addTemplates(box); PluginRegistry.SMS_BACKUP_PRO -> addBackup(box); PluginRegistry.SCHEDULED_SMS_PRO -> addInfo(box, "ارسال زمان‌بندی‌شده با AlarmManager اجرا می‌شود و پیام‌ها محلی نگهداری می‌شوند.") }
        }
        card.addView(box); content.addView(card, margins(0, 8))
    }
    private fun addAutomation(parent: LinearLayout) {
        parent.addView(MaterialSwitch(this).apply { text = "فعال بودن SMS Automation"; isChecked = SmsAutomationPlugin.isEnabled(this@PluginStoreActivity); setOnCheckedChangeListener { _, checked -> SmsAutomationPlugin.setEnabled(this@PluginStoreActivity, checked) } })
        parent.addView(TextView(this).apply { text = "قوانین فعال: ${SmsAutomationPlugin.getRules(this@PluginStoreActivity).count { it.enabled }}"; textSize = 13f; setTextColor(color(com.google.android.material.R.attr.colorOnSurfaceVariant)); setPadding(0, dp(6), 0, dp(8)) })
        parent.addView(Button(this).apply { text = "➕ افزودن قانون"; setOnClickListener { showAddRuleDialog() } })
        SmsAutomationPlugin.getRules(this).forEach { rule -> parent.addView(TextView(this).apply { text = "• ${rule.name.ifBlank { "بدون نام" }} — ${rule.sender.ifBlank { "همه فرستنده‌ها" }} / ${rule.containsText.ifBlank { "هر متن" }} / ${rule.action}"; textSize = 13f; setTextColor(color(com.google.android.material.R.attr.colorOnSurfaceVariant)); setPadding(dp(4), dp(4), dp(4), dp(4)) }) }
    }
    private fun addTemplates(parent: LinearLayout) {
        val list = SmsTemplatesPlugin.list(this); parent.addView(TextView(this).apply { text = "قالب‌های ذخیره‌شده: ${list.size}"; textSize = 13f; setTextColor(color(com.google.android.material.R.attr.colorOnSurfaceVariant)); setPadding(0, 0, 0, dp(8)) }); parent.addView(Button(this).apply { text = "➕ ساخت قالب جدید"; setOnClickListener { showTemplateDialog() } }); list.forEach { item -> parent.addView(TextView(this).apply { text = "• ${item.name}: ${item.body}"; textSize = 13f; setPadding(dp(4), dp(4), dp(4), dp(4)) }) }
    }
    private fun addBackup(parent: LinearLayout) { parent.addView(Button(this).apply { text = "📦 ساخت Backup"; setOnClickListener { createBackup() } }); parent.addView(Button(this).apply { text = "♻️ Restore از فایل"; setOnClickListener { restoreBackup() } }) }
    private fun addInfo(parent: LinearLayout, text: String) { parent.addView(TextView(this).apply { this.text = text; textSize = 13f; setTextColor(color(com.google.android.material.R.attr.colorOnSurfaceVariant)) }) }
    private fun showAddRuleDialog() {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(8), 0, dp(8), 0) }; val name = field("نام قانون"); val sender = field("شماره فرستنده"); val contains = field("متن شرط"); box.addView(name); box.addView(sender); box.addView(contains)
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this).setTitle("قانون SMS Automation").setView(box).setNegativeButton("لغو", null).setPositiveButton("ذخیره") { _, _ -> SmsAutomationPlugin.addRule(this, SmsAutomationPlugin.Rule(System.currentTimeMillis(), name.text.toString().trim(), sender.text.toString().trim(), contains.text.toString().trim(), SmsAutomationPlugin.Action.MARK_READ, true)); SmsAutomationPlugin.setEnabled(this, true); render() }.show()
    }
    private fun showTemplateDialog() {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(8), 0, dp(8), 0) }; val name = field("نام قالب"); val body = EditText(this).apply { hint = "متن پیام؛ مثلا سلام {name}"; minLines = 3; gravity = Gravity.TOP; textSize = 15f }; box.addView(name); box.addView(body)
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this).setTitle("قالب جدید").setView(box).setNegativeButton("لغو", null).setPositiveButton("ذخیره") { _, _ -> SmsTemplatesPlugin.save(this, SmsTemplatesPlugin.Template(System.currentTimeMillis(), name.text.toString().trim(), body.text.toString())) ; render() }.show()
    }
    private fun createBackup() { startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/json").putExtra(Intent.EXTRA_TITLE, "messages-backup.json"), 10) }
    private fun restoreBackup() { startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).setType("application/json").addCategory(Intent.CATEGORY_OPENABLE), 11) }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) { super.onActivityResult(requestCode, resultCode, data); if (resultCode != Activity.RESULT_OK || data?.data == null) return; runCatching { if (requestCode == 10) SmsBackupProPlugin.backup(this, data.data!!) else if (requestCode == 11) SmsBackupProPlugin.restore(this, data.data!!) }.onFailure { android.widget.Toast.makeText(this, "خطا: ${it.message}", android.widget.Toast.LENGTH_LONG).show() } }
    private fun field(hint: String) = EditText(this).apply { this.hint = hint; textSize = 15f; setSingleLine(true); layoutDirection = View.LAYOUT_DIRECTION_LTR; textDirection = View.TEXT_DIRECTION_LTR; setPadding(dp(8), dp(8), dp(8), dp(8)) }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun color(attr: Int): Int { val typed = obtainStyledAttributes(intArrayOf(attr)); val value = typed.getColor(0, Color.WHITE); typed.recycle(); return value }
}
