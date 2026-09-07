package org.fossify.messages.plugins

import android.content.ContentValues
import android.content.Context
import android.provider.Telephony
import org.json.JSONArray
import org.json.JSONObject
import org.fossify.messages.helpers.DebugLog

/** Built-in premium automation engine. It never forwards incoming messages. */
object SmsAutomationPlugin {
    private const val PREFS = "messages_sms_automation"
    private const val RULES = "rules"
    private const val ENABLED = "enabled"
    enum class Action { MARK_READ, DELETE }
    data class Rule(val id: Long, val name: String, val sender: String, val containsText: String, val action: Action = Action.MARK_READ, val enabled: Boolean = true)
    fun isEnabled(context: Context): Boolean = PluginLicenseStore.isLicensed(context, PluginRegistry.SMS_AUTOMATION) && context.getSharedPreferences(PREFS, 0).getBoolean(ENABLED, false)
    fun setEnabled(context: Context, enabled: Boolean) { context.getSharedPreferences(PREFS, 0).edit().putBoolean(ENABLED, enabled).apply() }
    fun getRules(context: Context): List<Rule> {
        val raw = context.getSharedPreferences(PREFS, 0).getString(RULES, "[]") ?: "[]"
        return runCatching { val a = JSONArray(raw); buildList(a.length()) { for (i in 0 until a.length()) { val o = a.getJSONObject(i); add(Rule(o.optLong("id"), o.optString("name"), o.optString("sender"), o.optString("containsText"), runCatching { Action.valueOf(o.optString("action", Action.MARK_READ.name)) }.getOrDefault(Action.MARK_READ), o.optBoolean("enabled", true))) } } }.getOrDefault(emptyList())
    }
    fun addRule(context: Context, rule: Rule) { saveRules(context, getRules(context).filterNot { it.id == rule.id } + rule) }
    fun removeRule(context: Context, id: Long) { saveRules(context, getRules(context).filterNot { it.id == id }) }
    fun setRuleEnabled(context: Context, id: Long, enabled: Boolean) { saveRules(context, getRules(context).map { if (it.id == id) it.copy(enabled = enabled) else it }) }
    fun processIncomingSms(context: Context, sender: String, body: String, subscriptionId: Int) {
        if (!isEnabled(context) || sender.isBlank() || body.isBlank()) return
        getRules(context).filter { it.enabled && matches(it, sender, body) }.forEach { applyAction(context, it.action, sender, body) }
    }
    private fun matches(rule: Rule, sender: String, body: String) = (rule.sender.isBlank() || numbersMatch(rule.sender, sender)) && (rule.containsText.isBlank() || body.contains(rule.containsText, true))
    private fun numbersMatch(expected: String, actual: String): Boolean { val a = digitsOnly(expected); val b = digitsOnly(actual); return a.isNotEmpty() && b.isNotEmpty() && (a == b || (a.length >= 8 && b.endsWith(a)) || (b.length >= 8 && a.endsWith(b))) }
    private fun digitsOnly(value: String): String = value.map { when (it) { in '۰'..'۹' -> ('0'.code + it.code - '۰'.code).toChar(); in '٠'..'٩' -> ('0'.code + it.code - '٠'.code).toChar(); else -> it } }.filter(Char::isDigit).joinToString("")
    private fun applyAction(context: Context, action: Action, sender: String, body: String) {
        runCatching {
            when (action) {
                Action.MARK_READ -> context.contentResolver.update(Telephony.Sms.CONTENT_URI, ContentValues().apply { put("read", 1) }, "address = ? AND body = ?", arrayOf(sender, body))
                Action.DELETE -> context.contentResolver.delete(Telephony.Sms.CONTENT_URI, "address = ? AND body = ?", arrayOf(sender, body))
            }
            DebugLog.write(context, "SMS_AUTOMATION action=$action sender=$sender")
        }.onFailure { DebugLog.write(context, "SMS_AUTOMATION_FAILED ${it.javaClass.simpleName}: ${it.message}") }
    }
    private fun saveRules(context: Context, rules: List<Rule>) {
        val a = JSONArray(); rules.forEach { a.put(JSONObject().apply { put("id", it.id); put("name", it.name); put("sender", it.sender); put("containsText", it.containsText); put("action", it.action.name); put("enabled", it.enabled) }) }
        context.getSharedPreferences(PREFS, 0).edit().putString(RULES, a.toString()).apply()
    }
}
