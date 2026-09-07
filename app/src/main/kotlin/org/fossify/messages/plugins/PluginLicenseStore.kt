package org.fossify.messages.plugins

import android.content.Context

object PluginLicenseStore {
    private const val PREFS = "messages_plugin_licenses"
    private const val ENABLED = "enabled_"
    private const val EXPIRES = "expires_"

    fun isLicensed(context: Context, pluginId: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getBoolean(ENABLED + pluginId, false) && prefs.getLong(EXPIRES + pluginId, 0L) > System.currentTimeMillis()
    }

    fun activateTrial(context: Context, pluginId: String, days: Int = 7) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(ENABLED + pluginId, true)
            .putLong(EXPIRES + pluginId, System.currentTimeMillis() + days.coerceAtLeast(1) * 24L * 60L * 60L * 1000L)
            .apply()
    }

    fun revoke(context: Context, pluginId: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(ENABLED + pluginId, false).remove(EXPIRES + pluginId).apply()
    }

    fun expiry(context: Context, pluginId: String): Long = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(EXPIRES + pluginId, 0L)

    fun isSmsAutomationLicensed(context: Context) = isLicensed(context, PluginRegistry.SMS_AUTOMATION)
    fun activateSmsAutomationTrial(context: Context, days: Int = 7) = activateTrial(context, PluginRegistry.SMS_AUTOMATION, days)
    fun revokeSmsAutomation(context: Context) = revoke(context, PluginRegistry.SMS_AUTOMATION)
    fun smsAutomationExpiry(context: Context) = expiry(context, PluginRegistry.SMS_AUTOMATION)
}
