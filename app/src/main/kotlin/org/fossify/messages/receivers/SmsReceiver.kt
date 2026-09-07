package org.fossify.messages.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import org.fossify.commons.extensions.baseConfig
import org.fossify.commons.extensions.getMyContactsCursor
import org.fossify.commons.extensions.isNumberBlocked
import org.fossify.commons.helpers.ContactLookupResult
import org.fossify.commons.helpers.SimpleContactsHelper
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.commons.models.PhoneNumber
import org.fossify.commons.models.SimpleContact
import org.fossify.messages.extensions.getConversations
import org.fossify.messages.extensions.getNameFromAddress
import org.fossify.messages.extensions.getNotificationBitmap
import org.fossify.messages.extensions.getThreadId
import org.fossify.messages.extensions.insertNewSMS
import org.fossify.messages.extensions.insertOrUpdateConversation
import org.fossify.messages.extensions.messagesDB
import org.fossify.messages.extensions.shouldUnarchive
import org.fossify.messages.extensions.showReceivedMessageNotification
import org.fossify.messages.extensions.updateConversationArchivedStatus
import org.fossify.messages.helpers.DebugLog
import org.fossify.messages.helpers.ReceiverUtils.isMessageFilteredOut
import org.fossify.messages.helpers.refreshConversations
import org.fossify.messages.helpers.refreshMessages
import org.fossify.messages.models.Message
import org.fossify.messages.plugins.SmsAutomationPlugin

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val appContext = context.applicationContext
        DebugLog.write(appContext, "RECEIVER_STARTED action=${intent.action}")
        ensureBackgroundThread {
            try {
                val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                if (parts.isEmpty()) return@ensureBackgroundThread
                val address = parts.last().originatingAddress.orEmpty()
                if (address.isBlank()) return@ensureBackgroundThread
                val subject = parts.last().pseudoSubject.orEmpty()
                val status = parts.last().status
                val body = buildString { parts.forEach { append(it.messageBody.orEmpty()) } }
                if (isMessageFilteredOut(appContext, body) || appContext.isNumberBlocked(address)) return@ensureBackgroundThread
                if (appContext.baseConfig.blockUnknownNumbers) {
                    val cursor = appContext.getMyContactsCursor(favoritesOnly = false, withPhoneNumbersOnly = true)
                    if (SimpleContactsHelper(appContext).existsSync(address, cursor) == ContactLookupResult.NotFound) return@ensureBackgroundThread
                }
                val date = System.currentTimeMillis()
                val threadId = appContext.getThreadId(address)
                val subscriptionId = intent.getIntExtra("subscription", -1)
                val newMessageId = handleMessageSync(
                    appContext,
                    address,
                    subject,
                    body,
                    date,
                    threadId,
                    subscriptionId,
                    status
                )
                SmsAutomationPlugin.processIncomingSms(appContext, newMessageId, address, body)
            } catch (e: Exception) {
                DebugLog.write(appContext, "RECEIVER_EXCEPTION ${e.javaClass.name}: ${e.message}")
            } finally {
                pending.finish()
                DebugLog.write(appContext, "RECEIVER_FINISHED")
            }
        }
    }

    private fun handleMessageSync(
        context: Context,
        address: String,
        subject: String,
        body: String,
        date: Long,
        threadId: Long,
        subscriptionId: Int,
        status: Int
    ): Long {
        val photoUri = SimpleContactsHelper(context).getPhotoUriFromPhoneNumber(address)
        val bitmap = context.getNotificationBitmap(photoUri)
        val newMessageId = context.insertNewSMS(
            address = address,
            subject = subject,
            body = body,
            date = date,
            read = 0,
            threadId = threadId,
            type = Telephony.Sms.MESSAGE_TYPE_INBOX,
            subscriptionId = subscriptionId
        )
        context.getConversations(threadId).firstOrNull()?.let {
            runCatching { context.insertOrUpdateConversation(it) }
        }
        val senderName = context.getMyContactsCursor(
            favoritesOnly = false,
            withPhoneNumbersOnly = true
        ).use { context.getNameFromAddress(address, it) }
        val participant = SimpleContact(
            rawId = 0,
            contactId = 0,
            name = senderName,
            photoUri = photoUri,
            phoneNumbers = arrayListOf(
                PhoneNumber(
                    value = address,
                    type = 0,
                    label = "",
                    normalizedNumber = address
                )
            ),
            birthdays = ArrayList(),
            anniversaries = ArrayList()
        )
        val message = Message(
            id = newMessageId,
            body = body,
            type = Telephony.Sms.MESSAGE_TYPE_INBOX,
            status = status,
            participants = arrayListOf(participant),
            date = (date / 1000).toInt(),
            read = false,
            threadId = threadId,
            isMMS = false,
            attachment = null,
            senderPhoneNumber = address,
            senderName = senderName,
            senderPhotoUri = photoUri,
            subscriptionId = subscriptionId
        )
        context.messagesDB.insertOrUpdate(message)
        if (context.shouldUnarchive()) context.updateConversationArchivedStatus(threadId, false)
        refreshMessages()
        refreshConversations()
        context.showReceivedMessageNotification(
            messageId = newMessageId,
            isMms = false,
            address = address,
            senderName = senderName,
            body = body,
            threadId = threadId,
            bitmap = bitmap
        )
        return newMessageId
    }
}
