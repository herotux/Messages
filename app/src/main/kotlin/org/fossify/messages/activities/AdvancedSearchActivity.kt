package org.fossify.messages.activities

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import org.fossify.commons.extensions.getProperBackgroundColor
import org.fossify.commons.extensions.getProperPrimaryColor
import org.fossify.commons.extensions.getProperTextColor
import org.fossify.commons.extensions.toast
import org.fossify.messages.helpers.AdvancedSearchFilter
import org.fossify.messages.helpers.AdvancedSearchHit
import org.fossify.messages.helpers.AdvancedSmsSearch
import org.fossify.messages.helpers.SEARCHED_MESSAGE_ID
import org.fossify.messages.helpers.THREAD_ID
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

class AdvancedSearchActivity : SimpleActivity() {
    private val searcher by lazy { AdvancedSmsSearch(this) }
    private lateinit var textInput: TextInputEditText
    private lateinit var senderInput: TextInputEditText
    private lateinit var directionSpinner: Spinner
    private lateinit var unreadCheck: CheckBox
    private lateinit var bankCheck: CheckBox
    private lateinit var fromButton: MaterialButton
    private lateinit var toButton: MaterialButton
    private lateinit var results: LinearLayout
    private var fromDate: Long? = null
    private var toDate: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bg = getProperBackgroundColor()
        val fg = getProperTextColor()
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(bg) }
        val toolbar = Toolbar(this).apply {
            title = "Advanced search"
            setTitleTextColor(fg)
            navigationIcon = getDrawable(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
            setNavigationOnClickListener { finish() }
        }
        root.addView(toolbar, LinearLayout.LayoutParams(-1, dp(56)))
        val scroll = android.widget.ScrollView(this)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(12), dp(16), dp(24)) }
        textInput = TextInputEditText(this)
        senderInput = TextInputEditText(this)
        content.addView(field("Text in message / subject", textInput))
        content.addView(field("Sender / phone number", senderInput))
        directionSpinner = Spinner(this)
        directionSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, arrayOf("Any direction", "Incoming", "Outgoing"))
        content.addView(label("Direction"))
        content.addView(directionSpinner, LinearLayout.LayoutParams(-1, dp(52)))
        val dates = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        fromButton = MaterialButton(this).apply { text = "From date" }
        toButton = MaterialButton(this).apply { text = "To date" }
        dates.addView(fromButton, LinearLayout.LayoutParams(0, dp(52), 1f))
        dates.addView(toButton, LinearLayout.LayoutParams(0, dp(52), 1f))
        content.addView(dates)
        unreadCheck = CheckBox(this).apply { text = "Unread only"; setTextColor(fg) }
        bankCheck = CheckBox(this).apply { text = "Bank messages only"; setTextColor(fg) }
        content.addView(unreadCheck)
        content.addView(bankCheck)
        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val search = MaterialButton(this).apply { text = "Search"; setOnClickListener { runSearch() } }
        val clear = MaterialButton(this).apply { text = "Clear"; setOnClickListener { clearFilters() } }
        actions.addView(search, LinearLayout.LayoutParams(0, dp(52), 1f))
        actions.addView(clear, LinearLayout.LayoutParams(0, dp(52), 1f))
        content.addView(actions)
        results = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dp(12), 0, 0) }
        content.addView(results)
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        fromButton.setOnClickListener { pickDate(true) }
        toButton.setOnClickListener { pickDate(false) }
    }

    private fun field(hint: String, input: TextInputEditText) = TextInputLayout(this).apply {
        this.hint = hint
        addView(input)
        layoutParams = LinearLayout.LayoutParams(-1, dp(64))
    }

    private fun label(text: String) = TextView(this).apply {
        this.text = text
        setTextColor(getProperTextColor())
        textSize = 13f
        setPadding(0, dp(8), 0, dp(2))
    }

    private fun pickDate(isFrom: Boolean) {
        val c = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d ->
            c.set(y, m, d, if (isFrom) 0 else 23, if (isFrom) 0 else 59, if (isFrom) 0 else 59)
            c.set(Calendar.MILLISECOND, if (isFrom) 0 else 999)
            val value = c.timeInMillis
            if (isFrom) { fromDate = value; fromButton.text = DateFormat.getDateInstance().format(Date(value)) }
            else { toDate = value; toButton.text = DateFormat.getDateInstance().format(Date(value)) }
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun runSearch() {
        if (fromDate != null && toDate != null && fromDate!! > toDate!!) { toast("From date must be before To date"); return }
        val direction = when (directionSpinner.selectedItemPosition) {
            1 -> AdvancedSearchFilter.Direction.INCOMING
            2 -> AdvancedSearchFilter.Direction.OUTGOING
            else -> AdvancedSearchFilter.Direction.ANY
        }
        val filter = AdvancedSearchFilter(
            text = textInput.text?.toString().orEmpty(), sender = senderInput.text?.toString().orEmpty(),
            fromDate = fromDate, toDate = toDate, direction = direction, unreadOnly = unreadCheck.isChecked,
            hasAttachment = null, bankOnly = bankCheck.isChecked
        )
        val hits = searcher.search(filter)
        render(hits)
    }

    private fun render(hits: List<AdvancedSearchHit>) {
        results.removeAllViews()
        if (hits.isEmpty()) { results.addView(label("No messages found")); return }
        hits.forEach { hit ->
            val row = TextView(this).apply {
                text = "${hit.address}\n${hit.body}\n${DateFormat.getDateTimeInstance().format(Date(hit.date))}"
                setTextColor(getProperTextColor())
                textSize = 15f
                setPadding(dp(12), dp(12), dp(12), dp(12))
                setOnClickListener { openThread(hit) }
            }
            results.addView(row, LinearLayout.LayoutParams(-1, -2))
        }
    }

    private fun openThread(hit: AdvancedSearchHit) {
        startActivity(Intent(this, ThreadActivity::class.java).apply {
            putExtra(THREAD_ID, hit.threadId)
            putExtra(SEARCHED_MESSAGE_ID, hit.messageId)
        })
    }

    private fun clearFilters() {
        textInput.text?.clear(); senderInput.text?.clear(); directionSpinner.setSelection(0)
        unreadCheck.isChecked = false; bankCheck.isChecked = false
        fromDate = null; toDate = null; fromButton.text = "From date"; toButton.text = "To date"
        results.removeAllViews()
    }
}
