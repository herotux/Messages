package org.fossify.messages.helpers

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.telephony.PhoneNumberUtils
import org.fossify.messages.models.Conversation
import java.util.concurrent.Executors

/**
 * Classifies ordinary personal conversations for the Personal folder.
 * A conversation is personal when it is not a group/bank conversation and its
 * address is either an Iranian mobile number or a number saved in Contacts.
 */
class PersonalConversationClassifier(context: Context) {
    private val appContext = context.applicationContext
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var contactNumbers: Set<String> = emptySet()
    @Volatile
    private var loaded = false
    @Volatile
    private var loading = false

    fun ensureLoaded(onLoaded: (() -> Unit)? = null) {
        if (loaded) {
            onLoaded?.let { mainHandler.post(it) }
            return
        }
        synchronized(this) {
            if (loaded || loading) return
            loading = true
        }
        executor.execute {
            val numbers = HashSet<String>()
            runCatching {
                appContext.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                    null,
                    null,
                    null,
                )?.use { cursor ->
                    val index = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    if (index >= 0) {
                        while (cursor.moveToNext()) {
                            canonical(cursor.getString(index))?.let(numbers::add)
                        }
                    }
                }
            }
            contactNumbers = numbers
            loaded = true
            loading = false
            onLoaded?.let { mainHandler.post(it) }
        }
    }

    fun isPersonal(conversation: Conversation, isBank: Boolean): Boolean {
        if (conversation.isGroupConversation || isBank) return false
        val canonical = canonical(conversation.phoneNumber)
        return canonical?.let { isIranianMobile(it) || it in contactNumbers } ?: false
    }

    private fun canonical(number: String?): String? {
        if (number.isNullOrBlank()) return null

        // PhoneNumberUtils may preserve a leading '+'. Remove it before applying
        // the Iranian +98/0098 conversion so both forms behave identically.
        val normalized = PhoneNumberUtils.normalizeNumber(
            PhoneNumberUtils.replaceUnicodeDigits(number)
        )
        val digits = normalized.removePrefix("+")
        if (digits.isEmpty()) return null

        return when {
            digits.startsWith("0098") && digits.length == 14 -> "0" + digits.substring(4)
            digits.startsWith("98") && digits.length == 12 -> "0" + digits.substring(2)
            digits.length == 10 && digits.startsWith("9") -> "0$digits"
            digits.length == 11 && digits.startsWith("09") -> digits
            else -> digits
        }
    }

    private fun isIranianMobile(number: String): Boolean =
        number.length == 11 && number.startsWith("09") && number[2].isDigit()
}
