package org.fossify.messages.ui

import android.text.Spanned
import android.text.TextClassifier
import android.text.style.URLSpan
import android.view.LayoutInflater
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.fossify.messages.R
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MessageBodyLinkificationTest {
    @Test
    fun iranianBankAmounts_areNotLinkifiedAsLocationOrUrl() {
        val amounts = listOf(
            "مبلغ 160,000,000 ريال",
            "مبلغ 160000000 ريال",
            "مبلغ ۱۶۰,۰۰۰,۰۰۰ ریال",
            "مبلغ ۱۶۰۰۰۰۰۰۰ تومان"
        )

        amounts.forEach { message ->
            val body = newMessageBody()
            body.text = "انتقال وجه آنی\n$message"

            val spanned = body.text as? Spanned
            val spans = spanned?.getSpans(0, body.length(), URLSpan::class.java).orEmpty()

            check(spans.isEmpty()) {
                "Financial amount must not be converted into a URL/location action: $message"
            }
            check(body.textClassifier == TextClassifier.NO_OP) {
                "SMS message bodies must not use smart text classification"
            }
        }
    }

    @Test
    fun normalWebUrl_remainsLinkified() {
        val body = newMessageBody()
        val message = "https://example.com"
        body.text = message

        val spanned = body.text as? Spanned
        val spans = spanned?.getSpans(0, message.length, URLSpan::class.java).orEmpty()

        check(spans.isNotEmpty()) {
            "Normal web URLs must remain linkified"
        }
    }

    @Test
    fun bankCardNumber_isPreservedInMessageBody() {
        val body = newMessageBody()
        val message = "شماره کارت: 6037 9918 1234 5678"
        body.text = message

        check(body.text.toString() == message) {
            "Bank card number text must remain visible and preserve spacing"
        }
        check(body.text.toString().contains("6037 9918 1234 5678")) {
            "Formatted 16-digit card number must remain visible"
        }
    }

    private fun newMessageBody(): TextView {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return LayoutInflater.from(context)
            .inflate(R.layout.item_message, null)
            .findViewById(R.id.thread_message_body)
    }
}
