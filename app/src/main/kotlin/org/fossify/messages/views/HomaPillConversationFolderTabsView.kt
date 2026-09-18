package org.fossify.messages.views

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.widget.TextView

/**
 * Presentation-only variant of the existing conversation-folder tabs.
 * Selection, filtering, ordering and reorder gestures remain in the fixed
 * folder-tab implementation; only the visual treatment is replaced here.
 */
class HomaPillConversationFolderTabsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : Android16SafeFixedConversationFolderTabsView(context, attrs) {

    init {
        setBackgroundColor(Color.TRANSPARENT)
    }

    override fun styleTabs(selected: String) {
        setBackgroundColor(Color.TRANSPARENT)
        val tabs = tabContainer() ?: return
        val folders = folderMap()
        val primary = primaryTabColor()

        tabs.overlay.clear()
        for (i in 0 until tabs.childCount) {
            val view = tabs.getChildAt(i) as? TextView ?: continue
            val id = view.tag as? String ?: continue
            val folder = folders[id]
            val active = id == selected
            HomaPillTabView.applyTo(view, active, if (active) folder?.color ?: primary else primary)
        }
    }

}
