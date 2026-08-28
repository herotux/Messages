package org.fossify.messages.views

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import org.fossify.commons.extensions.getContrastColor
import org.fossify.commons.extensions.getProperPrimaryColor
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
        if (force || selected != lastSelected) {
            lastSelected = selected
            // Folder color is intentionally NOT applied to the header/background.
            // It is used only for the corresponding tab text.
            styleTabs(selected)
        }

        val reorder = privateBool("reorderMode")
        if (force || reorder != lastReorder) {
            lastReorder = reorder
            wiggle(reorder)
        }
        if (reorder) installDrag()

        val now = android.os.SystemClock.uptimeMillis()
        if (force || now - lastLabelsAt > 250L) {
            lastLabelsAt = now
            updateLabels()
        }
    }

    /** Material-like tabs: transparent surface, themed indicator, folder color only on text. */
    private fun styleTabs(selected: String) {
        val tabs = privateField("tabs") as? LinearLayout ?: return
        val folders = ConversationFolderManager.getFolders(context).associateBy { it.id }
        val indicatorColor = context.getProperPrimaryColor()

        for (i in 0 until tabs.childCount) {
            val v = tabs.getChildAt(i) as? TextView ?: continue
            val id = v.tag as? String
            val f = id?.let { folders[it] }
            val isFolderTab = f != null
            val active = id == selected

            if (!isFolderTab) {
                v.setTextColor(indicatorColor)
                v.typeface = android.graphics.Typeface.DEFAULT
                v.background = null
                v.setCompoundDrawables(null, null, null, null)
                continue
            }

            // The folder color is used ONLY for the tab label text.
            v.setTextColor(f.color)
            v.typeface = if (active) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
            v.alpha = if (active) 1f else 0.82f
            v.setPadding(dp(16), dp(2), dp(16), dp(2))
            v.background = null

            if (active) {
                val indicator = ColorDrawable(indicatorColor).apply {
                    setBounds(0, 0, dp(28), dp(3))
                }
                v.setCompoundDrawables(null, null, null, indicator)
                v.compoundDrawablePadding = dp(3)
            } else {
                v.setCompoundDrawables(null, null, null, null)
            }
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

    /** Telegram-style folder labels. The RecyclerView is shared by the main list and folder-filtered list. */
    private fun updateLabels() {
        val rv=rootView.findViewById<RecyclerView>(R.id.conversations_list) ?: return
        val folders=ConversationFolderManager.getFolders(context).filter{!it.system&&it.enabled}.associateBy{it.id}
        for(i in 0 until rv.childCount){
            val item=rv.getChildAt(i) as? ConstraintLayout ?: continue
            val holder=rv.getChildViewHolder(item); val thread=holder.itemId; if(thread==RecyclerView.NO_ID) continue
            val memberships=ConversationFolderManager.getFolderMembership(context,thread).mapNotNull{folders[it]}
            var box=item.findViewWithTag<View>("folder_labels") as? LinearLayout
            if(box==null){
                box=LinearLayout(context).apply{tag="folder_labels";orientation=LinearLayout.HORIZONTAL;gravity=android.view.Gravity.START;clipToPadding=false}
                item.addView(box,ConstraintLayout.LayoutParams(0,dp(23)).apply{startToStart=ConstraintLayout.LayoutParams.PARENT_ID;endToEnd=ConstraintLayout.LayoutParams.PARENT_ID;bottomToBottom=ConstraintLayout.LayoutParams.PARENT_ID;bottomMargin=dp(1);marginStart=dp(56);marginEnd=dp(56)})
                item.findViewById<View>(R.id.conversation_body_short)?.let { body ->
                    (body.layoutParams as? ConstraintLayout.LayoutParams)?.let { lp -> lp.bottomMargin = dp(27); body.layoutParams = lp }
                }
            }
            box.removeAllViews()
            box.visibility=if(memberships.isEmpty())View.GONE else View.VISIBLE
            memberships.take(4).forEach{f->
                box.addView(TextView(context).apply{
                    text=f.name
                    textSize=10f
                    maxLines=1
                    gravity=android.view.Gravity.CENTER
                    setTextColor(f.color.getContrastColor())
                    setPadding(dp(7),0,dp(7),0)
                    background=GradientDrawable().apply{cornerRadius=dp(9).toFloat();setColor(f.color)}
                    layoutParams=LinearLayout.LayoutParams(-2,dp(20)).apply{marginEnd=dp(5)}
                })
            }
        }
    }

    private fun privateField(n:String):Any?=runCatching{ConversationFolderTabsView::class.java.getDeclaredField(n).apply{isAccessible=true}.get(this)}.getOrNull()
    private fun privateBool(n:String)=privateField(n) as? Boolean ?: false
    private fun invokePrivate(n:String){runCatching{ConversationFolderTabsView::class.java.getDeclaredMethod(n).apply{isAccessible=true}.invoke(this)}}
    private fun dp(v:Int)=(v*resources.displayMetrics.density).roundToInt()
}
