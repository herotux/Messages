package org.fossify.messages.helpers

import android.content.Context
import android.provider.Telephony

/**
 * Resolves a real incoming SMS sender for a thread.
 * Alphanumeric sender IDs must be read from ADDRESS; the conversation's
 * phoneNumber is not reliable for senders such as SEPAH BANK, TejaratBank,
 * and Bank Melli.
 */
object SmsSenderResolver {
    fun getLatestSender(context: Context, threadId: Long): String? {
        if (threadId <= 0L) return null

        return try {
            context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                arrayOf(Telephony.Sms.ADDRESS),
                "${Telephony.Sms.THREAD_ID} = ? AND ${Telephony.Sms.TYPE} = ?",
                arrayOf(threadId.toString(), Telephony.Sms.MESSAGE_TYPE_INBOX.toString()),
                "${Telephony.Sms.DATE} DESC"
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val sender = cursor.getString(0)?.trim().orEmpty()
                    if (sender.isNotEmpty()) return@use sender
                }
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}
