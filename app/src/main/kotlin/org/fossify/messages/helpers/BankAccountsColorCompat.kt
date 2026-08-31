package org.fossify.messages.helpers

import android.app.Activity
import android.content.res.Configuration
import android.graphics.Color

/**
 * Local theme-color compatibility helpers for the bank-account feature.
 *
 * The dedicated bank-card screen uses these local helpers and remains
 * independent from optional Commons color extensions.
 */
fun Activity.getProperPrimaryColor(): Int = Color.rgb(33, 150, 243)

fun Activity.getProperTextColor(): Int {
    val night = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    return if (night) Color.WHITE else Color.BLACK
}
