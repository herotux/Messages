package org.fossify.messages.plugins

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object SmsTemplatesPlugin {
    private const val PREFS = "plugin_sms_templates"
    private const val KEY = "templates"

    data class Template(val id: Long, val name: String, val body: String, val category: String = "")

    fun isAvailable(context: Context) = PluginLicenseStore.isLicensed(context, PluginRegistry.SMS_TEMPLATES)

    fun list(context: Context): List<Template> {
        val raw = context.getSharedPreferences(PREFS, 0).getString(KEY, "[]") ?: "[]"
        return runCatching {
            val a = JSONArray(raw)
            buildList(a.length()) { for (i in 0 until a.length()) { val o = a.getJSONObject(i); add(Template(o.optLong("id"), o.optString("name"), o.optString("body"), o.optString("category"))) } }
        }.getOrDefault(emptyList())
    }

    fun save(context: Context, template: Template) {
        require(isAvailable(context)) { "SMS Templates is not licensed" }
        val values = list(context).filterNot { it.id == template.id } + template
        val a = JSONArray(); values.forEach { a.put(JSONObject().apply { put("id", it.id); put("name", it.name); put("body", it.body); put("category", it.category) }) }
        context.getSharedPreferences(PREFS, 0).edit().putString(KEY, a.toString()).apply()
    }

    fun delete(context: Context, id: Long) {
        require(isAvailable(context)) { "SMS Templates is not licensed" }
        saveAll(context, list(context).filterNot { it.id == id })
    }

    fun render(template: Template, variables: Map<String, String>): String = variables.entries.fold(template.body) { text, (key, value) -> text.replace("{$key}", value) }

    private fun saveAll(context: Context, values: List<Template>) {
        val a = JSONArray(); values.forEach { a.put(JSONObject().apply { put("id", it.id); put("name", it.name); put("body", it.body); put("category", it.category) }) }
        context.getSharedPreferences(PREFS, 0).edit().putString(KEY, a.toString()).apply()
    }
}
