package org.fossify.messages.helpers

import android.content.Context
import android.provider.Telephony

class AdvancedSmsSearch(private val context: Context) {
    fun search(filter: AdvancedSearchFilter, limit: Int = 500): List<AdvancedSearchHit> {
        val projection = arrayOf(
            Telephony.Sms._ID, Telephony.Sms.THREAD_ID, Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY, Telephony.Sms.DATE, Telephony.Sms.TYPE,
            Telephony.Sms.READ, Telephony.Sms.SUBJECT
        )
        val clauses = mutableListOf<String>()
        val args = mutableListOf<String>()
        if (filter.text.isNotBlank()) {
            clauses += "(${Telephony.Sms.BODY} LIKE ? OR ${Telephony.Sms.ADDRESS} LIKE ? OR ${Telephony.Sms.SUBJECT} LIKE ?)"
            val q = "%${filter.text.trim()}%"
            args += q; args += q; args += q
        }
        if (filter.sender.isNotBlank()) {
            clauses += "${Telephony.Sms.ADDRESS} LIKE ?"
            args += "%${filter.sender.trim()}%"
        }
        filter.fromDate?.let { clauses += "${Telephony.Sms.DATE} >= ?"; args += it.toString() }
        filter.toDate?.let { clauses += "${Telephony.Sms.DATE} <= ?"; args += it.toString() }
        when (filter.direction) {
            AdvancedSearchFilter.Direction.INCOMING -> { clauses += "${Telephony.Sms.TYPE} = ?"; args += Telephony.Sms.MESSAGE_TYPE_INBOX.toString() }
            AdvancedSearchFilter.Direction.OUTGOING -> { clauses += "${Telephony.Sms.TYPE} = ?"; args += Telephony.Sms.MESSAGE_TYPE_SENT.toString() }
            AdvancedSearchFilter.Direction.ANY -> Unit
        }
        if (filter.unreadOnly) clauses += "${Telephony.Sms.READ} = 0"
        val selection = clauses.takeIf { it.isNotEmpty() }?.joinToString(" AND ")
        val result = ArrayList<AdvancedSearchHit>()
        context.contentResolver.query(
            Telephony.Sms.CONTENT_URI, projection, selection, args.toTypedArray(),
            "${Telephony.Sms.DATE} DESC"
        )?.use { cursor ->
            val id = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
            val thread = cursor.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)
            val address = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val body = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val date = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
            val type = cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE)
            val read = cursor.getColumnIndexOrThrow(Telephony.Sms.READ)
            val subject = cursor.getColumnIndex(Telephony.Sms.SUBJECT)
            while (cursor.moveToNext() && result.size < limit) {
                val bodyText = cursor.getString(body).orEmpty()
                val sender = cursor.getString(address).orEmpty()
                val bankId = BankSenderLogoResolver.resourceName(sender)
                if (filter.bankOnly && bankId == null) continue
                result += AdvancedSearchHit(
                    cursor.getLong(id), cursor.getLong(thread), sender, bodyText,
                    cursor.getLong(date), cursor.getInt(type), cursor.getInt(read) == 1,
                    if (subject >= 0) cursor.getString(subject) else null,
                    bankId != null, bankId
                )
            }
        }
        return result
    }
}
