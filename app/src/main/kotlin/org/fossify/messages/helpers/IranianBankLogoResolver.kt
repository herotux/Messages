package org.fossify.messages.views

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.fossify.commons.extensions.getProperBackgroundColor
import org.fossify.commons.extensions.getProperPrimaryColor
import org.fossify.messages.R
import org.fossify.messages.adapters.BaseConversationsAdapter
import org.fossify.messages.helpers.ConversationFolderManager

/** Folder tabs restored independently of the Commons migration. */
class ConversationFolderTabsView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : HorizontalScrollView(context, attrs) {
    private val tabs = LinearLayout(context).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER_VERTICAL; layoutDirection=LAYOUT_DIRECTION_RTL }
    private var adapter: BaseConversationsAdapter? = null
    private var selectedId = ConversationFolderManager.getSelectedFolderId(context)
    init { isHorizontalScrollBarEnabled=false; overScrollMode=OVER_SCROLL_NEVER; addView(tabs, LayoutParams(LayoutParams.WRAP_CONTENT, dp(48))); rebuild() }
    override fun onAttachedToWindow(){super.onAttachedToWindow();post{bindAdapter()}}
    private fun bindAdapter(){val rv=rootView.findViewById<RecyclerView>(R.id.conversations_list);val a=rv?.adapter as? BaseConversationsAdapter;if(a!=null){adapter=a;applyFilter()}else postDelayed({bindAdapter()},150)}
    fun refresh(){selectedId=ConversationFolderManager.getSelectedFolderId(context);rebuild();applyFilter()}
    private fun rebuild(){tabs.removeAllViews();ConversationFolderManager.getFolders(context).filter{it.enabled}.forEach{f->val v=TextView(context).apply{text=f.name;gravity=Gravity.CENTER;setPadding(dp(16),0,dp(16),0);textSize=14f;isSingleLine=true;tag=f.id;setOnClickListener{select(f.id)}};tabs.addView(v,LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.MATCH_PARENT))};updateSelection()}
    private fun select(id:String){selectedId=id;ConversationFolderManager.setSelectedFolderId(context,id);updateSelection();applyFilter()}
    private fun updateSelection(){val primary=context.getProperPrimaryColor();for(i in 0 until tabs.childCount){val v=tabs.getChildAt(i) as TextView;val id=v.tag as? String;val f=ConversationFolderManager.getFolder(context,id?:"");val active=id==selectedId;val c=f?.color?:primary;v.setTextColor(if(active)c else context.getProperBackgroundColor());v.typeface=if(active)Typeface.DEFAULT_BOLD else Typeface.DEFAULT;v.setBackgroundColor(if(active)Color.argb(38,Color.red(c),Color.green(c),Color.blue(c)) else Color.TRANSPARENT)}}
    private fun applyFilter(){val a=adapter?:return;when(selectedId){ConversationFolderManager.ALL_ID->a.clearConversationFilter();ConversationFolderManager.UNREAD_ID->a.filterConversations{!it.read};else->a.clearConversationFilter()}}
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
}
