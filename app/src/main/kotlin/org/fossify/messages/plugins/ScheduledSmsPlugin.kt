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
        require(item.destination.isNotBlank()) { "Destination is required" }
        require(item.body.isNotBlank()) { "Message body is required" }
        require(item.triggerAt > System.currentTimeMillis()) { "Scheduled time must be in the future" }

        save(context, item.copy(enabled = true))
        val alarm = context.getSystemService(AlarmManager::class.java)
        alarm.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            item.triggerAt,
            pendingIntent(context, item.id)
        )
    }

    fun cancel(context: Context, id: Long) {
        context.getSystemService(AlarmManager::class.java)
            .cancel(pendingIntent(context, id))
        saveAll(context, list(context).filterNot { it.id == id })
    }

    fun markCompleted(context: Context, id: Long) {
        context.getSystemService(AlarmManager::class.java)
            .cancel(pendingIntent(context, id))
        saveAll(context, list(context).mapNotNull { item ->
            if (item.id == id) item.copy(enabled = false) else item
        })
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
