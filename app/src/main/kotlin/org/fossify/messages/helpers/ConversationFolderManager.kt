package org.fossify.messages.helpers

import android.content.Context
import org.fossify.messages.models.Conversation
import org.json.JSONArray
import org.json.JSONObject

object ConversationFolderManager {
    data class Folder(
        val id: String,
        var name: String,
        var enabled: Boolean = true,
        val system: Boolean = false,
        var color: Int = DEFAULT_COLOR,
    )

    private const val PREFS = "conversation_folder_settings"
    private const val FOLDERS = "folders"
    private const val MEMBERS = "members"
    private const val EXCLUDED = "excluded_members"
    private const val SELECTED = "selected_folder"
    private const val FOLDERS_VISIBLE = "folders_visible"
    const val ALL_ID = "__all__"
    const val UNREAD_ID = "__unread__"
    const val BANKS_ID = "__banks__"
    const val PERSONAL_ID = "__personal__"
    const val DEFAULT_COLOR = 0xff607d8b.toInt()

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun areFoldersVisible(context: Context): Boolean = prefs(context).getBoolean(FOLDERS_VISIBLE, true)

    fun setFoldersVisible(context: Context, visible: Boolean) {
        prefs(context).edit().putBoolean(FOLDERS_VISIBLE, visible).apply()
        if (!visible) setSelectedFolderId(context, ALL_ID)
    }

    fun getFolders(context: Context): MutableList<Folder> {
        val raw = prefs(context).getString(FOLDERS, null)
        if (raw.isNullOrEmpty()) return defaultFolders()
        return try {
            val array = JSONArray(raw)
            MutableList(array.length()) { index ->
                val item = array.getJSONObject(index)
                Folder(item.getString("id"), item.getString("name"), item.optBoolean("enabled", true), item.optBoolean("system", false), item.optInt("color", colorForIndex(index)))
            }
        } catch (_: Exception) { defaultFolders() }
    }

    fun saveFolders(context: Context, folders: List<Folder>) {
        val array = JSONArray()
        folders.forEach { folder -> array.put(JSONObject().apply { put("id", folder.id); put("name", folder.name); put("enabled", folder.enabled); put("system", folder.system); put("color", folder.color) }) }
        prefs(context).edit().putString(FOLDERS, array.toString()).apply()
    }

    fun getEnabledFolders(context: Context): List<Folder> = getFolders(context).filter { it.enabled }
    fun getSelectedFolderId(context: Context): String = prefs(context).getString(SELECTED, ALL_ID) ?: ALL_ID
    fun setSelectedFolderId(context: Context, id: String) = prefs(context).edit().putString(SELECTED, id).apply()
    fun getFolder(context: Context, id: String): Folder? = getFolders(context).firstOrNull { it.id == id }
    fun getSelectedFolder(context: Context): Folder? = getFolder(context, getSelectedFolderId(context))

    fun getFolderMembership(context: Context, threadId: Long): Set<String> =
        (getManualMembership(context, threadId) + ConversationFolderRuleManager.getAutomaticMembership(context, threadId)) - getExcludedMembership(context, threadId)

    fun getManualMembership(context: Context, threadId: Long): Set<String> {
        val array = readMembers(context).optJSONArray(threadId.toString()) ?: return emptySet()
        return buildSet { for (i in 0 until array.length()) add(array.optString(i)) }
    }

    fun getExcludedMembership(context: Context, threadId: Long): Set<String> {
        val array = readExcluded(context).optJSONArray(threadId.toString()) ?: return emptySet()
        return buildSet { for (i in 0 until array.length()) add(array.optString(i)) }
    }

    fun setFolderMembership(context: Context, threadId: Long, folderIds: Set<String>, managedFolderIds: Set<String> = folderIds) {
        val members = readMembers(context)
        val currentManual = getManualMembership(context, threadId)
        val updatedManual = (currentManual - managedFolderIds) + folderIds
        if (updatedManual.isEmpty()) members.remove(threadId.toString()) else members.put(threadId.toString(), JSONArray(updatedManual.toList()))
        val excluded = readExcluded(context)
        val currentAuto = ConversationFolderRuleManager.getAutomaticMembership(context, threadId)
        val currentExcluded = getExcludedMembership(context, threadId).toMutableSet()
        managedFolderIds.forEach { folderId -> if (folderId in currentAuto && folderId !in folderIds) currentExcluded.add(folderId) else if (folderId in folderIds) currentExcluded.remove(folderId) }
        if (currentExcluded.isEmpty()) excluded.remove(threadId.toString()) else excluded.put(threadId.toString(), JSONArray(currentExcluded.toList()))
        prefs(context).edit().putString(MEMBERS, members.toString()).putString(EXCLUDED, excluded.toString()).apply()
    }

    fun conversationsInFolder(context: Context, conversations: List<Conversation>, folderId: String, isBank: (Conversation) -> Boolean): List<Conversation> = when (folderId) {
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
        removeMembershipId(context, MEMBERS, folderId); removeMembershipId(context, EXCLUDED, folderId)
        ConversationFolderRuleManager.removeRulesForFolder(context, folderId)
        if (getSelectedFolderId(context) == folderId) setSelectedFolderId(context, ALL_ID)
    }

    private fun removeMembershipId(context: Context, key: String, folderId: String) {
        val root = readJson(context, key); val updated = JSONObject(); val keys = root.keys()
        while (keys.hasNext()) { val k = keys.next(); val ids = root.optJSONArray(k) ?: continue; val remaining = buildList { for (i in 0 until ids.length()) if (ids.optString(i) != folderId) add(ids.optString(i)) }; if (remaining.isNotEmpty()) updated.put(k, JSONArray(remaining)) }
        prefs(context).edit().putString(key, updated.toString()).apply()
    }

    fun cleanupMembership(context: Context, validThreadIds: Set<Long>) { cleanupJson(context, MEMBERS, validThreadIds); cleanupJson(context, EXCLUDED, validThreadIds) }
    private fun cleanupJson(context: Context, key: String, validThreadIds: Set<Long>) { val root = readJson(context, key); val updated = JSONObject(); val keys = root.keys(); while (keys.hasNext()) { val k = keys.next(); val id = k.toLongOrNull(); if (id != null && id in validThreadIds) root.optJSONArray(k)?.let { updated.put(k, it) } }; prefs(context).edit().putString(key, updated.toString()).apply() }
    private fun defaultFolders() = mutableListOf(Folder(ALL_ID, "همه", true, true, 0xff607d8b.toInt()), Folder(UNREAD_ID, "خوانده‌نشده", true, true, 0xffef6c00.toInt()), Folder(BANKS_ID, "بانک‌ها", true, true, 0xff2e7d32.toInt()), Folder(PERSONAL_ID, "شخصی", true, true, 0xff1565c0.toInt()))
    private fun colorForIndex(i: Int) = intArrayOf(0xff607d8b.toInt(), 0xffef6c00.toInt(), 0xff2e7d32.toInt(), 0xff1565c0.toInt(), 0xff8e24aa.toInt(), 0xffc62828.toInt(), 0xff00838f.toInt(), 0xff6d4c41.toInt())[i % 8]
    private fun readMembers(context: Context) = readJson(context, MEMBERS)
    private fun readExcluded(context: Context) = readJson(context, EXCLUDED)
    private fun readJson(context: Context, key: String): JSONObject { val raw = prefs(context).getString(key, null); return try { if (raw.isNullOrEmpty()) JSONObject() else JSONObject(raw) } catch (_: Exception) { JSONObject() } }
}
