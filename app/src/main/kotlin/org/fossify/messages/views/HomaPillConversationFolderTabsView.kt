package org.fossify.messages.views

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.fossify.messages.helpers.ThemeManager

/**
 * Keeps the existing folder-tab behavior and changes only its visual presentation.
 * This makes the tab style replaceable without changing folder selection logic.
 */
class HomaPillConversationFolderTabsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : Android16SafeFixedConversationFolderTabsView(context, attrs, defStyleAttr) {

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        applyPresentation()
    }

    private fun applyPresentation() {
        val primary = ThemeManager.resolveColors(context).primary
        styleTree(this, primary)
    }

    private fun styleTree(view: View, primary: Int) {
        if (view is TextView && view !== this) {
            HomaPillTabView.applyTo(view, view.isSelected || view.isActivated, primary)
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                styleTree(view.getChildAt(i), primary)
            }
        }
    }
}
