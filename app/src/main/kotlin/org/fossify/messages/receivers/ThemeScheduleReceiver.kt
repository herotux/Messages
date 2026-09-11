package org.fossify.messages.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.fossify.messages.helpers.ThemeScheduleManager

/** Applies the active scheduled theme and schedules the next day/night transition. */
class ThemeScheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        ThemeScheduleManager.applyCurrent(context)
        ThemeScheduleManager.scheduleNext(context)
    }
}
