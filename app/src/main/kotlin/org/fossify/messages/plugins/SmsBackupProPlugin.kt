package org.fossify.messages.plugins

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.Telephony
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

object SmsBackupProPlugin {
    private const val FORMAT = "fossify-messages-sms-backup-v1"
    private const val MAX_MESSAGES = 100_000
    private const val MAX_ADDRESS_LENGTH = 512
    private const val MAX_BODY_LENGTH = 100_000

    data class BackupResult(val count: Int)
    data class RestoreResult(val inserted: Int, val skippedDuplicates: Int, val invalid: Int)

    fun isAvailable(context: Context) = PluginLicenseStore.isLicensed(context, PluginRegistry.SMS_BACKUP_PRO)

    fun backup(context: Context, uri: Uri): BackupResult {
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
        val root = JSONObject().put("format", FORMAT).put("messages", array)
        context.contentResolver.openOutputStream(uri)?.use { it.write(root.toString(2).toByteArray(Charsets.UTF_8)) }
            ?: error("Cannot open backup file")
        val result = BackupResult(array.length())
        Toast.makeText(context, "Backup انجام شد: ${result.count} پیام", Toast.LENGTH_LONG).show()
        return result
    }

    fun restore(context: Context, uri: Uri): RestoreResult {
        require(isAvailable(context)) { "SMS Backup Pro is not licensed" }
        val json = context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
            ?: error("Cannot read backup file")
        val root = try { JSONObject(json) } catch (_: JSONException) { error("فایل Backup معتبر نیست") }
        require(root.optString("format") == FORMAT) { "Unsupported backup format" }
        val messages = root.optJSONArray("messages") ?: error("Backup فاقد بخش messages است")
        require(messages.length() <= MAX_MESSAGES) { "Backup بیش از حد بزرگ است" }

        val existing = HashSet<String>()
        val projection = arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE, Telephony.Sms.TYPE)
        context.contentResolver.query(Telephony.Sms.CONTENT_URI, projection, null, null, null)?.use { cursor ->
            val address = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
            val body = cursor.getColumnIndex(Telephony.Sms.BODY)
            val date = cursor.getColumnIndex(Telephony.Sms.DATE)
            val type = cursor.getColumnIndex(Telephony.Sms.TYPE)
            while (cursor.moveToNext()) {
                existing.add(messageKey(
                    if (address >= 0) cursor.getString(address) else "",
                    if (body >= 0) cursor.getString(body) else "",
                    if (date >= 0) cursor.getLong(date) else 0L,
                    if (type >= 0) cursor.getInt(type) else Telephony.Sms.MESSAGE_TYPE_INBOX
                ))
            }
        }

        var inserted = 0
        var skippedDuplicates = 0
        var invalid = 0
        for (i in 0 until messages.length()) {
            val item = messages.optJSONObject(i)
            if (item == null) { invalid++; continue }
            val address = item.optString("address", "").trim()
            val body = item.optString("body", "")
            val date = item.optLong("date", -1L)
            val type = item.optInt("type", -1)
            val read = item.optInt("read", -1)
            if (!isValidMessage(address, body, date, type, read)) { invalid++; continue }

            val key = messageKey(address, body, date, type)
            if (!existing.add(key)) { skippedDuplicates++; continue }
            val values = ContentValues().apply {
                put(Telephony.Sms.ADDRESS, address)
                put(Telephony.Sms.BODY, body)
                put(Telephony.Sms.DATE, date)
                put(Telephony.Sms.TYPE, type)
                put(Telephony.Sms.READ, read)
            }
            try {
                if (context.contentResolver.insert(Telephony.Sms.CONTENT_URI, values) != null) inserted++
                else { invalid++; existing.remove(key) }
            } catch (_: SecurityException) {
                throw SecurityException("برای Restore پیامک، برنامه باید برنامه SMS پیش‌فرض باشد")
            } catch (_: Exception) {
                invalid++
                existing.remove(key)
            }
        }

        val result = RestoreResult(inserted, skippedDuplicates, invalid)
        Toast.makeText(context, "Restore: ${result.inserted} اضافه شد، ${result.skippedDuplicates} تکراری، ${result.invalid} نامعتبر", Toast.LENGTH_LONG).show()
        return result
    }

    private fun isValidMessage(address: String, body: String, date: Long, type: Int, read: Int): Boolean =
        address.isNotBlank() && address.length <= MAX_ADDRESS_LENGTH && body.length <= MAX_BODY_LENGTH &&
            date > 0L && type in 1..6 && read in 0..1

    private fun messageKey(address: String, body: String, date: Long, type: Int): String =
        buildString { append(normalizeAddress(address)); append('\u0000'); append(body); append('\u0000'); append(date); append('\u0000'); append(type) }

    private fun normalizeAddress(value: String): String = value.trim()
        .replace("٠", "0").replace("١", "1").replace("٢", "2").replace("٣", "3").replace("٤", "4")
        .replace("٥", "5").replace("٦", "6").replace("٧", "7").replace("٨", "8").replace("٩", "9")
        .replace("۰", "0").replace("۱", "1").replace("۲", "2").replace("۳", "3").replace("۴", "4")
        .replace("۵", "5").replace("۶", "6").replace("۷", "7").replace("۸", "8").replace("۹", "9")
}
