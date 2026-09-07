package org.fossify.messages.plugins

import android.content.Context

object PluginLicenseStore {
    private const val PREFS = "messages_plugin_licenses"
    private const val SMS_AUTOMATION = "sms_automation"
    private const val EXPIRES_AT = "sms_automation_expires_at"

    fun isSmsAutomationLicensed(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getBoolean(SMS_AUTOMATION, false) &&
            prefs.getLong(EXPIRES_AT, 0L) > System.currentTimeMillis()
    }

    /** Local trial hook. Production billing should replace this with a signed server entitlement. */
    fun activateSmsAutomationTrial(context: Context, days: Int = 7) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(SMS_AUTOMATION, true)
            .putLong(EXPIRES_AT, System.currentTimeMillis() + days * 24L * 60L * 60L * 1000L)
            .apply()
    }

    fun revokeSmsAutomation(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(SMS_AUTOMATION, false)
            .remove(EXPIRES_AT)
            .apply()
    }

    fun smsAutomationExpiry(context: Context): Long =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(EXPIRES_AT, 0L)
}
