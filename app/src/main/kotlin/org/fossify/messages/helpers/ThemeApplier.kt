package org.fossify.messages.helpers

import android.app.Activity
import android.app.Dialog
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

/** Single runtime application point for the app-owned visual theme. */
object ThemeApplier {
    fun apply(activity: Activity) {
        val theme = ThemeManager.themeForActivity(activity)
        val tokens = ThemeResolver.resolve(theme)
        ThemeManager.applyBackground(activity)
        applySystemBars(activity, tokens)
        (activity as? AppCompatActivity)?.supportActionBar?.let { actionBar ->
            actionBar.setBackgroundDrawable(ColorDrawable(tokens.toolbar))
            actionBar.setStackedBackgroundDrawable(ColorDrawable(tokens.toolbar))
        }
        val tabs = activity.findViewById<View>(R.id.folder_tabs)
        if (tabs is ViewGroup) styleFolderTabs(activity, tabs, tokens)
        val decor = activity.window.decorView
        applyPaletteToViewTree(activity, decor, tokens)
        clearToolbarBackgrounds(decor, tokens)
    }

    /** Applies the same resolved theme to a dialog after its content is inflated. */
    fun apply(dialog: Dialog, activity: Activity? = null) {
        val theme = activity?.let(ThemeManager::themeForActivity) ?: ThemeManager.current(dialog.context)
        val tokens = ThemeResolver.resolve(theme)
        dialog.window?.let { window ->
            window.setBackgroundDrawable(ColorDrawable(tokens.surface))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                window.statusBarColor = tokens.toolbar
                window.navigationBarColor = tokens.background
            }
            var flags = window.decorView.systemUiVisibility
            flags = if (isLight(tokens.toolbar)) flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            else flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                flags = if (isLight(tokens.background)) flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                else flags and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
            }
            window.decorView.systemUiVisibility = flags
            applyPaletteToViewTree(activity ?: (dialog.context as? Activity), window.decorView, tokens)
        }
    }

    private fun applySystemBars(activity: Activity, tokens: HomaThemeTokens) {
        activity.window.statusBarColor = tokens.toolbar
        activity.window.navigationBarColor = tokens.background
        var flags = activity.window.decorView.systemUiVisibility
        flags = if (isLight(tokens.toolbar)) flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        else flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            flags = if (isLight(tokens.background)) flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            else flags and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
        }
        activity.window.decorView.systemUiVisibility = flags
    }

    private fun applyPaletteToViewTree(activity: Activity?, view: View, tokens: HomaThemeTokens) {
        when (view) {
            is Toolbar -> {
                view.setBackgroundColor(tokens.toolbar)
                view.setTitleTextColor(tokens.onPrimary)
                view.setSubtitleTextColor(tokens.messageSecondaryText)
                view.navigationIcon?.setTint(tokens.onPrimary)
                for (index in 0 until view.menu.size()) view.menu.getItem(index).icon?.setTint(tokens.onPrimary)
            }
            is AppBarLayout -> view.setBackgroundColor(tokens.toolbar)
            is FloatingActionButton -> {
                view.backgroundTintList = ColorStateList.valueOf(tokens.fab)
                view.imageTintList = ColorStateList.valueOf(tokens.onPrimary)
            }
            is MaterialCardView -> {
                view.setCardBackgroundColor(tokens.surface)
                view.strokeColor = tokens.divider
            }
            is MaterialButton -> applyMaterialButton(view, tokens)
            is TextInputLayout -> {
                view.setBoxStrokeColorStateList(ColorStateList.valueOf(tokens.primary))
                view.hintTextColor = ColorStateList.valueOf(tokens.onSurfaceVariant)
            }
            is EditText -> {
                view.setTextColor(tokens.onSurface)
                view.setHintTextColor(tokens.onSurfaceVariant)
                view.highlightColor = tokens.secondary
            }
            is CompoundButton -> {
                view.buttonTintList = ColorStateList(
                    arrayOf(
                        intArrayOf(android.R.attr.state_checked),
                        intArrayOf(-android.R.attr.state_enabled),
                        intArrayOf()
                    ),
                    intArrayOf(tokens.primary, tokens.outline, tokens.onSurfaceVariant)
                )
            }
            is ProgressBar -> {
                view.progressTintList = ColorStateList.valueOf(tokens.primary)
                view.indeterminateTintList = ColorStateList.valueOf(tokens.secondary)
            }
            is ImageButton -> view.imageTintList = ColorStateList.valueOf(tokens.onSurface)
        }

        when (view.id) {
            R.id.message_holder, R.id.scheduled_message_holder -> view.setBackgroundColor(tokens.surface)
            R.id.thread_type_message -> if (view is TextView) {
                view.setTextColor(tokens.messageText)
                view.setHintTextColor(tokens.messageSecondaryText)
            }
            R.id.thread_send_message -> {
                view.backgroundTintList = ColorStateList.valueOf(tokens.fab)
                if (view is TextView) view.setTextColor(tokens.onPrimary)
            }
            R.id.thread_add_attachment,
            R.id.thread_select_sim_icon,
            R.id.thread_character_counter -> if (view is TextView) view.setTextColor(tokens.messageSecondaryText)
        }

        styleMessageBubble(view, tokens)

        val hasSemanticTextColor = view is MaterialButton ||
            view.id == R.id.thread_type_message ||
            view.id == R.id.thread_send_message ||
            view.id == R.id.thread_add_attachment ||
            view.id == R.id.thread_select_sim_icon ||
            view.id == R.id.thread_character_counter
        if (view is TextView &&
            view !is EditText &&
            view !is MaterialButton &&
            !hasSemanticTextColor &&
            view.id != R.id.folder_tabs &&
            view.id != R.id.thread_message_body
        ) {
            val current = view.currentTextColor
            if (current == Color.WHITE || current == Color.BLACK || current == Color.GRAY) {
                view.setTextColor(tokens.onSurface)
            }
        }

        if (view is ViewGroup) {
            for (index in 0 until view.childCount) applyPaletteToViewTree(activity, view.getChildAt(index), tokens)
        }
    }

    /** Theme a MaterialButton without flattening its Material 3 variant. */
    private fun applyMaterialButton(view: MaterialButton, tokens: HomaThemeTokens) {
        val hasStroke = view.strokeWidth > 0
        val existingTint = view.backgroundTintList
        val hasVisibleBackground = existingTint?.defaultColor?.let { Color.alpha(it) != 0 } == true

        if (hasStroke) {
            view.strokeColor = ColorStateList.valueOf(tokens.primary)
            view.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
            view.setTextColor(tokens.primary)
            view.iconTint = ColorStateList.valueOf(tokens.primary)
        } else if (hasVisibleBackground) {
            view.backgroundTintList = ColorStateList.valueOf(tokens.primary)
            view.setTextColor(tokens.onPrimary)
            view.iconTint = ColorStateList.valueOf(tokens.onPrimary)
        } else {
            view.setTextColor(tokens.primary)
            view.iconTint = ColorStateList.valueOf(tokens.primary)
        }
    }

    private fun styleMessageBubble(view: View, tokens: HomaThemeTokens) {
        if (view.id != R.id.thread_message_body || view !is TextView) return
        val wrapper = view.parent as? RelativeLayout ?: return
        val params = wrapper.layoutParams as? ConstraintLayout.LayoutParams ?: return
        val isOutgoing = params.endToEnd == ConstraintSet.PARENT_ID && params.startToStart != ConstraintSet.PARENT_ID
        val isIncoming = params.startToStart == ConstraintSet.PARENT_ID && params.endToEnd != ConstraintSet.PARENT_ID
        if (!isOutgoing && !isIncoming) return
        val bubbleColor = if (isOutgoing) tokens.outgoingMessage else tokens.incomingMessage
        val textColor = if (isOutgoing) ThemeResolver.contrastColor(bubbleColor) else tokens.messageText
        view.backgroundTintList = ColorStateList.valueOf(bubbleColor)
        view.setTextColor(textColor)
        view.setLinkTextColor(tokens.link)
    }

    private fun clearToolbarBackgrounds(view: View, tokens: HomaThemeTokens) {
        if (view is AppBarLayout) {
            view.setBackgroundColor(tokens.toolbar)
            view.elevation = 0f
        } else if (view.javaClass.name.contains("ActionBarContainer")) {
            view.background = ColorDrawable(tokens.toolbar)
            view.elevation = 0f
        }
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) clearToolbarBackgrounds(view.getChildAt(index), tokens)
        }
    }

    private fun styleFolderTabs(activity: Activity, view: ViewGroup, tokens: HomaThemeTokens) {
        for (index in 0 until view.childCount) {
            val child = view.getChildAt(index)
            if (child is TextView) {
                val tag = child.tag as? String
                val isAction = tag?.startsWith("__action__") == true
                child.setBackgroundColor(Color.TRANSPARENT)
                child.setTextColor(if (isAction) tokens.secondary else tokens.primary)
                child.elevation = if (!isAction && child.isSelected) dp(activity, 3) else 0f
            }
            if (child is ViewGroup) styleFolderTabs(activity, child, tokens)
        }
    }

    private fun dp(activity: Activity, value: Int): Float = value * activity.resources.displayMetrics.density

    private fun isLight(color: Int): Boolean {
        val red = (color shr 16) and 0xFF
        val green = (color shr 8) and 0xFF
        val blue = color and 0xFF
        val luminance = (0.299 * red + 0.587 * green + 0.114 * blue) / 255.0
        return luminance > 0.58
    }
}
