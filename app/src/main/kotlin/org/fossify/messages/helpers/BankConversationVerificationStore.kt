package org.fossify.messages.helpers

import android.content.Context

/** Persists the user's explicit bank choice for a conversation. */
object BankConversationVerificationStore {
    private const val PREFS_NAME = "bank_conversation_verification"
    private const val PREFIX = "bank_"
    private const val REJECTED_PREFIX = "rejected_"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getConfirmedBank(context: Context, threadId: Long): IranianBankRegistry.BankInfo? {
        if (threadId == 0L) return null
        val value = prefs(context).getString(PREFIX + threadId, null) ?: return null
        return runCatching { IranianBankRegistry.findById(IranianBankRegistry.BankId.valueOf(value)) }.getOrNull()
    }

    fun isRejected(context: Context, threadId: Long): Boolean =
        threadId != 0L && prefs(context).getBoolean(REJECTED_PREFIX + threadId, false)

    fun confirm(context: Context, threadId: Long, bank: IranianBankRegistry.BankInfo) {
        if (threadId == 0L) return
        prefs(context).edit()
            .putString(PREFIX + threadId, bank.id.name)
            .remove(REJECTED_PREFIX + threadId)
            .apply()
    }

    fun reject(context: Context, threadId: Long) {
        if (threadId == 0L) return
        prefs(context).edit()
            .remove(PREFIX + threadId)
            .putBoolean(REJECTED_PREFIX + threadId, true)
            .apply()
    }
}
