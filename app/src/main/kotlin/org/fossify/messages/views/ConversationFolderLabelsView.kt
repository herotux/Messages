package org.fossify.messages.views

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.fossify.commons.extensions.getContrastColor
import org.fossify.messages.helpers.ConversationFolderManager
import kotlin.math.roundToInt

/**
 * Stable folder-label renderer owned by the conversation item itself.
 * It resolves the RecyclerView holder's thread id, so labels survive recycling,
 * filtering and switching between the main list and a folder list.
 */
class ConversationFolderLabelsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private var lastThreadId = Long.MIN_VALUE
    private var lastMembershipKey = ""

    init {
        orientation = HORIZONTAL
        gravity = Gravity.START or Gravity.CENTER_VERTICAL
        clipToPadding = false
        clipChildren = false
    }

    override fun onDraw(canvas: android.graphics.Canvas) {
        refreshLabels()
        super.onDraw(canvas)
    }

    private fun refreshLabels() {
        val recyclerView = findRecyclerView() ?: return
        val holder = runCatching { recyclerView.getChildViewHolder(parent as View) }.getOrNull() ?: return
        val threadId = holder.itemId
        if (threadId == RecyclerView.NO_ID) return

        val folders = ConversationFolderManager.getFolders(context)
            .filter { it.enabled && !it.system }
            .associateBy { it.id }
        val memberships = ConversationFolderManager.getFolderMembership(context, threadId)
            .mapNotNull { folders[it] }
            .take(4)
        val key = memberships.joinToString("|") { "${it.id}:${it.name}:${it.color}" }

        if (threadId == lastThreadId && key == lastMembershipKey) {
            visibility = if (memberships.isEmpty()) View.GONE else View.VISIBLE
            return
        }

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
