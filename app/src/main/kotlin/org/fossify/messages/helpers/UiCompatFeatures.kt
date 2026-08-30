package org.fossify.messages.helpers

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.materialswitch.MaterialSwitch
import org.fossify.messages.R

/** Compatibility helpers kept separate from the feature implementations. */

fun BankAccountsFeature.settingsSection(context: Context): View {
    val density = context.resources.displayMetrics.density
    fun dp(value: Int) = (value * density).toInt()

    return LinearLayout(context).apply {
        tag = "bank_accounts_settings_section"
        orientation = LinearLayout.VERTICAL
        addView(TextView(context).apply {
            text = BankAccountsFeature.title(context, "کارت‌های بانکی", "Bank cards")
            textSize = 13f
            setTextColor(ContextCompat.getColor(context, org.fossify.commons.R.color.color_primary))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            gravity = Gravity.START
            setPadding(dp(16), dp(14), dp(16), dp(8))
        }, LinearLayout.LayoutParams(-1, -2))
        addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            isClickable = true
            isFocusable = true
            setPadding(dp(16), dp(8), dp(16), dp(8))
            background = ContextCompat.getDrawable(context, R.drawable.ripple_background)
            addView(ImageView(context).apply {
                setImageResource(R.drawable.ic_credit_card_24)
                contentDescription = BankAccountsFeature.title(context, "کارت بانکی", "Bank card")
            }, LinearLayout.LayoutParams(dp(40), dp(40)))
            addView(TextView(context).apply {
                text = BankAccountsFeature.title(context, "مدیریت کارت‌های بانکی", "Manage bank cards")
                textSize = 16f
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(12), 0, 0, 0)
            }, LinearLayout.LayoutParams(0, -2, 1f))
            addView(TextView(context).apply {
                text = "›"
                textSize = 26f
                gravity = Gravity.CENTER
            }, LinearLayout.LayoutParams(dp(32), dp(48)))
            setOnClickListener { BankAccountsFeature.showBankAccountManager(context) }
        }, LinearLayout.LayoutParams(-1, -2))
        addView(View(context).apply {
            setBackgroundColor(Color.argb(30, 128, 128, 128))
        }, LinearLayout.LayoutParams(-1, 1))
    }
}

/** Replaces the removed Context.refreshConversations extension. */
fun Context.refreshConversations() {
    if (this is Activity) recreate()
}
