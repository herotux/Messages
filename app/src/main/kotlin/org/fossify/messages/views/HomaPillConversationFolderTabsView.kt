package org.fossify.messages.views

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import org.fossify.commons.extensions.getProperPrimaryColor
import org.fossify.messages.helpers.ConversationFolderManager

/**
 * Presentation-only wrapper for the existing folder-tab behavior.
 *
 * Folder selection, filtering, reordering and actions remain owned by the existing
 * folder-tab implementation. This class only swaps the visual presentation to the
 * reusable HomaPillTabView style, making the UI easy to replace later.
 */
class HomaPillConversationFolderTabsView @JvmOverloads constructor(
    context: android.content.Context,
    attrs: android.util.AttributeSet? = null
) : Android16SafeFixedConversationFolderTabsView(context, attrs) {

    override fun dispatchDraw(canvas: android.graphics.Canvas) {
        super.dispatchDraw(canvas)
        applyPillPresentation()
    }

    private fun applyPillPresentation() {
        val tabs = getChildAt(0) as? ViewGroup ?: return
        val folders = ConversationFolderManager.getFolders(context).associateBy { it.id }
        val selectedId = ConversationFolderManager.getSelectedFolderId(context)
        val primary = context.getProperPrimaryColor()

        if (tabs is ViewGroup) tabs.overlay.clear()

        for (i in 0 until tabs.childCount) {
            val view = tabs.getChildAt(i) as? TextView ?: continue
            val id = view.tag as? String ?: continue
            val folder = folders[id] ?: continue
            val selected = id == selectedId
            HomaPillTabView.applyTo(view, selected, if (selected) folder.color else primary)
            view.layoutParams = (view.layoutParams ?: LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )).apply {
                width = ViewGroup.LayoutParams.WRAP_CONTENT
                height = ViewGroup.LayoutParams.MATCH_PARENT
            }
        }
    }
}
