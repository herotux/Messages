package org.fossify.messages.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.fossify.messages.plugins.ScheduledSmsPlugin

/** Restores Scheduled SMS alarms because AlarmManager entries do not survive a reboot. */
class PluginScheduledSmsBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> ScheduledSmsPlugin.rescheduleAll(context)
        }
    }
}
