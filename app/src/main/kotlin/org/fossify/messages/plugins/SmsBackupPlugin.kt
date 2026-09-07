package org.fossify.messages.plugins

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.Telephony
import org.json.JSONArray
import org.json.JSONObject

object SmsBackupPlugin {
    data class BackupResult(val count: Int, val json: String)

    fun isAvailable(context: Context) = PluginLicenseStore.isLicensed(context, PluginRegistry.SMS_BACKUP_PRO)

    fun createBackup(context: Context, sinceMillis: Long? = null, untilMillis: Long? = null): BackupResult {
        val projection = arrayOf("address", "date", "type", "subject", "body", "read", "thread_id", "status", "service_center")
        val selection = buildList { if (sinceMillis != null) add("date >= ?"); if (untilMillis != null) add("date <= ?") }.joinToString(" AND ").ifBlank { null }
        val args = buildList { if (sinceMillis != null) add(sinceMillis.toString()); if (untilMillis != null) add(untilMillis.toString()) }.toTypedArray()
        val messages = JSONArray()
        context.contentResolver.query(Telephony.Sms.CONTENT_URI, projection, selection, args.takeIf { it.isNotEmpty() }, "date ASC")?.use { c ->
            val idx = projection.associateWith { c.getColumnIndex(it) }
            while (c.moveToNext()) {
                messages.put(JSONObject().apply {
                    projection.forEach { key -> val i = idx[key] ?: -1; if (i >= 0) put(key, c.getString(i)) }
                })
            }
        }
        return BackupResult(messages.length(), JSONObject().put("format", 1).put("createdAt", System.currentTimeMillis()).put("messages", messages).toString())
    }

    /** Restores messages through the Telephony provider. Only works when Messages is the default SMS app. */
    fun restore(context: Context, json: String): Int {
        val root = JSONObject(json); val messages = root.optJSONArray("messages") ?: return 0
        var restored = 0
        for (i in 0 until messages.length()) {
            val o = messages.getJSONObject(i)
            val values = ContentValues().apply {
                put("address", o.optString("address")); put("date", o.optLong("date")); put("type", o.optInt("type", Telephony.Sms.MESSAGE_TYPE_INBOX)); put("subject", o.optString("subject")); put("body", o.optString("body")); put("read", o.optInt("read", 1)); put("status", o.optInt("status", -1)); put("service_center", o.optString("service_center"))
            }
            if (context.contentResolver.insert(Telephony.Sms.CONTENT_URI, values) != null) restored++
        }
        return restored
    }
}
