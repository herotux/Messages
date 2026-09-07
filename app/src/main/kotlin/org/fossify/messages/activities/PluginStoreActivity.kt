package org.fossify.messages.activities

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import android.widget.Toast
import com.google.android.material.card.MaterialCardView
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.fossify.messages.plugins.AiAssistantPlugin
import org.fossify.messages.plugins.PluginLicenseStore
import org.fossify.messages.plugins.PluginRegistry
import org.fossify.messages.plugins.ScheduledSmsPlugin
import org.fossify.messages.plugins.SmsAutomationPlugin
import org.fossify.messages.plugins.SmsBackupProPlugin
import org.fossify.messages.plugins.SmsTemplatesPlugin
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class PluginStoreActivity : SimpleActivity() {
    private lateinit var content: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); render() }

    private fun render() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(36), dp(20), dp(28)); layoutDirection = View.LAYOUT_DIRECTION_RTL }
        root.addView(TextView(this).apply { text = "🔌 پلاگین‌های Messages"; textSize = 24f; gravity = Gravity.CENTER })
        root.addView(TextView(this).apply { text = "قابلیت‌های حرفه‌ای را جداگانه فعال و مدیریت کنید"; textSize = 14f; gravity = Gravity.CENTER; setPadding(0, dp(6), 0, dp(20)) })
        val scroll = ScrollView(this); content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }; scroll.addView(content); root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f)); setContentView(root)
        PluginRegistry.all.forEach { addPlugin(it) }
    }

    private fun addPlugin(plugin: PluginRegistry.Plugin) {
        val licensed = PluginLicenseStore.isLicensed(this, plugin.id)
        val card = MaterialCardView(this).apply { radius = dp(22).toFloat(); cardElevation = 0f; strokeWidth = dp(1); strokeColor = color(com.google.android.material.R.attr.colorOutlineVariant) }
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(18), dp(18), dp(18)) }
        box.addView(TextView(this).apply { text = plugin.titleFa; textSize = 19f })
        box.addView(TextView(this).apply { text = plugin.descriptionFa; textSize = 14f; setPadding(0, dp(6), 0, dp(10)) })
        if (!licensed) {
            box.addView(TextView(this).apply { text = "Premium • آزمایشی ۷ روزه"; textSize = 13f; setPadding(0, 0, 0, dp(8)) })
            box.addView(Button(this).apply { text = "فعال‌سازی آزمایشی ۷ روزه"; setOnClickListener { PluginLicenseStore.activateTrial(this@PluginStoreActivity, plugin.id, 7); render() } })
        } else {
            box.addView(TextView(this).apply { text = "فعال • اعتبار تا ${DateFormat.getDateInstance().format(Date(PluginLicenseStore.expiry(this@PluginStoreActivity, plugin.id)))}"; textSize = 13f; setPadding(0, 0, 0, dp(8)) })
            when (plugin.id) {
                PluginRegistry.SMS_AUTOMATION -> addAutomation(box)
                PluginRegistry.SMS_TEMPLATES -> addTemplates(box)
                PluginRegistry.SMS_BACKUP_PRO -> addBackup(box)
                PluginRegistry.SCHEDULED_SMS_PRO -> addScheduledSms(box)
                PluginRegistry.AI_ASSISTANT -> addAi(box)
            }
        }
        card.addView(box); content.addView(card, margins(0, 8))
    }

    private fun addAi(parent: LinearLayout) {
        val configured = AiAssistantPlugin.getConfig(this).apiKey.isNotBlank()
        parent.addView(TextView(this).apply { text = if (configured) "اتصال AI تنظیم شده است" else "API هنوز تنظیم نشده است"; textSize = 13f; setPadding(0, 0, 0, dp(8)) })
        parent.addView(Button(this).apply { text = "🤖 باز کردن دستیار هوش مصنوعی"; setOnClickListener { startActivity(Intent(this@PluginStoreActivity, AiAssistantActivity::class.java)) } })
    }

    private fun addAutomation(parent: LinearLayout) {
        val rules = SmsAutomationPlugin.getRules(this)
        parent.addView(MaterialSwitch(this).apply { text = "فعال بودن SMS Automation"; isChecked = SmsAutomationPlugin.isEnabled(this@PluginStoreActivity); setOnCheckedChangeListener { _, checked -> SmsAutomationPlugin.setEnabled(this@PluginStoreActivity, checked) } })
        parent.addView(TextView(this).apply { text = "قوانین فعال: ${rules.count { it.enabled }} از ${rules.size}"; textSize = 13f; setPadding(0, dp(6), 0, dp(8)) })
        parent.addView(Button(this).apply { text = "➕ افزودن قانون"; setOnClickListener { showRuleDialog() } })
        rules.forEach { rule ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(4), dp(10), dp(4), dp(4)) }
            row.addView(TextView(this).apply { text = rule.name.ifBlank { "بدون نام" }; textSize = 15f })
            row.addView(TextView(this).apply { text = "فرستنده: ${rule.sender.ifBlank { "همه" }}\nشرط: ${rule.containsText.ifBlank { "هر متن" }}\nعملیات: ${actionLabel(rule.action)}"; textSize = 13f; setPadding(0, dp(4), 0, dp(4)) })
            val controls = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL }
            controls.addView(MaterialSwitch(this).apply { text = "فعال"; isChecked = rule.enabled; setOnCheckedChangeListener { _, checked -> SmsAutomationPlugin.setRuleEnabled(this@PluginStoreActivity, rule.id, checked); render() } }, LinearLayout.LayoutParams(0, -2, 1f))
            controls.addView(Button(this).apply { text = "ویرایش"; setOnClickListener { showRuleDialog(rule) } })
            controls.addView(Button(this).apply { text = "حذف"; setOnClickListener { MaterialAlertDialogBuilder(this@PluginStoreActivity).setTitle("حذف قانون").setMessage("این قانون حذف شود؟").setNegativeButton("لغو", null).setPositiveButton("حذف") { _, _ -> SmsAutomationPlugin.removeRule(this@PluginStoreActivity, rule.id); render() }.show() } })
            row.addView(controls); parent.addView(row)
        }
    }

    private fun actionLabel(action: SmsAutomationPlugin.Action) = when (action) { SmsAutomationPlugin.Action.MARK_READ -> "علامت‌گذاری خوانده‌شده"; SmsAutomationPlugin.Action.DELETE -> "حذف پیام" }

    private fun showRuleDialog(existing: SmsAutomationPlugin.Rule? = null) {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(8), 0, dp(8), 0) }
        val name = field("نام قانون"); val sender = field("شماره فرستنده"); val contains = field("متن شرط")
        name.setText(existing?.name.orEmpty()); sender.setText(existing?.sender.orEmpty()); contains.setText(existing?.containsText.orEmpty())
        var action = existing?.action ?: SmsAutomationPlugin.Action.MARK_READ
        box.addView(name); box.addView(sender); box.addView(contains)
        box.addView(Button(this).apply { text = "عملیات: ${actionLabel(action)}"; setOnClickListener { val values = SmsAutomationPlugin.Action.values(); MaterialAlertDialogBuilder(this@PluginStoreActivity).setTitle("انتخاب عملیات").setSingleChoiceItems(values.map { actionLabel(it) }.toTypedArray(), values.indexOf(action)) { d, which -> action = values[which]; text = "عملیات: ${actionLabel(action)}"; d.dismiss() }.show() } })
        MaterialAlertDialogBuilder(this).setTitle(if (existing == null) "قانون جدید" else "ویرایش قانون").setView(box).setNegativeButton("لغو", null).setPositiveButton("ذخیره") { _, _ ->
            val rule = SmsAutomationPlugin.Rule(existing?.id ?: System.currentTimeMillis(), name.text.toString().trim(), sender.text.toString().trim(), contains.text.toString().trim(), action, existing?.enabled ?: true)
            if (rule.name.isBlank() && rule.sender.isBlank() && rule.containsText.isBlank()) showError("حداقل یکی از نام، فرستنده یا شرط متن را وارد کنید") else { SmsAutomationPlugin.addRule(this, rule); SmsAutomationPlugin.setEnabled(this, true); render() }
        }.show()
    }

    private fun addTemplates(parent: LinearLayout) {
        val list = SmsTemplatesPlugin.list(this).sortedBy { it.name.lowercase(Locale.getDefault()) }
        parent.addView(TextView(this).apply { text = "قالب‌های ذخیره‌شده: ${list.size}"; textSize = 13f })
        parent.addView(Button(this).apply { text = "➕ ساخت قالب جدید"; setOnClickListener { showTemplateDialog() } })
        list.forEach { item ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(4), dp(10), dp(4), dp(6)) }
            row.addView(TextView(this).apply { text = item.name.ifBlank { "بدون نام" }; textSize = 15f })
            row.addView(TextView(this).apply { text = item.body; textSize = 13f; setPadding(0, dp(4), 0, dp(4)) })
            if (item.category.isNotBlank()) row.addView(TextView(this).apply { text = "دسته: ${item.category}"; textSize = 12f })
            val controls = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; layoutDirection = View.LAYOUT_DIRECTION_RTL }
            controls.addView(Button(this).apply { text = "ویرایش"; setOnClickListener { showTemplateDialog(item) } }, LinearLayout.LayoutParams(0, -2, 1f))
            controls.addView(Button(this).apply { text = "حذف"; setOnClickListener { MaterialAlertDialogBuilder(this@PluginStoreActivity).setTitle("حذف قالب").setMessage("این قالب حذف شود؟").setNegativeButton("لغو", null).setPositiveButton("حذف") { _, _ -> SmsTemplatesPlugin.delete(this@PluginStoreActivity, item.id); render() }.show() } })
            row.addView(controls); parent.addView(row)
        }
    }

    private fun showTemplateDialog(existing: SmsTemplatesPlugin.Template? = null) {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(8), 0, dp(8), 0) }
        val name = field("نام قالب"); val category = field("دسته‌بندی (اختیاری)"); val body = EditText(this).apply { hint = "متن پیام؛ مثلا سلام {name}"; minLines = 4; gravity = Gravity.TOP }
        name.setText(existing?.name.orEmpty()); category.setText(existing?.category.orEmpty()); body.setText(existing?.body.orEmpty()); box.addView(name); box.addView(category); box.addView(body)
        MaterialAlertDialogBuilder(this).setTitle(if (existing == null) "قالب جدید" else "ویرایش قالب").setView(box).setNegativeButton("لغو", null).setPositiveButton("ذخیره") { _, _ ->
            val item = SmsTemplatesPlugin.Template(existing?.id ?: System.currentTimeMillis(), name.text.toString().trim(), body.text.toString(), category.text.toString().trim())
            when { item.name.isBlank() -> showError("نام قالب را وارد کنید"); item.body.isBlank() -> showError("متن قالب را وارد کنید"); else -> { SmsTemplatesPlugin.save(this, item); render() } }
        }.show()
    }

    private fun addBackup(parent: LinearLayout) {
        parent.addView(Button(this).apply { text = "📦 ساخت Backup"; setOnClickListener { createBackup() } })
        parent.addView(Button(this).apply { text = "♻️ Restore از فایل"; setOnClickListener { restoreBackup() } })
    }

    private fun addScheduledSms(parent: LinearLayout) {
        parent.addView(Button(this).apply { text = "➕ زمان‌بندی پیام جدید"; setOnClickListener { showScheduledSmsDialog() } })
        parent.addView(TextView(this).apply { text = "Placeholderهای زمان‌بندی: {destination}، {date}، {time}"; textSize = 12f; setPadding(0, dp(6), 0, dp(6)) })
        val format = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
        ScheduledSmsPlugin.list(this).sortedBy { it.triggerAt }.forEach { item ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(4), dp(10), dp(4), dp(6)) }
            row.addView(TextView(this).apply { text = "${format.format(Date(item.triggerAt))} — ${item.destination}"; textSize = 14f; layoutDirection = View.LAYOUT_DIRECTION_LTR; textDirection = View.TEXT_DIRECTION_LTR })
            row.addView(TextView(this).apply { text = item.body; textSize = 13f; setPadding(0, dp(4), 0, dp(4)) })
            val status = when {
                item.completed -> "ارسال‌شده"
                !item.enabled && item.lastError.isNotBlank() -> "متوقف‌شده پس از خطا"
                item.lastError.isNotBlank() -> "در انتظار تلاش مجدد (${item.retryCount}/${ScheduledSmsPlugin.MAX_RETRIES})"
                item.enabled -> "در انتظار ارسال"
                else -> "غیرفعال"
            }
            row.addView(TextView(this).apply { text = "وضعیت: $status"; textSize = 12f })
            if (item.lastError.isNotBlank()) row.addView(TextView(this).apply { text = "خطا: ${item.lastError}"; textSize = 12f })
            val controls = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL }
            if (!item.completed) controls.addView(MaterialSwitch(this).apply { text = "فعال"; isChecked = item.enabled; setOnCheckedChangeListener { _, checked -> runCatching { ScheduledSmsPlugin.setEnabled(this@PluginStoreActivity, item.id, checked) }.onSuccess { render() }.onFailure { showError(it.message ?: "خطا") } } }, LinearLayout.LayoutParams(0, -2, 1f))
            if (!item.completed && item.lastError.isNotBlank()) controls.addView(Button(this).apply { text = "تلاش مجدد"; setOnClickListener { ScheduledSmsPlugin.retryNow(this@PluginStoreActivity, item.id); render() } })
            if (!item.completed && item.enabled) controls.addView(Button(this).apply { text = "ویرایش"; setOnClickListener { showScheduledSmsDialog(item) } })
            controls.addView(Button(this).apply { text = "حذف"; setOnClickListener { ScheduledSmsPlugin.cancel(this@PluginStoreActivity, item.id); render() } })
            row.addView(controls); parent.addView(row)
        }
    }

    private fun showScheduledSmsDialog(existing: ScheduledSmsPlugin.Item? = null) {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(8), 0, dp(8), 0) }
        val destination = field("شماره گیرنده")
        val body = EditText(this).apply { hint = "متن پیام"; minLines = 4; gravity = Gravity.TOP }
        destination.setText(existing?.destination.orEmpty()); body.setText(existing?.body.orEmpty())
        box.addView(destination)
        box.addView(Button(this).apply { text = "📋 انتخاب قالب"; setOnClickListener { showScheduledTemplatePicker(body) } })
        box.addView(body)
        val selected = Calendar.getInstance().apply { if (existing == null) add(Calendar.MINUTE, 5) else timeInMillis = existing.triggerAt }
        val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()); val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateButton = Button(this).apply { text = "تاریخ: ${dateFormat.format(selected.time)}" }
        val timeButton = Button(this).apply { text = "ساعت: ${timeFormat.format(selected.time)}" }
        dateButton.setOnClickListener { DatePickerDialog(this, { _, y, m, d -> selected.set(y, m, d); dateButton.text = "تاریخ: ${dateFormat.format(selected.time)}" }, selected.get(Calendar.YEAR), selected.get(Calendar.MONTH), selected.get(Calendar.DAY_OF_MONTH)).show() }
        timeButton.setOnClickListener { TimePickerDialog(this, { _, h, m -> selected.set(Calendar.HOUR_OF_DAY, h); selected.set(Calendar.MINUTE, m); selected.set(Calendar.SECOND, 0); selected.set(Calendar.MILLISECOND, 0); timeButton.text = "ساعت: ${timeFormat.format(selected.time)}" }, selected.get(Calendar.HOUR_OF_DAY), selected.get(Calendar.MINUTE), true).show() }
        box.addView(dateButton); box.addView(timeButton)
        MaterialAlertDialogBuilder(this).setTitle(if (existing == null) "زمان‌بندی پیام" else "ویرایش پیام زمان‌بندی‌شده").setView(box).setNegativeButton("لغو", null).setPositiveButton(if (existing == null) "زمان‌بندی" else "ذخیره") { _, _ ->
            val phone = destination.text.toString().trim(); val message = body.text.toString().trim()
            when { phone.isBlank() -> showError("شماره گیرنده را وارد کنید"); message.isBlank() -> showError("متن پیام را وارد کنید"); selected.timeInMillis <= System.currentTimeMillis() -> showError("زمان انتخاب‌شده باید در آینده باشد"); else -> runCatching {
                val item = ScheduledSmsPlugin.Item(existing?.id ?: System.currentTimeMillis(), phone, message, selected.timeInMillis, true, false)
                if (existing == null) ScheduledSmsPlugin.schedule(this, item) else ScheduledSmsPlugin.update(this, item)
            }.onSuccess { render(); Toast.makeText(this, "ذخیره شد", Toast.LENGTH_SHORT).show() }.onFailure { showError(it.message ?: "خطا در زمان‌بندی") } }
        }.show()
    }

    private fun showScheduledTemplatePicker(body: EditText) {
        val templates = SmsTemplatesPlugin.list(this).sortedBy { it.name.lowercase(Locale.getDefault()) }
        if (templates.isEmpty()) { showError("قالبی وجود ندارد؛ ابتدا یک قالب بسازید"); return }
        MaterialAlertDialogBuilder(this).setTitle("انتخاب قالب").setItems(templates.map { if (it.category.isBlank()) it.name else "${it.name} — ${it.category}" }.toTypedArray()) { _, which -> body.setText(templates[which].body); body.setSelection(body.text.length) }.show()
    }

    private fun showError(message: String) { Toast.makeText(this, message, Toast.LENGTH_LONG).show() }
    private fun createBackup() { startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/json").putExtra(Intent.EXTRA_TITLE, "messages-backup.json"), 10) }
    private fun restoreBackup() { startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).setType("application/json").addCategory(Intent.CATEGORY_OPENABLE), 11) }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) { super.onActivityResult(requestCode, resultCode, data); if (resultCode != Activity.RESULT_OK || data?.data == null) return; runCatching { if (requestCode == 10) SmsBackupProPlugin.backup(this, data.data!!) else if (requestCode == 11) SmsBackupProPlugin.restore(this, data.data!!) }.onFailure { showError("خطا: ${it.message}") } }
    private fun field(hint: String) = EditText(this).apply { this.hint = hint; textSize = 15f; setSingleLine(true); layoutDirection = View.LAYOUT_DIRECTION_LTR; textDirection = View.TEXT_DIRECTION_LTR; setPadding(dp(8), dp(8), dp(8), dp(8)) }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun color(attr: Int): Int { val typed = obtainStyledAttributes(intArrayOf(attr)); val value = typed.getColor(0, Color.WHITE); typed.recycle(); return value }
}
