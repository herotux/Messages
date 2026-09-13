package org.fossify.messages.activities

import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.helpers.FontHelper
import org.fossify.messages.R
import org.fossify.messages.extensions.config
import org.fossify.messages.helpers.ThemeApplier

open class SimpleActivity : BaseSimpleActivity() {
    private var appliedFontSize = -1
    private var themeObserverInstalled = false

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
        ThemeApplier.apply(this)
        installThemeObserver()
    }

    /** Re-applies the central theme after dynamic content such as tabs/messages is attached. */
    private fun installThemeObserver() {
        if (themeObserverInstalled) return
        val content = window.decorView as? ViewGroup ?: return
        themeObserverInstalled = true
        content.viewTreeObserver.addOnGlobalLayoutListener {
            ThemeApplier.apply(this)
        }
    }

    private fun applyLocaleLayoutDirection() {
        val direction = resources.configuration.layoutDirection
        window.decorView.layoutDirection = direction
    }

    private fun applySelectedFontToViewTree(view: View) {
        val typeface = runCatching { FontHelper.getTypeface(this) }.getOrElse { Typeface.DEFAULT }
        applyTypeface(view, typeface)
    }

    private fun applyTypeface(view: View, typeface: Typeface) {
        if (view is android.widget.TextView) {
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
