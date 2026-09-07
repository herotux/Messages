package org.fossify.messages.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import org.fossify.commons.extensions.showErrorToast
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.messages.extensions.conversationsDB
import org.fossify.messages.extensions.deleteScheduledMessage
import org.fossify.messages.extensions.getAddresses
import org.fossify.messages.extensions.messagesDB
import org.fossify.messages.helpers.SCHEDULED_MESSAGE_ID
import org.fossify.messages.helpers.THREAD_ID
import org.fossify.messages.helpers.refreshConversations
import org.fossify.messages.helpers.refreshMessages
import org.fossify.messages.messaging.sendMessageCompat
import org.fossify.messages.plugins.ScheduledSmsPlugin
import kotlin.time.Duration.Companion.minutes

class ScheduledMessageReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakelock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "simple.messenger:scheduled.message.receiver")
        wakelock.acquire(1.minutes.inWholeMilliseconds)
        val pendingResult = goAsync()
        ensureBackgroundThread {
            try { if (intent.action == "org.fossify.messages.PLUGIN_SCHEDULED_SMS") handlePluginIntent(context, intent) else handleIntent(context, intent) }
            finally { try { if (wakelock.isHeld) wakelock.release() } catch (_: Exception) {}; pendingResult.finish() }
        }
    }

    private fun handlePluginIntent(context: Context, intent: Intent) {
        val id = intent.getLongExtra(ScheduledSmsPlugin.EXTRA_ID_KEY, -1L)
        if (id < 0L || !ScheduledSmsPlugin.isAvailable(context)) return
        val item = ScheduledSmsPlugin.list(context).firstOrNull { it.id == id && it.enabled } ?: return
        Handler(Looper.getMainLooper()).post { context.sendMessageCompat(item.body, arrayListOf(item.destination), -1, emptyList()) }
    }

    private fun handleIntent(context: Context, intent: Intent) {
        val threadId = intent.getLongExtra(THREAD_ID, 0L)
        val messageId = intent.getLongExtra(SCHEDULED_MESSAGE_ID, 0L)
        val message = try { context.messagesDB.getScheduledMessageWithId(threadId, messageId) } catch (e: Exception) { e.printStackTrace(); return }
        val addresses = message.participants.getAddresses()
        val attachments = message.attachment?.attachments ?: emptyList()
        try {
            Handler(Looper.getMainLooper()).post { context.sendMessageCompat(message.body, addresses, message.subscriptionId, attachments) }
            context.deleteScheduledMessage(messageId)
            context.conversationsDB.deleteThreadId(messageId)
            refreshMessages(); refreshConversations()
        } catch (e: Exception) { context.showErrorToast(e) }
        catch (e: Error) { context.showErrorToast(e.localizedMessage ?: context.getString(org.fossify.messages.R.string.unknown_error_occurred)) }
    }
}
