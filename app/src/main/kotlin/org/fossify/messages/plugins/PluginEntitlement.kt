package org.fossify.messages.plugins

/** Purchase/trial state exposed to the UI and plugin runtime. */
enum class PluginEntitlement {
    INACTIVE,
    TRIAL,
    PURCHASED,
    EXPIRED
}

/** Stable product mapping. Real Bazaar SKU values can be supplied by the release build. */
object PluginProducts {
    const val AI_ASSISTANT = "plugin_ai_assistant"
    const val SMS_AUTOMATION = "plugin_sms_automation"
    const val SMS_TEMPLATES = "plugin_sms_templates"
    const val SMS_BACKUP_PRO = "plugin_sms_backup_pro"
    const val SCHEDULED_SMS_PRO = "plugin_scheduled_sms_pro"

    fun skuFor(pluginId: String): String = when (pluginId) {
        PluginRegistry.AI_ASSISTANT -> AI_ASSISTANT
        PluginRegistry.SMS_AUTOMATION -> SMS_AUTOMATION
        PluginRegistry.SMS_TEMPLATES -> SMS_TEMPLATES
        PluginRegistry.SMS_BACKUP_PRO -> SMS_BACKUP_PRO
        PluginRegistry.SCHEDULED_SMS_PRO -> SCHEDULED_SMS_PRO
        else -> error("Unknown plugin: $pluginId")
    }
}
