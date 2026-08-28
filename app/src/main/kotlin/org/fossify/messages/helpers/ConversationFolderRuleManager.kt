package org.fossify.messages.helpers

import android.content.Context
import org.fossify.messages.models.Conversation
import org.json.JSONArray
import org.json.JSONObject

/** Automatic routing rules for conversation folders. */
object ConversationFolderRuleManager {
    enum class MatchMode { ONE, ALL }
    enum class MatchField { MESSAGE, TITLE, PHONE }

    data class Rule(
        val id: String,
        var folderId: String,
        var keywords: List<String>,
        var mode: MatchMode = MatchMode.ONE,
        var fields: Set<MatchField> = setOf(MatchField.MESSAGE),
        var enabled: Boolean = true,
    )

    private const val PREFS = "conversation_folder_rules"
    private const val RULES = "rules"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getRules(context: Context): MutableList<Rule> {
        val raw = prefs(context).getString(RULES, null) ?: return mutableListOf()
        return try {
            val array = JSONArray(raw)
            MutableList(array.length()) { index ->
                val item = array.getJSONObject(index)
                val mode = runCatching { MatchMode.valueOf(item.optString("mode", MatchMode.ONE.name)) }
                    .getOrDefault(MatchMode.ONE)
                val fields = buildSet {
                    val values = item.optJSONArray("fields")
                    if (values != null) {
                        for (i in 0 until values.length()) {
                            runCatching { MatchField.valueOf(values.optString(i)) }.getOrNull()?.let(::add)
                        }
                    }
                    if (isEmpty()) add(MatchField.MESSAGE)
                }
                val keywords = buildList {
                    val values = item.optJSONArray("keywords")
                    if (values != null) {
                        for (i in 0 until values.length()) {
                            values.optString(i).trim().takeIf { it.isNotEmpty() }?.let(::add)
                        }
                    }
                }
                Rule(
                    id = item.getString("id"),
                    folderId = item.getString("folderId"),
                    keywords = keywords,
                    mode = mode,
                    fields = fields,
                    enabled = item.optBoolean("enabled", true),
                )
            }
        } catch (_: Exception) {
            mutableListOf()
        }
    }

    fun saveRules(context: Context, rules: List<Rule>) {
        val array = JSONArray()
        rules.forEach { rule ->
            array.put(JSONObject().apply {
                put("id", rule.id)
                put("folderId", rule.folderId)
                put("keywords", JSONArray(rule.keywords))
                put("mode", rule.mode.name)
                put("fields", JSONArray(rule.fields.map { it.name }))
                put("enabled", rule.enabled)
            })
        }
        prefs(context).edit().putString(RULES, array.toString()).apply()
    }

    fun removeRulesForFolder(context: Context, folderId: String) {
        saveRules(context, getRules(context).filterNot { it.folderId == folderId })
    }

    /**
     * Rebuilds only the automatic part of folder membership. Manual assignments remain intact.
     * Matching is deliberately Persian-friendly: Arabic/Persian letters, digits and spacing are normalized.
     */
    fun applyAutomaticRules(context: Context, conversations: List<Conversation>) {
        val rules = getRules(context).filter { it.enabled && it.keywords.isNotEmpty() }
        val validFolderIds = ConversationFolderManager.getFolders(context).map { it.id }.toSet()
        val autoByThread = HashMap<Long, MutableSet<String>>()

        conversations.forEach { conversation ->
            val memberships = mutableSetOf<String>()
            rules.forEach { rule ->
                if (rule.folderId in validFolderIds && matches(conversation, rule)) {
                    memberships.add(rule.folderId)
                }
            }
            autoByThread[conversation.threadId] = memberships
        }

        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val root = readAutoMembers(context)
        autoByThread.forEach { (threadId, folderIds) ->
            if (folderIds.isEmpty()) root.remove(threadId.toString())
            else root.put(threadId.toString(), JSONArray(folderIds.toList()))
        }
        prefs.edit().putString(AUTO_MEMBERS, root.toString()).apply()
    }

    fun getAutomaticMembership(context: Context, threadId: Long): Set<String> {
        val array = readAutoMembers(context).optJSONArray(threadId.toString()) ?: return emptySet()
        return buildSet { for (i in 0 until array.length()) add(array.optString(i)) }
    }

    private fun matches(conversation: Conversation, rule: Rule): Boolean {
        val texts = rule.fields.map { field ->
            when (field) {
                MatchField.MESSAGE -> conversation.snippet
                MatchField.TITLE -> conversation.title
                MatchField.PHONE -> conversation.phoneNumber
            }
        }.map(::normalize)

        val normalizedKeywords = rule.keywords.map(::normalize).filter { it.isNotEmpty() }
        if (normalizedKeywords.isEmpty()) return false

        return when (rule.mode) {
            MatchMode.ONE -> normalizedKeywords.any { keyword -> texts.any { it.contains(keyword) } }
            MatchMode.ALL -> normalizedKeywords.all { keyword -> texts.any { it.contains(keyword) } }
        }
    }

    private fun normalize(value: String): String = value
        .replace('ي', 'ی')
        .replace('ى', 'ی')
        .replace('ك', 'ک')
        .replace('\u200c', ' ')
        .replace('\u200f', ' ')
        .replace('\u200e', ' ')
        .replace(Regex("[\u064B-\u065F\u0670]"), "")
        .replace('۰', '0').replace('۱', '1').replace('۲', '2').replace('۳', '3').replace('۴', '4')
        .replace('۵', '5').replace('۶', '6').replace('۷', '7').replace('۸', '8').replace('۹', '9')
        .lowercase()
        .replace(Regex("\\s+"), " ")
        .trim()

    private const val AUTO_MEMBERS = "automatic_members"

    private fun readAutoMembers(context: Context): JSONObject {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(AUTO_MEMBERS, null)
        return try {
            if (raw.isNullOrEmpty()) JSONObject() else JSONObject(raw)
        } catch (_: Exception) {
            JSONObject()
        }
    }
}
