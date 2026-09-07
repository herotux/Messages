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
    data class Item(val id: Long, val destination: String, val body: String, val triggerAt: Long, val enabled: Boolean = true)

    fun isAvailable(context: Context) = PluginLicenseStore.isLicensed(context, PluginRegistry.SCHEDULED_SMS_PRO)

    fun list(context: Context): List<Item> {
        val a = JSONArray(context.getSharedPreferences(PREFS, 0).getString(KEY, "[]") ?: "[]")
        return buildList(a.length()) { for (i in 0 until a.length()) { val o = a.getJSONObject(i); add(Item(o.optLong("id"), o.optString("destination"), o.optString("body"), o.optLong("triggerAt"), o.optBoolean("enabled", true))) } }
    }

    fun schedule(context: Context, item: Item) {
        require(isAvailable(context)) { "Scheduled SMS Pro is not licensed" }
        save(context, item)
        val alarm = context.getSystemService(AlarmManager::class.java)
        val pi = pendingIntent(context, item.id)
        if (item.enabled) {
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, item.triggerAt, pi)
        } else alarm.cancel(pi)
    }

    fun cancel(context: Context, id: Long) {
        context.getSystemService(AlarmManager::class.java).cancel(pendingIntent(context, id))
        saveAll(context, list(context).filterNot { it.id == id })
    }

    private fun pendingIntent(context: Context, id: Long) = PendingIntent.getBroadcast(context, id.hashCode(), Intent(context, PluginScheduledSmsReceiver::class.java).putExtra("scheduled_plugin_id", id), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    private fun save(context: Context, item: Item) = saveAll(context, list(context).filterNot { it.id == item.id } + item)
    private fun saveAll(context: Context, values: List<Item>) { val a = JSONArray(); values.forEach { a.put(JSONObject().apply { put("id", it.id); put("destination", it.destination); put("body", it.body); put("triggerAt", it.triggerAt); put("enabled", it.enabled) }) }; context.getSharedPreferences(PREFS, 0).edit().putString(KEY, a.toString()).apply() }
}
