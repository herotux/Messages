package org.fossify.messages.activities

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import com.google.android.material.card.MaterialCardView
import org.fossify.commons.extensions.getProperBackgroundColor
import org.fossify.commons.extensions.getProperPrimaryColor
import org.fossify.commons.extensions.getProperTextColor
import org.fossify.commons.extensions.updateTextColors
import org.fossify.messages.BuildConfig

class HerotuxAboutActivity : SimpleActivity() {
    companion object {
        private const val EXTRA_CONTACT_PAGE = "contact_page"
        private const val EXTRA_THEME_BUILDER = "theme_builder"
        private const val EXTRA_THEME_ID = "theme_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        when {
            intent.getBooleanExtra(EXTRA_THEME_BUILDER, false) -> {
                startActivity(Intent(this, ThemeBuilderActivity::class.java).apply {
                    putExtra(ThemeBuilderActivity.EXTRA_THEME_ID, intent.getStringExtra(EXTRA_THEME_ID))
                })
                finish()
            }
            intent.getBooleanExtra(EXTRA_CONTACT_PAGE, false) -> renderContactPage()
            else -> renderAboutPage()
        }
    }

    private fun renderAboutPage() {
        val bg = getProperBackgroundColor(); val text = getProperTextColor(); val primary = getProperPrimaryColor()
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(bg) }
        val toolbar = Toolbar(this).apply { title = "درباره هما"; setTitleTextColor(text); navigationIcon = getDrawable(androidx.appcompat.R.drawable.abc_ic_ab_back_material); setNavigationOnClickListener { finish() }; elevation = dp(2).toFloat() }
        root.addView(toolbar, LinearLayout.LayoutParams(-1, dp(56)))
        val scroll = ScrollView(this).apply { isFillViewport = true; clipToPadding = false; setPadding(dp(24), dp(24), dp(24), dp(32)) }
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL }
        content.addView(infoText("هما", 34f, true, primary), LinearLayout.LayoutParams(-1, -2)); content.addView(infoText("پیام‌رسانک SMS و MMS", 18f, true, text), LinearLayout.LayoutParams(-1, -2)); content.addView(infoText("توسعه‌یافته توسط HEROTUX", 15f, false, text), LinearLayout.LayoutParams(-1, -2))
        val card = MaterialCardView(this).apply { radius = dp(18).toFloat(); cardElevation = dp(2).toFloat(); setContentPadding(dp(20), dp(18), dp(20), dp(18)); strokeWidth = dp(1); strokeColor = primary; setCardBackgroundColor(bg) }
        val cardText = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }; cardText.addView(infoText("درباره هما", 17f, true, text)); cardText.addView(infoText("هما یک برنامه سبک و حریم‌خصوصی‌محور برای مدیریت پیامک و MMS است.", 15f, false, text)); cardText.addView(infoText("نسخه ${BuildConfig.VERSION_NAME}", 14f, false, text)); card.addView(cardText); content.addView(card, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(28) })
        content.addView(infoText("اطلاعات تماس\nContact information", 16f, true, primary).apply { gravity = Gravity.CENTER; setPadding(dp(12), dp(22), dp(12), dp(12)); isClickable = true; setOnClickListener { startActivity(Intent(this@HerotuxAboutActivity, HerotuxAboutActivity::class.java).putExtra(EXTRA_CONTACT_PAGE, true)) } }, LinearLayout.LayoutParams(-1, -2))
        content.addView(infoText("herotux.github.io", 14f, false, primary).apply { gravity = Gravity.CENTER; setOnClickListener { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://herotux.github.io"))) } }, LinearLayout.LayoutParams(-1, -2))
        content.addView(infoText("HEROTUX · Software Development", 13f, false, text).apply { gravity = Gravity.CENTER; alpha = 0.62f }, LinearLayout.LayoutParams(-1, -2)); scroll.addView(content); root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f)); setContentView(root); updateTextColors(root)
    }

    private fun renderContactPage() {
        val bg = getProperBackgroundColor(); val text = getProperTextColor(); val primary = getProperPrimaryColor()
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(bg) }; val toolbar = Toolbar(this).apply { title = "اطلاعات تماس"; setTitleTextColor(text); navigationIcon = getDrawable(androidx.appcompat.R.drawable.abc_ic_ab_back_material); setNavigationOnClickListener { finish() } }; root.addView(toolbar, LinearLayout.LayoutParams(-1, dp(56)))
        val scroll = ScrollView(this).apply { setPadding(dp(20), dp(20), dp(20), dp(28)) }; val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL; textDirection = View.TEXT_DIRECTION_RTL }
        content.addView(infoText("ارتباط با هما", 24f, true, text).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(-1, -2)); content.addView(infoText("برای پشتیبانی، پیشنهادها و گزارش مشکلات با ما در تماس باشید.", 14f, false, text).apply { gravity = Gravity.CENTER; alpha = 0.75f }, LinearLayout.LayoutParams(-1, -2))
        contactCard(content, "ایمیل", "thefreetux@gmail.com", primary, text) { startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:thefreetux@gmail.com"))) }; contactCard(content, "تلفن پشتیبانی", "09375647544", primary, text) { startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:09375647544"))) }; contactCard(content, "وب‌سایت", "herotux.github.io", primary, text) { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://herotux.github.io"))) }
        scroll.addView(content); root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f)); setContentView(root); updateTextColors(root)
    }

    private fun contactCard(root: LinearLayout, label: String, value: String, primary: Int, text: Int, onClick: () -> Unit) {
        val card = MaterialCardView(this).apply { radius = dp(18).toFloat(); cardElevation = 0f; strokeWidth = dp(1); strokeColor = primary; setCardBackgroundColor(getProperBackgroundColor()); isClickable = true; setOnClickListener { onClick() } }
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(14), dp(18), dp(14)) }; content.addView(infoText(label, 13f, false, text)); content.addView(infoText(value, 17f, true, primary)); card.addView(content); root.addView(card, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })
    }

    private fun infoText(value: String, size: Float, bold: Boolean, color: Int) = TextView(this).apply { text = value; textSize = size; setTextColor(color); if (bold) typeface = Typeface.DEFAULT_BOLD; setPadding(0, dp(5), 0, dp(5)) }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}