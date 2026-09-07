package org.fossify.messages.plugins

import android.content.Context
import android.net.Uri
import android.provider.Telephony
import org.json.JSONArray
import org.json.JSONObject

/** Local-only SMS backup. No cloud upload is performed. */
object SmsBackupProPlugin {
    fun isAvailable(context: Context) = PluginLicenseStore.isLicensed(context, PluginRegistry.SMS_BACKUP_PRO)

    fun backup(context: Context, uri: Uri) {
        require(isAvailable(context)) { "SMS Backup Pro is not licensed" }
        val array = JSONArray()
        context.contentResolver.query(Telephony.Sms.CONTENT_URI, null, null, null, "date ASC")?.use { cursor ->
            val address = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
            val body = cursor.getColumnIndex(Telephony.Sms.BODY)
            val date = cursor.getColumnIndex(Telephony.Sms.DATE)
            val type = cursor.getColumnIndex(Telephony.Sms.TYPE)
            val read = cursor.getColumnIndex(Telephony.Sms.READ)
            val thread = cursor.getColumnIndex(Telephony.Sms.THREAD_ID)
            while (cursor.moveToNext()) {
                array.put(JSONObject().apply {
                    put("address", if (address >= 0) cursor.getString(address) else "")
                    put("body", if (body >= 0) cursor.getString(body) else "")
                    put("date", if (date >= 0) cursor.getLong(date) else 0L)
                    put("type", if (type >= 0) cursor.getInt(type) else Telephony.Sms.MESSAGE_TYPE_INBOX)
                    put("read", if (read >= 0) cursor.getInt(read) else 1)
                    put("thread_id", if (thread >= 0) cursor.getLong(thread) else 0L)
                })
            }
        }
        val root = JSONObject().put("format", "fossify-messages-sms-backup-v1").put("messages", array)
        context.contentResolver.openOutputStream(uri)?.use { it.write(root.toString(2).toByteArray(Charsets.UTF_8)) }
            ?: error("Cannot open backup file")
    }

    fun restore(context: Context, uri: Uri) {
        require(isAvailable(context)) { "SMS Backup Pro is not licensed" }
        val root = JSONObject(context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) } ?: error("Cannot read backup file"))
        require(root.optString("format") == "fossify-messages-sms-backup-v1") { "Unsupported backup format" }
        val messages = root.getJSONArray("messages")
        for (i in 0 until messages.length()) {
            val item = messages.getJSONObject(i)
            val values = android.content.ContentValues().apply {
                put(Telephony.Sms.ADDRESS, item.optString("address"))
                put(Telephony.Sms.BODY, item.optString("body"))
                put(Telephony.Sms.DATE, item.optLong("date"))
                put(Telephony.Sms.TYPE, item.optInt("type", Telephony.Sms.MESSAGE_TYPE_INBOX))
                put(Telephony.Sms.READ, item.optInt("read", 1))
            }
            context.contentResolver.insert(Telephony.Sms.CONTENT_URI, values)
        }
    }
}
