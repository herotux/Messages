package org.fossify.messages.receivers

import android.app.Activity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsStatusSentReceiverTest {
    @Test
    fun conversationUpdateRunsOnlyAfterSuccessfulSend() {
        assertTrue(
            SmsStatusSentReceiver.shouldUpdateConversationAfterSuccessfulSend(Activity.RESULT_OK)
        )
        assertFalse(
            SmsStatusSentReceiver.shouldUpdateConversationAfterSuccessfulSend(Activity.RESULT_CANCELED)
        )
    }
}
