package org.fossify.messages.helpers

import android.content.Context
import org.fossify.messages.models.Conversation
import org.json.JSONArray
import org.json.JSONObject

object ConversationFolderManager {
    data class Folder(val id: String, var name: String, var enabled: Boolean = true, val system: Boolean = false, var color: Int = DEFAULT_COLOR)
    private const val PREFS = "conversation_folder_settings"
    private const val FOLDERS = "folders"
    private const val MEMBERS = "members"
    private const val EXCLUDED = "excluded_members"
    private const val SELECTED = "selected_folder"
    const val ALL_ID = "__all__"
    const val UNREAD_ID = "__unread__"
    const val BANKS_ID = "__banks__"
    const val PERSONAL_ID = "__personal__"
    const val DEFAULT_COLOR = 0xff607d8b.toInt()
    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    fun getFolders(context: Context): MutableList<Folder> { val raw=prefs(context).getString(FOLDERS,null); if(raw.isNullOrEmpty()) return defaultFolders(); return try { val a=JSONArray(raw); MutableList(a.length()){i->val o=a.getJSONObject(i); Folder(o.getString("id"),o.getString("name"),o.optBoolean("enabled",true),o.optBoolean("system",false),o.optInt("color",colorForIndex(i))) } } catch(_:Exception){defaultFolders()} }
    fun saveFolders(context: Context, folders: List<Folder>) { val a=JSONArray(); folders.forEach{f->a.put(JSONObject().apply{put("id",f.id);put("name",f.name);put("enabled",f.enabled);put("system",f.system);put("color",f.color)})};prefs(context).edit().putString(FOLDERS,a.toString()).apply() }
    fun getEnabledFolders(context: Context)=getFolders(context).filter{it.enabled}
    fun getSelectedFolderId(context: Context)=prefs(context).getString(SELECTED,ALL_ID)?:ALL_ID
    fun setSelectedFolderId(context: Context,id:String)=prefs(context).edit().putString(SELECTED,id).apply()
    fun getFolder(context: Context,id:String)=getFolders(context).firstOrNull{it.id==id}
    fun getSelectedFolder(context: Context)=getFolder(context,getSelectedFolderId(context))
    fun getFolderMembership(context: Context,threadId:Long):Set<String>=(getManualMembership(context,threadId)+ConversationFolderRuleManager.getAutomaticMembership(context,threadId))-getExcludedMembership(context,threadId)
    fun getManualMembership(context: Context,threadId:Long):Set<String>{val a=readMembers(context).optJSONArray(threadId.toString())?:return emptySet();return buildSet{for(i in 0 until a.length())add(a.optString(i))}}
    fun getExcludedMembership(context: Context,threadId:Long):Set<String>{val a=readExcluded(context).optJSONArray(threadId.toString())?:return emptySet();return buildSet{for(i in 0 until a.length())add(a.optString(i))}}
    fun setFolderMembership(context: Context,threadId:Long,folderIds:Set<String>,managedFolderIds:Set<String>=folderIds){val m=readMembers(context);val current=getManualMembership(context,threadId);val updated=(current-managedFolderIds)+folderIds;if(updated.isEmpty())m.remove(threadId.toString())else m.put(threadId.toString(),JSONArray(updated.toList()));prefs(context).edit().putString(MEMBERS,m.toString()).apply()}
    fun conversationsInFolder(context: Context,conversations: List<Conversation>,folderId:String,isBank:(Conversation)->Boolean)=when(folderId){ALL_ID->conversations;UNREAD_ID->conversations.filter{!it.read};BANKS_ID->conversations.filter(isBank);PERSONAL_ID->conversations.filter{!it.isGroupConversation&&!isBank(it)};else->conversations.filter{folderId in getFolderMembership(context,it.threadId)}}
    fun removeFolder(context: Context,folderId:String){val f=getFolders(context);if(f.none{it.id==folderId&&!it.system})return;saveFolders(context,f.filterNot{it.id==folderId});if(getSelectedFolderId(context)==folderId)setSelectedFolderId(context,ALL_ID)}
    private fun defaultFolders()=mutableListOf(Folder(ALL_ID,"همه",true,true,0xff607d8b.toInt()),Folder(UNREAD_ID,"خوانده‌نشده",true,true,0xffef6c00.toInt()),Folder(BANKS_ID,"بانک‌ها",true,true,0xff2e7d32.toInt()),Folder(PERSONAL_ID,"شخصی",true,true,0xff1565c0.toInt()))
    private fun colorForIndex(i:Int)=intArrayOf(0xff607d8b.toInt(),0xffef6c00.toInt(),0xff2e7d32.toInt(),0xff1565c0.toInt(),0xff8e24aa.toInt(),0xffc62828.toInt())[i%6]
    private fun readMembers(c:Context)=readJson(c,MEMBERS);private fun readExcluded(c:Context)=readJson(c,EXCLUDED);private fun readJson(c:Context,k:String):JSONObject{val r=prefs(c).getString(k,null);return try{if(r.isNullOrEmpty())JSONObject()else JSONObject(r)}catch(_:Exception){JSONObject()}}
}
