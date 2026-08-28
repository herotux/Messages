package org.fossify.messages.views

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import org.fossify.commons.extensions.getProperBackgroundColor
import org.fossify.commons.extensions.getProperPrimaryColor
import org.fossify.messages.R
import org.fossify.messages.helpers.ConversationFolderManager
import kotlin.math.abs
import kotlin.math.roundToInt

class FixedConversationFolderTabsView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : ConversationFolderTabsView(context, attrs) {
    private var lastSelected: String? = null
    private var lastReorder = false
    private var preDrawInstalled = false

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        installPreDrawFix()
        post { sync(true) }
    }

    private fun installPreDrawFix() {
        if (preDrawInstalled) return
        preDrawInstalled = true
        viewTreeObserver.addOnPreDrawListener {
            setBackgroundColor(context.getProperBackgroundColor())
            styleTabs(ConversationFolderManager.getSelectedFolderId(context))
            true
        }
    }

    override fun dispatchDraw(canvas: android.graphics.Canvas) {
        setBackgroundColor(context.getProperBackgroundColor())
        sync()
        super.dispatchDraw(canvas)
    }

    private fun sync(force: Boolean = false) {
        val selected = ConversationFolderManager.getSelectedFolderId(context)
        if (force || selected != lastSelected) lastSelected = selected
        styleTabs(selected)

        val reorder = privateBool("reorderMode")
        if (force || reorder != lastReorder) {
            lastReorder = reorder
            wiggle(reorder)
        }
        if (reorder) installDrag()
    }

    /** No filled tab state; active tab gets an exact full-width bottom indicator. */
    private fun styleTabs(selected: String) {
        val tabs = privateField("tabs") as? LinearLayout ?: return
        val folders = ConversationFolderManager.getFolders(context).associateBy { it.id }
        val primary = context.getProperPrimaryColor()

        tabs.overlay.clear()
        for (i in 0 until tabs.childCount) {
            val v = tabs.getChildAt(i) as? TextView ?: continue
            val id = v.tag as? String
            val folder = id?.let { folders[it] }
            val active = id == selected

            v.setTextColor(folder?.color ?: primary)
            v.typeface = if (active) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
            v.alpha = if (active) 1f else 0.82f
            v.gravity = Gravity.CENTER
            v.setPadding(dp(16), 0, dp(16), 0)
            v.background = ColorDrawable(Color.TRANSPARENT)
            v.foreground = selectableItemBackgroundBorderless()

            if (active && v.width > 0 && tabs.height > 0) {
                val indicator = ColorDrawable(primary)
                indicator.setBounds(v.left, tabs.height - dp(2), v.right, tabs.height)
                tabs.overlay.add(indicator)
            }
        }
    }

    private fun selectableItemBackgroundBorderless(): Drawable? = runCatching {
        val value = android.util.TypedValue()
        context.theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, value, true)
        if (value.resourceId != 0) context.getDrawable(value.resourceId) else null
    }.getOrNull()

    private fun wiggle(enabled: Boolean) {
        val tabs = privateField("tabs") as? LinearLayout ?: return
        val ids = ConversationFolderManager.getFolders(context).map { it.id }.toSet()
        for (i in 0 until tabs.childCount) {
            val v = tabs.getChildAt(i)
            if ((v.tag as? String) !in ids) continue
            val old = v.getTag(R.id.folder_tabs) as? ObjectAnimator
            if (!enabled) {
                old?.cancel()
                v.setTag(R.id.folder_tabs, null)
                v.animate().rotation(0f).scaleX(1f).scaleY(1f).setDuration(120).start()
            } else if (old == null) {
                val a = ObjectAnimator.ofFloat(v, View.ROTATION, -1.7f, 1.7f).apply {
                    duration = 105
                    repeatMode = ValueAnimator.REVERSE
                    repeatCount = ValueAnimator.INFINITE
                }
                v.setTag(R.id.folder_tabs, a)
                a.start()
                v.animate().scaleX(1.04f).scaleY(1.04f).setDuration(100).start()
            }
        }
    }

    private fun installDrag() {
        val tabs = privateField("tabs") as? LinearLayout ?: return
        val ids = ConversationFolderManager.getFolders(context).map { it.id }.toSet()
        for (i in 0 until tabs.childCount) {
            val v = tabs.getChildAt(i)
            if ((v.tag as? String) !in ids || v.getTag(R.id.main_menu) == true) continue
            v.setTag(R.id.main_menu, true)
            var down = 0f
            var moved = false
            v.setOnTouchListener { view, e ->
                if (!privateBool("reorderMode")) return@setOnTouchListener false
                when (e.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        down = e.rawX
                        moved = false
                        parent?.requestDisallowInterceptTouchEvent(true)
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        parent?.requestDisallowInterceptTouchEvent(true)
                        val dx = e.rawX - down
                        if (abs(dx) > dp(5)) {
                            moved = true
                            view.translationX = dx
                            reorderByPosition(view)
                        }
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        parent?.requestDisallowInterceptTouchEvent(false)
                        view.animate().translationX(0f).setDuration(100).start()
                        if (moved) invokePrivate("saveOrder")
                        true
                    }
                    else -> false
                }
            }
        }
    }

    private fun reorderByPosition(view: View) {
        val tabs = privateField("tabs") as? LinearLayout ?: return
        val ids = ConversationFolderManager.getFolders(context).map { it.id }.toSet()
        val items = (0 until tabs.childCount)
            .map { tabs.getChildAt(it) }
            .filter { (it.tag as? String) in ids }
            .sortedBy { it.left }
        val from = items.indexOf(view)
        if (from < 0) return
        val center = view.left + view.translationX + view.width / 2f
        var target = from
        for (i in items.indices) {
            if (i != from && center < items[i].left + items[i].width / 2f) {
                target = i
                break
            }
        }
        if (target == from && center > items.last().left + items.last().width / 2f) target = items.lastIndex
        if (target == from) return
        val targetView = items[target]
        val a = tabs.indexOfChild(view)
        val b = tabs.indexOfChild(targetView)
        if (a < 0 || b < 0 || a == b) return
        tabs.removeViewAt(a)
        tabs.addView(view, b)
        view.translationX = 0f
        view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
    }

    private fun privateField(n: String): Any? = runCatching {
        ConversationFolderTabsView::class.java.getDeclaredField(n).apply { isAccessible = true }.get(this)
    }.getOrNull()

    private fun privateBool(n: String) = privateField(n) as? Boolean ?: false

    private fun invokePrivate(n: String) {
        runCatching {
            ConversationFolderTabsView::class.java.getDeclaredMethod(n).apply { isAccessible = true }.invoke(this)
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).roundToInt()
}
