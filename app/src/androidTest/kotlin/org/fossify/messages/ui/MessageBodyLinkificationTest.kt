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
    fun iranianBankAmount_isNotLinkifiedAsLocationOrUrl() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val body = LayoutInflater.from(context)
            .inflate(R.layout.item_message, null)
            .findViewById<TextView>(R.id.thread_message_body)

        val message = "انتقال وجه آنی\nمبلغ 160,000,000 ريال"
        body.text = message

        val spanned = body.text as? Spanned
        val spans = spanned?.getSpans(0, message.length, URLSpan::class.java).orEmpty()

        check(spans.isEmpty()) {
            "Financial amount must not be converted into a URL/location action: $spans"
        }
        check(body.textClassifier == TextClassifier.NO_OP) {
            "SMS message bodies must not use smart text classification"
        }
    }

    @Test
    fun normalWebUrl_remainsLinkified() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val body = LayoutInflater.from(context)
            .inflate(R.layout.item_message, null)
            .findViewById<TextView>(R.id.thread_message_body)

        val message = "https://example.com"
        body.text = message

        val spanned = body.text as? Spanned
        val spans = spanned?.getSpans(0, message.length, URLSpan::class.java).orEmpty()

        check(spans.isNotEmpty()) {
            "Normal web URLs must remain linkified"
        }
    }
}
