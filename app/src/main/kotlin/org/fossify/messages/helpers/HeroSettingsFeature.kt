package org.fossify.messages.helpers

import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.materialswitch.MaterialSwitch
import org.fossify.commons.extensions.beGone
import org.fossify.commons.extensions.getProperPrimaryColor
import org.fossify.messages.R
import org.fossify.messages.activities.SettingsActivity
import org.fossify.messages.extensions.config

/**
 * Keeps HEROTUX-specific settings together in one dedicated section.
 * The section is rebuilt on resume so configuration changes and locale changes
 * are reflected without accumulating duplicate rows.
 */
object HeroSettingsFeature {
    private const val SECTION_TAG = "hero_settings_section"
    private const val JALALI_TAG = "hero_jalali_switch"
    private const val BANKS_TAG = "hero_bank_accounts_row"
    private const val FOLDERS_TAG = "hero_folders_switch"
    private const val OLD_CALENDAR_TAG = "persian_calendar_switch"

    fun setup(context: SettingsActivity, holder: ViewGroup) {
        removePreviousSection(holder)
        hideLegacyFolderSetting(holder)

        val section = TextView(context).apply {
            tag = SECTION_TAG
            text = context.getString(R.string.hero_settings)
            textSize = 13f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(context.getProperPrimaryColor())
            setPadding(dp(context, 28), dp(context, 18), dp(context, 28), dp(context, 8))
        }

        val calendarRow = LinearLayout(context).apply {
            tag = JALALI_TAG
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(context, 28), dp(context, 6), dp(context, 28), dp(context, 6))
            background = context.getDrawable(R.drawable.ripple_background)
            isClickable = true
            isFocusable = true
        }

        val calendarSwitch = MaterialSwitch(context).apply {
            text = context.getString(R.string.use_persian_calendar)
            textSize = 16f
            isChecked = context.config.usePersianCalendar
            isClickable = false
            isFocusable = false
        }
        calendarRow.addView(calendarSwitch, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        calendarRow.setOnClickListener {
            calendarSwitch.isChecked = !calendarSwitch.isChecked
            context.config.usePersianCalendar = calendarSwitch.isChecked
            context.refreshConversations()
        }

        val bankRow = LinearLayout(context).apply {
            tag = BANKS_TAG
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(context, 28), dp(context, 10), dp(context, 28), dp(context, 10))
            background = context.getDrawable(R.drawable.ripple_background)
            isClickable = true
            isFocusable = true
        }

        bankRow.addView(TextView(context).apply {
            text = context.getString(R.string.bank_accounts)
            textSize = 16f
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        bankRow.addView(TextView(context).apply {
            text = "›"
            textSize = 24f
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(dp(context, 28), ViewGroup.LayoutParams.WRAP_CONTENT))

        bankRow.setOnClickListener { BankAccountsFeature.showBankAccountManager(context) }

        val foldersRow = LinearLayout(context).apply {
            tag = FOLDERS_TAG
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(context, 28), dp(context, 6), dp(context, 28), dp(context, 6))
            background = context.getDrawable(R.drawable.ripple_background)
            isClickable = true
            isFocusable = true
        }

        val foldersSwitch = MaterialSwitch(context).apply {
            text = context.getString(R.string.settings_show_folders)
            textSize = 16f
            isChecked = ConversationFolderManager.areFoldersVisible(context)
            isClickable = false
            isFocusable = false
        }
        foldersRow.addView(foldersSwitch, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        foldersRow.setOnClickListener {
            foldersSwitch.isChecked = !foldersSwitch.isChecked
            ConversationFolderManager.setFoldersVisible(context, foldersSwitch.isChecked)
            context.refreshConversations()
        }

        val heroContainer = LinearLayout(context).apply {
            tag = "hero_settings_container"
            orientation = LinearLayout.VERTICAL
            addView(section, LinearLayout.LayoutParams(-1, -2))
            addView(calendarRow, LinearLayout.LayoutParams(-1, -2))
            addView(bankRow, LinearLayout.LayoutParams(-1, -2))
            addView(foldersRow, LinearLayout.LayoutParams(-1, -2))
        }

        val generalLabel = holder.findViewById<View>(R.id.settings_general_settings_label)
        val index = if (generalLabel != null) holder.indexOfChild(generalLabel) else 0
        holder.addView(heroContainer, index)
    }

    private fun hideLegacyFolderSetting(holder: ViewGroup) {
        holder.findViewById<View>(R.id.settings_folders_label)?.beGone()
        holder.findViewById<View>(R.id.settings_folders_visibility_holder)?.beGone()
        holder.findViewById<View>(R.id.settings_folders_visibility_summary)?.beGone()
        holder.findViewById<View>(R.id.settings_folders_divider)?.beGone()
    }

    private fun removePreviousSection(holder: ViewGroup) {
        holder.findViewWithTag<View>("hero_settings_container")?.let { holder.removeView(it) }
        holder.findViewWithTag<View>(SECTION_TAG)?.let { holder.removeView(it) }
        holder.findViewWithTag<View>(JALALI_TAG)?.let { holder.removeView(it) }
        holder.findViewWithTag<View>(BANKS_TAG)?.let { holder.removeView(it) }
        holder.findViewWithTag<View>(FOLDERS_TAG)?.let { holder.removeView(it) }
        holder.findViewWithTag<View>(OLD_CALENDAR_TAG)?.let { holder.removeView(it) }
    }

    private fun dp(context: Context, value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()
}
