package org.fossify.messages.helpers

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import com.google.android.material.R as MaterialR

/** Shared Homa contract for the existing View-based screens. */
object HomaViewSystem {
    /** Applies Homa styling and the shared edge-to-edge/inset contract. */
    fun apply(activity: Activity) {
        val root = contentRoot(activity) ?: return
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)
        styleTree(root)
        installInsets(root)
    }

    /** Styles an existing screen without replacing its established inset contract. */
    fun style(activity: Activity) {
        contentRoot(activity)?.let(::styleTree)
    }

    private fun contentRoot(activity: Activity): ViewGroup? =
        activity.window.decorView.findViewById<ViewGroup>(android.R.id.content)?.getChildAt(0) as? ViewGroup

    private fun installInsets(root: ViewGroup) {
        val toolbar = findFirst<MaterialToolbar>(root)
        val scroll = findFirst<ScrollView>(root)
        val toolbarLeft = toolbar?.paddingLeft ?: 0
        val toolbarRight = toolbar?.paddingRight ?: 0
        val toolbarBottom = toolbar?.paddingBottom ?: 0
        val scrollLeft = scroll?.paddingLeft ?: 0
        val scrollTop = scroll?.paddingTop ?: 0
        val scrollRight = scroll?.paddingRight ?: 0
        val scrollBottom = scroll?.paddingBottom ?: 0

        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            toolbar?.updatePadding(
                left = toolbarLeft,
                top = toolbarBottom + bars.top,
                right = toolbarRight,
                bottom = toolbarBottom,
            )
            scroll?.updatePadding(
                left = scrollLeft,
                top = scrollTop,
                right = scrollRight,
                bottom = scrollBottom + maxOf(bars.bottom, ime.bottom),
            )
            insets
        }
        ViewCompat.requestApplyInsets(root)
    }

    private fun styleTree(view: View) {
        when (view) {
            is MaterialToolbar -> {
                view.minimumHeight = dp(view, 64)
                view.setBackgroundColor(resolveColor(view, MaterialR.attr.colorSurface))
                view.setTitleTextColor(resolveColor(view, MaterialR.attr.colorOnSurface))
                view.elevation = 0f
            }
            is MaterialCardView -> {
                view.radius = dp(view, 16).toFloat()
                view.cardElevation = dp(view, 1).toFloat()
                view.strokeWidth = dp(view, 1)
                view.strokeColor = resolveColor(view, MaterialR.attr.colorOutlineVariant)
                view.setCardBackgroundColor(resolveColor(view, MaterialR.attr.colorSurface))
            }
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) styleTree(view.getChildAt(i))
        }
    }

    private inline fun <reified T : View> findFirst(root: View): T? {
        if (root is T) return root
        if (root is ViewGroup) {
            for (i in 0 until root.childCount) findFirst<T>(root.getChildAt(i))?.let { return it }
        }
        return null
    }

    private fun resolveColor(view: View, attr: Int): Int {
        val value = android.util.TypedValue()
        view.context.theme.resolveAttribute(attr, value, true)
        return if (value.resourceId != 0) view.context.getColor(value.resourceId) else value.data
    }

    private fun dp(view: View, value: Int): Int = (value * view.resources.displayMetrics.density).toInt()
}
