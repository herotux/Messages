package org.fossify.messages.helpers

import android.content.Context
import android.provider.Telephony
import android.util.AttributeSet
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import org.fossify.messages.helpers.THREAD_ID

/**
 * Small confirmation banner shown at the top of a conversation when the
 * detector has medium confidence that the conversation is a bank SMS thread.
 */
class BankConversationVerificationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        orientation = VERTICAL
        setPadding(dp(16), dp(10), dp(16), dp(10))
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
        if (threadId == 0L || prefs.contains(key(threadId))) return

        val projection = arrayOf(
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
        )

        val detection = runCatching {
            context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                projection,
                "${Telephony.Sms.THREAD_ID} = ?",
                arrayOf(threadId.toString()),
                "${Telephony.Sms.DATE} DESC",
            )?.use { cursor ->
                val addressIndex = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyIndex = cursor.getColumnIndex(Telephony.Sms.BODY)
                var result: BankSmsDetector.Detection? = null
                var checked = 0
                while (cursor.moveToNext() && checked < MAX_MESSAGES_TO_CHECK) {
                    val sender = if (addressIndex >= 0) cursor.getString(addressIndex).orEmpty() else ""
                    val body = if (bodyIndex >= 0) cursor.getString(bodyIndex).orEmpty() else ""
                    val candidate = BankSmsDetector.detect(sender, body)
                    if (candidate?.confidence == BankSmsDetector.Confidence.MEDIUM) {
                        result = candidate
                        break
                    }
                    checked++
                }
                result
            }
        }.getOrNull() ?: return

        if (detection == null) return
        showSuggestion(threadId, detection)
    }

    private fun showSuggestion(threadId: Long, detection: BankSmsDetector.Detection) {
        removeAllViews()
        val bankName = detection.bank.persianName

        val title = TextView(context).apply {
            text = "آیا این پیام مربوط به $bankName است؟"
            textSize = 15f
            setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurface))
        }
        addView(title, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        val buttons = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.END
        }

        val noButton = Button(context).apply {
            text = "خیر"
            setOnClickListener {
                prefs.edit().putBoolean(key(threadId), false).apply()
                isVisible = false
            }
        }
        val yesButton = Button(context).apply {
            text = "بله، $bankName"
            setOnClickListener {
                prefs.edit().putBoolean(key(threadId), true).apply()
                isVisible = false
            }
        }

        buttons.addView(noButton, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
        buttons.addView(yesButton, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            marginStart = dp(8)
        })
        addView(buttons, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        isVisible = true
    }

    private fun key(threadId: Long) = "confirmed_$threadId"
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun resolveColor(attr: Int): Int {
        val typed = android.util.TypedValue()
        context.theme.resolveAttribute(attr, typed, true)
        return if (typed.resourceId != 0) {
            androidx.core.content.ContextCompat.getColor(context, typed.resourceId)
        } else typed.data
    }

    companion object {
        private const val PREFS_NAME = "bank_conversation_verification"
        private const val MAX_MESSAGES_TO_CHECK = 30
    }
}
