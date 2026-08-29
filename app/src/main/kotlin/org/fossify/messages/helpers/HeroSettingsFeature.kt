package org.fossify.messages.helpers

import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.materialswitch.MaterialSwitch
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

    fun setup(context: SettingsActivity, holder: ViewGroup) {
        holder.findViewWithTag<View>(SECTION_TAG)?.let { holder.removeView(it) }
        holder.findViewWithTag<View>(JALALI_TAG)?.let { holder.removeView(it) }
        holder.findViewWithTag<View>(BANKS_TAG)?.let { holder.removeView(it) }

        // Remove the old one-off Persian calendar switch left by the previous implementation.
        holder.findViewWithTag<View>("persian_calendar_switch")?.let { holder.removeView(it) }

        val section = TextView(context).apply {
            tag = SECTION_TAG
            text = context.getString(R.string.hero_settings)
            textSize = 13f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(context, 28), dp(context, 18), dp(context, 28), dp(context, 8))
        }
        val calendarRow = LinearLayout(context).apply {
            tag = JALALI_TAG
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(context, 28), dp(context, 12), dp(context, 28), dp(context, 12))
            background = context.getDrawable(org.fossify.messages.R.drawable.ripple_background)
            isClickable = true
        }
        val calendarSwitch = MaterialSwitch(context).apply {
            text = context.getString(R.string.use_persian_calendar)
            isChecked = context.config.usePersianCalendar
            isClickable = false
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
            setPadding(dp(context, 28), dp(context, 16), dp(context, 28), dp(context, 16))
            background = context.getDrawable(org.fossify.messages.R.drawable.ripple_background)
            isClickable = true
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

        val generalLabel = holder.findViewById<View>(R.id.settings_general_settings_label)
        val index = if (generalLabel != null) holder.indexOfChild(generalLabel) else 0
        holder.addView(section, index)
        holder.addView(calendarRow, index + 1)
        holder.addView(bankRow, index + 2)
    }

    private fun dp(context: Context, value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()
}
