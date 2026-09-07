package org.fossify.messages.receivers

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import org.fossify.messages.messaging.sendMessageCompat
import org.fossify.messages.plugins.ScheduledSmsPlugin

class PluginScheduledSmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(ScheduledSmsPlugin.EXTRA_ID_KEY, -1L)
        if (id < 0L || !ScheduledSmsPlugin.isAvailable(context)) return
        val item = ScheduledSmsPlugin.list(context).firstOrNull { it.id == id && it.enabled } ?: return
        val pendingResult = goAsync()
        Thread {
            try {
                if (context.checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                    fail(context, id, "مجوز ارسال پیامک در دسترس نیست")
                    return@Thread
                }
                val message = ScheduledSmsPlugin.resolvePlaceholders(item.body, item.destination, item.triggerAt)
                runCatching {
                    context.sendMessageCompat(message, arrayListOf(item.destination), -1, emptyList())
                }.onSuccess {
                    ScheduledSmsPlugin.markCompleted(context, id)
                    notify(context, "پیام زمان‌بندی‌شده ارسال شد", item.destination)
                }.onFailure { error ->
                    fail(context, id, error.message ?: "خطای ناشناخته")
                }
            } finally {
                pendingResult.finish()
            }
        }.start()
    }

    private fun fail(context: Context, id: Long, error: String) {
        ScheduledSmsPlugin.markFailed(context, id, error)
        val item = ScheduledSmsPlugin.list(context).firstOrNull { it.id == id }
        val message = if (item != null && item.retryCount <= ScheduledSmsPlugin.MAX_RETRIES) {
            "ارسال ناموفق بود؛ تلاش ${item.retryCount}/${ScheduledSmsPlugin.MAX_RETRIES} در ۱۵ دقیقه دیگر"
        } else "ارسال ناموفق بود؛ برای تلاش دوباره وارد مدیریت پیام‌های زمان‌بندی‌شده شوید"
        notify(context, message, error)
    }

    private fun notify(context: Context, title: String, text: String) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channelId = "scheduled_sms_plugin"
        manager.createNotificationChannel(NotificationChannel(channelId, "Scheduled SMS", NotificationManager.IMPORTANCE_DEFAULT))
        if (android.os.Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        manager.notify((System.currentTimeMillis() and 0x7fffffff).toInt(), NotificationCompat.Builder(context, channelId)
            .setSmallIcon(org.fossify.messages.R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .build())
    }
}
