package org.fossify.messages.helpers

import android.content.Context
import android.util.TypedValue

fun Context.getProperPrimaryColor(): Int = resolveBankThemeColor(com.google.android.material.R.attr.colorPrimary)
fun Context.getProperTextColor(): Int = resolveBankThemeColor(android.R.attr.textColorPrimary)

private fun Context.resolveBankThemeColor(attribute: Int): Int {
    val value = TypedValue()
    theme.resolveAttribute(attribute, value, true)
    return if (value.resourceId != 0) getColor(value.resourceId) else value.data
}
