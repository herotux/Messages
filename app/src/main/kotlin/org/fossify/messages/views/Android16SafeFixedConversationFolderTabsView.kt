package org.fossify.messages.views

import android.content.Context
import android.util.AttributeSet

/**
 * Android 16-safe wrapper for the folder tabs view.
 *
 * Android 16 can dispatch onRtlPropertiesChanged() while HorizontalScrollView
 * is still constructing its superclass. ConversationFolderTabsView historically
 * changes its own layoutDirection from that callback, which re-enters the
 * callback before the child fields are initialized. The existing parent/fixed
 * view already synchronizes direction after attachment/configuration, so this
 * constructor-time callback is intentionally suppressed here.
 */
class Android16SafeFixedConversationFolderTabsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FixedConversationFolderTabsView(context, attrs) {
    override fun onRtlPropertiesChanged(layoutDirection: Int) {
        // Do not call super: the superclass callback mutates layoutDirection
        // during construction and can recursively re-enter on Android 16.
    }
}
