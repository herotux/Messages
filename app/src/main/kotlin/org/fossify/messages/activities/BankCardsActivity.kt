package org.fossify.messages.activities

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.setPadding
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import org.fossify.messages.features.bankcards.BankCard
import org.fossify.messages.features.bankcards.BankCardsRepository

/** Standalone bank-card UI. It talks to Messages only through BankCardsRepository. */
class BankCardsActivity : AppCompatActivity() {
    companion object { private const val REQUEST_SCAN = 7401 }

    private lateinit var repository: BankCardsRepository
    private lateinit var pager: ViewPager2
    private lateinit var adapter: CardPagerAdapter
    private var cards = mutableListOf<BankCard>()
    private var wizard: CardWizard? = null

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        repository = BankCardsRepository(this)
        buildPage()
        loadCards()
    }

    private fun buildPage() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(surfaceColor())
        }
        val toolbar = MaterialToolbar(this).apply {
            title = "کارت‌های بانکی"
            setTitleTextColor(onSurfaceColor())
            setBackgroundColor(surfaceColor())
            navigationIcon = getDrawable(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
            setNavigationOnClickListener { finish() }
            val more = menu.add("⋮")
            more.setShowAsAction(2)
            setOnMenuItemClickListener { showTopMenu(); true }
        }
        root.addView(toolbar, LinearLayout.LayoutParams(-1, dp(64)))

        pager = ViewPager2(this).apply {
            orientation = ViewPager2.ORIENTATION_HORIZONTAL
            clipToPadding = false
            setPadding(dp(18), dp(12), dp(18), dp(12))
            offscreenPageLimit = 2
        }
        adapter = CardPagerAdapter()
        pager.adapter = adapter
        root.addView(pager, LinearLayout.LayoutParams(-1, dp(300)))

        root.addView(TextView(this).apply {
            text = "برای دیدن کارت‌های دیگر، صفحه را بکشید"
            gravity = Gravity.CENTER
            textSize = 12f
            setTextColor(onSurfaceVariant())
            setPadding(0, 0, 0, dp(8))
        }, LinearLayout.LayoutParams(-1, -2))

        val add = MaterialButton(this).apply {
            text = "افزودن کارت جدید"
            icon = getDrawable(android.R.drawable.ic_input_add)
            setOnClickListener { showWizard(null) }
        }
        root.addView(add, LinearLayout.LayoutParams(-1, dp(54)).apply {
            setMargins(dp(20), dp(8), dp(20), dp(20))
        })
        setContentView(root)
    }

    private fun loadCards() {
        Thread {
            val result = repository.getCards()
            runOnUiThread { cards = result.toMutableList(); adapter.notifyDataSetChanged(); updateEmptyState() }
        }.start()
    }

    private fun updateEmptyState() {
        // The pager remains intentionally empty when there are no cards; the add CTA stays visible.
    }

    private inner class CardPagerAdapter : RecyclerView.Adapter<CardPagerAdapter.Holder>() {
        inner class Holder(val container: LinearLayout) : RecyclerView.ViewHolder(container)
        override fun getItemCount() = cards.size
        override fun onCreateViewHolder(parent: ViewGroup, type: Int) = Holder(LinearLayout(parent.context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(4))
        })
        override fun onBindViewHolder(holder: Holder, position: Int) {
            holder.container.removeAllViews()
            holder.container.addView(createCard(cards[position]), LinearLayout.LayoutParams(-1, -1))
        }
    }

    private fun createCard(card: BankCard): View {
        val visual = card.visual
        val start = visual?.color ?: Color.rgb(58, 72, 90)
        val end = darken(start)
        val view = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(18), dp(22), dp(14))
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(start, end)).apply { cornerRadius = dp(26).toFloat() }
            elevation = dp(7).toFloat()
            setOnClickListener { showCardActions(card) }
        }
        val top = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val logo = ImageView(this).apply {
            visual?.logoResourceName?.let { name ->
                val id = resources.getIdentifier(name, "drawable", packageName)
                if (id != 0) setImageResource(id)
            }
            contentDescription = visual?.persianName ?: ""
        }
        top.addView(logo, LinearLayout.LayoutParams(dp(42), dp(42)))
        top.addView(TextView(this).apply {
            text = visual?.persianName ?: card.bankId
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setPadding(dp(10), 0, 0, 0)
        }, LinearLayout.LayoutParams(0, -2, 1f))
        top.addView(TextView(this).apply {
            text = "⋮"
            textSize = 28f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setOnClickListener { showCardActions(card) }
        }, LinearLayout.LayoutParams(dp(42), dp(44)))
        view.addView(top)
        view.addView(TextView(this).apply {
            text = repository.formatCard(card.cardNumber)
            textSize = 21f
            typeface = Typeface.MONOSPACE
            letterSpacing = .06f
            gravity = Gravity.CENTER
            textDirection = View.TEXT_DIRECTION_LTR
            setTextColor(Color.WHITE)
            setPadding(0, dp(24), 0, dp(12))
        })
        view.addView(TextView(this).apply {
            text = card.holderName.ifBlank { "نام صاحب کارت" }
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        })
        if (card.iban.isNotBlank()) view.addView(TextView(this).apply {
            text = repository.formatIban(card.iban)
            textSize = 11f
            gravity = Gravity.CENTER
            textDirection = View.TEXT_DIRECTION_LTR
            setTextColor(Color.WHITE)
            setPadding(0, dp(6), 0, 0)
        })
        return view
    }

    private fun showTopMenu() {
        val sheet = BottomSheetDialog(this)
        val root = sheetRoot("مدیریت کارت‌ها")
        sheetItem(root, sheet, "افزودن کارت جدید") { showWizard(null) }
        sheetItem(root, sheet, "مرتب‌سازی دلخواه") { showSort(sheet) }
        sheet.setContentView(root)
        sheet.show()
    }

    private fun showSort(previous: BottomSheetDialog? = null) {
        previous?.dismiss()
        val sheet = BottomSheetDialog(this)
        val root = sheetRoot("مرتب‌سازی کارت‌ها")
        cards.forEachIndexed { index, card ->
            val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
            row.addView(TextView(this).apply {
                text = card.visual?.persianName ?: card.bankId
                textSize = 15f
            }, LinearLayout.LayoutParams(0, dp(52), 1f))
            MaterialButton(this).apply {
                text = "↑"; isEnabled = index > 0
                setOnClickListener { moveCard(index, index - 1); sheet.dismiss(); showSort() }
            }.also { row.addView(it, LinearLayout.LayoutParams(dp(54), dp(52))) }
            MaterialButton(this).apply {
                text = "↓"; isEnabled = index < cards.lastIndex
                setOnClickListener { moveCard(index, index + 1); sheet.dismiss(); showSort() }
            }.also { row.addView(it, LinearLayout.LayoutParams(dp(54), dp(52)).apply { marginStart = dp(6) }) }
            root.addView(row)
        }
        sheet.setContentView(root)
        sheet.show()
    }

    private fun moveCard(from: Int, to: Int) {
        val card = cards.removeAt(from)
        cards.add(to, card)
        adapter.notifyDataSetChanged()
        Thread { repository.reorder(cards) }.start()
    }

    private fun showCardActions(card: BankCard) {
        val sheet = BottomSheetDialog(this)
        val root = sheetRoot(card.visual?.persianName ?: "کارت بانکی")
        sheetItem(root, sheet, "ویرایش") { showWizard(card) }
        sheetItem(root, sheet, "کپی شماره کارت") { copy(card.cardNumber) }
        if (card.iban.isNotBlank()) sheetItem(root, sheet, "کپی شماره شبا") { copy(card.iban) }
        sheetItem(root, sheet, "اشتراک‌گذاری") { share(card) }
        sheetItem(root, sheet, "حذف کارت") {
            MaterialAlertDialogBuilder(this).setTitle("حذف کارت").setMessage("این کارت حذف شود؟")
                .setNegativeButton("انصراف", null)
                .setPositiveButton("حذف") { _, _ -> Thread { repository.delete(card); runOnUiThread { loadCards() } }.start() }.show()
        }
        sheet.setContentView(root)
        sheet.show()
    }

    private fun sheetRoot(title: String) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        layoutDirection = View.LAYOUT_DIRECTION_RTL
        setPadding(dp(20), dp(16), dp(20), dp(28))
        addView(TextView(this@BankCardsActivity).apply { text = title; textSize = 20f; typeface = Typeface.DEFAULT_BOLD })
    }

    private fun sheetItem(root: LinearLayout, sheet: BottomSheetDialog, label: String, action: () -> Unit) {
        root.addView(MaterialButton(this).apply {
            text = label
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            setOnClickListener { sheet.dismiss(); action() }
        }, LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(8) })
    }

    private fun copy(value: String) {
        (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Messages", value))
        Toast.makeText(this, "کپی شد", Toast.LENGTH_SHORT).show()
    }

    private fun share(card: BankCard) {
        val text = buildString {
            append(card.visual?.persianName ?: "کارت بانکی").append('\n')
            append(repository.formatCard(card.cardNumber)).append('\n')
            if (card.holderName.isNotBlank()) append(card.holderName).append('\n')
            if (card.iban.isNotBlank()) append(repository.formatIban(card.iban))
        }
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }, "اشتراک‌گذاری"))
    }

    private fun showWizard(existing: BankCard?) {
        wizard?.dismiss()
        wizard = CardWizard(existing).also { it.show() }
    }

    private inner class CardWizard(private val existing: BankCard?) {
        private val dialog = BottomSheetDialog(this@BankCardsActivity)
        private val root = LinearLayout(this@BankCardsActivity).apply { orientation = LinearLayout.VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL; setPadding(dp(20), dp(12), dp(20), dp(28)) }
        private val groups = Array(4) { EditText(this@BankCardsActivity).apply { inputType = InputType.TYPE_CLASS_NUMBER; gravity = Gravity.CENTER; textSize = 17f; hint = "••••"; maxLines = 1 } }
        private val holder = TextInputEditText(this@BankCardsActivity)
        private val iban = TextInputEditText(this@BankCardsActivity)
        private val preview = LinearLayout(this@BankCardsActivity).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(dp(20), dp(16), dp(20), dp(14)) }

        fun show() { dialog.setContentView(root); buildCardStep(); dialog.show(); if (existing == null) pasteClipboard() else setCard(existing.cardNumber) }
        fun dismiss() = dialog.dismiss()

        private fun header(title: String) {
            root.removeAllViews()
            val row = LinearLayout(this@BankCardsActivity).apply { gravity = Gravity.CENTER_VERTICAL }
            row.addView(TextView(this@BankCardsActivity).apply { text = title; textSize = 20f; typeface = Typeface.DEFAULT_BOLD }, LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(MaterialButton(this@BankCardsActivity).apply {
                text = "اسکن کارت"
                icon = getDrawable(android.R.drawable.ic_menu_camera)
                setOnClickListener { startActivityForResult(Intent(this@BankCardsActivity, BankCardScannerActivity::class.java).putExtra(BankCardScannerActivity.EXTRA_RETURN_TO_FORM, true), REQUEST_SCAN) }
            })
            root.addView(row)
        }

        private fun buildCardStep() {
            header("افزودن کارت · ۱ از ۳")
            root.addView(preview, LinearLayout.LayoutParams(-1, dp(190)).apply { topMargin = dp(12) })
            val row = LinearLayout(this@BankCardsActivity).apply { gravity = Gravity.CENTER; layoutDirection = View.LAYOUT_DIRECTION_LTR }
            groups.forEach { row.addView(it, LinearLayout.LayoutParams(0, dp(56), 1f).apply { marginEnd = dp(6) }) }
            root.addView(row)
            root.addView(MaterialButton(this@BankCardsActivity).apply { text = "ادامه"; setOnClickListener { if (isValidCard()) buildHolderStep() } }, LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(14) })
            groups.forEach { field -> field.addTextChangedListener(SimpleWatcher { updatePreview() }) }
            updatePreview()
        }

        private fun buildHolderStep() {
            header("افزودن کارت · ۲ از ۳")
            root.addView(preview, LinearLayout.LayoutParams(-1, dp(190)).apply { topMargin = dp(12) })
            val layout = TextInputLayout(this@BankCardsActivity).apply { hint = "نام صاحب کارت (اختیاری)"; addView(holder) }
            root.addView(layout, LinearLayout.LayoutParams(-1, dp(72)).apply { topMargin = dp(14) })
            root.addView(MaterialButton(this@BankCardsActivity).apply { text = "ادامه"; setOnClickListener { buildIbanStep() } }, LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(14) })
            updatePreview()
        }

        private fun buildIbanStep() {
            header("افزودن کارت · ۳ از ۳")
            root.addView(preview, LinearLayout.LayoutParams(-1, dp(190)).apply { topMargin = dp(12) })
            val layout = TextInputLayout(this@BankCardsActivity).apply { hint = "شماره شبا (اختیاری)"; addView(iban) }
            root.addView(layout, LinearLayout.LayoutParams(-1, dp(72)).apply { topMargin = dp(14) })
            root.addView(MaterialButton(this@BankCardsActivity).apply { text = "ذخیره کارت"; setOnClickListener { save() } }, LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(14) })
            updatePreview()
        }

        private fun isValidCard(): Boolean {
            val value = groups.joinToString("") { repository.normalizeCard(it.text.toString()) }
            val ok = value.length == 16 && repository.validCard(value) && repository.detect(value) != null
            if (!ok) Toast.makeText(this@BankCardsActivity, "شماره کارت معتبر نیست یا بانک شناسایی نشد", Toast.LENGTH_SHORT).show()
            return ok
        }

        private fun save() {
            val card = groups.joinToString("") { repository.normalizeCard(it.text.toString()) }
            val holderText = holder.text?.toString().orEmpty()
            val ibanText = repository.normalizeIban(iban.text?.toString().orEmpty())
            if (!repository.validCard(card) || repository.detect(card) == null || !repository.validIban(ibanText)) {
                Toast.makeText(this@BankCardsActivity, "اطلاعات کارت معتبر نیست", Toast.LENGTH_SHORT).show(); return
            }
            Thread {
                val result = repository.save(existing, card, holderText, ibanText)
                runOnUiThread {
                    if (result.isSuccess) { dialog.dismiss(); loadCards() }
                    else Toast.makeText(this@BankCardsActivity, "ذخیره کارت انجام نشد", Toast.LENGTH_SHORT).show()
                }
            }.start()
        }

        private fun pasteClipboard() {
            val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val raw = cm.primaryClip?.getItemAt(0)?.coerceToText(this@BankCardsActivity)?.toString().orEmpty()
            val value = repository.normalizeCard(raw)
            if (value.length == 16 && repository.detect(value) != null) setCard(value)
        }

        private fun setCard(value: String) {
            value.chunked(4).take(4).forEachIndexed { index, part -> groups[index].setText(part) }
            updatePreview()
        }

        private fun updatePreview() {
            preview.removeAllViews()
            val card = groups.joinToString("") { repository.normalizeCard(it.text.toString()) }
            val visual = repository.detect(card)
            val start = visual?.color ?: Color.rgb(58, 72, 90)
            preview.background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(start, darken(start))).apply { cornerRadius = dp(22).toFloat() }
            preview.addView(TextView(this@BankCardsActivity).apply { text = visual?.persianName ?: "بانک را وارد کنید"; textSize = 14f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE) })
            preview.addView(TextView(this@BankCardsActivity).apply { text = repository.formatCard(card).ifBlank { "••••   ••••   ••••   ••••" }; textSize = 20f; typeface = Typeface.MONOSPACE; gravity = Gravity.CENTER; textDirection = View.TEXT_DIRECTION_LTR; setTextColor(Color.WHITE); setPadding(0, dp(34), 0, 0) })
        }
    }

    private class SimpleWatcher(private val changed: () -> Unit) : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = changed()
        override fun afterTextChanged(s: Editable?) = Unit
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_SCAN && resultCode == Activity.RESULT_OK) {
            val card = data?.getStringExtra(BankCardScannerActivity.EXTRA_CARD).orEmpty()
            if (card.isNotBlank()) wizard?.setCardFromScan(card, data.getStringExtra(BankCardScannerActivity.EXTRA_HOLDER).orEmpty(), data.getStringExtra(BankCardScannerActivity.EXTRA_IBAN).orEmpty())
        }
    }

    private fun CardWizard.setCardFromScan(card: String, holderName: String, ibanValue: String) {
        setCard(card)
        if (holderName.isNotBlank()) holder.setText(holderName)
        if (ibanValue.isNotBlank()) iban.setText(ibanValue)
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun surfaceColor() = resolveColor(com.google.android.material.R.attr.colorSurface, Color.WHITE)
    private fun onSurfaceColor() = resolveColor(com.google.android.material.R.attr.colorOnSurface, Color.BLACK)
    private fun onSurfaceVariant() = resolveColor(com.google.android.material.R.attr.colorOnSurfaceVariant, Color.DKGRAY)
    private fun resolveColor(attr: Int, fallback: Int): Int {
        val tv = android.util.TypedValue()
        return if (theme.resolveAttribute(attr, tv, true)) {
            if (tv.resourceId != 0) runCatching { getColor(tv.resourceId) }.getOrDefault(fallback) else tv.data
        } else fallback
    }
    private fun darken(color: Int): Int = Color.rgb((Color.red(color) * .72f).toInt(), (Color.green(color) * .72f).toInt(), (Color.blue(color) * .72f).toInt())
}
