package org.fossify.messages.plugins

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import org.fossify.messages.receivers.PluginScheduledSmsReceiver
import org.json.JSONArray
import org.json.JSONObject

object ScheduledSmsPlugin {
    private const val PREFS = "plugin_scheduled_sms"
    private const val KEY = "items"
    private const val EXTRA_ID = "scheduled_plugin_id"

    data class Item(
        val id: Long,
        val destination: String,
        val body: String,
        val triggerAt: Long,
        val enabled: Boolean = true
    )

    fun isAvailable(context: Context) =
        PluginLicenseStore.isLicensed(context, PluginRegistry.SCHEDULED_SMS_PRO)

    fun list(context: Context): List<Item> {
        val array = JSONArray(
            context.getSharedPreferences(PREFS, 0).getString(KEY, "[]") ?: "[]"
        )
        return buildList(array.length()) {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                add(
                    Item(
                        id = item.optLong("id"),
                        destination = item.optString("destination"),
                        body = item.optString("body"),
                        triggerAt = item.optLong("triggerAt"),
                        enabled = item.optBoolean("enabled", true)
                    )
                )
            }
        }
    }

    fun schedule(context: Context, item: Item) {
        require(isAvailable(context)) { "Scheduled SMS Pro is not licensed" }
        validate(item)
        cancelAlarm(context, item.id)
        save(context, item.copy(enabled = true))
        scheduleAlarm(context, item)
    }

    /** Updates an existing item and replaces its AlarmManager entry. */
    fun update(context: Context, item: Item) {
        require(isAvailable(context)) { "Scheduled SMS Pro is not licensed" }
        validate(item)
        cancelAlarm(context, item.id)
        save(context, item.copy(enabled = true))
        scheduleAlarm(context, item)
    }

    /** Enables or disables an existing scheduled message without deleting it. */
    fun setEnabled(context: Context, id: Long, enabled: Boolean) {
        val item = list(context).firstOrNull { it.id == id } ?: return
        cancelAlarm(context, id)
        val updated = item.copy(enabled = enabled)
        save(context, updated)
        if (enabled && item.triggerAt > System.currentTimeMillis() && isAvailable(context)) {
            scheduleAlarm(context, updated)
        }
    }

    /** Recreates AlarmManager entries after a reboot or package replacement. */
    fun rescheduleAll(context: Context) {
        if (!isAvailable(context)) return

        val now = System.currentTimeMillis()
        list(context)
            .asSequence()
            .filter { it.enabled && it.triggerAt > now }
            .forEach { scheduleAlarm(context, it) }
    }

    fun cancel(context: Context, id: Long) {
        cancelAlarm(context, id)
        saveAll(context, list(context).filterNot { it.id == id })
    }

    fun markCompleted(context: Context, id: Long) {
        cancelAlarm(context, id)
        saveAll(context, list(context).mapNotNull { item ->
            if (item.id == id) item.copy(enabled = false) else item
        })
    }

    private fun validate(item: Item) {
        require(item.destination.isNotBlank()) { "Destination is required" }
        require(item.body.isNotBlank()) { "Message body is required" }
        require(item.triggerAt > System.currentTimeMillis()) { "Scheduled time must be in the future" }
    }

    private fun scheduleAlarm(context: Context, item: Item) {
        context.getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            item.triggerAt,
            pendingIntent(context, item.id)
        )
    }

    private fun cancelAlarm(context: Context, id: Long) {
        context.getSystemService(AlarmManager::class.java)
            .cancel(pendingIntent(context, id))
    }

    private fun pendingIntent(context: Context, id: Long): PendingIntent {
        val requestCode = (id xor (id ushr 32)).toInt()
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, PluginScheduledSmsReceiver::class.java)
                .putExtra(EXTRA_ID, id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun save(context: Context, item: Item) =
        saveAll(context, list(context).filterNot { it.id == item.id } + item)

    private fun saveAll(context: Context, values: List<Item>) {
        val array = JSONArray()
        values.forEach { item ->
            array.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("destination", item.destination)
                    put("body", item.body)
                    put("triggerAt", item.triggerAt)
                    put("enabled", item.enabled)
                }
            )
        }
        context.getSharedPreferences(PREFS, 0)
            .edit()
            .putString(KEY, array.toString())
            .apply()
    }

    internal const val EXTRA_ID_KEY = EXTRA_ID
}