package org.fossify.messages.helpers

import android.content.Context
import java.util.concurrent.ConcurrentHashMap

/** Resolves bank sender IDs off the RecyclerView/UI thread and caches drawable names per thread. */
object BankLogoThreadCache {
    private val cache = ConcurrentHashMap<Long, String?>()

    fun get(threadId: Long): String? = cache[threadId]

    fun warm(context: Context, threadIds: Collection<Long>, onFinished: (() -> Unit)? = null) {
        val ids = threadIds.filter { it > 0L && !cache.containsKey(it) }
        if (ids.isEmpty()) {
            onFinished?.invoke()
            return
        }

        Thread {
            ids.forEach { threadId ->
                cache[threadId] = BankSenderLogoResolver.findLatestBankSender(context, threadId)
            }
            onFinished?.invoke()
        }.start()
    }

    fun clear() = cache.clear()
}
