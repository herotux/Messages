package org.fossify.messages.views

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.fossify.messages.helpers.ThemeManager

/** Presentation-only layer; folder behavior remains owned by the existing tab implementation. */
class HomaPillConversationFolderTabsView2 @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : Android16SafeFixedConversationFolderTabsView(context, attrs, defStyleAttr) {
    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        val primary = ThemeManager.resolveColors(context).primary
        styleViewTree(this, primary)
    }

    private fun styleViewTree(view: View, color: Int) {
        if (view is TextView && view !== this) {
            HomaPillTabView.applyTo(view, view.isSelected || view.isActivated, color)
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) styleViewTree(view.getChildAt(i), color)
        }
    }
}
