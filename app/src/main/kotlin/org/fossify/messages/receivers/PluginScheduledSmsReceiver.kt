package org.fossify.messages.receivers

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import org.fossify.messages.messaging.sendMessageCompat
import org.fossify.messages.plugins.ScheduledSmsPlugin

class PluginScheduledSmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(ScheduledSmsPlugin.EXTRA_ID_KEY, -1L)
        if (id < 0L || !ScheduledSmsPlugin.isAvailable(context)) return

        val item = ScheduledSmsPlugin.list(context)
            .firstOrNull { it.id == id && it.enabled } ?: return

        if (context.checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, "مجوز ارسال پیامک در دسترس نیست؛ پیام زمان‌بندی‌شده ارسال نشد", Toast.LENGTH_LONG).show()
            return
        }

        runCatching {
            context.sendMessageCompat(
                item.body,
                arrayListOf(item.destination),
                -1,
                emptyList()
            )
        }.onSuccess {
            ScheduledSmsPlugin.markCompleted(context, id)
        }.onFailure {
            Toast.makeText(
                context,
                "ارسال پیام زمان‌بندی‌شده ناموفق بود: ${it.message ?: "خطای ناشناخته"}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
