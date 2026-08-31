package org.fossify.messages.activities

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import org.fossify.messages.extensions.getMessagesDB
import org.fossify.messages.helpers.BankAccountsFeature
import org.fossify.messages.models.BankAccount

class BankCardsActivity : SimpleActivity() {
    private var accounts = mutableListOf<BankAccount>()
    private lateinit var pager: ViewPager2
    private lateinit var adapter: CardPagerAdapter
    private var wizard: CardWizard? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildPage()
        loadAccounts()
    }

    private fun buildPage() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(surface()) }
        val toolbar = MaterialToolbar(this).apply {
            title = "کارت‌های بانکی"
            setBackgroundColor(surface())
            navigationIcon = getDrawable(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
            setNavigationOnClickListener { finish() }
        }
        toolbar.menu.add("⋮").setShowAsAction(2)
        toolbar.setOnMenuItemClickListener { showMenu(); true }
        root.addView(toolbar, LinearLayout.LayoutParams(-1, dp(64)))
        pager = ViewPager2(this).apply { orientation = ViewPager2.ORIENTATION_HORIZONTAL; offscreenPageLimit = 2; clipToPadding = false; setPadding(dp(18), dp(10), dp(18), dp(10)) }
        adapter = CardPagerAdapter()
        pager.adapter = adapter
        root.addView(pager, LinearLayout.LayoutParams(-1, dp(292)))
        root.addView(TextView(this).apply { text = "کارت‌ها را به چپ و راست بکشید"; gravity = Gravity.CENTER; textSize = 12f; setTextColor(onSurfaceVariant()); setPadding(0, 0, 0, dp(8)) }, LinearLayout.LayoutParams(-1, -2))
        MaterialButton(this).apply { text = "افزودن کارت جدید"; icon = getDrawable(android.R.drawable.ic_input_add); setOnClickListener { showWizard(null) } }.also { root.addView(it, LinearLayout.LayoutParams(-1, dp(52)).apply { setMargins(dp(20), dp(8), dp(20), dp(18)) }) }
        setContentView(root)
    }

    private fun loadAccounts() {
        Thread { accounts = BankAccountsFeature.getAccounts(this).toMutableList(); runOnUiThread { adapter.notifyDataSetChanged() } }.start()
    }

    private fun showMenu() {
        val sheet = BottomSheetDialog(this)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(16), dp(20), dp(28)) }
        root.addView(TextView(this).apply { text = "کارت‌های بانکی"; textSize = 20f; typeface = Typeface.DEFAULT_BOLD })
        fun item(text: String, action: () -> Unit) { root.addView(MaterialButton(this).apply { this.text = text; gravity = Gravity.START or Gravity.CENTER_VERTICAL; setOnClickListener { sheet.dismiss(); action() } }, LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(8) }) }
        item("افزودن کارت جدید") { showWizard(null) }
        item("مرتب‌سازی دلخواه") { showSort() }
        sheet.setContentView(root); sheet.show()
    }

    private fun showSort() {
        val sheet = BottomSheetDialog(this)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(16), dp(20), dp(28)) }
        root.addView(TextView(this).apply { text = "مرتب‌سازی کارت‌ها"; textSize = 20f; typeface = Typeface.DEFAULT_BOLD })
        accounts.forEachIndexed { index, account ->
            val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
            row.addView(TextView(this).apply { text = BankAccountsFeature.bankFor(account)?.persianName ?: account.bankId; textSize = 15f }, LinearLayout.LayoutParams(0, dp(52), 1f))
            MaterialButton(this).apply { text = "↑"; isEnabled = index > 0; setOnClickListener { move(index, index - 1); sheet.dismiss(); showSort() } }.also { row.addView(it, LinearLayout.LayoutParams(dp(52), dp(52))) }
            MaterialButton(this).apply { text = "↓"; isEnabled = index < accounts.lastIndex; setOnClickListener { move(index, index + 1); sheet.dismiss(); showSort() } }.also { row.addView(it, LinearLayout.LayoutParams(dp(52), dp(52)).apply { marginStart = dp(6) }) }
            root.addView(row)
        }
        sheet.setContentView(ScrollView(this).apply { addView(root) }); sheet.show()
    }

    private fun move(from: Int, to: Int) {
        val item = accounts.removeAt(from); accounts.add(to, item)
        Thread { val dao = getMessagesDB().BankAccountsDao(); accounts.forEachIndexed { i, a -> dao.update(a.copy(updatedAt = System.currentTimeMillis() + accounts.size - i)) }; runOnUiThread { adapter.notifyDataSetChanged() } }.start()
    }

    private inner class CardPagerAdapter : RecyclerView.Adapter<CardPagerAdapter.Holder>() {
        inner class Holder(val root: LinearLayout) : RecyclerView.ViewHolder(root)
        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int) = Holder(LinearLayout(parent.context).apply { orientation = LinearLayout.VERTICAL; layoutParams = ViewPager2.LayoutParams(-1, -1); setPadding(dp(4), dp(4), dp(4), dp(4)) })
        override fun getItemCount() = accounts.size
        override fun onBindViewHolder(holder: Holder, position: Int) { bindCard(holder.root, accounts[position]) }
    }

    private fun bindCard(root: LinearLayout, account: BankAccount) {
        root.removeAllViews(); val bank = BankAccountsFeature.bankFor(account); val base = BankAccountsFeature.cardColor(account.bankId)
        val card = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(22), dp(18), dp(22), dp(14)); background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(base, darken(base))).apply { cornerRadius = dp(24).toFloat() }; elevation = dp(6).toFloat() }
        val top = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        top.addView(ImageView(this).apply { bank?.logoResourceName?.let { resources.getIdentifier(it, "drawable", packageName).takeIf { id -> id != 0 }?.let(::setImageResource) } }, LinearLayout.LayoutParams(dp(42), dp(42)))
        top.addView(TextView(this).apply { text = bank?.persianName ?: account.bankId; textSize = 15f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE); setPadding(dp(10), 0, 0, 0) }, LinearLayout.LayoutParams(0, -2, 1f))
        top.addView(TextView(this).apply { text = "⋮"; textSize = 28f; setTextColor(Color.WHITE); gravity = Gravity.CENTER; setOnClickListener { showCardActions(account) } }, LinearLayout.LayoutParams(dp(42), dp(44)))
        card.addView(top)
        card.addView(TextView(this).apply { text = BankAccountsFeature.formatCard(account.cardNumber); textSize = 21f; typeface = Typeface.MONOSPACE; setTextColor(Color.WHITE); gravity = Gravity.CENTER; layoutDirection = View.LAYOUT_DIRECTION_LTR; letterSpacing = .06f; setPadding(0, dp(22), 0, dp(12)) })
        card.addView(TextView(this).apply { text = account.holderName.ifBlank { "نام صاحب کارت" }; textSize = 13f; setTextColor(Color.WHITE); typeface = Typeface.DEFAULT_BOLD })
        if (account.iban.isNotBlank()) card.addView(TextView(this).apply { text = BankAccountsFeature.formatIban(account.iban); textSize = 11f; setTextColor(Color.WHITE); gravity = Gravity.CENTER; layoutDirection = View.LAYOUT_DIRECTION_LTR; setPadding(0, dp(5), 0, 0) })
        root.addView(card, LinearLayout.LayoutParams(-1, -1))
    }

    private fun showCardActions(account: BankAccount) {
        val sheet = BottomSheetDialog(this); val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(16), dp(20), dp(28)) }
        root.addView(TextView(this).apply { text = BankAccountsFeature.bankFor(account)?.persianName ?: "کارت بانکی"; textSize = 20f; typeface = Typeface.DEFAULT_BOLD })
        fun item(text: String, action: () -> Unit) { root.addView(MaterialButton(this).apply { this.text = text; gravity = Gravity.START or Gravity.CENTER_VERTICAL; setOnClickListener { sheet.dismiss(); action() } }, LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(8) }) }
        item("ویرایش") { showWizard(account) }
        item("کپی شماره کارت") { copy(account.cardNumber) }
        if (account.iban.isNotBlank()) item("کپی شماره شبا") { copy(account.iban) }
        item("اشتراک‌گذاری") { share(account) }
        item("حذف کارت") { MaterialAlertDialogBuilder(this).setTitle("حذف کارت").setMessage("این کارت حذف شود؟").setNegativeButton("انصراف", null).setPositiveButton("حذف") { _, _ -> Thread { getMessagesDB().BankAccountsDao().delete(account); runOnUiThread { loadAccounts() } }.start() }.show() }
        sheet.setContentView(ScrollView(this).apply { addView(root) }); sheet.show()
    }

    private fun copy(value: String) { (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Messages", value)); Toast.makeText(this, "کپی شد", Toast.LENGTH_SHORT).show() }
    private fun share(a: BankAccount) { startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "${BankAccountsFeature.bankFor(a)?.persianName ?: "کارت بانکی"}\n${BankAccountsFeature.formatCard(a.cardNumber)}\n${a.holderName}\n${BankAccountsFeature.formatIban(a.iban)}") }, "اشتراک‌گذاری")) }

    private fun showWizard(existing: BankAccount?) { wizard = CardWizard(existing); wizard!!.show() }

    private inner class CardWizard(private val existing: BankAccount?) {
        private val sheet = BottomSheetDialog(this@BankCardsActivity)
        private val root = LinearLayout(this@BankCardsActivity).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(12), dp(20), dp(28)) }
        private val parts = Array(4) { EditText(this@BankCardsActivity).apply { inputType = 2; gravity = Gravity.CENTER; textSize = 17f; hint = "••••"; maxLines = 1 } }
        private val holder = TextInputEditText(this@BankCardsActivity)
        private val iban = TextInputEditText(this@BankCardsActivity)
        private val preview = LinearLayout(this@BankCardsActivity).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(16), dp(20), dp(14)) }
        private var step = 1
        fun show() { sheet.setContentView(root); build1(); sheet.show(); if (existing == null) pasteClipboard() }
        private fun header(title: String) { root.removeAllViews(); val row = LinearLayout(this@BankCardsActivity).apply { gravity = Gravity.CENTER_VERTICAL }; row.addView(TextView(this@BankCardsActivity).apply { text = title; textSize = 20f; typeface = Typeface.DEFAULT_BOLD }, LinearLayout.LayoutParams(0, -2, 1f)); MaterialButton(this@BankCardsActivity).apply { text = "اسکن کارت"; icon = getDrawable(android.R.drawable.ic_menu_camera); setOnClickListener { startActivityForResult(Intent(this@BankCardsActivity, BankCardScannerActivity::class.java).putExtra(BankCardScannerActivity.EXTRA_RETURN_TO_FORM, true), 7401) } }.also { row.addView(it) }; root.addView(row) }
        private fun build1() { header("افزودن کارت · ۱ از ۳"); root.addView(preview, LinearLayout.LayoutParams(-1, dp(190)).apply { topMargin = dp(12) }); val row = LinearLayout(this@BankCardsActivity).apply { gravity = Gravity.CENTER; layoutDirection = View.LAYOUT_DIRECTION_LTR }; parts.forEach { row.addView(it, LinearLayout.LayoutParams(0, dp(56), 1f).apply { marginEnd = dp(6) }) }; root.addView(row); root.addView(MaterialButton(this@BankCardsActivity).apply { text = "ادامه"; setOnClickListener { if (validCard()) { step = 2; build2() } } }, LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(14) }); parts.forEach { it.addTextChangedListenerCompat { updatePreview() } }; updatePreview() }
        private fun build2() { header("افزودن کارت · ۲ از ۳"); root.addView(preview, LinearLayout.LayoutParams(-1, dp(190)).apply { topMargin = dp(12) }); root.addView(TextInputLayout(this@BankCardsActivity).apply { hint = "نام صاحب کارت (اختیاری)"; addView(holder); holder.setText(existing?.holderName ?: "") }, LinearLayout.LayoutParams(-1, dp(72)).apply { topMargin = dp(14) }); root.addView(MaterialButton(this@BankCardsActivity).apply { text = "ادامه"; setOnClickListener { step = 3; build3() } }, LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(14) }); updatePreview() }
        private fun build3() { header("افزودن کارت · ۳ از ۳"); root.addView(preview, LinearLayout.LayoutParams(-1, dp(190)).apply { topMargin = dp(12) }); root.addView(TextInputLayout(this@BankCardsActivity).apply { hint = "شماره شبا (اختیاری)"; addView(iban); iban.setText(existing?.iban?.let(BankAccountsFeature::formatIban) ?: "") }, LinearLayout.LayoutParams(-1, dp(72)).apply { topMargin = dp(14) }); root.addView(MaterialButton(this@BankCardsActivity).apply { text = "ذخیره کارت"; setOnClickListener { save() } }, LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(14) }); updatePreview() }
        private fun validCard(): Boolean { val c = parts.joinToString("") { BankAccountsFeature.normalizeCard(it.text.toString()) }; val ok = c.length == 16 && BankAccountsFeature.validCard(c) && BankAccountsFeature.bankForCard(c) != null; if (!ok) Toast.makeText(this@BankCardsActivity, "شماره کارت معتبر نیست یا بانک شناسایی نشد", Toast.LENGTH_SHORT).show(); return ok }
        private fun save() { val c = parts.joinToString("") { BankAccountsFeature.normalizeCard(it.text.toString()) }; val b = BankAccountsFeature.bankForCard(c); val i = BankAccountsFeature.normalizeIban(iban.text?.toString().orEmpty()); if (b == null || !BankAccountsFeature.validCard(c) || !BankAccountsFeature.validIban(i)) { Toast.makeText(this@BankCardsActivity, "اطلاعات کارت معتبر نیست", Toast.LENGTH_SHORT).show(); return }; Thread { BankAccountsFeature.save(this@BankCardsActivity, existing, b.id.name, c, holder.text?.toString().orEmpty(), i); runOnUiThread { sheet.dismiss(); loadAccounts() } }.start() }
        private fun pasteClipboard() { val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager; val value = BankAccountsFeature.normalizeCard(cm.primaryClip?.getItemAt(0)?.coerceToText(this@BankCardsActivity)?.toString().orEmpty()); if (value.length == 16 && BankAccountsFeature.bankForCard(value) != null) setCard(value) }
        fun setCard(value: String) { value.chunked(4).forEachIndexed { i, v -> if (i < 4) parts[i].setText(v) }; updatePreview() }
        private fun updatePreview() { preview.removeAllViews(); val c = parts.joinToString("") { BankAccountsFeature.normalizeCard(it.text.toString()) }; val b = BankAccountsFeature.bankForCard(c); val color = b?.let { BankAccountsFeature.cardColor(it.id.name) } ?: Color.rgb(80,88,100); preview.background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(color, darken(color))).apply { cornerRadius = dp(22).toFloat() }; preview.addView(TextView(this@BankCardsActivity).apply { text = b?.persianName ?: "بانک"; textSize = 15f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE) }); preview.addView(TextView(this@BankCardsActivity).apply { text = if (c.isBlank()) "••••  ••••  ••••  ••••" else BankAccountsFeature.formatCard(c); textSize = 19f; typeface = Typeface.MONOSPACE; setTextColor(Color.WHITE); gravity = Gravity.CENTER; layoutDirection = View.LAYOUT_DIRECTION_LTR; setPadding(0, dp(18), 0, dp(8)) }); preview.addView(TextView(this@BankCardsActivity).apply { text = holder.text?.toString().orEmpty().ifBlank { "نام صاحب کارت" }; setTextColor(Color.WHITE); textSize = 12f }) }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) { super.onActivityResult(requestCode, resultCode, data); if (requestCode == 7401 && resultCode == RESULT_OK) { data?.getStringExtra(BankCardScannerActivity.EXTRA_CARD)?.let { wizard?.setCard(it) } } }
    private fun surface() = color(com.google.android.material.R.attr.colorSurface)
    private fun onSurfaceVariant() = color(com.google.android.material.R.attr.colorOnSurfaceVariant)
    private fun darken(c: Int) = Color.rgb((Color.red(c)*.72).toInt(), (Color.green(c)*.72).toInt(), (Color.blue(c)*.72).toInt())
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
