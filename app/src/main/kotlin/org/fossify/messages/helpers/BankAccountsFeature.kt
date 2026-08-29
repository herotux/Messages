package org.fossify.messages.helpers

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
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
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.fossify.commons.extensions.beGone
import org.fossify.commons.extensions.getProperPrimaryColor
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.messages.R
import org.fossify.messages.activities.BankCardScannerActivity
import org.fossify.messages.extensions.getMessagesDB
import org.fossify.messages.models.BankAccount
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlin.math.max

object BankAccountsFeature {
    private val cardRegex = Regex("(?<!\\d)(?:[0-9۰-۹]{4}[ -]?){3}[0-9۰-۹]{4}(?!\\d)")
    private val ibanRegex = Regex("(?i)\\bIR[0-9۰-۹]{24}\\b")
    private val holderRegex = Regex("(?i)(?:به\\s*نام|بنام|نام\\s*صاحب\\s*کارت|صاحب\\s*کارت|name)\\s*[:：-]?\\s*([\\p{L}][\\p{L} ._-]{2,39})")

    fun isPersian(context: Context): Boolean = Locale.getDefault().language == "fa" || Locale.getDefault().language == "ckb"
    fun title(context: Context, fa: String, en: String): String = if (isPersian(context)) fa else en

    fun normalizeDigits(value: String): String = buildString(value.length) {
        value.forEach { c -> append(when (c) {
            in '۰'..'۹' -> ('0'.code + c.code - '۰'.code).toChar()
            in '٠'..'٩' -> ('0'.code + c.code - '٠'.code).toChar()
            else -> c
        }) }
    }
    fun normalizeCard(value: String): String = normalizeDigits(value).filter(Char::isDigit)
    fun formatCard(value: String): String = normalizeCard(value).chunked(4).joinToString(" ")
    fun normalizeIban(value: String): String = normalizeDigits(value).replace(" ", "").replace("-", "").uppercase(Locale.US)
    fun formatIban(value: String): String = normalizeIban(value).chunked(4).joinToString(" ")

    fun getAccounts(context: Context): List<BankAccount> = try { context.getMessagesDB().BankAccountsDao().getAll() } catch (_: Exception) { emptyList() }

    fun save(context: Context, existing: BankAccount?, bankId: String, card: String, holder: String, iban: String) {
        val dao = context.getMessagesDB().BankAccountsDao(); val now = System.currentTimeMillis()
        val account = BankAccount(id = existing?.id ?: 0, bankId = bankId, cardNumber = normalizeCard(card), holderName = holder.trim(), iban = normalizeIban(iban), createdAt = existing?.createdAt ?: now, updatedAt = now)
        if (existing == null) dao.insert(account) else dao.update(account)
    }

    fun delete(context: Context, account: BankAccount) { try { context.getMessagesDB().BankAccountsDao().delete(account) } catch (_: Exception) { } }
    fun bankFor(account: BankAccount): IranianBankRegistry.BankInfo? = try { IranianBankRegistry.findById(IranianBankRegistry.BankId.valueOf(account.bankId)) } catch (_: Exception) { null }
    fun bankForCard(card: String): IranianBankRegistry.BankInfo? = IranianBankRegistry.findByCard(normalizeCard(card))
    fun cardRegexForScanner(text: String): String? = cardRegex.find(normalizeDigits(text))?.value?.let(::normalizeCard)?.takeIf(::validCard)
    fun extractHolderForScanner(text: String): String = holderRegex.find(text)?.groupValues?.getOrNull(1)?.trim().orEmpty()
    fun extractIbanForScanner(text: String): String = ibanRegex.find(normalizeDigits(text))?.value?.let(::normalizeIban).orEmpty()

    fun cardColor(bankId: String): Int = when (bankId) {
        "MELLAT" -> Color.rgb(165, 30, 45); "MELLI" -> Color.rgb(20, 75, 135); "TEJARAT" -> Color.rgb(0, 112, 175); "SADERAT" -> Color.rgb(0, 92, 155); "SEPAH" -> Color.rgb(205, 160, 35); "PASARGAD" -> Color.rgb(32, 65, 105); "PARSIAN" -> Color.rgb(20, 115, 110); "SAMAN" -> Color.rgb(20, 110, 145); "SHAHR" -> Color.rgb(80, 55, 125); else -> Color.rgb(70, 80, 95)
    }
    fun validCard(card: String): Boolean = IranianBankRegistry.isValidCardNumber(card)
    fun validIban(iban: String): Boolean = iban.isBlank() || IranianBankRegistry.isValidIban(iban)

    fun showBankAccountManager(context: Context) {
        val dialog = BottomSheetDialog(context)
        val root = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(context, 20), dp(context, 16), dp(context, 20), dp(context, 24)) }
        root.addView(TextView(context).apply { text = title(context, "کارت‌های بانکی", "Bank cards"); textSize = 20f; setTypeface(typeface, Typeface.BOLD); setPadding(0, 0, 0, dp(context, 12)) })
        val cardsHolder = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        root.addView(HorizontalScrollView(context).apply { isHorizontalScrollBarEnabled = false; addView(cardsHolder, ViewGroup.LayoutParams(-1, dp(context, 235))) })
        val actions = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        actions.addView(Button(context).apply { text = title(context, "افزودن کارت", "Add bank card"); setOnClickListener { showEditDialog(context, null) { refreshManager(context, cardsHolder, dialog) } } }, LinearLayout.LayoutParams(0, -2, 1f))
        actions.addView(Button(context).apply { text = title(context, "اسکن کارت", "Scan card"); setOnClickListener { if (context is Activity) context.startActivity(Intent(context, BankCardScannerActivity::class.java)) else Toast.makeText(context, title(context, "امکان باز کردن دوربین وجود ندارد", "Cannot open camera"), Toast.LENGTH_SHORT).show() } }, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(actions); dialog.setContentView(root); dialog.show(); refreshManager(context, cardsHolder, dialog)
    }

    private fun refreshManager(context: Context, holder: LinearLayout, dialog: BottomSheetDialog? = null) {
        ensureBackgroundThread { val accounts = getAccounts(context); context.runOnUiThreadCompat {
            holder.removeAllViews()
            if (accounts.isEmpty()) holder.addView(TextView(context).apply { text = title(context, "هنوز کارتی اضافه نشده است", "No bank cards added yet"); gravity = Gravity.CENTER; setTextColor(Color.GRAY) }, LinearLayout.LayoutParams(dp(context, 260), -1))
            else accounts.forEach { account -> holder.addView(createCardView(context, account) { showEditDialog(context, account) { refreshManager(context, holder, dialog) } }, LinearLayout.LayoutParams(dp(context, 330), dp(context, 220)).apply { marginEnd = dp(context, 12) }) }
        } }
    }

    private fun createCardView(context: Context, account: BankAccount, click: () -> Unit): View {
        val bank = bankFor(account)
        val card = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(context, 18), dp(context, 14), dp(context, 18), dp(context, 12)); background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(cardColor(account.bankId), darken(cardColor(account.bankId)))); setOnClickListener { click() }; isClickable = true; elevation = dp(context, 4).toFloat() }
        val top = LinearLayout(context).apply { gravity = Gravity.CENTER_VERTICAL }
        bank?.logoResourceName?.let { name -> val id = context.resources.getIdentifier(name, "drawable", context.packageName); if (id != 0) top.addView(ImageView(context).apply { setImageResource(id); adjustViewBounds = true }, LinearLayout.LayoutParams(dp(context, 40), dp(context, 40))) }
        top.addView(TextView(context).apply { text = bank?.let { title(context, it.persianName, it.englishName) } ?: account.bankId; textSize = 15f; setTypeface(typeface, Typeface.BOLD); setTextColor(Color.WHITE); setPadding(dp(context, 10), 0, 0, 0) }); card.addView(top)
        card.addView(TextView(context).apply { text = formatCard(account.cardNumber); textSize = 20f; letterSpacing = 0.06f; setTextColor(Color.WHITE); setTypeface(Typeface.MONOSPACE, Typeface.BOLD); gravity = Gravity.CENTER; textDirection = View.TEXT_DIRECTION_LTR; setPadding(0, dp(context, 16), 0, dp(context, 10)) })
        card.addView(TextView(context).apply { text = account.holderName.ifBlank { "—" }; textSize = 13f; setTextColor(Color.WHITE); setTypeface(typeface, Typeface.BOLD) })
        if (account.iban.isNotBlank()) card.addView(TextView(context).apply { text = formatIban(account.iban); textSize = 11f; textDirection = View.TEXT_DIRECTION_LTR; setTextColor(Color.WHITE); setPadding(0, dp(context, 5), 0, 0) })
        val actions = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.END }
        actions.addView(Button(context).apply { text = title(context, "کپی", "Copy"); setTextColor(Color.WHITE); setOnClickListener { copyToClipboard(context, account.cardNumber) } }, LinearLayout.LayoutParams(0, dp(context, 42), 1f))
        actions.addView(Button(context).apply { text = title(context, "QR کارت", "Card QR"); setTextColor(Color.WHITE); setOnClickListener { shareQr(context, account, false) } }, LinearLayout.LayoutParams(0, dp(context, 42), 1f))
        if (account.iban.isNotBlank()) actions.addView(Button(context).apply { text = title(context, "QR شبا", "IBAN QR"); setTextColor(Color.WHITE); setOnClickListener { shareQr(context, account, true) } }, LinearLayout.LayoutParams(0, dp(context, 42), 1f))
        card.addView(actions); return card
    }

    private fun showEditDialog(context: Context, existing: BankAccount?, onSaved: () -> Unit) {
        val layout = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(context, 8), 0, dp(context, 8), 0) }
        val card = EditText(context).apply { hint = title(context, "شماره کارت", "Card number"); inputType = android.text.InputType.TYPE_CLASS_NUMBER; setText(existing?.cardNumber?.let(::formatCard) ?: "") }
        val bankText = TextView(context).apply { textSize = 14f; setPadding(0, dp(context, 6), 0, dp(context, 6)) }
        val holder = EditText(context).apply { hint = title(context, "نام صاحب کارت", "Card holder name"); setText(existing?.holderName ?: "") }
        val iban = EditText(context).apply { hint = title(context, "شماره شبا (اختیاری)", "IBAN (optional)"); setText(existing?.iban?.let(::formatIban) ?: ""); inputType = android.text.InputType.TYPE_CLASS_TEXT }
        layout.addView(card); layout.addView(bankText); layout.addView(holder); layout.addView(iban)
        fun updateBank() { val bank = bankForCard(card.text.toString()); bankText.text = bank?.let { title(context, "بانک: ${it.persianName}", "Bank: ${it.englishName}") } ?: title(context, "بانک شناسایی نشد", "Bank not detected") }
        updateBank(); card.addTextChangedListenerCompat { updateBank() }
        val dialog = MaterialAlertDialogBuilder(context).setTitle(title(context, if (existing == null) "افزودن کارت بانکی" else "ویرایش کارت بانکی", if (existing == null) "Add bank card" else "Edit bank card")).setView(layout).setNegativeButton(title(context, "انصراف", "Cancel"), null).create()
        dialog.setButton(android.content.DialogInterface.BUTTON_POSITIVE, title(context, "ذخیره", "Save"), null as android.content.DialogInterface.OnClickListener?)
        dialog.setOnShowListener {
            dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                val normalizedCard = normalizeCard(card.text.toString()); val normalizedIban = normalizeIban(iban.text.toString()); val bank = bankForCard(normalizedCard)
                if (bank == null || !validCard(normalizedCard)) { Toast.makeText(context, title(context, "شماره کارت معتبر نیست یا بانک شناسایی نشد", "Invalid card number or bank not detected"), Toast.LENGTH_SHORT).show(); return@setOnClickListener }
                if (!validIban(normalizedIban)) { Toast.makeText(context, title(context, "شماره شبا معتبر نیست", "Invalid IBAN"), Toast.LENGTH_SHORT).show(); return@setOnClickListener }
                ensureBackgroundThread { try { save(context, existing, bank.id.name, normalizedCard, holder.text.toString(), normalizedIban); context.runOnUiThreadCompat { dialog.dismiss(); onSaved() } } catch (_: Exception) { context.runOnUiThreadCompat { Toast.makeText(context, title(context, "ذخیره کارت انجام نشد", "Could not save bank card"), Toast.LENGTH_SHORT).show() } } }
            }
        }
        dialog.show()
    }

    fun showCardPicker(context: Context, onSelected: (BankAccount) -> Unit) {
        val dialog = BottomSheetDialog(context); val root = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(context, 18), dp(context, 16), dp(context, 18), dp(context, 24)) }
        root.addView(TextView(context).apply { text = title(context, "انتخاب کارت بانکی", "Select bank card"); textSize = 20f; setTypeface(typeface, Typeface.BOLD); setPadding(0, 0, 0, dp(context, 12)) })
        val scroll = HorizontalScrollView(context).apply { isHorizontalScrollBarEnabled = false }; val holder = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }; scroll.addView(holder, ViewGroup.LayoutParams(-1, dp(context, 220))); root.addView(scroll); dialog.setContentView(root); dialog.show()
        ensureBackgroundThread { val accounts = getAccounts(context); context.runOnUiThreadCompat { accounts.forEach { account -> holder.addView(createCardView(context, account) { onSelected(account); dialog.dismiss() }, LinearLayout.LayoutParams(dp(context, 330), dp(context, 220)).apply { marginEnd = dp(context, 12) }) }; if (accounts.isEmpty()) holder.addView(TextView(context).apply { text = title(context, "ابتدا یک کارت در تنظیمات اضافه کنید", "Add a bank card in Settings first"); gravity = Gravity.CENTER }, LinearLayout.LayoutParams(-1, -1)) } }
    }

    fun addPickerButton(activity: Activity) {
        val picker = activity.findViewById<ViewGroup>(R.id.attachment_picker) ?: return; if (picker.findViewWithTag<View>("bank_card_picker_button") != null) return
        val button = LinearLayout(activity).apply { tag = "bank_card_picker_button"; orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; isClickable = true; background = ContextCompat.getDrawable(activity, R.drawable.ripple_background); setPadding(dp(activity, 12), dp(activity, 12), dp(activity, 12), dp(activity, 12)); addView(TextView(activity).apply { text = "💳"; textSize = 26f; gravity = Gravity.CENTER }, LinearLayout.LayoutParams(-1, dp(42))); addView(TextView(activity).apply { text = title(activity, "کارت بانکی", "Bank card"); textSize = 12f; gravity = Gravity.CENTER }, LinearLayout.LayoutParams(-1, -2)); setOnClickListener { showCardPicker(activity) { account -> val edit = activity.findViewById<EditText>(R.id.thread_type_message); if (edit != null) { val text = buildString { append(title(activity, "شماره کارت", "Card number")); append(": "); append(formatCard(account.cardNumber)); if (account.holderName.isNotBlank()) { append("\n"); append(title(activity, "به نام", "Name")); append(": "); append(account.holderName) }; if (account.iban.isNotBlank()) { append("\nIBAN: "); append(formatIban(account.iban)) } }; val start = edit.selectionStart.coerceAtLeast(0); val end = edit.selectionEnd.coerceAtLeast(0); edit.text.replace(minOf(start, end), max(start, end), text); edit.setSelection((minOf(start, end) + text.length).coerceAtMost(edit.length())) } } } }
        picker.addView(button, ViewGroup.LayoutParams(dp(activity, 96), dp(activity, 112))); val flow = picker.getChildAt(0); if (flow is androidx.constraintlayout.helper.widget.Flow) { val ids = flow.referencedIds.toMutableList(); ids.add(button.id.takeIf { it != View.NO_ID } ?: View.generateViewId().also { button.id = it }); flow.referencedIds = ids.toIntArray() }
    }

    fun installMessageCardLinks(activity: Activity) {
        val list = activity.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.thread_messages_list) ?: return; val listener = object : androidx.recyclerview.widget.RecyclerView.OnChildAttachStateChangeListener { override fun onChildViewAttachedToWindow(view: View) { decorateTree(view, activity) }; override fun onChildViewDetachedFromWindow(view: View) = Unit }; if (list.getTag(R.id.bank_card_feature_tag) != true) { list.addOnChildAttachStateChangeListener(listener); list.setTag(R.id.bank_card_feature_tag, true) }; for (i in 0 until list.childCount) decorateTree(list.getChildAt(i), activity)
    }
    private fun decorateTree(root: View, activity: Activity) { if (root is TextView) decorateTextView(root, activity); if (root is ViewGroup) for (i in 0 until root.childCount) decorateTree(root.getChildAt(i), activity) }
    private fun decorateTextView(view: TextView, activity: Activity) { val original = view.text?.toString() ?: return; val matches = cardRegex.findAll(original).toList(); if (matches.isEmpty()) return; val spannable = SpannableString(original); matches.forEach { match -> val normalized = normalizeCard(match.value); if (!validCard(normalized)) return@forEach; spannable.setSpan(object : ClickableSpan() { override fun onClick(widget: View) { showDetectedCard(activity, normalized, original) }; override fun updateDrawState(ds: android.text.TextPaint) { ds.isUnderlineText = false; ds.color = ContextCompat.getColor(activity, org.fossify.commons.R.color.color_primary) } }, match.range.first, match.range.last + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) }; view.text = spannable; view.movementMethod = LinkMovementMethod.getInstance(); view.highlightColor = Color.TRANSPARENT }
    private fun showDetectedCard(activity: Activity, card: String, message: String) { val bank = IranianBankRegistry.findByCard(card); val iban = ibanRegex.find(normalizeDigits(message))?.value; val holder = holderRegex.find(message)?.groupValues?.getOrNull(1)?.trim(); val root = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(activity, 18), dp(activity, 18), dp(activity, 18), dp(activity, 24)) }; root.addView(TextView(activity).apply { text = title(activity, "اطلاعات کارت", "Card information"); textSize = 20f; setTypeface(typeface, Typeface.BOLD); setPadding(0, 0, 0, dp(activity, 14)) }); root.addView(createCardView(activity, BankAccount(bankId = bank?.id?.name ?: "", cardNumber = card, holderName = holder ?: "", iban = iban ?: "")) {}); val dialog = BottomSheetDialog(activity); dialog.setContentView(root); dialog.show() }
    private fun copyToClipboard(context: Context, card: String) { val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager; clipboard.setPrimaryClip(ClipData.newPlainText("Card number", normalizeCard(card))); Toast.makeText(context, title(context, "شماره کارت کپی شد", "Card number copied"), Toast.LENGTH_SHORT).show() }
    private fun shareQr(context: Context, account: BankAccount, iban: Boolean) { val payload = if (iban) normalizeIban(account.iban) else normalizeCard(account.cardNumber); val bitmap = BankCardShareQr.create(context, payload) ?: return; try { val dir = File(context.cacheDir, "attachments").apply { mkdirs() }; val file = File(dir, "bank_${account.id}_${if (iban) "iban" else "card"}.png"); FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }; val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file); val share = Intent(Intent.ACTION_SEND).apply { type = "image/png"; putExtra(Intent.EXTRA_STREAM, uri); putExtra(Intent.EXTRA_TEXT, payload); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }; context.startActivity(Intent.createChooser(share, title(context, "اشتراک‌گذاری QR", "Share QR"))) } catch (_: Exception) { Toast.makeText(context, title(context, "اشتراک‌گذاری انجام نشد", "Could not share QR"), Toast.LENGTH_SHORT).show() } }
    private fun darken(color: Int): Int { val r = (Color.red(color) * .72).toInt(); val g = (Color.green(color) * .72).toInt(); val b = (Color.blue(color) * .72).toInt(); return Color.rgb(r, g, b) }
    fun dp(context: Context, value: Int): Int = (value * context.resources.displayMetrics.density).toInt()
}

private fun Context.runOnUiThreadCompat(action: () -> Unit) { if (this is Activity) runOnUiThread(action) else android.os.Handler(android.os.Looper.getMainLooper()).post(action) }
private fun EditText.addTextChangedListenerCompat(action: () -> Unit) { addTextChangedListener(object : android.text.TextWatcher { override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit; override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = action(); override fun afterTextChanged(s: android.text.Editable?) = Unit }) }
