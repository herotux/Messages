package org.fossify.messages.helpers

import android.content.Context
import android.provider.Telephony
import android.util.AttributeSet
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible

/** Compact confirmation card for a medium-confidence bank conversation. */
class BankConversationVerificationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {
    init {
        orientation = VERTICAL
        setPadding(dp(16), dp(12), dp(16), dp(12))
        gravity = Gravity.CENTER_VERTICAL
        isVisible = false
        setBackgroundColor(resolveColor(com.google.android.material.R.attr.colorSurfaceContainerHighest))
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        post { inspectConversation() }
    }

    private fun inspectConversation() {
        val activity = context as? android.app.Activity ?: return
        val threadId = activity.intent.getLongExtra(THREAD_ID, 0L)
        if (threadId == 0L || BankConversationVerificationStore.getConfirmedBank(context, threadId) != null || BankConversationVerificationStore.isRejected(context, threadId)) return

        val projection = arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY)
        val detection = runCatching {
            context.contentResolver.query(
                Telephony.Sms.CONTENT_URI, projection,
                "${Telephony.Sms.THREAD_ID} = ?", arrayOf(threadId.toString()),
                "${Telephony.Sms.DATE} DESC"
            )?.use { cursor ->
                val addressIndex = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyIndex = cursor.getColumnIndex(Telephony.Sms.BODY)
                var result: BankSmsDetector.Detection? = null
                var checked = 0
                while (cursor.moveToNext() && checked < MAX_MESSAGES_TO_CHECK) {
                    val sender = if (addressIndex >= 0) cursor.getString(addressIndex).orEmpty() else ""
                    val body = if (bodyIndex >= 0) cursor.getString(bodyIndex).orEmpty() else ""
                    val candidate = BankSmsDetector.detect(sender, body)
                    if (candidate?.confidence == BankSmsDetector.Confidence.MEDIUM) { result = candidate; break }
                    checked++
                }
                result
            }
        }.getOrNull() ?: return
        detection?.let { showSuggestion(threadId, it) }
    }

    private fun showSuggestion(threadId: Long, detection: BankSmsDetector.Detection) {
        removeAllViews()
        val title = TextView(context).apply {
            text = "این گفتگو احتمالاً مربوط به ${detection.bank.persianName} است"
            textSize = 15f
            setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurface))
        }
        addView(title, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        val subtitle = TextView(context).apply {
            text = "آیا تشخیص بانک درست است؟"
            textSize = 13f
            setPadding(0, dp(4), 0, dp(8))
            setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurfaceVariant))
        }
        addView(subtitle, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        val buttons = LinearLayout(context).apply { orientation = HORIZONTAL; gravity = Gravity.END }

        fun dismiss(answer: Boolean) {
            if (answer) BankConversationVerificationStore.confirm(context, threadId, detection.bank)
            else BankConversationVerificationStore.reject(context, threadId)
            removeAllViews()
            isVisible = false
        }

        val noButton = Button(context).apply { text = "خیر"; setOnClickListener { dismiss(false) } }
        val yesButton = Button(context).apply { text = "بله، ${detection.bank.persianName}"; setOnClickListener { dismiss(true) } }
        buttons.addView(noButton, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
        buttons.addView(yesButton, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply { marginStart = dp(8) })
        addView(buttons, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        isVisible = true
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
    private fun resolveColor(attr: Int): Int {
        val typed = android.util.TypedValue()
        context.theme.resolveAttribute(attr, typed, true)
        return if (typed.resourceId != 0) androidx.core.content.ContextCompat.getColor(context, typed.resourceId) else typed.data
    }

    companion object {
        private const val MAX_MESSAGES_TO_CHECK = 30
    }
}
