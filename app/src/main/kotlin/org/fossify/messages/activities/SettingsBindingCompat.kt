package org.fossify.messages.activities

import android.widget.CompoundButton
import android.widget.TextView
import org.fossify.messages.R
import org.fossify.messages.databinding.ActivitySettingsBinding

/** Compatibility accessors for generated Settings view binding names. */
val ActivitySettingsBinding.settingsSendLongMessageMMS: CompoundButton
    get() = root.findViewById(R.id.settings_send_long_message_mms)

val ActivitySettingsBinding.settingsEmptyRecycleBinSize: TextView
    get() = root.findViewById(R.id.settings_empty_recycle_bin_size)
