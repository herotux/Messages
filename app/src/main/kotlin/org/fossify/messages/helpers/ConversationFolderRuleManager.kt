package org.fossify.messages.helpers
import android.content.Context
import org.fossify.messages.models.Conversation
import org.json.JSONArray
import org.json.JSONObject
object ConversationFolderRuleManager {
 enum class MatchMode{ONE,ALL}; enum class MatchField{MESSAGE,TITLE,PHONE}
 data class Rule(val id:String,var folderId:String,var keywords:List<String>,var mode:MatchMode=MatchMode.ONE,var fields:Set<MatchField>=setOf(MatchField.MESSAGE),var enabled:Boolean=true)
 private const val PREFS="conversation_folder_rules";private const val RULES="rules";private const val AUTO_MEMBERS="automatic_members"
 private fun prefs(c:Context)=c.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
 fun getRules(c:Context):MutableList<Rule>{val raw=prefs(c).getString(RULES,null)?:return mutableListOf();return try{val a=JSONArray(raw);MutableList(a.length()){i->val o=a.getJSONObject(i);Rule(o.getString("id"),o.getString("folderId"),buildList{val x=o.optJSONArray("keywords");if(x!=null)for(j in 0 until x.length())x.optString(j).trim().takeIf{it.isNotEmpty()}?.let(::add)},runCatching{MatchMode.valueOf(o.optString("mode",MatchMode.ONE.name))}.getOrDefault(MatchMode.ONE),setOf(MatchField.MESSAGE),o.optBoolean("enabled",true))}}catch(_:Exception){mutableListOf()}}
 fun saveRules(c:Context,rules:List<Rule>){val a=JSONArray();rules.forEach{r->a.put(JSONObject().apply{put("id",r.id);put("folderId",r.folderId);put("keywords",JSONArray(r.keywords));put("mode",r.mode.name);put("fields",JSONArray(r.fields.map{it.name}));put("enabled",r.enabled)})};prefs(c).edit().putString(RULES,a.toString()).apply()}
 fun removeRulesForFolder(c:Context,id:String)=saveRules(c,getRules(c).filterNot{it.folderId==id})
 fun getAutomaticMembership(c:Context,threadId:Long):Set<String>{val a=readAuto(c).optJSONArray(threadId.toString())?:return emptySet();return buildSet{for(i in 0 until a.length())add(a.optString(i))}}
 fun applyAutomaticRules(c:Context,conversations:List<Conversation>){val rules=getRules(c).filter{it.enabled&&it.keywords.isNotEmpty()};val root=readAuto(c);conversations.forEach{conversation->val ids=rules.filter{it.folderId in ConversationFolderManager.getFolders(c).map{f->f.id}&&matches(conversation,it)}.map{it.folderId};if(ids.isEmpty())root.remove(conversation.threadId.toString())else root.put(conversation.threadId.toString(),JSONArray(ids))};prefs(c).edit().putString(AUTO_MEMBERS,root.toString()).apply()}
 private fun matches(c:Conversation,r:Rule):Boolean{val texts=listOf(c.snippet,c.title,c.phoneNumber).map(::normalize);val keys=r.keywords.map(::normalize);return if(r.mode==MatchMode.ONE)keys.any{k->texts.any{it.contains(k)}}else keys.all{k->texts.any{it.contains(k)}}}
 private fun normalize(v:String)=v.replace('ي','ی').replace('ى','ی').replace('ك','ک').replace('۰','0').replace('۱','1').replace('۲','2').replace('۳','3').replace('۴','4').replace('۵','5').replace('۶','6').replace('۷','7').replace('۸','8').replace('۹','9').lowercase().trim()
 private fun readAuto(c:Context)=try{JSONObject(prefs(c).getString(AUTO_MEMBERS,null)? : "{}")}catch(_:Exception){JSONObject()}
}
