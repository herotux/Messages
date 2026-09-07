package org.fossify.messages.plugins

/** Central catalog for built-in premium plugins. */
object PluginRegistry {
    const val SMS_AUTOMATION = "sms_automation"
    const val SMS_TEMPLATES = "sms_templates"
    const val SMS_BACKUP_PRO = "sms_backup_pro"
    const val SCHEDULED_SMS_PRO = "scheduled_sms_pro"
    const val AI_ASSISTANT = "ai_assistant"

    data class Plugin(
        val id: String,
        val titleFa: String,
        val titleEn: String,
        val descriptionFa: String,
        val descriptionEn: String,
        val icon: String,
        val categoryFa: String,
        val featuresFa: List<String>
    )

    val all = listOf(
        Plugin(
            SMS_AUTOMATION,
            "اتوماسیون SMS",
            "SMS Automation",
            "قوانین هوشمند برای اجرای خودکار عملیات روی پیام‌های دریافتی",
            "Automate actions for incoming SMS",
            "⚙️",
            "اتوماسیون پیام",
            listOf("قوانین قابل فعال/غیرفعال شدن", "فیلتر بر اساس فرستنده و متن", "علامت‌گذاری خوانده‌شده یا حذف پیام")
        ),
        Plugin(
            SMS_TEMPLATES,
            "قالب‌های SMS",
            "SMS Templates",
            "پیام‌های آماده را ذخیره کنید و هنگام ارسال سریع دوباره استفاده کنید",
            "Create and reuse message templates",
            "📋",
            "بهره‌وری",
            listOf("ساخت و ویرایش قالب", "دسته‌بندی قالب‌ها", "پشتیبانی از Placeholderهایی مثل {name}")
        ),
        Plugin(
            SMS_BACKUP_PRO,
            "پشتیبان‌گیری حرفه‌ای",
            "SMS Backup Pro",
            "از پیامک‌ها نسخه پشتیبان بگیرید و در صورت نیاز آن‌ها را بازیابی کنید",
            "Back up and restore SMS messages",
            "💾",
            "پشتیبان‌گیری",
            listOf("خروجی JSON", "بازیابی پیام‌ها", "جلوگیری از درج پیام‌های تکراری")
        ),
        Plugin(
            SCHEDULED_SMS_PRO,
            "ارسال زمان‌بندی‌شده حرفه‌ای",
            "Scheduled SMS Pro",
            "پیام‌ها را برای تاریخ و ساعت دلخواه برنامه‌ریزی و مدیریت کنید",
            "Schedule SMS messages",
            "⏰",
            "زمان‌بندی",
            listOf("زمان‌بندی دقیق", "مدیریت پیام‌های فعال", "تلاش مجدد پس از خطا")
        ),
        Plugin(
            AI_ASSISTANT,
            "دستیار هوش مصنوعی",
            "AI Assistant",
            "تولید، بازنویسی، ترجمه و پیشنهاد پاسخ برای پیامک‌ها با سرویس هوش مصنوعی",
            "Generate, rewrite and suggest SMS replies with an AI service",
            "🤖",
            "هوش مصنوعی",
            listOf("پیشنهاد پاسخ", "ترجمه و بازنویسی", "ابزارهای جست‌وجو و تحلیل پیام‌ها", "بدون ارسال یا حذف خودکار پیام")
        )
    )
}
