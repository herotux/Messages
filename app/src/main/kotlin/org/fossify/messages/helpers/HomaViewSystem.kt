package org.fossify.messages.helpers

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.R as MaterialR
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import java.util.WeakHashMap

/** Shared Homa contract for the existing View-based screens. */
object HomaViewSystem {
    private data class PaddingSnapshot(
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int,
    )

    private val installedRoots = WeakHashMap<ViewGroup, Unit>()

    /** Applies Homa styling and the shared edge-to-edge/inset contract. */
    fun apply(activity: Activity) {
        val root = contentRoot(activity) ?: return
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)
        styleTree(root)
        installInsetsOnce(root)
    }

    /** Styles an existing screen without replacing its established inset contract. */
    fun style(activity: Activity) {
        contentRoot(activity)?.let(::styleTree)
    }

    private fun contentRoot(activity: Activity): ViewGroup? =
        activity.window.decorView.findViewById<ViewGroup>(android.R.id.content)?.getChildAt(0) as? ViewGroup

    private fun installInsetsOnce(root: ViewGroup) {
        synchronized(installedRoots) {
            if (installedRoots.containsKey(root)) return
            installedRoots[root] = Unit
        }

        val toolbar = findFirstToolbar(root)
        val scroll = findFirst(root, ScrollView::class.java)
        val toolbarPadding = toolbar?.let { PaddingSnapshot(it.paddingLeft, it.paddingTop, it.paddingRight, it.paddingBottom) }
        val scrollPadding = scroll?.let { PaddingSnapshot(it.paddingLeft, it.paddingTop, it.paddingRight, it.paddingBottom) }

        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            toolbar?.let { base ->
                toolbarPadding?.let { p ->
                    base.updatePadding(
                        left = p.left,
                        top = p.top + bars.top,
                        right = p.right,
                        bottom = p.bottom,
                    )
                }
            }
            scroll?.let { base ->
                scrollPadding?.let { p ->
                    base.updatePadding(
                        left = p.left,
                        top = p.top,
                        right = p.right,
                        bottom = p.bottom + maxOf(bars.bottom, ime.bottom),
                    )
                }
            }
            insets
        }
        ViewCompat.requestApplyInsets(root)
    }

    private fun styleTree(view: View) {
        when (view) {
            is MaterialToolbar -> styleToolbar(view)
            is Toolbar -> styleToolbar(view)
            is MaterialCardView -> {
                view.radius = dp(view, 16).toFloat()
                view.cardElevation = dp(view, 1).toFloat()
                if (view.strokeWidth > 0) {
                    view.strokeWidth = dp(view, 1)
                    view.strokeColor = resolveColor(view, MaterialR.attr.colorOutlineVariant)
                }
            }
            is MaterialButton -> {
                view.minHeight = dp(view, 48)
                view.cornerRadius = dp(view, 12)
            }
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) styleTree(view.getChildAt(i))
        }
    }

    private fun styleToolbar(toolbar: Toolbar) {
        toolbar.minimumHeight = dp(toolbar, 64)
        toolbar.setBackgroundColor(resolveColor(toolbar, MaterialR.attr.colorSurface))
        toolbar.setTitleTextColor(resolveColor(toolbar, MaterialR.attr.colorOnSurface))
        toolbar.elevation = 0f
    }

    private fun findFirstToolbar(root: View): Toolbar? {
        if (root is Toolbar) return root
        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                findFirstToolbar(root.getChildAt(i))?.let { return it }
            }
        }
        return null
    }

    private fun <T : View> findFirst(root: View, type: Class<T>): T? {
        if (type.isInstance(root)) return type.cast(root)
        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                findFirst(root.getChildAt(i), type)?.let { return it }
            }
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
