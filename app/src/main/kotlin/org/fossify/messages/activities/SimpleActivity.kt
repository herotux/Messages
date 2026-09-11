package org.fossify.messages.activities

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputLayout
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.helpers.FontHelper
import org.fossify.messages.R
import org.fossify.messages.extensions.config
import org.fossify.messages.helpers.ThemeManager

open class SimpleActivity : BaseSimpleActivity() {
    private var appliedFontSize = -1
    private var chromeObserverInstalled = false

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        applyLocaleLayoutDirection()
        applyVisualTheme()
        applySelectedFontToViewTree(window.decorView)
        appliedFontSize = config.fontSize
    }

    override fun onResume() {
        super.onResume()
        applyLocaleLayoutDirection()
        applyVisualTheme()
        applySelectedFontToViewTree(window.decorView)

        if (appliedFontSize != -1 && appliedFontSize != config.fontSize && this !is SettingsActivity) {
            appliedFontSize = config.fontSize
            recreate()
            return
        }
        appliedFontSize = config.fontSize
    }

    private fun applyVisualTheme() {
        ThemeManager.applyBackground(this)
        applyThemeChrome()
        installThemeChromeObserver()
    }

    /** Applies the active global or conversation-specific ThemeManager palette. */
    private fun applyThemeChrome() {
        val colors = ThemeManager.themeForActivity(this).colors

        supportActionBar?.setBackgroundDrawable(ColorDrawable(colors.toolbar))
        supportActionBar?.setStackedBackgroundDrawable(ColorDrawable(colors.toolbar))

        window.statusBarColor = colors.toolbar
        window.navigationBarColor = colors.background

        val tabs = findViewById<View>(R.id.folder_tabs)
        if (tabs is ViewGroup) styleFolderTabs(tabs, colors)

        applyPaletteToCommonViews(window.decorView, colors)
        clearToolbarBackgrounds(window.decorView, colors)
    }

    private fun applyPaletteToCommonViews(view: View, colors: ThemeManager.ThemeColors) {
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
        }

        styleMessageBubble(view, colors)

        if (view is TextView && view !is EditText && view.id != R.id.folder_tabs && view.id != R.id.thread_message_body) {
            val current = view.currentTextColor
            if (current == Color.WHITE || current == Color.BLACK || current == Color.GRAY) view.setTextColor(colors.textPrimary)
        }

        if (view is ViewGroup) {
            for (index in 0 until view.childCount) applyPaletteToCommonViews(view.getChildAt(index), colors)
        }
    }

    /** Applies theme colors to message bubbles while preserving their existing shape. */
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

    private fun Int.contrastColor(): Int {
        val luminance = (0.299 * Color.red(this) + 0.587 * Color.green(this) + 0.114 * Color.blue(this)) / 255.0
        return if (luminance > 0.55) Color.BLACK else Color.WHITE
    }

    private fun clearToolbarBackgrounds(view: View, colors: ThemeManager.ThemeColors) {
        if (view is AppBarLayout) {
            view.setBackgroundColor(colors.toolbar)
            view.elevation = 0f
        } else {
            val name = view.javaClass.name
            if (name.contains("ActionBarContainer")) {
                view.background = ColorDrawable(colors.toolbar)
                view.elevation = 0f
            }
        }
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) clearToolbarBackgrounds(view.getChildAt(index), colors)
        }
    }

    /** Keeps folder tabs tied to the active theme instead of legacy folder colors. */
    private fun styleFolderTabs(view: ViewGroup, colors: ThemeManager.ThemeColors) {
        for (index in 0 until view.childCount) {
            val child = view.getChildAt(index)
            if (child is TextView) {
                val tag = child.tag as? String
                val isAction = tag?.startsWith("__action__") == true
                child.setBackgroundColor(Color.TRANSPARENT)
                child.setTextColor(if (isAction) colors.accent else colors.primary)
                child.elevation = if (!isAction && child.isSelected) dp(3) else 0f
            }
            if (child is ViewGroup) styleFolderTabs(child, colors)
        }
    }

    private fun installThemeChromeObserver() {
        if (chromeObserverInstalled) return
        val content = window.decorView as? ViewGroup ?: return
        chromeObserverInstalled = true
        content.viewTreeObserver.addOnGlobalLayoutListener {
            val colors = ThemeManager.themeForActivity(this).colors
            val tabs = findViewById<View>(R.id.folder_tabs)
            if (tabs is ViewGroup) styleFolderTabs(tabs, colors)
            applyPaletteToCommonViews(content, colors)
            clearToolbarBackgrounds(content, colors)
        }
    }

    private fun dp(value: Int): Float = value * resources.displayMetrics.density

    private fun applyLocaleLayoutDirection() {
        val direction = resources.configuration.layoutDirection
        window.decorView.layoutDirection = direction
    }

    private fun applySelectedFontToViewTree(view: View) {
        val typeface = runCatching { FontHelper.getTypeface(this) }.getOrElse { Typeface.DEFAULT }
        applyTypeface(view, typeface)
    }

    private fun applyTypeface(view: View, typeface: Typeface) {
        if (view is TextView) {
            val currentStyle = when (view.typeface?.style) {
                Typeface.BOLD -> Typeface.BOLD
                Typeface.ITALIC -> Typeface.ITALIC
                Typeface.BOLD_ITALIC -> Typeface.BOLD_ITALIC
                else -> Typeface.NORMAL
            }
            view.setTypeface(typeface, currentStyle)
        }
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) applyTypeface(view.getChildAt(index), typeface)
        }
    }

    override fun getAppIconIDs() = arrayListOf(
        R.mipmap.ic_launcher_red,
        R.mipmap.ic_launcher_pink,
        R.mipmap.ic_launcher_purple,
        R.mipmap.ic_launcher_deep_purple,
        R.mipmap.ic_launcher_indigo,
        R.mipmap.ic_launcher_blue,
        R.mipmap.ic_launcher_light_blue,
        R.mipmap.ic_launcher_cyan,
        R.mipmap.ic_launcher_teal,
        R.mipmap.ic_launcher,
        R.mipmap.ic_launcher_light_green,
        R.mipmap.ic_launcher_lime,
        R.mipmap.ic_launcher_yellow,
        R.mipmap.ic_launcher_amber,
        R.mipmap.ic_launcher_orange,
        R.mipmap.ic_launcher_deep_orange,
        R.mipmap.ic_launcher_brown,
        R.mipmap.ic_launcher_blue_grey,
        R.mipmap.ic_launcher_grey_black
    )

    override fun getAppLauncherName() = getString(R.string.app_launcher_name)
    override fun getRepositoryName() = "Messages"
}
