package org.fossify.messages.helpers

import android.content.Context
import android.provider.Telephony

/**
 * Resolves the strongest sender identity for a conversation from real incoming SMS rows.
 * Verified alphanumeric Sender IDs are preferred over an arbitrary latest address.
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
                var fallback: String? = null
                while (cursor.moveToNext()) {
                    val sender = cursor.getString(0)?.trim().orEmpty()
                    if (sender.isEmpty()) continue
                    if (fallback == null) fallback = sender
                    if (BankSenderAliases.find(sender) != null ||
                        IranianBankSenderProfiles.find(sender) != null ||
                        IranianBankRegistry.findBySmsSender(sender) != null
                    ) {
                        return@use sender
                    }
                }
                fallback
            }
        } catch (_: Exception) {
            null
        }
    }
}
