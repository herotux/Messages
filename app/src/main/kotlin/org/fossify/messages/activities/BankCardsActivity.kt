package org.fossify.messages.activities

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
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

/** Fully standalone bank-card UI. */
class BankCardsActivity : AppCompatActivity() {
    private lateinit var repository: BankCardsRepository
    private lateinit var pager: ViewPager2
    private lateinit var adapter: CardPagerAdapter
    private var cards = mutableListOf<BankCard>()
    private var wizard: CardWizard? = null

    private val scannerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
        val data = result.data ?: return@registerForActivityResult
        val number = data.getStringExtra(BankCardScannerActivity.EXTRA_CARD).orEmpty()
        if (number.isBlank()) return@registerForActivityResult
        wizard?.applyScanResult(number, data.getStringExtra(BankCardScannerActivity.EXTRA_HOLDER).orEmpty(), data.getStringExtra(BankCardScannerActivity.EXTRA_IBAN).orEmpty())
    }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        repository = BankCardsRepository(this)
        buildUi()
        loadCards()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(resolveColor(com.google.android.material.R.attr.colorSurface, Color.WHITE))
        }
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(dp(16), bars.top + dp(8), dp(16), bars.bottom + dp(16))
            insets
        }

        val toolbar = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL }
        toolbar.addView(iconButton(android.R.drawable.ic_menu_revert, "بازگشت") { finish() }, lp(48, 48))
        toolbar.addView(TextView(this).apply {
            text = "کارت‌های بانکی"
            textSize = 21f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER_VERTICAL
            setTextColor(onSurface())
        }, LinearLayout.LayoutParams(0, dp(56), 1f))
        toolbar.addView(iconButton(android.R.drawable.ic_menu_more, "گزینه‌ها") { showTopMenu() }, lp(48, 48))
        root.addView(toolbar, lp(-1, 56))

        pager = ViewPager2(this).apply {
            orientation = ViewPager2.ORIENTATION_HORIZONTAL
            clipToPadding = false
            clipChildren = false
            offscreenPageLimit = 2
            setPadding(dp(2), dp(10), dp(2), dp(8))
            setPageTransformer { page, position ->
                val d = kotlin.math.abs(position).coerceAtMost(1f)
                page.scaleY = 1f - d * .035f
                page.alpha = 1f - d * .14f
                page.translationX = position * -dp(5).toFloat()
            }
        }
        adapter = CardPagerAdapter()
        pager.adapter = adapter
        root.addView(pager, LinearLayout.LayoutParams(-1, dp(285)))
        root.addView(TextView(this).apply {
            text = "برای مشاهده کارت‌های دیگر، به چپ یا راست بکشید"
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(onSurfaceVariant())
        }, lp(-1, 28))
        root.addView(actionTile(android.R.drawable.ic_input_add, "افزودن کارت جدید", "ثبت یک کارت بانکی جدید") { showWizard(null) }, LinearLayout.LayoutParams(-1, dp(68)).apply { topMargin = dp(10) })
        setContentView(root)
    }

    private fun loadCards() {
        Thread {
            val loaded = repository.getCards()
            runOnUiThread {
                cards = loaded.toMutableList()
                adapter.notifyDataSetChanged()
                if (cards.isEmpty()) pager.setCurrentItem(0, false)
                else pager.setCurrentItem(pager.currentItem.coerceAtMost(cards.lastIndex), false)
            }
        }.start()
    }

    private inner class CardPagerAdapter : RecyclerView.Adapter<CardPagerAdapter.Holder>() {
        inner class Holder(val page: FrameLayout) : RecyclerView.ViewHolder(page)
        override fun getItemCount() = cards.size
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(FrameLayout(parent.context).apply {
            layoutParams = RecyclerView.LayoutParams(-1, -1)
        })
        override fun onBindViewHolder(holder: Holder, position: Int) {
            holder.page.removeAllViews()
            holder.page.addView(createBankCard(cards[position]), FrameLayout.LayoutParams(-1, -1).apply {
                leftMargin = dp(12); rightMargin = dp(12); topMargin = dp(2); bottomMargin = dp(6)
            })
        }
    }

    private fun createBankCard(card: BankCard): View {
        val visual = card.visual
        val base = visual?.color ?: 0xFF37465A.toInt()
        val outer = MaterialCardView(this).apply {
            radius = dp(22).toFloat()
            cardElevation = dp(7).toFloat()
            strokeWidth = dp(1)
            strokeColor = 0x2DFFFFFF.toInt()
            isClickable = true
            isFocusable = true
            setOnClickListener { showCardActions(card) }
        }
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(22), dp(18), dp(22), dp(16))
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(lighten(base), darken(base))).apply { cornerRadius = dp(22).toFloat() }
        }
        val top = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL }
        val logo = ImageView(this).apply {
            visual?.logoResourceName?.let { name -> resources.getIdentifier(name, "drawable", packageName).takeIf { it != 0 }?.let(::setImageResource) }
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            contentDescription = visual?.persianName ?: "بانک"
        }
        top.addView(logo, lp(44, 44))
        top.addView(TextView(this).apply { text = visual?.persianName ?: card.bankId; textSize = 16f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE); setPadding(dp(10), 0, 0, 0) }, LinearLayout.LayoutParams(0, dp(44), 1f))
        val more = iconButton(android.R.drawable.ic_menu_more, "عملیات کارت") { showCardActions(card) }
        tint(more, Color.WHITE)
        top.addView(more, lp(44, 44))
        body.addView(top)
        body.addView(TextView(this).apply {
            text = repository.normalizeCard(card.cardNumber).chunked(4).joinToString("   ")
            textSize = 21f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            letterSpacing = .035f
            gravity = Gravity.CENTER
            textDirection = View.TEXT_DIRECTION_LTR
            setSingleLine(true)
            includeFontPadding = false
            setTextColor(Color.WHITE)
        }, LinearLayout.LayoutParams(-1, dp(76)).apply { topMargin = dp(20) })
        val bottom = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL }
        bottom.addView(TextView(this).apply { text = card.holderName.ifBlank { "نام صاحب کارت" }; textSize = 14f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE); maxLines = 1 }, LinearLayout.LayoutParams(0, dp(28), 1f))
        if (card.iban.isNotBlank()) bottom.addView(TextView(this).apply { text = "شبا"; textSize = 10f; gravity = Gravity.CENTER; setTextColor(0xBFFFFFFF.toInt()) }, lp(38, 28))
        body.addView(bottom)
        outer.addView(body, ViewGroup.LayoutParams(-1, -1))
        return outer
    }

    private fun showTopMenu() {
        val sheet = BottomSheetDialog(this)
        val root = sheetRoot("مدیریت کارت‌ها")
        root.addView(actionTile(android.R.drawable.ic_input_add, "افزودن کارت جدید", "ثبت کارت بانکی") { sheet.dismiss(); showWizard(null) }, lp(-1, 68).apply { topMargin = dp(10) })
        root.addView(actionTile(android.R.drawable.ic_menu_sort_by_size, "مرتب‌سازی کارت‌ها", "تغییر ترتیب نمایش") { sheet.dismiss(); showSort() }, lp(-1, 68).apply { topMargin = dp(8) })
        sheet.setContentView(root); sheet.show()
    }

    private fun showSort() {
        val sheet = BottomSheetDialog(this)
        val root = sheetRoot("مرتب‌سازی کارت‌ها")
        cards.forEachIndexed { index, card ->
            val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL }
            row.addView(TextView(this).apply { text = card.visual?.persianName ?: card.bankId; textSize = 15f; setTextColor(onSurface()) }, LinearLayout.LayoutParams(0, dp(52), 1f))
            row.addView(iconButton(android.R.drawable.arrow_up_float, "بالا") { if (index > 0) { moveCard(index, index - 1); sheet.dismiss(); showSort() } }, lp(48, 48))
            row.addView(iconButton(android.R.drawable.arrow_down_float, "پایین") { if (index < cards.lastIndex) { moveCard(index, index + 1); sheet.dismiss(); showSort() } }, lp(48, 48))
            root.addView(row)
        }
        sheet.setContentView(root); sheet.show()
    }

    private fun moveCard(from: Int, to: Int) {
        if (from !in cards.indices || to !in cards.indices) return
        val card = cards.removeAt(from); cards.add(to, card)
        adapter.notifyItemMoved(from, to); pager.setCurrentItem(to, false)
        Thread { repository.reorder(cards) }.start()
    }

    private fun showCardActions(card: BankCard) {
        val sheet = BottomSheetDialog(this)
        val root = sheetRoot(card.visual?.persianName ?: "کارت بانکی")
        root.addView(actionTile(android.R.drawable.ic_menu_edit, "ویرایش کارت", "تغییر اطلاعات کارت") { sheet.dismiss(); showWizard(card) }, lp(-1, 66).apply { topMargin = dp(10) })
        root.addView(actionTile(android.R.drawable.ic_menu_save, "کپی شماره کارت", repository.normalizeCard(card.cardNumber)) { copy(repository.normalizeCard(card.cardNumber)); sheet.dismiss() }, lp(-1, 66).apply { topMargin = dp(8) })
        if (card.iban.isNotBlank()) root.addView(actionTile(android.R.drawable.ic_menu_save, "کپی شماره شبا", repository.normalizeIban(card.iban)) { copy(repository.normalizeIban(card.iban)); sheet.dismiss() }, lp(-1, 66).apply { topMargin = dp(8) })
        if (card.holderName.isNotBlank()) root.addView(actionTile(android.R.drawable.ic_menu_save, "کپی نام صاحب کارت", card.holderName) { copy(card.holderName); sheet.dismiss() }, lp(-1, 66).apply { topMargin = dp(8) })
        root.addView(actionTile(android.R.drawable.ic_menu_share, "اشتراک‌گذاری", "ارسال اطلاعات کارت") { sheet.dismiss(); share(card) }, lp(-1, 66).apply { topMargin = dp(8) })
        root.addView(actionTile(android.R.drawable.ic_menu_delete, "حذف کارت", "حذف از برنامه") { sheet.dismiss(); confirmDelete(card) }, lp(-1, 66).apply { topMargin = dp(8) })
        sheet.setContentView(root); sheet.show()
    }

    private fun confirmDelete(card: BankCard) { MaterialAlertDialogBuilder(this).setTitle("حذف کارت").setMessage("این کارت حذف شود؟").setNegativeButton("انصراف", null).setPositiveButton("حذف") { _, _ -> Thread { repository.delete(card); runOnUiThread { loadCards() } }.start() }.show() }
    private fun share(card: BankCard) { val text = buildString { append(card.visual?.persianName ?: "کارت بانکی").append('\n'); append("شماره کارت: ").append(repository.normalizeCard(card.cardNumber)).append('\n'); if (card.holderName.isNotBlank()) append("صاحب کارت: ").append(card.holderName).append('\n'); if (card.iban.isNotBlank()) append("شبا: ").append(repository.normalizeIban(card.iban)) }; startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }, "اشتراک‌گذاری کارت")) }
    private fun copy(value: String) { (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Messages", value)); Toast.makeText(this, "کپی شد", Toast.LENGTH_SHORT).show() }

    private fun showWizard(existing: BankCard?) { wizard?.dismiss(); wizard = CardWizard(existing).also { it.show() } }
    private inner class CardWizard(private val existing: BankCard?) {
        private val sheet = BottomSheetDialog(this@BankCardsActivity)
        private val root = LinearLayout(this@BankCardsActivity).apply { orientation = LinearLayout.VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL; setPadding(dp(20), dp(12), dp(20), dp(28)) }
        private val groups = Array(4) { EditText(this@BankCardsActivity).apply { inputType = InputType.TYPE_CLASS_NUMBER; gravity = Gravity.CENTER; textSize = 17f; hint = "••••"; maxLines = 1 } }
        private val holder = TextInputEditText(this@BankCardsActivity)
        private val iban = TextInputEditText(this@BankCardsActivity)
        private var preview: MaterialCardView? = null
        fun show() { buildCardStep(); sheet.setContentView(root); sheet.show(); if (existing == null) pasteClipboard() else setCard(existing.cardNumber) }
        fun dismiss() = sheet.dismiss()
        private fun header(title: String) { root.removeAllViews(); val row = LinearLayout(this@BankCardsActivity).apply { gravity = Gravity.CENTER_VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL }; row.addView(TextView(this@BankCardsActivity).apply { text = title; textSize = 20f; typeface = Typeface.DEFAULT_BOLD; setTextColor(onSurface()) }, LinearLayout.LayoutParams(0, dp(48), 1f)); val scan = iconButton(android.R.drawable.ic_menu_camera, "اسکن کارت") { scannerLauncher.launch(Intent(this@BankCardsActivity, BankCardScannerActivity::class.java).putExtra(BankCardScannerActivity.EXTRA_RETURN_TO_FORM, true)) }; tint(scan, resolveColor(android.R.attr.colorAccent, onSurfaceVariant())); row.addView(scan, lp(48, 48)); root.addView(row) }
        private fun buildCardStep() { header("افزودن کارت · ۱ از ۳"); addPreview(); val row = LinearLayout(this@BankCardsActivity).apply { gravity = Gravity.CENTER; layoutDirection = View.LAYOUT_DIRECTION_LTR }; groups.forEachIndexed { index, edit -> edit.setText(""); row.addView(edit, LinearLayout.LayoutParams(0, dp(54), 1f).apply { if (index < 3) marginEnd = dp(6) }); edit.addTextChangedListener(SimpleTextWatcher { updatePreview() }) }; root.addView(row, lp(-1, 58).apply { topMargin = dp(12) }); root.addView(actionTile(android.R.drawable.ic_menu_send, "ادامه", "اطلاعات کارت را کامل کنید") { buildHolderStep() }, lp(-1, 68).apply { topMargin = dp(14) }) }
        private fun buildHolderStep() { header("افزودن کارت · ۲ از ۳"); addField(holder, "نام صاحب کارت", "نام و نام خانوادگی"); root.addView(actionTile(android.R.drawable.ic_menu_send, "ادامه", "ثبت شماره شبا") { buildIbanStep() }, lp(-1, 68).apply { topMargin = dp(14) }) }
        private fun buildIbanStep() { header("افزودن کارت · ۳ از ۳"); addField(iban, "شماره شبا", "IR..."); root.addView(actionTile(android.R.drawable.ic_menu_save, "ذخیره کارت", "ثبت اطلاعات کارت") { save() }, lp(-1, 68).apply { topMargin = dp(14) }) }
        private fun addPreview() { preview = MaterialCardView(this@BankCardsActivity).apply { radius = dp(18).toFloat(); cardElevation = dp(4).toFloat() }; root.addView(preview, lp(-1, 170).apply { topMargin = dp(10) }); updatePreview() }
        private fun addField(edit: TextInputEditText, label: String, hintText: String) { val layout = TextInputLayout(this@BankCardsActivity).apply { hint = label }; edit.hint = hintText; layout.addView(edit, ViewGroup.LayoutParams(-1, -2)); root.addView(layout, lp(-1, 64).apply { topMargin = dp(12) }) }
        private fun setCard(number: String) { val n = repository.normalizeCard(number).filter(Char::isDigit).padEnd(16, ' '); groups.forEachIndexed { i, edit -> edit.setText(n.substring(i * 4, i * 4 + 4).trim()) }; updatePreview() }
        private fun applyScanResult(number: String, holderName: String, ibanValue: String) { setCard(number); holder.setText(holderName); iban.setText(ibanValue) }
        private fun pasteClipboard() { val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager; val text = cm.primaryClip?.getItemAt(0)?.coerceToText(this@BankCardsActivity)?.toString().orEmpty(); if (text.filter(Char::isDigit).length >= 16) setCard(text) }
        private fun updatePreview() { preview?.removeAllViews(); val number = groups.joinToString("") { it.text?.toString().orEmpty() }; preview?.addView(TextView(this@BankCardsActivity).apply { text = number.chunked(4).joinToString("   ").ifBlank { "••••   ••••   ••••   ••••" }; gravity = Gravity.CENTER; textSize = 20f; typeface = Typeface.MONOSPACE }) }
        private fun save() { val number = groups.joinToString("") { it.text?.toString().orEmpty() }; if (number.filter(Char::isDigit).length < 16) { Toast.makeText(this@BankCardsActivity, "شماره کارت کامل نیست", Toast.LENGTH_SHORT).show(); return }; val card = existing?.copy(cardNumber = number, holderName = holder.text?.toString().orEmpty(), iban = iban.text?.toString().orEmpty()) ?: BankCard(cardNumber = number, holderName = holder.text?.toString().orEmpty(), iban = iban.text?.toString().orEmpty(), bankId = repository.detectBank(number)); Thread { repository.save(card); runOnUiThread { sheet.dismiss(); loadCards() } }.start() }
    }

    private fun sheetRoot(title: String) = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL; setPadding(dp(20), dp(14), dp(20), dp(28)); addView(TextView(this@BankCardsActivity).apply { text = title; textSize = 20f; typeface = Typeface.DEFAULT_BOLD; setTextColor(onSurface()) }, lp(-1, 48)) }
    private fun actionTile(icon: Int, title: String, subtitle: String, action: () -> Unit) = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL; isClickable = true; isFocusable = true; setOnClickListener { action() }; addView(iconButton(icon, title, action), lp(48, 48)); addView(LinearLayout(this@BankCardsActivity).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_VERTICAL; addView(TextView(this@BankCardsActivity).apply { text = title; textSize = 15f; typeface = Typeface.DEFAULT_BOLD; setTextColor(onSurface()) }); addView(TextView(this@BankCardsActivity).apply { text = subtitle; textSize = 12f; setTextColor(onSurfaceVariant()) }) }, LinearLayout.LayoutParams(0, -1, 1f)) }
    private fun iconButton(icon: Int, description: String, action: () -> Unit) = ImageButton(this).apply { setImageResource(icon); contentDescription = description; setBackgroundColor(Color.TRANSPARENT); setOnClickListener { action() } }
    private fun tint(view: ImageView, color: Int) { view.setColorFilter(color) }
    private fun resolveColor(attr: Int, fallback: Int): Int { val tv = android.util.TypedValue(); return if (theme.resolveAttribute(attr, tv, true)) { if (tv.resourceId != 0) androidx.core.content.ContextCompat.getColor(this, tv.resourceId) else tv.data } else fallback }
    private fun onSurface() = resolveColor(com.google.android.material.R.attr.colorOnSurface, Color.BLACK)
    private fun onSurfaceVariant() = resolveColor(com.google.android.material.R.attr.colorOnSurfaceVariant, Color.DKGRAY)
    private fun rgb(r: Int, g: Int, b: Int) = (0xFF shl 24) or ((r and 0xFF) shl 16) or ((g and 0xFF) shl 8) or (b and 0xFF)
    private fun darken(c: Int) = rgb((Color.red(c) * 72) / 100, (Color.green(c) * 72) / 100, (Color.blue(c) * 72) / 100)
    private fun lighten(c: Int) = rgb((Color.red(c) + 255) / 2, (Color.green(c) + 255) / 2, (Color.blue(c) + 255) / 2)
    private fun lp(w: Int, h: Int) = LinearLayout.LayoutParams(if (w < 0) w else dp(w), dp(h))
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private class SimpleTextWatcher(private val changed: () -> Unit) : android.text.TextWatcher { override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) = Unit; override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) = changed(); override fun afterTextChanged(s: android.text.Editable?) = Unit }
}