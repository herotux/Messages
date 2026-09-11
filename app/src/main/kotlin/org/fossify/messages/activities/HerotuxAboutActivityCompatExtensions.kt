package org.fossify.messages.activities

import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import org.fossify.commons.helpers.FontHelper
import org.fossify.messages.extensions.config

/**
 * Compatibility helpers for the legacy HerotuxAboutActivity UI.
 * Keep these small so the activity can continue using the older API surface
 * without coupling ThemeManager to Android UI code.
 */
val HerotuxAboutActivity.config
    get() = applicationContext.config

var EditText.singleLine: Boolean
    get() = maxLines == 1
    set(value) {
        setSingleLine(value)
    }

fun View.applyPersianFont() {
    val typeface = runCatching { FontHelper.getTypeface(context) }.getOrElse { Typeface.DEFAULT }

    fun apply(view: View) {
        if (view is TextView) {
            val style = when (view.typeface?.style) {
                Typeface.BOLD -> Typeface.BOLD
                Typeface.ITALIC -> Typeface.ITALIC
                Typeface.BOLD_ITALIC -> Typeface.BOLD_ITALIC
                else -> Typeface.NORMAL
            }
            view.setTypeface(typeface, style)
        }
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                apply(view.getChildAt(index))
            }
        }
    }

    apply(this)
}

/** Legacy call site compatibility: the activity itself is a convenient root. */
fun HerotuxAboutActivity.applyPersianFont() {
    window.decorView.applyPersianFont()
}
