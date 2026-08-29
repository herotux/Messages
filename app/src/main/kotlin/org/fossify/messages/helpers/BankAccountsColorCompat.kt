package org.fossify.messages.helpers

import android.content.Context
import androidx.core.content.ContextCompat

fun Context.getProperPrimaryColor(): Int = ContextCompat.getColor(this, org.fossify.commons.R.color.color_primary)
fun Context.getProperTextColor(): Int = ContextCompat.getColor(this, org.fossify.commons.R.color.default_text_color)
