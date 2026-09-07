package org.fossify.messages.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.fossify.messages.messaging.sendMessageCompat
import org.fossify.messages.plugins.ScheduledSmsPlugin

class PluginScheduledSmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(ScheduledSmsPlugin.EXTRA_ID_KEY, -1L)
        if (id < 0L || !ScheduledSmsPlugin.isAvailable(context)) return

        val item = ScheduledSmsPlugin.list(context)
            .firstOrNull { it.id == id && it.enabled } ?: return

        runCatching {
            context.sendMessageCompat(
                item.body,
                arrayListOf(item.destination),
                -1,
                emptyList()
            )
        }.onSuccess {
            ScheduledSmsPlugin.markCompleted(context, id)
        }
    }
}
