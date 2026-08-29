package org.fossify.messages.helpers

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.fossify.messages.R
import org.fossify.messages.extensions.getMessagesDB
import org.fossify.messages.models.BankAccount
import java.util.Locale
import kotlin.math.max

object BankAccountsFeature {
    private val cardRegex = Regex("(?<!\\d)(?:[0-9۰-۹]{4}[ -]?){3}[0-9۰-۹]{4}(?!\\d)")
    private val ibanRegex = Regex("(?i)\\bIR[0-9۰-۹]{24}\\b")
    private val holderRegex = Regex("(?i)(?:به\\s*نام|بنام|نام\\s*صاحب\\s*کارت|صاحب\\s*کارت|name)\\s*[:：-]?\\s*([\\p{L}][\\p{L} ._-]{2,39})")

    fun isPersian(context: Context): Boolean = Locale.getDefault().language == "fa" || Locale.getDefault().language == "ckb"

    fun title(context: Context, fa: String, en: String): String = if (isPersian(context)) fa else en

    fun normalizeDigits(value: String): String = buildString(value.length) {
        value.forEach { c ->
            append(
                when (c) {
                    in '۰'..'۹' -> ('0'.code + c.code - '۰'.code).toChar()
                    in '٠'..'٩' -> ('0'.code + c.code - '٠'.code).toChar()
                    else -> c
                }
            )
        }
    }

    fun normalizeCard(value: String): String = normalizeDigits(value).filter(Char::isDigit)
    fun formatCard(value: String): String = normalizeCard(value).chunked(4).joinToString(" ")
    fun normalizeIban(value: String): String = normalizeDigits(value).replace(" ", "").replace("-", "").uppercase(Locale.US)
    fun formatIban(value: String): String = normalizeIban(value).chunked(4).joinToString(" ")

    fun getAccounts(context: Context): List<BankAccount> = try {
        context.getMessagesDB().BankAccountsDao().getAll()
    } catch (_: Exception) { emptyList() }

    fun save(context: Context, existing: BankAccount?, bankId: String, card: String, holder: String, iban: String) {
        val dao = context.getMessagesDB().BankAccountsDao()
        val now = System.currentTimeMillis()
        val account = BankAccount(
            id = existing?.id ?: 0,
            bankId = bankId,
            cardNumber = normalizeCard(card),
            holderName = holder.trim(),
            iban = normalizeIban(iban),
            createdAt = existing?.createdAt ?: now,
            updatedAt = now
        )
        if (existing == null) dao.insert(account) else dao.update(account)
    }

    fun delete(context: Context, account: BankAccount) {
        try { context.getMessagesDB().BankAccountsDao().delete(account) } catch (_: Exception) { }
    }

    fun bankFor(account: BankAccount): IranianBankRegistry.BankInfo? = try {
        IranianBankRegistry.findById(IranianBankRegistry.BankId.valueOf(account.bankId))
    } catch (_: Exception) { null }

    fun cardColor(bankId: String): Int = when (bankId) {
        "MELLAT" -> Color.rgb(165, 30, 45)
        "MELLI" -> Color.rgb(20, 75, 135)
        "TEJARAT" -> Color.rgb(0, 112, 175)
        "SADERAT" -> Color.rgb(0, 92, 155)
        "SEPAH" -> Color.rgb(205, 160, 35)
        "PASARGAD" -> Color.rgb(32, 65, 105)
        "PARSIAN" -> Color.rgb(20, 115, 110)
        "SAMAN" -> Color.rgb(20, 110, 145)
        "SHAHR" -> Color.rgb(80, 55, 125)
        else -> Color.rgb(70, 80, 95)
    }

    fun validCard(card: String): Boolean = IranianBankRegistry.isValidCardNumber(card)
    fun validIban(iban: String): Boolean = iban.isBlank() || IranianBankRegistry.isValidIban(iban)

    fun showBankAccountManager(context: Context) {
        val dialog = BottomSheetDialog(context)
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(context, 20), dp(context, 16), dp(context, 20), dp(context, 24))
        }
        val title = TextView(context).apply {
            text = title(context, "کارت‌های بانکی", "Bank cards")
            textSize = 20f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, 0, 0, dp(context, 12))
        }
        root.addView(title)

        val cardsHolder = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val scroll = HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            addView(cardsHolder, ViewGroup.LayoutParams(-1, dp(context, 220)))
        }
        root.addView(scroll)

        val actions = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val add = Button(context).apply {
            text = title(context, "افزودن کارت", "Add bank card")
            setOnClickListener { showEditDialog(context, null) { refreshManager(context, cardsHolder) } }
        }
        actions.addView(add, LinearLayout.LayoutParams(-1, -2))
        root.addView(actions)

        dialog.setContentView(root)
        refreshManager(context, cardsHolder, dialog)
        dialog.show()
    }

    private fun refreshManager(context: Context, holder: LinearLayout, dialog: BottomSheetDialog? = null) {
        holder.removeAllViews()
        val accounts = getAccounts(context)
        if (accounts.isEmpty()) {
            holder.addView(TextView(context).apply {
                text = title(context, "هنوز کارتی اضافه نشده است", "No bank cards added yet")
                gravity = Gravity.CENTER
                setTextColor(Color.GRAY)
            }, LinearLayout.LayoutParams(dp(context, 260), -1))
            return
        }
        accounts.forEach { account ->
            holder.addView(createCardView(context, account) {
                showEditDialog(context, account) { refreshManager(context, holder, dialog) }
            }, LinearLayout.LayoutParams(dp(context, 310), dp(context, 190)).apply { marginEnd = dp(context, 12) })
        }
    }

    private fun createCardView(context: Context, account: BankAccount, click: () -> Unit): View {
        val bank = bankFor(account)
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(context, 18), dp(context, 14), dp(context, 18), dp(context, 14))
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(cardColor(account.bankId), darken(cardColor(account.bankId))))
            setOnClickListener { click() }
            isClickable = true
            elevation = dp(context, 4).toFloat()
        }
        val top = LinearLayout(context).apply { gravity = Gravity.CENTER_VERTICAL }
        bank?.logoResourceName?.let { name ->
            val id = context.resources.getIdentifier(name, "drawable", context.packageName)
            if (id != 0) top.addView(ImageView(context).apply { setImageResource(id); adjustViewBounds = true; maxWidth = dp(context, 38); maxHeight = dp(context, 38) }, LinearLayout.LayoutParams(dp(context, 40), dp(context, 40)))
        }
        top.addView(TextView(context).apply {
            text = bank?.let { title(context, it.persianName, it.englishName) } ?: account.bankId
            textSize = 15f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.WHITE)
            setPadding(dp(context, 10), 0, 0, 0)
        })
        card.addView(top)
        card.addView(TextView(context).apply {
            text = formatCard(account.cardNumber)
            textSize = 20f
            letterSpacing = 0.06f
            setTextColor(Color.WHITE)
            setTypeface(Typeface.MONOSPACE, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, dp(context, 18), 0, dp(context, 12))
        })
        card.addView(TextView(context).apply { text = account.holderName.ifBlank { "—" }; textSize = 13f; setTextColor(Color.WHITE); setTypeface(typeface, Typeface.BOLD) })
        if (account.iban.isNotBlank()) card.addView(TextView(context).apply { text = formatIban(account.iban); textSize = 11f; setTextColor(Color.WHITE); setPadding(0, dp(context, 5), 0, 0) })
        return card
    }

    private fun showEditDialog(context: Context, existing: BankAccount?, onSaved: () -> Unit) {
        val layout = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(context, 8), 0, dp(context, 8), 0) }
        val bankButton = Button(context).apply { text = title(context, "انتخاب بانک", "Select bank") }
        val card = EditText(context).apply { hint = title(context, "شماره کارت", "Card number"); inputType = android.text.InputType.TYPE_CLASS_NUMBER; setText(existing?.cardNumber?.let(::formatCard) ?: "") }
        val holder = EditText(context).apply { hint = title(context, "نام صاحب کارت", "Card holder name"); setText(existing?.holderName ?: "") }
        val iban = EditText(context).apply { hint = title(context, "شماره شبا (اختیاری)", "IBAN (optional)"); setText(existing?.iban?.let(::formatIban) ?: ""); inputType = android.text.InputType.TYPE_CLASS_TEXT }
        layout.addView(bankButton); layout.addView(card); layout.addView(holder); layout.addView(iban)
        var selected = bankFor(existing ?: BankAccount(bankId = "MELLI", cardNumber = "", holderName = "", iban = ""))
        if (selected != null) bankButton.text = title(context, selected!!.persianName, selected!!.englishName)
        bankButton.setOnClickListener {
            val banks = IranianBankRegistry.allBanks()
            val names = banks.map { title(context, it.persianName, it.englishName) }.toTypedArray()
            MaterialAlertDialogBuilder(context).setTitle(title(context, "انتخاب بانک", "Select bank")).setItems(names) { _, which -> selected = banks[which]; bankButton.text = names[which] }.show()
        }
        MaterialAlertDialogBuilder(context)
            .setTitle(title(context, if (existing == null) "افزودن کارت بانکی" else "ویرایش کارت بانکی", if (existing == null) "Add bank card" else "Edit bank card"))
            .setView(layout)
            .setNegativeButton(title(context, "انصراف", "Cancel"), null)
            .setPositiveButton(title(context, "ذخیره", "Save")) { _, _ ->
                val normalizedCard = normalizeCard(card.text.toString())
                val normalizedIban = normalizeIban(iban.text.toString())
                val bank = selected
                if (bank == null || !validCard(normalizedCard)) {
                    android.widget.Toast.makeText(context, title(context, "شماره کارت معتبر نیست", "Invalid card number"), android.widget.Toast.LENGTH_SHORT).show()
                } else if (!validIban(normalizedIban)) {
                    android.widget.Toast.makeText(context, title(context, "شماره شبا معتبر نیست", "Invalid IBAN"), android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    save(context, existing, bank!!.id.name, normalizedCard, holder.text.toString(), normalizedIban)
                    onSaved()
                }
            }.show()
    }

    fun showCardPicker(context: Context, onSelected: (BankAccount) -> Unit) {
        val dialog = BottomSheetDialog(context)
        val root = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(context, 18), dp(context, 16), dp(context, 18), dp(context, 24)) }
        root.addView(TextView(context).apply { text = title(context, "انتخاب کارت بانکی", "Select bank card"); textSize = 20f; setTypeface(typeface, Typeface.BOLD); setPadding(0, 0, 0, dp(context, 12)) })
        val accounts = getAccounts(context)
        val scroll = HorizontalScrollView(context).apply { isHorizontalScrollBarEnabled = false }
        val holder = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        scroll.addView(holder, ViewGroup.LayoutParams(-1, dp(context, 210)))
        accounts.forEach { account -> holder.addView(createCardView(context, account) { onSelected(account); dialog.dismiss() }, LinearLayout.LayoutParams(dp(context, 310), dp(context, 190)).apply { marginEnd = dp(context, 12) }) }
        if (accounts.isEmpty()) holder.addView(TextView(context).apply { text = title(context, "ابتدا یک کارت در تنظیمات اضافه کنید", "Add a bank card in Settings first"); gravity = Gravity.CENTER }, LinearLayout.LayoutParams(-1, -1))
        root.addView(scroll)
        dialog.setContentView(root)
        dialog.show()
    }

    fun addPickerButton(activity: Activity) {
        val picker = activity.findViewById<ViewGroup>(R.id.attachment_picker) ?: return
        if (picker.findViewWithTag<View>("bank_card_picker_button") != null) return
        val button = LinearLayout(activity).apply {
            tag = "bank_card_picker_button"
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            isClickable = true
            background = ContextCompat.getDrawable(activity, R.drawable.ripple_background)
            setPadding(dp(activity, 12), dp(activity, 12), dp(activity, 12), dp(activity, 12))
            val icon = TextView(activity).apply { text = "💳"; textSize = 26f; gravity = Gravity.CENTER }
            val label = TextView(activity).apply { text = title(activity, "کارت بانکی", "Bank card"); textSize = 12f; gravity = Gravity.CENTER }
            addView(icon, LinearLayout.LayoutParams(-1, dp(activity, 42)))
            addView(label, LinearLayout.LayoutParams(-1, -2))
            setOnClickListener {
                showCardPicker(activity) { account ->
                    val edit = activity.findViewById<EditText>(R.id.thread_type_message)
                    if (edit != null) {
                        val text = buildString {
                            append(title(activity, "شماره کارت", "Card number")); append(": "); append(formatCard(account.cardNumber))
                            if (account.holderName.isNotBlank()) { append("\n"); append(title(activity, "به نام", "Name")); append(": "); append(account.holderName) }
                            if (account.iban.isNotBlank()) { append("\n"); append("IBAN: "); append(formatIban(account.iban)) }
                        }
                        val start = edit.selectionStart.coerceAtLeast(0)
                        val end = edit.selectionEnd.coerceAtLeast(0)
                        edit.text.replace(minOf(start, end), max(start, end), text)
                        edit.setSelection((minOf(start, end) + text.length).coerceAtMost(edit.length()))
                    }
                }
            }
        }
        val size = dp(activity, 96)
        picker.addView(button, ViewGroup.LayoutParams(size, dp(activity, 112)))
        val flow = picker.getChildAt(0)
        if (flow is androidx.constraintlayout.helper.widget.Flow) {
            val ids = flow.referencedIds.toMutableList()
            ids.add(button.id.takeIf { it != View.NO_ID } ?: View.generateViewId().also { button.id = it })
            flow.referencedIds = ids.toIntArray()
        }
    }

    fun installMessageCardLinks(activity: Activity) {
        val list = activity.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.thread_messages_list) ?: return
        val listener = object : androidx.recyclerview.widget.RecyclerView.OnChildAttachStateChangeListener {
            override fun onChildViewAttachedToWindow(view: View) { decorateTree(view, activity) }
            override fun onChildViewDetachedFromWindow(view: View) = Unit
        }
        if (list.getTag(R.id.bank_card_feature_tag) != true) {
            list.addOnChildAttachStateChangeListener(listener)
            list.setTag(R.id.bank_card_feature_tag, true)
        }
        for (i in 0 until list.childCount) decorateTree(list.getChildAt(i), activity)
    }

    private fun decorateTree(root: View, activity: Activity) {
        if (root is TextView) decorateTextView(root, activity)
        if (root is ViewGroup) for (i in 0 until root.childCount) decorateTree(root.getChildAt(i), activity)
    }

    private fun decorateTextView(view: TextView, activity: Activity) {
        val original = view.text?.toString() ?: return
        val matches = cardRegex.findAll(original).toList()
        if (matches.isEmpty()) return
        val spannable = SpannableString(original)
        matches.forEach { match ->
            val normalized = normalizeCard(match.value)
            if (!validCard(normalized)) return@forEach
            spannable.setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) { showDetectedCard(activity, normalized, original) }
                override fun updateDrawState(ds: android.text.TextPaint) { ds.isUnderlineText = false; ds.color = ContextCompat.getColor(activity, org.fossify.commons.R.color.color_primary) }
            }, match.range.first, match.range.last + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        view.text = spannable
        view.movementMethod = LinkMovementMethod.getInstance()
        view.highlightColor = Color.TRANSPARENT
    }

    private fun showDetectedCard(activity: Activity, card: String, message: String) {
        val bank = IranianBankRegistry.findByCard(card)
        val iban = ibanRegex.find(normalizeDigits(message))?.value
        val holder = holderRegex.find(message)?.groupValues?.getOrNull(1)?.trim()
        val root = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(activity, 18), dp(activity, 18), dp(activity, 18), dp(activity, 24)) }
        root.addView(TextView(activity).apply { text = title(activity, "اطلاعات کارت", "Card information"); textSize = 20f; setTypeface(typeface, Typeface.BOLD); setPadding(0, 0, 0, dp(activity, 14)) })
        val account = BankAccount(bankId = bank?.id?.name ?: "", cardNumber = card, holderName = holder ?: "", iban = iban ?: "")
        root.addView(createCardView(activity, account) {})
        if (iban != null) root.addView(TextView(activity).apply { text = "IBAN: ${formatIban(iban)}"; textSize = 14f; setPadding(0, dp(activity, 12), 0, 0) })
        if (holder != null) root.addView(TextView(activity).apply { text = title(activity, "نام صاحب حساب: $holder", "Account holder: $holder"); textSize = 14f; setPadding(0, dp(activity, 8), 0, 0) })
        val dialog = BottomSheetDialog(activity)
        dialog.setContentView(root)
        dialog.show()
    }

    fun settingsSection(activity: Activity): View {
        val section = LinearLayout(activity).apply {
            tag = "bank_accounts_settings_section"
            orientation = LinearLayout.VERTICAL
            setPadding(dp(activity, 16), dp(activity, 14), dp(activity, 16), 0)
        }
        section.addView(TextView(activity).apply {
            text = title(activity, "کارت‌های بانکی", "Bank cards")
            textSize = 13f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(activity.getProperPrimaryColor())
            setPadding(0, 0, 0, dp(activity, 8))
        })
        val row = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = ContextCompat.getDrawable(activity, R.drawable.ripple_background)
            setPadding(0, 0, 0, dp(activity, 8))
            setOnClickListener { showBankAccountManager(activity) }
        }
        row.addView(TextView(activity).apply { text = title(activity, "مدیریت کارت‌های بانکی و شماره شبا", "Manage bank cards and IBAN"); textSize = 16f; setTextColor(activity.getProperTextColor()) })
        row.addView(TextView(activity).apply { text = title(activity, "افزودن، ویرایش و حذف کارت‌ها", "Add, edit and delete cards"); textSize = 12f; setTextColor(Color.GRAY); setPadding(0, dp(context = activity, value = 4), 0, 0) })
        section.addView(row, LinearLayout.LayoutParams(-1, -2))
        val divider = View(activity).apply { setBackgroundColor(Color.argb(30, 128, 128, 128)) }
        section.addView(divider, LinearLayout.LayoutParams(-1, 1))
        return section
    }

    private fun darken(color: Int): Int {
        val r = (Color.red(color) * .72).toInt(); val g = (Color.green(color) * .72).toInt(); val b = (Color.blue(color) * .72).toInt()
        return Color.rgb(r, g, b)
    }

    fun dp(context: Context, value: Int): Int = (value * context.resources.displayMetrics.density).toInt()
}
