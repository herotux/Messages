package org.fossify.messages.extensions

import android.widget.CompoundButton
import android.widget.TextView
import org.fossify.messages.R
import org.fossify.messages.databinding.ActivitySettingsBinding

/**
 * Compatibility accessors for ActivitySettingsBinding.
 *
 * The generated ViewBinding names changed casing between layout/compiler
 * versions. Keep SettingsActivity source stable while resolving the views
 * from the actual layout IDs used by this app.
 */
val ActivitySettingsBinding.settingsSendLongMessageMMS: CompoundButton
    get() = root.findViewById(R.id.settings_send_long_message_mms)

val ActivitySettingsBinding.settingsEmptyRecycleBinSize: TextView
    get() = root.findViewById(R.id.settings_empty_recycle_bin_size)
