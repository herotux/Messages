package org.fossify.messages.helpers

import android.content.Context
import android.provider.Telephony
import org.fossify.commons.extensions.formatDateOrTime
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.messages.R
import org.fossify.messages.adapters.SearchResultsAdapter
import org.fossify.messages.models.SearchResult

/**
 * Supplements the normal Room-backed search with the Android SMS provider.
 * This is intentionally presentation-only: it never writes SMS into Room and
 * therefore cannot change conversation state or bank classification.
 */
object ProviderSearchBridge {
    private const val MAX_RESULTS = 300

    fun search(context: Context, text: String, onResult: (List<SearchResult>) -> Unit) {
        val query = text.trim()
        if (query.length < 2) {
            onResult(emptyList())
            return
        }

        ensureBackgroundThread {
            val results = ArrayList<SearchResult>()
            val projection = arrayOf(
                Telephony.Sms._ID,
                Telephony.Sms.THREAD_ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE
            )
            val like = "%$query%"
            val selection = "(${Telephony.Sms.BODY} LIKE ? OR ${Telephony.Sms.ADDRESS} LIKE ?)"
            val args = arrayOf(like, like)

            context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                projection,
                selection,
                args,
                "${Telephony.Sms.DATE} DESC"
            )?.use { cursor ->
                val id = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
                val threadId = cursor.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)
                val address = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val body = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val date = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)

                while (cursor.moveToNext() && results.size < MAX_RESULTS) {
                    val sender = cursor.getString(address).orEmpty()
                    val messageBody = cursor.getString(body).orEmpty()
                    val messageDate = cursor.getLong(date)
                    results += SearchResult(
                        messageId = cursor.getLong(id),
                        title = sender,
                        snippet = messageBody,
                        date = (messageDate / 1000L).formatDateOrTime(
                            context = context,
                            hideTimeOnOtherDays = true,
                            showCurrentYear = true
                        ),
                        threadId = cursor.getLong(threadId),
                        photoUri = ""
                    )
                }
            }

            context.runOnUiThreadSafe { onResult(results) }
        }
    }

    private fun Context.runOnUiThreadSafe(action: () -> Unit) {
        if (this is android.app.Activity) {
            runOnUiThread(action)
        } else {
            android.os.Handler(android.os.Looper.getMainLooper()).post(action)
        }
    }

    fun mergeIntoAdapter(adapter: SearchResultsAdapter, providerResults: List<SearchResult>, query: String) {
        if (providerResults.isEmpty()) return
        val merged = ArrayList<SearchResult>(adapter.searchResults.size + providerResults.size)
        val seenMessageIds = HashSet<Long>()

        adapter.searchResults.forEach { result ->
            if (result.messageId >= 0) seenMessageIds.add(result.messageId)
            merged.add(result)
        }
        providerResults.forEach { result ->
            if (result.messageId < 0 || seenMessageIds.add(result.messageId)) {
                merged.add(result)
            }
        }

        merged.sortByDescending { it.date }
        adapter.updateItems(merged, query)
    }
}
