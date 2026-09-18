package org.fossify.messages.helpers

import android.app.Activity
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

/** Shared edge-to-edge contract for legacy View-based Homa pages. */
object HomaEdgeToEdge {
    /**
     * Lets the root draw behind system bars while assigning status-bar insets to the header
     * and navigation/IME insets to the scrollable content. The header surface itself therefore
     * reaches behind the status bar instead of starting below it.
     */
    fun install(activity: Activity, root: View, header: View? = null, content: View? = null) {
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)

        val headerLeft = header?.paddingLeft ?: 0
        val headerRight = header?.paddingRight ?: 0
        val headerBottom = header?.paddingBottom ?: 0
        val contentLeft = content?.paddingLeft ?: 0
        val contentTop = content?.paddingTop ?: 0
        val contentRight = content?.paddingRight ?: 0
        val contentBottom = content?.paddingBottom ?: 0

        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            header?.updatePadding(
                left = headerLeft,
                top = bars.top,
                right = headerRight,
                bottom = headerBottom,
            )
            content?.updatePadding(
                left = contentLeft,
                top = contentTop,
                right = contentRight,
                bottom = contentBottom + maxOf(bars.bottom, ime.bottom),
            )
            insets
        }
        ViewCompat.requestApplyInsets(root)
    }
}
