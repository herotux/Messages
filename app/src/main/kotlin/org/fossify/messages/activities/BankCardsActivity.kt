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
import android.view.Window
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.EditText
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
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.fossify.messages.features.bankcards.BankCard
import org.fossify.messages.features.bankcards.BankCardsRepository

/** Fully standalone presentation layer for bank cards. */
class BankCardsActivity : AppCompatActivity() {
    private lateinit var repo: BankCardsRepository
    private lateinit var pager: ViewPager2
    private lateinit var pagerAdapter: CardsAdapter
    private var cards = mutableListOf<BankCard>()
    private var wizard: Wizard? = null

    private val scanner = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
        val data = result.data ?: return@registerForActivityResult
        val number = data.getStringExtra(BankCardScannerActivity.EXTRA_CARD).orEmpty()
        if (number.isNotBlank()) wizard?.applyScan(number, data.getStringExtra(BankCardScannerActivity.EXTRA_HOLDER).orEmpty(), data.getStringExtra(BankCardScannerActivity.EXTRA_IBAN).orEmpty())
    }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        repo = BankCardsRepository(this)
        buildUi()
        loadCards()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(surface()); layoutDirection = View.LAYOUT_DIRECTION_RTL }
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(left = dp(16), right = dp(16), top = bars.top, bottom = bars.bottom + dp(16))
            insets
        }

        val toolbar = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(4), 0, dp(4)) }
        toolbar.addView(TextView(this).apply { text = "کارت‌های بانکی"; textSize = 21f; typeface = Typeface.DEFAULT_BOLD; setTextColor(onSurface()) }, LinearLayout.LayoutParams(0, dp(56), 1f))
        toolbar.addView(iconButton(android.R.drawable.ic_menu_more, "گزینه‌ها") { showTopMenu() }, LinearLayout.LayoutParams(dp(48), dp(48)))
        root.addView(toolbar)

        pager = ViewPager2(this).apply {
            orientation = ViewPager2.ORIENTATION_HORIZONTAL
            clipToPadding = false
            clipChildren = false
            offscreenPageLimit = 2
            setPageTransformer { page, position ->
                val p = kotlin.math.abs(position).coerceAtMost(1f)
                page.scaleY = .94f + (.06f * (1f - p))
                page.alpha = .68f + (.32f * (1f - p))
            }
        }
        pagerAdapter = CardsAdapter()
        pager.adapter = pagerAdapter
        root.addView(pager, LinearLayout.LayoutParams(-1, dp(270)))

        root.addView(TextView(this).apply { text = "‹   برای دیدن کارت بعدی بکشید   ›"; gravity = Gravity.CENTER; textSize = 12f; setTextColor(onSurfaceVariant()) }, LinearLayout.LayoutParams(-1, dp(34)))

        val add = TextView(this).apply {
            text = "＋  افزودن کارت جدید"
            gravity = Gravity.CENTER
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(accent())
            background = roundedStroke(accent(), 1, 16)
            isClickable = true
            setOnClickListener { showWizard(null) }
        }
        root.addView(add, LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(10) })
        setContentView(root)
    }

    private fun loadCards() {
        Thread { val result = repo.getCards(); runOnUiThread { cards = result.toMutableList(); pagerAdapter.notifyDataSetChanged() } }.start()
    }

    private inner class CardsAdapter : RecyclerView.Adapter<CardsAdapter.Holder>() {
        inner class Holder(val page: FramePage) : RecyclerView.ViewHolder(page)
        override fun getItemCount() = cards.size
        override fun onCreateViewHolder(parent: ViewGroup, type: Int) = Holder(FramePage(parent.context))
        override fun onBindViewHolder(holder: Holder, position: Int) {
            holder.page.removeAllViews()
            holder.page.addView(createBankCard(cards[position]), FramePage.params())
        }
    }

    private class FramePage(context: Context) : LinearLayout(context) {
        init { orientation = VERTICAL; gravity = Gravity.CENTER; layoutParams = RecyclerView.LayoutParams(-1, -1) }
        companion object { fun params() = LayoutParams((ResourcesHolder.width * .90f).toInt(), LayoutParams.WRAP_CONTENT).apply { gravity = Gravity.CENTER } }
        object ResourcesHolder { var width = 1080 }
        override fun onMeasure(w: Int, h: Int) { ResourcesHolder.width = MeasureSpec.getSize(w); super.onMeasure(w, h) }
    }

    private fun createBankCard(card: BankCard): View {
        val visual = card.visual
        val primary = visual?.color ?: Color.rgb(55, 67, 82)
        val cardView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(18), dp(22), dp(16))
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(primary, darken(primary))).apply { cornerRadius = dp(22).toFloat() }
            elevation = dp(7).toFloat()
            isClickable = true
            setOnClickListener { showActions(card) }
        }
        val top = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL }
        val logo = ImageView(this).apply { scaleType = ImageView.ScaleType.CENTER_INSIDE; visual?.logoResourceName?.let { n -> resources.getIdentifier(n, "drawable", packageName).takeIf { it != 0 }?.let(::setImageResource) } }
        top.addView(logo, LinearLayout.LayoutParams(dp(42), dp(42)))
        top.addView(TextView(this).apply { text = visual?.persianName ?: card.bankId; textSize = 16f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE); gravity = Gravity.CENTER_VERTICAL; setPadding(dp(10), 0, 0, 0) }, LinearLayout.LayoutParams(0, dp(42), 1f))
        top.addView(iconButton(android.R.drawable.ic_menu_more, "عملیات کارت") { showActions(card) }, LinearLayout.LayoutParams(dp(42), dp(42)))
        cardView.addView(top)

        cardView.addView(TextView(this).apply {
            text = repo.normalizeCard(card.cardNumber).chunked(4).joinToString("   ")
            textSize = 20f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            letterSpacing = .035f
            textDirection = View.TEXT_DIRECTION_LTR
            gravity = Gravity.CENTER
            includeFontPadding = false
            setTextColor(Color.WHITE)
        }, LinearLayout.LayoutParams(-1, dp(78)).apply { topMargin = dp(10) })

        val bottom = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL }
        bottom.addView(TextView(this).apply { text = card.holderName.ifBlank { "نام صاحب کارت" }; textSize = 14f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE) }, LinearLayout.LayoutParams(0, dp(28), 1f))
        bottom.addView(TextView(this).apply { text = "کارت بانکی"; textSize = 10f; setTextColor(Color.argb(190,255,255,255)) })
        cardView.addView(bottom)
        return cardView
    }

    private fun showTopMenu() {
        val sheet = BottomSheetDialog(this)
        val root = sheetRoot("کارت‌های بانکی")
        actionRow(root, android.R.drawable.ic_input_add, "افزودن کارت جدید") { sheet.dismiss(); showWizard(null) }
        actionRow(root, android.R.drawable.ic_menu_sort_by_size, "مرتب‌سازی کارت‌ها") { sheet.dismiss(); showSort() }
        sheet.setContentView(root); sheet.show()
    }

    private fun showActions(card: BankCard) {
        val sheet = BottomSheetDialog(this)
        val root = sheetRoot(card.visual?.persianName ?: "کارت بانکی")
        actionRow(root, android.R.drawable.ic_menu_edit, "ویرایش کارت") { sheet.dismiss(); showWizard(card) }
        actionRow(root, android.R.drawable.ic_menu_copy, "کپی شماره کارت") { sheet.dismiss(); copy(repo.normalizeCard(card.cardNumber)) }
        if (card.iban.isNotBlank()) actionRow(root, android.R.drawable.ic_menu_copy, "کپی شماره شبا") { sheet.dismiss(); copy(repo.normalizeIban(card.iban)) }
        actionRow(root, android.R.drawable.ic_menu_share, "اشتراک‌گذاری") { sheet.dismiss(); share(card) }
        actionRow(root, android.R.drawable.ic_menu_delete, "حذف کارت", true) { sheet.dismiss(); MaterialAlertDialogBuilder(this).setTitle("حذف کارت").setMessage("این کارت حذف شود؟").setNegativeButton("انصراف", null).setPositiveButton("حذف") { _, _ -> Thread { repo.delete(card); runOnUiThread { loadCards() } }.start() }.show() }
        sheet.setContentView(root); sheet.show()
    }

    private fun showSort() {
        val sheet = BottomSheetDialog(this); val root = sheetRoot("مرتب‌سازی")
        cards.forEachIndexed { i, card ->
            val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL }
            row.addView(TextView(this).apply { text = card.visual?.persianName ?: card.bankId; textSize = 15f }, LinearLayout.LayoutParams(0, dp(56), 1f))
            row.addView(iconButton(android.R.drawable.arrow_up_float, "بالا") { if (i > 0) { move(i, i-1); sheet.dismiss(); showSort() } }, LinearLayout.LayoutParams(dp(48), dp(48)))
            row.addView(iconButton(android.R.drawable.arrow_down_float, "پایین") { if (i < cards.lastIndex) { move(i, i+1); sheet.dismiss(); showSort() } }, LinearLayout.LayoutParams(dp(48), dp(48)))
            root.addView(row)
        }
        sheet.setContentView(root); sheet.show()
    }
    private fun move(from: Int, to: Int) { val c = cards.removeAt(from); cards.add(to, c); pagerAdapter.notifyDataSetChanged(); Thread { repo.reorder(cards) }.start() }

    private fun showWizard(existing: BankCard?) { wizard?.dismiss(); wizard = Wizard(existing).also { it.show() } }

    private inner class Wizard(private val existing: BankCard?) {
        private val sheet = BottomSheetDialog(this@BankCardsActivity)
        private val root = LinearLayout(this@BankCardsActivity).apply { orientation = VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL; setPadding(dp(20), dp(10), dp(20), dp(28)) }
        private val groups = Array(4) { EditText(this@BankCardsActivity).apply { inputType = InputType.TYPE_CLASS_NUMBER; gravity = Gravity.CENTER; textSize = 16f; hint = "••••"; maxLines = 1; background = roundedStroke(onSurfaceVariant(), 1, 12) } }
        private val holder = EditText(this@BankCardsActivity).apply { hint = "نام صاحب کارت (اختیاری)"; singleLine = true }
        private val iban = EditText(this@BankCardsActivity).apply { hint = "شماره شبا (اختیاری)"; singleLine = true; inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_CLASS_NUMBER }
        private var preview: LinearLayout? = null
        fun show() { buildStep1(); sheet.setContentView(root); sheet.show(); if (existing == null) paste() else setCard(existing.cardNumber) }
        fun dismiss() = sheet.dismiss()
        private fun title(text: String) { val row = LinearLayout(this@BankCardsActivity).apply { gravity = Gravity.CENTER_VERTICAL }; row.addView(TextView(this@BankCardsActivity).apply { this.text=text; textSize=20f; typeface=Typeface.DEFAULT_BOLD }, LinearLayout.LayoutParams(0,dp(52),1f)); row.addView(iconButton(android.R.drawable.ic_menu_camera,"اسکن کارت") { scanner.launch(Intent(this@BankCardsActivity, BankCardScannerActivity::class.java).putExtra(BankCardScannerActivity.EXTRA_RETURN_TO_FORM,true)) }, LinearLayout.LayoutParams(dp(48),dp(48))); root.addView(row) }
        private fun buildStep1() { root.removeAllViews(); title("افزودن کارت"); root.addView(stepLabel("۱  شماره کارت")); addPreview(); val row=LinearLayout(this@BankCardsActivity).apply{gravity=Gravity.CENTER;layoutDirection=View.LAYOUT_DIRECTION_LTR}; groups.forEachIndexed{index,e->row.addView(e,LinearLayout.LayoutParams(0,dp(54),1f).apply{if(index<3)marginEnd=dp(7)})};root.addView(row);root.addView(primaryAction("ادامه"){if(valid())buildStep2()}); groups.forEach{it.setOnFocusChangeListener{_,_->updatePreview()};it.addTextChangedListener(SimpleWatcher{updatePreview()})};updatePreview() }
        private fun buildStep2(){root.removeAllViews();title("افزودن کارت");root.addView(stepLabel("۲  نام صاحب کارت  ·  اختیاری"));addPreview();root.addView(holder,LinearLayout.LayoutParams(-1,dp(58)).apply{topMargin=dp(16)});root.addView(primaryAction("ادامه"){buildStep3()});updatePreview()}
        private fun buildStep3(){root.removeAllViews();title("افزودن کارت");root.addView(stepLabel("۳  شماره شبا  ·  اختیاری"));addPreview();root.addView(iban,LinearLayout.LayoutParams(-1,dp(58)).apply{topMargin=dp(16)});root.addView(primaryAction(if(existing==null)"ذخیره کارت" else "ذخیره تغییرات"){save()});updatePreview()}
        private fun stepLabel(t:String)=TextView(this@BankCardsActivity).apply{text=t;textSize=13f;setTextColor(onSurfaceVariant());setPadding(0,dp(6),0,0)}
        private fun addPreview(){preview=LinearLayout(this@BankCardsActivity).apply{orientation=VERTICAL;setPadding(dp(20),dp(15),dp(20),dp(15));gravity=Gravity.CENTER};root.addView(preview,LinearLayout.LayoutParams(-1,dp(190)).apply{topMargin=dp(8)})}
        private fun updatePreview(){val p=preview?:return;p.removeAllViews();val number=groups.joinToString(""){repo.normalizeCard(it.text.toString())};val v=repo.detect(number);val c=v?.color?:Color.rgb(55,67,82);p.background=GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(c,darken(c))).apply{cornerRadius=dp(20).toFloat()};p.addView(TextView(this@BankCardsActivity).apply{text=v?.persianName?:"بانک کارت";textSize=14f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.WHITE)});p.addView(TextView(this@BankCardsActivity).apply{text=number.chunked(4).joinToString("   ").ifBlank{"••••   ••••   ••••   ••••"};textSize=19f;typeface=Typeface.MONOSPACE;gravity=Gravity.CENTER;textDirection=View.TEXT_DIRECTION_LTR;setTextColor(Color.WHITE);setPadding(0,dp(28),0,dp(12))});if(holder.text?.isNotBlank()==true)p.addView(TextView(this@BankCardsActivity).apply{text=holder.text.toString();textSize=12f;setTextColor(Color.WHITE)})}
        private fun valid():Boolean{val n=groups.joinToString(""){repo.normalizeCard(it.text.toString())};val ok=n.length==16&&repo.validCard(n)&&repo.detect(n)!=null;if(!ok)Toast.makeText(this@BankCardsActivity,"شماره کارت معتبر نیست",Toast.LENGTH_SHORT).show();return ok}
        private fun save(){val n=groups.joinToString(""){repo.normalizeCard(it.text.toString())};val h=holder.text?.toString().orEmpty().trim();val ib=repo.normalizeIban(iban.text?.toString().orEmpty());if(n.length!=16||!repo.validCard(n)||repo.detect(n)==null||!repo.validIban(ib)){Toast.makeText(this@BankCardsActivity,"اطلاعات کارت معتبر نیست",Toast.LENGTH_SHORT).show();return};Thread{val r=repo.save(existing,n,h,ib);runOnUiThread{if(r.isSuccess){sheet.dismiss();wizard=null;loadCards()}else Toast.makeText(this@BankCardsActivity,"ذخیره کارت انجام نشد",Toast.LENGTH_SHORT).show()}}.start()}
        private fun paste(){val cm=getSystemService(CLIPBOARD_SERVICE) as ClipboardManager;val raw=cm.primaryClip?.getItemAt(0)?.coerceToText(this@BankCardsActivity)?.toString().orEmpty();val n=repo.normalizeCard(raw);if(n.length==16&&repo.detect(n)!=null)setCard(n)}
        private fun setCard(n:String){n.chunked(4).take(4).forEachIndexed{i,s->groups[i].setText(s)}}
        fun applyScan(n:String,h:String,ib:String){setCard(n);if(h.isNotBlank())holder.setText(h);if(ib.isNotBlank())iban.setText(ib);updatePreview()}
    }

    private class SimpleWatcher(val f:()->Unit):android.text.TextWatcher{override fun beforeTextChanged(s:CharSequence?,st:Int,c:Int,a:Int){};override fun onTextChanged(s:CharSequence?,st:Int,b:Int,c:Int){f()};override fun afterTextChanged(e:android.text.Editable?) {}}

    private fun sheetRoot(t:String)=LinearLayout(this).apply{orientation=VERTICAL;layoutDirection=View.LAYOUT_DIRECTION_RTL;setPadding(dp(20),dp(16),dp(20),dp(30));addView(TextView(this@BankCardsActivity).apply{text=t;textSize=20f;typeface=Typeface.DEFAULT_BOLD;setTextColor(onSurface())},LinearLayout.LayoutParams(-1,dp(44)))}
    private fun actionRow(root:LinearLayout,res:Int,label:String,danger:Boolean=false,action:()->Unit){val row=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL;layoutDirection=View.LAYOUT_DIRECTION_RTL;setPadding(dp(4),0,dp(4),0);isClickable=true;setOnClickListener{action()}};row.addView(ImageView(this).apply{setImageResource(res);setColorFilter(if(danger)Color.rgb(190,45,55) else onSurfaceVariant())},LinearLayout.LayoutParams(dp(40),dp(52)));row.addView(TextView(this).apply{text=label;textSize=15f;setTextColor(if(danger)Color.rgb(190,45,55) else onSurface())},LinearLayout.LayoutParams(0,dp(52),1f));root.addView(row)}
    private fun primaryAction(label:String,action:()->Unit)=TextView(this).apply{text=label;gravity=Gravity.CENTER;textSize=15f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.WHITE);background=GradientDrawable().apply{setColor(accent());cornerRadius=dp(16).toFloat()};isClickable=true;setOnClickListener{action()};layoutParams=LinearLayout.LayoutParams(-1,dp(52)).apply{topMargin=dp(16)}}
    private fun iconButton(res:Int,desc:String,action:()->Unit)=ImageButton(this).apply{setImageResource(res);contentDescription=desc;background=transparentCircle();setColorFilter(onSurface());setOnClickListener{action()}}
    private fun copy(v:String){(getSystemService(CLIPBOARD_SERVICE)as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Messages",v));Toast.makeText(this,"کپی شد",Toast.LENGTH_SHORT).show()}
    private fun share(c:BankCard){val text=buildString{append(c.visual?.persianName?:"کارت بانکی").append('\n');append(repo.normalizeCard(c.cardNumber)).append('\n');if(c.holderName.isNotBlank())append(c.holderName).append('\n');if(c.iban.isNotBlank())append(repo.normalizeIban(c.iban))};startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply{type="text/plain";putExtra(Intent.EXTRA_TEXT,text)},"اشتراک‌گذاری"))}
    private fun roundedStroke(color:Int,stroke:Int,radius:Int)=GradientDrawable().apply{setColor(Color.TRANSPARENT);setStroke(dp(stroke),color);cornerRadius=dp(radius).toFloat()}
    private fun transparentCircle()=GradientDrawable().apply{shape=GradientDrawable.OVAL;setColor(Color.TRANSPARENT)}
    private fun surface()=resolve(com.google.android.material.R.attr.colorSurface,Color.WHITE);private fun onSurface()=resolve(com.google.android.material.R.attr.colorOnSurface,Color.BLACK);private fun onSurfaceVariant()=resolve(com.google.android.material.R.attr.colorOnSurfaceVariant,Color.DKGRAY);private fun accent()=resolve(com.google.android.material.R.attr.colorPrimary,Color.rgb(35,90,150));private fun resolve(a:Int,f:Int):Int{val t=android.util.TypedValue();return if(theme.resolveAttribute(a,t,true)){if(t.resourceId!=0)runCatching{getColor(t.resourceId)}.getOrDefault(f)else t.data}else f}
    private fun darken(c:Int)=Color.rgb((Color.red(c)*.74f).toInt(),(Color.green(c)*.74f).toInt(),(Color.blue(c)*.74f).toInt());private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
}
