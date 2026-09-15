package org.fossify.messages.views

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.fossify.messages.helpers.ThemeManager

/** Presentation-only styling layer for the existing conversation-folder tabs. */
class HomaPillConversationFolderTabsView2 @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : Android16SafeFixedConversationFolderTabsView(context, attrs, defStyleAttr) {

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        styleChildren()
    }

    private fun styleChildren() {
        val primary = ThemeManager.resolveColors(context).primary
        for (i in 0 until childCount) {
            styleView(getChildAt(i), primary)
        }
    }

    private fun styleView(view: View, primary: Int) {
        if (view is TextView) {
            HomaPillTabView.applyTo(view, view.isSelected || view.isActivated, primary)
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                styleView(view.getChildAt(i), primary)
            }
        }
    }
}
