package org.fossify.messages.activities

import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.helpers.FontHelper
import org.fossify.messages.R
import org.fossify.messages.helpers.BackgroundThemeManager

open class SimpleActivity : BaseSimpleActivity() {
    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        BackgroundThemeManager.apply(this)
        applySelectedFontToViewTree(window.decorView)
    }

    override fun onResume() {
        super.onResume()
        applySelectedFontToViewTree(window.decorView)
    }

    /**
     * Applies the selected Messages font to every text-based view currently attached
     * to this activity, so screens do not silently fall back to the system font.
     */
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
