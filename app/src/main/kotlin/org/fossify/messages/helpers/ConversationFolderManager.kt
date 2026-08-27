package org.fossify.messages.helpers

import android.content.Context
import org.fossify.messages.models.Conversation
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persists user-defined conversation folders independently from the SMS database.
 * Folder membership is an app-level relationship keyed by stable thread id.
 */
object ConversationFolderManager {
    data class Folder(
        val id: String,
        var name: String,
        var enabled: Boolean = true,
        val system: Boolean = false,
    )

    private const val PREFS = "conversation_folder_settings"
    private const val FOLDERS = "folders"
    private const val MEMBERS = "members"
    private const val SELECTED = "selected_folder"

    const val ALL_ID = "__all__"
    const val UNREAD_ID = "__unread__"
    const val BANKS_ID = "__banks__"
    const val PERSONAL_ID = "__personal__"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getFolders(context: Context): MutableList<Folder> {
        val raw = prefs(context).getString(FOLDERS, null)
        if (raw.isNullOrEmpty()) return defaultFolders()
        return try {
            val array = JSONArray(raw)
            MutableList(array.length()) { index ->
                val item = array.getJSONObject(index)
                Folder(
                    id = item.getString("id"),
                    name = item.getString("name"),
                    enabled = item.optBoolean("enabled", true),
                    system = item.optBoolean("system", false),
                )
            }
        } catch (_: Exception) {
            defaultFolders()
        }
    }

    fun saveFolders(context: Context, folders: List<Folder>) {
        val array = JSONArray()
        folders.forEach { folder ->
            array.put(JSONObject().apply {
                put("id", folder.id)
                put("name", folder.name)
                put("enabled", folder.enabled)
                put("system", folder.system)
            })
        }
        prefs(context).edit().putString(FOLDERS, array.toString()).apply()
    }

    fun getEnabledFolders(context: Context): List<Folder> = getFolders(context).filter { it.enabled }

    fun getSelectedFolderId(context: Context): String =
        prefs(context).getString(SELECTED, ALL_ID) ?: ALL_ID

    fun setSelectedFolderId(context: Context, id: String) {
        prefs(context).edit().putString(SELECTED, id).apply()
    }

    /** Returns the union of manual and automatic folder membership. */
    fun getFolderMembership(context: Context, threadId: Long): Set<String> =
        getManualMembership(context, threadId) + ConversationFolderRuleManager.getAutomaticMembership(context, threadId)

    fun getManualMembership(context: Context, threadId: Long): Set<String> {
        val root = readMembers(context)
        val array = root.optJSONArray(threadId.toString()) ?: return emptySet()
        return buildSet {
            for (i in 0 until array.length()) add(array.optString(i))
        }
    }

    fun setFolderMembership(context: Context, threadId: Long, folderIds: Set<String>) {
        val root = readMembers(context)
        if (folderIds.isEmpty()) root.remove(threadId.toString())
        else root.put(threadId.toString(), JSONArray(folderIds.toList()))
        prefs(context).edit().putString(MEMBERS, root.toString()).apply()
    }

    fun conversationsInFolder(
        context: Context,
        conversations: List<Conversation>,
        folderId: String,
        isBank: (Conversation) -> Boolean,
    ): List<Conversation> = when (folderId) {
        ALL_ID -> conversations
        UNREAD_ID -> conversations.filter { !it.read }
        BANKS_ID -> conversations.filter(isBank)
        PERSONAL_ID -> conversations.filter { !it.isGroupConversation && !isBank(it) }
        else -> conversations.filter { folderId in getFolderMembership(context, it.threadId) }
    }

    fun removeFolder(context: Context, folderId: String) {
        val folders = getFolders(context)
        if (folders.none { it.id == folderId && !it.system }) return
        saveFolders(context, folders.filterNot { it.id == folderId })

        val root = readMembers(context)
        val keys = root.keys()
        val updated = JSONObject()
        while (keys.hasNext()) {
            val key = keys.next()
            val ids = root.optJSONArray(key) ?: continue
            val remaining = buildList {
                for (i in 0 until ids.length()) {
                    val id = ids.optString(i)
                    if (id != folderId) add(id)
                }
            }
            if (remaining.isNotEmpty()) updated.put(key, JSONArray(remaining))
        }
        prefs(context).edit().putString(MEMBERS, updated.toString()).apply()
        ConversationFolderRuleManager.removeRulesForFolder(context, folderId)
        if (getSelectedFolderId(context) == folderId) setSelectedFolderId(context, ALL_ID)
    }

    fun cleanupMembership(context: Context, validThreadIds: Set<Long>) {
        val root = readMembers(context)
        val updated = JSONObject()
        val keys = root.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val threadId = key.toLongOrNull()
            if (threadId != null && threadId in validThreadIds) {
                root.optJSONArray(key)?.let { updated.put(key, it) }
            }
        }
        prefs(context).edit().putString(MEMBERS, updated.toString()).apply()
    }

    private fun defaultFolders() = mutableListOf(
        Folder(ALL_ID, "همه", true, true),
        Folder(UNREAD_ID, "خوانده‌نشده", true, true),
        Folder(BANKS_ID, "بانک‌ها", true, true),
        Folder(PERSONAL_ID, "شخصی", true, true),
    )

    private fun readMembers(context: Context): JSONObject {
        val raw = prefs(context).getString(MEMBERS, null)
        return try {
            if (raw.isNullOrEmpty()) JSONObject() else JSONObject(raw)
        } catch (_: Exception) {
            JSONObject()
        }
    }
}
