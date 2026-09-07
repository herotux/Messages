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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        render()
    }

    private fun render() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(36), dp(20), dp(28))
            setBackgroundColor(color(com.google.android.material.R.attr.colorSurface))
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        root.addView(TextView(this).apply {
            text = "🔌 پلاگین‌های Messages"
            textSize = 24f
            setTextColor(color(com.google.android.material.R.attr.colorOnSurface))
            gravity = Gravity.CENTER
        })
        root.addView(TextView(this).apply {
            text = "قابلیت‌های حرفه‌ای را جداگانه فعال و مدیریت کنید"
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(color(com.google.android.material.R.attr.colorOnSurfaceVariant))
            setPadding(0, dp(6), 0, dp(20))
        })
        val scroll = ScrollView(this)
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        PluginRegistry.all.forEach { addPlugin(it) }
    }

    private fun addPlugin(plugin: PluginRegistry.Plugin) {
        val licensed = PluginLicenseStore.isLicensed(this, plugin.id)
        val card = MaterialCardView(this).apply {
            radius = dp(22).toFloat()
            cardElevation = 0f
            strokeWidth = dp(1)
            strokeColor = color(com.google.android.material.R.attr.colorOutlineVariant)
            setCardBackgroundColor(color(com.google.android.material.R.attr.colorSurfaceVariant))
        }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
        }
        box.addView(TextView(this).apply {
            text = plugin.titleFa
            textSize = 19f
            setTextColor(color(com.google.android.material.R.attr.colorOnSurface))
        })
        box.addView(TextView(this).apply {
            text = plugin.descriptionFa
            textSize = 14f
            setTextColor(color(com.google.android.material.R.attr.colorOnSurfaceVariant))
            setPadding(0, dp(6), 0, dp(10))
        })
        box.addView(TextView(this).apply {
            text = if (licensed) "فعال" else "Premium • آزمایشی ۷ روزه"
            textSize = 13f
            setTextColor(color(com.google.android.material.R.attr.colorPrimary))
            setPadding(0, 0, 0, dp(10))
        })
        if (!licensed) {
            box.addView(Button(this).apply {
                text = "فعال‌سازی آزمایشی ۷ روزه"
                setOnClickListener {
                    PluginLicenseStore.activateTrial(this@PluginStoreActivity, plugin.id, 7)
                    render()
                }
            })
        } else {
            val expiry = PluginLicenseStore.expiry(this, plugin.id)
            box.addView(TextView(this).apply {
                text = "اعتبار تا ${DateFormat.getDateInstance().format(Date(expiry))}"
                textSize = 13f
                setTextColor(color(com.google.android.material.R.attr.colorPrimary))
                setPadding(0, 0, 0, dp(8))
            })
            when (plugin.id) {
                PluginRegistry.SMS_AUTOMATION -> addAutomation(box)
                PluginRegistry.SMS_TEMPLATES -> addTemplates(box)
                PluginRegistry.SMS_BACKUP_PRO -> addBackup(box)
                PluginRegistry.SCHEDULED_SMS_PRO -> addScheduledSms(box)
            }
        }
        card.addView(box)
        content.addView(card, margins(0, 8))
    }

    private fun addAutomation(parent: LinearLayout) {
        val rules = SmsAutomationPlugin.getRules(this)
        parent.addView(MaterialSwitch(this).apply {
            text = "فعال بودن SMS Automation"
            isChecked = SmsAutomationPlugin.isEnabled(this@PluginStoreActivity)
            setOnCheckedChangeListener { _, checked ->
                SmsAutomationPlugin.setEnabled(this@PluginStoreActivity, checked)
            }
        })
        parent.addView(TextView(this).apply {
            text = "قوانین فعال: ${rules.count { it.enabled }} از ${rules.size}"
            textSize = 13f
            setTextColor(color(com.google.android.material.R.attr.colorOnSurfaceVariant))
            setPadding(0, dp(6), 0, dp(8))
        })
        parent.addView(Button(this).apply {
            text = "➕ افزودن قانون"
            setOnClickListener { showRuleDialog() }
        })

        if (rules.isEmpty()) {
            parent.addView(TextView(this).apply {
                text = "هنوز قانونی ساخته نشده است."
                textSize = 13f
                setTextColor(color(com.google.android.material.R.attr.colorOnSurfaceVariant))
                setPadding(0, dp(8), 0, 0)
            })
            return
        }

        rules.forEach { rule ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(4), dp(12), dp(4), dp(4))
            }
            row.addView(TextView(this).apply {
                text = rule.name.ifBlank { "بدون نام" }
                textSize = 15f
                setTextColor(color(com.google.android.material.R.attr.colorOnSurface))
            })
            row.addView(TextView(this).apply {
                text = "فرستنده: ${rule.sender.ifBlank { "همه" }}\nشرط متن: ${rule.containsText.ifBlank { "هر متن" }}\nعملیات: ${actionLabel(rule.action)}"
                textSize = 13f
                setTextColor(color(com.google.android.material.R.attr.colorOnSurfaceVariant))
                setPadding(0, dp(4), 0, dp(4))
                layoutDirection = View.LAYOUT_DIRECTION_RTL
            })
            val controls = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutDirection = View.LAYOUT_DIRECTION_RTL
            }
            controls.addView(MaterialSwitch(this).apply {
                text = "فعال"
                isChecked = rule.enabled
                setOnCheckedChangeListener { _, checked ->
                    SmsAutomationPlugin.setRuleEnabled(this@PluginStoreActivity, rule.id, checked)
                    render()
                }
            }, LinearLayout.LayoutParams(0, -2, 1f))
            controls.addView(Button(this).apply {
                text = "ویرایش"
                setOnClickListener { showRuleDialog(rule) }
            })
            controls.addView(Button(this).apply {
                text = "حذف"
                setOnClickListener { confirmDeleteRule(rule) }
            })
            row.addView(controls)
            parent.addView(row)
        }
    }

    private fun actionLabel(action: SmsAutomationPlugin.Action): String = when (action) {
        SmsAutomationPlugin.Action.MARK_READ -> "علامت‌گذاری به‌عنوان خوانده‌شده"
        SmsAutomationPlugin.Action.DELETE -> "حذف پیام"
    }

    private fun confirmDeleteRule(rule: SmsAutomationPlugin.Rule) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("حذف قانون")
            .setMessage("قانون «${rule.name.ifBlank { "بدون نام" }}» حذف شود؟")
            .setNegativeButton("لغو", null)
            .setPositiveButton("حذف") { _, _ ->
                SmsAutomationPlugin.removeRule(this, rule.id)
                render()
            }
            .show()
    }

    private fun showRuleDialog(existing: SmsAutomationPlugin.Rule? = null) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), 0, dp(8), 0)
        }
        val name = field("نام قانون")
        val sender = field("شماره فرستنده")
        val contains = field("متن شرط")
        name.setText(existing?.name.orEmpty())
        sender.setText(existing?.sender.orEmpty())
        contains.setText(existing?.containsText.orEmpty())

        var selectedAction = existing?.action ?: SmsAutomationPlugin.Action.MARK_READ
        val actionButton = Button(this).apply {
            text = "عملیات: ${actionLabel(selectedAction)}"
            setOnClickListener {
                val actions = SmsAutomationPlugin.Action.values()
                val labels = actions.map { actionLabel(it) }.toTypedArray()
                val checked = actions.indexOf(selectedAction)
                com.google.android.material.dialog.MaterialAlertDialogBuilder(this@PluginStoreActivity)
                    .setTitle("انتخاب عملیات")
                    .setSingleChoiceItems(labels, checked) { dialog, which ->
                        selectedAction = actions[which]
                        text = "عملیات: ${actionLabel(selectedAction)}"
                        dialog.dismiss()
                    }
                    .show()
            }
        }
        box.addView(name)
        box.addView(sender)
        box.addView(contains)
        box.addView(actionButton)

        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle(if (existing == null) "قانون SMS Automation" else "ویرایش قانون")
            .setView(box)
            .setNegativeButton("لغو", null)
            .setPositiveButton("ذخیره") { _, _ ->
                val rule = SmsAutomationPlugin.Rule(
                    id = existing?.id ?: System.currentTimeMillis(),
                    name = name.text.toString().trim(),
                    sender = sender.text.toString().trim(),
                    containsText = contains.text.toString().trim(),
                    action = selectedAction,
                    enabled = existing?.enabled ?: true
                )
                if (rule.name.isBlank() && rule.sender.isBlank() && rule.containsText.isBlank()) {
                    showError("حداقل یکی از نام، فرستنده یا شرط متن را وارد کنید")
                } else {
                    SmsAutomationPlugin.addRule(this, rule)
                    SmsAutomationPlugin.setEnabled(this, true)
                    render()
                }
            }
            .show()
    }

    private fun addTemplates(parent: LinearLayout) {
        val list = SmsTemplatesPlugin.list(this)
        parent.addView(TextView(this).apply {
            text = "قالب‌های ذخیره‌شده: ${list.size}"
            textSize = 13f
            setTextColor(color(com.google.android.material.R.attr.colorOnSurfaceVariant))
            setPadding(0, 0, 0, dp(8))
        })
        parent.addView(Button(this).apply {
            text = "➕ ساخت قالب جدید"
            setOnClickListener { showTemplateDialog() }
        })
        list.forEach { item ->
            parent.addView(TextView(this).apply {
                text = "• ${item.name}: ${item.body}"
                textSize = 13f
                setPadding(dp(4), dp(4), dp(4), dp(4))
            })
        }
    }

    private fun addBackup(parent: LinearLayout) {
        parent.addView(Button(this).apply {
            text = "📦 ساخت Backup"
            setOnClickListener { createBackup() }
        })
        parent.addView(Button(this).apply {
            text = "♻️ Restore از فایل"
            setOnClickListener { restoreBackup() }
        })
    }

    private fun addScheduledSms(parent: LinearLayout) {
        parent.addView(Button(this).apply {
            text = "➕ زمان‌بندی پیام جدید"
            setOnClickListener { showScheduledSmsDialog() }
        })
        val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
        val items = ScheduledSmsPlugin.list(this).sortedBy { it.triggerAt }
        if (items.isEmpty()) {
            parent.addView(TextView(this).apply {
                text = "هنوز پیام زمان‌بندی‌شده‌ای ثبت نشده است."
                textSize = 13f
                setTextColor(color(com.google.android.material.R.attr.colorOnSurfaceVariant))
                setPadding(0, dp(8), 0, 0)
            })
            return
        }
        items.forEach { item ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(4), dp(10), dp(4), dp(6))
            }
            row.addView(TextView(this).apply {
                text = "${dateFormat.format(Date(item.triggerAt))} — ${item.destination}"
                textSize = 14f
                setTextColor(color(com.google.android.material.R.attr.colorOnSurface))
                layoutDirection = View.LAYOUT_DIRECTION_LTR
                textDirection = View.TEXT_DIRECTION_LTR
            })
            row.addView(TextView(this).apply {
                text = item.body
                textSize = 13f
                setTextColor(color(com.google.android.material.R.attr.colorOnSurfaceVariant))
                setPadding(0, dp(4), 0, dp(4))
            })
            row.addView(TextView(this).apply {
                text = if (item.enabled) "وضعیت: در انتظار ارسال" else "وضعیت: ارسال‌شده"
                textSize = 12f
                setTextColor(color(com.google.android.material.R.attr.colorPrimary))
            })
            if (item.enabled) {
                row.addView(Button(this).apply {
                    text = "لغو زمان‌بندی"
                    setOnClickListener {
                        ScheduledSmsPlugin.cancel(this@PluginStoreActivity, item.id)
                        render()
                    }
                })
            }
            parent.addView(row)
        }
    }

    private fun showScheduledSmsDialog() {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), 0, dp(8), 0)
        }
        val destination = field("شماره گیرنده")
        val body = EditText(this).apply {
            hint = "متن پیام"
            minLines = 4
            gravity = Gravity.TOP
            textSize = 15f
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        val dateButton = Button(this).apply { text = "انتخاب تاریخ" }
        val timeButton = Button(this).apply { text = "انتخاب ساعت" }
        val selected = Calendar.getInstance().apply { add(Calendar.MINUTE, 5) }
        val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        dateButton.text = "تاریخ: ${dateFormat.format(selected.time)}"
        timeButton.text = "ساعت: ${timeFormat.format(selected.time)}"
        dateButton.setOnClickListener {
            DatePickerDialog(this, { _, year, month, day ->
                selected.set(Calendar.YEAR, year)
                selected.set(Calendar.MONTH, month)
                selected.set(Calendar.DAY_OF_MONTH, day)
                dateButton.text = "تاریخ: ${dateFormat.format(selected.time)}"
            }, selected.get(Calendar.YEAR), selected.get(Calendar.MONTH), selected.get(Calendar.DAY_OF_MONTH)).show()
        }
        timeButton.setOnClickListener {
            TimePickerDialog(this, { _, hour, minute ->
                selected.set(Calendar.HOUR_OF_DAY, hour)
                selected.set(Calendar.MINUTE, minute)
                selected.set(Calendar.SECOND, 0)
                selected.set(Calendar.MILLISECOND, 0)
                timeButton.text = "ساعت: ${timeFormat.format(selected.time)}"
            }, selected.get(Calendar.HOUR_OF_DAY), selected.get(Calendar.MINUTE), true).show()
        }
        box.addView(destination)
        box.addView(body)
        box.addView(dateButton)
        box.addView(timeButton)
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("زمان‌بندی پیام")
            .setView(box)
            .setNegativeButton("لغو", null)
            .setPositiveButton("زمان‌بندی") { _, _ ->
                val phone = destination.text.toString().trim()
                val message = body.text.toString().trim()
                when {
                    phone.isBlank() -> showError("شماره گیرنده را وارد کنید")
                    message.isBlank() -> showError("متن پیام را وارد کنید")
                    selected.timeInMillis <= System.currentTimeMillis() -> showError("زمان انتخاب‌شده باید در آینده باشد")
                    else -> runCatching {
                        ScheduledSmsPlugin.schedule(this, ScheduledSmsPlugin.Item(System.currentTimeMillis(), phone, message, selected.timeInMillis, true))
                    }.onSuccess {
                        render()
                        Toast.makeText(this, "پیام زمان‌بندی شد", Toast.LENGTH_SHORT).show()
                    }.onFailure { showError(it.message ?: "خطا در زمان‌بندی پیام") }
                }
            }
            .show()
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun showTemplateDialog() {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(8), 0, dp(8), 0) }
        val name = field("نام قالب")
        val body = EditText(this).apply {
            hint = "متن پیام؛ مثلا سلام {name}"
            minLines = 3
            gravity = Gravity.TOP
            textSize = 15f
        }
        box.addView(name)
        box.addView(body)
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("قالب جدید")
            .setView(box)
            .setNegativeButton("لغو", null)
            .setPositiveButton("ذخیره") { _, _ ->
                SmsTemplatesPlugin.save(this, SmsTemplatesPlugin.Template(System.currentTimeMillis(), name.text.toString().trim(), body.text.toString()))
                render()
            }
            .show()
    }

    private fun createBackup() {
        startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/json").putExtra(Intent.EXTRA_TITLE, "messages-backup.json"), 10)
    }

    private fun restoreBackup() {
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).setType("application/json").addCategory(Intent.CATEGORY_OPENABLE), 11)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK || data?.data == null) return
        runCatching {
            if (requestCode == 10) SmsBackupProPlugin.backup(this, data.data!!)
            else if (requestCode == 11) SmsBackupProPlugin.restore(this, data.data!!)
        }.onFailure { Toast.makeText(this, "خطا: ${it.message}", Toast.LENGTH_LONG).show() }
    }

    private fun field(hint: String) = EditText(this).apply {
        this.hint = hint
        textSize = 15f
        setSingleLine(true)
        layoutDirection = View.LAYOUT_DIRECTION_LTR
        textDirection = View.TEXT_DIRECTION_LTR
        setPadding(dp(8), dp(8), dp(8), dp(8))
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun color(attr: Int): Int {
        val typed = obtainStyledAttributes(intArrayOf(attr))
        val value = typed.getColor(0, Color.WHITE)
        typed.recycle()
        return value
    }
}
