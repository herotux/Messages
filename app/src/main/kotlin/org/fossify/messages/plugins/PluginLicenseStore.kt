package org.fossify.messages.plugins

import android.content.Context

/** Local entitlement cache. Production purchases are written here only after store verification. */
object PluginLicenseStore {
    private const val PREFS = "messages_plugin_licenses"
    private const val ENABLED = "enabled_"
    private const val EXPIRES = "expires_"
    private const val PURCHASED = "purchased_"

    fun entitlement(context: Context, pluginId: String): PluginEntitlement {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(PURCHASED + pluginId, false)) return PluginEntitlement.PURCHASED
        if (prefs.getBoolean(ENABLED + pluginId, false)) {
            return if (prefs.getLong(EXPIRES + pluginId, 0L) > System.currentTimeMillis()) {
                PluginEntitlement.TRIAL
            } else {
                PluginEntitlement.EXPIRED
            }
        }
        return PluginEntitlement.INACTIVE
    }

    fun isLicensed(context: Context, pluginId: String): Boolean = when (entitlement(context, pluginId)) {
        PluginEntitlement.TRIAL, PluginEntitlement.PURCHASED -> true
        PluginEntitlement.INACTIVE, PluginEntitlement.EXPIRED -> false
    }

    fun activateTrial(context: Context, pluginId: String, days: Int = 7) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(ENABLED + pluginId, true)
            .putLong(EXPIRES + pluginId, System.currentTimeMillis() + days.coerceAtLeast(1) * 24L * 60L * 60L * 1000L)
            .apply()
    }

    /** Called only after a payment provider has confirmed ownership. */
    fun markPurchased(context: Context, pluginId: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(PURCHASED + pluginId, true)
            .putBoolean(ENABLED + pluginId, true)
            .remove(EXPIRES + pluginId)
            .apply()
    }

    fun revoke(context: Context, pluginId: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(ENABLED + pluginId, false)
            .putBoolean(PURCHASED + pluginId, false)
            .remove(EXPIRES + pluginId)
            .apply()
    }

    /** Debug-only helper for UI/testing. It does not exist as a production payment path. */
    fun resetForDemo(context: Context, pluginId: String) = revoke(context, pluginId)

    fun expiry(context: Context, pluginId: String): Long =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(EXPIRES + pluginId, 0L)

    fun isSmsAutomationLicensed(context: Context) = isLicensed(context, PluginRegistry.SMS_AUTOMATION)
    fun activateSmsAutomationTrial(context: Context, days: Int = 7) = activateTrial(context, PluginRegistry.SMS_AUTOMATION, days)
    fun revokeSmsAutomation(context: Context) = revoke(context, PluginRegistry.SMS_AUTOMATION)
    fun smsAutomationExpiry(context: Context) = expiry(context, PluginRegistry.SMS_AUTOMATION)
}
