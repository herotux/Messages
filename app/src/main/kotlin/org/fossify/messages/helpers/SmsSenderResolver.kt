package org.fossify.messages.helpers

import android.content.Context
import android.provider.Telephony

/**
 * Resolves the actual SMS sender stored by Android for a thread.
 * This is important for alphanumeric sender IDs such as SEPAH BANK,
 * TejaratBank and Bank Melli, which are not reliable conversation phone numbers.
 */
object SmsSenderResolver {
    fun getLatestSender(context: Context, threadId: Long): String? {
        if (threadId <= 0L) return null

        return try {
            context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                arrayOf(Telephony.Sms.ADDRESS),
                "${Telephony.Sms.THREAD_ID} = ?",
                arrayOf(threadId.toString()),
                "${Telephony.Sms.DATE} DESC LIMIT 1"
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0)?.trim()?.takeIf { it.isNotEmpty() } else null
            }
        } catch (_: Exception) {
            null
        }
    }
}
