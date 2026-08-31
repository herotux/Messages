package org.fossify.messages.helpers

import android.app.Activity
import android.content.res.Configuration
import android.graphics.Color

/**
 * Local theme-color compatibility helpers for the bank-account feature.
 *
 * This feature is intentionally kept independent from optional Commons color
 * extensions so the Core build does not depend on extension imports that may
 * differ between Commons revisions.
 */
fun Activity.getProperPrimaryColor(): Int = Color.rgb(33, 150, 243)

fun Activity.getProperTextColor(): Int {
    val night = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    return if (night) Color.WHITE else Color.BLACK
}
