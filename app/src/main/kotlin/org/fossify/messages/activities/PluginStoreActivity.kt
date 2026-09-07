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
        val scroll = ScrollView(this)
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(content); root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f)); setContentView(root)
        PluginRegistry.all.forEach { plugin -> addPlugin(plugin) }
    }

    private fun addPlugin(plugin: PluginRegistry.Plugin) {
        val licensed = PluginLicenseStore.isLicensed(this, plugin.id)
        val card = MaterialCardView(this).apply { radius = dp(22).toFloat(); cardElevation = 0f; strokeWidth = dp(1); strokeColor = color(com.google.android.material.R.attr.colorOutlineVariant); setCardBackgroundColor(color(com.google.android.material.R.attr.colorSurfaceVariant)) }
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(18), dp(18), dp(18)) }
        box.addView(TextView(this).apply { text = plugin.titleFa; textSize = 19f; setTextColor(color(com.google.android.material.R.attr.colorOnSurface)) })
        box.addView(TextView(this).apply { text = plugin.descriptionFa; textSize = 14f; setTextColor(color(com.google.android.material.R.attr.colorOnSurfaceVariant)); setPadding(0, dp(6), 0, dp(10)) })
        box.addView(TextView(this).apply { text = if (licensed) "فعال" else "Premium • آزمایشی ۷ روزه"; textSize = 13f; setTextColor(color(com.google.android.material.R.attr.colorPrimary)); setPadding(0, 0, 0, dp(10)) })
        if (!licensed) {
            box.addView(Button(this).apply { text = "فعال‌سازی آزمایشی ۷ روزه"; setOnClickListener { PluginLicenseStore.activateTrial(this@PluginStoreActivity, plugin.id, 7); render() } })
        } else {
            box.addView(TextView(this).apply { val expiry = PluginLicenseStore.expiry(this@PluginStoreActivity, plugin.id); text = "اعتبار تا ${DateFormat.getDateInstance().format(Date(expiry))}"; textSize = 13f; setTextColor(color(com.google.android.material.R.attr.colorPrimary)); setPadding(0, 0, 0, dp(8)) })
            when (plugin.id) {
                PluginRegistry.SMS_AUTOMATION -> addAutomationButton(box)
                PluginRegistry.SMS_TEMPLATES -> addTemplatesButton(box)
                PluginRegistry.SMS_BACKUP_PRO -> addInfo(box, "برای Backup/Restore از صفحه مدیریت پشتیبان استفاده کنید.")
                PluginRegistry.SCHEDULED_SMS_PRO -> addInfo(box, "قابلیت زمان‌بندی پیامک در سیستم Alarm برنامه فعال است.")
            }
        }
        card.addView(box); content.addView(card, margins(0, 8))
    }

    private fun addAutomationButton(parent: LinearLayout) {
        val sw = MaterialSwitch(this).apply { text = "فعال بودن SMS Automation"; isChecked = SmsAutomationPlugin.isEnabled(this@PluginStoreActivity); setOnCheckedChangeListener { _, checked -> SmsAutomationPlugin.setEnabled(this@PluginStoreActivity, checked) } }
        parent.addView(sw)
        parent.addView(TextView(this).apply { text = "قوانین فعال: ${SmsAutomationPlugin.getRules(this@PluginStoreActivity).count { it.enabled }}"; textSize = 13f; setTextColor(color(com.google.android.material.R.attr.colorOnSurfaceVariant)); setPadding(0, dp(6), 0, dp(8)) })
        parent.addView(Button(this).apply { text = "➕ افزودن قانون"; setOnClickListener { showAddRuleDialog() } })
        SmsAutomationPlugin.getRules(this).forEach { rule ->
            parent.addView(TextView(this).apply { text = "• ${rule.name.ifBlank { "بدون نام" }} — ${rule.sender.ifBlank { "همه فرستنده‌ها" }} / ${rule.containsText.ifBlank { "هر متن" }}"; textSize = 13f; setTextColor(color(com.google.android.material.R.attr.colorOnSurfaceVariant)); setPadding(dp(4), dp(4), dp(4), dp(4)) })
        }
    }

    private fun addTemplatesButton(parent: LinearLayout) {
        parent.addView(TextView(this).apply { text = "قالب‌های ذخیره‌شده: ${SmsTemplatesPlugin.list(this@PluginStoreActivity).size}"; textSize = 13f; setTextColor(color(com.google.android.material.R.attr.colorOnSurfaceVariant)); setPadding(0, 0, 0, dp(8)) })
        parent.addView(Button(this).apply { text = "➕ ساخت قالب جدید"; setOnClickListener { showTemplateDialog() } })
        SmsTemplatesPlugin.list(this).forEach { item -> parent.addView(TextView(this).apply { text = "• ${item.name}: ${item.body}"; textSize = 13f; setPadding(dp(4), dp(4), dp(4), dp(4)) }) }
    }

    private fun addInfo(parent: LinearLayout, text: String) { parent.addView(TextView(this).apply { this.text = text; textSize = 13f; setTextColor(color(com.google.android.material.R.attr.colorOnSurfaceVariant)) }) }

    private fun showAddRuleDialog() {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(8), 0, dp(8), 0) }
        val name = field("نام قانون")
        val sender = field("شماره فرستنده")
        val contains = field("متن شرط")
        box.addView(name); box.addView(sender); box.addView(contains)
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this).setTitle("قانون SMS Automation").setView(box).setNegativeButton("لغو", null).setPositiveButton("ذخیره") { _, _ ->
            SmsAutomationPlugin.addRule(this, SmsAutomationPlugin.Rule(System.currentTimeMillis(), name.text.toString().trim(), sender.text.toString().trim(), contains.text.toString().trim(), true, false))
            SmsAutomationPlugin.setEnabled(this, true); render()
        }.show()
    }

    private fun showTemplateDialog() {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(8), 0, dp(8), 0) }
        val name = field("نام قالب")
        val body = EditText(this).apply { hint = "متن پیام؛ مثلا سلام {name}"; minLines = 3; gravity = Gravity.TOP; textSize = 15f }
        box.addView(name); box.addView(body)
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this).setTitle("قالب جدید").setView(box).setNegativeButton("لغو", null).setPositiveButton("ذخیره") { _, _ ->
            SmsTemplatesPlugin.add(this, SmsTemplatesPlugin.Template(System.currentTimeMillis(), name.text.toString().trim(), body.text.toString()))
            render()
        }.show()
    }

    private fun field(hint: String) = EditText(this).apply { this.hint = hint; textSize = 15f; setSingleLine(true); layoutDirection = View.LAYOUT_DIRECTION_LTR; textDirection = View.TEXT_DIRECTION_LTR; setPadding(dp(8), dp(8), dp(8), dp(8)) }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun color(attr: Int): Int { val typed = obtainStyledAttributes(intArrayOf(attr)); val value = typed.getColor(0, Color.WHITE); typed.recycle(); return value }
}
