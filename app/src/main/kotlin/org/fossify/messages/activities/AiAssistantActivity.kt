package org.fossify.messages.activities

import android.os.Bundle
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
            text = "🤖 دستیار هوش مصنوعی"
            textSize = 24f
            gravity = Gravity.CENTER
        })
        root.addView(TextView(this).apply {
            text = "با هر سرویس OpenAI-compatible مثل OpenAI یا سرویس‌های سازگار کار می‌کند."
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, dp(16))
        })

        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val config = AiAssistantPlugin.getConfig(this)
        val endpoint = field("API Endpoint", config.endpoint)
        val key = field("API Key", config.apiKey).apply { inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD }
        val model = field("Model", config.model)
        val system = multiline("System prompt", config.systemPrompt)
        val prompt = multiline("درخواست شما؛ مثلا: برای این پیام یک پاسخ مؤدبانه بنویس", "")
        val result = multiline("نتیجه", "")
        result.isFocusable = false

        content.addView(endpoint); content.addView(key); content.addView(model); content.addView(system)
        content.addView(Button(this).apply {
            text = "💾 ذخیره تنظیمات"
            setOnClickListener {
                AiAssistantPlugin.saveConfig(this@AiAssistantActivity, AiAssistantPlugin.Config(endpoint.text.toString(), key.text.toString(), model.text.toString(), system.text.toString()))
                Toast.makeText(this@AiAssistantActivity, "تنظیمات ذخیره شد", Toast.LENGTH_SHORT).show()
            }
        })
        content.addView(prompt)
        content.addView(Button(this).apply {
            text = "✨ تولید پاسخ"
            setOnClickListener {
                val text = prompt.text.toString().trim()
                if (text.isBlank()) { prompt.error = "درخواست را وارد کنید"; return@setOnClickListener }
                isEnabled = false
                text = "⏳ در حال دریافت پاسخ..."
                AiAssistantPlugin.generate(this@AiAssistantActivity, text) { response ->
                    isEnabled = true
                    response.onSuccess { result.setText(it) }.onFailure { result.setText("خطا: ${it.message ?: "خطای ناشناخته"}") }
                }
            }
        })
        content.addView(result)
        content.addView(Button(this).apply {
            text = "📋 کپی نتیجه"
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

    private fun field(hint: String, value: String) = EditText(this).apply {
        this.hint = hint; setText(value); setSingleLine(true); textSize = 15f
        layoutDirection = android.view.View.LAYOUT_DIRECTION_LTR
        textDirection = android.view.View.TEXT_DIRECTION_LTR
        setPadding(dp(8), dp(8), dp(8), dp(8))
    }

    private fun multiline(hint: String, value: String) = EditText(this).apply {
        this.hint = hint; setText(value); minLines = 3; gravity = Gravity.TOP; textSize = 15f
        layoutDirection = android.view.View.LAYOUT_DIRECTION_RTL
        setPadding(dp(8), dp(8), dp(8), dp(8))
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
