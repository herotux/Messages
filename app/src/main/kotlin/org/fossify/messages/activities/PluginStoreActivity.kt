package org.fossify.messages.activities

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
import org.fossify.messages.plugins.SmsAutomationPlugin
import java.text.DateFormat
import java.util.Date

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
        val title = TextView(this).apply {
            text = "🔌 پلاگین‌های Messages"
            textSize = 24f
            setTextColor(color(com.google.android.material.R.attr.colorOnSurface))
            gravity = Gravity.CENTER
        }
        root.addView(title, LinearLayout.LayoutParams(-1, -2))

        val subtitle = TextView(this).apply {
            text = "قابلیت‌های اضافی را جداگانه فعال کنید"
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(color(com.google.android.material.R.attr.colorOnSurfaceVariant))
            setPadding(0, dp(6), 0, dp(20))
        }
        root.addView(subtitle, LinearLayout.LayoutParams(-1, -2))

        val scroll = ScrollView(this)
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        renderSmsAutomation()
    }

    private fun renderSmsAutomation() {
        content.removeAllViews()
        val licensed = PluginLicenseStore.isSmsAutomationLicensed(this)
        val enabled = SmsAutomationPlugin.isEnabled(this)

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
            text = "SMS Auto Forward"
            textSize = 19f
            setTextColor(color(com.google.android.material.R.attr.colorOnSurface))
        })
        box.addView(TextView(this).apply {
            text = "ارسال خودکار کل متن پیام‌های دریافتی بر اساس قانون‌هایی که خودتان تعریف می‌کنید."
            textSize = 14f
            setTextColor(color(com.google.android.material.R.attr.colorOnSurfaceVariant))
            setPadding(0, dp(6), 0, dp(14))
        })

        if (!licensed) {
            box.addView(TextView(this).apply {
                text = "Premium • آزمایشی ۷ روزه"
                textSize = 14f
                setTextColor(color(com.google.android.material.R.attr.colorPrimary))
                setPadding(0, 0, 0, dp(10))
            })
            box.addView(Button(this).apply {
                text = "فعال‌سازی آزمایشی ۷ روزه"
                setOnClickListener {
                    PluginLicenseStore.activateSmsAutomationTrial(this@PluginStoreActivity)
                    render()
                }
            })
        } else {
            val expiry = PluginLicenseStore.smsAutomationExpiry(this)
            box.addView(TextView(this).apply {
                text = "فعال • اعتبار تا ${DateFormat.getDateInstance().format(Date(expiry))}"
                textSize = 13f
                setTextColor(color(com.google.android.material.R.attr.colorPrimary))
                setPadding(0, 0, 0, dp(10))
            })
            val switch = MaterialSwitch(this).apply {
                text = "فعال بودن پلاگین"
                isChecked = enabled
                setOnCheckedChangeListener { _, checked -> SmsAutomationPlugin.setEnabled(this@PluginStoreActivity, checked) }
            }
            box.addView(switch)
            box.addView(TextView(this).apply {
                text = "قوانین فعال: ${SmsAutomationPlugin.getRules(this@PluginStoreActivity).count { it.enabled }}"
                textSize = 13f
                setTextColor(color(com.google.android.material.R.attr.colorOnSurfaceVariant))
                setPadding(0, dp(4), 0, dp(10))
            })
            box.addView(Button(this).apply {
                text = "➕ افزودن قانون جدید"
                setOnClickListener { showAddRuleDialog() }
            })
            addRules(box)
        }
        card.addView(box)
        content.addView(card, margins(0, 8))

        if (licensed) {
            content.addView(TextView(this).apply {
                text = "نکته: پیام به‌صورت کامل و بدون استخراج OTP فوروارد می‌شود."
                textSize = 13f
                setTextColor(color(com.google.android.material.R.attr.colorOnSurfaceVariant))
                setPadding(dp(4), dp(14), dp(4), 0)
            })
        }
    }

    private fun addRules(parent: LinearLayout) {
        SmsAutomationPlugin.getRules(this).forEach { rule ->
            val row = MaterialCardView(this).apply {
                radius = dp(16).toFloat()
                cardElevation = 0f
                strokeWidth = dp(1)
                strokeColor = color(com.google.android.material.R.attr.colorOutlineVariant)
                setCardBackgroundColor(color(com.google.android.material.R.attr.colorSurface))
            }
            val box = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), dp(12), dp(14), dp(12))
            }
            box.addView(TextView(this).apply {
                text = rule.name.ifBlank { "بدون نام" }
                textSize = 16f
                setTextColor(color(com.google.android.material.R.attr.colorOnSurface))
            })
            box.addView(TextView(this).apply {
                text = "از: ${rule.sender.ifBlank { "همه شماره‌ها" }}\nشرط: ${rule.containsText.ifBlank { "هر متن" }}\nبه: ${rule.destination}"
                textSize = 13f
                setTextColor(color(com.google.android.material.R.attr.colorOnSurfaceVariant))
                setPadding(0, dp(5), 0, dp(5))
            })
            val actions = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
            val sw = MaterialSwitch(this).apply {
                isChecked = rule.enabled
                text = "فعال"
                setOnCheckedChangeListener { _, checked -> SmsAutomationPlugin.setRuleEnabled(this@PluginStoreActivity, rule.id, checked) }
            }
            actions.addView(sw, LinearLayout.LayoutParams(0, -2, 1f))
            actions.addView(Button(this).apply {
                text = "حذف"
                setOnClickListener {
                    SmsAutomationPlugin.removeRule(this@PluginStoreActivity, rule.id)
                    render()
                }
            })
            box.addView(actions)
            row.addView(box)
            parent.addView(row, margins(0, 8))
        }
    }

    private fun showAddRuleDialog() {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), 0, dp(8), 0)
        }
        val name = field("نام قانون، مثلا کد وام ازدواج")
        val sender = field("شماره فرستنده؛ مثلا 3000xxxx")
        val contains = field("متن شرط؛ مثلا کد تایید")
        val destination = field("شماره مقصد برای فوروارد")
        box.addView(name)
        box.addView(sender)
        box.addView(contains)
        box.addView(destination)

        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("قانون فوروارد SMS")
            .setView(box)
            .setNegativeButton("لغو", null)
            .setPositiveButton("ذخیره") { _, _ ->
                SmsAutomationPlugin.addRule(
                    this,
                    SmsAutomationPlugin.Rule(
                        id = System.currentTimeMillis(),
                        name = name.text.toString().trim(),
                        sender = sender.text.toString().trim(),
                        containsText = contains.text.toString().trim(),
                        destination = destination.text.toString().trim(),
                        enabled = true
                    )
                )
                SmsAutomationPlugin.setEnabled(this, true)
                render()
            }.show()
    }

    private fun field(hint: String): EditText = EditText(this).apply {
        this.hint = hint
        textSize = 15f
        setSingleLine(true)
        layoutDirection = View.LAYOUT_DIRECTION_LTR
        textDirection = View.TEXT_DIRECTION_LTR
        setPadding(dp(8), dp(8), dp(8), dp(8))
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun color(attr: Int): Int {
        val typed = obtainStyledAttributes(intArrayOf(attr))
        val value = typed.getColor(0, Color.WHITE)
        typed.recycle()
        return value
    }
}
