package org.fossify.messages.helpers

import android.content.Context
import android.provider.Telephony
import java.util.concurrent.ConcurrentHashMap

/**
 * Resolves bank sender IDs off the RecyclerView/UI thread and caches the result per thread.
 * The conversation adapter must never query Telephony.Sms directly from onBindViewHolder.
 */
object BankLogoThreadCache {
    private val cache = ConcurrentHashMap<Long, BankInfo?>()

    fun get(threadId: Long): BankInfo? = cache[threadId]

    fun warm(context: Context, threadIds: Collection<Long>) {
        val ids = threadIds.filter { it > 0L && !cache.containsKey(it) }
        if (ids.isEmpty()) return

        Thread {
            val resolver = context.contentResolver
            ids.forEach { threadId ->
                val bank = try {
                    resolver.query(
                        Telephony.Sms.CONTENT_URI,
                        arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.DATE),
                        "${Telephony.Sms.THREAD_ID}=?",
                        arrayOf(threadId.toString()),
                        "${Telephony.Sms.DATE} DESC"
                    )?.use { cursor ->
                        var result: BankInfo? = null
                        while (cursor.moveToNext() && result == null) {
                            val address = cursor.getString(0).orEmpty()
                            result = BankSmsDetector.detect(address, "")
                        }
                        result
                    }
                } catch (_: Exception) {
                    null
                }
                cache[threadId] = bank
            }
        }.start()
    }

    fun clear() = cache.clear()
}
