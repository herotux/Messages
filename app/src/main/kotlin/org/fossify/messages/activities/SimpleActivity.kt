package org.fossify.messages.activities

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.helpers.FontHelper
import org.fossify.messages.R
import org.fossify.messages.extensions.config
import org.fossify.messages.helpers.BackgroundThemeManager
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
        BackgroundThemeManager.apply(this)
        applyThemeChrome()
        installThemeChromeObserver()
    }

    /** Applies the active ThemeManager palette to system/app chrome. */
    private fun applyThemeChrome() {
        val colors = ThemeManager.colors(this)

        supportActionBar?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        supportActionBar?.setStackedBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        window.statusBarColor = colors.toolbar
        window.navigationBarColor = colors.background

        val tabs = findViewById<View>(R.id.folder_tabs)
        if (tabs is ViewGroup) {
            styleFolderTabs(tabs, colors)
        }

        clearToolbarBackgrounds(window.decorView)
    }

    private fun clearToolbarBackgrounds(view: View) {
        val name = view.javaClass.name
        if (name.contains("Toolbar") || name.contains("AppBarLayout") || name.contains("ActionBarContainer")) {
            view.background = null
            view.elevation = 0f
        }
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                clearToolbarBackgrounds(view.getChildAt(index))
            }
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
            val colors = ThemeManager.colors(this)
            val tabs = findViewById<View>(R.id.folder_tabs)
            if (tabs is ViewGroup) styleFolderTabs(tabs, colors)
            clearToolbarBackgrounds(content)
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
            for (index in 0 until view.childCount) {
                applyTypeface(view.getChildAt(index), typeface)
            }
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
