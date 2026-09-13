package org.fossify.messages.helpers

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputLayout
import org.fossify.messages.R

/**
 * Single runtime application point for the app-owned visual theme.
 *
 * ThemeManager resolves the active ThemeDefinition; this class is responsible
 * only for translating that resolved palette into the current Android view tree.
 */
object ThemeApplier {
    fun apply(activity: Activity) {
        val theme = ThemeManager.themeForActivity(activity)
        ThemeManager.applyBackground(activity)
        applySystemBars(activity, theme.colors)
        (activity as? AppCompatActivity)?.supportActionBar?.let { actionBar ->
            actionBar.setBackgroundDrawable(ColorDrawable(theme.colors.toolbar))
            actionBar.setStackedBackgroundDrawable(ColorDrawable(theme.colors.toolbar))
        }
        val tabs = activity.findViewById<View>(R.id.folder_tabs)
        if (tabs is ViewGroup) styleFolderTabs(activity, tabs, theme.colors)
        val decor = activity.window.decorView
        applyPaletteToViewTree(activity, decor, theme.colors)
        clearToolbarBackgrounds(decor, theme.colors)
    }

    private fun applySystemBars(activity: Activity, colors: ThemeManager.ThemeColors) {
        activity.window.statusBarColor = colors.toolbar
        activity.window.navigationBarColor = colors.background
        var flags = activity.window.decorView.systemUiVisibility
        flags = if (isLight(colors.toolbar)) flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        else flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            flags = if (isLight(colors.background)) flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            else flags and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
        }
        activity.window.decorView.systemUiVisibility = flags
    }

    private fun applyPaletteToViewTree(activity: Activity, view: View, colors: ThemeManager.ThemeColors) {
        when (view) {
            is Toolbar -> {
                view.setBackgroundColor(colors.toolbar)
                view.setTitleTextColor(colors.textPrimary)
                view.setSubtitleTextColor(colors.textSecondary)
                view.navigationIcon?.setTint(colors.textPrimary)
                for (index in 0 until view.menu.size()) view.menu.getItem(index).icon?.setTint(colors.textPrimary)
            }
            is AppBarLayout -> view.setBackgroundColor(colors.toolbar)
            is FloatingActionButton -> {
                view.backgroundTintList = ColorStateList.valueOf(colors.fab)
                view.imageTintList = ColorStateList.valueOf(colors.textPrimary)
            }
            is MaterialCardView -> {
                view.setCardBackgroundColor(colors.surface)
                view.strokeColor = colors.divider
            }
            is MaterialButton -> {
                view.backgroundTintList = ColorStateList.valueOf(colors.primary)
                view.setTextColor(colors.textPrimary)
            }
            is TextInputLayout -> {
                view.setBoxStrokeColorStateList(ColorStateList.valueOf(colors.primary))
                view.hintTextColor = ColorStateList.valueOf(colors.textSecondary)
            }
            is EditText -> {
                view.setTextColor(colors.textPrimary)
                view.setHintTextColor(colors.textSecondary)
                view.highlightColor = colors.accent
            }
            is CompoundButton -> {
                view.buttonTintList = ColorStateList(
                    arrayOf(
                        intArrayOf(android.R.attr.state_checked),
                        intArrayOf(-android.R.attr.state_enabled),
                        intArrayOf()
                    ),
                    intArrayOf(colors.primary, colors.divider, colors.textSecondary)
                )
            }
            is ProgressBar -> {
                view.progressTintList = ColorStateList.valueOf(colors.primary)
                view.indeterminateTintList = ColorStateList.valueOf(colors.accent)
            }
            is ImageButton -> view.imageTintList = ColorStateList.valueOf(colors.textPrimary)
        }

        when (view.id) {
            R.id.message_holder, R.id.scheduled_message_holder -> view.setBackgroundColor(colors.surface)
            R.id.thread_type_message -> if (view is TextView) {
                view.setTextColor(colors.textPrimary)
                view.setHintTextColor(colors.textSecondary)
            }
            R.id.thread_send_message -> {
                view.backgroundTintList = ColorStateList.valueOf(colors.fab)
                if (view is TextView) view.setTextColor(colors.textPrimary)
            }
            R.id.thread_add_attachment,
            R.id.thread_select_sim_icon,
            R.id.thread_character_counter -> if (view is TextView) view.setTextColor(colors.textSecondary)
        }

        styleMessageBubble(view, colors)

        if (view is TextView && view !is EditText && view.id != R.id.folder_tabs && view.id != R.id.thread_message_body) {
            val current = view.currentTextColor
            if (current == Color.WHITE || current == Color.BLACK || current == Color.GRAY) view.setTextColor(colors.textPrimary)
        }

        if (view is ViewGroup) {
            for (index in 0 until view.childCount) applyPaletteToViewTree(activity, view.getChildAt(index), colors)
        }
    }

    private fun styleMessageBubble(view: View, colors: ThemeManager.ThemeColors) {
        if (view.id != R.id.thread_message_body || view !is TextView) return
        val wrapper = view.parent as? RelativeLayout ?: return
        val params = wrapper.layoutParams as? ConstraintLayout.LayoutParams ?: return
        val isOutgoing = params.endToEnd == ConstraintSet.PARENT_ID && params.startToStart != ConstraintSet.PARENT_ID
        val isIncoming = params.startToStart == ConstraintSet.PARENT_ID && params.endToEnd != ConstraintSet.PARENT_ID
        if (!isOutgoing && !isIncoming) return
        val bubbleColor = if (isOutgoing) colors.outgoingBubble else colors.incomingBubble
        val textColor = if (isOutgoing) bubbleColor.contrastColor() else colors.textPrimary
        view.backgroundTintList = ColorStateList.valueOf(bubbleColor)
        view.setTextColor(textColor)
        view.setLinkTextColor(colors.accent)
    }

    private fun clearToolbarBackgrounds(view: View, colors: ThemeManager.ThemeColors) {
        if (view is AppBarLayout) {
            view.setBackgroundColor(colors.toolbar)
            view.elevation = 0f
        } else if (view.javaClass.name.contains("ActionBarContainer")) {
            view.background = ColorDrawable(colors.toolbar)
            view.elevation = 0f
        }
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) clearToolbarBackgrounds(view.getChildAt(index), colors)
        }
    }

    private fun styleFolderTabs(activity: Activity, view: ViewGroup, colors: ThemeManager.ThemeColors) {
        for (index in 0 until view.childCount) {
            val child = view.getChildAt(index)
            if (child is TextView) {
                val tag = child.tag as? String
                val isAction = tag?.startsWith("__action__") == true
                child.setBackgroundColor(Color.TRANSPARENT)
                child.setTextColor(if (isAction) colors.accent else colors.primary)
                child.elevation = if (!isAction && child.isSelected) dp(activity, 3) else 0f
            }
            if (child is ViewGroup) styleFolderTabs(activity, child, colors)
        }
    }

    private fun dp(activity: Activity, value: Int): Float = value * activity.resources.displayMetrics.density

    private fun isLight(color: Int): Boolean {
        val luminance = (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255.0
        return luminance > 0.58
    }

    private fun Int.contrastColor(): Int {
        val luminance = (0.299 * Color.red(this) + 0.587 * Color.green(this) + 0.114 * Color.blue(this)) / 255.0
        return if (luminance > 0.55) Color.BLACK else Color.WHITE
    }
}
