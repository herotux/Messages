package org.fossify.messages.views
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.fossify.messages.R
import org.fossify.messages.adapters.BaseConversationsAdapter
import org.fossify.messages.helpers.ConversationFolderManager
class FixedConversationFolderTabsView @JvmOverloads constructor(context:Context,attrs:AttributeSet?=null):HorizontalScrollView(context,attrs){
 private val row=LinearLayout(context).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;layoutDirection=LAYOUT_DIRECTION_RTL};private var selected=ConversationFolderManager.getSelectedFolderId(context)
 init{isHorizontalScrollBarEnabled=false;overScrollMode=OVER_SCROLL_NEVER;addView(row,LayoutParams(-2,dp(48)));rebuild()}
 override fun onAttachedToWindow(){super.onAttachedToWindow();post{rebuild()}}
 private fun rebuild(){row.removeAllViews();ConversationFolderManager.getFolders(context).filter{it.enabled}.forEach{f->row.addView(TextView(context).apply{text=f.name;tag=f.id;gravity=Gravity.CENTER;isSingleLine=true;textSize=14f;setPadding(dp(16),0,dp(16),0);setOnClickListener{select(f.id)};style(this,f.id==selected,f.color)},LinearLayout.LayoutParams(-2,-1))}}
 private fun select(id:String){selected=id;ConversationFolderManager.setSelectedFolderId(context,id);val a=rootView.findViewById<RecyclerView>(R.id.conversations_list)?.adapter as? BaseConversationsAdapter;if(a!=null)when(id){ConversationFolderManager.ALL_ID->a.clearConversationFilter();ConversationFolderManager.UNREAD_ID->a.filterConversations{!it.read};ConversationFolderManager.BANKS_ID->a.filterConversations{a.isBankConversation(it)};ConversationFolderManager.PERSONAL_ID->a.filterConversations{!it.isGroupConversation&&!a.isBankConversation(it)};else->a.filterConversations{id in ConversationFolderManager.getFolderMembership(context,it.threadId)}};rebuild()}
 private fun style(v:TextView,active:Boolean,c:Int){v.setTextColor(if(active)Color.WHITE else c);v.background=GradientDrawable().apply{cornerRadius=dp(18).toFloat();setColor(if(active)c else Color.TRANSPARENT);setStroke(dp(1),c)}}
 private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
}