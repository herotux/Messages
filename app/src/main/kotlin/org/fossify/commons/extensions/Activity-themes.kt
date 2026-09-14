package org.fossify.commons.extensions

import android.app.Activity
import org.fossify.messages.R

/**
 * Returns the single canonical application theme resource.
 *
 * Runtime theme selection is owned by ThemeManager/ThemeApplier; this extension
 * deliberately does not select or expose any of the former per-palette XML themes.
 */
fun Activity.getThemeId(
    color: Int = baseConfig.primaryColor,
    showTransparentTop: Boolean = false
): Int = R.style.AppTheme_Base
