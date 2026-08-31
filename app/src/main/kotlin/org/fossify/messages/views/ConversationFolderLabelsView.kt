package org.fossify.messages.views

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.fossify.commons.extensions.getContrastColor
import org.fossify.messages.helpers.ConversationFolderManager
import kotlin.math.roundToInt

/** Stable folder-label renderer owned by each conversation item. */
class ConversationFolderLabelsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private var lastThreadId = Long.MIN_VALUE
    private var lastMembershipKey = ""
    private var observedRecyclerView: RecyclerView? = null

    init {
        orientation = HORIZONTAL
        gravity = Gravity.START or Gravity.CENTER_VERTICAL
        clipToPadding = false
        clipChildren = false
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        visibility = View.VISIBLE
        post {
            attachObserver()
            refreshLabels()
        }
    }

    override fun onDetachedFromWindow() {
        observedRecyclerView?.adapter?.unregisterAdapterDataObserver(observer)
        observedRecyclerView = null
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: android.graphics.Canvas) {
        refreshLabels()
        super.onDraw(canvas)
    }

    private val observer = object : RecyclerView.AdapterDataObserver() {
        override fun onChanged() = refreshSoon()
        override fun onItemRangeChanged(positionStart: Int, itemCount: Int) = refreshSoon()
        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) = refreshSoon()
        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) = refreshSoon()
        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) = refreshSoon()
    }

    private fun refreshSoon() {
        post {
            visibility = View.VISIBLE
            lastThreadId = Long.MIN_VALUE
            lastMembershipKey = ""
            refreshLabels()
        }
    }

    private fun attachObserver() {
        val recyclerView = findRecyclerView() ?: return
        if (observedRecyclerView === recyclerView) return
        observedRecyclerView?.adapter?.unregisterAdapterDataObserver(observer)
        observedRecyclerView = recyclerView
        recyclerView.adapter?.registerAdapterDataObserver(observer)
    }

    private fun refreshLabels() {
        val recyclerView = findRecyclerView() ?: return
        attachObserver()
        val itemRoot = parent as? View ?: return
        val holder = runCatching { recyclerView.getChildViewHolder(itemRoot) }.getOrNull() ?: return
        val threadId = holder.itemId
        if (threadId == RecyclerView.NO_ID) return

        // The older adapter-side renderer used this tag. Remove it so there is only
        // one label row and the XML-defined view controls its own constraints.
        (itemRoot as? ViewGroup)?.let { root ->
            val old = root.findViewWithTag<View>("conversation_folder_labels")
            if (old != null && old !== this) root.removeView(old)
        }

        val folders = ConversationFolderManager.getFolders(context)
            .filter { it.enabled && !it.system }
            .associateBy { it.id }
        val memberships = ConversationFolderManager.getFolderMembership(context, threadId)
            .mapNotNull { folders[it] }
            .take(4)
        val key = memberships.joinToString("|") { "${it.id}:${it.name}:${it.color}" }

        if (threadId == lastThreadId && key == lastMembershipKey) return

        lastThreadId = threadId
        lastMembershipKey = key
        removeAllViews()
        memberships.forEach { folder ->
            addView(TextView(context).apply {
                text = folder.name
                textSize = 10f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                setTextColor(folder.color.getContrastColor())
                setPadding(dp(8), 0, dp(8), 0)
                background = GradientDrawable().apply {
                    cornerRadius = dp(10).toFloat()
                    setColor(folder.color)
                }
                layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, dp(20)).apply {
                    marginEnd = dp(5)
                }
            })
        }
        visibility = if (memberships.isEmpty()) View.GONE else View.VISIBLE
        requestLayout()
    }

    private fun findRecyclerView(): RecyclerView? {
        var current: View? = parent as? View
        while (current != null) {
            if (current is RecyclerView) return current
            current = current.parent as? View
        }
        return null
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).roundToInt()
}
