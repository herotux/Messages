package org.fossify.messages.plugins

/** Central catalog for built-in premium plugins. */
object PluginRegistry {
    const val SMS_AUTOMATION = "sms_automation"
    const val SMS_TEMPLATES = "sms_templates"
    const val SMS_BACKUP_PRO = "sms_backup_pro"
    const val SCHEDULED_SMS_PRO = "scheduled_sms_pro"

    data class Plugin(
        val id: String,
        val titleFa: String,
        val titleEn: String,
        val descriptionFa: String,
        val descriptionEn: String
    )

    val all = listOf(
        Plugin(SMS_AUTOMATION, "اتوماسیون SMS", "SMS Automation", "اجرای خودکار عملیات روی پیام‌های دریافتی", "Automate actions for incoming SMS"),
        Plugin(SMS_TEMPLATES, "قالب‌های SMS", "SMS Templates", "ساخت و استفاده سریع از پیام‌های آماده", "Create and reuse message templates"),
        Plugin(SMS_BACKUP_PRO, "پشتیبان‌گیری حرفه‌ای", "SMS Backup Pro", "پشتیبان‌گیری و بازیابی پیامک‌ها", "Back up and restore SMS messages"),
        Plugin(SCHEDULED_SMS_PRO, "ارسال زمان‌بندی‌شده حرفه‌ای", "Scheduled SMS Pro", "زمان‌بندی ارسال پیامک", "Schedule SMS messages")
    )
}
