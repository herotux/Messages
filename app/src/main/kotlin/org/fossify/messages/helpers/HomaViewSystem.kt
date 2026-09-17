package org.fossify.messages.helpers

import android.app.Activity
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.R as MaterialR
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputLayout
import java.util.WeakHashMap

/** Shared Homa contract for the existing View-based screens. */
object HomaViewSystem {
    private data class PaddingSnapshot(val left: Int, val top: Int, val right: Int, val bottom: Int)
    private val installedRoots = WeakHashMap<ViewGroup, Unit>()

    fun apply(activity: Activity) {
        val root = contentRoot(activity) ?: return
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)
        styleTree(root)
        installInsetsOnce(root)
    }

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
            toolbar?.let { base -> toolbarPadding?.let { p ->
                base.updatePadding(left = p.left, top = p.top + bars.top, right = p.right, bottom = p.bottom)
            } }
            scroll?.let { base -> scrollPadding?.let { p ->
                base.updatePadding(left = p.left, top = p.top, right = p.right, bottom = p.bottom + maxOf(bars.bottom, ime.bottom))
            } }
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
            is Chip -> view.minHeight = dp(view, 40)
            is SwitchMaterial -> view.minHeight = dp(view, 48)
            is TextInputLayout -> {
                val radius = dp(view, 12).toFloat()
                view.setBoxCornerRadii(radius, radius, radius, radius)
                view.setBoxStrokeColor(resolveColor(view, MaterialR.attr.colorOutline))
                view.setBoxStrokeWidthFocused(dp(view, 2))
            }
            is FloatingActionButton -> view.elevation = dp(view, 3).toFloat()
            is EditText -> styleEditText(view)
            is TextView -> if (view.isClickable && view !is MaterialButton && view !is Chip) styleClickableText(view)
        }
        if (view is ViewGroup) for (i in 0 until view.childCount) styleTree(view.getChildAt(i))
    }

    private fun styleToolbar(toolbar: Toolbar) {
        toolbar.minimumHeight = dp(toolbar, 72)
        toolbar.layoutParams?.let { params -> if (params.height > 0) params.height = maxOf(params.height, dp(toolbar, 72)) }
        // Keep the Toolbar only as a navigation/action container; Homa does not use a title strip.
        toolbar.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        toolbar.setTitleTextColor(resolveColor(toolbar, MaterialR.attr.colorOnBackground))
        toolbar.elevation = 0f
        toolbar.translationZ = 0f
    }

    private fun styleEditText(editText: EditText) {
        editText.minHeight = maxOf(editText.minHeight, dp(editText, 48))
        if (editText.background == null || editText.background !is GradientDrawable) editText.background = roundedSurface(editText, 12)
        val horizontal = maxOf(editText.paddingLeft, dp(editText, 14))
        val vertical = maxOf(editText.paddingTop, dp(editText, 10))
        editText.setPadding(horizontal, vertical, horizontal, vertical)
    }

    private fun styleClickableText(view: TextView) {
        view.minHeight = maxOf(view.minimumHeight, dp(view, 48))
        view.layoutParams?.let { params -> if (params.height > 0) params.height = maxOf(params.height, dp(view, 48)) }
        if (view.background == null || view.background is GradientDrawable) view.background = roundedSurface(view, 12)
        val horizontal = maxOf(view.paddingLeft, dp(view, 12))
        val vertical = maxOf(view.paddingTop, dp(view, 8))
        view.setPadding(horizontal, vertical, horizontal, vertical)
    }

    private fun roundedSurface(view: View, radiusDp: Int): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dp(view, radiusDp).toFloat()
        setColor(resolveColor(view, MaterialR.attr.colorSurfaceVariant))
    }

    private fun findFirstToolbar(root: View): Toolbar? {
        if (root is Toolbar) return root
        if (root is ViewGroup) for (i in 0 until root.childCount) findFirstToolbar(root.getChildAt(i))?.let { return it }
        return null
    }

    private fun <T : View> findFirst(root: View, type: Class<T>): T? {
        if (type.isInstance(root)) return type.cast(root)
        if (root is ViewGroup) for (i in 0 until root.childCount) findFirst(root.getChildAt(i), type)?.let { return it }
        return null
    }

    private fun resolveColor(view: View, attr: Int): Int {
        val value = android.util.TypedValue()
        view.context.theme.resolveAttribute(attr, value, true)
        return if (value.resourceId != 0) view.context.getColor(value.resourceId) else value.data
    }

    private fun dp(view: View, value: Int): Int = (value * view.resources.displayMetrics.density).toInt()
}
