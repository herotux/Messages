package org.fossify.messages.activities

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import org.fossify.messages.features.bankcards.BankCard
import org.fossify.messages.features.bankcards.BankCardsRepository

/** Dedicated bank-card UI. No legacy Common activity hierarchy is used. */
class BankCardsActivity : AppCompatActivity() {
    private lateinit var repository: BankCardsRepository
    private lateinit var pager: ViewPager2
    private lateinit var adapter: CardPagerAdapter
    private var cards = mutableListOf<BankCard>()
    private var wizard: CardWizard? = null

    private val scannerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
        val data = result.data ?: return@registerForActivityResult
        val card = data.getStringExtra(BankCardScannerActivity.EXTRA_CARD).orEmpty()
        if (card.isBlank()) return@registerForActivityResult
        wizard?.applyScanResult(card, data.getStringExtra(BankCardScannerActivity.EXTRA_HOLDER).orEmpty(), data.getStringExtra(BankCardScannerActivity.EXTRA_IBAN).orEmpty())
    }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        repository = BankCardsRepository(this)
        buildUi()
        loadCards()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL; setBackgroundColor(resolveColor(com.google.android.material.R.attr.colorSurface, Color.WHITE)) }
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(dp(16), bars.top + dp(4), dp(16), bars.bottom + dp(12)); insets
        }

        val toolbar = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL }
        toolbar.addView(iconButton(android.R.drawable.ic_menu_revert, "بازگشت") { finish() }, lp(48, 48))
        toolbar.addView(TextView(this).apply { text = "کارت‌های بانکی"; textSize = 21f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER_VERTICAL; setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurface, Color.BLACK)) }, LinearLayout.LayoutParams(0, dp(56), 1f))
        toolbar.addView(iconButton(android.R.drawable.ic_menu_more, "گزینه‌ها") { showTopMenu() }, lp(48, 48))
        root.addView(toolbar, lp(-1, 56))

        pager = ViewPager2(this).apply {
            orientation = ViewPager2.ORIENTATION_HORIZONTAL; clipToPadding = false; clipChildren = false; offscreenPageLimit = 2
            setPadding(dp(2), dp(10), dp(2), dp(6))
            setPageTransformer { page, position -> val d = kotlin.math.abs(position).coerceAtMost(1f); page.scaleY = 1f - d * .04f; page.alpha = 1f - d * .16f; page.translationX = position * -dp(6).toFloat() }
        }
        adapter = CardPagerAdapter(); pager.adapter = adapter
        root.addView(pager, LinearLayout.LayoutParams(-1, dp(270)))
        root.addView(TextView(this).apply { text = "برای دیدن کارت‌های دیگر به چپ یا راست بکشید"; textSize = 12f; gravity = Gravity.CENTER; setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurfaceVariant, Color.GRAY)) }, lp(-1, 30))
        root.addView(actionTile(android.R.drawable.ic_input_add, "افزودن کارت جدید", "ثبت کارت بانکی") { showWizard(null) }, LinearLayout.LayoutParams(-1, dp(68)).apply { topMargin = dp(8) })
        root.addView(TextView(this).apply { text = "برای عملیات کارت روی کارت یا ⋮ بزنید"; textSize = 11f; gravity = Gravity.CENTER; setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurfaceVariant, Color.GRAY)) }, lp(-1, 30))
        setContentView(root)
    }

    private fun loadCards() { Thread { val loaded = repository.getCards(); runOnUiThread { cards = loaded.toMutableList(); adapter.notifyDataSetChanged(); pager.setCurrentItem(pager.currentItem.coerceAtMost((cards.size - 1).coerceAtLeast(0)), false) } }.start() }

    private inner class CardPagerAdapter : RecyclerView.Adapter<CardPagerAdapter.Holder>() {
        inner class Holder(val page: FrameLayout) : RecyclerView.ViewHolder(page)
        override fun getItemCount() = cards.size
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(FrameLayout(parent.context).apply { layoutParams = RecyclerView.LayoutParams(-1, -1) })
        override fun onBindViewHolder(holder: Holder, position: Int) { holder.page.removeAllViews(); if (position < cards.size) holder.page.addView(createBankCard(cards[position]), FrameLayout.LayoutParams(-1, -1).apply { leftMargin = dp(12); rightMargin = dp(12); topMargin = dp(2); bottomMargin = dp(6) }) }
    }

    private fun createBankCard(card: BankCard): View {
        val visual = card.visual; val base = visual?.color ?: Color.rgb(60, 72, 88)
        val outer = MaterialCardView(this).apply { radius = dp(22).toFloat(); cardElevation = dp(8).toFloat(); strokeWidth = dp(1); strokeColor = Color.argb(55, 255, 255, 255); isClickable = true; isFocusable = true; setOnClickListener { showCardActions(card) } }
        val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL; setPadding(dp(22), dp(18), dp(22), dp(14)); background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(lighten(base), darken(base))).apply { cornerRadius = dp(22).toFloat() } }
        val top = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL }
        val logo = ImageView(this).apply { visual?.logoResourceName?.let { n -> resources.getIdentifier(n, "drawable", packageName).takeIf { it != 0 }?.let(::setImageResource) }; scaleType = ImageView.ScaleType.CENTER_INSIDE; contentDescription = visual?.persianName ?: "بانک" }
        top.addView(logo, lp(42, 42))
        top.addView(TextView(this).apply { text = visual?.persianName ?: card.bankId; textSize = 16f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE); setPadding(dp(10), 0, 0, 0) }, LinearLayout.LayoutParams(0, dp(42), 1f))
        val more = iconButton(android.R.drawable.ic_menu_more, "عملیات کارت") { showCardActions(card) }; tint(more, Color.WHITE); top.addView(more, lp(42, 42)); body.addView(top)
        body.addView(TextView(this).apply { text = repository.normalizeCard(card.cardNumber).chunked(4).joinToString("   "); textSize = 21f; typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD); letterSpacing = .035f; gravity = Gravity.CENTER; textDirection = View.TEXT_DIRECTION_LTR; setSingleLine(true); includeFontPadding = false; setTextColor(Color.WHITE) }, LinearLayout.LayoutParams(-1, dp(70)).apply { topMargin = dp(24) })
        val bottom = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL }
        bottom.addView(TextView(this).apply { text = card.holderName.ifBlank { "نام صاحب کارت" }; textSize = 14f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE); maxLines = 1 }, LinearLayout.LayoutParams(0, dp(28), 1f))
        if (card.iban.isNotBlank()) bottom.addView(TextView(this).apply { text = "شبا"; textSize = 10f; gravity = Gravity.CENTER; setTextColor(Color.argb(195, 255, 255, 255)) }, lp(36, 28))
        body.addView(bottom); outer.addView(body, ViewGroup.LayoutParams(-1, -1)); return outer
    }

    private fun showTopMenu() { val sheet = BottomSheetDialog(this); val root = sheetRoot("مدیریت کارت‌ها"); root.addView(actionTile(android.R.drawable.ic_input_add, "افزودن کارت جدید", "ثبت کارت بانکی") { sheet.dismiss(); showWizard(null) }, lp(-1, 68).apply { topMargin = dp(10) }); root.addView(actionTile(android.R.drawable.ic_menu_sort_by_size, "مرتب‌سازی کارت‌ها", "تغییر ترتیب نمایش") { sheet.dismiss(); showSort() }, lp(-1, 68).apply { topMargin = dp(8) }); sheet.setContentView(root); sheet.show() }

    private fun showSort() { val sheet = BottomSheetDialog(this); val root = sheetRoot("مرتب‌سازی کارت‌ها"); cards.forEachIndexed { index, card -> val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL }; row.addView(TextView(this).apply { text = card.visual?.persianName ?: card.bankId; textSize = 15f; setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurface, Color.BLACK)) }, LinearLayout.LayoutParams(0, 52.dp(), 1f)); row.addView(iconButton(android.R.drawable.arrow_up_float, "بالا") { if (index > 0) { moveCard(index, index - 1); sheet.dismiss(); showSort() } }, lp(48, 48)); row.addView(iconButton(android.R.drawable.arrow_down_float, "پایین") { if (index < cards.lastIndex) { moveCard(index, index + 1); sheet.dismiss(); showSort() } }, lp(48, 48)); root.addView(row) }; sheet.setContentView(root); sheet.show() }
    private fun moveCard(from: Int, to: Int) { if (from !in cards.indices || to !in cards.indices) return; val c = cards.removeAt(from); cards.add(to, c); adapter.notifyItemMoved(from, to); pager.setCurrentItem(to, false); Thread { repository.reorder(cards) }.start() }

    private fun showCardActions(card: BankCard) { val sheet = BottomSheetDialog(this); val root = sheetRoot(card.visual?.persianName ?: "کارت بانکی"); root.addView(actionTile(android.R.drawable.ic_menu_edit, "ویرایش کارت", "نام صاحب کارت یا شبا") { sheet.dismiss(); showWizard(card) }, lp(-1, 66).apply { topMargin = dp(10) }); root.addView(actionTile(android.R.drawable.ic_menu_save, "کپی شماره کارت", repository.normalizeCard(card.cardNumber)) { copy(repository.normalizeCard(card.cardNumber)); sheet.dismiss() }, lp(-1, 66).apply { topMargin = dp(8) }); if (card.iban.isNotBlank()) root.addView(actionTile(android.R.drawable.ic_menu_save, "کپی شماره شبا", repository.normalizeIban(card.iban)) { copy(repository.normalizeIban(card.iban)); sheet.dismiss() }, lp(-1, 66).apply { topMargin = dp(8) }); if (card.holderName.isNotBlank()) root.addView(actionTile(android.R.drawable.ic_menu_save, "کپی نام صاحب کارت", card.holderName) { copy(card.holderName); sheet.dismiss() }, lp(-1, 66).apply { topMargin = dp(8) }); root.addView(actionTile(android.R.drawable.ic_menu_share, "اشتراک‌گذاری", "ارسال اطلاعات کارت") { sheet.dismiss(); share(card) }, lp(-1, 66).apply { topMargin = dp(8) }); root.addView(actionTile(android.R.drawable.ic_menu_delete, "حذف کارت", "حذف از برنامه") { sheet.dismiss(); confirmDelete(card) }, lp(-1, 66).apply { topMargin = dp(8) }); sheet.setContentView(root); sheet.show() }

    private fun confirmDelete(card: BankCard) { MaterialAlertDialogBuilder(this).setTitle("حذف کارت").setMessage("این کارت حذف شود؟").setNegativeButton("انصراف", null).setPositiveButton("حذف") { _, _ -> Thread { repository.delete(card); runOnUiThread { loadCards() } }.start() }.show() }
    private fun share(card: BankCard) { val text = buildString { append(card.visual?.persianName ?: "کارت بانکی").append('\n'); append("شماره کارت: ").append(repository.normalizeCard(card.cardNumber)).append('\n'); if (card.holderName.isNotBlank()) append("صاحب کارت: ").append(card.holderName).append('\n'); if (card.iban.isNotBlank()) append("شبا: ").append(repository.normalizeIban(card.iban)) }; startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }, "اشتراک‌گذاری کارت")) }
    private fun copy(value: String) { (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Messages", value)); Toast.makeText(this, "کپی شد", Toast.LENGTH_SHORT).show() }

    private fun showWizard(existing: BankCard?) { wizard?.dismiss(); wizard = CardWizard(existing).also { it.show() } }
    private inner class CardWizard(private val existing: BankCard?) {
        private val sheet = BottomSheetDialog(this@BankCardsActivity)
        private val root = LinearLayout(this@BankCardsActivity).apply { orientation = LinearLayout.VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL; setPadding(dp(20), dp(10), dp(20), dp(26)) }
        private val groups = Array(4) { EditText(this@BankCardsActivity).apply { inputType = InputType.TYPE_CLASS_NUMBER; gravity = Gravity.CENTER; textSize = 17f; hint = "••••"; maxLines = 1 } }
        private val holder = TextInputEditText(this@BankCardsActivity)
        private val iban = TextInputEditText(this@BankCardsActivity)
        private var preview: MaterialCardView? = null
        fun show() { buildCardStep(); sheet.setContentView(root); sheet.show(); if (existing == null) pasteClipboard() else setCard(existing.cardNumber) }
        fun dismiss() = sheet.dismiss()
        private fun header(title: String) { root.removeAllViews(); val row = LinearLayout(this@BankCardsActivity).apply { gravity = Gravity.CENTER_VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL }; row.addView(TextView(this@BankCardsActivity).apply { text = title; textSize = 20f; typeface = Typeface.DEFAULT_BOLD; setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurface, Color.BLACK)) }, LinearLayout.LayoutParams(0, 48.dp(), 1f)); val scan = iconButton(android.R.drawable.ic_menu_camera, "اسکن کارت") { scannerLauncher.launch(Intent(this@BankCardsActivity, BankCardScannerActivity::class.java).putExtra(BankCardScannerActivity.EXTRA_RETURN_TO_FORM, true)) }; tint(scan, resolveColor(androidx.appcompat.R.attr.colorPrimary, Color.DKGRAY)); row.addView(scan, lp(48, 48)); root.addView(row) }
        private fun buildCardStep() { header("افزودن کارت · ۱ از ۳"); addPreview(); val row = LinearLayout(this@BankCardsActivity).apply { gravity = Gravity.CENTER; layoutDirection = View.LAYOUT_DIRECTION_LTR }; groups.forEachIndexed { index, edit -> edit.setText(""); row.addView(edit, LinearLayout.LayoutParams(0, 54.dp(), 1f).apply { if (index < 3) marginEnd = dp(6) }); edit.addTextChangedListener(SimpleTextWatcher { updatePreview() }) }; root.addView(row, lp(-1, 58).apply { topMargin = dp(12) }); root.addView(actionTile(android.R.drawable.ic_media_next, "ادامه", "بررسی شماره کارت") { if (validCard()) buildHolderStep() }, lp(-1, 64).apply { topMargin = dp(12) }); updatePreview() }
        private fun buildHolderStep() { header("افزودن کارت · ۲ از ۳"); addPreview(); root.addView(TextInputLayout(this@BankCardsActivity).apply { hint = "نام صاحب کارت (اختیاری)"; addView(holder) }, lp(-1, 72).apply { topMargin = dp(12) }); root.addView(actionTile(android.R.drawable.ic_media_next, "ادامه", "شماره شبا در مرحله بعد") { buildIbanStep() }, lp(-1, 64).apply { topMargin = dp(12) }); updatePreview() }
        private fun buildIbanStep() { header("افزودن کارت · ۳ از ۳"); addPreview(); root.addView(TextInputLayout(this@BankCardsActivity).apply { hint = "شماره شبا (اختیاری)"; addView(iban) }, lp(-1, 72).apply { topMargin = dp(12) }); root.addView(actionTile(android.R.drawable.ic_menu_save, "ذخیره کارت", "ثبت کارت در برنامه") { save() }, lp(-1, 64).apply { topMargin = dp(12) }); updatePreview() }
        private fun addPreview() { preview = MaterialCardView(this@BankCardsActivity).apply { radius = dp(20).toFloat(); cardElevation = dp(4).toFloat() }; root.addView(preview, lp(-1, 188).apply { topMargin = dp(10) }) }
        private fun updatePreview() { val number = groups.joinToString("") { repository.normalizeCard(it.text.toString()) }; val visual = repository.detect(number); val base = visual?.color ?: 0xff46505f.toInt(); val body = LinearLayout(this@BankCardsActivity).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(dp(20), dp(16), dp(20), dp(12)); background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(lighten(base), darken(base))).apply { cornerRadius = dp(20).toFloat() } }; body.addView(TextView(this@BankCardsActivity).apply { text = visual?.persianName ?: "بانک را تشخیص می‌دهیم"; textSize = 14f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE) }); body.addView(TextView(this@BankCardsActivity).apply { text = number.chunked(4).joinToString("   ").ifBlank { "••••   ••••   ••••   ••••" }; textSize = 20f; typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD); gravity = Gravity.CENTER; textDirection = View.TEXT_DIRECTION_LTR; setSingleLine(true); setTextColor(Color.WHITE); setPadding(0, dp(30), 0, dp(14)) }); body.addView(TextView(this@BankCardsActivity).apply { text = holder.text?.toString().orEmpty().ifBlank { "نام صاحب کارت" }; textSize = 12f; setTextColor(Color.WHITE) }); preview?.removeAllViews(); preview?.addView(body, ViewGroup.LayoutParams(-1, -1)) }
        private fun validCard(): Boolean { val v = groups.joinToString("") { repository.normalizeCard(it.text.toString()) }; val ok = v.length == 16 && repository.validCard(v) && repository.detect(v) != null; if (!ok) Toast.makeText(this@BankCardsActivity, "شماره کارت معتبر نیست یا بانک شناسایی نشد", Toast.LENGTH_SHORT).show(); return ok }
        private fun save() { val v = groups.joinToString("") { repository.normalizeCard(it.text.toString()) }; val i = repository.normalizeIban(iban.text?.toString().orEmpty()); if (!validCard()) return; if (!repository.validIban(i)) { Toast.makeText(this@BankCardsActivity, "شماره شبا معتبر نیست", Toast.LENGTH_SHORT).show(); return }; Thread { val result = repository.save(existing, v, holder.text?.toString().orEmpty(), i); runOnUiThread { if (result.isSuccess) { sheet.dismiss(); wizard = null; loadCards() } else Toast.makeText(this@BankCardsActivity, "ذخیره کارت انجام نشد", Toast.LENGTH_SHORT).show() } }.start() }
        private fun pasteClipboard() { val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager; val v = repository.normalizeCard(cm.primaryClip?.getItemAt(0)?.coerceToText(this@BankCardsActivity)?.toString().orEmpty()); if (v.length == 16 && repository.detect(v) != null) setCard(v) }
        private fun setCard(v: String) { v.chunked(4).take(4).forEachIndexed { i, part -> groups[i].setText(part) }; updatePreview() }
        fun applyScanResult(card: String, holderName: String, ibanValue: String) { setCard(card); if (holderName.isNotBlank()) holder.setText(holderName); if (ibanValue.isNotBlank()) iban.setText(ibanValue); updatePreview() }
    }

    private fun sheetRoot(title: String) = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL; setPadding(dp(20), dp(16), dp(20), dp(28)); addView(TextView(this@BankCardsActivity).apply { text = title; textSize = 20f; typeface = Typeface.DEFAULT_BOLD; setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurface, Color.BLACK)) }, lp(-1, 34)) }
    private fun actionTile(icon: Int, title: String, subtitle: String, click: () -> Unit): View { val card = MaterialCardView(this).apply { radius = dp(16).toFloat(); cardElevation = 0f; strokeWidth = dp(1); strokeColor = resolveColor(com.google.android.material.R.attr.colorOutline, Color.LTGRAY); isClickable = true; isFocusable = true; setOnClickListener { click() } }; val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL; setPadding(dp(14), dp(8), dp(14), dp(8)) }; val image = ImageView(this).apply { setImageResource(icon); tint(this, resolveColor(androidx.appcompat.R.attr.colorPrimary, Color.DKGRAY)) }; row.addView(image, lp(42, 42)); row.addView(LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(12), 0, 0, 0); addView(TextView(this@BankCardsActivity).apply { text = title; textSize = 15f; typeface = Typeface.DEFAULT_BOLD; setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurface, Color.BLACK)) }); addView(TextView(this@BankCardsActivity).apply { text = subtitle; textSize = 11f; maxLines = 1; setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurfaceVariant, Color.GRAY)) }) }, LinearLayout.LayoutParams(0, -1, 1f)); card.addView(row, ViewGroup.LayoutParams(-1, -1)); return card }
    private fun iconButton(icon: Int, description: String, click: () -> Unit) = ImageButton(this).apply { setImageResource(icon); contentDescription = description; background = GradientDrawable().apply { setColor(Color.TRANSPARENT) }; setPadding(dp(10)); scaleType = ImageView.ScaleType.CENTER; setOnClickListener { click() } }
    private fun tint(view: View, value: Int) { if (view is ImageView) view.setColorFilter(value) }
    private fun resolveColor(attr: Int, fallback: Int): Int { val tv = android.util.TypedValue(); if (!theme.resolveAttribute(attr, tv, true)) return fallback; return if (tv.resourceId != 0) runCatching { ContextCompat.getColor(this, tv.resourceId) }.getOrDefault(fallback) else tv.data }
    private fun darken(c: Int) = Color.rgb((Color.red(c) * .72f).toInt(), (Color.green(c) * .72f).toInt(), (Color.blue(c) * .72f).toInt())
    private fun lighten(c: Int) = Color.rgb((Color.red(c) + 255) / 2, (Color.green(c) + 255) / 2, (Color.blue(c) + 255) / 2)
    private fun lp(w: Int, h: Int) = LinearLayout.LayoutParams(if (w < 0) w else dp(w), dp(h))
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun Int.dp() = dp(this)
    private class SimpleTextWatcher(private val changed: () -> Unit) : android.text.TextWatcher { override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) = Unit; override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) = changed(); override fun afterTextChanged(s: android.text.Editable?) = Unit }
}
