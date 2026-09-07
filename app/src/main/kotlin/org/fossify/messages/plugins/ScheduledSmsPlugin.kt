package org.fossify.messages.plugins

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import org.fossify.messages.receivers.PluginScheduledSmsReceiver
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ScheduledSmsPlugin {
    private const val PREFS = "plugin_scheduled_sms"
    private const val KEY = "items"
    private const val EXTRA_ID = "scheduled_plugin_id"
    const val MAX_RETRIES = 3
    const val RETRY_DELAY_MS = 15 * 60 * 1000L

    data class Item(
        val id: Long,
        val destination: String,
        val body: String,
        val triggerAt: Long,
        val enabled: Boolean = true,
        val completed: Boolean = false,
        val retryCount: Int = 0,
        val lastError: String = ""
    )

    fun isAvailable(context: Context) = PluginLicenseStore.isLicensed(context, PluginRegistry.SCHEDULED_SMS_PRO)

    fun list(context: Context): List<Item> {
        val array = JSONArray(context.getSharedPreferences(PREFS, 0).getString(KEY, "[]") ?: "[]")
        return buildList(array.length()) {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                add(Item(
                    id = item.optLong("id"),
                    destination = item.optString("destination"),
                    body = item.optString("body"),
                    triggerAt = item.optLong("triggerAt"),
                    enabled = item.optBoolean("enabled", true),
                    completed = item.optBoolean("completed", false),
                    retryCount = item.optInt("retryCount", 0),
                    lastError = item.optString("lastError", "")
                ))
            }
        }
    }

    fun schedule(context: Context, item: Item) {
        require(isAvailable(context)) { "Scheduled SMS Pro is not licensed" }
        validate(item)
        cancelAlarm(context, item.id)
        val scheduled = item.copy(enabled = true, completed = false, retryCount = 0, lastError = "")
        save(context, scheduled)
        scheduleAlarm(context, scheduled)
    }

    fun update(context: Context, item: Item) {
        require(isAvailable(context)) { "Scheduled SMS Pro is not licensed" }
        validate(item)
        cancelAlarm(context, item.id)
        val updated = item.copy(enabled = true, completed = false, retryCount = 0, lastError = "")
        save(context, updated)
        scheduleAlarm(context, updated)
    }

    fun setEnabled(context: Context, id: Long, enabled: Boolean) {
        val item = list(context).firstOrNull { it.id == id } ?: return
        if (item.completed) return
        if (enabled) require(item.triggerAt > System.currentTimeMillis()) { "زمان ارسال باید در آینده باشد" }
        cancelAlarm(context, id)
        val updated = item.copy(enabled = enabled)
        save(context, updated)
        if (enabled && isAvailable(context)) scheduleAlarm(context, updated)
    }

    fun rescheduleAll(context: Context) {
        if (!isAvailable(context)) return
        val now = System.currentTimeMillis()
        list(context).asSequence()
            .filter { it.enabled && !it.completed && it.triggerAt > now }
            .forEach { scheduleAlarm(context, it) }
    }

    fun cancel(context: Context, id: Long) {
        cancelAlarm(context, id)
        saveAll(context, list(context).filterNot { it.id == id })
    }

    fun markCompleted(context: Context, id: Long) {
        cancelAlarm(context, id)
        saveAll(context, list(context).map { item -> if (item.id == id) item.copy(enabled = false, completed = true, lastError = "") else item })
    }

    /** Records a failure and schedules an automatic retry up to MAX_RETRIES times. */
    fun markFailed(context: Context, id: Long, error: String) {
        val item = list(context).firstOrNull { it.id == id } ?: return
        val nextRetry = item.retryCount + 1
        cancelAlarm(context, id)
        if (nextRetry > MAX_RETRIES) {
            save(context, item.copy(enabled = false, lastError = error.take(500), retryCount = nextRetry))
            return
        }
        val retryAt = System.currentTimeMillis() + RETRY_DELAY_MS
        val updated = item.copy(enabled = true, triggerAt = retryAt, retryCount = nextRetry, lastError = error.take(500))
        save(context, updated)
        scheduleAlarm(context, updated)
    }

    fun retryNow(context: Context, id: Long) {
        val item = list(context).firstOrNull { it.id == id && !it.completed } ?: return
        val updated = item.copy(enabled = true, triggerAt = System.currentTimeMillis() + 2_000L)
        save(context, updated)
        cancelAlarm(context, id)
        scheduleAlarm(context, updated)
    }

    fun resolvePlaceholders(body: String, destination: String, at: Long): String {
        val date = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(at))
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(at))
        return body
            .replace("{destination}", destination)
            .replace("{date}", date)
            .replace("{time}", time)
    }

    private fun validate(item: Item) {
        require(item.destination.isNotBlank()) { "شماره گیرنده الزامی است" }
        require(item.body.isNotBlank()) { "متن پیام الزامی است" }
        require(item.triggerAt > System.currentTimeMillis()) { "زمان ارسال باید در آینده باشد" }
    }

    private fun scheduleAlarm(context: Context, item: Item) {
        context.getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, item.triggerAt, pendingIntent(context, item.id))
    }

    private fun cancelAlarm(context: Context, id: Long) {
        context.getSystemService(AlarmManager::class.java).cancel(pendingIntent(context, id))
    }

    private fun pendingIntent(context: Context, id: Long): PendingIntent {
        val requestCode = (id xor (id ushr 32)).toInt()
        return PendingIntent.getBroadcast(context, requestCode, Intent(context, PluginScheduledSmsReceiver::class.java).putExtra(EXTRA_ID, id), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun save(context: Context, item: Item) = saveAll(context, list(context).filterNot { it.id == item.id } + item)

    private fun saveAll(context: Context, values: List<Item>) {
        val array = JSONArray()
        values.forEach { item ->
            array.put(JSONObject().apply {
                put("id", item.id); put("destination", item.destination); put("body", item.body)
                put("triggerAt", item.triggerAt); put("enabled", item.enabled); put("completed", item.completed)
                put("retryCount", item.retryCount); put("lastError", item.lastError)
            })
        }
        context.getSharedPreferences(PREFS, 0).edit().putString(KEY, array.toString()).apply()
    }

    internal const val EXTRA_ID_KEY = EXTRA_ID
}
