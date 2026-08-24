package org.fossify.messages.helpers

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatImageButton
import org.fossify.commons.extensions.formatDateOrTime
import org.fossify.messages.activities.ThreadActivity
import org.fossify.messages.models.AdvancedSearchHit

class AdvancedSearchActionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatImageButton(context, attrs) {
    init {
        setImageResource(android.R.drawable.ic_menu_search)
        contentDescription = "Advanced search"
        setOnClickListener { openFilters() }
    }

    private fun openFilters() {
        AdvancedSearchDialog.show(context, AdvancedSearchFilter()) { filter ->
            if (!filter.hasFilters()) return@show
            searchAsync(filter)
        }
    }

    private fun searchAsync(filter: AdvancedSearchFilter) {
        Thread {
            val results = try {
                AdvancedSmsSearch.search(context, filter)
            } catch (_: SecurityException) {
                emptyList()
            } catch (_: Exception) {
                emptyList()
            }
            post { showResults(results) }
        }.start()
    }

    private fun showResults(results: List<AdvancedSearchHit>) {
        if (results.isEmpty()) {
            AlertDialog.Builder(context)
                .setTitle("Advanced search")
                .setMessage("No messages found")
                .setPositiveButton(android.R.string.ok, null)
                .show()
            return
        }

        val rows = results.map { hit ->
            val sender = hit.sender.ifBlank { "Unknown sender" }
            val date = hit.dateMillis.formatDateOrTime(
                context,
                hideTimeOnOtherDays = false,
                showCurrentYear = true
            )
            "$sender\n${hit.body.trim()}\n$date"
        }

        val list = ListView(context).apply {
            adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, rows)
        }

        val dialog = AlertDialog.Builder(context)
            .setTitle("${results.size} results")
            .setView(list)
            .setPositiveButton(android.R.string.ok, null)
            .create()

        list.setOnItemClickListener { _, _, position, _ ->
            val hit = results[position]
            context.startActivity(Intent(context, ThreadActivity::class.java).apply {
                putExtra(THREAD_ID, hit.threadId)
                putExtra(THREAD_TITLE, hit.sender)
                putExtra(SEARCHED_MESSAGE_ID, hit.messageId)
            })
            dialog.dismiss()
        }
        dialog.show()
    }
}
