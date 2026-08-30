package org.fossify.messages.databinding

import android.widget.TextView
import org.fossify.messages.R

/**
 * Keeps compatibility with legacy binding property casing and with layouts
 * where a view is present in only one resource configuration.
 */
val ActivitySettingsBinding.settingsSendLongMessageMMS
    get() = settingsSendLongMessageMms

val ActivitySettingsBinding.settingsEmptyRecycleBinSize
    get() = root.findViewById<TextView>(R.id.settings_empty_recycle_bin_size)
