package org.fossify.messages.activities

import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.fossify.messages.plugins.AiAssistantPlugin
import org.fossify.messages.plugins.PluginLicenseStore
import org.fossify.messages.plugins.PluginRegistry

/** UI for the optional, user-configured, tool-based AI Assistant plugin. */
class AiAssistantActivity : SimpleActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!PluginLicenseStore.isLicensed(this, PluginRegistry.AI_ASSISTANT)) {
            Toast.makeText(this, "ابتدا AI Assistant را از فروشگاه پلاگین‌ها فعال کنید", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        render()
    }

    private fun render() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(28), dp(20), dp(24))
            layoutDirection = android.view.View.LAYOUT_DIRECTION_RTL
        }
        root.addView(TextView(this).apply {
            text = "🤖 دستیار هوشمند SMS"
            textSize = 24f
            gravity = Gravity.CENTER
        })
        root.addView(TextView(this).apply {
            text = "این پلاگین فقط وقتی فعال و دارای API Key باشد کار می‌کند. جست‌وجوی SMS به‌صورت Tool روی خود گوشی انجام می‌شود."
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, dp(16))
        })

        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val config = AiAssistantPlugin.getConfig(this)

        val endpoint = field("API Endpoint (HTTPS)", config.endpoint)
        val key = field("API Key", config.apiKey).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val model = field("Model", config.model)
        val system = multiline("System prompt", config.systemPrompt)
        val prompt = multiline("درخواست شما", "")
        val result = multiline("پاسخ AI", "")
        result.isFocusable = false

        content.addView(section("تنظیمات اتصال"))
        content.addView(endpoint)
        content.addView(key)
        content.addView(model)
        content.addView(system)
        content.addView(TextView(this).apply {
            text = "🔐 API Key با Android Keystore رمزنگاری می‌شود و در SharedPreferences به‌صورت متن خام ذخیره نمی‌شود. Endpoint فقط HTTPS پذیرفته می‌شود."
            textSize = 12f
            setPadding(0, dp(8), 0, dp(8))
        })
        content.addView(Button(this).apply {
            text = "💾 ذخیره تنظیمات"
            setOnClickListener {
                runCatching {
                    AiAssistantPlugin.saveConfig(
                        this@AiAssistantActivity,
                        AiAssistantPlugin.Config(endpoint.text.toString(), key.text.toString(), model.text.toString(), system.text.toString())
                    )
                }.onSuccess {
                    Toast.makeText(this@AiAssistantActivity, "تنظیمات ذخیره شد", Toast.LENGTH_SHORT).show()
                }.onFailure {
                    Toast.makeText(this@AiAssistantActivity, it.message ?: "خطا در ذخیره تنظیمات", Toast.LENGTH_LONG).show()
                }
            }
        })

        content.addView(section("نمونه درخواست‌ها"))
        val examples = listOf(
            "قرارهای ملاقات این هفته‌ام را از پیامک‌ها پیدا کن.",
            "آخرین پیام‌های مربوط به بانک را پیدا و خلاصه کن.",
            "گفتگوهای اخیر با این شماره را بررسی کن و بگو موضوعشان چیست: 09120000000",
            "برای آخرین پیام پیدا شده یک پاسخ کوتاه و محترمانه پیشنهاد بده."
        )
        examples.forEach { example ->
            content.addView(Button(this).apply {
                text = example
                setOnClickListener { prompt.setText(example); prompt.requestFocus() }
            })
        }

        content.addView(section("گفتگو"))
        content.addView(prompt)
        content.addView(Button(this).apply {
            text = "✨ پرسش از دستیار"
            setOnClickListener {
                val promptText = prompt.text.toString().trim()
                if (promptText.isBlank()) {
                    prompt.error = "درخواست را وارد کنید"
                    return@setOnClickListener
                }
                if (AiAssistantPlugin.getConfig(this@AiAssistantActivity).apiKey.isBlank()) {
                    Toast.makeText(this@AiAssistantActivity, "ابتدا API Key را ذخیره کنید", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                isEnabled = false
                text = "⏳ در حال بررسی پیام‌ها و دریافت پاسخ..."
                AiAssistantPlugin.generate(this@AiAssistantActivity, promptText) { response ->
                    isEnabled = true
                    text = "✨ پرسش از دستیار"
                    response.onSuccess { result.setText(it) }
                        .onFailure { result.setText("خطا: ${it.message ?: "خطای ناشناخته"}") }
                }
            }
        })
        content.addView(result)
        content.addView(TextView(this).apply {
            text = "🛡️ ایمنی: ابزارهای فعلی فقط خواندن و جست‌وجوی SMS و ساخت Draft هستند؛ ارسال، حذف یا زمان‌بندی خودکار توسط AI انجام نمی‌شود."
            textSize = 12f
            setPadding(0, dp(10), 0, dp(10))
        })
        content.addView(Button(this).apply {
            text = "📋 کپی پاسخ"
            setOnClickListener {
                val value = result.text.toString()
                if (value.isNotBlank()) {
                    val clipboard = getSystemService(android.content.ClipboardManager::class.java)
                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("AI response", value))
                    Toast.makeText(this@AiAssistantActivity, "کپی شد", Toast.LENGTH_SHORT).show()
                }
            }
        })

        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    private fun section(title: String) = TextView(this).apply {
        text = title
        textSize = 17f
        setPadding(0, dp(14), 0, dp(6))
    }

    private fun field(hint: String, value: String) = EditText(this).apply {
        this.hint = hint
        setText(value)
        setSingleLine(true)
        textSize = 15f
        layoutDirection = android.view.View.LAYOUT_DIRECTION_LTR
        textDirection = android.view.View.TEXT_DIRECTION_LTR
        setPadding(dp(8), dp(8), dp(8), dp(8))
    }

    private fun multiline(hint: String, value: String) = EditText(this).apply {
        this.hint = hint
        setText(value)
        minLines = 3
        gravity = Gravity.TOP
        textSize = 15f
        layoutDirection = android.view.View.LAYOUT_DIRECTION_RTL
        setPadding(dp(8), dp(8), dp(8), dp(8))
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
