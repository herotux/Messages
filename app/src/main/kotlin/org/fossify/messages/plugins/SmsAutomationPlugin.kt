package org.fossify.messages.plugins

import android.content.Context
import android.os.Build
import android.telephony.SmsManager
import org.json.JSONArray
import org.json.JSONObject
import org.fossify.messages.helpers.DebugLog

/**
 * Built-in premium plugin. The plugin is compiled into Messages but is inactive
 * unless the user has an entitlement and explicitly enables it.
 */
object SmsAutomationPlugin {
    private const val PREFS = "messages_sms_automation"
    private const val RULES = "rules"
    private const val ENABLED = "enabled"

    data class Rule(
        val id: Long,
        val name: String,
        val sender: String,
        val containsText: String,
        val destination: String,
        val enabled: Boolean = true
    )

    fun isEnabled(context: Context): Boolean =
        PluginLicenseStore.isSmsAutomationLicensed(context) &&
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(ENABLED, enabled)
            .apply()
    }

    fun getRules(context: Context): List<Rule> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(RULES, "[]") ?: "[]"
        return runCatching {
            val array = JSONArray(raw)
            buildList(array.length()) {
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    add(
                        Rule(
                            id = item.optLong("id"),
                            name = item.optString("name"),
                            sender = item.optString("sender"),
                            containsText = item.optString("containsText"),
                            destination = item.optString("destination"),
                            enabled = item.optBoolean("enabled", true)
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun addRule(context: Context, rule: Rule) {
        val rules = getRules(context).toMutableList()
        rules.removeAll { it.id == rule.id }
        rules.add(rule)
        saveRules(context, rules)
    }

    fun removeRule(context: Context, id: Long) {
        saveRules(context, getRules(context).filterNot { it.id == id })
    }

    fun setRuleEnabled(context: Context, id: Long, enabled: Boolean) {
        saveRules(context, getRules(context).map { if (it.id == id) it.copy(enabled = enabled) else it })
    }

    fun processIncomingSms(context: Context, sender: String, body: String, subscriptionId: Int) {
        if (!isEnabled(context) || sender.isBlank() || body.isBlank()) return

        getRules(context).filter { it.enabled && matches(it, sender, body) }.forEach { rule ->
            sendFullMessage(context, rule, body, subscriptionId)
        }
    }

    private fun saveRules(context: Context, rules: List<Rule>) {
        val array = JSONArray()
        rules.forEach { rule ->
            array.put(
                JSONObject()
                    .put("id", rule.id)
                    .put("name", rule.name)
                    .put("sender", rule.sender)
                    .put("containsText", rule.containsText)
                    .put("destination", rule.destination)
                    .put("enabled", rule.enabled)
            )
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(RULES, array.toString())
            .apply()
    }

    private fun matches(rule: Rule, sender: String, body: String): Boolean {
        val senderMatches = rule.sender.isBlank() || numbersMatch(rule.sender, sender)
        val textMatches = rule.containsText.isBlank() || body.contains(rule.containsText, ignoreCase = true)
        return senderMatches && textMatches && rule.destination.isNotBlank()
    }

    private fun numbersMatch(expected: String, actual: String): Boolean {
        val a = digitsOnly(expected)
        val b = digitsOnly(actual)
        if (a.isEmpty() || b.isEmpty()) return false
        return a == b || (a.length >= 8 && b.endsWith(a)) || (b.length >= 8 && a.endsWith(b))
    }

    private fun digitsOnly(value: String): String {
        val normalized = value.map {
            when (it) {
                '۰' -> '0'; '۱' -> '1'; '۲' -> '2'; '۳' -> '3'; '۴' -> '4'
                '۵' -> '5'; '۶' -> '6'; '۷' -> '7'; '۸' -> '8'; '۹' -> '9'
                '٠' -> '0'; '١' -> '1'; '٢' -> '2'; '٣' -> '3'; '٤' -> '4'
                '٥' -> '5'; '٦' -> '6'; '٧' -> '7'; '٨' -> '8'; '٩' -> '9'
                else -> it
            }
        }
        return normalized.filter(Char::isDigit)
    }

    private fun sendFullMessage(context: Context, rule: Rule, body: String, subscriptionId: Int) {
        runCatching {
            val manager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (subscriptionId >= 0) SmsManager.getSmsManagerForSubscriptionId(subscriptionId) else SmsManager.getDefault()
            } else if (subscriptionId >= 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                SmsManager.getSmsManagerForSubscriptionId(subscriptionId)
            } else {
                SmsManager.getDefault()
            }

            val parts = manager.divideMessage(body)
            manager.sendMultipartTextMessage(rule.destination, null, parts, null, null)
            DebugLog.write(context, "SMS_AUTOMATION_FORWARDED rule=${rule.id} parts=${parts.size}")
        }.onFailure {
            DebugLog.write(context, "SMS_AUTOMATION_SEND_FAILED rule=${rule.id} ${it.javaClass.simpleName}: ${it.message}")
        }
    }
}
