package org.fossify.messages.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.messages.extensions.rescheduleAllScheduledMessages
import org.fossify.messages.helpers.ThemeScheduleManager

/** Reschedules message and automatic theme alarms after boot/package/time changes. */
class RescheduleAlarmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        ensureBackgroundThread {
            context.rescheduleAllScheduledMessages()
            ThemeScheduleManager.applyCurrent(context)
            ThemeScheduleManager.scheduleNext(context)
            pendingResult.finish()
        }
    }
}
