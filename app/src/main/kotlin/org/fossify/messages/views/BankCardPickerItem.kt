package org.fossify.messages.views

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import org.fossify.messages.R
import org.fossify.messages.helpers.BankAccountsFeature
import org.fossify.messages.models.BankAccount

class BankCardPickerItem @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER
        val margin = resources.getDimensionPixelSize(org.fossify.commons.R.dimen.medium_margin)
        setPadding(margin, margin, margin, margin)
        minimumHeight = resources.getDimensionPixelSize(R.dimen.attachment_button_height)
        isClickable = true
        isFocusable = true

        val label = if (resources.configuration.locales[0].language.equals("fa", ignoreCase = true)) {
            "کارت بانکی"
        } else {
            "Bank card"
        }

        val primaryColor = resolveThemeColor(context, androidx.appcompat.R.attr.colorPrimary)

        val icon = AppCompatImageView(context).apply {
            layoutParams = LayoutParams(
                resources.getDimensionPixelSize(R.dimen.medium_icon_size),
                resources.getDimensionPixelSize(R.dimen.medium_icon_size)
            )
            setPadding(margin, margin, margin, margin)
            setImageResource(R.drawable.ic_credit_card_vector)
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL }
            backgroundTintList = primaryColor
            contentDescription = label
        }
        addView(icon)

        val text = AppCompatTextView(context).apply {
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = margin
            }
            gravity = Gravity.CENTER
            text = label
            textAlignment = TEXT_ALIGNMENT_CENTER
            setTextColor(resolveThemeColor(context, android.R.attr.textColorPrimary))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        }
        addView(text)

        setOnClickListener {
            BankAccountsFeature.showCardPicker(context) { account ->
                insertCardIntoComposer(context, account)
            }
        }
    }

    private fun insertCardIntoComposer(context: Context, account: BankAccount) {
        val activity = context as? Activity ?: return
        val messageId = resources.getIdentifier("thread_type_message", "id", context.packageName)
        if (messageId == 0) return
        val message = activity.findViewById<EditText>(messageId) ?: return

        val bank = BankAccountsFeature.bankFor(account)
        val isPersian = BankAccountsFeature.isPersian(context)
        val bankName = bank?.let { if (isPersian) it.persianName else it.englishName } ?: account.bankId
        val card = BankAccountsFeature.formatCard(account.cardNumber)
        val holder = account.holderName.trim()
        val iban = account.iban.trim()

        val selectedText = if (isPersian) {
            buildString {
                append("بانک: ").append(bankName)
                append("\nشماره کارت:")
                append("\n")
                append(card)
                if (holder.isNotEmpty()) append("\nصاحب کارت: ").append(holder)
                if (iban.isNotEmpty()) append("\nشماره شبا: ").append(BankAccountsFeature.formatIban(iban))
            }
        } else {
            buildString {
                append("Bank: ").append(bankName)
                append("\nCard number:")
                append("\n")
                append(card)
                if (holder.isNotEmpty()) append("\nCard holder: ").append(holder)
                if (iban.isNotEmpty()) append("\nIBAN: ").append(BankAccountsFeature.formatIban(iban))
            }
        }

        val existing = message.text?.toString().orEmpty()
        val newText = if (existing.isBlank()) selectedText else "$existing\n$selectedText"
        message.setText(newText)
        message.setSelection(newText.length)
        message.requestFocus()
    }

    private fun resolveThemeColor(context: Context, attr: Int): ColorStateList {
        val value = TypedValue()
        context.theme.resolveAttribute(attr, value, true)
        return if (value.resourceId != 0) {
            ContextCompat.getColorStateList(context, value.resourceId)
                ?: ColorStateList.valueOf(value.data)
        } else {
            ColorStateList.valueOf(value.data)
        }
    }
}
