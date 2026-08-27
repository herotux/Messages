package org.fossify.messages.views

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import org.fossify.commons.extensions.getContrastColor
import org.fossify.commons.extensions.getProperBackgroundColor
import org.fossify.messages.R
import org.fossify.messages.helpers.ConversationFolderManager
import kotlin.math.abs
import kotlin.math.roundToInt

class FixedConversationFolderTabsView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : ConversationFolderTabsView(context, attrs) {
    private var lastSelected: String? = null
    private var lastReorder = false
    private var lastLabelsAt = 0L

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        post { sync(true) }
    }

    override fun dispatchDraw(canvas: android.graphics.Canvas) {
        sync()
        super.dispatchDraw(canvas)
    }

    private fun sync(force: Boolean = false) {
        val selected = ConversationFolderManager.getSelectedFolderId(context)
        val folder = ConversationFolderManager.getFolders(context).firstOrNull { it.id == selected }
        if (force || selected != lastSelected) {
            lastSelected = selected
            val color = folder?.color ?: context.getProperBackgroundColor()
            rootView.findViewById<View>(R.id.main_menu)?.apply {
                setBackgroundColor(color)
                tintText(this, color.getContrastColor())
            }
            setBackgroundColor(color)
            styleTabs(selected)
        }
        val reorder = privateBool("reorderMode")
        if (force || reorder != lastReorder) {
            lastReorder = reorder
            wiggle(reorder)
        }
        if (reorder) installDrag()
        val now = android.os.SystemClock.uptimeMillis()
        if (force || now - lastLabelsAt > 350L) {
            lastLabelsAt = now
            updateLabels()
        }
    }

    private fun styleTabs(selected: String) {
        val tabs = privateField("tabs") as? LinearLayout ?: return
        val folders = ConversationFolderManager.getFolders(context).associateBy { it.id }
        for (i in 0 until tabs.childCount) {
            val v = tabs.getChildAt(i) as? TextView ?: continue
            val id = v.tag as? String ?: continue
            val f = folders[id] ?: continue
            val active = id == selected
            v.background = GradientDrawable().apply {
                cornerRadius = dp(17).toFloat()
                setColor(if (active) f.color else Color.TRANSPARENT)
                setStroke(dp(1), if (active) f.color.getContrastColor() else withAlpha(f.color, .35f))
            }
            v.setTextColor(if (active) f.color.getContrastColor() else f.color)
            v.alpha = if (active) 1f else .9f
            v.setPadding(dp(16), dp(5), dp(16), dp(5))
        }
    }

    private fun wiggle(enabled: Boolean) {
        val tabs = privateField("tabs") as? LinearLayout ?: return
        val ids = ConversationFolderManager.getFolders(context).map { it.id }.toSet()
        for (i in 0 until tabs.childCount) {
            val v = tabs.getChildAt(i)
            if ((v.tag as? String) !in ids) continue
            val old = v.getTag(R.id.folder_tabs) as? ObjectAnimator
            if (!enabled) {
                old?.cancel(); v.setTag(R.id.folder_tabs, null)
                v.animate().rotation(0f).scaleX(1f).scaleY(1f).setDuration(120).start()
            } else if (old == null) {
                val a = ObjectAnimator.ofFloat(v, View.ROTATION, -1.7f, 1.7f).apply {
                    duration = 105; repeatMode = ValueAnimator.REVERSE; repeatCount = ValueAnimator.INFINITE
                }
                v.setTag(R.id.folder_tabs, a); a.start()
                v.animate().scaleX(1.04f).scaleY(1.04f).setDuration(100).start()
            }
        }
    }

    private fun installDrag() {
        val tabs = privateField("tabs") as? LinearLayout ?: return
        val ids = ConversationFolderManager.getFolders(context).map { it.id }.toSet()
        for (i in 0 until tabs.childCount) {
            val v = tabs.getChildAt(i)
            if ((v.tag as? String) !in ids || v.getTag(R.id.main_menu) == true) continue
            v.setTag(R.id.main_menu, true)
            var down = 0f; var moved = false
            v.setOnTouchListener { view, e ->
                if (!privateBool("reorderMode")) return@setOnTouchListener false
                when (e.actionMasked) {
                    MotionEvent.ACTION_DOWN -> { down = e.rawX; moved = false; parent?.requestDisallowInterceptTouchEvent(true); true }
                    MotionEvent.ACTION_MOVE -> { parent?.requestDisallowInterceptTouchEvent(true); val dx=e.rawX-down; if(abs(dx)>dp(5)){ moved=true; view.translationX=dx; reorderByPosition(view) }; true }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { parent?.requestDisallowInterceptTouchEvent(false); view.animate().translationX(0f).setDuration(100).start(); if(moved) invokePrivate("saveOrder"); true }
                    else -> false
                }
            }
        }
    }

    private fun reorderByPosition(view: View) {
        val tabs = privateField("tabs") as? LinearLayout ?: return
        val ids = ConversationFolderManager.getFolders(context).map { it.id }.toSet()
        val items = (0 until tabs.childCount).map { tabs.getChildAt(it) }.filter { (it.tag as? String) in ids }.sortedBy { it.left }
        val from = items.indexOf(view); if(from<0) return
        val center=view.left+view.translationX+view.width/2f
        var target=from
        for(i in items.indices) if(i!=from && center < items[i].left+items[i].width/2f){ target=i; break }
        if(target==from && center > items.last().left+items.last().width/2f) target=items.lastIndex
        if(target==from) return
        val targetView=items[target]; val a=tabs.indexOfChild(view); val b=tabs.indexOfChild(targetView)
        if(a<0||b<0||a==b) return
        tabs.removeViewAt(a); tabs.addView(view,b); view.translationX=0f
        view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
    }

    private fun updateLabels() {
        val rv=rootView.findViewById<RecyclerView>(R.id.conversations_list) ?: return
        val folders=ConversationFolderManager.getFolders(context).filter{!it.system&&it.enabled}.associateBy{it.id}
        for(i in 0 until rv.childCount){
            val item=rv.getChildAt(i) as? ConstraintLayout ?: continue
            val holder=rv.getChildViewHolder(item); val thread=holder.itemId; if(thread==RecyclerView.NO_ID) continue
            val memberships=ConversationFolderManager.getFolderMembership(context,thread).mapNotNull{folders[it]}
            var box=item.findViewWithTag<View>("folder_labels") as? LinearLayout
            if(box==null){
                box=LinearLayout(context).apply{tag="folder_labels";orientation=LinearLayout.HORIZONTAL;gravity=android.view.Gravity.START}
                item.addView(box,ConstraintLayout.LayoutParams(0,dp(23)).apply{startToStart=ConstraintLayout.LayoutParams.PARENT_ID;endToEnd=ConstraintLayout.LayoutParams.PARENT_ID;bottomToBottom=ConstraintLayout.LayoutParams.PARENT_ID;bottomMargin=dp(2);marginStart=dp(56);marginEnd=dp(56)})
                item.findViewById<View>(R.id.conversation_body_short)?.let { body ->
                    (body.layoutParams as? ConstraintLayout.LayoutParams)?.let { lp -> lp.bottomMargin = dp(27); body.layoutParams = lp }
                }
            }
            box.removeAllViews(); box.visibility=if(memberships.isEmpty())View.GONE else View.VISIBLE
            memberships.take(4).forEach{f->box.addView(TextView(context).apply{text=f.name;textSize=10f;maxLines=1;gravity=android.view.Gravity.CENTER;setTextColor(f.color.getContrastColor());setPadding(dp(7),0,dp(7),0);background=GradientDrawable().apply{cornerRadius=dp(9).toFloat();setColor(f.color)};layoutParams=LinearLayout.LayoutParams(-2,dp(20)).apply{marginEnd=dp(5)}})}
        }
    }

    private fun tintText(v:View,c:Int){if(v is TextView)v.setTextColor(c);if(v is android.view.ViewGroup)for(i in 0 until v.childCount)tintText(v.getChildAt(i),c)}
    private fun privateField(n:String):Any?=runCatching{ConversationFolderTabsView::class.java.getDeclaredField(n).apply{isAccessible=true}.get(this)}.getOrNull()
    private fun privateBool(n:String)=privateField(n) as? Boolean ?: false
    private fun invokePrivate(n:String){runCatching{ConversationFolderTabsView::class.java.getDeclaredMethod(n).apply{isAccessible=true}.invoke(this)}}
    private fun withAlpha(c:Int,a:Float)=Color.argb((255*a).toInt(),Color.red(c),Color.green(c),Color.blue(c))
    private fun dp(v:Int)=(v*resources.displayMetrics.density).roundToInt()
}
