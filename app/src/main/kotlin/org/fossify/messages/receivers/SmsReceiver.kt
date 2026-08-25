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

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val appContext = context.applicationContext
        DebugLog.write(appContext, "RECEIVER_STARTED action=${intent.action}")

        ensureBackgroundThread {
            try {
                val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                DebugLog.write(appContext, "SMS_PARTS count=${parts.size}")
                if (parts.isEmpty()) return@ensureBackgroundThread

                val address = parts.last().originatingAddress.orEmpty()
                DebugLog.write(appContext, "ADDRESS=$address")
                if (address.isBlank()) return@ensureBackgroundThread
                val subject = parts.last().pseudoSubject.orEmpty()
                val status = parts.last().status
                val body = buildString { parts.forEach { append(it.messageBody.orEmpty()) } }
                DebugLog.write(appContext, "BODY_LENGTH=${body.length}")

                if (isMessageFilteredOut(appContext, body)) {
                    DebugLog.write(appContext, "FILTERED_OUT")
                    return@ensureBackgroundThread
                }
                if (appContext.isNumberBlocked(address)) {
                    DebugLog.write(appContext, "NUMBER_BLOCKED")
                    return@ensureBackgroundThread
                }
                if (appContext.baseConfig.blockUnknownNumbers) {
                    DebugLog.write(appContext, "CHECKING_UNKNOWN_NUMBER")
                    val privateCursor =
                        appContext.getMyContactsCursor(favoritesOnly = false, withPhoneNumbersOnly = true)
                    val result = SimpleContactsHelper(appContext).existsSync(address, privateCursor)
                    DebugLog.write(appContext, "CONTACT_LOOKUP=$result")
                    if (result == ContactLookupResult.NotFound) {
                        DebugLog.write(appContext, "UNKNOWN_NUMBER_REJECTED")
                        return@ensureBackgroundThread
                    }
                }

                val date = System.currentTimeMillis()
                val threadId = appContext.getThreadId(address)
                val subscriptionId = intent.getIntExtra("subscription", -1)
                DebugLog.write(appContext, "BEFORE_HANDLE threadId=$threadId")

                handleMessageSync(
                    context = appContext,
                    address = address,
                    subject = subject,
                    body = body,
                    date = date,
                    threadId = threadId,
                    subscriptionId = subscriptionId,
                    status = status
                )
                DebugLog.write(appContext, "AFTER_HANDLE")
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
        read: Int = 0,
        threadId: Long,
        type: Int = Telephony.Sms.MESSAGE_TYPE_INBOX,
        subscriptionId: Int,
        status: Int
    ) {
        DebugLog.write(context, "HANDLE_START")
        val photoUri = SimpleContactsHelper(context).getPhotoUriFromPhoneNumber(address)
        val bitmap = context.getNotificationBitmap(photoUri)

        val newMessageId = context.insertNewSMS(
            address = address,
            subject = subject,
            body = body,
            date = date,
            read = read,
            threadId = threadId,
            type = type,
            subscriptionId = subscriptionId
        )
        DebugLog.write(context, "INSERT_NEW_SMS id=$newMessageId")

        context.getConversations(threadId).firstOrNull()?.let { conv ->
            runCatching { context.insertOrUpdateConversation(conv) }
        }

        val senderName = context.getMyContactsCursor(favoritesOnly = false, withPhoneNumbersOnly = true).use {
            context.getNameFromAddress(address, it)
        }

        val participant = SimpleContact(
            rawId = 0,
            contactId = 0,
            name = senderName,
            photoUri = photoUri,
            phoneNumbers = arrayListOf(PhoneNumber(value = address, type = 0, label = "", normalizedNumber = address)),
            birthdays = ArrayList(),
            anniversaries = ArrayList()
        )

        val message = Message(
            id = newMessageId,
            body = body,
            type = type,
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

        DebugLog.write(context, "BEFORE_ROOM_INSERT id=$newMessageId threadId=$threadId")
        context.messagesDB.insertOrUpdate(message)
        DebugLog.write(context, "AFTER_ROOM_INSERT id=$newMessageId")

        if (context.shouldUnarchive()) {
            context.updateConversationArchivedStatus(threadId, false)
        }

        refreshMessages()
        refreshConversations()
        DebugLog.write(context, "REFRESH_EVENTS_SENT")
        context.showReceivedMessageNotification(
            messageId = newMessageId,
            isMms = false,
            address = address,
            senderName = senderName,
            body = body,
            threadId = threadId,
            bitmap = bitmap
        )
        DebugLog.write(context, "HANDLE_FINISHED")
    }
}
